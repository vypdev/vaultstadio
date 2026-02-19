/**
 * VaultStadio Image Metadata Plugin
 *
 * Extracts EXIF and other metadata from images.
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
    implementation(project(":plugins-api"))
    implementation(project(":core"))

    // Image metadata extraction
    implementation("com.drewnoakes:metadata-extractor:2.19.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

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
            "Plugin-Id" to "com.vaultstadio.plugins.image-metadata",
            "Plugin-Version" to version,
            "Plugin-Class" to "com.vaultstadio.plugins.image.ImageMetadataPlugin",
        )
    }
}
