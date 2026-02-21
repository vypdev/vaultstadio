/**
 * VaultStadio Backend - Standalone Kotlin project.
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "backend"

// Domain (innermost; no or minimal project deps)
include(":domain:common")
include(":domain:storage")
include(":domain:auth")
include(":domain:share")
include(":domain:activity")
include(":domain:admin")
include(":domain:version")
include(":domain:sync")
include(":domain:federation")
include(":domain:collaboration")
include(":domain:metadata")
include(":domain:plugin")

include(":core")
include(":api")
include(":plugins-api")
include(":infrastructure")
include(":plugins:image-metadata")
include(":plugins:video-metadata")
include(":plugins:fulltext-search")
include(":plugins:ai-classification")
