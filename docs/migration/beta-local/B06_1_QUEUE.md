# B06.1 — coordinador y cola local

Estado: implementado, pendiente de compilación/pruebas en GitHub y comprobación
Android real. Autorización de B06; entrega limitada a B06.1 para validar antes de
B06.2. Base `cefe533dd1e256c67a720a97a241e95789c1fa9d`, misma rama y PR1 («Aurelis Music Beta UI and components»).
No reinicio ni sustitución del plan; B06/B05/paridad no se declaran terminados.

1. **Objetivo:** Play Next local, edición de cola en sitio y misma canción/posición.
2. **Dependencias:** B05 entregado; puerta CI05 anterior confirmada por run139 SUCCESS
   del mismo HEAD. Esto acredita CI previo, no reproducción ni GPU en un teléfono.
3. **Referencia:** F08/F09. BitChord fijo b690dc146009d4815825fa4a20815ef2e4ee8898,
   `app/src/main/java/com/music/bitchord/playback/QueueCoordinator.kt`,
   `app/src/main/java/com/music/bitchord/ui/components/SongActionsMenu.kt` y
   `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerQueue.kt`.
   Copia de referencia leída sin modificar; no importación de lógica remota/party.
4. **Existentes modificados:** playback/{QueueCoordinator,PlaybackViewModel,
   MediaItemExtensions,PlayerConnection,ProfilePlaybackBinding}.kt;
   MainActivity.kt; ui/components/SongActionsDialog.kt;
   ui/screens/{NowPlayingScreen,QueueScreen}.kt; ui/player/PlayerQueue.kt;
   tests/{RecordingPlayer,PlaybackIntegrationTest,MediaSessionBridgeTest,
   PlayerIntegrationTest,SharedGesturesTest}.kt. Lista exacta/hashes en JSON.
5. **Nuevos:** QueueCoordinatorTest.kt y estas evidencias MD/JSON.
6. **No tocar:** servicio AurelisPlaybackService, motores, DSP/ONNX/JNI/ABI, firma,
   manifest, dependencias, Liquid Glass/QA01, workflows, LICENSE y plan maestro.
   NOTICE y registros se amplían para trazabilidad, sin sustituir textos legales.
7. **Arquitectura:** se completa QueueCoordinator existente en lugar de duplicarlo
   con el LocalPlaybackCoordinator previsto. Solo reenvía comandos al Player
   conectado: no posee lista canónica, motor ni sesión adicionales. Media3 conserva
   identidad/posición con moveMediaItem/removeMediaItem/addMediaItems, sin reset/seek.
   La creación de una ocurrencia genera UUID en extras; mover conserva ese mismo
   MediaItem. Restaurar genera IDs nuevos sin cambiar el formato privado guardado.
   Las adiciones tienen su propia cadena FIFO; un nuevo playSongs/AutoMix invalida
   ediciones pendientes anteriores. No sobrescriben ya el Job del último playSongs.
8. **Conexiones:** menús activos de biblioteca/Home y reproductor → playNext/enqueue;
   QueueScreen → InlineQueue existente → comandos del mismo ViewModel/Player.
   Lista plana según timeline real: no presentar grupos USER_QUEUE antes de contextos
   que realmente se reproducirían primero. Arrastre entre próximas ocurrencias;
   acciones accesibles subir/bajar, incluida la fila actual, y eliminación individual
   conservando la actual. Motion reducido, hit targets de48dp y controles bloqueados
   sin conexión/comando. La operación de limpiar USER_QUEUE conserva contexto/historial;
   el renderer agrupado la admite, la presentación plana no muestra ese botón.
9. **Riesgos de compilación:** Compose semantics/drag y contratos SimpleBasePlayer;
   Media3 Commands.Builder.remove inspeccionado con javap. Compilación pendiente CI.
10. **Licencias:** renderer existente adaptado de BitChord GPLv3; procedencia y cambios
    añadidos a NOTICE y al archivo. No dependencias/binarios/fuentes nuevas; riesgos
    legales pendientes de B00/B09/B10 se conservan, no se dan por resueltos.
11. **Pruebas:** 21 casos nuevos preparados:11 coordinador/metadata/presentación,
    4 ViewModel/conexión,1 puente real MediaSession/MediaController bajo Robolectric,
    3 Compose de integración y2 menús. Todos los nombres de pruebas previas se
    conservan; dos comprobaciones completas de metadatos ahora separan el UUID nuevo
    y lo exigen no nulo. Harness RecordingPlayer usa UID único por inserción y mantiene
    la actual al insertar antes; contadores detectan reset/prepare/seek adicionales.
12. **Comandos:** exclusivamente en GitHub, workflow android.yml existente:
    `bash gradlew :app:compileDebugKotlin :app:compileReleaseKotlin --stacktrace`;
    `bash gradlew :app:testDebugUnitTest :app:testReleaseUnitTest --stacktrace`;
    `bash gradlew :app:lintDebug :app:lintRelease --stacktrace`;
    APK debug/release según las puertas actuales del workflow. No ejecutados aquí.
13. **Aceptación:** orden/identidad estable, repetidos distinguibles, pausa/posición
    intactas, solicitudes FIFO sin devolver una selección antigua, comandos revocados
    sin editar; menús y drag activos. CI más validación Android necesarios para cerrar.
14. **Parada:** fallo CI, pérdida de estado, licencia nueva no acreditada o conflicto
    con reproducción principal. Publicar, avisar y esperar resultado del usuario;
    no consultar automáticamente la nueva ejecución ni pasar a B06.2 en esta entrega.
15. **Resultado:** integración de B06.1 revisable en PR existente. Pendientes:
    Play Next con shuffle se deshabilita y explica: insertar físicamente después de
    actual no garantiza el sucesor aleatorio. Requiere política dentro de sesión,
    no apagar shuffle en secreto ni prometer soporte. Hardware/sonido/arranque/GPU,
    cancelación/auto-scroll de drag y notificaciones sin verificar en Android.
16. **Complejidad:** alta; B06 total muy alta y B06.2/.3/.4 permanecen pendientes.

## Revisión1 — arquitectura y lógica

Pasada estática del mismo agente: uso único de Player/servicio, validaciones de
comandos/índices, metadatos, duplicados y solicitudes concurrentes. Corregidas en
esta revisión la separación de jobs de reproducción/adición, la identidad del
harness al insertar y la divergencia entre grupos visuales y timeline real.
`git diff --check` correcto; APIs leídas con javap. No compilación Android local.

## Revisión2 — integración, interfaz, licencia y regresiones

Segunda pasada estática del mismo agente: ambos menús conectados, renderer existente
activo, acción accesible en la fila actual, ausencia de botón de eliminación inerte
en la actual, motion reducido y tamaños táctiles; tests anteriores conservados.
No cambio a workflows/servicio/efectos/plan/firmas. Atribución incorporada sin
reemplazar licencia. Estas revisiones no equivalen a pruebas ejecutadas en Android.

La evidencia estructurada identifica todos los casos y hashes. Compilación, tests,
capturas, Logcat y sonido de esta entrega siguen pendientes; no declarar éxito.
