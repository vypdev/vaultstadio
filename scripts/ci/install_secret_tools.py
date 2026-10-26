#!/usr/bin/env python3
"""Install only reviewed, digest-checked upstream scanner bytes into a private directory."""

import argparse
import hashlib
import json
import os
import platform
import stat
import subprocess
import tarfile
import tempfile
from pathlib import Path

from secret_process import environment

MANIFEST = Path(__file__).with_name("secret_tools.json")
MAX_ARCHIVE = 128 * 1024 * 1024
MAX_BINARY = 256 * 1024 * 1024


def selected_assets(system=None, machine=None):
    system = (system or platform.system()).lower()
    machine = (machine or platform.machine()).lower()
    machine = {"aarch64": "arm64", "amd64": "x86_64"}.get(machine, machine)
    target = f"{system}-{machine}"
    tools = json.loads(MANIFEST.read_text())
    return [(name, info["version"], info["repository"], *info["platforms"][target])
            for name, info in tools.items()]


def install_archive(archive, digest, destination):
    if archive.stat().st_size > MAX_ARCHIVE:
        raise ValueError("archive-size")
    with archive.open("rb") as stream:
        if hashlib.file_digest(stream, "sha256").hexdigest() != digest:
            raise ValueError("archive-integrity")
    with tarfile.open(archive) as package:
        matches = [member for member in package.getmembers() if member.name == destination.name]
        if len(matches) != 1 or not matches[0].isfile() or not 0 < matches[0].size <= MAX_BINARY:
            raise ValueError("archive-member")
        # Extract only this regular file. Never materialize archive paths, links or metadata.
        binary = package.extractfile(matches[0]).read(MAX_BINARY + 1)
    if len(binary) != matches[0].size:
        raise ValueError("binary-size")
    descriptor = os.open(destination, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o700)
    try:
        with os.fdopen(descriptor, "wb") as stream:
            stream.write(binary)
    except BaseException:
        destination.unlink()
        raise


def install(directory, download=None):
    metadata = directory.lstat()
    if (not stat.S_ISDIR(metadata.st_mode) or metadata.st_uid != os.getuid()
            or stat.S_IMODE(metadata.st_mode) != 0o700 or any(directory.iterdir())):
        raise ValueError("private-empty-directory-required")
    installed = []
    try:
        with tempfile.TemporaryDirectory(prefix="secret-install-", dir=directory) as private:
            for name, version, repository, asset, digest in selected_assets():
                archive = Path(private) / asset
                url = f"https://github.com/{repository}/releases/download/v{version}/{asset}"
                if download:
                    download(url, archive)
                else:
                    subprocess.run([
                        "curl", "--disable", "--fail", "--silent", "--show-error", "--location",
                        "--proto", "=https", "--proto-redir", "=https", "--max-redirs", "3",
                        "--connect-timeout", "20", "--max-time", "180", "--max-filesize",
                        str(MAX_ARCHIVE), "--output", str(archive), url,
                    ], check=True, capture_output=True, timeout=190, stdin=subprocess.DEVNULL,
                        env=environment(private), cwd=private)
                destination = directory / name
                install_archive(archive, digest, destination)
                installed.append(destination)
                print(f"Installed {name} {version}; release archive SHA256 {digest}")
    except BaseException:
        for path in installed:
            path.unlink()
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    arguments = parser.parse_args()
    try:
        install(arguments.directory)
    except (OSError, ValueError, KeyError, tarfile.TarError, subprocess.SubprocessError) as error:
        # Do not print raw transport responses or subprocess output.
        print(f"Scanner installation failed ({type(error).__name__}).")
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
