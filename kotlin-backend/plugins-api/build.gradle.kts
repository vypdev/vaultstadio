/**
 * VaultStadio Plugins API Module
 *
 * SDK for developing VaultStadio plugins.
 * This module provides the interfaces and base classes that plugins must implement.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Core module for domain models
    api(project(":kotlin-backend:core"))

    // Kotlinx
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)

    // Arrow for functional programming
    api(libs.bundles.arrow)

    // Logging
    api(libs.kotlin.logging)

    // Testing
    testImplementation(libs.bundles.testing)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.vaultstadio"
            artifactId = "plugins-api"
            version = project.version.toString()

            from(components["java"])
        }
    }
}
