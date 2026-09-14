#!/usr/bin/env python3
"""Exercise only this lab's loopback fixtures. Requires Python 3 and curl."""
import argparse
import json
import subprocess
import tempfile
from pathlib import Path
from xml.sax.saxutils import quoteattr

parser = argparse.ArgumentParser()
parser.add_argument('--port', type=int, default=8089)
parser.add_argument('--summary', action='store_true', help='also run the unfinished later consumer')
args = parser.parse_args()
base = f'http://127.0.0.1:{args.port}'
with tempfile.TemporaryDirectory(prefix='book21-curl-') as tmp:
    jar = str(Path(tmp) / 'cookies')
    token = None
    def call(path, body=None):
        cmd = ['curl', '-sS', '--max-time', '10', '-u', 'alice:local-only',
               '-b', jar, '-c', jar, '-w', '\n%{http_code}', base + path]
        if body is not None:
            cmd += ['-H', 'Content-Type: application/xml', '-H', 'X-CSRF-TOKEN: ' + token,
                    '--data-binary', '@-']
        result = subprocess.run(cmd, input=body, text=True, capture_output=True, check=True)
        text, status = result.stdout.rsplit('\n', 1)
        return int(status), text
    status, csrf = call('/csrf')
    assert status == 200, (status, csrf)
    token = json.loads(csrf)['token']
    status, targets = call('/lab/targets')
    assert status == 200
    targets = json.loads(targets)
    cases = [('ordinary', '<invoice><title>Cedar &amp; Sons</title></invoice>')]
    for kind in ['file', 'http']:
        cases.append((kind, '<!DOCTYPE invoice [<!ENTITY note SYSTEM ' + quoteattr(targets[kind])
                      + '>]><invoice><title>&note;</title></invoice>'))
    cases.append(('dtd', '<!DOCTYPE invoice SYSTEM ' + quoteattr(targets['dtd'])
                  + '><invoice><title>&note;</title></invoice>'))
    for name, xml in cases:
        _, before = call('/lab/targets')
        status, upload_id = call('/api/uploads', xml)
        assert status == 201, (status, upload_id)
        _, stored = call('/lab/targets')
        assert json.loads(before) == json.loads(stored), 'Upload itself resolved a reference'
        status, body = call('/api/uploads/' + upload_id + '/preview', '')
        _, after = call('/lab/targets')
        a, b = json.loads(after), json.loads(before)
        print(json.dumps({'case': name, 'upload': 201, 'preview': status, 'body': body,
                          'httpDelta': a['httpHits'] - b['httpHits'],
                          'resolutionDelta': a['resolutions'] - b['resolutions']}))
        if args.summary:
            status, body = call('/api/uploads/' + upload_id + '/summary', '')
            _, counts = call('/lab/targets')
            print(json.dumps({'case': name, 'summary': status, 'body': body, 'counts': json.loads(counts)}))
