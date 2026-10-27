#!/usr/bin/env python3
"""Select source-bound canonical JVM reports and optional verification tooling coverage."""

import argparse
import hashlib
import json
from pathlib import Path

from discovery import VerificationError
from reports import coverage
from state import execution_context
from tooling_report import verified_input


def inputs(directories: list[Path]) -> list[str]:
    records, selected = [], []
    for directory in directories:
        reports = sorted(directory.rglob("coverage.xml"))
        receipts = sorted(directory.rglob("coverage.json"))
        if len(reports) != 1 or len(receipts) != 1:
            raise VerificationError(
                f"Expected exactly one canonical report/receipt in {directory}"
            )
        report, receipt = reports[0], receipts[0]
        if report.is_symlink() or receipt.is_symlink():
            raise VerificationError("Symlinked canonical coverage input")
        record = json.loads(receipt.read_text())
        if record.get("success") is not True or record["summary"] != coverage(report):
            raise VerificationError(f"Coverage summary/receipt mismatch in {directory}")
        if (
            record["summary"]["sha256"]
            != hashlib.sha256(report.read_bytes()).hexdigest()
        ):
            raise VerificationError(f"Changed canonical coverage in {directory}")
        records.append(record)
        selected.append(str(report))
    if records[0]["suite"] != "backend" or records[1]["suite"] != "desktop":
        raise VerificationError(
            "Canonical coverage root populations are reversed or missing"
        )
    # Successful producers from an earlier attempt of this same workflow run remain
    # valid for an upload-only retry; a different workflow run is not reusable.
    expected_run = execution_context()["GITHUB_RUN_ID"]
    if any(
        record.get("context", {}).get("GITHUB_RUN_ID") != expected_run
        for record in records
    ):
        raise VerificationError("Coverage belongs to another workflow run")
    if records[0]["source"] != records[1]["source"]:
        raise VerificationError(
            "Backend/frontend coverage certify different source identities"
        )
    return selected


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backend", type=Path, required=True)
    parser.add_argument("--frontend", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument(
        "--python",
        type=Path,
        help="Require same-source Python verification coverage too",
    )
    args = parser.parse_args()
    selected = inputs([args.backend, args.frontend])
    if args.python:
        receipt = json.loads(next(args.backend.rglob("coverage.json")).read_text())
        selected.append(str(verified_input(args.python, receipt["source"])))
    if any("\n" in path or "," in path for path in selected):
        raise VerificationError("Invalid coverage output-channel path")
    with args.output.open("a") as output:
        output.write("list=" + ",".join(selected) + "\n")
    print("Canonical JVM coverage inputs:", *selected)


if __name__ == "__main__":
    main()
