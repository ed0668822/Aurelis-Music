# TEST_STRATEGY

## Comandos y evidencia

Usar JDK17/SDK36 del entorno mediante `source /workspace/toolchains/activate.sh`.
Gradle wrapper se ejecuta con bash. U = assembleDebug + testDebugUnitTest + lintDebug;
N = externalNativeBuildDebug, además de U; D = connectedDebugAndroidTest;
R = assembleDebug/assembleRelease + testDebugUnitTest/testReleaseUnitTest +
lintDebug/lintRelease. Siempre `--no-daemon --max-workers=4`.

Para acreditar pruebas nuevas, usar la opción de tarea `--rerun` en las tareas
de tests; no forzar recompilación de todo el proyecto sin causa. Distinguir
UP-TO-DATE, casos ejecutados, compilación, análisis estático y dispositivos.
La evidencia actual y comandos exactos quedan en B00_AUDIT y B00_EVIDENCE.json.

## Datos

Migración perfil único desde cero/uno/varios; fallo/reinicio y recuperación privada;
lectura de versión desconocida/corrupción sin sobrescritura; permisos denegados y
revocados; SAF/SD/volúmenes; archivos movidos/eliminados/modificados; géneros/tamaños
ausentes; búsqueda con acentos; favoritos/playlists/cola preservados. Importaciones
inválidas o interrumpidas conservan datos anteriores. No aproximar identidad por título.

## Playback y servicios

Una sesión/servicio/estado público. Play Next, duplicados, drag/remove, cola pausada
restaurada, pausa/seek/next durante transición, foco/llamada/auriculares/Bluetooth,
notificación/bloqueo, temporizador/velocidad/silencio/autoplay local. Contabilidad
monotónica: seek no es escucha; global no se duplica al mezclar. Android Auto requiere
contrato de biblioteca y controladores autorizados probados, no solo intent-filter.

## Letras y red

Fixtures LRC/enhanced/TTML con timestamps inválidos, cambios de canción, offset,
lectura manual, caché/offline/timeout/404/429 y resultados equivocados. Cliente de
traducción permitido, cancelación y límites. Tokens revocados/reintentos y outbox
idempotente de scrobbling. No confundir respuesta HTTP con autorización legal.

## Audio y AutoMix

Señales PCM conocidas para EQ/balance/normalización/limitador/canales; formato
origen y salida conocido/desconocido; BPM conocido y confianza; ONNX configurado
y perfilado1/2/3; liberación de sesiones; caché invalidada al cambiar archivo/modelo.
Crossfade/LoopMix con archivos reales y análisis ausente/fallido. Medir CPU/memoria,
termal/batería, priorización de reproducción y cancelación. Validar bit-perfect en
hardware, no inferirlo de un badge. ZIP/ELF16KB, ARM64, JNI y R8 en release.

## UI y cierre

Compose/rotación/restauración de rutas activas; goldens con referencia y diferencias
aprobadas; accesibilidad; archivos musicales sin datos de ejemplo en producción.
U por unidad, N si JNI cambia y R al cierre o cambios relevantes de release. D y
pruebas físicas adicionales. CI por SHA exacto; no heredar verde de run histórico.
No tolerar pérdida de datos, cambio de identidad, exclusiones activas o defecto
crítico de reproducción. Hardware/no disponible = pendiente. Dos revisiones separadas:
1 arquitectura/lógica/integración/build; 2 UI/comportamiento/licencias/regresiones.
En B00 serán dos pasadas del mismo agente, sin alegar revisores humanos independientes.
