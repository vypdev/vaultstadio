/**
 * VaultStadio Full-Text Search Plugin
 *
 * Provides full-text search capabilities for file content.
 * Uses Apache Lucene for indexing and searching.
 */

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
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

val luceneVersion = "9.11.1"

dependencies {
    // Plugin SDK
    implementation(project(":kotlin-backend:plugins-api"))
    implementation(project(":kotlin-backend:core"))

    // Apache Lucene for full-text search
    implementation("org.apache.lucene:lucene-core:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
    implementation("org.apache.lucene:lucene-analysis-common:$luceneVersion")
    implementation("org.apache.lucene:lucene-highlighter:$luceneVersion")

    // Apache Tika for content extraction (PDF, DOCX, etc.)
    implementation("org.apache.tika:tika-core:2.9.2")
    implementation("org.apache.tika:tika-parsers-standard-package:2.9.2")

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
            "Plugin-Id" to "com.vaultstadio.plugins.fulltext-search",
            "Plugin-Version" to version,
            "Plugin-Class" to "com.vaultstadio.plugins.search.FullTextSearchPlugin",
        )
    }
}
