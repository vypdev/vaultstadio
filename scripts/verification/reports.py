"""Bounded raw JUnit parsing and exact task/case reconciliation."""

from __future__ import annotations

import hashlib
import json
import xml.etree.ElementTree as ET
from pathlib import Path

from discovery import VerificationError

COUNTERS = ("tests", "failures", "errors", "skipped")
MAX_XML_BYTES = 64 * 1024 * 1024


def xml_document(path: Path) -> tuple[ET.Element, str]:
    if path.is_symlink():
        raise VerificationError(f"Symlinked XML: {path}")
    with path.open("rb") as stream:
        data = stream.read(MAX_XML_BYTES + 1)
    if len(data) > MAX_XML_BYTES or b"<!ENTITY" in data.upper():
        raise VerificationError(f"Unsafe or oversized XML: {path}")
    try:
        return ET.fromstring(data), hashlib.sha256(data).hexdigest()
    except ET.ParseError as error:
        raise VerificationError(f"Malformed XML {path}: {error}") from error


def integers(element: ET.Element, fields: tuple[str, ...]) -> dict:
    try:
        result = {key: int(element.attrib[key]) for key in fields}
        if any(value < 0 for value in result.values()):
            raise ValueError("negative counter")
        return result
    except (KeyError, ValueError) as error:
        raise VerificationError(
            f"Invalid {element.tag} counters: {element.attrib}"
        ) from error


def junit(path: Path, task: str, seen: set[tuple[str, str, str]]) -> dict:
    document, digest = xml_document(path)
    if document.tag != "testsuite":
        raise VerificationError(f"Expected one Gradle testsuite per XML: {path}")
    declared = integers(document, COUNTERS)
    cases = document.findall("testcase")
    actual = dict.fromkeys(COUNTERS, 0)
    for case in cases:
        name, classname = case.get("name"), case.get("classname")
        if not name or not classname:
            raise VerificationError(f"Unnamed test case: {path}")
        key = (task, classname, name)
        if key in seen:
            raise VerificationError(f"Duplicate collected case: {key}")
        seen.add(key)
        actual["tests"] += 1
        statuses = [
            key
            for key, tag in (
                ("failures", "failure"),
                ("errors", "error"),
                ("skipped", "skipped"),
            )
            if case.find(tag) is not None
        ]
        if len(statuses) > 1:
            raise VerificationError(f"Conflicting case outcomes: {key}")
        for status in statuses:
            actual[status] += 1
    if actual != declared:
        raise VerificationError(
            f"Suite/case counters disagree in {path}: {declared} != {actual}"
        )
    return {"path": str(path), "sha256": digest, **actual}


def collect(task: dict, has_test_sources: bool) -> dict:
    files = sorted(Path(task["xml"]).rglob("*.xml"))
    if len(files) > 10000:
        raise VerificationError(f"Unbounded XML set: {task['path']}")
    seen: set[tuple[str, str, str]] = set()
    reports = [junit(path, task["path"], seen) for path in files]
    totals = {key: sum(report[key] for report in reports) for key in COUNTERS}
    completed = Path(task["marker"]).is_file()
    runtime = None
    if completed:
        try:
            marker = json.loads(Path(task["marker"]).read_text())
            if marker["task"] != task["path"] or marker["completed"] is not True:
                raise ValueError("wrong task/outcome")
            runtime = marker["runtime"]
        except (KeyError, ValueError) as error:
            raise VerificationError(
                f"Invalid task completion marker: {task['path']}"
            ) from error
    failed_cases = totals["failures"] or totals["errors"]
    if has_test_sources and (
        totals["tests"] == 0 or (not completed and not failed_cases)
    ):
        raise VerificationError(f"Required task has no completed cases: {task['path']}")
    if not completed and reports and not failed_cases:
        raise VerificationError(f"Reports without completed task: {task['path']}")
    if failed_cases:
        outcome = "failed"
    elif not completed and not has_test_sources:
        outcome = "no-test-sources"
    elif totals["skipped"]:
        outcome = "skipped-cases"
    else:
        outcome = "passed"
    return {
        "task": task["path"],
        "outcome": outcome,
        "reports": reports,
        "runtime": runtime,
        **totals,
    }


def coverage(path: Path) -> dict:
    document, digest = xml_document(path)
    if document.tag != "report":
        raise VerificationError(f"Expected JaCoCo report: {path}")
    classes = document.findall(".//class")
    if not classes:
        raise VerificationError("Coverage contains no production classes")
    names = [element.attrib["name"] for element in classes]
    if len(names) != len(set(names)):
        raise VerificationError("Duplicate production classes in canonical coverage")
    # JaCoCo packages aggregate source files plus classes lacking SourceFile metadata.
    # Shared source lines must be counted once, rather than once per nested class.
    aggregate_nodes = []
    for package in document.findall(".//package"):
        files = package.findall("sourcefile")
        filenames = [file.get("name") for file in files]
        if len(filenames) != len(set(filenames)) or any(not name for name in filenames):
            raise VerificationError("Duplicate/unnamed coverage source file")
        aggregate_nodes.extend(files)
        for element in package.findall("class"):
            filename = element.get("sourcefilename")
            if filename is None:
                aggregate_nodes.append(element)
            elif filename not in filenames:
                raise VerificationError(
                    "Class refers to an absent coverage source file"
                )
    result = {"classes": len(classes), "sha256": digest}
    for kind in ("LINE", "BRANCH", "INSTRUCTION"):
        counters = [
            counter
            for counter in document.findall("counter")
            if counter.get("type") == kind
        ]
        per_class = [
            counter
            for element in aggregate_nodes
            for counter in element.findall("counter")
            if counter.get("type") == kind
        ]
        class_counters = [
            counter
            for element in classes
            for counter in element.findall("counter")
            if counter.get("type") == kind
        ]
        class_totals = {
            key: sum(
                integers(counter, ("missed", "covered"))[key]
                for counter in class_counters
            )
            for key in ("missed", "covered")
        }
        if kind == "BRANCH" and not counters and not per_class:
            if any(class_totals.values()):
                raise VerificationError(
                    "Class branch coverage is missing from source/root counters"
                )
            result["branch"] = {"missed": 0, "covered": 0}
            continue
        if len(counters) != 1:
            raise VerificationError(
                f"Missing/duplicate canonical coverage counter: {kind}"
            )
        total = integers(counters[0], ("missed", "covered"))
        summed = {
            key: sum(
                integers(counter, ("missed", "covered"))[key] for counter in per_class
            )
            for key in ("missed", "covered")
        }
        if summed != total:
            raise VerificationError(
                f"Source/class/root coverage counters disagree: {kind}"
            )
        result[kind.lower()] = total
    if sum(result["line"].values()) == 0:
        raise VerificationError("Empty production line denominator")
    return result
