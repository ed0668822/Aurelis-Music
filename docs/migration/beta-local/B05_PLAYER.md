# B05 — MiniPlayer, calidad y disposiciones del reproductor

Base: 4c40785 (B04). Rama: feature/bitchord-local-migration.
PR1: https://github.com/ed0668822/Aurelis-Music/pull/1; título sin cambios.
Autorización B04–B05, reconfirmada por «autorización concedida» tras entrega B04.
No se presupone resultado exitoso de CI de B04. No se consulta Actions ni se
compila Android localmente: publicar, avisar y esperar resultado del usuario.

## Alcance y correspondencia

Se hereda el contrato B05 y sus16 campos en IMPLEMENTATION_BLOCKS, sin reordenar
ni sustituir el plan. Complejidad alta; entrega incremental, sin declarar paridad
completa ni cierre de pruebas. Depende de B04; su CI sigue pendiente de confirmación.

B05.1: AudioQualityInfo tipa procedencia desconocida/metadatos/pista seleccionada.
PlaybackViewModel consulta currentTracks solo con COMMAND_GET_TRACKS disponible,
como parte de su sincronización habitual; no se crea otro observador, reproductor
o conexión. Recoge MIME, sample rate, canales y bitrate medio cuando existen;
profundidad PCM solo para audio/raw con encoding explícito. Múltiples candidatos
adaptativos no permiten inferir el formato activo: fallback a metadatos de archivo.
No infiere FLAC/bit-depth/HiRes desde extensiones ni etiquetas sourceQuality legacy.
No afirma Dolby, bit-perfect ni formato del DAC. El diálogo identifica la fuente
de datos y declara salida no medida. Al desaparecer la canción, calidad desconocida.

MiniPlayer conserva título/artista/imagen, reproducción/pausa, siguiente, acceso al
player y progreso. Forma cápsula, carátula40dp/8dp de esquina, gutter12dp y altura
mínima56dp, guiadas por referencia. La altura puede crecer por accesibilidad y por
la etiqueta de formato adicional. No se eliminan controles existentes para forzar
una captura. Imagen Crop, fallback local y material Liquid Glass conservados.

B05.2: apertura/cierre existentes mantienen AnimatedVisibility y la navegación
activa; se añade swipe ascendente en MiniPlayer y descendente en cabecera, umbral
48dp después del slop, con cancelación sin acción. Visibilidad sigue siendo de
AppNavigator. No mueve ni recrea el servicio o la cola. Letras/fuentes/calidad usan
estado guardable. Esta integración no equivale al panel arrastrable completo del
origen ni al morph de la carátula hasta su dock: quedan pendientes dentro de B05.2.
PlayerDock/PlayerSheetMotion legacy no se conectan parcialmente simulando ese morph.

B05.3: NowPlayingScreen selecciona por restricciones de espacio una disposición
vertical o LandscapePlayerLayout existente. Horizontal usa carátula izquierda y
controles derecha con scroll para pantallas bajas; comparte exactamente las mismas
lambdas de cabecera, metadatos, seek, shuffle, repeat, transporte y errores.
No usa orientación global de pantalla para decidir tamaño de una ventana libre.
Letras y cola conservan los overlays activos; el cambio a paneles laterales del
origen no se presenta como implementado. MeshGradient conserva colores y animación
al cambiar canción, se asienta como referencia en lugar de orbitar continuamente;
reduceAnimation detiene esa animación y las transiciones existentes. Reducir
desenfoque omite el mesh y usa el fondo sólido del tema. No shaders nuevos.

Referencias leídas desde BitChord b690dc146009d4815825fa4a20815ef2e4ee8898, archivo
local de solo lectura: MiniPlayer.kt, LandscapePlayer.kt (métricas/disposición) y
SourceQualityBadge.kt (diferencia entre etiquetas ofrecidas y calidad confirmada).
No se importa su clasificación de streaming o sus símbolos Dolby. NOTICE conserva
procedencia/atribuciones y se actualiza solo para esta adaptación.

## Archivos

- Nuevos: playback/AudioQualityInfo.kt, ui/player/PlayerSwipeTrigger.kt y
  test/playback/AudioQualityInfoTest.kt.
- Modificados: playback/PlaybackViewModel.kt, MainActivity.kt, GlassNavBar.kt,
  MiniPlayer.kt, ui/screens/NowPlayingScreen.kt y test/ui/PlayerIntegrationTest.kt.
- Preservados: servicio, controlador/conexión, cola, DSP, JNI/CMake/ABI, manifiesto,
  Gradle/workflows, firmas, LICENSE/LICENSES, shaders y defensas/tests QA01.
- No nuevas dependencias, binarios, fuentes, imágenes ni proveedores de Internet.

## Doble revisión

Primera pasada estática del mismo agente: un solo estado/controller, getters de
Media3 con permisos de comando, ausencia de cifras inventadas, layouts acotados,
controles reales compartidos, inferencia de tipos/imports. Firmas de Tracks.Group,
Format y COMMAND_GET_TRACKS contrastadas con javap sobre media3-common1.11.1 en
caché; no compilación de Android. Corregido el uso de bitrate a averageBitrate y
el caso de varios formatos seleccionados para evitar atribuirles precisión falsa.

Segunda pasada estática del mismo agente: cancelación de gestos, callbacks actuales
sin cierre obsoleto, contenido en pantallas bajas, alternativas sin GPU, reducción
de animación, calidad desconocida, preservación de QA01/GLASS01 y licencias.
Git diff --check sin incidencias. No se presenta como revisión de otro ingeniero,
compilación exitosa o prueba de funcionamiento. Hashes y alcance en JSON adjunto.

## Validación pendiente en GitHub y Android

Preparadas seis pruebas:4 AudioQualityInfoTest (metadatos/marketing, pista única,
valores desconocidos/PCM y candidatos adaptativos);2 PlayerIntegrationTest (swipes
sin cambiar cola, horizontal con controles y diálogo de origen/salida).
Todas NO EJECUTADAS localmente. Workflow existente ejecutará testDebugUnitTest,
testReleaseUnitTest, lintDebug, lintRelease, assembleDebug y assembleRelease.
También mantiene las regresiones previas de player, navegación y tamaño/attach.

Las pruebas UI son Robolectric con RecordingPlayer: no certifican reproducción
sonora, arranque físico ni rotación de Activity con un servicio Android real.
Instalación, rotación real preservando posición/cola, gestos/taps bajo TalkBack,
320/360/600dp/texto grande, capturas, efectos GPU, Logcat y audio: pendientes.
Parar ante fallo de CI o regresión de reproducción, pérdida de datos o licencia
dudosa. No iniciar B06 hasta autorización; no iniciar QA02.

## Estado y continuación

B05.1 implementado, verificación CI pendiente. B05.2 parcial: entrada/cierre/gestos
conectados; dock/morph y panel interactivo fiel pendientes. B05.3 layouts conectados,
rotación Android y fidelidad visual pendientes; letras/cola laterales no migradas.
No declarar B05 completo ni idéntico a BitChord. Próximo paso: recibir resultado de
GitHub, corregir errores si aparecen y completar los pendientes de B05 autorizados,
sin avanzar B06 automáticamente. El plan principal permanece intacto.
