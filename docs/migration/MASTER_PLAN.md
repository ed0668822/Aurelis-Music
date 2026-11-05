# Migración BitChord → Aurelis Music

## Alcance y referencias

Documento iniciado en B01 (auditoría y planificación). B02, B03 y B04 fueron autorizados
después e incorporan únicamente sus secciones específicas. B05 y siguientes no están autorizados.
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
