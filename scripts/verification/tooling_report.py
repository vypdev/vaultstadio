#!/usr/bin/env python3
"""Bind Python verification coverage to its original source and execution context."""

from __future__ import annotations

import argparse
import hashlib
import platform
from pathlib import Path

from discovery import REPOSITORY, VerificationError, owned_output, source_identity
from reports import integers, xml_document
from state import execution_context, read_json, write_json


def summary(xml: Path, data: Path, repository: Path = REPOSITORY) -> dict:
    document, digest = xml_document(xml)
    raw = read_json(data)
    if document.tag != "coverage" or raw["meta"].get("branch_coverage") is not True:
        raise VerificationError("Expected branch-enabled Python Cobertura/JSON reports")
    fields = {
        "num_statements": "lines-valid",
        "covered_lines": "lines-covered",
        "num_branches": "branches-valid",
        "covered_branches": "branches-covered",
    }
    counters = integers(document, tuple(fields.values()))
    for key, field in fields.items():
        if raw["totals"][key] != counters[field]:
            raise VerificationError("Python XML/JSON counters disagree")
    if (
        counters["lines-valid"] == 0
        or counters["lines-covered"] > counters["lines-valid"]
        or counters["branches-covered"] > counters["branches-valid"]
    ):
        raise VerificationError("Invalid Python coverage denominator")
    expected = sorted(
        str(path.relative_to(repository))
        for path in (repository / "scripts/verification").glob("*.py")
    )
    xml_files = [element.get("filename") for element in document.findall(".//class")]
    if sorted(xml_files) != expected or sorted(raw["files"]) != expected:
        raise VerificationError("Incomplete or duplicated Python production population")
    return {
        "xmlSha256": digest,
        "counters": counters,
        "files": expected,
        "toolVersion": raw["meta"]["version"],
    }


def start(directory: Path) -> None:
    for name in ["source.json", "receipt.json", "coverage.xml", "coverage.json"]:
        path = directory / name
        owned_output(REPOSITORY, str(path))
        if path.is_file():
            path.unlink()
    write_json(
        directory / "source.json",
        {"source": source_identity(), "context": execution_context()},
    )


def finish(directory: Path) -> None:
    origin = read_json(directory / "source.json")
    if (
        origin["source"] != source_identity()
        or origin["context"] != execution_context()
    ):
        raise VerificationError(
            "Python verification source/context changed during tests"
        )
    report = summary(directory / "coverage.xml", directory / "coverage.json")
    write_json(
        directory / "receipt.json",
        {
            "schemaVersion": 1,
            "kind": "python-verification",
            **origin,
            "summary": report,
            "pythonVersion": platform.python_version(),
            "success": True,
        },
    )


def verified_input(directory: Path, source: dict) -> Path:
    receipt = read_json(directory / "receipt.json")
    if (
        receipt.get("success") is not True
        or receipt.get("kind") != "python-verification"
        or receipt["source"] != source
    ):
        raise VerificationError(
            "Python/JVM coverage source identities differ or tooling failed"
        )
    report = directory / "coverage.xml"
    if receipt["summary"] != summary(report, directory / "coverage.json"):
        raise VerificationError("Python coverage receipt/report changed")
    if (
        hashlib.sha256(report.read_bytes()).hexdigest()
        != receipt["summary"]["xmlSha256"]
    ):
        raise VerificationError("Python report changed during validation")
    return report


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=("start", "finish"))
    args = parser.parse_args()
    directory = owned_output(REPOSITORY, str(REPOSITORY / "build/verification/python"))
    directory.mkdir(parents=True, exist_ok=True)
    (start if args.operation == "start" else finish)(directory)


if __name__ == "__main__":
    main()
