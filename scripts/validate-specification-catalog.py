#!/usr/bin/env python3
"""Validate the VaultStadio SDD catalog and its repository evidence."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "specs" / "catalog.json"
ALLOWED_STATUS = {"Draft", "Proposed", "As-built baseline", "Ready for implementation", "Implemented", "Superseded", "Rejected"}
REQUIRED_METADATA = [
    "Status:", "Date:", "Catalog capability ID:", "Last verified:",
    "Owners:", "Scope:", "Related issues/PRs:", "Required review gates:",
    "Open decisions blocking readiness:",
]
REQUIRED_HEADINGS = [
    "## 1. Executive summary", "## 2. Problem, current behavior, and evidence",
    "## 3. Actors, surfaces, and terminology", "## 4. Goals, non-goals, and fixed invariants",
    "## 5. Current versus proposed product journey", "## 6. Functional behavior and state model",
    "## 7. User-facing configuration", "## 8. Clean Architecture design",
    "## 9. UI/UX and content contract", "## 10. Failure, recovery, and cleanup",
    "## 11. Security, permissions, and privacy", "## 12. Observability and operational UX",
    "## 13. Compatibility, migration, rollout, and rollback", "## 14. Testing strategy and numeric budget",
    "## 15. Documentation and discoverability", "## 16. Acceptance scenarios",
    "## 17. Requirements traceability", "## 18. Implementation or maintenance sequence",
    "## 19. Definition of Done", "## 20. References and decisions",
]

def fail(message: str) -> None:
    print(f"ERROR: {message}")

def main() -> int:
    errors: list[str] = []
    try:
        data = json.loads(CATALOG.read_text())
    except Exception as exc:
        fail(f"cannot parse {CATALOG}: {exc}")
        return 1
    capabilities = data.get("capabilities")
    if not isinstance(capabilities, list) or not capabilities:
        fail("catalog.capabilities must be a non-empty array")
        return 1
    ids: set[str] = set()
    owners: set[str] = set()
    for index, item in enumerate(capabilities):
        prefix = f"capabilities[{index}]"
        for key in ("id", "title", "status", "scope", "owner", "specs", "evidence"):
            if key not in item:
                errors.append(f"{prefix} missing {key}")
        cid = item.get("id")
        if cid in ids:
            errors.append(f"duplicate capability id: {cid}")
        ids.add(cid)
        if item.get("status") not in ALLOWED_STATUS:
            errors.append(f"{cid}: unsupported status {item.get('status')!r}")
        specs = item.get("specs", [])
        if not isinstance(specs, list) or len(specs) != 1:
            errors.append(f"{cid}: exactly one owning SDD path is required")
        for raw in specs:
            path = ROOT / raw
            if not path.is_file():
                errors.append(f"{cid}: missing SDD path {raw}")
                continue
            text = path.read_text()
            for marker in REQUIRED_METADATA:
                if marker not in text:
                    errors.append(f"{cid}: {path}: missing metadata {marker}")
            for heading in REQUIRED_HEADINGS:
                if heading not in text:
                    errors.append(f"{cid}: {path}: missing heading {heading}")
            if f"Catalog capability ID: `{cid}`" not in text:
                errors.append(f"{cid}: {path}: metadata ID does not match catalog")
        evidence = item.get("evidence", [])
        if not isinstance(evidence, list) or not evidence:
            errors.append(f"{cid}: evidence must be a non-empty array")
        for raw in evidence:
            if not (ROOT / raw).exists():
                errors.append(f"{cid}: missing evidence path {raw}")
    if errors:
        for error in errors:
            fail(error)
        print(f"specification_catalog=FAIL errors={len(errors)}")
        return 1
    print(f"specification_catalog=PASS capabilities={len(capabilities)}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
