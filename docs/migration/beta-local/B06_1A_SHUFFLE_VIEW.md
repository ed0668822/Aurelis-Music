<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B06.1-a — orden aleatorio visible y reversible

«Continúa» confirma B05.3-b y autoriza el siguiente bloque. Base
2e85dc5959f46e217c438a57571651406d3455cc, misma rama/PR1. Sin descargar ZIP.
Máximo30 archivos distintos acumulados; manifiesto previsto (once):
- app/src/main/java/com/music/aurelis/playback/QueuePlaybackOrder.kt
- app/src/main/java/com/music/aurelis/playback/PlaybackViewModel.kt
- app/src/main/java/com/music/aurelis/ui/player/PlayerQueue.kt
- app/src/main/java/com/music/aurelis/ui/screens/QueueScreen.kt
- app/src/main/res/values/strings.xml
- app/src/test/java/com/music/aurelis/playback/QueuePlaybackOrderTest.kt
- app/src/test/java/com/music/aurelis/ui/PlayerIntegrationTest.kt
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/B06_1A_SHUFFLE_VIEW.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

Referencia BitChord fija b690dc1, playback/QueueShuffle.kt: el orden mostrado debe
coincidir con el seguido y el toggle debe poder revertirse sin reiniciar la canción.
Adaptación delimitada: Aurelis conserva el shuffle nativo Media3 y lee su Timeline;
no copia el objeto global/crossfade/canonicalContext ni reordena la cola desde UI.
Se proyectan índices físicos según el orden expuesto. Desactivar shuffle vuelve al
orden físico intacto. El prefijo/historial representa posiciones del ciclo mostrado,
no certifica eventos de escucha; repeat/wrap siguen a cargo de Media3. B06.1-b sigue
pendiente para Play Next y edición bajo shuffle, incluidos USER_QUEUE y sucesor.

Referencia consultada, SHA256 QueueShuffle.kt:
06fb7c84d7b66fabfa1c361ed6e5ab1a8a7f40b9c52d1b37edd544e16a6f3985.
No se afirma copia del algoritmo: Media3 conserva aleatoriedad/repeat/transporte.

## Integración y límites

PlaybackState añade queuePlaybackOrder nullable. syncState lo lee del Player
conectado junto al snapshot de medios; al desconectar se invalida. El helper recorre
Timeline en una pasada con repeatOFF y shuffle true; exige permutación completa,
acotada y sin ciclos, o devuelve desconocido. Shuffle off devuelve orden físico.

splitQueue acepta la proyección, mantiene timelineIndex físico y queueEntryId por
ocurrencia. La cola activa plana (sin agrupar tiers) la utiliza; jump/remove apuntan
a los mismos índices del controlador. Con shuffle activo se retiran drag/acciones
accesibles de reordenación y se muestra explicación; elegir/quitar siguen posibles.
No se promete que un move físico cambie el sucesor oculto. Con orden desconocido se
muestra aviso en vez de una cola inventada. Desactivar restaura la vista física sin
reset/prepare/seek. Play Next con shuffle sigue bloqueado y explicado hasta B06.1-b.
Este bloque no implementa el pinning USER_QUEUE ni cambia persistencia o servicio.

## Pruebas y dos revisiones

Seis regresiones preparadas para Actions:
- ExoPlayer con permutación fija: vista coincide con nextMediaItemIndex y toggle
  restaura orden físico, medio actual/posición/pausa sin prepare.
- Selección manual de ocurrencia repetida: índice físico correcto y sucesor real.
- Orden cíclico/inválido/incompleto rechazado y Timeline.EMPTY permitido.
- Shuffle sin permiso de Timeline es desconocido, sin asumir orden secuencial.
- MediaSession/MediaController reciben la misma permutación y desactivan shuffle.
- Interfaz real con RecordingPlayer: selección mantiene identidad, reordenación no
  se ofrece bajo shuffle, desactivar conserva canción/posición y contadores.

ExoPlayer de tests no prepara audio ni abre contenido; no se afirma decodificación.
Se inspeccionaron con javap las APIs DefaultShuffleOrder(int[],long), ForwardingTimeline
y ExoPlayer.setShuffleOrder en Media3 existente1.11.1: inspección, no compilación.
Dos revisiones del mismo agente: permisos/permutación/índices/duplicados/servicio
único; después diff/API/imports/fixtures/UI/capacidades y once rutas de máximo30.
git diff --check local; XML de recursos parseado. Sin Gradle/Lint/tests Android
locales. CI nuevo, seis pruebas y dispositivo físico pendientes. No se cierra B06.1
completo ni se inicia B06.1-b/B06.2/B09 hasta la siguiente autorización del usuario.
Tras push avisar y esperar; no crear Release oficial ni descargar ZIP preventivo.


## Corrección de run 131

Run 43632198693, commit 627749c7885417b430b73aec4a371ef6f08b6f4e: FAILURE.
Informe original Aurelis-Informes-B01-B06-131 descargado y ZIP verificado sin errores.
B01/B02/B05 SUCCESS; B03/B06 FAILURE; B04 SKIPPED. results.json registra cero
pruebas y cero XML en ambas variantes: no llegaron a ejecutarse las suites.
Debug/debug.log:156 y Release/release.log:154 identifican el mismo error de
compilación en QueuePlaybackOrderTest.kt:104: «No type arguments expected for
static fun getApplication(): Application!». La llamada de Robolectric no es genérica.
Se retira únicamente el argumento de tipo; conserva Application inferido y toda
la prueba MediaSession/MediaController. No se altera producción ni las puertas CI.
La cobertura omitida y las puertas finales no demuestran otro defecto de cobertura.

Corrección en tres archivos ya incluidos en el bloque: test, este informe y tracker.
Acumulado sigue en once archivos distintos, máximo30. Dos revisiones estáticas del
mismo agente: diagnóstico comparado entre variantes y firma/call sites; después diff,
alcance y conservación de las seis regresiones. git diff --check pasa. No Gradle,
Lint ni pruebas Android locales. Compilación y ejecución nuevas pendientes de Actions.
