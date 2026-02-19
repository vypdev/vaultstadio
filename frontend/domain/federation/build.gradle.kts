plugins { alias(libs.plugins.kotlin.multiplatform); alias(libs.plugins.android.kotlin.multiplatform.library) }

val libNamespace = "com.vaultstadio.app.domain.federation"

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
        implementation(project(":domain:result"))
        implementation(libs.kotlinx.datetime)
    }
        }
    }
}
afterEvaluate { group = libNamespace }
