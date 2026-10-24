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
    implementation(project(":core:common"))
    implementation(project(":core:auth"))
    implementation(project(":domain:common"))
    implementation(project(":domain:storage"))
    implementation(project(":domain:share"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.arrow)
    implementation(libs.koin.core)
    implementation(libs.kotlin.logging)
    testImplementation(libs.bundles.testing)
}

tasks.test { useJUnitPlatform() }
