# MASTER_PLAN — Aurelis Music beta local

> **Regla de ejecución vigente (11-11-2026):** 30 rutas como máximo por subbloque/entrega, con conteo acumulado y divisiones previas cuando proceda. Ver [PLAN_30_FILES](PLAN_30_FILES.md), que conserva los IDs y dependencias B00–B13. El estado histórico de este documento no sustituye la verificación reciente por SHA.

## Estado vigente

B01.1/.2/.3 y B02.1/.2/.3 implementados y comprobados localmente
con autorización «continúa hasta B02». Consultar PROGRESS_TRACKER y evidencias
por unidad. B03 y siguientes requieren autorización; no se ha iniciado B03.
Las observaciones de auditoría que siguen conservan la base B00.

## Estado y alcance

Plan aprobado mediante «Implement the proposed plan». La ejecución se registra por unidad en PROGRESS_TRACKER;
cada bloque requiere autorización según IMPLEMENTATION_BLOCKS. La numeración pertenece a este
nuevo plan: los documentos y bloques anteriores permanecen como historial.

- Rama: `feature/bitchord-local-migration`.
- Base: `01c1ba511b9eb8ecb3eb40c98afb46d8d17a017a`; árbol inicialmente limpio.
- Origen leído: BitChord `b690dc146009d4815825fa4a20815ef2e4ee8898`.
- Árbol: 1.396 archivos regulares, inventariados con rutas, tamaños y SHA-256 en
  [SOURCE_INVENTORY.tsv](SOURCE_INVENTORY.tsv). Clasificación por ruta, no auditoría
  semántica exhaustiva ni autorización de copia. Metadatos en SOURCE_REFERENCE.json.
- Referencia externa al repo: `/workspace/reference/BitChord-b690dc1.tar.gz`, solo
  lectura. No se modifica ni publica nada en BitChord.
- Objetivo: experiencia visual y funcional de BitChord con reproducción local,
  un perfil único, servicios online opcionales autorizados y funciones reales.
- [Correspondencia](FILE_MAPPING.md), [unidades](IMPLEMENTATION_BLOCKS.md),
  [licencias](LICENSE_AUDIT.md), [paridad](UI_PARITY_CHECKLIST.md),
  [pruebas](TEST_STRATEGY.md), [progreso](PROGRESS_TRACKER.md),
  [decisiones](DECISIONS.md).

## Base funcional y brechas demostradas

El flujo activo es MainActivity/AurelisNavigationContent → LibraryViewModel y
PlaybackViewModel → MediaController → AurelisPlaybackService → ExoPlayer/DspChain.
Una MediaSession y un servicio de reproducción declarados. El catálogo proviene
de MediaStore; LocalUserLibrary/QueueStore y preferencias persisten datos locales.
La aplicación enlaza repositorios por perfil y la UI ofrece crear/cambiar/borrar.

Hallazgos que no se consideran funciones completas:

- MainActivity selecciona para EXPLORE un texto pendiente; existe ExploreScreen,
  pero no está conectado a esa ruta.
- LocalMediaRepository.scanLocalFolders devuelve una lista vacía. El catálogo
  activo utiliza getAllSongs; no presentar el método legacy como escáner funcional.
- La pantalla activa NowPlayingScreen está en ui/screens; componentes adicionales
  en ui/player no acreditan por sí solos integración vertical/horizontal.
- SettingsScreen activo es más simple que SettingsSheet del origen; no se pegará
  una segunda función SettingsScreen que duplique contratos.
- Replay activo muestra totales/historial. LocalListening solo conserva listenedMs,
  playCount y lastPlayedAt; no permite reconstruir períodos antiguos completos.
- BeatTracker.detectBeats devuelve vacío y VocalTracker.hasVocals devuelve true.
  ScrobbleManager es no-op. AutoMixPanel contiene estado visual no conectado a DSP.
- AutoMix vigente crea colas por metadatos. No demuestra BPM, crossfade ni LoopMix.
- AutomixPerformanceMode actual y origen: 1/2/4; objetivo aprobado: 1/2/3.
- La existencia de widgets, parsers o helpers no demuestra su integración activa.

## Funciones aprobadas

Incluir Home, Explore por géneros reales, Search local, Library, carpetas, favoritos,
playlists, historial; MiniPlayer, player vertical/horizontal, panel/dock/fondos,
menús, Play Next y cola; letras avanzadas/traducción/compartir; Playback y DSP,
USB/bit-perfect; AutoMix Beta; ajustes aplicables, idiomas, Replay y respaldos;
widgets, enriquecimiento remoto opcional, Last.fm/ListenBrainz y etiquetas locales.

Excluir WebDAV, SMB, firmas gráficas de artistas, Cast, escuchar juntos/servidor,
Spotify, YouTube Music, JioSaavn, JavaScript sources, TV, Wear OS, web y selector
de icono. Aplazar canvas externo, videoclips y presencia Discord fuera de la beta.
Retirar del flujo activo referencias que contradigan exclusiones; no borrar en
masa archivos legacy sin comprobar usos ni funciones propias compatibles.

Decisiones del usuario posteriores a la consulta: perfil único y retirada de
Dolby Atmos. El MiniPlayer conserva un indicador de calidad real, no un interruptor
ni certificación Dolby. La sección Audio Quality sin opciones restantes se omite.

## Arquitectura y contratos objetivo

### Perfil y biblioteca

Conservar nombre, avatar privado, preferencias y datos del perfil activo anterior.
Nueva instalación: solo perfil local. Ante registro antiguo múltiple: respaldo
privado recuperable antes de consolidar; no fusionar historiales ni borrar datos
silenciosamente. Un ID interno legacy podrá subsistir sin selector de cuentas.
Retirar APIs multiusuario del flujo activo y simplificar el binding de playback.

Reutilizar LocalSong/LocalLibrarySource/LibraryViewModel. Extender metadatos de
volumen/carpeta/procedencia sin reidentificar canciones por título aproximado.
MediaStore y SAF autorizado; sin acceso general al filesystem. Géneros de
MediaStore y etiquetas compatibles, desconocido → «Sin clasificar». Las tarjetas
abren canciones reales. Search incluye canciones/álbumes/artistas/playlists/géneros.
Storage muestra canciones y suma de tamaños conocidos, indicando desconocidos;
no muestra el tamaño del APK/caché como música. Sin desglose por formato en beta.

### UI y sesión

Mantener AppNavigator y la entrada del reproductor de ui/screens como integración;
componentes de ui/player se adaptan detrás de ella, sin otro estado musical.
Instalar los contratos locales de AppUi antes de usar componentes que requieren
host; no simular descargas/cuentas remotas con datos ficticios para compilar.
Roboto del sistema por defecto y Vazirmatn autorizada como opción; sin SF Pro.
Medir layouts/gestos contra referencia y registrar cambios de identidad/fuentes.

Mantener AurelisPlaybackService y una MediaSession pública con Player lógico
estable. LocalPlaybackCoordinator será propietario de cola canónica, foco,
restauración, comandos, temporizador y transiciones. MediaController seguirá siendo
la interfaz de UI/notificación/widgets. Una entrada de cola tendrá identidad de
ocurrencia para conservar duplicados y orden al cambiar decks.

Dos decks ExoPlayer internos solo para crossfade/AutoMix: coordinados dentro del
mismo servicio, sin sesiones extra. Definir handoff único, cancelación de mezcla
por seek/cambios de cola/dispositivo y liberación del standby. Bit-perfect impide
mezcla, velocidad alterada y DSP incompatible, con explicación visible y estado de
salida real; no presentar fallback como bit-perfect activo.

AudioQualityInfo separa formato/códec del archivo y formato de salida. Frecuencia,
profundidad y bitrate solo cuando se conocen mediante lector/decoder/sink. Un
archivo con calidad desconocida no recibe etiquetas manuales ni datos de ejemplo.
El indicador del MiniPlayer abre detalles accesibles; no promete mejora del sonido.

### Historial, Replay e importación

SQLite nativo con SQLiteOpenHelper para eventos temporales y outbox de scrobbling,
sin Room/KSP. Conservar preferencias/JSON funcionales actuales. Registrar desde el
servicio fuera del hilo de audio; eventos identificables para evitar duplicados.
Tiempo global una vez durante mezcla y contribuciones reales de cada canción.
Totales anteriores como histórico previo; no inventar fechas/gráficas retroactivas.

Respaldos versionados de datos, no música ni secretos. Importar: validar, mostrar
resumen, confirmar restauración, respaldar estado previo y usar staging/journal
para recuperar/rollback tras fallo entre SQLite y preferencias. No fusionar
implícitamente ni convertir datos inválidos en colecciones vacías.

### AutoMix y procesamiento

Separar recomendaciones locales, análisis y mezcla. Reemplazar stubs por análisis
real y cachear por archivo/tamaño/modificación/versión de modelo/analizador.
Un archivo en análisis a la vez; ONNX intraop 1/2/3 e interop 1. Balanceado=2 por
defecto. Evitar multiplicar sesiones/trabajos. Cambiar modo recrea sesión tras
terminar/cancelar con seguridad. Hilos UI/audio no forman parte del presupuesto
anunciado de inferencia. Medir configuración efectiva y ejecución.

La reproducción tiene prioridad; análisis cancelable fuera de main/audio. Plan
sin análisis usable recurre a crossfade configurado o reproducción normal con
motivo observable. Ganancias/transiciones deben evitar clipping y probarse; no
prometer sincronización a nivel de muestra entre dos ExoPlayers. LoopMix depende
de permisos legales, compatibilidad nativa y escucha real. Mantener indicador Beta.

### Red y archivos

Audio local funciona offline. Letras: local → caché → búsqueda online explícita.
LRCLIB primero; evaluar BetterLyrics/BiniLyrics/LRC Red antes de habilitar cada uno.
Conservar cliente configurable de traducción; no reactivar endpoints legacy.
Carátulas/metadatos remotos son opt-in, overlays que no sobrescriben etiquetas.
Last.fm/ListenBrainz son conexiones opcionales, no cuentas de Aurelis. Secretos en
Keystore, fuera de logs, APK publicado y exportaciones. Falta de claves autorizadas
implica estado indisponible; no crear backend ni inventar credenciales.

Etiquetas: guardar copia es operación predeterminada; comprobar reproducción y
metadatos de salida. Sustituir original requiere autorización Android, confirmación
y recuperación comprobada; no afirmar atomicidad del proveedor si no existe.
Sin Downloader/DownloadService ni descargas musicales de streaming.

## Plataforma y límites protegidos

Aurelis: Kotlin2.2.21, AGP8.10.1, compile36/target35/min33, Media3 1.11.1,
ONNX1.19.2 y ARM64. BitChord: Kotlin2.3.20, compile37/target36/min26,
Media3 1.11.0 y ONNX1.28.0. Mantener base actual; incompatibilidad demostrada
obliga a propuesta específica y validación, no a importar Gradle entero.

No cambiar LICENSE, applicationId/namespace com.music.aurelis, firma, secretos,
enlaces oficiales ni identidad. No actualizar SDK solo por dependencias streaming
excluidas. No modificar source BitChord ni añadir otro servicio. Reutilizar
[DEPENDENCIES_RELEASE.tsv](../DEPENDENCIES_RELEASE.tsv) como auditoría histórica,
no como certificación de todos los binarios.

## Cierre

Riesgos: datos/perfil, funciones desconectadas, decks/cola/escucha, DSP/USB,
versiones/JNI/ONNX, modelos/licencias/fuentes, rendimiento, historia incompleta,
escritura de música y fidelidad no acreditada. B13 exige pruebas físicas y legales
pendientes; compilación o capturas aisladas no acreditan funcionalidad completa.
