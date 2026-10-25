# Frontend KMP and Platform Architecture

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `frontend-kmp-and-platform-architecture`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define frontend domain/data/feature boundaries, navigation, state ownership, platform entrypoints, and user-facing capability mapping.
- Related issues/PRs: #71
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

### 2.5 Audit checkpoint — 2026-10-25

A03/A04 bloquean el arranque web y desktop aunque pasen 1.130 tests desktop y 1.070 Wasm. Android genera APK sin tests unitarios de app; A09 impide compilar la entrada iOS y documenta stubs de plataforma. El soporte debe expresarse por capacidad y destino.

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

### 6.1.1 Accessible interaction contract (#80)

| ID | Required behavior | Acceptance evidence |
|---|---|---|
| A11Y-RUN-01 | Desktop packaging MUST use a reviewed runtime that supports editable accessibility and real automatic text entry. Bytecode compatibility alone is insufficient. | Same application JARs compared across runtimes; generated package runtime identity; autonomous normal login, without seeded sessions. |
| A11Y-FIELD-01 | Every active editable field MUST expose a stable test identifier and a meaningful localized accessible name, and retain its production value/callback, validation and IME behavior. | Explicit active-route/field inventory; real production component edit, clear and focus cases for each field. |
| A11Y-STATE-01 | Disabled and read-only controls MUST retain their intended restrictions. A test adapter MUST NOT make them editable or bypass application policy. | Negative component cases, with callback and state assertions. |
| A11Y-SEC-01 | Password fields MUST remain masked in screenshots and OS/browser accessibility output by default. Visibility controls MUST have localized names and reflect state. | Native/browser masked-output checks and real component visibility/state tests. |
| A11Y-ACT-01 | Every interactive control type MUST expose its meaningful name, role, state and supported action. Navigation, dialogs, menus, lists, toggles and dropdowns MUST be independently operable. | Per-control-type action/state cases and representative real file/profile/dialog flows. |
| A11Y-FOCUS-01 | Editing MUST support selection, replacement, deletion, Unicode and focus transfer. Dialog dismissal/reopening MUST restore a usable state. | Positive/negative real-state component cases and native/browser observations; document supported focus recovery when a tool loses activation. |
| A11Y-EVID-01 | Coverage MUST distinguish production component semantics, actual OS/browser actions and external automation limitations. Static label assertions and replacement widgets do not prove interaction. | At least 24 distinct risk cases plus inventory-derived per-field and per-control-type cases; raw collected results retained privately and sanitized summary published. |

Wasm password verification MUST inspect both the visible semantic widget and the actual focused native editor in the host shadow root. The observed Compose 1.10.1 backing editor sets `inputmode=password` while leaving its native type as text; this exposes its buffer to accessibility despite the masked canvas widget. A focused Wasm adapter owns protection of this transient editor. It MUST preserve native input events, value, caret/selection and focus, handle late editor creation, and disconnect on disposal. Browser checks MUST exercise the real composed application rather than treating a desktop semantics password assertion as browser proof.

Web accessibility MUST remain available across successful login, logout, destination changes and dialog dismissal/reopening. A visible canvas with an empty semantic tree is a failed interaction state, even when component tests pass. The real application reproduced `Node … not found` in the Compose 1.10.1 semantics listener after normal login. The upstream fix is documented in [Compose 1.11.1](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.11.1) and [JetBrains PR #3065](https://github.com/JetBrains/compose-multiplatform-core/pull/3065). The frontend will use Compose 1.11.1 with a compatible stable Kotlin toolchain (2.3.20 or newer); root plugin declarations and the version catalog MUST agree. This dependency correction requires fresh desktop/Wasm contract execution, real packaged web/navigation evidence, native launch/editing and the existing Android compile gate. It MUST NOT be replaced by swallowing accessibility exceptions or by private framework state manipulation. Until that verification passes, the dependency change is proposed remediation rather than a resolved readiness claim.

The dependency migration MUST preserve the image-preview adapter ABI as well as text interaction. The initial compatibility run detected Coil 3.0.3 targeting Skiko 0.8.4 while Compose 1.11.1 resolves Skiko 0.144.6. The reviewed aligned combination is Coil 3.5.0, Compose 1.11.1 and Kotlin 2.4.0, matching [Coil’s release notes](https://coil-kt.github.io/coil/changelog/#350-june-10-2026). Existing Gradle 9.1/AGP 9.0 and minimum Android SDK 24 meet the documented compatibility limits. Real image decode and invalid-image outcomes MUST be verified with the resulting runtime; dependency warnings MUST NOT be hidden through force rules, exclusions or disabled checks. Frontend Wasm dependency locks MUST be regenerated through the supported upgrade task and reviewed.

The existing Koin compiler plugin 0.3.0 fails FIR extension registration on Kotlin 2.4.0, reproduced on JVM and Wasm. The migration therefore includes stable Koin 4.2.2 and compiler plugin 1.2.1, whose [published compatibility matrix](https://insert-koin.io/docs/setup/compiler-plugin/) explicitly verifies Kotlin 2.4.0. Compile safety remains enabled and the annotation-bearing storage module MUST compile on JVM and Wasm. Existing manual storage DSL definitions MUST continue to resolve through normal application composition and real login/files flows; compilation of the annotations does not prove that generated definitions are loaded at runtime. Unused UI injection dependencies in the files state module may be removed only after source/import and compile verification. The Wasm lockfile will be versioned through a narrow ignore exception; unrelated generated caches remain excluded.

Versioning the previously ignored Wasm lockfile exposed vulnerable transitive `ws` 8.18.0/8.18.3 entries in Trivy (CVE-2026-48779 and CVE-2026-45736). The root Wasm Yarn configuration MUST explicitly resolve this package to reviewed same-major patch 8.21.3 and regenerate its lock through `kotlinWasmUpgradeYarnLock`. This is a dependency upgrade, with unchanged scanner severity and lock mismatch failure behavior. Confirm the actual installed version, a bounded local WebSocket exchange and the regenerated browser contracts; the remote security check MUST pass before merge. References: [upstream advisories](https://github.com/websockets/ws/security) and [8.21.3 release](https://github.com/websockets/ws/releases/tag/8.21.3).

Actual compiled-browser verification of Compose 1.11.1 found its open shadow root on a nested generated DIV, rather than on `document.body`. The application MUST provide an explicitly identified viewport container to the supported `ComposeViewport` API. Password protection MUST resolve exactly one framework shadow host within that owned container, identify its canvas/accessibility signature, and reject missing or ambiguous hosts. It MUST NOT traverse or modify unrelated browser/interop shadow roots. Add real DOM cases for the nested layout, scope isolation, missing/ambiguous host rejection and normal bridge operation through the resolved host; repeat actual browser password typing and navigation after the correction.

The real owned-viewport run passed normal login and navigation, but closing Edit Profile removed the accessible tree while the canvas remained visible. Source inspection confirms Compose 1.11.1 and 1.12.2 store only one `SemanticsOwner`; removing a dialog's owner leaves no previous owner to restore. The official 1.13.0-beta01 source instead maintains multiple owners and invalidates on append/remove. A local candidate will evaluate this upstream correction with Kotlin 2.4.20, Koin 4.2.2/compiler 1.2.1, Coil 3.6.3 and the existing patched ws 8.21.3. This is an explicitly prerelease UI experiment, not an accepted production-readiness claim. Reference: [official beta release](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.13.0-beta01).

Coil 3.6.3's published Android AAR metadata requires compile SDK 37. The reviewed candidate therefore uses AGP 9.1.1 and Gradle 9.3.1, meeting the [official API/Gradle matrix](https://developer.android.com/build/releases/about-agp). Only compile SDK changes; minimum/target SDK behavior remains unchanged. Pin the official Gradle distribution checksum, provision platform 37 in application CI/Docker/local verification, retain strict security/coverage gates and test all APK variants. Do not suppress unsupported-SDK or compiler compatibility warnings. If the candidate passes, its prerelease status and rollback to the retained stable commit must be explicit in the PR and maintainer merge decision. No Copilot workflow, runtime activation or branch protection is changed.

Acceptance requires repeated open/edit/cancel/reopen of real dialogs, menu dismissal and continued navigation without an empty accessibility tree; secure native editors must remain protected in each exercised layer. Repeat full JVM/Wasm tests, package launch and Android compilation against the exact candidate. A passing dependency compile or the upstream source change alone does not close this requirement.

Accessibility belongs to shared presentation and platform composition; it MUST NOT introduce dependencies into domain policy or transport, nor production authentication/test backdoors. New helpers and suites MUST have focused responsibilities. Existing dormant duplicate screens are explicitly inventoried and MUST NOT be mistaken for active-route evidence. Any reactivated field becomes subject to the same contract.

The controlled investigation used identical application JARs: Corretto 17.0.9 exposed non-settable fields and did not accept automatic typing; JetBrains Runtime 21.0.11 exposed editable fields and completed Unicode paste, selection-based clearing, password masking and normal local login without human typing. Runtime version and vendor both differ, so this is evidence for the tested combination, not attribution to a particular upstream bug. Coordinate input remains unavailable in the automation tool; semantic actions are the supported verified path.

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
