# Test Coverage Action Plan

This document defines a phased plan to achieve full test coverage across VaultStadio. It complements [TESTING.md](TESTING.md) (strategy, running tests, untestable components) and aligns with [CODE_QUALITY.md](CODE_QUALITY.md) and [AI_CODING_GUIDELINES.md](AI_CODING_GUIDELINES.md).

**Last updated**: 2026-02-18

---

## Table of Contents

1. [Goals and Targets](#goals-and-targets)
2. [Current State Summary](#current-state-summary)
3. [Gap Analysis by Module](#gap-analysis-by-module)
4. [Phased Action Plan](#phased-action-plan)
5. [CI and Quality Gates](#ci-and-quality-gates)
6. [Success Criteria](#success-criteria)

---

## Goals and Targets

| Goal | Target |
|------|--------|
| **Line coverage (backend)** | ≥ 80% per module (core, api, infrastructure, plugins-api, each plugin) |
| **Branch coverage (critical paths)** | Auth, storage CRUD, share, sync, and versioning: key branches covered |
| **Frontend (composeApp)** | All ViewModels and shared business logic covered; UI components via state/callback tests |
| **No untested public API** | Every public function in domain/services and API route handlers has at least one test (or is documented as untestable in TESTING.md) |
| **CI** | Coverage reports generated and uploaded; optional coverage gate (e.g. fail if project drops > 1%) |

Existing Codecov config (`codecov.yml`) uses `range: "60..80"` and `threshold: 1%`; this plan aims to reach and hold the upper part of that range and then consider raising the floor.

---

## Current State Summary

| Area | Test files (approx.) | Coverage level | Notes |
|------|----------------------|----------------|-------|
| **Backend API routes** | 19+ | High | Route tests with testApplication; some handlers only indirectly covered |
| **Backend core services** | 11 service + 4 model + 1 event + 4 AI | High | Storage, User, Share, Sync, Federation, Collaboration, FileVersion, DeltaSync, etc. |
| **Backend core (gaps)** | — | — | TransactionManager, ActivityLogger, CollaborationOT, StorageService* split files not explicitly tested |
| **Backend plugins-api** | **0** | **None** | Plugin, PluginContext, Hooks, MetadataExtractor, PluginLifecycle, PluginConfiguration |
| **Backend infrastructure** | 12+ | High | Exposed repos, LocalStorageBackend, S3StorageBackend, BCrypt |
| **Backend plugins** | 4 | High | image-metadata, video-metadata, fulltext-search, ai-classification |
| **Backend API (config/services)** | 6 | Medium | AppConfig, Security, ErrorHandling, Logging, PluginManager, CronScheduler; UploadSessionManager, ThumbnailCache, RouteExtensions untested |
| **Frontend commonTest** | 41 | Medium | ViewModels, screens, components, API models, navigation, i18n, platform; many screens/components only lightly covered |

---

## Gap Analysis by Module

### 1. kotlin-backend/plugins-api (Priority: High)

**Current:** No test source set; no tests.

**Main sources to cover:**

| File | Suggested focus |
|------|------------------|
| `Plugin.kt` | Interface contract; default implementations if any |
| `PluginContext.kt` | Context methods (storage, metadata, events) with mocks |
| `PluginConfiguration.kt` | Parsing, validation, defaults |
| `MetadataExtractor.kt` | Interface; sample implementations of extract() |
| `PluginLifecycle.kt` | Lifecycle states and transitions |
| `Hooks.kt` | Hook invocation and ordering with mocks |

**Action:** Add `src/test/kotlin` and create unit tests for each public type. Use MockK for dependencies. This is the plugin SDK; full coverage here reduces regressions for all plugins.

---

### 2. kotlin-backend/core (Priority: High)

**Current:** Services, models, event bus, and AI are well covered. Gaps:

| Component | File(s) | Suggested tests |
|-----------|---------|------------------|
| TransactionManager | `TransactionManager.kt` | Run block in transaction; rollback on exception; commit on success |
| ActivityLogger | `ActivityLogger.kt` | Log activity with different types and payloads; verify repository calls |
| CollaborationOT | `CollaborationOT.kt` | Apply operations; transform; convergence (if used as standalone logic) |
| StorageService split | `StorageServiceQueries.kt`, `StorageServiceMutations.kt`, `StorageServiceWrite.kt` | If public APIs are not fully exercised by `StorageServiceTest.kt`, add targeted tests or extend StorageServiceTest |
| FederationService extensions | `FederationServiceSigning.kt`, `FederationServiceMaintenance.kt` | Sign/verify and maintenance flows; extend FederationServiceTest or add dedicated tests |

**Action:** Add or extend unit tests so that every public function in these files is covered. Prefer extending existing test classes where it keeps related behavior together.

---

### 3. kotlin-backend/api (Priority: High)

**Current:** Routes, Security, AppConfig, ErrorHandling, Logging, PluginManager, CronScheduler are tested. Gaps:

| Component | File(s) | Suggested tests |
|-----------|---------|------------------|
| UploadSessionManager | `UploadSessionManager.kt` | Create session; add parts; complete; abort; expiry |
| ThumbnailCache | `ThumbnailCache.kt` | Get (hit/miss); put; invalidate; size/limits if applicable |
| RouteExtensions | `RouteExtensions.kt` | Any extension functions used by routes (e.g. user/session extraction); test with mock call context |

**Optional (lower priority):** Application.kt, Database.kt, Serialization.kt, Swagger.kt are wiring/bootstrap; document as “covered by integration/run” or add minimal smoke tests if desired.

**Action:** Add `UploadSessionManagerTest`, `ThumbnailCacheTest`, and tests for `RouteExtensions` (or embed in existing route test utilities).

---

### 4. kotlin-backend/infrastructure (Priority: Medium)

**Current:** All Exposed* repositories, LocalStorageBackend, S3StorageBackend, BCrypt have tests. Ensure:

- Any new repository (e.g. ExposedFederationRepository, ExposedCollaborationRepository) has a dedicated test class if not already.
- Edge cases (empty results, duplicates, constraints) are covered where business-critical.

**Action:** Audit repository list vs test list; add tests for any missing repository. Add edge-case tests for repositories involved in sync/federation/collaboration.

---

### 5. kotlin-backend/plugins (Priority: Medium)

**Current:** Each plugin has one main test class. Ensure:

- Image/Video metadata: malformed files, missing fields, unsupported formats.
- Fulltext-search: indexing lifecycle, empty content, large content.
- AI-classification: error handling, timeouts, fallbacks.

**Action:** Review each plugin’s public API and main branches; add tests for error paths and edge cases. Keep tests fast (no real external services).

---

### 6. compose-frontend/composeApp (Priority: Medium – High for business logic)

**Current:** 41 test files; ViewModels, several screens, dialogs, and API models covered. Gaps (examples):

| Area | Suggested focus |
|------|------------------|
| **ViewModels** | FilesViewModel, UploadManager (or equivalent) – all user-driven state transitions and error handling |
| **Screens** | SettingsScreen, AdminScreen, ProfileScreen, SecurityScreen, SharedWithMeScreen – state and navigation (not visual) |
| **Components** | MoveDialog, ContextMenu, SelectionToolbar, MainSidebar, Breadcrumbs, DropZone – callbacks and state |
| **Feature** | MainContent, FilesContent, FilesLoader, UploadAction – logic that can run in commonTest |
| **Navigation** | RootComponent, RootContent, RouteMatch – all route matching and navigation outcomes |
| **Domain** | UploadQueueEntry, ChunkedFileSource – validation and state transitions |
| **i18n** | All language files (keys, placeholders) – already have StringsTest; extend for new keys |

**Action:** Prioritise ViewModels and navigation; then screens (state only); then feature/domain and components. Use `runTest` for coroutines; mock platform/API where needed. Document any UI that is only testable via Compose UI Test or manual testing in TESTING.md.

---

## Phased Action Plan

### Phase 1 – Critical gaps (plugins-api + core + API services)

**Goal:** No major backend surface area without tests; plugins-api and core/API service gaps closed.

| Step | Task | Owner / note |
|------|------|------------------|
| 1.1 | Add `plugins-api` test source set and implement tests for Plugin, PluginContext, PluginConfiguration, MetadataExtractor, PluginLifecycle, Hooks | Backend |
| 1.2 | Add or extend core tests: TransactionManager, ActivityLogger, CollaborationOT; extend FederationService (signing/maintenance) and StorageService split if needed | Backend |
| 1.3 | Add UploadSessionManagerTest, ThumbnailCacheTest, and RouteExtensions tests in api module | Backend |
| 1.4 | Run full backend test suite and jacoco; fix any regressions; ensure plugins-api included in CI jacoco | All |

**Exit criteria:** plugins-api has ≥ 80% line coverage; core and api modules do not drop; new tests are in CI.

---

### Phase 2 – Backend depth and infrastructure

**Goal:** Edge cases and error paths covered; all repositories and plugins have explicit tests.

| Step | Task | Owner / note |
|------|------|------------------|
| 2.1 | Audit Exposed* repositories vs tests; add tests for any missing repo; add edge-case tests for sync/federation/collaboration repos | Backend |
| 2.2 | For each plugin (image, video, fulltext, ai): add tests for error paths, malformed input, and main branches | Backend |
| 2.3 | Review route tests: ensure every route file has at least one test and critical status codes (401, 403, 404, 422) are asserted where relevant | Backend |
| 2.4 | Generate jacoco reports for all backend modules; document current coverage in TESTING.md | All |

**Exit criteria:** No repository or plugin without a dedicated test class; route tests cover success and main error responses.

---

### Phase 3 – Frontend ViewModels and navigation

**Goal:** All ViewModels and navigation logic covered in commonTest.

| Step | Task | Owner / note |
|------|------|------------------|
| 3.1 | List all ViewModels and navigation entry points (RootComponent, RouteMatch, etc.); mark which already have tests | Frontend |
| 3.2 | Add or extend tests for FilesViewModel, UploadManager, and any other ViewModels that drive main flows | Frontend |
| 3.3 | Add tests for RootComponent, RootContent, RouteMatch, and navigation state so all routes and outcomes are exercised | Frontend |
| 3.4 | Run composeApp desktopTest and jacoco; track coverage of commonMain code | Frontend |

**Exit criteria:** Every ViewModel and navigation path has at least one test; coverage report shows improvement for commonMain.

**Phase 3 inventory (3.1):**
- **ViewModels:** AuthViewModel, FilesViewModel, AdminViewModel, ProfileViewModel, PluginsViewModel, SettingsViewModel, ChangePasswordViewModel, SecurityViewModel, CollaborationViewModel, FederationViewModel, AIViewModel, SyncViewModel, VersionHistoryViewModel, SharesViewModel, SharedWithMeViewModel, ActivityViewModel (16). AppViewModel is covered by AppViewModelTest, AppViewModelDetailedTest, AppViewModelFunctionalTest, AppViewModelPhase6Test, AdvancedFeaturesTest. FilesViewModel logic is covered by FilesModeAndTrashStarTest and FilesUploadAndMoveTest (mode, upload destination, move logic); no direct ViewModel test (would require mocking in commonTest).
- **UploadManager:** Now has UploadManagerTest (destination and minimized state) with StubStorageRepository.
- **Navigation:** AppRoutes, AppRoute, RoutePaths, RouteMatch covered by AppRoutesTest and RouteMatchTest. RootComponent/DefaultRootComponent and RootContent are Composable/wiring (auth vs main child); not unit-tested in commonTest (covered by integration/run).

---

### Phase 4 – Frontend screens and feature logic

**Goal:** Screen and feature logic (state, callbacks) covered; components covered where testable without UI runtime.

| Step | Task | Owner / note |
|------|------|------------------|
| 4.1 | Add state/callback tests for SettingsScreen, AdminScreen, ProfileScreen, SecurityScreen, SharedWithMeScreen | Frontend |
| 4.2 | Add tests for MoveDialog, ContextMenu, SelectionToolbar, MainSidebar, Breadcrumbs, DropZone (logic only) | Frontend |
| 4.3 | Add tests for MainContent, FilesContent, FilesLoader, UploadAction, and domain types (e.g. UploadQueueEntry, ChunkedFileSource) | Frontend |
| 4.4 | Extend i18n tests so all language files and new keys are covered | Frontend |

**Exit criteria:** All screens and major components listed in TESTING.md as “testable in commonTest” have tests; i18n coverage is up to date.

**Phase 4 summary:**
- **4.1 Screens:** Settings, Admin, Profile, SharedWithMe logic already in ScreensTest; SecurityScreenTest added (ActiveSession, LoginEvent, SecuritySettings, SessionDeviceType, TwoFactorMethod, filter current session).
- **4.2 Components:** DragDropComponentsTest already covers DragOverlay, DropZone, ContextMenu, MoveDialog; SelectionToolbar and MainSidebar are UI-only (no separate logic tests).
- **4.3 Feature/domain:** UploadQueueEntryTest added for UploadQueueEntry.WithData/Chunked, FolderUploadEntry (equality, properties), and ChunkedFileSource (readChunk range). MainContent/FilesContent/FilesLoader are Composable or internal with use-case deps; logic covered via ViewModel and Files*Test.
- **4.4 i18n:** StringsTest extended with settingsSecurity in settings and in allLanguages_haveConsistentStrings.

---

### Phase 5 – CI gates and maintenance

**Goal:** Coverage is enforced and regressions are visible.

| Step | Task | Owner / note |
|------|------|------------------|
| 5.1 | Ensure CI runs jacoco for backend (core, api, infrastructure, plugins-api, all plugins) and frontend (composeApp); upload to Codecov | DevOps / maintainer |
| 5.2 | (Optional) Add a coverage gate job: fail if project coverage drops by more than X% (e.g. 1%) from baseline | DevOps |
| 5.3 | Add a short “Coverage” section to CONTRIBUTING.md: run `make test-coverage` before PR; keep coverage ≥ 80% for new code | Maintainer |
| 5.4 | Update TESTING.md with final coverage summary and link to this action plan | Maintainer |

**Exit criteria:** CI runs coverage on every PR; contributors are instructed to run test-coverage; TESTING.md is up to date.

**Phase 5 summary:**
- **5.1:** CI already runs jacoco for backend (core, api, infrastructure, plugins-api, image-metadata, video-metadata, fulltext-search, ai-classification) and frontend (composeApp), then uploads artifacts to the coverage job which sends them to Codecov.
- **5.2:** Coverage gate (fail if coverage drops > X%) left optional; can be enabled in Codecov or CI later.
- **5.3:** CONTRIBUTING.md updated with a “Coverage” subsection: run `make test-coverage` before PR, keep coverage ≥ 80% for new code, links to TESTING.md and TEST_COVERAGE_ACTION_PLAN.md; PR checklist includes “Run make test-coverage” as recommended.
- **5.4:** TESTING.md updated with “Phase 5 (CI and coverage maintenance)” and final coverage summary.

---

### Phase 6 (Optional) – Advanced quality

**Goal:** Higher confidence via mutation testing or E2E where useful.

| Step | Task | Owner / note |
|------|------|------------------|
| 6.1 | Consider PIT mutation testing for core and api (or a subset) to find weak tests | Backend |
| 6.2 | Consider Testcontainers for one or two integration tests (e.g. full stack with Postgres) | Backend |
| 6.3 | Document in TESTING.md any E2E or Compose UI Test plans (e.g. for critical user flows) | All |

---

## CI and Quality Gates

- **Current:** CI runs backend and frontend tests and generates jacoco reports; Codecov upload is configured.
- **Recommended:**
  - Keep `make test-coverage` (or equivalent Gradle jacoco commands) as the single way to generate all reports.
  - In Codecov, use status checks so PRs see coverage change; consider “fail if project coverage drops by > 1%” once baseline is met.
  - Pre-commit: run `./gradlew ktlintFormat && ./scripts/check-no-fqn.sh && ./gradlew detekt` and, before pushing, `./gradlew test` (or `make test`). Optionally run `make test-coverage` before opening a PR.

---

## Success Criteria

1. **plugins-api:** Full test suite; ≥ 80% line coverage.
2. **Backend (core, api, infrastructure, each plugin):** No untested public API in domain/services and route handlers (or documented in TESTING.md); ≥ 80% line coverage per module where feasible.
3. **Frontend (composeApp):** All ViewModels and navigation paths tested; screens and major components covered for state/callbacks; coverage report generated and uploaded.
4. **CI:** All tests and coverage run on every PR; coverage visible in Codecov; optional gate to prevent coverage regression.
5. **Docs:** TESTING.md and this plan updated; CONTRIBUTING.md mentions coverage expectations.

---

## References

- [TESTING.md](TESTING.md) – Test strategy, running tests, coverage commands, untestable components
- [CODE_QUALITY.md](CODE_QUALITY.md) – ktlint, detekt, pre-commit order
- [AI_CODING_GUIDELINES.md](AI_CODING_GUIDELINES.md) – Unit tests for non-trivial logic
- [codecov.yml](../../codecov.yml) – Codecov configuration
- [.github/workflows/ci.yml](../../.github/workflows/ci.yml) – CI test and coverage steps
