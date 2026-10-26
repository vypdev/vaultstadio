import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "ci"))
from secret_history import changed_blobs


class HistoryInventory(unittest.TestCase):
    def test_paths_that_resemble_metadata_cannot_change_the_inventory(self):
        first, fake = b"a" * 40, b"b" * 40
        metadata = b":000000 100644 " + b"0" * 40 + b" " + first + b" A"
        path = b":000000 100644 " + b"0" * 40 + b" " + fake + b" A"
        self.assertEqual(changed_blobs(metadata + b"\0" + path + b"\0"), {first})

    def test_mode_only_changes_and_deletions_do_not_claim_new_content(self):
        key = b"a" * 40
        mode = b":100644 100755 " + key + b" " + key + b" M\0mode-file\0"
        deleted = b":100644 000000 " + key + b" " + b"0" * 40 + b" D\0deleted-file\0"
        self.assertEqual(changed_blobs(mode + deleted), set())

    def test_malformed_or_truncated_inventory_fails_closed(self):
        for raw in (b"unexpected", b":invalid\0file\0", b":000000 100644 " + b"0" * 40 + b" " + b"a" * 40 + b" A"):
            with self.subTest(raw_length=len(raw)), self.assertRaises(ValueError):
                changed_blobs(raw)


if __name__ == "__main__":
    unittest.main()
