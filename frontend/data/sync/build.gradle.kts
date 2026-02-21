plugins { alias(libs.plugins.kotlin.multiplatform); alias(libs.plugins.kotlin.serialization); alias(libs.plugins.android.kotlin.multiplatform.library) }

val libNamespace = "com.vaultstadio.app.data.sync"

kotlin {
  jvm("desktop")
  android {
    namespace = libNamespace
    compileSdk = 36
    minSdk = 24
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
  }
  wasmJs { browser() }
  iosArm64(); iosSimulatorArm64()
  sourceSets {
    val commonMain by getting {
      kotlin.srcDirs("src/main")
      dependencies {
        implementation(project(":domain:result"))
        implementation(project(":domain:sync"))
        implementation(project(":data:network"))
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.datetime)
        implementation(libs.ktor.client.core)
        implementation(libs.koin.core)
      }
    }
  }
}
afterEvaluate { group = libNamespace }
