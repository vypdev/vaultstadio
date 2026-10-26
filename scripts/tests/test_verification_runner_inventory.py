"""Privacy and endpoint contracts for the read-only operator inventory."""
import importlib.util
import json
from pathlib import Path
import stat
import subprocess
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[1] / 'ci/inspect_verification_runner.py'
spec = importlib.util.spec_from_file_location('runner_inventory', SCRIPT)
inventory = importlib.util.module_from_spec(spec)
spec.loader.exec_module(inventory)


class VerificationRunnerInventoryTests(unittest.TestCase):
    def test_timeouts_and_missing_commands_do_not_publish_provider_errors(self):
        for error in [OSError('RAW_PRIVATE_SENTINEL'), subprocess.TimeoutExpired('RAW_PRIVATE_SENTINEL', 1)]:
            with self.subTest(error=type(error).__name__), patch.object(inventory.subprocess, 'run', side_effect=error):
                self.assertEqual(inventory.command(['java', '-version'], {}), (None, ''))
                self.assertEqual(inventory.version('java', [], r'(17)', {}), {'available': False, 'version': None})

    def test_failed_provider_output_and_unknown_version_are_not_disclosed(self):
        with patch.object(inventory, 'command', return_value=(42, 'RAW_PRIVATE_SENTINEL')):
            self.assertEqual(inventory.version('java', [], r'(17)', {})['available'], False)
        with patch.object(inventory, 'command', return_value=(0, 'unexpected RAW_PRIVATE_SENTINEL')):
            result = inventory.version('java', [], r'version "(17)"', {})
            self.assertNotIn('RAW_PRIVATE_SENTINEL', json.dumps(result))

    def test_remote_and_malformed_docker_endpoints_are_never_contacted(self):
        endpoints = ['tcp://127.0.0.1:2375', 'ssh://host', 'https://host', 'unix://relative', 'unix:///a\nsecret']
        with patch.object(inventory, 'command') as command:
            for endpoint in endpoints:
                with self.subTest(endpoint=endpoint):
                    result = inventory.docker_status('docker', {'DOCKER_HOST': endpoint})
                    self.assertFalse(result['daemon'])
                    self.assertFalse(result['localEndpoint'])
            command.assert_not_called()

    def test_context_resolution_does_not_turn_remote_docker_into_local(self):
        with patch.object(inventory, 'command', return_value=(0, 'tcp://remote:2375\n')) as command:
            self.assertFalse(inventory.docker_status('docker', {})['daemon'])
            self.assertEqual(command.call_count, 1)

    def test_local_daemon_probe_uses_empty_private_credentials(self):
        observed = []
        def execute(arguments, environment):
            directory = Path(environment['DOCKER_CONFIG'])
            self.assertTrue(directory.is_dir())
            self.assertEqual(list(directory.iterdir()), [])
            self.assertEqual(environment['DOCKER_CONTEXT'], '')
            self.assertEqual(environment['DOCKER_HOST'], 'unix:///var/run/docker.sock')
            self.assertEqual(arguments, ['docker', 'info', '--format', '{{.ServerVersion}}'])
            observed.append(directory)
            return 0, '28.0.1\n'
        with patch.object(inventory, 'command', side_effect=execute):
            self.assertEqual(inventory.docker_status('docker', {'DOCKER_HOST': 'unix:///var/run/docker.sock'})['version'], '28.0.1')
        self.assertFalse(observed[0].exists())

    def test_inventory_cannot_claim_readiness_and_does_not_forward_secrets(self):
        environment = {'PATH': '/usr/bin', 'HOME': '/private-profile', 'RUNNER_NAME': 'runner-fixture',
                       'PAT': 'RAW_PRIVATE_SENTINEL', 'OPENAI_API_KEY': 'RAW_PRIVATE_SENTINEL',
                       'DOCKER_AUTH_CONFIG': 'RAW_PRIVATE_SENTINEL', 'DOCKER_TLS_VERIFY': '1'}
        seen = []
        def docker(binary, clean):
            seen.append(clean)
            return {'cli': False, 'localEndpoint': False, 'daemon': False, 'version': None}
        with patch.object(inventory.shutil, 'which', return_value=None), patch.object(inventory, 'docker_status', side_effect=docker), patch.object(inventory, 'sandbox_status', return_value={'present': False, 'protected': False}):
            result = inventory.inventory(environment)
        self.assertTrue(result['inventoryOnly'])
        self.assertFalse(result['projectSetupExecuted'])
        self.assertFalse(result['readinessCertified'])
        self.assertIn('java17', result['missing'])
        self.assertIn('localDocker', result['missing'])
        self.assertNotIn('RAW_PRIVATE_SENTINEL', json.dumps(result))
        for key in ['PAT', 'OPENAI_API_KEY', 'DOCKER_AUTH_CONFIG', 'DOCKER_TLS_VERIFY', 'RUNNER_NAME']:
            self.assertNotIn(key, seen[0])

    def test_sandbox_metadata_rejects_links_unsafe_modes_and_nonroot_owners(self):
        regular = stat.S_IFREG | 0o4755
        parent = SimpleNamespace(st_mode=stat.S_IFDIR | 0o755, st_uid=0)
        for mode, owner, expected in [(regular, 0, True), (stat.S_IFLNK | 0o777, 0, False),
                                       (stat.S_IFREG | 0o755, 0, False), (regular | 0o022, 0, False),
                                       (regular, 1000, False)]:
            with self.subTest(mode=mode, owner=owner), patch.object(Path, 'lstat', side_effect=[SimpleNamespace(st_mode=mode, st_uid=owner, st_gid=0), parent]):
                self.assertEqual(inventory.sandbox_status(Path('/fixture/helper'))['protected'], expected)

    def test_empty_android_jar_is_missing_without_installing_anything(self):
        with tempfile.TemporaryDirectory() as raw:
            jar = Path(raw) / 'platforms/android-37.0/android.jar'
            jar.parent.mkdir(parents=True)
            jar.write_bytes(b'')
            with patch.object(inventory.shutil, 'which', return_value=None), patch.object(inventory, 'sandbox_status', return_value={'present': False, 'protected': False}):
                result = inventory.inventory({'PATH': '/nonexistent', 'ANDROID_HOME': raw})
            self.assertFalse(result['android37Present'])
            self.assertIn('android37', result['missing'])
            self.assertEqual(jar.read_bytes(), b'')


if __name__ == '__main__':
    unittest.main()
