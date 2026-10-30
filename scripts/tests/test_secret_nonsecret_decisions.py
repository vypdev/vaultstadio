import json
import hashlib
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "ci"))
from secret_findings import reviewed_count


class HistoricalNonSecretDecisions(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.manifest = Path(self.temporary.name) / "decisions.json"
        self.decision = {"tool": "gitleaks", "rule": "example", "commit": "a" * 40,
                         "path": "docs/example.txt", "line": 12, "reason": "Reviewed non-secret fixture"}
        self.manifest.write_text(json.dumps({"schemaVersion": 1, "tools": {"gitleaks": "8.30.1"},
                                            "decisions": [self.decision]}))
        self.finding = {"RuleID": "example", "Commit": "a" * 40, "File": "docs/example.txt", "StartLine": 12}

    def test_only_exact_immutable_coordinates_are_accepted(self):
        self.assertEqual(reviewed_count("gitleaks", "8.30.1", [self.finding], self.manifest), 1)
        for key, value in [("RuleID", "other"), ("Commit", "b" * 40), ("File", "src/production.txt"), ("StartLine", 13)]:
            with self.subTest(field=key):
                changed = {**self.finding, key: value}
                self.assertEqual(reviewed_count("gitleaks", "8.30.1", [changed], self.manifest), 0)

    def test_changed_tool_version_requires_new_review(self):
        with self.assertRaises(ValueError):
            reviewed_count("gitleaks", "8.30.2", [self.finding], self.manifest)

    def test_no_reason_never_accepts_a_finding(self):
        data = json.loads(self.manifest.read_text())
        data["decisions"][0]["reason"] = ""
        self.manifest.write_text(json.dumps(data))
        self.assertEqual(reviewed_count("gitleaks", "8.30.1", [self.finding], self.manifest), 0)

    def test_unknown_and_malformed_trufflehog_metadata_is_not_accepted(self):
        data = json.loads(self.manifest.read_text())
        data["tools"]["trufflehog"] = "3.99.7"
        self.manifest.write_text(json.dumps(data))
        for item in ({}, {"SourceMetadata": None}, {"SourceMetadata": {"Data": None}}):
            self.assertEqual(reviewed_count("trufflehog", "3.99.7", [item], self.manifest), 0)

    def test_sdd_integrity_checksum_is_public_source_with_exact_coordinates(self):
        repository = Path(__file__).resolve().parents[2]
        commit = "e81da9c26ba72274eee16b62b56b8af3ab7201b5"
        path = "docs/development/verification/sdd-consumer-adoption-145-preservation.json"
        encoded = subprocess.check_output(["git", "show", commit + ":" + path], cwd=repository)
        preservation = json.loads(encoded)
        original_path = "specs/SDD-0005-upload-and-transfer-protocols.md"
        original = subprocess.check_output(["git", "show", preservation["source"] + ":" + original_path], cwd=repository)
        checksum = hashlib.sha256(original).hexdigest()
        self.assertEqual(checksum, preservation["files"][original_path]["original_sha256"])
        self.assertEqual(len(original), preservation["files"][original_path]["original_bytes"])
        self.assertIn(checksum, encoded.decode().splitlines()[468])
        coordinates = {"commit": commit, "file": path, "line": 469}
        finding = {"DetectorName": "ProtocolsIO", "SourceMetadata": {"Data": {"Git": coordinates}}}
        self.assertEqual(1, reviewed_count("trufflehog", "3.99.7", [finding]))
        for field, replacement in [("commit", "0" * 40), ("file", "docs/another.json"), ("line", 470)]:
            changed = {**finding, "SourceMetadata": {"Data": {"Git": {**coordinates, field: replacement}}}}
            self.assertEqual(0, reviewed_count("trufflehog", "3.99.7", [changed]))


if __name__ == "__main__":
    unittest.main()
