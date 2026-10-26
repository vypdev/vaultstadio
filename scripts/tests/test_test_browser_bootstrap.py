"""Failure controls use fixture executables, not a real-browser readiness claim."""
import hashlib
import importlib.util
from http.server import BaseHTTPRequestHandler, HTTPServer
import os
from pathlib import Path
import stat
import ssl
import subprocess
import tempfile
import threading
import unittest
from unittest.mock import patch
import zipfile

SOURCE = Path(__file__).resolve().parents[1] / "ci/setup_test_browser.py"
SPEC = importlib.util.spec_from_file_location("test_browser_bootstrap", SOURCE)
bootstrap = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(bootstrap)


class BrowserBootstrapTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.owned = self.root / "owned"
        self.owned.mkdir()

    def archive(self, entries):
        path = self.root / "fixture.zip"
        with zipfile.ZipFile(path, "w") as archive:
            for name, content, mode in entries:
                entry = zipfile.ZipInfo(name)
                entry.external_attr = mode << 16
                archive.writestr(entry, content)
        return path

    def executable(self, path, version=None):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("#!/bin/sh\nprintf '%s\\n' 'Google Chrome for Testing " +
                        (version or bootstrap.BROWSER_VERSION) + "'\n")
        path.chmod(0o700)
        return path

    def curl_response(self, content):
        actual_run = subprocess.run

        def run(command, **options):
            if command[0] != "/usr/bin/curl":
                return actual_run(command, **options)
            self.assertNotIn("--insecure", command)
            self.assertEqual("=https", command[command.index("--proto-redir") + 1])
            self.assertEqual("600", command[command.index("--max-time") + 1])
            self.assertEqual(630, options["timeout"])
            Path(command[command.index("--output") + 1]).write_bytes(content)
            return subprocess.CompletedProcess(command, 0)

        return patch.object(bootstrap.subprocess, "run", side_effect=run)

    def test_supported_architectures_are_explicit(self):
        for architecture, expected in [("arm64", "mac-arm64"), ("x86_64", "mac-x64")]:
            with patch.object(bootstrap.platform, "system", return_value="Darwin"):
                with patch.object(bootstrap.platform, "machine", return_value=architecture):
                    self.assertEqual(expected, bootstrap.platform_tag())

    def test_unsupported_platform_does_not_guess_a_browser(self):
        with patch.object(bootstrap.platform, "system", return_value="Linux"):
            with self.assertRaisesRegex(ValueError, "matrix"):
                bootstrap.platform_tag()

    def test_downloaded_bytes_must_match_the_pinned_digest(self):
        content = b"actual response fixture"
        with self.curl_response(content):
            bootstrap.download_archive("https://fixture.invalid", self.root / "ok", hashlib.sha256(content).hexdigest())
        self.assertEqual(content, (self.root / "ok").read_bytes())
        with self.curl_response(content):
            with self.assertRaisesRegex(ValueError, "integrity"):
                bootstrap.download_archive("https://fixture.invalid", self.root / "bad", "0" * 64)

    def test_download_size_and_transport_time_budgets_fail_closed(self):
        with patch.object(bootstrap, "MAX_ARCHIVE_BYTES", 0), self.curl_response(b"x"):
            with self.assertRaisesRegex(ValueError, "budget"):
                bootstrap.download_archive("https://fixture.invalid", self.root / "over", "0" * 64)

    def test_transport_certificate_failure_is_not_hidden_by_hash_handling(self):
        failure = subprocess.CalledProcessError(60, ["curl"], stderr="SSL certificate problem")
        with patch.object(bootstrap.subprocess, "run", side_effect=failure):
            with self.assertRaisesRegex(RuntimeError, "curl 60.*SSL certificate"):
                bootstrap.download_archive("https://fixture.invalid", self.root / "absent", "0" * 64)
        self.assertFalse((self.root / "absent").exists())

    def test_native_https_rejects_an_actual_untrusted_certificate(self):
        key, certificate = self.root / "key.pem", self.root / "certificate.pem"
        subprocess.run(["/usr/bin/openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes",
                        "-days", "1", "-subj", "/CN=localhost", "-keyout", str(key),
                        "-out", str(certificate)], check=True, stdout=subprocess.DEVNULL,
                       stderr=subprocess.DEVNULL, timeout=30)

        class Handler(BaseHTTPRequestHandler):
            def do_GET(self):
                self.send_response(200)
                self.end_headers()
                self.wfile.write(b"fixture")

        server = HTTPServer(("127.0.0.1", 0), Handler)
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.load_cert_chain(certificate, key)
        server.socket = context.wrap_socket(server.socket, server_side=True)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with patch.dict(os.environ, {"NO_PROXY": "127.0.0.1", "no_proxy": "127.0.0.1"}):
                with self.assertRaisesRegex(RuntimeError, "curl 60"):
                    bootstrap.download_archive(f"https://127.0.0.1:{server.server_port}/",
                                               self.root / "rejected", hashlib.sha256(b"fixture").hexdigest())
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)

    def test_safe_extraction_preserves_executable_mode(self):
        archive = self.archive([("bin/browser", b"fixture", stat.S_IFREG | 0o755)])
        bootstrap.extract_archive(archive, self.owned)
        self.assertEqual(b"fixture", (self.owned / "bin/browser").read_bytes())
        self.assertTrue(os.access(self.owned / "bin/browser", os.X_OK))

    def test_traversal_absolute_backslash_and_symlink_entries_are_rejected_before_extraction(self):
        cases = [("../escape", stat.S_IFREG), ("/escape", stat.S_IFREG),
                 ("a\\escape", stat.S_IFREG), ("link", stat.S_IFLNK)]
        for name, mode in cases:
            with self.subTest(name=name):
                archive = self.archive([("valid", b"first", stat.S_IFREG), (name, b"bad", mode | 0o644)])
                with self.assertRaisesRegex(ValueError, "Unsafe"):
                    bootstrap.extract_archive(archive, self.owned)
                self.assertFalse((self.owned / "valid").exists())

    def test_duplicate_normalized_paths_are_rejected(self):
        archive = self.archive([("a/b", b"one", stat.S_IFREG), ("a//b", b"two", stat.S_IFREG)])
        with self.assertRaisesRegex(ValueError, "duplicate"):
            bootstrap.extract_archive(archive, self.owned)

    def test_empty_and_oversized_archives_are_rejected(self):
        for entries in [[], [("big", b"xx", stat.S_IFREG)]]:
            with self.subTest(entries=entries), patch.object(bootstrap, "MAX_UNPACKED_BYTES", 1):
                with self.assertRaisesRegex(ValueError, "oversized"):
                    bootstrap.extract_archive(self.archive(entries), self.owned)

    def test_actual_fixture_process_version_and_owned_path_are_checked(self):
        binary = self.executable(self.owned / "browser")
        self.assertEqual(binary.resolve(), bootstrap.validate_browser(binary, self.owned))

    def test_symlink_outside_owned_directory_is_rejected(self):
        external = self.executable(self.root / "unowned")
        link = self.owned / "borrowed"
        link.symlink_to(external)
        with self.assertRaisesRegex(ValueError, "outside"):
            bootstrap.validate_browser(link, self.owned)

    def test_wrong_version_and_non_executable_are_rejected(self):
        binary = self.executable(self.owned / "browser", "154.0.0.0")
        with self.assertRaisesRegex(ValueError, "version"):
            bootstrap.validate_browser(binary, self.owned)
        binary.chmod(0o600)
        with self.assertRaisesRegex(ValueError, "not executable"):
            bootstrap.validate_browser(binary, self.owned)

    def test_full_installation_ignores_shared_cache_and_retains_the_executable(self):
        archive = self.archive([("chrome-headless-shell-mac-arm64/chrome-headless-shell",
                                 b"#!/bin/sh\necho 'Google Chrome for Testing 155.0.8059.84'\n",
                                 stat.S_IFREG | 0o755)])
        content = archive.read_bytes()
        shared = self.root / "shared"
        shared.mkdir()
        marker = shared / "do-not-touch"
        marker.write_bytes(b"original")
        environment = dict(os.environ, RUNNER_TEMP=str(self.root), RUNNER_TOOL_CACHE=str(shared))
        with patch.object(bootstrap, "platform_tag", return_value="mac-arm64"):
            with patch.dict(bootstrap.ARCHIVE_SHA256, {"mac-arm64": hashlib.sha256(content).hexdigest()}):
                with self.curl_response(content):
                    binary, workspace = bootstrap.install_browser(environment)
        self.assertTrue(binary.is_file())
        self.assertTrue(binary.is_relative_to(workspace.resolve()))
        self.assertEqual(b"original", marker.read_bytes())
        self.assertEqual([marker], list(shared.iterdir()))


if __name__ == "__main__":
    unittest.main()
