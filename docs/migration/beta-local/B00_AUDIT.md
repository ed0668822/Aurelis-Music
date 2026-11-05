# B00_AUDIT — resultado verificable

## Alcance y acceso

B00.1–B00.3 del nuevo plan beta-local; no implementación B01. Base01c1ba5,
rama feature/bitchord-local-migration, árbol inicial limpio. Git ls-remote confirmó
main de BitChord en b690dc146009d4815825fa4a20815ef2e4ee8898. Archivo codeload del
commit leído/enumerado:1.396 archivos. SOURCE_REFERENCE fija URL/SHA; inventario
completo en SOURCE_INVENTORY. La clasificación es por ruta y no autoriza copia.
La lectura selectiva reutiliza investigación previa de esta sesión, sin repetir
análisis completo ni alegar ejecución de BitChord.

## Arquitectura activa y puntos de entrada

| Evidencia | Hallazgo | Trabajo futuro |
|---|---|---|
| MainActivity.kt AurelisNavigationContent | Conecta VM de biblioteca/player; ruta EXPLORE usa navigation_explore_pending | B03/B04 |
| AurelisApplication.kt + ProfileRepository/Storage | libraries StateFlow y callbacks por ámbito; registry v1 múltiple | B01 migración respaldada |
| SettingsScreen.kt create/switch/delete | Selector múltiple real | Retirar del flujo, mantener perfil activo/datos |
| LocalMediaRepository.kt getAllSongs/scanLocalFolders | MediaStore con género/tamaño; método File legacy vacío | B01, sin prometer filesystem scan actual |
| LocalLibrarySource/LibraryViewModel | ContentObserver, permisos y refresh debounced | Preservar, ampliar carpetas/metadata |
| AurelisPlaybackService/PlayerConnection/PlaybackViewModel | Una ExoPlayer/MediaSession; MediaController; servicio único en manifest | B06 coordinador; B10 decks internos |
| MainActivity ui/screens/NowPlayingScreen | Reproductor activo más simple que componentes player heredados | B05 conexión visual |
| LocalLyricsRepository/LyricsTranslation | Local/caché/lookup explícito y servicio configurable | B07/B08; no legacy endpoint automático |
| LocalUserLibrary.LocalListening | Solo total/count/lastPlayedAt | Eventos nuevos para Replay sin cronología ficticia |
| BeatTracker/VocalTracker | emptyList/true constantes | B10 análisis real |
| AutoMixPanel + LocalAutoMix | Sliders locales visuales y cola por metadatos | B10; no mezcla avanzada acreditada |
| ScrobbleManager | no-op | B12 conexiones opt-in |
| AutomixPerformanceMode | PERFORMANCE4 | B10 nuevo1/2/3, default2 |

## Dependencias y límites de importación

Aurelis Kotlin2.2.21/AGP8.10.1/SDK36-min33-target35/Media3 1.11.1/ORT1.19.2;
BitChord Kotlin2.3.20/SDK37-min26-target36/Media3 1.11.0/ORT1.28.0. No upgrades en B00.
175 módulos/constraints/BOMs resueltos, mismas versiones que inventario anterior.
DEPENDENCY_INVENTORY conserva licencias POM históricas solo si coincide versión;
no equivale a175 binarios ni certificación legal. No nuevas dependencias.

## Revisión1 — código/arquitectura y plan

Pasada separada del mismo agente: rutas activas, almacenamiento, servicio/estado,
API locales y correspondencia.180 expresiones/rutas origen/destino de la matriz
resuelven contra árbol fijado/repositorio real; enlaces locales correctos.14 fichas
con16 campos y48 subunidades que heredan esos campos con alcance/aceptación propios.
Se corrigieron errores del texto propuesto al comprobar archivos: LocalAutoMix
está en playback; PlayerLyrics es nuevo en destino; LrcRed ya existe. Cambios
exclusivamente documentales. No se cambió LICENSE/NOTICE/firma/Gradle/manifest/workflow.

## Revisión2 — integración y regresiones de base

Comando íntegro y hashes en B00_EVIDENCE.json. Se forzaron solo las tareas de tests
con --rerun, conservando caché del resto donde era válido. No exclusiones ni
supresión de checks. `BUILD SUCCESSFUL in 58s`,120 tareas (11 ejecutadas/109 cache).
Debug126 y release126 casos, sin fallos/errores/omitidos. Lint0 errores,218/217
advertencias previas. No se afirma que todos esos tests prueben las nuevas funciones
ni los cambios futuros. actionlint1.7.7 exit0 sobre workflow existente.

Firmas APK e integridad ZIP correctas. Firma de desarrollo/fallback original,
no firma de publicación. Estado de cada ELF/SHA de APK registrado; los hashes
corresponden a artefactos locales, no prueban publicación de una release.

### Hallazgo nativo crítico para el cierre

ZIP alignment16KB correcto en debug y release. Sin embargo libonnxruntime.so y
libonnxruntime4j_jni.so presentan LOAD alignment4096. No acreditan soporte nativo
16KB; tampoco se ejecutó un dispositivo16KB para afirmar un crash observado.
B09/B10 deben resolver/verificar el runtime compatible antes de cerrar B13.
No se sustituyen binarios ni se eleva ORT silenciosamente en B00. El resto de los
LOAD verificados cumple el umbral de alineación estática; no es prueba hardware.

## Pendientes y siguiente unidad

No ejecutados dispositivo/emulador, USB/DAC/Bluetooth, pruebas audibles, BitChord,
fidelidad visual, servicios de terceros en vivo ni certificación legal. Quedan
CSV, procedencia exacta de modelos y fuentes/configuración de binarios; detalles
en LICENSE_AUDIT. SDK XMLv3/v4 emitió aviso de herramientas, sin fallo de build.

Siguiente unidad: B01.1 perfil único y respaldo, solo después de autorización.
CI/commit/PR de esta entrega se registran tras publicar. No se hace merge.
