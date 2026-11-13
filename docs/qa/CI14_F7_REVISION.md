# CI14 F7 — Revisión de la aplicación (Android / distribución)

Fecha: 2026-11-12. Fuente de comprobación: rama feature/bitchord-local-migration. Alcance: revisar H15–H17 sin borrar funciones valiosas; aplicar únicamente cambios concretos y de bajo impacto.

## H15: Media3/FFmpeg — conservar mientras falta prueba funcional

- El reproductor AurelisRenderersFactory.kt utiliza io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FfmpegAudioRenderer, de nextlib-media3ext 1.11.1-0.16.0.
- Los cuatro archivos Java locales están en el paquete diferente androidx.media3.decoder.ffmpeg y no son esa clase importada. La similitud de nombre no demuestra por sí misma una duplicación binaria de clase.
- app/src/main/jniLibs/arm64-v8a/libffmpegJNI.so y código JNI figuran presentes en el proyecto. **No se elimina ni reemplaza ninguna clase/libFFmpeg sin revisar el AAR efectivo y probar audio en hardware**, incluida carga de símbolos nativos.

## H16: org.json — dependencia aún usada

- LrcLib.kt importa org.json.JSONArray y referencia org.json.JSONObject para procesar resultados de letras.
- Android incluye estas APIs pero retirar org.json:json:20240303 de Gradle sin analizar el classpath de tests JVM/Robolectric podría cambiar el comportamiento. Se conserva hasta una eliminación probada con pruebas y análisis del árbol de dependencias.
- Ninguna eliminación se ha afirmado como completada.

## H17: targetSdk — actualización requerida

- Google Play exige desde el 31 de agosto de 2026 que nuevas apps y actualizaciones de teléfonos normales apunten a Android 16, API 36. El proyecto tenía compileSdk=36 y targetSdk=35.
- Se ajustó targetSdk a **36**, manteniendo minSdk=33 y la arquitectura exclusivamente arm64-v8a. La actualización debe validarse por B01–B06; el emulador B05 actual usa Android 15 y no sustituye una prueba real o emulada de Android 16.
- Revisar edge-to-edge, barra de estado/navegación, foreground-service/media playback y permisos de instalación en un dispositivo Android 16 antes de publicación en Google Play.

## Reparación adicional: actualizaciones

- AppUpdateChecker consultaba el repositorio inexistente/antiguo ed0668822/AurelisMusic. Se reemplazó por ed0668822/Aurelis-Music, repositorio real.
- check() consulta releases/latest, **no** los artifacts de GitHub Actions. Solo detectará actualizaciones cuando exista un GitHub Release publicado y firmado por el canal F1, no cuando simplemente termine una compilación de CI.

## Verificación final pendiente

Esperar el pipeline B01–B06 del commit final y auditar ambos APK (Debug/Release), cobertura y Media3. Verificar UI/audio en Android 16 antes de promocionar el targetSdk al canal de publicación protegido. Los cambios de esta revisión son dos ajustes puntuales; los archivos de FFmpeg y la dependencia org.json permanecen sin cambios.
