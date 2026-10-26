import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

tasks.named<Detekt>("detekt") {
    setSource(
        files(
            "src/commonMain/kotlin",
            "src/wasmJsMain/kotlin",
            "src/desktopMain/kotlin",
            "src/androidMain/kotlin",
            "src/iosMain/kotlin",
        ),
    )
}

kotlin {
    jvm("desktop")
    android {
        namespace = "com.vaultstadio.app.core.form"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = 24
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    wasmJs {
        browser { testTask { useKarma { useChromeHeadless() } } }
        binaries.executable()
    }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:ui"))
                implementation(compose.material3)
                implementation(compose.foundation)
            }
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        wasmJsTest.dependencies { implementation(libs.kotlinx.coroutines.test) }
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}
