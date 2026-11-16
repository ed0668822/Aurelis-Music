<!-- Aurelis © 2026. GPL-3.0-or-later -->
# Reproductor y letras — transición entre canciones

Base publicada d181c97a. El usuario autoriza mejorar completamente la presentación
activa de reproductor/letras y corregir el parpadeo al cambiar canciones. Entrega
máxima60 archivos; conservar sesión y funciones locales, referencia fija de BitChord.
Esta implementación adapta las rutas realmente conectadas: NowPlayingScreen y
ui/player/SyncedLyricsPanel; no basta editar el LyricsScreen legacy sin llamadas.

## Cambios

- Carátula del player y MiniPlayer: mantienen el frame decodificado mientras llega
  el sucesor; transición220ms, inmediata con reducción de movimiento. Sin imagen o
  fallo se resuelve al placeholder, sin dejar la carátula incorrecta indefinidamente.
  Decodificación128px para mini y1024px para player. El placeholder no invierte su
  gradiente al pasar del50% de progreso. Mantiene dock/gestos y recorte existentes.
- Paleta: ya no se reinicializa con cada URI antes de terminar la extracción.
  Conserva color anterior durante carga y pasa al siguiente con el fade de mesh.
  Cancelación y generación impiden que una petición superada publique otro frame
  o paleta. No se toca Media3 ni se reinicia reproducción para animar la interfaz.
- Letras: fondo mesh del player, cabecera con canción/artista, controles de cola y
  transporte sobre la sesión activa; importación/búsqueda en herramientas cuando
  hay letra (si está vacía siguen visibles), traducción/offset conservados.
  Tipografía34/41sp y blanco activo/color tenue con transición como referencia;
  manual/follow, foco central, blur, voz secundaria, TTML/LRC y traducción preservados.
  No se duplican transportes al abrir letras en horizontal. Textos propios ES/EN.
- Compartir: pulsación larga elige una línea; botón comparte la línea actual según
  offset (o primera válida), máximo250 caracteres. Traducción adjunta solo si cabe
  en el presupuesto. Sheet real con PNG de texto ajustado, título y artista;
  normal1080x1350 e historia1080x1920. Se corrige el callback antes inerte de acciones.
  Exportación por FileProvider únicamente del subdirectorio de imágenes compartidas;
  nombres únicos y permiso de lectura. Atrás cierra primero compartir; cambiar de
  canción/cerrar player descarta la solicitud. Guardado fallido no comunica éxito.

## Evidencia y alcance

Mecanismo encontrado en código: remember(artworkUri) construía una paleta de reserva
por cada URI antes de obtener la paleta nueva; dos cargas distintas y AsyncImage
reiniciaban la presentación de portada. Se corrige ese mecanismo. No se declara
que todo destello del dispositivo haya sido reproducido o eliminado físicamente.
No se copian literalmente miles de líneas/dependencias de la app remota: se adaptan
patrones y medidas del commit fijado a la arquitectura local de Aurelis.
No incorporar streaming, Cast, Canvas/vídeo externo, TV, Auto ni Wear.
No cambiar proveedores ni condiciones verificadas, parsers, filtros ABI, SDK o firma.

Se preparan3 pruebas del estado de transición (retención, carga tardía, ausencia/
fallo),1 prueba Robolectric de tarjeta/story y PNG mediante FileProvider, y se
amplía la integración de letras: pulsación larga, sheet, Atrás y posición conservada.
Se mantienen pruebas existentes de seek con offset, bilingüe, cola, gestos, volumen,
rotación y transporte compacto. No se ejecuta Gradle/Android/Lint localmente.
Dos revisiones estáticas del mismo agente, no independientes: API/imports/callbacks
activos y cancelación/compartir/semántica/variantes. Parser Kotlin, XML, referencias
y formatos ES/EN y diff-check se comprueban localmente; no acreditan compilación.

Pendiente de GitHub Actions para el SHA nuevo, comparación visual completa con
BitChord y prueba física de cambios rápidos de canción en Redmi Note14 Pro5G,
Android16/HyperOS3. No certificar identidad visual de todo el player por estas
comprobaciones. Los efectos avanzados de partículas/glow de referencia, selección
múltiple de versos y exportación visual idéntica conservan evaluación pendiente de
B07; compartir actual es una línea real. Esta entrega no cierra B07/B13.
Después de push avisar y ESPERAR resultado comunicado por usuario, sin vigilar
conclusiones ni avanzar automáticamente. B09 permanece sin iniciar.

## Procedencia

BitChord GPLv3, commit b690dc146009d4815825fa4a20815ef2e4ee8898, solo lectura.
Se conservan atribuciones; no relabel GPLv3 como or-later. Loader, protección de
solicitudes, renderer PNG y recursos propios son aportaciones Aurelis.

- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/NowPlayingScreen.kt
  SHA256: 1d5ceae1f1c045881a49601f3fa535ecd4cf8efe45837fc97990045e3ed9024e
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerLyrics.kt
  SHA256: 41d28e9c1513243362487e7f425f35f55af1aaa7bcf06ffc9688146ea21214b8
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/LyricsShare.kt
  SHA256: 36c1e6bb450600c61685f02de83bd68b8ca7101020bed1621b1a2b8feaa71572

## Inventario:19 archivos

- NOTICE
- app/src/main/java/com/music/aurelis/ui/components/AnimatedArtwork.kt
- app/src/main/java/com/music/aurelis/ui/components/MiniPlayer.kt
- app/src/main/java/com/music/aurelis/ui/player/AndroidLyricsShareSheet.kt
- app/src/main/java/com/music/aurelis/ui/player/ArtworkTransition.kt
- app/src/main/java/com/music/aurelis/ui/player/LyricsShareRenderer.kt
- app/src/main/java/com/music/aurelis/ui/player/LyricsShareUtils.kt
- app/src/main/java/com/music/aurelis/ui/player/MeshGradient.kt
- app/src/main/java/com/music/aurelis/ui/player/SyncedLyricsPanel.kt
- app/src/main/java/com/music/aurelis/ui/screens/NowPlayingScreen.kt
- app/src/main/res/values-en/strings.xml
- app/src/main/res/values/strings.xml
- app/src/main/res/xml/file_paths.xml
- app/src/test/java/com/music/aurelis/ui/PlayerIntegrationTest.kt
- app/src/test/java/com/music/aurelis/ui/player/ArtworkTransitionTest.kt
- app/src/test/java/com/music/aurelis/ui/player/LyricsShareRendererTest.kt
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/PLAYER_LYRICS_TRANSITIONS.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

## Reparación de Main176

Run43912192270/38980bfc terminó FAILURE. Logs originales descargados y CRC válido.
B01/B02/B05 SUCCESS; B03 ejecutó439 casos por variante con un fallo en cada una;
B04 SKIPPED y B06 FAILURE. La cobertura quedó sin generar tras el fallo de suites,
no es la causa raíz acreditada.

La prueba disabledOnlineSearchKeepsImportedLyricsAndDoesNotSendMetadata buscaba
LRCLIB directamente aunque ahora importación/búsqueda están dentro de Herramientas
cuando hay letra. Se actualiza el recorrido: acción oculta inicialmente, abrir
Herramientas, comprobar cero peticiones y pulsar búsqueda explícita por su recurso.
Se conservan letra importada visible, lookup fallido al estar deshabilitado, cero
peticiones remotas y reproducción pausada. No se cambia producción ni se omite
la prueba. Dos revisiones del mismo agente: alcance de variables/recursos y
contrato de privacidad/recuperación. Parser y diff-check locales; Android solo CI.
Dos archivos correctivos ya pertenecían a la entrega:19 distintos acumulados
desde d181c97a, máximo60. Nuevo CI pendiente; avisar push y esperar al usuario.
