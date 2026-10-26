#!/usr/bin/env python3
"""Run the pinned upstream installer once with a genuinely private child cache."""
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import urllib.request

BROWSER_VERSION = "155.0.8059.84"
BUNDLE_REVISION = "48ad923757ca74d66703209fe939badbdf80f2f4"
BUNDLE_SHA256 = "64ef3f87734770e613d120b5cf09711c8acecb1425308ce8d9068cd56af81974"
BUNDLE_URL = f"https://raw.githubusercontent.com/browser-actions/setup-chrome/{BUNDLE_REVISION}/index.js"
MAX_BUNDLE_BYTES = 2 * 1024 * 1024


def save_verified_bundle(content, destination):
    if len(content) > MAX_BUNDLE_BYTES or hashlib.sha256(content).hexdigest() != BUNDLE_SHA256:
        raise ValueError("The official browser installer failed its pinned integrity check")
    destination.write_bytes(content)


def read_outputs(path):
    lines = path.read_text().splitlines()
    result = {}
    index = 0
    while index < len(lines):
        line = lines[index]
        index += 1
        if "<<" in line:
            name, delimiter = line.split("<<", 1)
            start = index
            while index < len(lines) and lines[index] != delimiter:
                index += 1
            if not delimiter or index == len(lines):
                raise ValueError("Incomplete browser installer output")
            value = "\n".join(lines[start:index])
            index += 1
        elif "=" in line:
            name, value = line.split("=", 1)
        else:
            raise ValueError("Malformed browser installer output")
        if name not in {"chrome-path", "chrome-version"} or name in result or not value:
            raise ValueError("Unexpected, duplicate or empty browser installer output")
        result[name] = value
    if set(result) != {"chrome-path", "chrome-version"}:
        raise ValueError("Missing browser executable or version output")
    return result


def validate_browser(outputs, cache):
    if outputs["chrome-version"] != BROWSER_VERSION:
        raise ValueError("The installer returned an unexpected browser version")
    binary = Path(outputs["chrome-path"]).resolve(strict=True)
    if not binary.is_relative_to(cache.resolve()) or not binary.is_file() or not os.access(binary, os.X_OK):
        raise ValueError("The browser executable is outside the owned cache or is not executable")
    version = subprocess.check_output([str(binary), "--version"], text=True, timeout=30).strip()
    if not re.fullmatch(r"(?:Chromium|Google Chrome(?: for Testing)?) " + re.escape(BROWSER_VERSION), version):
        raise ValueError("The actual executable reported an unexpected browser version")
    return binary


def install_browser(environment):
    node = shutil.which("node", path=environment.get("PATH"))
    if not node or not re.fullmatch(r"v24\.\d+\.\d+", subprocess.check_output([node, "--version"], text=True, timeout=15).strip()):
        raise ValueError("Node.js24 is required for the pinned official browser installer")
    workspace = Path(tempfile.mkdtemp(prefix="vaultstadio-browser-", dir=environment["RUNNER_TEMP"]))
    cache = workspace / "tool-cache"
    download = workspace / "downloads"
    cache.mkdir()
    download.mkdir()
    bundle = workspace / "setup-chrome.mjs"
    with urllib.request.urlopen(BUNDLE_URL, timeout=30) as response:
        save_verified_bundle(response.read(MAX_BUNDLE_BYTES + 1), bundle)
    child = dict(environment, RUNNER_TOOL_CACHE=str(cache), RUNNER_TEMP=str(download),
                 GITHUB_OUTPUT=str(workspace / "outputs"), GITHUB_PATH=str(workspace / "paths"))
    Path(child["GITHUB_OUTPUT"]).touch()
    Path(child["GITHUB_PATH"]).touch()
    child.update({"INPUT_CHROME-VERSION": BROWSER_VERSION, "INPUT_INSTALL-DEPENDENCIES": "false",
                  "INPUT_INSTALL-CHROMEDRIVER": "false", "INPUT_NO-SUDO": "true"})
    subprocess.run([node, str(bundle)], env=child, check=True, timeout=600)
    outputs = read_outputs(workspace / "outputs")
    binary = validate_browser(outputs, cache)
    return binary, workspace


def main():
    environment = dict(os.environ)
    destination = Path(environment["GITHUB_OUTPUT"])
    binary, workspace = install_browser(environment)
    with destination.open("a") as output:
        output.write(f"chrome-path={binary}\nchrome-version={BROWSER_VERSION}\n")
    print(f"Verified browser {BROWSER_VERSION} in owned directory {workspace.name}")


if __name__ == "__main__":
    main()
