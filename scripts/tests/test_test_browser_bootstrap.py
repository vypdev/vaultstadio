"""Failure controls for CI installation; fixture processes are not real browsers."""
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

SOURCE = Path(__file__).resolve().parents[1] / "ci/setup_test_browser.py"
SPEC = importlib.util.spec_from_file_location("test_browser_bootstrap", SOURCE)
bootstrap = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(bootstrap)


class BrowserBootstrapTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.cache = self.root / "cache"
        self.cache.mkdir()

    def executable(self, path, version=None):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("#!/bin/sh\nprintf '%s\\n' 'Google Chrome for Testing " +
                        (version or bootstrap.BROWSER_VERSION) + "'\n")
        path.chmod(0o700)
        return path

    def outputs(self, binary):
        return {"chrome-path": str(binary), "chrome-version": bootstrap.BROWSER_VERSION}

    def test_actual_fixture_process_version_and_private_app_path_are_checked(self):
        binary = self.executable(self.cache / "Browser.app/Contents/MacOS/Browser")
        self.assertEqual(binary.resolve(), bootstrap.validate_browser(self.outputs(binary), self.cache))

    def test_a_symlink_outside_the_cache_is_rejected(self):
        external = self.executable(self.root / "unowned")
        link = self.cache / "borrowed"
        link.symlink_to(external)
        with self.assertRaisesRegex(ValueError, "outside the owned cache"):
            bootstrap.validate_browser(self.outputs(link), self.cache)

    def test_non_executable_browser_is_rejected(self):
        binary = self.executable(self.cache / "browser")
        binary.chmod(0o600)
        with self.assertRaisesRegex(ValueError, "not executable"):
            bootstrap.validate_browser(self.outputs(binary), self.cache)

    def test_a_promised_version_cannot_hide_a_different_executable(self):
        binary = self.executable(self.cache / "browser", "154.0.0.0")
        with self.assertRaisesRegex(ValueError, "actual executable"):
            bootstrap.validate_browser(self.outputs(binary), self.cache)

    def test_different_installer_version_is_rejected(self):
        outputs = self.outputs(self.cache / "absent")
        outputs["chrome-version"] = "154.0.0.0"
        with self.assertRaisesRegex(ValueError, "installer returned"):
            bootstrap.validate_browser(outputs, self.cache)

    def test_hash_mismatch_never_writes_an_executable_bundle(self):
        destination = self.root / "installer.mjs"
        with self.assertRaisesRegex(ValueError, "integrity"):
            bootstrap.save_verified_bundle(b"corrupted", destination)
        self.assertFalse(destination.exists())

    def test_oversized_download_is_rejected_even_with_a_matching_digest(self):
        content = b"x" * (bootstrap.MAX_BUNDLE_BYTES + 1)
        with patch.object(bootstrap, "BUNDLE_SHA256", hashlib.sha256(content).hexdigest()):
            with self.assertRaisesRegex(ValueError, "integrity"):
                bootstrap.save_verified_bundle(content, self.root / "bundle")

    def test_real_action_multiline_output_protocol_is_supported(self):
        output = self.root / "outputs"
        output.write_text("chrome-path<<unique\n/a/b.app/Chrome\nunique\nchrome-version=" +
                          bootstrap.BROWSER_VERSION + "\n")
        self.assertEqual("/a/b.app/Chrome", bootstrap.read_outputs(output)["chrome-path"])

    def test_missing_duplicate_unknown_and_unterminated_outputs_fail_closed(self):
        cases = ["", "chrome-path=/a\n", "chrome-path=/a\nchrome-path=/b\n",
                 "unknown=value\n", "chrome-path<<end\n/a\n", "malformed\n"]
        for content in cases:
            with self.subTest(content=content):
                output = self.root / "outputs"
                output.write_text(content)
                with self.assertRaises(ValueError):
                    bootstrap.read_outputs(output)

    def test_child_process_uses_owned_cache_and_retains_browser_for_next_step(self):
        node = self.root / "node"
        node.write_text(f"#!{sys.executable}\n" + '''import os,sys,json
from pathlib import Path
if sys.argv[1] == "--version":
    print("v24.20.0");sys.exit(0)
assert Path(os.environ["GITHUB_OUTPUT"]).is_file()
assert Path(os.environ["GITHUB_PATH"]).is_file()
cache=Path(os.environ["RUNNER_TOOL_CACHE"])
browser=cache/"Browser.app/Contents/MacOS/Browser"
browser.parent.mkdir(parents=True)
browser.write_text("#!/bin/sh\\nprintf '%s\\\\n' 'Google Chrome for Testing 155.0.8059.84'\\n")
browser.chmod(0o700)
Path(os.environ["GITHUB_OUTPUT"]).write_text("chrome-path="+str(browser)+"\\nchrome-version=155.0.8059.84\\n")
keys=["RUNNER_TOOL_CACHE","GITHUB_OUTPUT","INPUT_INSTALL-DEPENDENCIES"]
(cache.parent/"observed.json").write_text(json.dumps({key:os.environ[key] for key in keys}))
''')
        node.chmod(0o700)
        old_cache = self.root / "shared-cache"
        old_cache.mkdir()
        marker = old_cache / "do-not-touch"
        marker.write_bytes(b"original")
        destination = self.root / "public-outputs"
        environment = dict(os.environ, PATH=str(self.root), RUNNER_TEMP=str(self.root),
                           RUNNER_TOOL_CACHE=str(old_cache), GITHUB_OUTPUT=str(destination))
        content = b"official-installer-fixture"
        with patch.object(bootstrap, "BUNDLE_SHA256", hashlib.sha256(content).hexdigest()):
            with patch.object(bootstrap.urllib.request, "urlopen", return_value=io.BytesIO(content)):
                binary, workspace = bootstrap.install_browser(environment)
        observed = json.loads((workspace / "observed.json").read_text())
        self.assertEqual(str(workspace / "tool-cache"), observed["RUNNER_TOOL_CACHE"])
        self.assertNotEqual(environment["GITHUB_OUTPUT"], observed["GITHUB_OUTPUT"])
        self.assertEqual("false", observed["INPUT_INSTALL-DEPENDENCIES"])
        self.assertEqual(b"original", marker.read_bytes())
        self.assertFalse(destination.exists())
        self.assertTrue(binary.is_file())


if __name__ == "__main__":
    unittest.main()
