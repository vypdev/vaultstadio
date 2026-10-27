"""Preserve both original process streams while retaining durable command evidence."""

from __future__ import annotations

import datetime
import json
import subprocess
import sys
import uuid
from pathlib import Path
from threading import Thread


def execute(root: Path, command: list[str]) -> int:
    directory = root / "build/verification/commands"
    directory.mkdir(parents=True, exist_ok=True)
    stem = directory / uuid.uuid4().hex
    result = {
        "command": command,
        "cwd": str(root),
        "startedUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    }
    with (
        stem.with_suffix(".stdout.log").open("wb") as stdout_log,
        stem.with_suffix(".stderr.log").open("wb") as stderr_log,
    ):
        process = subprocess.Popen(
            command, cwd=root, stdout=subprocess.PIPE, stderr=subprocess.PIPE
        )
        errors = []

        def forward(source, saved, target):
            try:
                while data := source.read1(8192):
                    saved.write(data)
                    saved.flush()
                    target.buffer.write(data)
                    target.flush()
            except (OSError, AttributeError) as error:
                errors.append(str(error))
            finally:
                source.close()

        workers = [
            Thread(target=forward, args=(process.stdout, stdout_log, sys.stdout)),
            Thread(target=forward, args=(process.stderr, stderr_log, sys.stderr)),
        ]
        for worker in workers:
            worker.start()
        code = process.wait()
        for worker in workers:
            worker.join()
        result.update(
            exitCode=code,
            streamErrors=errors,
            finishedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        )
        stem.with_suffix(".json").write_text(json.dumps(result, indent=2) + "\n")
        return code if code else (1 if errors else 0)
