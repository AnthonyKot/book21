"""Post-attempt check for consumer/verify.py. Do not open this file before your attempt is saved:
the scenario list below is the answer to part of the worksheet.

    python3 consumer_check.py [--verifier consumer/verify.py]

It builds deliveries with the lab's runner and platform key, hands each to your verifier through
the command-line contract, and compares accept/reject with the expected outcome. It judges only
behaviour on these deliveries; your written policy, tests and limits are judged by the review guide.
"""
import argparse
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

import attest
import runner

ROOT = Path(__file__).resolve().parent

PREVIEW_WORKFLOW = """name: preview
on:
  pull_request:
    branches: [main]
permissions:
  contents: read
  id-token: write
  attestations: write
jobs:
  preview:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1
      - run: bash scripts/build.sh
      - uses: actions/attest-build-provenance@4d101475d8b20a2381f78447822ac1eab6504dd8
        with:
          subject-path: dist/app.tar.gz
"""

EXPERIMENT_WORKFLOW = """name: experiment
on:
  push:
permissions:
  contents: read
  id-token: write
  attestations: write
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1
      - run: bash scripts/build.sh
      - uses: actions/attest-build-provenance@4d101475d8b20a2381f78447822ac1eab6504dd8
        with:
          subject-path: dist/app.tar.gz
"""


def build(tmp, name, workflow_text, event, workflow_name='release-v2.yml'):
    """Run a workflow on an event and return the delivery directory (artifact, provenance, signature)."""
    wdir = tmp / name
    wdir.mkdir()
    workflow = wdir / workflow_name
    workflow.write_text(workflow_text)
    event_path = wdir / 'event.json'
    event_path.write_text(json.dumps(event))
    summary = runner.run(workflow, event_path, wdir / 'run', echo=False)
    delivery = wdir / 'delivery'
    delivery.mkdir()
    out = wdir / 'run'
    for file in ('app.tar.gz', 'provenance.json', 'provenance.sig'):
        if not (out / file).exists():
            raise RuntimeError(f'{name}: the build produced no {file}; jobs {summary["jobs"]}')
        shutil.copy(out / file, delivery / file)
    return delivery


def scenarios(tmp):
    release = (ROOT / 'workflows' / 'release-v2.yml').read_text()
    push_main = {'event_name': 'push', 'repository': 'doc-approval/service', 'ref': 'refs/heads/main',
                 'base_tree': 'repo/base', 'contribution': 'repo/contribution-feature'}
    cases = []

    good = build(tmp, 'release-from-main', release, push_main)
    cases.append(('release from main, unmodified', good, True))

    modified = tmp / 'modified'
    shutil.copytree(good, modified)
    with (modified / 'app.tar.gz').open('ab') as f:
        f.write(b'\n# appended after the build\n')
    cases.append(('artifact bytes changed after the build', modified, False))

    forged = tmp / 'forged'
    shutil.copytree(good, forged)
    attest.generate_keypair(tmp / 'other.key', tmp / 'other.pub')
    (forged / 'provenance.sig').write_bytes(attest.sign(tmp / 'other.key', (forged / 'provenance.json').read_bytes()))
    cases.append(('provenance re-signed with a key that is not the platform', forged, False))

    preview = build(tmp, 'preview-of-in-repo-pull-request', PREVIEW_WORKFLOW,
                    {'event_name': 'pull_request', 'repository': 'doc-approval/service',
                     'head_repository': 'doc-approval/service', 'from_fork': False, 'base_ref': 'main',
                     'pull_request': {'number': 57}, 'contribution': 'repo/contribution-build'},
                    workflow_name='preview.yml')
    cases.append(('platform-signed build of an unmerged pull request (preview workflow)', preview, False))

    edited = tmp / 'edited'
    shutil.copytree(preview, edited)
    statement = json.loads((edited / 'provenance.json').read_text())
    external = statement['predicate']['buildDefinition']['externalParameters']
    external['workflow'].update({'ref': 'refs/heads/main', 'path': 'release-v2.yml'})
    external['event'] = 'push'
    (edited / 'provenance.json').write_text(json.dumps(statement, indent=1, sort_keys=True))
    cases.append(('the preview provenance edited to claim a release from main (signature no longer matches)',
                  edited, False))

    branch = build(tmp, 'push-to-experiment-branch', EXPERIMENT_WORKFLOW,
                   {'event_name': 'push', 'repository': 'doc-approval/service', 'ref': 'refs/heads/experiment',
                    'base_tree': 'repo/base', 'contribution': 'repo/contribution-build'},
                   workflow_name='experiment.yml')
    cases.append(('platform-signed push to a non-release branch by another workflow', branch, False))

    fork = build(tmp, 'fork-main', release,
                 {'event_name': 'push', 'repository': 'mallory/service', 'ref': 'refs/heads/main',
                  'base_tree': 'repo/base', 'contribution': 'repo/contribution-build'})
    cases.append(("a fork's own main branch, built by the same platform with the same workflow file", fork, False))

    renamed = tmp / 'renamed'
    shutil.copytree(good, renamed)
    (renamed / 'app.tar.gz').rename(renamed / 'service-1.4.tar.gz')
    cases.append(('same bytes delivered under a different file name', renamed, True, 'service-1.4.tar.gz'))

    older = build(tmp, 'older-release', release, {**push_main, 'contribution': None})
    cases.append(('an earlier release from main (not the newest commit)', older, True))
    return cases


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--verifier', default=str(ROOT / 'consumer' / 'verify.py'))
    args = parser.parse_args()
    if not (runner.PLATFORM / 'builder.key').exists():
        attest.generate_keypair(runner.PLATFORM / 'builder.key', runner.PLATFORM / 'builder.pub')
    mismatches = 0
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        for case in scenarios(tmp):
            label, delivery, expected = case[:3]
            artifact = delivery / (case[3] if len(case) > 3 else 'app.tar.gz')
            result = subprocess.run([sys.executable, args.verifier, '--artifact', str(artifact),
                                     '--provenance', str(delivery / 'provenance.json'),
                                     '--signature', str(delivery / 'provenance.sig'),
                                     '--builder-key', str(runner.PLATFORM / 'builder.pub')],
                                    capture_output=True, text=True)
            accepted = result.returncode == 0
            first = (result.stdout.strip().splitlines() or [result.stderr.strip()[-200:] or '(no output)'])[0]
            verdict = 'ok' if accepted == expected else 'MISMATCH'
            mismatches += verdict == 'MISMATCH'
            print(f'[{verdict}] {label}\n      expected {"accept" if expected else "reject"}, '
                  f'got {"accept" if accepted else "reject"}: {first}')
    print(f'\n{mismatches} mismatch(es). The check cannot judge your policy text, your own tests or the limits you state.')
    return 1 if mismatches else 0


if __name__ == '__main__':
    sys.exit(main())
