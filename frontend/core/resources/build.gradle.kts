/**
 * Core: shared resources (i18n string resources).
 * No platform dependencies; app is responsible for persisting language preference.
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val libNamespace = "com.vaultstadio.app.core.resources"

kotlin {
    jvm("desktop")
    android {
        namespace = libNamespace
        compileSdk = 36
        minSdk = 24
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    wasmJs { browser() }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("src/main")
            dependencies {
                implementation(compose.runtime)
            }
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
