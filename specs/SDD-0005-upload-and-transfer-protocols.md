# Upload and Transfer Protocols

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `upload-and-transfer-protocols`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define normal, chunked, folder, multipart, WebDAV, and S3-compatible transfer lifecycles.
- Related issues/PRs: #71
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

Transfer protocols are stateful capabilities layered over storage. This baseline gives each protocol explicit session, integrity, retry, cancellation, and cleanup behavior.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

Upload routes and protocol handlers exist, but their state transitions and unsupported operations are not consolidated into one contract.

### 2.2 Current behavior

The backend implements normal storage upload, chunked upload sessions, folder upload, multipart managers, WebDAV methods, and S3 routes gated by storage configuration.

### 2.3 Evidence

- `backend/application/chunkedupload`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/ChunkedUploadRoutes.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/FolderUploadRoutes.kt`
- `backend/core/common/src/main/kotlin/com/vaultstadio/core/domain/service/MultipartUploadManager.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/WebDAVRoutes.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/S3Routes.kt`

### 2.4 Retrospective classification

- **Observed behavior:** the current behavior described above is supported by the listed checkout evidence.
- **Intentional contract:** only the invariants and acceptance scenarios in this SDD are proposed for preservation; historical motivation is not inferred.
- **Known debt and limitations:** documentation/source mismatches, incomplete external verification, and explicitly named unsupported operations remain debt until closed.
- **Unknown rationale:** historic reasons for the current structure are unknown unless a source reference states them.
- **Proposed improvements:** any future change is listed in the implementation sequence and must not be read as implemented behavior.

### 2.5 Audit checkpoint — 2026-10-25

El upload normal y el ensamblado por chunks conservan los bytes. A12 falla la aceptación de completado idempotente; A10 muestra éxito de COPY sin el destino solicitado y A08 bloquea el arranque S3/MINIO.

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

1. Define transfer sessions and terminal states.
2. Guarantee incomplete/canceled sessions are cleaned or recoverable.
3. Specify checksum, chunk ordering, size limits, and idempotency.
4. Separate protocol adapters from storage policy.

### 4.2 Non-goals

1. AWS Signature V4 is not claimed implemented until verified.
2. Protocol handlers do not invent cross-protocol semantics.
3. No unbounded upload/session retention.

### 4.3 Fixed product and safety invariants

1. Chunk indexes, sizes, checksums, and session ownership are validated.
2. Completion is successful only after all required content and metadata effects succeed.
3. Cancel and expiry are idempotent.
4. Protocol errors do not leak credentials or raw storage failures.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | Client starts transfer | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | sends parts/method requests | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | client can retry, cancel, or inspect status. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: Client starts transfer -> sends parts/method requests -> server validates ownership and integrity -> server completes or retains explicit partial state -> client can retry, cancel, or inspect status..

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
| `initialized` | Session allocated | client | upload parts |
| `in-progress` | Some content received | client | continue/status/cancel |
| `complete` | All content committed | client | use item |
| `canceled` | Client/operator stopped session | client | terminal cleanup |
| `expired` | Retention deadline passed | operator | cleanup/retry from new session |
| `failed` | Validation/dependency failure | client/operator | retry or inspect |

### 6.4 Rendered upload/download integrity — issue #78

Pre-code contract (2026-10-27), TRANS78: a normal UI-selected zero-byte file is valid content,
not a failed read or a silently discarded queue item. Preserve valid names, media type, exact
bytes and chosen parent through production picker, queue, multipart upload, listing and download.
A nonempty file whose picker returned no bytes must not become a fabricated empty upload.
Failed upload remains a visible failed queue entry, is not announced as completion and can be
retried explicitly. Cancellation stops owned reads/network work and releases progress/resources;
no automatic retry may duplicate committed content. Chunked/folder protocol redesign is separate.

A download proceeds idle -> fetching -> choosing/saving -> handed-to-browser or saved, canceled,
or failed. One active operation owns its result; duplicate clicks cannot dispatch duplicate
transfers and cancellation/disposal cannot later invoke a save. A request/body failure never saves
an error document as user content. Browser initiation is not proof of a completed OS disk write;
the actual download file and its SHA256 are inspected in acceptance evidence. Native chooser
cancellation performs no write. Desktop writes use an owned temporary sibling and atomic publish,
with handles/temp files cleaned on success, cancellation and failure; existing content is retained
until the selected publication succeeds. Resource limits are the explicit32MiB client /24MiB
archive-input /32MiB archive-output /256 entries /16 levels in SDD-0002 section6.6.

At least60 new deterministic cases cover16 HTTP/transport,12 operation/state,12 archive,
12 platform-save/cleanup and8 upload boundaries. Add12 separately recorded actual callback and
rendered browser/native scenarios. The identified inventory is the
[pre-code transfer plan](../docs/development/verification/authenticated-transfers-78-plan.json).
Risk families can map to several tests; repeated JVM/Wasm executions are not distinct scenarios.
Retain red-phase evidence and original XML, then execute complete backend/desktop/Wasm suites,
canonical full-population coverage, existing gates/lint/contracts and Copilot review on the final
source. State mobile, native OS and large-file limits precisely; compilation is not runtime proof.

### 6.5 Real Web directory selection — issue #121

Pre-implementation contract (2026-10-28), FOLDER121, based on adopted develop
`95d2adb1fa2a01653ac0660e6add5f3245de3ab0`: Web **Upload folder** MUST open a real
directory-capable input and preserve the selected root plus every represented relative path.
An unsupported browser returns a visible capability result; it MUST NOT fall back to a flat
multiple-file selection. Ordinary single/multiple and large/chunked file selection retain
their existing entry shapes, byte semantics and explicit user-selected upload destination.

Before reading file bodies, validate the complete selection: exact basename/path agreement,
relative slash-separated paths, no empty/navigation/control/separator/drive-prefixed segments,
no duplicate paths or file/directory collisions. Preserve valid Unicode, casing and bytes;
do not trim or normalize user names into a different path. Fixed preparation bounds are
32MiB aggregate declared and actual bytes, 256 combined file/implicit-directory entries,
16 path segments and 255 UTF-16 code units per segment. Reject the whole invalid selection
before any body read or remote mutation. The separate ordinary/chunked thresholds are unchanged.

Read accepted file sources sequentially through a cancellable owned reader. A legitimate
zero-byte file is retained; unreadable, truncated or inconsistent content is a visible failure,
never fabricated empty content or a partially accepted selection. Cancel/dispose aborts owned
reads and prevents a late result from enqueuing work. Preparation keeps the original parent
snapshot. Existing upload failures retain truthful per-item state and never upload into the
wrong parent after folder creation fails. Recovery is explicit; no automatic duplicate upload.

The file-list directory API represents files and their ancestor paths, not empty directories.
The Web surface MUST explain that empty directories are omitted; an empty selection/cancel
performs no mutation. Empty files remain supported. This increment does not certify empty-
directory enumeration, unsupported mobile pickers, other OS hardware or all browser engines.

The pre-code [risk inventory](../docs/development/verification/folder-selection-121-plan.json)
defines at least60 deterministic risk cases and8 separately recorded real-package journeys.
Include path/budget boundaries, lifecycle and original picker regressions, queue parent/failure
contracts, actual Web hierarchy/list/download byte hashes, and complete backend/desktop/Wasm
suites with full-population coverage and existing gates. A DOM attribute or fake picker alone
does not establish acceptance. Preserve source-bound failures, current data/packages/rollback,
both detached SDD drafts and the published #78 history throughout this work.

### 6.5.1 Desktop preparation resource refinement — issue #121

Pre-code review refinement (2026-10-28): the desktop adapter MUST reject metadata/path/resource violations before reading file bodies, using the same32MiB aggregate policy and the application reader described above. Its selected-root-relative paths retain existing desktop semantics (requireRoot=false); Web still preserves the selected root (requireRoot=true). Native traversal is independently bounded to256 visited entries including the selected root/empty directories and16 relative path segments. Unsupported aliases/symlinks and unreadable or changing files fail the whole selection; ordinary/large-file picker thresholds remain unchanged. Each accepted native read is limited to snapshotted size plus1 byte and compared against that snapshot. See SDD0013 section6.8.3 and the12-family native resource matrix. This does not certify positive physical Swing selection or every native filesystem race.

## 7. User-facing configuration

Maximum file/chunk/session sizes, session TTL, allowed methods, WebDAV auth requirement, S3 route enablement, and checksum policy are bounded configuration.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Upload and Transfer Protocols invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

Protocol adapters parse HTTP/WebDAV/S3/stream inputs and call semantic transfer ports. Application owns session policy. Infrastructure owns storage streams and timers.

Each protocol has a contract test suite and a shared semantic transfer result; no route handler constructs storage backend clients.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

A failed completion retains an inspectable session state or performs documented cleanup. Automatic retries are bounded and never duplicate committed bytes.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Validate content length, path, method, auth, request body limits, XML/WebDAV input, S3 signatures when supported, and upload abuse.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Expose session ID, bytes, state, bounded reason, and correlation. Do not log content or credentials.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 30 cases: normal, chunk order, missing chunk, duplicate chunk, checksum, oversized input, cancel, expiry, retry, concurrent completion, folder rollback, WebDAV methods, S3 auth, and cleanup.**

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

1. Given an incomplete chunk session, when completion is requested, then it returns missing chunks without committing a complete item.
2. Given a duplicate completion request, when the item is already complete, then the operation is idempotent.
3. Given an expired session, when status is queried, then retained/removed state is explicit and no new content is accepted.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

Executed #78 evidence is recorded in [authenticated transfer acceptance](../docs/development/verification/AUTHENTICATED_TRANSFERS_78.md), the 72-family risk matrix and separate GUI receipts. Clean source `ae5f22a` passed 6,386 product executions including review/CI refinements and all 22 archive path-policy branches; real Web/macOS observations retain their actual client/server sources. Earlier 6,355 acceptance and independent failures remain historical evidence. Transfer bounds, Web directory selection (#121), Web origin (#116), mobile hardware and final-ref CI/review/adoption retain separate acceptance.


Executed #121 evidence is recorded in [Web directory selection verification](../docs/development/verification/FOLDER_SELECTION_121.md), its96-family risk matrix and separate GUI receipts. Source `d3816d6` passes6,606 complete product executions with zero failures/errors/skips and retains full44/52 module populations. Domain path/budget policy covers33/33lines and48/48branches; actual browser reads check every byte at the32MiB bound. All9 rendered journeys pass within their recorded scope, including separate physical Chrome/macOS directory selection, native Escape cancellation with no mutation, three-file confirmation/upload and a saved binary round trip. Initial browser-chooser API and native journeys retain their own mechanism and earlier sources. The action-specific error mapper covers38/38lines and12/12branches; a real four-byte unreadable-file selection on package4d3fb74 shows file-specific feedback and leaves all13 files/8 folders unchanged. The desktop-only follow-up moves Swing chooser ownership to EventQueue, retains filesystem reads on IO and passes9 controlled desktop cases plus real native login/application-cancel smoke without mutation. The subsequent native resource repair validates bounded metadata before reads, caps actual reads at snapshot+1 and rejects changing/unreadable/link sources atomically, with12 filesystem cases and48 focused controls plus the complete campaign. Native legacy relative paths and browser selected-root semantics remain distinct. Physical positive Swing selection is not certified. Final-ref CI, review and explicit adoption remain pending; no whole-platform readiness claim is made.

## 18. Implementation or maintenance sequence

Inventory transfer states -> define shared semantic contract -> test chunked/folder/multipart -> verify WebDAV -> resolve S3 signature status -> update API docs.

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

- `backend/application/chunkedupload`
- `backend/core/common/src/main/kotlin/com/vaultstadio/core/domain/service/MultipartUploadManager.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/WebDAVRoutes.kt`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.
