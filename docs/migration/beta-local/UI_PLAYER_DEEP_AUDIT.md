<!-- Aurelis © 2026. GPL-3.0-or-later -->
# Investigación de UI, Replay y reapertura al cambiar de canción

## Petición y evidencia

Base30e78cf4f2ade2df0031d56588deca4bba443428. El usuario aporta capturas y el ZIP
Aurelis-Informes-B01-B06-177.zip, y confirma que tocar minutos cierra TODA la app.
La app conserva Aurelis Music/com.music.aurelis, servicio y reproducción locales.
El usuario exige consultar BitChord real y no inventar piezas. Checkout de solo
lectura /workspace/BitChord, commit b690dc146009d4815825fa4a20815ef2e4ee8898.
No actualizar main indiscriminadamente. Máximo60 archivos por entrega y reparaciones.

ZIP aportado validado por CRC,314 entradas, extraído fuera del checkout. B06 PASS,
B01–B06 SUCCESS para30e78cf4; B03 tiene439/439 PASS por variante, sin omitidos. Los
logs Debug/Release registran tareas test ejecutadas; Kover posterior UP-TO-DATE no
se confunde con ejecución de la suite. Cobertura5439/6023=90,30%, umbral80% cumplido.
Ese resultado pertenece a la BASE, no a los cambios nuevos.

La comparación UI real es otro workflow: run43912269724/UI31, SHA38980bfc. Logs
originales descargados y ZIP CRC válido. Cada material alcanzó18 estados Aurelis y6
BitChord,24/36. Plain falló en uiautomator dump de player-portrait de referencia
(exit255); glass agotó60s en shell rm del mismo estado. Ambas: capture_complete=false,
visual_verdict=NOT_REVIEWED. Causa final del fallo ADB de referencia NO demostrada.
Los ZIP de imágenes14249607403/plain y14250910010/glass redirigen a un destino
rechazado con403; no se han abierto imágenes nuevas inexistentes. Las capturas del
usuario y el código fijo sí se han revisado. No modificar APK/referencia para dar verde.

El workflow de poses abre pantallas y comprueba requisitos/capturas; NO compara
por sí solo la igualdad de cada píxel. Reduce movimiento para poses estáticas,
no incluye Replay en sus36 estados y no observa el panel DURANTE cada salto de
canción. Esto explica por qué alcanzar18 estados no descubría estas regresiones.
Las pruebas nativas independientes de API35/36 se amplían para cubrirlas.

## Búsqueda y conexiones reales

Se generó inventario externo UI_SOURCE_SEARCH_AUDIT.csv con rutas, SHA256,
candidatos de referencia por nombre y llamadas de funciones por nombre:
- 214 archivos Kotlin UI de Aurelis.
- 165 archivos UI de BitChord indexados.
- 363 Kotlin de src/main buscados para referencias de nombres.
- SHA256 del CSV: a77df8bc768fdb339157d76075bb2f67f29a3e718971289a4bfc75157e9ad85b.

El inventario es una búsqueda exhaustiva de nombres/archivos, NO una auditoría
manual completa de cada archivo ni prueba de integración/compilación. Sobrecargas,
callbacks y reflection requieren revisión manual. Se inspeccionaron manualmente
los flujos activos siguientes y sus consumidores/pruebas; no se declaran módulos
conectados por tener su archivo presente. Todo src/main sigue compilándose según
Gradle; ausencia de llamadas no significa exclusión de compilación.

| Superficie | Flujo activo y referencia | Hallazgo/acción/evidencia pendiente |
|---|---|---|
| Navegación/player | MainActivity → NowPlayingScreen → PlayerSheetMotion; patrón sheet/dock de BitChord | reveal dependía de song.id; corregido a visibilidad/disponibilidad, no identidad. Pruebas con animación real pendientes. Reloj y selección se vinculan a la ocurrencia local de cola. |
| Replay/minutos | LibraryReplayPreview → AppRoute.Replay → ReplayScreen/FrostedTopBar | Cabecera lectora se grababa en su fuente global; se excluye grabación del host y se graba solo LazyColumn. Se elimina doble reserva inferior. Regresión nativa con y sin música. |
| MiniPlayer | BottomPlayerBar → MiniPlayer/AnimatedArtwork/PlayerDock | Retención/crossfade anteriores conservados; nueva investigación distingue imagen de reapertura del panel. No reescribir lo ya implementado. |
| Portrait/credits | NowPlayingScreen, fuentes PlayerControls/NowPlayingScreen/Theme | Título20sp W700, artista20sp W500/alpha0,55; cabecera compacta16sp. CircleGlyph34/icon19, alpha0,18/0,34, crossfade180 y hápticos. Footer44/icon26 y tile35/corner7/alpha0,62 copiados del BottomGlyph. |
| Iconos | BitChordIcons Queue/LyricsQuote/Star/StarFilled | Se adaptan los trazados REALES y helper star, no se sustituyen por glifos Material parecidos. Pausa/play/skip ya eran vectores reales. |
| Transporte/volumen | PlayerControls/PlayerVolumeRow → mismo ViewModel/AudioManager | Skip53dp como fuente, slot volumen32dp visible/oculto y huecos10. Sin cambiar lógica ni inventar medición de salida. |
| Pie del player | PlayerActionRow de referencia | Letras/cola reales y modos locales; selección de letras anunciada. Caption local honesto. Cast/party se excluyen por alcance explícito; no botones ficticios. |
| Letras en portrait | ui/player/SyncedLyricsPanel y NowPlayingScreen, fuentes PlayerLyrics/NowPlayingScreen | Header con miniatura, créditos/estrella/menú y asa; controles compartidos con pantalla principal. No el layout legacy LyricsScreen sin llamadas. |
| Lectura/foco | PlayerLyrics → SyncedLyricsPanel |34/41sp sincronizado y30/38 sin tiempos, W800, gutter30; duet44 y ancla al primer vocal sin terminar. Puntos13/gap5 para instrumental; foco0,98→1 y blur por distancia. |
| Gestos de letras | rememberPlayerControlsOnScroll y LyricsControlsGesture reales de BitChord | Umbral20dp, solo UserInput; adelante oculta, atrás revela, tap inferior revela. Retorno600ms/5000ms tras parar drag mientras suena; lectura pausada permanece. |
| Herramientas/estado | Repositorios locales ya activos y Source/Translation/Offset dialogs | Proveedor subrayado, traducción y herramientas accesibles; labels locales ES/EN en vez de Incrustadas fijo en inglés. No activar proveedores pendientes. Se reutilizan traducciones EN de diálogo/errores existentes, conservando placeholders y política de privacidad. |
| Compartir | LyricsShare.rememberLyricsPicker/LyricsPickBar → renderer/sheet existentes | Multiselección real, máximo250 caracteres originales y traducción que cabe; gaps no seleccionables. Seleccionar no hace seek. Cancelar/Atrás conservan audio. |
| Cola/horizontal | QueueScreen/LandscapePlayerLayout | Mantiene controlador/ocurrencias y paneles existentes, sin transportes duplicados. Verificación de layouts y gestos continúa en pruebas. |
| Home/Search/Library/Settings | MainActivity → pantallas actuales y entradas de navegación | Adaptaciones previas preservadas. Inventario compara candidatos; esta entrega no acredita paridad global ni cambia sin una brecha demostrada. |
| Widgets/metadatos | Funciones locales/B12 | No entran en el protocolo de36 estados; deben probarse en launcher. No se declaran completos por existir clases. |

## Causas y límites de las correcciones

1. LaunchedEffect(isVisible,song.id,panelMotion) llamaba reveal con cada pista;
   reveal coloca held=travel y vuelve a animar a cero. Se dispara ahora por cambio
   de visibilidad/disponibilidad; siguiente/anterior no son aperturas nuevas. No se
   resuelve desactivando animaciones, ocultando errores o cambiando reproducción.
2. Replay tenía FrostedTopBar dentro de pageBackdropModifier que contenía un lector
   del mismo layer. Es el patrón de autorreferencia RenderNode ya evitado en Settings.
   La corrección rompe esa dependencia. Se confirma el defecto de composición; sin
   logcat del Redmi no se afirma una traza nativa específica ni se certifica cierre
   eliminado hasta ejecutar la nueva regresión y prueba física.

## Pruebas y lo que falta

Cuatro casos JVM nuevos: geometría del panel en12 frames tras Next con animaciones;
contrato Replay cabecera fuera del backdrop con tarjeta real; puntos instrumentales
y selección sin seek; controles reaccionan a UserInput y no al auto-follow. Se amplía
la integración de letras para elegir dos versos/compartir/volver sin cambiar posición.
Las dos regresiones instrumentadas existentes de API35/36 ahora abren Replay,
recrean la actividad con GL/Glass activo y prueban Next/Previous con dos items PCM
reales, muestreando que el header no desaparece ni reinicia la entrada. Usan datos
CI explícitos en emulador desechable; no se suman minutos a un dispositivo de usuario.

Se realizaron dos revisiones estáticas por el mismo agente, no independientes. Kotlin/XML,
recursos, formato y diff-check locales; Gradle/Lint/APK/tests Android SOLO Actions.
No afirmar que las pruebas nuevas pasaron antes de CI. Esperar resultado del usuario
tras avisar push; no monitorizar conclusiones ni avanzar automáticamente.

Quedan sin certificar igualdad visual global, todos los estados de referencia,
shaders avanzados de bloom/partículas y trayectoria exacta de carátula al colapsar;
el fade adaptado420ms no se presenta como una copia completa del motor de BitChord.
Replay por semana/mes/año requiere eventos y legado en B11; no usar lastPlayedAt para
fabricar períodos. Replay stories, widgets en launcher, audio avanzado/B09 y DSP no
se inician aquí. Tampoco streaming, Cast, TV, Wear, Auto ni Canvas/vídeo externo.
La prueba Redmi Note14 Pro5G Android16/HyperOS3 de cierre de Replay y continuidad
entre canciones sigue pendiente. No cerrar B05/B07/B13 solo por compilar.

## Fuentes y procedencia

BitChord GPLv3, commit fijo indicado; atribuciones preservadas. Las correcciones de
ciclo de presentación y grabación son aportaciones de Aurelis sobre su flujo activo.

- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/NowPlayingScreen.kt
  SHA256: 1d5ceae1f1c045881a49601f3fa535ecd4cf8efe45837fc97990045e3ed9024e
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerControls.kt
  SHA256: cfd2de8f97375f7c697c590d7984de596408c734ef055b17565b47abbfb4742f
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerLyrics.kt
  SHA256: 41d28e9c1513243362487e7f425f35f55af1aaa7bcf06ffc9688146ea21214b8
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/LyricsShare.kt
  SHA256: 36c1e6bb450600c61685f02de83bd68b8ca7101020bed1621b1a2b8feaa71572
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/LyricsControlsGesture.kt
  SHA256: 4ffcec2ceff3ca7eb16a65a29313aa7181100055c78ff2b40b9a2e1600d50710
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/icons/BitChordIcons.kt
  SHA256: f08d862396d3c1d067f13e01feef3a9e7ce10914d5418b96dccc5f926ca3a202
- app/src/main/java/com/music/bitchord/ui/theme/Theme.kt
  SHA256: 8a0fe2f0118da55fe1448443344160c1a9e20cfda2d9c1acf7286c0e17fba900

## Inventario de entrega: 24 archivos

- NOTICE
- app/src/androidTest/java/com/music/aurelis/playback/RealSettingsNavigationInstrumentedTest.kt
- app/src/main/java/com/music/aurelis/MainActivity.kt
- app/src/main/java/com/music/aurelis/ui/components/LocalLyricsLabels.kt
- app/src/main/java/com/music/aurelis/ui/components/LyricsSourcesDialog.kt
- app/src/main/java/com/music/aurelis/ui/components/PlayerBottomGlyph.kt
- app/src/main/java/com/music/aurelis/ui/components/PlayerCircleGlyph.kt
- app/src/main/java/com/music/aurelis/ui/components/PlayerControls.kt
- app/src/main/java/com/music/aurelis/ui/components/PlayerVolumeRow.kt
- app/src/main/java/com/music/aurelis/ui/icons/AurelisPlayerIcons.kt
- app/src/main/java/com/music/aurelis/ui/player/LyricsShare.kt
- app/src/main/java/com/music/aurelis/ui/player/PlayerControlsOnScroll.kt
- app/src/main/java/com/music/aurelis/ui/player/SyncedLyricsPanel.kt
- app/src/main/java/com/music/aurelis/ui/screens/NowPlayingScreen.kt
- app/src/main/java/com/music/aurelis/ui/screens/ReplayScreen.kt
- app/src/main/res/values-en/strings.xml
- app/src/main/res/values/strings.xml
- app/src/test/java/com/music/aurelis/ui/LyricsPanelTest.kt
- app/src/test/java/com/music/aurelis/ui/PlayerIntegrationTest.kt
- app/src/test/java/com/music/aurelis/ui/navigation/ChromeContractTest.kt
- docs/migration/beta-local/FILE_MAPPING.md
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/PROGRESS_TRACKER.md
- docs/migration/beta-local/UI_PLAYER_DEEP_AUDIT.md
