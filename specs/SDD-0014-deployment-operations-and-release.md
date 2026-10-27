# Deployment, Operations and Release

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `deployment-operations-and-release`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define Docker, Helm, configuration, health, migrations, backup, CI, release, versioning, rollback, and operator evidence.
- Related issues/PRs: #71
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

VaultStadio ships through Docker/Helm and GitHub workflows, but current operational documentation and workflow templates are inconsistent. This baseline records observed behavior and blocks unsupported release claims.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

CI uses JDK 17 while onboarding asks for JDK 21; release workflows reference nonexistent root `package.json`/`build`; security references a nonexistent Dockerfile; versions diverge.

### 2.2 Current behavior

Docker Compose renders successfully with a deprecated `version` warning. CI/Docker workflows trigger on master and are currently queued for the audit SHA. Backend/frontend use separate wrappers and workflows.

### 2.3 Evidence

- `.github/workflows/ci.yml`
- `.github/workflows/docker.yml`
- `.github/workflows/security.yml`
- `.github/workflows/release_workflow.yml`
- `.github/workflows/hotfix_workflow.yml`
- `docker/docker-compose.yml`
- `docker/Dockerfile.backend`
- `docker/Dockerfile.frontend`
- `helm/vaultstadio/Chart.yaml`
- `helm/vaultstadio/values.yaml`
- `docs/operations/DEPLOYMENT.md`
- `docs/operations/BACKUP_RESTORE.md`

### 2.4 Retrospective classification

- **Observed behavior:** the current behavior described above is supported by the listed checkout evidence.
- **Intentional contract:** only the invariants and acceptance scenarios in this SDD are proposed for preservation; historical motivation is not inferred.
- **Known debt and limitations:** documentation/source mismatches, incomplete external verification, and explicitly named unsupported operations remain debt until closed.
- **Unknown rationale:** historic reasons for the current structure are unknown unless a source reference states them.
- **Proposed improvements:** any future change is listed in the implementation sequence and must not be read as implemented behavior.

### 2.5 Audit checkpoint — 2026-10-25

Las imágenes de master son trazables al commit auditado y la recuperación de DB/contenido pasa. A08/A13/A14 muestran arranque S3/MINIO roto, readiness de DB sin respuesta en 40 segundos y referencias Helm a una DB/Service no generados. Render/lint y CI verde no prueban operación completa.

Evidencia parcial de producto en master `615f11adf5272bd1d4626f9571b31e691eeee062`: [auditoría integral](../docs/development/REPOSITORY_AUDIT_2026-10-25.md), [resultados saneados](../docs/development/audits/2026-10-25/verification-summary.json) y [roadmap](../docs/development/AUDIT_ROADMAP_2026-10-25.md), issue #71. Este checkpoint no modifica el estado declarado ni sustituye la verificación original de la baseline.

## 3. Actors, surfaces, and terminology

| Actor | Goal | Entry point | Visible surfaces |
|---|---|---|---|
| End user | Use the capability safely | REST/API or frontend | response, screen, error/recovery state |
| Administrator/operator | Configure, diagnose, recover | configuration, Docker/Helm, admin API | logs, health, reports, retained state |
| Contributor | Change the capability without boundary drift | backend/frontend source | tests, SDD, CI, docs |
| External client/provider | Interoperate through an explicit contract | protocol adapter | bounded request/response or failure |

**Capability:** the user-visible behavior owned by this SDD. **Semantic port:** an application-facing capability interface that does not expose provider DTOs or transport clients. **Composition root:** the explicit boundary where concrete adapters and credentials are wired. **Retained state:** facts that remain after a failure and determine safe recovery.

## 4. Goals, non-goals, and fixed invariants

### 4.1 Goals

1. Publish one toolchain and release source of truth.
2. Make workflow gates honest and fail-closed where required.
3. Define deployment configuration, health/readiness, migration, backup, and rollback.
4. Separate observed local evidence from external runner/deployment evidence.

### 4.2 Non-goals

1. Queued or canceled CI is not green evidence.
2. A syntactically valid Compose file is not deployment success.
3. No release workflow publishes artifacts from nonexistent project shape.

### 4.3 Fixed product and safety invariants

1. Secrets are injected, never committed.
2. Health and readiness distinguish process liveness from dependency readiness.
3. Release version is derived from one authoritative source.
4. Failed/partial deployment retains recovery state and never claims completion.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | Operator selects version/config | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | validates prerequisites | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | records rollback/recovery evidence. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: Operator selects version/config -> validates prerequisites -> builds artifact -> runs checks -> publishes image/chart -> deploys -> checks health/readiness -> records rollback/recovery evidence..

## 6. Functional behavior and state model

### 6.1 Happy path

1. The entrypoint validates the request and resolves the capability owner.
2. The application policy evaluates invariants and calls semantic ports in an explicit order.
3. Adapters perform bounded external I/O and map provider results to semantic facts.
4. The presentation surface reports completed facts, current state, next action, and safe evidence.

### 6.2 Alternative, duplicate, stale, and partial paths

- Duplicate requests MUST be idempotent or return an explicit duplicate outcome.
- Stale, out-of-order, canceled, and partially completed operations MUST retain enough state for a safe retry or a clear terminal action.
- A provider failure MUST NOT be converted into an empty success result.
- If an irreversible effect completed before a later step failed, the result MUST distinguish completed effects from pending recovery.

### 6.3 State machine

| State | Trigger/condition | Owner | Allowed next states |
|---|---|---|---|---|
| `planned` | Inputs and target selected | operator | validate |
| `validated` | Prerequisites/config accepted | operator | build |
| `built` | Artifact produced | operator | scan/publish |
| `deployed` | Workload applied | operator | health/rollback |
| `degraded` | Partial or unhealthy deployment | operator | reconcile/rollback |
| `released` | Remote artifacts and evidence verified | maintainer | maintain |

### 6.4 Project verification runtime repair — issue #79

The official managed-branch synchronization failed before `make test` because its isolated Linux job lacked Java. This is a project prerequisite repair through the existing project-owned setup extension; it does not change Copilot implementation, generated workflows, approval policy or branch ownership.

| ID | Required behavior | Acceptance target |
|---|---|---|
| VFY-JAVA-01 | Provision Java 17 in the same verification job; reject missing, unexecutable or incompatible Java before project checks. | Official setup action, executable/version checks and negative controls. |
| VFY-TOOLS-01 | Provision the current compile SDK through the official package `platforms;android-37.0`, build tools 36.0.0, and Chrome 155.0.8059.84 for frontend configuration/testing. Resolve tool paths explicitly, reject a missing/empty platform JAR or a browser that cannot start or reports a different version, and record actual runtime versions. A trailing whitespace terminator in the official CLI output is formatting, not a different version; unexpected non-whitespace output remains a rejection. | Real frontend verification plus missing, legacy-only SDK, unusable and incompatible browser controls. |
| VFY-DOCKER-01 | The runner MUST already provide a usable local Docker daemon for PostgreSQL integration tests. Setup MUST fail clearly when unavailable; it MUST NOT grant daemon access, change host permissions or start a remote daemon. | Local endpoint/availability controls and actual migrated-PostgreSQL tests. |
| VFY-ENV-01 | Additional SDK/browser/Docker configuration MUST reach the existing `make` commands through a trusted runtime directory accepted by the closed setup protocol. Use only private profile output directories; preserve arguments literally and avoid candidate-file changes or credential stores. | Executed wrapper tests, special-character arguments/paths, private output and failure controls. |
| VFY-FAIL-01 | Setup failures and nonzero verification results MUST propagate. The configured command list remains `make test,make test-coverage,make lint,git diff --check`; no skips or lowered coverage gates are introduced. | Negative setup/command tests and unchanged configuration receipt. Existing Makefile test-selection/fail-open debt remains separately tracked in R04. |
| VFY-TRUST-01 | Native setup MUST use the reviewed extension from immutable trusted main. Candidate changes MUST NOT activate or replace it. Activation remains disabled until the concrete setup is reviewed, merged to trusted main and separately authorized. | Native setup source receipt and real official sync after authorized activation. |
| VFY-LIFE-01 | Source work, protections and Action ownership of remote branch synchronization remain intact. Local static tests alone MUST NOT be reported as operational readiness. | Preservation/protection receipts, official sync result and explicit remaining limitations. |
| VFY-ACTION-01 | Reference external GitHub Actions through their official major version tags (`@v5`, `@v3`, `@v2`), never a commit SHA. Record resolved source identities separately as verification evidence. Keep the generated Copilot workflows unchanged. | Inventory of external `uses:` references and YAML/action validation. |
| VFY-NATIVE-01 | An ordinary same-repository Linux CI job MUST exercise the candidate composite and unchanged configured commands, plus complete backend/desktop/Wasm selections with visible failure status and retained fresh XML. This does not activate Copilot or certify its immutable trusted-main protocol. | Native Ubuntu execution, source/runtime provenance, individual command results and raw reports. |
| VFY-SPACE-01 | Before downloading the native verification toolchain, require at least 12GiB available on both workspace and temporary filesystems as an operational budget. Its private HOME belongs under runner.temp with runner-owned permissions; native post steps finish before runner job cleanup. Never delete shared caches or unrelated state to pass this gate. | Actual disk preflight and lifecycle evidence; interrupted local ENOSPC runs remain rejected. |
| VFY-DBTIME-01 | PostgreSQL integration fixtures MUST return the complete persisted row after seeding or updating it. Baselines for unchanged-state assertions MUST use the adapter's actual timestamp precision; retain comparison of every field. Exercise both metadata-only and byte-backed seeds with explicit nanosecond inputs, independently of host clock resolution. Do not change production clocks or discard timestamp assertions to obtain green. | Real migrated PostgreSQL regression cases plus the existing complete-row authorization/deletion assertions. |
| VFY-SANDBOX-01 | Linux browser verification MUST retain Chrome's sandbox. On Ubuntu runners, use the operator-installed root-owned setuid helper at `/opt/vaultstadio-verification-sandbox/chrome-sandbox` through `CHROME_DEVEL_SANDBOX`; reject missing, non-regular, writable or improperly owned/mode helpers. The composite MUST NOT change host permissions, AppArmor or sysctls. The ordinary ephemeral hosted CI may provision this operator prerequisite through a verified signed system package before executing candidate scripts. Download/extract as root in a private root-owned directory and install only those protected package bytes into a root-owned non-writable namespace; never promote a candidate-writable helper. Record package/helper identity and metadata. Forward the selected helper through the private command adapter and require a real headless DOM smoke before browser suites. | Signed package provisioning receipt, helper rejection controls, literal environment propagation, real sandboxed startup and complete Wasm results. |

The project-owned extension may provision public language/SDK/browser packages. A local Docker daemon and its existing access are operator prerequisites. SDK and generated command wrappers belong to the isolated profile; vendor-managed tool caches retain their normal lifecycle. No production endpoint, application credential or deployment is needed.

For VFY-SANDBOX-01, hosted operator provisioning MUST bootstrap Google's published active primary signing fingerprint `EB4C1BFD4F042F6DDDCCEC917721F63BD38B4796` through verified HTTPS and an isolated GPG home. Export only that primary key and its signing subkeys into a source-scoped `Signed-By` keyring; do not modify global trusted keyrings or permit unauthenticated apt downloads. The golden image's installed package does not establish an available authenticated download source. The project composite never invokes this privileged operator provisioning script.

### 6.5 Executable secret scanning — issue #87

The 2026-10-26 promotion check on `9f21f1e` exposed two scanner startup failures hidden by `continue-on-error`: Docker was missing for TruffleHog and the Gitleaks Action required an unconfigured organization license. A successful wrapper job did not establish a successful scan.

| ID | Required behavior | Verification |
|---|---|---|
| SEC-TOOLS-01 | Use upstream Gitleaks 8.30.1 and TruffleHog 3.99.7 CLI archives, selected by supported OS/architecture and checked against reviewed SHA256 release-asset digests before extraction/execution. Missing, altered, unsupported, unexecutable or incompatible tools fail visibly. No Docker, organization license, global installation or injected scanner credential is required. | Real version/startup checks; installer integrity, archive and platform controls. |
| SEC-SCOPE-01 | Scan all commits reachable from the exact checked-out HEAD for same-repository PRs and scheduled/manual verification (cadence in §6.6), including secrets deleted by later commits. Reject shallow/missing Git history or a source identity mismatch. Ignore unrelated refs. | Clean/detection/deleted-secret Git fixtures and real CI source receipt. |
| SEC-MERGE-01 | Inventory reachable Git blobs and merge parents. Both ordinary patch scanners omit merge-only additions; supplement both with private regular-file copies of blobs introduced in merges but absent from ordinary ancestor diffs. Later renames, mode changes or deletion MUST NOT hide the original content. Validate object IDs, parse NUL-separated paths as data, bound materialization to 64MiB/blob and 512MiB total, and fail rather than skip an exceeded budget. Never follow repository symlinks or execute candidate content. | Real merge-only/deleted/renamed fixtures, ordinary-parent exclusion, parser/size controls and both real scanners. |
| SEC-RESULT-01 | Both scanners execute independently and offline. Every unreviewed match rejects the job without contacting credential providers; scanner/configuration/parse/unexpected-exit failures also reject it. Reject Gitleaks internal errors or zero scanned commits even if its exit code is zero. A failed first scanner must not skip the second or become green. Historical non-secrets may be accepted only by reviewed tool/version/rule/immutable-commit/path/line coordinates and a concrete reason; expose their counts separately and reject the same match in a new commit. No global path/rule exclusion or undisclosed baseline is permitted. | Real detector controls, exact non-secret decisions, zero-commit/error and continuation cases. |
| SEC-PRIVACY-01 | Capture raw detector output only in private owned temporary state, never stream it to CI, upload it or include it in errors. Publish source/tool identities, outcome/counts and safe diagnostics without secret values, snippets or raw provider responses. Retire raw output on success and failure. | Generated synthetic markers are absent from public output/report; temporary cleanup controls. |
| SEC-BOUNDARY-01 | Keep orchestration in the project Security workflow and small installer/scanner adapters. Read-only Git/source access, credential-free subprocess environment, bounded downloads/execution and private temporary state preserve unrelated hosts, credentials and source. GitHub Actions use major tags. Copilot, its generated workflows, branch protection and activation remain unchanged. | Workflow/adapter controls, unchanged source, actual CI and preservation receipts. |

Both tools perform offline detection. TruffleHog explicitly disables provider verification and treats unverified matches as blocking, rather than the former verified-only filter. This avoids sending suspected credentials to external services and can expose historical synthetic examples requiring explicit triage. Do not claim a match is a live credential. A clean scan is not evidence of complete vulnerability analysis: the legacy dependency-listing job remains separate advisory debt.

Minimum acceptance inventory: the sixteen risk cases in issue #87, expanded into installer, scanner, privacy, real Git-history and workflow controls before publication. See the [scanner runbook](../docs/development/verification/SECRET_SCANNING_87.md) for exact scope, commands, source and limitations. The implementation and its CI remain pending until the corresponding evidence is retained; this contract does not authorize a merge or deployment.

### 6.6 CI cadence and complete PR verification — issue #106

Proposed contract (2026-10-26), pending final-head execution. The successful timing samples retained in the CI cadence runbook establish redundant work, not a speed guarantee. Execution duration and run-created-to-job-start latency are separate; the latter includes dependency waits.

| ID | Required behavior | Verification |
|---|---|---|
| CI-PR-01 | Every opened, reopened or synchronized same-repository PR targeting develop/master/main MUST execute complete root-selected backend `test`, frontend `desktopTest` and `wasmJsBrowserTest` suites. Do not use changed-path filters or selected-module subsets. Force the test tasks themselves to execute on each PR head; allow unchanged compilation and dependency reuse. A failed test or command MUST fail its job. Fork source MUST NOT execute on shared self-hosted runners. | Actual complete task inventory and fresh XML; a deliberately failing Gradle command stays nonzero. Fork checks remain explicitly skipped, never represented as executed verification. |
| CI-QUEUE-01 | Application CI and secret scanning run on PR updates and explicit manual requests, without duplicate develop/master branch-push launches. Scope cancellation by workflow and PR number/ref: superseded work for one PR may be cancelled, independent PRs cannot cancel each other. Copilot workflows, approvals, protection and Action-owned lifecycle remain unchanged. | Event/cancellation regression controls, parsed YAML and action validation, immutable Copilot source comparison. |
| CI-GATES-01 | Keep storage authorization, input accessibility and instant serializer coverage gates and strict authenticated/integrity-checked Codecov publication. Keep small accessibility/FQN/catalog/helper/MIME checks and both offline secret detectors on PRs. Preserve raw test XML and coverage artifacts with explicit missing-file failures. Do not restore test outputs as successful new execution. | Current coverage checks unchanged; safe source identities, raw reports and strict upload outcomes retained. |
| CI-HEAVY-01 | Full CodeQL/container security analysis, project runtime bootstrap verification, Kotlin lint and debug/distribution packaging move to staggered weekly schedules plus explicit manual dispatch. They MUST NOT run on every ordinary PR update or branch push. Keep their substantive verification; configured Make commands remain unchanged and their known gaps remain tracked separately. A periodic schedule uses the default branch; it does not certify develop unless explicitly selected manually. | Event matrix and documented cadence; deliberate dispatch selects the required branch/SHA. Scheduling does not by itself prove successful execution. |
| CI-DOCKER-01 | Docker validation runs explicitly on demand and on existing version-tag release events. Manual dispatch MUST build, load and smoke-test the candidate without registry login, image publication or release creation. Only a version-tag push may retain publication/release behavior. Do not create a tag or release as part of this change. Remove a redundant host backend build when the Dockerfile already compiles the source. All manual build/smoke failures remain visible. | Event/publication guards and existing Docker runtime/MIME contracts; actual manual candidate execution when requested. |
| CI-OBSERVE-01 | Retain a cadence table, old execution samples and job inventory. Compare actual new execution and start latency only after final-head CI; never count cancellation, skip, missing runtime or queue time as green verification or a measured execution speedup. | CI cadence runbook, final-head runs and explicit remaining limits. |

Gradle `--rerun` is a task-specific option; `--rerun-tasks` reruns the dependency graph. The PR workflow uses task selectors at the project root and task-specific rerun so all suites execute while unchanged compilation may be reused. Heavy native runtime verification retains its independent, uncached protocol. This does not repair suppressed local Make/run-tests entry points or certify the pending master promotion.

### 6.7 Runtime rollout and explicit activation — issue #85

The maintainer authorized configuring the required project dependencies and enabling `VERIFICATION_SETUP` when necessary on 2026-10-26. The current effective value remains false until trusted-source and operator prerequisites are verified. The official sync 40399399779 failed before publication because Java was absent; its diagnostic is not a passing project verification. The existing reviewed composite is on develop; trusted master still contains the intentionally failing starter.

| ID | Required behavior | Verification |
|---|---|---|
| OPS-SETUP-01 | Configure only the supported literal runner input of the two native comment callbacks when their current route lacks required Docker capability. Keep their events, guards, permissions, named Secrets, coordinator reference and all other Copilot behavior unchanged. Use the repository-owned composite for project runtimes. A dry run, package install, candidate CI or installer PR alone MUST NOT certify the immutable trusted-main setup protocol. | Official CLI preview, source hashes and actual native execution identities. |
| OPS-INVENTORY-01 | Before changing a persistent runner, obtain a bounded read-only inventory on the current and proposed verification routes. Report runtime identities, local Docker availability, Android/browser presence and protected sandbox metadata. Do not expose credentials, raw provider errors or environment dumps; never connect to a remote Docker endpoint or install/start anything during inventory. Missing prerequisites are reported as missing, not silently accepted as readiness. | Privacy/endpoint/timeout controls; actual runner/source report labelled inventory-only. |
| OPS-MANUAL-01 | Offer explicit optional native project verification or runner inventory on manual CI. Complete application suites retain their ordinary check identities and run on every CI invocation, including manual inventory; an information-only success MUST NOT replace the test producer. PR events always retain the complete application suites. Heavy native verification stays manual/weekly and may be reused by the manual CI entrypoint; no new per-push/PR heavy trigger is allowed. Dispatch inputs never expose provider Secrets to project setup. | Event/input/job-routing controls and deliberate final-ref dispatch. |
| OPS-TRUST-01 | The configured MAIN_BRANCH must contain the reviewed action and all its assets before activation. Keep source identity, package/signing trust and the protected Chrome sandbox. Required Docker access and privileged host prerequisites remain explicit; do not disable sandboxing, broaden credential scope or mistake a separate hosted job for setup of the self-hosted runner. | Trusted-main snapshot, actual official placement, sandbox and dependency checks. |
| OPS-ACTIVATE-01 | After prerequisites and the trusted snapshot are ready, the existing maintainer authorization permits setting the repository Variable to true. Then run an official admitted operation that actually needs verification; retain all configured command outcomes, native post-step/private-profile cleanup and ref publication. On failure retain state and correct the project-owned setup; never claim activation success from a no-op or missing-runtime failure. | Effective Variable receipt, real official sync/verification and unchanged protections/Copilot evidence. |

The Linux inventories at 649c13c and ebcf8cc found no Java/browser/Android/Docker CLI; the second also found no standard local Docker socket. The proposed native verification route is the existing Docker-capable macOS ARM64 group, already used for real application/PostgreSQL CI. Private setup HOME must not hide the operator-provided local Docker endpoint: resolve only current-user standard Unix sockets, probe with empty private client credentials, and export a literal local endpoint. Never read registry credentials, connect remotely, start a daemon or modify host permissions. Manual candidate verification may explicitly select this macOS route; weekly native Linux verification remains on hosted Ubuntu with its existing signed sandbox prerequisite. Both routes retain actual headless startup and complete independent suites. Record the selected runner's prerequisites before installing tools and then exercise endpoint discovery with the private HOME and no inherited endpoint.

Inventory is an information-gathering operation, explicitly separate from runtime installation and readiness. Complete product suites and the configured Make commands remain independently measured. The known local entrypoint/reporting gaps remain owned by issue #84; their suppression is not repaired or accepted by this rollout.

The macOS route reuses the existing project-owned, hash-verified official Headless Shell installer as a shared trusted-action asset. Install below the private profile without consulting or modifying the runner's shared browser cache. The ordinary CI entrypoint delegates to the same implementation; repeated installs must preserve the shared cache and retain separate executable outputs. Linux retains its existing Chrome setup and protected sandbox. Provenance and smoke checks use the exact setup-selected executable, including macOS paths with spaces, rather than assuming a `chrome` PATH alias.

Diagnostic-only inventory checks out the trusted default branch, whose resolved commit is recorded, rather than executing an inventory implementation from the dispatch candidate. Until that helper reaches trusted main, this diagnostic mode must fail instead of falling back to candidate code. Complete candidate verification remains an explicit execution of the operator-selected candidate and records its own pre-setup facts. During composite setup, cap the macOS browser transfer at 120 seconds plus 30 seconds for subprocess termination, within the existing five-minute total setup deadline; ordinary CI retains its separate download budget. Missing/slow prerequisites fail visibly and never cause the total deadline to be increased silently.

### 6.8 Complete verification and truthful evidence — issue #84

This pre-implementation contract is based on `816e6732b7fd605a2852745420ad7d2c75bf1474`. The existing PR CI already selects complete root suites; local Make/shell entrypoints still omit modules or suppress failures. Isolated controls of the unchanged frontend Make and `scripts/run-tests.sh` both return zero after a deliberate command exit 42 and hide stderr; the shell script additionally prints completion. These are entrypoint failure fixtures, not product test executions. The accepted previous CI baseline is 3,249 backend, 1,324 desktop and 1,200 browser case executions, with zero failures/errors/skips; no #84 result is claimed by that baseline.

| ID | Normative contract | Required evidence |
|---|---|---|
| R04-ROOT-01 | Keep independent project wrappers/settings and delegate contributor entrypoints to complete per-root verification. Do not add a shared Gradle root or production imports for CI. | Standalone executions and real Make/shell controls. |
| R04-DISC-01 | Derive current expected modules, test tasks, source sets and production class outputs from Gradle. Include all applicable backend JVM, frontend desktop and Wasm browser modules without allowlists. Identify unsupported destinations explicitly. | Live inventory plus disposable new-module controls in both roots. |
| R04-FAIL-01 | Retain original stdout/stderr and nonzero outcomes. Continue independent suites only while retaining final failure. Missing/incompatible runtimes, discovery, parsing, report and gate errors MUST NOT become success. | Real formerly omitted backend and asynchronous Wasm assertion failures; actual entrypoint propagation controls. |
| R04-FRESH-01 | Bind each receipt to source HEAD/tree/dirty identity, root, selected tasks, actual runtimes, commands, start/end and run-owned reports/execution data. Stale/retired outputs, skipped tasks, NO-SOURCE and cached/up-to-date results MUST NOT be counted as freshly executed cases. Required test sources with absent results fail closed. | Stale, missing, partial and source-change controls; actual test-only rerun and restored complete execution. |
| R04-COUNT-01 | Reconcile suite/case/module/root totals, retaining failures/errors/skips separately. Reject malformed or inconsistent counters and duplicate collection. NO-SOURCE is a task outcome, not a passed case. | Raw XML reconciliation, parameterized/duplicate/malformed/zero-result/skip controls. |
| R04-COV-01 | Canonical JVM root coverage MUST include all current compiled production classes/source sets against all matching run execution data, including code exercised from another module and modules with no tests. Do not add exclusions, lower thresholds or omit missed classes to improve percentages. | Cross-module and untested-module controls; class/source/exec inventory and raw class/root counters. |
| R04-COV-02 | Use unambiguous canonical backend/desktop report inputs for aggregate upload. Retain per-module/focused diagnostic and gate populations. Disclose denominator changes and compiler effects; keep Wasm executions and mobile readiness separate from JVM coverage. | Upload manifest, population comparison and counter reconciliation. |
| R04-GATE-01 | Preserve existing storage, form, helper and serializer contracts and strict signed/checksummed Codecov validation/upload failure behavior. Diagnostic upload after failure MUST NOT alter the failed outcome. | Deliberate gate failure and missing class/exec controls; uploader contracts. |
| R04-CLI-01 | Root/per-root Make and `scripts/run-tests.sh` MUST share complete suite behavior. Unknown flags and prerequisite failures must be actionable failures. Coverage entrypoints MUST establish or validate matching fresh execution before publishing results. | Actual entrypoint controls and complete real executions. |
| R04-LINT-01 | Configured lint MUST resolve actual tasks and retain their diagnostics/status. Disclose the existing advisory Detekt baseline and existing debt. New global lint/coverage thresholds require a separately reviewed decision. | Real task resolution/execution, failed lint propagation and strict changed-source verification. |
| R04-CI-01 | Preserve complete suites on every PR commit, per-PR cancellation and fork protections. Keep heavy security, Docker and full runtime verification at their reviewed lighter cadence; do not add duplicate heavy per-push work. | Cadence/event controls, failed producer/artifact controls and final-ref application CI. |
| R04-PRESERVE-01 | Negative controls use owned disposable state/reversible inputs. Restore every deliberate fault and verify source hashes before final green runs. Preserve drafts, local runtime/data, one runnable rollback, protections, master and setup activation. | Before/after source, Git/remote and protected artifact receipts. |
| R04-TOOL-01 | Measure the changed Python verification adapters during their existing hosted quality test execution, including real CLI subprocesses. Publish this separate Python population and a source/hash-bound report alongside the two canonical JVM populations. Missing, altered or mixed-source tooling evidence MUST fail aggregate selection. Preserve the current global targets, signed uploader and suite frequency; do not rerun tests solely to produce coverage. | Same-run Python coverage, complete file population, raw counter/hash/source consistency and input-failure controls. |

R04-TOOL-01 is specified before its CI implementation: the existing quality job instruments its existing verification tests once, publishes Cobertura plus a source-bound receipt, and the existing coverage job consumes that third, explicitly separate population. This does not claim JVM coverage for Python or quantitative line coverage for the Gradle init adapter. JaCoCo source-file counters consolidate lines shared by nested classes; root reconciliation must follow that model rather than adding class line counts.

Global coverage thresholds and a strict global legacy lint migration remain separate proposals. The #84 implementation must measure the full denominator honestly; passing tests do not establish 100% coverage. Copilot implementation/generated workflows, branch lifecycle ownership, protections, `VERIFICATION_SETUP=false` and master promotion remain outside this repair. Action versions remain major tags.

Local R04 checkpoint (2026-10-27): complete product execution and failure controls are recorded in [the verification report](../docs/development/verification/COMPLETE_VERIFICATION_84.md). This verifies the declared local scope only; final PR CI, Copilot review and adoption remain pending. JVM, Wasm and Python populations and mobile limitations remain separate.

## 7. User-facing configuration

JDK/Gradle versions, image tags, chart/app version, environment variables, health endpoints, migration policy, storage/secrets, and runner labels are explicit.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Deployment, Operations and Release invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

Workflow files are presentation/orchestration. Application/runtime code owns health/config semantics. Docker/Helm are deployment adapters. Release mutation is separate from build/test evidence.

Workflow validators check referenced files, triggers, fail-open exceptions, artifact paths, version source, and secret names. A release cannot rely on root Node artifacts absent from this Kotlin repository.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

Build, scan, push, deployment, and health failures remain distinct. `continue-on-error` is allowed only with documented advisory semantics and owner/removal criteria.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Protect registry credentials, JWT/S3/federation secrets, image provenance, Helm values, and logs. Security scans must not be suppressed by missing-file commands.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Job summaries identify SHA, version, artifact digest, environment, health/readiness, retained state, and rollback action without secrets.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 26 cases: workflow syntax, referenced paths, toolchain matrix, Compose/Helm render, secret safety, health/readiness, version consistency, artifact naming, migration, rollback, and fail-open policy.**

Issue #79 adds at least 12 independently collected runtime cases before activation. The concrete inventory is: (1) complete toolchain prepares an executable wrapper; (2) missing Java home; (3) missing Java executable; (4) incompatible Java major; (5) missing SDK platform; (6) missing Chrome; (7) unexecutable Chrome; (8) missing Docker CLI; (9) unavailable local daemon; (10) network Docker endpoint rejected; (11) missing runtime-output channel; (12) symlinked output root rejected; (13) arguments and tool paths containing spaces/quotes/shell metacharacters remain literal; (14) real command exit code/stderr propagate; (15) generated directories remain private and source files unchanged. Add actual backend/frontend commands under the prepared environment, plus (16) native trusted-main setup and official synchronization after authorized activation. Additional regression cases cover empty/legacy-only SDK platforms, incompatible/unstartable browsers and unchanged major-tag Action references. Unit fixtures do not substitute for the last two operational checks.

The budget is a floor, not a substitute for covering every normative requirement. Tests SHOULD include pure policy tests, application/use-case tests, adapter contract tests, route/workflow/schema tests, security/abuse tests, and integration or human evidence where automation cannot establish the contract. No live external service is required for deterministic unit tests; authorized external smoke evidence MUST be labelled separately.

Issue #84 adds a floor of **40 risk-derived controls**, not a cap or a claim of completion. R04-01–06 cover live backend/desktop/browser discovery, new backend/frontend modules and explicit extra-platform scope. R04-07–17 cover real omitted-module and asynchronous Wasm failures, independent-suite continuation, frontend/root Make and shell propagation, stderr, missing/incompatible Java/browser and unknown arguments. R04-18–28 cover stale/retired/missing XML, NO-SOURCE/skips, malformed/inconsistent counters, parameterized cases, duplicate collection, failed/partial runs, source/runtime/task binding and invalidated reuse. R04-29–38 cover cross-module execution, untested production classes, all main outputs, missing/stale coverage inputs, raw counter reconciliation, changed denominators, JVM/Wasm distinctions, deliberate focused gate failure, strict uploader behavior and actual/advisory lint distinctions. R04-39–40 cover unchanged CI cadence/fork/cancellation and final restored source-bound complete suites/CI with preservation and alignment. Record each collected control and command in the owning evidence; process fixtures do not substitute for actual Gradle/browser negative controls or product suites.

## 15. Documentation and discoverability

| Audience | Required artifact | Purpose |
|---|---|---|
| User/client | API or frontend guide | Normal journey, defaults, examples, visible states |
| Operator | configuration/recovery/runbook | prerequisites, health, failure, retry, cleanup |
| Contributor | this SDD and architecture guide | boundaries, contracts, tests, implementation order |
| Maintainer | catalog and traceability | ownership, verification commit, unresolved decisions |

The owning user-facing documentation MUST link back to this SDD or its catalog entry. Examples MUST be checked against current code or contract fixtures.

## 16. Acceptance scenarios

1. Given the current repository shape, when release workflow validation runs, then it does not reference root `package.json`, `build/`, or missing Dockerfiles.
2. Given an artifact is built but health fails, when deployment evidence is recorded, then the result is degraded/rollback-required, not released.
3. Given version metadata differs across Gradle, Helm, changelog, and workflow defaults, when validation runs, then it blocks until the source of truth is resolved.

For #84, acceptance additionally requires: (a) a new applicable module is discovered without a module-list edit; (b) a real failed backend or asynchronous browser assertion makes all owning entrypoints fail while retaining diagnostics; (c) stale/missing/retired reports cannot produce a green current receipt; (d) code covered through another module contributes to its owning classes and untested classes remain in the denominator; (e) existing focused gates and strict uploader failures remain effective; and (f) all deliberate faults are restored, complete fresh suites and CI pass, and preserved work/remote alignment are checked. Android/iOS/device and whole-platform readiness are not inferred from this tooling repair.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |
| R04-ROOT/DISC/FAIL/FRESH/COUNT/COV/GATE/CLI/LINT/CI/PRESERVE | Focused Gradle and contributor verification adapters | R04-01–40, real negative controls, complete root suites and final-ref CI | #84 verification evidence, contributor commands and SDD-0001/0013/0014 |

## 18. Implementation or maintenance sequence

Choose version source -> publish toolchain matrix -> repair workflow references/policies -> add render/path validators -> verify isolated build/deploy smoke -> document rollback.

## 19. Definition of Done

- [ ] Every normative requirement has acceptance and traceability.
- [ ] Current behavior, intentional contract, debt, unknowns, and proposals are separated.
- [ ] Architecture boundaries and configuration validation are executable.
- [ ] Numeric test budget and all applicable failure/security cases pass.
- [ ] User, operator, contributor, and migration documentation is synchronized.
- [ ] Catalog evidence points to the verified commit and all referenced paths exist.
- [ ] `python3 scripts/validate-specification-catalog.py` passes.
- [ ] `git diff --check` passes and no generated or secret-like artifact is staged.
- [ ] No readiness-blocking decision remains unresolved for the declared status.

## 20. References and decisions

- `.github/workflows/ci.yml`
- `.github/workflows/docker.yml`
- `docker/docker-compose.yml`
- `helm/vaultstadio/Chart.yaml`
- `docs/operations/DEPLOYMENT.md`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.
