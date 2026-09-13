import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve('docs');
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)]);}
const pages=walk(root).filter(f=>f.endsWith('.html'));
for(const file of pages){const html=fs.readFileSync(file,'utf8');
 if((html.match(/<h1\b/g)||[]).length!==1)throw Error(`Expected one h1: ${file}`);
 if(/\/home\/diablo|file:\/\/|\.md["#]/.test(html))throw Error(`Private or unconverted path: ${file}`);
 for(const [,href] of html.matchAll(/(?:href|src)="([^"]+)"/g)){
  if(/^(https?:|data:|#)/.test(href))continue;
  const target=path.resolve(path.dirname(file),href.split('#')[0]);
  if(!target.startsWith(root+path.sep)||!fs.existsSync(target))throw Error(`Broken link: ${file} -> ${href}`);
 }
}
if(pages.length!==9)throw Error(`Expected nine pages, got ${pages.length}`);
console.log('Checked nine pages, headings, local destinations and private-path exclusions.');
