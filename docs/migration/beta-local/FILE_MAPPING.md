# FILE_MAPPING — referencia → destino

Origen fijo b690dc146009d4815825fa4a20815ef2e4ee8898. Destino base 01c1ba5.
A=`app/src/main/java/com/music/bitchord/`; C=`shared/src/commonMain/kotlin/com/music/bitchord/`;
S=`shared/src/jvmSharedMain/kotlin/com/music/bitchord/`;
U=`sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/`;
D=`app/src/main/java/com/music/aurelis/`. Después de D, las rutas separadas por
punto y coma conservan D salvo rutas que empiezan app/ o docs/.
Globs son familias, no permiso para sustituir módulos enteros. «Nuevo» es previsto,
no implementado. El inventario enumera todos los archivos; solo una selección se
ha revisado semánticamente. Revalidar cada fila al iniciar su unidad.

| ID | BitChord | Aurelis existente | Nuevos previstos | Estrategia/dependencias |
|---|---|---|---|---|
| F00 | LICENSE; build.gradle.kts; app/build.gradle.kts; app/src/main/AndroidManifest.xml; .github/workflows/** | LICENSE; NOTICE; LICENSES/**; build.gradle.kts; app/build.gradle.kts; app/src/main/AndroidManifest.xml; .github/workflows/android.yml | docs/migration/beta-local/** | Auditoría; no sustituir plataforma |
| F01 | A:auth/** (solo apariencia) | D:data/ProfileRepository.kt; data/ProfileStorage.kt; data/ProfileAvatars.kt; ui/screens/SettingsScreen.kt | D:data/SingleProfileMigration.kt | Perfil único, respaldo y conservación de ID legacy |
| F02 | A:data/LocalMediaRepository.kt; data/LocalGroups.kt; ui/screens/LocalFolder.kt | D:data/LocalMediaRepository.kt; data/model/LocalSong.kt; data/LocalLibrarySource.kt; LibraryViewModel.kt | D:data/LocalFolderRepository.kt; data/LocalMetadataReader.kt | MediaStore/SAF/géneros reales |
| F03 | A:ui/components/backdrop/**; ui/components/LiquidGlass.kt; ui/components/floatingtabbar/**; ui/theme/Theme.kt; U:ui/graphics/**; sharedUi/src/androidMain/** | D:ui/components/backdrop/**; ui/components/LiquidGlass.kt; ui/components/floatingtabbar/**; ui/theme/** | Recursos de fuente autorizados y pruebas visuales | Portar efectos sin retirar avisos ni copiar SF Pro |
| F04 | A:ui/navigation/AppNavigator.kt; U:ui/AppUi.kt; ui/screens/HomeScreen.kt | D:MainActivity.kt; ui/navigation/**; ui/AppUi.kt; ui/screens/HomeScreen.kt; data/LocalHomeData.kt | Contratos locales dentro de archivos existentes | Host inicializado y carruseles reales; no pegar MainActivity origen |
| F05 | U:ui/screens/ExploreScreen.kt; ui/screens/SearchScreen.kt; ui/screens/LibraryScreen.kt; A:ui/screens/LibraryPhoneParts.kt | D:ui/screens/ExploreScreen.kt; ui/screens/SearchScreen.kt; ui/screens/LibraryScreen.kt; ui/screens/LocalCollectionScreen.kt; data/LocalSearch.kt | D:data/LocalGenreRepository.kt; data/LocalSearchHistory.kt | Conectar rutas; ampliar búsqueda; selección múltiple |
| F06 | A:ui/components/MiniPlayer.kt; U:ui/player/NowPlayingScreen.kt; ui/player/LandscapePlayer.kt; ui/player/PlayerDock.kt; ui/player/PlayerDrawer.kt; ui/player/PlayerHost.kt | D:ui/components/MiniPlayer.kt; ui/screens/NowPlayingScreen.kt; ui/player/PlayerHost.kt; ui/player/LandscapePlayer.kt; ui/player/PlayerDock.kt; ui/player/PlayerDrawer.kt; ui/player/FullScreenPlayerHost.kt | Pruebas visuales de las variantes | Una entrada activa que adapta componentes existentes |
| F07 | S:data/NerdStats.kt; U:ui/components/SourceQualityBadge.kt | D:ui/components/SourceQualityBadge.kt; playback/PlaybackViewModel.kt; data/model/LocalSong.kt | D:playback/AudioQualityInfo.kt; playback/AudioQualityObserver.kt | Calidad origen/salida, desconocidos explícitos |
| F08 | A:playback/PlaybackService.kt; playback/PlayerConnection.kt; playback/SleepTimer.kt; playback/Queue*.kt; S:playback/Queue*.kt | D:playback/AurelisPlaybackService.kt; playback/PlayerConnection.kt; playback/PlaybackViewModel.kt; playback/ProfilePlaybackBinding.kt; playback/ServicePlaybackRecorder.kt; data/LocalQueueStore.kt | D:playback/LocalPlaybackCoordinator.kt; playback/LocalSessionPlayer.kt | Servicio/sesión únicos; cola canónica y puente de comandos |
| F09 | A:ui/components/SongActionsMenu.kt; U:ui/player/PlayerQueue.kt | D:ui/components/SongActionsDialog.kt; ui/components/SongActionsMenu.kt; ui/player/PlayerQueue.kt | Comandos Play Next en coordinador | Reordenar y preservar ocurrencias duplicadas |
| F10 | A:data/lyrics/EmbeddedLyrics.kt; S:data/lyrics/**; U:ui/player/SyncedLyricsPanel.kt; ui/player/PlayerLyrics.kt; ui/player/LyricClock.kt; ui/player/LyricFocus.kt | D:data/lyrics/**; ui/player/SyncedLyricsPanel.kt; ui/player/LyricClock.kt; ui/player/LyricFocus.kt | D:ui/player/PlayerLyrics.kt y fixtures temporales | Un repositorio activo; parsers y reloj real |
| F11 | U:ui/player/LyricsControlsGesture.kt; ui/player/LyricsOffsetSheet.kt; ui/player/LyricsShare.kt; A:ui/player/LyricsShareCard.kt; ui/player/AndroidLyricsShareSheet.kt | D:ui/player/LyricsControlsGesture.kt; ui/player/LyricsOffsetSheet.kt; ui/player/LyricsShare.kt; ui/player/LyricsShareCard.kt; ui/player/AndroidLyricsShareSheet.kt | Pruebas de gestos/exportación | Conectar UI existente al flujo activo |
| F12 | S:data/lyrics/LyricsRepository.kt; data/lyrics/LrcLib.kt; data/lyrics/BetterLyrics.kt; data/lyrics/BiniLyrics.kt; data/lyrics/LrcRed.kt; data/lyrics/LyricsTranslation.kt | D:data/lyrics/LocalLyricsRepository.kt; data/lyrics/LyricsRepository.kt; data/lyrics/BetterLyrics.kt; data/lyrics/BiniLyrics.kt; data/lyrics/LrcRed.kt; data/lyrics/LyricsTranslation.kt; data/SettingsRepository.kt | D:data/lyrics/LyricsProviderRegistry.kt | LRCLIB primero; evaluación legal individual; conservar traducción configurada |
| F13 | A:playback/audio/**; playback/EqualizerProcessor.kt; playback/LoudnessProcessor.kt; playback/SpatialAudioProcessor.kt; C:playback/PeakLimiter.kt | D:playback/audio/**; playback/EqualizerProcessor.kt; playback/LoudnessProcessor.kt; playback/SpatialAudioProcessor.kt; playback/PeakLimiter.kt; playback/AudioSettingsBinding.kt | Tests PCM/capacidades | Sin duplicar efectos; incompatibilidades explícitas |
| F14 | A:playback/audio/bitperfect/**; playback/audio/bluetooth/**; app/src/main/cpp/bitperfect/** | D:playback/AudioRouting.kt; playback/AudioOutputPolicy.kt; app/src/main/cpp/** | D:playback/audio/bitperfect/**; playback/audio/bluetooth/**; app/src/main/cpp/bitperfect/** | Adaptación LGPL/Mornye/Oboe/libusb y prueba hardware |
| F15 | A:playback/smart/**; C:playback/smart/**; S:playback/smart/**; C:data/settings/AutomixPerformanceMode.kt | D:playback/smart/**; data/settings/AutomixPerformanceMode.kt; ui/components/AutoMixPanel.kt; playback/LocalAutoMix.kt | D:playback/smart/TrackAnalyzer.kt; playback/smart/LocalAnalysisScheduler.kt | Sustituir stubs; modos inferencia1/2/3, default2 |
| F16 | A:playback/CrossfadeController.kt; playback/loopmix/**; native/loopmix/**; native/third_party/** | D:playback/CrossfadeController.kt; app/src/main/cpp/** | D:playback/loopmix/**; app/src/main/cpp/loopmix/** y terceros autorizados | Dos decks internos coordinados, no dos servicios |
| F17 | A:ui/screens/SettingsSheet.kt; data/settings/AppSettings.kt; ui/performance/DisplayRefreshRates.kt | D:ui/screens/SettingsScreen.kt; ui/screens/SettingsSheet.kt; data/SettingsRepository.kt | D:ui/performance/DisplayRefreshRates.kt | Una pantalla activa; no trasladar cuentas/streaming |
| F18 | A:ui/replay/**; data/stats/Backup.kt; U:ui/replay/ReplayCard.kt | D:ui/screens/ReplayScreen.kt; ui/replay/ReplayModel.kt; ui/replay/ReplayCard.kt; data/stats/**; data/LocalUserLibrary.kt | D:data/stats/ListeningDatabase.kt; data/stats/ListeningEventStore.kt; data/stats/LocalBackupRepository.kt; ui/replay/ReplayStories.kt; ui/replay/ReplayPoster.kt; ui/replay/ReplayShareSheet.kt | SQLite para eventos; preservar totales sin cronología ficticia |
| F19 | A:ui/components/AppLanguageDialog.kt; app/src/main/res/values*/**; sharedUi/src/commonMain/composeResources/values*/** | D:ui/components/AppLanguageDialog.kt; app/src/main/res/values*/** | Traducciones/plurales adaptados | Sin traducción automática de interfaz en runtime |
| F20 | A:widget/**; app/src/main/res/layout/widget_*; app/src/main/res/xml/widget_* | D:widget/**; app/src/main/AndroidManifest.xml | Recursos/registros de widgets faltantes | Acciones mediante sesión única |
| F21 | A:data/remote/** (solo partes reutilizables); S:data/AppleArtistPhotos.kt; data/AppleCatalogue.kt | Sin integración equivalente activa acreditada | D:data/artwork/OptionalArtworkRepository.kt | Solo enriquecimiento HTTPS opt-in; no audio remoto/WebDAV/SMB |
| F22 | A:data/scrobbling/** | D:data/scrobbling/ScrobbleManager.kt | D:data/scrobbling/LastFmClient.kt; data/scrobbling/ListenBrainzClient.kt; data/scrobbling/ScrobbleOutbox.kt; data/scrobbling/ServiceCredentialsStore.kt | Reemplazar no-op con servicios autorizados y credenciales protegidas |
| F23 | A:download/FlacTagger.kt; download/Mp4Tagger.kt; download/MediaTagger.kt; download/LyricsTag.kt; download/WebmTagger.kt | Sin editor equivalente activo acreditado | D:data/files/LocalFileActions.kt; data/files/LocalTagEditor.kt | Copia verificada por defecto; sin Downloader/DownloadService |

## Retiradas y compatibilidad

No importar backend/web/TV/Wear/streaming. Retirar rutas/manifest/contratos activos
que contradigan exclusiones; archivos legacy sin referencia no se borran en masa.
Conservar entrypoints del destino. Core de letras/DSP debe tener un único propietario.
LrcRed existe también en destino. PlayerLyrics.kt no existe en el destino: su
adaptación se marca nueva. LocalAutoMix está en playback/, no en data/.
AutoMixPanel existe solo en Aurelis; no hay archivo homónimo en el árbol origen.
Cada port requiere las dependencias aprobadas en B00, procedencia/avisos del
LICENSE_AUDIT y pruebas del IMPLEMENTATION_BLOCKS.
