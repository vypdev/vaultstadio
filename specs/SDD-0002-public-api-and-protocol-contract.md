# Public API and Protocol Contract

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `public-api-and-protocol-contract`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define the authoritative HTTP, OpenAPI, WebDAV, S3-compatible, and collaboration protocol surfaces.
- Related issues/PRs: none recorded
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

VaultStadio exposes a broad protocol surface. This baseline inventories the implemented routes and records the unresolved choice between generated OpenAPI and an explicitly partial specification.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

The route tree, OpenAPI YAML, and API Markdown do not currently describe one provably complete contract. Swagger is implemented at `/swagger` while guides publish `/swagger-ui`.

### 2.2 Current behavior

Routing includes auth, storage, uploads, sharing, versions, sync, federation, collaboration, metadata/search, plugins, admin, AI, WebDAV, and S3 routes.

### 2.3 Evidence

- `backend/api/src/main/kotlin/com/vaultstadio/api/config/Routing.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/config/Swagger.kt`
- `backend/api/src/main/resources/openapi/documentation.yaml`
- `docs/api/API.md`
- `backend/api/src/test/kotlin/com/vaultstadio/api/routes`

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

1. Select one authoritative API contract strategy.
2. Make route availability and authentication explicit per protocol.
3. Standardize Swagger URL and error semantics.
4. Prevent docs and OpenAPI from silently drifting.

### 4.2 Non-goals

1. Do not promise endpoint completeness before route/spec coverage is measured.
2. Do not merge WebDAV or S3 semantics into generic REST documentation.
3. Do not change public paths without compatibility and migration evidence.

### 4.3 Fixed product and safety invariants

1. Every public endpoint has an owning capability SDD and test evidence.
2. OpenAPI status is explicit: generated/validated or partial.
3. Unsupported protocol operations return bounded protocol-appropriate errors.
4. Authentication and authorization are documented per surface.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | Client discovers contract | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | authenticates | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | retries only when contract marks operation retryable. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: Client discovers contract -> authenticates -> calls bounded protocol surface -> receives stable response/error -> retries only when contract marks operation retryable..

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
| `available` | Route is registered and documented | client | call |
| `unauthorized` | Credentials absent/invalid | client | authenticate |
| `forbidden` | Identity lacks permission | owner | request access |
| `invalid` | Request violates schema | client | repair request |
| `unavailable` | Dependency cannot serve request | operator/client | bounded retry |

## 7. User-facing configuration

Swagger path, API version prefix, protocol enablement, authentication mode, and route limits are explicit configuration contracts. Unknown or malformed values fail closed.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Public API and Protocol Contract invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

Presentation routes adapt transport DTOs. Application owns use cases and semantic errors. Domain owns invariants. Protocol adapters map WebDAV/S3/WS semantics without leaking transport details into domain policy.

The route inventory, OpenAPI path inventory, API Markdown, and tests are reconciled by a deterministic validator. The validator reports missing, extra, or ambiguous paths.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

A missing OpenAPI entry is not reported as complete. A provider/database failure preserves stable error code and does not expose raw exception text.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Validate path, method, auth scheme, content size, protocol-specific signatures, and untrusted headers at the boundary.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Expose request correlation, route/method, bounded status, and retryability without secrets, bodies, tokens, or raw provider failures.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 24 cases: route registration, auth, authorization, schema validation, error mapping, OpenAPI parity, Swagger path, REST, WebDAV, S3, WebSocket, pagination, limits, malformed input, and replay/idempotency where applicable.**

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

1. Given a documented endpoint, when the route/spec validator runs, then its implementation, OpenAPI entry, test, and owning SDD are traceable.
2. Given `/swagger` is the implementation path, when Quick Start is rendered, then it does not advertise `/swagger-ui`.
3. Given an unsupported S3 Signature V4 operation, when invoked, then the response states unsupported behavior without claiming success.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

## 18. Implementation or maintenance sequence

Inventory routes -> decide source of truth -> reconcile Swagger/OpenAPI -> add parity validator -> update API docs -> add protocol-specific contract tests.

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

- `backend/api/src/main/kotlin/com/vaultstadio/api/config/Swagger.kt`
- `backend/api/src/main/resources/openapi/documentation.yaml`
- `docs/api/API.md`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.
