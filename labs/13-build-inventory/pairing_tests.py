"""Guided checks of pairing.py over the two real scans; run after record.py."""
import copy
import hashlib
import json
import os
from pathlib import Path
import unittest
from pairing import accepts

ROOT = Path(os.environ.get('BOOK21_INVENTORY_OUT', Path(__file__).resolve().parent/'out'))
TAG = 'book21-inventory:candidate'

class PairingVerifier(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.records = {x: json.loads((ROOT/x/'record.json').read_text()) for x in ['a','b']}
        cls.raw = {x: (ROOT/x/'image.syft.json').read_bytes() for x in ['a','b']}
        cls.cdx = {x: (ROOT/x/'image.cdx.json').read_bytes() for x in ['a','b']}

    def test_matching_a_allowed(self):
        self.assertTrue(accepts(self.records['a'], TAG, self.records['a']['imageID'], self.raw['a'], self.cdx['a']))

    def test_matching_b_allowed(self):
        self.assertTrue(accepts(self.records['b'], TAG, self.records['b']['imageID'], self.raw['b'], self.cdx['b']))

    def test_other_tag_rejected(self):
        self.assertFalse(accepts(self.records['a'], 'another:tag', self.records['a']['imageID'], self.raw['a'], self.cdx['a']))

    def test_old_record_after_tag_move_rejected(self):
        self.assertFalse(accepts(self.records['a'], TAG, self.records['b']['imageID'], self.raw['a'], self.cdx['a']))

    def test_swapped_raw_report_rejected(self):
        self.assertFalse(accepts(self.records['a'], TAG, self.records['a']['imageID'], self.raw['b'], self.cdx['a']))

    def test_swapped_cyclonedx_report_rejected(self):
        self.assertFalse(accepts(self.records['a'], TAG, self.records['a']['imageID'], self.raw['a'], self.cdx['b']))

    def test_relabelled_record_still_has_wrong_scanner_subject(self):
        record = copy.deepcopy(self.records['a'])
        record['imageID'] = self.records['b']['imageID']
        self.assertFalse(accepts(record, TAG, self.records['b']['imageID'], self.raw['a'], self.cdx['a']))

    def test_missing_record_field_rejected(self):
        record = copy.deepcopy(self.records['a'])
        del record['inventorySha256']
        self.assertFalse(accepts(record, TAG, record['imageID'], self.raw['a'], self.cdx['a']))

    def test_invalid_json_with_matching_hash_rejected(self):
        record = copy.deepcopy(self.records['a'])
        damaged = b'not json'
        record['inventorySha256'] = hashlib.sha256(damaged).hexdigest()
        self.assertFalse(accepts(record, TAG, record['imageID'], damaged, self.cdx['a']))

if __name__ == '__main__':
    unittest.main(verbosity=2)
