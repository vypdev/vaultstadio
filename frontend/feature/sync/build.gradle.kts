plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val libNamespace = "com.vaultstadio.app.feature.sync"

kotlin {
    jvm("desktop")
    android {
        namespace = libNamespace
        compileSdk = 34
        minSdk = 24
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    wasmJs { browser() }
    iosX64(); iosArm64(); iosSimulatorArm64()
    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("src/kotlin")
            dependencies {
                implementation(compose.runtime)
                implementation(project(":domain:result"))
                implementation(project(":data:network"))
            }
        }
    }
}
afterEvaluate { group = libNamespace }
