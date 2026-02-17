/**
 * VaultStadio Infrastructure Module
 *
 * Contains implementations of repositories, storage backends, and other infrastructure.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
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
    // Project modules
    api(project(":kotlin-backend:core"))

    // Kotlinx
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Database
    implementation(libs.bundles.exposed)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    // Storage
    implementation(libs.minio)
    api(libs.aws.s3)

    // Security
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Logging
    implementation(libs.logback)
    implementation(libs.kotlin.logging)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.testcontainers)
}

tasks.test {
    useJUnitPlatform()
}
