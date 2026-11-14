<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B04-a — comprobación incremental de conexiones y selección

## Autorización y manifiesto previo

«Continúa» confirma el éxito de B03.3-a y autoriza el siguiente bloque sin
volver a descargar ZIP. Base 5fde06acfe4e3c2ff192a855eb4367f334a9b82e,
misma rama feature/bitchord-local-migration y PR1. No se atribuye al agente una
lectura de informes del bloque anterior. Máximo vigente 30 archivos distintos,
incluidas correcciones; esta subentrega acota la continuación B04 a selección.

Manifiesto previsto (seis archivos):
- app/src/main/java/com/music/aurelis/ui/screens/LocalCollectionScreen.kt
- app/src/test/java/com/music/aurelis/ui/CollectionSelectionTest.kt
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/B04_A_SELECTION.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

## Comprobación y defectos verificables

Revisados los consumidores activos de MainActivity: Explore resuelve géneros
por clave (incluido unknown/Sin clasificar); Search filtra/ordena datos locales
con LocalSearch y abre rutas por clave; Library enlaza canciones, álbumes,
artistas, carpetas, historial, favoritos y playlists. LocalFolderRepository
mantiene el catálogo MediaStore y las claves por volumen/ruta. Suites existentes
LibraryCatalogueIntegrationTest, LocalCollectionsIntegrationTest, LocalGenresTest,
LocalSearchHistoryTest y PlayerIntegrationTest ya cubren esos flujos. Esta revisión
es estática, no una nueva certificación física ni una repetición de B04 histórico.

Brechas confirmadas en LocalCollectionScreen:
1. selectedSongs se calculaba con songs antes de aplicar el orden Z–A. Ejemplo:
   catálogo Árbol/Cielo/Zorro, pantalla Zorro/Cielo/Árbol: seleccionar Árbol y Zorro
   enviaba Árbol/Zorro a reproducir/añadir a playlist, distinto del orden visible.
2. selectionMode y descending usaban rememberSaveable, pero selected solo
   remember. Recrear restauraba el modo y orden, perdiendo los IDs marcados.

Se usará la lista visible vigente para resolver selección y se guardarán solo
IDs (strings), nunca LocalSong ni callbacks, conservando los datos privados.

## Referencia y corrección

Referencia consultada, solo lectura: BitChord local fijo b690dc1,
app/src/main/java/com/music/bitchord/ui/screens/LocalFolder.kt, SHA256
177dd478a46ad1ec78a07a36e14d92955e412990a36001dea9eeae056b6db5db.
LocalFolderState mantiene IDs; LocalFolderSelectionBar resuelve chosen() contra
songs del consumidor. No se copia el archivo ni se importan funciones remotas.
La serialización/restauración añadida aquí es propia de Aurelis, no una función
atribuida al archivo origen; se usa el listSaver Compose ya disponible.

selectedSongs ahora deriva de displaySongs, conservando cada acción y el orden
original cuando Z–A no está activo. Añadir a playlist conserva las entradas
previas y anexa la selección en orden visible; no reordena la playlist guardada.
selected usa rememberSaveable con saver Set<String> ↔ List<String>, acotado por
la ruta como selectionMode/descending. No se guardan medios/callbacks en Bundle.
Cancelar selección sigue vaciando los IDs. Se conservan los checks de solo lectura,
la resolución contra canciones actuales y las referencias privadas a medios ausentes.

## Pruebas, revisiones y aceptación

Tres pruebas nuevas para Actions en CollectionSelectionTest:
- Orden Z–A de un subconjunto con acento, callback de reproducción/índice, añadido
  real a playlist persistente y retorno al orden original sin alterar lo guardado.
- Recreación con StateRestorationTester: IDs marcados y orden conservados; cancelar
  y reabrir selección deja todas las casillas desmarcadas.
- Catálogo actualizado con medios ausentes: restaurar/reproducir solo incluye
  canciones vigentes; al quedar vacío, acciones deshabilitadas y favoritos intactos.

La pantalla real y LocalUserLibrary se usan con fixtures locales en src/test.
El callback de reproducción se observa sin afirmar decodificación ni audio físico.
Se conservan las suites previas de géneros/Sin clasificar, búsquedas/acentos,
carpetas por volumen, rutas y controlador de reproducción. No se ejecutan Gradle,
Lint, APK ni pruebas Android localmente; las nuevas esperan Actions por SHA.

Revisión 1 del mismo agente: orden visible/original, selección parcial, store y
consumidores, IDs vigentes y reglas de solo lectura. Revisión 2 del mismo agente:
diff completo, tipos/imports y saver, restauración/cancelación, atribuciones y seis
rutas acumuladas de máximo 30. git diff --check local sin incidencias. No se
certifica paridad visual/física completa de Explore/Search/Library ni se rehace
el B04 histórico. No se inicia B05/CI12.3/B09 ni otro bloque.

Al enviar, avisar y esperar: «continúa» confirma éxito y autoriza el siguiente
bloque; si el usuario comunica un fallo, leer sus evidencias antes de corregir.
No descargar ZIP preventivamente ni crear Release oficial.
