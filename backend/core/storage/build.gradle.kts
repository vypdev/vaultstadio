import java.math.BigDecimal

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

kotlin { jvmToolchain(17) }

val libGroup = "com.vaultstadio.core"
afterEvaluate {
    group = libGroup
    tasks.jar.get().archiveBaseName.set("core-${project.name}")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core:common"))
    implementation(project(":domain:common"))
    implementation(project(":domain:storage"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.arrow)
    implementation(libs.koin.core)
    implementation(libs.kotlin.logging)
    testImplementation(libs.bundles.testing)
}

tasks.test { useJUnitPlatform() }

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
            includes = listOf("com.vaultstadio.core.domain.service.StorageHierarchyKt")
            for (metric in listOf("LINE", "BRANCH")) {
                limit {
                    counter = metric
                    minimum = BigDecimal.ONE
                }
            }
        }
    }
}
