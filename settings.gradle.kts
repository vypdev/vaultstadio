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

// Backend modules (backend is a standalone project; build with: cd backend && ./gradlew build)
include(":backend:core")
include(":backend:api")
include(":backend:plugins-api")
include(":backend:infrastructure")
include(":backend:plugins:image-metadata")
include(":backend:plugins:video-metadata")
include(":backend:plugins:fulltext-search")
include(":backend:plugins:ai-classification")

// Frontend is standalone: build with cd frontend && ./gradlew build (not included from root)
