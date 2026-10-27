"""Gradle discovery and source identity; no application imports or module lists."""

from __future__ import annotations

import hashlib
import json
import os
import subprocess
from pathlib import Path

from process import execute

REPOSITORY = Path(__file__).resolve().parents[2]
MODEL = REPOSITORY / "scripts/verification/model.gradle"
SUITES = {"backend": ("backend",), "frontend": ("desktop", "browser")}


class VerificationError(RuntimeError):
    """A verification failure with an actionable, secret-safe diagnostic."""


def source_identity(repository: Path = REPOSITORY) -> dict:
    def git(*arguments: str) -> bytes:
        return subprocess.check_output(["git", *arguments], cwd=repository)

    digest = hashlib.sha256()
    paths = sorted(
        set(git("ls-files", "-co", "--exclude-standard", "-z").split(b"\0")) - {b""}
    )
    for encoded in paths:
        path = repository / os.fsdecode(encoded)
        digest.update(encoded + b"\0")
        if path.is_symlink():
            digest.update(b"symlink\0" + os.fsencode(os.readlink(path)))
        elif path.is_file():
            digest.update(b"file\0" + path.read_bytes())
        else:
            digest.update(b"absent\0")
    return {
        "head": git("rev-parse", "HEAD").decode().strip(),
        "tree": git("rev-parse", "HEAD^{tree}").decode().strip(),
        "status": git("status", "--porcelain").decode(),
        "digest": digest.hexdigest(),
    }


def gradle(root: Path, arguments: list[str]) -> int:
    command = [
        str(root / "gradlew"),
        "--console=plain",
        "--continue",
        "--build-cache",
        "--init-script",
        str(MODEL),
        *arguments,
    ]
    # Inherit tool provisioning; do not capture or suppress the original diagnostics.
    print("Verification:", root.name, *arguments, flush=True)
    try:
        return execute(root, command)
    except OSError as error:
        raise VerificationError(
            f"Cannot execute {root / 'gradlew'}: {error}"
        ) from error


def owned_output(root: Path, value: str) -> Path:
    """Reject sources, traversal and symlink escapes before reading/removing outputs."""
    path = Path(value)
    if not path.is_absolute() or ".." in path.parts:
        raise VerificationError(f"Not an absolute Gradle output: {value}")
    try:
        relative = path.relative_to(root)
    except ValueError as error:
        raise VerificationError(f"Output escapes project root: {path}") from error
    if (
        "build" not in relative.parts
        or relative.parts.index("build") == len(relative.parts) - 1
    ):
        raise VerificationError(f"Not a generated build output: {path}")
    current = root
    for part in relative.parts:
        current /= part
        if current.is_symlink():
            raise VerificationError(f"Symlinked verification output: {current}")
    return path


def discover(root: Path) -> dict:
    code = gradle(root, ["verificationInventory", "--rerun"])
    if code:
        raise VerificationError(f"Gradle inventory failed ({code}) in {root.name}")
    path = owned_output(root, str(root / "build/verification/inventory.json"))
    try:
        model = json.loads(path.read_text())
        if model["schemaVersion"] != 1 or model["root"] != str(root):
            raise ValueError("wrong schema or project root")
        tasks = model["tasks"]
        paths = [task["path"] for task in tasks]
        if not tasks or len(paths) != len(set(paths)) or len(tasks) > 2000:
            raise ValueError("empty, duplicated or unbounded task inventory")
        for task in tasks:
            if not task["path"].startswith(":") or task["suite"] not in (
                *SUITES[root.name],
                "additional",
            ):
                raise ValueError(f"unexpected test task: {task['path']}")
            for field in ("xml", "marker", "execution", "testHome"):
                if task.get(field):
                    owned_output(root, task[field])
        if not model["production"]:
            raise ValueError("empty production population")
    except (OSError, KeyError, TypeError, ValueError) as error:
        raise VerificationError(f"Invalid Gradle inventory: {error}") from error
    return model


def has_sources(task: dict) -> bool:
    return any(
        path.is_file()
        for directory in task["sources"]
        for suffix in ("*.kt", "*.java")
        for path in Path(directory).rglob(suffix)
    )
