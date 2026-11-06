# B08 — proveedores y traducción

Base: `32a40c935c7b1b05625e40c2527fe8f2cf751a24`; misma rama
`feature/bitchord-local-migration`, mismo PR1. Autorización del usuario para
terminar B08. No B09. No cierre retroactivo de los pendientes visuales/compartir
de B07 ni de B06. El plan principal y su orden permanecen intactos.

## Estado y dependencias

| Unidad | Implementación | Validación de esta entrega |
|---|---|---|
| B08.1 Registro/LRCLIB | Preparada y conectada | Pendiente de GitHub y Android real |
| B08.2 Evaluación de otros proveedores | Evaluados; tres deshabilitados con motivo | Guardas preparadas para pruebas; no disponibilidad remota certificada |
| B08.3 Traducción/caché | Cliente existente reforzado y conectado | Pendiente de GitHub y Android real |

La API local/temporización de B07 sirve de base. Run147 SUCCESS confirmado en
la página pública de Actions y job128195419590, cuyo enlace de commit apunta
al SHA base anterior. Esto valida el CI anterior, no los cambios de B08 ni la
aplicación en un teléfono. Véase ../../qa/B08_PROVIDERS.json.

## Arquitectura y flujo conectado

- `data/lyrics/LyricsProviderRegistry.kt`: registro extensible, LRCLIB primero y
  único proveedor activo. Las preferencias guardadas y los setters filtran fuentes
  no habilitadas. Desactivar LRCLIB persiste y no se revierte al reabrir ajustes.
- `LocalLyricsRepository.kt`: conserva importación → incrustadas → caché → red
  explícita. La nueva consulta pasa por el registro; no hay otro repositorio ni
  otro motor de reproducción. Una letra sincronizada que empieza en cero conserva
  su indicador de sincronización. El formato de caché rico de B07 no cambia.
- `LrcLib.kt`: HTTPS `/api/search`, codificación de título/artista, selección por
  título/artista/duración (tolerancia existente de cinco segundos); sin elegir una
  grabación distinta cuando no hay coincidencia. Duración comprobada localmente.
  No se afirma reconocimiento acústico ni coincidencia perfecta con metadatos malos.
- `LyricsRequest.kt`: OkHttp asíncrono, cancelación real de Call, respuestas acotadas,
  errores separados NETWORK/TIMEOUT/RATE_LIMIT/HTTP/INVALID_RESPONSE/TOO_LARGE/DISABLED.
  404 de LRCLIB y ausencia de coincidencia son resultados vacíos; 429 conserva
  Retry-After numérico (máximo 24h). Fecha HTTP en Retry-After usa mensaje genérico.
  No hay bucle de reintentos ni se imprime contenido, URL o credenciales en errores.
  Clientes de producción sin redirecciones ni reintentos de conexión automáticos.
- `PlaybackViewModel.kt`: generación/cancelación por canción y consulta; estados de
  búsqueda separados de la letra cargada. Fallar, deshabilitar o cancelar una
  consulta no borra una letra ya mostrada. Cancelación y reintento explícitos.
  No cambia comandos de reproducción, cola, servicio, MediaSession ni DSP.
- `LyricsSourcesDialog.kt`: reemplaza texto provisional por interruptor persistente,
  fuente actual, tres proveedores deshabilitados con motivo y búsqueda explícita.
  Seleccionar/cerrar no envía metadatos. Estados y cancelación en NowPlaying activo.
- Traducción: conserva servidor HTTPS configurable compatible con LibreTranslate,
  idiomas original/destino y confirmación antes del envío. Caché por canción,
  contenido, servidor e idiomas; original y traducción visibles simultáneamente.
  Límites existentes: 500 fragmentos, 30.000 caracteres totales, 3.500 por lote;
  como máximo dos solicitudes simultáneas por traducción y respuesta de 512 KiB.
  Solo guarda resultados completos; no convierte fallos o cancelación en texto
  traducido. La escritura opcional de caché no invalida una traducción completa.
  Preserva voces de fondo, marcas temporales y proyección temporal existente;
  no inventa temporización individual traducida ni traducciones.

## Evaluación B08.2 y licencias

| Proveedor | Código inspeccionado / mecanismo | Decisión |
|---|---|---|
| LRCLIB | Cliente existente; API pública descrita por su README, consulta expresamente aprobada en el plan | Activo manualmente; sin garantías sobre disponibilidad o derechos de cada letra |
| BetterLyrics | `lyrics-api.boidu.dev/getLyrics`, variante QQ `/qq/getLyrics`; ProviderLyrics | Deshabilitado: no hay condiciones acreditadas para reutilizar servicio/contenido Apple/QQ |
| BiniLyrics | `lyrics-api.binimum.org/`, primer resultado y `lyricsUrl` con TTML | Deshabilitado: condiciones sin acreditar; antes de habilitar necesita verificar grabación y destinos de documentos |
| lrc.red | búsqueda y documento TTML por ISRC; validación existente de ISRC/versión | Deshabilitado: faltan condiciones acreditadas para servicio/contenido |

Referencias de BitChord: seis archivos F12 en
`shared/src/jvmSharedMain/kotlin/com/music/bitchord/data/lyrics/`, commit fijo
`b690dc146009d4815825fa4a20815ef2e4ee8898`; copia local de solo lectura.
Conservar atribuciones de NOTICE/GPLv3 originales; LICENSE de Aurelis no cambia.
README consultado sin letras ni metadatos:
https://github.com/tranxuanthang/lrclib/blob/main/README.md.
No se incorpora código de ese servidor ni se infiere licencia del contenido a
partir de la del cliente/servidor. Los demás clientes legacy se conservan en disco,
fuera del registro y del despacho activo. Romanización/endpoints Google legacy
no se conectan. No se incluyen claves ni se activan servicios de pago.

## Verificación y punto de reanudación

18 pruebas nuevas preparadas: ocho proveedores/transporte/caché, seis traducción,
dos integración de reproducción y dos selector/UI. Se ajustan dos expectativas
existentes de TranslationTest para distinguir errores tipados de indisponibilidad
local. Se conservan todos los métodos anteriores, incluida CI10 y sus comprobaciones
de offset ±500, selección, canción, cola y pausa.

Primera revisión: arquitectura/estado/red/cancelación y alcance, estática.
Segunda revisión: recursos/UI/casos de error/regresión/licencias, estática.
Ambas realizadas en pasadas separadas por el mismo agente; no se presentan como
revisores independientes ni como pruebas ejecutadas. Se corrigió un import de
stringResource, se rechazaron objetos/números como texto traducido y se precisó
qué metadatos se transmiten durante estas revisiones.
XML nuevo parseado; nombres de recursos/referencias y diff --check correctos.

Por instrucción del usuario, NO se ejecutaron Gradle, suites Android ni APK aquí.
El workflow existente ejecutará en GitHub las suites debug/release, lint y APK:
`./gradlew :app:testDebugUnitTest :app:testReleaseUnitTest`,
`:app:lintDebug :app:lintRelease`, `:app:assembleDebug :app:assembleRelease`.
No se alteran tareas, workflow, gates o comprobaciones para eludir fallos.

Pendientes: CI de esta entrega por su SHA, ejecutar instalación/arranque y acciones
en Android real, lookup LRCLIB real y traducción con servidor autorizado del usuario,
modo avión/caché, cancelación/cambio de canción y layout/accesibilidad en teléfono.
La política de red de esta nube no habilita LRCLIB ni un servidor de traducción;
no se intentan consultas ni se eluden restricciones. Fixtures originales mediante
interceptores no acreditan disponibilidad, autorización de contenido o fidelidad visual.
Publicar en PR1, avisar al usuario y esperar su resultado antes de otra entrega.
No declarar B08 validado hasta obtener esas evidencias.
