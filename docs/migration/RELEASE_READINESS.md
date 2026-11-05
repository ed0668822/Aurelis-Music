# Estado de integración y publicación

**Verificación local completada; migración completa y publicación final pendientes.**
B13/B14 autorizados expresamente después de B11; B12 no autorizado ni implementado.
No se hace merge ni se publica un APK como versión final.

| Área | Evidencia | Estado |
|---|---|---|
| Debug | assembleDebug, 114 casos, lint0 errores/211 advertencias, firma/ZIP | Correcto local |
| Release | assembleRelease con R8, 114 casos, lint0 errores/210 advertencias | Correcto local |
| Firma release | Certificado Android Debug por fallback original | Desarrollo; no producción |
| Identidad | com.music.aurelis, Aurelis Music, min33/target35/compile36, ARM64 | Conservada |
| Artefactos | APK_EVIDENCE.json, nueve textos legales y doce .so | Verificados localmente |
| Runtime | Reproductor/MediaSession/MediaStore y UI probados con entornos controlados | No prueba física |
| B12 | Solo perfil único legado y ámbitos base | Pendiente, sin autorización |
| Traducción | Cliente configurable y caché, sin servidor desplegado/configurado aquí | Prueba en vivo pendiente |
| AutoMix | Cola local finita por metadatos, sin BPM inventado | Sin crossfade/beatmatching |
| Licencias | NOTICE corregido, textos completos e inventario POM | Cierre legal pendiente |
| CI | actionlint/YAML/bash válidos; SDK36 y gate de errores corregidos | Run del último commit por verificar |

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

1. Autorizar y completar B12: creación/cambio/eliminación confirmada de perfiles,
   preferencias, favoritos, listas, historial/cola aislados e integración de servicio.
   Conservar datos legacy; no repetir la auditoría inicial.
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
