# Storage Domain and Data Lifecycle

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `storage-domain-and-data-lifecycle`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define files, folders, metadata, ownership, quotas, trash, storage backends, and content lifecycle.
- Related issues/PRs: none recorded
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

Storage is the product core. This baseline separates metadata lifecycle from byte storage and records how local and S3-compatible backends are selected.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

Storage documentation uses inconsistent environment prefixes and mixes backend selection with protocol exposure. It does not provide one lifecycle contract for create, move, delete, trash, restore, quota, and backend failures.

### 2.2 Current behavior

The backend has domain/core/application storage modules, local and S3 backends, storage routes, metadata, quotas, trash/starred operations, and configuration-driven backend selection.

### 2.3 Evidence

- `backend/domain/storage`
- `backend/core/storage`
- `backend/application/storage`
- `backend/infrastructure/src/main/kotlin/com/vaultstadio/infrastructure/storage`
- `backend/api/src/main/kotlin/com/vaultstadio/api/config/AppConfig.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/config/Koin.kt`
- `docs/operations/STORAGE_CONFIGURATION.md`
- `docs/operations/BACKUP_RESTORE.md`

### 2.4 Retrospective classification

- **Observed behavior:** the current behavior described above is supported by the listed checkout evidence.
- **Intentional contract:** only the invariants and acceptance scenarios in this SDD are proposed for preservation; historical motivation is not inferred.
- **Known debt and limitations:** documentation/source mismatches, incomplete external verification, and explicitly named unsupported operations remain debt until closed.
- **Unknown rationale:** historic reasons for the current structure are unknown unless a source reference states them.
- **Proposed improvements:** any future change is listed in the implementation sequence and must not be read as implemented behavior.

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

1. Define authoritative metadata and byte ownership.
2. Make backend selection and S3 API exposure explicit.
3. Specify lifecycle invariants for ownership, paths, quotas, trash, and restore.
4. Preserve consistency across local and S3 implementations.

### 4.2 Non-goals

1. No storage backend is treated as interchangeable without contract tests.
2. No silent data loss on partial metadata/byte failure.
3. No claim of distributed consistency beyond verified behavior.

### 4.3 Fixed product and safety invariants

1. Every stored item has an owner and valid parent/path relationship.
2. Metadata and content operations have explicit failure/compensation behavior.
3. Quota and authorization are checked before mutation.
4. Backend-specific details remain behind storage ports.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | User authenticates | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | lists/creates/moves/uploads item | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | delete moves to trash or permanently removes according to policy. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: User authenticates -> lists/creates/moves/uploads item -> metadata and bytes are persisted -> event/response reports durable state -> delete moves to trash or permanently removes according to policy..

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

| State | Entered when | User-visible meaning | Allowed next states | Recovery/owner |
|---|---|---|---|---|
| `active` | Item is visible and usable | user | read/update/share |
| `trashed` | Item retained but hidden from normal listing | user | restore/delete |
| `restored` | Item returned to active hierarchy | user | use/share |
| `deleted` | Metadata and bytes removed per policy | operator | backup/recovery only |
| `degraded` | Metadata/content operation partially completed | operator | reconcile/retry |

## 7. User-facing configuration

Storage type, local path, S3 endpoint/region/bucket/credentials, path style, quota, and trash retention are validated with cross-field rules. Storage type controls backend and S3 route registration.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Storage Domain and Data Lifecycle invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

Storage domain owns item/value invariants. Application owns use cases and storage ports. Local/S3 adapters own byte operations. API maps requests/responses. Koin is composition only.

A storage port must not expose SDK clients or filesystem primitives to application policy. Local and S3 adapters share semantic contract tests.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

If metadata and bytes diverge, the operation reports retained state and recovery action; it never reports success solely because one side completed.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Path traversal, symlink escape, bucket abuse, ownership bypass, secret leakage, and quota exhaustion are boundary threats.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Record operation, item ID, backend kind, bytes, duration, and correlation; never paths containing secrets or access keys.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 28 cases: item invariants, ownership, path validation, quota, local backend, S3 backend, create/move/delete/trash/restore, metadata failure, byte failure, consistency, authorization, and cleanup.**

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

1. Given `STORAGE_TYPE=LOCAL`, when the application starts, then S3 routes are not registered.
2. Given a quota-exceeding upload, when the request is handled, then no partial item is reported as complete.
3. Given a trashed item, when it is restored, then ownership/path/quota rules are revalidated.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

## 18. Implementation or maintenance sequence

Freeze domain lifecycle -> reconcile configuration names -> write backend contract tests -> validate local/S3 parity -> update protocol and operation docs.

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

- `backend/core/storage/src/main/kotlin/com/vaultstadio/core/domain/service/StorageService.kt`
- `backend/infrastructure`
- `docs/operations/STORAGE_CONFIGURATION.md`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.
