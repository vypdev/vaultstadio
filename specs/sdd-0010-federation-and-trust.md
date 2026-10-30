# Federation and Trust

- Status: Proposed
- Date: 2026-09-21
- Catalog capability ID: `federation-and-trust`
- Last verified: not applicable
- Owners: VaultStadio maintainers
- Scope: Define cross-instance identity, signed messages, federated shares, activities, trust, key lifecycle, and failure behavior.
- Related issues/PRs: #71, #124
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

Federation crosses instance trust boundaries and must be specified before it is presented as a complete Phase 6 capability. The current implementation provides models, crypto, services, and routes, but documentation also records unimplemented outbound behavior.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

Federation claims mix implemented routes and proposed operations. Trust, replay protection, key rotation, domain policy, and outbound request status are not one closed contract.

### 2.2 Current behavior

Federation domain/core models, crypto, service, and routes exist. The current docs explicitly mark `sendFederationRequest()` as not implemented.

### 2.3 Evidence

- `backend/domain/federation`
- `backend/core/federation/src/main/kotlin/com/vaultstadio/core/domain/service/FederationCrypto.kt`
- `backend/core/federation/src/main/kotlin/com/vaultstadio/core/domain/service/FederationService.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/federation/FederationRoutes.kt`
- `docs/api/PHASE6_ADVANCED_FEATURES.md`
- `docs/operations/SECURITY.md`

### 2.4 Retrospective classification

- **Observed behavior:** the current behavior described above is supported by the listed checkout evidence.
- **Intentional contract:** only the invariants and acceptance scenarios in this SDD are proposed for preservation; historical motivation is not inferred.
- **Known debt and limitations:** documentation/source mismatches, incomplete external verification, and explicitly named unsupported operations remain debt until closed.
- **Unknown rationale:** historic reasons for the current structure are unknown unless a source reference states them.
- **Proposed improvements:** any future change is listed in the implementation sequence and must not be read as implemented behavior.

### 2.5 Audit checkpoint — 2026-10-25

El endpoint de liveness responde; requestFederation crea el registro pending sin ejecutar el envío HTTP descrito en su comentario. No se verificó interoperabilidad ni ciclo de claves entre dos instancias; se conserva Proposed.

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

1. Separate modeled, implemented, and externally verified federation behavior.
2. Define trust and signed-message rules.
3. Specify inbound/outbound lifecycle, replay, rotation, revocation, and failure.
4. Prevent federation from bypassing local authorization.

### 4.2 Non-goals

1. No remote instance is trusted solely by URL or prompt/config text.
2. No outbound operation is called implemented until real contract evidence exists.
3. No compatibility alias hides unsupported federation behavior.

### 4.3 Fixed product and safety invariants

1. Every remote identity has explicit trust state.
2. Messages are authenticated, bounded, replay-protected, and versioned.
3. Local owner/permission rules remain authoritative.
4. Key material never enters logs, docs, or application results.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | Operator configures identity | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | instance advertises/requests trust | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | revoke/rotate or recover failure. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: Operator configures identity -> instance advertises/requests trust -> request is verified and accepted/rejected -> signed share/activity flows -> revoke/rotate or recover failure..

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
| `untrusted` | Remote identity unknown | operator | inspect/request |
| `pending` | Trust request awaiting decision | operator | accept/reject |
| `trusted` | Signed operations allowed | system | share/activity/revoke |
| `revoked` | Remote/key no longer accepted | operator | rotate/re-establish |
| `degraded` | Remote unavailable or invalid | operator | retry/investigate |

### 6.4 Accepted product discovery — issue #124 (2026-10-29)

These are **accepted product targets, proposed for implementation**. They do not change the observed baseline, catalog status or last-verified evidence. The [discovery index](product-discovery-2026-10-29.md) retains the source answers and cross-capability dependencies; this SDD owns the requirements below.

<a id="pd-002"></a>

#### PD-002: Managed nodes and independent installations

Adding a server MUST allow an explicit choice between a storage node governed by the primary and an independent installation with its own authority. Independent installations expose only mutually authorized resources; adding their URL does not delegate administration.

**Blocking technical decisions:** Enrollment, trust establishment, identity mapping, supported remote operations, protocol compatibility and mode conversion. Managed-node enrollment and federation trust need distinct semantic ports.

<a id="pd-003"></a>

#### PD-003: Administrative server enrollment and capacity grants

Adding managed nodes or independent installations and enabling their capacity for users or spaces MUST be restricted to administrators. Enrollment alone does not confer file access, a remote write grant or backup authority.

**Blocking technical decisions:** Administrative role granularity, grant revocation during transfers, audit retention and limits on delegated administration.

## 7. User-facing configuration

Federation enablement, instance domain/name, public/private keys, allowed/blocked domains, HTTPS requirement, and key rotation are explicit and fail closed.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

### Accepted target configuration (#124)

| Requirement | Authority, default and inheritance |
|---|---|
| PD-002 | Administrator chooses the mode for each addition. Neither model is a universal default chosen in this round; record the selected mode explicitly. |
| PD-003 | Administrator-managed per-server capacity assignments to users/spaces. Effective assignments remain constrained by independent remote authority and content permissions. |

These are product-policy semantics, not new environment variable/API field names. Before implementation, define typed validation/bounds, persistence, effective policy revision for pending work and migration/rollback. An override cannot bypass content authorization, E2EE boundaries or protected retention. Unknown/invalid policy is not permission to select a weaker default.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Federation and Trust invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

Crypto and wire adapters remain infrastructure. Domain owns trust/message invariants. Application owns federation use cases. API maps inbound requests and never decides trust through route conditionals.

Signed message schema, canonicalization, identity/key fingerprint, replay nonce/timestamp, and response states must be versioned.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

Invalid signature, stale message, remote timeout, or partial share never becomes trusted success. Retained state and retry action are explicit.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Threats include spoofing, replay, SSRF, domain confusion, key theft, trust escalation, and cross-instance data leakage.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Record remote identity fingerprint, operation, trust state, bounded reason, and correlation without URLs containing secrets or key material.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 28 cases: trust states, signature, canonicalization, replay, key rotation, invalid remote, domain policy, inbound/outbound, authorization, timeout, partial share, and secret-safe logging.**

The budget is a floor, not a substitute for covering every normative requirement. Tests SHOULD include pure policy tests, application/use-case tests, adapter contract tests, route/workflow/schema tests, security/abuse tests, and integration or human evidence where automation cannot establish the contract. No live external service is required for deterministic unit tests; authorized external smoke evidence MUST be labelled separately.

For #124, add at least **6 future acceptance families** (2 decisions × positive, denied/invalid and failure/recovery). The concrete families are listed in §16; they are **planned, not executed tests**. Expand actor, policy, boundary, concurrency, platform and cryptographic matrices before the affected implementation is marked ready. Preserve existing regression budgets.

## 15. Documentation and discoverability

| Audience | Required artifact | Purpose |
|---|---|---|
| User/client | API or frontend guide | Normal journey, defaults, examples, visible states |
| Operator | configuration/recovery/runbook | prerequisites, health, failure, retry, cleanup |
| Contributor | this SDD and architecture guide | boundaries, contracts, tests, implementation order |
| Maintainer | catalog and traceability | ownership, verification commit, unresolved decisions |

The owning user-facing documentation MUST link back to this SDD or its catalog entry. Examples MUST be checked against current code or contract fixtures.

## 16. Acceptance scenarios

1. Given an unsigned or invalidly signed request, when received, then it is rejected without state mutation.
2. Given a revoked remote key, when a signed message arrives, then it does not authorize a share or activity.
3. Given outbound federation is not implemented, when configured, then startup/doctor reports unsupported rather than advertising readiness.

### Planned discovery acceptance (#124)

- **PD-002-P** — Add one server of each type and verify the displayed mode, authority and authorized capacity against both servers.
- **PD-002-N** — A managed-node credential cannot administer an independent installation or enumerate its unshared content.
- **PD-002-F** — A failed enrollment or revoked peer leaves no falsely usable capacity; retain enough state to retry or remove only the incomplete enrollment.

- **PD-003-P** — An administrator enables a server for one space; an authorized member can use it while an unrelated space cannot.
- **PD-003-N** — A non-administrator enrollment/assignment request is denied before remote enrollment or local catalog effects.
- **PD-003-F** — Revoke an assignment during a pending operation and verify a defined retained outcome without falling back to an unauthorized server.

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
| [PD-002](#pd-002) | PD-002-P / -N / -F | Accepted target; implementation and current execution evidence still required | [M4](../docs/development/PRODUCT_ROADMAP_2026-10-29.md#m4) |
| [PD-003](#pd-003) | PD-003-P / -N / -F | Accepted target; implementation and current execution evidence still required | [M4](../docs/development/PRODUCT_ROADMAP_2026-10-29.md#m4) |

## 18. Implementation or maintenance sequence

Classify current behavior -> choose wire/trust contract -> define key lifecycle -> implement/verify outbound contract -> add isolated interoperability evidence.

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

- `backend/core/federation/src/main/kotlin/com/vaultstadio/core/domain/service/FederationCrypto.kt`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/federation/FederationRoutes.kt`
- `docs/api/PHASE6_ADVANCED_FEATURES.md`

- Portfolio governance: `sdd-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.

Discovery provenance and ownership: [30 accepted decisions](product-discovery-2026-10-29.md), [verbatim answers](discovery/2026-10-29/answers.json), [issue #124](https://github.com/vypdev/vaultstadio/issues/124).
