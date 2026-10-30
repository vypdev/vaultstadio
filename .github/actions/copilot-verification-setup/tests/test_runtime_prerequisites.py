import unittest
from runtime_fixture import RuntimeFixture


class RuntimePrerequisitesTest(unittest.TestCase):
    def setUp(self):
        self.fixture = RuntimeFixture()
        self.addCleanup(self.fixture.close)

    def rejected(self, semantic):
        result = self.fixture.prepare()
        self.assertEqual(78, result.returncode, result.stderr)
        self.assertIn(semantic, result.stderr)
        self.assertEqual("", self.fixture.output.read_text())

    def test_missing_java_home(self):
        self.fixture.environment.pop("JAVA_HOME")
        self.rejected("Java 17 executable")

    def test_missing_java_executable(self):
        (self.fixture.java / "bin/java").unlink()
        self.rejected("Java 17 executable")

    def test_incompatible_java_major(self):
        self.fixture.environment["_VF_JAVA_VERSION"] = "21.0.1"
        self.rejected("Java major version 17")

    def test_java_cannot_start(self):
        self.fixture.environment["_VF_JAVA_EXIT"] = "9"
        self.rejected("Java could not start")

    def test_missing_sdk_platform(self):
        self.fixture.jar.unlink()
        self.rejected("Android SDK platform 37.0")

    def test_empty_sdk_platform(self):
        self.fixture.jar.write_bytes(b"")
        self.rejected("Android SDK platform 37.0")

    def test_legacy_sdk_does_not_satisfy_current_compile_platform(self):
        legacy = self.fixture.sdk / "platforms/android-36/android.jar"
        legacy.parent.mkdir(parents=True)
        self.fixture.jar.rename(legacy)
        self.rejected("Android SDK platform 37.0")

    def test_missing_chrome(self):
        self.fixture.environment.pop("CHROME_BIN")
        self.rejected("Chrome browser")

    def test_unexecutable_chrome(self):
        self.fixture.chrome.chmod(0o600)
        self.rejected("Chrome browser")

    def test_browser_with_missing_runtime_libraries_is_rejected(self):
        self.fixture.environment["_VF_CHROME_EXIT"] = "127"
        self.rejected("Chrome could not start")

    def test_incompatible_browser_version_is_rejected(self):
        self.fixture.environment["_VF_CHROME_VERSION"] = "Google Chrome 156.0.0.0"
        self.rejected("Chrome version 155.0.8059.84")

    def test_browser_version_suffix_is_rejected(self):
        self.fixture.environment["_VF_CHROME_VERSION"] = "Google Chrome 155.0.8059.840"
        self.rejected("Chrome version 155.0.8059.84")

    def test_official_linux_browser_version_terminator_is_accepted(self):
        self.fixture.environment["_VF_CHROME_VERSION"] = "Google Chrome for Testing 155.0.8059.84 \n"
        result = self.fixture.prepare()
        self.assertEqual(0, result.returncode, result.stderr)

    def test_plain_google_chrome_identity_is_accepted(self):
        self.fixture.environment["_VF_CHROME_VERSION"] = "Google Chrome 155.0.8059.84"
        result = self.fixture.prepare()
        self.assertEqual(0, result.returncode, result.stderr)

    def test_chromium_identity_is_accepted(self):
        self.fixture.environment["_VF_CHROME_VERSION"] = "Chromium 155.0.8059.84"
        result = self.fixture.prepare()
        self.assertEqual(0, result.returncode, result.stderr)

    def test_unexpected_nonwhitespace_browser_output_is_rejected(self):
        self.fixture.environment["_VF_CHROME_VERSION"] = "Google Chrome 155.0.8059.84\nunexpected output"
        self.rejected("Chrome version 155.0.8059.84")

    def test_unknown_browser_identity_is_rejected(self):
        self.fixture.environment["_VF_CHROME_VERSION"] = "Unrecognized Browser 155.0.8059.84"
        self.rejected("Chrome version 155.0.8059.84")

    def test_missing_docker_cli(self):
        (self.fixture.bin / "docker").unlink()
        self.rejected("Docker CLI")

    def test_local_daemon_unavailable(self):
        self.fixture.environment["_VF_DOCKER_EXIT"] = "1"
        self.rejected("local Docker daemon")

    def test_network_docker_endpoint_rejected(self):
        self.fixture.environment["DOCKER_HOST"] = "tcp://remote.invalid:2375"
        self.rejected("local Unix socket")

    def test_missing_local_socket(self):
        self.fixture.environment["DOCKER_HOST"] = "unix:///nonexistent/audit.sock"
        self.rejected("Docker socket is unavailable")

    def test_missing_output_channel(self):
        self.fixture.environment.pop("GITHUB_PATH")
        self.rejected("PATH output channel")

    def test_symlinked_runtime_root_rejected_without_external_writes(self):
        external = self.fixture.root / "external"
        external.mkdir()
        (self.fixture.home / ".vaultstadio-verification").symlink_to(external)
        self.rejected("must not be a symlink")
        self.assertEqual([], list(external.iterdir()))

    def test_symlinked_private_home_rejected(self):
        link = self.fixture.root / "home-link"
        link.symlink_to(self.fixture.home)
        self.fixture.environment["HOME"] = str(link)
        self.rejected("real private HOME")
