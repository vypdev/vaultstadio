# Architecture Quality and Product Contract

- Status: Proposed
- Date: 2026-09-21
- Catalog capability ID: `architecture-quality-and-product-contract`
- Last verified: not applicable
- Owners: VaultStadio maintainers
- Scope: Coordinate the SDD portfolio, architectural ratchets, evidence policy, and ordered remediation of documentation and contract drift.
- Related issues/PRs: none recorded
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

VaultStadio needs one program contract that turns its broad architecture and feature documents into a reviewable, traceable portfolio. The fixed rule is that measurable quality and RepoWise/Graphify signals support decisions but never replace semantic ownership, tests, or product acceptance.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

Current architecture material mixes implemented structure, historical proposals, Phase 6 claims, and future work. There is no catalog that identifies an owning contract or blocks drift between code, tests, workflows, and docs.

### 2.2 Current behavior

The repository has standalone backend and frontend Gradle projects, broad capability modules, 44 tracked documentation files, and no `specs/` catalog. The audit identified stale paths, broken links, API contract divergence, release workflow drift, and missing SDD/ADR artifacts.

### 2.3 Evidence

- `docs/development/REPOSITORY_AUDIT.md`
- `backend/settings.gradle.kts`
- `frontend/settings.gradle.kts`
- `docs/architecture/ARCHITECTURE.md`
- `docs/architecture/BACKEND_CLEAN_ARCHITECTURE_PROPOSAL.md`

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

1. Create one authoritative SDD/catalog ownership model.
2. Classify existing docs as current reference, as-built baseline, proposal, or historical evidence.
3. Introduce deterministic validation for SDD metadata, evidence paths, and links.
4. Sequence work from architecture/API/security foundations into capabilities.

### 4.2 Non-goals

1. Do not rewrite every historical document in one change.
2. Do not create one SDD per Gradle module or route.
3. Do not refactor code to improve a structural score without caller and contract evidence.

### 4.3 Fixed product and safety invariants

1. A capability has one owning SDD even when it has companion documents.
2. An `Implemented` status requires current evidence at a recorded commit.
3. A catalog or documentation validator never mutates source, workflows, or generated artifacts.
4. Blocking decisions cannot be hidden in prose or bypassed by a green unit suite.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | Audit current repository | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | classify evidence | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | verify immutable commit. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: Audit current repository -> classify evidence -> define owner SDD -> validate catalog -> implement one bounded capability -> update SDD/tests/docs together -> verify immutable commit..

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
| `draft` | Portfolio incomplete | maintainers | proposed |
| `proposed` | Scope and ordering under review | maintainers | ready-for-implementation or blocked |
| `ready-for-implementation` | Required decisions and evidence targets resolved | maintainers | implementing |
| `implementing` | One bounded capability is changing | owner | validating or blocked |
| `implemented` | Contract and evidence agree at a verified commit | maintainers | superseded or maintained |

## 7. User-facing configuration

The catalog location, required headings, status vocabulary, and evidence fields are non-configurable. Validation command is `python3 scripts/validate-specification-catalog.py`.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Architecture Quality and Product Contract invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

The program SDD owns cross-cutting sequencing and gates. Capability SDDs own product contracts. Application code remains inward-facing: presentation/API -> application -> domain; infrastructure implements semantic ports; composition wires concrete adapters.

The catalog is a closed JSON schema. Each capability has an ID, title, status, owner, SDD paths, evidence paths, and last-verified commit/date. Unknown IDs, duplicate owners, missing paths, and unsupported statuses fail validation.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

Invalid catalog blocks publication without changing repository content. A stale evidence commit changes status to blocked or requires re-verification; it must not be silently refreshed by a validator.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Catalog inputs are repository-controlled data. Validators must not execute document code, load credentials, or trust prose as executable configuration.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Validation reports exact capability, path, and rule failures. It reports missing external evidence as unknown, not as success.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 12 cases: catalog schema, duplicate IDs, duplicate owning capability, missing SDD, missing evidence, invalid status, missing metadata, required headings, malformed JSON, path traversal, stale verification metadata, and clean success.**

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

1. Given a new capability, when it is added to the catalog, then its owning SDD and every evidence path validate.
2. Given an invalid status or missing path, when validation runs, then it fails with a precise path and no mutation.
3. Given a historical proposal, when it is classified, then it cannot appear as current implemented behavior without an explicit status change.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

## 18. Implementation or maintenance sequence

Governance files -> catalog -> validator -> architecture baseline -> API/security baselines -> capability SDDs -> final portfolio audit.

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

- `docs/development/REPOSITORY_AUDIT.md`
- `vypdev/copilot/specs/README.md`
- `vypdev/copilot/specs/_template.md`
- `vypdev/copilot/specs/architecture-quality-and-scalability-hardening.md`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.
