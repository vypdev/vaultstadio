# VaultStadio Frontend Components

This document catalogs all reusable UI components in the VaultStadio frontend.

## Component Structure

All components are located in:
```
compose-frontend/composeApp/src/commonMain/kotlin/com/vaultstadio/app/ui/components/
```

## Navigation Components

### Sidebar

Main navigation sidebar with storage quota display.

```kotlin
@Composable
fun Sidebar(
    currentDestination: NavDestination,
    user: User?,
    quota: StorageQuota?,
    onNavigate: (NavDestination) -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Props:**
| Prop | Type | Description |
|------|------|-------------|
| `currentDestination` | `NavDestination` | Currently active navigation destination |
| `user` | `User?` | Current logged-in user |
| `quota` | `StorageQuota?` | User's storage quota information |
| `onNavigate` | `(NavDestination) -> Unit` | Callback when navigation item is clicked |
| `onUploadClick` | `() -> Unit` | Callback when upload button is clicked |

### Breadcrumbs

Navigation path display.

```kotlin
@Composable
fun Breadcrumbs(
    items: List<Breadcrumb>,
    onItemClick: (Breadcrumb) -> Unit,
    modifier: Modifier = Modifier
)
```

**Props:**
| Prop | Type | Description |
|------|------|-------------|
| `items` | `List<Breadcrumb>` | List of breadcrumb items in the path |
| `onItemClick` | `(Breadcrumb) -> Unit` | Callback when a breadcrumb is clicked |

## File Display Components

### FileItem

Basic file/folder display item for grid view.

```kotlin
@Composable
fun FileItem(
    item: StorageItem,
    onItemClick: () -> Unit,
    onStarClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

### SelectableFileItem

File item with selection checkbox for multi-selection.

```kotlin
@Composable
fun SelectableFileItem(
    item: StorageItem,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onSelectClick: () -> Unit,
    onStarClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

### FileInfoPanel

Sidebar panel showing detailed file properties.

```kotlin
@Composable
fun FileInfoPanel(
    item: StorageItem,
    onClose: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
)
```

### FilePreviewDialog

Full-screen file preview dialog.

```kotlin
@Composable
fun FilePreviewDialog(
    item: StorageItem,
    previewUrl: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
)
```

**Supported File Types:**
- Images (JPEG, PNG, GIF, WebP, SVG)
- Videos (MP4, WebM, MOV)
- Audio (MP3, WAV, OGG, FLAC)
- Documents (PDF)
- Text files (TXT, MD, JSON, etc.)

## Action Components

### ContextMenu

Right-click context menu for file actions.

```kotlin
@Composable
fun ContextMenu(
    item: StorageItem,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onStar: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
)
```

### UploadDialog

Upload progress dialog with support for multiple files.

```kotlin
@Composable
fun UploadDialog(
    items: List<UploadItem>,
    onDismiss: () -> Unit,
    onCancelUpload: (UploadItem) -> Unit,
    modifier: Modifier = Modifier
)
```

**UploadItem Data Class:**
```kotlin
data class UploadItem(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val progress: Float,
    val status: UploadStatus
)

enum class UploadStatus {
    PENDING, UPLOADING, COMPLETED, FAILED, CANCELLED
}
```

### MoveDialog

Dialog for selecting destination folder.

```kotlin
@Composable
fun MoveDialog(
    folders: List<StorageItem>,
    onDismiss: () -> Unit,
    onFolderSelect: (String?) -> Unit, // null for root
    modifier: Modifier = Modifier
)
```

### ShareDialog

Dialog for creating and managing share links.

```kotlin
@Composable
fun ShareDialog(
    item: StorageItem,
    onDismiss: () -> Unit,
    onCreateShare: (password: String?, expirationDays: Int?, maxDownloads: Int?) -> Unit,
    modifier: Modifier = Modifier
)
```

### RenameDialog

Dialog for renaming files and folders.

```kotlin
@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit,
    modifier: Modifier = Modifier
)
```

### CreateFolderDialog

Dialog for creating new folders.

```kotlin
@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String) -> Unit,
    modifier: Modifier = Modifier
)
```

## Drag & Drop Components

### DropZone

Drag-and-drop area for file upload.

```kotlin
@Composable
fun DropZone(
    onFilesDropped: (List<SelectedFile>) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
```

### DragOverlay

Visual overlay shown during drag operations.

```kotlin
@Composable
fun DragOverlay(
    modifier: Modifier = Modifier
)
```

## Utility Components

### KeyboardShortcuts

Keyboard shortcut handler modifier.

```kotlin
fun Modifier.keyboardShortcuts(
    actions: KeyboardActions
): Modifier
```

**Supported Shortcuts:**
| Shortcut | Action |
|----------|--------|
| `Ctrl+A` | Select all |
| `Ctrl+C` | Copy |
| `Ctrl+V` | Paste |
| `Ctrl+X` | Cut |
| `Delete` | Delete |
| `F2` | Rename |
| `Enter` | Open |
| `Escape` | Clear selection |

### EmptyState

Placeholder for empty views.

```kotlin
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

### LoadingIndicator

Loading spinner with optional message.

```kotlin
@Composable
fun LoadingIndicator(
    message: String? = null,
    modifier: Modifier = Modifier
)
```

### ErrorMessage

Error display with retry option.

```kotlin
@Composable
fun ErrorMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

## Theme Components

### VaultStadioTheme

Main theme wrapper with Material 3 styling.

```kotlin
@Composable
fun VaultStadioTheme(
    darkTheme: Boolean = ThemeSettings.isDarkMode,
    content: @Composable () -> Unit
)
```

### Theme Colors

Primary colors defined in theme:

| Token | Light | Dark |
|-------|-------|------|
| Primary | Blue 600 | Blue 400 |
| Secondary | Teal 600 | Teal 400 |
| Error | Red 600 | Red 400 |
| Background | White | Gray 900 |
| Surface | Gray 50 | Gray 800 |

## Best Practices

### 1. Always Use Modifier Parameter

```kotlin
@Composable
fun MyComponent(
    // ... other props
    modifier: Modifier = Modifier  // Always last
)
```

### 2. Hoist State

Keep state in ViewModel, not in components:

```kotlin
// Good
@Composable
fun FileList(
    items: List<StorageItem>,  // State from ViewModel
    onItemClick: (StorageItem) -> Unit  // Event up to ViewModel
)

// Avoid
@Composable
fun FileList() {
    var items by remember { mutableStateOf<List<StorageItem>>(emptyList()) }
    // Loading data inside component
}
```

### 3. Use Callbacks for Events

```kotlin
@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,  // Callback, not direct action
    modifier: Modifier = Modifier
)
```

### 4. Preview with Sample Data

```kotlin
@Preview
@Composable
fun FileItemPreview() {
    VaultStadioTheme {
        FileItem(
            item = sampleStorageItem(),
            onItemClick = {},
            onStarClick = {},
            onMenuClick = {}
        )
    }
}
```
