#!/usr/bin/env python3
"""Controlled local curl schedule; requires lab.gates=true. Never run demos concurrently."""
import argparse,concurrent.futures,json,subprocess,tempfile,time
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--port',type=int,default=8091);p.add_argument('--first',choices=['A','B'],default='A');p.add_argument('--worker',action='store_true');args=p.parse_args()
base=f'http://127.0.0.1:{args.port}'
with tempfile.TemporaryDirectory(prefix='book21-credit-') as tmp:
 jar=str(Path(tmp)/'cookies')
 def call(path,post=False,headers=(),save_cookie=False):
  cmd=['curl','-sS','--max-time','20','-u','alice:local-only','-b',jar,'-w','\n%{http_code}',base+path]
  if save_cookie:cmd+=['-c',jar]
  if post:cmd+=['-X','POST','-H','X-CSRF-TOKEN: '+token]
  for h in headers:cmd+=['-H',h]
  r=subprocess.run(cmd,capture_output=True,text=True,check=True);body,status=r.stdout.rsplit('\n',1);return int(status),body
 status,body=call('/csrf',save_cookie=True);assert status==200;token=json.loads(body)['token']
 assert call('/lab/reset',True)[0]==200,'Start with --lab.gates=true'
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
  pending={lane:pool.submit(call,'/api/credits/C-1001/apply',True,['X-Lab-Lane: '+lane]) for lane in ['A','B']}
  try:
   end=time.monotonic()+10
   while True:
    status,body=call('/lab/gates');assert status==200
    if all(json.loads(body).values()):break
    if time.monotonic()>end:raise RuntimeError('Both requests did not reach the gate')
    time.sleep(.05)
   print('Both requests observed unused credit; neither has written.')
   assert call('/lab/release/'+args.first,True)[0]==200
   first=pending[args.first].result(timeout=10)
   other='B' if args.first=='A' else 'A'
   assert call('/lab/release/'+other,True)[0]==200
   second=pending[other].result(timeout=10)
   status,body=call('/api/state');assert status==200
   print(json.dumps({'first':args.first,'statuses':[first[0],second[0]],'state':json.loads(body)}))
  finally:
   for lane in ['A','B']:call('/lab/release/'+lane,True)
 if args.worker:
  assert call('/lab/reset',True)[0]==200
  status,body=call('/api/credits/C-1001/worker',True,['X-Lab-Fault: claim'])
  _,state=call('/api/state')
  print(json.dumps({'workerFaultStatus':status,'stateAfterFault':json.loads(state)}))
  status,body=call('/api/credits/C-1001/worker',True)
  _,state=call('/api/state')
  print(json.dumps({'workerRetryStatus':status,'stateAfterRetry':json.loads(state)}))
