"""Real upstream binaries and Git history; synthetic detector fixtures stay offline."""

import hashlib
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
import secret_process
from secret_history import prepare_merge_blobs


class RealSecretScanners(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tools = Path(os.environ["SECRET_SCANNER_TOOLS"]).resolve()
        for name in scanner.VERSIONS:
            if not os.access(cls.tools / name, os.X_OK):
                raise RuntimeError("Real scanner binaries are required; this suite must not silently skip.")

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="real scanner spaces ")
        self.root = Path(self.temporary.name)
        self.addCleanup(self.temporary.cleanup)
        self.repository = self.root / "source with spaces"
        self.repository.mkdir()
        self.git("init", "-q")
        self.git("config", "user.name", "Synthetic audit")
        self.git("config", "user.email", "scanner-fixture@audit.invalid")
        self.commit("ordinary = true\n")

    def git(self, *arguments):
        return subprocess.check_output(["git", "-C", str(self.repository), *arguments],
                                       stderr=subprocess.DEVNULL, text=True).strip()

    def commit(self, content):
        (self.repository / "fixture.txt").write_text(content)
        self.git("add", "fixture.txt")
        self.git("commit", "-qm", "synthetic fixture")
        return self.git("rev-parse", "HEAD")

    def run_one(self, name, head=None):
        with tempfile.TemporaryDirectory(dir=self.root) as temporary:
            return scanner.scan_one(name, self.tools / name, self.repository,
                                    head or self.git("rev-parse", "HEAD"), Path(temporary))

    def test_clean_history_executes_both_scanners(self):
        result = scanner.scan(self.repository, self.tools, self.git("rev-parse", "HEAD"), self.root)
        self.assertEqual([entry["status"] for entry in result["tools"]], ["clean", "clean"])
        self.assertEqual([entry["findings"] for entry in result["tools"]], [0, 0])

    def test_real_gitleaks_detects_synthetic_marker_without_exposing_it(self):
        marker = "gh" + "p_" + hashlib.sha256(b"audit87-not-an-external-credential").hexdigest()[:36]
        self.commit(f'api_key = "{marker}"\n')
        result = self.run_one("gitleaks")
        self.assertEqual(result["status"], "findings")
        self.assertGreater(result["findings"], 0)
        self.assertTrue(marker not in json.dumps(result), "Raw fixture escaped into summary")

    def test_real_gitleaks_detects_deleted_secret_in_reachable_history(self):
        marker = "gh" + "p_" + hashlib.sha256(b"audit87-deleted-synthetic-marker").hexdigest()[:36]
        self.commit(f'api_key = "{marker}"\n')
        self.commit("ordinary = true\n")
        self.assertEqual(self.run_one("gitleaks")["status"], "findings")

    def test_secret_created_only_in_merge_and_later_deleted_is_detected(self):
        self.merge_fixture(renamed=False)

    def test_merge_secret_later_renamed_and_deleted_remains_detected(self):
        self.merge_fixture(renamed=True)

    def merge_fixture(self, renamed):
        original = self.git("branch", "--show-current")
        self.git("checkout", "-qb", "synthetic-side")
        (self.repository / "side.txt").write_text("ordinary side\n")
        self.git("add", "side.txt")
        self.git("commit", "-qm", "side fixture")
        self.git("checkout", "-q", original)
        self.commit("ordinary main\n")
        self.git("merge", "--no-ff", "--no-commit", "synthetic-side")
        marker = "gh" + "p_" + hashlib.sha256(b"merge-only-synthetic").hexdigest()[:36]
        self.commit(f'api_key = "{marker}"\n')
        if renamed:
            self.git("mv", "fixture.txt", "renamed.txt")
            self.git("commit", "-qm", "ordinary rename")
            (self.repository / "renamed.txt").write_text("ordinary restored\n")
            self.git("add", "renamed.txt")
            self.git("commit", "-qm", "remove synthetic marker")
        else:
            self.commit("ordinary restored\n")
        with tempfile.TemporaryDirectory(dir=self.root) as private:
            with patch("secret_history.MAX_BLOB", 1), self.assertRaises(ValueError):
                prepare_merge_blobs(self.repository, self.git("rev-parse", "HEAD"), Path(private) / "blobs")
        report = scanner.scan(self.repository, self.tools, self.git("rev-parse", "HEAD"), self.root)
        self.assertEqual(report["history"]["mergeOnlyBlobs"], 1)
        for name in scanner.VERSIONS:
            with self.subTest(scanner=name):
                matches = [item for item in report["tools"] if item["name"].startswith(name)]
                self.assertTrue(any(item["status"] == "findings" for item in matches))

    def test_ordinary_parent_blobs_are_not_repeated_as_merge_only(self):
        original = self.git("branch", "--show-current")
        self.git("checkout", "-qb", "ordinary-side")
        (self.repository / "side.txt").write_text("ordinary side\n")
        self.git("add", "side.txt")
        self.git("commit", "-qm", "ordinary side")
        self.git("checkout", "-q", original)
        self.commit("ordinary main\n")
        self.git("merge", "--no-ff", "-qm", "ordinary merge", "ordinary-side")
        with tempfile.TemporaryDirectory(dir=self.root) as private:
            result = prepare_merge_blobs(self.repository, self.git("rev-parse", "HEAD"), Path(private) / "blobs")
        self.assertEqual(result["mergeCommits"], 1)
        self.assertEqual(result["mergeOnlyBlobs"], 0)

    def test_real_scanners_reject_invalid_configuration(self):
        original = scanner.scanner_command

        def invalid(name, *arguments):
            command = original(name, *arguments)
            config = self.root / "invalid.config"
            config.write_text("[this is invalid:\n")
            if name == "gitleaks":
                command[command.index("--config") + 1] = str(config)
            else:
                command += ["--config", str(config)]
            return command

        for name in scanner.VERSIONS:
            with self.subTest(name=name), patch.object(scanner, "scanner_command", side_effect=invalid):
                self.assertEqual(self.run_one(name)["status"], "error")

    def test_real_gitleaks_git_error_cannot_become_a_clean_zero_exit(self):
        wrapper = self.root / "git-wrapper"
        wrapper.mkdir()
        git = wrapper / "git"
        git.write_text('#!/bin/sh\nfor argument in "$@"; do\n  if [ "$argument" = "log" ]; then echo "synthetic Git failure" >&2; exit 42; fi\ndone\nexec /usr/bin/git "$@"\n')
        git.chmod(0o700)
        original = secret_process.environment

        def with_git_failure(private):
            return {**original(private), "PATH": str(wrapper) + os.pathsep + os.defpath}

        with patch.object(secret_process, "environment", side_effect=with_git_failure):
            result = self.run_one("gitleaks")
        self.assertEqual(result["status"], "error")
        self.assertEqual(result["reason"], "git-scan-incomplete-or-reported-error")

    def test_real_trufflehog_rejects_synthetic_offline_fixture(self):
        self.trufflehog_fixture(deleted=False)

    def test_real_trufflehog_detects_deleted_offline_fixture(self):
        self.trufflehog_fixture(deleted=True)

    def trufflehog_fixture(self, deleted):
        marker = "audit87_" + hashlib.sha256(b"offline-synthetic").hexdigest()[:40]
        self.commit(f"fixture {marker}\n")
        if deleted:
            self.commit("ordinary = true\n")
        original = scanner.scanner_command
        config = self.root / "synthetic-detector.yaml"
        config.write_text(
            "detectors:\n  - name: Audit87Offline\n    keywords: [audit87_]\n"
            "    regex:\n      token: '(audit87_[a-f0-9]{40})'\n"
        )

        def controlled(name, *arguments):
            return original(name, *arguments) + ["--config", str(config)]

        with patch.object(scanner, "scanner_command", side_effect=controlled):
            result = self.run_one("trufflehog")
        self.assertEqual(result["status"], "findings")
        self.assertGreater(result["findings"], 0)
        self.assertTrue(marker not in json.dumps(result), "Raw fixture escaped into summary")

    def test_actual_entrypoint_is_nonzero_for_detected_fixture_and_report_is_safe(self):
        marker = "gh" + "p_" + hashlib.sha256(b"entrypoint-synthetic-marker").hexdigest()[:36]
        head = self.commit(f'api_key = "{marker}"\n')
        report = self.root / "safe-report.json"
        actual = subprocess.run([
            sys.executable, str(Path(scanner.__file__)), "--repository", str(self.repository),
            "--tools", str(self.tools), "--head", head, "--report", str(report),
        ], capture_output=True, text=True, timeout=90)
        self.assertEqual(actual.returncode, 1)
        self.assertTrue(marker not in actual.stdout + actual.stderr + report.read_text(),
                        "Raw fixture escaped into public output")
        self.assertEqual(len(json.loads(report.read_text())["tools"]), 2)

    def test_actual_source_identity_is_checked_before_scan(self):
        with self.assertRaises(ValueError):
            scanner.scan(self.repository, self.tools, "0" * 40, self.root)


if __name__ == "__main__":
    unittest.main()
