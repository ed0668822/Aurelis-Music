# B04 — Explore, Search y Library

Base de código: 7db72d9. Rama existente feature/bitchord-local-migration.
PR: https://github.com/ed0668822/Aurelis-Music/pull/1 (título intacto).
Autorización vigente: «Continúa con B04 - B05». B05 queda autorizado, pero espera
el resultado de B04 comunicado por el usuario, conforme a su puerta de CI.

## Implementación y dependencia

Depende de B03 y conserva GLASS01/QA01. Contrato de 16 campos heredado de
IMPLEMENTATION_BLOCKS, B04. No se cambia ese plan ni el orden de sus bloques.

- B04.1: Explore sustituye marcador por tarjetas de los géneros reales del
  catálogo; grupo Sin clasificar para metadatos ausentes, sin inferir estilos.
  Claves distintas para ausencia de etiqueta y una etiqueta literalmente llamada
  Sin clasificar. Reutiliza diseño GenreSleeve/espaciados existentes. Estados de
  carga, error con reintento, permiso y biblioteca vacía; sin shimmer infinito.
- B04.2: Search conserva canciones y ordenación existente; añade destinos de
  álbumes, artistas, playlists y géneros, normalizando acentos y mayúsculas.
  Filtros explícitos Todo/Canciones/Álbumes/Artistas/Playlists/Géneros. Historial privado, ordenado y limitado a20 entradas/200 caracteres, se guarda al
  enviar consulta, reproducir o abrir colección. Repetidos normalizados se mueven
  al principio. Consulta anterior seleccionable, borrado individual y completo.
  JSON inválido no se sobrescribe. No se añade ninguna fuente remota.
- B04.3: Library añade álbumes/artistas/carpetas/historial como rutas restaurables.
  Directorios en lista o cuadrícula; carpetas y colecciones resuelven el catálogo
  vigente. Canciones conservan pantalla existente. Dentro de colecciones se
  seleccionan varias canciones para reproducir, añadir a favoritos sin alternar
  los ya favoritos, o añadir a una playlist existente. Orden Z–A reversible sin
  sobrescribir orden de playlists ni historial; botón general reproduce orden visible.
  Gestión previa de playlists, favoritos y menús conservada.

## Archivos / correspondencia

Referencia fija BitChord b690dc146009d4815825fa4a20815ef2e4ee8898, consultada
exclusivamente desde el archivo local de lectura. Se consultaron las secciones de
Explore/Search/Library en sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/:
GenreSleeve/métricas, filtros/historial de búsqueda y LibraryLinkList.
Se reutilizan las pantallas de Aurelis existentes; no se trasplantan dependencias
multiplataforma ni se importan recursos/licencias nuevos.

Archivos modificados: MainActivity.kt (grafo activo), AppNavigator.kt (tipos y
restauración), ExploreScreen.kt, SearchScreen.kt, LocalCollectionScreen.kt.
Nuevos: data/LocalGenres.kt (nombre real del agrupador previsto en F05 como
LocalGenreRepository) y data/LocalSearchHistory.kt. Pruebas: LocalGenresTest,
LocalSearchHistoryTest, AppNavigatorTest y PlayerIntegrationTest.

No se tocan servicio/cola/motor de reproducción, Liquid Glass, NowPlayingScreen,
ABI/nativo, modelos, manifiesto, Gradle, workflows, firmas, LICENSE/NOTICE/LICENSES.
Perfil único, MediaStore y permisos existentes conservados. No nuevas dependencias.

## Primera revisión — arquitectura e integración estática

Casos nuevos de enum exhaustivos en resolución/títulos y permitidos al restaurar
rutas sin clave. Géneros con clave estable, datos reales y memoria por catálogo;
búsqueda/colecciones no almacenan objetos de medios en Bundle. Selecciones se
resuelven contra canciones actuales, evitando reproducir archivos ya ausentes.
Historial no usa preferencias globales ni modifica el historial musical, y respeta
perfil de solo lectura/datos corruptos. Callbacks reutilizan ViewModel/almacén.
Se corrigieron ámbito de Context y colisión de nombre de género durante revisión.

## Segunda revisión — regresiones y límites

Segunda pasada del mismo agente, no revisor externo: navegación/estado vacío,
claves Unicode, estado corrupto, escritura de favoritos/playlists y reproducción
ordenada. git diff --check correcto. Se compararon sin cambios configuración,
licencias y todo el pipeline Liquid Glass/QA01 contra base. Evidencia JSON adjunta.

Se prepararon seis casos nuevos (2 géneros,2 historial,2 UI) y se amplió la prueba
existente de restauración con cinco destinos. Todos PENDIENTES DE EJECUCIÓN EN
GITHUB: no se ejecuta Gradle ni se compila Android localmente. Las pruebas UI usan
Robolectric/RecordingPlayer, no certifican Android físico ni sonido real.
Workflow existente: lintDebug/lintRelease, testDebugUnitTest/testReleaseUnitTest,
assembleDebug/assembleRelease. No cambiar comprobaciones para ocultar errores.

Aceptación estática: rutas/datos conectados y cambios documentados. Cierre de
compilación/pruebas pendiente. Verificación visual/dispositivo pendiente; no se
certifica fidelidad total a BitChord. Las tarjetas del directorio local son
controles de texto funcionales, sin simular carátulas. La pantalla de canciones
original y sus filtros siguen disponibles; no se presenta selección de colecciones
como una reimplementación de toda su UI. La paridad visual completa requiere comprobación en dispositivo, sin declarar cierre total.

Parar ante fallos de CI, regresión de reproducción/persistencia o licencia dudosa.
Dispositivo ausente impide cerrar arranque/permisos/SAF reales. B05 permanece
pendiente de iniciar tras confirmar el usuario el resultado de GitHub de B04;
su autorización ya está concedida. No iniciar B06 ni QA02.
