/**
 * Data: config (ConfigRepository impl, config use-case impls).
 * Depends on :domain:config and :data:network.
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val libNamespace = "com.vaultstadio.app.data.config"

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
                implementation(project(":domain:config"))
                implementation(project(":data:network"))
                implementation(libs.koin.core)
            }
        }
    }
}
afterEvaluate { group = libNamespace }
