"""Post-attempt comparison for the essay 13 release review. Opening this file reveals the answers.

Save your record assessments and release answer first. This script recomputes the checkable facts
for each record in data/release-review and compares your verdicts with the authored ones.
"""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PACKET = ROOT / 'data/release-review'
ACCEPTED = {
    'R1': {'describes'},
    'R2': {'does-not-describe'},
    'R3': {'partial'},
    'R4': {'cannot-rely', 'does-not-describe'},
    'R5': {'does-not-describe', 'cannot-rely'},
}

parser = argparse.ArgumentParser()
parser.add_argument('--assessment', required=True)
args = parser.parse_args()
mine = json.loads(Path(args.assessment).read_text())
selection = json.loads((PACKET / 'selection.json').read_text())
records = json.loads((PACKET / 'records.json').read_text())

print('Recomputed facts:')
for record in records:
    data = (PACKET / record['reportFile']).read_bytes()
    report = json.loads(data)
    hash_ok = hashlib.sha256(data).hexdigest() == record['reportSha256']
    if 'source' in report:
        subject = report['source'].get('metadata', {}).get('imageID')
    else:
        component = report.get('metadata', {}).get('component', {})
        subject = f"{component.get('type')} {component.get('name')}:{component.get('version')}"
    print(f"  {record['id']}: recorded hash matches bytes={hash_ok}; report subject={subject}; "
          f"matches selection={subject == selection['selectedImageID'] or subject == 'container sha256:' + selection['selectedImageID'][7:]}; "
          f"claimed platform={record['platform']}")

failures = 0
print('Your verdicts:')
for key, allowed in ACCEPTED.items():
    verdict = mine.get('records', {}).get(key)
    ok = verdict in allowed
    failures += not ok
    print(f"  {'PASS' if ok else 'FAIL'} {key}: yours={verdict}; accepted={sorted(allowed)}")

answer = mine.get('containsHelper100')
supporting = set(mine.get('supportingRecords', []))
ok = answer in (False, 'false', 'no') and 'R1' in supporting and not supporting & {'R2', 'R3', 'R4', 'R5'}
failures += not ok
print(f"  {'PASS' if ok else 'FAIL'} release question: containsHelper100={answer}; supportingRecords={sorted(supporting)}")
print(f'Review check: {failures} failure(s). Your written reasons and next evidence request are judged by the review guide.')
raise SystemExit(1 if failures else 0)
