# Complete verification and canonical coverage — issue #84

Local verification is complete for the recorded sources below. Final PR CI, Copilot review and adoption remain pending. This repairs the verification path; it does not certify whole-platform readiness or close the remaining product roadmap.

The pre-code contract is in SDD-0001/0013/0014 (`be76a1d`); the separate Python coverage producer was specified before implementation in `6c7b546`. The machine-readable [results and forty risk controls](complete-verification-84.json) distinguish product executions, process fixtures, real Gradle/Karma controls and source populations.

## Result and scope

| Population | Executed cases | Failures / errors / skips | Source |
|---|---:|---|---|
| Backend | 3,249 | 0 / 0 / 0 | `a40445c9ad0543263a44a00dbb84ac18e4fe071e` |
| Desktop JVM | 1,324 | 0 / 0 / 0 | same product execution |
| Wasm browser | 1,200 | 0 / 0 / 0 | same product execution |
| Python verifier controls | 131, including nine pre-existing inventory controls | 0 / 0 / 0 | `c8ebf066f40f3849bc3d75f0403a6f864750967a` |

`make test-coverage` executed the 5,773 product cases in 413.14 seconds on this machine, including fresh tests and canonical JVM reports. Unchanged compilation remained reusable. The 1,092 raw product XML reconcile to the case totals; common cases executed on desktop and Wasm are repeated platform executions, not additional distinct scenarios.

The dynamic inventory declares 44 backend tasks and 52 tasks per supported frontend destination. Eighteen backend tasks and 47 in each frontend destination have no test sources. They contribute no passed cases. Another 53 iOS simulator and three Android flavor unit tasks are explicitly outside this default execution certificate.

## Coverage populations

| Population | Covered / total lines | Covered / total branches |
|---|---|---|
| Backend JVM | 8,958 / 16,877 (53.08%) | 1,934 / 6,042 (32.01%) |
| Desktop JVM | 6,153 / 21,079 (29.19%) | 853 / 5,508 (15.49%) |
| Python verification adapters | 584 / 595 (98.15%) | 242 / 254 (95.28%) |

Canonical JVM inputs include 44 backend and 52 desktop production modules. All 2,450 backend and 1,891 desktop compiled class inputs are recorded with hashes; JaCoCo emits 2,177 and 1,644 report classes respectively after its own compiler/synthetic/non-executable filtering. The adapter supplies all main class outputs and introduces no class/source exclusion. These input/report counts are not interchangeable denominators.

Source-file lines shared by nested classes are consolidated according to [JaCoCo's counter semantics](https://www.jacoco.org/jacoco/trunk/doc/counters.html). Class line counters cannot simply be added. Source/root counters and additive class branch/instruction counters are reconciled; legitimate branch-free code has zero branches. The current source inventory found no package/filename collisions across modules within either root.

The Python population is separate tooling code, rather than JVM or whole-platform coverage. Its existing hosted tests are instrumented once, including real CLI subprocesses, with coverage.py 7.17.0. The signed/checksummed Codecov CLI and global targets remain unchanged. The upload requires canonical reports with matching source and workflow-run identities; a successful producer from an earlier attempt of the same run remains usable for an upload-only retry.

These full populations differ from earlier partial per-module reports. Do not interpret population changes as newly tested behavior. The low global JVM coverage and the many frontend modules without their own tests remain concrete roadmap work.

## Deliberate failures and restoration

A temporary assertion in the formerly omitted `:core:ai` product suite produced 3,250 cases with exactly one failure and made actual backend Make fail. A temporary asynchronous Wasm assertion produced 1,201 browser cases with exactly one failure and made frontend Make fail. Desktop still passed all 1,324 cases. Both temporary source files were hash-checked, removed and the exact original source identity restored before the complete green run.

Separate real Gradle8.10/JUnit6/JaCoCo fixtures demonstrate that adding a JVM module enters the default selection, a consumer test covers another module's production class, and untested classes retain missed lines. A retired failed module's XML remains present while the restored default run counts only its current case. A real Gradle9.3.1/Kotlin2.4.20 fixture grows from one to two cases in both desktop and Wasm after adding a module, without changing verifier module lists. The initial generated Wasm lock caused an honest source-drift failure; freezing that lock before verification produces green. No product lock changed.

A temporary impossible coverage rule and a deliberate lint failure both produce nonzero results with their original diagnostics. The original unchanged gate and lint task pass afterward. Both actual project coverage gate groups and `make lint` pass; the legacy advisory Detekt baseline is disclosed, not treated as zero debt.

Process/XML controls cover independent-suite continuation, original stderr/status, unknown flags, stale/retired/missing/malformed reports, skipped/zero cases, duplicates, source/class/exec mutation and mixed-source uploads. Final inspection also reproduced a stale inventory false green when a producer returned zero without rewriting its output; the regression requires invalidating the previous model before discovery.

## Evidence and next acceptance step

Raw product XML, commands, exec inputs and receipts are preserved in a source-bound archive with SHA256 `8d900a42fa86b879c7946a6a65e65864f061db195699141685451229918a8dbc`. The JSON result lists the risk controls and exact measured revisions; a fixture is never substituted for a product suite or CI result.

Current workflow syntax, cadence, secret-scanner/keyring and setup contracts, anonymous-share Node/proxy tests, actual Nginx MIME/startup, accessibility inventory and architecture checks pass locally. Full-history Gitleaks8.30.1 and TruffleHog3.99.7 report no new findings; only the existing exactly reviewed non-secret matches remain. Original source drafts, active dogfood data/package and rollback are preserved.

Final-ref CI must execute all producers, validate the three report populations and run Copilot review before adoption. No admin merge exception is implied by the authorized branch recovery. Master, branch protections, Copilot implementation/generated workflows and `VERIFICATION_SETUP=false` remain unchanged.

Contributor commands and output paths: [Testing guide](../TESTING.md). Outstanding capabilities and test gaps: [audit roadmap](../AUDIT_ROADMAP_2026-10-25.md).
