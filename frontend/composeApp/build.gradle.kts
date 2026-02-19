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

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    jacoco
}

kotlin {
    jvm("desktop")

    android {
        namespace = "com.vaultstadio.app.shared"
        compileSdk = 34
        minSdk = 24
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
            kotlin.srcDirs("src/main")
            dependencies {
                implementation(project(":domain:result"))
                implementation(project(":domain:auth"))
                implementation(project(":domain:storage"))
                implementation(project(":domain:config"))
                implementation(project(":domain:share"))
                implementation(project(":domain:activity"))
                implementation(project(":domain:admin"))
                implementation(project(":domain:plugin"))
                implementation(project(":domain:version"))
                implementation(project(":domain:sync"))
                implementation(project(":domain:metadata"))
                implementation(project(":domain:federation"))
                implementation(project(":domain:ai"))
                implementation(project(":domain:collaboration"))
                implementation(project(":domain:upload"))
                implementation(project(":core:resources"))
                implementation(project(":feature:auth"))
                implementation(project(":feature:settings"))
                implementation(project(":data:network"))
                implementation(project(":data:sync"))
                implementation(project(":data:metadata"))
                implementation(project(":data:federation"))
                implementation(project(":data:config"))
                implementation(project(":data:auth"))
                implementation(project(":data:activity"))
                implementation(project(":data:admin"))
                implementation(project(":data:share"))
                implementation(project(":data:plugin"))
                implementation(project(":data:version"))
                implementation(project(":data:storage"))
                implementation(project(":data:ai"))
                implementation(project(":data:collaboration"))
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
            kotlin.srcDirs("src/test")
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(project(":domain:result"))
                implementation(project(":domain:federation"))
                implementation(project(":domain:storage"))
                implementation(project(":domain:config"))
                implementation(project(":domain:share"))
                implementation(project(":domain:activity"))
                implementation(project(":domain:plugin"))
                implementation(project(":domain:version"))
                implementation(project(":data:storage"))
                implementation(project(":data:federation"))
                implementation(project(":data:config"))
                implementation(project(":data:share"))
                implementation(project(":data:activity"))
                implementation(project(":data:plugin"))
                implementation(project(":data:admin"))
                implementation(project(":data:version"))
            }
        }
    }
}

// ViewModel and UploadManager tests use UnconfinedTestDispatcher for Main (ViewModelTestBase)
// so they run on desktop and Android without a real main looper.

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
