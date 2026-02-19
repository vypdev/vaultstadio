/**
 * VaultStadio Video Metadata Plugin
 *
 * Extracts metadata from video files using FFprobe.
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
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Plugin-Id" to "com.vaultstadio.plugins.video-metadata",
            "Plugin-Version" to version,
            "Plugin-Class" to "com.vaultstadio.plugins.video.VideoMetadataPlugin",
        )
    }
}
