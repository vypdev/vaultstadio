# Directory Structure Analysis

Analysis of directories with many files and recommendations for grouping by theme/area (frontend and backend). Generated from a file-count scan.

---

## Summary: Directories with Most Files

### Frontend (`compose-frontend/composeApp/src/commonMain/kotlin/com/vaultstadio/app/`)

| Directory | File count | Notes |
|-----------|------------|--------|
| `ui/components` | **4** (root) + **files/** (12) + **dialogs/** (6) + **layout/** (6) | **Done.** Split into `files/`, `dialogs/`, `layout/` subpackages. |
| `domain/usecase/storage` | **22** | Single theme (storage). Acceptable as-is or split by subdomain. |
| `ui/screens` | **17** (root) + subdirs | Root has screen files; subdirs (activity/, admin/, ai/, …) already thematic. OK. |
| `data/repository` | **16** | One repo per domain. By-layer structure is standard. OK. |
| `domain/usecase/federation` | **15** | Single theme. OK. |
| `domain/model` | **15** | Shared models. OK. |
| `feature/files` | **14** | Files feature. OK. |
| `domain/usecase/collaboration` | **13** | Single theme. OK. |
| `domain/usecase/ai` | **12** | Single theme. OK. |
| `data/service` | **12** (+ .tmp) | One service per domain. Remove `.tmp` leftovers. OK. |
| `data/mapper` | **12** | One mapper per domain. OK. |
| `data/api` | **12** | One API per domain. OK. |

### Backend

| Directory | File count | Notes |
|-----------|------------|--------|
| `api/routes` | **1** (root) + **12 subdirs** | **Done.** Grouped by domain: auth, storage, federation, collaboration, ai, sync, version, share, metadata, admin, activity, plugin, health. |
| `core/domain/service` | **19** | Services + extracted helpers. **Optional subdirs by domain.** |
| `infrastructure/persistence` | **13** | Repos + mappers. OK. |

---

## Recommendations

### 1. Frontend: `ui/components` — DONE

**Done:** Components are grouped by theme:

- **`ui/components/files/`** – `FileGridItem`, `FileItem`, `SelectableFileItem`, `FileInfoPanel`, `FileInfoPanelParts`, `FilePreview`, `FilePreviewDialog`, `MetadataPanel`, `VersionPanel`, `StorageChart`, `UploadBanner`, `EmptyState`; plus `Breadcrumbs` and a second `EmptyState` overload in `FileItem.kt`.
- **`ui/components/dialogs/`** – `AdvancedSearchDialog`, `ContextMenu`, `FilterDialog`, `MoveDialog`, `SortDialog`, `UploadDialog` (and `UploadItem`/`UploadStatus`).
- **`ui/components/layout/`** – `Breadcrumbs` (path with `Breadcrumb`), `DragOverlay`, `DropZone`, `KeyboardShortcuts`, `MainSidebar`, `SelectionToolbar`.
- **`ui/components/`** (root) – `ComponentsTest.kt` only.

All consumer imports were updated. Tests were moved to `components/files/`, `components/dialogs/`, `components/layout/` with matching packages.

---

### 2. Frontend: `domain/usecase/storage` (22 files)

**Current:** All storage use cases in one folder (GetFolderItems, UploadFile, BatchMove, …).

**Recommendation:** Keep as-is, or split by subdomain if it grows:

- **Option A (minimal):** Leave as single `storage/` (22 files is still manageable).
- **Option B:** Subdirs e.g. `storage/items/`, `storage/batch/`, `storage/trash/` if you prefer smaller folders.

---

### 3. Backend: `api/routes` — DONE

**Done:** Routes are grouped by domain; packages are `com.vaultstadio.api.routes.<domain>`.

- **`api/routes/auth/`** – `AuthRoutes.kt`
- **`api/routes/storage/`** – `StorageRoutes`, `S3Routes`, `S3Handlers`, `WebDAVRoutes`, `WebDAVHandlers`, `ChunkedUploadRoutes`, `BatchRoutes`, `FolderUploadRoutes`, `ThumbnailRoutes`
- **`api/routes/federation/`** – `FederationRoutes`, `FederationHandlers`
- **`api/routes/collaboration/`** – `CollaborationRoutes`, `CollaborationRoutesModels`, `CollaborationWebSocket`
- **`api/routes/ai/`** – `AIRoutes`, `AIHandlers`
- **`api/routes/sync/`** – `SyncRoutes`, `SyncHandlers`
- **`api/routes/version/`** – `VersionRoutes`
- **`api/routes/share/`** – `ShareRoutes`
- **`api/routes/metadata/`** – `MetadataRoutes`, `SearchRoutes`
- **`api/routes/admin/`** – `AdminRoutes`, `UserRoutes`
- **`api/routes/activity/`** – `ActivityRoutes`
- **`api/routes/plugin/`** – `PluginRoutes`
- **`api/routes/health/`** – `HealthRoutes`
- **`api/routes/`** (root) – `RouteExtensions.kt` only

`Routing.kt` imports were updated to use the new subpackages. API tests under `api/src/test/.../routes/` were updated to import DTOs from the corresponding subpackages (e.g. `com.vaultstadio.api.routes.storage.BatchResult`, `com.vaultstadio.api.routes.collaboration.JoinSessionRequest`).

---

### 4. Backend: `core/domain/service` (19 files)

**Current:** Services and extracted helpers (StorageService, StorageServiceWrite, FederationService, FederationServiceMaintenance, …).

**Recommendation:** Optional subdirs by domain; only if you want clearer grouping:

- **`service/storage/`** – `StorageService`, `StorageServiceQueries`, `StorageServiceWrite`, `StorageServiceMutations`
- **`service/federation/`** – `FederationService`, `FederationServiceMaintenance`, `FederationServiceSigning`, `FederationCrypto.kt`
- **`service/collaboration/`** – `CollaborationService`, `CollaborationOT`
- **`service/sync/`** – `SyncService`, `DeltaSync`
- **`service/`** (root) – `UserService`, `ShareService`, `FileVersionService`, `ActivityLogger`, `LockManager`, `MultipartUploadManager`, `TransactionManager`

**Impact:** Package renames; DI and all references to these services must be updated. Higher effort; only do if you value domain grouping over a single flat service folder.

---

### 5. Data layer (frontend): `data/api`, `data/mapper`, `data/repository`, `data/service`

**Current:** By layer (api, mapper, repository, service) with one file per domain (StorageApi, StorageMapper, …). Pattern is consistent.

**Recommendation:** Keep by-layer structure. Alternative “by domain” (e.g. `data/storage/` with Api + Mapper + Repository + Service) would cross-cut the current layering and require a larger refactor for limited benefit.

**Cleanup:** Remove any `.tmp` files under `data/service/` (e.g. `AIService.kt.tmp`, `StorageService.kt.tmp`) if they are leftovers.

---

## What Is Already in Good Shape

- **Frontend:** `ui/screens` (root + feature subdirs like `activity/`, `admin/`, `ai/`), `feature/*`, `domain/model`, and the data layer (api/mapper/repository/service) are already thematic or follow a clear pattern.
- **Backend:** `infrastructure/persistence` (repos + mappers), `core/domain/repository`, `core/domain/model` are coherent. Routes and services are the only “many files in one dir” spots.

---

## Suggested Order of Work

1. **Low risk / cleanup**
   - Delete `*.tmp` under `compose-frontend/.../data/service/` if unused.
2. **Optional, medium effort**
   - Frontend: Split `ui/components` into `components/files/`, `components/dialogs/`, `components/layout/` (with package and import updates).
3. **Optional, higher effort**
   - Backend: Group `api/routes` into subdirs by domain (auth, storage, federation, collaboration, ai, sync, etc.) and update registration/imports.
   - Backend: Optionally group `core/domain/service` into subdirs by domain.

If you tell me which of these you want to apply first (e.g. only cleanup, or components split, or routes subdirs), I can outline the exact file moves and package/import changes step by step.
