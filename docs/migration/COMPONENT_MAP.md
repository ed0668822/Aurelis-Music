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

## Cambios propios B02

Sin nuevas copias de BitChord ni cambios en sus fuentes. Referencias históricas
anteriores se mantienen para próximos bloques.

| Contrato / integración | Destino añadido o modificado | Resultado |
|---|---|---|
| Identidad local | `data/model/ProfileId.kt` | ID estable; default conserva la instalación previa |
| Ámbitos privados | `data/ProfileStorage.kt` | Cinco áreas; archivos separados para otros IDs, sin CRUD ni selector de perfiles |
| Compatibilidad heredada | `data/ProfileRepository.kt`, `data/SettingsRepository.kt` | Mismos archivos y claves del perfil default, sin copiar/borrar datos |
| Inicio de aplicación | `AurelisApplication.kt` | Inicializa ProfileRepository para las escrituras de Ajustes existentes |
| Lectura de biblioteca | `data/LocalLibrarySource.kt`, `LibraryViewModel.kt` | Adapter MediaStore existente, inyección y propagación de cancelación |
| Verificación | `app/src/test/java/com/music/aurelis/` desde raíz | Pruebas Robolectric de persistencia y contrato de biblioteca |

No se modifican los stubs de playlists/historial ni la UI. Los ámbitos de almacenamiento
permiten implementarlos después sin mezclar datos de perfiles.

## Sistema visual B03

Referencia consultada: A:`ui/theme/Theme.kt` en el mismo SHA fijo de BitChord.
Se reaprovechan valores y jerarquía ya adaptados en Theme.kt de Aurelis, sin importar
fuentes ni módulos compartidos del origen. Texto secundario se ajusta para contraste.

| Área | Destino | Decisión B03 |
|---|---|---|
| Color | `ui/theme/ColorSchemes.kt` | Una paleta clara/oscura con roles opacos y superficies neutras; se corrige alfa secundario |
| Tipografía | `ui/theme/Theme.kt` | Activar escala existente con sans del sistema e interlineados; retirar paletas privadas inactivas |
| Tema raíz | `ui/theme/AurelisTheme.kt` | Preferencias de tema/movimiento, formas comunes e iconos de barras del sistema |
| Tema de carátula | `ui/theme/DynamicAurelisTheme.kt` | Conservar colores dinámicos y compartir tipografía/formas |
| Búsqueda | `ui/components/SearchField.kt`, `SearchBar.kt` | Campo único, áreas táctiles, altura adaptable y semántica existente de buscar/borrar |
| Ajustes | `ui/components/SettingsComponents.kt` | Superficies, títulos extensibles, selección, estado deshabilitado y switch único |
| Carga | `ui/components/Skeletons.kt` | Superficie estática cuando se reduce movimiento |
| Verificación | `app/src/test/java/com/music/aurelis/ui/theme/` desde raíz | Contraste y composición/interacción real de componentes, más capturas nativas |

No se reemplaza la biblioteca de iconos ya existente ni se copia una familia de
fuentes del origen. Home, navegación y reproductor mantienen su estructura para
sus bloques posteriores. El PR se identifica como #1 por referencia Git coincidente;
su título fue proporcionado por el usuario y después confirmado en la página HTML
de GitHub, junto al estado OPEN. La API sigue sin estar disponible.

## Navegación B04

Se integra la adaptación AppNavigator existente en Aurelis; no se vuelve a copiar
el navegador del origen ni se crean servicios de audio. Se conservan sus callbacks
de detalle, mientras las rutas de datos complejos quedan diferidas.

| Área | Destino | Resultado |
|---|---|---|
| Pila y estado | `ui/navigation/AppNavigator.kt` | AppTab tipada, rutas NowPlaying/Queue, retorno a Inicio y Saver pequeño/versionado |
| Host activo | `ui/navigation/AppNavigationHost.kt` | BackHandler único, SaveableStateHolder, página activa y overlays existentes |
| Integración real | `MainActivity.kt` | AurelisNavigationContent separado de arranque; enlaces Biblioteca y Ajustes conectados; sin booleanos de rutas |
| Barra inferior | `ui/components/GlassNavBar.kt` | Selección accesible y entrada única al reproductor; MiniPlayer independiente conservado como componente |
| Música local | `ui/screens/LocalMusicScreen.kt` | Estado UI guardable y búsqueda interna con prioridad de Atrás |
| Páginas existentes | `ui/screens/ReplayScreen.kt`, `EqualizerScreen.kt` | Se explicita que métricas/controles de estas estructuras no están disponibles |
| Textos | `app/src/main/res/values/strings.xml` desde raíz | Etiquetas de navegación y estados incompletos sin referencias a bloques técnicos |
| Pruebas | `app/src/test/java/com/music/aurelis/ui/navigation/` desde raíz | Pila pura y grafo Compose real, sin efectos de arranque/red/audio |

NavigationController y sus referencias estáticas de auditoría permanecen como legado
sin uso en el grafo activo; no se usa navigateBack por decremento de índices.
Home y callbacks de detalles de álbum/artista permanecen pendientes de B05/B06.

## Home y datos reales B05

| Área | Destino | Resultado |
|---|---|---|
| Selección local | `data/LocalHomeData.kt` | Resuelve recientes/frecuentes/favoritas y recomendaciones contra el catálogo; QueueBuilder existente para cola local |
| Persistencia básica | `data/LocalUserLibrary.kt` | Ámbitos B02, favoritos, playlists e indicadores de escucha por ID; versiones desconocidas conservadas sin escribir |
| Registro real | `data/stats/ListeningStats.kt`, `playback/PlaybackViewModel.kt` | Se conecta el hook existente de deltas y transiciones; no motor nuevo |
| Home | `ui/screens/HomeScreen.kt`, `ui/components/home/HomeHero.kt` | Estados de biblioteca y secciones reales; continuar sin reiniciar cola; no datos simulados |
| Colecciones | `ui/screens/LocalCollectionScreen.kt` | Álbum/artista/favoritos y listas actuales, reproducción, favoritos y edición básica de listas con confirmación |
| Rutas/integración | AppNavigator, MainActivity, AurelisApplication | Claves pequeñas de colecciones, estado reactivo privado y acciones reales de Home/Biblioteca |
| Estado/modelo | LibraryState, LibraryViewModel, LocalAlbum | Error de permiso diferenciable y clave de álbum sin colisiones de separador |

OfflinePlaylists y ListeningRecorder legados no forman una segunda persistencia activa;
se mantiene un único LocalUserLibrary para estos datos. Su compatibilidad/integración
avanzada se revisará en B08; no se declaran esos stubs como funciones completas.
No se importó nuevo código de BitChord ni DSP/recursos externos en B05.
