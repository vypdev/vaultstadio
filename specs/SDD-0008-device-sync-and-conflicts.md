# Device Sync and Conflict Resolution

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `device-sync-and-conflicts`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define device registration, delta synchronization, conflict detection, replay, deactivation, and recovery.
- Related issues/PRs: #71, #124
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

Sync is a stateful protocol, not merely a set of HTTP endpoints. This baseline defines device identity, change ordering, conflict visibility, and idempotent replay.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

Sync models, routes, frontend modules, and docs exist but do not expose one authoritative state/conflict protocol.

### 2.2 Current behavior

Backend contains Sync domain/core/application modules and `/api/v1/sync` routes for devices, conflicts, delta, pull, and push.

### 2.3 Evidence

- `backend/domain/sync`
- `backend/core/src/main/kotlin/com/vaultstadio/core/domain/model/Sync.kt`
- `backend/core/sync`
- `backend/application/sync`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/sync/SyncRoutes.kt`
- `backend/api/src/test/kotlin/com/vaultstadio/api/routes/SyncRoutesTest.kt`
- `frontend/domain/sync`
- `frontend/data/sync`
- `frontend/feature/sync`

### 2.4 Retrospective classification

- **Observed behavior:** the current behavior described above is supported by the listed checkout evidence.
- **Intentional contract:** only the invariants and acceptance scenarios in this SDD are proposed for preservation; historical motivation is not inferred.
- **Known debt and limitations:** documentation/source mismatches, incomplete external verification, and explicitly named unsupported operations remain debt until closed.
- **Unknown rationale:** historic reasons for the current structure are unknown unless a source reference states them.
- **Proposed improvements:** any future change is listed in the implementation sequence and must not be read as implemented behavior.

### 2.5 Audit checkpoint — 2026-10-25

Pull con dispositivo válido funciona. A06 reproduce 500 después de persistir, duplicación por replay y cambios de un dispositivo desactivado; A07 observa que un nuevo upload no aparece automáticamente en sync. Las aceptaciones de replay y desactivación no se cumplen.

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

1. Define device and change identity.
2. Make ordering, conflict, replay, and deactivation deterministic.
3. Bound sync payloads and retries.
4. Align backend and frontend state models.

### 4.2 Non-goals

1. No conflict is silently discarded.
2. No duplicate replay creates duplicate mutation.
3. No sync contract assumes a single device or single instance.

### 4.3 Fixed product and safety invariants

1. A device has a stable owner-bound identity.
2. Changes are ordered or explicitly conflict-marked.
3. Push/pull operations are idempotent under replay.
4. A deactivated device cannot mutate until re-authorized.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | Device registers | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | pulls known changes | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | state converges. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: Device registers -> pulls known changes -> pushes local changes -> server validates sequence -> applies or records conflict -> client resolves/retries -> state converges..

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
| `unregistered` | Device unknown | client | register |
| `active` | Device may sync | client | pull/push/deactivate |
| `conflicted` | Change cannot apply automatically | user/client | resolve |
| `deactivated` | Device blocked | owner | reactivate |
| `converged` | Client and server share known state | client | continue |

### 6.4 Accepted product discovery — issue #124 (2026-10-29)

These are **accepted product targets, proposed for implementation**. They do not change the observed baseline, catalog status or last-verified evidence. The [discovery index](discovery/2026-10-29/README.md) retains the source answers and cross-capability dependencies; this SDD owns the requirements below.

<a id="pd-009"></a>

#### PD-009: Bidirectional on-demand synchronization and offline pinning

Clients MUST synchronize bidirectionally, download file content when used and let users pin files or folders for offline availability. Metadata visibility is not proof that a body is present locally; pending offline edits and their synchronization status must remain visible.

**Blocking technical decisions:** Placeholders, background execution by platform, cache/key protection, subtree pinning, tombstones, disk-full behavior, replay and permission revocation while offline.

<a id="pd-010"></a>

#### PD-010: Three configurable conflict strategies

The conflict policy MUST offer preserve-both/manual resolution; safe automatic merge for supported compatible formats with preserve-both/manual fallback; and latest-received as current while retaining the other version in history. Safe automatic merge with that fallback is the default; unsupported, binary, unsafe or E2EE-server merges cannot silently discard either side.

**Blocking technical decisions:** Compatible formats, merge safety/detection, receipt ordering, history admission under quota, user resolution UX and client-side E2EE merge boundaries.

<a id="pd-011"></a>

#### PD-011: Conflict policy inheritance

Administrators MUST manage a general conflict default with explicit space/vault overrides. An applicable explicit override takes precedence over the general default; an absent override inherits it.

**Blocking technical decisions:** Policy revision/snapshot behavior for in-flight/offline edits, auditing, validation and migration of retired strategies.

## 7. User-facing configuration

Device limits, delta block size, conflict retention, sync enablement, and payload bounds are explicit. Defaults are safe and bounded.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

### Accepted target configuration (#124)

| Requirement | Authority, default and inheritance |
|---|---|
| PD-009 | On-demand bodies by default; explicit file/folder offline pins. Device cache budgets and unpin/eviction policy remain undecided. |
| PD-010 | Administrators choose the strategy under PD-011. Latest received means server receipt order, not untrusted device wall-clock order; exact concurrency ordering is still a design decision. |
| PD-011 | Installation default is PD-010 safe compatible merge with manual fallback; administrator-managed space/vault exception wins. A device preference cannot override the effective administrative policy. |

These are product-policy semantics, not new environment variable/API field names. Before implementation, define typed validation/bounds, persistence, effective policy revision for pending work and migration/rollback. An override cannot bypass content authorization, E2EE boundaries or protected retention. Unknown/invalid policy is not permission to select a weaker default.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Device Sync and Conflict Resolution invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

Domain owns change/conflict invariants. Application owns sync use cases. Persistence and transport are ports/adapters. Frontend data maps protocol DTOs into domain state and features render it.

Sync cursor/sequence/conflict schemas are versioned. Unknown schema or stale cursor fails closed or returns explicit resync requirement.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

Partial push reports applied and conflicted changes separately. Provider/database failure never becomes an empty successful delta.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Device credentials, replay, cross-user changes, malicious deltas, and oversized payloads are protected.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Record device ID, cursor, counts, conflict IDs, and correlation; never file content or credentials.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 30 cases: registration, device ownership, pull/push, cursor, ordering, duplicate replay, conflict types, partial batch, resync, deactivation, limits, auth, and frontend rendering.**

The budget is a floor, not a substitute for covering every normative requirement. Tests SHOULD include pure policy tests, application/use-case tests, adapter contract tests, route/workflow/schema tests, security/abuse tests, and integration or human evidence where automation cannot establish the contract. No live external service is required for deterministic unit tests; authorized external smoke evidence MUST be labelled separately.

For #124, add at least **9 future acceptance families** (3 decisions × positive, denied/invalid and failure/recovery). The concrete families are listed in §16; they are **planned, not executed tests**. Expand actor, policy, boundary, concurrency, platform and cryptographic matrices before the affected implementation is marked ready. Preserve existing regression budgets.

## 15. Documentation and discoverability

| Audience | Required artifact | Purpose |
|---|---|---|
| User/client | API or frontend guide | Normal journey, defaults, examples, visible states |
| Operator | configuration/recovery/runbook | prerequisites, health, failure, retry, cleanup |
| Contributor | this SDD and architecture guide | boundaries, contracts, tests, implementation order |
| Maintainer | catalog and traceability | ownership, verification commit, unresolved decisions |

The owning user-facing documentation MUST link back to this SDD or its catalog entry. Examples MUST be checked against current code or contract fixtures.

## 16. Acceptance scenarios

1. Given a replayed push, when the server has already applied it, then the response is idempotent and does not duplicate the mutation.
2. Given an out-of-order change, when automatic application is unsafe, then the server records an explicit conflict.
3. Given a deactivated device, when it pushes, then no mutation occurs.

### Planned discovery acceptance (#124)

- **PD-009-P** — Two real clients synchronize edits both ways; opening an unpinned file downloads its bytes and a completed folder pin remains usable offline.
- **PD-009-N** — An unavailable body cannot be shown as offline-ready; deactivated devices and revoked permissions cannot perform new server effects.
- **PD-009-F** — Interrupt download/upload then reconnect: retain local edits, resume or retry without duplicate mutations, and distinguish partial pins from completed ones.

- **PD-010-P** — Exercise the same two-client divergence under all three strategies; verify default compatible merge, incompatible fallback and retained loser history for latest-received.
- **PD-010-N** — Binary/incompatible or unavailable-key input does not trigger a claimed safe merge or silent overwrite.
- **PD-010-F** — An interrupted merge or failed history write retains both source revisions and exposes a retryable/unresolved conflict instead of acknowledging destructive success.

- **PD-011-P** — An inheriting space follows the general strategy and an explicitly overridden vault follows its own strategy on the same conflict inputs.
- **PD-011-N** — A non-administrator or device-supplied conflict strategy cannot weaken the effective policy.
- **PD-011-F** — A concurrent policy change/reconnect yields one recorded effective policy revision; retain conflict inputs if a valid policy cannot be resolved.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

### Discovery traceability (#124)

| Requirement owner in §6 | Planned acceptance in §16 | Implementation/evidence state | Milestone |
|---|---|---|---|
| [PD-009](#pd-009) | PD-009-P / -N / -F | Accepted target; implementation and current execution evidence still required | [M3](../docs/development/PRODUCT_ROADMAP_2026-10-29.md#m3) |
| [PD-010](#pd-010) | PD-010-P / -N / -F | Accepted target; implementation and current execution evidence still required | [M3](../docs/development/PRODUCT_ROADMAP_2026-10-29.md#m3) |
| [PD-011](#pd-011) | PD-011-P / -N / -F | Accepted target; implementation and current execution evidence still required | [M3](../docs/development/PRODUCT_ROADMAP_2026-10-29.md#m3) |

## 18. Implementation or maintenance sequence

Define protocol identity -> document state machine -> add replay/conflict tests -> align frontend models -> verify operational recovery.

Discovery targets follow the dependency order and evidence gates in the [2026-10-29 roadmap](../docs/development/PRODUCT_ROADMAP_2026-10-29.md). Each implementation requires its own admitted, bounded issue/PR; this documentation revision does not activate or certify these features.

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

- `backend/core/src/main/kotlin/com/vaultstadio/core/domain/model/Sync.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/sync/SyncRoutes.kt`
- `frontend/feature/sync`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.

Discovery provenance and ownership: [30 accepted decisions](discovery/2026-10-29/README.md), [verbatim answers](discovery/2026-10-29/answers.json), [issue #124](https://github.com/vypdev/vaultstadio/issues/124).

## Supported publication-format supplement — #145, 2026-10-30

This append-only supplement renders the existing §5 journey and §9 presentation
contract. It preserves the preceding historical text, readiness, open decisions
and verification receipts. The examples below are contract examples, not newly
implemented screens or executed runtime acceptance.

```mermaid
flowchart LR
    S0["Device registers"]
    S1["pulls known changes"]
    S2["pushes local changes"]
    S3["server validates sequence"]
    S4["applies or records conflict"]
    S5["client resolves/retries"]
    S6["state converges"]
    S0 --> S1
    S1 --> S2
    S2 --> S3
    S3 --> S4
    S4 --> S5
    S5 --> S6
```

Text equivalent: Device registers -> pulls known changes -> pushes local changes -> server validates sequence -> applies or records conflict -> client resolves/retries -> state converges..

Representative state content (apply the evidence limitations in §2 and §17):

- Pending: Synchronizing the registered device from its known cursor.
- Action required: Resolve the reported conflict or follow the explicit resync path.
- Blocked: A deactivated or foreign device cannot apply changes.
- Partial: The acknowledged subset is retained; remaining changes are still pending.
- Complete: The declared device/server revision has converged without omitted pending work.

The following allocation restates the original §14 base floor. Later scoped
matrices, discovery families, concurrency controls and higher budgets in the
preceding contract remain additional requirements; this table neither replaces
them nor claims that these cases ran during the format migration.

| Existing base risk area | Minimum distinct cases |
|---|---:|
| Device authority | 6 |
| Cursor/order/replay | 8 |
| Conflict/resync | 8 |
| Failure/limits/rendering | 8 |
| **Total** | **30** |

Acceptance/traceability remains in §16–17. Actual implementation must reconcile
those requirements and all later scoped matrices before claiming completeness.
