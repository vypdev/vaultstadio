plugins {
    alias(libs.plugins.kotlin.jvm)
    jacoco
}
kotlin { jvmToolchain(17) }
val libGroup = "com.vaultstadio.core"
afterEvaluate {
    group = libGroup
    tasks.jar.get().archiveBaseName.set("core-${project.name}")
}
dependencies {
    implementation(project(":domain:share"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.bundles.testing)
}
tasks.test { useJUnitPlatform() }
tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
