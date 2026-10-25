import java.math.BigDecimal

/**
 * VaultStadio Domain Storage
 *
 * Storage entities and repository port.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":domain:common"))
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)
    api(libs.bundles.arrow)

    testImplementation(libs.bundles.testing)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// STO-AUTH-01–06: enforce the measured policy budget, independently of upload services.
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    doFirst {
        check(executionData.files.any { it.isFile && it.length() > 0 }) {
            "Storage authorization coverage execution data is missing"
        }
    }
    violationRules {
        rule {
            element = "CLASS"
            includes = listOf("com.vaultstadio.domain.storage.model.StorageAccessPolicy")
            for (metric in listOf("LINE", "BRANCH")) {
                limit {
                    counter = metric
                    minimum = BigDecimal.ONE
                }
            }
        }
    }
}
