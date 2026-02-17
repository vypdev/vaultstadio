# WASM Bundle Optimization

This document describes the current approach to Web (WASM) bundle size and load behaviour, and what has been done to optimize it.

## Current situation

- **Output**: One `.wasm` binary (~7.9 MiB) and one JS loader (`composeApp.js`, ~542 KiB). The report target was to stay under 244 KiB for the JS entrypoint; the single-bundle nature of Kotlin/Wasm makes that difficult without toolchain support for splitting.
- **Kotlin/Wasm**: The compiler produces a single WebAssembly module. There is no built-in code-splitting or multiple chunks (unlike webpack-style JS apps). All Kotlin code is in one bundle.

## What we did (lazy loading and build optimizations)

### 1. Lazy loading at runtime (already in place)

- **Navigation**: Decompose creates screen components only when the user navigates to that route. `MainComponent.createChild` is called only when a config is pushed, so we do not instantiate all screens at startup.
- **Effect**: Initial memory and work are lower; only the active screen and its dependencies are created. The bytecode for all screens is still in the single `.wasm` (unavoidable with current Kotlin/Wasm).

### 2. Build and compiler settings

- **Incremental WASM compilation** (`gradle.properties`): `kotlin.incremental.wasm=true` to speed up dev builds. It does not reduce bundle size.
- **FQN disabled (default)**: Kotlin/Wasm does not store fully qualified class names in the binary by default, which keeps the `.wasm` smaller. We do not enable `-Xwasm-kclass-fqn`.

### 3. What we do not do (and why)

- **No multi-chunk code-splitting**: Kotlin/Wasm does not support emitting multiple chunks or dynamic `import()` of Kotlin code. When/if the toolchain adds support, we can revisit.
- **No custom webpack split**: The Compose/Wasm pipeline produces one executable; we do not maintain a custom webpack config to split the Kotlin output.

## Recommendations for future

- **Monitor Kotlin releases**: Watch for official code-splitting or lazy-loading support for the Wasm target.
- **Keep dependencies lean**: Prefer pulling in only what is needed for the web target (e.g. avoid pulling heavy JVM-only libs into `commonMain` if they are not used on WASM).
- **Heavy features**: If a feature is very large and only used on a few screens, consider `expect`/`actual` with a stub on WASM that loads or enables the feature on demand (where the toolchain allows).

## References

- [Kotlin Wasm configuration](https://kotlinlang.org/docs/wasm-configuration.html)
- [PLATFORM_BUILD_REPORT.md](PLATFORM_BUILD_REPORT.md) – bundle size notes and priorities
- [FRONTEND_DEVELOPMENT.md](../frontend/FRONTEND_DEVELOPMENT.md) – build commands and structure
