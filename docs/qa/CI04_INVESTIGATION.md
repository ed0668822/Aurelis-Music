# CI04 — causa identificada tras recibir el registro de pruebas

Base81e12a9, rama existente feature/bitchord-local-migration y PR1.
Run: https://github.com/ed0668822/Aurelis-Music/actions/runs/42234180764
Job127947410667, commit81e12a9afbf0a2a50abb6e1bb78085f665f3fcba.

## Evidencia y causa

La vista pública solo permitía ver tests=failure. El usuario aportó el registro
completo del paso11, que demuestra que se ejecutaron200 pruebas por variante,
con1 fallo en debug y el mismo en release:
PlayerIntegrationTest.localSearchOpensAnAlbumAndRecordsTheSubmittedQuery,
java.lang.AssertionError en PlayerIntegrationTest.kt:75.
No es compilación de Kotlin, error de recursos ni recomendación de actualizar
versiones: son aserciones fallidas de una prueba ejecutada. APK omitido correctamente.

La línea75 usaba onNodeWithText("Canción local 1").assertDoesNotExist() sobre toda
la pantalla. start() inicia la reproducción de esa canción y MiniPlayer sigue
mostrando su título independientemente del filtro. La aserción global confunde
el transporte musical persistente con la lista filtrada. La lógica de MainActivity
ya pasa una lista vacía de canciones al elegir ALBUMS, conservando destinos de álbum.
No se modifica ese filtro ni se elimina el MiniPlayer para satisfacer la prueba.

## Corrección acotada y controles positivos

SearchScreen: testTag estable local-search-results en la LazyColumn de resultados,
sin cambio de tamaño, colores, lógica ni interacción de producción.
PlayerIntegrationTest: matcher por texto Y ancestro de resultados; conserva
assertDoesNotExist tras filtrar y comprueba también:

- La canción sí existe en resultados antes del filtro.
- Al seleccionar Álbumes no existe dentro de resultados.
- El MiniPlayer conserva la canción y PlaybackViewModel conserva su ID.
- Al volver a Todo, reaparece en resultados.
- El álbum abre su colección y guarda el historial, como exigía la prueba original.

Así una lista siempre vacía o un filtro que no oculta canciones siguen fallando.
No se desactiva, ignora ni reduce la cobertura de la prueba.

## Mejor diagnóstico de CI

android_ci.py publica hasta10 fallos JUnit identificados por variante, clase,
método, tipo y mensaje en anotaciones nativas. Ya estaban en report.json/XML;
antes no se imprimían como anotaciones públicas. Mensajes redactados, acotados y
escapados para evitar filtración de secretos o inyección de comandos por saltos
de línea. No se deduce archivo/línea falsos ni se altera la puerta de aprobación.
También reconoce FAILURE: Build completed with N failures, que aparece en este
registro y no coincidía con el patrón anterior de Build failed.

## Doble revisión y validación

Primera pasada estática: correlación registro/línea, fixture que inicia canción1,
texto real del MiniPlayer y ámbito del filtro; corrección localizada y controles
positivos. Segunda pasada del mismo agente: etiqueta/ancestro, conservación de
reproducción/historial, separación JUnit/compilador, redacción/escape de anotaciones,
puertas de CI y alcance protegido. No revisor externo ni ejecución Android.

23 pruebas Python de utilidades correctas, incluidas2 nuevas regresiones y
la comprobación de anotaciones de ambos XML de variantes. git diff --check
correcto. No se ejecutaron Gradle, suite Android ni APK aquí. La prueba UI
corregida debe confirmarse en GitHub; no se afirma resultado verde nuevo.

Inicialmente se solicitó el registro porque logs públicos devolvían404; recibido
ahora, ya no falta ese dato. Se conserva su SHA y extracto mínimo en JSON, sin
copiar variables de entorno ni el registro completo al repositorio.

Plan, workflow YAML, firma, ABI, dependencias, servicio, Glass/QA01 y licencias
intactos. Publicar mismo PR y esperar resultado del usuario. B06 y QA02 detenidos.
