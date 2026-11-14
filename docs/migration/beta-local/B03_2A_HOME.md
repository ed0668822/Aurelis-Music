<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B03.2-a — fidelidad incremental del Home local

## Autorización y manifiesto previo

Autorización «Continúa con el siguiente bloque». Base
43cdc0c0b86e16dca6905041e44857a56f875bb5, rama
feature/bitchord-local-migration, PR1 y título existentes. B03.1-a tiene
run120/43537277492 SUCCESS sobre b1142727; la base añade solo su cierre documental.
Máximo vigente 30 archivos distintos acumulados; dificultad media.

Manifiesto previsto (siete archivos, incluidas correcciones de esta entrega):
- app/src/main/java/com/music/aurelis/ui/screens/HomeScreen.kt
- app/src/main/java/com/music/aurelis/ui/components/home/HomeCarousels.kt
- app/src/test/java/com/music/aurelis/ui/HomeShelfLayoutTest.kt
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/B03_2A_HOME.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

## Referencia local y diferencias verificadas

BitChord /workspace/BitChord, HEAD separado y limpio en
b690dc146009d4815825fa4a20815ef2e4ee8898, solo lectura.
sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/HomeScreen.kt:
SHA256 d6f6a11a322eb968823fd6abd861f3e8ebff381e71e629a2a77a55d38d66e017.
Se consultan HomeTitle, SectionHeader, Shelf, ShelfCard, HeroShelf y CompactTrackRow.
SongText.kt, mismo directorio ui/components, SHA256
2579d2a9eb577c04de023e7dc1ce9ae00e9ec22477fc7706e135ff1e8e4ac79c:
thumbnailBorder contrastado con Aurelis: su helper global es un no-op. Se adapta
un borde privado del Home; no se modifica el helper global ni otros componentes.

Aurelis tenía gutters 20dp frente a 16dp; espaciado de tarjetas 12dp frente a
14dp; cabeceras titleLarge/bold frente a titleMedium; captions bodyMedium/small
frente a titleMedium/bodyMedium. Faltaban los 26dp entre varios estantes y los
bordes de carátulas presentes en la referencia. El icono de Home era fijo de 30dp en vez de
escalar con displayLarge; se adapta la proporción 0,9 manteniendo icono/texto Aurelis.
No se copia la marca BitChord ni se importa su feed remoto.

## Implementación y límites

Gutter compartido 16dp; separación 14dp entre tarjetas normales y 12dp entre
columnas de recientes (como la referencia). Separación inferior de estantes 26dp;
cabeceras titleMedium con hasta dos líneas; tarjetas cuadradas de 150dp con
caption titleMedium/bodyMedium, 10dp entre portada y título. Bordes de 1dp al
15% del color onSurface del tema activo Aurelis, para respetar claro/oscuro
manuales. El indicador de reproducción existente se conecta a tarjetas normales
solo para la canción actual en reproducción; respeta LocalPlayingBarsMotion.
El icono de Home escala con displayLarge manteniendo el texto/icono de Aurelis.
Se elimina el Spacer final de 100dp duplicado: se conserva el contentPadding
inferior del host (180dp en la entrada activa), además del margen del estante.

Se conservan las funciones y contratos de LocalHomeData, PlaybackViewModel y
navegación, incluidos los callbacks y listas/índices de cola, favoritos, playlists,
hero local y AutoMix. No se modifica el servicio, archivos ni almacenamiento.
Excepciones visuales explícitas: icono/texto propios frente al wordmark BitChord;
hero de continuación y acciones locales conservados; artistas circulares de
100dp; sin feed remoto ni carátulas animadas. No se declara paridad completa.
B03.3, B05, CI12.3 y las demás entregas siguen pendientes según el plan.

## Verificación y dos revisiones

Dos pruebas nuevas para GitHub Actions: alineación/gutter/ancho/separación de
canciones y álbumes con selección independiente; nombres locales largos y
acciones de álbum/artista con texto 1,5x en ambos temas. Se conservan las suites
HomeLocalTest y HomeShelvesIntegrationTest para hero, favoritos, playlists,
recientes/índice global, mantener, permisos y recuperación. No se ejecutan
Gradle, Android Lint ni pruebas Android localmente.

Revisión 1 del mismo agente: comparación con las funciones upstream, modelos y
consumidores; límites del tema/borde/animación y colas existentes. Revisión 2 del
mismo agente: diff completo, tipos/imports de las pruebas, atribuciones y siete
rutas dentro de 30. git diff --check local. La ausencia de hallazgos estáticos no
certifica ejecución Android ni fidelidad visual; B01–B06 del SHA publicado deben
comprobarse antes de cerrar el bloque. Pruebas físicas y capturas comparativas
con BitChord ejecutable siguen pendientes.


## Corrección posterior de B03 — run121

Run121/43549705914, SHA da2163196f2fa76f1673c528ae0f90e7dd38843a,
FAILURE según la API: B01/B02/B05 success, B03/B06 failure y B04 skipped.
Las puertas F5 y ambas suites fallaron. Las anotaciones solo aportan códigos
de salida, sin causa detallada. Los informes y logs redirigen a
productionresultssa31.blob.core.windows.net y el proxy devuelve Forbidden;
no se afirma haber leído XML/resultados que no se han podido descargar.

La inspección estática confirmó un defecto de compilación en la prueba nueva:
import com.music.aurelis.RobolectricActivityRule no existe. La declaración real
está en app/src/test/java/com/music/aurelis/ui/RobolectricActivityRule.kt,
paquete com.music.aurelis.ui, el mismo que HomeShelfLayoutTest. Se elimina solo
el import erróneo; la clase se resuelve en su paquete sin cambiar las pruebas.
No se presenta este defecto comprobado como explicación exclusiva de todos
los resultados sin disponer de logs. Hace falta comprobar el nuevo CI.

Dos revisiones del mismo agente: resolución de la clase y tipos/imports de la
prueba; después diff completo, aserciones conservadas y presupuesto acumulado.
git diff --check local; sin Gradle/Android locales. La corrección toca tres
rutas ya incluidas: se mantienen siete archivos distintos en B03.2-a de máximo 30.


## Resultado posterior — run122

[Run122/43552336276](https://github.com/ed0668822/Aurelis-Music/actions/runs/43552336276),
SHA fcc10d128df854ef819d786cb14d5641e9724460, terminó SUCCESS según la API.
B01, B02, B03, B04, B05 y B06 terminaron success. En B03 también terminaron
success Generate Kover Debug and Release XML coverage, Assess changed-line
Kover coverage, Enforce F5 changed-line coverage threshold y Enforce both test
suites. Esto confirma el CI de la corrección sin rebajar puertas ni retirar tests.

Artefactos publicados por Actions: Aurelis-Informes-B01-B06-122 (14097236880),
Aurelis-Release-ARM64-122 (14097067805) y Aurelis-Pruebas-ARM64-122 (14097351303).
No se creó una Release oficial. La descarga del informe volvió a fallar por el
proxy; no se afirma haber leído sus XML/results.json ni se inventan números de
pruebas o porcentajes de cobertura. El verde corresponde al SHA fcc10d12, no al
commit documental posterior. El checkout de entrega permanece limpio tras commit.

Se cierra la implementación de esta subentrega y su fallo de CI. Quedan pendientes
las comprobaciones visuales/físicas y las diferencias explícitas del apartado
Implementación y límites. No se cierra B03 completo ni se inicia B03.3 u otro bloque.
Cierre limitado a este informe y PROGRESS_TRACKER.md; siguen siendo siete rutas
acumuladas de máximo 30. Dos revisiones del mismo agente: cotejo API/SHA/jobs y
limitaciones de acceso; después diff/documentación/presupuesto y git diff --check.
Sin Gradle/Android locales. Esperar autorización para el siguiente bloque.
