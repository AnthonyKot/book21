"""Check this exercise's decision-record contract, not optimal risk ranking."""
import argparse
from datetime import datetime, timedelta
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent

def instant(value):
    result = datetime.fromisoformat(value.replace('Z', '+00:00'))
    if result.tzinfo is None:
        raise ValueError('Use an explicit timezone')
    return result

def validate(packet, decisions, at):
    now = instant(at)
    items = {row['id']: row for row in packet['items']}
    errors = []
    seen = set()
    for decision in decisions:
        key = decision.get('id')
        if key not in items or key in seen:
            errors.append(f'{key}: unknown or duplicate item')
            continue
        seen.add(key)
        item = items[key]
        for field in ['owner', 'rationale', 'plan', 'reopenOn', 'reviewBy']:
            if not isinstance(decision.get(field), str) or not decision[field].strip():
                errors.append(f'{key}: missing {field}')
        if decision.get('action') not in {'patch-now', 'investigate', 'deferred'}:
            errors.append(f'{key}: unsupported action; no closure is modeled')
        if decision.get('artifact') != item['artifact'] or decision.get('evidenceRevision') != item['evidenceRevision']:
            errors.append(f'{key}: stale artifact or evidence association')
        if isinstance(decision.get('reviewBy'), str) and decision['reviewBy'].strip():
            try:
                due = instant(decision['reviewBy'])
                # This is an explicit teaching-case limit, not an industry SLA.
                if not now < due <= now + timedelta(hours=24):
                    errors.append(f'{key}: review must be future and within 24 hours')
            except ValueError:
                errors.append(f'{key}: invalid review time')
        if decision.get('action') == 'deferred' and item['knownPath'] != 'specific-exploit-blocked':
            errors.append(f'{key}: no documented basis for this narrow deferral')
    for missing in items.keys() - seen:
        errors.append(f'{missing}: missing decision')
    return errors

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--decisions', type=Path, default=ROOT/'data/guided-decisions.json')
    parser.add_argument('--at', default='2026-09-15T09:00:00Z')
    args = parser.parse_args()
    packet = json.loads((ROOT/'data/packet.json').read_text())
    decisions = json.loads(args.decisions.read_text())
    failures = validate(packet, decisions, args.at)
    for problem in failures:
        print('REJECT', problem)
    if not failures:
        for row in decisions:
            print(f"ACCEPT RECORD {row['id']}: {row['action']}; {row['owner']}; review by {row['reviewBy']}")
        print('3 records satisfy the fixture contract. Human reasoning still requires review.')
    raise SystemExit(1 if failures else 0)
