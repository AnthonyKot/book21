#!/usr/bin/env python3
"""Local synthetic review trace; requires Python3, curl and --lab.fixtures=true."""
import argparse,json,subprocess,tempfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--port',type=int,default=8092);args=p.parse_args()
base=f'http://127.0.0.1:{args.port}'
with tempfile.TemporaryDirectory(prefix='book21-review-') as tmp:
 jar=str(Path(tmp)/'cookies');token=''
 def call(path,user='alice',post=False,save=False):
  cmd=['curl','-sS','--max-time','10','-w','\n%{http_code}',base+path]
  if user:cmd+=['-u',user+':local-only']
  if save:cmd+=['-c',jar]
  if post:cmd+=['-X','POST','-b',jar,'-H','X-CSRF-TOKEN: '+token]
  r=subprocess.run(cmd,capture_output=True,text=True,check=True);body,status=r.stdout.rsplit('\n',1);return int(status),body
 status,body=call('/csrf',save=True);assert status==200;token=json.loads(body)['token']
 def reset():assert call('/lab/reset',post=True)[0]==200,'Start with --lab.fixtures=true'
 def observe(name,user,id,route='preview'):
  status,body=call('/api/documents/'+id+'/'+route,user)
  code,state=call('/lab/state');assert code==200
  print(json.dumps({'case':name,'status':status,'body':body,'cache':json.loads(state)}))
 reset()
 observe('foreign-cold','bob','C-1001')
 observe('owner-warms','alice','C-1001')
 observe('foreign-warm','bob','C-1001')
 observe('owner-repeat','alice','C-1001')
 reset()
 observe('reverse-cold','alice','B-2001')
 observe('reverse-owner','bob','B-2001')
 observe('reverse-warm','alice','B-2001')
 observe('anonymous-warm','','B-2001')
