# Sharing and Permissions

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `sharing-and-permissions`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define internal shares, public share tokens, permission inheritance, expiration, revocation, and shared-with-me behavior.
- Related issues/PRs: #71, #90
- Required review gates: product UX, architecture, testing, documentation, security/operations
- Open decisions blocking readiness: see the explicit decisions and limitations below

## 1. Executive summary

Sharing is the authorization capability for granting access to storage items. It must remain separate from user authentication and from transport-specific protocol details.

This document is an integrated product and engineering contract. For an as-built baseline, it records current behavior and the contract maintainers choose to preserve; it does not by itself authorize a behavior change.

## 2. Problem, current behavior, and evidence

### 2.1 Problem

Current docs describe shares across API and frontend surfaces but do not provide one state model for token lifecycle, permissions, expiry, revoke, and inherited folder access.

### 2.2 Current behavior

Backend has share domain/core/application modules and share routes; frontend has shares and shared-with-me features.

### 2.3 Evidence

- `backend/domain/share`
- `backend/core/share`
- `backend/application/share`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/share/ShareRoutes.kt`
- `frontend/feature/shares`
- `frontend/feature/sharedwithme`
- `docs/api/API.md`

### 2.4 Retrospective classification

- **Observed behavior:** the current behavior described above is supported by the listed checkout evidence.
- **Intentional contract:** only the invariants and acceptance scenarios in this SDD are proposed for preservation; historical motivation is not inferred.
- **Known debt and limitations:** documentation/source mismatches, incomplete external verification, and explicitly named unsupported operations remain debt until closed.
- **Unknown rationale:** historic reasons for the current structure are unknown unless a source reference states them.
- **Proposed improvements:** any future change is listed in the implementation sequence and must not be read as implemented behavior.

### 2.5 Audit checkpoint — 2026-10-25

Crear, descargar y revocar un enlace simple funcionan; A05 muestra que su información pública y la del enlace protegido con contraseña correcta no se serializan. No declarar completo el recorrido de sharing.

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

1. Define share ownership and permission invariants.
2. Make public token and internal share behavior explicit.
3. Guarantee revoke/expiry takes effect before access.
4. Align API and frontend states.

### 4.2 Non-goals

1. A share never bypasses item ownership or server authorization.
2. Tokens are not treated as user identity.
3. No indefinite public access by accidental default.

### 4.3 Fixed product and safety invariants

1. Only an owner/authorized manager can create or revoke a share.
2. Expired/revoked shares cannot authorize reads or writes.
3. Permission scope is bounded to the shared item/tree.
4. Token values never appear in logs or docs.

## 5. Current versus proposed product journey

| Stage | Current/observed | Contract to preserve or implement | User/operator effect |
|---|---|---|---|
| Entry | Owner selects item | Validate identity, input, and configuration before mutation | Invalid work is rejected early |
| Operation | chooses recipient/permission/expiry | Apply one bounded semantic use case | Progress and ownership are explicit |
| Outcome | access reflects current state. | Return success, partial, or failure with retained state | Next action is inspectable |

Text equivalent: Owner selects item -> chooses recipient/permission/expiry -> share is created -> recipient discovers/uses it -> owner can change/revoke -> access reflects current state..

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
| `draft` | Share inputs not persisted | owner | validate/create |
| `active` | Share grants bounded access | owner/recipient | use/update/revoke |
| `expired` | Time limit elapsed | recipient | request new share |
| `revoked` | Owner removed access | recipient | no access |
| `invalid` | Token or permission invalid | recipient | safe denial |

## 7. User-facing configuration

Permission values, maximum expiry, public-share enablement, and inherited-folder rules are bounded and validated; secure defaults deny anonymous write.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

## 8. Clean Architecture design

### 8.1 Responsibilities and dependency direction

| Boundary | Owns | Must not own/import |
|---|---|---|
| Domain/pure policy | Sharing and Permissions invariants and provider-independent decisions | Ktor, Compose, database, SDK, process, or credentials |
| Application | use cases, semantic ports, ordering, typed outcomes | concrete transport/provider clients or raw DTO policy |
| Adapters/data | protocol mapping and external I/O | product policy or cross-capability orchestration |
| Infrastructure/composition | concrete clients, credentials, Koin/wiring, deployment | hidden business decisions |
| Entrypoints/presentation | input adaptation, state rendering, navigation/HTTP mapping | duplicated domain mutation policy |

### 8.2 Contracts, state, and trust boundaries

Application owns share policy and semantic share ports. API adapters map tokens/DTOs. Auth supplies identity; storage supplies resource facts; neither owns share presentation.

Share access decisions are pure/testable from actor, item, permission, state, and time. Token lookup and storage reads are adapters.

### 8.3 Executable architecture constraints

- Application and domain imports MUST be checked for forbidden transport, framework, process, SDK, and provider dependencies.
- Every concrete adapter MUST have focused contract tests and every public route/workflow MUST have an owning SDD/evidence entry.
- The validator MUST report missing evidence rather than infer success from file existence alone.
- RepoWise and Graphify results MAY guide investigation but MUST NOT be used as the sole acceptance criterion.

## 9. UI/UX and content contract

Primary user-facing states MUST show current status, completed facts, next action, impact, inspectable links, and progressively disclosed technical evidence. Machine identifiers and protocol paths remain stable and English.

Errors and partial results use the order **impact -> cause -> action -> retained state**. Technical identifiers, URLs, and protocol names remain stable. Visual diagrams are supplementary; the state tables and text above are normative.

## 10. Failure, recovery, and cleanup

A failed share creation does not report a link. A revoke failure retains active state and tells the operator whether retry is safe.

| Failure/partial state | Impact | Retained facts | Automatic retry | Required action | Cleanup |
|---|---|---|---|---|---|
| Invalid input/configuration | No operation starts | validation result | No | Repair input/config | None |
| Provider/storage failure | Operation may be partial | operation ID and completed effects | Bounded only if safe | Inspect and retry/reconcile | Release transient resources |
| Stale/duplicate request | No unsafe overwrite | authoritative current state | Safe replay if idempotent | Re-read and retry | None |
| Cancellation/timeout | Work may be incomplete | session/state/phase | Explicit | Resume, cancel, or recover | Expire owned transient state |

## 11. Security, permissions, and privacy

Threats include token guessing, leakage, privilege escalation, permission inheritance bugs, replay, and unbounded public write.

Untrusted content MUST remain data. Secrets, credentials, private content, raw provider responses, and stack traces MUST NOT appear in public errors, logs, generated docs, fixtures, or catalog evidence.

## 12. Observability and operational UX

Record share operation, item/share IDs, actor, outcome, expiry class, and correlation without token values.

Operator evidence MUST distinguish liveness, readiness, dependency failure, configuration failure, and retained partial state. Correlation IDs and stable semantic codes are preferred over raw exceptions.

## 13. Compatibility, migration, rollout, and rollback

Existing behavior is preserved for an as-built baseline unless a future revision explicitly changes the contract. Any behavior-changing migration MUST define in-flight state, rollback, and compatibility evidence.

For this baseline, the initial rollout is documentation and validation only. Future implementation work MUST state whether it is greenfield, backward-compatible, migratory, or breaking, and MUST define rollback for every irreversible effect.

## 14. Testing strategy and numeric budget

Minimum risk-derived budget: **Minimum 24 cases: create, permission matrix, ownership, internal recipient, public token, expiry, revoke, inherited folder, read/write/delete, invalid token, replay, concurrent revoke/use, and frontend state.**

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

1. Given a revoked share token, when it is used, then access is denied even if the token format is valid.
2. Given a folder share, when a child is moved outside the scope, then access follows the declared inheritance rule.
3. Given public sharing with no explicit expiry, when configuration requires expiry, then creation fails.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

## 18. Implementation or maintenance sequence

Define permission vocabulary -> inventory route behavior -> add policy tests -> verify token lifecycle -> align frontend/API/security docs.

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

- `backend/domain/share`
- `backend/api/src/main/kotlin/com/vaultstadio/api/routes/share/ShareRoutes.kt`
- `frontend/feature/shares`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.


## 21. Revision R05 — public file access (#90, pre-implementation contract)

Status: implementation planned on the Action-linked issue branch. This revision supersedes
historical metadata counter increments and password-in-query behavior; it is not completion evidence.
Internal recipient permissions, folder traversal, canonical collaboration persistence (#112), and
history grants remain outside #90. The portfolio capability remains partial.

### 21.1 Authority and information

A public token grants metadata and download of exactly one current, non-trashed FILE. Its creator
must still own that item. It never authenticates a user or grants listing, mutation, versions, or
collaboration. PRIVATE/SHARED/PUBLIC and ADMIN do not change this decision. Missing item, folder,
changed ownership, missing content key, negative size, malformed counter/limit, inactive link, expiry
at or before the decision clock, and exhausted limits fail closed. Tokens remain case sensitive.

Metadata returns a serializable public DTO containing only filename, byte size, MIME type,
password-required flag and optional expiry. It exposes no item/owner IDs, paths, content keys,
hashes of passwords, or recipient lists. Repeated successful metadata reads and every denied
read leave counters and persisted state unchanged. Password checking precedes protected metadata.

### 21.2 Admission and streaming

Domain owns pure access policy, safe semantic information, credential, clock, content and atomic
access ports. A focused application service depends only on domain modules. Infrastructure owns
BCrypt, Exposed/PostgreSQL and StorageBackend adapters. Transport owns serialization and headers.
No download uses a creator ID as authenticated user identity.

Information is inspected under item-then-share locks. Downloads first inspect an authorized
snapshot, open the provider stream and prepare at most one bounded prefix, then atomically recheck
the exact item/content identity and password hash, current ownership, link state, expiry and limit
under those same locks. The clock is read after lock acquisition. A successful admission increments
the counter once in the same transaction. Concurrent contenders for one remaining allowance admit
exactly one. Provider/preparation failure, pre-admission cancellation, stale authority/content,
password change, database failure and denied admission close owned streams and consume no allowance.

The counter measures **admitted download attempts**, not proof that a browser saved a file. After
admission commits, late provider I/O, disconnect or cancellation can leave partial bytes and a
consumed allowance. The server closes the stream, does not send a second success/error body or
silently retry, and retains that count. Revocation cannot retract already admitted bytes. Large
files stream with bounded memory; empty files are valid. Content length mismatch is an error,
including empty/truncated or extra provider bytes, not a successful complete transfer.

### 21.3 HTTP and human journey

- `GET /api/v1/share/{token}` returns typed metadata, without login.
- `GET /api/v1/share/{token}/download` streams admitted bytes as an attachment.
- Machine credentials use `X-Share-Password` containing the password's UTF-8 bytes encoded as
  canonical unpadded Base64url (1..72 decoded bytes, at most96 ASCII characters). This is transport
  encoding, not encryption; deployment still owns TLS. Malformed/noncanonical Base64url or invalid
  UTF-8 is400. This explicit encoding supports non-Latin-1 passwords in browser Fetch headers.
  A `password` query parameter is rejected (400). The native POST form uses normal UTF-8 form data.
- `GET /api/v1/share/{token}/view` serves an accessible public page outside the authenticated SPA.
- The public page reads metadata and submits a native POST form to
  `/api/v1/share/{token}/view/download`; the password travels only in the request body/header.
  Native attachment streaming avoids buffering the full file in browser JavaScript.
- Generated links point to that view URL using the configured API origin, retaining token case.
  Legacy machine paths remain stable; the former password query interface is intentionally removed.
- Public responses use no-store and no-referrer. The page inserts filenames using textContent;
  no token/password is written to console, analytics, browser storage, or server request logs.
- Missing/invalid resource is 404; revoked, expired and limit-exhausted links are 410;
  missing/wrong password is 403 with a stable safe code; invalid request is 400; unexpected
  provider/database failures are safe 500 responses without raw exceptions or credentials.
- The page has labelled password, load and download controls, visible loading/success/denial,
  an accessible status region and explicit retry. Metadata denial preserves local form inputs.
  A native POST denial renders a retry page with an empty password field; the password is never
  echoed in HTML responses or URLs. The page never claims a completed download.

### 21.4 Creation and migration

New public shares require an active owned FILE with a content key. Default expiry remains seven
 days; explicit null keeps existing deliberate non-expiring links. Numeric expiry is 1..365 days;
maxDownloads is null or 1..Int.MAX_VALUE. A password is null or 1..72 UTF-8 bytes, matching BCrypt's
input bound. Existing valid rows and download counts need no migration. Invalid legacy rows are
denied rather than repaired by access. Generic share lifecycle updates must not overwrite counters
with a stale caller snapshot. Rollback restores the previous executable package without deleting
rows or content; it also restores the known legacy public-access defects, so requires an operator
maintenance window rather than claiming equivalent security.

### 21.5 Verification budget and acceptance

Retain the 2,740 backend cases and the desktop/browser baseline (1,315/1,191), accounting separately
for moved legacy cases and intentional corrected expectations. Add at least **100 genuine #90 cases**:
30 pure policy/input boundaries, 20 orchestration/resource/failure cases, 30 actual HTTP/production-DI
cases against migrated PostgreSQL and local bytes, and 20 atomic SQL/concurrency/rollback cases.
These are risk floors, not a quota to fill with getters or duplicated assertions. All normative
branches need an explicit case, including equality at expiry, lock waits, stale passwords/content,
last-allowance concurrency, SQL/provider/cancellation failures, unknown fields and hostile filenames.

New public-page logic and URL generation require automated tests included in existing CI, plus
actual anonymous browser dogfood for plain/protected download, bad password, invalid/revoked/expired
link, retry, and native attachment byte verification. Record safe raw test inventory, actual commit,
coverage denominators, failures found and fixed, and browser limitations. Target complete line/branch
coverage of new pure access decisions; report uncovered branches honestly without new exclusions or
lowered existing gates. Run architecture, strict changed-source lint, specification catalog, secret
scan and full app CI on the final revision. Evidence is added only after executing these checks.


### 21.6 Pre-code protocol refinement — Unicode header transport

Inspection of the browser boundary exposed that arbitrary Unicode cannot be put directly in a
Fetch header value. Before implementing that boundary refinement, this contract fixes canonical
Base64url of strict UTF-8 for the machine header. Creation and access reject unpaired surrogates
rather than silently changing a password. Tests must include ASCII, whitespace, plus/percent,
non-Latin-1, multibyte byte limits, malformed encodings and actual BCrypt verification. This changes
only the new unpublished header contract; token paths and the body-form journey remain unchanged.
