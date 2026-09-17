"""Guided trace: what one leaked credential can do under each design, and what the repaired
design refuses. Prints a transcript and writes runs/trace.txt.

    python3 trace.py            # both versions
    python3 trace.py --v1       # the static key only
    python3 trace.py --v2       # the role-based design only
"""
import argparse
import json
import shutil
import sys
from datetime import timedelta
from pathlib import Path

import authority
import signing
from authority import Denied, PLATFORM, ROOT

CLOCK = '2026-09-17T09:00:00Z'
PUBLISH_JOB = {'repository': 'doc-approval/service', 'ref': 'refs/heads/main', 'event_name': 'push',
               'job_workflow_ref': 'doc-approval/service/.github/workflows/release-v2.yml@refs/heads/main',
               'sha': '539e29e7f8e1'}
PREVIEW_JOB = {'repository': 'doc-approval/service', 'ref': 'refs/pull/57/merge', 'event_name': 'pull_request',
               'job_workflow_ref': 'doc-approval/service/.github/workflows/preview.yml@refs/pull/57/merge',
               'sha': '7bc04512f843'}
CI_JOB_ON_MAIN = {'repository': 'doc-approval/service', 'ref': 'refs/heads/main', 'event_name': 'push',
                  'job_workflow_ref': 'doc-approval/service/.github/workflows/ci.yml@refs/heads/main',
                  'sha': '539e29e7f8e1'}
FORK_MAIN = {'repository': 'mallory/service', 'ref': 'refs/heads/main', 'event_name': 'push',
             'job_workflow_ref': 'mallory/service/.github/workflows/release-v2.yml@refs/heads/main',
             'sha': '7bc04512f843'}

REQUESTS = [
    ('artifacts:put', 'artifacts:releases/service/1.5.tar.gz', 'publish the new release (the task)'),
    ('secrets:get', 'secrets:prod/db-password', 'read the production database password'),
    ('artifacts:delete', 'artifacts:releases/service/1.4.tar.gz', 'delete the current release'),
    ('deploy:run', 'deploy:production', 'deploy to production'),
]


class Out:
    def __init__(self):
        self.lines = []

    def say(self, line=''):
        print(line)
        self.lines.append(line)


def at(base, **delta):
    return authority.instant(base) + timedelta(**delta)


def show_requests(out, credential, when, policies=None):
    for action, resource, why in REQUESTS:
        verdict, reason = authority.decide(credential, action, resource, when)
        out.say(f'     {verdict:5} {action} {resource}  ({why})')
        out.say(f'           {reason}')


def trace_v1(out):
    policies = authority.load_policies(ROOT / 'policies' / 'v1.json')
    out.say('== Version 1: one static key, RELEASE_TOKEN, shared by every job')
    cred = authority.static_credential('deployer', policies['static_keys'])
    out.say(f"   key created {cred['issued']}, expires {cred['expires']}")
    out.say(f'   The key leaked from a build job (essay 15). Whoever holds it now, at {CLOCK}:')
    show_requests(out, cred, CLOCK)
    later = authority.stamp(at(CLOCK, days=365))
    out.say(f'   A year later, at {later}, the same key:')
    verdict, reason = authority.decide(cred, 'secrets:get', 'secrets:prod/db-password', later)
    out.say(f'     {verdict:5} secrets:get secrets:prod/db-password  ({reason})')
    out.say('   Nothing in the key says which job it was for, when it was issued, or when it stops working.')
    return cred


def trace_v2(out):
    policies = authority.load_policies(ROOT / 'policies' / 'v2.json')
    roles = policies['roles']
    broker = authority.Broker(policies['broker'], CLOCK)
    out.say('\n== Version 2: no static key. A job presents its identity token and receives a role session')

    out.say(f'\n   The publish job of release-v2.yml on main asks the issuer for a token at {CLOCK}:')
    token = authority.issue_token(PUBLISH_JOB, 'lab-broker', CLOCK)
    out.say('     ' + json.dumps({k: token['claims'][k] for k in ('sub', 'repository', 'ref', 'event_name', 'job_workflow_ref', 'exp')}))
    cred = broker.assume(token, 'release-publisher', roles)
    out.say(f"   Broker: release-publisher session issued, expires {cred['expires']} (role maximum {roles['release-publisher']['max_session_seconds']}s)")
    show_requests(out, cred, CLOCK)

    out.say('\n   Same credential, sixteen minutes later:')
    later = authority.stamp(at(CLOCK, minutes=16))
    verdict, reason = authority.decide(cred, 'artifacts:put', 'artifacts:releases/service/1.5.tar.gz', later)
    out.say(f'     {verdict:5} artifacts:put artifacts:releases/service/1.5.tar.gz  ({reason})')

    out.say('\n   The identity token itself, leaked from the job and presented six minutes after issue:')
    late_broker = authority.Broker(policies['broker'], at(CLOCK, minutes=6))
    try:
        late_broker.assume(token, 'release-publisher', roles)
        out.say('     exchanged (unexpected)')
    except Denied as e:
        out.say(f'     refused: {e}')

    out.say('\n   Other jobs of the same platform ask for the same role:')
    for label, job in (('preview job of an unmerged pull request', PREVIEW_JOB),
                       ('ci.yml on main (right branch, wrong workflow)', CI_JOB_ON_MAIN),
                       ("a fork's own release-v2.yml on its main", FORK_MAIN)):
        t = authority.issue_token(job, 'lab-broker', CLOCK)
        try:
            broker.assume(t, 'release-publisher', roles)
            out.say(f'     {label}: exchanged (unexpected)')
        except Denied as e:
            out.say(f'     {label}: refused: {e}')

    out.say('\n   A token minted for a different audience (another broker):')
    t = authority.issue_token(PUBLISH_JOB, 'some-other-service', CLOCK)
    try:
        broker.assume(t, 'release-publisher', roles)
    except Denied as e:
        out.say(f'     refused: {e}')

    out.say('\n   The publisher role tries the deployer role:')
    try:
        broker.assume(token, 'production-deployer', roles)
    except Denied as e:
        out.say(f'     refused: {e}')

    out.say('\n   Issuer key rotation: the platform retires its signing key and publishes a new one')
    old_key, old_pub = PLATFORM / 'issuer.key', PLATFORM / 'issuer.pub'
    retired_key, retired_pub = PLATFORM / 'issuer-retired.key', PLATFORM / 'issuer-retired.pub'
    shutil.copy(old_key, retired_key)
    shutil.copy(old_pub, retired_pub)
    signing.generate_keypair(old_key, old_pub)
    fresh_broker = authority.Broker(policies['broker'], CLOCK)
    try:
        fresh_broker.assume(token, 'release-publisher', roles)
        out.say('     token signed with the retired key: exchanged (unexpected)')
    except Denied as e:
        out.say(f'     token signed with the retired key: refused: {e}')
    new_token = authority.issue_token(PUBLISH_JOB, 'lab-broker', CLOCK)
    cred2 = fresh_broker.assume(new_token, 'release-publisher', roles)
    out.say(f"     token signed with the new key: release-publisher session issued, expires {cred2['expires']}")
    retired_key.unlink()
    retired_pub.unlink()
    out.say('   No job configuration changed for the rotation; only the issuer key and the broker\'s trusted key did.')
    return cred


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--v1', action='store_true')
    parser.add_argument('--v2', action='store_true')
    args = parser.parse_args()
    if authority.ensure_issuer_key():
        print('issuer key created in platform/')
    out = Out()
    if args.v1 or not args.v2:
        trace_v1(out)
    if args.v2 or not args.v1:
        trace_v2(out)
    runs = ROOT / 'runs'
    runs.mkdir(exist_ok=True)
    (runs / 'trace.txt').write_text('\n'.join(out.lines) + '\n')
    print('\nrecorded in runs/trace.txt')


if __name__ == '__main__':
    sys.exit(main())
