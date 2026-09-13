import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {marked, Renderer} from 'marked';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const out = path.join(root, 'docs');
const title = 'Security Is an Engineering Job';
const essays = [
  {slug:'01-which-security-job',part:'Change the engineering question',title:'Which security job uses what you already know?',payoff:'Choose the responsibility that builds on your engineering experience, and identify the evidence you still need.'},
  {slug:'02-security-requirement',part:'Change the engineering question',title:'A security requirement must survive a hostile input',payoff:'Turn a document-export feature into testable rules for identity, permission and changes over time.'},
  {slug:'03-draw-the-boundary',part:'Change the engineering question',title:'Draw the boundary before choosing the control',payoff:'Trace who is asking through every component, find where trust changes, and pick controls that fit those crossings.'},
  {slug:'04-wrong-invoice',part:'Find and repair application failures',title:'A valid user can still read the wrong invoice',payoff:'Reproduce cross-tenant access in a Spring Boot lab, repair the lookup, and follow the same check into the export worker and download.'},
  {slug:'05-token-boundary',part:'Find and repair application failures',title:'A signed token answers only the question you validate',payoff:'Reject a valid token intended for another API, then examine purpose, account recovery and the limits of local verification.'},
  {slug:'06-browser-authority',part:'Find and repair application failures',title:'The browser brings authority to the request',payoff:'Trace a forged form and an injected script through one browser session, and repair each at its actual boundary.'},
];
const escape = s => s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const renderer = new Renderer();
renderer.link = function(token) {
  return Renderer.prototype.link.call(this, {...token,href:token.href.replace(/\.md(?=#|$)/,'.html')});
};
renderer.table = function(token) {
  return `<div class="table-scroll" tabindex="0" role="region" aria-label="Table; scroll horizontally on small screens">${Renderer.prototype.table.call(this,token)}</div>`;
};
marked.use({renderer});
function shell(name, description, body, prefix='') {
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${escape(name)} · Book 21</title><meta name="description" content="${escape(description)}">
<meta property="og:title" content="${escape(name)}"><meta property="og:description" content="${escape(description)}">
<meta name="color-scheme" content="light dark"><link rel="icon" href="data:,">
<link rel="stylesheet" href="${prefix}assets/style.css"></head>
<body><a class="skip" href="#main">Skip to content</a><header class="site-header"><a href="${prefix}index.html">${title}</a><nav aria-label="Book"><a href="${prefix}index.html#essays">Essays</a><a href="${prefix}about.html">About</a></nav></header>
<main id="main">${body}</main><footer><span>Book 21 · Learn, practise, verify.</span><a href="https://github.com/AnthonyKot/book21">Source on GitHub</a></footer></body></html>`;
}
function write(file,content) {const dest=path.join(out,file);fs.mkdirSync(path.dirname(dest),{recursive:true});fs.writeFileSync(dest,content);}
function markdown(file) {return marked.parse(fs.readFileSync(path.join(root,file),'utf8').replace('<!--mission-->','<hr>'));}
write('index.html',shell(title,'From experienced developer to application and product security in the age of AI.',`
<p class="kicker">Book 21 · A book in progress</p><h1>${title}</h1><p class="lede">From experienced developer to application and product security in the age of AI.</p>
<p>Learn to investigate a failure, repair its cause and verify the result. These essays connect security reasoning to the software you already know how to build, with worked examples and practice.</p>
<p class="status">Six essays available. New chapters will extend the path into application security, secure delivery and AI systems.</p>
<section id="essays"><h2>Start reading</h2><ol class="essay-list">${essays.map(e=>`<li><h3><a href="essays/${e.slug}.html">${escape(e.title)}</a></h3><p>${e.payoff}</p></li>`).join('')}</ol></section>
<section><h2>Learn by making a decision</h2><p>The opening essays use a document-export service to connect roles, requirements and security evidence. Read in order, attempt the exercise, then compare your reasoning with the review notes where provided.</p><a href="essays/${essays[0].slug}.html">Begin with essay 1 →</a></section>`));
for (const [i,e] of essays.entries()) {
  const prev=essays[i-1],next=essays[i+1];
  write(`essays/${e.slug}.html`,shell(e.title,e.payoff,`<p class="kicker">Essay ${i+1} · ${e.part}</p><article>${markdown(`essays/${e.slug}.md`)}</article><nav class="pager" aria-label="Adjacent essays">${prev?`<a href="${prev.slug}.html">← Essay ${i}</a>`:'<a href="../index.html">← Contents</a>'}${next?`<a href="${next.slug}.html">Essay ${i+2} →</a>`:'<a href="../index.html">Back to the book →</a>'}</nav>`,'../'));
}
const practice=[
  {n:2,essay:'02-security-requirement',sheet:'02-export-contract',review:'02-export-review',name:'Export contract'},
  {n:3,essay:'03-draw-the-boundary',sheet:'03-boundary-worksheet',review:'03-boundary-review',name:'Scheduled export boundary'},
  {n:4,essay:'04-wrong-invoice',sheet:'04-lab-worksheet',review:'04-lab-review',name:'Export permission lab'},
  {n:5,essay:'05-token-boundary',sheet:'05-token-worksheet',review:'05-token-review',name:'Token acceptance'},
  {n:6,essay:'06-browser-authority',sheet:'06-browser-worksheet',review:'06-browser-review',name:'Browser authority'},
];
for (const p of practice) for (const slug of [p.sheet,p.review]) {
  const isSheet=slug===p.sheet, name=`${p.name} ${isSheet?'worksheet':'review'}`;
  write(`practice/${slug}.html`,shell(name,`${name} for essay ${p.n}.`,`<p class="kicker">Essay ${p.n} · Practice</p><article>${markdown(`practice/${slug}.md`)}</article><div class="practice-actions"><button onclick="window.print()">Print this page</button><a href="../essays/${p.essay}.html">Return to essay ${p.n}</a>${isSheet?`<a href="${p.review}.html">Review after your attempt →</a>`:`<a href="${p.sheet}.html">Back to worksheet</a>`}</div>`,'../'));
}
write('about.html',shell('About the book','The purpose and method of Book 21.',`<p class="kicker">About the book</p><h1>Security through engineering practice</h1><p>This book is for experienced developers moving toward application and product security, whether inside a product team or through a bounded consulting engagement.</p><p>Each essay explains a mechanism or decision through a concrete example, then asks you to apply it. Reading, solving with help and demonstrating an independent result are different kinds of progress.</p><h2>Sources and limits</h2><p>Sources appear beside the claims they support. Constructed examples are labelled. These essays are source-checked drafts published for reading. Some have also received review from a second AI model, with findings checked against the sources and examples; this is not an independent human security audit. Essays 2 and 3 are design exercises; from essay 4 the exercises include a local lab whose tests were run while writing.</p><p>The book does not promise a salary, certification or protection from changes in the job market. Its purpose is to help you build inspectable evidence of security capability.</p><p><a href="index.html#essays">Read the essays →</a></p>`));
write('.nojekyll','');
write('assets/style.css',fs.readFileSync(path.join(root,'site/style.css'),'utf8'));
console.log(`Built homepage, about page, ${essays.length} essays and ${practice.length*2} practice pages.`);
