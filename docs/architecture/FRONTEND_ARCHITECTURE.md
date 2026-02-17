# VaultStadio Frontend Architecture

**Last updated**: 2026-02-16

This document describes the architecture of the VaultStadio frontend built with Compose Multiplatform.

## Architecture Overview (Decompose-based)

The frontend uses **Decompose** for navigation and component lifecycle management, with **per-screen ViewModels** for state management.

```
┌─────────────────────────────────────────────────────────────────┐
│                      App.kt (Entry Point)                        │
│                            │                                     │
│                    ┌───────┴───────┐                             │
│                    │ RootComponent │                             │
│                    └───────┬───────┘                             │
│                            │                                     │
│              ┌─────────────┴─────────────┐                       │
│              │                           │                       │
│     ┌────────┴────────┐       ┌─────────┴────────┐              │
│     │  AuthComponent  │       │  MainComponent   │              │
│     │  (AuthViewModel)│       │  (Sidebar + Stack)│             │
│     └─────────────────┘       └─────────┬────────┘              │
│                                         │                       │
│              ┌──────────────────────────┼──────────────────┐    │
│              │                          │                  │    │
│     ┌────────┴───────┐      ┌───────────┴──────┐    ┌─────┴──┐ │
│     │ FilesComponent │      │ SettingsComponent│    │ Admin  │ │
│     │ (FilesViewModel)│      │ (SettingsViewModel)│  │Component││
│     └────────────────┘      └──────────────────┘    └────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Repositories                             │
│         (AuthRepository, StorageRepository, etc.)               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Shared API Client                             │
│                    (VaultStadioApi)                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                      Backend (Ktor)
```

## Directory Structure

```
compose-frontend/composeApp/src/commonMain/kotlin/com/vaultstadio/app/
├── navigation/                    # Decompose navigation
│   ├── RootComponent.kt          # Auth/Main split
│   ├── RootContent.kt            # Root composable
│   └── MainDestination.kt        # Navigation destinations
├── feature/                       # Feature modules
│   ├── auth/
│   │   ├── AuthComponent.kt
│   │   ├── AuthViewModel.kt
│   │   └── AuthContent.kt
│   ├── files/
│   │   ├── FilesComponent.kt
│   │   ├── FilesViewModel.kt
│   │   └── FilesContent.kt
│   ├── main/
│   │   ├── MainComponent.kt
│   │   └── MainContent.kt
│   ├── admin/
│   ├── ai/
│   ├── sync/
│   ├── federation/
│   ├── collaboration/
│   └── ...
├── ui/
│   ├── components/               # Reusable UI components
│   │   ├── MainSidebar.kt
│   │   ├── Breadcrumbs.kt
│   │   ├── EmptyState.kt
│   │   └── ...
│   ├── screens/                  # Legacy screens (being migrated)
│   └── theme/
│       └── Theme.kt
├── i18n/
│   ├── Strings.kt                # String resources
│   └── StringExtensions.kt       # Shorthand properties
└── platform/                      # Platform-specific code
```

## Core Patterns

### 1. Component Pattern (Decompose)

Each feature has a Component that manages lifecycle and creates ViewModels:

```kotlin
// feature/files/FilesComponent.kt
interface FilesComponent {
    val viewModel: FilesViewModel
    val mode: MainComponent.FilesMode
}

class DefaultFilesComponent(
    componentContext: ComponentContext,
    storageRepository: StorageRepository,
    apiBaseUrl: String,
    override val mode: MainComponent.FilesMode
) : FilesComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main + SupervisorJob())

    override val viewModel = FilesViewModel(
        storageRepository = storageRepository,
        scope = scope,
        apiBaseUrl = apiBaseUrl,
        mode = mode
    )
}
```

### 2. ViewModel Pattern (Per-Screen)

Each screen has its own ViewModel with focused responsibilities:

```kotlin
// feature/files/FilesViewModel.kt
class FilesViewModel(
    private val storageRepository: StorageRepository,
    private val scope: CoroutineScope,
    private val apiBaseUrl: String,
    private val mode: MainComponent.FilesMode
) {
    // State
    var items by mutableStateOf<List<StorageItem>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    // Actions
    fun loadItems() {
        scope.launch {
            isLoading = true
            when (val result = storageRepository.getItems(currentFolderId)) {
                is ApiResult.Success -> items = result.data
                is ApiResult.Error -> error = result.message
            }
            isLoading = false
        }
    }
}
```

### 3. Navigation (Decompose ChildStack)

Navigation uses Decompose's ChildStack for type-safe navigation:

```kotlin
// navigation/RootComponent.kt
interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        data class Auth(val component: AuthComponent) : Child()
        data class Main(val component: MainComponent) : Child()
    }
}

// Usage in RootContent.kt
@Composable
fun RootContent(component: RootComponent) {
    Children(stack = component.stack) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Auth -> AuthContent(instance.component)
            is RootComponent.Child.Main -> MainContent(instance.component)
        }
    }
}
```

### 4. Destinations Enum

Navigation destinations for the main authenticated area:

```kotlin
enum class MainDestination {
    // Core file management
    FILES, RECENT, STARRED, TRASH,
    // Sharing
    SHARED, SHARED_WITH_ME,
    // User
    SETTINGS, PROFILE,
    // Admin
    ADMIN, ACTIVITY, PLUGINS,
    // Advanced features
    AI, SYNC, FEDERATION, COLLABORATION, VERSION_HISTORY
}
```

Navigation is handled by MainComponent:

```kotlin
// In MainComponent
fun navigateTo(destination: MainDestination) {
    val config = when (destination) {
        MainDestination.FILES -> Config.Files(FilesMode.ALL)
        MainDestination.SETTINGS -> Config.Settings
        // ...
    }
    navigation.push(config)
    NavDestination.AI -> AIScreen(...)
    // ...
}
```

### 3. Component Structure

Components follow a consistent pattern:

```kotlin
@Composable
fun MyComponent(
    // Data (state)
    data: DataType,
    isLoading: Boolean,
    
    // Event handlers (callbacks)
    onAction: () -> Unit,
    onItemClick: (Item) -> Unit,
    
    // Optional modifier (always last)
    modifier: Modifier = Modifier
) {
    // Component implementation
}
```

### 4. Platform Abstraction (expect/actual)

Platform-specific code uses Kotlin's expect/actual mechanism:

```kotlin
// commonMain - Declaration
expect class LargeSelectedFile {
    val name: String
    val size: Long
    val mimeType: String
    suspend fun readChunk(start: Long, end: Long): ByteArray
}

expect suspend fun openFilePicker(multiple: Boolean): List<SelectedFile>
expect fun openDownloadUrl(url: String)
expect fun initializeDragDrop(onFilesDropped: (List<SelectedFile>) -> Unit)
```

```kotlin
// desktopMain - JVM Implementation
actual class LargeSelectedFile(private val file: File) {
    actual val name: String = file.name
    actual val size: Long = file.length()
    actual val mimeType: String = // detect from extension
    
    actual suspend fun readChunk(start: Long, end: Long): ByteArray {
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(start)
            val buffer = ByteArray((end - start).toInt())
            raf.readFully(buffer)
            return buffer
        }
    }
}
```

```kotlin
// wasmJsMain - Web Implementation
actual class LargeSelectedFile(private val jsFile: File) {
    actual val name: String = jsFile.name
    actual val size: Long = jsFile.size.toLong()
    
    actual suspend fun readChunk(start: Long, end: Long): ByteArray {
        val blob = jsFile.slice(start.toInt(), end.toInt())
        // Convert blob to ByteArray using JavaScript APIs
    }
}
```

## Data Flow

```
User Action → Composable → ViewModel → API Client → Backend
     ↑                         │
     └─────────────────────────┘
           State Update
```

### Example: Loading Files

1. User navigates to FILES
2. `MainContent` shows `FilesContent` (via Decompose child stack)
3. `FilesContent` calls `viewModel.navigateToFolder(null)` on the screen's `FilesViewModel`
4. `FilesViewModel` uses `storageRepository.getItems()` (API client)
5. API returns data
6. ViewModel updates `items` state
7. Compose recomposes the screen with new data

## Screen Architecture

### Screen Types

1. **List Screens** (FilesScreen, StarredScreen, TrashScreen)
   - Display items in grid or list view
   - Support selection and batch operations

2. **Form Screens** (LoginScreen, ProfileScreen, SettingsScreen)
   - User input and configuration
   - Validation and submission

3. **Feature Screens** (AIScreen, SyncScreen, FederationScreen)
   - Complex multi-section layouts
   - Real-time updates

4. **Detail Screens** (VersionHistoryScreen, CollaborationScreen)
   - Item-specific views
   - Context-dependent actions

### Screen Structure

```kotlin
@Composable
fun FeatureScreen(
    // State
    data: FeatureData,
    isLoading: Boolean,
    
    // Lifecycle
    onLoad: () -> Unit,
    
    // Actions
    onAction1: () -> Unit,
    onAction2: (String) -> Unit,
    
    modifier: Modifier = Modifier
) {
    // Load data on first composition
    LaunchedEffect(Unit) {
        onLoad()
    }
    
    Scaffold(
        topBar = { /* ... */ },
        floatingActionButton = { /* ... */ }
    ) { padding ->
        when {
            isLoading -> LoadingIndicator()
            data.isEmpty -> EmptyState()
            else -> ContentList(data)
        }
    }
}
```

## Theming

Material 3 theming with light/dark mode support:

```kotlin
@Composable
fun VaultStadioTheme(
    darkTheme: Boolean = ThemeSettings.isDarkMode,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

## Internationalization

Strings are provided via `CompositionLocal`:

```kotlin
val LocalStrings = staticCompositionLocalOf<StringResources> { EnglishStrings }

@Composable
fun App() {
    CompositionLocalProvider(
        LocalStrings provides Strings.getStrings(Strings.currentLanguage)
    ) {
        // App content
    }
}

// Usage
@Composable
fun MyComponent() {
    val strings = LocalStrings.current
    Text(strings.myString)
}
```

## Error Handling

Errors are handled at the ViewModel level (each screen has its own ViewModel):

```kotlin
class FilesViewModel(...) {
    var error by mutableStateOf<String?>(null)
    
    fun loadItems() {
        scope.launch {
            val result = storageRepository.getItems(folderId)
            when (result) {
                is ApiResult.Success -> { /* update items */ }
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }
    
    fun clearError() {
        error = null
    }
}
```

Errors are displayed via Snackbar or dialog in the root or screen composable.

## Performance Considerations

### 1. State Hoisting
Keep state in ViewModel, not in components.

### 2. Remember
Use `remember` for expensive calculations:

```kotlin
val filteredItems = remember(items, filter) {
    items.filter { it.matches(filter) }
}
```

### 3. Lazy Layouts
Use `LazyColumn`/`LazyGrid` for lists:

```kotlin
LazyVerticalGrid(columns = GridCells.Adaptive(180.dp)) {
    items(items) { item ->
        FileItem(item)
    }
}
```

### 4. Image Loading
Use thumbnails and progressive loading for images.

## Testing Strategy

1. **Unit Tests**: Test ViewModel logic and data transformations
2. **Component Tests**: Test component behavior with mocked data
3. **Integration Tests**: Test full flows with API mocking

## Module Dependencies

```
shared (domain models, API client)
    ↑
composeApp (UI, ViewModel)
    ↑
androidApp / iosApp / desktopMain / wasmJsMain
```
