/**
 * VaultStadio Backend - Root build.
 * Detekt, ktlint, and common config for all backend subprojects.
 * Uses this project's own config only: config/detekt/detekt.yml and config/detekt/baseline.xml
 * (no shared root config).
 */

plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

allprojects {
    group = "com.vaultstadio"
    version = "1.0.0-SNAPSHOT"
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("$rootDir/config/detekt/baseline.xml")
    parallel = true
    autoCorrect = false
    ignoreFailures = false
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            exclude { lintError ->
                val path = lintError.file.path
                path.contains("/build/") ||
                    path.contains("/generated/")
            }
        }
    }

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
        ignoreFailures = false
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
