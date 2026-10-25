@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

/**
 * Data: network layer (ApiResult, BaseApi, HttpClientFactory, ApiResultMapper, common DTOs).
 * Depends on :domain:result for Result type at the boundary.
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val libNamespace = "com.vaultstadio.app.data.network"

kotlin {
    jvm("desktop")
    android {
        namespace = libNamespace
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    wasmJs { browser() }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("src/main")
            dependencies {
                api(project(":domain:result"))
                implementation(libs.kotlinx.serialization.json)
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
