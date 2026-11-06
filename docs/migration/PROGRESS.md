# Progreso de migración

Estado vigente: nuevo plan en [beta-local/PROGRESS_TRACKER](beta-local/PROGRESS_TRACKER.md).
B01/B02 implementados y comprobados localmente; B03 no iniciado. Las secciones
homónimas antiguas siguientes son historial y no autorizan bloques del nuevo plan.

## Reanudación

Leer MASTER_PLAN, este registro, COMPONENT_MAP, BUILD_STATUS y LICENSE_AUDIT.
Comprobar rama, HEAD, cambios locales y último commit antes de actuar. No repetir
la auditoría completa; verificar solo referencias cambiadas y componentes del bloque
autorizado. B05 y B06 fueron autorizados juntos; B07 autorizado después; B08/B09 autorizados juntos; B10/B11 autorizados después; B13/B14 autorizados expresamente. B12 sigue pendiente y no autorizado.

PR actual: [Aurelis Music Beta UI and components](https://github.com/ed0668822/Aurelis-Music/pull/1).
Creado y nombrado por el usuario. Referencia `refs/pull/1/head` coincidente con la
rama publicada `feature/bitchord-local-migration`, comprobada al iniciar B03.
Después del push B03, la página HTML de GitHub confirmó título y estado OPEN
(5 commits en ese momento), aunque la referencia de lectura del PR aún mostraba
el SHA anterior. La rama remota sí confirmó el SHA nuevo; no se reescribió ninguna referencia.
Los errores de creación API de B01/B02 registrados abajo son históricos y no
significan que actualmente no exista el PR.

## B01 — Auditoría y planificación

- Estado: auditoría y documentación completadas; rama publicada; creación de PR bloqueada por acceso API.
- Rama: `feature/bitchord-local-migration`.
- Base Aurelis: `9bf3fd3db8f5870f942f5167dca5bc77e3c48edd`.
- Origen BitChord: `b690dc146009d4815825fa4a20815ef2e4ee8898`.
- Acceso: ambos repositorios legibles; BitChord solo referencia, sin cambios.
- Creados: `MASTER_PLAN.md`, `PROGRESS.md`, `COMPONENT_MAP.md`, `BUILD_STATUS.md`,
  `LICENSE_AUDIT.md`, todos dentro de `docs/migration/`.
- Archivos existentes modificados: ninguno; no migración UI ni B02.
- Revisión 1 — arquitectura/procedencia: completada en una pasada separada.
  Se verificaron 24 rutas destino, versiones Gradle/Kotlin/SDK/Media3, stubs,
  servicio Media3, rutas de origen seleccionadas y avisos AGPL. Se mantuvieron
  explícitos los límites de la inspección; no se certificó el árbol completo.
- Revisión 2 — integración/verificabilidad: completada en una segunda pasada
  del mismo agente, con objetivo distinto; no intervino un segundo revisor humano.
  Se contrastaron los registros reales de build y NO-SOURCE, enlaces locales,
  cobertura B01–B14 y diff documental. Pruebas funcionales/CI siguen pendientes.
- Compilación: línea base debug/lint correcta; unitarias NO-SOURCE. No nuevo build
  por documentación; ejecución funcional y release pendientes.
- GitHub Actions: consulta API denegada (`Forbidden`); estado remoto desconocido.
- Commit de documentos: `04c28c90a07c3dcc668d232fe2c2dbb8d24779cc`.
  Este registro se completa en un commit posterior de trazabilidad; consultar
  `git log -2 --oneline` para ambos commits, sin referencias circulares a su propio SHA.
- Publicación: `git push --set-upstream origin feature/bitchord-local-migration`
  terminó con exit 0 y confirmó la nueva rama remota. El commit posterior de
  trazabilidad requiere su propio push y verificación remota.
- Pull Request: no creado. `gh pr create --base main --head
  feature/bitchord-local-migration` falló con
  `Post https://api.github.com/graphql: Forbidden`. No se recibió número ni URL de PR.
- Enlace de creación devuelto por GitHub (no es un PR existente):
  https://github.com/ed0668822/Aurelis-Music/pull/new/feature/bitchord-local-migration
- Título sugerido: `B01: auditoría y plan de migración local de BitChord`.
  Descripción: cinco documentos de auditoría/planificación; sin cambios de aplicación;
  dos revisiones documentales; línea base debug/lint correcta, unitarias NO-SOURCE;
  CSV, dispositivo y consulta de Actions pendientes.

## Hallazgos y bloqueos

- Ya existen componentes adaptados; preservar el servicio Media3 y evitar duplicación.
- Playlists locales y escaneo de carpetas tienen puntos de entrada vacíos; perfil
  actual único. No afirmar cobertura funcional completa a partir de nombres de archivo.
- Origen multiplataforma y versiones distintas requieren adaptaciones selectivas.
- AutoMix incluye procedencia AGPL/Orchard; nuevas copias requieren revisar condiciones.
- Falta `Formato de licencias.csv`: incorporarlo antes de cabeceras definitivas.
- Lectura y escritura Git HTTPS confirmadas en Aurelis. GitHub API devuelve
  Forbidden tanto al consultar permisos como al crear el PR; acceso de creación
  de PR y consulta de Actions no disponible. No se deduce que falte un token.
  Se necesita acceso autorizado a la API de GitHub o crear el PR con el enlace
  anterior. No solicitar credenciales en chat ni cambiar secretos para resolverlo.

## Próximo bloque

B05 y después B06, ambos autorizados en esta sesión. Cerrar/revisar/publicar cada
bloque por separado. B07 requerirá nueva autorización.

## Verificaciones documentales B01

- Rutas de destino del mapa contrastadas con el checkout.
- Enlaces Markdown relativos de esta carpeta comprobados.
- Catorce bloques con dificultad y aceptación; sin autorización implícita de B02.
- `git diff --cached --check` requerido antes de cada commit documental.
- No se repitió Gradle: no hubo cambios de código, recursos o dependencias.
- Las dos revisiones son pasadas separadas; los resultados no sustituyen una
  revisión externa de licencias ni una validación de audio en dispositivo.

## B02 — Arquitectura base

- Autorización: concedida expresamente por el usuario tras configurar su identidad Git local.
- Estado: implementación y revisiones completadas; rama publicada; PR bloqueado por API.
- Base: `e6f58f937a1f84166236705c7f8b9a3a2947f571`; misma rama de migración.
- Añadidos: ProfileId, ProfileStorage, LocalLibrarySource; ProfileStorageTest y
  LibraryViewModelTest en los paquetes correspondientes de `app/src/test/`.
- Modificados: AurelisApplication, ProfileRepository, SettingsRepository,
  LibraryViewModel y dependencias de pruebas de `app/build.gradle.kts`;
  documentos de esta carpeta actualizados.
- Migración: ámbitos default apuntan a los mismos archivos/claves, sin copia ni
  borrado; identidad independiente del nombre. Aislamiento preparado para B08/B12.
- Funcionamiento existente corregido: inicialización de perfil antes de Ajustes;
  cancelación de biblioteca propagada, sin convertirla en error visible.
- Revisión 1 (arquitectura/código): completada como pasada separada del mismo agente.
  Se contrastaron las llamadas de Ajustes, archivos previos, inicialización Application,
  límites de perfil, constructor Android, imports y ausencia de duplicación de escáner/motor.
  Sin cambios de firmas, identificadores de app ni versiones de producción.
- Revisión 2 (funcionamiento/integración): completada como segunda pasada del mismo
  agente. Se añadió cobertura de una canción local y se ejecutaron las pruebas finales;
  conservación de datos/aislamiento y reintentos contrastados con resultados XML.
  No equivale a una revisión de otro humano ni a pruebas en dispositivo.
- Compilación: assembleDebug correcto. Unitarias finales: 11 correctas, 0 fallos,
  0 errores, 0 omitidas. Lint sin errores y 209 advertencias; no se deshabilitan checks.
- Commit de implementación: `2a10e63fd89db9993ebfd1ef21920cc761aec886`.
  Autor/committer: `ed0668822 <ed0668822@gmail.com>`, verificados antes del commit.
  El commit posterior de documentación registra la entrega; consultar `git log -2`.
- Publicación: push confirmado de `e6f58f9` a `2a10e63` en
  `feature/bitchord-local-migration`; registro de entrega se publica en otro commit.
- Pull Request: intento de creación B01–B02 falló con
  `Post https://api.github.com/graphql: Forbidden`. No hay URL de PR confirmada.
  Se conserva la rama y todo el trabajo. Enlace de creación previamente devuelto
  por GitHub: https://github.com/ed0668822/Aurelis-Music/pull/new/feature/bitchord-local-migration
  Título: `B01–B02: auditoría y arquitectura local por perfil`.
- GitHub Actions: consulta de runs de esta rama falló con Forbidden en api.github.com;
  estado remoto desconocido, sin afirmar checks exitosos ni modificar workflows.
- Revisión documental final: enlaces relativos y `git diff --check` correctos;
  manifiesto confirma AurelisApplication como punto de inicio y APK con firma válida.
- Límites: sin UI de perfiles ni funcionalidades de B08; sin nuevo motor Media3.
- Cabeceras definitivas pendientes del CSV; no se alteran atribuciones de terceros.
- Próximo bloque: B03, pendiente de autorización. Ningún cambio visual implementado.

## B03 — Sistema visual

- Autorización: concedida expresamente; PR existente nombrado por el usuario.
- Base: `d1006710aa1b300a3e6aa862a0bdca26a13884a8`; misma rama y PR #1.
- Estado: implementación y dos revisiones completadas; rama publicada para el PR #1.
- Modificados: temas raíz/dinámico, ColorSchemes y Theme; SearchField, SearchBar,
  SettingsComponents, Skeletons y configuración/dependencias de pruebas Compose.
- Añadidos: ThemeContrastTest y VisualSystemTest; documentación y cuatro PNG en
  `docs/migration/screenshots/` (claro/oscuro, escala de texto 1 y 1.5).
- Revisión 1 (arquitectura/recursos/licencias): completada como pasada separada
  del mismo agente. Se contrastaron preferencia persistida, llamadas del tema,
  formas, fuente de sistema, alias compatibles, semántica de controles y POM de
  dependencias. No hay nuevas fuentes, iconos o imágenes de terceros importados.
- Revisión 2 (funcionamiento/integración): completada como segunda pasada del
  mismo agente. Interacción real Compose, contraste y las 11 pruebas previas
  correctas. Las capturas iniciales mostraron una etiqueta recortada al ampliar
  texto: se corrigió a dos líneas y se comprobó ausencia de elipsis/recorte vertical
  y presencia de todos los caracteres, además de revisión visual de los PNG finales.
- Compilación: assembleDebug correcto, firma APK verificada. Suite final: 16 pruebas,
  0 fallos, 0 errores, 0 omitidas. Lint sin errores y 208 advertencias.
  Se corrigió una llamada de test a la API de Robolectric; la captura PixelCopy
  no recibía callback y se sustituyó por dibujo real de la vista con gráficos
  nativos. Se afinó la comprobación de texto para no confundir redondeo de ancho
  subpíxel con truncación. No se deshabilitaron pruebas ni se cambiaron workflows.
- Commit de implementación: `7f747a32e920940b25e7a3ea3553ccf9949021dd`.
  Autor/committer `ed0668822 <ed0668822@gmail.com>`, verificados antes del commit.
- Publicación: push de `d100671` a `7f747a3` confirmado en la rama existente;
  el registro final se publica en otro commit documental (consultar `git log -2`).
- PR: https://github.com/ed0668822/Aurelis-Music/pull/1,
  «Aurelis Music Beta UI and components», confirmado OPEN por la página de GitHub.
  Se conserva su título y no se crea otro PR ni se sobrescribe una descripción de usuario desconocida.
- Actions: la consulta de runs de la rama devuelve Forbidden en api.github.com;
  estado remoto sin verificar. No se afirma CI verde ni se modifican workflows.
- Límites: sin cambios de navegación, Home, motor musical ni diseño de reproductor.
  SF Pro no se copia; validación física y cabeceras definitivas siguen pendientes.
- Próximo bloque B04 requiere autorización nueva.

## B04 — Navegación

- Autorización: concedida expresamente por el usuario tras B03.
- Base: `309c732e5051f80f198e533ac6752c3902678f64`; misma rama y PR #1.
- Estado: implementación y dos revisiones completadas; rama publicada para el PR #1.
- Modificados: MainActivity, AppNavigator, GlassNavBar, LocalMusicScreen,
  ReplayScreen, EqualizerScreen, strings y documentación.
- Añadidos: AppNavigationHost, AppNavigatorTest, NavigationIntegrationTest.
- Integración: pila única y pestañas tipadas; enlaces de Biblioteca y Ajustes;
  regreso cola→reproductor→origen, Back Android y estado por pestaña/página guardable.
  Se mantiene el motor Media3 y se evita el mini reproductor duplicado.
- Revisión 1 (arquitectura/preservación): completada como pasada separada del mismo
  agente. Se revisaron rutas/callbacks, constructor del navegador, orden de detalles,
  estados guardables pequeños, ausencia de doble controlador activo y preservación
  del arranque/Media3. Se verificaron XML y enlaces documentales; sin dependencias nuevas.
- Revisión 2 (funcionamiento/integración): completada como segunda pasada del mismo
  agente. Se ejercitó el grafo real Compose con enlaces y pantallas existentes, Atrás,
  scroll y consultas restauradas, selección accesible y retorno de cola. Se amplió el
  caso de búsqueda local para incluir texto y restauración antes de cerrar con Atrás.
  No equivale a una revisión humana independiente ni a una prueba física del servicio.
- Compilación incremental: compileDebugKotlin correcto en 26s.
- Debug: correcto; firma APK verificada. Suite final: 28 pruebas correctas,
  0 fallos, 0 errores y 0 omitidas. Lint sin errores y 205 advertencias.
- Implementación inicial publicada: `7e477befcd547966f06ba1bc0c777a1f290586b5`.
- En la revisión posterior a ese commit se detectó el caso de Canciones sin permiso:
  se evitaba invocar al ViewModel y podía quedar Loading. Se trasladó la solicitud
  de refresh a la entrada de ruta, conservando la comprobación de permisos del
  ViewModel. La prueba del grafo ahora deniega audio y verifica su Error visible.
- Tras la corrección: suite/debug/lint completos correctos en 1m 38s; 28 pruebas
  sin fallos/errores/omisiones, 205 advertencias de lint y firma APK verificada de nuevo.
- Commit de corrección: `462ecb25294694ff685aa31f2b0fa05ab550482a`.
  Push confirmado desde `7e477be` y SHA de rama remota verificado. Autor/committer
  `ed0668822 <ed0668822@gmail.com>`. Este cierre se registra en un commit documental
  posterior, sin reescribir ninguno de los commits anteriores.
- PR: https://github.com/ed0668822/Aurelis-Music/pull/1,
  «Aurelis Music Beta UI and components»; misma rama. No se crea ni renombra otro PR.
- Actions: consulta REST de runs de la rama denegada (Forbidden); estado remoto
  desconocido. No se modifican workflows ni se afirma CI verde.
- Límites: sin Home nueva, detalles álbum/artista, estadísticas ni controles EQ nuevos.
  Replay/Equalizer se exponen como estructuras existentes incompletas; no funciones simuladas.
- Próximo bloque: B05, pendiente de nueva autorización.

## B05 — Home local

- Autorización: B05 y B06 concedidos juntos; se ejecuta primero B05.
- Base: `2e82e1a2d87fc8f11452da2e5f0b0a31f597da09`; misma rama/PR #1.
- Estado: implementación y dos revisiones completadas; publicado en la rama del PR #1.
- Cambios: LocalUserLibrary y LocalHomeData nuevos; Home/hero con datos/estados reales;
  rutas y pantalla de colecciones nuevas, wiring en Application/MainActivity,
  hook ListeningStats existente implementado y transición de sesión en PlaybackViewModel.
  LocalAlbum tiene clave derivada sin colisiones; LibraryState.Error diferencia permiso.
- Secciones sin registros no muestran resultados inventados. Playlists/favoritos tienen
  persistencia privada y operaciones básicas necesarias para Home y Biblioteca.
- AutoMix en Home es selección de cola local con QueueBuilder; análisis/transiciones B11 pendientes.
- Revisión 1 (arquitectura/preservación): completada como pasada separada del mismo
  agente. Se comprobaron claves/versiones privadas, aislamiento, corrupción sin
  sobrescritura, resolución de IDs, callbacks reales de Home y cola local existente.
  Sin nuevas dependencias ni cambios de firma/identidad; alcance avanzado de B08/B11 preservado.
- Revisión 2 (integración/visual): completada como otra pasada del mismo agente.
  38 pruebas correctas, 0 fallos/errores/omitidas; debug y firma verificados; lint
  sin errores y 207 advertencias. Capturas clara/oscura revisadas y copiadas a docs.
  Se verifican creación, membresía, favorito, eliminación cancelada/confirmada y
  conservación de favoritos al borrar una lista en el grafo real.
- El caso de diálogo detectó bucle de medida; se aisló renderer y
  se corrigió fijando ancho del diálogo y acotando/desplazando su contenido, sin retirar aserciones.
- Las capturas usan el fondo de aplicación explícito, también aplicado en el host.
- Build final: exit 0, 1m 34s. Commit B05:
  `6e07a40b66e285daf2084bf64ada3063ec129f3f`, con identidad Git local del usuario.
  Push confirmado en la rama `feature/bitchord-local-migration`; no se crea otro PR.
  Actions sigue sin verificarse por Forbidden al consultar su API.
- Límites: seguimiento actual desde ViewModel, no servicio íntegro; Replay e integración
  avanzada de B08 siguen pendientes, sin afirmar estadísticas de fondo completas.
- Después de cerrar B05 se continuará B06, ya autorizado; no se pide permiso otra vez.

## B06 — Biblioteca local

- Autorización: concedida junto con B05; base `7426157e00bc0187c7068fc6eda08aad7c531636`.
- Estado: implementación, doble revisión y validación final completadas; publicado.
- Modificados: LocalMediaRepository, LocalLibrarySource, LibraryViewModel, modelos
  LocalSong/Album/Artist, permisos, MainActivity, LocalMusicScreen, colecciones,
  carrusel de artistas, puente Media3 y strings. Nuevos: LocalSearch y cuatro suites
  de pruebas (MediaStoreLibraryTest, LibraryUpdatesTest, LocalMetadataTest,
  LibraryCatalogueIntegrationTest). Actualizados los cinco documentos de migración.
- Revisión 1 (código/arquitectura): pasada separada del mismo agente; revisados
  cierre de cursor, IO/cancelación, observador y ciclo de vida, permisos, claves,
  recopilaciones y compatibilidad de referencias privadas/Media3. Sin nuevas
  dependencias, motor, permisos amplios ni cambios de firma/applicationId.
- Revisión 2 (funcionamiento/integración): segunda pasada del mismo agente, con
  proveedor MediaStore de prueba y grafo Compose real. Se ejercitan ráfagas,
  cambios durante lectura, primer plano, revocación, archivos movidos/eliminados,
  persistencia y rutas restauradas. Detectado y corregido estado sin resultados
  en álbumes/artistas. No equivale a dos revisores humanos independientes.
- Primera ejecución: 51 casos, un fallo por expectativa contradictoria con búsqueda
  sin acentos; expectativa corregida. Suite ampliada a 53 casos y aprobada;
  validación final tras corregir los estados vacíos: exit 0 en 1m 37s,
  53 pruebas, 0 fallos/errores/omitidas; APK debug y firma verificados.
  Lint: 209 advertencias y 0 errores. Actions no verificable: proxy CONNECT 403.
- Entrega: mismo PR #1 «Aurelis Music Beta UI and components», sin renombrarlo.
- Límites: instantánea completa + reconciliación, no SQL delta; rendimiento masivo,
  caché de carátula tras cambiar solo imagen y reproducción física no verificados.
  Referencias ausentes conservadas; reconstrucción de IDs no recuperada por nombre.
- Próximo bloque: B07, requiere autorización nueva. No comenzar automáticamente.

- Commit B06: `f917b59cc18a758716b0b55a9346d44f2f530a36`. Push confirmado
  desde `7426157`. Autor y committer locales: `ed0668822 <ed0668822@gmail.com>`.
- PR #1 confirmado OPEN por HTML público, 12 commits, B06 presente y título
  «Aurelis Music Beta UI and components» conservado. Este cierre se publica como
  commit documental posterior, sin reescribir historial. Actions sigue pendiente.
- B05/B06 cerrados. Detener aquí y esperar autorización expresa para B07.

## B07 — Reproductor

- Autorización: concedida expresamente después de entregar B05/B06.
- Base: `15582e8139173802bc01ba7046a38d461e7ae03c`; misma rama y PR #1.
- Estado: implementación, doble revisión y validación completadas; publicado.
- Modificados: PlaybackViewModel, PlayerConnection, MediaItemExtensions,
  MiniPlayer, GlassNavBar, AnimatedArtwork, PlayerControls, PlayerSlider,
  NowPlayingScreen, QueueScreen, MainActivity y strings. Nuevos tests:
  RecordingPlayer (solo fixture), PlaybackIntegrationTest, PlayerIntegrationTest
  y MediaSessionBridgeTest; documentación/capturas actualizadas al cierre.
- Revisión 1 (código/arquitectura): pasada separada del mismo agente, revisando
  propiedad única del motor/servicio, cancelación/conexión, eventos externos,
  conservación de metadatos, rangos de seek, callbacks y preservación de DSP,
  letras, identidad y licencias. Se retiró el conversor duplicado y la mutación
  doble de cola tras callbacks sincrónicos. No se añaden dependencias.
- Revisión 2 (funcionamiento/integración): completada como otra pasada del mismo
  agente; 66 pruebas correctas, incluidas 9 de eventos Media3, 1 de transporte real
  MediaSession/MediaController y 3 de grafo Compose (vertical/horizontal). Capturas
  inspeccionadas y guardadas. Son pasadas del mismo agente; no se presentan como
  dos revisores humanos independientes.
- Primera suite completa: 65 casos y una aserción ambigua por título repetido en
  Home/barra. Se acotó al mini reproductor, conservando la comprobación. Suite final
  ampliada: 66 casos, 0 fallos/errores/omisiones; build exit 0 en 1m 51s.
- APK debug y firma verificados; lint sin errores y 208 advertencias. XML válido
  sin recursos duplicados; git diff --check correcto. Sin pruebas release.
- Actions: consulta de runs bloqueada por proxy CONNECT 403, estado remoto pendiente.
- PR: https://github.com/ed0668822/Aurelis-Music/pull/1, mismo título del usuario.
- Límites: no pruebas físicas de decodificación/DSP, notificación, bloqueo,
  Bluetooth/foco ni sesiones de larga duración. Integración avanzada de B08,
  letras B09 y AutoMix B11 conservan su alcance; ningún bloque siguiente autorizado.
- Próximo bloque: B08, esperar autorización expresa.

- Commit B07: `26b50820291b8e9d92e8469e8cb292b6eb4d8ec1`; push confirmado
  desde `15582e8` a la rama existente. Autor/committer verificados:
  `ed0668822 <ed0668822@gmail.com>`; configuración únicamente local al repositorio.
- PR #1 confirmado OPEN por HTML público, 14 commits y B07 presente; título
  «Aurelis Music Beta UI and components» conservado. No se cambia descripción,
  no se crea otro PR y no se hace merge. Este cierre se publica como otro commit
  documental, sin reescribir historial. Actions no verificable por proxy 403.
- B07 cerrado con comprobaciones físicas pendientes; detener antes de B08.

## B08 — Playlists, favoritos, historial y cola

- Autorización: B08/B09 concedidos juntos, se cierra B08 antes de implementar B09.
- Base: `697b2b8be6aa04ccc9cefe958a23b838bc606cca`; misma rama/PR #1.
- Estado: implementación, doble revisión y publicación completadas.
- Nuevos: LocalQueueStore, ServicePlaybackRecorder, SongActionsDialog y
  ServiceCollectionsTest. Modificados: ProfileStorage, LocalUserLibrary, servicio,
  PlaybackViewModel, MainActivity, NowPlayingScreen, ReplayScreen, QueueScreen,
  LocalCollectionScreen, RecordingPlayer y PlayerIntegrationTest; cinco documentos.
- Revisión 1: pasada separada del mismo agente sobre propietario único del registro
  de escucha, pausa/buffering/seek, restauración sin autoplay, carrera con selección
  nueva, aislamiento/versiones privadas, IDs ausentes y referencias de listas.
- Revisión 2: segunda pasada del mismo agente, 73 pruebas correctas, sin fallos,
  errores ni omitidas. Incluye tiempo frente a seek, versiones desconocidas,
  aislamiento de cola, orden, favorito/playlist/append y borrado confirmado.
- Validación final: exit 0 en 1m 49s; APK debug y firma verificados; lint sin
  errores y 209 advertencias. Fallo inicial de prueba: comparación Integer/Long,
  corregida conservando el valor esperado. No se afirman revisores humanos distintos.
- Actions: API bloqueada por proxy CONNECT 403; no se afirma CI verde.
- Límites: historial agregado por canción, no diario de eventos ni Replay anual;
  recuperación depende de IDs MediaStore. Registro de segundo plano conectado al
  servicio y probado con eventos controlados, sin prueba física de larga duración.
- Siguiente bloque: B09 ya autorizado; B10 no autorizado.

- Publicación B08 confirmada: `75d2d6bc3faf00dd0c79f46128f9af30438889f9`,
  push desde `697b2b8` a la rama del PR #1; identidad local del usuario.

## B09 — Letras sincronizadas

- Autorización: concedida junto a B08; base `75d2d6bc3faf00dd0c79f46128f9af30438889f9`.
- Estado: implementación, doble revisión y publicación completadas; B10 no iniciado.
- Nuevos: LocalLyricsRepository, LocalLyricsTest y LyricsPanelTest. Modificados:
  parser Lyrics/LrcLib, lector EmbeddedLyrics, LyricsSource/LyricsHttp,
  PlaybackViewModel, NowPlayingScreen, SyncedLyricsPanel, PlayerIntegrationTest
  y documentación. Captura de prueba local prevista al cierre.
- Revisión 1: pasada separada del mismo agente; prioridad local, ámbitos de archivos,
  tamaño/formatos, metadatos frente a versión de canción, red explícita, cancelación
  y generación de solicitudes, escrituras atómicas serializadas, offsets y ausencia
  de tiempos ficticios para texto plano. Sin nuevas dependencias ni motor musical.
- Revisión 2: segunda pasada del mismo agente, completada. Parser/repositorio/panel
  y grafo de importación/seek con offset correctos tras procesar el looper Android en el test.
  Se conserva la comprobación sobre el estado real, sin sustituirlo por un mock.
  Captura detectó contraste y aislamiento de overlay, corregidos y revisados visualmente.
  No se afirman dos revisores humanos independientes.
- Validación final: 85 casos, 0 fallos/errores/omitidos; exit 0 en 1m 43s, APK debug
  y firma verificados, lint 0 errores y 210 advertencias; XML/diff correctos.
- Actions: API bloqueada por proxy CONNECT 403; estado remoto no verificable.
- Límites: importación explícita SAF por restricciones de almacenamiento Android;
  no se promete lectura automática de LRC arbitrarios junto al audio sin permiso.
  ID3v2.3/2.4 USLT y comentarios FLAC compatibles; SYLT, etiquetas comprimidas/
  unsynchronized y otros contenedores pendientes. LRCLIB no probado en vivo por
  política de red; no se descargaron ni inventaron letras reales en verificaciones.
- Próximo bloque B10 requiere autorización nueva.

- Commit B09: `cb4ba329a2146bc007b8670c9c2d54d86a01a916`; push confirmado
  desde B08 `75d2d6b` a la misma rama. Autor/committer del usuario verificados.
- PR #1 confirmado OPEN por HTML público con 17 commits y B08/B09 presentes;
  título «Aurelis Music Beta UI and components» conservado. Rama remota coincide
  con el commit B09. No se crea otro PR, no se renombra, no se hace merge.
- Este cierre se publica como commit documental posterior sin reescribir historial.
  B08 y B09 cerrados; detener y esperar autorización para B10. Mantener pendientes
  pruebas físicas y verificación de LRCLIB/Actions en vivo, sin repetir la auditoría.

## B10 — Traducción

- Autorización: B10 y B11 concedidos juntos; ejecutar primero B10.
- Base: `3eb1867a812c6b9ba2b2ebcf42b80fbc4fe3dde2`; misma rama y PR #1.
- Estado: implementación, doble revisión y publicación completadas.
- Modificados: LyricsTranslation, TranslationLanguages, SettingsRepository,
  AurelisApplication, PlaybackViewModel, TranslationLanguageDialog,
  SyncedLyricsPanel y NowPlayingScreen; nuevo TranslationTest y ampliación de
  PlayerIntegrationTest. Cinco documentos y captura bilingüe al cierre.
- Revisión 1: pasada separada del mismo agente sobre consentimiento explícito,
  configuración HTTPS sin secretos, protocolo/lotes, aislamiento de caché v4,
  límites/escritura atómica, cancelación, generación y preservación del original.
  No se usan endpoints Google/Input Tools en la ruta activa de traducción.
- Revisión 2: segunda pasada del mismo agente completada. 93 pruebas correctas,
  0 fallos/errores/omitidas, incluidas protocolo, caché tras expulsión de memoria,
  aislamiento por contenido/idioma/servidor, errores y cancelación; diálogo cancelado
  no envía, original bilingüe intacto y traducción atrasada descartada.
  Captura bilingüe inspeccionada; no se afirman revisores humanos independientes.
- Build final: exit 0 en 1m 55s, APK debug y firma verificados; lint 0 errores y
  210 advertencias. Actions bloqueado por proxy CONNECT 403, estado pendiente.
- Límite operativo: necesita servidor LibreTranslate-compatible autorizado, sin
  clave, configurado por el usuario. No hay servidor desplegado ni traducción
  remota en vivo verificada. Fixtures solo bajo tests, nunca letras de producción.
- Próximo bloque B11 ya autorizado; B12 no autorizado.

- Publicación B10 confirmada: `248461abf63ef4ad83c15640dcf7c54f97718a5f`,
  push desde `3eb1867` a la rama del PR #1, identidad local del usuario.

## B11 — AutoMix local

- Autorización: concedida junto con B10; misma rama y PR #1.
- Base: `248461abf63ef4ad83c15640dcf7c54f97718a5f`.
- Estado: implementación, doble revisión y publicación completadas.
- Nuevo: LocalAutoMix y LocalAutoMixTest. Modificados: modelo LocalSong, scanner
  MediaStore, puente MediaItem, QueueBuilder, PlaybackViewModel, HomeScreen,
  MainActivity, MediaStoreLibraryTest, LocalMetadataTest y PlayerIntegrationTest;
  cinco documentos y captura de cola al cierre.
- Revisión 1: pasada separada del mismo agente; fuente real BitChord leída antes
  de implementar, límites/licencias separados de selección local, ausencia de red
  y BPM inventado, ID/grabación, artistas/colaboraciones, reloj/recencia, semilla,
  cancelación y preservación de la instancia/posición del tema actual.
- Revisión 2: segunda pasada del mismo agente completada: 101 casos, sin fallos,
  errores u omisiones. Selección, género leído/roundtrip, duplicados/caps/recencia,
  determinismo/variación, Home→cola preservando instancia/posición y error sin borrar.
  Captura de cola inspeccionada. Sin afirmar revisores humanos independientes.
- Validación final: exit 0 en 1m 53s, 57 tareas (15 ejecutadas), APK debug y firma
  verificados; lint 0 errores y 210 advertencias, diff correcto. Actions bloqueado
  por proxy CONNECT 403; estado remoto pendiente.
- Límites: mezcla finita por metadatos, sin radio infinita, análisis PCM, crossfade
  ni beatmatching activados. Sin perfiles múltiples todavía (B12). Rendimiento en
  catálogo masivo y reproducción/consumo físicos no medidos.
- Próximo bloque: B12 requiere autorización nueva.

- Commit B11: `75b885786628d8cb012d2b667ffea4d2c3056040`, push confirmado
  desde B10 `248461a`. Autor y committer locales verificados como
  `ed0668822 <ed0668822@gmail.com>`, sin cambios globales ni historial reescrito.
- PR #1 confirmado OPEN por HTML público con 20 commits y B10/B11 presentes;
  título «Aurelis Music Beta UI and components» conservado. SHA remoto coincide
  con B11. No se crea otro PR, no se cambia descripción ni se hace merge.
- Este cierre se publica en un commit documental posterior. B10 y B11 cerrados;
  detener antes de B12. Recordar requisitos de servidor de traducción y límites
  de AutoMix por metadatos, y pendientes de hardware/Actions/licencias previas.

## B13 — Ajustes y animaciones

- Autorización: B13 y B14 concedidos juntos; B12 no autorizado, no implementado.
- Base: `c115b1bdc2eb32383ef69071c7007969271b1739`; misma rama/PR #1.
- Estado: implementación, doble revisión y publicación completadas.
- Modificados: SettingsScreen/Repository, EqualizerScreen/Processor, servicio,
  tema/Haze/LiquidGlass, panel/NowPlaying/Queue, diálogo traducción y tests de letras.
  Nuevos: AudioSettingsBinding, EqualizerProcessorTest, SettingsPersistenceTest
  y SettingsIntegrationTest; documentos/capturas actualizados al cierre.
- Revisión 1: pasada separada del mismo agente; se detectaron controles sin efecto
  y bandas ignoradas. Revisados propiedad de filtros, snapshot/coeficientes, estados,
  persistencia, Nyquist, ciclo de vida y ausencia de motor duplicado. Preferencias
  legacy no se borran; no se anuncian Dolby/crossfade/loudness inexistentes.
- Revisión 2: segunda pasada del mismo agente completada; 114 casos sin fallos,
  errores ni omisiones. Señales PCM confirman respuesta espectral, bypass,
  balance, continuidad y límites. Configuración servicio/UI/persistencia y efectos
  probados. Fallo AGSL de captura corregido usando borde ligero portable; no se
  desactivó el renderer nativo ni se retiraron aserciones. Mismo agente en ambas
  pasadas, no revisores humanos independientes.
- B14 autorizado a continuación; no permite declarar completa la migración sin
  B12, CSV de licencias, pruebas físicas y verificaciones remotas pendientes.

- B13 validación final: exit 0 en 2m 3s, 57 tareas (17 ejecutadas), APK debug y
  firma verificados; lint 0 errores y 211 advertencias. Capturas clara/oscura/EQ
  inspeccionadas. También se corrigió contraste de etiquetas sobre fondo oscuro
  usando Surface con color de contenido explícito; se repitió la suite completa.
- Actions: API bloqueada previamente por proxy; consultar entrega del bloque.

- Publicación B13 confirmada: `640b936179e95356fc58df6aa458e3d0858fd226`,
  push desde `c115b1b`, identidad Git local del usuario. Actions API CONNECT 403.

## B14 — Integración y revisión final

- Autorización: concedida junto con B13; B12 sigue no autorizado y pendiente.
- Base: `640b936179e95356fc58df6aa458e3d0858fd226`; rama/PR #1 existentes.
- Estado: verificaciones locales completadas; cierre global pendiente.
- Cambios: workflow SDK 36 y gate de lint/tests/build; build.gradle empaqueta
  avisos legales; NOTICE corregido con licencias POM. LICENSES añade textos íntegros
  de terceros. RobolectricActivityRule y ocho suites registran actividad solo en
  tests para release. Inventario TSV, evidencia APK y RELEASE_READINESS nuevos.
- Revisión 1: pasada separada del mismo agente sobre Gradle/CI, alcance real de
  pruebas, manifest, ABI/identidad/firma, licencias/copyrights y datos no autorizados.
  No se cambian secretos, claves, firma configurada ni workflows para esconder fallos.
- Revisión 2: segunda pasada del mismo agente; debug 114 y release 114 casos, todos
  sin fallos/errores/omitidos; assemble y lint de ambas variantes correctos.
  No se presentan como revisores humanos independientes ni pruebas físicas.
- Primera ejecución release: 28 casos Compose sin actividad de test; se corrigió
  el harness y se repitió todo. No se excluyó ninguna prueba ni se añadió actividad
  de test al APK release. Logs y evidencias en BUILD_STATUS/APK_EVIDENCE.
- Cierre pendiente: B12 (sin autorización), CSV requerido para cabeceras, procedencia
  exacta de exportaciones ONNX/obligaciones de fuentes de binarios y pruebas físicas.
  CI del commit de entrega se consulta después de publicar, sin asumir verde.
- Próximo trabajo: B12 solo con autorización; luego cerrar los pendientes de B14.

- Evidencia B14 local: pipeline debug/release exit0 en 3m 1s, 114 casos por variante,
  lint sin errores (211/210 advertencias), R8 y firmas correctos. Textos legales
  finales reempaquetados sin cambios de código: exit0 en51s; nueve textos verificados
  y APKs/identidad/ABI registrados en APK_EVIDENCE.json. Firma release de desarrollo
  por fallback original; no se declara publicación de producción.
- actionlint 1.7.7/YAML/bash válidos; CI cancela solo PRs sustituidos, mantiene
  todas las verificaciones en el último y no cambia main/manual.
- Microprueba AutoMix JVM17 10k temas: 494.62ms calentamiento, 311.24/194.72ms
  siguientes; no son resultados de dispositivo/batería.
- RELEASE_READINESS.md agrupa los bloqueos y requisitos de cierre.

- Entrega B14: `ecee4c1f00a8bbd7dd9876629f6fbb4735fdda11`, push confirmado
  desde B13 a la misma rama. Autor/committer del usuario verificados.
- PR #1 confirmado OPEN por HTML público, 23 commits al revisar, B13/B14 presentes
  y título «Aurelis Music Beta UI and components» conservado; rama remota coincide.
- Vista web de Actions: última ejecución observada #116
  https://github.com/ed0668822/Aurelis-Music/actions/runs/42074651656 en curso.
  La anterior #115 figura exitosa, pero las páginas específicas no permitieron
  acreditar el SHA exacto; no se traslada ese éxito a B14 ni al checkpoint posterior.
- Este cierre se publica como commit documental posterior, sin merge ni reescribir
  historial. El último commit debe consultarse en Actions antes de afirmar CI verde.
- B13 completado; B14 verificado localmente y pendiente de cierre global según
  RELEASE_READINESS. Incorporar «Formato de licencias.csv» y autorizar B12 antes
  de completar esos pendientes. No se inicia B12 automáticamente.


## B12 — Perfiles locales / B13 — integración por perfil (2026-11-05)

- Autorización: B12 y B13 concedidos expresamente. B13 ya estaba completado;
  solo se adapta su integración con los perfiles, sin repetir el bloque visual.
- Base: `2ecce6e96d15f6e033a9ee407fa9f0958504c911`, misma rama y PR #1
  «Aurelis Music Beta UI and components», sin modificar el título.
- Cambios: registro persistente privado y metadata; creación/renombrado/cambio,
  avatar privado y eliminación confirmada/reintentable. Ámbitos independientes
  para preferencias, favoritos, playlists, historial y cola. Perfil legacy intacto.
- Nuevos: ProfileAvatars, ProfilePlaybackBinding; LocalProfilesTest,
  ProfileAvatarsTest, ProfilePlaybackTest y ProfilesIntegrationTest.
- Modificados: Application, MainActivity, ProfileRepository/Storage,
  LocalUserLibrary/QueueStore, SettingsRepository, PlaybackViewModel/Service,
  Settings/NowPlaying/EqualizerScreen, fixture RecordingPlayer y pruebas de
  playback/UI; MASTER_PLAN, COMPONENT_MAP, BUILD_STATUS, LICENSE_AUDIT,
  RELEASE_READINESS y APK_EVIDENCE.
- Revisión 1: pasada de código/arquitectura completada. Revisados callbacks del
  servicio, orden guardar/cerrar/cambiar, cancelación de operaciones anteriores,
  scopes de UI, almacenamiento, corrupción, limpieza limitada y licencias.
  No se cambia identidad técnica, firma, secretos, dependencias ni motor de audio.
- Estado: B12 implementado y B13 integrado por perfil, validación local completada.
- Revisión 2: 126 casos debug y 126 release, sin fallos/errores/omitidos;
  assemble y lint de ambas variantes correctos. ZIP, firma y avisos verificados.
  Ambas revisiones son pasadas separadas del mismo agente, no dos revisores humanos.
- Primera selección: 31 pruebas correctas, sin omisiones. Se corrigieron imports
  de tests y el fixture Media3 al vaciar cola; avatares se prueban con decodificador
  nativo para no aceptar imágenes inválidas por simulación. No se excluyeron casos.
- Compilación: debug/release correctas, R8 y lint sin errores; evidencia en
  BUILD_STATUS y APK_EVIDENCE. Commit y Actions se registran tras publicar.
- Pendientes globales: CSV de licencias, procedencia/fuentes de binarios, Android
  físico y servicios externos/CI del commit exacto. B12 ya tiene autorización;
  no se debe seguir listando «B12 sin autorización» como bloqueo actual.
- Siguiente trabajo: cierre de pendientes B14 según RELEASE_READINESS; no se inicia
  automáticamente otro bloque con esta entrega B12/B13.


- Entrega B12/B13 confirmada: `30569f194d8f05ebe728caa3b51406a27ccbf96b`; push desde `2ecce6e` y
  rama remota coincidente. Autor y committer `ed0668822 <ed0668822@gmail.com>`;
  configuración exclusivamente local, sin reescribir commits anteriores.
- PR existente: https://github.com/ed0668822/Aurelis-Music/pull/1, abierto y
  título «Aurelis Music Beta UI and components» conservado.
- Actions: vista pública consultada tras el push; todavía mostraba el run anterior
  #117 exitoso (42075256890). No acredita CI del nuevo commit; comprobación del
  commit B12/B13 y checkpoint documental pendiente. API sigue no disponible según
  diagnóstico del entorno; no se cambian comprobaciones ni políticas de acceso.
- Cierre local: exit0 en 5m39s; 126 casos por variante, lint0 errores con
  218/217 advertencias debug/release. Firmas/ZIP/9 avisos/12 .so ARM64 correctos.
- Este checkpoint documental registra el commit real después de su publicación;
  no cambia el código validado ni inicia otro bloque. Próximo: pendientes B14
  de RELEASE_READINESS (CSV, procedencia/fuentes, hardware, servicios y CI).


## Nuevo plan beta-local — B00

Plan aprobado y B00 ejecutado sin cambios de código de app. Ver
[progreso vigente](beta-local/PROGRESS_TRACKER.md) y [auditoría](beta-local/B00_AUDIT.md).
126 pruebas debug +126 release ejecutadas nuevamente con --rerun; build/lint correctos.
Hallazgo: dos ELF de ONNX con LOAD4KB, pendiente B09/B10/B13 aunque ZIP16KB pase.
La numeración B00–B13 nueva no equivale a los bloques históricos. B01 no iniciado.


## Beta-local B01.1 — perfil único y respaldo

Autorizado expresamente; implementado perfil único/nombre/foto y respaldo privado
validado, preservando ámbito activo y datos legacy. Revisiones1/2 completadas,
137 tests por variante, build/lint debug/release correctos. Ver
[registro vigente](beta-local/PROGRESS_TRACKER.md) y [migración](beta-local/B01_1_MIGRATION.md).
B01.2 no iniciado. Entrega/CI se registran en el progreso vigente tras publicar.


## Nuevo plan — B01.2/.3 y B02.1/.2/.3

Autorización «continúa hasta B02» atendida en la misma rama y PR1.
Catálogo/metadatos/carpetas indexadas, fuente autorizada, cristal compartido y
componentes/gestos funcionales. R correcto,164 casos por variante; dos revisiones
separadas del mismo agente. B02_1/2/3 y checkpoint guardan archivos, resultados,
commits y limitaciones. Paridad final/SAF físico/GPU/CI actual pendientes.
No iniciar B03 sin autorización; LICENSE/identidad/firma/workflows intactos.
