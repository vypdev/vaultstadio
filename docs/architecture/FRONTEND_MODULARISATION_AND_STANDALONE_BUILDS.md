# Frontend modularisation and standalone projects (backend / frontend)

**Last updated**: 2026-02-19

### Current implementation status

- **:domain:result** – Done. `Result`, `ApiException`, `NetworkException`.
- **:domain:auth** – Done. Auth use-case interfaces, `AuthRepository`, models (`User`, `Security`).
- **:domain:storage** – Done. Storage use-case interfaces, `StorageRepository`, `StorageModels`.
- **:domain:config** – Done. `ConfigRepository`, use-case interfaces (`GetStorageUrlsUseCase`, `GetShareUrlUseCase`, `GetCollaborationUrlUseCase`, `GetVersionUrlsUseCase`).
- **:domain:share** – Done. `ShareRepository`, `ShareLink` model, use-case interfaces (`GetMySharesUseCase`, `GetSharedWithMeUseCase`, `CreateShareUseCase`, `DeleteShareUseCase`).
- **:data:network** – Done. `ApiResult`, `BaseApi`, `HttpClientFactory`, `ApiClientConfig`, `TokenStorage`, `TokenProvider`, `ApiResultMapper`, DTOs.
- **:data:auth** – Done. AuthApi, AuthService, AuthRepositoryImpl, auth DTOs/mapper, use-case impls, `authModule` (Koin DSL).
- **:data:storage** – Done. StorageApi, StorageService, StorageRepositoryImpl, DTOs, mapper, use-case impls, `storageModule` (Koin DSL). ComposeApp uses `storageModule`; storage beans removed from `appModule`.
- **:data:config** – Done. `ConfigRepositoryImpl`, config use-case impls, `configModule`. ComposeApp uses `configModule`; config beans removed from `appModule`.
- **:domain:activity**, **:domain:admin**, **:domain:version**, **:domain:plugin** – Done. Models, repository interfaces, use-case interfaces.
- **:data:share** – Done. ShareApi, ShareService, ShareRepositoryImpl, DTOs, mapper, use-case impls, `shareModule`. ComposeApp uses `shareModule`; share beans removed from `appModule`.
- **:data:activity** – Done. ActivityApi, ActivityService, ActivityRepositoryImpl, DTO, mapper, use-case impls, `activityModule`. ComposeApp uses `activityModule`; activity beans removed from `appModule`.
- **:data:admin** – Done. AdminApi, AdminService, AdminRepositoryImpl, DTOs, mapper, use-case impls, `adminModule`. ComposeApp uses `adminModule`; admin beans removed from `appModule`.
- **:data:plugin** – Done. PluginApi, PluginService, PluginRepositoryImpl, DTO, mapper, use-case impls, `pluginModule`. ComposeApp uses `pluginModule`; plugin beans removed from `appModule`.
- **:data:version** – Done. VersionApi, VersionService, VersionRepositoryImpl (uses ApiClientConfig, TokenStorage for download URL), DTOs, mapper, 6 use-case impls, `versionModule`. ComposeApp uses `versionModule`; version beans removed from `appModule`.
- **:domain:sync** – Done. Models (DeviceType, SyncDevice, SyncChange, SyncConflict, SyncResponse, ConflictResolution, etc.), SyncRepository, 7 use-case interfaces. One file per class/enum.
- **:data:sync** – Done. SyncApi, SyncService, SyncRepositoryImpl, DTOs (one file per DTO), SyncMapper, 7 use-case impls, `syncModule`. ComposeApp uses `syncModule`; sync beans removed from `appModule`.
- **:domain:metadata** – Done. Models (FileMetadata, ImageMetadata, VideoMetadata, DocumentMetadata, MetadataSearchResult, ThumbnailSize), MetadataRepository, 7 use-case interfaces. Depends on :domain:storage for PaginatedResponse/StorageItem.
- **:data:metadata** – Done. MetadataApi, MetadataService, MetadataRepositoryImpl, DTOs (one file per DTO), MetadataMapper, 7 use-case impls, `metadataModule`. Depends on :data:storage for advancedSearch mapping. ComposeApp uses `metadataModule`; metadata beans removed from `appModule`.
- **:domain:federation** – Done. Models (InstanceStatus, FederationCapability, SharePermission, FederatedShareStatus, FederatedActivityType, FederatedInstance, FederatedShare, FederatedIdentity, FederatedActivity), FederationRepository, 15 use-case interfaces. One file per class/enum.
- **:data:federation** – Done. FederationApi, FederationService, FederationRepositoryImpl, DTOs (one file per DTO), FederationMapper, 15 use-case impls, `federationModule`. ComposeApp uses `federationModule`; federation beans removed from `appModule`. Old composeApp domain/model/Federation.kt and domain/usecase/federation/* removed.
- **:feature:*** – Placeholder only; ViewModels/screens still in composeApp.

Next: migrate remaining data slices (ai, collaboration) and feature modules per §10.

---

This document describes the target layout for:

1. **Backend** and **frontend** as **fully independent Kotlin/Gradle projects** (each with its own `settings.gradle.kts`, `build.gradle.kts`, `gradle/`, detekt, `libs.versions.toml`). No Gradle at repository root.
2. **Renaming**: `kotlin-backend` → **backend**, `compose-frontend` → **frontend**.
3. **Frontend** split into **domain**, **data**, and **feature**, each with **submodules by area/scope** (e.g. `:domain:auth`, `:data:storage`, `:feature:admin`).

It complements [ARCHITECTURE.md](ARCHITECTURE.md) and [CLEAN_ARCHITECTURE_REVIEW.md](CLEAN_ARCHITECTURE_REVIEW.md).

---

## 1. Repository layout: no Gradle at root

The repository root **does not** contain any Gradle configuration. All Gradle files, version catalogs, and tooling (detekt, ktlint) live inside **backend** and **frontend** respectively.

**Detekt and baseline are per-project:** each project has its own `config/detekt/detekt.yml` and `config/detekt/baseline.xml`. There is no shared root config; backend and frontend maintain separate baselines and can evolve their rules independently.

```
vaultstadio/
├── backend/                    # Standalone Kotlin project (ex kotlin-backend)
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   ├── gradle/
│   │   └── libs.versions.toml
│   ├── config/                 # detekt, etc.
│   ├── core/
│   ├── api/
│   ├── plugins-api/
│   ├── infrastructure/
│   └── plugins/
│       ├── image-metadata/
│       ├── video-metadata/
│       ├── fulltext-search/
│       └── ai-classification/
├── frontend/                   # Standalone Kotlin Multiplatform project (ex compose-frontend)
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   ├── gradle/
│   │   └── libs.versions.toml
│   ├── config/
│   ├── domain/                 # Submodules by area (see §2)
│   ├── data/                   # Submodules by area (see §3)
│   ├── feature/                # Submodules per screen (see §4)
│   ├── composeApp/
│   ├── androidApp/
│   └── iosApp/
├── docker/
├── helm/
├── docs/
└── scripts/
```

- **Build backend**: `cd backend && ./gradlew build`
- **Build frontend**: `cd frontend && ./gradlew build`
- **IDE**: Open `backend/` or `frontend/` as the project root to work on one side only.

### 1.1 Module source layout (domain, data, feature)

All frontend modules use the **same source layout** (no `commonMain` directory):

- **Path**: `frontend/<layer>/<module>/src/kotlin/com/vaultstadio/app/<layer>/<module>/<rest>/`
- **Module identifier** = package root: `com.vaultstadio.app.<layer>.<module>` (e.g. `com.vaultstadio.app.domain.auth`, `com.vaultstadio.app.data.auth`).

In each module’s `build.gradle.kts`, point the `commonMain` Kotlin source set to `src/kotlin`:

```kotlin
sourceSets {
    val commonMain by getting {
        kotlin.srcDirs("src/kotlin")
        // ... dependencies
    }
}
```

Examples:

- **:domain:auth**: `src/kotlin/com/vaultstadio/app/domain/auth/` (e.g. `AuthRepository.kt`, `model/User.kt`, `usecase/LoginUseCase.kt`).
- **:data:auth**: `src/kotlin/com/vaultstadio/app/data/auth/` (e.g. `api/AuthApi.kt`, `service/`, `repository/`, `dto/`, `mapper/`, `di/`, `usecase/`).
- **:domain:result**: `src/kotlin/com/vaultstadio/app/domain/result/Result.kt`.
- **:data:network**: `src/kotlin/com/vaultstadio/app/data/network/` (core types), `data/network/dto/common/` (ApiResponseDTO, etc.), `data/network/mapper/` (ApiResultMapper / toResult).
- **:domain:storage** and all other domain stubs (admin, sync, share, activity, metadata, plugin, version, collaboration, federation, ai, config, upload) use `src/kotlin` with a single `Placeholder.kt` until real code is moved in.
- **:data:storage** and all other data stubs (admin, config, sync, share, activity, metadata, plugin, version, collaboration, federation, ai) use `src/kotlin` with a single `Placeholder.kt` until real code is moved in.
- **:feature:auth**, **:feature:main**, and all other feature modules (admin, sync, shares, sharedwithme, activity, profile, settings, security, changepassword, plugins, files, upload, versionhistory, collaboration, federation, ai, licenses) use `src/kotlin` with a single `Placeholder.kt` until screen/UI code is moved in.
- **:composeApp**: `src/kotlin/com/vaultstadio/app/` (package root `com.vaultstadio.app`). All shared app code (di, config, platform, navigation, viewmodel, ui, data, feature, utils) lives under this path; `commonMain` only holds non-Kotlin resources if any.

Apply this pattern to every new or migrated module.

---

## 2. Domain: submodules by area/scope

**domain** is a container; it has **no** single `domain/build.gradle.kts` with code. Instead, each area is a submodule under `domain/<area>`.

- Only **commonMain** (and **commonTest**). No Compose, no Android/Wasm/iOS app plugins.
- Contents: models for that area, use-case **interfaces**, and (for a shared module) the **Result** type.

### 2.1 Suggested domain submodules

| Submodule        | Scope       | Typical contents |
|------------------|------------|-------------------|
| **:domain:result** | Shared     | `Result`, `ApiException`, `NetworkException`; can also hold shared interfaces used across domain. |
| **:domain:auth**   | Auth      | Auth use-case interfaces, auth-related models (e.g. User, LoginResult, SecuritySettings). |
| **:domain:storage** | Storage  | Storage use-case interfaces, StorageItem, Breadcrumb, PaginatedResponse, ChunkedUpload*, etc. |
| **:domain:admin**  | Admin     | Admin use-case interfaces, AdminUser, UserRole, UserStatus. |
| **:domain:sync**   | Sync      | Sync use-case interfaces, SyncDevice, SyncResponse, SyncConflict. |
| **:domain:share**  | Share     | Share use-case interfaces, ShareLink. |
| **:domain:activity** | Activity | Activity use-case interfaces, Activity model. |
| **:domain:metadata** | Metadata | Metadata use-case interfaces, FileMetadata, ImageMetadata, etc. |
| **:domain:plugin**  | Plugin   | Plugin use-case interfaces, PluginInfo. |
| **:domain:version** | Version  | Version use-case interfaces, FileVersion, FileVersionHistory, VersionDiff. |
| **:domain:collaboration** | Collaboration | Collaboration use-case interfaces, CollaborationSession, DocumentComment, etc. |
| **:domain:federation** | Federation | Federation use-case interfaces, FederatedInstance, FederatedShare, etc. |
| **:domain:ai**      | AI       | AI use-case interfaces, AIProviderInfo, AIModel, AIChatResponse. |
| **:domain:config**  | Config   | Config use-case interfaces (e.g. GetStorageUrlsUseCase). |
| **:domain:upload**  | Upload   | UploadQueueEntry, ChunkedFileSource (or merge into :domain:storage if preferred). |

- **:domain:result** is depended on by all other domain modules that return `Result<T>` or use shared exceptions.
- Other **:domain:xxx** modules may depend on **:domain:result** and optionally on each other when an area uses another area’s types (prefer minimal cross-area deps).

### 2.2 Domain layout (directory)

```
frontend/domain/
├── result/
│   └── build.gradle.kts
├── auth/
│   └── build.gradle.kts
├── storage/
│   └── build.gradle.kts
├── admin/
│   └── build.gradle.kts
├── sync/
│   └── build.gradle.kts
├── share/
│   └── build.gradle.kts
├── activity/
│   └── build.gradle.kts
├── metadata/
│   └── build.gradle.kts
├── plugin/
│   └── build.gradle.kts
├── version/
│   └── build.gradle.kts
├── collaboration/
│   └── build.gradle.kts
├── federation/
│   └── build.gradle.kts
├── ai/
│   └── build.gradle.kts
├── config/
│   └── build.gradle.kts
└── upload/
    └── build.gradle.kts
```

In **settings.gradle.kts**: `include(":domain:result")`, `include(":domain:auth")`, … (no `include(":domain")` with code).

---

## 3. Data: submodules by area/scope

**data** is a container; each area is a submodule under `data/<area>`. There is also a **shared** data layer (network, maybe common DTOs).

- **commonMain** (+ platform source sets if needed for Ktor/Coil).
- Each **:data:xxx** implements repository interfaces from the corresponding **:domain:xxx**, and depends on **:domain:xxx** (and usually **:domain:result**). It can depend on **:data:network** for `ApiResult` and HTTP.

### 3.1 Suggested data submodules

| Submodule           | Scope    | Typical contents |
|---------------------|----------|-------------------|
| **:data:network**   | Shared   | ApiResult, BaseApi, HttpClientFactory; optionally ApiResultMapper (ApiResult → Result). |
| **:data:auth**      | Auth     | AuthApi, AuthRepository impl, AuthService, AuthMapper, auth DTOs, TokenStorage. |
| **:data:storage**   | Storage  | StorageApi, StorageRepository impl, StorageService, StorageMapper, storage DTOs. |
| **:data:admin**     | Admin    | AdminApi, AdminRepository impl, AdminService, AdminMapper, admin DTOs. |
| **:data:sync**      | Sync     | SyncApi, SyncRepository impl, SyncService, SyncMapper, sync DTOs. |
| **:data:share**     | Share    | ShareApi, ShareRepository impl, ShareService, ShareMapper, share DTOs. |
| **:data:activity**  | Activity | ActivityApi, ActivityRepository impl, ActivityService, ActivityMapper, activity DTOs. |
| **:data:metadata**  | Metadata | MetadataApi, MetadataRepository impl, MetadataService, MetadataMapper, metadata DTOs. |
| **:data:plugin**    | Plugin   | PluginApi, PluginRepository impl, PluginService, PluginMapper, plugin DTOs. |
| **:data:version**   | Version  | VersionApi, VersionRepository impl, VersionService, VersionMapper, version DTOs. |
| **:data:collaboration** | Collaboration | CollaborationApi, CollaborationRepository impl, CollaborationService, CollaborationMapper, CollaborationWebSocket, DTOs. |
| **:data:federation** | Federation | FederationApi, FederationRepository impl, FederationService, FederationMapper, federation DTOs. |
| **:data:ai**        | AI       | AIApi, AIRepository impl, AIService, AIMapper, AI DTOs. |
| **:data:config**    | Config   | ConfigRepository impl if any, config-related API. |

- **:data:network** is depended on by other **:data:xxx** modules. The mapper from ApiResult to Result can live in **:data:network** or in a small **:data:common** if you prefer.
- **:data:xxx** depends on **:domain:xxx** and **:domain:result** (and **:data:network** where needed).

### 3.2 Data layout (directory)

```
frontend/data/
├── network/
│   └── build.gradle.kts
├── auth/
│   └── build.gradle.kts
├── storage/
│   └── build.gradle.kts
├── admin/
│   └── build.gradle.kts
├── sync/
│   └── build.gradle.kts
├── share/
│   └── build.gradle.kts
├── activity/
│   └── build.gradle.kts
├── metadata/
│   └── build.gradle.kts
├── plugin/
│   └── build.gradle.kts
├── version/
│   └── build.gradle.kts
├── collaboration/
│   └── build.gradle.kts
├── federation/
│   └── build.gradle.kts
├── ai/
│   └── build.gradle.kts
└── config/
    └── build.gradle.kts
```

In **settings.gradle.kts**: `include(":data:network")`, `include(":data:auth")`, … (no single `include(":data")` with code).

---

## 4. Feature: submodules per screen

**feature** is a container; each **screen** is a submodule under `feature/<screen>`.

- KMP library with Compose; depends on the **:domain** and **:data** modules it needs (use-case interfaces and repository types; DI is assembled in composeApp).
- Contains ViewModels, screen UI, and screen-specific components for that screen.

### 4.1 Suggested feature submodules (per screen)

| Submodule            | Screen        |
|----------------------|---------------|
| **:feature:auth**    | Login / register |
| **:feature:admin**   | Admin users   |
| **:feature:sync**    | Sync / devices |
| **:feature:shares**  | My shares     |
| **:feature:sharedwithme** | Shared with me |
| **:feature:activity** | Activity     |
| **:feature:profile** | Profile       |
| **:feature:settings** | Settings     |
| **:feature:security** | Security / sessions |
| **:feature:changepassword** | Change password |
| **:feature:plugins** | Plugins       |
| **:feature:files**   | Files / storage browser |
| **:feature:upload**  | Upload        |
| **:feature:versionhistory** | Version history |
| **:feature:collaboration** | Collaboration |
| **:feature:federation** | Federation  |
| **:feature:ai**      | AI            |
| **:feature:main**    | Main shell / navigation (optional; can live in composeApp) |
| **:feature:licenses**| Licenses      |

### 4.2 Feature layout (directory)

```
frontend/feature/
├── auth/
│   └── build.gradle.kts
├── admin/
│   └── build.gradle.kts
├── sync/
│   └── build.gradle.kts
├── shares/
│   └── build.gradle.kts
├── sharedwithme/
│   └── build.gradle.kts
├── activity/
│   └── build.gradle.kts
├── profile/
│   └── build.gradle.kts
├── settings/
│   └── build.gradle.kts
├── security/
│   └── build.gradle.kts
├── changepassword/
│   └── build.gradle.kts
├── plugins/
│   └── build.gradle.kts
├── files/
│   └── build.gradle.kts
├── upload/
│   └── build.gradle.kts
├── versionhistory/
│   └── build.gradle.kts
├── collaboration/
│   └── build.gradle.kts
├── federation/
│   └── build.gradle.kts
├── ai/
│   └── build.gradle.kts
├── main/
│   └── build.gradle.kts
└── licenses/
    └── build.gradle.kts
```

In **settings.gradle.kts**: `include(":feature:auth")`, `include(":feature:admin")`, … (no single `include(":feature")` with code).

---

## 5. composeApp, androidApp, iosApp

- **composeApp**: App shell; navigation, DI (aggregates modules from **:data:*** and **:feature:***), resources. Depends on all **:feature:*** modules (and possibly **:data:*** for DI only). No business logic; only wiring.
- **androidApp** / **iosApp**: As today; depend only on **composeApp**.

---

## 6. Dependency direction (summary)

- **:domain:result** ← every other **:domain:xxx** that uses Result.
- **:domain:xxx** ← **:data:xxx** (data implements domain interfaces).
- **:data:network** ← **:data:auth**, **:data:storage**, … (where they need ApiResult/HTTP).
- **:domain:xxx** ← **:feature:yyy** (feature uses use-case interfaces and domain types).
- **:data:xxx** ← **:feature:yyy** (feature may use repository interfaces or Koin modules from data).
- **composeApp** ← all **:feature:*** (and optionally **:data:*** for DI).

So: **composeApp** → **feature:*** → **domain:*** and **data:*** → **domain:***.

---

## 7. frontend/settings.gradle.kts (example)

```kotlin
rootProject.name = "frontend"

// Domain (by area)
include(":domain:result")
include(":domain:auth")
include(":domain:storage")
include(":domain:admin")
include(":domain:sync")
include(":domain:share")
include(":domain:activity")
include(":domain:metadata")
include(":domain:plugin")
include(":domain:version")
include(":domain:collaboration")
include(":domain:federation")
include(":domain:ai")
include(":domain:config")
include(":domain:upload")

// Data (by area)
include(":data:network")
include(":data:auth")
include(":data:storage")
include(":data:admin")
include(":data:sync")
include(":data:share")
include(":data:activity")
include(":data:metadata")
include(":data:plugin")
include(":data:version")
include(":data:collaboration")
include(":data:federation")
include(":data:ai")
include(":data:config")

// Feature (per screen)
include(":feature:auth")
include(":feature:admin")
include(":feature:sync")
include(":feature:shares")
include(":feature:sharedwithme")
include(":feature:activity")
include(":feature:profile")
include(":feature:settings")
include(":feature:security")
include(":feature:changepassword")
include(":feature:plugins")
include(":feature:files")
include(":feature:upload")
include(":feature:versionhistory")
include(":feature:collaboration")
include(":feature:federation")
include(":feature:ai")
include(":feature:main")
include(":feature:licenses")

// App and platforms
include(":composeApp")
include(":androidApp")
include(":iosApp")
```

---

## 8. backend/settings.gradle.kts (unchanged concept)

```kotlin
rootProject.name = "backend"

include(":core")
include(":api")
include(":plugins-api")
include(":infrastructure")
include(":plugins:image-metadata")
include(":plugins:video-metadata")
include(":plugins:fulltext-search")
include(":plugins:ai-classification")
```

---

## 9. Root: no Gradle

- Remove or do not create **build.gradle.kts**, **settings.gradle.kts**, and **gradle/** at the repository root.
- **backend** and **frontend** each have their own Gradle wrapper (**gradlew**, **gradle/wrapper**), **gradle/libs.versions.toml**, and **config/** (e.g. detekt). They are fully self-contained.

---

## 10. Extraction plan: monolith → modules

To move code from the **composeApp** monolith into **domain**, **data**, and **feature** modules, follow this order per area (one vertical slice at a time).

### 10.1 Order per area (vertical slice)

For each area (e.g. storage, auth, admin):

1. **Domain**  
   Move from composeApp to **:domain:&lt;area&gt;**:
   - Domain **models** (from `composeApp/domain/model` by area).
   - Repository **interface** (e.g. `StorageRepository`) if it lives in composeApp.
   - Use-case **interfaces** only (from `composeApp/domain/usecase/&lt;area&gt;`).  
   Keep use-case **implementations** and repository **implementations** in composeApp (or move them in step 2).

2. **Data**  
   Move from composeApp to **:data:&lt;area&gt;**:
   - API, Service, DTOs, Mappers.
   - Repository **implementation** and use-case **implementations** that depend on that repository/API.  
   Add dependency on **:domain:&lt;area&gt;** and **:data:network** (and **:domain:result**).  
   Expose a Koin module if the area registers components.

3. **Feature**  
   Move from composeApp to **:feature:&lt;screen&gt;**:
   - ViewModel, Component, Content, and screen-specific UI (from `composeApp/feature/&lt;area&gt;` and `composeApp/ui/screens` for that screen).  
   Add dependency on the **:domain:*** and **:data:*** modules the screen needs.

4. **ComposeApp**  
   After each slice: keep only **app shell** (navigation, DI wiring, config, platform, shared UI/theme).  
   ComposeApp depends on **:feature:*** (and optionally **:data:*** for DI) and no longer contains the moved domain/data/feature code.

### 10.2 Suggested slice order

- **Storage** (domain:storage → data:storage → feature:files).
- **Auth** (domain:auth already has content; move auth data/impl from composeApp to data:auth; move auth UI to feature:auth).
- **Admin, Sync, Share, Activity, Metadata, Plugin, Version, Collaboration, Federation, AI, Config, Upload** (same pattern).

### 10.3 Package after move

- **:domain:storage**: `com.vaultstadio.app.domain.storage` (model/, usecase/; repository interface at root or in repository/).
- **:data:storage**: `com.vaultstadio.app.data.storage` (api/, service/, dto/, mapper/, repository/).
- **:feature:files**: `com.vaultstadio.app.feature.files` (ViewModel, Component, Content, etc.).

---

## 11. Implementation notes

- **Package names**: Can stay as today (e.g. `com.vaultstadio.app.domain.auth`, `com.vaultstadio.app.data.storage`); moving code is mainly moving files and adjusting module dependencies.
- **Koin / KSP**: Each **:data:xxx** and **:feature:xxx** can expose a Koin module; **composeApp** collects them and provides them to the app.
- **Cross-domain/data deps**: Prefer a single area per module; where an area needs another (e.g. feature:files needs domain:storage and domain:share), depend only on the required **:domain:*** and **:data:*** modules.
- **Testing**: Each **:domain:xxx**, **:data:xxx**, and **:feature:xxx** can have **commonTest**; run tests from **frontend** root (e.g. on JVM/desktop for KMP).

---

## 12. References

- [ARCHITECTURE.md](ARCHITECTURE.md) – Current system and module layout.
- [CLEAN_ARCHITECTURE_REVIEW.md](CLEAN_ARCHITECTURE_REVIEW.md) – Clean Architecture and Result/ApiResult boundary.
- [Gradle Version Catalog](https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog).
