plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin { jvmToolchain(17) }

val libGroup = "com.vaultstadio.application"
afterEvaluate {
    group = libGroup
    tasks.jar.get().archiveBaseName.set("application-${project.name}")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core:storage"))
    implementation(project(":domain:common"))
    implementation(project(":domain:auth"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.arrow)
    implementation(libs.koin.core)
    implementation(libs.kotlin.logging)
    testImplementation(libs.bundles.testing)
}

tasks.test { useJUnitPlatform() }
