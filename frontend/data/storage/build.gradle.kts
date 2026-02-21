@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

/**
 * Data: storage (API client, repository impl).
 * Distinct group so resolution does not substitute with :domain:storage (see FRONTEND_KMP_TASK_CYCLE.md).
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.koin.compiler)
}

val libNamespace = "com.vaultstadio.app.data.storage"

kotlin {
    jvm("desktop")
    android {
        namespace = libNamespace
        compileSdk = 36
        minSdk = 24
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    wasmJs { browser() }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("src/main")
            dependencies {
                implementation(project(":domain:result"))
                implementation(project(":domain:storage"))
                implementation(project(":data:network"))
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json.multiplatform)
                implementation(libs.ktor.client.logging)
                implementation(libs.koin.core)
                api(libs.koin.annotations)
            }
        }
    }
}
afterEvaluate { group = libNamespace }
