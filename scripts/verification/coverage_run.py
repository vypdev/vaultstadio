"""Canonical JVM coverage consumes only successful, matching suite execution."""

from __future__ import annotations

import shutil
from pathlib import Path

from discovery import VerificationError, gradle, owned_output, source_identity
from population import snapshot
from reports import coverage
from state import execution_context, new_run, now, read_json, verify_saved, write_json


def generate(root: Path, model: dict) -> int:
    suite = "backend" if root.name == "backend" else "desktop"
    receipt = read_json(
        owned_output(root, str(root / f"build/verification/{suite}.json"))
    )
    if root.name == "frontend":
        browser = read_json(
            owned_output(root, str(root / "build/verification/browser.json"))
        )
        expected_browser = sorted(
            task["path"] for task in model["tasks"] if task["suite"] == "browser"
        )
        if (
            browser.get("success") is not True
            or browser.get("context") != execution_context()
            or browser["source"] != source_identity()
            or sorted(browser["selectedTasks"]) != expected_browser
        ):
            raise VerificationError(
                "Frontend coverage needs matching successful browser execution too"
            )
        verify_saved(browser)
    if receipt.get("context") != execution_context():
        raise VerificationError("Coverage cannot reuse another CI run/job execution")
    if not receipt.get("success") or receipt["source"] != source_identity():
        raise VerificationError(
            f"Coverage needs successful current-source {suite} execution"
        )
    expected = sorted(task["path"] for task in model["tasks"] if task["suite"] == suite)
    if sorted(receipt["selectedTasks"]) != expected:
        raise VerificationError("Coverage task inventory changed since test execution")
    verify_saved(receipt)
    if snapshot(root, model) != receipt["production"]:
        raise VerificationError(
            "Compiled production classes changed since test execution"
        )
    if not receipt["executionData"]:
        raise VerificationError("No fresh JVM execution data for coverage")
    # Exclude stale data from tasks which did not execute in this matching run.
    executed = {record["path"] for record in receipt["executionData"]}
    for task in model["tasks"]:
        if (
            task["suite"] == suite
            and task.get("execution")
            and task["execution"] not in executed
        ):
            path = owned_output(root, task["execution"])
            if path.exists():
                raise VerificationError(f"Stale/unattested execution input: {path}")
    report = owned_output(root, str(root / "build/verification/coverage/coverage.xml"))
    if report.exists():
        report.unlink()
    code = gradle(root, ["verificationCoverage", "--rerun"])
    if code:
        return code
    if source_identity() != receipt["source"]:
        raise VerificationError("Source changed during coverage generation")
    verify_saved(receipt)
    summary = coverage(report)
    run = new_run(root, "coverage")
    shutil.copyfile(report, run / "coverage.xml")
    population = snapshot(root, model)
    if population != receipt["production"]:
        raise VerificationError(
            "Coverage compilation changed the tested production classes"
        )
    result = {
        "schemaVersion": 1,
        "source": receipt["source"],
        "suite": suite,
        "testStartedUtc": receipt["startedUtc"],
        "generatedUtc": now(),
        "population": population,
        "executionData": receipt["executionData"],
        "summary": summary,
        "report": str(report),
        "success": True,
    }
    write_json(run / "receipt.json", result)
    write_json(root / "build/verification/coverage.json", result)
    print(
        f"{root.name} canonical JVM coverage: {summary}; receipt={run / 'receipt.json'}",
        flush=True,
    )
    return 0
