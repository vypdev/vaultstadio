# VaultStadio Architecture v2.0

**Last updated**: 2026-02-16

## Overview

VaultStadio is a self-hosted, plugin-extensible storage platform built with:

- **Backend**: Kotlin/Ktor with coroutines for high-performance async I/O
- **Frontend**: Compose Multiplatform for Web, Android, iOS, and Desktop
- **Frontend**: Compose Multiplatform holds shared business logic and API client in `composeApp/commonMain`

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              VaultStadio                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     Frontend (Compose Multiplatform)                 │   │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐            │   │
│  │  │    Web    │ │  Android  │ │    iOS    │ │  Desktop  │            │   │
│  │  │  (Wasm)   │ │           │ │           │ │  (JVM)    │            │   │
│  │  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘            │   │
│  │        └─────────────┼─────────────┼─────────────┘                   │   │
│  │                      ▼                                               │   │
│  │              ┌───────────────┐                                       │   │
│  │              │ Shared Module │ (KMP - Business Logic, API Client)    │   │
│  │              └───────┬───────┘                                       │   │
│  └──────────────────────┼───────────────────────────────────────────────┘   │
│                         │ HTTP/REST                                         │
│                         ▼                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     Backend (Kotlin/Ktor)                            │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐│   │
│  │  │                      API Layer (Ktor)                            ││   │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────────┐││   │
│  │  │  │  Auth    │ │ Storage  │ │  Share   │ │      Plugins         │││   │
│  │  │  │ Routes   │ │  Routes  │ │  Routes  │ │      Routes          │││   │
│  │  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────────┬───────────┘││   │
│  │  └───────┼────────────┼────────────┼──────────────────┼────────────┘│   │
│  │          └────────────┼────────────┼──────────────────┘              │   │
│  │                       ▼            ▼                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐│   │
│  │  │                     Core Layer                                   ││   │
│  │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────────┐ ││   │
│  │  │  │   Services   │ │  Event Bus   │ │     Domain Models        │ ││   │
│  │  │  │ (Storage,    │ │  (Pub/Sub)   │ │  (StorageItem, User,     │ ││   │
│  │  │  │  User, Share)│ │              │ │   ShareLink, etc.)       │ ││   │
│  │  │  └──────┬───────┘ └──────┬───────┘ └──────────────────────────┘ ││   │
│  │  └─────────┼────────────────┼──────────────────────────────────────┘│   │
│  │            │                │                                        │   │
│  │            │                ▼                                        │   │
│  │  ┌─────────┼──────────────────────────────────────────────────────┐ │   │
│  │  │         │           Plugin System                               │ │   │
│  │  │         │    ┌──────────────┐ ┌──────────────┐ ┌─────────────┐ │ │   │
│  │  │         │    │  Metadata    │ │     AI       │ │   Custom    │ │ │   │
│  │  │         │    │  Extractor   │ │   Analysis   │ │   Plugins   │ │ │   │
│  │  │         │    └──────────────┘ └──────────────┘ └─────────────┘ │ │   │
│  │  └─────────┼──────────────────────────────────────────────────────┘ │   │
│  │            ▼                                                         │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐│   │
│  │  │                   Infrastructure Layer                           ││   │
│  │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────────┐ ││   │
│  │  │  │  PostgreSQL  │ │   Storage    │ │        Security          │ ││   │
│  │  │  │  (Exposed)   │ │   Backend    │ │       (BCrypt)           │ ││   │
│  │  │  │              │ │ (Local/S3)   │ │                          │ ││   │
│  │  │  └──────────────┘ └──────────────┘ └──────────────────────────┘ ││   │
│  │  └─────────────────────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Backend Architecture

### Module Structure

```
kotlin-backend/
├── core/                    # Core domain logic
│   ├── domain/
│   │   ├── model/          # Domain entities (StorageItem, User, etc.)
│   │   ├── repository/     # Repository interfaces
│   │   ├── service/        # Business logic services
│   │   └── event/          # Event system for plugin communication
│   ├── exception/          # Exception hierarchy
│   └── util/               # Utility functions
│
├── plugins-api/             # Plugin SDK
│   ├── api/                # Plugin interface and metadata
│   ├── lifecycle/          # Plugin lifecycle management
│   ├── context/            # Plugin context (APIs available to plugins)
│   ├── hooks/              # Hook system for intercepting operations
│   ├── config/             # Plugin configuration schema
│   └── metadata/           # Metadata extraction base classes
│
├── api/                     # REST API layer
│   ├── routes/             # Ktor route handlers
│   ├── dto/                # Data transfer objects
│   ├── security/           # Authentication/authorization
│   ├── plugins/            # Plugin manager
│   ├── config/             # Application configuration
│   └── middleware/         # Error handling, logging
│
├── infrastructure/          # Infrastructure implementations
│   ├── persistence/        # Database (Exposed ORM)
│   │   └── entities/       # Table definitions
│   ├── storage/            # Storage backends (Local, S3, MinIO)
│   └── security/           # Password hashing
│
└── plugins/                 # Built-in plugins
    ├── image-metadata/
    ├── video-metadata/
    └── document-indexer/
```

### Core Design Principles

1. **Neutral Core**: The core knows nothing about file types or specific operations. It handles:
   - File storage and retrieval
   - Folder structure management
   - User authentication and authorization
   - Sharing and access control
   - Event publishing

2. **Plugin System**: All specialized functionality is implemented as plugins:
   - Metadata extraction (images, videos, documents)
   - Content classification
   - AI integration
   - Thumbnail generation
   - Full-text search indexing

3. **Event-Driven Architecture**: The core publishes events that plugins can subscribe to:
   - `FileEvent.Uploaded` - When a file is uploaded
   - `FileEvent.Downloaded` - When a file is downloaded
   - `FileEvent.Deleted` - When a file is deleted
   - And many more...

4. **Functional Error Handling**: Using Arrow's `Either` type for explicit error handling without exceptions.

### Plugin Architecture

```kotlin
// Plugin implementation example
class ImageMetadataPlugin : AbstractPlugin() {
    override val metadata = PluginMetadata(
        id = "com.vaultstadio.image-metadata",
        name = "Image Metadata Extractor",
        version = "1.0.0",
        description = "Extracts EXIF and other metadata from images",
        author = "VaultStadio",
        permissions = listOf(
            PluginPermission.READ_FILES,
            PluginPermission.WRITE_METADATA
        )
    )
    
    override suspend fun onInitialize(context: PluginContext) {
        // Subscribe to file upload events
        context.eventBus.subscribe<FileEvent.Uploaded>(metadata.id) { event ->
            if (event.item.mimeType?.startsWith("image/") == true) {
                extractImageMetadata(event.item, context)
            }
            EventHandlerResult.Success
        }
    }
    
    private suspend fun extractImageMetadata(item: StorageItem, context: PluginContext) {
        context.storage.readFile(item.id).onRight { stream ->
            val metadata = // ... extract metadata
            context.metadata.setValues(item.id, metadata)
        }
    }
}
```

### Plugin Capabilities

| Capability | Description |
|------------|-------------|
| Event Subscription | React to storage events |
| File Access | Read file content (with permission) |
| Metadata Storage | Attach custom metadata to files |
| Custom Endpoints | Register API endpoints under `/plugins/{id}/` |
| Background Tasks | Schedule periodic tasks |
| Configuration | User-configurable settings |

## Frontend Architecture

### Compose Multiplatform Structure

```
compose-frontend/
├── composeApp/
│   ├── commonMain/         # Shared logic + Compose UI (API client, models, ViewModels)
│   │   ├── domain/model/   # Shared data models
│   │   ├── network/        # API client (Ktor)
│   │   ├── ui/
│   │   │   ├── theme/      # Material 3 theme
│   │   │   ├── components/ # Reusable components
│   │   │   └── screens/    # Screen composables
│   │   └── viewmodel/      # ViewModels
│   ├── desktopMain/        # Desktop entry point
│   └── wasmJsMain/         # Web entry point
├── androidApp/             # Android app
├── iosApp/                 # iOS app
└── desktopApp/            # Desktop app
```

### UI Components

#### Core Components
- **Sidebar**: Navigation with storage quota display
- **FileGridItem / FileListItem**: File and folder display
- **Breadcrumbs**: Navigation path
- **EmptyState**: Placeholder for empty views
- **Login/Register screens**: Authentication

#### Multi-Selection & Batch Operations
- **SelectableFileGridItem / SelectableFileListItem**: Items with selection checkboxes
- **SelectionToolbar**: Floating toolbar with batch actions (delete, move, copy, download ZIP, star)

#### File Management
- **FileInfoPanel**: Sidebar showing detailed file properties and quick actions
- **FilePreviewDialog**: Full-screen preview for images, video, audio, PDF, text
- **MoveDialog**: Dialog for selecting destination folder
- **ContextMenu**: Right-click menu with file actions

#### Upload Components
- **UploadDialog**: Upload progress with support for regular and chunked uploads
- **DropZone**: Drag & drop area for file upload
- **DragOverlay**: Visual feedback during drag operations

#### Utilities
- **KeyboardShortcuts**: Keyboard shortcut handler (Ctrl+A, Ctrl+C, etc.)

### Navigation Destinations

```kotlin
enum class NavDestination {
    // Core navigation
    FILES, RECENT, STARRED, SHARED, SHARED_WITH_ME, TRASH,
    // User settings
    SETTINGS, PROFILE,
    // Administration
    ADMIN, ACTIVITY, PLUGINS,
    // Advanced features (Phase 6)
    AI, SYNC, FEDERATION, COLLABORATION, VERSION_HISTORY
}
```

### State Management (AppViewModel)

```kotlin
// Core state
var currentItems: List<StorageItem>      // Current folder contents
var breadcrumbs: List<Breadcrumb>        // Navigation path
var isLoading: Boolean                   // Loading indicator
var error: String?                       // Error message

// Selection state
var selectedItems: Set<String>           // Selected item IDs
var isSelectionMode: Boolean             // Selection mode active
var showInfoPanel: Boolean               // Info panel visibility
var selectedInfoItem: StorageItem?       // Item shown in info panel

// Admin state
var adminUsers: List<AdminUser>          // Users list (admin only)
var isAdminLoading: Boolean              // Admin loading state

// AI state (Phase 6)
var aiProviders: List<AIProviderInfo>    // Available AI providers
var aiModels: List<AIModel>              // Available models
var isAILoading: Boolean                 // AI operations loading

// Sync state (Phase 6)
var syncDevices: List<SyncDevice>        // Registered devices
var syncConflicts: List<SyncConflict>    // Pending conflicts
var isSyncLoading: Boolean               // Sync operations loading

// Federation state (Phase 6)
var federatedInstances: List<FederatedInstance>  // Connected instances
var federatedShares: List<FederatedShare>        // Federated shares
var isFederationLoading: Boolean                 // Federation operations loading

// Collaboration state (Phase 6)
var activeCollaborationSession: CollaborationSession?  // Current session
var documentComments: List<DocumentComment>            // Document comments
var isCollaborationLoading: Boolean                    // Collaboration loading

// Version history state (Phase 6)
var versionHistory: FileVersionHistory?  // File version history
var versionDiff: VersionDiff?            // Version comparison
var isVersionLoading: Boolean            // Version operations loading
```

### Platform-Specific Implementations

| Feature | WASM (Browser) | Desktop (JVM) |
|---------|----------------|---------------|
| File Picker | HTML `<input type="file">` | AWT `FileDialog` |
| Large File Reading | `File.slice()` | `RandomAccessFile` |
| Folder Upload | Falls back to multi-file | `JFileChooser` directory mode |
| Token Storage | `localStorage` | Java `Preferences` |
| File Download | Create `<a>` element | Save dialog |

## Deployment

### Docker Compose

```bash
# Development
docker-compose -f docker/docker-compose.kotlin.yml up --build

# Production with MinIO
docker-compose -f docker/docker-compose.kotlin.yml --profile s3 up -d
```

### TrueNAS Scale

Deploy using the Helm chart:

```bash
helm install vaultstadio ./helm/vaultstadio \
  --set backend.persistence.hostPath.enabled=true \
  --set backend.persistence.hostPath.path=/mnt/pool/vaultstadio \
  --set postgresql.auth.password=your_secure_password
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/vaultstadio` |
| `DATABASE_USER` | Database username | `vaultstadio` |
| `DATABASE_PASSWORD` | Database password | `vaultstadio` |
| `VAULTSTADIO_STORAGE_PATH` | Path for file storage | `/data/storage` |
| `VAULTSTADIO_DATA_PATH` | Path for plugin data | `/data/plugins` |
| `JAVA_OPTS` | JVM options | `-Xms256m -Xmx1024m` |

## Roadmap

### Phase 1: Core Functionality ✅
- [x] Storage engine (upload, download, folders)
- [x] User authentication and sessions
- [x] File sharing with expiration and passwords
- [x] Activity logging
- [x] REST API
- [x] Basic web UI

### Phase 2: Plugin System ✅
- [x] Plugin architecture and SDK
- [x] Event-driven communication
- [x] Plugin configuration
- [x] Custom endpoint registration

### Phase 3: Advanced File Operations ✅
- [x] Multi-selection with batch operations
- [x] Batch delete, move, copy, star
- [x] Download as ZIP
- [x] Large file upload (chunked, up to 60GB)
- [x] Folder upload with structure preservation
- [x] Thumbnail generation for images
- [x] File preview (images, video, audio, PDF, text)
- [x] File info panel with properties
- [x] Keyboard shortcuts
- [x] Admin user quota management
- [x] Empty trash

### Phase 4: Plugins ✅
- [x] Image metadata extractor (EXIF, IPTC, XMP, dimensions)
- [x] Video metadata extractor (FFprobe-based)
- [x] Document indexer (PDF, Office via Apache Tika)
- [x] Full-text search (Apache Lucene)
- [x] Metadata API endpoints
- [x] Advanced search with filters

### Phase 5: AI Integration ✅
- [x] AI Provider abstraction layer (multiple backends)
- [x] Local model integration (Ollama, LM Studio)
- [x] OpenRouter integration (Claude, GPT-4, Gemini, etc.)
- [x] Image recognition and tagging
- [x] Content classification
- [x] Auto-description generation
- [x] Text summarization
- [x] AI API endpoints

### Phase 6: Advanced Features ✅
- [x] File versioning with history, restore, and diff
- [x] Sync client protocol with delta sync and conflict resolution
- [x] WebDAV support for network drive mounting
- [x] S3-compatible API for tool integration
- [x] Federation between VaultStadio instances
- [x] Real-time collaboration with OT and presence

---

## Phase 6 Architecture Details

### File Versioning Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                       File Version System                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐     ┌─────────────────┐     ┌──────────────────┐  │
│  │  API Layer  │────▶│ Version Service │────▶│ Version Storage  │  │
│  │  (Routes)   │     │                 │     │  (PostgreSQL +   │  │
│  └─────────────┘     │ - Create        │     │   File System)   │  │
│                      │ - List          │     └──────────────────┘  │
│                      │ - Restore       │                           │
│                      │ - Compare       │     ┌──────────────────┐  │
│                      │ - Cleanup       │────▶│  Delta Storage   │  │
│                      └─────────────────┘     │  (Binary diffs)  │  │
│                                              └──────────────────┘  │
│                                                                     │
│  Version Flow:                                                      │
│  ┌────────┐   ┌────────┐   ┌────────┐   ┌────────┐   ┌────────┐   │
│  │ V1 ────│──▶│ V2 ────│──▶│ V3 ────│──▶│ V4 ────│──▶│ V5     │   │
│  │(Base)  │   │(+Δ1)   │   │(+Δ2)   │   │(Base)  │   │(+Δ4)   │   │
│  └────────┘   └────────┘   └────────┘   └────────┘   └────────┘   │
│                                                                     │
│  Storage Optimization:                                              │
│  - Full snapshot every N versions                                   │
│  - Delta compression for intermediate versions                      │
│  - Retention policy: max versions, max age, min keep                │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Sync Protocol Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Sync Protocol System                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Client                                 Server                      │
│  ┌─────────────┐                       ┌─────────────────────────┐ │
│  │ Sync Client │                       │     Sync Service        │ │
│  │             │                       │                         │ │
│  │ ┌─────────┐ │   1. Register         │ ┌───────────────────┐   │ │
│  │ │ Device  │◀├───────────────────────│▶│ Device Registry   │   │ │
│  │ │ Manager │ │                       │ └───────────────────┘   │ │
│  │ └─────────┘ │                       │                         │ │
│  │             │   2. Pull Changes     │ ┌───────────────────┐   │ │
│  │ ┌─────────┐ │◀───────────────────────│▶│ Change Tracker    │   │ │
│  │ │ Change  │ │   (cursor-based)      │ │ (timestamp-based) │   │ │
│  │ │ Queue   │ │                       │ └───────────────────┘   │ │
│  │ └─────────┘ │                       │                         │ │
│  │             │   3. Push Changes     │ ┌───────────────────┐   │ │
│  │ ┌─────────┐ │───────────────────────│▶│ Conflict Detector │   │ │
│  │ │ Local   │ │                       │ │ & Resolver        │   │ │
│  │ │ State   │ │                       │ └───────────────────┘   │ │
│  │ └─────────┘ │                       │                         │ │
│  │             │   4. Delta Sync       │ ┌───────────────────┐   │ │
│  │ ┌─────────┐ │◀──────────────────────│▶│ Delta Engine      │   │ │
│  │ │ Delta   │ │   (binary patches)    │ │ (rsync-like)      │   │ │
│  │ │ Engine  │ │                       │ └───────────────────┘   │ │
│  │ └─────────┘ │                       │                         │ │
│  └─────────────┘                       └─────────────────────────┘ │
│                                                                     │
│  Conflict Resolution Strategies:                                    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │
│  │ Keep Local  │ │ Keep Remote │ │ Keep Both   │ │   Manual    │   │
│  │             │ │             │ │ (rename)    │ │ Resolution  │   │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Federation Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Federation System                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Instance A (local.vault.io)          Instance B (remote.vault.io) │
│  ┌─────────────────────────┐         ┌─────────────────────────┐   │
│  │                         │         │                         │   │
│  │  ┌───────────────────┐  │  HTTPS  │  ┌───────────────────┐  │   │
│  │  │ Federation Service│◀─│─────────│─▶│ Federation Service│  │   │
│  │  └─────────┬─────────┘  │         │  └─────────┬─────────┘  │   │
│  │            │            │         │            │            │   │
│  │  ┌─────────▼─────────┐  │         │  ┌─────────▼─────────┐  │   │
│  │  │ Instance Registry │  │         │  │ Instance Registry │  │   │
│  │  │ - Status: ACTIVE  │  │         │  │ - Status: ACTIVE  │  │   │
│  │  │ - Trust: VERIFIED │  │         │  │ - Trust: VERIFIED │  │   │
│  │  └───────────────────┘  │         │  └───────────────────┘  │   │
│  │                         │         │                         │   │
│  │  ┌───────────────────┐  │ Shares  │  ┌───────────────────┐  │   │
│  │  │ Outgoing Shares   │──│────────▶│  │ Incoming Shares   │  │   │
│  │  │ (file-1, file-2)  │  │         │  │ (pending/accepted)│  │   │
│  │  └───────────────────┘  │         │  └───────────────────┘  │   │
│  │                         │         │                         │   │
│  │  ┌───────────────────┐  │ Identity│  ┌───────────────────┐  │   │
│  │  │ Local Identities  │◀─│────────▶│  │ Federated Users   │  │   │
│  │  │ user@local.vault  │  │  Link   │  │ user@remote.vault │  │   │
│  │  └───────────────────┘  │         │  └───────────────────┘  │   │
│  │                         │         │                         │   │
│  └─────────────────────────┘         └─────────────────────────┘   │
│                                                                     │
│  Federation Protocol:                                               │
│  1. Discovery: GET /.well-known/vaultstadio                        │
│  2. Key Exchange: Public key verification                          │
│  3. Trust Establishment: Mutual approval                           │
│  4. Share Exchange: Encrypted share notifications                  │
│  5. Activity Stream: Cross-instance activity feed                  │
│                                                                     │
│  Security:                                                          │
│  - All requests signed with instance private key                   │
│  - TLS 1.3 required for federation                                 │
│  - Certificate pinning optional                                    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Real-time Collaboration Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Real-time Collaboration System                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Client A                Server              Client B              │
│  ┌─────────┐           ┌─────────────────┐   ┌─────────┐          │
│  │ Editor  │           │ Session Manager │   │ Editor  │          │
│  │         │  WebSocket│                 │   │         │          │
│  │ ┌─────┐ │◀─────────▶│ ┌─────────────┐ │◀─▶│ ┌─────┐ │          │
│  │ │ OT  │ │           │ │ OT Engine   │ │   │ │ OT  │ │          │
│  │ │     │ │ Operations│ │             │ │   │ │     │ │          │
│  │ └─────┘ │───────────│▶│ Transform   │ │───│▶│     │ │          │
│  │         │           │ │ Apply       │ │   │ └─────┘ │          │
│  └─────────┘           │ │ Broadcast   │ │   └─────────┘          │
│                        │ └─────────────┘ │                         │
│                        │                 │                         │
│                        │ ┌─────────────┐ │                         │
│                        │ │  Presence   │ │                         │
│                        │ │  Tracker    │ │                         │
│                        │ │             │ │                         │
│                        │ │ - Cursors   │ │                         │
│                        │ │ - Selection │ │                         │
│                        │ │ - Status    │ │                         │
│                        │ └─────────────┘ │                         │
│                        │                 │                         │
│                        │ ┌─────────────┐ │                         │
│                        │ │  Comments   │ │                         │
│                        │ │  Manager    │ │                         │
│                        │ │             │ │                         │
│                        │ │ - Threads   │ │                         │
│                        │ │ - Replies   │ │                         │
│                        │ │ - Resolve   │ │                         │
│                        │ └─────────────┘ │                         │
│                        └─────────────────┘                         │
│                                                                     │
│  Operational Transformation:                                        │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                                                             │   │
│  │  User A: Insert "Hello" at position 0                      │   │
│  │  User B: Insert "World" at position 0 (concurrent)         │   │
│  │                                                             │   │
│  │  Without OT: Conflict / Inconsistent state                 │   │
│  │  With OT: B's operation transformed to position 5          │   │
│  │           Result: "HelloWorld" (consistent everywhere)     │   │
│  │                                                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Operation Types:                                                   │
│  - INSERT: Add text at position                                    │
│  - DELETE: Remove text range                                       │
│  - RETAIN: Skip characters                                         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### WebDAV & S3 Protocol Layer

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Protocol Compatibility Layer                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  External Clients           VaultStadio                            │
│                                                                     │
│  ┌─────────────────┐       ┌─────────────────────────────────────┐ │
│  │ macOS Finder    │       │           WebDAV Handler            │ │
│  │ Windows Explorer│──────▶│  /webdav/*                          │ │
│  │ Linux GVFS     │       │                                      │ │
│  │ Cyberduck      │       │  PROPFIND, GET, PUT, MKCOL,         │ │
│  └─────────────────┘       │  COPY, MOVE, DELETE, LOCK, UNLOCK   │ │
│                            └─────────────────────────────────────┘ │
│                                           │                        │
│                                           ▼                        │
│  ┌─────────────────┐       ┌─────────────────────────────────────┐ │
│  │ rclone          │       │           S3 Handler                │ │
│  │ s3cmd           │──────▶│  /s3/*                              │ │
│  │ AWS CLI         │       │                                      │ │
│  │ MinIO Client    │       │  ListBuckets, GetObject, PutObject, │ │
│  │ Backup tools    │       │  DeleteObject, CreateBucket, etc.   │ │
│  └─────────────────┘       └─────────────────────────────────────┘ │
│                                           │                        │
│                                           ▼                        │
│                            ┌─────────────────────────────────────┐ │
│                            │        Core Storage Service         │ │
│                            │                                      │ │
│                            │  Unified file operations:           │ │
│                            │  - read(), write(), delete()        │ │
│                            │  - mkdir(), list(), stat()          │ │
│                            │                                      │ │
│                            └─────────────────────────────────────┘ │
│                                                                     │
│  Authentication:                                                    │
│  - WebDAV: Basic Auth (email:password)                             │
│  - S3: AWS Signature V4 (email as AccessKey, password as Secret)   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Phase 7: Polish & Production (Future)
- [ ] End-to-end encryption
- [ ] Advanced audit logging
- [ ] SAML/OIDC SSO
- [ ] Backup and disaster recovery
- [ ] High availability clustering
- [ ] Advanced analytics dashboard
- [ ] Offline mode
