"""Exercise the real shell boundary without touching the user's GPG state."""

import os
from pathlib import Path
import stat
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "ci" / "prepare_codecov_keyring.sh"


class CodecovKeyringTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="codecov-keyring-test-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.runtime = self.root / "runner temp"
        self.runtime.mkdir()
        self.output = self.root / "github-env"

    def run_script(self, **overrides):
        env = {"PATH": os.environ["PATH"], "RUNNER_TEMP": str(self.runtime), "GITHUB_ENV": str(self.output)}
        env.update(overrides)
        return subprocess.run(["bash", str(SCRIPT)], env=env, capture_output=True, text=True)

    def exported_home(self):
        lines = self.output.read_text().splitlines()
        self.assertEqual(1, len(lines))
        key, value = lines[0].split("=", 1)
        self.assertEqual("GNUPGHOME", key)
        return Path(value)

    def test_creates_private_empty_keyring_inside_runner_temp(self):
        self.assertEqual(0, self.run_script().returncode)
        home = self.exported_home()
        self.assertEqual(self.runtime.resolve(), home.parent.resolve())
        self.assertEqual(0o700, stat.S_IMODE(home.stat().st_mode))
        self.assertEqual([], list(home.iterdir()))

    def test_repeated_runs_never_share_a_keyring(self):
        self.assertEqual(0, self.run_script().returncode)
        first = self.exported_home()
        self.output.unlink()
        self.assertEqual(0, self.run_script().returncode)
        second = self.exported_home()
        self.assertNotEqual(first, second)
        self.assertTrue(first.is_dir())

    def test_existing_global_home_and_lock_are_untouched(self):
        shared = self.root / "shared keyring"
        shared.mkdir()
        marker = shared / "pubring.db.lock"
        marker.write_bytes(b"another job owns this lock")
        self.assertEqual(0, self.run_script(GNUPGHOME=str(shared)).returncode)
        self.assertNotEqual(shared, self.exported_home())
        self.assertEqual(b"another job owns this lock", marker.read_bytes())
        self.assertEqual([marker], list(shared.iterdir()))

    def test_output_is_appended_without_replacing_existing_job_variables(self):
        self.output.write_text("EXISTING=value\n")
        self.assertEqual(0, self.run_script().returncode)
        self.assertTrue(self.output.read_text().startswith("EXISTING=value\nGNUPGHOME="))

    def test_missing_runner_temp_fails_before_creating_state(self):
        self.assertNotEqual(0, self.run_script(RUNNER_TEMP="").returncode)
        self.assertEqual([], list(self.runtime.iterdir()))
        self.assertFalse(self.output.exists())

    def test_missing_env_file_fails_before_creating_state(self):
        self.assertNotEqual(0, self.run_script(GITHUB_ENV="").returncode)
        self.assertEqual([], list(self.runtime.iterdir()))

    def test_nonexistent_temp_directory_fails_without_an_export(self):
        self.assertNotEqual(0, self.run_script(RUNNER_TEMP=str(self.root / "absent")).returncode)
        self.assertFalse(self.output.exists())

    def test_failed_env_write_is_not_success(self):
        self.assertNotEqual(0, self.run_script(GITHUB_ENV=str(self.root)).returncode)


if __name__ == "__main__":
    unittest.main()
