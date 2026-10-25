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
    // Core module for types; core:common for EventBus, EventHandlerResult
    api(project(":core"))
    api(project(":core:common"))
    api(project(":domain:common"))
    api(project(":domain:storage"))
    api(project(":domain:auth"))

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

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
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
