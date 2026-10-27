"""Exercise the shipped proxy with isolated real Nginx peers; no application build."""
from pathlib import Path
import os
import subprocess
import tempfile
import time
import unittest
import uuid

ROOT = Path(__file__).resolve().parents[2]


def docker(*arguments, check=True):
    return subprocess.run(['docker', *arguments], capture_output=True, text=True,
                          timeout=90, check=check)


class PublicShareProxyTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temporary = tempfile.TemporaryDirectory(prefix='vaultstadio-share-proxy-')
        cls.addClassCleanup(cls.temporary.cleanup)
        directory = Path(cls.temporary.name)
        backend = directory / 'backend.conf'
        backend.write_text('server { listen 8080; location / { return 200 '
                           '"$http_host|$http_x_forwarded_host"; } '
                           'location ~ /broken$ { return 444; } }\n')
        cls.image = os.environ.get('SHARE_PROXY_NGINX_IMAGE', 'nginx:alpine')
        cls.network = 'vaultstadio-share-proxy-' + uuid.uuid4().hex
        docker('network', 'create', '--internal', cls.network)
        cls.addClassCleanup(docker, 'network', 'rm', cls.network)
        for name, source in [('backend', backend), ('frontend', ROOT / 'docker/nginx.conf')]:
            result = docker('run', '-d', '--network', cls.network, '--network-alias', name,
                            '-v', str(source) + ':/etc/nginx/conf.d/default.conf:ro', cls.image)
            identifier = result.stdout.strip()
            cls.addClassCleanup(docker, 'rm', '-f', identifier)
            setattr(cls, name, identifier)
        docker('exec', cls.frontend, 'nginx', '-t')
        for _ in range(40):
            result = docker('exec', cls.frontend, 'wget', '-qO-', 'http://127.0.0.1/health', check=False)
            if result.returncode == 0:
                break
            time.sleep(0.1)
        else:
            raise AssertionError('Isolated Nginx peers failed readiness')

    def request(self, path, headers=(), expected_failure=False):
        before = docker('logs', self.frontend).stdout.count('[redacted]')
        arguments = ['exec', self.frontend, 'wget', '-qO-']
        for header in headers:
            arguments.extend(['--header', header])
        arguments.append('http://127.0.0.1' + path)
        response = docker(*arguments, check=not expected_failure)
        if expected_failure:
            self.assertNotEqual(0, response.returncode)
            self.assertIn('502', response.stderr)
        for _ in range(20):
            logs = docker('logs', self.frontend)
            if logs.stdout.count('[redacted]') > before:
                break
            time.sleep(0.05)
        else:
            self.fail('Proxy omitted its redacted request status')
        combined = logs.stdout + logs.stderr
        for marker in ['CAPABILITY_MARKER_90', 'QuerySensitiveMarker90', 'RefererSensitiveMarker90']:
            self.assertNotIn(marker, combined)
        self.assertIn('/api/v1/share/[redacted]', logs.stdout)
        return response, logs

    def test_authority_and_nondefault_port_reach_upstream(self):
        response, _ = self.request('/api/v1/share/CAPABILITY_MARKER_90',
                                   ['Host: public.invalid:18086'])
        self.assertEqual('public.invalid:18086|public.invalid:18086', response.stdout)

    def test_metadata_view_and_download_paths_are_redacted(self):
        for suffix in ['', '/view', '/download', '/view/download']:
            with self.subTest(suffix=suffix):
                _, logs = self.request('/api/v1/share/CAPABILITY_MARKER_90' + suffix)
                self.assertIn('HTTP/1.1" 200 ', logs.stdout)

    def test_normalized_encoded_capability_is_redacted(self):
        self.request('/api/v1/share/%43APABILITY_MARKER_90/view')

    def test_query_and_referer_are_not_written_to_access_logs(self):
        self.request('/api/v1/share/CAPABILITY_MARKER_90?password=QuerySensitiveMarker90',
                     ['Referer: http://public.invalid/api/v1/share/RefererSensitiveMarker90/view'])

    def test_upstream_failure_keeps_status_without_raw_diagnostic_request(self):
        _, logs = self.request('/api/v1/share/CAPABILITY_MARKER_90/broken',
                               ['Referer: http://public.invalid/RefererSensitiveMarker90'],
                               expected_failure=True)
        self.assertIn('HTTP/1.1" 502 ', logs.stdout)


if __name__ == '__main__':
    unittest.main()
