# What VaultStadio Offers

**Last updated**: 2026-02-16

This page summarises what VaultStadio is, who it is for, and what it offers from a product and technical perspective. Use it as a one-page orientation for humans and AIs.

---

## In One Sentence

VaultStadio is a **self-hosted, plugin-extensible storage platform** with a modern Kotlin backend (Ktor), a Compose Multiplatform frontend (Web, Android, iOS, Desktop), and shared business logic via Kotlin Multiplatform.

---

## Target Users

- **End users**: People who want to store, share, and manage files in their own infrastructure (e.g. TrueNAS, Docker, Kubernetes).
- **Developers**: Contributors or integrators who extend the backend (plugins, API) or the frontend (Compose UI).
- **Operators**: Teams who deploy, configure, secure, and monitor the service.

---

## Product Capabilities (What Users Get)

### Core

- **File storage**: Upload, download, organise in folders; multi-platform clients (Web, Desktop, Android, iOS).
- **Sharing**: Share links with optional password and expiration; control who sees what.
- **User management**: Register, login, profiles; admin can manage users and quotas.
- **Activity**: Audit trail of who did what (uploads, downloads, shares).

### Advanced (Phase 6)

- **File versioning**: Full history, restore previous versions, diff between versions.
- **Sync**: Multi-device sync with conflict resolution and delta transfer.
- **Real-time collaboration**: Multi-user editing with operational transformation (OT) and presence.
- **Federation**: Share and collaborate across multiple VaultStadio instances.
- **WebDAV & S3**: Mount storage as a network drive (WebDAV) or use S3-compatible tools (rclone, AWS CLI).
- **AI**: Optional image tagging, content classification, summarisation, chat; pluggable providers (Ollama, OpenRouter, etc.).

---

## Technical Offer (Backend)

- **REST API** under `/api/v1/`: auth, storage, batch ops, search, metadata, shares, admin, plugins, AI, versioning, sync, federation, collaboration. See [api/API.md](api/API.md).
- **Plugin system**: Event-driven; plugins can react to file events, read/write metadata, register custom endpoints, run background tasks. See [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) and [plugins/PLUGIN_DEVELOPMENT.md](plugins/PLUGIN_DEVELOPMENT.md).
- **Storage backends**: Local filesystem and S3-compatible (e.g. MinIO). See [operations/STORAGE_CONFIGURATION.md](operations/STORAGE_CONFIGURATION.md).
- **Stack**: Kotlin, Ktor, Coroutines, Arrow `Either`, PostgreSQL (Exposed, Flyway), optional Redis for multi-instance.

---

## Technical Offer (Frontend)

- **Compose Multiplatform**: Shared UI for Web (WASM), Android, iOS, Desktop (JVM).
- **Decompose**: Type-safe, stack-based navigation; per-screen ViewModels (e.g. FilesViewModel, AuthViewModel).
- **Shared module (KMP)**: API client, DTOs, repositories used by all platforms.
- **Platform abstractions**: expect/actual for file picker, token storage, download, drag-and-drop (Web vs Desktop differ).
- **Features**: Multi-selection, batch delete/move/copy/star, chunked upload (large files), folder upload, file preview, info panel, keyboard shortcuts. See [architecture/FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md) and [frontend/FRONTEND_FEATURES.md](frontend/FRONTEND_FEATURES.md).

---

## Where to Go Next

| Goal | Document |
|------|----------|
| Run the app in 5 minutes | [getting-started/QUICK_START.md](getting-started/QUICK_START.md) |
| Understand system design | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) |
| Use or implement the API | [api/API.md](api/API.md) |
| Understand the frontend | [architecture/FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md) |
| Contribute (including with AI) | [../CONTRIBUTING.md](../CONTRIBUTING.md), [development/AI_CODING_GUIDELINES.md](development/AI_CODING_GUIDELINES.md) |
| Deploy to production | [operations/DEPLOYMENT.md](operations/DEPLOYMENT.md) |
| Look up a term | [GLOSSARY.md](GLOSSARY.md) |
| See all docs | [INDEX.md](INDEX.md) |
