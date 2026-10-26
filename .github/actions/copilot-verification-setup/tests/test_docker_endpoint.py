"""No credential reuse, remote access or output-channel replacement during setup."""
import importlib.util
from pathlib import Path
import os
import stat
import subprocess
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('docker_endpoint', Path(__file__).resolve().parents[1] / 'prepare-docker-endpoint.py')
endpoint = importlib.util.module_from_spec(spec)
spec.loader.exec_module(endpoint)


class DockerEndpointTests(unittest.TestCase):
    def test_remote_and_control_character_endpoints_fail_before_any_probe(self):
        with patch.object(endpoint.shutil, 'which', return_value='/bin/docker'), patch.object(endpoint.subprocess, 'run') as execute:
            for value in ['tcp://remote:2375', 'ssh://remote', 'unix://relative', 'unix:///socket\nAUTH=secret']:
                with self.subTest(value=value), self.assertRaises(endpoint.EndpointUnavailable):
                    endpoint.resolve_endpoint({'DOCKER_HOST': value})
            execute.assert_not_called()

    def test_private_home_uses_only_standard_current_account_sockets(self):
        observed = []
        def inspect(path, *args, **kwargs):
            observed.append(str(path))
            if str(path) != '/account/.docker/run/docker.sock':
                raise FileNotFoundError()
            return SimpleNamespace(st_mode=stat.S_IFSOCK | 0o660)
        with patch.object(endpoint.shutil, 'which', return_value='/bin/docker'), patch.object(Path, 'stat', inspect), patch.object(endpoint.subprocess, 'run', return_value=SimpleNamespace(returncode=0, stdout='28.0.1\n', stderr='')):
            value = endpoint.resolve_endpoint({'HOME': '/private-verification'}, account_home='/account')
        self.assertEqual(value, 'unix:///account/.docker/run/docker.sock')
        self.assertTrue(all('config.json' not in value for value in observed))

    def test_docker_receives_literal_arguments_and_no_provider_or_client_credentials(self):
        directories = []
        def execute(arguments, env, **kwargs):
            self.assertEqual(arguments, ['/bin/docker', '--host', 'unix:///socket with $(literal)', 'info', '--format', '{{.ServerVersion}}'])
            self.assertEqual(env['HOME'], '/private-verification')
            self.assertEqual(env['DOCKER_CONTEXT'], '')
            self.assertEqual(list(Path(env['DOCKER_CONFIG']).iterdir()), [])
            for name in ['PAT', 'OPENAI_API_KEY', 'DOCKER_AUTH_CONFIG', 'DOCKER_CERT_PATH', 'DOCKER_TLS_VERIFY']:
                self.assertNotIn(name, env)
            directories.append(Path(env['DOCKER_CONFIG']))
            return SimpleNamespace(returncode=0, stdout='28.0.1\n', stderr='')
        environment = {'HOME': '/private-verification', 'DOCKER_HOST': 'unix:///socket with $(literal)',
                       'PAT': 'PRIVATE_SENTINEL', 'OPENAI_API_KEY': 'PRIVATE_SENTINEL',
                       'DOCKER_AUTH_CONFIG': 'PRIVATE_SENTINEL', 'DOCKER_TLS_VERIFY': '1'}
        with patch.object(endpoint.shutil, 'which', return_value='/bin/docker'), patch.object(Path, 'stat', return_value=SimpleNamespace(st_mode=stat.S_IFSOCK)), patch.object(endpoint.subprocess, 'run', side_effect=execute):
            self.assertEqual(endpoint.resolve_endpoint(environment), environment['DOCKER_HOST'])
        self.assertFalse(directories[0].exists())

    def test_unavailable_cli_socket_and_daemon_never_become_success(self):
        with patch.object(endpoint.shutil, 'which', return_value=None), self.assertRaises(endpoint.EndpointUnavailable):
            endpoint.resolve_endpoint({})
        with patch.object(endpoint.shutil, 'which', return_value='/bin/docker'), patch.object(Path, 'stat', side_effect=FileNotFoundError()), self.assertRaises(endpoint.EndpointUnavailable):
            endpoint.resolve_endpoint({}, account_home='/account')
        for error in [SimpleNamespace(returncode=1, stdout='', stderr='PRIVATE_SENTINEL'), SimpleNamespace(returncode=0, stdout='invalid PRIVATE_SENTINEL', stderr='')]:
            with self.subTest(error=error.returncode), patch.object(endpoint.shutil, 'which', return_value='/bin/docker'), patch.object(Path, 'stat', return_value=SimpleNamespace(st_mode=stat.S_IFSOCK)), patch.object(endpoint.subprocess, 'run', return_value=error), self.assertRaises(endpoint.EndpointUnavailable) as caught:
                endpoint.resolve_endpoint({'DOCKER_HOST': 'unix:///socket'})
            self.assertNotIn('PRIVATE_SENTINEL', str(caught.exception))

    def test_timeout_retires_private_client_state_without_raw_errors(self):
        with patch.object(endpoint.shutil, 'which', return_value='/bin/docker'), patch.object(Path, 'stat', return_value=SimpleNamespace(st_mode=stat.S_IFSOCK)), patch.object(endpoint.subprocess, 'run', side_effect=subprocess.TimeoutExpired('PRIVATE_SENTINEL', 15)), self.assertRaises(endpoint.EndpointUnavailable) as caught:
            endpoint.resolve_endpoint({'DOCKER_HOST': 'unix:///socket'})
        self.assertNotIn('PRIVATE_SENTINEL', str(caught.exception))

    def test_env_export_appends_without_overwriting_existing_job_values(self):
        with tempfile.TemporaryDirectory() as raw:
            output = Path(raw) / 'environment'
            output.write_text('KEEP=original\n')
            with patch.object(endpoint, 'resolve_endpoint', return_value='unix:///socket'):
                endpoint.main({'GITHUB_ENV': str(output)})
            self.assertEqual(output.read_text(), 'KEEP=original\nDOCKER_HOST=unix:///socket\nDOCKER_CONTEXT=\n')

    def test_symlink_hardlink_and_missing_output_channels_are_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            original = root / 'original'
            original.write_text('unchanged')
            link = root / 'symlink'
            link.symlink_to(original)
            hard = root / 'hardlink'
            os.link(original, hard)
            for path in [link, hard, root / 'missing']:
                with self.subTest(path=path.name), patch.object(endpoint, 'resolve_endpoint') as resolve, self.assertRaises(endpoint.EndpointUnavailable):
                    endpoint.main({'GITHUB_ENV': str(path)})
                resolve.assert_not_called()
            self.assertEqual(original.read_text(), 'unchanged')

    def test_output_replacement_during_probe_cannot_write_an_external_file(self):
        with tempfile.TemporaryDirectory() as raw:
            output = Path(raw) / 'environment'
            external = Path(raw) / 'external'
            output.write_text('original')
            external.write_text('protected')
            def replace(environment):
                output.unlink()
                output.symlink_to(external)
                return 'unix:///socket'
            with patch.object(endpoint, 'resolve_endpoint', side_effect=replace), self.assertRaises(endpoint.EndpointUnavailable):
                endpoint.main({'GITHUB_ENV': str(output)})
            self.assertEqual(external.read_text(), 'protected')


if __name__ == '__main__':
    unittest.main()
