plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val libNamespace = "com.vaultstadio.app.feature.ai"

kotlin {
    jvm("desktop")
    android {
        namespace = libNamespace
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = 24
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    wasmJs {
        browser()
        binaries.executable()
    }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("src/main")
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(project(":domain:result"))
                implementation(project(":domain:ai"))
                implementation(project(":core:resources"))
                implementation(project(":core:ui"))
                implementation(project(":core:form"))
                implementation(libs.jetbrains.lifecycle.viewmodel)
                implementation(libs.decompose)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
            }
        }
    }
}
afterEvaluate { group = libNamespace }
