import {chromium} from 'playwright';
import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import {once} from 'node:events';
import {setTimeout as delay} from 'node:timers/promises';
import {attackerServer} from './attacker.mjs';
const base='http://127.0.0.1:8083', evil='http://127.0.0.1:8084';
const negative=process.argv.includes('--negative');
const practice=process.argv.includes('--practice');
let failures=0, passes=0;
const server=attackerServer();
server.listen(8084,'127.0.0.1'); await once(server,'listening');
const browser=await chromium.launch({headless:true});
console.log(`Playwright Chromium ${browser.version()}; negative=${negative}; practice=${practice}`);
async function login(page) {
  await page.goto(base+'/login');
  await page.locator('[name=username]').fill('alice');
  await page.locator('[name=password]').fill('local-only');
  await Promise.all([page.waitForURL(base+'/'),page.getByRole('button',{name:'Sign in'}).click()]);
}
async function state(context) { return (await context.request.get(base+'/api/state')).json(); }
const payload=`<img src="/missing-image" onerror="fetch('/csrf').then(r=>r.json()).then(c=>fetch('/api/approve',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded',[c.header]:c.token},body:'invoice=C-1001'})).then(r=>{document.body.dataset.attack=String(r.status)})">`;
async function check(name,fn) {
  const context=await browser.newContext(), page=await context.newPage();
  try { await login(page); await fn(page,context); passes++; console.log('PASS '+name); }
  catch(e) { failures++; console.log('FAIL '+name+' '+e.message); }
  finally { await context.close(); }
}
try {
  const modes=negative||practice ? [['fixed',true,true]] : [['vulnerable',false,false],['csrf-only',true,false],['fixed',true,true]];
  for(const [name,csrf,safe] of modes) {
    const child=spawn('java',['-jar','target/browser-authority-1.0.jar',`--lab.csrf=${negative?false:csrf}`,`--lab.safe-render=${negative?false:safe}`],{stdio:['ignore','pipe','pipe']});
    let output=''; child.stdout.on('data',d=>output+=d); child.stderr.on('data',d=>output+=d);
    try {
      let ready=false;
      for(let i=0;i<100;i++) { if(child.exitCode!==null) throw Error(output); try { const r=await fetch(base+'/login'); if(r.ok) {ready=true;break;} } catch {} await delay(100); }
      if(!ready) throw Error('Server startup timeout '+output);
      if(practice) {
        await check('summary rejects executable title',async(p,c)=>{
          await p.goto(base+'/summary?title='+encodeURIComponent(payload));
          assert.equal(await p.locator('#summary strong').textContent(),payload);
          assert.equal(await p.locator('#summary img').count(),0);
          assert.equal((await state(c)).approved,false);
        });
        await check('summary preserves text and strong structure',async(p)=>{
          const title='Cedar < Q4 & "review"';
          await p.goto(base+'/summary?title='+encodeURIComponent(title));
          assert.equal(await p.locator('#summary > strong').count(),1);
          assert.equal(await p.locator('#summary strong').textContent(),title);
        });
        continue;
      }
      await check(name+' forged form',async(p,c)=>{
        await p.goto(evil);
        const response=p.waitForResponse(r=>r.url()===base+'/api/approve'&&r.request().method()==='POST');
        await p.getByRole('button').click(); const r=await response;
        const headers=await r.request().allHeaders();
        assert.equal(headers.origin,evil); assert.equal(headers['sec-fetch-site'],'same-site');
        assert.ok(headers.cookie?.includes('JSESSIONID='));
        assert.equal(r.status(),csrf?403:200); assert.equal((await state(c)).approved,!csrf);
        console.log(`  Origin=${headers.origin} Sec-Fetch-Site=${headers['sec-fetch-site']} session-cookie=present HTTP=${r.status()}`);
      });
      await check(name+' simple fetch response blocked',async(p,c)=>{
        await p.goto(evil);
        const result=await p.evaluate(async(base)=>{
          try { await fetch(base+'/api/approve',{method:'POST',credentials:'include',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'invoice=C-1001'});return 'readable'; }
          catch {return 'blocked';}
        },base);
        assert.equal(result,'blocked'); assert.equal((await state(c)).approved,!csrf);
      });
      await check(name+' custom-header preflight stops POST',async(p,c)=>{
        await p.goto(evil); const traceStart=output.length;
        const result=await p.evaluate(async(base)=>{
          try {await fetch(base+'/api/approve',{method:'POST',credentials:'include',headers:{'Content-Type':'application/json','X-Lab':'custom'},body:'{"invoice":"C-1001"}'});return 'readable';}catch{return 'blocked';}
        },base);
        assert.equal(result,'blocked');
        for(let i=0;i<100&&!output.slice(traceStart).includes('HTTP OPTIONS /api/approve');i++) await delay(20);
        const received=output.slice(traceStart);
        assert.ok(received.includes('HTTP OPTIONS /api/approve'));
        assert.ok(!received.includes('HTTP POST /api/approve'));
        assert.equal((await state(c)).approved,false);
        console.log('  server received OPTIONS; no approval POST');
      });
      await check(name+' injected script',async(p,c)=>{
        const cookie=(await c.cookies(base)).find(x=>x.name==='JSESSIONID');
        assert.equal(cookie.httpOnly,true); assert.equal(cookie.sameSite,'Strict');
        assert.equal(await p.evaluate(()=>document.cookie.includes('JSESSIONID=')),false);
        await p.goto(base+'/?note='+encodeURIComponent(payload));
        if(safe) {
          assert.equal(await p.locator('#note').textContent(),payload);
          assert.equal(await p.locator('#note img').count(),0);
          assert.equal((await state(c)).approved,false);
        } else {
          await p.waitForFunction(()=>document.body.dataset.attack==='200');
          assert.equal((await state(c)).approved,true);
        }
      });
      await check(name+' ordinary note remains readable',async(p)=>{
        const note='Cedar <b>Q4</b> & review';
        await p.goto(base+'/?note='+encodeURIComponent(note));
        assert.equal(await p.locator('#note').textContent(),safe?note:'Cedar Q4 & review');
      });
      await check(name+' legitimate browser approval',async(p,c)=>{
        await Promise.all([p.waitForURL(base+'/api/approve'),p.getByRole('button',{name:'Approve invoice'}).click()]);
        assert.equal((await state(c)).approved,true);
      });
    } finally {
      if(child.exitCode===null) { const done=once(child,'exit'); child.kill('SIGTERM'); await done; }
    }
  }
} finally { await browser.close(); await new Promise(resolve=>server.close(resolve)); }
console.log(`RESULT passes=${passes} failures=${failures}`);
if(failures) process.exitCode=1;
