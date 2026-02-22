# Test Coverage Action Plan

This document defines a phased plan to achieve full test coverage across VaultStadio. It complements [TESTING.md](TESTING.md) (strategy, running tests, untestable components) and aligns with [CODE_QUALITY.md](CODE_QUALITY.md) and [AI_CODING_GUIDELINES.md](AI_CODING_GUIDELINES.md).

**Last updated**: 2026-02-22

---

## Executive summary – path to full coverage

To get **full coverage** (front + back) without leaving any test or piece of code out:

1. **Use the [Exhaustive checklist](#exhaustive-checklist--full-coverage-no-gaps)** as the single source of truth: it lists every backend main file and every frontend area with the test to add or extend.
2. **Follow the [Phased Action Plan](#phased-action-plan)** in order (Phase 1 → 5). Each phase references the same checklist.
3. **After each phase**, run [Verification checklist](#verification-checklist-after-each-phase): `make test-coverage`, open Jacoco HTML reports, tick off covered items, update "Recent coverage improvements".
4. **Backend priority:** api (routes + services + DTOs) → core (services, events, AI) → infrastructure (persistence, storage) → plugins. Target ≥80% instruction coverage per reported module.
5. **Frontend priority:** domain.usecase.* and ViewModels first (they drive coverage of data/feature); then navigation, utils, i18n; then screen/component state tests. Maximise testable logic in commonTest.

---

## Project structure (modules)

The following reflects the current **backend** and **frontend** Gradle projects (see `backend/settings.gradle.kts` and `frontend/settings.gradle.kts`). Use this to keep the plan aligned with the codebase.

### Backend (`backend/` – standalone Gradle project)

| Layer | Modules | Jacoco in `make test-coverage`? |
|-------|---------|----------------------------------|
| **domain** | common, storage, auth, share, activity, admin, version, sync, federation, collaboration, metadata, plugin | No (tested transitively via core/api) |
| **core** | core (aggregator), common, auth, storage, share, activity, version, sync, federation, collaboration, ai | **Yes** – `:core:jacocoTestReport` |
| **application** | auth, storage, share, user, admin, activity, metadata, version, sync, plugin, chunkedupload, health, ai | No (tested transitively via api) |
| **api** | api | **Yes** – `:api:jacocoTestReport` |
| **plugins-api** | plugins-api | **Yes** – `:plugins-api:jacocoTestReport` |
| **infrastructure** | infrastructure | **Yes** – `:infrastructure:jacocoTestReport` |
| **plugins** | image-metadata, video-metadata, fulltext-search, ai-classification | **Yes** – each has `:plugins/<name>:jacocoTestReport` |

Backend tests run via `./gradlew :core:test :api:test` (see `backend/Makefile`). Coverage reports are generated only for the modules listed as “Yes” above.

### Frontend (`frontend/` – standalone Gradle project)

| Layer | Modules | Jacoco in `make test-coverage`? |
|-------|---------|----------------------------------|
| **domain** | result, auth, storage, admin, sync, share, activity, metadata, plugin, version, collaboration, federation, ai, config, upload | No (included in composeApp report) |
| **core** | resources | No (included in composeApp report) |
| **data** | network, auth, storage, admin, sync, share, activity, metadata, plugin, version, collaboration, federation, ai, config | No (included in composeApp report) |
| **feature** | auth, admin, sync, shares, sharedwithme, activity, profile, settings, security, changepassword, plugins, files, upload, versionhistory, collaboration, federation, ai, main, licenses | No (included in composeApp report) |
| **app** | composeApp, androidApp, iosApp | **Yes** – `:composeApp:jacocoTestReport` (desktopTest only) |

Frontend coverage is a single report for **composeApp** (desktop JVM tests); it includes code from domain, data, and feature modules that is exercised by those tests.

---

## Current coverage snapshot (Jacoco)

*Generated from backend test tasks + `:composeApp:desktopTest` and respective `jacocoTestReport` (see [Running Tests](TESTING.md#running-tests)). Run `make test-coverage` from repo root (or `make test-coverage` in `backend/` and `frontend/`).*

### Backend

| Module | Instruction cov. | Branch cov. | Target | Priority |
|--------|------------------|-------------|--------|----------|
| **core** | **66%** | 46% | ≥80% | High |
| **api** | 12% | 4% | ≥80% | High |
| **plugins-api** | **80%** | 57% | ≥80% | **Met** – maintain |
| **infrastructure** | 13% | 9% | ≥80% | Medium |
| **image-metadata** | 13% | 1% | ≥80% | Medium |
| **video-metadata** | 9% | 0% | ≥80% | Medium |
| **fulltext-search** | 17% | 0% | ≥80% | Medium |
| **ai-classification** | 16% | 0% | ≥80% | Medium |

*Backend coverage is reported only for these modules (see [Project structure](#project-structure-modules)). Domain and application layers are tested when core and api tests run but do not produce separate Jacoco reports in `make test-coverage`.*

### Frontend (composeApp – desktopTest)

| Module | Instruction cov. | Branch cov. | Notes |
|--------|------------------|-------------|--------|
| **composeApp** | **5%** | ~0% | Most code in screens/components; only desktop JVM code measured |

**Frontend packages with non-zero coverage:** `domain.upload` 76%, `navigation` 61%, `domain.model` 59%, `i18n` 50%, `ui.components.dialogs` 3%, `feature.upload` 7%, `feature.main` 1%, `platform` 1%, `data.repository` 2%, **`utils`** (FormattingTest covers formatFileSize, formatRelativeTime, formatDate, getFileIconName, getFileTypeName, isValidEmail, isStrongPassword, truncateText). All `ui.screens.*` and most `feature.*` are 0%.

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

**Recent coverage improvements:** **Applied (continuation 2026-02-22):** **Backend core/activity:** **ActivityLoggerTest** extended with `start then publish FileEvent Restored calls activityRepository create with FILE_RESTORED` and `start then publish FileEvent Copied calls activityRepository create with FILE_COPIED` (details contain sourceId and sourcePath). **Frontend composeApp:** **VersionApiModelsTest** extended with **testFileVersionHistoryCreation** (FileVersionHistory fields), **testFileVersionIsRestoreWhenRestoredFromSet**, **testFileVersionIsNotRestoreWhenRestoredFromNull** (FileVersion.isRestore). —
**Applied (continuation 2026-02-22):** **Backend core/activity:** **ActivityLoggerTest** extended with FileEvent.Renamed test. **Backend api:** **ApiResponseTest** extended with SearchRequestTest and PluginInfoResponseTest. —
**Applied (continuation 2026-02-22):** **Backend api:** **ApiResponseTest** extended with CreateShareRequestTest and AccessShareRequestTest. **Frontend composeApp:** **FormattingTest** extended with getFileTypeName_extensionUsedWhenMimeUnrecognized. —
**Applied (continuation 2026-02-22):** **Frontend composeApp:** **AIApiModelsTest** extended with testChatRoleValues and testChatRoleNames. **Backend core/activity:** **ActivityLoggerTest** extended with FileEvent.Deleted handler test. —
**Applied (continuation 2026-02-22):** **Backend api:** **ApiResponseTest** extended with **UserInfoToResponseTest** – `toResponse maps UserInfo to UserResponse`, `toResponse with null avatarUrl`. **ThumbnailCacheTest** extended with `invalidate for non-existent itemId does not throw`. —
**Applied (continuation 2026-02-22):** **Frontend composeApp:** **VersionApiModelsTest** extended with **VersionDiff** tests: testVersionDiffCreation, testVersionDiffBinary. —
**Applied (continuation 2026-02-22):** **Frontend composeApp:** **FederationViewModelTest** fixed and run: use case fakes updated to correct signatures (GetFederatedInstancesUseCase with status, GetIncomingFederatedSharesUseCase with status, GetFederatedActivitiesUseCase with instance/since/limit); GetFederatedInstanceUseCase returns Result<FederatedInstance>. **CollaborationViewModelTest** added: loadSession (success/error), loadParticipants (success after loadSession), leaveSession (success/error), updatePresence, loadUserPresences, setOffline, clearError; uses initialItemId = "" to avoid joinSession/WebSocket in init; fakes for all 15 use cases including GetCurrentUserUseCase (StateFlow). **AIViewModelTest** added: loadProviders (success/error), loadModels, chat (success/error with callback), describeImage, tagImage, classifyContent, summarize, configureProvider, hideProviderConfig, deleteProvider success, checkProviderStatus, loadProviderModels, clearProviderStatus, clearProviderModels, clearError. **AIUseCaseTest** extended: **GetProviderModelsUseCaseTest** (invoke returns repo result, invoke propagates error), **GetAIProviderStatusUseCaseTest** (invoke returns repo result, invoke propagates error), **DeleteAIProviderUseCaseTest** (invoke returns repo result, invoke propagates error); FakeAIRepository now has getProviderModelsResult, getProviderStatusResult, deleteProviderResult. **CollaborationUseCaseTest** extended: FakeCollaborationRepository now has configurable getParticipantsResult, getDocumentStateResult, saveDocumentResult, createCommentResult, resolveCommentResult, deleteCommentResult, updatePresenceResult, getUserPresenceResult, setOfflineResult; added **GetDocumentStateUseCaseTest**, **SaveDocumentUseCaseTest**, **GetSessionParticipantsUseCaseTest**, **CreateDocumentCommentUseCaseTest**, **ResolveDocumentCommentUseCaseTest**, **DeleteDocumentCommentUseCaseTest**, **UpdatePresenceUseCaseTest**, **GetUserPresenceUseCaseTest**, **SetOfflineUseCaseTest** (each: invoke success, invoke propagates error). **FormattingTest** extended: getFileIconName_archiveAndCodeVariants (compressed, x-archive, javascript, xml), getFileTypeName_archiveByMimeContains (x-archive). **GetStorageUrlsUseCaseTest (config):** GetVersionUrlsUseCaseTest extended with downloadUrl_withDifferentItemAndVersion_buildsCorrectUrl (doc-id, version 5; different base URL). **SyncUseCaseTest** extended: **PullChangesUseCaseTest** added (invoke_returnsRepositoryPullChangesResult, invoke_propagatesError). **FederationUseCaseTest** extended: FakeFederationRepository now has removeInstanceResult, getOutgoingSharesResult, acceptShareResult; **GetOutgoingFederatedSharesUseCaseTest**, **RemoveInstanceUseCaseTest**, **AcceptFederatedShareUseCaseTest** added (each: invoke success, invoke propagates error). Further extended: declineShareResult, revokeShareResult, getIdentitiesResult, getActivitiesResult; helpers testFederatedIdentity(), testFederatedActivity(); **DeclineFederatedShareUseCaseTest**, **RevokeFederatedShareUseCaseTest**, **GetFederatedIdentitiesUseCaseTest**, **GetFederatedActivitiesUseCaseTest** added (each: invoke success, invoke propagates error). Completed: createShareResult, linkIdentityResult, unlinkIdentityResult; **CreateFederatedShareUseCaseTest**, **LinkIdentityUseCaseTest**, **UnlinkIdentityUseCaseTest** added (each: invoke success, invoke propagates error). All 15 federation use cases now have tests; FakeFederationRepository has no remaining stubResult(). **AuthUseCaseTest** added (new file): FakeAuthRepository with loginResult, registerResult, logoutResult, getCurrentUserResult; **LoginUseCaseTest**, **RegisterUseCaseTest**, **LogoutUseCaseTest**, **GetCurrentUserUseCaseTest** (each: invoke success, invoke propagates error). Fills domain.usecase.auth coverage referenced in checklist. Extended: getQuotaResult, updateProfileResult, changePasswordResult; **GetQuotaUseCaseTest**, **UpdateProfileUseCaseTest**, **ChangePasswordUseCaseTest** added (each: invoke success, invoke propagates error); testStorageQuota() helper. **GetLoginHistoryUseCaseTest** (invoke_returnsEmptyList), **RevokeSessionUseCaseTest** (invoke_returnsNotImplementedError) added for current impl behaviour. **GetSecuritySettingsUseCaseTest** (invoke_returnsDefaultSecuritySettings), **GetActiveSessionsUseCaseTest** (invoke_returnsEmptyList) added. All 11 auth use cases now covered. **Frontend i18n (core/resources):** StringsTest extended with **allLanguages_haveSyncAndPluginsNav** (all 7 languages have non-empty navSync and navPlugins). — **Applied (continuation):** **Frontend composeApp:** **VersionHistoryViewModelTest** added for VersionHistoryViewModel: loadHistory (success/error), getVersion success, clearSelectedVersion, downloadVersion, clearDownloadUrl, compareVersions success, clearDiff, clearError, deleteVersion error path. **SharedWithMeViewModelTest** added for SharedWithMeViewModel: loadSharedItems (empty list, with share+item, error), onItemClick, clearSelectedItem, downloadItem, clearDownloadUrl, removeShare (success/error), clearError. **ActivityViewModelTest** added for ActivityViewModel: loadActivities (success/error), onActivityClick, clearSelectedActivity, clearError, loadActivities(limit) passes limit to use case. **SyncViewModelTest** added for SyncViewModel: loadDevices (success/error), loadConflicts (success/error), clearError, clearSyncResponse, pullChanges then clearSyncResponse. Uses fake use cases (GetDevices, GetConflicts, RegisterDevice, DeactivateDevice, RemoveDevice, ResolveConflict, PullChanges) and ViewModelTestBase.runTestWithMain. — **Applied Phase 4 (2026-02-22):** **Frontend i18n:** StringsTest extended with navProfile, navAdmin, navPlugins, navSync in englishStrings_hasNavigationStrings; new test allLanguages_haveProfileAndAdminNav ensures all 7 languages have non-empty navProfile and navAdmin. Screens and components (ScreensTest, SharedWithMeScreenTest, BreadcrumbsLogicTest, SortDialogTest, AdvancedSearchDialogTest, etc.) already present; no new screen test added. — **Applied Phase 3 (2026-02-22):** **Frontend composeApp:** **SharesViewModelTest** added for SharesViewModel: loadShares (success/error), copyLink, clearClipboardLink, deleteShare (success/error), clearError. Uses FakeGetMySharesUseCase, FakeDeleteShareUseCase, FakeGetShareUrlUseCase and ViewModelTestBase.runTestWithMain. Navigation (AppRoutes, RouteMatch, RoutePaths) already covered by AppRoutesTest. — **Applied Phase 2 (2026-02-22):** **Backend infrastructure:** **CollaborationContentApplyTest** added for `applyOperationToContent`: Insert (start, middle, end, position clamped, empty string), Delete (range, full content, clamped), Retain unchanged. Covers persistence helper used by ExposedCollaborationRepository. — **Applied Phase 1 (2026-02-22):** **Backend api:** ApiResponseTest extended with **UserToAdminResponseTest** – `toAdminResponse maps user to AdminUserResponse with usedBytes`, `toAdminResponse with default usedBytes yields zero` (covers User.toAdminResponse and AdminUserResponse). **Backend core/activity:** ActivityLoggerTest extended with `start then publish FileEvent Downloaded with accessedViaShare includes shareId in details` (covers FILE_DOWNLOADED branch with shareId in details) and `when activityRepository create returns Left logger still invokes create and does not throw` (covers logActivity error path when repository returns Either.Left). **Phase 1 checklist:** Core TransactionManager, ActivityLogger, CollaborationOT tests confirmed present and complete; FederationServiceSigning/Maintenance tests present; API ErrorHandling, CronScheduler, PluginManager tests present; DTO mappers extended. — **Snapshot (Feb 2026):** core 66%, plugins-api 80%, composeApp 5%; backend api 12%, infrastructure 13%, plugins 9–17%. **Frontend (Feb 2026):** **FormattingTest** added for `com.vaultstadio.app.utils`: formatFileSize (bytes, KB, MB, GB, TB, decimal rounding), formatRelativeTime, formatDate, getFileIconName, getFileTypeName, isValidEmail, isStrongPassword, truncateText (full coverage of Formatting.kt). StorageUseCaseTest extended with full FakeStorageRepository (configurable getItems, renameItem, toggleStar, trashItem, restoreItem, getTrash, getStarred, getRecent, emptyTrash, deleteItemPermanently, batchDelete, batchMove, batchStar) and 13 new use-case test classes. **Backend (Feb 2026):** UploadSessionManagerTest: added `removeSession returns session even when temp dir cleanup fails` (covers exception path in InMemoryUploadSessionManager); fixed flaky `cleanupExpiredSessions does not remove recent sessions` by using 10.seconds maxAge instead of 1.milliseconds. **RouteExtensionsTest:** added tests for default status parameters. **ApiResponseTest (PagedResultToResponseTest):** `toResponse with partial page sets hasMore true and totalPages`, `toResponse with limit zero yields zero totalPages and page one`. **AppConfigTest (StorageConfigTests):** `StorageType valueOf returns correct enum`, `StorageType has exactly three values`. **Frontend FormattingTest:** `formatFileSize_petabytes` (1 PB), `truncateText_exactLength_returnsAsIs`. **domain/common StorageExceptionTest:** `invalidOperationExceptionHasCorrectErrorCodeAndStatus`, `storageBackendExceptionHasCorrectErrorCodeAndStatus`, `concurrentModificationExceptionHasCorrectErrorCodeAndStatus`, `shareDownloadLimitExceptionHasCorrectErrorCodeAndStatus`, `sharePasswordRequiredExceptionHasCorrectErrorCodeAndStatus`, `pluginLoadExceptionHasCorrectErrorCodeAndStatus`. **api/dto ShareLinkToResponseTest:** `toResponse builds url from baseUrl and token`, `toResponse sets hasPassword true when password is not null`. **Frontend SyncUseCaseTest:** `ResolveConflictUseCaseTest invoke_propagatesError`, `RemoveDeviceUseCaseTest invoke_propagatesError`, `GetConflictsUseCaseTest invoke_propagatesError`. **ApiResponseTest (api/dto):** **StorageQuotaToResponseTest** (`toResponse maps all fields`, `toResponse with null quotaBytes`), **ActivityToResponseTest** (`toResponse maps all fields`), **UserToResponseTest** (`toResponse maps user to UserResponse`). **VersionUseCaseTest (frontend):** `RestoreVersionUseCaseTest invoke_propagatesError`, `CompareVersionsUseCaseTest invoke_propagatesError`, `DeleteVersionUseCaseTest invoke_propagatesError`, `CleanupVersionsUseCaseTest invoke_propagatesError`. **ApiResponseTest (api/dto):** **StorageItemToResponseTest** (`toResponse maps item to StorageItemResponse`, `toResponse with metadata passes metadata to response`). **AdminUseCaseTest (frontend):** `UpdateUserQuotaUseCaseTest invoke_propagatesError`, `UpdateUserRoleUseCaseTest invoke_propagatesError`, `UpdateUserStatusUseCaseTest invoke_propagatesError`. **Tests:** All tests pass; Android unit tests exclude ViewModel and UploadManager tests (they require main looper; run on desktopTest). **Plugins:** AIClassificationPluginTest getConfigurationSchema (non-null, groups with key "classification"); ImageMetadataPluginTest getConfigurationSchema (non-null, groups not empty). — Prior: Phase 3 ViewModels; Phase 2 VideoMetadataPluginTest, FullTextSearchPluginTest; Phase 4 SharedWithMeScreenTest, BreadcrumbsLogicTest, i18n; StorageItemTest; etc. See TESTING.md § Untestable Components for Redis-backed classes.

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

**How to regenerate this snapshot:** From repo root run `make test-coverage` (or from `backend/` run `make test-coverage`, and from `frontend/` run `make test-coverage`). Then open:
- **Backend:** each reported module’s `build/reports/jacoco/test/html/index.html` (e.g. `backend/core/build/reports/jacoco/test/html/index.html`, `backend/api/...`, `backend/infrastructure/...`, `backend/plugins-api/...`, `backend/plugins/<name>/...`).
- **Frontend:** `frontend/composeApp/build/reports/jacoco/jacocoTestReport/html/index.html`.

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
| 1        | **domain.usecase.*** (auth, share, storage, sync, version, admin, ai, collaboration, federation, metadata, activity, plugin) | Improved (storage) | **Storage:** GetFolderItems, RenameItem, ToggleStar, TrashItem, RestoreItem, GetTrash, GetStarred, GetRecent, EmptyTrash, DeleteItem, BatchDelete, BatchMove, BatchStar covered in StorageUseCaseTest. Add same pattern for remaining use-case packages (auth, share, etc.) |
| 2        | **data.repository** | 2%    | Test repository implementations with fake services or test doubles |
| 3        | **feature.*** (files, auth, main, upload) | 0–7% | Test ViewModels and feature logic in commonTest; mock use cases |
| 4        | **navigation**      | 61%   | Keep; add branch coverage if new routes |
| 5        | **domain.upload**   | 76%   | Keep |
| 6        | **domain.model**    | 59%   | Extend model tests for new types |
| 7        | **i18n**            | 50%   | Extend StringsTest |
| –        | **ui.screens.***    | 0%    | Mostly Composables; cover via state/callback tests or document as UI-only |

### Quick wins

- **Frontend:** Add one test file per use-case package (auth, share, storage, etc.) with a fake repository and tests for success/error paths. Each use case is a thin wrapper around the repository, so tests are small and repetitive. **Done for storage:** StorageUseCaseTest now covers GetFolderItems, RenameItem, ToggleStar, TrashItem, RestoreItem, GetTrash, GetStarred, GetRecent, EmptyTrash, DeleteItem, BatchDelete, BatchMove, BatchStar (same FakeStorageRepository pattern).
- **Backend api:** Add or extend unit tests for non-route code (e.g. AppConfigTest, SecurityTest, route extension helpers) so the api module coverage rises even if route handlers stay integration-only.
- **Backend infrastructure:** Add tests for any Exposed* repository method not yet covered; add edge-case tests for LocalStorageBackend.

---

## Table of Contents

1. [Project structure (modules)](#project-structure-modules)
2. [Current coverage snapshot (Jacoco)](#current-coverage-snapshot-jacoco)
3. [Goals and Targets](#goals-and-targets)
4. [Current State Summary](#current-state-summary)
5. [Coverage analysis – where to focus efforts](#coverage-analysis--where-to-focus-efforts)
6. [Gap Analysis by Module](#gap-analysis-by-module)
7. [**Exhaustive checklist – full coverage (no gaps)**](#exhaustive-checklist--full-coverage-no-gaps)
8. [Phased Action Plan](#phased-action-plan)
9. [CI and Quality Gates](#ci-and-quality-gates)
10. [Success Criteria](#success-criteria)

---

## Goals and Targets

| Goal | Target |
|------|--------|
| **Line coverage (backend)** | ≥ 80% per **reported** module: core, api, infrastructure, plugins-api, and each plugin (image-metadata, video-metadata, fulltext-search, ai-classification). Domain and application layers are tested transitively. |
| **Branch coverage (critical paths)** | Auth, storage CRUD, share, sync, and versioning: key branches covered |
| **Frontend (composeApp)** | All ViewModels and shared business logic covered; UI components via state/callback tests |
| **No untested public API** | Every public function in domain/services and API route handlers has at least one test (or is documented as untestable in TESTING.md) |
| **CI** | Coverage reports generated and uploaded; optional coverage gate (e.g. fail if project drops > 1%) |

Existing Codecov config (`codecov.yml`) uses `range: "60..80"` and `threshold: 1%`; this plan aims to reach and hold the upper part of that range and then consider raising the floor.

---

## Current State Summary

| Area | Test files (approx.) | Coverage level | Notes |
|------|----------------------|----------------|-------|
| **Backend api** (reported) | 19+ route tests, 6+ config/service | 12% instr. | Route test classes exist but many handlers not executed; storage 3%, metadata/ai/auth 0%. api.service 68%, config 32%. UploadSessionManager, ThumbnailCache, RouteExtensions have tests. |
| **Backend core** (reported) | 11+ service, model, event, AI | 66% instr. | domain.service 62%, domain.event 76%, exception 97%; domain.repository 41%. Domain/application layers tested transitively. |
| **Backend plugins-api** (reported) | 6 | **80%** | Plugin, PluginContext, Hooks, MetadataExtractor, PluginLifecycle, PluginConfiguration |
| **Backend infrastructure** (reported) | 12+ | 13% | Persistence 9%, storage 29%, security 100%; Exposed repos, LocalStorageBackend, S3, BCrypt have tests |
| **Backend plugins** (reported) | 4 modules | 9–16% | image-metadata, video-metadata, fulltext-search, ai-classification; extraction logic largely untested |
| **Frontend composeApp** (reported) | 41+ in commonTest | ~5% (desktop report) | Single report; includes domain, data, feature code. domain.upload 76%, navigation 61%, domain.model 59%, i18n 50%, utils (FormattingTest); screens/features mostly 0%. |

---

## Gap Analysis by Module

### 1. backend/plugins-api (Priority: Maintain – target met)

**Current:** ~80% instruction, 57% branch. Tests exist for Plugin, PluginContext, PluginConfiguration, MetadataExtractor, PluginLifecycle, Hooks.

**Remaining gaps:** Context package ~52%; some DefaultImpls and branches. Optional: add tests for any new public APIs and edge cases.

**Action:** Keep coverage ≥80%; add branch/context coverage if adding new plugin SDK surface.

---

### 2. backend/core (Priority: High)

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

### 3. backend/api (Priority: High)

**Current:** Routes, Security, AppConfig, ErrorHandling, Logging, PluginManager, CronScheduler are tested. Gaps:

| Component | File(s) | Suggested tests |
|-----------|---------|------------------|
| UploadSessionManager | `InMemoryUploadSessionManager.kt` | Create session; add parts; complete; abort; expiry |
| ThumbnailCache | `ThumbnailCache.kt` | Get (hit/miss); put; invalidate; size/limits if applicable |
| RouteExtensions | `RouteExtensions.kt` | Any extension functions used by routes (e.g. user/session extraction); test with mock call context |

**Optional (lower priority):** Application.kt, Database.kt, Serialization.kt, Swagger.kt are wiring/bootstrap; document as “covered by integration/run” or add minimal smoke tests if desired.

**Action:** Add `UploadSessionManagerTest`, `ThumbnailCacheTest`, and tests for `RouteExtensions` (or embed in existing route test utilities).

---

### 4. backend/infrastructure (Priority: Medium)

**Current:** 13% instruction, 8% branch. Persistence 9%, storage 29%, security 100%. Exposed* repositories, LocalStorageBackend, S3StorageBackend, BCrypt have test classes but many code paths (persistence) are not covered by unit tests.

**Action:** Increase persistence coverage by exercising more repository methods and branches in unit tests (or document reliance on integration tests). Add edge-case tests for repositories involved in sync/federation/collaboration. Extend storage backend tests where feasible.

---

### 5. backend/plugins (Priority: Medium)

**Current coverage:** image-metadata 13% (1% branch), video-metadata 9% (0% branch), fulltext-search 14% (0% branch), ai-classification 16% (0% branch). Each plugin has a test class but most code is extraction/runtime logic that is not exercised in unit tests.

**Focus:**

- Image/Video metadata: test entry points, configuration, and error paths (malformed input, unsupported format); extraction may stay low if it relies on native/IO.
- Fulltext-search: indexing lifecycle, empty content, error handling.
- AI-classification: error handling, timeouts, fallbacks; mock external AI calls.

**Action:** Add tests for public API, config, and mockable boundaries; improve branch coverage where logic is testable without real files or external services.

---

### 6. frontend/composeApp (Priority: Medium – High for business logic)

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

## Exhaustive checklist – full coverage (no gaps)

This section is the **master checklist** to reach full coverage: every test to add and every piece of code to cover. Use it to avoid leaving any area out. Tick items as you complete them; run `make test-coverage` after each phase and verify the reported modules meet targets.

### How to use this checklist

- **Backend:** For each **main** source file, ensure there is a corresponding test (same module or a module that depends on it). “Covered transitively” = exercised by api/core tests; “Unit test” = dedicated test class in that module.
- **Frontend:** Coverage is reported only for `composeApp` (desktopTest). Code in domain/data/feature is included when exercised by composeApp tests. Ensure every ViewModel, use case, and testable util has at least one test in `composeApp/src/test` (commonTest).
- After implementing tests, run `make test-coverage` and open the Jacoco HTML reports; confirm no new uncovered file and that instruction/branch coverage per module meets the targets in [Goals and Targets](#goals-and-targets).

---

### Backend – file-by-file checklist

#### backend/api (target ≥80% instruction)

| Main source file | Test / coverage | Action if missing |
|------------------|-----------------|-------------------|
| `config/AppConfig.kt` | AppConfigTest | Add StorageConfigTests, env parsing |
| `config/Security.kt` | SecurityTest | Extend for all branches |
| `config/Routing.kt` | Route tests | Covered by route tests or add RoutingTest |
| `config/Koin.kt` | Integration / bootstrap | Document as wiring; optional smoke test |
| `config/Database.kt` | Integration | Document as wiring |
| `config/Serialization.kt` | Integration | Document as wiring |
| `config/Swagger.kt` | Optional | Document or minimal test |
| `Application.kt` | Integration | Document as bootstrap |
| `service/InMemoryUploadSessionManager.kt` | UploadSessionManagerTest | Extend for all branches (create, addPart, complete, abort, expiry, cleanup) |
| `service/ThumbnailCache.kt` | ThumbnailCacheTest | Extend for hit/miss, put, invalidate, limits |
| `routes/RouteExtensions.kt` | RouteExtensionsTest | Extend for every extension (user/session extraction, status defaults) |
| `dto/ApiResponse.kt` | ApiResponseTest | Extend for every DTO mapper (StorageItem, User, Activity, ShareLink, PagedResult, etc.) |
| `middleware/ErrorHandling.kt` | ErrorHandling test | Add or extend middleware test |
| `middleware/Logging.kt` | Optional | Document or minimal test |
| `plugins/PluginManager.kt` | PluginManager test | Add or extend |
| `plugins/CronScheduler.kt` | CronScheduler test | Add or extend |
| `routes/auth/AuthRoutes.kt` | AuthRoutesTest | Use testApplication + module() for handler execution or document integration-only |
| `routes/storage/StorageRoutes.kt` | StorageRoutesTest | Same |
| `routes/storage/BatchRoutes.kt` | BatchRoutesTest | Same |
| `routes/storage/ChunkedUploadRoutes.kt` | ChunkedUploadRoutesTest | Same |
| `routes/storage/FolderUploadRoutes.kt` | FolderUploadRoutesTest | Same |
| `routes/storage/ThumbnailRoutes.kt` | ThumbnailRoutesTest | Same |
| `routes/storage/S3Routes.kt` | S3RoutesTest | Same |
| `routes/storage/S3Handlers.kt` | S3RoutesTest / S3HandlersTest | Same |
| `routes/storage/S3Operations.kt` | Unit test or S3RoutesTest | Add S3OperationsTest if not covered |
| `routes/storage/WebDAVRoutes.kt` | WebDAVRoutesTest | Same |
| `routes/storage/WebDAVHandlers.kt` | WebDAVHandlersTest or WebDAVRoutesTest | Same |
| `routes/storage/WebDAVOperations.kt` | Unit test or WebDAV test | Add if not covered |
| `routes/share/ShareRoutes.kt` | ShareRoutesTest | Same |
| `routes/plugin/PluginRoutes.kt` | PluginRoutesTest | Same |
| `routes/metadata/SearchRoutes.kt` | SearchRoutesTest | Same |
| `routes/metadata/MetadataRoutes.kt` | MetadataRoutesTest | Same |
| `routes/ai/AIRoutes.kt` | AIRoutesTest | Same |
| `routes/ai/AIHandlers.kt` | AIHandlersTest or AIRoutesTest | Same |
| `routes/admin/AdminRoutes.kt` | AdminRoutesTest | Same |
| `routes/admin/UserRoutes.kt` | UserRoutesTest | Same |
| `routes/health/HealthRoutes.kt` | HealthRoutesTest | Same |
| `routes/activity/ActivityRoutes.kt` | ActivityRoutesTest | Same |
| `routes/version/VersionRoutes.kt` | VersionRoutesTest | Same |
| `routes/sync/SyncRoutes.kt` | SyncRoutesTest | Same |
| `routes/sync/SyncHandlers.kt` | SyncHandlersTest or SyncRoutesTest | Same |
| `routes/collaboration/CollaborationRoutes.kt` | CollaborationRoutesTest | Same |
| `routes/collaboration/CollaborationWebSocket.kt` | WebSocket test or document | Add or document |
| `routes/collaboration/CollaborationRoutesModels.kt` | Model test | Add if DTOs not covered |
| `routes/federation/FederationRoutes.kt` | FederationRoutesTest | Same |
| `routes/federation/FederationHandlers.kt` | FederationHandlersTest or FederationRoutesTest | Same |
| **Application use cases** (auth, storage, share, plugin, metadata, health, chunkedupload, admin, user) | Each has *UseCaseTest in api | Extend for missing branches |

#### backend/core (target ≥80% instruction)

| Main source file | Test / coverage | Action if missing |
|------------------|-----------------|-------------------|
| `domain/event/StorageEvent.kt` | StorageEventTest / event tests | Extend event tests |
| `domain/event/EventBus.kt` | EventBusTest | Add or extend |
| `domain/event/AdvancedEvents.kt` | AdvancedEventsTest | Add or extend |
| `domain/service/TransactionManager.kt` | TransactionManagerTest | Add: run in transaction, rollback, commit |
| `domain/service/ActivityLogger.kt` | ActivityLoggerTest | Add: log with types, verify repo calls |
| `domain/service/UploadSessionManager.kt` (interface) | Covered by api impl test | N/A |
| `domain/service/MultipartUploadManager.kt` | Unit test | Add if logic present |
| `domain/service/LockManager.kt` | LockManagerTest | Add or extend |
| `domain/model/*` (FileVersion, StorageItemMetadata, Sync, Federation, Collaboration) | Model tests | Add per-model tests |
| `domain/repository/*` (interfaces) | Covered by impl tests | N/A |
| core/storage: StorageService, StorageServiceQueries, StorageServiceMutations, StorageServiceWrite | StorageServiceTest | Extend for every public function and branch |
| core/share: ShareService.kt | ShareServiceTest | Extend for all branches |
| core/auth: UserService.kt | UserServiceTest | Add or extend |
| core/version: FileVersionService.kt | FileVersionServiceTest | Extend for all branches |
| core/sync: SyncService.kt, DeltaSync.kt | SyncServiceTest | Extend for all branches |
| core/activity: ActivityLogger.kt | ActivityLoggerTest | Add |
| core/federation: FederationService, FederationServiceSigning, FederationServiceMaintenance, FederationCrypto | FederationServiceTest | Extend for signing, maintenance, crypto |
| core/collaboration: CollaborationService.kt, CollaborationOT.kt | CollaborationServiceTest, CollaborationOTTest | Add CollaborationOTTest; extend service |
| core/ai: AIService.kt, AIProvider.kt, providers (OpenRouter, Ollama, LMStudio) | AIServiceTest, provider tests | Extend for all providers and error paths |

#### backend/infrastructure (target ≥80% instruction)

| Main source file | Test / coverage | Action if missing |
|------------------|-----------------|-------------------|
| `persistence/ExposedTransactionManager.kt` | TransactionManagerTest (impl) | Add or extend |
| `persistence/ExposedStorageItemRepository.kt` | ExposedStorageItemRepositoryTest | Extend every method and branch |
| `persistence/ExposedUserRepository.kt` | ExposedUserRepositoryTest | Same |
| `persistence/ExposedShareRepository.kt` | ExposedShareRepositoryTest | Same |
| `persistence/ExposedFileVersionRepository.kt` | ExposedFileVersionRepositoryTest | Same |
| `persistence/ExposedMetadataRepository.kt` | ExposedMetadataRepositoryTest | Same |
| `persistence/ExposedSyncRepository.kt` | ExposedSyncRepositoryTest | Same |
| `persistence/ExposedFederationRepository.kt` | ExposedFederationRepositoryTest | Same |
| `persistence/ExposedCollaborationRepository.kt` | ExposedCollaborationRepositoryTest | Same |
| `persistence/Exposed*Mappers.kt` | Covered by repo tests | Extend repo tests to hit mappers |
| `persistence/CollaborationContentApply.kt` | Unit test | Add CollaborationContentApplyTest |
| `persistence/entities/Tables.kt` | Covered by repo tests | N/A |
| `storage/LocalStorageBackend.kt` | LocalStorageBackendTest | Extend for all operations and errors |
| `storage/S3StorageBackend.kt` | S3StorageBackendTest | Same |
| `security/BCryptPasswordHasher.kt` | BCrypt test | Extend for hash/verify branches |

#### backend/plugins-api (target ≥80% – maintain)

| Main source file | Test / coverage | Action if missing |
|------------------|-----------------|-------------------|
| Plugin, PluginContext, Hooks, MetadataExtractor, PluginLifecycle, PluginConfiguration | Existing tests | Maintain; add branch coverage for context (52%) |

#### backend/plugins (each target ≥80% instruction)

| Plugin | Main files | Test / coverage | Action if missing |
|--------|------------|-----------------|-------------------|
| image-metadata | ImageMetadataPlugin, extraction | ImageMetadataPluginTest | Add config, error paths, unsupported format |
| video-metadata | VideoMetadataPlugin, extraction | VideoMetadataPluginTest | Same |
| fulltext-search | FullTextSearchPlugin, indexing | FullTextSearchPluginTest | Add indexing lifecycle, empty content, errors |
| ai-classification | AIClassificationPlugin, AI calls | AIClassificationPluginTest | Add error handling, timeouts; mock AI |

#### backend/domain and application (covered transitively)

Domain and application modules do not produce their own Jacoco report; they are exercised when core and api tests run. Ensure every **public** function in domain services and application use cases is hit by at least one test in core or api (see api application use case tests and core service tests).

- **domain/** (common, storage, auth, share, activity, admin, version, sync, federation, collaboration, metadata, plugin): model and exception tests exist; extend for any new public API.
- **application/** (auth, storage, share, user, admin, activity, metadata, version, sync, plugin, chunkedupload, health, ai): each use case has *UseCaseTest in api; extend for missing branches.

---

### Frontend – exhaustive checklist (composeApp report target: raise from ~5% to maximum testable)

Coverage is measured only for code executed by `composeApp` desktopTest. The following lists **every area** that should have at least one test so no testable piece is left out.

#### composeApp – app layer (src/main in composeApp)

| Component / file | Test file | Action if missing |
|------------------|-----------|-------------------|
| `utils/Formatting.kt` | FormattingTest | Done – keep and extend for new helpers |
| `navigation/AppRoute.kt`, RoutePaths, RouteMatch, MainDestination | AppRoutesTest, RouteMatchTest | Extend for new routes and branches |
| `navigation/RootComponent.kt`, RootContent.kt | Document or state test | Optional: navigation state test |
| `viewmodel/AppViewModel` | AppViewModelTest, AppViewModelDetailedTest, etc. | Extend for new flows |
| `viewmodel/NavDestination.kt` | Covered by navigation tests | N/A |
| `feature/upload/UploadManager.kt`, LocalUploadManager.kt | UploadManagerTest | Extend for all states and error paths |
| `feature/files/FilesViewModel.kt` | FilesModeAndTrashStarTest, FilesUploadAndMoveTest | Add direct ViewModel test if needed for uncovered logic |
| `feature/files/UploadAction.kt` | Covered by upload tests | Extend if branches missing |
| `feature/main/MainComponent.kt`, MainContent.kt | Covered by AppViewModel / integration | Optional state test |
| `di/AppModule.kt`, AppModuleDsl.kt, InMemoryTokenStorage.kt | Integration / document | Document or minimal test |
| `config/AppConfig.kt` | Optional | Add if logic present |
| `platform/*` (Download, DragDrop, FilePicker, etc.) | expect/actual – document | Document as platform; test any shared logic |

#### composeApp – UI (screens and components)

| Area | Test file | Action if missing |
|------|-----------|-------------------|
| Screens (Settings, Admin, Profile, Security, SharedWithMe, Activity, Sync, VersionHistory, Federation, Collaboration, AI, Login) | ScreensTest, SecurityScreenTest, SharedWithMeScreenTest, etc. | Add state/callback tests for any screen not yet covered |
| Components: MoveDialog, ContextMenu, SelectionToolbar, MainSidebar, Breadcrumbs, DropZone, DragOverlay | DragDropComponentsTest, BreadcrumbsLogicTest | Extend for every callback and branch |
| File components: FileItem, FileInfoPanel, MetadataPanel, VersionPanel, etc. | FileItemTest, FileInfoPanelTest, etc. | Extend for new behaviour |
| Dialogs: SortDialog, AdvancedSearchDialog, FilterDialog, UploadDialog | SortDialogTest, AdvancedSearchDialogTest | Extend for new dialogs |

#### Frontend – domain (included in composeApp report when exercised)

| Package | Test file | Action if missing |
|---------|-----------|-------------------|
| domain.upload | UploadQueueEntryTest, ChunkedFileSource tests | Keep; extend for new types |
| domain.model | ModelTests | Extend for new models |
| domain.usecase.storage | StorageUseCaseTest | Done – extend for new use cases |
| domain.usecase.auth | AuthViewModelTest / AuthUseCaseTest | Extend for error paths |
| domain.usecase.share | ShareUseCaseTest | Extend for all use cases and errors |
| domain.usecase.sync | SyncUseCaseTest | Same |
| domain.usecase.version | VersionUseCaseTest | Same |
| domain.usecase.admin | AdminUseCaseTest | Same |
| domain.usecase.activity | ActivityUseCaseTest | Same |
| domain.usecase.metadata | MetadataUseCaseTest | Same |
| domain.usecase.plugin | PluginUseCaseTest | Same |
| domain.usecase.collaboration | CollaborationUseCaseTest | Same |
| domain.usecase.federation | FederationUseCaseTest | Same |
| domain.usecase.ai | AIUseCaseTest | Same |
| domain.usecase.config | GetStorageUrlsUseCaseTest | Extend for new config use cases |

#### Frontend – data (included in composeApp report when exercised)

| Package | Test file | Action if missing |
|---------|-----------|-------------------|
| data.repository (storage, auth, admin, sync, etc.) | Test via use case tests or add Fake* in tests | Add repository tests with fakes or test doubles for uncovered impls |
| data.*.service, data.*.mapper | Covered by use case / ViewModel tests | Extend use case tests to hit all branches |

#### Frontend – feature ViewModels and logic

| ViewModel / feature | Test file | Action if missing |
|---------------------|-----------|-------------------|
| AuthViewModel | AuthViewModelTest | Extend for all states |
| FilesViewModel | FilesModeAndTrashStarTest, FilesUploadAndMoveTest | Add FilesViewModelTest if branches missing |
| AdminViewModel | AdminViewModelTest | Extend |
| ProfileViewModel | ProfileViewModelTest | Extend |
| SettingsViewModel | SettingsViewModelTest | Extend |
| ChangePasswordViewModel | ChangePasswordViewModelTest | Extend |
| SecurityViewModel | SecurityViewModelTest | Extend |
| PluginsViewModel | PluginsViewModelTest | Extend |
| SyncViewModel | SyncScreenTest / ViewModel test | Add SyncViewModelTest if needed |
| VersionHistoryViewModel | VersionHistoryScreenTest | Same |
| SharesViewModel | Add SharesViewModelTest | Add if not present |
| SharedWithMeViewModel | SharedWithMeScreenTest | Extend |
| ActivityViewModel | ActivityScreenTest | Extend |
| CollaborationViewModel | CollaborationScreenTest | Extend |
| FederationViewModel | FederationScreenTest | Extend |
| AIViewModel | AIScreenTest | Extend |

#### Frontend – core/resources (i18n)

| Area | Test file | Action if missing |
|------|-----------|-------------------|
| Strings, all languages | StringsTest | Extend for every new key and placeholder; allLanguages_haveConsistentStrings |

#### Frontend – API models (composeApp api package)

| Area | Test file | Action if missing |
|------|-----------|-------------------|
| Version, Sync, Collaboration, Federation, AI API models | VersionApiModelsTest, SyncApiModelsTest, etc. | Add test for any new DTO or mapper |

---

### Verification checklist (after each phase)

- [ ] Run `make test-coverage` from repo root.
- [ ] Backend: open each module’s `build/reports/jacoco/test/html/index.html` (core, api, infrastructure, plugins-api, each plugin). Confirm instruction coverage ≥80% (or document exception).
- [ ] Frontend: open `frontend/composeApp/build/reports/jacoco/jacocoTestReport/html/index.html`. Confirm no new uncovered package and that domain/usecase/viewmodel coverage has increased.
- [ ] No main source file in the tables above is left without a tick in “Test / coverage” (either existing test or “Action if missing” done).
- [ ] Update “Recent coverage improvements” in this document with the new snapshot and new tests added.

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
- **5.1:** CI runs jacoco for backend **reported modules** (core, api, infrastructure, plugins-api, plugins:image-metadata, video-metadata, fulltext-search, ai-classification) and frontend (composeApp); coverage job uploads to Codecov. Backend domain/* and application/* are tested transitively when core and api tests run. See [.github/workflows/ci.yml](../../.github/workflows/ci.yml).
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
2. **Backend (reported modules: core, api, infrastructure, plugins-api, each plugin):** No untested public API in domain/services and route handlers (or documented in TESTING.md); ≥ 80% line coverage per reported module where feasible. Domain and application modules are covered indirectly.
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
