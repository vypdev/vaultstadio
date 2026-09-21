# Real-Time Collaboration

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `real-time-collaboration`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define collaboration sessions, WebSocket lifecycle, presence, operations, OT, comments, reconnect, and participant limits.
- Related issues/PRs: none recorded
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

Collaboration coordinates concurrent document edits and presence over a WebSocket boundary. Correctness depends on ordering, identity, reconnect, and operation transformation.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

Collaboration has a substantial implementation surface and RepoWise hotspots, but its state and protocol invariants are not captured in one authoritative SDD.

### 2.2 Current behavior

Backend contains collaboration domain/core modules, REST session/document/presence routes, a WebSocket route, OT and service classes, and frontend collaboration features.

### 2.3 Evidence

- `backend/domain/collaboration`
- `backend/core/collaboration`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/collaboration/CollaborationRoutes.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/collaboration/CollaborationWebSocket.kt`
- `backend/core/collaboration/src/main/kotlin/com/vaultstadio/core/domain/service/CollaborationService.kt`
- `backend/core/collaboration/src/main/kotlin/com/vaultstadio/core/domain/service/CollaborationOT.kt`
- `frontend/feature/collaboration`

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

1. Define session and participant ownership.
2. Make operation ordering and transformation deterministic.
3. Specify reconnect, duplicate, stale, and disconnect behavior.
4. Bound resource use and user-visible presence.

### 4.2 Non-goals

1. No WebSocket message is trusted without session/participant validation.
2. No OT refactor is justified by complexity score alone.
3. No presence state is treated as durable document content.

### 4.3 Fixed product and safety invariants

1. Only authorized participants can join or mutate.
2. Operations have identity, ordering, and bounded payload.
3. Duplicate operations are idempotent or rejected explicitly.
4. Session timeout and disconnect cleanup are deterministic.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | User opens document | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | joins session | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | disconnect/reconnect resumes or requires resync. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: User opens document -> joins session -> receives snapshot/presence -> sends operation -> server validates/transforms/broadcasts -> clients converge -> disconnect/reconnect resumes or requires resync..

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
| `available` | Document can accept session | user | join |
| `joining` | Membership being authorized | system | active/rejected |
| `active` | Participant connected | participant | edit/leave |
| `degraded` | Connection or provider issue | participant | reconnect/resync |
| `closed` | Session ended/expired | system | new session |

## 7. User-facing configuration

Session timeout, maximum participants, operation/body limits, reconnect/resync policy, and collaboration enablement are bounded.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Real-Time Collaboration invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

Pure OT/domain policies are framework-free. Application owns session/use-case ordering. WebSocket/REST adapters own transport. Presence and comments are separate semantic capabilities from document mutation.

WebSocket message schemas, operation IDs, sequence/ack, and close reasons are versioned and validated.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

A failed broadcast does not claim all clients converged. A stale client receives resync/closure semantics rather than silent data loss.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Protect session join, cross-document access, message injection, resource exhaustion, and presence privacy.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Record session ID, participant count, operation IDs, sequence, close reason, and bounded latency; never document content.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 34 cases: join/auth, lifecycle, OT transforms, ordering, duplicate, malformed message, disconnect, reconnect, resync, limits, concurrent participants, presence, comments, and security.**

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

1. Given two concurrent compatible operations, when both reach the server, then all active clients converge on the same transformed document state.
2. Given a stale sequence, when an operation arrives, then the server rejects or resyncs it without applying ambiguous content.
3. Given disconnect, when reconnect occurs, then the client either resumes from a verified cursor or receives a full resync.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

## 18. Implementation or maintenance sequence

Freeze message/schema inventory -> test OT/state semantics -> test WebSocket lifecycle -> add bounded integration scenario -> update frontend/API docs.

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

- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/collaboration/CollaborationWebSocket.kt`
- `backend/core/collaboration/src/main/kotlin/com/vaultstadio/core/domain/service/CollaborationOT.kt`
- `frontend/feature/collaboration`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.
