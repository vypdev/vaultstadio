"""Owned output lifecycle, exclusive execution and durable verification receipts."""

from __future__ import annotations

import datetime
import fcntl
import hashlib
import json
import os
import shutil
import uuid
from contextlib import contextmanager
from pathlib import Path

from discovery import VerificationError, owned_output


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n")
    temporary.replace(path)


def read_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text())
    except (OSError, ValueError) as error:
        raise VerificationError(f"Missing/invalid receipt {path}: {error}") from error


def now() -> str:
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def new_run(root: Path, label: str) -> Path:
    path = owned_output(
        root, str(root / "build/verification/runs" / f"{label}-{uuid.uuid4().hex}")
    )
    path.mkdir(parents=True, exist_ok=False)
    return path


@contextmanager
def exclusive(root: Path):
    path = owned_output(root, str(root / "build/verification/execution.lock"))
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor = os.open(path, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
    with os.fdopen(descriptor, "w") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as error:
            raise VerificationError(f"Verification already active in {root}") from error
        yield


def reset_outputs(root: Path, tasks: list[dict]) -> None:
    for task in tasks:
        xml = owned_output(root, task["xml"])
        files = sorted(xml.rglob("*.xml"))
        files += [owned_output(root, task["marker"])]
        if task.get("execution"):
            files.append(owned_output(root, task["execution"]))
        for path in files:
            owned_output(root, str(path))
            if path.is_file():
                path.unlink()
        home = owned_output(root, task["testHome"])
        expected = root / "build/verification/test-home"
        if not home.is_relative_to(expected) or home == expected:
            raise VerificationError(f"Unexpected test HOME: {home}")
        if home.exists():
            if any(path.is_symlink() for path in home.rglob("*")):
                raise VerificationError(f"Symlink in owned test HOME: {home}")
            shutil.rmtree(home)
        home.mkdir(parents=True)


def preserve_reports(run: Path, rows: list[dict]) -> list[dict]:
    saved = []
    for row in rows:
        for number, report in enumerate(row["reports"]):
            source = Path(report["path"])
            if hashlib.sha256(source.read_bytes()).hexdigest() != report["sha256"]:
                raise VerificationError(f"Report changed during collection: {source}")
            destination = (
                run
                / "junit"
                / hashlib.sha256(row["task"].encode()).hexdigest()
                / f"{number}.xml"
            )
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, destination)
            saved.append(
                {**report, "originalPath": str(source), "path": str(destination)}
            )
    return saved


def verify_saved(receipt: dict) -> None:
    for record in receipt["savedReports"]:
        path = Path(record["path"])
        if (
            not path.is_file()
            or hashlib.sha256(path.read_bytes()).hexdigest() != record["sha256"]
        ):
            raise VerificationError(f"Missing/changed saved report: {path}")
    for record in receipt["executionData"]:
        path = Path(record["path"])
        if (
            not path.is_file()
            or hashlib.sha256(path.read_bytes()).hexdigest() != record["sha256"]
        ):
            raise VerificationError(f"Missing/changed execution data: {path}")


def execution_context() -> dict:
    return {
        key: os.environ.get(key)
        for key in ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT", "GITHUB_JOB")
    }


def invalidate(root: Path, suites: tuple[str, ...]) -> None:
    paths = [root / f"build/verification/{suite}.json" for suite in suites]
    paths += [
        root / "build/verification/coverage.json",
        root / "build/verification/coverage/coverage.xml",
    ]
    for path in paths:
        owned_output(root, str(path))
        if path.is_file():
            path.unlink()
