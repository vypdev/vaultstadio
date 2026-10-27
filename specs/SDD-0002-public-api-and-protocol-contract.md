# Public API and Protocol Contract

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `public-api-and-protocol-contract`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define the authoritative HTTP, OpenAPI, WebDAV, S3-compatible, and collaboration protocol surfaces.
- Related issues/PRs: #71, #91
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

### 2.5 Audit checkpoint — 2026-10-25

A05/A10/A11 reproducen fallos de respuesta pública, COPY WebDAV y paginación. La paridad entre rutas, DTOs, OpenAPI y URLs construidas por los clientes todavía necesita validación ejecutable.

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

### 6.4 Client bootstrap transport — issue #76

The owning frontend startup contract is BOOT-SER-01/02 and BOOT-HTTP-01 in [SDD-0013](SDD-0013-frontend-kmp-and-platform-architecture.md#64-client-startup-correction--issue-76). Client-side serializer descriptor identity MUST NOT change the public ISO-8601 JSON wire values. Serving a Wasm artifact MUST use one correct MIME header; missing assets retain their HTTP error status. Public API routes, authentication and versioning are not changed by this bootstrap repair.

### 6.5 Bounded storage selections — issue #91

This revision is an implementation contract, committed before code on the Action-linked branch
based on develop c9f1ac5. The wider API/search/storage portfolio remains partial.

| ID | Required behavior | Failure/retained state |
|---|---|---|
| PGN-01 | After ordinary authentication, reject invalid pagination/sort input before storage/metadata lookup, count, query, plugin checks, bytes or events. Direct storage listing/search/recent service calls must enforce the same semantic bounds before even a parent lookup. | Safe VALIDATION_ERROR/400 at HTTP; no storage selection/effect. Authentication reads remain part of normal authentication. |
| PGN-02 | Public REST limit is1..1000, offset0..2147483647. Missing values alone use endpoint defaults. Query integers are nonempty ASCII digits (leading zeros permitted), with no signs, whitespace, decimals or exponents, and must fit Int. Duplicate known selection keys are invalid. Do not clamp or reinterpret malformed values as defaults. | Safe field identity only; no raw request value or parse/SQL exception in the response. |
| PGN-03 | Folder listing defaults limit100/offset0/name/asc. It accepts name,size,created_at,updated_at,type and asc/desc, ASCII case-insensitively; unknown/blank/padded tokens fail. | Existing valid frontend enum spellings remain compatible. |
| PGN-04 | Basic search GET/POST and advanced POST default limit50/offset0. Recent, suggestions and by-metadata default limit20,10,50 respectively and have no offset/sort controls. Basic search GET has no sort controls. Reject unsupported known selection keys on these GET surfaces. Suggestions validate limit even for a short prefix; advanced input validates before plugin/filename selection. JSON integer type/null/overflow failures are bounded400. | Existing query/filter semantics are separate; invalid pagination never starts those operations. |
| PGN-05 | Valid requests preserve owner, exact folder parent, trash/starred/type predicates and typed repository/provider failures. Authenticated foreign parents remain403 and missing parents404 after input validation; no partial foreign metadata is disclosed. | Do not turn database failure into400 or empty success; cancellation remains control flow. |
| PGN-06 | SQL selection has a unique stable id ASC final tiebreaker. Primary direction remains requested; TYPE retains its existing name ASC secondary key. Repeated pages over an unchanged dataset have no overlaps/omissions caused by equal sort keys. | No snapshot-isolation promise across concurrent writes or separate HTTP requests. |
| PGN-07 | A page is floor(offset/limit)+1 (one-based even beyond the end); totalPages is ceil(total/limit), zero for an empty dataset; hasMore compares offset+returned size to total in Long. Compute without overflowing numerator/addition. Widen page/totalPages to Long through domain, server DTO and shared KMP DTO/domain models; do not saturate, wrap or silently clamp. | Existing ordinary JSON integers retain their values/field names. Clients must support the documented int64 metadata range for extreme valid pages. No schema/data migration. |
| PGN-08 | Keep raw protocol StorageItemQuery separate from validated REST selections. S3/WebDAV internal limits and zero-page conventions are not blanket-rejected by a REST constructor. Common PagedResult keeps the existing limit<=0 totalPages0/page1 convention for legacy/protocol callers, with Long results. | This does not certify S3/WebDAV request validation or all search/filter semantics. |

Domain owns a framework-free validated selection and its bounds/failure identity. API owns query
cardinality, lexical/token adaptation and JSON error mapping; core orchestration validates typed
selections and maps into the existing legacy semantic exception port without adding HTTP decisions.
Extract focused list/recent/search and request handlers instead of growing StorageRoutes,
SearchRoutes or a ViewModel. SQL owns deterministic ordering. No HTTP/SQL/DI imports in new policy.

Folder/basic-search totals describe the complete selected owned dataset before pagination. The
by-metadata endpoint remains a bounded authorized projection, not a global authorized count; its
returned projection uses one-based page1 and totalPages0/1 consistently. Advanced post-page filters,
filename matching, full-text fallback and global filtered counts remain the separate search work
in#82/SDD-0012; this input repair must not falsely certify those journeys or mask their failures.

Risk floor: **160 genuinely new backend cases plus8 shared KMP client cases**, excluding migrated
or expectation-only updates:40 pure policy/request parser,24 service/call-order/failure,72 actual
HTTP/production-DI/PostgreSQL,16 SQL stable-page/ownership and8 overflow/page arithmetic. Cover
all normative branches rather than filling a quota with getters. Retain the2981backend,
1316desktop and1192browser baseline; Long expectation adaptations keep original assertions.

Execute actual query/JSON boundaries including absence, empty, whitespace, signs, leading zeros,
Unicode digits, overflow, min/max/max+1, duplicates and case variants; verify invalid input causes
no selection even for a foreign parent/short prefix/plugin path. Positive SQL controls include
same names in different parents/owners, tied sizes/times/types, root/child, trash, empty/end/beyond
pages, totals and failures distinct from validation. Reproduce negative limit against the current
local package before the repair. Verify shared int64 serialization on JVM/Wasm and actual browser
sorting/recovery through user-reachable controls; label raw-HTTP versus GUI evidence honestly when
numeric text is not exposed by the UI. Preserve raw failures, exact source/commands, full coverage
denominators, original SDD drafts, current data and runnable rollback. Complete strict changed-source
lint, existing coverage gates, architecture/catalog/secret checks, full CI and Copilot review; do not
weaken them, add heavy workflow frequency, enable setup prematurely or promote master.

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
