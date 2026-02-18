# Test Coverage Action Plan

This document defines a phased plan to achieve full test coverage across VaultStadio. It complements [TESTING.md](TESTING.md) (strategy, running tests, untestable components) and aligns with [CODE_QUALITY.md](CODE_QUALITY.md) and [AI_CODING_GUIDELINES.md](AI_CODING_GUIDELINES.md).

**Last updated**: 2026-02-18

---

## Current coverage snapshot (Jacoco)

*Generated from `./gradlew test jacocoTestReport` (all modules).*

### Backend

| Module | Instruction cov. | Branch cov. | Target | Priority |
|--------|------------------|-------------|--------|----------|
| **core** | **65%** | 46% | ≥80% | High |
| **api** | 12% | 4% | ≥80% | High |
| **plugins-api** | **80%** | 57% | ≥80% | **Met** – maintain |
| **infrastructure** | 13% | 8% | ≥80% | Medium |
| **image-metadata** | 13% | 1% | ≥80% | Medium |
| **video-metadata** | 9% | 0% | ≥80% | Medium |
| **fulltext-search** | 14% | 0% | ≥80% | Medium |
| **ai-classification** | 16% | 0% | ≥80% | Medium |

### Frontend (composeApp – desktopTest)

| Module | Instruction cov. | Branch cov. | Notes |
|--------|------------------|-------------|--------|
| **composeApp** | **3%** | 0% | Most code in screens/components; only desktop JVM code measured |

**Frontend packages with non-zero coverage:** `domain.upload` 76%, `navigation` 61%, `domain.model` 59%, `i18n` 50%, `ui.components.dialogs` 3%, `feature.upload` 7%, `feature.main` 1%, `platform` 1%, `data.repository` 2%. All `ui.screens.*` and most `feature.*` are 0%.

### Backend – coverage by package (core, api, infrastructure)

- **core**: domain.service 62%, domain.model 71%, ai 69%, ai.providers 48%, domain.repository 41%, **domain.event 76%**, **exception 97%** (improved).
- **api**: api.service 68%, config 32%, plugins 25%, sync 27%, version 25%, federation 22%; **routes (storage 3%, metadata 0%, ai 0%, auth 0%, health 0%, etc.)** and middleware 0%. Route tests use empty `application { }` so handlers are not executed; full coverage needs testApplication with `module()` + test DB.
- **infrastructure**: **persistence 9%**, storage 29%, persistence.entities 22%; security 100%.

**Focus areas for improvement:**

1. **api**: Route handlers are barely executed (storage 3%, metadata/ai/auth 0%). Route tests use empty `application { }`; to hit handlers either use `testApplication { application { module() } }` with a test DB (e.g. Testcontainers) or document routes as integration-only. api.service (68%) and config (32%) can be extended.
2. **core**: Keep improving domain.service (62%) and domain.repository (41%) branch coverage; domain.event (76%) and exception (97%) are in good shape.
3. **plugins-api**: Target met (80%); maintain and add branch coverage for context (52%) where useful.
4. **infrastructure**: Persistence 9% – add or extend repository tests; storage 29% – extend LocalStorageBackend/S3 tests.
5. **Plugins (image, video, fulltext, ai)**: 9–16% instruction coverage; extraction logic uses native/IO. Add tests for entry points, error paths, and mockable boundaries.
6. **Frontend**: Global 3%; focus on ViewModels, navigation (61%), domain.upload (76%), and feature/usecase logic testable in commonTest; screens are mostly Composables (0% in report).

---

**Recent coverage improvements:** **Phase 3 (frontend ViewModels):** AuthViewModelTest; ChangePasswordViewModelTest (validation, visibility, dismissSuccess); SecurityViewModelTest (showRevokeDialog, dismissRevokeDialog, dismissError, toggleTwoFactor); SettingsViewModelTest (toggleDarkMode, updateThemeMode, setLanguage, resetCacheCleared); ProfileViewModelTest (clearError, clearSuccessMessage); AdminViewModelTest (clearError, loadUsers on success, clearError doesNotThrow); PluginsViewModelTest (clearError, loadPlugins doesNotThrow). **Phase 2 (plugins):** VideoMetadataPluginTest getConfigurationSchema. FullTextSearchPluginTest: analyzeContent (empty stream, plain text), getConfigurationSchema. **Frontend (composeApp):** FederationUseCaseTest extended with GetIncomingFederatedSharesUseCaseTest. **Backend (core):** StorageItemTest for StorageItem. — Prior: CollaborationUseCaseTest; FederationUseCaseTest; AIUseCaseTest; ApiResponseTest; ActivityUseCaseTest, PluginUseCaseTest, MetadataUseCaseTest, AuthUseCaseTest, ShareUseCaseTest, SyncUseCaseTest, VersionUseCaseTest, AdminUseCaseTest, StorageUseCaseTest; LocalStorageBackendTest; StorageExceptionTest, StorageEventTest, AdvancedEventsTest; HealthRoutesTest, FileVersionServiceTest, ActivityLoggerTest, MetadataExtractorTest, StorageServiceTest, UserServiceTest, etc. See TESTING.md § Untestable Components for Redis-backed classes.

---

## Analysis summary – where to focus

**Highest impact (backend):**

1. **api** (12% instr.) – Routes dominate the module. Existing route tests use empty `application { }`, so handlers are never executed. Either: (a) add integration tests with `testApplication { application { module() } }` and a test DB (e.g. Testcontainers), or (b) document route coverage as integration-only. Extend api.service (68%) and config (32%) with unit tests where useful.
2. **core** (65%) – Improve **domain.repository** (41%) and **domain.service** (62%) branch coverage; **domain.event** (76%) and **exception** (97%) are in good shape.
3. **infrastructure** (13%) – **Persistence** (9%) is the main gap; add/expand Exposed* repository tests. Storage (29%) second.

**Medium impact (backend):**

4. **Plugins** (image 13%, video 9%, fulltext 14%, ai 16%) – Test plugin entry points, configuration, and error branches; extraction logic may remain low if it depends on native/IO.
5. **plugins-api** – Already at 80%; maintain and optionally improve context (52%) and branch coverage.

**Frontend:**

6. **composeApp** (3% overall) – Coverage is measured on desktop JVM only; most UI (screens, components) is not exercised by unit tests. Prioritise: **domain.upload** (76% – keep), **navigation** (61% – keep), **domain.model** (59%), **i18n** (50%); then add tests for **domain.usecase.*** (many at 0%) and ViewModel logic that can run in commonTest. Screens are typically covered only by integration or manual testing.

**How to regenerate this snapshot:** Run `./gradlew test jacocoTestReport` (or `make test-coverage`), then open each module’s `build/reports/jacoco/test/html/index.html` (or `compose-frontend/composeApp/build/reports/jacoco/jacocoTestReport/html/index.html` for the frontend).

---

## Coverage analysis – where to focus efforts

This section summarises **concrete focus areas** so efforts are directed at the code that most needs tests (all modules, backend and frontend).

### Backend – priority order

| Priority | Module        | Instr. | Branch | Main gap | Action |
|----------|---------------|--------|--------|----------|--------|
| 1        | **api**       | 12%    | 4%     | Route handlers not executed (tests use empty `application { }`) | Use `testApplication { application { module() } }` + test DB for critical routes, or extend **api.service** (68%) and **config** (32%) with unit tests |
| 2        | **core**      | 65%    | 46%    | domain.service 62%, domain.repository 41% | Extend existing service tests for missing branches; add repository interface tests where useful |
| 3        | **infrastructure** | 13% | 8%  | persistence 9%, storage 29% | Extend Exposed* repository tests; add LocalStorageBackend/S3 edge cases |
| 4        | **image-metadata**  | 13% | 1%  | Extraction logic | Test entry points, config, error paths |
| 5        | **video-metadata**  | 9%  | 0%  | Same as image | Same as image-metadata |
| 6        | **fulltext-search**| 14% | 0%  | Indexing/search | Entry points, empty content, error handling |
| 7        | **ai-classification** | 16% | 0% | AI calls | Error handling, timeouts; mock external AI |
| –        | **plugins-api**| 80% | 57% | Target met | Maintain; optional: context and branch coverage |

### Frontend – priority order

| Priority | Package / area      | Cov.  | Action |
|----------|---------------------|-------|--------|
| 1        | **domain.usecase.*** (auth, share, storage, sync, version, admin, ai, collaboration, federation, metadata, activity, plugin) | 0% (most) | Add unit tests with fake repositories; only **config** has tests (100%). Same pattern as GetStorageUrlsUseCaseTest |
| 2        | **data.repository** | 2%    | Test repository implementations with fake services or test doubles |
| 3        | **feature.*** (files, auth, main, upload) | 0–7% | Test ViewModels and feature logic in commonTest; mock use cases |
| 4        | **navigation**      | 61%   | Keep; add branch coverage if new routes |
| 5        | **domain.upload**   | 76%   | Keep |
| 6        | **domain.model**    | 59%   | Extend model tests for new types |
| 7        | **i18n**            | 50%   | Extend StringsTest |
| –        | **ui.screens.***    | 0%    | Mostly Composables; cover via state/callback tests or document as UI-only |

### Quick wins

- **Frontend:** Add one test file per use-case package (auth, share, storage, etc.) with a fake repository and tests for success/error paths. Each use case is a thin wrapper around the repository, so tests are small and repetitive.
- **Backend api:** Add or extend unit tests for non-route code (e.g. AppConfigTest, SecurityTest, route extension helpers) so the api module coverage rises even if route handlers stay integration-only.
- **Backend infrastructure:** Add tests for any Exposed* repository method not yet covered; add edge-case tests for LocalStorageBackend.

---

## Table of Contents

1. [Goals and Targets](#goals-and-targets)
2. [Current State Summary](#current-state-summary)
3. [Coverage analysis – where to focus efforts](#coverage-analysis--where-to-focus-efforts)
4. [Gap Analysis by Module](#gap-analysis-by-module)
5. [Phased Action Plan](#phased-action-plan)
6. [CI and Quality Gates](#ci-and-quality-gates)
7. [Success Criteria](#success-criteria)

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
| **Backend API routes** | 19+ | Low (12% module) | Route test classes exist but many handlers not executed; storage 3%, metadata/ai/auth 0% |
| **Backend core** | 11+ service, model, event, AI | 65% instr. | domain.service 62%, domain.event 76%, exception 97%; domain.repository 41% |
| **Backend plugins-api** | 6 | **80%** | Plugin, PluginContext, Hooks, MetadataExtractor, PluginLifecycle, PluginConfiguration |
| **Backend infrastructure** | 12+ | 13% | Persistence 9%, storage 29%, security 100%; Exposed repos, LocalStorageBackend, S3, BCrypt have tests |
| **Backend plugins** | 4 | 9–16% | image-metadata, video-metadata, fulltext-search, ai-classification; extraction logic largely untested |
| **Backend API (config/services)** | 6 | api.service 68% | UploadSessionManager, ThumbnailCache, RouteExtensions have tests; config 32% |
| **Frontend commonTest** | 41+ | 3% (desktop report) | domain.upload 76%, navigation 61%, domain.model 59%, i18n 50%; screens/features mostly 0% in report |

---

## Gap Analysis by Module

### 1. kotlin-backend/plugins-api (Priority: Maintain – target met)

**Current:** ~80% instruction, 57% branch. Tests exist for Plugin, PluginContext, PluginConfiguration, MetadataExtractor, PluginLifecycle, Hooks.

**Remaining gaps:** Context package ~52%; some DefaultImpls and branches. Optional: add tests for any new public APIs and edge cases.

**Action:** Keep coverage ≥80%; add branch/context coverage if adding new plugin SDK surface.

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

**Current:** 13% instruction, 8% branch. Persistence 9%, storage 29%, security 100%. Exposed* repositories, LocalStorageBackend, S3StorageBackend, BCrypt have test classes but many code paths (persistence) are not covered by unit tests.

**Action:** Increase persistence coverage by exercising more repository methods and branches in unit tests (or document reliance on integration tests). Add edge-case tests for repositories involved in sync/federation/collaboration. Extend storage backend tests where feasible.

---

### 5. kotlin-backend/plugins (Priority: Medium)

**Current coverage:** image-metadata 13% (1% branch), video-metadata 9% (0% branch), fulltext-search 14% (0% branch), ai-classification 16% (0% branch). Each plugin has a test class but most code is extraction/runtime logic that is not exercised in unit tests.

**Focus:**

- Image/Video metadata: test entry points, configuration, and error paths (malformed input, unsupported format); extraction may stay low if it relies on native/IO.
- Fulltext-search: indexing lifecycle, empty content, error handling.
- AI-classification: error handling, timeouts, fallbacks; mock external AI calls.

**Action:** Add tests for public API, config, and mockable boundaries; improve branch coverage where logic is testable without real files or external services.

---

### 6. compose-frontend/composeApp (Priority: Medium – High for business logic)

**Current:** JaCoCo report (desktopTest) shows **3% instruction, 0% branch** overall. Packages with meaningful coverage: domain.upload 76%, navigation 61%, domain.model 59%, i18n 50%; feature.upload 7%; most ui.screens.* and feature.* are 0%. 41+ test files exist in commonTest; coverage is measured only for code run by desktop JVM tests.

**Suggested focus:**

| Area | Suggested focus |
|------|------------------|
| **ViewModels** | FilesViewModel, UploadManager – state transitions and error handling (mock deps in commonTest) |
| **Screens** | State and navigation only for Settings, Admin, Profile, Security, SharedWithMe (not visual rendering) |
| **Components** | MoveDialog, ContextMenu, SelectionToolbar, MainSidebar, Breadcrumbs, DropZone – callbacks and state |
| **Feature / domain** | MainContent, FilesContent, FilesLoader, UploadAction; UploadQueueEntry, ChunkedFileSource (already tested) |
| **Navigation** | RootComponent, RootContent, RouteMatch – route matching and navigation outcomes (navigation already 61%) |
| **i18n** | Extend StringsTest for new keys and placeholders |

**Action:** Prioritise ViewModels and feature/usecase logic that can run in commonTest; then state-only screen tests. Use `runTest` for coroutines; mock platform/API where needed. Document UI that is only testable via Compose UI Test or manual testing in TESTING.md.

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
- **4.1 Screens:** Settings, Admin, Profile, SharedWithMe logic in ScreensTest; SecurityScreenTest (ActiveSession, LoginEvent, SecuritySettings, SessionDeviceType, TwoFactorMethod, filter current session). **SharedWithMeScreenTest** added for SharedWithMeItem properties, groupBy owner, empty list, filter by file/folder.
- **4.2 Components:** DragDropComponentsTest covers DragOverlay, DropZone, ContextMenu, MoveDialog; **BreadcrumbsLogicTest** added for Breadcrumb display name (home vs name), isHome, isLast, isClickable; SelectionToolbar and MainSidebar are UI-only (no separate logic tests).
- **4.3 Feature/domain:** UploadQueueEntryTest for UploadQueueEntry.WithData/Chunked, FolderUploadEntry, ChunkedFileSource. MainContent/FilesContent/FilesLoader covered via ViewModel and Files*Test.
- **4.4 i18n:** StringsTest extended with navHome, navSharedWithMe in navigation; englishStrings_hasAuthStrings; allLanguages_haveNavHome; allLanguages_haveConsistentStrings includes navHome.

---

### Phase 5 – CI gates and maintenance

**Goal:** Coverage is enforced and regressions are visible.

| Step | Task | Owner / note |
|------|------|------------------|
| 5.1 | Ensure CI runs jacoco for backend (core, api, infrastructure, plugins-api, all plugins) and frontend (composeApp); upload to Codecov | DevOps / maintainer |
| 5.2 | Add a coverage gate: fail if patch coverage drops by more than 1% (implemented in codecov.yml) | DevOps / maintainer |
| 5.3 | Add a short “Coverage” section to CONTRIBUTING.md: run `make test-coverage` before PR; keep coverage ≥ 80% for new code | Maintainer |
| 5.4 | Update TESTING.md with final coverage summary and link to this action plan | Maintainer |

**Exit criteria:** CI runs coverage on every PR; contributors are instructed to run test-coverage; TESTING.md is up to date; coverage gate (1% patch threshold) enabled in codecov.yml. **All met.**

**Phase 5 summary (complete):**
- **5.1:** CI runs jacoco for backend (core, api, infrastructure, plugins-api, image-metadata, video-metadata, fulltext-search, ai-classification) and frontend (composeApp); coverage job uploads to Codecov. See [.github/workflows/ci.yml](../../.github/workflows/ci.yml).
- **5.2:** Coverage gate implemented in [codecov.yml](../../codecov.yml): `status.project.default.threshold: 1%`. The Codecov status check fails if patch coverage (new/changed code in the PR) drops by more than 1%. Optional: a stricter project-level gate can be added in the Codecov UI if desired.
- **5.3:** CONTRIBUTING.md has a “Coverage” subsection and PR checklist: run `make test-coverage` before PR, keep coverage ≥ 80% for new code, links to TESTING.md and this plan; local HTML report paths documented; PR checklist requires coverage step and states Codecov 1% threshold.
- **5.4:** TESTING.md updated with Phase 5 (CI and coverage maintenance), coverage gate explanation (codecov.yml 1% threshold), refreshed Coverage Summary table, Phase 4 summary, and link to this plan for latest snapshot.

---

### Phase 6 (Optional) – Advanced quality

**Goal:** Higher confidence via mutation testing or E2E where useful.

| Step | Task | Owner / note |
|------|------|------------------|
| 6.1 | Consider PIT mutation testing for core and api (or a subset) to find weak tests | Backend |
| 6.2 | Consider Testcontainers for one or two integration tests (e.g. full stack with Postgres) | Backend |
| 6.3 | Document in TESTING.md any E2E or Compose UI Test plans (e.g. for critical user flows) | All |

**Phase 6 summary (documented):**
- **6.1:** [TESTING.md](TESTING.md) has a “Phase 6.1 – Mutation testing (PIT)” section: scope (core/api or subset), how to add the plugin, run command, and that it is optional.
- **6.2:** [TESTING.md](TESTING.md) has “Phase 6.2 – Testcontainers and integration tests”: current use (ExposedStorageItemRepositoryTest, ExposedUserRepositoryTest with PostgreSQLContainer; api has testcontainers deps; CI has Docker). Next step: one or two full-stack integration tests (testApplication + Postgres) for api routes.
- **6.3:** [TESTING.md](TESTING.md) has “Phase 6.3 – E2E and Compose UI Test plans”: E2E (Playwright/Selenium for web, Appium for mobile); Compose UI (desktop uiTestJUnit4 for critical flows). Status: documented, not yet implemented.

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
