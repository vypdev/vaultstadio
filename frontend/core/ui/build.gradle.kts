/**
 * Core: shared presentation accessibility primitives.
 * Controls retain the framework edit actions, focus, enabled state and password semantics.
 */

import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    jacoco
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val libNamespace = "com.vaultstadio.app.core.ui"

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
                implementation(compose.ui)
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

val inputExecution = project(":composeApp").layout.buildDirectory.file("jacoco/desktopTest.exec")
val inputClasses = layout.buildDirectory.dir("classes/kotlin/desktop/main")
val verifyInputCoverageInputs = tasks.register("verifyInputCoverageInputs") {
    group = "verification"
    dependsOn(":composeApp:desktopTest")
    inputs.file(inputExecution)
    inputs.dir(inputClasses)
    doLast {
        check(inputs.files.files.any { it.extension == "exec" && it.length() > 0 }) {
            "Production UI interaction execution data is required"
        }
        check(
            inputs.files.files.any { file ->
                file.isFile && file.extension == "class" && file.length() > 0
            },
        ) { "Production accessibility classes are required" }
    }
}
val inputReport = tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(verifyInputCoverageInputs)
    executionData.from(inputExecution)
    classDirectories.from(inputClasses)
    sourceDirectories.from(kotlin.sourceSets.getByName("commonMain").kotlin.srcDirs)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
tasks.register<JacocoCoverageVerification>("verifyInputSemanticsCoverage") {
    group = "verification"
    dependsOn(inputReport)
    executionData.from(inputExecution)
    classDirectories.from(inputClasses)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "1.0".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "1.0".toBigDecimal()
            }
        }
    }
}
