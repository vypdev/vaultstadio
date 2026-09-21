# Security, Privacy and Threat Model

- Status: Proposed
- Date: 2026-09-21
- Catalog capability ID: `security-privacy-and-threat-model`
- Last verified: not applicable
- Owners: VaultStadio maintainers
- Scope: Define cross-cutting threat models, privacy rules, trust boundaries, abuse controls, logging constraints, and security evidence.
- Related issues/PRs: none recorded
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

Storage, public shares, WebDAV, S3, federation, plugins, AI, and backups create cross-cutting risks that cannot be closed by authentication documentation alone. This SDD provides the threat model and security acceptance layer for every capability SDD.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

Security guidance is distributed across operations and feature docs, while several capabilities have explicit external or extensibility boundaries. The current workflow also contains fail-open/missing-file risks.

### 2.2 Current behavior

JWT, storage, federation, WebDAV, S3, plugins, AI, and deployment security documentation exists, but no threat-model SDD maps assets, actors, abuse cases, mitigations, and evidence.

### 2.3 Evidence

- `docs/operations/SECURITY.md`
- `backend/api/src/main/kotlin/com/vaultstadio/api/config/Security.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/WebDAVRoutes.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/S3Routes.kt`
- `backend/core/federation`
- `backend/plugins`
- `docs/plugins/AI_INTEGRATION.md`
- `.github/workflows/security.yml`

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

1. Create an asset/trust-boundary/threat inventory.
2. Define fail-closed security invariants shared by all SDDs.
3. Make secret/log/privacy rules executable.
4. Separate current evidence from unverified security claims.

### 4.2 Non-goals

1. No threat rating without an explicit asset and attack path.
2. No raw credentials or content in audit evidence.
3. No security check is called blocking until its actual command and failure behavior are verified.

### 4.3 Fixed product and safety invariants

1. Least privilege and resource ownership are enforced at every mutation boundary.
2. Untrusted input cannot become executable code, path, query, protocol command, or plugin authority.
3. Secrets and private content never enter logs, public errors, docs, or generated artifacts.
4. Missing security evidence is unknown/blocking, never implicit success.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | Identify asset | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | cross trust boundary | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | retain/recover state on failure. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: Identify asset -> cross trust boundary -> validate identity/input/policy -> perform bounded operation -> emit safe evidence -> retain/recover state on failure..

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
| `unclassified` | Asset/threat not mapped | maintainer | classify |
| `mitigated` | Control exists and is tested | maintainer | verify |
| `exposed` | Known gap or fail-open path | maintainer | block/remediate |
| `accepted` | Risk explicitly accepted with owner/expiry | maintainer | review |
| `closed` | Evidence proves control | maintainer | monitor |

## 7. User-facing configuration

Security limits such as token expiry, upload size, rate limits, plugin capabilities, federation domains, AI send policy, and secret sources are bounded and not weakened by arbitrary user input.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Security, Privacy and Threat Model invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

Security policies remain pure/application-level where possible; transport/provider adapters enforce boundary mechanics; composition binds secrets and trusted clients; presentation exposes safe outcomes only.

Each capability SDD maps threat -> invariant -> implementation -> test/evidence -> docs. A central forbidden-content scan checks current tree and generated artifacts.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

Security control failure blocks the operation or reports degraded/unknown state. It never falls back to anonymous, alternate provider, unrestricted path, or raw diagnostics.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Primary threats: auth/replay, path traversal, upload abuse, token/share leakage, plugin escalation, federation spoofing, AI exfiltration, registry/artifact tampering, and sensitive logging.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Security events use safe codes and correlation; sensitive values are absent. Operator evidence states what was retained and what action is required.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 32 cases: input abuse, auth/replay, ownership, path/XML/signature, secret/log scans, plugin boundary, federation trust, AI privacy, rate/size limits, artifact/workflow security, and recovery.**

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

1. Given any untrusted path or protocol input, when validation runs, then it cannot escape the authorized resource boundary.
2. Given a security scanner command references a missing file, when the workflow runs, then it fails or reports explicit advisory status rather than silently succeeding.
3. Given a provider or credential is unavailable, when a sensitive operation starts, then it fails closed without selecting an unsafe alternate.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

## 18. Implementation or maintenance sequence

Inventory assets/trust boundaries -> threat matrix -> define shared invariants -> add static/security contract checks -> reconcile capability SDDs -> perform authorized integration tests.

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

- `docs/operations/SECURITY.md`
- `.github/workflows/security.yml`
- `SDD-0003`
- `SDD-0004`
- `SDD-0010`
- `SDD-0011`
- `SDD-0012`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.
