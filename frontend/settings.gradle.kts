/**
 * VaultStadio Frontend - Standalone Kotlin Multiplatform project.
 * Uses this project's own config only: config/detekt (no shared root config).
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

rootProject.name = "frontend"

// Domain (by area)
include(":domain:result")
include(":domain:auth")
include(":domain:storage")
include(":domain:admin")
include(":domain:sync")
include(":domain:share")
include(":domain:activity")
include(":domain:metadata")
include(":domain:plugin")
include(":domain:version")
include(":domain:collaboration")
include(":domain:federation")
include(":domain:ai")
include(":domain:config")
include(":domain:upload")

// Data (by area)
include(":data:network")
include(":data:auth")
include(":data:storage")
include(":data:admin")
include(":data:sync")
include(":data:share")
include(":data:activity")
include(":data:metadata")
include(":data:plugin")
include(":data:version")
include(":data:collaboration")
include(":data:federation")
include(":data:ai")
include(":data:config")

// Feature (per screen)
include(":feature:auth")
include(":feature:admin")
include(":feature:sync")
include(":feature:shares")
include(":feature:sharedwithme")
include(":feature:activity")
include(":feature:profile")
include(":feature:settings")
include(":feature:security")
include(":feature:changepassword")
include(":feature:plugins")
include(":feature:files")
include(":feature:upload")
include(":feature:versionhistory")
include(":feature:collaboration")
include(":feature:federation")
include(":feature:ai")
include(":feature:main")
include(":feature:licenses")

// App and platforms
include(":composeApp")
include(":androidApp")
include(":iosApp")
