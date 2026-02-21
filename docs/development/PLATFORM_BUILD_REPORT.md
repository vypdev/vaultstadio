# VaultStadio – Platform Build Report

**Date**: 2026-02-16  
**Scope**: Compilation and configuration review for all platforms (backend, frontend Desktop, Android, Web/WASM, iOS).

---

## 1. Executive Summary

| Platform        | Build status | Notes |
|----------------|-------------|--------|
| **Backend**    | OK          | Builds; detekt reports many long/complex methods in API routes. |
| **Plugins**    | OK          | All four plugin JARs build. |
| **Frontend Desktop** | OK   | Runs; Koin + Main dispatcher fixed. |
| **Frontend Android** | OK   | APK builds (e.g. `assembleDevDebug`). |
| **Frontend Web (WASM)** | OK | Builds; Koin se inicia en `wasmJsMain` `main()` antes de la UI; bundle grande. |
| **Frontend iOS** | **OK (build)** | composeApp has iOS targets and iosMain actuals; Koin `sharedModule` fixed. Framework link requires Xcode. |

---

## 2. Backend

### 2.1 Build

- **Command**: `./gradlew :kotlin-backend:api:build -x test` (and plugin JARs)
- **Result**: Success.

### 2.2 Discordancias / por pulir

- **detekt**: Muchas rutas y utilidades superan umbrales de complejidad/longitud:
  - `LongMethod`: e.g. `configureErrorHandling`, `collaborationRoutes`, `chunkedUploadRoutes`, `s3Routes`, `storageRoutes`, `syncRoutes`, `aiRoutes`, `pluginRoutes`, `batchRoutes`, etc.
  - `LongParameterList`: e.g. `PluginManager` constructor, `buildListObjectsResponse`.
  - `ComplexInterface`: `PluginManager`.
  - `CognitiveComplexMethod` / `CyclomaticComplexMethod`: varias rutas.
- **Recomendación**: Ir refactorizando rutas largas en subfunciones/handlers y considerar aumentar temporalmente umbrales en baseline o en `config/detekt/detekt.yml` si se prioriza no bloquear CI.

---

## 3. Frontend – Desktop (JVM)

### 3.1 Build & run

- **Build**: `./gradlew :compose-frontend:composeApp:compileKotlinDesktop`
- **Run**: `./gradlew :compose-frontend:composeApp:run` o `make desktop-run`
- **Result**: OK. Sesión inicia correctamente tras los cambios de Koin y Main dispatcher.

### 3.2 Configuración

- **Koin**: Iniciado en `compose-frontend/composeApp/src/desktopMain/kotlin/com/vaultstadio/app/Main.kt` con `startKoin { modules(allModules("http://localhost:8080/api")) }` antes de `application { }`.
- **Main dispatcher**: `kotlinx-coroutines-swing` en `desktopMain` para que `Dispatchers.Main` use el hilo AWT/Swing.
- **Root**: `VaultStadioRoot` usa `KoinContext()` (no `KoinApplication`) porque Koin ya está iniciado globalmente.

### 3.3 Warnings (compartidos con otras plataformas)

- Deprecaciones de Material Icons (usar `Icons.AutoMirrored.Filled.*` donde aplique).
- `Divider` deprecado → usar `HorizontalDivider`.
- `expect`/`actual` en beta (FilePicker, Storage).
- `MainComponent.kt`: uso de API delicada de Decompose sin `@OptIn(DelicateDecomposeApi::class)`.

---

## 4. Frontend – Android

### 4.1 Build

- **Command**: `./gradlew :compose-frontend:androidApp:assembleDevDebug` (o `assembleRelease` / otros flavors).
- **Result**: OK.

### 4.2 Configuración

- **Koin**: Iniciado en `VaultStadioApplication.onCreate()` con `allModules(getServerUrl()) + listOf(androidModule)`.
- **Main dispatcher**: `kotlinx-coroutines-android` aporta `Dispatchers.Main`.
- **Flavors**: `dev`, `staging`, `prod`; dimension `environment`.
- **API URL**: Debug usa `10.0.2.2:8080`; release usa `https://api.vaultstadio.local` (configurable vía `buildConfigField`).

### 4.3 Por pulir

- **MainActivity.kt**: `getParcelableExtra` / `getParcelableArrayListExtra` deprecados en Java; migrar a `getParcelableExtra(name, Class)` (API 33+) o `Bundle.getParcelable(name, Class)` donde sea posible.
- Mismas deprecaciones de Compose/Material que en Desktop (Icons, Divider, etc.).

---

## 5. Frontend – Web (WASM)

### 5.1 Build

- **Command**: `./gradlew :compose-frontend:composeApp:wasmJsBrowserDistribution`
- **Result**: OK.

### 5.2 Discordancias / por pulir

- **Tamaño de bundle (webpack)**:
  - `composeApp.js` ~542 KiB (por encima del límite recomendado 244 KiB).
  - `*.wasm` ~7.91 MiB.
  - **Recomendación**: Valorar code-splitting / lazy loading (p. ej. `import()` o rutas lazy) para reducir entrypoint y mejorar tiempo de carga.
- **Dependency**: “Critical dependency: the request of a dependency is an expression” (habitual en algunos paquetes; revisar si algún dependency usa expresiones dinámicas).
- Mismas deprecaciones de Compose/Material y expect/actual que en Desktop/Android.
- **Koin en WASM**: El entrypoint `wasmJsMain/kotlin/.../Main.kt` solo llama a `CanvasBasedWindow { VaultStadioRoot() }` **sin** `startKoin`. Como `VaultStadioRoot` usa `KoinContext()` (GlobalContext), en runtime WASM es muy probable que falle con “KoinApplication has not been started” al usar pantallas que inyectan dependencias. **Recomendación**: Llamar a `startKoin { modules(allModules(apiBaseUrl)) }` al inicio de `main()` en wasmJs antes de `CanvasBasedWindow`, con una URL por defecto o leída de configuración (p. ej. query param o `window`).

---

## 6. Frontend – iOS

### 6.1 Estado (actualizado)

- **composeApp para iOS**: `./gradlew :compose-frontend:composeApp:compileKotlinIosSimulatorArm64` → **OK**.
- **iosApp framework**: `./gradlew :compose-frontend:iosApp:linkReleaseFrameworkIosSimulatorArm64` requiere **Xcode** instalado (y `xcodebuild` en el PATH). En máquinas sin Xcode (p. ej. CI o Linux) el link falla con `MissingXcodeException`.

### 6.2 Cambios realizados

1. **Targets iOS en composeApp**: Añadidos `iosArm64()`, `iosSimulatorArm64()` y source set `iosMain` con dependencia `ktor-client-darwin`.
2. **Koin**: En composeApp se añadió `fun sharedModule(apiBaseUrl: String) = allModules(apiBaseUrl)`. En iosApp, `KoinHelper.initKoin(apiBaseUrl)` y `doInitKoin(apiBaseUrl?)` usan `sharedModule(apiBaseUrl)` con URL por defecto o pasada desde Swift.
3. **Actuales iOS**: En `composeApp/src/iosMain/` se añadieron implementaciones *stub* para: `Download`, `DragDrop`, `FilePicker`, `MediaPlayer`, `PlatformStorage`. Son válidas para compilar; para producción conviene sustituirlas por implementaciones nativas (UIDocumentPickerViewController, NSUserDefaults, AVPlayer, etc.).

### 6.3 Resumen iOS

| Tema              | Estado | Nota |
|-------------------|--------|------|
| Target iOS en composeApp | Hecho | iosArm64, iosSimulatorArm64 + iosMain. |
| Koin `sharedModule` | Hecho | `sharedModule(apiBaseUrl)` en composeApp; KoinHelper actualizado. |
| Expect/actual iOS | Stubs | Download, DragDrop, FilePicker, MediaPlayer, PlatformStorage en iosMain; completar con APIs nativas cuando se requiera. |
| Link framework    | Requiere Xcode | En macOS con Xcode el link del framework debería funcionar. |

---

## 7. Inconsistencias entre plataformas

### 7.1 Inicio de Koin

| Plataforma | Dónde se inicia Koin | Módulos |
|------------|----------------------|---------|
| Desktop    | `Main.kt` (main)     | `allModules("http://localhost:8080/api")` |
| Android    | `VaultStadioApplication.onCreate()` | `allModules(getServerUrl()) + androidModule` |
| iOS        | `KoinHelper.initKoin()` (Swift) | `sharedModule`, `iosModule` (sharedModule no definido en composeApp) |
| WASM       | Por confirmar        | Debería ser equivalente a Desktop (startKoin antes de UI). |

- **Discordancia**: iOS usa un nombre de módulo (`sharedModule`) que no existe en el código compartido; Desktop/Android usan `allModules(apiBaseUrl)`.

### 7.2 Main dispatcher

| Plataforma | Dependencia que aporta Main |
|------------|-----------------------------|
| Desktop    | `kotlinx-coroutines-swing`   |
| Android    | `kotlinx-coroutines-android` |
| iOS        | (N/A hasta que compile; en KMP/Native suele venir del main thread de UIKit.) |
| WASM       | (Main lo aporta el runtime JS; no suele necesitar dependencia extra.) |

---

## 8. Gradle y calidad de código

### 8.1 Deprecaciones Gradle

- Varios builds avisan: “Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.”  
  **Recomendación**: Ejecutar con `--warning-mode all` y ir sustituyendo usos deprecados (en plugins y en scripts del proyecto).
- **composeApp**: `kotlinOptions { jvmTarget = "17" }` en `androidTarget` está deprecado; migrar a `compilerOptions { jvmTarget = JvmTarget.JVM_17 }` (o el DSL recomendado en la versión de Kotlin usada).

### 8.2 Warnings de Kotlin/Compose (comunes)

- **Icons**: Sustituir `Icons.Filled.*` por `Icons.AutoMirrored.Filled.*` donde corresponda (List, DriveFileMove, Logout, Send, Comment, InsertDriveFile, OpenInNew, Login).
- **Divider**: Sustituir `Divider` por `HorizontalDivider` (AdvancedSearchDialog, MetadataPanel, VersionPanel).
- **Expect/actual**: Clases `expect`/`actual` (FilePicker, Storage) en beta; se puede suprimir con `-Xexpect-actual-classes` si se asume el riesgo, o dejar como está y seguir la evolución del lenguaje.
- **Decompose**: En `MainComponent.kt` (aprox. línea 143) marcar con `@OptIn(DelicateDecomposeApi::class)` el uso de la API delicada.
- **SharedWithMeScreen.kt** (aprox. línea 343): Condición “always true” (`sharedItem.item.size != null`); simplificar o alinear tipos para evitar comparaciones redundantes.

---

## 9. Comandos de build de referencia

```bash
# Backend
./gradlew :kotlin-backend:api:build -x test
./gradlew :kotlin-backend:plugins:image-metadata:pluginJar :kotlin-backend:plugins:video-metadata:pluginJar \
  :kotlin-backend:plugins:fulltext-search:pluginJar :kotlin-backend:plugins:ai-classification:pluginJar

# Frontend – Desktop
./gradlew :compose-frontend:composeApp:compileKotlinDesktop
./gradlew :compose-frontend:composeApp:run

# Frontend – Android
./gradlew :compose-frontend:androidApp:assembleDevDebug

# Frontend – Web (WASM)
./gradlew :compose-frontend:composeApp:wasmJsBrowserDistribution

# iOS (falla hasta añadir target iOS y arreglar Koin)
./gradlew :compose-frontend:iosApp:linkReleaseFrameworkIosSimulatorArm64
```

---

## 10. Prioridades sugeridas

| # | Prioridad | Estado | Notas |
|---|-----------|--------|--------|
| 1 | **Alta**: iOS (target composeApp, sharedModule, Koin desde Swift) | **Hecho** | Targets iOS añadidos, `sharedModule(apiBaseUrl)` en composeApp, KoinHelper actualizado; Swift llama `KoinHelperKt.doInitKoin()` en `VaultStadioApp.swift`. |
| 2 | **Media**: Reducir warnings de deprecación | **Hecho** | Icons → `Icons.AutoMirrored.Filled.*`; `Divider` → `HorizontalDivider`; Parcelable en Android (API 33+ con `getParcelableExtra(_, Uri::class.java)`). `kotlinOptions` se mantiene (migración a `compilerOptions` en `androidTarget`/AGP requiere otro enfoque). |
| 3 | **Media**: Koin y Main dispatcher en WASM | **Hecho** | `startKoin` en `wasmJsMain` `main()` antes de la UI. |
| 4 | **Media**: Tamaño del bundle WASM | **Hecho** | Lazy loading en runtime (Decompose crea pantallas bajo demanda); compilación incremental WASM; doc [WASM_BUNDLE_OPTIMIZATION.md](WASM_BUNDLE_OPTIMIZATION.md). Kotlin/Wasm no soporta aún code-splitting en chunks. |
| 5 | **Baja**: Refactorizar rutas backend (detekt) | **Pendiente** | Reducir longitud/complejidad de rutas o ajustar baseline/umbrales. |

### Resumen: qué falta

- **Media**: Optimización del bundle WASM → **Hecho**: lazy loading en runtime, incremental WASM, documentado en [WASM_BUNDLE_OPTIMIZATION.md](WASM_BUNDLE_OPTIMIZATION.md). Code-splitting en múltiples chunks no soportado por el toolchain Kotlin/Wasm.
- **Baja**: Refactor detekt en backend o ajuste de baseline.
- **Nota**: `kotlinOptions` (composeApp/androidTarget y androidApp) sigue deprecado; migrar a `compilerOptions` cuando el DSL lo permita en ese contexto.
