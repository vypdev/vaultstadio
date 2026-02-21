# Backend Clean Architecture – Implementation Proposal

**Last updated**: 2026-02-21

This document proposes a phased evolution of the VaultStadio backend toward Clean Architecture. It is actionable and complements [CLEAN_ARCHITECTURE_REVIEW.md](CLEAN_ARCHITECTURE_REVIEW.md) and [ARCHITECTURE.md](ARCHITECTURE.md).

---

## Target Model (Clean Architecture)

| Layer | Responsibility | Depends on |
|-------|----------------|------------|
| **Domain** | Entities, repository (and other) ports, events. No I/O, no frameworks. | Nothing (Kotlin, Arrow, kotlinx only). |
| **Application** | Use-case interfaces and implementations; orchestration. | Domain only. |
| **Infrastructure** | Repository implementations, storage backends, security. | Domain only. |
| **API** | HTTP routes, DTOs, mapping, Koin wiring. | Application + Domain (for mapping types). |

**Dependency rule**: Domain ← Application ← API; Domain ← Infrastructure. API and Infrastructure never depend on each other; both are composed at runtime via DI.

---

## Current State (Summary)

- **core**: Domain models, repository interfaces, **domain services** (StorageService, UserService, ShareService, etc.), events. Core has optional Exposed/Redis/Ktor client in Gradle (to be cleaned).
- **api**: Ktor routes, DTOs, **use cases** (in `api/application/usecase/*`), Koin modules. Use cases are thin wrappers over core services.
- **infrastructure**: Implements core repository interfaces; storage backends; password hashing.

**Gap**: Use cases and “application” logic live inside the delivery layer (api). Domain and “application services” are mixed in core. There is no dedicated **application** module that depends only on domain.

---

## Proposal: Three Phases

### Phase 1 – Extract application module (low risk)

**Goal**: Move all use-case interfaces and implementations out of `api` into a new **application** module. Routes only call use cases; no business logic in api.

**Steps**:

1. **Create module `backend/application`**
   - `settings.gradle.kts`: `include(":application")`
   - `application/build.gradle.kts`: dependencies only on `core`, `plugins-api` (if use cases need plugin types), and Kotlin/Arrow/kotlinx. No Ktor, no Exposed, no infrastructure.

2. **Move packages**
   - Move `api/src/main/kotlin/com/vaultstadio/api/application/usecase/*` → `application/src/main/kotlin/com/vaultstadio/application/usecase/*`.
   - Keep the same public interfaces and impls; use cases continue to depend on **core** (UserService, StorageService, ShareService, repositories, etc.). No change to core in this phase.

3. **API module**
   - Remove all use-case source files from api.
   - Add `implementation(project(":application"))` to api.
   - Update imports in routes and in **Koin**: move use-case bindings from `api/config/Koin.kt` into an **application** Koin module (e.g. `application/di/ApplicationKoin.kt`) that declares all use-case interfaces → impls. Api’s Koin loads that module plus `core`, `infrastructure`, and api-specific modules (DTOs, routes, config). Use-case impls still receive core services and (via Koin) infrastructure repositories; those are still wired in api’s Koin or in a shared “composition” module.

4. **Tests**
   - Move use-case tests from `api/src/test/.../usecase/*` to `application/src/test/.../usecase/*`. Use-case tests keep using mocks of core services/repositories as today.

**Result**: Clear separation: **api** = delivery (routes + DTOs + mapping + DI); **application** = use cases only, depends on core. No change to core or infrastructure.

---

### Phase 2 – Domain vs application inside core (medium risk)

**Goal**: Separate “pure domain” from “application services” inside core, and make core domain framework-free.

**Steps**:

1. **Structure inside core**
   - Keep `core/domain/model`, `core/domain/repository`, `core/domain/event` as the **domain** surface (entities + ports + events).
   - Introduce `core/application/` (or `core/service/`) for current **domain services**: StorageService, UserService, ShareService, FileVersionService, SyncService, FederationService, CollaborationService, ActivityLogger, etc. These are “application services” that orchestrate repositories and domain types. They stay in core for now but are clearly named (e.g. `core.application.StorageService`).
   - Optional: define **ports** (interfaces) for these services in `core/domain/port/` so that the application module could depend only on interfaces; implementations stay in core. That would allow testing and swapping without touching use cases.

2. **Remove framework dependencies from core**
   - In `core/build.gradle.kts`, remove Exposed, Hikari, Ktor client, Redis (Lettuce) from **main** source set if not used. If any are used only in tests, keep them as `testImplementation`. If core needs an interface (e.g. HTTP client for AI), keep only the interface in core and move the implementation to infrastructure or api.
   - Ensure core has no dependency on `infrastructure` or `api`.

**Result**: Core has a clear **domain** (model, repository, event, optional port interfaces) and **application services** (orchestration). Application module (use cases) still depends on core; use cases call core application services and/or repository interfaces. No new Gradle module yet; just internal layout and dependency cleanup.

---

### Phase 3 – Pure domain module (optional, higher effort)

**Goal**: Extract a **domain** Gradle module with zero framework dependencies; core becomes “application services” that depend on domain; application (use cases) depends only on domain (and optionally core for services).

**Steps**:

1. **Create module `backend/domain`**
   - `domain/build.gradle.kts`: only Kotlin stdlib, Arrow, kotlinx (coroutines, serialization, datetime). No Exposed, no Ktor, no Redis.
   - Move from core into domain:
     - `core/domain/model/*` → `domain/src/.../model/`
     - `core/domain/repository/*` (interfaces) → `domain/src/.../repository/`
     - `core/domain/event/*` → `domain/src/.../event/`
     - Optional: `core/domain/port/*` (service ports) → `domain/src/.../port/`
   - Keep in **core**: all “application services” (StorageService, UserService, etc.), EventBus implementation, and any types that depend on domain. Core then depends on **domain**.

2. **Dependency graph**
   - **domain**: no project dependencies.
   - **core**: `implementation(project(":domain"))`. Core contains application services and EventBus; services use domain entities and repository interfaces.
   - **infrastructure**: `implementation(project(":domain"))`; optionally `api(project(":core"))` if it still needs core for something, or only domain if repositories are the only link.
   - **application**: `implementation(project(":core"))` (or, if ports are in domain, `implementation(project(":domain"))` and get service implementations via DI from core).
   - **api**: `implementation(project(":application"))`, `implementation(project(":domain"))` (if needed for mapping), `implementation(project(":infrastructure"))` for wiring.

3. **Koin**
   - Domain has no Koin. Core and infrastructure expose modules that bind repository interfaces to implementations. Application exposes a module that binds use-case interfaces to implementations (injected with core services and/or repositories). Api loads all modules and wires routes to use cases only.

**Result**: **domain** is a minimal, framework-free module. **application** (use cases) and **core** (application services) sit above it; **api** and **infrastructure** are the outermost layers. This matches Clean Architecture and aligns the backend with the frontend’s domain/application/data split.

---

## Optional Quick Wins (Any Phase)

- **Centralise domain → DTO mapping**: Move all `toResponse()` and request→domain mapping into a single `api/boundary` or `api/mapper` package (or a small `api-mapping` module). Routes only call use cases and pass DTOs to `respond`. See CLEAN_ARCHITECTURE_REVIEW.md.
- **WebDAV / S3**: Prefer use-case facades (e.g. `WebDAVUseCase`, `S3UseCase`) so routes depend on use-case interfaces instead of services directly. Implementation can still delegate to existing service wrappers inside the use case.
- **Remove Exposed from core**: Confirm no main source in core uses Exposed; use `testImplementation` only if needed. Keeps core (or domain) framework-agnostic.

---

## Module Layout After Full Proposal

See **[Final module structure after full migration](#final-module-structure-after-full-migration)** below for the detailed layout. Summary:

```
backend/
├── domain/          # Phase 3: entities, repository interfaces, events (no frameworks)
├── core/            # Application services, EventBus; depends on domain
├── application/    # Phase 1: use-case interfaces + implementations; depends on core (or domain)
├── infrastructure/ # Repository impls, storage, security; depends on domain (and optionally core)
├── plugins-api/    # Plugin SDK; depends on domain/core as today
├── api/            # Ktor, DTOs, mapping, Koin; depends on application + domain (+ infrastructure for wiring)
└── plugins/        # Built-in plugins; unchanged
```

**Dependency summary**:

- `domain` ← core, application (use cases), infrastructure
- `core` ← application, api (for DI composition), infrastructure (for wiring)
- `api` ← application, domain (types), infrastructure (wiring); no business logic

---

## Final module structure after full migration

This section describes the **final** backend layout after all three phases (including Phase 3: pure domain module).

### Gradle includes (`backend/settings.gradle.kts`)

```kotlin
rootProject.name = "backend"

// Inner layers (no delivery or infra)
include(":domain")
include(":core")
include(":application")

// Infrastructure
include(":infrastructure")
include(":plugins-api")

// Delivery
include(":api")

// Built-in plugins
include(":plugins:image-metadata")
include(":plugins:video-metadata")
include(":plugins:fulltext-search")
include(":plugins:ai-classification")
```

### Directory and package layout

```
backend/
├── domain/
│   └── src/main/kotlin/com/vaultstadio/domain/
│       ├── model/              # StorageItem, User, ShareLink, FileVersion, Sync, Federation, Collaboration, Activity, etc.
│       ├── repository/         # Interfaces only: StorageItemRepository, UserRepository, ShareRepository, ...
│       ├── event/              # StorageEvent, EventBus (interface), AdvancedEvents
│       └── exception/          # StorageException and domain exceptions
│
├── core/
│   └── src/main/kotlin/com/vaultstadio/core/
│       ├── application/       # Application services (orchestration; use domain + repositories via DI)
│       │   ├── StorageService, StorageServiceQueries, StorageServiceMutations, StorageServiceWrite
│       │   ├── UserService, ShareService, ActivityLogger
│       │   ├── FileVersionService, SyncService, FederationService, CollaborationService
│       │   ├── LockManager, TransactionManager, MultipartUploadManager
│       │   └── EventBus (implementation)
│       └── ai/                # AIService interface + AIServiceImpl (if kept in core)
│
├── application/
│   └── src/main/kotlin/com/vaultstadio/application/
│       └── usecase/
│           ├── auth/          # LoginUseCase, RegisterUseCase, LogoutUseCase, RefreshSessionUseCase
│           ├── storage/       # ListFolderUseCase, GetItemUseCase, UploadFileUseCase, ...
│           ├── share/         # CreateShareUseCase, GetSharesByUserUseCase, ...
│           ├── user/          # GetUserInfoUseCase, ChangePasswordUseCase, ...
│           ├── admin/         # ListUsersUseCase, UpdateQuotaUseCase, ...
│           ├── activity/      # GetRecentActivityByUserUseCase, GetRecentActivityByItemUseCase
│           ├── metadata/      # GetItemMetadataUseCase, GetMetadataByItemIdAndPluginUseCase
│           ├── version/       # GetVersionHistoryUseCase, RestoreVersionUseCase, ...
│           ├── sync/          # RegisterDeviceUseCase, SyncPullUseCase, ...
│           ├── plugin/        # ListPluginsUseCase, EnablePluginUseCase, ...
│           ├── chunkedupload/ # InitChunkedUploadUseCase, UploadChunkUseCase, ...
│           ├── health/        # GetReadinessUseCase, GetDetailedHealthUseCase
│           └── ai/            # AIServiceUseCase
│       └── di/                # Koin module(s): use-case interface → implementation
│
├── infrastructure/
│   └── src/main/kotlin/com/vaultstadio/infrastructure/
│       ├── persistence/       # ExposedStorageItemRepository, ExposedUserRepository, ...
│       ├── storage/           # LocalStorageBackend, S3StorageBackend
│       └── security/          # BCryptPasswordHasher
│
├── plugins-api/
│   └── src/main/kotlin/...    # Plugin interface, PluginContext, hooks; depends on domain (events, models)
│
├── api/
│   └── src/main/kotlin/com/vaultstadio/api/
│       ├── routes/            # AuthRoutes, StorageRoutes, ShareRoutes, ... (call use cases only)
│       ├── dto/               # Request/response DTOs
│       ├── boundary/          # or mapper/: domain ↔ DTO mapping (toResponse(), fromRequest(), etc.)
│       ├── config/            # Koin composition root, AppConfig, DatabaseInitializer
│       ├── security/          # JWT, auth pipeline
│       ├── plugins/           # PluginManager (wires plugins with core/infrastructure)
│       └── middleware/        # Error handling, logging
│   └── src/test/              # Route/integration tests only
│
└── plugins/
    ├── image-metadata/
    ├── video-metadata/
    ├── fulltext-search/
    └── ai-classification/
```

### Gradle dependency graph

| Module           | Depends on (Gradle)        | Does not depend on    |
|------------------|---------------------------|------------------------|
| **domain**       | (none; Kotlin, Arrow, kotlinx only) | core, application, api, infrastructure |
| **core**         | `domain`                  | api, infrastructure   |
| **application**  | `core` (and optionally `plugins-api` if needed) | api, infrastructure   |
| **infrastructure** | `domain`                | core, application, api |
| **plugins-api**  | `domain` (and optionally `core` for EventBus type) | api, application, infrastructure |
| **api**          | `application`, `domain`, `infrastructure`, `core`, `plugins-api` | — |

- **api** is the only module that pulls in **infrastructure** and **core** for **wiring** (Koin): it binds repository implementations (from infrastructure) and core services, then injects them into application use-case implementations. No business logic in api.
- **plugins** (image-metadata, etc.) depend on **plugins-api** and, at runtime, get core/domain types via the plugin context; they do not depend on api or application.

### What each module contains (responsibilities)

| Module           | Contains |
|------------------|----------|
| **domain**       | Entities, repository interfaces (ports), event types and EventBus interface, domain exceptions. Zero I/O, zero frameworks. |
| **core**         | Application services (business orchestration using repositories and domain types), EventBus implementation, optional AI service. Depends only on domain. |
| **application**  | Use-case interfaces and implementations; Koin module binding use cases. Use cases call core services and/or repository interfaces. No HTTP, no DTOs. |
| **infrastructure** | Exposed repository implementations, LocalStorageBackend, S3StorageBackend, BCryptPasswordHasher. Implements domain ports only. |
| **plugins-api**  | Plugin SDK: Plugin interface, PluginContext, hooks, metadata types. Used by plugins and by core/api for plugin lifecycle. |
| **api**          | Ktor application, routes, DTOs, domain↔DTO mapping, Koin composition root, auth middleware, PluginManager. Entry point; no business logic. |
| **plugins/**    | Built-in plugin implementations (image-metadata, video-metadata, fulltext-search, ai-classification). |

### Dependency direction (Clean Architecture)

```
                    ┌─────────────┐
                    │   domain    │
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         ▼                 ▼                 ▼
  ┌─────────────┐   ┌─────────────┐   ┌──────────────┐
  │    core     │   │application  │   │infrastructure │
  │ (services)  │   │ (use cases) │   │ (repos impl)  │
  └──────┬──────┘   └──────┬──────┘   └───────┬────────┘
         │                 │                  │
         └─────────────────┼──────────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │     api     │  (wires all via Koin; routes call use cases only)
                    └─────────────┘
```

---

## Submodules by area (optional)

The proposal above keeps **one module per layer** (domain, core, application, infrastructure, api). The frontend instead uses **many submodules per layer** (e.g. `:domain:auth`, `:domain:storage`, `:data:auth`, `:feature:auth`). The question is whether the backend should be split the same way.

### When more submodules can help

| Driver | Benefit |
|--------|--------|
| **Compile time** | Changing only storage code recompiles `domain:storage`, `application:storage`, `infrastructure:persistence` (and api), not the whole domain/application/infrastructure. |
| **Team ownership** | Clear boundaries (e.g. “auth team” owns `domain:auth`, `application:auth`, `infrastructure:auth`). |
| **Reuse** | A separate service or CLI might depend only on `:application:storage` + `:domain:storage` without pulling auth, share, sync, etc. |
| **Bounded contexts** | DDD-style: each area (auth, storage, share, versioning, sync, federation, collaboration, plugins) is a submodule; dependencies between areas are explicit. |

### When it can hurt

| Issue | Why |
|-------|-----|
| **Cross-area dependencies** | Storage use cases need User (auth); share needs StorageItem; version needs storage. You get a web of `domain:storage`, `domain:auth`, `domain:share` and must manage shared types (e.g. a minimal `domain:common` or `domain:model`). |
| **Gradle and Koin** | Many modules ⇒ more `settings.gradle.kts` includes, more `build.gradle.kts` dependencies, and more Koin modules to load and order correctly in api. |
| **Single deployable** | Backend is one process; you don’t ship “only storage.” Fine-grained modules pay off more when you have multiple deployables or libraries. |
| **Team size** | With a small team, a few modules per layer are easier to reason about than dozens. |

### Variant: full layout with submodules by area

Below is a **concrete** layout with one submodule per area per layer. Use it as a reference if you adopt the “many submodules” variant.

#### 1. Gradle includes (`backend/settings.gradle.kts`)

```kotlin
rootProject.name = "backend"

// ─── Domain (innermost; no project deps) ─────────────────────────────────────
include(":domain:common")
include(":domain:auth")
include(":domain:storage")
include(":domain:share")
include(":domain:activity")
include(":domain:admin")
include(":domain:version")
include(":domain:sync")
include(":domain:federation")
include(":domain:collaboration")
include(":domain:metadata")
include(":domain:plugin")

// ─── Core (application services; depend on domain) ──────────────────────────
include(":core:common")      // EventBus, LockManager, TransactionManager, MultipartUploadManager
include(":core:auth")
include(":core:storage")
include(":core:share")
include(":core:activity")
include(":core:version")
include(":core:sync")
include(":core:federation")
include(":core:collaboration")
include(":core:ai")

// ─── Application (use cases; depend on core + domain) ─────────────────────────
include(":application:auth")
include(":application:storage")
include(":application:share")
include(":application:user")
include(":application:admin")
include(":application:activity")
include(":application:metadata")
include(":application:version")
include(":application:sync")
include(":application:plugin")
include(":application:chunkedupload")
include(":application:health")
include(":application:ai")

// ─── Infrastructure (implement domain ports) ────────────────────────────────
include(":infrastructure:persistence")
include(":infrastructure:storage")
include(":infrastructure:security")

// ─── Plugin SDK & delivery ──────────────────────────────────────────────────
include(":plugins-api")
include(":api")

// ─── Built-in plugins ───────────────────────────────────────────────────────
include(":plugins:image-metadata")
include(":plugins:video-metadata")
include(":plugins:fulltext-search")
include(":plugins:ai-classification")
```

#### 2. Directory and package layout

```
backend/
├── domain/
│   ├── common/       # com.vaultstadio.domain.common  → StorageException, EventBus (interface), event types, optional Result
│   ├── auth/         # com.vaultstadio.domain.auth   → User, Session; UserRepository, SessionRepository, ApiKeyRepository
│   ├── storage/      # com.vaultstadio.domain.storage → StorageItem, Breadcrumb; StorageItemRepository
│   ├── share/        # com.vaultstadio.domain.share   → ShareLink; ShareRepository  (depends on domain:storage for item refs)
│   ├── activity/     # com.vaultstadio.domain.activity → Activity; ActivityRepository
│   ├── admin/        # com.vaultstadio.domain.admin   → AdminUser, UserRole; no repo (uses auth)
│   ├── version/      # com.vaultstadio.domain.version → FileVersion; FileVersionRepository
│   ├── sync/         # com.vaultstadio.domain.sync    → SyncDevice, SyncChange, SyncConflict; SyncRepository
│   ├── federation/   # com.vaultstadio.domain.federation → FederatedInstance, FederatedShare; FederationRepository
│   ├── collaboration/# com.vaultstadio.domain.collaboration → CollaborationSession, DocumentComment; CollaborationRepository
│   ├── metadata/     # com.vaultstadio.domain.metadata → (interfaces only) MetadataRepository
│   └── plugin/       # com.vaultstadio.domain.plugin  → PluginInfo, plugin-related types (or keep only in plugins-api)
│
├── core/
│   ├── common/       # EventBus impl, LockManager, TransactionManager, MultipartUploadManager (dep: domain:common + repos)
│   ├── auth/         # UserService  (dep: domain:auth, domain:common, repos)
│   ├── storage/      # StorageService  (dep: domain:storage, domain:common, StorageBackend, repos)
│   ├── share/        # ShareService  (dep: domain:share, domain:storage, repos)
│   ├── activity/     # ActivityLogger  (dep: domain:activity, domain:common/event)
│   ├── version/      # FileVersionService  (dep: domain:version, domain:storage, repos)
│   ├── sync/         # SyncService  (dep: domain:sync, repos)
│   ├── federation/   # FederationService  (dep: domain:federation, FederationCryptoService)
│   ├── collaboration/# CollaborationService  (dep: domain:collaboration, repos)
│   └── ai/           # AIService, AIServiceImpl  (dep: domain if any AI types; else none)
│
├── application/
│   ├── auth/         # LoginUseCase, RegisterUseCase, LogoutUseCase, RefreshSessionUseCase  (dep: core:auth, domain:auth)
│   ├── storage/      # ListFolderUseCase, GetItemUseCase, UploadFileUseCase, ...  (dep: core:storage, domain:storage)
│   ├── share/        # CreateShareUseCase, GetSharesByUserUseCase, ...  (dep: core:share, domain:share)
│   ├── user/         # GetUserInfoUseCase, ChangePasswordUseCase, UpdateUserUseCase, GetQuotaUseCase, LogoutAllUseCase  (dep: core:auth, domain:auth)
│   ├── admin/        # ListUsersUseCase, GetAdminStatisticsUseCase, UpdateQuotaUseCase, DeleteUserUseCase  (dep: core:auth, domain:admin)
│   ├── activity/     # GetRecentActivityByUserUseCase, GetRecentActivityByItemUseCase  (dep: core:activity, domain:activity)
│   ├── metadata/     # GetItemMetadataUseCase, GetMetadataByItemIdAndPluginUseCase  (dep: domain:metadata, domain:storage; core if needed)
│   ├── version/      # GetVersionHistoryUseCase, RestoreVersionUseCase, CompareVersionsUseCase, ...  (dep: core:version, domain:version)
│   ├── sync/         # RegisterDeviceUseCase, SyncPullUseCase, ResolveConflictUseCase, ...  (dep: core:sync, domain:sync)
│   ├── plugin/       # ListPluginsUseCase, EnablePluginUseCase, HandlePluginEndpointUseCase, ...  (dep: plugins-api, domain:plugin)
│   ├── chunkedupload/# InitChunkedUploadUseCase, UploadChunkUseCase, CompleteChunkedUploadUseCase, ...  (dep: core:storage, core:common)
│   ├── health/       # GetReadinessUseCase, GetDetailedHealthUseCase  (dep: infra or config from api)
│   └── ai/            # AIServiceUseCase  (dep: core:ai)
│
├── infrastructure/
│   ├── persistence/  # Exposed*Repository for all domains  (dep: domain:auth, domain:storage, domain:share, ...)
│   ├── storage/      # LocalStorageBackend, S3StorageBackend  (dep: domain if StorageBackend interface in domain)
│   └── security/     # BCryptPasswordHasher  (dep: domain:auth or core:auth for PasswordHasher interface)
│
├── plugins-api/      # Plugin, PluginContext, hooks  (dep: domain:common, domain:storage, domain:metadata, ...)
├── api/              # Ktor, routes, DTOs, boundary mappers, Koin composition root  (dep: all application:*, infrastructure:*, core:*, domain:*)
└── plugins/
    ├── image-metadata/
    ├── video-metadata/
    ├── fulltext-search/
    └── ai-classification/
```

#### 3. Gradle dependency matrix (who depends on whom)

| Module | Depends on (Gradle) |
|--------|---------------------|
| **domain:common** | (none) |
| **domain:auth** | domain:common |
| **domain:storage** | domain:common |
| **domain:share** | domain:common, domain:storage |
| **domain:activity** | domain:common |
| **domain:admin** | domain:common, domain:auth |
| **domain:version** | domain:common, domain:storage |
| **domain:sync** | domain:common, domain:storage |
| **domain:federation** | domain:common |
| **domain:collaboration** | domain:common |
| **domain:metadata** | domain:common, domain:storage |
| **domain:plugin** | domain:common |
| **core:common** | domain:common, (all domain:* that have repos used by EventBus/Lock/Transaction/Multipart) — or aggregate “domain” jar |
| **core:auth** | domain:auth, domain:common |
| **core:storage** | domain:storage, domain:common |
| **core:share** | domain:share, domain:storage, domain:common |
| **core:activity** | domain:activity, domain:common |
| **core:version** | domain:version, domain:storage, domain:common |
| **core:sync** | domain:sync, domain:common |
| **core:federation** | domain:federation, domain:common |
| **core:collaboration** | domain:collaboration, domain:common |
| **core:ai** | domain:common (if needed) |
| **application:auth** | core:auth, domain:auth |
| **application:storage** | core:storage, domain:storage |
| **application:share** | core:share, domain:share |
| **application:user** | core:auth, domain:auth |
| **application:admin** | core:auth, domain:admin |
| **application:activity** | core:activity, domain:activity |
| **application:metadata** | domain:metadata, domain:storage; optionally core:storage |
| **application:version** | core:version, domain:version |
| **application:sync** | core:sync, domain:sync |
| **application:plugin** | plugins-api, domain:plugin |
| **application:chunkedupload** | core:storage, core:common |
| **application:health** | (config/database from api or a small application:health dep on infra) |
| **application:ai** | core:ai |
| **infrastructure:persistence** | all domain:* that define repository interfaces |
| **infrastructure:storage** | domain:storage (or core if StorageBackend is there) |
| **infrastructure:security** | domain:auth or core:auth (PasswordHasher port) |
| **plugins-api** | domain:common, domain:storage, domain:metadata, domain:plugin |
| **api** | all application:*, all infrastructure:*, all core:*, all domain:* (or only what routes need), plugins-api |

To avoid a long list of domain dependencies in core and infrastructure, you can introduce an **aggregate** module:

- **domain** (single module) that depends on all `domain:*` and exposes a single API, or
- **domain-api** (optional) that only re-exports the public interfaces/models of domain submodules so that core and infrastructure depend on `domain-api` instead of each `domain:auth`, `domain:storage`, etc.

#### 4. Koin strategy

- **api** is the composition root. It loads:
  - Config (api)
  - All **infrastructure** modules (persistence, storage, security) → bind repository interfaces to implementations.
  - All **core** modules → bind services (UserService, StorageService, …).
  - All **application** modules → bind use-case interfaces to implementations (constructors receive core services and repos via `get()`).
  - Plugin manager and api-specific beans (PluginManager, ThumbnailCache, UploadSessionManager, etc.).
- Each **application:xxx** module can expose a single Koin module (e.g. `applicationAuthModule`) that declares only its use cases. Same for **core:xxx** and **infrastructure:xxx** if you split their DI per area.

#### 5. Dependency diagram (conceptual)

```
                    domain:common
                         │
    domain:auth   domain:storage   domain:share   ... (other domain:*)
         │                │              │
         └────────────────┼──────────────┘
                          │
         core:auth   core:storage   core:share   ...
                          │
         application:auth   application:storage   ...
                          │
         infrastructure:persistence   infrastructure:storage   infrastructure:security
                          │
                          ▼
                        api  (wires everything; routes call use cases only)
```

#### 6. Simplifications to reduce modules

If the full matrix is too much, you can:

- **Keep domain as one module** (no `domain:auth`, `domain:storage`, …) and only split **core**, **application**, and optionally **infrastructure** by area. Then `application:auth` depends on `core`, `application`, and the single `domain`.
- **Merge small areas**: e.g. `application:user` and `application:admin` into `application:auth` (user + admin use cases); `application:health` into `api` or a single `application:support`.
- **Single infrastructure** module that implements all repository interfaces (as today), instead of `infrastructure:persistence` / `storage` / `security`, to avoid many domain dependencies in infra.

### Recommendation

- **Don’t** submodule the backend by area **until** the current proposal (Phase 1–3) is in place and you feel a concrete pain (e.g. long full builds, need for a separate CLI/library, or multiple teams owning different areas).
- **Do** keep **one module per layer** (domain, core, application, infrastructure, api) as the default. Use **packages** inside each module to separate areas (`com.vaultstadio.application.usecase.auth`, `com.vaultstadio.domain.model`, etc.); that already gives clear structure and keeps Gradle/Koin simple.
- **Consider** submodules by area only if:
  - You introduce another **entry point** (e.g. CLI or worker) that should depend only on a subset of use cases, or
  - **Compile times** or **ownership** become real problems and you’ve already optimized build (incremental, build cache, etc.).

In short: **sí puede interesarte desgranar en más submódulos** si tienes equipos por contexto, varios artefactos o tiempos de compilación altos; pero no es un requisito para Clean Architecture y añade complejidad, así que conviene hacerlo solo cuando haya una necesidad clara.

---

## Risks and Mitigations

| Risk | Mitigation |
|------|-------------|
| Large refactor in Phase 3 | Phase 1 and 2 can be done independently; Phase 3 can be skipped or done later. |
| Plugin system coupling | plugins-api and plugins stay as today; they depend on core (and later domain) for events and storage contracts. |
| Koin wiring complexity | Keep a single composition root in api that loads domain (if needed), core, application, infrastructure, and api modules. Use-case impls receive core services and repos via constructor injection. |
| Test duplication | Use-case tests live in application; route/integration tests stay in api. Mock domain/core in application tests. |

---

## Recommendation

1. **Implement Phase 1** first: new **application** module and move use cases + their tests. This gives an immediate Clean Architecture win (delivery layer vs application layer) with minimal risk.
2. **Then Phase 2**: Clean core layout (domain vs application packages) and remove unused framework deps from core.
3. **Phase 3** only if you want a strict “domain with zero dependencies” and are willing to move a lot of files and adjust infrastructure/core boundaries.

After Phase 1, the backend will have a clear **application** layer (use cases) separate from the **api** (routes, DTOs, DI), which aligns with the frontend’s use-case–driven design and improves testability and maintainability.

---

## References

- [ARCHITECTURE.md](ARCHITECTURE.md) – Current backend layout and diagrams.
- [CLEAN_ARCHITECTURE_REVIEW.md](CLEAN_ARCHITECTURE_REVIEW.md) – Principles and current gaps.
- [FRONTEND_MODULARISATION_AND_STANDALONE_BUILDS.md](FRONTEND_MODULARISATION_AND_STANDALONE_BUILDS.md) – Frontend domain/data/feature pattern for comparison.
