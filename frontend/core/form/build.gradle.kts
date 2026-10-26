import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    jacoco
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val interactionExecution = project(":composeApp").layout.buildDirectory.file("jacoco/desktopTest.exec")
val modelExecution = layout.buildDirectory.file("jacoco/desktopTest.exec")
val formClasses = layout.buildDirectory.dir("classes/kotlin/desktop/main")
val verifyFormCoverageInputs = tasks.register("verifyFormCoverageInputs") {
    group = "verification"
    dependsOn(":composeApp:desktopTest", "desktopTest")
    inputs.files(interactionExecution, modelExecution)
    inputs.dir(formClasses)
    doLast {
        val executions = inputs.files.files.filter { it.extension == "exec" }
        check(executions.size == 2 && executions.all { it.length() > 0 }) {
            "Both real component interactions and form-model execution data are required"
        }
        check(inputs.files.files.any { it.isFile && it.extension == "class" }) {
            "Compiled JVM form production classes are required"
        }
    }
}
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(verifyFormCoverageInputs)
    executionData.from(interactionExecution, modelExecution)
    classDirectories.from(formClasses)
    sourceDirectories.from(
        kotlin.sourceSets.getByName("commonMain").kotlin.srcDirs,
        kotlin.sourceSets.getByName("desktopMain").kotlin.srcDirs,
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
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
