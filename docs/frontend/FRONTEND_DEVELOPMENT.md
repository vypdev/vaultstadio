# VaultStadio Frontend Development Guide

This guide covers everything you need to develop, build, and run the VaultStadio frontend across all supported platforms.

## Overview

VaultStadio uses **Compose Multiplatform** to deliver a native experience on:
- **Web** (WebAssembly)
- **Desktop** (Windows, macOS, Linux)
- **Android**
- **iOS**

## Prerequisites

- **JDK 17+** (recommended: Azul Zulu or Adoptium)
- **Kotlin 2.0+**
- **Gradle 8.x**
- **Android Studio** (for Android development)
- **Xcode 15+** (for iOS development, macOS only)

## Project Structure

```
compose-frontend/
├── composeApp/              # Shared Compose UI
│   └── src/
│       ├── commonMain/      # Shared code (all platforms)
│       │   └── kotlin/com/vaultstadio/app/
│       │       ├── App.kt           # Root composable
│       │       ├── i18n/            # Internationalization
│       │       ├── platform/        # expect declarations
│       │       ├── ui/
│       │       │   ├── components/  # Reusable UI components
│       │       │   ├── screens/     # Full-page screens
│       │       │   └── theme/       # Material 3 theming
│       │       └── viewmodel/       # State management
│       ├── commonTest/      # Shared tests
│       ├── desktopMain/     # Desktop actual implementations
│       ├── wasmJsMain/      # Web (WASM) actual implementations
│       ├── androidMain/     # Android actual implementations
│       └── iosMain/         # iOS actual implementations
├── androidApp/              # Android app module
├── iosApp/                  # iOS app module
└── build.gradle.kts         # Gradle configuration
```

## Quick Start

### 1. Clone and Setup

```bash
# Clone the repository
git clone https://github.com/your-org/vaultstadio.git
cd vaultstadio

# Build the shared module first
./gradlew :shared:build
```

### 2. Running the Frontend

#### Web (WebAssembly)

```bash
# Development server with hot reload
./gradlew :compose-frontend:composeApp:wasmJsBrowserDevelopmentRun

# Production build
./gradlew :compose-frontend:composeApp:wasmJsBrowserProductionWebpack
```

The web app will be available at `http://localhost:8080`.

#### Desktop (JVM)

```bash
# Run desktop app
./gradlew :compose-frontend:composeApp:run

# Package for distribution
./gradlew :compose-frontend:composeApp:packageDmg       # macOS
./gradlew :compose-frontend:composeApp:packageMsi       # Windows
./gradlew :compose-frontend:composeApp:packageDeb       # Linux (Debian)
./gradlew :compose-frontend:composeApp:packageRpm       # Linux (RHEL)
```

#### Android

```bash
# Install debug APK
./gradlew :compose-frontend:androidApp:installDebug

# Build release APK
./gradlew :compose-frontend:androidApp:assembleRelease
```

Or open in Android Studio and run directly.

#### iOS

```bash
# Build iOS framework
./gradlew :compose-frontend:iosApp:build
```

Then open `compose-frontend/iosApp/iosApp.xcodeproj` in Xcode and run.

## Development Workflow

### 1. Making UI Changes

All shared UI code lives in `composeApp/src/commonMain/kotlin/com/vaultstadio/app/ui/`.

```kotlin
// Example: Creating a new component
@Composable
fun MyComponent(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(title)
    }
}
```

### 2. Adding a New Screen

1. Create the screen in `ui/screens/`:

```kotlin
// ui/screens/MyNewScreen.kt
@Composable
fun MyNewScreen(
    data: MyData,
    isLoading: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Screen") })
        }
    ) { padding ->
        // Content
    }
}
```

2. Add to `NavDestination` in `AppViewModel.kt`:

```kotlin
enum class NavDestination {
    // ... existing
    MY_NEW_SCREEN
}
```

3. Add navigation in `App.kt`:

```kotlin
NavDestination.MY_NEW_SCREEN -> {
    MyNewScreen(
        data = viewModel.myData,
        isLoading = viewModel.isLoading,
        onAction = { viewModel.doAction() }
    )
}
```

4. Add sidebar link in `Sidebar.kt`.

### 3. Platform-Specific Code

Use `expect`/`actual` for platform-specific implementations:

```kotlin
// commonMain - Declaration
expect fun openFilePicker(): List<File>

// desktopMain - Implementation
actual fun openFilePicker(): List<File> {
    val chooser = JFileChooser()
    // ...
}

// wasmJsMain - Implementation
actual fun openFilePicker(): List<File> {
    // Use JavaScript File API
}
```

### 4. State Management

Use the `AppViewModel` for centralized state:

```kotlin
// Add state
var myData by mutableStateOf<MyData?>(null)
    private set

// Add method
fun loadMyData() {
    scope.launch {
        isLoading = true
        val result = api.getMyData()
        if (result.isSuccess()) {
            myData = result.getOrNull()
        }
        isLoading = false
    }
}
```

## Internationalization (i18n)

All strings are defined in `i18n/Strings.kt` with support for 7 languages:
- English (en)
- Spanish (es)
- French (fr)
- German (de)
- Portuguese (pt)
- Chinese (zh)
- Japanese (ja)

### Adding New Strings

1. Add to the interface:

```kotlin
interface StringResources {
    // ...
    val myNewString: String
}
```

2. Add to all language implementations:

```kotlin
object EnglishStrings : StringResources {
    override val myNewString = "My New String"
}

object SpanishStrings : StringResources {
    override val myNewString = "Mi Nueva Cadena"
}
// ... etc for all languages
```

3. Use in composables:

```kotlin
val strings = LocalStrings.current
Text(strings.myNewString)
```

## Testing

### Running Tests

```bash
# All frontend tests
./gradlew :compose-frontend:composeApp:allTests

# Common tests only
./gradlew :compose-frontend:composeApp:commonTest

# Desktop tests
./gradlew :compose-frontend:composeApp:desktopTest
```

### Writing Tests

```kotlin
class MyComponentTest {
    @Test
    fun `should handle empty state`() {
        // Test logic
        assertTrue(condition)
    }
}
```

## Environment Configuration

### API Base URL

Configure in `AppViewModel.kt`:

```kotlin
class AppViewModel(
    baseUrl: String = "http://localhost:8080"  // Default for development
)
```

For production, set via environment or build config.

## Debugging

### Web

Use browser DevTools. Source maps are available in development mode.

### Desktop

Use IntelliJ IDEA debugger. Set breakpoints in Compose code.

### Android

Use Android Studio debugger with Layout Inspector.

### iOS

Use Xcode debugger.

## Performance Tips

1. **Minimize recompositions**: Use `remember` and `derivedStateOf`
2. **Lazy loading**: Use `LazyColumn`/`LazyGrid` for lists
3. **Image optimization**: Use thumbnails for previews
4. **Chunked uploads**: Large files use chunked upload API

### WASM bundle and lazy loading

The Web (WASM) build produces a single `.wasm` and one JS loader. Kotlin/Wasm does not support code-splitting into multiple chunks. We optimize by:

- **Lazy screen creation**: Decompose creates each screen component only when the user navigates to that route, so only the active screen is instantiated at runtime.
- **Incremental compilation**: `kotlin.incremental.wasm=true` in `gradle.properties` speeds up dev builds.

See [WASM_BUNDLE_OPTIMIZATION.md](../development/WASM_BUNDLE_OPTIMIZATION.md) for details and future options.

## Common Issues

### "Unresolved reference" after changes

```bash
./gradlew clean
./gradlew :compose-frontend:composeApp:build
```

### WASM build fails

Ensure you're using Kotlin 2.0+ with WASM support enabled.

### iOS build issues

Ensure Xcode Command Line Tools are installed:
```bash
xcode-select --install
```

## Resources

- [Compose Multiplatform Documentation](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Material 3 Design](https://m3.material.io/)
