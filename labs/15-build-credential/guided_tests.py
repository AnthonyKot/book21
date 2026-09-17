"""Guided checks: which context each workflow gives a contribution, and what leaves the job.

    python3 guided_tests.py                        # release-v2 must keep the credential inside
    python3 guided_tests.py --release workflows/release-v1.yml   # negative control: v1 must fail

The vulnerable-workflow tests always run against release-v1.yml; the repair tests run against
the workflow named by --release (default release-v2.yml). Runs are written under runs/tests/.
"""
import argparse
import sys
import unittest
from pathlib import Path

import runner

ROOT = Path(__file__).resolve().parent
RELEASE = [ROOT / 'workflows' / 'release-v2.yml']
OUT = ROOT / 'runs' / 'tests'


def run(workflow, event):
    return runner.run(ROOT / 'workflows' / workflow if not Path(workflow).is_absolute() else workflow,
                      ROOT / 'events' / event, OUT / f'{Path(workflow).stem}-{Path(event).stem}', echo=False)


def requests_of(summary):
    return [req for job in summary['jobs'] for step in job['steps'] for req in step.get('requests', [])]


def secret_left(summary):
    return any(req['carries_secret'] for req in requests_of(summary))


def pushes(summary):
    return [req['result'] for req in requests_of(summary) if req['method'] == 'PUT']


class VulnerableRelease(unittest.TestCase):
    """What release-v1.yml does with a fork's pull request that changes the build script."""

    @classmethod
    def setUpClass(cls):
        cls.build = run('release-v1.yml', 'fork-prt-build.json')

    def test_pull_request_target_runs_the_contribution_with_repository_secrets(self):
        job = self.build['jobs'][0]
        checkout = job['steps'][0]
        self.assertIn('contribution as pushed', checkout['checkout'])
        self.assertTrue(secret_left(self.build), 'RELEASE_TOKEN reached an outbound request')

    def test_write_token_lets_the_script_change_the_repository(self):
        self.assertTrue(any(r.startswith('200') for r in pushes(self.build)))

    def test_the_contribution_could_publish_a_release(self):
        uploads = [req for req in requests_of(self.build) if req['path'] == '/upload']
        self.assertEqual([u['result'] for u in uploads], ['accepted'])


class RepairedRelease(unittest.TestCase):
    """The repaired division: pull requests build without secrets; releases come from main."""

    @classmethod
    def setUpClass(cls):
        cls.ci_build = run('ci.yml', 'fork-pr-build.json')
        cls.ci_feature = run('ci.yml', 'fork-pr-feature.json')
        cls.release_prt = run(RELEASE[0], 'fork-prt-build.json')
        cls.release_pr = run(RELEASE[0], 'fork-pr-build.json')
        cls.release_push = run(RELEASE[0], 'push-main.json')

    def test_release_workflow_ignores_pull_requests(self):
        self.assertEqual(self.release_prt['jobs'], [])
        self.assertEqual(self.release_pr['jobs'], [])

    def test_fork_pull_request_still_builds_and_tests(self):
        job = self.ci_feature['jobs'][0]
        self.assertEqual(job['steps'][-1]['exit'], 0)
        self.assertIn('merged onto base', job['steps'][0]['checkout'])

    def test_fork_pull_request_gets_no_repository_secret(self):
        self.assertFalse(secret_left(self.ci_build))
        telemetry = [r for r in requests_of(self.ci_build) if r['path'] == '/telemetry']
        self.assertEqual(len(telemetry), 1)
        self.assertIn('release_token=&', telemetry[0]['body'] + '&')

    def test_fork_pull_request_token_cannot_write(self):
        self.assertTrue(all(r.startswith('403') for r in pushes(self.ci_build)))
        self.assertTrue(pushes(self.ci_build))

    def test_release_from_main_builds_the_pushed_commit_without_persisting_a_token(self):
        build = self.release_push['jobs'][0]
        self.assertIn('pushed commit', build['steps'][0]['checkout'])
        self.assertFalse(build['steps'][0]['persist'])

    def test_release_token_appears_only_in_the_publish_upload(self):
        carrying = [r for r in requests_of(self.release_push) if r['carries_secret']]
        self.assertEqual([(r['path'], r['result']) for r in carrying], [('/upload', 'accepted')])

    def test_release_records_signed_provenance_for_the_uploaded_bytes(self):
        publish = self.release_push['jobs'][-1]
        self.assertIsNotNone(publish['provenance'])
        out = Path(publish['provenance']).parent
        import json
        import attest
        statement = json.loads((out / 'provenance.json').read_bytes())
        self.assertEqual(statement['subject'][0]['digest']['sha256'], attest.sha256_file(out / 'app.tar.gz'))
        self.assertTrue(attest.verify_signature(runner.PLATFORM / 'builder.pub', (out / 'provenance.json').read_bytes(),
                                                (out / 'provenance.sig').read_bytes()))
        self.assertEqual(statement['predicate']['buildDefinition']['externalParameters']['event'], 'push')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--release', default=str(RELEASE[0]))
    args, rest = parser.parse_known_args()
    RELEASE[0] = Path(args.release).resolve()
    if not (runner.PLATFORM / 'builder.key').exists():
        import attest
        attest.generate_keypair(runner.PLATFORM / 'builder.key', runner.PLATFORM / 'builder.pub')
    unittest.main(argv=[sys.argv[0]] + rest, verbosity=2)
