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
| VFY-SANDBOX-01 | Linux browser verification MUST retain Chrome's sandbox. On Ubuntu runners, use the operator-installed root-owned setuid helper at `/opt/google/chrome/chrome-sandbox` through `CHROME_DEVEL_SANDBOX`; reject missing, non-regular, writable or improperly owned/mode helpers. The composite MUST NOT change host permissions, AppArmor or sysctls. The ordinary ephemeral hosted CI may provision this operator prerequisite through a verified signed system package before executing candidate scripts; it MUST NOT promote a candidate-writable helper with chmod/chown. Record package/helper identity and metadata. Forward the selected helper through the private command adapter and require a real headless DOM smoke before browser suites. | Signed package provisioning receipt, helper rejection controls, literal environment propagation, real sandboxed startup and complete Wasm results. |

The project-owned extension may provision public language/SDK/browser packages. A local Docker daemon and its existing access are operator prerequisites. SDK and generated command wrappers belong to the isolated profile; vendor-managed tool caches retain their normal lifecycle. No production endpoint, application credential or deployment is needed.

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

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

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
