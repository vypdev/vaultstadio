/**
 * Domain: shared result type (Result, ApiException, NetworkException).
 * No dependencies on other domain modules.
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val libNamespace = "com.vaultstadio.app.domain.result"

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
        }
        val commonTest by getting {
            kotlin.srcDirs("src/test")
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
afterEvaluate { group = libNamespace }
