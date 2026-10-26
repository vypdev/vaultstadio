import importlib.util
import os
from pathlib import Path
import stat
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

SOURCE = Path(__file__).resolve().parents[1] / "configure-browser-sandbox.py"
SPEC = importlib.util.spec_from_file_location("browser_sandbox", SOURCE)
sandbox = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(sandbox)


class BrowserSandboxTest(unittest.TestCase):
    def validate(self, mode, uid=0):
        sandbox.validate_sandbox(SimpleNamespace(st_mode=mode, st_uid=uid))

    def test_operator_installed_regular_root_owned_setuid_helper_is_accepted(self):
        self.validate(stat.S_IFREG | 0o4755)

    def test_symlink_and_directory_cannot_substitute_for_the_helper(self):
        for kind in [stat.S_IFLNK, stat.S_IFDIR]:
            with self.subTest(kind=kind), self.assertRaisesRegex(ValueError, "regular"):
                self.validate(kind | 0o4755)

    def test_unprivileged_owner_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "root-owned"):
            self.validate(stat.S_IFREG | 0o4755, uid=1000)

    def test_group_or_world_writable_helper_is_rejected(self):
        for mode in [0o4775, 0o4757]:
            with self.subTest(mode=mode), self.assertRaisesRegex(ValueError, "writable"):
                self.validate(stat.S_IFREG | mode)

    def test_missing_setuid_or_execute_permission_is_rejected(self):
        for mode in [0o755, 0o4744]:
            with self.subTest(mode=mode), self.assertRaisesRegex(ValueError, "setuid"):
                self.validate(stat.S_IFREG | mode)

    def test_missing_linux_helper_fails_without_publishing_an_environment(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            output = root / "environment"
            output.write_text("retained\n")
            with patch.object(sandbox.platform, "system", return_value="Linux"), \
                    patch.object(sandbox, "SANDBOX", root / "absent"), \
                    patch.dict(os.environ, {"GITHUB_ENV": str(output)}):
                with self.assertRaises(FileNotFoundError):
                    sandbox.main()
            self.assertEqual("retained\n", output.read_text())

    def test_native_macos_uses_no_linux_helper_and_clears_inherited_selection(self):
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary) / "environment"
            output.touch()
            with patch.object(sandbox.platform, "system", return_value="Darwin"), \
                    patch.dict(os.environ, {"GITHUB_ENV": str(output), "CHROME_DEVEL_SANDBOX": "/unexpected"}):
                sandbox.main()
            self.assertEqual("CHROME_DEVEL_SANDBOX=\n", output.read_text())

    def test_symlinked_output_cannot_modify_an_external_file(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            retained = root / "retained"
            retained.write_text("original\n")
            output = root / "environment"
            output.symlink_to(retained)
            with patch.object(sandbox.platform, "system", return_value="Darwin"), \
                    patch.dict(os.environ, {"GITHUB_ENV": str(output)}):
                with self.assertRaisesRegex(ValueError, "output"):
                    sandbox.main()
            self.assertEqual("original\n", retained.read_text())
