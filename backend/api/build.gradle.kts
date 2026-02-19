/**
 * VaultStadio API Module
 *
 * REST API layer using Ktor.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
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

application {
    mainClass.set("com.vaultstadio.api.ApplicationKt")
}

dependencies {
    // Project modules
    implementation(project(":core"))
    implementation(project(":plugins-api"))
    implementation(project(":infrastructure"))

    // Ktor Server
    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)

    // Kotlinx
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)

    // Database
    implementation(libs.bundles.exposed)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.h2)

    // Database migrations
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    // Logging
    implementation(libs.logback)
    implementation(libs.kotlin.logging)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.ktor.client.content.negotiation)
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

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.vaultstadio.api.ApplicationKt"
    }
}
