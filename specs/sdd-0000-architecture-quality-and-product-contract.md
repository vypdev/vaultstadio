# Architecture Quality and Product Contract

- Status: Proposed
- Date: 2026-09-21
- Catalog capability ID: `architecture-quality-and-product-contract`
- Last verified: not applicable
- Owners: VaultStadio maintainers
- Scope: Coordinate the SDD portfolio, architectural ratchets, evidence policy, and ordered remediation of documentation and contract drift.
- Related issues/PRs: #71, #124
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

### 2.5 Audit checkpoint — 2026-10-25

El catálogo estructural valida 16 capacidades; ninguna declara Implemented. La nueva evidencia de runtime contiene escenarios fallidos. Mantener separados validación documental y readiness de producto.

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

| State | Trigger/condition | Owner | Allowed next states |
|---|---|---|---|---|
| `draft` | Portfolio incomplete | maintainers | proposed |
| `proposed` | Scope and ordering under review | maintainers | ready-for-implementation or blocked |
| `ready-for-implementation` | Required decisions and evidence targets resolved | maintainers | implementing |
| `implementing` | One bounded capability is changing | owner | validating or blocked |
| `implemented` | Contract and evidence agree at a verified commit | maintainers | superseded or maintained |

### 6.4 Accepted product discovery — issue #124 (2026-10-29)

These are **accepted product targets, proposed for implementation**. They do not change the observed baseline, catalog status or last-verified evidence. The [discovery index](product-discovery-2026-10-29.md) retains the source answers and cross-capability dependencies; this SDD owns the requirements below.

<a id="pd-001"></a>

#### PD-001: Primary installation and expandable capacity

The product MUST provide an operator-configured primary installation where users sign in, support additional authorized servers that expand capacity, and offer unified and server-grouped views of authorized files. Operator and user may be the same person; their authorities remain distinct.

**Blocking technical decisions:** Primary discovery, stable identities across views, offline/unavailable server presentation and the expansion protocol. #116 fixes Web API origin only; it does not implement this topology.

<a id="pd-016"></a>

#### PD-016: Explicit scale target and configurable limits

The product target MUST be up to 100 GB per file and 10 million files per installation by default, with administrator-configurable limits and space/vault overrides. These are verification targets, not implemented capacity claims; numeric settings alone do not establish this scale.

**Blocking technical decisions:** GB units, population counted as files, installation-wide versus per-space ceilings, hardware/workload/concurrency, latency/memory SLOs and reproducible resource budget. Current 32 MiB bounded UI evidence remains valid only for that bound.

## 7. User-facing configuration

The catalog location, required headings, status vocabulary, and evidence fields are non-configurable. Validation command is `python3 scripts/validate-specification-catalog.py`.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

### Accepted target configuration (#124)

| Requirement | Authority, default and inheritance |
|---|---|
| PD-001 | Operator selects the primary; administrators control expansion. Fixed installation is the default journey. A file-view preference cannot grant access or turn an independent installation into a managed node. |
| PD-016 | Default target 100 GB/file and 10 million files/installation. Overrides cannot create physical resources or bypass permission, quota, memory or safety limits. |

These are product-policy semantics, not new environment variable/API field names. Before implementation, define typed validation/bounds, persistence, effective policy revision for pending work and migration/rollback. An override cannot bypass content authorization, E2EE boundaries or protected retention. Unknown/invalid policy is not permission to select a weaker default.

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

1. Given a new capability, when it is added to the catalog, then its owning SDD and every evidence path validate.
2. Given an invalid status or missing path, when validation runs, then it fails with a precise path and no mutation.
3. Given a historical proposal, when it is classified, then it cannot appear as current implemented behavior without an explicit status change.

### Planned discovery acceptance (#124)

- **PD-001-P** — Given two authorized servers and one forbidden file, switching unified/grouped views preserves the same authorized file identities and hides the forbidden file.
- **PD-001-N** — A normal user cannot replace the operator-selected primary or gain a remote grant by changing a view.
- **PD-001-F** — When one server becomes unavailable, the view reports that loss and retains known source identity; it does not claim an empty successful server or lose pending work.

- **PD-016-P** — On a declared representative environment, transfer/checksum a 100 GB file and query/mutate a 10-million-file catalog within agreed memory/latency objectives.
- **PD-016-N** — Configured lower limits reject boundary+1 without partial admitted content; an override cannot exceed a governing installation safety ceiling once defined.
- **PD-016-F** — Interrupt/restart large transfers and index operations under resource pressure; prove bounded memory, resumability and no lost/duplicated data before claiming acceptance.

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
| [PD-001](#pd-001) | PD-001-P / -N / -F | Accepted target; implementation and current execution evidence still required | [M0](../docs/development/PRODUCT_ROADMAP_2026-10-29.md#m0) |
| [PD-016](#pd-016) | PD-016-P / -N / -F | Accepted target; implementation and current execution evidence still required | [M8](../docs/development/PRODUCT_ROADMAP_2026-10-29.md#m8) |

## 18. Implementation or maintenance sequence

Governance files -> catalog -> validator -> architecture baseline -> API/security baselines -> capability SDDs -> final portfolio audit.

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

- `docs/development/REPOSITORY_AUDIT.md`
- `vypdev/copilot/specs/README.md`
- `vypdev/copilot/specs/_template.md`
- `vypdev/copilot/specs/architecture-quality-and-scalability-hardening.md`

- Portfolio governance: `sdd-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.

Discovery provenance and ownership: [30 accepted decisions](product-discovery-2026-10-29.md), [verbatim answers](discovery/2026-10-29/answers.json), [issue #124](https://github.com/vypdev/vaultstadio/issues/124).

## Consumer SDD publication migration — issue #145, 2026-10-30

This appendix is the pre-implementation contract for the maintainer-authorized
adoption of the supported Copilot SDD system. Every byte of the preceding
historical contract is retained. The capability remains Proposed; this migration
does not implement PD-001/PD-016 or certify any application journey.

### Origin, authority and scope

At develop `46225cd47b10d248277f41310575d32e78203ca8`, the native validator
accepts 16 capabilities. Actual #82 run 41081742618 fails closed with
`configuration.unsupported` / `invalid-metadata` before publishing an SDD or
branch. Copilot's supported version-1 writer requires normalized inventory
states, valid dates, typed regular-file evidence, unique ownership, and the
standard/template prerequisites. Its complete validator also requires baseline
overview diagrams, representative presentation states and tabulated budgets.

The maintainer explicitly selected adoption on 2026-10-30. This changes the
consumer standard, catalog, rendering and validation only. The ordinary
Action-created documentation branch owns this prerequisite; #82 retains its
pre-branch gate. SDK internals, generated Copilot guidance/workflows, credentials,
protections, `VERIFICATION_SETUP=false`, unrelated worktrees and their dirty SDD
drafts are outside this migration. The existing application CI contract step
must execute the catalog regression controls explicitly; add only that command,
preserving its job graph, triggers, cadence and major-tag Action references.

### Requirements and observable acceptance

| ID | Requirement | Acceptance/evidence target |
|---|---|---|
| SDD145-01 | The catalog MUST retain all 16 stable IDs, original titles/scopes/owners, primary SDDs, accepted decisions, open decisions and original flat evidence. | Compare decoded before/after inventories against the immutable source and preservation manifest. |
| SDD145-02 | Inventory states MUST map Proposed → `proposed` and As-built baseline → `as-built-baseline`; no capability becomes implemented. Detailed document readiness remains separate. | Reject unsupported states; assert the original four/twelve distribution and no readiness promotion. |
| SDD145-03 | Every original catalog verification value, including null, MUST remain explicit history. A newly inspected structural date MUST NOT be described as a runtime or implementation acceptance date. | Compare legacy metadata including nulls; review the generated view and migration explanation. |
| SDD145-04 | Typed specs/workflows/entrypoints/code/tests/documentation MUST contain meaningful regular contained files in their declared roles. Existing directory evidence remains historical inventory, not a new file claim. | Reject missing, escaped, symlink, duplicate and wrong-boundary paths; inspect role assignments. |
| SDD145-05 | The discovery index MUST be an explicitly identified companion of this governance capability. The 30 accepted requirement owners remain the existing primary SDDs. | Register every specification Markdown file once; preserve the discovery index/answers bytes and existing requirement links. |
| SDD145-06 | Every original SDD MUST remain an unchanged byte prefix. Append-only baseline supplements restate the existing journey, presentation contract and numeric floor without lowering later scoped matrix budgets or inventing execution. | Verify prefix hashes; compare each repeated floor with original section 14; keep unknown/partial/failure receipts visible. |
| SDD145-07 | The standard/template MUST satisfy the supported structural prerequisite checks while retaining VaultStadio's metadata, history and evidence distinctions. | Validate eight populated standard sections, twenty template sections, normative guidance and required headings. |
| SDD145-08 | Default validation MUST be deterministic, offline and read-only. An explicit generation mode may change only `specs/CATALOG.md`. | Snapshot files before/after valid and invalid runs; reject stale generated output and prove generation idempotent. |
| SDD145-09 | Consumer and unmodified SDK validation MUST both pass on the candidate. A green catalog MUST NOT imply that #82, native UI, external providers or all setup journeys work. | Retain exact policy provenance, candidate tree, original command output and limitations; re-enter #82 only after reviewed adoption. |

The generated view must distinguish absence of prior verification metadata for a
new capability from an explicitly preserved historical null. CI must collect the
new catalog controls in its existing contract job; a local-only test file is not
evidence that those controls execute on the final PR.

PR #164 review identified an additional structural acceptance boundary: every
required primary section 1–20 MUST contain visible content or an explicit
not-applicable explanation. Metadata and empty headings alone cannot establish a
valid primary contract. Preserve populated later historical sections (including
SDD-0006's section 21) and appended revisions; fenced examples, comments and bare
subheadings do not supply a missing section body. Verify empty-primary rejection,
an explicit not-applicable explanation and retained historical appendices in the
catalog filesystem/CLI controls before adoption.

### Verification metadata and migration semantics

`legacyVerification` preserves each capability's exact previous status and
`lastVerified`, including the three null values. The original root checkpoint
fields and flat evidence remain historical. In the adopted profile a capability's
`lastVerified` is the date its declared structural evidence inventory was
inspected. `structuralVerification` records the reviewed baseline and explicitly
limited scope. Neither field refreshes runtime receipts, resolves open decisions,
changes document readiness or supersedes the SDD's historical Last verified line.
Future behavior changes must record their actual runtime source and evidence in
the owning SDD; an automatic catalog refresh is insufficient.

### Risk-derived migration test budget

These are planned distinct validator/migration controls, separate from the
application's 6800-test baseline and all existing product budgets.

| Risk area | Minimum cases | Observable controls |
|---|---:|---|
| Metadata and ownership | 7 | Clean success; malformed JSON; malformed root; duplicate ID/title; duplicate SDD owner; unsupported status; impossible verification date |
| File and publication boundaries | 6 | Missing file; directory in typed evidence; traversal; symlink; wrong workflow/spec boundary; unregistered specification |
| Standard and generated view | 5 | Valid populated standard/template; missing/empty required section; missing normative guidance; stale generated view; deterministic generation |
| Preservation and failure effects | 4 | Original nullable metadata retained; owner/decision/evidence inventory unchanged; original SDD prefix retained; default success/failure does not mutate inputs |
| **Total** | **22** | Each collected behavior counts once; expand for additional branches |

Traceability: SDD145-01/02/03/05/06 map to preservation controls and the migration
report; SDD145-04 maps to real filesystem fixtures; SDD145-07 maps to structural
document checks; SDD145-08 maps to CLI/generated-view controls; SDD145-09 maps to
the independent unmodified SDK readback and PR evidence. The native historical
twelve-case floor remains in §14; the twenty-two migration controls are additional
scope, not a rewrite of previously executed product counts.

### Sequence, recovery and remaining work

Document this contract first, inspect/register typed evidence, append baseline
format supplements, adopt the standard/template, implement focused consumer
validation/rendering, run negative/preservation controls, then review the PR and
same-source CI. Obtain any merge authorization separately. Reverting the focused
consumer change restores the previous native format and its explicit #82 block;
no user data migration or irreversible external effect occurs.

The broader #145 environment/setup journeys remain separate acceptance work.
Filename-search implementation starts only after #82's SDD and exact branch are
published by the Action. The current compatibility failure and historical
verification limits remain inspectable in the migration report and issue cards.

### Exact public-checksum security review — PR #164

Native security run 41093209961 flagged one `ProtocolsIO` candidate at commit
`e81da9c26ba72274eee16b62b56b8af3ab7201b5`, line 469 of
`docs/development/verification/sdd-consumer-adoption-145-preservation.json`.
That field is the original SHA-256 of SDD-0005. Recomputing it from the immutable
`46225cd47b10d248277f41310575d32e78203ca8` source blob matches the detected value;
the value is used only to verify source preservation, not for authentication.

Record this disclosed finding through the existing exact immutable-coordinate
non-secret decision mechanism, bound to TruffleHog 3.99.7. Keep both scanners,
complete history, offline execution and rejection of every unreviewed finding.
No rule/path-wide exception or detector suppression is part of this review.
The added security control MUST independently recompute the historical checksum
and prove that changing commit, path or line does not inherit this decision.
Its evidence target is `scripts/tests/test_secret_nonsecret_decisions.py` and
the original native scan plus complete-source scanner readback.


## Consumer owner-path publication contract — issue #178

This scoped maintenance contract is recorded before changing catalog policy or
migration tooling. It preserves the product requirements and acceptance budgets
above. Baseline: develop `a72344dddb8232ca496ecf0ad614d940ec05c858`, following #164.

### Observed prerequisite and intended result

The native #82 attempts 41108456778 and 41109113102 published no SDD or branch.
The latter passed companion ownership analysis but the published SDK reader
rejected an existing owner path before drafting. An independent execution of the
unchanged installed SDK workspace adapter against the immutable baseline confirms
that all 17 registered owner paths are rejected: 16 uppercase primary filenames
and the nested discovery companion. Catalog acceptance and populated sections do
not establish workspace-reader or publication acceptance.

Every registered owner MUST be a real, unique, contained file with a flat
lowercase `specs/<name>.md` path accepted by the unchanged SDK writer and reader.
Reserved standard/template/catalog paths MUST NOT become capability owners.
The consumer validator MUST enforce this publication boundary while retaining
regular-file, symlink, ownership, populated-section, evidence and discovery checks.

### Migration, preservation and compatibility

1. Rename the 16 current primary files from `SDD-...md` to `sdd-...md` and move the
   existing discovery owner to `specs/product-discovery-2026-10-29.md`. Capability
   IDs, primary/companion order and logical ownership remain the same.
2. Preserve every original requirement, decision, case ID, budget, date, nullable
   historical value and readiness classification. Canonical path/link rebasing is
   the only permitted transformation of pre-existing normative content. New
   migration guidance is explicitly appended, not presented as historical intent.
3. Keep verbatim discovery answers and dated source receipts unchanged. Historical
   source-coordinate references, including reviewed-nonsecret decisions, retain
   their original immutable commit/path/line semantics.
4. Record a bijective old/new path map, immutable Git source origins and reversible
   literal link/reference changes. Verification MUST reconstruct the original
   baseline bytes from each moved owner and compare its Git blob identity. Existing
   #145 preservation checks MUST still reconstruct and verify their earlier
   prefixes; a renamed path does not waive historical preservation.
5. Rebase the moved discovery document's relative links according to its new
   directory and update live internal links/catalog paths. Validate canonical
   path spelling against a case-sensitive inventory so macOS file lookup cannot
   hide links that fail on Linux/GitHub.
6. Publish the path/catalog/reference set together through a normal reviewed
   contributor commit on the exact branch created by the Action. There is no
   database, file-content, user-data, runtime or product API migration.

Rollback uses a reviewed forward commit restoring the complete prior
path/catalog/reference set. Original Git blobs, answers and source receipts remain
available; no history rewrite, destructive reset or work deletion is required.
Concurrent edits require reconciliation against the immutable source and final
Action branch head before a normal push.

### Architecture, verification and acceptance

Publication-path policy belongs to focused repository catalog tooling; product
domain/application/adapters and Copilot internals remain unchanged. One-time
migration orchestration is not a new product capability. Preserve every CI
trigger, job, runner, permission, existing threshold and major Action reference.
Focused controls may run within the existing contract job without replacing any
existing command.

Minimum additional budget: **12 distinct path, reconstruction and preservation
controls**, beyond the original 44 catalog controls. Cover valid flat ownership,
uppercase/nested/reserved path denial, duplicate/colliding mappings, complete
primary/companion inventory, original owner reconstruction, discovery index and
verbatim answers, historical metadata and decision ownership, exact live-link
rebasing, tamper detection, partial-migration rejection and reversible rollback.
Use owned real temporary files for failure controls. Fixture variants and
repeated platform executions do not multiply the budget.

Independently run the actual unchanged SDK workspace reader on the committed
candidate: all 17 registered owners MUST be read successfully and invalid-path
controls MUST remain denied. These are one integration family with 17 owner
observations, not 17 new product tests. Preserve the original all-17-rejected
baseline proof. No provider, model, CLI entrypoint or native branch operation is
needed for this reader check.

Run all four unchanged contributor commands, scoped tooling coverage, actual
native CI/security and full Copilot review, retaining original artifacts and
failures. Product test totals remain separate from catalog/path controls. A
successful reader check does not certify model drafting, native publication or
filename-search behavior; #82 still requires its own Action-published SDD,
verified branch and implementation acceptance after authorized adoption.

Documentation MUST explain canonical paths, historical origins, the migration and
rollback, reader evidence and remaining limits. Keep #165's retained setup scope,
#105's consolidated dependency update, #111's separate activation responsibility,
`VERIFICATION_SETUP=false`, all protections, draft #86 and unrelated local drafts.
No SDK edit, manual managed branch, Copilot cancellation, deployment, release or
tag is authorized. This follow-up PR needs its own explicit merge authorization.
