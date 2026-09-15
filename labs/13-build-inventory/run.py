"""Build and scan two local images. No image is pushed to a registry."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import time

ROOT = Path(__file__).resolve().parent
parser = argparse.ArgumentParser()
parser.add_argument('--maven', default='mvn')
parser.add_argument('--syft', default='syft')
args = parser.parse_args()
OUT = ROOT / 'out'
OUT.mkdir(exist_ok=True)
env = dict(os.environ, SYFT_CHECK_FOR_APP_UPDATE='false')

def run(command, log=None):
    print('+ ' + ' '.join(map(str, command)), flush=True)
    result = subprocess.run(list(map(str, command)), cwd=ROOT, env=env,
                            text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if log:
        (OUT / log).write_text(result.stdout + '\n' + result.stderr)
    if result.returncode:
        raise RuntimeError(f'{command[0]} exited {result.returncode}: {result.stderr[-2500:]} (see {log})')
    return result.stdout.strip()

version = run([args.syft, 'version'])
if 'Version:       1.51.1' not in version:
    raise SystemExit('Use pinned Syft 1.51.1 for the measured lab')
(OUT / 'syft-version.txt').write_text(version + '\n')
run([args.maven, '-B', '-f', 'app/pom.xml', 'clean', 'package'], 'app-build.log')
shutil.copy2(ROOT / 'app/target/bom.json', OUT / 'maven.cdx.json')
# Retain the resolved graph as another view; this is not an image inventory.
run([args.maven, '-B', '-f', 'app/pom.xml', 'org.apache.maven.plugins:maven-dependency-plugin:3.11.0:tree'], 'dependency-tree.log')
for label, helper_version in [('a', '1.0.0'), ('b', '1.1.0')]:
    stage = OUT / label
    stage.mkdir(exist_ok=True)
    run([args.maven, '-B', '-f', 'helper/pom.xml', f'-Drevision={helper_version}', 'clean', 'package'], f'helper-{label}.log')
    shutil.copy2(ROOT / 'app/target/inventory-app-1.0.0.jar', stage / 'app.jar')
    shutil.copy2(ROOT / f'helper/target/export-helper-{helper_version}.jar', stage / 'export-helper.jar')
    shutil.copy2(ROOT / 'Dockerfile', stage / 'Dockerfile')
    run(['docker', 'build', '--platform', 'linux/amd64', '-t', f'book21-inventory:{label}', str(stage)], f'image-{label}.log')
    image = json.loads(run(['docker', 'image', 'inspect', f'book21-inventory:{label}']))[0]
    (stage / 'image.json').write_text(json.dumps(image, indent=2) + '\n')
    image_id = image['Id']
    run([args.syft, 'scan', f'docker:{image_id}', '--config', str(ROOT / 'syft.yaml'), '--scope', 'squashed',
         '-o', f'syft-json={stage / "image.syft.json"}', '-o', f'cyclonedx-json@1.6={stage / "image.cdx.json"}'], f'scan-{label}.log')
    container = run(['docker', 'run', '--rm', '-d', '-p', '127.0.0.1::8080', image_id])
    try:
        port = run(['docker', 'port', container, '8080/tcp']).split(':')[-1]
        response = None
        for _ in range(100):
            attempt = subprocess.run(['curl', '-fsS', f'http://127.0.0.1:{port}/status'], text=True, capture_output=True)
            if attempt.returncode == 0:
                response = attempt.stdout
                break
            time.sleep(0.2)
        if response != 'document service ready':
            raise RuntimeError(f'Image {label} HTTP smoke failed: {response}')
        helper = run(['docker', 'exec', container, 'java', '-cp', '/opt/book21/tools/export-helper.jar', 'book21.helper.ExportHelper'])
        if helper != 'export helper ready':
            raise RuntimeError(f'Image {label} helper smoke failed: {helper}')
        (stage / 'smoke.txt').write_text(f'GET /status -> HTTP200 {response}\nSeparate helper invocation -> {helper}\n')
    finally:
        run(['docker', 'stop', container])
# Reuse only this lab's explicit candidate tag; keep both image IDs.
run(['docker', 'tag', 'book21-inventory:a', 'book21-inventory:candidate'])
before = json.loads(run(['docker', 'image', 'inspect', 'book21-inventory:candidate']))[0]['Id']
run(['docker', 'tag', 'book21-inventory:b', 'book21-inventory:candidate'])
after = json.loads(run(['docker', 'image', 'inspect', 'book21-inventory:candidate']))[0]['Id']
(OUT / 'tag-move.json').write_text(json.dumps({'tag': 'book21-inventory:candidate', 'before': before, 'after': after}, indent=2) + '\n')
print('Built, scanned and smoke-tested both images; local candidate tag now identifies B.', flush=True)
