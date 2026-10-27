# Real-Time Collaboration

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `real-time-collaboration`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define collaboration sessions, WebSocket lifecycle, presence, operations, OT, comments, reconnect, and participant limits.
- Related issues/PRs: #71, #89
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

### 2.5 Audit checkpoint — 2026-10-25

A01 confirma que una cuenta ajena puede unirse a una sesión del archivo privado. No se verificó convergencia OT ni reconexión de varios clientes reales; las suites unitarias no cierran estas aceptaciones.

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

| State | Trigger/condition | Owner | Allowed next states |
|---|---|---|---|---|
| `available` | Document can accept session | user | join |
| `joining` | Membership being authorized | system | active/rejected |
| `active` | Participant connected | participant | edit/leave |
| `degraded` | Connection or provider issue | participant | reconnect/resync |
| `closed` | Session ended/expired | system | new session |

### 6.4 Collaboration authorization repair — issue #89

Contract accepted before code on 2026-10-27 (`0e8535f`); locally verified implementation source `7bb140e`. R01c closes the observed cross-user admission and all equivalent REST/WebSocket/application bypasses. The repository has no implemented durable read/edit/comment collaboration grant source: download tokens, sharedWithUsers, visibility and previously unguarded membership are not grants. Current canonical file ownership is the authoritative decision for this increment; supported nonowner collaboration remains incomplete until a separately reviewed durable grant lifecycle exists. Do not simulate collaboration by denying the owner's valid flows.

| ID | Required behavior | Verification |
|---|---|---|
| C89-ACTOR | Every public application operation carries its explicit actor. Blank/anonymous actors stop before scope, metadata or effects. There is no nullable/system/actor-less fallback. Repository persistence primitives are internal capabilities, not authenticated user entrypoints. | Complete service matrix, blank actors and actual auth middleware. |
| C89-OWNER | Authorize against the current canonical item owner, then require an active FILE. PUBLIC/SHARED, download recipients, prior participant/comment authors and nonowner ADMIN do not override this. Missing items never authorize orphan state; foreign requests fail before type/trash/state disclosure. | PRIVATE/PUBLIC/SHARED owner/unrelated matrix; shared/admin HTTP controls; missing/trashed/folder variants. |
| C89-SCOPE | Session-ID requests first resolve minimal item/status facts; participant and comment references use minimal session/item scope ports. Full sessions/comments follow the canonical owner decision. Minimal facts must not be exposed or interpreted as a grant. | Real minimal-column SQL, call-order and typed scope failure controls. |
| C89-BIND | Bind returned item/session/comment/participant/document identities before disclosure or further effects. Comment route itemId is required and must match the actual comment; a socket's item/session is fixed by its authorized handshake. Unknown, mixed, stale or mismatched results fail safely. | Wrong parent/session/participant/item, mixed rows and malicious provider results. |
| C89-MEMBER | Cursor/selection/leave and socket join use a participant belonging to the actor and bound session. Comment deletion additionally preserves the existing author-only rule. Closing/expired sessions cannot accept admission or mutations; authorized leave may clean up its own expired/closed membership. | Participant/author mismatches, closed/expired controls and owner positives. |
| C89-ATOMIC | Admission and leave lock the canonical item then session in a consistent order, recheck current ownership/liveness and persist membership atomically. Last leave closes only an actually empty session under that lock; a concurrent join cannot be admitted into a closed session or be closed by a stale empty snapshot. Participant updates bind item/session/user/active row in the same boundary. | Migrated PostgreSQL two-order races, owner recheck and SQL/cancellation rollback. |
| C89-PRESENCE | Global presence is self-scoped: only the actor's status is writable/readable/subscribable without a durable collaboration grant. Validate every supplied active item/session pointer and their mutual binding before writes; validate returned identities/pointers before disclosure. An explicit self-offline operation clears only that actor. | Self/foreign/empty targets, pointer mismatches, poisoned rows and retained-state tests. |
| C89-STREAM | Authorize before returning a protected stream and again on collection and each emission. No forbidden subscription starts; changing ownership/membership before a coordinated delivery prevents that delivery. Flow errors remain typed and cancellation propagates. Existing empty provider streams are not certified as functioning real-time infrastructure. | Cold subscription, revoked emission and cancellation controls. |
| C89-WS | Authorize the session before registering a socket; validate participant binding before its join/updates, and reauthorize before message effects and recipient delivery. No denied/failed operation broadcasts success. Use typed JSON for quoted/untrusted fields and safe errors. Each connection has independent identity: another connection of the same owner receives updates, and an older disconnect cannot remove its replacement. | Actual Ktor sockets, denied handshakes/frames, two-owner-device delivery, escaping and disconnect races. |
| C89-ERROR | Preserve typed authorization/not-found/validation/provider outcomes; HTTP uses401/403/404/400/500 appropriately. No raw provider diagnostics, names, document contents or keys in denied errors. Cancellation is not converted to an ordinary result. | Actual SQL/provider faults, safe HTTP/socket errors and zero effects. |
| C89-STATE | Save means persisting an existing collaboration working-state snapshot, with an explicit typed not-found if absent. It must not acknowledge an absent state as saved. This increment does not claim cold initialization from uploaded file bytes, canonical-file commit, causal replay, durable streams or general OT convergence. Record those missing journeys as separate R14 work. | Real persisted snapshot, absent-state negative and explicit scope documentation. |
| C89-MAINT | Expired-session maintenance exposed by the application is actor-scoped and closes only currently owned expired sessions using a database-authoritative owner predicate. It cannot provide global cleanup/count access through an actor-less public method or an inferred administrator role. | Two owners, future/closed rows, blank actor, SQL failure and cancellation. |
| C89-ARCH | Pure model/port ownership resides in domain/collaboration; preserve compatible class/serializer names during module moves. Application orchestration, reads, comments, presence and membership decisions are focused helpers; SQL/HTTP/socket concerns remain adapters. Avoid enlarging the existing service/route/repository monoliths. | Source/module graph, no duplicate runtime model classes, original tests and complete affected suites. |

A denial preserves full item/session/participant/document/operation/comment/reply/presence/share rows, physical file hashes, counters and successful broadcasts. Only minimal scope reads needed for the decision are permitted before rejection. A forged historical membership cannot become a lasting grant after this repair.

The WebSocket delivery guarantee is checked at authorization/delivery boundaries, not a distributed atomic transaction across a database and a remote client. Explicit global self-presence remains account-level last-write state, not a claim of per-device presence ownership; disconnect does not implicitly clear another connection's/global presence. General durable cross-instance real-time delivery, OT/replay correctness, cold document initialization and canonical file commit remain R14 and require their own acceptance. Keep these limits visible even when the access matrix passes. [#112](https://github.com/vypdev/vaultstadio/issues/112) owns the concrete R14 follow-up, including the existing frontend/server route, authentication and message mismatch.

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


R01c derived minimum, enumerated before code (new matrix/contract executions, excluding the original regression cases):

| Group | Derivation | Minimum |
|---|---|---:|
| C89-SVC | 18 item/session/comment application entrypoints ×3visibilities ×owner/unrelated |108|
| C89-HTTP | 15 item/session/comment HTTP surfaces ×3visibilities ×4authenticated actors, plus15anonymous controls |195|
| C89-PRES | Self/foreign target sets, active-pointer binding, malformed returned rows, offline isolation and presence subscriptions |18|
| C89-WS |12actor/visibility handshakes; anonymous; foreign participant; other owned item injection; operation actor binding; provider failure/no broadcast; left participant; same-owner two-connection delivery; stale disconnect; revoked sender; revoked recipient; quoted JSON; malformed frame |24|
| C89-SQL | Three minimal scope projections ×positive/missing/SQL error/cancellation |12|
| C89-RACE | Membership lock order/race outcomes and owner-scoped expiry, rollback/cancellation |10|
| C89-FAULT | Missing/trashed/folder resources, mixed/stale identities, blank actors and typed provider errors beyond matrix overlap |24|
| Total floor |Risk-derived, not391independent product features |**391**|

The18 scoped service operations are joinSession, leaveSession, getSession, getParticipants, authorizeParticipant, updateCursor, updateSelection, getDocumentState, applyOperation, getOperationsSince, saveDocument, streamOperations, createComment, getComments, resolveComment, addReply, deleteComment and streamComments. Four presence operations and actor-scoped expiry maintenance have separate inventories because a different actor may operate on its own global presence/expired resources without accessing the fixture owner's data.

The15 HTTP surfaces are join/leave/session/participants/cursor/selection, document state/apply/list/save, and comment list/create/resolve/reply/delete. Actors are current owner, unrelated account, download-share recipient without collaboration grant and nonowner administrator. WebSocket message tests cover ping/join/cursor/selection/operation/presence plus the12explicit boundaries above; do not count one denial repeatedly as independent message semantics. Stable collected IDs use C89-SVC/HTTP/WS/PRES/SQL/RACE/FAULT prefixes and identify operation, visibility and actor where applicable.

Use actual production DI, routing, authentication adaptation, migrated PostgreSQL and isolated local bytes; only disposable token validation is a credential seam. Seed an existing working-state document explicitly where needed and label that fixture, rather than claim the missing cold-start journey worked. Retain original12service,35API DTO and11model regression cases during refactoring; do not add trivial getter tests to inflate the new budget. Run complete backend/desktop/browser CI, report lines/branches with real denominators and no new exclusions/baselines. Passing scoped authorization is not completion of this entire SDD.

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
| C89-ACTOR/OWNER/SCOPE/BIND/MEMBER/ATOMIC/PRESENCE/STREAM/WS/ERROR/STATE/MAINT/ARCH | R01c domain/application and adapters |442 new collected cases plus preserved regressions; [local evidence](../docs/development/verification/COLLABORATION_AUTHORIZATION_89.md), final-head CI/review/adoption pending |issue#89 and§6.4 |

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

### R01c local verification checkpoint — 2026-10-27

The complete backend gate passes2697 cases with zero failures/errors/skips;442 new cases fulfill the pre-code391 floor. The [receipt](../docs/development/verification/collaboration-authorization-89.json) retains denominators and collected IDs. Packaged loopback checks28 HTTP requests, nine native owner frames, foreign close1008, retained membership denial and unchanged prior bytes. Working-state seeding is explicit; bootstrap, canonical editing and the full client journey remain#112. Final-head remote CI/review/adoption remain pending.

### R01c snapshot-integrity addendum — before follow-up code

Persisting an existing working snapshot must not introduce lost edits: a state read before a concurrent document commit may not overwrite that newer state or decrease its version. A stale snapshot must return a typed conflict/client failure and preserve the later committed content. The persistence adapter must atomically compare the expected snapshot; its ordinary bootstrap/upsert port is not the save boundary. This does not certify general OT convergence or canonical byte saving.

Add five executions before implementing this follow-up: one real lock-wait save race, plus exact-current, changed-current, deleted-current and SQL-failure snapshot adapter cases. Preserve all442 existing new cases; the follow-up target is447 new cases. Keep current bytes and working rows intact on rejected saves. Record the actual red race and final outcomes separately.
