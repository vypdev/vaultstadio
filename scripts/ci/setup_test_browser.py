#!/usr/bin/env python3
"""Install a pinned official headless browser without consulting a shared cache."""
import hashlib
import os
from pathlib import Path, PurePosixPath
import platform
import re
import stat
import subprocess
import tempfile
import zipfile

BROWSER_VERSION = "155.0.8059.84"
ARCHIVE_SHA256 = {
    "mac-arm64": "7f111980bcb41e908d0fa3e969a625e7777ff683c1fc43fd7759d05ec3ffe52b",
    "mac-x64": "3c5bf34eb8daa874e8b50edf900eab618b74c1b9596e596340278eeba4ff1ae1",
}
MAX_ARCHIVE_BYTES = 250 * 1024 * 1024
MAX_UNPACKED_BYTES = 512 * 1024 * 1024


def platform_tag():
    architectures = {"arm64": "mac-arm64", "aarch64": "mac-arm64", "x86_64": "mac-x64"}
    if platform.system() != "Darwin" or platform.machine() not in architectures:
        raise ValueError("The browser installer supports the macOS CI architecture matrix only")
    return architectures[platform.machine()]


def download_archive(url, destination, expected_sha256):
    # Use macOS trust rather than an independently configured Python/OpenSSL CA path.
    command = ["/usr/bin/curl", "--fail", "--show-error", "--silent", "--location",
               "--proto", "=https", "--proto-redir", "=https", "--tlsv1.2",
               "--connect-timeout", "30", "--max-time", "600",
               "--max-filesize", str(MAX_ARCHIVE_BYTES), "--output", str(destination), url]
    try:
        subprocess.run(command, check=True, timeout=630, stderr=subprocess.PIPE, text=True)
    except subprocess.CalledProcessError as error:
        raise RuntimeError(f"Browser HTTPS download failed (curl {error.returncode}): {(error.stderr or '').strip()}") from error
    digest = hashlib.sha256()
    total = 0
    with destination.open("rb") as downloaded:
        while chunk := downloaded.read(1024 * 1024):
            total += len(chunk)
            if total > MAX_ARCHIVE_BYTES:
                raise ValueError("Browser download exceeded its size budget")
            digest.update(chunk)
    if digest.hexdigest() != expected_sha256:
        raise ValueError("The official browser archive failed its pinned integrity check")


def extract_archive(archive, destination):
    with zipfile.ZipFile(archive) as bundle:
        entries = bundle.infolist()
        if not entries or sum(entry.file_size for entry in entries) > MAX_UNPACKED_BYTES:
            raise ValueError("Empty or oversized browser archive")
        names = set()
        for entry in entries:
            name = PurePosixPath(entry.filename)
            mode = entry.external_attr >> 16
            if (name.is_absolute() or ".." in name.parts or "\\" in entry.filename or
                    not name.parts or name in names or
                    stat.S_IFMT(mode) not in {0, stat.S_IFREG, stat.S_IFDIR}):
                raise ValueError("Unsafe, duplicate or non-regular browser archive entry")
            names.add(name)
        bundle.extractall(destination)
        for entry in entries:
            target = destination / entry.filename
            if not entry.is_dir():
                target.chmod((entry.external_attr >> 16) & 0o777 or 0o644)


def validate_browser(binary, directory):
    binary = binary.resolve(strict=True)
    if not binary.is_relative_to(directory.resolve()) or not binary.is_file() or not os.access(binary, os.X_OK):
        raise ValueError("Browser is outside the owned directory or is not executable")
    version = subprocess.check_output([str(binary), "--version"], text=True, timeout=30).strip()
    if not re.fullmatch(r"(?:Chromium|Google Chrome(?: for Testing)?) " + re.escape(BROWSER_VERSION), version):
        raise ValueError("The actual browser reported an unexpected version")
    return binary


def install_browser(environment):
    tag = platform_tag()
    workspace = Path(tempfile.mkdtemp(prefix="vaultstadio-browser-", dir=environment["RUNNER_TEMP"]))
    url = (f"https://storage.googleapis.com/chrome-for-testing-public/{BROWSER_VERSION}/"
           f"{tag}/chrome-headless-shell-{tag}.zip")
    archive = workspace / "browser.zip"
    download_archive(url, archive, ARCHIVE_SHA256[tag])
    unpacked = workspace / "unpacked"
    unpacked.mkdir()
    extract_archive(archive, unpacked)
    binary = unpacked / f"chrome-headless-shell-{tag}" / "chrome-headless-shell"
    return validate_browser(binary, workspace), workspace


def main():
    environment = dict(os.environ)
    destination = Path(environment["GITHUB_OUTPUT"])
    binary, workspace = install_browser(environment)
    with destination.open("a") as output:
        output.write(f"chrome-path={binary}\nchrome-version={BROWSER_VERSION}\n")
    print(f"Verified Headless Shell {BROWSER_VERSION} in owned directory {workspace.name}")


if __name__ == "__main__":
    main()
