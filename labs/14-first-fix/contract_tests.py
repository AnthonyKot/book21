import copy
import json
from pathlib import Path
import unittest
from triage import validate

ROOT = Path(__file__).resolve().parent
AT = '2026-09-15T09:00:00Z'

class RecordContract(unittest.TestCase):
    def setUp(self):
        self.packet = json.loads((ROOT/'data/packet.json').read_text())
        self.decisions = json.loads((ROOT/'data/guided-decisions.json').read_text())
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

if __name__ == '__main__':
    unittest.main(verbosity=2)
