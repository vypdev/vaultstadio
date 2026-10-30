#!/usr/bin/env python3
"""Scan the complete checked-out HEAD history with both reviewed upstream CLIs."""

import argparse
import json
import re
import subprocess
import tempfile
from pathlib import Path

from secret_process import execute
from secret_findings import reviewed_count
from secret_history import prepare_merge_blobs

VERSIONS = {"gitleaks": "8.30.1", "trufflehog": "3.99.7"}


def source_identity(repository, expected, private):
    code, head, _ = execute(["git", "rev-parse", "HEAD"], private, repository, timeout=30)
    if code or not re.fullmatch(r"[0-9a-f]{40}", expected) or head.decode().strip() != expected:
        raise ValueError("source-identity")
    code, shallow, _ = execute(
        ["git", "rev-parse", "--is-shallow-repository"], private, repository, timeout=30,
    )
    if code or shallow.strip() != b"false":
        raise ValueError("complete-history-required")


def scanner_command(name, binary, repository, head, private, mode="git"):
    if name == "gitleaks":
        config = private / "gitleaks.toml"
        config.write_text("[extend]\nuseDefault = true\n")
        empty_ignore = private / ".gitleaksignore"
        empty_ignore.touch(mode=0o600)
        scope = (["git", str(repository), "--log-opts", f"--full-history {head}"]
                 if mode == "git" else ["dir", str(repository)])
        return [str(binary), *scope, "--config", str(config), "--gitleaks-ignore-path", str(empty_ignore),
                "--ignore-gitleaks-allow", "--redact=100", "--no-banner", "--no-color",
                "--exit-code=1", "--report-format=json", "--report-path", str(private / "gitleaks.json")]
    scope = (["git", repository.as_uri(), f"--branch={head}", "--skip-additional-refs"]
             if mode == "git" else ["filesystem", str(repository)])
    return [str(binary), *scope, "--no-ignore-tag", "--no-update", "--json", "--no-color",
            "--no-verification", "--results=verified,unknown,unverified",
            "--fail", "--fail-on-scan-errors", "--concurrency=2"]


def scan_one(name, binary, repository, head, private, mode="git"):
    result = {"name": name, "version": VERSIONS[name], "status": "error", "findings": None}
    code, output, errors = execute([str(binary), "version" if name == "gitleaks" else "--version"],
                                   private, timeout=30)
    version = (output + errors).decode(errors="replace").strip()
    expected = VERSIONS[name] if name == "gitleaks" else f"trufflehog {VERSIONS[name]}"
    if code or version != expected:
        return {**result, "reason": "tool-unavailable-or-incompatible"}
    command = (scanner_command(name, binary, repository, head, private) if mode == "git"
               else scanner_command(name, binary, repository, head, private, mode))
    code, output, diagnostics = execute(command, private)
    if name == "gitleaks":
        # Gitleaks 8.30.1 can return zero when its Git subprocess writes errors
        # to stderr, and then report zero scanned commits. Never certify that.
        log = diagnostics.decode(errors="replace")
        progress = re.search(r"\b([0-9]+) commits? scanned\b" if mode == "git"
                             else r"scanned ~([0-9]+) bytes", log)
        if (not progress or int(progress[1]) == 0
                or re.search(r"(?:^|\s)(?:ERR|FTL|FATAL)\s", log)):
            return {**result, "reason": "git-scan-incomplete-or-reported-error", "exitCode": code}
    try:
        if name == "gitleaks":
            report = private / "gitleaks.json"
            if report.stat().st_size > 16 * 1024 * 1024:
                raise ValueError("report-size")
            findings = json.loads(report.read_text())
        else:
            findings = [json.loads(line) for line in output.splitlines() if line.strip()]
        if not isinstance(findings, list) or any(not isinstance(item, dict) for item in findings):
            raise ValueError("report-shape")
        total = len(findings)
        reviewed = reviewed_count(name, VERSIONS[name], findings)
        count = total - reviewed
    except (OSError, ValueError, TypeError, KeyError):
        return {**result, "reason": "scanner-failed-or-invalid-report", "exitCode": code}
    status = "clean" if code == 0 and count == 0 else "error"
    if total and code in (0, 1 if name == "gitleaks" else 183):
        status = "findings" if count else "clean-with-reviewed-nonsecrets"
    return {**result, "status": status, "findings": count, "reviewedNonSecrets": reviewed,
            "totalMatches": total, "exitCode": code}


def scan(repository, tools, head, temporary=None):
    repository, tools = repository.resolve(), tools.resolve()
    with tempfile.TemporaryDirectory(prefix="secret-scan-", dir=temporary) as scratch:
        private = Path(scratch)
        source_identity(repository, head, private)
        merge_input = private / "merge-input"
        history = prepare_merge_blobs(repository, head, merge_input)
        results = []
        for name in VERSIONS:
            owned = private / name
            owned.mkdir(mode=0o700)
            results.append(scan_one(name, tools / name, repository, head, owned))
            if history["mergeOnlyBlobs"]:
                merge_output = private / (name + "-merge")
                merge_output.mkdir(mode=0o700)
                result = scan_one(name, tools / name, merge_input, head, merge_output, "filesystem")
                results.append({**result, "name": name + "-merge-blobs"})
        return {"source": head, "scope": "all commits reachable from checked-out HEAD",
                "history": history, "tools": results}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True, type=Path)
    parser.add_argument("--tools", required=True, type=Path)
    parser.add_argument("--head", required=True)
    parser.add_argument("--report", required=True, type=Path)
    arguments = parser.parse_args()
    try:
        report = scan(arguments.repository, arguments.tools, arguments.head)
    except (OSError, ValueError, subprocess.SubprocessError):
        print("Secret scanning failed: source/history or private runtime is unavailable.")
        return 2
    # The report intentionally contains no raw findings, provider responses, paths or snippets.
    arguments.report.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report))
    return 0 if all(tool["status"] in ("clean", "clean-with-reviewed-nonsecrets")
                    for tool in report["tools"]) else 1


if __name__ == "__main__":
    raise SystemExit(main())
