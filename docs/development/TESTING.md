# VaultStadio Testing Guide

This document describes the testing strategy, current coverage, and known limitations of the VaultStadio test suite.

## Table of Contents

1. [Test Overview](#test-overview)
2. [Running Tests](#running-tests)
3. [Test Categories](#test-categories)
4. [Coverage Summary](#coverage-summary)
5. [Untestable Components](#untestable-components)
6. [Future Improvements](#future-improvements)

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

# Shared module tests
./gradlew :shared:test
```

### Run with Coverage Report

```bash
./gradlew test jacocoTestReport
```

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

### 8. Frontend Tests (5 files)

Location: `compose-frontend/composeApp/src/commonTest/kotlin/`

| Test File | Coverage |
|-----------|----------|
| `AppViewModelTest.kt` | Basic ViewModel tests |
| `AppViewModelDetailedTest.kt` | Detailed state management |
| `ComponentsTest.kt` | UI component logic |
| `ScreensTest.kt` | Screen state logic |
| `PlatformTest.kt` | Platform abstraction logic |

### 9. Shared Module Tests (6 files)

Location: `shared/src/commonTest/kotlin/`

| Test File | Coverage |
|-----------|----------|
| `ModelsTest.kt` | Domain models |
| `ApiModelsTest.kt` | Request/response models |
| `FormattingTest.kt` | Utility functions |
| `ApiClientTest.kt` | API client configuration |
| `VaultStadioApiTest.kt` | API methods and results |
| `AuthRepositoryTest.kt` | Auth state and token management |
| `SharedModuleTest.kt` | DI configuration |

---

### 10. Phase 6 Model Tests (4 files)

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
| Frontend ViewModel | 2 | Medium |
| Frontend Components | 3 | Medium |
| Shared Models | 4 | High |
| Shared Network | 2 | High |

**Total Test Files: 65+**

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

### 7. File Upload/Download Streams

**Reason:** Large file handling with streams requires:
- Memory management
- Disk I/O
- Network bandwidth

**Workaround:**
- Test with small mock data
- Separate performance test suite

---

## Future Improvements

### Recommended Test Additions

1. **Testcontainers Integration**
   ```kotlin
   // Add to build.gradle.kts
   testImplementation("org.testcontainers:postgresql:1.19.0")
   testImplementation("org.testcontainers:junit-jupiter:1.19.0")
   ```

2. **Compose UI Testing**
   ```kotlin
   // Add to compose-frontend/composeApp/build.gradle.kts
   testImplementation(compose.desktop.uiTestJUnit4)
   ```

3. **E2E Test Framework**
   - Consider Playwright or Selenium for web frontend
   - Consider Appium for mobile (iOS/Android)

4. **Performance Tests**
   - JMeter or Gatling for API load testing
   - Large file upload benchmarks

5. **Mutation Testing**
   ```kotlin
   // Add PIT mutation testing
   plugins {
       id("info.solidsoft.pitest") version "1.9.0"
   }
   ```

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
