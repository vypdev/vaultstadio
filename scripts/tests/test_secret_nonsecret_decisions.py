import json
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


if __name__ == "__main__":
    unittest.main()
