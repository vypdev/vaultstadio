/**
 * Domain: storage (models, interfaces).
 * Distinct group so resolution does not substitute with :data:storage (see FRONTEND_KMP_TASK_CYCLE.md).
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val libNamespace = "com.vaultstadio.app.domain.storage"

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
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}
afterEvaluate { group = libNamespace }
