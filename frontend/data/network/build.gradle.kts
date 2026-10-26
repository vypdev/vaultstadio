@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.math.BigDecimal

/**
 * Data: network layer (ApiResult, BaseApi, HttpClientFactory, ApiResultMapper, common DTOs).
 * Depends on :domain:result for Result type at the boundary.
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    jacoco
}

val libNamespace = "com.vaultstadio.app.data.network"

kotlin {
    jvm("desktop")
    android {
        namespace = libNamespace
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = 24
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    wasmJs {
        browser()
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("src/main")
            dependencies {
                api(project(":domain:result"))
                implementation(libs.kotlinx.serialization.json)
                // 0.9.1 uses the supported Wasm module loader under Kotlin 2.4.20.
                implementation(libs.kotlinx.io.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json.multiplatform)
                implementation(libs.ktor.client.logging)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
            }
        }
        val wasmJsMain by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
afterEvaluate { group = libNamespace }

// BOOT-SER-01/02: publish the complete desktop module report and enforce the changed serializer budget.
val networkDesktopTest = tasks.named("desktopTest")
val networkDesktopExecution = networkDesktopTest.map {
    requireNotNull(it.extensions.getByType(JacocoTaskExtension::class.java).destinationFile)
}
val networkDesktopClasses = layout.buildDirectory.dir("classes/kotlin/desktop/main")

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(networkDesktopTest)
    executionData.from(networkDesktopExecution)
    classDirectories.from(networkDesktopClasses)
    sourceDirectories.from(
        kotlin.sourceSets.getByName("commonMain").kotlin.srcDirs,
        kotlin.sourceSets.getByName("desktopMain").kotlin.srcDirs,
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml"))
    }
}

tasks.register<JacocoCoverageVerification>("verifyInstantSerializerCoverage") {
    dependsOn(networkDesktopTest)
    executionData.from(networkDesktopExecution)
    classDirectories.from(networkDesktopClasses)
    doFirst {
        check(executionData.files.any { it.isFile && it.length() > 0 }) {
            "Instant serializer coverage execution data is missing"
        }
        check(
            classDirectories.files.any {
                it.resolve("com/vaultstadio/app/data/network/serialization/InstantIso8601Serializer.class").isFile
            },
        ) {
            "Instant serializer production class is missing from coverage"
        }
    }
    violationRules {
        rule {
            element = "CLASS"
            includes = listOf("com.vaultstadio.app.data.network.serialization.InstantIso8601Serializer")
            limit {
                counter = "LINE"
                minimum = BigDecimal.ONE
            }
        }
    }
}
