"""Transport-boundary checks for the opt-in disposable-instance smoke runner."""
import contextlib
import http.server
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import unittest

RUNNER = Path(__file__).with_name('local-api-smoke.py')
REPOSITORY = RUNNER.resolve().parents[2]
TEST_ENV = {key: os.environ[key] for key in ('PATH', 'HOME', 'SYSTEMROOT', 'TMPDIR', 'LANG') if key in os.environ}

@contextlib.contextmanager
def server(redirect=None):
    hits = []
    class Handler(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            hits.append(self.path)
            if redirect:
                self.send_response(302)
                self.send_header('Location', redirect)
                self.end_headers()
                return
            body = json.dumps({'success': False, 'error': {'code': 'FIXTURE_ONLY'}}).encode()
            self.send_response(400)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        do_POST = do_GET
        def log_message(self, *_):
            pass
    instance = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Handler)
    thread = threading.Thread(target=instance.serve_forever, daemon=True)
    thread.start()
    try:
        yield 'http://127.0.0.1:' + str(instance.server_port), hits
    finally:
        instance.shutdown()
        instance.server_close()
        thread.join(timeout=5)

def run(base, output, environment=None):
    return subprocess.run(
        [sys.executable, str(RUNNER), '--base-url', base, '--output-dir', str(output)],
        env=TEST_ENV if environment is None else environment, capture_output=True, text=True, timeout=30,
    )

class LocalApiSmokeBoundaryTest(unittest.TestCase):
    def test_environment_proxy_never_receives_fixture_requests(self):
        with tempfile.TemporaryDirectory() as tmp, server() as (proxy, proxy_hits), server() as (base, target_hits):
            env = {**TEST_ENV, 'HTTP_PROXY': proxy, 'HTTPS_PROXY': proxy, 'ALL_PROXY': proxy,
                   'http_proxy': proxy, 'https_proxy': proxy, 'all_proxy': proxy,
                   'NO_PROXY': '', 'no_proxy': ''}
            result = run(base, Path(tmp) / 'output', env)
            self.assertEqual(result.returncode, 1)  # Fixture server is deliberately not a VaultStadio API.
            self.assertGreater(len(target_hits), 0)
            self.assertEqual(proxy_hits, [])
            self.assertTrue((Path(tmp) / 'output/evidence/api-dogfood-results.json').is_file())

    def test_redirect_never_reaches_another_origin(self):
        with tempfile.TemporaryDirectory() as tmp, server() as (destination, destination_hits), server(redirect=destination) as (base, target_hits):
            result = run(base, Path(tmp) / 'output')
            self.assertEqual(result.returncode, 1)
            self.assertGreater(len(target_hits), 0)
            self.assertEqual(destination_hits, [])

    def test_non_loopback_origin_is_rejected_before_creating_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            output = Path(tmp) / 'output'
            result = run('http://example.invalid', output)
            self.assertEqual(result.returncode, 2)
            self.assertFalse(output.exists())

    def test_repository_output_is_rejected_before_writing_credentials(self):
        output = REPOSITORY / '.audit-credentials-must-not-be-created'
        self.assertFalse(output.exists())
        result = run('http://127.0.0.1:1', output)
        self.assertEqual(result.returncode, 2)
        self.assertFalse(output.exists())

if __name__ == '__main__':
    unittest.main()
