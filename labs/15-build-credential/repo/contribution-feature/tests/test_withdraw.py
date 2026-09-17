import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'src'))
import approval  # noqa: E402


class WithdrawTests(unittest.TestCase):
    def test_author_can_withdraw_submitted(self):
        self.assertTrue(approval.can_withdraw('author', 'submitted'))

    def test_reviewer_cannot_withdraw(self):
        self.assertFalse(approval.can_withdraw('reviewer', 'submitted'))


if __name__ == '__main__':
    unittest.main()
