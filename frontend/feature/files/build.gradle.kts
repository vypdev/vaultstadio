plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val libNamespace = "com.vaultstadio.app.feature.files"

kotlin {
    jvm("desktop")
    android {
        namespace = libNamespace
        compileSdk = 36
        minSdk = 24
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    wasmJs { browser() }
    iosArm64(); iosSimulatorArm64()
    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("src/main")
            dependencies {
                implementation(compose.runtime)
                implementation(project(":domain:result"))
                implementation(project(":domain:storage"))
                implementation(project(":domain:activity"))
                implementation(project(":domain:config"))
                implementation(project(":domain:metadata"))
                implementation(project(":domain:share"))
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
