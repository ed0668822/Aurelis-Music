# Migración BitChord → Aurelis Music

## Alcance y referencias

Documento iniciado en B01 (auditoría y planificación). B02–B11 fueron autorizados
después e incorporan únicamente sus secciones específicas. B10 y B11 autorizados juntos; B12 y siguientes no están autorizados.
Documentación redactada el 4 de noviembre de 2026 (America/El_Salvador).

- Destino: `ed0668822/Aurelis-Music`, base `9bf3fd3db8f5870f942f5167dca5bc77e3c48edd`.
- Origen: `kushagrasinghx/BitChord`, referencia fija `b690dc146009d4815825fa4a20815ef2e4ee8898`.
- BitChord se consultó exclusivamente en lectura mediante Git HTTPS y archivos de GitHub; no se modificó ni publicó nada allí.
- Público: usuarios de música almacenada en su dispositivo y futuros mantenedores de Aurelis.
- Éxito final: experiencia visual cercana a BitChord, reproducción local sin Internet, funciones reales, datos persistentes y sin regresiones demostradas.

Consultar [mapa](COMPONENT_MAP.md), [licencias](LICENSE_AUDIT.md),
[validación](BUILD_STATUS.md) y [progreso](PROGRESS.md) antes de continuar.
Cada tarea cloud ya está aislada: utilizar el checkout existente, sin worktrees salvo petición expresa.

## Arquitectura y límites

Conservar `com.music.aurelis`, las firmas, publicación y funciones existentes compatibles.
Mantener `AurelisPlaybackService` (MediaSessionService + ExoPlayer) y su cadena DSP;
conectar las pantallas a su controlador, sin añadir otro motor de reproducción.
Los modelos `LocalSong`, `LocalAlbum` y `LocalArtist`, MediaStore y repositorios actuales
son el punto de partida. No sustituirlos solo para imitar la estructura del origen.

Flujo previsto: permisos → MediaStore → repositorio local → estado de biblioteca →
Home/biblioteca/búsqueda → controlador Media3 → servicio y controles Android.
Favoritos, listas e historial alimentarán Home y recomendaciones. B02 definirá un
identificador de perfil estable para esos datos; B12 implementará su gestión visual.
No elegir una nueva base de datos sin comprobar primero la persistencia existente y
su migración: esa decisión corresponde a la especificación autorizada de B02.

Letras: archivo LRC e incrustadas → caché → proveedor externo permitido; traducción
separada, cancelable y cacheada. La disponibilidad de un endpoint no acredita permiso
de uso. Reproducción local no dependerá de proveedores de letras o traducción.
AutoMix separará selección local, análisis y transiciones; conservará una cola
reproducible cuando no existan metadatos o características musicales suficientes.

No importar módulos de streaming, autenticación remota, TV, Wear OS o escritorio.
No eliminar funciones originales en esta migración sin autorización. No copiar
recursos o código con procedencia no resuelta. No introducir credenciales locales
para perfiles salvo requisito posterior; nunca contraseñas en texto plano.

## Dependencias y compatibilidad

| Área | Aurelis base | BitChord inspeccionado | Decisión |
|---|---|---|---|
| Estructura | módulo Android `app` | `shared`, `sharedUi`, app y otras plataformas | Adaptar piezas; no importar el monorepo |
| AGP / Gradle | 8.10.1 / wrapper 8.11.1 | AGP 8.10.1 | Conservar toolchain destino inicialmente |
| Kotlin | 2.2.21 | 2.3.20 | Adaptar APIs antes de proponer actualización |
| SDK compile/min/target | 36 / 33 / 35 | teléfono 37 / 26 / 36 | Conservar identidad y compatibilidad destino |
| Media3 | 1.11.1 | 1.11.0 | No degradar ni copiar dependencias de streaming |
| Compose BOM | 2024.12.01 | mismo BOM más Foundation 1.10.0 explícita | Revisar cada API visual importada |
| Nativo destino | NDK 26.1.10909125, CMake 3.22.1, ARM64 | DSP y componentes con licencias individuales | Auditar procedencia antes de adaptar |

## Bloques y aceptación

El orden B01–B14 se mantiene. Cada fila requiere autorización explícita independiente;
no representa permiso para ejecutarla. Dificultad relativa, sin promesas de tiempo o créditos.

| Bloque | Trabajo y áreas previstas | Dificultad / riesgo | Aceptación y comprobación |
|---|---|---|---|
| B01 | Los cinco documentos de esta carpeta | Media: precisión de evidencias/licencias | Dos revisiones; diff documental; entrega trazable |
| B02 | `data`, modelos, servicios y persistencia existentes | Alta: pérdida de datos y duplicación | Contratos locales y perfil definidos; migración probada con datos previos; build |
| B03 | Temas, componentes compartidos y recursos autorizados | Media: APIs Compose y derechos de recursos | Claro/oscuro, tipografía accesible, capturas y compilación |
| B04 | Navegación existente, barra inferior y rutas | Media: pilas de retorno | Atrás, restauración, pestañas y apertura del reproductor sin duplicaciones |
| B05 | Home y componentes `home` | Alta: datos vacíos o simulados | Continuar, recientes, más escuchadas, álbumes, artistas, favoritos, listas, añadidas, recomendaciones y AutoMix con datos reales; carga/vacío/error/permisos |
| B06 | MediaStore, permisos, biblioteca y búsqueda | Alta: URI obsoleta y coste de escaneo | Revocación, búsqueda/orden/filtros, metadatos/carátulas, altas/bajas/movidos y actualización incremental |
| B07 | Mini/reproductor completo y host Media3 existentes | Alta: regresión de audio | Play/pausa, anterior/siguiente, seek, repetición, aleatorio, cola, fondo, notificación y bloqueo en dispositivo |
| B08 | Persistencia de favoritos/listas/historial y cola | Alta: integridad de relaciones | Reinicio conserva datos, acciones idempotentes y referencias a canciones eliminadas seguras |
| B09 | Repositorio, parsers y paneles de letras | Alta: sincronización y proveedores | LRC/incrustadas/caché antes de red; seek, resaltado, scroll y ausencia de letras con fixtures |
| B10 | Traducción, caché y presentación bilingüe | Media: endpoint y privacidad | Original intacto, idioma y caché correctos; offline, timeout y cancelación explícitos |
| B11 | Selección local y análisis/transiciones existentes | Alta: licencias, DSP y batería | Cola exclusivamente local, sin repeticiones excesivas; fallback sin BPM/género; límites y consumo documentados |
| B12 | Perfiles y asociaciones preparadas en B02 | Alta: aislamiento y migración | Crear/cambiar/borrar con confirmación; avatar opcional; favoritos/listas/historial separados sin Internet |
| B13 | Ajustes, animaciones y personalización | Media: rendimiento y accesibilidad | Persistencia, reducción de movimiento, fluidez y regresiones medidas |
| B14 | Integración, optimización y auditoría final | Alta: compatibilidad completa | Debug/release, pruebas relevantes, dispositivo, licencias y CI con estados independientes |

Dividir un bloque grande en subbloques antes de implementarlo: cada uno tendrá diff
acotado, criterios, revisiones y commit. B09 y B10 son prioridad alta dentro del orden
solicitado; no adelantarlos sin autorización. B11 depende de biblioteca, historial y
cola; B12 depende del diseño de aislamiento de B02. No simular una función pendiente.

## Protocolo de trabajo y entrega

1. Leer esta carpeta, comprobar Git y ejecutar únicamente el bloque autorizado.
2. Inspeccionar solo componentes afectados; actualizar mapa y riesgos con evidencia.
3. Implementar sin cambios de identidad, secretos, firmas o dependencias innecesarias.
4. Revisión 1: arquitectura, imports, recursos, licencias, duplicaciones y preservación.
5. Revisión 2 separada: comportamiento, integración, build/pruebas pertinentes y CI disponible.
6. Corregir hallazgos y repetir solo verificaciones afectadas; registrar pendientes reales.
7. Actualizar documentos, commit y PR sin force push, merge automático ni borrado de ramas ajenas.
8. Consultar Actions si existe acceso; no alterar workflows para ocultar fallos.
9. Parar antes del siguiente bloque. Mantener un punto seguro de reanudación.

Para B01, el diff es exclusivamente documental: `git diff --check`, enlaces/rutas y
coherencia de referencias son las comprobaciones requeridas. La compilación previa
sirve de línea base, no de prueba funcional ni de CI del futuro commit.
No repetir builds completos sin cambios que los justifiquen.

## B02 — Decisiones de arquitectura implementadas

Autorizado expresamente después de B01. Su alcance es la base de almacenamiento,
identidad y lectura local; no adelanta UI de perfiles (B12), CRUD de listas/historial
(B08), nuevo escáner incremental (B06) ni cambios visuales (B03).

- `data/model/ProfileId.kt`: identidad estable, independiente del nombre visible.
  El perfil heredado es `ProfileId.DEFAULT` (`default`). IDs de 1–64 caracteres
  ASCII minúsculos, dígitos, guion o guion bajo, empezando por letra/dígito.
  No derivar IDs del nombre editable; B12 podrá generar identificadores UUID.
- `data/ProfileStorage.kt`: límite de persistencia privada por perfil y área.
  `open(profileId, area)` devuelve SharedPreferences; los repositorios de dominio
  serán responsables de tipos, serialización y operaciones. No crea cuentas ni
  activa perfiles; no contiene credenciales remotas.
- Áreas `PROFILE`, `PREFERENCES`, `FAVORITES`, `PLAYLISTS`, `HISTORY`.
  Default reutiliza `aurelis_profile`, `aurelis_settings`, `aurelis_favorites`,
  `aurelis_playlists`, `aurelis_history`, respectivamente. Solo los dos primeros
  tenían un repositorio persistente observado. Los restantes son límites reservados,
  no una implementación completa de favoritos/listas/historial.
- Otros perfiles: `aurelis_profiles.<id>.<area>`; los puntos están prohibidos en
  IDs para que no se mezclen límites. Abrir un perfil nuevo no hereda preferencias
  del predeterminado ni copia datos sensibles. El acceso sigue siendo privado a la
  aplicación; esto no proporciona autenticación ni aislamiento frente a código de
  la propia app. No hay selección global de perfil activo en B02.
- La transición del esquema previo es en el mismo archivo: no hay renombrado,
  copia, borrado, conversión de claves ni marcador que pueda quedar a medio migrar.
  Las claves desconocidas y valores se conservan. Inicializar repetidamente es
  idempotente; renombrar el usuario no cambia ProfileId. No se requiere Room ni
  DataStore para este cambio. La biblioteca y sus IDs actuales no se migran.
- `AurelisApplication` inicializa ProfileRepository antes de mostrar Ajustes;
  ProfileRepository y SettingsRepository usan los ámbitos predeterminados. No se
  añade un motor ni se modifica el servicio Media3. Widget, metadatos de app,
  archivos musicales y cachés compartidas permanecen fuera de estos ámbitos.
- `data/LocalLibrarySource.kt`: `suspend songs(filterNonMusic): List<LocalSong>`
  propaga fallos/cancelación. MediaStoreLibrarySource delega al escáner actual con
  applicationContext. LibraryViewModel permite inyectar el contrato conservando
  su constructor Android de un argumento; permisos y agrupaciones siguen vigentes.
  CancellationException se propaga y permite reintento, sin mostrarla como error.
- Dependencias nuevas solo de pruebas: JUnit 4.13.2, Robolectric 4.14.1 y
  kotlinx-coroutines-test 1.9.0. Toolchain y dependencias de producción conservados.

### Aceptación B02

Probar datos heredados reales en SharedPreferences simuladas por Robolectric/API 33:
nombre, avatar, ajustes y claves desconocidas intactos tras dos inicializaciones;
escrituras siguen en archivos heredados; separación por perfil y área; validación de
IDs; inicialización desde Application. Probar el contrato de biblioteca, permiso
denegado, caché/reescaneo forzado, error recuperable, cancelación y constructor Android.
Ejecutar unitarias, assembleDebug y lintDebug. Las simulaciones no acreditan reinicio
real de proceso, MediaStore físico ni audio en dispositivo; esas pruebas siguen pendientes.

## B03 — Sistema visual

Autorizado tras B02. Se utiliza la misma rama y el PR existente comunicado por el
usuario, «Aurelis Music Beta UI and components». Se confirmó `refs/pull/1/head`
apuntando al HEAD publicado de la rama; la API sigue devolviendo Forbidden.

- Una paleta semántica clara/oscura en ColorSchemes, con superficies neutras de la
  adaptación existente, textos opacos y roles completos de Material 3. Se elimina
  la paleta privada inactiva de Theme para evitar dos fuentes divergentes.
- AurelisTheme aplica la preferencia persistida 0=Sistema, 1=Claro, 2=Oscuro.
  Valores desconocidos siguen el sistema; un override explícito sigue disponible.
  Se aplica el contraste de iconos del sistema correspondiente al tema raíz.
- Se activa la jerarquía tipográfica adaptada que ya existía, con interlineados
  explícitos y fuente sans del sistema. Sin archivos SF Pro ni descargas de fuentes.
  El alias previo SFProDisplay se conserva como compatibilidad deprecada, sin
  implicar que esa fuente se distribuya. AccentRed permanece para usos existentes.
- Formas compartidas de 5/11/14/20/28 dp. El tema de artwork conserva sus colores
  y su comportamiento oscuro; comparte tipografía y formas, sin sustituir el motor
  de extracción de carátulas ni el diseño del reproductor.
- SettingsGroup y SearchField consumen formas/colores del tema. SearchBar conserva
  su API y delega al mismo campo para evitar mantenimiento de dos implementaciones.
  Se amplían las acciones de búsqueda/borrado a 48 dp y se evita altura fija del campo.
- Las filas de ajustes permiten dos líneas en títulos; el control segmentado expone
  selección y grupos accesibles. Cada ajuste binario tiene una sola acción semántica
  de switch; filas deshabilitadas comunican su estado.
- El tema distribuye reducción de movimiento a barras de reproducción y esqueletos
  compartidos. No se promete aún cobertura de todas las animaciones (B13).
- Solo nuevas dependencias de prueba Compose: ui-test-junit4 y ui-test-manifest
  (este último en debug), versiones del BOM existente; recursos Android habilitados
  para pruebas JVM. Sin cambios de dependencias release, SDK, Kotlin o firmas.

Aceptación: contraste de texto >=4.5:1 en roles comprobados, modos de tema y cambios
de preferencias observados en composición, acciones únicas de switch, selección y
áreas táctiles de búsqueda. Capturas de componentes reales mediante Robolectric
con gráficos nativos en claro/oscuro y escala 1/1.5; no equivalen a validación física
del dispositivo ni a una comparación visual exacta con BitChord. Sin nuevos botones
de producto, rutas ni contenido simulado de streaming.

## B04 — Navegación local

Autorizado tras B03; misma rama y PR #1, sin migrar Home ni reorganizar Ajustes por
categorías todavía. Se integra AppNavigator existente, conservando sus operaciones
de pila/detalles para usos posteriores. El NavigationController legado no se conecta
al grafo ni se crea una segunda instancia de navegación de producción.

- Cuatro pestañas tipadas: HOME, EXPLORE, LIBRARY, SEARCH. La barra usa recursos de
  texto existentes y semántica de selección accesible; no acumula pestañas en la pila.
- Una sola pila sustituye booleanos independientes de pantallas. Biblioteca abre
  LocalMusic, Replay y Settings; Settings abre Equalizer. Son las pantallas existentes;
  Replay/Equalizer siguen siendo estructuras incompletas, con mensajes honestos sobre
  estadísticas/controles no disponibles. No se implementa historial ni DSP nuevo en B04.
- NowPlaying y Queue son destinos de esa pila: abrir cola conserva el reproductor
  debajo; Atrás/descartar vuelve a él y después a la pestaña original. El servicio y
  los modelos de reproducción no se modifican. Los overlays existentes permanecen
  compuestos para sus animaciones, con visibilidad derivada de la ruta actual.
- Un único acceso al reproductor en GlassNavBar, sin el segundo MiniPlayer simultáneo.
  El componente MiniPlayer no se borra; su adaptación visual continúa en B07.
- AppNavigationHost muestra solo la página activa, sin controles de páginas ocultas
  accesibles detrás. La barra inferior solo aparece en la raíz de una pestaña.
  Los diálogos locales conservan prioridad; la búsqueda interna de música local
  consume Atrás antes de abandonar su página y el aviso de actualización lo consume
  antes de cerrar una ruta. No se modifica la lógica del actualizador.
- Atrás: primero página/detalle, después Inicio desde otra pestaña raíz. En Inicio
  raíz no se intercepta: el cierre/retorno corresponde a Android, sin cambiar audio.
- rememberSaveable + Saver versionado conservan pestaña y rutas implementadas por
  claves estables, nunca canciones, carátulas, callbacks o listas de biblioteca.
  Estado incompatible/desconocido se descarta completo y se inicia en Inicio.
  Las rutas diferidas con LocalShelf/detalles externos no se serializan en B04;
  requerirán IDs pequeños y una política de carga al integrarse en otro bloque.
- SaveableStateHolder guarda estado por pestaña/página finita (consulta, scroll y
  selección local). Restaurar vuelve a la ruta superior y mantiene sus padres.
  Sin canción disponible al restaurar el reproductor se muestra un estado vacío con
  regreso, en lugar de dejar pantalla vacía o descartar prematuramente la ruta mientras
  el servicio conecta. La disponibilidad de audio no se serializa como navegación.
- Se separa AurelisNavigationContent de los efectos de arranque en AurelisApp para
  probar el mismo grafo sin conectar Media3, solicitar permisos ni consultar redes.
  Arranque, permisos y conexión reales se mantienen en MainActivity.
  Al entrar en LocalMusic, el grafo solicita refresh al ViewModel incluso si el
  permiso falta: el ViewModel existente debe exponer su estado Error, sin dejar
  Loading indefinido ni saltarse sus comprobaciones. El test del grafo deniega
  explícitamente audio y verifica el mensaje antes de probar búsqueda/restauración.

Aceptación: pruebas de orden de pila y compatibilidad de detalles, ausencia de rutas
duplicadas consecutivas, retorno cola→reproductor→origen, raíz Inicio no interceptada,
validación/restauración del Saver y pruebas Compose del grafo real con enlaces de
Biblioteca/Ajustes, Atrás Android, estado de búsqueda, scroll de Ajustes y recreación
de estado guardado. Ver BUILD_STATUS para resultados y límites de la simulación.

## B05 — Home de música local

Autorizado junto a B06, ejecutado primero como bloque separado. Se reutilizan Hero,
carruseles, temas, formas y controles existentes; sin nuevo contenido remoto ni fuentes.
Home recibe biblioteca, estado de reproducción y datos privados reales, no listas
ficticias: continuar (reanuda sin cambiar posición/cola), recientes, más escuchadas,
álbumes, artistas, favoritas, playlists, añadidas y recomendaciones locales. Apartados
sin datos no inventan resultados; carga, permiso, error y biblioteca vacía tienen
estados propios y acciones conectadas. Álbum/artista abren sus canciones actuales.

LocalHomeData resuelve únicamente IDs presentes en el catálogo. AutoMix de Home
es una **selección de cola local** con QueueBuilder existente: semilla actual/reciente/
favorita, prioridad de favoritas y diversidad/deduplicación ya implementada. No es
el análisis ni las transiciones avanzadas de B11 y no se presenta como tal.

Prerrequisito compartido de B05/B06: LocalUserLibrary ofrece colecciones privadas
por ProfileId sobre los ámbitos B02. Favoritos usan `song_ids`; playlists usan JSON
`playlists_v1` (version=1, items con id/nombre/IDs ordenados y sin duplicados); escucha
usa `listening_v1` (version=1, tracks con ms/plays/last). No se copian canciones ni
se borran archivos. IDs que faltan se conservan para almacenamiento desmontable y
se excluyen de reproducción/presentación mientras no se resuelven en MediaStore.
Versiones desconocidas/JSON corrupto se preservan y bloquean escrituras, con aviso UI.
No se cambian claves de perfil, preferencias, firmas ni applicationId.

La UI básica permite crear/renombrar listas, añadir/quitar temas y eliminar una lista
con confirmación. Eliminar una lista no elimina favoritos ni archivos musicales.
Las rutas de colecciones serializan tipo/clave codificada, nunca modelos o bibliotecas.
La clave derivada de álbum incluye longitud para evitar colisiones de separadores.

ListeningStats sustituye su stub utilizando el hook de reproducción ya existente.
Solo suma deltas positivos de 1–5000 ms (no saltos de seek); cuenta una reproducción
al escuchar 30s o la mitad de una canción más corta, una vez por sesión. La transición
de Media3 inicia sesión incluso en repetición del mismo tema. Se publica/guarda al
primer registro, al calificar, cada 30s y al pausar/cambiar/limpiar mediante los hooks
existentes. SharedPreferences.apply conserva el patrón asíncrono de ajustes; forzar
el cierre puede perder el último intervalo. El registro depende del polling del
ViewModel actual: la integración íntegra en servicio y gestión avanzada de historial/
cola siguen en B08, junto con revisión de almacenamiento para catálogos muy grandes.

No se considera B08 terminado por estos prerrequisitos. Su integración avanzada y
la pantalla Replay siguen pendientes. B06 mejorará el catálogo y su actualización;
B07 mantendrá la validación de reproducción/controles. No se afirman pruebas físicas
ni todas las sesiones en segundo plano a partir de las pruebas de Home.

## B06 — Biblioteca MediaStore reactiva

Se mantiene un único catálogo MediaStore y el motor Media3 existente. La lectura
usa Dispatchers.IO, cierra cursores, propaga cancelación y distingue fallos de
lectura, permisos y catálogo vacío. Filtra entradas pendientes, papelera y duración
menor de 500 ms; el filtro de contenido no musical sigue siendo configurable.
Metadatos incompletos usan nombre de archivo/etiquetas locales, sin consultas remotas.
TRACK empaquetado se separa en disco y pista. Álbumes y artistas se agrupan por
ID MediaStore cuando existe, incluyendo recopilaciones; las rutas previas siguen
resolviéndose. Se conservan IDs numéricos y URIs de canciones para no migrar favoritos.

Actualización: observador registrado antes de la primera consulta, avisos conflados
y debounce de 350 ms. Si llega un aviso durante la lectura queda una actualización
pendiente. Al detener la actividad se liberan observador y trabajos; al volver se
consulta de nuevo, incluyendo cambios de permisos. Las respuestas obsoletas no se
publican. Se consulta una instantánea completa de metadatos y se reconcilia por ID,
reutilizando objetos/grupos sin cambios: no se afirma lectura SQL incremental por
generación ni rendimiento medido en catálogos masivos.

Búsqueda común sin distinción de acentos/mayúsculas, ocho ordenamientos existentes
y navegación real de álbum/artista. Se conservan referencias privadas de archivos
ausentes para soportar medios desmontables; esos temas no se reproducen. Mover un
archivo manteniendo su ID conserva su vínculo. Si MediaStore recrea IDs, no se
promete recuperación por nombre ni entre dispositivos. No se recorren carpetas
fuera de MediaStore. La carátula utiliza la URI local del álbum; invalidación de
caché cuando solo cambia su imagen y pruebas de proveedores OEM quedan pendientes.

Permisos: sin acceso general a archivos; se verifica antes/después de leer y al
regresar a primer plano. Denegación permanente ofrece Ajustes del sistema. El
chequeo remoto de actualizaciones ya no retrasa el arranque de biblioteca sin red.
El paso LocalSong ↔ MediaItem conserva duración y metadatos locales; no se añade
un reproductor ni se declara B07 realizado. B08 mantiene historial/cola avanzados.

## B07 — Reproductor y sesión local

Autorización expresa tras B06; base `15582e8139173802bc01ba7046a38d461e7ae03c`.
Se adapta la pantalla activa NowPlayingScreen y el MiniPlayer integrado en
GlassNavBar, reutilizando temas, mesh, carátulas y controles presentes en Aurelis.
No se instala otro host ni motor. PlayerHost/FullScreenPlayerHost de referencia
no sustituyen a la pantalla activa y no se declaran completados sus stubs.

- Mini reproductor único con carátula local, título truncado, progreso, transporte
  y apertura accesible; el texto ocupa solo el espacio disponible y no oculta abrir.
- Reproductor completo con carátula real/fallback, tiempos, seek confirmado,
  repetición off→all→one, aleatorio, controles habilitados según estado y cola.
  Layout desplazable en pantallas pequeñas; reducción de movimiento respetada.
- No se deduce «Lossless» del ajuste global: se identifica archivo local sin
  afirmar un formato no medido. No se reduce arbitrariamente la carátula al 50%.
- Conexión idempotente; el primer toque espera al controlador y gana la última
  selección pendiente. Cancelación y desconexión liberan el controlador.
  Eventos de Media3 actualizan modos, posición, errores y cambios externos.
- La pausa sigue disponible durante buffering. Fin de cola permite reiniciar;
  seek se acota y no reinicia la cola; errores muestran mensaje y reintento.
- Cola: saltar y quitar canciones no actuales mediante comandos existentes.
  Se elimina el indicador de arrastre inoperante; reordenación avanzada sigue B08.
- Se retira el conversor privado duplicado de PlayerConnection y se utiliza
  el puente B06 para conservar IDs, duración, disco y demás metadatos locales.

AurelisPlaybackService sigue siendo el propietario único de ExoPlayer, MediaSession,
DSP, foco de audio y manejo de auriculares desconectados. MediaSessionService
conserva notificación y controles del sistema; la actividad de sesión abre Aurelis.
Cerrar la UI libera su MediaController y no el reproductor de la sesión. No se
cambian permisos de servicio, firmas, workflows, DSP ni dependencias de producción.

Validación: pruebas de comandos/eventos sobre SimpleBasePlayer real con estado
de audio controlado, puente real MediaSession↔MediaController y grafo Compose.
Las pruebas no decodifican audio ni validan hardware, notificación visible o
pantalla de bloqueo física. Esas comprobaciones deben hacerse en dispositivo
antes de afirmar aceptación física completa; B08 no queda autorizado por B07.

## B08 — Colecciones, historial y cola

Base `697b2b8be6aa04ccc9cefe958a23b838bc606cca`. Se amplía LocalUserLibrary
y no se instala la persistencia legacy OfflinePlaylists/ListeningRecorder.
Favoritos y pertenencia a listas se editan desde opciones de canción del reproductor
y pulsación larga de Home/Búsqueda. Se conserva crear/renombrar/eliminar con
confirmación; listas permiten subir/bajar sin eliminar referencias a medios ausentes.
Replay usa estadísticas reales por canción, último acceso, tiempo y frecuencia,
con borrado confirmado que conserva listas/favoritos; no representa un diario
de cada sesión individual ni un resumen anual inventado.

ServicePlaybackRecorder, propiedad exclusiva de AurelisPlaybackService, sustituye
el registro de escucha del ViewModel. Solo cuenta tiempo monotónico mientras
Media3 reproduce; seeks no suman distancia de audio, buffering/pausa no acumulan,
y cambios/repeticiones reinician sesión. Guarda al pausar, cambiar y cerrar; el
proceso forzado puede perder hasta el último intervalo de guardado. El ViewModel
conserva polling visual, sin segundo contador. Ajustar almacenamiento para catálogos
masivos queda como trabajo de rendimiento; no se afirma medición en hardware.

Cola: añadir sin reiniciar, saltar, quitar no actual y subir/bajar conservando el
tema actual por Media3. LocalQueueStore usa ámbito privado QUEUE de ProfileStorage,
JSON versionado con IDs ordenados (permite repeticiones), índice, posición, repeat y
shuffle. No almacena intención de autoplay ni rutas. Versiones desconocidas se
conservan sin escribir. Al crear el servicio se resuelve contra MediaStore, filtra
ausentes y restaura idle/pausado; una selección nueva impide sobrescritura tardía.
Si no hay permiso/catálogo se conserva la copia. Checkpoint cada 15s y en eventos.
No se inicia otro reproductor. Perfiles múltiples se integrarán en B12.

## B09 — Letras locales, caché y sincronización

Se integra LocalLyricsRepository con el reproductor existente: LRC importado
mediante Storage Access Framework → etiquetas incrustadas compatibles → caché
privada → LRCLIB únicamente al solicitar «Buscar en LRCLIB». Respeta la selección
de LRCLIB en ajustes. Reproducción y carga inicial de letras no esperan red.
Otras clases de proveedores existentes se conservan; no se habilitan nuevas
integraciones sin revisar sus condiciones. La traducción existente no se amplía
en B09: B10 conserva su alcance y sigue pendiente de autorización.

LRC admite marcas repetidas, orden, fracciones, offset global (positivo adelanta),
BOM UTF-8/UTF-16 y huecos temporales. Texto no sincronizado conserva tiempos cero
y nunca se resalta ni permite seek. No se inventan tiempos ni contenido. Lectura
incrustada acotada a 2 MiB de metadatos, ID3v2.3/2.4 USLT y comentarios FLAC
LYRICS/UNSYNCEDLYRICS; SYLT, ID3 comprimido/unsync/extended headers y otros formatos
no se declaran soportados. No se añaden dependencias ni acceso general a archivos.

Por scoped storage, un LRC externo requiere selección explícita; se guarda una
copia privada validada de hasta 1 MiB. Una importación fallida no sustituye la
anterior. Nombre interno SHA-256 de URI/tamaño/duración/título/artista; no depende
del path del archivo y evita reutilizar automáticamente otra grabación tras
cambiar esos metadatos. No detecta reemplazos idénticos en todos esos campos.
Importaciones en filesDir; caché descargada en cacheDir, versionada y acotada a
100 entradas/32 MiB. Caché/importaciones de letras pertenecen al catálogo compartido,
no a las playlists/historial privados de perfil. No se incluyen en Git.

Las escrituras son atómicas y serializadas. Jobs/generaciones evitan resultados
atrasados al cambiar de canción o petición; selección de archivo captura ID para
no importar en otra canción tras cambiar mientras el selector estaba abierto.
Caché no disponible no invalida un resultado remoto válido. Cancelación propagada.

UI: fuente y caché visibles, estados vacío/error/carga, importar y buscar, ajuste
de sincronización, resaltado y desplazamiento. Arrastrar o pausar seguimiento
permite lectura manual; «Seguir letras» lo reanuda. Seek resta el offset de UI;
muestreo visual Media3 cada 250 ms, separado del contador de escucha del servicio.
El panel sin sincronización permite lectura, sin atribuir tiempos falsos.
LRCLIB codifica parámetros y filtra coincidencias de título/artista/duración
(tolerancia 5 s); sin coincidencia se informa ausencia/fallo, no se toma otro tema.
Los permisos de contenido siguen siendo responsabilidad del proveedor/usuario;
la licencia de servidor no concede derechos sobre cualquier letra.

## B10 — Traducción y vista bilingüe

Base `3eb1867a812c6b9ba2b2ebcf42b80fbc4fe3dde2`. El selector anterior solo
contenía Close; ahora ofrece idioma original/auto, destino y servidor HTTPS
compatible con LibreTranslate sin clave. No se inventa un servidor público ni
se presume permiso para endpoints no documentados. **Requiere configurar un
servidor autorizado y disponible**; no se distribuye ni despliega uno en este bloque.
Solo confirmar «Traducir» permite enviar texto. Cancelar/cerrar no solicita nada.
Se informa del envío de letras; se rechazan HTTP, credenciales en URL, query y
fragmentos, y el cliente de producción no sigue redirecciones. No se añaden claves.

Se reutiliza LyricsTranslation: slots, lotes, reconstrucción y límites de caché.
Ruta activa usa POST /translate con q array/source/target/format=text según la API
pública oficial LibreTranslate. Máximo dos lotes concurrentes; respuestas vacías,
parciales o inválidas no se muestran como traducciones. Original, tiempos y huecos
se mantienen; traducción aparece debajo de cada línea. No se afirma sincronización
por palabra de la traducción. Estados carga/error/mismo idioma/caché y ocultar.
Las respuestas de otra petición/canción/versión de letra se descartan; cancelación
cancela la llamada OkHttp. No se habilitan romanización ni endpoints legacy.

Caché v4 privada configurada en Application, separada de v3: clave SHA-256 de
servidor, idiomas, ID y contenido original; memoria LRU 12, disco gzip 2 MiB
acotado, lectura limitada y escritura atómica. Permite traducido guardado sin red
con el mismo servidor/idiomas/texto configurados. Si la caché falta y el servidor
no responde, se conserva el original y se informa el error, sin resultados simulados.
Límites de entrada: 500 slots/30 000 caracteres, 3500 por slot; respuestas hasta
512 KiB por lote. Cinco idiomas del catálogo existente; modelos reales dependen
del servidor. Condiciones, privacidad y cuotas de cada servidor deben ser respetadas.

## B11 — AutoMix local por metadatos

Base: commit B10 `248461abf63ef4ad83c15640dcf7c54f97718a5f`. Antes de implementar
se volvieron a leer en BitChord SHA fijo QueueBuilder (jvmSharedMain),
TransitionPolicy y AutomixAnalysisSource (commonMain). La primera limita artistas/
grabaciones; la política de transición tiene procedencia Orchard AGPL y contratos
distintos; la fuente de análisis exige la versión canónica YouTube/Opus. No se
copia esa resolución remota ni nuevo DSP/algoritmo de transición AGPL.

LocalAutoMix es selección propia sobre el catálogo real: URI local, ID válido,
duración >=500 ms y sin IDs/grabaciones duplicadas. Semilla actual disponible,
último tema disponible del historial, favoritas o catálogo. Usa género MediaStore
cuando existe, artistas, favoritos, frecuencia, historial, duración y año.
No inventa géneros/tempo/energía ni usa el fallback ficticio de 120 BPM del helper
legacy de análisis. Sin metadatos musicales, recurre al resto de datos reales.

Los cinco temas más recientes dentro de dos horas quedan detrás de alternativas
no recientes. Si el catálogo es pequeño, pueden entrar después; no se inventan
resultados para completar tamaño. Artista semilla máximo 4, demás 2, incluyendo
colaboraciones; evita artistas de los dos últimos temas cuando hay alternativas.
Artistas desconocidos sin ID no bloquean toda la mezcla. Límite 20 por defecto,
50 máximo. Regenerar varía desempates; mismas entradas/variación son deterministas.
No es radio infinita ni crossfade/beatmatching. El final sigue los controles de
repetición elegidos por el usuario; puede solicitar otra mezcla explícitamente.

La lectura de género usa la columna de MediaStore de API33+ y se conserva en
MediaItem. El botón Home genera en Dispatchers.Default con estado/errores reales.
Una selección o append nuevo cancela la petición previa; cambios de pista durante
la generación no son sobrescritos. Si la semilla sigue activa, modifica solo
antes/después de ella sin reemplazarla ni reiniciar posición. Desactiva shuffle
al aplicar el orden recomendado; conserva repeat. Se usa el mismo controlador,
servicio y persistencia de cola B08, sin reproductor duplicado ni tráfico de red.

Se conserva LocalHomeData para sus secciones y recomendaciones básicas existentes;
su snapshot mix no es la acción AutoMix activa. QueueBuilder cuenta ahora artistas
ya presentes y colaboraciones, manteniendo su API. No se activan AutoMixPanel ni
helpers legacy con controles/valores que no estén integrados.

Coste: normalización de metadatos una vez y selección acotada, sin decodificar ni
analizar archivos y sin modelos. No se afirma medición en bibliotecas masivas,
consumo físico ni transiciones DSP probadas. Un trabajo posterior de audio debe
revisar licencias/avisos y probar en hardware antes de activar esas transiciones.
