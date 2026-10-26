#!/usr/bin/env python3
"""Select an existing operator-installed sandbox; never change host permissions."""
import os
from pathlib import Path
import platform
import stat

SANDBOX = Path("/opt/google/chrome/chrome-sandbox")


def validate_sandbox(metadata):
    if not stat.S_ISREG(metadata.st_mode):
        raise ValueError("Chrome sandbox helper must be a regular file, not a symlink")
    if metadata.st_uid != 0:
        raise ValueError("Chrome sandbox helper must be root-owned")
    if metadata.st_mode & 0o022:
        raise ValueError("Chrome sandbox helper must not be group or world writable")
    if not metadata.st_mode & stat.S_ISUID or not metadata.st_mode & stat.S_IXOTH:
        raise ValueError("Chrome sandbox helper must be setuid and executable by the runner")


def main():
    selected = ""
    if platform.system() == "Linux":
        validate_sandbox(SANDBOX.lstat())
        selected = str(SANDBOX)
    output = Path(os.environ["GITHUB_ENV"])
    if output.is_symlink() or not output.is_file():
        raise ValueError("An existing regular environment output file is required")
    with output.open("a") as environment:
        environment.write(f"CHROME_DEVEL_SANDBOX={selected}\n")
    print("Selected the existing Chrome sandbox helper" if selected else "Using the native browser sandbox")


if __name__ == "__main__":
    main()
