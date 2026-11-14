# Estado de integración y publicación

**Verificación local completada; migración completa y publicación final pendientes.**
B12 y seguimiento de B13 autorizados e integrados el 5 de noviembre de 2026.
B14 conserva los pendientes legales, físicos y remotos indicados abajo.
No se hace merge ni se publica un APK como versión final.

| Área | Evidencia | Estado |
|---|---|---|
| Debug | assembleDebug, 126 casos, lint0 errores/218 advertencias, firma/ZIP | Correcto local |
| Release | assembleRelease con R8, 126 casos, lint0 errores/217 advertencias | Correcto local |
| Firma release | Certificado Android Debug por fallback original | Desarrollo; no producción |
| Identidad | com.music.aurelis, Aurelis Music, min33/target35/compile36, ARM64 | Conservada |
| Artefactos | APK_EVIDENCE.json, nueve textos legales y doce .so | Verificados localmente |
| Runtime | Reproductor/MediaSession/MediaStore y UI probados con entornos controlados | No prueba física |
| B12 | Gestión de perfiles, datos/ajustes/cola aislados; legado y música conservados | Implementado y probado localmente |
| Traducción | Cliente configurable y caché, sin servidor desplegado/configurado aquí | Prueba en vivo pendiente |
| AutoMix | Cola local finita por metadatos, sin BPM inventado | Sin crossfade/beatmatching |
| Licencias | NOTICE corregido, textos completos e inventario POM | Cierre legal pendiente |
| CI | actionlint/YAML/bash válidos; SDK36 y gate de errores corregidos | Push B12/B13 confirmado; CI del commit exacto pendiente (#117 es anterior) |

## Cambios de integración

La variante release no contenía la actividad usada por Robolectric. Una regla de
prueba la registra en memoria antes de ActivityScenario, sin modificar el manifest
de producción ni excluir ninguno de los 114 casos. El APK release no contiene
ese registro ni la actividad de test declarada; el helper no está en DEX.

CI conserva informes tras errores, pero falla al final si lint, tests o build
fallan. PRs con commits sustituidos se cancelan para no duplicar trabajo; el
último conserva todas las comprobaciones, y main/manual no comparten ese grupo.
No se cambian secretos, permisos del workflow, publicación ni configuración de firma.

La dependencia NextLib 1.11.1-0.16.0 declara GPLv3 en su POM, corrigiendo el Apache
incorrecto de NOTICE. Checker-qual declara MIT. LICENSE/NOTICE y siete textos
originales de terceros se empaquetan sin prefijos/copyright propios. Inventario
DEPENDENCIES_RELEASE.tsv refleja metadatos declarados, incluyendo BOMs y aliases;
no implica 175 binarios ni sustituye auditoría de contenido/vulnerabilidades.

## Pendientes necesarios para el cierre

1. B12 implementado y validado localmente. Validar también en dispositivo el cambio
   de perfil con servicio activo y SAF, reinicio, cola pausada y borrado confirmado.
   La música y cachés compartidas se conservan; los perfiles locales no son login.
2. Incorporar **Formato de licencias.csv**, solicitado en B01, antes de aplicar
   cabeceras definitivas propias. Los avisos de terceros se conservan.
3. Registrar procedencia exacta/exportación de los dos ONNX existentes y acreditar
   fuentes correspondientes de binarios enlazados (incluido FFmpeg/NextLib).
   README Beat This! declara MIT de código/pesos; no se confunde umxl no comercial
   con umxhq declarado en la app. No se certifica cada binario por su nombre.
4. Probar en Android real/ARM64: permisos y revocación, biblioteca vacía/grande,
   archivos movidos/eliminados, reproducción larga/segundo plano, notificación y
   bloqueo, foco/auriculares/Bluetooth, DSP escuchado y cambios de curva sin
   transitorios molestos, SAF, letras/offset, accesibilidad y consumo.
5. Validar servicios elegidos con permiso (LRCLIB y servidor de traducción) y CI
   del commit exacto. Firma de publicación queda a cargo de la configuración
   autorizada del usuario; no se crean ni reemplazan claves en esta sesión.

## Rendimiento y límites de la evidencia

AutoMix se ejecuta en Dispatchers.Default sin leer/decodificar audio. Microprueba
JVM17, heap máximo512MiB, 10 000 temas sintéticos/800 artistas y salida20: 494.62ms
en calentamiento; 311.24ms y 194.72ms siguientes, con tamaño/IDs únicos comprobados.
No mide Android, batería ni rendimiento percibido en catálogo físico.

El DSP tiene pruebas PCM espectrales y continuidad por bloques; la UI tiene
capturas reales de Compose con datos de fixture. Esas pruebas no acreditan un
renderizador/decoder/dispositivo real ni ausencia de transitorios audibles.
Consultar PROGRESS y BUILD_STATUS antes de continuar; los éxitos históricos de CI
no validan automáticamente el último commit.

Las huellas de APK_EVIDENCE corresponden al build local v1.9.0-dev y certificado
de desarrollo. CI puede usar versión por número de ejecución y firma configurada
del usuario, por lo que no se espera igualdad de bytes con ese build local.
