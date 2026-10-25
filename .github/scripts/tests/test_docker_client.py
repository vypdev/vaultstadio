"""Exercise the actual action and native Docker auth without the host keychain."""

import base64
import http.server
import json
import os
import pathlib
import shutil
import stat
import subprocess
import sys
import tempfile
import threading
import unittest

import yaml


ACTION = pathlib.Path(__file__).parents[2] / "actions/prepare-docker/action.yml"


class Registry(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("Docker-Distribution-Api-Version", "registry/2.0")
        self.send_header("Content-Length", "2")
        self.end_headers()
        self.wfile.write(b"{}")

    def log_message(self, *args):
        pass


class DockerClientTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if sys.platform != "darwin":
            raise RuntimeError("Run this native keychain regression on macOS.")
        cls.docker = shutil.which("docker")
        if not cls.docker:
            raise RuntimeError("The runner needs its Docker CLI.")
        cls.host = subprocess.check_output(
            [cls.docker, "context", "inspect", "--format", "{{.Endpoints.docker.Host}}"],
            text=True, timeout=15,
        ).strip()
        cls.commands = [step["run"] for step in yaml.safe_load(ACTION.read_text())["runs"]["steps"]]

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="docker-client-contract-")
        self.addCleanup(self.temporary.cleanup)
        self.root = pathlib.Path(self.temporary.name)
        self.helpers = self.root / "helpers"
        self.helpers.mkdir()
        self.helper_log = self.root / "helper-calls"
        self.helper_log.touch()
        helper = self.helpers / "docker-credential-osxkeychain"
        helper.write_text('#!/bin/sh\nprintf "%s\\n" "$1" >> "$HELPER_LOG"\ncat >/dev/null\nexit 65\n')
        helper.chmod(0o700)
        self.environment = {
            "PATH": os.pathsep.join([str(self.helpers), str(pathlib.Path(self.docker).parent), "/usr/bin", "/bin"]),
            "DOCKER_HOST": self.host,
            "DOCKER_CONTEXT": "",
            "HELPER_LOG": str(self.helper_log),
        }
        if "HOME" in os.environ:
            self.environment["HOME"] = os.environ["HOME"]
        self.server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Registry)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.addCleanup(self.stop_registry)
        self.registry = "127.0.0.1:" + str(self.server.server_port)

    def stop_registry(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=5)

    def prepare(self, name, ambient_auth=""):
        folder = self.root / name
        folder.mkdir()
        incoming = folder / "incoming"
        incoming.mkdir()
        output = folder / "environment"
        output.touch()
        environment = dict(self.environment, RUNNER_TEMP=str(folder), GITHUB_ENV=str(output),
                           DOCKER_CONFIG=str(incoming), DOCKER_AUTH_CONFIG=ambient_auth)
        for command in self.commands:
            subprocess.run(["/bin/bash", "-e", "-c", command], env=environment,
                           check=True, capture_output=True, text=True, timeout=30)
        environment.update(line.split("=", 1) for line in output.read_text().splitlines())
        self.assertTrue(pathlib.Path(environment["DOCKER_CONFIG"]).is_relative_to(folder))
        return environment

    def cli(self, environment, *args, password=None):
        result = subprocess.run(
            [self.docker, "--config", environment["DOCKER_CONFIG"], *args],
            input=password, env=environment, capture_output=True, text=True,
            timeout=15,
        )
        self.assertEqual(result.returncode, 0, result.stderr + "\nhelper calls: " + self.helper_log.read_text())
        return result

    def login(self, environment, name):
        self.cli(environment, "login", self.registry, "--username", name,
                 "--password-stdin", password="fixture-password\n")

    def config(self, environment):
        return json.loads((pathlib.Path(environment["DOCKER_CONFIG"]) / "config.json").read_text())

    def test_private_config_has_owner_only_permissions_and_clears_ambient_auth(self):
        ambient = json.dumps({"auths": {self.registry: {"auth": base64.b64encode(b"ambient:fixture").decode()}}})
        environment = self.prepare("first", ambient)
        directory = pathlib.Path(environment["DOCKER_CONFIG"])
        self.assertEqual(stat.S_IMODE(directory.stat().st_mode), 0o700)
        self.assertEqual(stat.S_IMODE((directory / "config.json").stat().st_mode), 0o600)
        self.assertEqual(environment.get("DOCKER_AUTH_CONFIG"), "")
        self.assertNotIn(self.registry, self.config(environment).get("auths", {}))
        self.assertEqual(self.helper_log.read_text(), "")

    def test_native_login_uses_only_the_job_config(self):
        environment = self.prepare("first")
        self.login(environment, "first-job")
        auth = self.config(environment)["auths"][self.registry]["auth"]
        self.assertEqual(base64.b64decode(auth), b"first-job:fixture-password")
        self.assertEqual(self.helper_log.read_text(), "")

    def test_logout_cannot_erase_another_job_or_use_the_keychain_after_logout(self):
        first = self.prepare("first")
        second = self.prepare("second")
        self.login(first, "first-job")
        self.login(second, "second-job")
        saved = self.config(second)
        self.cli(first, "logout", self.registry)
        self.assertNotIn(self.registry, self.config(first).get("auths", {}))
        self.assertEqual(self.config(second), saved)
        self.cli(first, "logout", self.registry)
        self.cli(second, "logout", self.registry)
        self.cli(second, "logout", self.registry)
        self.assertEqual(self.helper_log.read_text(), "")


if __name__ == "__main__":
    unittest.main()
