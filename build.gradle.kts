/**
 * VaultStadio - Self-Hosted Storage Platform
 *
 * Root build configuration for the entire project.
 * Manages versions, plugins, and common configurations.
 */

plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("multiplatform") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    kotlin("android") version "2.0.21" apply false
    id("org.jetbrains.compose") version "23.10.16.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.android.application") version "8.7.2" apply false
    id("com.android.library") version "8.7.2" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

allprojects {
    group = "com.vaultstadio"
    version = "1.0.0-SNAPSHOT"
}

// Detekt configuration for root project
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("$rootDir/config/detekt/baseline.xml")
    parallel = true
    autoCorrect = false
    ignoreFailures = false // Build fails when detekt finds issues (not in baseline)
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            exclude { lintError ->
                val path = lintError.file.path
                path.contains("/build/") ||
                    path.contains("/generated/") ||
                    path.contains("resourceGenerator") ||
                    path.contains("commonResClass") ||
                    path.contains("ActualResourceCollectors") ||
                    path.contains("ExpectResourceCollectors")
            }
        }
    }

    // Run ktlint (formatting) before detekt (quality). Workflow: ktlintFormat then detekt.
    afterEvaluate {
        tasks.findByName("ktlintCheck")?.let { ktlintCheck ->
            tasks.findByName("detekt")?.dependsOn(ktlintCheck)
        }
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom("$rootDir/config/detekt/detekt.yml")
        baseline = file("$rootDir/config/detekt/baseline.xml")
        parallel = true
        autoCorrect = false
        ignoreFailures = false // Build fails when detekt finds issues (not in baseline)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}
