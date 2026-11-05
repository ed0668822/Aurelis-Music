# Mapa de componentes

Origen fijo: `kushagrasinghx/BitChord@b690dc146009d4815825fa4a20815ef2e4ee8898`.
Destino inspeccionado: `ed0668822/Aurelis-Music@9bf3fd3db8f5870f942f5167dca5bc77e3c48edd`.
Las rutas abreviadas de destino parten de `app/src/main/java/com/music/aurelis/`.
En origen, A = `app/src/main/java/com/music/bitchord/`,
S = `shared/src/jvmSharedMain/kotlin/com/music/bitchord/`,
C = `shared/src/commonMain/kotlin/com/music/bitchord/`,
U = `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/`.
Cada fila identifica una referencia leída/listada, no una copia autorizada automáticamente.

| Área / bloque | Origen exacto abreviado | Destino existente | Decisión y dependencias |
|---|---|---|---|
| Biblioteca B06 | A:`data/LocalMediaRepository.kt` | `data/LocalMediaRepository.kt` | Conservar MediaStore; adaptar mejoras selectivas tras permisos/URI/modelos. `scanLocalFolders` devuelve vacío; no presentarlo como funcional |
| Home B05 | U:`ui/screens/HomeScreen.kt` | `ui/screens/HomeScreen.kt`, `ui/components/home/` | Adaptar composición visual a LocalSong/álbum/artista. Home destino ya recibe recientes/más reproducidas/añadidas; verificar proveedores de datos |
| Sistema visual B03 | A:`ui/theme/Theme.kt` | `ui/theme/AurelisTheme.kt` y temas actuales | Adaptar tokens y efectos compatibles; auditar fuentes, recursos y APIs Compose por separado |
| Navegación B04 | A:`ui/navigation/AppNavigator.kt` | `ui/navigation/AppNavigator.kt`, `NavigationController.kt` | Evaluar ambas rutas existentes antes de integrar; excluir rutas remotas del origen |
| Barra/mini B04/B07 | A:`ui/components/GlassNavBar.kt`, `MiniPlayer.kt` | mismos nombres en `ui/components/` | Adaptar controles sobre host actual; GlassNavBar origen declara procedencia Echo-Music y FloatingTabBar: preservar avisos |
| Reproductor B07 | U:`ui/player/PlayerHost.kt` | `ui/player/PlayerHost.kt`, `FullScreenPlayerHost.kt`, `AurelisPlayerHost.kt` | Conservar contratos compatibles; servicio destino `playback/AurelisPlaybackService.kt` crea ExoPlayer y MediaSession. No crear motor paralelo |
| Letras B09 | A:`data/lyrics/EmbeddedLyrics.kt`; S:`data/lyrics/LyricsRepository.kt`, `LrcLib.kt` | mismos nombres bajo `data/lyrics/` | Conservar parsers/proveedores compatibles; auditar orden local/caché/red en llamadas. El repositorio observado lanza consultas de proveedores concurrentes |
| Traducción B10 | S:`data/lyrics/LyricsTranslation.kt` | `data/lyrics/LyricsTranslation.kt` | Conservar solo partes utilizables con servicios permitidos, caché y cancelación; endpoint observado translate.googleapis.com no acredita condiciones de uso |
| Historial B08 | A:`data/stats/ListeningRecorder.kt`, `ListeningStats.kt` | mismos nombres bajo `data/stats/` | Conservar registro compatible; comprobar persistencia e integración por perfil |
| Cola B07/B08 | S:`playback/QueueBuilder.kt` | `playback/QueueBuilder.kt` y cola existente | Conservar deduplicación y modelos locales; verificar uso efectivo, restauración y cambios de biblioteca |
| Audio local B11 | A:`playback/smart/LocalAudioSource.kt` | `playback/smart/LocalAudioSource.kt` | Fuente Android usa content URI sin duplicar caché de disco; comprobar adaptación y cancelación |
| AutoMix B11 | C:`playback/smart/TransitionPolicy.kt`; S:`playback/smart/TrackFeatures.kt` | mismos nombres bajo `playback/smart/` y DSP nativo | Auditar antes de copiar: origen declara Orchard/AGPL. Contratos destino difieren; no sustituir por nombre. Separar análisis/transiciones de recomendación local |
| AutoMix remoto | C:`playback/smart/AutomixAnalysisSource.kt` | `playback/smart/AutomixAnalysisSource.kt` | Origen contiene resolución YouTube/Opus; excluir esa dependencia del flujo local |
| Playlists B08 | No se seleccionó implementación origen | `data/OfflinePlaylists.kt` | Método observado devuelve lista vacía. Especificar persistencia en B02; no afirmar que otras rutas de playlists no existen sin trazarlas |
| Perfiles B12 | No se seleccionó implementación origen | `data/ProfileRepository.kt` | Perfil único con nombre/foto en SharedPreferences privadas. Preparar aislamiento en B02; gestión múltiple en B12 |

## Límites y próximos detalles

No se auditaron todos los archivos ni se ejecutó BitChord. El inventario es una
selección de puntos de entrada para B01; cada bloque ampliará solo las filas afectadas.
No hay evidencia de fidelidad visual en dispositivo ni de funcionamiento de todos
los proveedores. No seleccionar automáticamente recursos SF Pro del origen.
Las nuevas rutas de producción se decidirán en el bloque correspondiente tras
trazar las llamadas existentes; B01 solo añade documentos.
