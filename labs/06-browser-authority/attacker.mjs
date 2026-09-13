import http from 'node:http';
import {pathToFileURL} from 'node:url';
export function attackerServer() {
  return http.createServer((req,res) => {
    // Never log cookies: ports share cookie scope on this loopback host.
    res.writeHead(200, {'Content-Type':'text/html; charset=utf-8'});
    res.end(`<!doctype html><html lang="en"><meta charset="utf-8"><title>Local forged form</title>
<h1>Untrusted local page</h1><form method="post" action="http://127.0.0.1:8083/api/approve">
<input type="hidden" name="invoice" value="C-1001"><button>Submit forged approval</button></form></html>`);
  });
}
if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  attackerServer().listen(8084,'127.0.0.1',()=>console.log('Local fixture: http://127.0.0.1:8084'));
}
