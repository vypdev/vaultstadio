# VaultStadio Frontend Features

## Overview

The VaultStadio frontend is built with **Kotlin Multiplatform** and **Compose Multiplatform**, supporting:
- **Web (WASM)**: Browser-based interface
- **Desktop**: Native desktop application (Windows, macOS, Linux)

---

## Multi-Selection

### Components

- **`SelectableFileGridItem`**: Grid view item with selection checkbox
- **`SelectableFileListItem`**: List view item with selection checkbox
- **`SelectionToolbar`**: Floating toolbar with batch actions

### Usage

```kotlin
SelectableFileGridItem(
    item = storageItem,
    isSelected = viewModel.selectedItems.contains(item.id),
    isSelectionMode = viewModel.isSelectionMode,
    onClick = { viewModel.toggleItemSelection(item.id) },
    onLongClick = { viewModel.toggleItemSelection(item.id) },
    onStarClick = { viewModel.toggleStar(item.id) },
    onMenuClick = { /* show context menu */ }
)
```

### ViewModel Methods

| Method | Description |
|--------|-------------|
| `toggleItemSelection(itemId)` | Toggle selection of an item |
| `selectAll()` | Select all items in current view |
| `clearSelection()` | Clear all selections |
| `batchDeleteSelected(permanent)` | Delete all selected items |
| `batchMoveSelected(destinationId)` | Move selected items |
| `batchCopySelected(destinationId)` | Copy selected items |
| `batchStarSelected()` | Star all selected items |

---

## File Info Panel

Side panel showing detailed information about a selected file.

### Component

```kotlin
FileInfoPanel(
    item = selectedItem,
    onClose = { viewModel.hideItemInfo() },
    onRename = { /* rename */ },
    onMove = { /* move */ },
    onCopy = { /* copy */ },
    onShare = { /* share */ },
    onDownload = { /* download */ },
    onStar = { /* star */ },
    onDelete = { /* delete */ }
)
```

### Information Displayed

- File icon and name
- Type (file/folder)
- Size
- MIME type
- Created date
- Modified date
- Path
- Visibility

### Quick Actions

- Star/Unstar
- Download
- Share
- Rename
- Move
- Copy
- Delete

---

## Keyboard Shortcuts

### Component

```kotlin
Box(
    modifier = Modifier.keyboardShortcuts(
        KeyboardShortcutActions(
            onSelectAll = { viewModel.selectAll() },
            onDelete = { viewModel.batchDeleteSelected() },
            onCopy = { /* copy */ },
            onPaste = { /* paste */ },
            onCut = { /* cut */ },
            onRename = { /* rename */ },
            onNewFolder = { /* new folder */ },
            onRefresh = { viewModel.refresh() },
            onEscape = { viewModel.clearSelection() },
            onUpload = { /* upload */ }
        )
    )
)
```

### Supported Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl/⌘ + A` | Select all |
| `Ctrl/⌘ + C` | Copy selected |
| `Ctrl/⌘ + V` | Paste |
| `Ctrl/⌘ + X` | Cut selected |
| `Ctrl/⌘ + N` | New folder |
| `Ctrl/⌘ + U` | Upload files |
| `Ctrl/⌘ + R` / `F5` | Refresh |
| `F2` | Rename |
| `Delete` / `Backspace` | Move to trash |
| `Escape` | Clear selection |

---

## File Preview

Full-screen preview dialog for files.

### Supported Types

| Type | Preview |
|------|---------|
| Images | Visual preview with zoom |
| Videos | Video player placeholder |
| Audio | Audio player placeholder |
| Text/JSON/XML | Text content display |
| PDF | PDF document placeholder |
| Other | Generic file info |

### Component

```kotlin
FilePreviewDialog(
    isOpen = showPreview,
    item = previewItem,
    onDismiss = { showPreview = false },
    onDownload = { /* download */ },
    onShare = { /* share */ },
    onDelete = { /* delete */ }
)
```

---

## Large File Upload

Supports files up to 60GB using chunked upload.

### Threshold

- Files < 100MB: Loaded in memory, uploaded in single request
- Files >= 100MB: Streamed in chunks of 10MB

### Platform-Specific Implementation

**WASM (Browser)**:
- Uses `File.slice()` for chunking
- `LargeSelectedFile` wraps browser File object

**Desktop**:
- Uses `RandomAccessFile` for chunking
- `LargeSelectedFile` wraps Java File object

### Usage

```kotlin
// Open large file picker
val largeFiles = openLargeFilePicker()

// Upload with progress
viewModel.uploadLargeFile(
    file = largeFiles.first(),
    onProgress = { progress -> /* update UI */ },
    onComplete = { success -> /* handle result */ }
)
```

---

## Folder Upload

Upload entire directories with structure preserved.

### Platform Support

| Platform | Support |
|----------|---------|
| Desktop | Full folder selection with JFileChooser |
| WASM | Falls back to multi-file selection |

### Usage

```kotlin
val folderFiles = openFolderPicker()

viewModel.uploadFolder(
    files = folderFiles,
    parentId = currentFolderId,
    onProgress = { progress -> /* update UI */ }
)
```

### Data Model

```kotlin
data class FolderFile(
    val name: String,
    val relativePath: String,  // e.g., "docs/images/photo.jpg"
    val size: Long,
    val mimeType: String,
    val data: ByteArray
)
```

---

## State Management

### Selection State

```kotlin
// In AppViewModel
var selectedItems by mutableStateOf<Set<String>>(emptySet())
var isSelectionMode by mutableStateOf(false)
var showInfoPanel by mutableStateOf(false)
var selectedInfoItem by mutableStateOf<StorageItem?>(null)
```

### Batch Operations State

```kotlin
// Loading state during batch operations
var isLoading by mutableStateOf(false)
var error by mutableStateOf<String?>(null)
```

---

## Component Architecture

```
compose-frontend/
├── composeApp/
│   └── src/
│       ├── commonMain/
│       │   └── kotlin/com/vaultstadio/app/
│       │       ├── ui/
│       │       │   ├── components/
│       │       │   │   ├── SelectableFileItem.kt    # Multi-selection items
│       │       │   │   ├── FileInfoPanel.kt         # Info sidebar
│       │       │   │   ├── KeyboardShortcuts.kt     # Keyboard handling
│       │       │   │   ├── FilePreviewDialog.kt     # Preview dialog
│       │       │   │   └── ...
│       │       │   └── screens/
│       │       │       ├── FilesScreen.kt
│       │       │       └── ...
│       │       ├── platform/
│       │       │   ├── FilePicker.kt                # expect declarations
│       │       │   └── ...
│       │       └── viewmodel/
│       │           └── AppViewModel.kt              # State management
│       ├── wasmJsMain/
│       │   └── kotlin/com/vaultstadio/app/platform/
│       │       └── FilePicker.wasmJs.kt             # WASM implementation
│       └── desktopMain/
│           └── kotlin/com/vaultstadio/app/platform/
│               └── FilePicker.desktop.kt            # Desktop implementation
```

---

## Best Practices

### Selection Handling

1. Always check `isSelectionMode` before normal click actions
2. Use long-press/right-click to enter selection mode
3. Clear selection when navigating away

### Large File Handling

1. Check file size before loading into memory
2. Use `LargeSelectedFile` for files > 100MB
3. Provide progress feedback during upload
4. Handle upload cancellation gracefully

### Keyboard Shortcuts

1. Bind shortcuts at the root level
2. Prevent default browser behavior for Ctrl+S, etc.
3. Show shortcut hints in tooltips

---

## Phase 6 Features

### AI Integration

**AIScreen** - Full AI assistant interface for interacting with local or cloud AI providers.

#### Modes

| Mode | Description |
|------|-------------|
| Chat | Interactive conversation with AI |
| Describe | Generate descriptions for images |
| Tag | Auto-generate tags for images |
| Classify | Classify content into categories |
| Summarize | Summarize text documents |

#### Components

- `AIScreen.kt` - Main AI interface with chat and mode selection
- Provider configuration (Admin only)
- Model selection dropdown
- Chat history with message bubbles

#### Usage

```kotlin
AIScreen(
    providers = viewModel.aiProviders,
    models = viewModel.aiModels,
    activeProvider = viewModel.activeProvider,
    isAdmin = viewModel.isAdmin(),
    onChat = { messages, model, callback -> 
        viewModel.aiChat(messages, model, callback) 
    },
    onDescribe = { imageBase64, mimeType, callback ->
        viewModel.aiDescribeImage(imageBase64, mimeType, callback)
    },
    // ... other callbacks
)
```

---

### Version History

**VersionHistoryScreen** - View, compare, restore, and manage file versions.

#### Features

- Timeline view of all versions
- Diff comparison between versions
- Restore previous versions
- Download specific versions
- Cleanup old versions

#### Components

- `VersionHistoryScreen.kt` - Full version history view
- `VersionPanel.kt` - Inline panel for FileInfoPanel
- Version cards with restore/download actions
- Diff viewer dialog

#### Usage

```kotlin
VersionHistoryScreen(
    itemId = selectedItem.id,
    itemName = selectedItem.name,
    versionHistory = viewModel.versionHistory,
    versionDiff = viewModel.versionDiff,
    onLoadHistory = { viewModel.loadVersionHistory(it) },
    onRestoreVersion = { itemId, versionNum, comment ->
        viewModel.restoreVersion(itemId, versionNum, comment)
    },
    onCompareVersions = { itemId, from, to ->
        viewModel.compareVersions(itemId, from, to)
    },
    onBack = { /* navigate back */ }
)
```

---

### Sync & Devices

**SyncScreen** - Manage synchronized devices and resolve conflicts.

#### Tabs

| Tab | Content |
|-----|---------|
| Devices | List of connected sync devices |
| Conflicts | Pending sync conflicts to resolve |

#### Device Operations

- View device info and last sync time
- Deactivate devices (stop syncing)
- Remove devices permanently

#### Conflict Resolution

| Resolution | Action |
|------------|--------|
| Keep Local | Use local version |
| Keep Remote | Use server version |
| Keep Both | Rename and keep both |
| Merge | Attempt automatic merge |
| Manual | Resolve manually |

#### Usage

```kotlin
SyncScreen(
    devices = viewModel.syncDevices,
    conflicts = viewModel.syncConflicts,
    onDeactivateDevice = { viewModel.deactivateSyncDevice(it) },
    onRemoveDevice = { viewModel.removeSyncDevice(it) },
    onResolveConflict = { conflictId, resolution ->
        viewModel.resolveSyncConflict(conflictId, resolution)
    }
)
```

---

### Real-Time Collaboration

**CollaborationScreen** - Edit documents collaboratively in real-time.

#### Features

- Live document editing with OT
- Remote cursor visualization
- Presence indicators
- Comment threads with replies
- Auto-save

#### Components

- `CollaborationScreen.kt` - Full collaboration editor
- Participants avatars
- Comments sidebar
- Version indicator

#### Usage

```kotlin
CollaborationScreen(
    itemId = selectedItem.id,
    itemName = selectedItem.name,
    session = viewModel.collaborationSession,
    documentState = viewModel.documentState,
    comments = viewModel.documentComments,
    onJoinSession = { viewModel.joinCollaborationSession(it) },
    onLeaveSession = { viewModel.leaveCollaborationSession() },
    onSaveDocument = { viewModel.saveDocument() },
    onAddComment = { content, startLine, startCol, endLine, endCol, quotedText ->
        viewModel.addComment(content, startLine, startCol, endLine, endCol, quotedText)
    }
)
```

---

### Federation

**FederationScreen** - Connect and share with other VaultStadio instances.

#### Tabs

| Tab | Content |
|-----|---------|
| Instances | Federated VaultStadio instances |
| Shares | Incoming and outgoing federated shares |
| Identities | Linked remote identities |

#### Instance Operations

- Request federation with new instances
- Block/unblock instances
- Remove federated instances

#### Share Operations

- Accept/decline incoming shares
- Revoke outgoing shares
- View share permissions

#### Usage

```kotlin
FederationScreen(
    instances = viewModel.federatedInstances,
    outgoingShares = viewModel.outgoingFederatedShares,
    incomingShares = viewModel.incomingFederatedShares,
    identities = viewModel.federatedIdentities,
    onRequestFederation = { domain, message ->
        viewModel.requestFederation(domain, message)
    },
    onAcceptShare = { viewModel.acceptFederatedShare(it) },
    onDeclineShare = { viewModel.declineFederatedShare(it) },
    onRevokeShare = { viewModel.revokeFederatedShare(it) }
)
```

---

### Activity Log

**ActivityScreen** - View all user activity with filtering.

#### Filters

| Filter | Activities |
|--------|-----------|
| All | All activities |
| Files | Upload, download, create, delete, move, rename, copy |
| Sharing | Share, unshare, share access |
| Auth | Login, logout, register, password change |

#### Components

- `ActivityScreen.kt` - Activity timeline
- Activity cards with icons and details
- Filter chips

---

### Advanced Search

**AdvancedSearchDialog** - Search with filters for file type, size, date, and content.

#### Filters

| Filter | Options |
|--------|---------|
| File Type | Images, Videos, Audio, Documents, Spreadsheets, Presentations, Archives, Code |
| Date Range | Any, Today, Past week, Past month, Past 3 months, Past year |
| Size Range | Any, < 100 KB, 100 KB - 1 MB, 1 - 10 MB, 10 - 100 MB, > 100 MB |
| Content | Full-text search within file content |

#### Usage

```kotlin
AdvancedSearchDialog(
    initialQuery = currentSearchQuery,
    onSearch = { request ->
        viewModel.advancedSearch(request)
    },
    onDismiss = { showAdvancedSearch = false }
)
```

---

### Metadata Display

**MetadataPanel** - Display extracted metadata for images, videos, and documents.

#### Image Metadata

- Resolution, color space, bit depth
- Camera: make, model, aperture, exposure, ISO, focal length
- Location: GPS coordinates, altitude
- IPTC/XMP: keywords, description, artist, copyright

#### Video Metadata

- Resolution, duration, frame rate, bitrate
- Codecs: video, audio
- HDR support
- Audio: channels, sample rate, languages
- Subtitles

#### Document Metadata

- Title, author, subject
- Page count, word count
- Creation/modification dates
- Full-text indexing status

#### Usage

```kotlin
MetadataPanel(
    imageMetadata = viewModel.imageMetadata,
    videoMetadata = viewModel.videoMetadata,
    documentMetadata = viewModel.documentMetadata
)
```

---

## Internationalization (i18n)

### Supported Languages

| Language | Code | Status |
|----------|------|--------|
| English | en | ✅ Complete |
| Spanish | es | ✅ Complete |
| French | fr | ✅ Complete |
| German | de | ✅ Complete |
| Portuguese | pt | ✅ Complete |
| Chinese (Simplified) | zh | ✅ Complete |
| Japanese | ja | ✅ Complete |

### Usage

```kotlin
// Access strings
val strings = strings()
Text(strings.navMyFiles)

// Change language
Strings.currentLanguage = Language.SPANISH
```

---

## API Client Extensions

All Phase 6 backend endpoints are now available in `VaultStadioApi`:

### AI Endpoints

```kotlin
api.getAIProviders()
api.configureAIProvider(request)
api.setActiveAIProvider(type)
api.getAIModels()
api.aiChat(request)
api.aiVision(request)
api.aiDescribeImage(imageBase64, mimeType)
api.aiTagImage(imageBase64, mimeType)
api.aiClassify(content, categories)
api.aiSummarize(text, maxLength)
```

### Version Endpoints

```kotlin
api.getVersionHistory(itemId)
api.getVersion(itemId, versionNumber)
api.getVersionDownloadUrl(itemId, versionNumber)
api.restoreVersion(itemId, versionNumber, comment)
api.compareVersions(itemId, from, to)
api.deleteVersion(versionId)
api.cleanupVersions(itemId, maxVersions, maxAgeDays, minVersionsToKeep)
```

### Sync Endpoints

```kotlin
api.registerSyncDevice(deviceId, deviceName, deviceType)
api.getSyncDevices(activeOnly)
api.deactivateSyncDevice(deviceId)
api.removeSyncDevice(deviceId)
api.pullSyncChanges(deviceId, cursor, limit, includeDeleted)
api.getSyncConflicts()
api.resolveSyncConflict(conflictId, resolution)
```

### Collaboration Endpoints

```kotlin
api.joinCollaborationSession(itemId)
api.leaveCollaborationSession(sessionId, participantId)
api.getCollaborationSession(sessionId)
api.getDocumentState(itemId)
api.saveDocument(itemId)
api.getDocumentComments(itemId, includeResolved)
api.createDocumentComment(...)
api.resolveComment(itemId, commentId)
api.updatePresence(status, activeDocument)
```

### Federation Endpoints

```kotlin
api.requestFederation(targetDomain, message)
api.getFederatedInstances(status)
api.blockFederatedInstance(instanceId)
api.removeFederatedInstance(instanceId)
api.createFederatedShare(request)
api.getOutgoingFederatedShares()
api.getIncomingFederatedShares(status)
api.acceptFederatedShare(shareId)
api.declineFederatedShare(shareId)
api.revokeFederatedShare(shareId)
api.linkFederatedIdentity(request)
api.getFederatedIdentities()
api.unlinkFederatedIdentity(identityId)
```

### Advanced Search Endpoints

```kotlin
api.advancedSearch(request)
api.searchByMetadata(key, value, pluginId, limit)
api.getSearchSuggestions(prefix, limit)
```

### Metadata Endpoints

```kotlin
api.getFileMetadata(itemId)
api.getImageMetadata(itemId)
api.getVideoMetadata(itemId)
api.getDocumentMetadata(itemId)
api.getThumbnailUrl(itemId, size)
api.getPreviewUrl(itemId)
```

---

## Testing

### New Test Files

| File | Coverage |
|------|----------|
| `Phase6ScreensTest.kt` | AI, Version, Sync, Collaboration, Federation, Activity screens |
| `Phase6ComponentsTest.kt` | AdvancedSearchDialog, MetadataPanel, VersionPanel |
| `ApiClientExtensionsTest.kt` | All new API request/response models |

### Running Tests

```bash
./gradlew :compose-frontend:composeApp:commonTest
```

---

## Future Enhancements

- [x] AI Integration
- [x] File Versioning
- [x] Sync & Devices
- [x] Real-time Collaboration
- [x] Federation
- [x] Advanced Search
- [x] Metadata Display
- [x] Full i18n (7 languages)
- [ ] End-to-end encryption
- [ ] SAML/OIDC SSO
- [ ] Mobile apps (Android, iOS)
- [ ] Offline mode
- [ ] WebSocket for real-time updates
