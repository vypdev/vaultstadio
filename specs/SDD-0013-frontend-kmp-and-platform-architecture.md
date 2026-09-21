# Frontend KMP and Platform Architecture

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `frontend-kmp-and-platform-architecture`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define frontend domain/data/feature boundaries, navigation, state ownership, platform entrypoints, and user-facing capability mapping.
- Related issues/PRs: none recorded
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

The frontend is a standalone Kotlin Multiplatform project with domain, data, feature, app, and platform modules. This baseline replaces historical monolithic ViewModel examples with the current per-screen and feature-oriented structure.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

Frontend documentation mixes current per-screen ViewModels with legacy `AppViewModel` and deleted `compose-frontend` paths. Platform readiness and backend capability mapping are not consistently documented.

### 2.2 Current behavior

`frontend/settings.gradle.kts` defines domain/data/feature modules plus Compose, Android, and iOS app modules. Current source includes `VaultStadioRoot.kt` and feature-specific modules.

### 2.3 Evidence

- `frontend/settings.gradle.kts`
- `frontend/composeApp/src/main/kotlin/com/vaultstadio/app/VaultStadioRoot.kt`
- `frontend/domain`
- `frontend/data`
- `frontend/feature`
- `docs/architecture/FRONTEND_ARCHITECTURE.md`
- `docs/architecture/FRONTEND_MODULARISATION_AND_STANDALONE_BUILDS.md`
- `docs/frontend/FRONTEND_TESTING.md`

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

1. Document current module and state ownership.
2. Define platform entrypoint and navigation contracts.
3. Keep API/data adapters outside domain/features.
4. Synchronize frontend capability status with backend SDDs.

### 4.2 Non-goals

1. No global ViewModel owns unrelated features.
2. No frontend feature bypasses data/domain contracts.
3. Platform support is not claimed from module presence alone.

### 4.3 Fixed product and safety invariants

1. Domain modules are platform/framework independent where intended.
2. Feature state is owned by the relevant screen/feature.
3. Data adapters map backend contracts and auth failures consistently.
4. Navigation does not perform storage/auth mutation directly.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | Platform entrypoint starts app | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | root resolves navigation/session | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | navigation remains explicit. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: Platform entrypoint starts app -> root resolves navigation/session -> feature loads domain/data state -> user action invokes use case -> state renders loading/success/error -> navigation remains explicit..

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
| `booting` | Platform app starting | user | wait/error |
| `unauthenticated` | No valid session | user | login |
| `loading` | Feature request active | user | wait/cancel |
| `ready` | Feature state rendered | user | interact |
| `failed` | Bounded error state | user | retry/re-authenticate |

## 7. User-facing configuration

API base URL, platform environment, locale, feature enablement, and auth persistence are validated at platform/composition boundaries.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Frontend KMP and Platform Architecture invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

Frontend domain owns models/policies. Data owns network/storage adapters. Features own screen state and ViewModels. Compose/app modules own navigation and platform composition.

Feature modules must not import backend transport clients directly. State transitions and error mapping are testable without platform UI.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

Loading/error/partial states are visible and recoverable. A backend auth/storage failure is not rendered as an empty successful list.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Protect token persistence, deep links, local caches, clipboard/share flows, and platform-specific secrets.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Capture feature/action and bounded error codes; avoid file content, tokens, and private paths.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 30 cases: domain policy, data mapping, auth, feature state, navigation, loading/error/empty/partial, platform entrypoint, localization, accessibility, and representative desktop tests.**

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

1. Given a feature request failure, when the UI renders, then it shows a recoverable semantic error rather than an empty success state.
2. Given a new feature module, when architecture validation runs, then its dependencies follow the documented domain/data/feature direction.
3. Given the desktop test command, when run from `frontend/`, then it executes the intended project task.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

## 18. Implementation or maintenance sequence

Inventory current modules -> classify legacy docs -> publish baseline -> add dependency/state checks -> reconcile feature/API docs -> validate platform claims.

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

- `frontend/settings.gradle.kts`
- `frontend/composeApp/src/main/kotlin/com/vaultstadio/app/VaultStadioRoot.kt`
- `docs/architecture/FRONTEND_ARCHITECTURE.md`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.
