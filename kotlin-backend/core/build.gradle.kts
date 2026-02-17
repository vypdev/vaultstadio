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
    // Kotlinx
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)

    // Arrow for functional programming
    api(libs.bundles.arrow)

    // Database
    implementation(libs.bundles.exposed)
    implementation(libs.hikari)

    // HTTP Client for AI providers
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Logging
    implementation(libs.kotlin.logging)

    // DI
    implementation(libs.koin.core)

    // Redis (for distributed lock/multipart upload managers)
    implementation(libs.lettuce.core)

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
