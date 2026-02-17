# Glossary

**Last updated**: 2026-02-16

Short definitions of terms used across VaultStadio documentation and code. Links point to where the concept is explained in detail.

---

## Backend / Domain

| Term | Definition | See |
|------|------------|-----|
| **StorageItem** | Domain entity representing a file or folder: id, name, path, type, size, mimeType, parentId, isStarred, isTrashed, timestamps. | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md), [api/API.md](api/API.md) |
| **User** | Registered user: id, email, username, role (user/admin). Quota (used/total) is exposed via API. | [api/API.md](api/API.md) |
| **ShareLink** | A shareable link for a file or folder; optional password, expiration, download limit. | [api/API.md](api/API.md) (Share Endpoints) |
| **Plugin** | Extension that implements the Plugin interface: metadata, lifecycle (onInitialize, onShutdown), event subscriptions, optional custom endpoints. | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md), [plugins/PLUGIN_DEVELOPMENT.md](plugins/PLUGIN_DEVELOPMENT.md) |
| **PluginContext** | API provided to plugins: eventBus, storage (read files), metadata (set/get), config, HTTP. | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) |
| **PluginMetadata** | Plugin identity: id, name, version, description, author, permissions. | [plugins/PLUGIN_DEVELOPMENT.md](plugins/PLUGIN_DEVELOPMENT.md) |
| **FileEvent** | Sealed hierarchy of storage events (e.g. Uploaded, Downloaded, Deleted) published by the core; plugins subscribe via EventBus. | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) |
| **Either** | Arrow type for type-safe errors: `Either<Left, Right>` (e.g. `Either<StorageException, ByteArray>`). Used instead of exceptions in core. | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) |
| **ApiResponse** | Standard REST wrapper: success flag, data, optional error (code, message, details). | [api/API.md](api/API.md) |
| **StorageBackend** | Interface for storing file bytes: Local (filesystem) or S3-compatible. | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md), [operations/STORAGE_CONFIGURATION.md](operations/STORAGE_CONFIGURATION.md) |

---

## Frontend

| Term | Definition | See |
|------|------------|-----|
| **Decompose** | Navigation library for Kotlin Multiplatform: component lifecycle, ChildStack for type-safe back stack. | [architecture/FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md) |
| **Component** | Decompose unit (e.g. FilesComponent, AuthComponent): holds ViewModel and child configuration; created by parent. | [architecture/FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md) |
| **ChildStack** | Decompose value holding the current navigation stack (config → component). Used by RootComponent and MainComponent. | [architecture/FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md) |
| **MainDestination** | Enum of main app destinations: FILES, RECENT, STARRED, TRASH, SHARED, SETTINGS, ADMIN, AI, SYNC, FEDERATION, etc. | [architecture/FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md) |
| **ViewModel (per-screen)** | One ViewModel per screen (e.g. FilesViewModel, AuthViewModel); holds state and calls repositories/API. | [architecture/FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md), [frontend/FRONTEND_FEATURES.md](frontend/FRONTEND_FEATURES.md) |
| **expect / actual** | Kotlin Multiplatform: declare API in commonMain (`expect`), implement per platform (e.g. `actual` in desktopMain, wasmJsMain). Used for file picker, storage, download, drag-drop. | [architecture/FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md) |
| **VaultStadioApi** | Shared KMP API client (Ktor); used by frontend repositories to call the backend REST API. | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md), [api/API.md](api/API.md) |
| **ApiResult** | Sealed type for API outcomes: Success, Error, NetworkError. Used in ViewModels. | Frontend codebase |

---

## Operations / Deployment

| Term | Definition | See |
|------|------------|-----|
| **JWT** | JSON Web Token used for authentication; sent as `Authorization: Bearer <token>`. | [api/API.md](api/API.md) (Authentication) |
| **WebDAV** | Protocol for file access over HTTP; allows mounting VaultStadio as a network drive. | [api/API.md](api/API.md) (WebDAV), [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) |
| **S3-compatible API** | Subset of AWS S3 operations; enables tools like rclone, s3cmd, AWS CLI. | [api/API.md](api/API.md) (S3), [operations/STORAGE_CONFIGURATION.md](operations/STORAGE_CONFIGURATION.md) |
| **Federation** | Linking multiple VaultStadio instances: discovery, trust, cross-instance shares and identities. | [api/API.md](api/API.md) (Federation), [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) |
| **Sync (protocol)** | Client-server sync: device registration, pull/push changes, conflict resolution, delta transfer. | [api/API.md](api/API.md) (Sync), [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) |

---

## Documentation

| Term | Definition | See |
|------|------------|-----|
| **INDEX** | Central documentation entry point; lists all docs by category and audience. | [INDEX.md](INDEX.md) |
| **DOCS_STRUCTURE** | Describes the docs directory layout and how to navigate it. | [DOCS_STRUCTURE.md](DOCS_STRUCTURE.md) |

---

If a term is missing or unclear, consider adding it here and linking to the relevant doc. Keep definitions short; detail stays in the linked document.
