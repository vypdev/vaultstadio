"""Compiled production identity shared by execution and coverage reconciliation."""

import hashlib
from pathlib import Path

from discovery import VerificationError, owned_output


def snapshot(root: Path, model: dict) -> list[dict]:
    population = []
    for entry in model["production"]:
        files = []
        for directory in entry["classes"]:
            path = owned_output(root, directory)
            for file in sorted(path.rglob("*.class")):
                owned_output(root, str(file))
                files.append(
                    {
                        "path": str(file),
                        "sha256": hashlib.sha256(file.read_bytes()).hexdigest(),
                    }
                )
        population.append({**entry, "classFiles": files})
    if not any(entry["classFiles"] for entry in population):
        raise VerificationError("Empty compiled production population")
    return population
