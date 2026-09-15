import copy
import json
from pathlib import Path
import unittest
from triage import needs_review, review_queue, validate

ROOT = Path(__file__).resolve().parent
AT = '2026-09-15T09:00:00Z'


def load(path):
    return json.loads((ROOT/path).read_text())


class RecordContract(unittest.TestCase):
    """The decision-record contract at the 09:00 scenario clock."""

    def setUp(self):
        self.packet = load('data/packet.json')
        self.decisions = load('data/guided-decisions.json')

    def test_guided_records_accepted(self):
        self.assertEqual([], validate(self.packet, self.decisions, AT))

    def test_owner_required(self):
        self.decisions[0]['owner'] = ' '
        self.assertIn('R1: missing owner', validate(self.packet, self.decisions, AT))

    def test_expiry_required(self):
        del self.decisions[0]['reviewBy']
        self.assertIn('R1: missing reviewBy', validate(self.packet, self.decisions, AT))

    def test_exact_expiry_rejected(self):
        self.decisions[0]['reviewBy'] = AT
        self.assertTrue(validate(self.packet, self.decisions, AT))

    def test_long_deferral_rejected(self):
        self.decisions[0]['reviewBy'] = '2026-09-20T09:00:00Z'
        self.assertTrue(validate(self.packet, self.decisions, AT))

    def test_unknown_evidence_cannot_justify_deferral(self):
        self.decisions[2]['action'] = 'deferred'
        self.assertIn('R3: no documented basis for this narrow deferral', validate(self.packet, self.decisions, AT))

    def test_absent_prerequisites_can_support_deferral(self):
        self.packet['items'][2]['knownPath'] = 'prerequisites-absent'
        self.decisions[2]['action'] = 'deferred'
        self.assertEqual([], validate(self.packet, self.decisions, AT))

    def test_changed_evidence_rejected(self):
        self.packet['items'][0]['evidenceRevision'] = 'R1-e2'
        self.assertIn('R1: stale artifact or evidence association', validate(self.packet, self.decisions, AT))

    def test_changed_artifact_rejected(self):
        self.packet['items'][0]['artifact'] = 'fixture-sha256:changed'
        self.assertTrue(validate(self.packet, self.decisions, AT))

    def test_missing_item_rejected(self):
        self.decisions.pop()
        self.assertIn('R3: missing decision', validate(self.packet, self.decisions, AT))

    def test_duplicate_item_rejected(self):
        self.decisions.append(copy.deepcopy(self.decisions[0]))
        self.assertIn('R1: unknown or duplicate item', validate(self.packet, self.decisions, AT))

    def test_no_fake_closure(self):
        self.decisions[0]['action'] = 'fixed'
        self.assertTrue(validate(self.packet, self.decisions, AT))

    def test_plan_and_reopen_trigger_required(self):
        self.decisions[0]['plan'] = ''
        self.decisions[0]['reopenOn'] = ''
        self.assertEqual(2, len(validate(self.packet, self.decisions, AT)))


class ReopenScheduler(unittest.TestCase):
    """When an existing decision must go back to a human, before or at its review time."""

    def setUp(self):
        self.decision = load('data/guided-decisions.json')[0]
        self.item = copy.deepcopy(load('data/packet.json')['items'][0])
        self.noon = '2026-09-15T12:00:00Z'

    def test_unchanged_evidence_before_deadline_stays_scheduled(self):
        self.assertEqual([], needs_review(self.decision, self.item, self.noon))

    def test_review_time_reopens(self):
        self.assertIn('review time reached', needs_review(self.decision, self.item, self.decision['reviewBy']))

    def test_changed_artifact_reopens_before_deadline(self):
        self.item['artifact'] = 'fixture-sha256:other'
        self.assertIn('artifact changed', needs_review(self.decision, self.item, self.noon))

    def test_changed_evidence_revision_reopens_before_deadline(self):
        self.item['evidenceRevision'] = 'R1-e2'
        self.assertIn('evidenceRevision changed', needs_review(self.decision, self.item, self.noon))

    def test_missing_comparison_evidence_reopens(self):
        del self.item['evidenceRevision']
        self.assertIn('current evidenceRevision missing', needs_review(self.decision, self.item, self.noon))

    def test_review_does_not_modify_the_old_decision(self):
        old = copy.deepcopy(self.decision)
        self.item['evidenceRevision'] = 'R1-e2'
        needs_review(self.decision, self.item, self.noon)
        self.assertEqual(old, self.decision)

    def test_new_item_without_decision_is_queued(self):
        packet = load('data/packet.json')
        packet['items'].append({'id': 'R9', 'artifact': 'fixture-sha256:new', 'evidenceRevision': 'R9-e1'})
        self.assertEqual(['no decision recorded'], review_queue(packet, load('data/guided-decisions.json'), self.noon)['R9'])


class UpdatePacket(unittest.TestCase):
    """The 09:00 records against the 13:00 evidence: mechanical checks only."""

    def setUp(self):
        self.packet = load('data/update-1300/packet.json')
        self.decisions = load('data/guided-decisions.json')
        self.at = self.packet['clock']

    def test_deferral_without_classification_needs_a_stated_basis(self):
        item = self.packet['items'][0]
        record = {'id': item['id'], 'artifact': item['artifact'], 'evidenceRevision': item['evidenceRevision'],
                  'action': 'deferred', 'owner': 'o', 'rationale': 'r', 'plan': 'p', 'reopenOn': 'x',
                  'reviewBy': '2026-09-15T17:00:00Z'}
        self.assertIn(f"{item['id']}: no documented basis for this narrow deferral", validate(self.packet, [record], self.at))
        record['deferralBasis'] = 'specific-exploit-blocked'
        self.assertNotIn(f"{item['id']}: no documented basis for this narrow deferral", validate(self.packet, [record], self.at))

    def test_every_item_needs_a_decision_at_1300(self):
        self.assertEqual({'R1', 'R2', 'R3', 'R4'}, set(review_queue(self.packet, self.decisions, self.at)))

    def test_old_records_do_not_satisfy_the_contract_at_1300(self):
        errors = validate(self.packet, self.decisions, self.at)
        self.assertIn('R4: missing decision', errors)
        self.assertIn('R3: stale artifact or evidence association', errors)


if __name__ == '__main__':
    unittest.main(verbosity=2)
