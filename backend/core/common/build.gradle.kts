plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin { jvmToolchain(17) }

val libGroup = "com.vaultstadio.core"
afterEvaluate {
    group = libGroup
    tasks.jar.get().archiveBaseName.set("core-${project.name}")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.arrow)
    implementation(libs.koin.core)
    implementation(libs.kotlin.logging)
    implementation(libs.lettuce.core)
    testImplementation(libs.bundles.testing)
    testImplementation(project(":domain:storage"))
}

tasks.test { useJUnitPlatform() }
