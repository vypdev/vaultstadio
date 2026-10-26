#!/usr/bin/env python3
"""Exercise the production Nginx config with real HTTP and disposable static assets."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, file, code, message, headers, new_url):
        return None


def command(arguments, environment):
    try:
        return subprocess.run(
            ["docker", *arguments], env=environment, check=True, capture_output=True,
            text=True, timeout=60,
        ).stdout.strip()
    except subprocess.CalledProcessError as error:
        raise RuntimeError(f"Docker {arguments[0]} failed with exit code {error.returncode}") from None
    except subprocess.TimeoutExpired:
        raise RuntimeError(f"Docker {arguments[0]} timed out") from None


def request(opener, url, method="GET"):
    try:
        response = opener.open(urllib.request.Request(url, method=method), timeout=3)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, response.headers.get_all("Content-Type", []), response.read()


def probe(config, image):
    checks = []
    allowed = ("PATH", "HOME", "USER", "TMPDIR", "LANG", "DOCKER_HOST", "DOCKER_CONFIG", "DOCKER_CONTEXT")
    environment = {key: os.environ[key] for key in allowed if key in os.environ}
    host = environment.get("DOCKER_HOST") or command(
        ["context", "inspect", "--format", "{{.Endpoints.docker.Host}}"], environment,
    )
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
    name = "vaultstadio-mime-" + uuid.uuid4().hex[:12]
    with tempfile.TemporaryDirectory(prefix="vaultstadio-mime-") as temporary:
        root = Path(temporary)
        assets = root / "assets"
        assets.mkdir(mode=0o755)
        wasm = b"\x00asm\x01\x00\x00\x00"
        (assets / "probe.wasm").write_bytes(wasm)
        (assets / "index.html").write_text("<!doctype html><title>mime-contract-fixture</title>")
        docker_config = root / "docker-client"
        docker_config.mkdir(mode=0o700)
        (docker_config / "config.json").write_text('{"auths":{"job-credentials.invalid":{}}}')
        environment.update(DOCKER_HOST=host, DOCKER_CONTEXT="", DOCKER_CONFIG=str(docker_config))
        try:
            command([
                "run", "--detach", "--rm", "--name", name,
                "--label", "com.vaultstadio.test=frontend-mime",
                "--publish", "127.0.0.1::80", "--add-host", "backend:127.0.0.1",
                "--volume", f"{config}:/etc/nginx/conf.d/default.conf:ro",
                "--volume", f"{assets}:/usr/share/nginx/html:ro", image,
            ], environment)
            image_id = command(["inspect", "--format", "{{.Image}}", name], environment)
            ports = json.loads(command([
                "inspect", "--format", '{{json (index .NetworkSettings.Ports "80/tcp")}}', name,
            ], environment))
            if len(ports) != 1 or ports[0]["HostIp"] != "127.0.0.1":
                raise RuntimeError("Probe must publish exactly one loopback endpoint")
            port = int(ports[0]["HostPort"])
            if not 0 < port < 65536:
                raise RuntimeError("Invalid probe port")
            base_url = f"http://127.0.0.1:{port}"
            deadline = time.monotonic() + 15
            while True:
                try:
                    request(opener, base_url)
                    break
                except (OSError, urllib.error.URLError):
                    if time.monotonic() >= deadline:
                        raise RuntimeError("Nginx did not become ready") from None
                    time.sleep(0.2)

            for method in ("GET", "HEAD"):
                status, types, body = request(opener, base_url + "/probe.wasm", method)
                valid = status == 200 and len(types) == 1 and types[0].lower() == "application/wasm"
                valid = valid and body == (wasm if method == "GET" else b"")
                checks.append({"case": f"wasm-{method.lower()}", "passed": valid, "status": status, "contentTypes": types})
            status, types, _ = request(opener, base_url + "/missing.wasm")
            checks.append({"case": "missing-wasm", "passed": status == 404, "status": status})
            status, types, body = request(opener, base_url + "/deep/client/route")
            valid = status == 200 and len(types) == 1 and types[0].startswith("text/html")
            checks.append({"case": "spa-fallback", "passed": valid and b"mime-contract-fixture" in body, "status": status})
        finally:
            # Only the unique container owned by this invocation is eligible for cleanup.
            subprocess.run(
                ["docker", "rm", "--force", name], env=environment,
                capture_output=True, timeout=30, check=False,
            )
    return {
        "passed": all(check["passed"] for check in checks), "checks": checks,
        "image": image, "imageId": image_id,
        "configSha256": hashlib.sha256(config.read_bytes()).hexdigest(),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, default=Path(__file__).resolve().parents[1] / "docker/nginx.conf")
    parser.add_argument("--image", default="nginx:alpine")
    arguments = parser.parse_args()
    try:
        result = probe(arguments.config.resolve(strict=True), arguments.image)
    except (OSError, RuntimeError, ValueError) as error:
        result = {"passed": False, "error": str(error)}
    print(json.dumps(result, indent=2))
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
