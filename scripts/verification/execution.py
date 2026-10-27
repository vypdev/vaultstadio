"""Complete suite execution and source-bound result reconciliation."""

from __future__ import annotations

import hashlib
from pathlib import Path

from discovery import (
    VerificationError,
    discover,
    gradle,
    has_sources,
    owned_output,
    source_identity,
)
from population import snapshot
from reports import COUNTERS, collect
from state import (
    execution_context,
    new_run,
    now,
    preserve_reports,
    reset_outputs,
    write_json,
)


def test_suites(root: Path, suites: tuple[str, ...], model: dict | None = None) -> int:
    model = model or discover(root)
    overall = 0
    for suite in suites:
        run = new_run(root, suite)
        source = source_identity()
        tasks = [task for task in model["tasks"] if task["suite"] == suite]
        if not tasks:
            raise VerificationError(f"No applicable {suite} tasks in {root.name}")
        started = now()
        reset_outputs(root, tasks)
        arguments = [
            argument for task in tasks for argument in (task["path"], "--rerun")
        ]
        code = gradle(root, arguments)
        rows, errors = [], []
        for task in tasks:
            try:
                rows.append(collect(task, has_sources(task)))
            except (OSError, VerificationError) as error:
                errors.append(str(error))
        if source_identity() != source:
            errors.append(
                "Source changed during verification; results cannot certify the new source"
            )
        totals = {key: sum(row[key] for row in rows) for key in COUNTERS}
        if (
            totals["tests"] == 0
            or totals["failures"]
            or totals["errors"]
            or totals["skipped"]
        ):
            errors.append(f"Incomplete or unsuccessful {suite} cases: {totals}")
        execution = []
        for task in tasks:
            if task.get("execution") and Path(task["marker"]).is_file():
                path = owned_output(root, task["execution"])
                if not path.is_file() or path.stat().st_size == 0:
                    errors.append(f"Missing JVM execution data: {task['path']}")
                else:
                    execution.append(
                        {
                            "path": str(path),
                            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                        }
                    )
        receipt = {
            "schemaVersion": 1,
            "source": source,
            "context": execution_context(),
            "root": str(root),
            "suite": suite,
            "startedUtc": started,
            "finishedUtc": now(),
            "gradleExit": code,
            "tasks": rows,
            "selectedTasks": [task["path"] for task in tasks],
            "additionalTasks": [
                task["path"] for task in model["tasks"] if task["suite"] == "additional"
            ],
            "production": snapshot(root, model) if execution else [],
            "totals": totals,
            "errors": errors,
            "executionData": execution,
            "savedReports": preserve_reports(run, rows),
            "success": code == 0 and not errors,
        }
        write_json(run / "receipt.json", receipt)
        write_json(root / f"build/verification/{suite}.json", receipt)
        print(
            f"{root.name}/{suite}: {totals}; receipt={run / 'receipt.json'}", flush=True
        )
        for error in errors:
            print(f"Verification error: {error}", flush=True)
        if not receipt["success"]:
            overall = overall or code or 1
    return overall
