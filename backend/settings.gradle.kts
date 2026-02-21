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
include(":core:common")
include(":core:auth")
include(":core:storage")
include(":core:share")
include(":core:activity")
include(":core:version")
include(":core:sync")
include(":core:federation")
include(":core:collaboration")
include(":core:ai")
include(":application:auth")
include(":application:storage")
include(":application:share")
include(":application:user")
include(":application:admin")
include(":application:activity")
include(":application:metadata")
include(":application:version")
include(":application:sync")
include(":application:plugin")
include(":application:chunkedupload")
include(":application:health")
include(":application:ai")
include(":api")
include(":plugins-api")
include(":infrastructure")
include(":plugins:image-metadata")
include(":plugins:video-metadata")
include(":plugins:fulltext-search")
include(":plugins:ai-classification")
