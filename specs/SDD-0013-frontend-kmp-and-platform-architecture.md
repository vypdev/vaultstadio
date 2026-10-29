# Frontend KMP and Platform Architecture

- Status: As-built baseline
- Date: 2026-09-21
- Catalog capability ID: `frontend-kmp-and-platform-architecture`
- Last verified: `603d84a2f88297c9e7fd1b7143df3980fb21e24d` (2026-09-20 UTC)
- Owners: VaultStadio maintainers
- Scope: Define frontend domain/data/feature boundaries, navigation, state ownership, platform entrypoints, and user-facing capability mapping.
- Related issues/PRs: #71, #124, #116
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
| A11Y-LAYOUT-01 | Authentication fields and actions MUST fit the available horizontal viewport while retaining a bounded form width on larger screens. Preferred dimensions MUST respect parent constraints. | Measure unclipped production-control bounds on narrow and wide layouts, exercise editing/validation, and demonstrate that an overflowing layout fails the regression. |
| A11Y-EVID-01 | Coverage MUST distinguish production component semantics, actual OS/browser actions and external automation limitations. Static label assertions and replacement widgets do not prove interaction. | At least 24 distinct risk cases plus inventory-derived per-field and per-control-type cases; raw collected results retained privately and sanitized summary published. |

All 29 maintained field declarations MUST use the shared `:core:form` presentation boundary. Wasm renders real owned HTML input/textarea controls through public `HtmlElementView`; desktop/Android/iOS retain their Material/Basic Compose renderer. The adapter MUST preserve production callbacks, controlled value, selection, Unicode, IME, validation, disabled/read-only restrictions, localized names, placeholders, typography and disposal. No framework backing-editor mutation, swallowed rendering exception, token seeding or production authentication backdoor is permitted. Features depend on this presentation boundary; domain and transport acquire no accessibility dependency.

The nine secret declarations MUST default to native `type=password` on Wasm and masked rendering on other platforms. Explicit visibility controls MUST update native protection and their localized action name together. Actual native editing must update the production model; changing only a semantic mirror is failed evidence. DOM tests MUST exercise the owned control, rejection reconciliation, caret, IME, stale callbacks, Enter/default behavior, multiline sizing and listener/frame cleanup. Actual Compose widget tests MUST cover production-dialog editing, validation and repeated dismissal/reopening, plus protected input and multiline behavior.

Plain Enter in a multiline Web control MUST preserve native newline handling even when an IME callback is supplied. Only editable single-line controls may consume unmodified, non-composing Enter to invoke that callback. A composition session MUST prevent submission even when an individual keyboard event omits its composition flag. Web startup MUST replace the initial loading content with the initialized Compose viewport; verify the framework startup contract and the actual packaged page before inferring missing cleanup from host markup alone.

Web semantics MUST survive normal login/logout, destination changes, menus and repeated dialog dismissal/reopening. The current candidate uses the following reviewed pins; root declarations and the catalog MUST agree:

| Boundary | Current candidate | Acceptance/remaining limit |
|---|---|---|
| UI/compiler | Compose 1.13.0-beta01, Kotlin 2.4.20 | Multiple semantic owners restore the background after dialog dismissal. This is prerelease software; merge requires explicit maintainer risk acceptance. |
| Injection | Koin 4.2.2/compiler 1.2.1 | Compile safety stays enabled; annotation-bearing storage compiles on JVM/Wasm and normal app wiring/login must work. |
| Images | Coil 3.6.3; resolved Skiko 0.156.0 | Coil targets Skiko 0.150.1: warning remains visible. Actual valid PNG and invalid-byte pipeline cases pass; other image formats are not certified. No force/exclusion/warning suppression. |
| Android/build | compile SDK 37, AGP 9.1.1, Gradle 9.3.1 with official distribution SHA | All selected debug APK variants must compile. Minimum/target SDK behavior is unchanged. Provision the required SDK; compile is not device execution. |
| Network Wasm loader | kotlinx-io 0.9.1 explicitly at network boundary | Independent data/auth modules must load and pass, not rely on an accidental app-level newer transitive dependency. |
| WebSocket test tooling | ws 8.21.3, supported regenerated Wasm lock | Installed version, bounded fragmented-Unicode/invalid-close/valid-close loopback contracts, root browser suites and unchanged scanner severity must pass. |
| Native packaging | JetBrains Runtime 21.0.11, JVM bytecode17 | Check exact vendor/version before packaging. Compare app JAR identity and exercise real automatic input/login with isolated preferences. |

These pins follow the [official Compose beta release](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.13.0-beta01), [Koin compatibility matrix](https://insert-koin.io/docs/setup/compiler-plugin/), [Coil changelog](https://coil-kt.github.io/coil/changelog/), [Android compatibility matrix](https://developer.android.com/build/releases/about-agp), [kotlinx-io loader change](https://github.com/Kotlin/kotlinx-io/blob/master/CHANGELOG.md#091) and [ws security advisories](https://github.com/websockets/ws/security). The investigated stable Compose versions fixed the earlier node-ID error but lost semantic owners after dialog close; the beta's framework editor then failed privacy/selection. The owned native controls and multiple-owner upstream implementation are both required for this candidate. The initial compatibility observer and its 30 cases are retired, preserved in Git history, and excluded from current counts. Keep an explicitly owned ComposeViewport; no private framework state may be changed. Historical failures, exact results and rollback identities belong in the verification report.

The inventory MUST distinguish maintained declarations from routed journeys: the three retained `profile.*password` dialog fields have no current caller, while the dedicated Change Password screen is routed. They retain component tests but are not claimed as routed GUI proof. Dormant duplicate screens outside maintained feature trees are separate architecture debt; any reactivated field acquires this contract. Inventory consistency alone does not prove runtime reachability.

Application CI MUST execute complete root desktop and Wasm test selections, including pure resource/auth/network and form-model suites. The affected Compose modules must use supported executable Wasm test configuration so Skiko is bundled, without disabling prerequisites or dropping suites. The app browser runner MUST allow the existing bounded 45-second coroutine contract to complete; a 60-second outer Mocha deadline may replace its conflicting default two-second deadline. Retain inner ten-second state waits, every assertion and failure propagation; never disable timeouts or convert late failures into success. Record actual timings and recheck a deliberately failing asynchronous assertion. Chrome provisioning MUST use an execution-owned tool cache without deleting/repairing another job's shared cache; installation failure remains a failed check. GitHub ignores workflow overrides of reserved RUNNER_* variables, so assigning RUNNER_TOOL_CACHE through GITHUB_ENV does not establish isolation. Use the official Chrome for Testing Headless Shell155.0.8059.84 artifact in an execution-owned directory, with a reviewed archive SHA256 for each supported macOS architecture. Use the native macOS HTTPS client with its normal OS trust store when the runner Python CA configuration is incomplete. TLS verification and HTTPS-only redirects MUST remain enabled; never change the global trust store or use insecure download flags. Verify the full archive before safe bounded extraction, preserve required executable modes, reject traversal/symlink/duplicate entries and verify the actual executable/version and directory containment before publishing its path. Download once and reuse that path for all browser steps. This CI runtime is distinct from actual GUI dogfooding; full Chrome widget timeouts remain rejected evidence rather than silent successes. Preserve integrity, timeout and negative-input controls; do not mutate the shared cache or skip browser contracts. The Wasm-only Docker build must satisfy its own prerequisites.

Publish separate coverage for all compiled JVM form classes, combining actual app component execution and model-suite execution. Missing execution data or classes MUST fail closed. Preserve the existing core-UI helper gate and full audit production denominator. DOM/widget case counts are not JVM line coverage; compiler changes to branch denominators MUST be disclosed. Keep strict locks, scanner severity, configuration-cache validation, Copilot, branch protections and runtime-setup activation unchanged. Shared iOS sources must be wired and compile on the selected Apple targets without claiming whole-app iOS readiness.

Coverage publication MUST verify the uploader's signature and checksum before executing it. Use an execution-owned private GPG home rather than the runner's shared keyring; never repair its locks, kill shared agents or disable verification. Pin the reviewed uploader version and enable failure propagation for import, signature, checksum and upload errors. A successful upload after a reported verification failure is rejected evidence. Verify isolated directory permissions and noninterference, actual official signed artifacts, and negative controls that stop execution after validation fails.

Acceptance requires representative real packaged Web and native login, Unicode selection/replacement/clear with model validation, folder persistence, menu dismissal and repeated dialog reopening without reload recovery. Native/browser secret output must remain protected in the exercised paths. Same-JAR Corretto17/JBR21 investigation supports the tested vendor/version combination; both changed, so no isolated upstream cause is claimed. Automation's app-selector timeout and occasional capture failures remain tool limitations, distinct from field-editing success. Other operating systems and business journeys require their own evidence.

For #80, [frontend accessibility verification](../docs/development/verification/FRONTEND_ACCESSIBILITY_80.md) and its companion JSON record 29 declaration contracts, actual Wasm controls/widgets, complete desktop/browser selections, exact package identities and assistant-operated local Web/native journeys. The candidate has passed those local checks, while prerelease merge acceptance, remote CI/review and broader platform readiness remain separate decisions.

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

### 6.4 Client startup correction — issue #76

This is an implementation contract for R03's confirmed startup blockers, not an assertion that every platform capability is operational.

| ID | Required behavior | Acceptance/evidence |
|---|---|---|
| BOOT-SER-01 | The actual production `HttpClientFactory` Json/contextual module MUST initialize with the current dependencies without a descriptor-name collision. Custom and built-in Instant serialization MUST coexist. | Production-factory DTO tests on desktop JVM and Wasm; actual Koin app startup. |
| BOOT-SER-02 | Contextual Instant MUST retain ISO-8601 string encoding, UTC normalization and nanosecond precision, including nested and nullable DTOs. Invalid or missing required values MUST fail rather than silently become valid timestamps. | Wire-level round trips and malformed/type/required-field cases using the production Json instance. |
| BOOT-HTTP-01 | A successful `.wasm` response from the actual Nginx configuration MUST contain exactly one Content-Type value, `application/wasm`. A missing Wasm asset MUST return 404 rather than successful SPA HTML. | Real Nginx GET/HEAD/missing-asset/SPA fallback regression; real built web artifact in a browser. |
| BOOT-UI-01 | Fresh web and desktop app starts MUST reach a visible usable login surface without serialization/Koin initialization failure. Local login MUST be exercised against a disposable backend account. | Browser/native GUI evidence and logs; HTTP 200 HTML alone does not satisfy acceptance. |
| BOOT-PKG-01 | Desktop packaging MUST succeed from versioned inputs. Missing optional branding assets MUST NOT be configured as mandatory files; the platform packaging default icon is acceptable until real assets are versioned. | Execute `:composeApp:createDistributable` and launch the generated application on this machine. Other desktop OS packages remain unverified until built and launched there. |
| BOOT-ISO-01 | Verification MUST isolate browser storage, desktop preferences and backend test data. No production credential or real user's preferences may be replaced. | Dedicated local origin, in-memory/temporary desktop preferences, disposable account/database/content. |

Serialization stays in the data/network adapter; domain/features gain no Ktor or serialization workaround. Nginx owns transport MIME mapping. Existing dependencies, Copilot configuration and wire schemas are preserved. If a subsequent URL/feature failure is uncovered, record and fix it under its owning contract rather than claim the full platform is ready.

### 6.5 Complete platform verification — issue #84

The default frontend verification MUST execute every configured desktop JVM and Wasm browser module, including pure domain/resource/auth/network and form suites. Discover expected tasks from the live Gradle model and compare results to that inventory; do not restrict execution to `composeApp`. New modules using supported targets MUST enter without editing a module allowlist. Additional target conventions MUST be discovered and either supported explicitly or rejected with an actionable scope diagnostic.

At the pre-implementation source `816e6732b7fd605a2852745420ad7d2c75bf1474`, configuration-only inventory declares 52 desktop and 52 Wasm browser test tasks, plus 53 iOS simulator and three Android flavor unit tasks. These are declared tasks, not executed case counts. Default JVM/Wasm execution MUST NOT certify Android/iOS compilation, device integration or whole-app readiness; their actual verification remains separately stated.

R04-FAIL-01 requires original browser/compiler/runtime diagnostics and final nonzero status after any failed applicable suite, including asynchronous browser assertions. Independent suites may continue collecting evidence, but the final outcome MUST remain failed. R04-COV-01/02 require all current JVM main compilation classes/source sets against matching cross-module execution data, including untested modules. Wasm case counts are not JVM line/branch coverage. Preserve the existing serializer, input helper and form coverage input/gate contracts. The owning freshness, counting, CI and risk-derived test budget are [SDD-0014 §6.8 and §14](SDD-0014-deployment-operations-and-release.md#68-complete-verification-and-truthful-evidence--issue-84).

R04 verification infrastructure was adopted on develop through PR #118; its local, final CI and post-merge evidence are recorded in [the verification report](../docs/development/verification/COMPLETE_VERIFICATION_84.md). Current critical behavioral coverage continues in [issue #119](../docs/development/verification/CRITICAL_COVERAGE_119.md), with its own final-ref CI/review/adoption steps. JVM, Wasm and Python populations, mobile limitations and whole-platform readiness remain separate.

### 6.6 Critical client transport and file-state coverage — issue #119

Pre-implementation contract (2026-10-27), CLIENT119-TRANSPORT/FILES: exercise BaseApi/ApiResult, StorageApi and the production HttpClient configuration through a deterministic injected engine while keeping the ordinary platform engine as the default. The seam belongs to the data/composition boundary; domain and feature ports gain no Ktor imports. Preserve token lookup per request, serialization, exact request/byte shape, safe structured/fallback errors and coroutine cancellation. A successful envelope with null data is a valid Unit result only when Unit is the requested result; a missing required typed payload is an error.

FilesLoader/FilesViewModel tests use controllable domain ports and a per-test coroutine scheduler. Exercise modes, paging, breadcrumbs/path resolution, preferences, selection, mutations, activity/share state, failure and retry. Only the current folder/mode/sort request may publish contents/counts/progress; cancellation or stale pagination must not write into a newer navigation. A failed mutation must preserve a visible error and truthful retained state; successful background refresh must not erase the failure before explicit recovery. Keep independent state/transport fixtures focused; avoid a broad feature rewrite.

A batch with failed items is a partial result even when its transport/result wrapper is successful. Keep the known failed IDs selected for an explicit retry; if the failure details are incomplete, retain the original selection and report the uncertainty rather than guessing which effects completed. Do not retry automatically or clear selection as though all items succeeded. A late activity result cannot repopulate a closed/different info panel; the selected item's star state follows the returned server item.

NET119-01–24 and FILE119-01–32 in [the scenario inventory](../docs/development/verification/critical-coverage-119-scenarios.json) are acceptance risks, not a claim of executed cases. Run applicable shared contracts on desktop and Wasm; disclose repeated executions. This does not close native file-picker/download, search-response ordering or mobile readiness issues.

Multipart transfers preserve the actual Content-Type/boundary, original bytes and names through the ordinary engine, including folder uploads. Progress is bounded by actual transfer work; preparing a folder body must not announce completed transfer. A missing optional token must not create a literal `Bearer null` credential. Verify both the deterministic engine contracts and an isolated loopback HTTP exchange for the default desktop engine.

BaseApi's reified wrappers supply serialization information while one shared execution path owns transport failure, cancellation and response/error mapping. Apply the Unit-versus-required-payload rule consistently to no-body, body and form commands. Measure ordinary executed behavior without exclusions or reflective invocation of inline implementation stubs. A default-preserving logger seam may capture the actual HTTP plugin output; bearer headers and public-share capability paths must not reveal credentials. Exercise an actual configured timeout and body-decoding cancellation independently from an ordinary connection failure.

A shared data-network cancellation classifier owns caller-context and wrapped-cause handling for BaseApi and every manual multipart/binary StorageApi adapter. The shared policy must be executable on JVM and Wasm and preserve Ktor transport deadlines as network errors. Controlled incoming-body cancellation tests must cover all four manual adapters, rather than only engine request entry. The production engine, domain ports and public response schemas remain unchanged.

### 6.7 Focused authenticated downloads — issue #78

Pre-code contract on Action branch #78 (baseline `15574b9`). Details, grid menu, list menu and
selection-ZIP controls MUST dispatch the same focused download operation, not compute an unused
URL. Folders dispatch their bounded recursive archive. Keep FilesContent/FilesViewModel from
growing: application/feature state owns ordering, busy/error/cancel/retry; domain owns neutral
transfer/save ports and bounds; data owns Ktor/bytes/auth; composition owns browser/AWT/filesystem.
No Ktor, browser, AWT, file or DI policy is added to domain decisions. Existing default engines,
normal login and current session token lookup remain in use; no seeded authentication backdoor.

Success preserves valid original Unicode names and bytes, including empty content. Reject unsafe
or ambiguous save/archive path names instead of interpreting them as local paths. Credential-free
URL helpers cannot contain tokens or duplicate the configured API prefix; authenticated downloads
use the header-bearing data operation exclusively. Show localized bounded impact/recovery text,
not raw provider exceptions, private paths, content or session identifiers. The operation must
retain a recoverable terminal outcome and release busy state after failure/cancel; screen/session
disposal cancels owned work before later bytes can reach a save callback.

Web uses a binary Blob/object URL rather than a base64 data URL or external navigation, removing
owned anchors and revoking URLs/timers after handoff, failure and disposal with a bounded grace
period. Desktop uses a real native save chooser with explicit saved/canceled/failure outcomes and
atomic owned-file cleanup. Existing Android/iOS placeholders must not claim success: unsupported
saving returns an explicit capability outcome; no device readiness is inferred. The existing
profile-export callback remains compatible with the platform boundary.

Use actual production callbacks/component tests and a normal-login production package for the
browser binary/UTF-8/empty upload-download and ZIP byte/hash journeys. Native chooser acceptance
requires real interaction on this machine as well as deterministic filesystem tests. Preserve the
current dataset, both detached SDD drafts, existing package and runnable rollback before installing
a candidate. See SDD-0002 section6.6, SDD-0005 section6.4 and the identified transfer plan. The wider
preview/search/origin/mobile portfolio remains separately tracked; this does not certify it.

Post-review CI harness clarification before configuration repair (2026-10-28): the storage-data
Wasm suite must allow its existing coroutine contracts a finite60-second outer Mocha budget,
using the repository's module-local Karma snippet mechanism. The exact32MiB binary-boundary
assertion remains unchanged and is not a two-second latency benchmark. Existing explicit
transport-deadline/cancellation assertions remain authoritative; do not reduce the payload,
skip the case or disable browser deadlines to recover a green run. This only changes the
storage test harness, not production request timeouts or the supported resource bounds.

### 6.8 Focused directory picker and preparation — issue #121

Pre-implementation contract (2026-10-28), FOLDER121, on the Action-linked issue121 branch
from develop `95d2adb`. Pure upload-domain policy owns relative paths, entry collisions and
the fixed preparation bounds in SDD-0005 section6.5. A focused application reader owns
validation-before-read, sequential byte reconciliation and cancellation. Browser adapters
own public directory capability detection, input lifecycle, `webkitRelativePath`, Blob/FileReader
I/O and cleanup. Presentation owns busy/cancel/failure/retry and the selected-parent snapshot.
Keep DOM/Compose/Ktor/filesystem types out of the domain policy and file-source port.

Decompose the existing Wasm file-picker implementation into focused input, byte-reader and
selection adapters, with thin expect/actual entrypoints. Do not enlarge FilesContent,
FilesViewModel or the old picker with directory orchestration. A shared browser input/reader
may serve ordinary and large-file pickers only with explicit byte, cancellation, threshold,
source-shape and cleanup regressions; preserve their existing data/streaming distinction.

An input belongs to one operation. Install listeners before activation, handle real change
and cancel events, settle once, and remove owned inputs/listeners on completion, failure and
disposal. Abort an active FileReader after detaching its handlers on cancellation; stale or
duplicate callbacks cannot resume twice or publish files. Expected DOM/read failures become
bounded semantic outcomes; cancellation and unexpected fatal errors retain their meaning.
Never hide a failure as empty bytes or report a canceled selection as successful upload.

Folder preparation has an observable active state and cancel action. Duplicate activation
cannot dispatch another chooser/read, screen/session disposal cancels owned work, and a late
read cannot enqueue into a new route. Feedback is localized and avoids private paths, content,
tokens and provider exception messages. Explain the empty-directory limitation and bounded
folder selection where it helps the user choose the appropriate action. Unsupported pickers
return an explicit result rather than a misleading ordinary-file fallback.

Tests MUST exercise real browser File/Blob/FileReader and owned DOM input behavior as well
as controllable domain ports. Distinguish controlled DOM tests from a real OS directory
chooser and actual packaged upload/list/download verification. Native/mobile readiness stays
separate; preserve their existing entrypoints and record the exercised scope precisely.
Keep Copilot, branch protections, workflow cadence, dependency pins and setup activation intact.
The risk-derived floor and real journeys are in the
[pre-code inventory](../docs/development/verification/folder-selection-121-plan.json).

Pre-code refinement after actual Web acceptance on `8320a43` (2026-10-28),
FOLDER121-LAYER: the native HTML search input visibly covers the upload popup and its
new folder hint. Green6,559-case execution does not satisfy this rendered acceptance.
While an owned Files upload/sort popup is open, suspend the background search control
and reserve its measured layout space. Preserve its controlled query and restore a
real editable control on dismissal; release detached DOM listeners rather than leave
an invisible hit target over the popup. Keep this policy in the owning presentation,
using public component lifecycle; no framework-parent style or private editor mutation.
Render folder preparation/error feedback in normal Files layout flow so it cannot
cover the native search input. Add eight risk families for popup occlusion, repeated
query restoration, focused editing after dismissal, stale-listener cleanup, measured
layout at different font scales, inline feedback geometry and actual menu operation.
Retain the original overlap screenshot and separately verify the corrected package.

### 6.8.1 Review refinement: file-picker failure language — issue #121

Pre-code review refinement (2026-10-28), FOLDER121-COPY: preserve the originating upload action through safe problem delivery. Ordinary-file and large-file picker failures MUST use file-selection language. Unsupported file selection MUST NOT claim missing directory support; file preparation limits MUST NOT advertise the separate32MiB/256-entry/16-level folder contract. Read failures identify selected files, confirm that no selection was added and permit explicit selection again. Folder operations retain their existing localized capability/path/budget/read messages. Raw provider diagnostics are never presented; cancellation and current-action cleanup retain their existing behavior.

Keep the action-aware presentation mapping pure and separate from picker/platform I/O. Maintain all seven locales. Add at least8 meaningful deterministic routing/copy cases, including ordinary/large read failure, ordinary/large capability refusal, ordinary/large non-folder bounds, unchanged folder mapping and maintained-language coverage. Preserve original assertion-red and green evidence, then repeat complete canonical suites, coverage, gates and lint on the resulting source. Previously verified native positive/cancel journeys keep their actual package/source identity; this refinement does not relabel old observations as a new build.

### 6.8.2 Review refinement: desktop chooser thread ownership — issue #121

Pre-code review refinement (2026-10-28), FOLDER121-DESKTOP: the desktop availability adapter exposes the existing Swing folder chooser. Construct, display and dispose that chooser exclusively on the actual AWT event dispatch thread. Return only the approved directory across that boundary; recursive file enumeration and reads remain on Dispatchers.IO. Keep those platform responsibilities in focused collaborators, with no Swing or filesystem type in the domain/controller.

The operation owns its dialog: pre-dispatch cancellation must not construct it; cancellation of an active modal dialog schedules disposal on its owning event queue and cannot publish a late selection. Expected native cancellation returns no selection. Factory, selection and disposal exceptions must reach the suspended caller safely, without escaping the event-queue callback or claiming a chosen directory. Preserve the existing desktop enumeration/path/size behavior in this thread-ownership repair; broader native hierarchy/resource/symlink acceptance remains separate from the Web contract.

Add at least8 meaningful desktop cases using the real EventQueue and controlled dialogs: thread ownership and chosen result, native cancel/no read, factory failure, selection failure/disposal, disposal failure, cancellation before dispatch, active modal cancellation/late-result rejection and filesystem I/O outside the UI thread. Preserve assertion-red and green originals, repeat complete suites/coverage/gates/lint, and keep the prior Web/native observations tied to their actual sources. A display-independent controlled dialog test does not certify an actual OS chooser.

### 6.8.3 Review refinement: bounded native folder preparation — issue #121

Pre-code review refinement (2026-10-28), FOLDER121-NATIVE: move native validation ahead of body reads. The desktop enumerator MUST use a lazy no-follow filesystem traversal on IO, with cancellation checks and a cap of256 visited entries including the selected root and empty directories. Stop beyond16 relative path segments. Preserve legacy desktop root-relative paths; the shared pure policy already supports requireRoot=false. Validate every accumulated metadata snapshot through that policy and validate the complete selection before creating body reads. Keep browser requireRoot=true as the default.

Use the existing application reader for sequential, atomic, cancellable body reconciliation, with an explicit root-policy option. Desktop folder selection uses the same32MiB aggregate declared/actual bound; replace silent skipping of files above the ordinary-file threshold or unreadable entries with a typed whole-selection failure. Do not change ordinary/large-file picker thresholds or chunking. Read at most snapshotted size plus1 byte per accepted source so a growing file cannot allocate an unbounded buffer. A truncated/growing/unreadable file cannot publish earlier files. Do not follow symlink entries/root; refuse a swapped leaf link at open. Keep physical filesystem types and traversal in focused platform collaborators, neutral metadata/policy in domain and orchestration in the application reader.

Add at least12 native risk families: real basic binary/Unicode/empty reads, exact32MiB aggregate bytes, aggregate plus1, single oversize refusal, exact visited-entry bound, visited-entry plus1, exact depth, depth plus1, static/swapped symlink refusal, read failure without partial output, changed byte lengths and cancellation before later reads. Assert no body callback for metadata/resource refusal. Keep actual on-disk IO and controlled callback evidence distinct from a physical chooser; broader native OS/UI readiness and parent-directory race guarantees are not claimed. Preserve red/green originals, rerun full suites/coverage/gates/lint and retain previous sources and failed CI evidence.

### 6.9 Accepted product discovery — issue #124 (2026-10-29)

These are **accepted product targets, proposed for implementation**. They do not change the observed baseline, catalog status or last-verified evidence. The [discovery index](discovery/2026-10-29/README.md) retains the source answers and cross-capability dependencies; this SDD owns the requirements below.

<a id="pd-019"></a>

#### PD-019: Functional parity with explicit platform limits

The product MUST target the same capabilities on Web, Windows, macOS, Linux, Android and iOS, adapting interaction and documenting technical platform limits explicitly. A compiled module, shared test or emulated callback is not proof of real platform functionality.

**Blocking technical decisions:** Supported version matrix, background/offline behavior, filesystem integration, accessibility, mobile/device campaigns and unavoidable restrictions.

### 6.10 Web API origin contract — issue #116

Pre-implementation contract at develop `7b52127` (2026-10-29). This implements only the fixed-primary slice of PD-001; multi-server product targets remain future milestones.

API116-BOOT: load `/vaultstadio-config.json` before Koin, API clients or the application root. The packaged default is `{}`; only the optional `apiBaseUrl` string is accepted. Unknown keys, null/non-string/empty values, invalid JSON/URI and unsafe transport are failures, never an absent-value fallback. URL/page query/hash, mutable global JavaScript and normal user preferences are not configuration sources. Capture one validated startup string and pass it to runtime modules and application configuration.

API116-LOAD: the platform reader requests the same-origin static resource with no-store, omitted credentials and redirect refusal; require successful JSON content, bound the streamed response to16KiB and the whole fetch/read to5seconds. UTF-8 decoding errors, non-JSON/SPA fallback, missing/failed response, timeout and excess bytes produce a safe load failure. Cancellation releases the reader/timer/abort resources and cannot launch late. No credential/config value enters error text or logs.

API116-UX: invalid/unavailable configuration replaces the loader with an accessible native error/status and named Reload control. Explain impact and operator correction without echoing the supplied URL or secrets. Valid configuration launches once; invalid/load-failed/canceled work causes zero API/Koin effects. Correcting the served config and reloading recovers through the normal login. Presentation/media URL consumers use inward-facing configuration/storage URL ports; remove divergent raw-origin concatenation. Keep existing authentic input, normal authentication, file-selection and transfer behavior.

API116-BOUNDARY: the two-origin packaged Web campaign includes login, folder/sort, error/retry, actual upload/saved binary or ZIP bytes, preview/location behavior and logged-out public-link flow. Separate component/MockEngine/controlled-stream cases from actual browser evidence. Desktop/common suites preserve configuration compatibility; mobile/hardware parity is not inferred.

API116-MEDIA: private image fetching must reuse the configured API client and its current scoped bearer, rather than Coil's anonymous default client. Disable unscoped private image memory/disk caches; the no-store image request remains subject to explicit server CORS policy. Text previews use the existing domain download port instead of creating a transport client in presentation; preserve their existing100KiB displayed-text cap, cancellation and safe failures. This URL/auth repair does not certify bounded full-file preview buffering or implement the separate unsupported native media players.

API116-CORS: preserve the configured HTTP versus HTTPS scheme in each explicitly allowed backend origin; do not broaden the origin list. Support the Cache-Control header emitted by uncached private-image requests, retaining all other origin/method/credential restrictions. Verify configured HTTPS acceptance, corresponding unconfigured HTTP rejection, other-host/port denial and actual preflight behavior.

## 7. User-facing configuration

API base URL, platform environment, locale, feature enablement, and auth persistence are validated at platform/composition boundaries.

Configuration MUST define type, recommended default, allowed values, validation, precedence, persistence/snapshot behavior, migration for unknown or retired values, and values that are intentionally not configurable. A configuration change MUST update the relevant docs and contract tests.

### Accepted target configuration (#124)

| Requirement | Authority, default and inheritance |
|---|---|
| PD-019 | Capability availability is reported per platform/build, with supported OS/browser versions and unavoidable limitations. No platform was accepted as feature-complete by this decision. |

These are product-policy semantics, not new environment variable/API field names. Before implementation, define typed validation/bounds, persistence, effective policy revision for pending work and migration/rollback. An override cannot bypass content authorization, E2EE boundaries or protected retention. Unknown/invalid policy is not permission to select a weaker default.

### Web API configuration (#116)

| Source/field | Type/default | Validation and precedence |
|---|---|---|
| `/vaultstadio-config.json` | UTF-8 JSON object, default `{}` | Same-origin static resource, successful JSON MIME, no redirects/credentials/cache;16KiB and5second total load/read budget. |
| `apiBaseUrl` | Optional string; absent uses page origin + `/api` | Absolute HTTP(S), ≤2048 UTF-16 units, safe authority/path, no userinfo/query/fragment or HTTPS downgrade. Explicit invalid/null/unknown fields fail. |
| Effective configuration | One immutable startup value | Operator resource is the sole override; no user/query/hash/global precedence. Fix resource and reload for recovery. |

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

For #76 the additional floor is 12 independently collected serialization cases, run on JVM and Wasm and reported as repeated platform executions; cover factory initialization, built-in coexistence, epoch/pre-epoch, offset/nanoseconds, nested/nullable values, unknown fields and malformed/missing/wrong-type values. Add four real Nginx checks (GET, HEAD, missing Wasm, SPA route) and actual web/desktop startup/login evidence. A test that only asserts a descriptor constant does not establish startup. Record the exact built artifact, source revision, commands, collected cases and remaining limitations.

For #124, add at least **3 future acceptance families** (1 decisions × positive, denied/invalid and failure/recovery). The concrete families are listed in §16; they are **planned, not executed tests**. Expand actor, policy, boundary, concurrency, platform and cryptographic matrices before the affected implementation is marked ready. Preserve existing regression budgets.

For #116, the pre-code [38-family risk inventory](../docs/development/verification/api-origin-116-plan.json) is a floor, not a test count. Expand grammar, API consumer, request/body/auth, generated URL, prefix, deadline/cancel and two-origin browser matrices; retain source-bound red/green and failure evidence. Existing regression budgets and global denominators/gates remain unchanged.

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

### Planned discovery acceptance (#124)

- **PD-019-P** — Run each supported product journey on all six targets with actual package/device identities, adapted accessible interaction and independent retained evidence.
- **PD-019-N** — Unsupported or unverified platform actions cannot present successful completion or borrow another platform's acceptance evidence.
- **PD-019-F** — Exercise native cancel, permissions, background interruption and offline recovery per target; retain user data and explicit actionable limitations.

### Web origin acceptance (#116)

- API116-01–13: controlled valid/invalid/default/prefixed/IP/port inputs and actual invalid-config screen produce the exact effective root or safe failure; invalid/canceled startup launches no Koin/API and operator correction/reload recovers.
- API116-14–25: production request shapes, fresh scoped bearer, binary/multipart bytes and canonical encoded location consumers retain methods/query/results/cancellation without prefix duplication, root loss, token URL or capability log leakage.
- API116-26–32: GET/denied-POST public-page assets/action, valid/invalid path parser, privacy and development/common compatibility pass real adapters and focused controls.
- API116-33–36: one source-identified production Web package passes two serving-origin campaigns with normal disposable login, actual saved bytes/public links, and malformed config/no-API/reload recovery. Component or mocked transport assertions alone do not satisfy these journeys.

## 17. Requirements traceability

| Requirement | Owner | Verification target | Documentation |
|---|---|---|---|
| Fixed invariants | Domain/application policy | focused policy and architecture tests | this SDD |
| Public behavior | Use case and adapter | route/protocol/integration tests | API/frontend/operations reference |
| Failure and recovery | Application outcome policy | partial, retry, cancellation, and cleanup tests | troubleshooting/recovery guide |
| Security/privacy | boundary policy and composition | abuse, secret-safe, and permission tests | security guide and threat model |
| Operational evidence | workflow/deployment adapter | render, health, artifact, and rollback checks | operations runbook |

For #76, executed evidence is recorded in [frontend startup verification](../docs/development/verification/FRONTEND_STARTUP_76.md) and its companion JSON: BOOT-SER-01/02 → 15 production-factory cases on JVM and Wasm; BOOT-HTTP-01 → 4 actual Nginx checks plus built-artifact browser load; BOOT-PKG-01 → generated and launched macOS distributable. BOOT-UI-01 is verified for the exercised local startup/login path: browser login, folder navigation and upload passed; the maintainer entered the disposable desktop credentials, then the assistant submitted and verified native HTTP 200, My Files and folder/file navigation. Autonomous text entry is separately unresolved under #80. BOOT-ISO-01 → dedicated browser origin, disposable backend volumes/account and confirmed in-memory desktop preferences. These results do not certify other platforms or complete feature readiness.

Executed #78 evidence is recorded in [authenticated transfer acceptance](../docs/development/verification/AUTHENTICATED_TRANSFERS_78.md), the 72-family risk matrix and separate GUI receipts. Clean source `ae5f22a` passed 6,386 product executions including review/CI refinements and all 22 archive path-policy branches; real Web/macOS observations retain their actual client/server sources. Earlier 6,355 acceptance and independent failures remain historical evidence. Transfer bounds, Web directory selection (#121), Web origin (#116), mobile hardware and final-ref CI/review/adoption retain separate acceptance.


Executed #121 evidence is recorded in [Web directory selection verification](../docs/development/verification/FOLDER_SELECTION_121.md), its96-family risk matrix and separate GUI receipts. Source `d3816d6` passes6,606 complete product executions with zero failures/errors/skips and retains full44/52 module populations. Domain path/budget policy covers33/33lines and48/48branches; actual browser reads check every byte at the32MiB bound. All9 rendered journeys pass within their recorded scope, including separate physical Chrome/macOS directory selection, native Escape cancellation with no mutation, three-file confirmation/upload and a saved binary round trip. Initial browser-chooser API and native journeys retain their own mechanism and earlier sources. The action-specific error mapper covers38/38lines and12/12branches; a real four-byte unreadable-file selection on package4d3fb74 shows file-specific feedback and leaves all13 files/8 folders unchanged. The desktop-only follow-up moves Swing chooser ownership to EventQueue, retains filesystem reads on IO and passes9 controlled desktop cases plus real native login/application-cancel smoke without mutation. The subsequent native resource repair validates bounded metadata before reads, caps actual reads at snapshot+1 and rejects changing/unreadable/link sources atomically, with12 filesystem cases and48 focused controls plus the complete campaign. Native legacy relative paths and browser selected-root semantics remain distinct. Physical positive Swing selection is not certified. Final-ref CI, review and explicit adoption remain pending; no whole-platform readiness claim is made.

### Discovery traceability (#124)

| Requirement owner in §6 | Planned acceptance in §16 | Implementation/evidence state | Milestone |
|---|---|---|---|
| [PD-019](#pd-019) | PD-019-P / -N / -F | Accepted target; implementation and current execution evidence still required | [M8](../docs/development/PRODUCT_ROADMAP_2026-10-29.md#m8) |

| #116 contract | Implementation owner | Verification/evidence target |
|---|---|---|
| Startup grammar, load and safe failure | data/platform reader and composition root | API116-01–13/36; pure configuration, browser stream/lifecycle and real startup/recovery |
| API root, trust, prefix and generated locations | shared network/data URL adapters, domain URL ports | API116-14–25; request/consumer/byte/privacy matrices and complete suites |
| Public-page and operator/development behavior | bounded route/static/platform adapters | API116-26–35; Node/real route/proxy/assets plus actual two-origin browser evidence |

## 18. Implementation or maintenance sequence

Inventory current modules -> classify legacy docs -> publish baseline -> add dependency/state checks -> reconcile feature/API docs -> validate platform claims.

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

- `frontend/settings.gradle.kts`
- `frontend/composeApp/src/main/kotlin/com/vaultstadio/app/VaultStadioRoot.kt`
- `docs/architecture/FRONTEND_ARCHITECTURE.md`

- Portfolio governance: `SDD-0000-architecture-quality-and-product-contract.md`.
- Related capability contracts are listed in `specs/catalog.json`.

Discovery provenance and ownership: [30 accepted decisions](discovery/2026-10-29/README.md), [verbatim answers](discovery/2026-10-29/answers.json), [issue #124](https://github.com/vypdev/vaultstadio/issues/124).


Issue #116 clipboard refinement is specified before code in SDD-0006 §21.11. Clipboard IO stays in
platform adapters behind a neutral domain output port, wired by the application composition root.
Shares presentation depends on that port and generated-link use case, and confirms copy only after
acknowledgment. It must not import browser, AWT, Android or UIKit APIs or infer copy success from
opening a dialog. Existing Copilot configuration, generated guides and workflow policy are unchanged.


## Scoped local verification checkpoint — issue #116

The fixed operator Web API origin, prefix-safe consumers and acknowledged public-link clipboard contract are implemented at `35ce407`. See the [executed report](../docs/development/verification/WEB_API_ORIGIN_116.md) and [38-family traceability](../docs/development/verification/api-origin-116-results.json): 3,397 backend + 1,729 desktop + 1,636 browser executions, zero failures/errors/skips, source-bound JVM coverage, real two-origin packaged transfers/public pages, clipboard-value comparison and invalid-config/no-API/reload recovery. Native mobile compilation and remaining platform/media/version/collaboration gaps are explicitly separated. Final-ref PR CI, Copilot review, protected adoption and master remain separate gates; this does not promote the whole capability status.
