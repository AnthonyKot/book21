import copy
import json
from pathlib import Path
import unittest
from reopen_exercise import needs_review

class ReopenExercise(unittest.TestCase):
    def setUp(self):
        self.decision = json.loads((Path(__file__).resolve().parent/'data/guided-decisions.json').read_text())[0]
        self.event = {k: self.decision[k] for k in ['id', 'artifact', 'evidenceRevision']}
        self.at = '2026-09-15T12:00:00Z'
    def test_unchanged_before_deadline_stays_scheduled(self):
        self.assertFalse(needs_review(self.decision, self.event, self.at))
    def test_at_deadline_reopens(self):
        self.assertTrue(needs_review(self.decision, self.event, self.decision['reviewBy']))
    def test_after_deadline_reopens(self):
        self.assertTrue(needs_review(self.decision, self.event, '2026-09-17T00:00:00Z'))
    def test_other_item_does_not_reopen_this_record(self):
        self.event['id'] = 'R2'
        self.assertFalse(needs_review(self.decision, self.event, self.at))
    def test_new_war_artifact_reopens_before_deadline(self):
        self.event['artifact'] = 'fixture-sha256:new-war'
        self.assertTrue(needs_review(self.decision, self.event, self.at))
    def test_same_artifact_changed_ingress_evidence_reopens(self):
        self.event['evidenceRevision'] = 'R1-e2'
        self.assertTrue(needs_review(self.decision, self.event, self.at))
    def test_missing_artifact_evidence_requires_review(self):
        del self.event['artifact']
        self.assertTrue(needs_review(self.decision, self.event, self.at))
    def test_missing_revision_requires_review(self):
        del self.event['evidenceRevision']
        self.assertTrue(needs_review(self.decision, self.event, self.at))
    def test_no_mutation_of_old_decision(self):
        old = copy.deepcopy(self.decision)
        self.event['evidenceRevision'] = 'R1-e2'
        needs_review(self.decision, self.event, self.at)
        self.assertEqual(old, self.decision)

if __name__ == '__main__':
    unittest.main(verbosity=2)
