"""Exact historical non-secret decisions; new commits and other matches still block."""

import json
from pathlib import Path

REVIEWED = Path(__file__).with_name("reviewed_nonsecrets.json")


def identity(name, item):
    if name == "gitleaks":
        return (name, item.get("RuleID"), item.get("Commit"), item.get("File"), item.get("StartLine"))
    metadata = item
    for field in ("SourceMetadata", "Data", "Git"):
        if not isinstance(metadata, dict):
            return None
        metadata = metadata.get(field, {})
    if not isinstance(metadata, dict):
        return None
    return (name, item.get("DetectorName"), metadata.get("commit"), metadata.get("file"), metadata.get("line"))


def reviewed_count(name, version, findings, manifest=REVIEWED):
    decisions = json.loads(manifest.read_text())
    if decisions["schemaVersion"] != 1 or decisions["tools"].get(name) != version:
        raise ValueError("non-secret-decisions-require-tool-review")
    exact = {(entry["tool"], entry["rule"], entry["commit"], entry["path"], entry["line"])
             for entry in decisions["decisions"] if entry["reason"]}
    return sum(identity(name, item) in exact for item in findings)
