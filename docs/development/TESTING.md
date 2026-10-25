# VaultStadio Testing Guide

This document describes the testing strategy, current coverage, and known limitations of the VaultStadio test suite.

## Table of Contents

1. [Test Overview](#test-overview)
2. [Running Tests](#running-tests)
3. [Test Categories](#test-categories)
4. [Coverage Summary](#coverage-summary)
5. [Untestable Components](#untestable-components)
6. [Phase 6 (Optional) – Advanced quality](#phase-6-optional--advanced-quality)
7. [Future Improvements](#future-improvements)

---

## Test Overview

VaultStadio uses a comprehensive testing approach with **65+ test files** covering:

- **Unit Tests**: Testing individual components in isolation
- **Integration Tests**: Testing Ktor API routes with `testApplication`
- **Model Tests**: Validating data classes and enums
- **Logic Tests**: Testing business logic and state management

### Testing Frameworks

| Module | Framework | Purpose |
|--------|-----------|---------|
| Backend (Kotlin) | JUnit 5 + Mockk | Unit and integration testing |
| Backend API | Ktor Test Host | HTTP route testing |
| Frontend (Compose) | kotlin-test | Shared logic testing |
| Shared | kotlin-test | Cross-platform model testing |

---

## Running Tests

### Run All Tests

```bash
./gradlew test
```

### Run Specific Module Tests

```bash
# Backend API tests
./gradlew :kotlin-backend:api:test

# Backend Core tests
./gradlew :kotlin-backend:core:test

# Backend Infrastructure tests
./gradlew :kotlin-backend:infrastructure:test

# Plugin tests
./gradlew :kotlin-backend:plugins:image-metadata:test
./gradlew :kotlin-backend:plugins:video-metadata:test
./gradlew :kotlin-backend:plugins:fulltext-search:test
./gradlew :kotlin-backend:plugins:ai-classification:test

# Phase 6 tests only
./gradlew :kotlin-backend:api:test --tests "*S3RoutesTest" --tests "*WebDAVRoutesTest" --tests "*SyncRoutesTest" --tests "*VersionRoutesTest"
./gradlew :kotlin-backend:core:test --tests "*FileVersionServiceTest" --tests "*SyncServiceTest" --tests "*FederationServiceTest" --tests "*CollaborationServiceTest"

# Frontend tests
./gradlew :compose-frontend:composeApp:desktopTest
```

**Note:** ViewModel and UploadManager tests in `commonTest` use `ViewModelTestBase.withMainDispatcher` / `runTestWithMain` (UnconfinedTestDispatcher for Main), so they run on both **desktopTest** and Android unit tests without a real main looper. Coverage reports use desktopTest for the frontend.

### Run with Coverage Report

**All modules (same as CI / Codecov):**

```bash
make test-coverage
```

Or with Gradle directly:

```bash
./gradlew :kotlin-backend:core:jacocoTestReport \
  :kotlin-backend:api:jacocoTestReport \
  :kotlin-backend:infrastructure:jacocoTestReport \
  :kotlin-backend:plugins-api:jacocoTestReport \
  :kotlin-backend:plugins:image-metadata:jacocoTestReport \
  :kotlin-backend:plugins:video-metadata:jacocoTestReport \
  :kotlin-backend:plugins:fulltext-search:jacocoTestReport \
  :kotlin-backend:plugins:ai-classification:jacocoTestReport \
  :compose-frontend:composeApp:jacocoTestReport \
  --continue
```

**Output locations:**

| Module | HTML report | XML (for Codecov) |
|--------|-------------|-------------------|
| Backend (core, api, infra, plugins-api, each plugin) | `kotlin-backend/<module>/build/reports/jacoco/test/html/` | `kotlin-backend/<module>/build/reports/jacoco/test/jacocoTestReport.xml` |
| Frontend (composeApp) | `compose-frontend/composeApp/build/reports/jacoco/jacocoTestReport/html/` | `compose-frontend/composeApp/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml` |

**Validate Codecov config locally:**

```bash
curl -s --data-binary @codecov.yml https://codecov.io/validate
```

**Optional: upload from local** (to verify Codecov accepts your reports): install the [Codecov CLI](https://github.com/codecov/codecov-cli), set `CODECOV_TOKEN`, then run an upload with the same XML paths the CI uses (see `.github/workflows/ci.yml`).

### Codecov (CI)

Coverage is uploaded to [Codecov](https://codecov.io) on every push and pull request. To enable uploads:

1. Add the repository to Codecov (e.g. at [codecov.io](https://codecov.io)).
2. Add the `CODECOV_TOKEN` secret to the GitHub repository (Settings → Secrets and variables → Actions).

The CI workflow runs coverage for all backend modules (core, api, infrastructure, plugins-api, and the four plugins: image-metadata, video-metadata, fulltext-search, ai-classification) and for the frontend (composeApp desktop tests). Configuration is in [codecov.yml](../../codecov.yml) at the repo root.

**Coverage gate (Phase 5.2):** [codecov.yml](../../codecov.yml) sets `status.project.default.threshold: 1%`. The Codecov status check on PRs will fail if **patch coverage** (coverage of new or changed lines) drops by more than 1%, so coverage regressions are visible and enforced.

---

## Test Categories

### 1. Backend API Route Tests (15 files)

Location: `kotlin-backend/api/src/test/kotlin/com/vaultstadio/api/routes/`

| Test File | Coverage |
|-----------|----------|
| `AuthRoutesTest.kt` | Login, register, logout, session |
| `StorageRoutesTest.kt` | CRUD operations, file listing |
| `ShareRoutesTest.kt` | Share creation, access, deletion |
| `SearchRoutesTest.kt` | Full-text and metadata search |
| `AdminRoutesTest.kt` | User management, system stats |
| `ActivityRoutesTest.kt` | Activity logging and querying |
| `BatchRoutesTest.kt` | Batch operations (delete, move, copy) |
| `ChunkedUploadRoutesTest.kt` | Large file uploads |
| `FolderUploadRoutesTest.kt` | Folder structure uploads |
| `ThumbnailRoutesTest.kt` | Thumbnail generation and retrieval |
| `PluginRoutesTest.kt` | Plugin management |
| `HealthRoutesTest.kt` | Health check endpoints |
| `UserRoutesTest.kt` | User profile and settings |
| `MetadataRoutesTest.kt` | Item metadata operations |
| `AIRoutesTest.kt` | AI provider management |
| `S3RoutesTest.kt` | S3-compatible API, multipart uploads |
| `WebDAVRoutesTest.kt` | WebDAV protocol, lock management |
| `SyncRoutesTest.kt` | Sync protocol DTOs |
| `VersionRoutesTest.kt` | File versioning DTOs |

### 2. Core Service Tests (7 files)

Location: `kotlin-backend/core/src/test/kotlin/com/vaultstadio/core/domain/service/`

| Test File | Coverage |
|-----------|----------|
| `StorageServiceTest.kt` | File/folder operations |
| `UserServiceTest.kt` | User management, authentication |
| `ShareServiceTest.kt` | Share link operations |
| `FileVersionServiceTest.kt` | Version history, restore, retention |
| `SyncServiceTest.kt` | Device registration, change tracking |
| `FederationServiceTest.kt` | Instance discovery, federated sharing |
| `CollaborationServiceTest.kt` | Session management, OT operations |

### 3. AI Provider Tests (4 files)

Location: `kotlin-backend/core/src/test/kotlin/com/vaultstadio/core/ai/`

| Test File | Coverage |
|-----------|----------|
| `AIServiceTest.kt` | AI service orchestration |
| `OllamaProviderTest.kt` | Ollama local AI |
| `LMStudioProviderTest.kt` | LM Studio local AI |
| `OpenRouterProviderTest.kt` | OpenRouter cloud AI |

### 4. Infrastructure Tests (9 files)

Location: `kotlin-backend/infrastructure/src/test/kotlin/`

| Test File | Coverage |
|-----------|----------|
| `ExposedStorageItemRepositoryTest.kt` | Storage item CRUD |
| `ExposedUserRepositoryTest.kt` | User CRUD |
| `ExposedMetadataRepositoryTest.kt` | Metadata operations |
| `ExposedShareRepositoryTest.kt` | Share link operations |
| `ExposedActivityRepositoryTest.kt` | Activity logging |
| `ExposedSessionRepositoryTest.kt` | Session management |
| `ExposedApiKeyRepositoryTest.kt` | API key operations |
| `BCryptPasswordHasherTest.kt` | Password hashing |
| `LocalStorageBackendTest.kt` | File storage operations |

### 5. Plugin Tests (4 files)

Location: `kotlin-backend/plugins/*/src/test/kotlin/`

| Test File | Coverage |
|-----------|----------|
| `ImageMetadataPluginTest.kt` | Image EXIF extraction |
| `VideoMetadataPluginTest.kt` | Video metadata extraction |
| `FullTextSearchPluginTest.kt` | Document indexing |
| `AIClassificationPluginTest.kt` | AI-based classification |

### 6. Configuration and Middleware Tests (4 files)

Location: `kotlin-backend/api/src/test/kotlin/com/vaultstadio/api/`

| Test File | Coverage |
|-----------|----------|
| `AppConfigTest.kt` | Environment configuration |
| `SecurityTest.kt` | JWT, authentication |
| `ErrorHandlingTest.kt` | Exception handling |
| `LoggingTest.kt` | Request/response logging |

### 7. API Components Tests (2 files)

| Test File | Coverage |
|-----------|----------|
| `CronSchedulerTest.kt` | Cron expression parsing and scheduling |
| `PluginManagerTest.kt` | Plugin lifecycle management |

### 8. Frontend Tests (20+ files)

Location: `compose-frontend/composeApp/src/commonTest/kotlin/`

| Test File / Area | Coverage |
|------------------|----------|
| `viewmodel/AppViewModelTest.kt`, `AppViewModelDetailedTest.kt`, etc. | App state, navigation |
| `feature/auth/AuthViewModelTest.kt` | Login/register validation, clearError |
| `feature/changepassword/ChangePasswordViewModelTest.kt` | Password validation, visibility |
| `feature/security/SecurityViewModelTest.kt` | Sessions, revoke dialog, two-factor |
| `feature/settings/SettingsViewModelTest.kt` | Theme, language, cache |
| `feature/profile/ProfileViewModelTest.kt` | clearError, clearSuccessMessage |
| `feature/admin/AdminViewModelTest.kt` | clearError, loadUsers |
| `feature/plugins/PluginsViewModelTest.kt` | clearError, loadPlugins |
| `ui/screens/ScreensTest.kt` | Screen logic (files, admin, settings, profile, shared, plugins, trash) |
| `ui/screens/SecurityScreenTest.kt` | Security models, sessions, device types |
| `ui/screens/SharedWithMeScreenTest.kt` | SharedWithMeItem, groupBy owner |
| `ui/components/layout/DragDropComponentsTest.kt` | DragOverlay, DropZone, ContextMenu, MoveDialog |
| `ui/components/layout/BreadcrumbsLogicTest.kt` | Breadcrumb display, isHome, isLast |
| `i18n/StringsTest.kt` | Languages, navHome, auth, all languages consistent |
| `domain/upload/UploadQueueEntryTest.kt` | Upload queue, ChunkedFileSource |
| `feature/upload/UploadManagerTest.kt` | Upload destination, minimized state |
| `navigation/AppRoutesTest.kt`, `RouteMatchTest.kt` | Routes, path params |
| `ComponentsTest.kt`, `PlatformTest.kt` | Component logic, platform abstraction |

### 9. Phase 6 Model Tests (4 files)

Location: `kotlin-backend/core/src/test/kotlin/com/vaultstadio/core/domain/model/`

| Test File | Coverage |
|-----------|----------|
| `FileVersionTest.kt` | Version models, retention policies |
| `SyncTest.kt` | Sync device, change, conflict models |
| `FederationTest.kt` | Federation capabilities, signed messages |
| `CollaborationTest.kt` | Session, participant, operation models |

---

## Coverage Summary

| Module | Test Files | Coverage Level |
|--------|------------|----------------|
| Backend API Routes | 19 | High |
| Backend Core Services | 7 | High |
| Backend Core AI | 4 | High |
| Backend Core Domain (Phase 6) | 4 | High |
| Backend Infrastructure | 9 | High |
| Backend Plugins | 4 | High |
| Backend Config/Middleware | 6 | High |
| Frontend ViewModels | 8+ | Medium–High |
| Frontend Screens / Components | 5+ | Medium |
| Shared Models | 4 | High |
| Shared Network | 2 | High |

For the latest instruction/branch coverage snapshot and per-module targets, see [TEST_COVERAGE_ACTION_PLAN.md](TEST_COVERAGE_ACTION_PLAN.md).

### Phase 2 (Backend depth) summary

- **Routes**: Every route module under `kotlin-backend/api/.../routes/` has a corresponding `*RoutesTest` (Health, Plugin, Activity, User, Admin, Search, Metadata, Share, Version, Sync, AI, Collaboration, Federation, WebDAV, Storage, S3, Thumbnail, FolderUpload, ChunkedUpload, Batch, Auth).
- **Plugins**: Image, video, fulltext-search, and AI classification plugins have error-path tests (unsupported MIME types, empty streams, or non-supported types).
- **Repos and infrastructure**: Exposed repositories and storage/security implementations have tests; optional edge-case tests can be added per the action plan.

The full phased plan (goals, gap analysis, Phases 1–6) is in [TEST_COVERAGE_ACTION_PLAN.md](TEST_COVERAGE_ACTION_PLAN.md).

### Phase 3 (Frontend ViewModels and navigation) summary

- **ViewModels:** AppViewModel and navigation state are covered by AppViewModelTest, AppViewModelDetailedTest, AppViewModelFunctionalTest, AppViewModelPhase6Test, and AdvancedFeaturesTest. Files logic is covered by FilesModeAndTrashStarTest and FilesUploadAndMoveTest.
- **UploadManager:** UploadManagerTest covers upload destination (`setUploadDestination` / `getCurrentDestinationFolderId`) and minimized state (`setMinimized` / `isMinimized`).
- **Navigation:** AppRoutes, AppRoute, RoutePaths, and RouteMatch are covered by AppRoutesTest and RouteMatchTest (pathSegments, pathParams, destination). RootComponent/RootContent are integration-level (auth vs main child).
- **Coverage:** Run `./gradlew :compose-frontend:composeApp:desktopTest` and `:compose-frontend:composeApp:jacocoTestReport` to generate the frontend coverage report.

### Phase 4 (Frontend screens and feature logic) summary

- **Screens:** Settings, Admin, Profile, SharedWithMe logic in ScreensTest; SecurityScreenTest (security models, session, login-event); SharedWithMeScreenTest (SharedWithMeItem, groupBy owner, filter by type).
- **Components:** DragDropComponentsTest (DragOverlay, DropZone, ContextMenu, MoveDialog); BreadcrumbsLogicTest (Breadcrumb display name, isHome, isLast, isClickable). SelectionToolbar and MainSidebar are UI-only.
- **Domain:** UploadQueueEntryTest (UploadQueueEntry, FolderUploadEntry, ChunkedFileSource).
- **i18n:** StringsTest (navHome, navSharedWithMe, auth strings, allLanguages_haveNavHome, settingsSecurity).

### Phase 5 (CI and coverage maintenance) — complete

- **CI:** Every push/PR runs backend tests (core, api, infrastructure, plugins-api, all four plugins) and frontend desktop tests; jacoco reports are generated and uploaded to Codecov. See [.github/workflows/ci.yml](../../.github/workflows/ci.yml).
- **Local:** Run `make test-coverage` to generate the same reports as CI (backend + frontend). Contributors should run this before opening a PR; see [CONTRIBUTING.md](../../CONTRIBUTING.md#coverage).
- **Coverage gate:** Implemented in [codecov.yml](../../codecov.yml): `threshold: 1%` so the Codecov status check fails if patch coverage (new/changed code) drops by more than 1%. PRs see coverage status and regressions are blocked.
- **Docs:** [TEST_COVERAGE_ACTION_PLAN.md](TEST_COVERAGE_ACTION_PLAN.md) describes goals, phases, and exit criteria.

**Total Test Files: 70+**

---

## Untestable Components

The following components cannot be effectively tested with unit tests due to their nature:

### 1. Platform-Specific Implementations

**Files:**
- `compose-frontend/composeApp/src/desktopMain/kotlin/com/vaultstadio/app/platform/`
- `compose-frontend/composeApp/src/wasmJsMain/kotlin/com/vaultstadio/app/platform/`
- `compose-frontend/composeApp/src/iosMain/kotlin/com/vaultstadio/app/platform/`

**Reason:** These implementations rely on platform-specific APIs (JVM File APIs, JavaScript Web APIs, iOS Foundation) that are not available in shared test environments.

**Workaround:** 
- Test the shared interface logic in `commonTest`
- Manual testing on each platform
- Consider platform-specific test frameworks (XCTest for iOS, Selenium for Web)

### 2. Compose UI Components (Visual Testing)

**Files:**
- `compose-frontend/composeApp/src/commonMain/kotlin/com/vaultstadio/app/ui/components/`
- `compose-frontend/composeApp/src/commonMain/kotlin/com/vaultstadio/app/ui/screens/`

**Reason:** Visual appearance, layout, and user interactions require a UI testing framework.

**Workaround:**
- Test component logic (state, callbacks) in `ComponentsTest.kt` and `ScreensTest.kt`
- Use Compose UI Testing framework for visual tests:
  ```kotlin
  // Example for future implementation
  @Test
  fun fileItemDisplaysCorrectly() = runComposeUiTest {
      setContent { FileItem(item = testItem, onClick = {}) }
      onNodeWithText("document.pdf").assertExists()
  }
  ```

### 3. Real Database Integration Tests

**Reason:** Tests run against mocked repositories. Full database integration requires:
- Running PostgreSQL instance
- Test data setup/teardown
- Transaction management

**Workaround:**
- Use Testcontainers for integration tests:
  ```kotlin
  @Container
  val postgres = PostgreSQLContainer<Nothing>("postgres:15")
  ```

### 4. Real File System Operations

**Reason:** `LocalStorageBackend` tests create temporary files but don't test actual production paths.

**Workaround:**
- Integration tests with Docker volumes
- Manual testing with real file systems

### 5. External AI Provider Integration

**Files:**
- `kotlin-backend/core/src/main/kotlin/com/vaultstadio/core/ai/providers/`

**Reason:** Tests verify structure but don't call actual AI APIs (Ollama, OpenRouter, LM Studio) as they require:
- Running AI services
- API keys
- Network connectivity

**Workaround:**
- Mock responses in unit tests (current approach)
- Integration tests with local Ollama instance
- Separate E2E test suite with real APIs

### 6. WebSocket/Real-time Features

**Reason:** WebSocket connections require:
- Running server
- Client-server handshake
- Event timing

**Workaround:**
- Mock WebSocket clients in tests
- Integration tests with test server

### 7. Redis-Backed Implementations (0% unit coverage)

**Files:**
- `kotlin-backend/core/.../RedisMultipartUploadManager.kt`
- `kotlin-backend/core/.../RedisLockManager.kt`

**Reason:** These require a running Redis instance and are used in multi-instance production. Unit tests use in-memory implementations (`InMemoryMultipartUploadManager`, `InMemoryLockManager`), which are fully covered.

**Workaround:** Integration tests with Testcontainers (Redis) if multi-instance behaviour needs to be asserted.

### 8. ActivityLogger Event Handlers (partial coverage)

**Reason:** `ActivityLogger` subscribes to many `FileEvent` and `FolderEvent` types; each type has an anonymous handler. Tests cover the main subscription and one or two event types; exercising every event variant would require publishing all event types in a single test or many tests.

**Workaround:** Add tests for critical event types (e.g. `Uploaded`, `Deleted`) as needed; document remaining handlers as covered indirectly by integration.

### 9. File Upload/Download Streams

**Reason:** Large file handling with streams requires:
- Memory management
- Disk I/O
- Network bandwidth

**Workaround:**
- Test with small mock data
- Separate performance test suite

---

## Phase 6 (Optional) – Advanced quality

The following items are documented for future implementation. See [TEST_COVERAGE_ACTION_PLAN.md](TEST_COVERAGE_ACTION_PLAN.md) for the full Phase 6 scope.

### Phase 6.1 – Mutation testing (PIT)

**Goal:** Find weak tests by mutating production code (e.g. changing `>` to `>=`) and checking if tests fail. Low mutation score indicates tests that do not assert enough.

**Scope (if adopted):** Start with `kotlin-backend/core` or `kotlin-backend/api` (or a subset such as domain services). PIT can be run locally or in a separate CI job (it is slow).

**How to add (when desired):**

- Add the [PIT mutation testing](https://pitest.org/) plugin to the root or module `build.gradle.kts`:
  ```kotlin
  plugins {
      id("info.solidsoft.pitest") version "1.15.0"  // check latest version
  }
  pitest {
      targetClasses.set(listOf("com.vaultstadio.*"))
      targetTests.set(listOf("com.vaultstadio.*"))
      outputFormats.set(listOf("HTML", "XML"))
  }
  ```
- Run: `./gradlew pitest` (or `test pitest`). Reports are in `build/reports/pitest/`.

**Status:** Not yet enabled; optional for teams that want to harden critical paths.

### Phase 6.2 – Testcontainers and integration tests

**Current state:** Testcontainers is already in use:

- **Infrastructure:** [ExposedStorageItemRepositoryTest](../../kotlin-backend/infrastructure/src/test/kotlin/com/vaultstadio/infrastructure/persistence/ExposedStorageItemRepositoryTest.kt) and [ExposedUserRepositoryTest](../../kotlin-backend/infrastructure/src/test/kotlin/com/vaultstadio/infrastructure/persistence/ExposedUserRepositoryTest.kt) use `PostgreSQLContainer` for real DB tests.
- **API module:** Has `testImplementation(libs.bundles.testcontainers)` (Postgres, MinIO, JUnit 5); CI ensures Docker is available before running tests.

**Possible next step:** Add one or two **full-stack integration tests** (e.g. Ktor `testApplication { application { module() } }` with a Testcontainers Postgres instance) to hit real route handlers and DB. This would increase api module coverage for routes that are currently tested with an empty `application { }`. See “Real Database Integration Tests” under Untestable Components for context.

**Test environment (optional):** For manual or extended integration testing, a [docker-compose.test.yml](#test-environment-setup) style setup is documented below.

### Phase 6.3 – E2E and Compose UI Test plans

**E2E (end-to-end):**

- **Web (WASM):** Consider Playwright or Selenium for critical user flows (login, upload, browse, share). Not yet implemented.
- **Mobile:** Consider Appium for iOS/Android if/when mobile flows become critical. Not yet implemented.

**Compose UI Testing:**

- For **desktop**, Compose provides `compose.desktop.uiTestJUnit4` (or the multiplatform equivalent). Plans (not yet implemented):
  - Add dependency in `compose-frontend/composeApp/build.gradle.kts` for the desktop test source set.
  - Write UI tests for critical flows (e.g. login screen → main screen, file list → open folder) using `runComposeUiTest` and `onNodeWithText` / `onNodeWithTag`.
- **Web (WASM):** Compose for Web may have different UI test support; document when evaluated.

**Status:** Documented here for future work; no E2E or Compose UI tests in the repo yet. When adding them, update this section and [TEST_COVERAGE_ACTION_PLAN.md](TEST_COVERAGE_ACTION_PLAN.md).

---

## Future Improvements

### Recommended Test Additions

1. **Testcontainers:** Already used in infrastructure and api (see Phase 6.2). Dependencies are in `libs.versions.toml` and module `build.gradle.kts` files.

2. **Compose UI Testing:** See Phase 6.3. Add `testImplementation(compose.desktop.uiTestJUnit4)` to the desktop test source set when implementing.

3. **E2E:** See Phase 6.3 (Playwright/Selenium for web, Appium for mobile).

4. **Performance Tests:** JMeter or Gatling for API load testing; large file upload benchmarks. Not yet in scope.

5. **Mutation Testing (PIT):** See Phase 6.1. Add the pitest plugin when the team wants to run mutation testing on core or api.

### Test Environment Setup

For complete integration testing, set up:

```yaml
# docker-compose.test.yml
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: vaultstadio_test
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "5433:5432"
  
  ollama:
    image: ollama/ollama
    ports:
      - "11435:11434"
```

---

## Best Practices

1. **Test Naming**: Use descriptive names
   ```kotlin
   @Test
   fun `login should return error when credentials are invalid`()
   ```

2. **Test Structure**: Follow Arrange-Act-Assert pattern
   ```kotlin
   @Test
   fun exampleTest() {
       // Arrange
       val input = "test"
       
       // Act
       val result = function(input)
       
       // Assert
       assertEquals(expected, result)
   }
   ```

3. **Mocking**: Use MockK for Kotlin
   ```kotlin
   val repository = mockk<UserRepository>()
   every { repository.findById(any()) } returns Either.Right(user)
   ```

4. **Coroutines**: Use `runTest` for suspend functions
   ```kotlin
   @Test
   fun `async operation should complete`() = runTest {
       val result = asyncOperation()
       assertNotNull(result)
   }
   ```

---

## Contributing

When adding new features:

1. Write tests first (TDD) or alongside the feature
2. Ensure all tests pass: `./gradlew test`
3. Maintain minimum 80% coverage for business logic
4. Document any untestable components in this file

For a phased plan to improve coverage across the whole project, see [TEST_COVERAGE_ACTION_PLAN.md](TEST_COVERAGE_ACTION_PLAN.md).
