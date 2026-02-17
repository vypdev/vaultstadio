/**
 * VaultStadio - Self-Hosted Storage Platform
 *
 * Root settings for the multi-module Kotlin project including:
 * - Kotlin Backend (Ktor-based)
 * - Kotlin Multiplatform Shared Module
 * - Compose Multiplatform Frontend
 */

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    }
}

rootProject.name = "vaultstadio"

// Backend modules
include(":kotlin-backend:core")
include(":kotlin-backend:api")
include(":kotlin-backend:plugins-api")
include(":kotlin-backend:infrastructure")

// Backend plugins
include(":kotlin-backend:plugins:image-metadata")
include(":kotlin-backend:plugins:video-metadata")
include(":kotlin-backend:plugins:fulltext-search")
include(":kotlin-backend:plugins:ai-classification")

// Compose Multiplatform Frontend
include(":compose-frontend:composeApp")
include(":compose-frontend:androidApp")
include(":compose-frontend:iosApp")
