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

include(":core")
include(":api")
include(":plugins-api")
include(":infrastructure")
include(":plugins:image-metadata")
include(":plugins:video-metadata")
include(":plugins:fulltext-search")
include(":plugins:ai-classification")
