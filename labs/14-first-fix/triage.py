"""Check this exercise's decision-record contract and report which records need review.

The checks cover record hygiene: ownership, expiry, current evidence association and the
documented basis for a narrow deferral. They do not judge whether a decision is wise.
"""
import argparse
from datetime import datetime, timedelta
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
ACTIONS = {'patch-now', 'investigate', 'deferred'}
# Evidence classifications that can support a narrow, time-limited deferral in this exercise.
DEFERRAL_BASIS = {'specific-exploit-blocked', 'prerequisites-absent'}


def instant(value):
    result = datetime.fromisoformat(value.replace('Z', '+00:00'))
    if result.tzinfo is None:
        raise ValueError('Use an explicit timezone')
    return result


def validate(packet, decisions, at):
    """Return contract violations for a decision set at the given time."""
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
        if decision.get('action') not in ACTIONS:
            errors.append(f'{key}: unsupported action; no closure is modeled')
        if decision.get('artifact') != item['artifact'] or decision.get('evidenceRevision') != item['evidenceRevision']:
            errors.append(f'{key}: stale artifact or evidence association')
        if isinstance(decision.get('reviewBy'), str) and decision['reviewBy'].strip():
            try:
                due = instant(decision['reviewBy'])
                # An explicit teaching-case limit, not an industry SLA.
                if not now < due <= now + timedelta(hours=24):
                    errors.append(f'{key}: review must be future and within 24 hours')
            except ValueError:
                errors.append(f'{key}: invalid review time')
        if decision.get('action') == 'deferred':
            # A packet classification, where supplied, is the basis; otherwise the record must
            # state one. The validator cannot tell whether a stated basis is true.
            basis = item.get('knownPath', decision.get('deferralBasis'))
            if basis not in DEFERRAL_BASIS:
                errors.append(f'{key}: no documented basis for this narrow deferral')
    for missing in sorted(items.keys() - seen):
        errors.append(f'{missing}: missing decision')
    return errors


def needs_review(decision, item, at):
    """Return the reasons an existing decision must be reviewed; an empty list means none.

    A decision is reopened when its review time arrives, or when the current evidence no longer
    describes the artifact and evidence revision the decision was made on. Missing comparison
    evidence also reopens it. Reopening asks for a new human decision; it changes nothing itself.
    """
    reasons = []
    if instant(at) >= instant(decision['reviewBy']):
        reasons.append('review time reached')
    for field in ['artifact', 'evidenceRevision']:
        if field not in item:
            reasons.append(f'current {field} missing')
        elif item[field] != decision.get(field):
            reasons.append(f'{field} changed')
    return reasons


def review_queue(packet, decisions, at):
    """Map each packet item to the reasons it needs a decision now."""
    by_id = {row['id']: row for row in decisions}
    queue = {}
    for item in packet['items']:
        decision = by_id.get(item['id'])
        reasons = ['no decision recorded'] if decision is None else needs_review(decision, item, at)
        if reasons:
            queue[item['id']] = reasons
    return queue


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--packet', type=Path, default=ROOT/'data/packet.json')
    parser.add_argument('--decisions', type=Path, default=ROOT/'data/guided-decisions.json')
    parser.add_argument('--at', default=None, help='explicit clock; defaults to the packet clock')
    parser.add_argument('--reopen', action='store_true', help='list records that need review instead of validating')
    args = parser.parse_args()
    packet = json.loads(args.packet.read_text())
    decisions = json.loads(args.decisions.read_text())
    at = args.at or packet['clock']

    if args.reopen:
        queue = review_queue(packet, decisions, at)
        for key, reasons in queue.items():
            print(f'REVIEW {key}: ' + '; '.join(reasons))
        if not queue:
            print('No record needs review at', at)
        return 0

    failures = validate(packet, decisions, at)
    for problem in failures:
        print('REJECT', problem)
    if not failures:
        for row in decisions:
            print(f"ACCEPT RECORD {row['id']}: {row['action']}; {row['owner']}; review by {row['reviewBy']}")
        print(f'{len(decisions)} records satisfy the fixture contract. Human reasoning still requires review.')
    return 1 if failures else 0


if __name__ == '__main__':
    raise SystemExit(main())
