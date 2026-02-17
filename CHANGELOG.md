# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.2.0] - 2026-01-30

### Added

#### Frontend Internationalization (i18n)
- **Complete i18n implementation**: All 7 languages now fully supported
  - English, Spanish, French, German, Portuguese, Chinese, Japanese
  - All screens use localized strings from `StringResources`
  - Language selector in Settings now shows all available languages

- **New UI Components**
  - `SortDialog.kt`: Sort files by name, size, date, type with ASC/DESC order
  - `FilterDialog.kt`: Advanced filter by file type, date range, size range
  - `StorageChart.kt`: Visual donut chart for storage usage by category
  - `StorageWarningBanner.kt`: Warning banner when storage is 85%+ full
  - `SharedWithMeScreen.kt`: View files shared with current user

#### Hardcoded String Extraction
- Extracted ~100+ hardcoded strings from screens:
  - `SettingsScreen.kt`
  - `AIScreen.kt`
  - `FederationScreen.kt`
  - `CollaborationScreen.kt`
  - `SyncScreen.kt`
  - `VersionHistoryScreen.kt`
  - `AdminScreen.kt`
  - `PluginsScreen.kt`

#### New i18n Keys Added
- Settings: `settingsAutoSync`, `settingsClearCache`, `settingsNotifications`, 
  `settingsPushNotifications`, `settingsSecurity`, `settingsChangePassword`, 
  `settingsLicenses`, `settingsLogoutConfirm`
- AI: `aiStartConversation`, `aiDescribeImage`, `aiAutoTagContent`, 
  `aiSummarizeText`, `aiNoProviderConfigured`, `aiGeneratedTags`, `aiSelectModel`
- Federation: `federationTitle`, `federationEnterDomain`, `federationBlockConfirm`,
  `federationIncomingShares`, `federationOutgoingShares`
- Collaboration: `collaborationJoiningSession`, `collaborationCouldNotJoin`,
  `collaborationAddComment`, `collaborationNoCommentsYet`
- Sync: `syncTitle`, `syncDeactivateConfirm`, `syncRemoveConfirm`,
  `syncResolveConflict`, `syncChooseResolution`
- Version: `versionTitle`, `versionRestoreConfirm`, `versionCleanupOld`,
  `versionKeepLast`, `versionDeleteOlderThan`
- File Info: `infoType`, `infoFolder`, `infoFile`, `infoSize`, `infoMimeType`,
  `infoCreated`, `infoModified`, `infoPath`, `infoVisibility`
- Common: `commonBack`, `commonRequest`, `commonAccept`, `commonDecline`,
  `commonRevoke`, `commonBlock`, `commonRemove`, `commonDeactivate`,
  `commonResolve`, `commonSend`, `commonRetry`, `commonUnlink`, `commonConfigure`

### Tests Added
- `SortDialogTest.kt`: Tests for sort field and order enums
- `FilterDialogTest.kt`: Tests for filter types, date ranges, size ranges
- `StorageChartTest.kt`: Tests for storage category calculations
- `StringsTest.kt`: Comprehensive i18n tests for all languages

### Fixed
- **Language selector in SettingsScreen**: Now shows all 7 languages instead of just 2
- **Consistency**: All UI text now uses localized strings

---

## [2.1.0] - 2026-01-29

### Added

#### Phase 1-4 Completions
- **Activity Auto-logging**: `ActivityLogger` service that automatically logs all events to the Activity repository
  - Subscribes to FileEvent, FolderEvent, ShareEvent, UserEvent, SystemEvent
  - Creates audit trail without manual intervention

- **Plugin Endpoint Routing**: Dynamic routing for plugin-registered HTTP endpoints
  - Plugins can register custom API endpoints via `PluginContext.registerEndpoint()`
  - Endpoints accessible at `/api/v1/plugins/{pluginId}/api/{path}`

- **Search API Integration**: Full integration with `FullTextSearchPlugin`
  - `/search/advanced` with `searchContent: true` uses Lucene-based full-text search
  - Returns snippets and relevance scores

- **Metadata Search**: Implemented `/search/by-metadata` endpoint
  - Search files by plugin-attached metadata
  - Supports key-value pattern matching

#### Phase 5 Completions
- **AI Plugin Refactor**: `AIClassificationPlugin` now uses centralized `AIService`
  - Added `AIApi` interface to `PluginContext`
  - Plugins use managed AI providers instead of direct HTTP calls
  - Simplified configuration (no per-plugin API keys)

#### Phase 6 Completions
- **LockManager Abstraction**: Interface for distributed lock management
  - `InMemoryLockManager` for development/single-instance
  - `RedisLockManager` placeholder for production (requires Redis client)
  - Supports lock timeout, refresh, and cleanup

- **MultipartUploadManager Abstraction**: Interface for multipart upload state
  - `InMemoryMultipartUploadManager` for development
  - `RedisMultipartUploadManager` placeholder for production
  - Streaming completion support for large files

- **Federation Cryptography**: Ed25519/RSA digital signatures
  - `FederationCryptoService` for signing and verifying federation messages
  - Timestamp and nonce validation to prevent replay attacks
  - Supports Ed25519 (preferred) and RSA-SHA256

- **Delta Sync**: Rsync-style rolling checksum algorithm
  - `DeltaSyncService` for efficient file synchronization
  - Adler-32 style weak checksum + MD5 strong checksum
  - Block-level delta calculation and application

- **WebSocket Collaboration**: Real-time OT message processing
  - `CollaborationWebSocketManager` for connection tracking
  - Full message protocol: join, cursor_update, selection_update, operation, presence
  - Broadcast support for multi-user editing

### Tests
- Added comprehensive test suites for new components:
  - `DeltaSyncServiceTest` - Rolling checksum and delta calculation
  - `FederationCryptoServiceTest` - Signing and verification
  - `LockManagerTest` - Lock acquisition, release, and expiration
  - `MultipartUploadManagerTest` - Multipart upload lifecycle

### Documentation
- Updated CHANGELOG with all Phase 1-6 completions

---

## [2.0.0] - 2026-01-28

### Changed
- **Complete rewrite in Kotlin**
  - Backend: Migrated from Python/FastAPI to Kotlin/Ktor
  - Frontend: Migrated from SvelteKit to Compose Multiplatform
  - Database: Migrated from SQLite to PostgreSQL with Exposed ORM
- **Multi-platform support**: Web, Android, iOS, Desktop from single codebase
- **Plugin architecture**: Extensible system for metadata extraction, AI, search

### Added
- Kotlin Multiplatform shared module for cross-platform code
- Plugin system with event bus
- Full-text search plugin
- AI image classification plugin
- Image/Video metadata extraction plugins
- Compose Multiplatform UI with Material 3
- Comprehensive test suite with JUnit 5, MockK, Testcontainers
- OpenAPI/Swagger documentation
- GitHub Actions CI/CD pipeline
- Flyway database migrations
- Health check endpoints with real DB/storage checks

### Removed
- Python/FastAPI backend
- SvelteKit frontend
- SQLite database support
- Nintendo Switch specific features (Tinfoil, NSP/NSZ support)

## [1.0.0] - 2025-01-27

### Added
- Initial release
- Backend API with FastAPI (Python)
- Frontend UI with SvelteKit
- Helm chart for TrueNAS SCALE deployment
- Docker support for local development
