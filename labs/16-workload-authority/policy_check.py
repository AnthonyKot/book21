"""Post-attempt check for policies/cleanup.json. Do not open this file before your attempt is
saved: the request list below is the answer to part of the worksheet.

    python3 policy_check.py [--policy policies/cleanup.json]

It issues identity tokens for several workloads, asks the broker for your role, and sends
requests through the authorizer, comparing each outcome with the intended one. It judges only
these outcomes; your written reasoning, your own tests and the widening decision are judged by
the review guide.
"""
import argparse
import sys
from datetime import timedelta
from pathlib import Path

import authority
from authority import Denied, ROOT

CLOCK = '2026-09-17T02:00:00Z'
REPO = 'doc-approval/service'
CLEANUP = {'repository': REPO, 'ref': 'refs/heads/main', 'event_name': 'schedule',
           'job_workflow_ref': f'{REPO}/.github/workflows/cleanup.yml@refs/heads/main', 'sha': '539e29e7f8e1'}
CI_MAIN = {**CLEANUP, 'event_name': 'push', 'job_workflow_ref': f'{REPO}/.github/workflows/ci.yml@refs/heads/main'}
PREVIEW = {'repository': REPO, 'ref': 'refs/pull/57/merge', 'event_name': 'pull_request',
           'job_workflow_ref': f'{REPO}/.github/workflows/cleanup.yml@refs/pull/57/merge', 'sha': '7bc04512f843'}
FORK = {**CLEANUP, 'repository': 'mallory/service',
        'job_workflow_ref': 'mallory/service/.github/workflows/cleanup.yml@refs/heads/main'}
FEATURE_BRANCH = {**CLEANUP, 'ref': 'refs/heads/experiment',
                  'job_workflow_ref': f'{REPO}/.github/workflows/cleanup.yml@refs/heads/experiment'}

TASK_REQUESTS = [
    ('artifacts:list', 'artifacts:staging/service/', 'list the staging artifacts'),
    ('artifacts:get', 'artifacts:staging/service/pr-38.tar.gz', 'read a staging artifact\'s metadata'),
    ('artifacts:delete', 'artifacts:staging/service/pr-38.tar.gz', 'delete a 41-day-old staging artifact'),
    ('artifacts:put', 'artifacts:reports/cleanup-2026-09-17.json', 'write the run report'),
]
OVERREACH = [
    ('artifacts:delete', 'artifacts:releases/service/1.4.tar.gz', 'delete the current release'),
    ('artifacts:put', 'artifacts:releases/service/1.5.tar.gz', 'publish a release'),
    ('secrets:get', 'secrets:staging/db-password', 'read the staging database password'),
    ('secrets:get', 'secrets:prod/db-password', 'read the production database password'),
    ('deploy:run', 'deploy:staging', 'deploy to staging'),
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--policy', default=str(ROOT / 'policies' / 'cleanup.json'))
    args = parser.parse_args()
    authority.ensure_issuer_key()
    base = authority.load_policies(ROOT / 'policies' / 'v2.json')
    mine = authority.load_policies(args.policy)
    roles = {**base['roles'], **mine['roles']}
    if 'staging-cleanup' not in mine['roles']:
        print('MISMATCH: policies/cleanup.json must define the role staging-cleanup')
        return 1
    broker = authority.Broker(base['broker'], CLOCK)
    mismatches = 0

    def report(ok, label, detail):
        nonlocal mismatches
        mismatches += not ok
        print(f'[{"ok" if ok else "MISMATCH"}] {label}\n      {detail}')

    try:
        # The job asks for the longest session the role permits: what a role allows is what a leak gets.
        longest = roles['staging-cleanup'].get('max_session_seconds')
        if not longest:
            report(False, 'the role states a session maximum', 'max_session_seconds is not set')
            print('\nWithout a session maximum the broker issues nothing; nothing else can be checked.')
            return 1
        cred = broker.assume(authority.issue_token(CLEANUP, 'lab-broker', CLOCK), 'staging-cleanup', roles, longest)
        report(True, 'the scheduled cleanup job on main assumes the role', f"session until {cred['expires']} (role maximum {longest}s)")
    except Denied as e:
        report(False, 'the scheduled cleanup job on main assumes the role', f'refused: {e}')
        print('\nThe cleanup job itself cannot obtain credentials; nothing else can be checked.')
        return 1

    for action, resource, why in TASK_REQUESTS:
        verdict, reason = authority.decide(cred, action, resource, CLOCK)
        report(verdict == 'allow', f'task: {why}', f'{verdict}: {reason}')
    for action, resource, why in OVERREACH:
        verdict, reason = authority.decide(cred, action, resource, CLOCK)
        report(verdict == 'deny', f'beyond the task: {why}', f'{verdict}: {reason}')

    later = authority.stamp(authority.instant(CLOCK) + timedelta(minutes=30))
    verdict, reason = authority.decide(cred, 'artifacts:delete', 'artifacts:staging/service/pr-38.tar.gz', later)
    report(verdict == 'deny', 'the session thirty minutes after the job started (organisation rule)', f'{verdict}: {reason}')

    for label, job in (('a manual (workflow_dispatch) run of cleanup.yml on main', {**CLEANUP, 'event_name': 'workflow_dispatch'}),
                       ('a pull request run of cleanup.yml', PREVIEW),
                       ('ci.yml on main', CI_MAIN),
                       ('cleanup.yml on a feature branch', FEATURE_BRANCH),
                       ("a fork's cleanup.yml on its main", FORK)):
        try:
            broker.assume(authority.issue_token(job, 'lab-broker', CLOCK), 'staging-cleanup', roles)
            report(False, f'{label} asks for the role', 'exchanged')
        except Denied as e:
            report(True, f'{label} asks for the role', f'refused: {e}')

    print(f'\n{mismatches} mismatch(es). The check cannot judge your reasoning, your own tests or the widening decision.')
    return 1 if mismatches else 0


if __name__ == '__main__':
    sys.exit(main())
