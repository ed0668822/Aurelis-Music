<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B05.3-a — letras y cola en panel horizontal

«Continúa con el siguiente bloque» confirma la reparación previa y autoriza este
bloque. Base44c0c9dbb84c306bc17df8c2c057218671f155a9, misma rama/PR1.
No se descarga ZIP del bloque previo. Máximo30 rutas acumuladas.
Manifiesto previsto (ocho archivos):
- app/src/main/java/com/music/aurelis/ui/screens/NowPlayingScreen.kt
- app/src/main/java/com/music/aurelis/ui/screens/QueueScreen.kt
- app/src/main/java/com/music/aurelis/ui/player/LandscapePlayer.kt
- app/src/test/java/com/music/aurelis/ui/PlayerIntegrationTest.kt
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/B05_3A_SIDE_PANES.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

Referencia local fija BitChord b690dc146009d4815825fa4a20815ef2e4ee8898:
sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/LandscapePlayer.kt,
SHA256 04ccd317d0e37822dbc0fd3b5ea041c36a296c396af20c3e91f7d15774ca013d;
NowPlayingScreen.kt, SHA256
1d5ceae1f1c045881a49601f3fa535ecd4cf8efe45837fc97990045e3ed9024e.
Se adapta la elección Main/Lyrics/Queue del layout ya portado, reutilizando los
estados y callbacks Aurelis. No se copian archivos completos ni funciones remotas.
La base pasaba Lyrics/Queue vacíos y Main constante: brecha conectada aquí.

## Implementación

La disposición se decide por restricciones, no por orientación global. Main/Lyrics/
Queue activa el slot derecho correspondiente; cabecera y carátula permanecen a
izquierda. Al abrir un panel se conserva transporte con mismas capacidades,
reducción de animación y contraste. Atrás/cerrar panel regresa al principal; abrir
otro panel cierra el anterior. La cola local usa un booleano guardable en vez de
crear otra ruta/servicio; el acceso de cola en portrait conserva su ruta original.
Si se cambia a portrait con un panel abierto, el mismo contenido usa su presentación
completa. La validación adicional de recreación/rotación/fondos pertenece a B05.3-b.

QueueScreen añade embedded=false; el contenido se extrae una sola vez para modal
y lateral: mismo PlaybackViewModel, currentIndex, orden real Media3 y comandos
seek/remove/move/clear con isConnected/canEditQueue. Las letras se extraen como
contenido composable reutilizado sin alterar estado, prioridad local/caché/red,
lookup/traducción/import/offset ni cálculo de seek. No se conecta el wrapper legacy
QueueSidePanel que fuerza controlsLocked=false ni se crea un segundo repositorio.

LandscapePlayerLayout añade reduceMotion=false y omite fades cuando se solicita;
los lectores de pantalla conservan la cabecera izquierda accesible cuando el panel
lateral está abierto. Las acciones izquierdas pueden desplazarse en ventanas bajas.

## Pruebas y revisiones

Dos regresiones en PlayerIntegrationTest, preparadas para Actions, 800x360dp:
- Cola lateral, jump/removal reales por RecordingPlayer, cambio a letras y Atrás,
  sin prepare/replace adicionales al navegar entre paneles.
- Import LRC propio, panel lateral, seek con offset500ms, mismas ocurrencias de cola
  y contadores de prepare/replace; cerrar letras conserva reproductor horizontal.
Sin capturas nuevas. RecordingPlayer no certifica audio ni permisos físicos.
Se conservan las pruebas previas portrait/cola/calidad/lyrics/traducción/QA01.

Dos revisiones del mismo agente: grafo/Back/capacidades/estado único y contenido
compartido; luego diff completo, lambda scopes/imports/API, selectores de pruebas,
semántica/animación reducida, atribuciones y ocho rutas de máximo30. git diff --check
local. Sin Gradle/Lint/APK/tests Android locales: nuevo CI y ambas regresiones
pendientes. No se declara paridad visual/GPU/rotación física ni se cierra B05.
B05.3-b, B06 y B09 no se inician. Tras envío, avisar y esperar resultado del usuario;
«continúa» confirma éxito y autoriza siguiente bloque, sin descargar ZIP preventivo.
