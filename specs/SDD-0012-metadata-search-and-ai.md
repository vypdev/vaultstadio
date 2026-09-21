# Metadata, Search and AI

- Status: Proposed
- Date: 2026-09-21
- Catalog capability ID: `metadata-search-and-ai`
- Last verified: not applicable
- Owners: VaultStadio maintainers
- Scope: Define metadata extraction, indexing/search, AI-assisted classification, provider boundaries, privacy, and readiness evidence.
- Related issues/PRs: none recorded
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

Metadata, search, and AI are related but not identical capabilities. This SDD defines their shared lifecycle while requiring separate contracts for deterministic indexing and external AI providers.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

Routes, plugins, configuration, and docs indicate metadata, search, and AI support, but provider readiness, privacy, indexing consistency, and failure behavior are not consolidated.

### 2.2 Current behavior

The repository has metadata and search routes, application metadata use cases, full-text and AI classification plugins, and AI routes/configuration.

### 2.3 Evidence

- `backend/domain/metadata`
- `backend/application/metadata`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/metadata/MetadataRoutes.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/metadata/SearchRoutes.kt`
- `backend/plugins/fulltext-search`
- `backend/plugins/ai-classification`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/ai/AIRoutes.kt`
- `docs/plugins/AI_INTEGRATION.md`

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

1. Separate deterministic metadata/search from provider-dependent AI.
2. Define indexing freshness and reindex behavior.
3. Define AI credential, prompt, output, privacy, and failure boundaries.
4. Classify provider readiness honestly.

### 4.2 Non-goals

1. A mocked adapter is not end-to-end provider evidence.
2. AI is never an implicit fallback for deterministic search.
3. User content is not sent externally without explicit policy/consent.

### 4.3 Fixed product and safety invariants

1. Metadata ownership and provenance are explicit.
2. Search never returns unauthorized items.
3. Index failure does not silently delete source metadata.
4. Selected AI provider failure is terminal for that operation and never silently falls back.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | File changes | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | metadata is extracted/indexed | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | reindex/retry is explicit. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: File changes -> metadata is extracted/indexed -> user searches or requests AI action -> policy checks authorization/privacy/provider readiness -> result or bounded partial state is returned -> reindex/retry is explicit..

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
| `unindexed` | Source exists without current derived metadata | system | index |
| `indexed` | Derived metadata/search entry current | user | search/use |
| `stale` | Source changed after index | system | reindex |
| `processing` | AI/index operation active | user/operator | complete/fail/cancel |
| `failed` | Derived operation failed | operator/user | retry/inspect |

## 7. User-facing configuration

Index path, extraction enablement, AI provider/model, credentials, content limits, retention, and external-send policy are explicit. Missing selected-provider credentials fail closed.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Metadata, Search and AI invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

Metadata/search/AI policies are application-level. Provider SDK/HTTP/process details stay in adapters. Plugin registration is composition. Privacy and prompt policies are pure and testable.

Provider readiness has separate modeled, unit-tested, provisioned, smoke-tested, and end-to-end states. Search results carry authorization-filtered provenance.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

Partial indexing reports stale/failed items without false completeness. AI provider errors are sanitized and do not trigger another provider implicitly.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Protect file content, prompts, metadata, API keys, model outputs, prompt injection, data retention, and result authorization.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Record operation, provider class (not secret), item count, status, duration, and correlation; never content, prompt, key, or raw provider response.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 30 cases: metadata extraction, indexing, stale/reindex, authorization, search ranking contract, AI config, missing credential, provider error, prompt injection, output schema, privacy, cancellation, and partial results.**

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

1. Given an unauthorized file, when search executes, then it is absent regardless of index contents.
2. Given a configured AI provider without credentials, when the operation starts, then it fails closed before external execution.
3. Given an AI provider failure, when the operation returns, then no alternative provider is selected implicitly and the source file remains unchanged.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

## 18. Implementation or maintenance sequence

Separate metadata/search/AI contracts -> define privacy/provider policy -> add readiness validator -> verify deterministic indexing -> run authorized provider smoke only after policy is accepted.

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

- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/ai/AIRoutes.kt`
- `backend/plugins/ai-classification`
- `docs/plugins/AI_INTEGRATION.md`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.
