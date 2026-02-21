/**
 * VaultStadio Core Module
 *
 * Contains the core storage engine, domain models, and business logic.
 * This module is storage-agnostic and provides interfaces for:
 * - File storage operations
 * - Metadata management
 * - Event publishing for plugins
 * - User and access management
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    jacoco
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
    // Domain (types and ports)
    api(project(":domain:common"))
    api(project(":domain:storage"))
    api(project(":domain:auth"))
    api(project(":domain:share"))
    api(project(":domain:activity"))
    api(project(":domain:admin"))

    // Kotlinx
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)

    // Arrow for functional programming
    api(libs.bundles.arrow)

    // Logging
    implementation(libs.kotlin.logging)

    // Testing
    testImplementation(libs.bundles.testing)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
