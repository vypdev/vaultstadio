# Testing and coverage

Backend and frontend are independent Gradle projects. There is no repository-root Gradle wrapper. Complete contributor and CI verification follows [SDD-0001](../../specs/sdd-0001-standalone-projects-and-clean-architecture.md), [SDD-0013](../../specs/sdd-0013-frontend-kmp-and-platform-architecture.md) and the [R04 contract in SDD-0014](../../specs/sdd-0014-deployment-operations-and-release.md#68-complete-verification-and-truthful-evidence--issue-84).

## Prerequisites

Use a POSIX host (macOS/Linux), Python 3.10+, Make and each project's committed wrapper. Backend requires Java 17 and a working local Docker daemon for PostgreSQL/Testcontainers integration tests. Frontend tests require a Java 17 toolchain, the configured Android SDK (currently platform 37/build tools 36.0.0), and an executable supported Chrome Headless Shell for Wasm. Native desktop launch/packaging additionally requires the JetBrains runtime selected by `frontend/.java-version`. The reviewed runtime installer and its integrity controls are documented in [PROJECT_RUNTIME_79.md](verification/PROJECT_RUNTIME_79.md) and [RUNTIME_ROLLOUT_85.md](verification/RUNTIME_ROLLOUT_85.md).

Set `JAVA_HOME`/`PATH`, `ANDROID_HOME`/`ANDROID_SDK_ROOT` and `CHROME_BIN` to the provisioned tools. For a nondefault local Docker socket, set `DOCKER_HOST` to its actual local Unix endpoint and the appropriate `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`. Do not expose a remote unauthenticated Docker endpoint. Verification does not install tools or change the user's Docker context. Missing tools remain failures with their original diagnostics.

## Complete execution

From the repository root:

```bash
make test
# Equivalent shell entrypoint, also usable from another working directory:
bash scripts/run-tests.sh
```

Individual project roots, invoked from the repository:

```bash
make -C backend test
make -C frontend test
```

The Python adapter discovers actual Gradle tasks and invokes every applicable backend JVM, frontend desktop JVM and Wasm browser test task with task-specific `--rerun` and `--continue`. Unchanged compilation can use the existing build cache. Independent suites continue to collect results, but any failure makes the final command fail; no stderr or failure is suppressed. Unknown CLI arguments fail before a partial run. JVM tests use verification-owned temporary preferences/HOME rather than the user's application preferences.

For a deliberately narrower investigation:

```bash
python3 scripts/verification/run.py test --root frontend --suite desktop
python3 scripts/verification/run.py test --root frontend --suite browser
(cd backend && ./gradlew :core:storage:test --tests '*Storage*')
```

A focused run is not evidence that every platform/module passed. Additional Android and iOS tasks are listed separately in the receipt and are not silently certified by desktop/Wasm tests. Device integration, packaged startup and business dogfooding retain their own evidence.

## Canonical coverage

```bash
make test-coverage
# Equivalent:
bash scripts/run-tests.sh --coverage
```

This runs the complete suites and then composes canonical JVM reports using all discovered production class/source outputs and matching test execution data. Classes exercised by tests in another module contribute to their owning code. Compiled classes without tests remain in the denominator. No new source/class exclusions or global thresholds are introduced.

| Root | Canonical XML | HTML | Source/population receipt |
|---|---|---|---|
| Backend | `backend/build/verification/coverage/coverage.xml` | `backend/build/verification/coverage/html/index.html` | `backend/build/verification/coverage.json` |
| Frontend desktop JVM | `frontend/build/verification/coverage/coverage.xml` | `frontend/build/verification/coverage/html/index.html` | `frontend/build/verification/coverage.json` |

JaCoCo reconciles package/root counters from source files plus classes without source-file metadata. Lines shared by nested classes count once at source-file level; summing their class line counters would duplicate them. See the [official counter semantics](https://www.jacoco.org/jacoco/trunk/doc/counters.html).

Wasm cases are executions, not JVM line/branch coverage. A valid branch-free class has zero branches; it does not have a fabricated percentage. Percentages from older partial module reports use different populations and must not be described as a testing improvement or regression without reconciling their denominators.

CI runs tests once and explicitly reuses their matching source-bound results for coverage:

```bash
python3 scripts/verification/run.py coverage --root backend --reuse-tests
python3 scripts/verification/run.py coverage --root frontend --reuse-tests
```

Reuse fails if required results are absent/failed, the CI run/job or selected tasks change, source identity changes, saved reports or execution bytes change, or compiled production classes change. Frontend reuse requires matching successful desktop and browser execution. Coverage records the original test execution time; report generation is not a new test execution.

Existing storage authorization, serializer, helper and form gates remain required in CI, as do their focused report populations. The CI artifact uploader retains raw per-module results and canonical receipts; Codecov receives exactly one validated canonical XML per root. Its signed/checksummed CLI, isolated GPG keyring, strict validation and upload-error behavior are unchanged. `codecov.yml` configures an automatic **project** target with a 1% threshold; this is not a promise of patch coverage or 100% global coverage. A new global quality policy needs its own reviewed decision.

## Verification tooling coverage

The existing hosted quality job measures its Python verifier tests once with coverage.py 7.17.0, including real CLI subprocesses. `scripts/verification/coverage.ini` aliases disposable copied entrypoints back to the corresponding repository source. CI sets an absolute `COVERAGE_FILE` so child processes cannot lose their data when temporary fixtures are retired.

`tooling_report.py start/finish` records the original source/context, rejects source drift and reconciles Cobertura/JSON totals and the complete Python file population. It publishes `build/verification/python/coverage.xml`, `coverage.json` and `receipt.json`. The existing Codecov job requires the same source identity for these files and both canonical JVM reports; missing, altered or mixed-source evidence fails selection. This is a separate tooling population with its own denominator, rather than JVM coverage or a claim about all platform code. The Gradle init adapter is exercised by real Gradle controls; these Python percentages do not quantify its line coverage. No global target or threshold changes.

## Results and failure investigation

Each root writes `build/verification/inventory.json`, `<suite>.json` and a separate `runs/<suite>-<id>/receipt.json` with copied XML. It also retains original Gradle command/stdout/stderr files under `build/verification/commands/`. Receipts identify source HEAD/tree/dirty digest, task selection, actual JVM launcher metadata, raw totals, separate failures/errors/skips and JVM execution/class hashes. Browser identity remains in the actual runner logs and JUnit descriptions. Tasks without test sources are labeled separately; their actual Gradle `NO-SOURCE`/skip diagnostics remain in the logs and never become passed cases.

Only reports from current selected tasks are collected after their old XML/exec/marker outputs are reset. Retired module reports cannot enter the totals. Missing output for required test sources, zero-case required tasks, stale/unattested results, inconsistent counters, duplicate collection and failed/skipped cases prevent a green complete receipt. Independent simultaneous verification of the same root is rejected to avoid overwriting its evidence.

Keep the native Gradle report paths for diagnosis (`<module>/build/test-results/<task>` and `<module>/build/reports/tests/<task>`). Failed tests can leave useful raw XML even when their task never completes. Artifact upload after failure does not make the failed test producer successful. Before cleanup, preserve the needed original failure evidence and source/commands; never delete user data or another active run.

## Tooling and lint contracts

```bash
python3 -m unittest discover -s scripts/tests -p 'test_verification_*.py' -v
python3 -m unittest discover -s scripts/tests -p 'test_ci_cadence.py' -v
python3 scripts/validate-specification-catalog.py
make lint
git diff --check
```

`make lint` invokes both independent roots (`detektMain` and `detektDesktopMain`) and preserves failures/diagnostics. The existing Detekt configuration includes advisory legacy debt; a successful command does not establish zero lint debt. Changed verification sources need strict focused checks and actual failure controls in addition to the legacy baseline.

Application suites remain on every new PR commit. Heavy security, Docker and native project verification retain their manual/staggered weekly cadence. Fork and concurrency protections remain in effect. See [CI_CADENCE_106.md](verification/CI_CADENCE_106.md).

The previously accepted source `816e673` had 3,249 backend, 1,324 desktop and 1,200 browser executions (5,773 total), with no failures/errors/skips. This is a historical baseline, not the final result of the R04 implementation. The [audit roadmap](AUDIT_ROADMAP_2026-10-25.md) keeps unfinished capabilities and mobile/operational limitations separate from test tooling. Older phased targets in [TEST_COVERAGE_ACTION_PLAN.md](TEST_COVERAGE_ACTION_PLAN.md) are historical proposals, not a current coverage certificate.
