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
    implementation(project(":domain:common"))
    implementation(project(":domain:storage"))
    implementation(project(":domain:collaboration"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.arrow)
    testImplementation(libs.bundles.testing)
}

tasks.test { useJUnitPlatform() }

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
