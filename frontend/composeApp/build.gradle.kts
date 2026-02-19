/**
 * VaultStadio Compose Multiplatform App
 *
 * Shared UI code for all platforms.
 */

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    jacoco
}

kotlin {
    jvm("desktop")

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    wasmJs {
        browser()
        binaries.executable()
        // Compiler keeps FQN disabled by default to reduce .wasm size (see wasm-configuration docs).
        // Lazy loading: Decompose creates screen components on demand (createChild per route),
        // so only the active screen is instantiated; all code remains in a single bundle (Kotlin/Wasm has no chunk splitting yet).
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                // Kotlinx
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // Ktor Client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json.multiplatform)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.websockets)

                // Koin
                implementation(libs.koin.core)
                implementation(libs.koin.core.viewmodel)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                api(libs.koin.annotations)

                // JetBrains Lifecycle (KMP ViewModel)
                implementation(libs.jetbrains.lifecycle.viewmodel)
                implementation(libs.jetbrains.lifecycle.viewmodel.compose)

                // Decompose (Navigation)
                implementation(libs.decompose)
                implementation(libs.decompose.compose)
                implementation(libs.essenty.lifecycle)
                implementation(libs.essenty.lifecycle.coroutines)
                implementation(libs.essenty.statekeeper)
                implementation(libs.essenty.backhandler)

                // Image loading
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
                // Provides Dispatchers.Main for Desktop (AWT/Swing event thread)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        val wasmJsMain by getting {
            dependencies {
                // Web-specific dependencies
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

// KSP configuration for Koin Annotations
dependencies {
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
    add("kspDesktop", libs.koin.ksp.compiler)
    add("kspWasmJs", libs.koin.ksp.compiler)
    add("kspAndroid", libs.koin.ksp.compiler)
    add("kspIosX64", libs.koin.ksp.compiler)
    add("kspIosArm64", libs.koin.ksp.compiler)
    add("kspIosSimulatorArm64", libs.koin.ksp.compiler)
}

android {
    namespace = "com.vaultstadio.app.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// ViewModel and UploadManager tests use UnconfinedTestDispatcher for Main (ViewModelTestBase)
// so they run on desktop and Android without a real main looper.

// Make sure compilation tasks depend on KSP metadata generation
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

// Make ktlint tasks depend on KSP to avoid race conditions
tasks.matching { it.name.contains("Ktlint") && it.name.contains("CommonMain") }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

// Add generated source directories
kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

compose.desktop {
    application {
        mainClass = "com.vaultstadio.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "VaultStadio"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("icons/icon.icns"))
            }
            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}

// JaCoCo coverage for desktop (JVM) tests; commonTest runs on desktop as desktopTest
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates JaCoCo coverage report for desktop tests"

    val desktopTest = tasks.named("desktopTest").get()
    dependsOn(desktopTest)

    val jacocoExt = desktopTest.extensions.findByType(JacocoTaskExtension::class.java)
    executionData.setFrom(
        if (jacocoExt != null) {
            files(jacocoExt.destinationFile)
        } else {
            fileTree(layout.buildDirectory) { include("jacoco/desktopTest.exec") }
        },
    )

    val commonMainKotlin = kotlin.sourceSets.getByName("commonMain").kotlin.srcDirs
    val desktopMainKotlin = kotlin.sourceSets.getByName("desktopMain").kotlin.srcDirs
    sourceDirectories.setFrom(files(commonMainKotlin, desktopMainKotlin))

    val desktopClasses = layout.buildDirectory.dir("classes/kotlin/desktop/main")
    classDirectories.setFrom(
        desktopClasses.map { dir -> fileTree(dir.asFile) { exclude("**/$$*") } },
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
