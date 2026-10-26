import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "ci"))
import run_secret_scan as scanner
from secret_process import environment, execute


class SecretScanAdapter(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="scan spaces ")
        self.root = Path(self.temporary.name)
        self.addCleanup(self.temporary.cleanup)

    def executable(self, body):
        target = self.root / "fixture"
        target.write_text(f"#!{sys.executable}\n" + body)
        target.chmod(0o700)
        return target

    def test_process_gets_private_home_and_no_inherited_credentials(self):
        with patch.dict(os.environ, {"GITHUB_TOKEN": "never-forward-this", "GITLEAKS_CONFIG": "unsafe"}):
            code, output, _ = execute([sys.executable, "-c", "import os,json;print(json.dumps(dict(os.environ)))"], self.root)
        actual = json.loads(output)
        self.assertEqual(code, 0)
        self.assertEqual(actual["HOME"], str(self.root))
        self.assertNotIn("GITHUB_TOKEN", actual)
        self.assertNotIn("GITLEAKS_CONFIG", actual)
        self.assertEqual(actual["GIT_CONFIG_GLOBAL"], os.devnull)

    def test_missing_executable_and_timeout_are_nonzero(self):
        self.assertEqual(execute([str(self.root / "missing")], self.root)[0], 127)
        self.assertEqual(execute([sys.executable, "-c", "import time;time.sleep(5)"], self.root, timeout=0.05)[0], 124)

    def test_raw_failure_output_is_captured_without_printing(self):
        fixture = self.executable("import sys; print('synthetic-private'); print('provider-response',file=sys.stderr);sys.exit(42)\n")
        code, output, errors = execute([str(fixture)], self.root)
        self.assertEqual(code, 42)
        self.assertIn(b"synthetic-private", output)
        self.assertIn(b"provider-response", errors)
        self.assertEqual(list(self.root.iterdir()), [fixture])

    def test_source_mismatch_and_shallow_clone_are_rejected(self):
        for responses in [[(0, b"a" * 40, b"")],
                          [(0, b"b" * 40, b""), (0, b"true", b"")]]:
            with self.subTest(responses=responses), patch.object(scanner, "execute", side_effect=responses):
                with self.assertRaises(ValueError):
                    scanner.source_identity(self.root, "b" * 40, self.root)

    def test_missing_or_incompatible_tool_never_becomes_clean(self):
        for response in [(127, b"", b""), (0, b"wrong-version", b"")]:
            with patch.object(scanner, "execute", return_value=response):
                result = scanner.scan_one("gitleaks", self.root / "tool", self.root, "a" * 40, self.root)
                self.assertEqual(result["status"], "error")

    def test_scanner_errors_missing_malformed_and_oversized_reports_fail(self):
        report = self.root / "gitleaks.json"
        for content in (None, "not JSON", "{}", "[1]", "x" * (16 * 1024 * 1024 + 1)):
            if content is not None:
                report.write_text(content)
            with self.subTest(content_type=type(content).__name__):
                with patch.object(scanner, "execute", side_effect=[(0, b"8.30.1", b""), (42, b"", b"INF 1 commits scanned")]):
                    result = scanner.scan_one("gitleaks", self.root / "tool", self.root, "a" * 40, self.root)
                    self.assertEqual(result["status"], "error")
            report.unlink(missing_ok=True)

    def test_both_scanners_run_and_private_state_is_retired_after_failure(self):
        with patch.object(scanner, "source_identity"), \
                patch.object(scanner, "prepare_merge_blobs", return_value={"mergeOnlyBlobs": 0}), \
                patch.object(scanner, "scan_one") as scan_one:
            scan_one.side_effect = [{"name": "gitleaks", "status": "error"},
                                    {"name": "trufflehog", "status": "clean"}]
            result = scanner.scan(self.root, self.root, "a" * 40, self.root)
        self.assertEqual(scan_one.call_count, 2)
        self.assertEqual(len(result["tools"]), 2)
        self.assertEqual(list(self.root.iterdir()), [])

    def test_gitleaks_zero_exit_with_internal_errors_or_no_commits_is_rejected(self):
        for diagnostics in (b"INF 0 commits scanned", b"ERR git failed\nINF 1 commits scanned", b""):
            with patch.object(scanner, "execute", side_effect=[(0, b"8.30.1", b""), (0, b"", diagnostics)]):
                result = scanner.scan_one("gitleaks", self.root / "tool", self.root, "a" * 40, self.root)
            self.assertEqual(result["status"], "error")
            self.assertEqual(result["reason"], "git-scan-incomplete-or-reported-error")

    def test_private_state_is_retired_after_source_failure(self):
        with patch.object(scanner, "source_identity", side_effect=ValueError("failure")):
            with self.assertRaises(ValueError):
                scanner.scan(self.root, self.root, "a" * 40, self.root)
        self.assertEqual(list(self.root.iterdir()), [])

    def test_findings_and_error_exit_classification_never_expose_values(self):
        for code, count, expected in [(0, 0, "clean"), (183, 1, "findings"), (42, 0, "error"), (183, 0, "error")]:
            raw = (json.dumps({"Raw": "synthetic-private"}) + "\n").encode() if count else b""
            with patch.object(scanner, "execute", side_effect=[(0, b"trufflehog 3.99.7", b""), (code, raw, b"")]):
                result = scanner.scan_one("trufflehog", self.root / "tool", self.root, "a" * 40, self.root)
            self.assertEqual(result["status"], expected)
            self.assertNotIn("synthetic-private", json.dumps(result))


if __name__ == "__main__":
    unittest.main()
