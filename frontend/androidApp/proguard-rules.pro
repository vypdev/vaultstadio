# VaultStadio Android ProGuard Rules

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.vaultstadio.**$$serializer { *; }
-keepclassmembers class com.vaultstadio.** {
    *** Companion;
}
-keepclasseswithmembers class com.vaultstadio.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# Keep Koin
-keepnames class org.koin.**
-keep class org.koin.** { *; }

# Keep Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep data classes
-keep class com.vaultstadio.shared.domain.model.** { *; }
-keep class com.vaultstadio.shared.network.** { *; }
