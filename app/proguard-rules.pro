# Aurelis © 2026. GPL-3.0-or-later.

# ---------------------------------------------------------
# 1. Conservar TODAS las clases de la app
# ---------------------------------------------------------
-keep class com.music.aurelis.** { *; }
-keep interface com.music.aurelis.** { *; }
-keep enum com.music.aurelis.** { *; }

# 2. Conservar métodos nativos (JNI)
-keepclasseswithmembernames class * {
    native <methods>;
}

# 3. Conservar clases de serialización (kotlinx.serialization)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 4. Conservar modelos de datos (data classes) usados en JSON
-keep class com.music.aurelis.data.lyrics.** { *; }
-keep class com.music.aurelis.data.model.** { *; }

# 5. Librerías de terceros que usan reflexión
-keep class org.jsoup.** { *; }
-keep class okhttp3.** { *; }
-keep class androidx.media3.** { *; }
-keep class com.microsoft.onnxruntime.** { *; }

# 5b. Coil 3 (usa ServiceLoader y reflexión para ImageLoader)
-keep class coil3.** { *; }
-keep class coil3.network.** { *; }
-keep class coil3.request.** { *; }

# 5c. Haze (Compose + RuntimeShader)
-keep class dev.chrisbanes.haze.** { *; }

# 5d. NextLib (Media3 extension vendoreada)
-keep class io.github.anilbeesetti.nextlib.** { *; }

# 5e. Guava (reflexión en algunas clases internas)
-keep class com.google.common.** { *; }
-dontwarn com.google.common.**

# 6. Evitar que R8 elimine metadatos de depuración
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 7. Reglas específicas para las librerías nativas y FFmpeg vendorizado
-keep class androidx.media3.decoder.ffmpeg.** { *; }

# 8. Silenciar warnings de Jsoup 1.18+ y JSpecify
-dontwarn org.jspecify.annotations.**
-dontwarn javax.annotation.**
