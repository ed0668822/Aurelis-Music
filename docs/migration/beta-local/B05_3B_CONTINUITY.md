<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B05.3-b — continuidad de tamaño, recreación y fondo

Autorización «Continúa con el siguiente bloque», base
c5139134d9ebc235898c6d02c64013563fdfe57f, misma rama/PR1. Resultado previo
aceptado por el usuario; sin descargar informes. Máximo30 rutas acumuladas.
Manifiesto previsto (ocho archivos):
- app/src/main/java/com/music/aurelis/ui/player/PlayerQueue.kt
- app/src/main/java/com/music/aurelis/ui/screens/QueueScreen.kt
- app/src/main/java/com/music/aurelis/ui/screens/NowPlayingScreen.kt
- app/src/test/java/com/music/aurelis/ui/PlayerResizeContinuityTest.kt
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/B05_3B_CONTINUITY.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

Brecha: el cambio entre cola lateral y modal recomponía InlineQueue con otra
LazyListState. Se comparte un estado guardable desde NowPlayingScreen entre
ambas presentaciones, conservando el estado del controlador, no serializando medios.
Otra brecha: recordar/extractar paleta se ejecutaba aun omitiendo el mesh por
reduceBlur. Se evita pedir la paleta mientras no se dibuja ese fondo.
No se reescribe MeshGradient, su fase, renderer, servicio ni política de permisos.

## Implementación y alcance

InlineQueue permite recibir LazyListState (por defecto conserva el existente)
y scrollToCurrentOnAttach=true. QueueScreen recibe opcionalmente ese estado y,
si lo recibe, respeta su viewport al entrar en vez de recentrarlo en la canción
actual. NowPlaying mantiene rememberLazyListState fuera de ambas ramas y lo pasa
a las dos presentaciones. LazyListState.Saver guarda índice/offset, sin canciones,
carátulas o callbacks. Otros consumidores conservan su foco inicial anterior.

La selección de foco sigue reaccionando a cambios de canción/índice/historial;
reduceMotion ya no dispara un recentrado por sí sola. No cambian comandos ni orden
de Media3. La paleta solo se prepara si el fondo mesh va a mostrarse; reduceAnimation
sigue parando deriva/crossfade con el código existente. No nuevo renderer ni shaders.

Al pasar la cola lateral a modal, el reproductor subyacente se retira de semántica
y el scrim consume taps fuera de la cola, evitando activar controles de fondo.
La regresión comprueba que cerrar reproductor no queda expuesto bajo ese modal.

## Pruebas y dos revisiones

PlayerResizeContinuityTest prepara dos regresiones para Actions sobre grafo activo,
LibraryViewModel/PlaybackViewModel y RecordingPlayer, sin capturas guardadas:
1. Cola con40 canciones, desplazamiento al tema25, restricciones800x360 →360x800
   →800x360 y restauración con StateRestorationTester: viewport/panel conservados.
2. Panel de letras, cambios de efectos/tamaño y restauración: selección del panel
   conservada; mesh presente/omitido según reduceBlur.
En ambos se exige cola con mismas ocurrencias, posición12345ms, pausa y ausencia
de nuevos prepare/replace al cambiar la interfaz. Se restauran los ajustes al salir.
Son cambios de restricciones/restauración Compose, no una certificación de rotación
física/servicio Android; dispositivos, GPU y fidelidad visual siguen pendientes.

Dos revisiones del mismo agente: continuidad de composición/viewport, foco y claves,
callbacks/estado de playback; después diff completo, tipos/imports, fixtures,
preferencias, atribuciones y ocho rutas. git diff --check local. Sin ejecutar Gradle,
Lint, APK o tests Android localmente. Nuevas pruebas/CI quedan pendientes por SHA.
La navegación de paneles/letras ya conserva sus flags; no se reimplementan. El scroll
manual/seguimiento de letras conserva los pendientes registrados de B07; no se declara
resuelto por estas pruebas. No se cierra B05 físicamente ni se inicia B06/B09.
Tras enviar, avisar y esperar resultado del usuario antes de avanzar.
