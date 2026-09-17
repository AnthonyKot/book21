"""Guided checks over the model: what the static key permits, what the role design refuses.

    python3 guided_tests.py                  # version 2 must pass every repair test
    python3 guided_tests.py --broad          # negative control: a role design that looks repaired

The negative control keeps version 2's shape but trusts the repository alone and grants
artifacts:* on every artifact. Its failures show which checks the narrow design actually earns.
"""
import argparse
import copy
import sys
import unittest
from datetime import timedelta

import authority
from authority import Denied, ROOT
import trace

POLICIES = [None]


def broad(policies):
    p = copy.deepcopy(policies)
    role = p['roles']['release-publisher']
    role['trust'] = {'aud': 'lab-broker', 'repository': 'doc-approval/service'}
    role['permissions'] = [{'effect': 'allow', 'actions': ['artifacts:*'], 'resources': ['artifacts:*']}]
    role['max_session_seconds'] = 3600
    return p


class StaticKey(unittest.TestCase):
    def setUp(self):
        v1 = authority.load_policies(ROOT / 'policies' / 'v1.json')
        self.cred = authority.static_credential('deployer', v1['static_keys'])

    def test_leaked_key_can_read_production_secret(self):
        self.assertEqual(authority.decide(self.cred, 'secrets:get', 'secrets:prod/db-password', trace.CLOCK)[0], 'allow')

    def test_leaked_key_can_delete_the_current_release(self):
        self.assertEqual(authority.decide(self.cred, 'artifacts:delete', 'artifacts:releases/service/1.4.tar.gz', trace.CLOCK)[0], 'allow')

    def test_leaked_key_never_expires(self):
        later = authority.stamp(trace.at(trace.CLOCK, days=365))
        self.assertEqual(authority.decide(self.cred, 'deploy:run', 'deploy:production', later)[0], 'allow')


class RoleSessions(unittest.TestCase):
    def setUp(self):
        self.policies = POLICIES[0]
        self.roles = self.policies['roles']
        self.broker = authority.Broker(self.policies['broker'], trace.CLOCK)
        self.token = authority.issue_token(trace.PUBLISH_JOB, 'lab-broker', trace.CLOCK)
        self.cred = self.broker.assume(self.token, 'release-publisher', self.roles)

    def test_publish_job_can_publish_a_release(self):
        self.assertEqual(authority.decide(self.cred, 'artifacts:put', 'artifacts:releases/service/1.5.tar.gz', trace.CLOCK)[0], 'allow')

    def test_publish_job_cannot_read_a_secret(self):
        self.assertEqual(authority.decide(self.cred, 'secrets:get', 'secrets:prod/db-password', trace.CLOCK)[0], 'deny')

    def test_publish_job_cannot_delete_a_release(self):
        self.assertEqual(authority.decide(self.cred, 'artifacts:delete', 'artifacts:releases/service/1.4.tar.gz', trace.CLOCK)[0], 'deny')

    def test_publish_job_cannot_deploy(self):
        self.assertEqual(authority.decide(self.cred, 'deploy:run', 'deploy:production', trace.CLOCK)[0], 'deny')

    def test_session_is_unusable_within_the_hour(self):
        later = authority.stamp(trace.at(trace.CLOCK, minutes=16))
        self.assertEqual(authority.decide(self.cred, 'artifacts:put', 'artifacts:releases/service/1.5.tar.gz', later)[0], 'deny')

    def test_leaked_identity_token_expires_in_minutes(self):
        late = authority.Broker(self.policies['broker'], trace.at(trace.CLOCK, minutes=6))
        with self.assertRaises(Denied):
            late.assume(self.token, 'release-publisher', self.roles)

    def test_pull_request_token_cannot_assume_the_publisher_role(self):
        t = authority.issue_token(trace.PREVIEW_JOB, 'lab-broker', trace.CLOCK)
        with self.assertRaises(Denied):
            self.broker.assume(t, 'release-publisher', self.roles)

    def test_other_workflow_on_main_cannot_assume_the_publisher_role(self):
        t = authority.issue_token(trace.CI_JOB_ON_MAIN, 'lab-broker', trace.CLOCK)
        with self.assertRaises(Denied):
            self.broker.assume(t, 'release-publisher', self.roles)

    def test_fork_cannot_assume_the_publisher_role(self):
        t = authority.issue_token(trace.FORK_MAIN, 'lab-broker', trace.CLOCK)
        with self.assertRaises(Denied):
            self.broker.assume(t, 'release-publisher', self.roles)

    def test_token_for_another_audience_is_refused(self):
        t = authority.issue_token(trace.PUBLISH_JOB, 'some-other-service', trace.CLOCK)
        with self.assertRaises(Denied):
            self.broker.assume(t, 'release-publisher', self.roles)

    def test_publisher_cannot_take_the_deployer_role(self):
        with self.assertRaises(Denied):
            self.broker.assume(self.token, 'production-deployer', self.roles)

    def test_explicit_deny_beats_a_wildcard_allow(self):
        deployer = copy.deepcopy(self.roles['production-deployer'])
        deployer['permissions'].insert(0, {'effect': 'allow', 'actions': ['*'], 'resources': ['*']})
        cred = {'permissions': deployer['permissions'], 'expires': None}
        self.assertEqual(authority.decide(cred, 'secrets:get', 'secrets:prod/db-password', trace.CLOCK)[0], 'deny')

    def test_retired_issuer_key_is_refused_after_rotation(self):
        import shutil
        import signing
        key, pub = authority.PLATFORM / 'issuer.key', authority.PLATFORM / 'issuer.pub'
        backup_key, backup_pub = key.read_bytes(), pub.read_bytes()
        try:
            signing.generate_keypair(key, pub)
            fresh = authority.Broker(self.policies['broker'], trace.CLOCK)
            with self.assertRaises(Denied):
                fresh.assume(self.token, 'release-publisher', self.roles)
            new_token = authority.issue_token(trace.PUBLISH_JOB, 'lab-broker', trace.CLOCK)
            self.assertEqual(fresh.assume(new_token, 'release-publisher', self.roles)['role'], 'release-publisher')
        finally:
            key.write_bytes(backup_key)
            pub.write_bytes(backup_pub)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--broad', action='store_true', help='negative control: repository-only trust, artifacts:* permissions')
    args, rest = parser.parse_known_args()
    authority.ensure_issuer_key()
    POLICIES[0] = authority.load_policies(ROOT / 'policies' / 'v2.json')
    if args.broad:
        POLICIES[0] = broad(POLICIES[0])
    unittest.main(argv=[sys.argv[0]] + rest, verbosity=2)
