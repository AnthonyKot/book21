import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'src'))
import approval  # noqa: E402


class ApprovalTests(unittest.TestCase):
    def test_reviewer_can_approve_submitted(self):
        self.assertTrue(approval.can_approve('reviewer', 'submitted'))

    def test_author_cannot_approve(self):
        self.assertFalse(approval.can_approve('author', 'submitted'))

    def test_decision_moves_state(self):
        self.assertEqual(approval.next_state('submitted', 'approve'), 'approved')


if __name__ == '__main__':
    unittest.main()
