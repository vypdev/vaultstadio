/**
 * VaultStadio AI Classification Plugin
 *
 * Provides AI-powered image classification and tagging.
 * Supports multiple backends: Ollama (local), OpenAI, and custom providers.
 */

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    jacoco
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

dependencies {
    // Plugin SDK
    implementation(project(":kotlin-backend:plugins-api"))
    implementation(project(":kotlin-backend:core"))

    // HTTP Client for AI APIs
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Logging
    implementation(libs.kotlin.logging)

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
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

// Create fat JAR for plugin distribution
tasks.register<Jar>("pluginJar") {
    archiveClassifier.set("plugin")
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") && !it.name.contains("plugins-api") && !it.name.contains("core") }
            .map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Plugin-Id" to "com.vaultstadio.plugins.ai-classification",
            "Plugin-Version" to version,
            "Plugin-Class" to "com.vaultstadio.plugins.ai.AIClassificationPlugin",
        )
    }
}
