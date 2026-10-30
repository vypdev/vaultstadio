from pathlib import Path
import stat
import unittest
from runtime_fixture import RuntimeFixture


class RuntimeEnvironmentTest(unittest.TestCase):
    def setUp(self):
        self.fixture = RuntimeFixture()
        self.addCleanup(self.fixture.close)

    def prepare(self):
        result = self.fixture.prepare()
        self.assertEqual(0, result.returncode, result.stderr)

    def test_existing_make_command_receives_the_required_environment(self):
        self.prepare()
        result = self.fixture.run_make("test-coverage")
        self.assertEqual(0, result.returncode, result.stderr)
        data = self.fixture.captured()
        self.assertEqual(["test-coverage"], data["arguments"])
        environment = data["environment"]
        self.assertEqual(str(self.fixture.java), environment["JAVA_HOME"])
        self.assertEqual(str(self.fixture.sdk), environment["ANDROID_HOME"])
        self.assertEqual(str(self.fixture.sdk), environment["ANDROID_SDK_ROOT"])
        self.assertEqual(str(self.fixture.chrome), environment["CHROME_BIN"])
        self.assertEqual(self.fixture.environment["DOCKER_HOST"], environment["DOCKER_HOST"])
        self.assertEqual(str(self.fixture.home / ".docker"), environment["DOCKER_CONFIG"])
        self.assertEqual(str(self.fixture.home / ".gradle"), environment["GRADLE_USER_HOME"])

    def test_sandbox_helper_survives_closed_command_environment_literally(self):
        marker = self.fixture.root / "must-not-exist"
        helper = str(self.fixture.root / f"sandbox ' $(touch {marker})")
        self.fixture.environment["CHROME_DEVEL_SANDBOX"] = helper
        self.prepare()
        self.fixture.environment.pop("CHROME_DEVEL_SANDBOX")
        result = self.fixture.run_make("test")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(helper, self.fixture.captured()["environment"]["CHROME_DEVEL_SANDBOX"])
        self.assertFalse(marker.exists())

    def test_arguments_remain_literal_without_command_substitution(self):
        self.prepare()
        marker = self.fixture.root / "must-not-exist"
        arguments = ["test", "space ' quote", f"$(touch {marker})", "line\nbreak"]
        self.assertEqual(0, self.fixture.run_make(*arguments).returncode)
        self.assertEqual(arguments, self.fixture.captured()["arguments"])
        self.assertFalse(marker.exists())

    def test_nonzero_make_exit_and_stderr_are_preserved(self):
        self.prepare()
        self.fixture.environment.update(_VF_MAKE_EXIT="42", _VF_STDERR="synthetic failure")
        result = self.fixture.run_make("test")
        self.assertEqual(42, result.returncode)
        self.assertIn("synthetic failure", result.stderr)

    def test_wrapper_removes_inherited_docker_credentials_and_tls_settings(self):
        self.prepare()
        for name in ("DOCKER_AUTH_CONFIG", "DOCKER_TLS_VERIFY", "DOCKER_CERT_PATH"):
            self.fixture.environment[name] = "synthetic-not-a-real-credential"
        self.assertEqual(0, self.fixture.run_make("lint").returncode)
        environment = self.fixture.captured()["environment"]
        for name in ("DOCKER_AUTH_CONFIG", "DOCKER_TLS_VERIFY", "DOCKER_CERT_PATH"):
            self.assertIsNone(environment[name])

    def test_output_is_private_and_no_candidate_file_is_modified(self):
        candidate = self.fixture.root / "Makefile"
        candidate.write_text("owned fixture\n")
        self.prepare()
        directory = Path(self.fixture.output.read_text().strip())
        self.assertTrue(directory.is_relative_to(self.fixture.home))
        self.assertEqual(0o700, stat.S_IMODE(directory.stat().st_mode))
        self.assertEqual(0o700, stat.S_IMODE((directory / "make").stat().st_mode))
        self.assertEqual("owned fixture\n", candidate.read_text())

    def test_preparation_replays_without_overwriting_existing_wrappers(self):
        self.prepare()
        first = Path(self.fixture.output.read_text().strip()) / "make"
        original = first.read_bytes()
        self.prepare()
        directories = self.fixture.output.read_text().splitlines()
        self.assertEqual(2, len(set(directories)))
        self.assertEqual(original, first.read_bytes())
        self.assertEqual(0, self.fixture.run_make("test").returncode)
