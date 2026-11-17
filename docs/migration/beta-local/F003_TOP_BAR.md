<!-- Aurelis © 2026. GPL-3.0-or-later -->
# F003 — Cabeceras, perfil y acceso a ajustes

## Alcance autorizado

El usuario pide continuar con el siguiente bloque del inventario, F003. Base
`9d203d13dc86bfddf90ac64dd39ee2e679ef1cdb`, rama única
`feature/bitchord-local-migration`, PR1 y título conservados. CI183 de la base
terminó SUCCESS en B01–B06; ese resultado no se atribuye a los cambios nuevos.

Clasificación media, máximo40 rutas distintas incluyendo reparaciones. Lista
cerrada antes de implementar:14 rutas, dos de ellas documentación de la nueva
organización40/10 preparada en el turno anterior. No se implementa F004 ni se
reabre F001/F002. Las funciones posteriores siguen pendientes de decisión.
Estas secciones describen la entrega inicial; el apartado CI185 registra la
mejora de pruebas autorizada después y el acumulado vigente19/40.

## Diferencias comprobadas

La cabecera activa usaba controles de48dp, foto de32dp sin el aro original,
acciones contiguas y campo Buscar con mínimo52dp dentro de una superficie48dp.
El parámetro de Haze no se consumía para elegir el material de los controles.

BitChord fijado usa barra52dp, separación de contenido12dp, controles44dp,
avatar28dp con aro1dp y glifo de respaldo18dp, gutter de página16dp, gap entre
acciones10dp, inset de extremos1,5dp cuando hay varias acciones y gap entre
campo y acciones8dp. Las dimensiones del inset se redondean por extremo en
píxeles: a mdpi dos controles miden102px, no101; no se cambia la fórmula fuente.

El título centrado pequeño está deshabilitado en el archivo original fijado,
también al desplazarse. Se conserva esa presentación: no se inventa una nueva
cabecera colapsable. Las páginas mantienen sus títulos grandes y sus estados
de scroll existentes. Se conservan los parámetros públicos por compatibilidad.

## Implementación

- FrostedTopBar adapta las funciones reales ArtworkPageBackButton,
  ArtworkPageActions, ActionsRow, AccessoryPill, artworkActionEdgePadding y
  artworkPageSurface. Se conserva colocación relativa para RTL y crecimiento
  del grupo de acciones hacia la izquierda manteniendo su borde derecho.
- Se incorpora el tipo Shape de SquirclePillShape desde el archivo original,
  cambiando paquete y añadiendo aviso de adaptación, sin alterar el algoritmo
  ni sus constantes. El archivo homónimo de Aurelis contenía un composable
  Surface, sin llamadas activas; se conserva ese contrato junto al tipo nuevo.
- LocalProfileButton consume el ProfileRepository existente. Conserva su
  callback y etiqueta accesible; muestra foto local o persona sobre círculo
  relleno. El aro usa el color de contenido del tema efectivo, para respetar
  la elección claro/oscuro de Aurelis en lugar de solo la del sistema.
- El campo de búsqueda solo cambia cuando está alojado sobre la superficie
  de la cabecera: toma su altura, padding8/12dp, magnifier32dp y borrar28dp.
  Mantiene búsqueda local, teclado, submit, foco tras borrar y etiquetas.
  Fuera de esa superficie se conserva su presentación previa.
- SettingsScreen y ReplayScreen usan la misma variante flotante. Replay
  recibe onProfile y MainActivity lo conecta a openSettings: se limpia foco,
  se oculta teclado y se añade Settings sobre Replay en la pila existente.
  Atrás retorna a Replay y después a la pestaña Biblioteca; no se añade otro
  acceso a ajustes en la lista principal de Biblioteca.

## Estabilidad y límites de adaptación

El selector del material conserva Liquid Glass de Aurelis y sus correcciones.
Sin hardware o con blur reducido usa relleno sólido. Sin Liquid Glass, si hay
hardware y una fuente Haze válida, adapta la elección de material del origen.
El HazeState por defecto proviene de LocalAppHazeState ya instalado en el host.
No se crea otro grabador ni se introduce hazeSource/layerBackdrop en la cabecera.

El cuerpo de Ajustes, Replay y Buscar sigue grabándose separado de su lector.
pageHandlesBackdrop y pageBackdropModifier conservan su lógica: no se vuelve
a grabar la página completa incluyendo una cabecera que lee esa misma fuente.
Las pruebas ChromeContractTest existentes de aislamiento permanecen intactas.

La animación de tamaño del grupo y el cambio de color del divisor respetan
LocalReduceMotion con snap. Se mantiene el chevrón existente de Aurelis:
no se importa SfIcons ni fuentes SF. Las funciones remotas de cuenta, cambio
entre perfiles, descargas remotas y salas no se trasladan.

No se porta RefreshPuck ni un nuevo gesto de recarga: los parámetros existentes
y el indicador previo se conservan; completar actualización corresponde a F012.
Tampoco se adapta el cuerpo de Inicio, búsqueda de datos, Replay o Ajustes como
parte de F003. No se modifica MediaStore, perfil persistido, modelos, servicio,
sesión, reproducción, Gradle, dependencias, SDK, ABI, firmas o workflows.

## Pruebas y revisión

Se añaden seis regresiones para GitHub Actions, sin omitir las anteriores:

1. Medidas y alineación de foto/acciones/Atrás en claro/oscuro, Glass/plain y
   blur reducido, sin título duplicado al variar el estado de desplazamiento.
2. Círculo con una acción y gaps/insets reales con dos y tres, manteniendo anclaje.
3. Campo en RTL y fuente1,5: altura44dp, gap8dp, submit, borrar/foco y perfil.
4. Dibujo software con foto local y respaldo, controles interactivos y perfil
   sin cambio de identidad; se comprueba que la captura no está vacía.
5. Crecimiento y retorno de acciones con movimiento habilitado.
6. Recorrido Biblioteca→Replay→foto→Ajustes→Atrás→Replay→Biblioteca en el grafo real.

Las nuevas capturas software declaran LocalGlassHardware=false, de acuerdo con
Canvas(Bitmap); no fingen una GPU ni certifican fidelidad de píxeles. Las pruebas
existentes de navegación, permisos, perfil, Photo Picker y aislamiento no se
eliminan. Compilación, Lint y pruebas Android exclusivamente en Actions.

Comprobaciones locales realizadas: git diff --check, ámbito14/40, reutilización
exacta de ActionsRow y algoritmo SquirclePillShape, y siete Kotlin sin errores
en Tree-sitter. El parser no sustituye Kotlin/Compose: SettingsScreen ya
produce un error de parser en la base publicada; aquí solo cambia el booleano
artworkPageChrome, por lo que no se presenta su sintaxis como validada por esa
herramienta. Los tipos y la compilación se verificarán en GitHub Actions.

Dos revisiones del mismo agente, no independientes: (1) APIs, procedencia,
geometría e integración del perfil; (2) captura aislada, estados/RTL/movimiento,
regresiones, límites y diff. Estado del nuevo CI: **PENDIENTE**. La fidelidad
visual completa y la prueba en Redmi Note14 Pro5G, Android16/HyperOS3, siguen
pendientes. Se avisará del envío y se esperará al usuario antes de F004.

## Reparación posterior — CI184

[CI184/run43997300231](https://github.com/ed0668822/Aurelis-Music/actions/runs/43997300231),
SHA `ca3316af5cbba112385371dd10aacb5956770cee`: B01/B02/B05 SUCCESS,
B03/B06 FAILURE y B04 SKIPPED. El APK Debug e instrumentación sí compilaron;
no se acredita un APK Release verificado porque B04 no se ejecutó.

Los logs originales pasan CRC, guardados fuera del checkout como
`run184-logs.zip`, SHA256
`3df9ca6b8b619493ad9d24a3a427bbeb2e51f7093ba8b16a1a6f71a02675ef28`.
Ambas variantes fallan en compileDebugUnitTestKotlin/compileReleaseUnitTestKotlin
por el mismo error, FrostedTopBarTest.kt:132:72. performTextInput() devuelve
Unit; encadenar performImeAction(), que requiere SemanticsNodeInteraction,
produce Unresolved reference por incompatibilidad del receptor.

No hay eventos AURELIS_TEST_END en ninguna variante: no se afirma que las seis
regresiones nuevas ni las pruebas anteriores aprobaran. Kover se omitió y F5
rechazó COVERAGE_XML_UNAVAILABLE; no es evidencia de un porcentaje bajo.

Corrección mínima: las acciones de escribir y confirmar IME se ejecutan en dos
sentencias sobre el mismo selector del campo. Se mantienen las seis pruebas,
su secuencia y todas sus aserciones. No cambia código de producción, parámetros
del pipeline, dependencias o umbrales. Tres rutas ya incluidas, acumulado14/40.
Dos revisiones del mismo agente: (1) causa y API/receptor, (2) continuidad de
acciones/aserciones, alcance y diff. Sintaxis/diff locales; compilación y pruebas
Android exclusivamente en Actions. Nuevo CI PENDIENTE; F004 sigue sin iniciar.

## CI185 — corrección del recorrido y mejora de pruebas

El usuario autoriza «Mejora eso y corrige el error» después de preguntar por las
pruebas. [CI185/run43999110484](https://github.com/ed0668822/Aurelis-Music/actions/runs/43999110484),
SHA `1ca0093a7fd98dd3c9afb3727a0599a57db6de61`, aprobó B01/B02/B05; B03/B06
FAILURE y B04 SKIPPED. En cada variante se ejecutaron457 casos:456 SUCCESS y
1 FAILURE. Las cinco pruebas de FrostedTopBarTest aprobaron en Debug y Release.
El único fallo fue la aserción final de
NavigationIntegrationTest.replayProfileReturnsThroughSettingsToTheSameLibraryTab,
línea107. Logs originales CRC válidos, `run185-logs.zip`, SHA256
`c4fedcd96732c4e1ea6f14fe73819ea82b79ec7216d4137192f0d167b67d140a`.

AppNavigator.canGoBack incluye la condición selectedTab!=HOME. Volver de Replay
a Biblioteca vacía la pila de páginas pero todavía permite Atrás→Inicio. La
prueba ahora verifica ambas cosas y el último paso hacia Inicio; no se debilita
el modelo para satisfacer una expectativa errónea. También restaura estado en
Ajustes y Replay antes de completar el recorrido. F5 sin XML fue consecuencia
de la prueba fallida, no evidencia de una cobertura porcentual baja.

### Mejora controlada (dificultad alta,10 archivos)

1. NavigationIntegrationTest: corregir y ampliar el recorrido; SDK33/35.
2. FrostedTopBarTest: ejecutar las cinco regresiones en SDK33/35.
3. HomeShelvesIntegrationTest: permisos, carga, vacío, error y recuperación en SDK33/35.
4. .github/workflows/b03-unit-tests.yml: compilación independiente Kotlin/Java de
   Debug y Release antes de ejecutar; ejecutar cada suite solo si compiló su
   variante. El fallo de una no omite la otra. Kover/F5 permanecen obligatorios.
5. .github/scripts/b03-phase.sh: conservar logs y estados de comando, timeout y
   escritura de logs; un error no se pierde al usar tee.
6. .github/scripts/b03_test_summary.py: extraer JUnit con causas, clase/caso y
   ubicación; distinguir fallo de dependencias/compilación, ejecución, timeout,
   proceso terminado y evidencia incompleta. Exit137 no prueba por sí mismo un
   timeout. Mantiene campos y rutas que B06 consume, y añade diagnóstico por fase.
7. .github/scripts/test_b03_test_summary.py: fixtures de informes y códigos de
   salida con comandos simulados, sin Gradle/Android local.
8. Este informe.
9. PROGRESS_TRACKER.md.
10. FUNCTION_BLOCKS_40_10.md, actualizado con el presupuesto acumulado.

Robolectric4.14.1 de la base no soporta API36: no se fuerza ni se actualiza una
dependencia para aparentar esa cobertura. B03 añade el emulador Android16
reutilizando ui_parity.py settings-navigation y las dos pruebas instrumentadas
existentes de recreación y continuidad de reproducción en ajustes. Descarga los
APK Debug/instrumentación de B01 y verifica SHA, run, intento y hashes mediante
ci14_handoff antes de ejecutarlos. No recompila APK ni modifica su firma/ABI.

El emulador valida API36, commit y ambos casos concretos. Evidencia ausente,
reporte inválido, caso omitido o comando fallido rechazan la puerta final aunque
los pasos de diagnóstico usen continue-on-error. Los artefactos mantienen nombres
y estructura; compile-*.log y android16/ quedan en el paquete B03. Las capturas
unitarias siguen declarando software; la evidencia del emulador se identifica
como tal y no acredita igualdad visual o validación física de HyperOS3.
La comprobación ampliada necesita el pipeline unificado con B01: un despacho
aislado de B03 sin esos APK no puede acreditar Android16 y falla por falta de
evidencia, igual que las otras fases que dependen del handoff de B01.

Esta comprobación adicional puede alargar B03 por el arranque del emulador.
Se conserva su límite90min y se acota el paso Android16 a25min; un fallo de
preparación queda diferenciado de una aserción de la app. B05 conserva sus pruebas
de audio Android15 y el DAG B01→B02/B03/B05→B04→B06 no se altera.

Controles nuevos: XML ausente/vacío/inválido, compilación fallida con XML antiguo,
skips, errores, un comando fallido pese a XML positivo, timeout con caso activo,
proceso terminado, API/commit/casos Android incorrectos y fallo de tee. Ninguna
prueba existente ni umbral se elimina. Las verificaciones Android nuevas quedan
PENDIENTES de Actions. Dos revisiones del mismo agente: causa/APIs y después
gates/artefactos/independencia/matriz/scope. Comprobaciones locales permitidas:
fixtures Python, actionlint, bash -n, parser Kotlin y diff --check.

Resultado local:17 fixtures nuevas y132 regresiones existentes de B06, handoff,
comparación UI y cobertura aprobadas (149 casos distintos). También aprobaron
actionlint, bash -n, sintaxis de los tres Kotlin modificados y git diff --check.
El fixture de compatibilidad verifica que B06 sigue acreditando casos JUnit con
el nuevo esquema. Nada de ello sustituye compilar/ejecutar Android en Actions.

El bloque F003 pasa de14 a19 rutas acumuladas (máximo40), añadiendo cinco:
HomeShelvesIntegrationTest, el workflow B03, el script de fases, el generador de
informes y sus fixtures. La mejora alta ocupa10/10. F004 sigue sin iniciar.

Antes del commit se detectaron cinco commits remotos ajenos que solo cambian
cinco recursos de iconos. Se incorporaron por avance directo a
`69480b41012d3091e340be2535ebcfc6004acf19`, sin rebase ni commit de merge.
Esta reparación parte de esa base y modifica10 rutas; los iconos se conservan
sin edición. El acumulado propio de F003 es19; el diff global desde su primera
base contiene además esas cinco rutas ajenas, que no se ocultan ni se atribuyen
a esta entrega.

## CI191 — B03 aprobado; recuperación acotada de B02

[CI191/run44003106973](https://github.com/ed0668822/Aurelis-Music/actions/runs/44003106973),
SHA `3463388ec03d0f0301a73e73985c61dcd012c791`: B01/B03/B05 SUCCESS;
B02/B06 FAILURE, B04 SKIPPED. B03 ejecutó474 casos Debug y474 Release, todos
SUCCESS, sin errores ni omitidos. También aprobaron en el emulador API36
profileAndLibrarySettingsSurviveRecreation y
profileAndLibrarySettingsPreserveRealPlayback. El bloque de mejora B03 de10/10
queda validado para ese SHA; no se le atribuye un verde global ni prueba física.

Logs originales CRC válidos, `run191-logs.zip`, SHA256
`a975f8f5041c1e60110b2c23b74ab90830b6c87341d18c45ec0494b4c8dc6bf7`.
El fallo nuevo de B02 es de configuración de Gradle: Maven Central devuelve
HTTP429 Too Many Requests al descargar los POM kover-features-jvm y
kover-jvm-agent0.9.11. No aparece ninguna tarea Lint ejecutada. Por tanto no se
atribuye este rojo a un defecto detectado por Android Lint en la UI.

b02-run-lint.sh ya tenía recuperación de un «Could not find» de esos dos
módulos, pero no reconocía el «Could not resolve» con429 observado. La reparación
mantiene esa recuperación de metadatos una sola vez y añade una ruta específica:
HTTP429 de los POM0.9.11 en repo.maven.apache.org, error de configuración raíz,
ninguna tarea iniciada y ninguna señal de error Kotlin/Lint. Solo entonces hay
un segundo y, si persiste exactamente esa causa, tercer intento tras15/30s.
No usa --refresh-dependencies para429 ni añade mirrors o cambia versiones.

Cada intento conserva su log original y el conjunto lint.log. Se preservan los
códigos del comando y de los escritores de logs; un fallo de tee no activa la
recuperación. Un error real de Lint, cualquier tarea iniciada, otro host/módulo/
versión/estado HTTP o una causa distinta en el siguiente intento detiene la
recuperación. Tres fallos conservan el último resultado fallido; no hay cuarto
intento ni fabricación de XML. Los informes y puertas Debug/Release no cambian.

Esta reparación específica de B02 tiene cinco rutas, máximo10 por su dificultad:
b02-run-lint.sh, test_b02_run_lint.py, este informe, PROGRESS_TRACKER.md y
FUNCTION_BLOCKS_40_10.md. No se toca el bloque B03 ya aprobado. El acumulado
propio de F003 es21/40, al añadir dos scripts existentes no editados antes;
el diff de la rama desde la base inicial incluye además cinco recursos ajenos
de iconos (26 rutas globales), que se conservan sin edición.

Validación local:20 fixtures/regresiones Python B02 aprobadas, incluidas nueve
nuevas, con Gradle y sleep simulados; bash -n y diff --check aprobados. No se
ejecuta Gradle/Android localmente. Dos revisiones del mismo agente: (1) logs,
selector exacto de429 y compatibilidad de recuperación anterior; (2) límites,
salidas, conservación de fallos/logs, fixtures y diff. El resultado del nuevo
pipeline completo queda PENDIENTE. F004 sigue sin iniciar.

## Rutas de la entrega inicial (14 de40)

1. app/src/main/java/com/music/aurelis/MainActivity.kt
2. app/src/main/java/com/music/aurelis/ui/components/FrostedTopBar.kt
3. app/src/main/java/com/music/aurelis/ui/components/SquirclePillShape.kt
4. app/src/main/java/com/music/aurelis/ui/components/SearchField.kt
5. app/src/main/java/com/music/aurelis/ui/screens/SettingsScreen.kt
6. app/src/main/java/com/music/aurelis/ui/screens/ReplayScreen.kt
7. app/src/test/java/com/music/aurelis/ui/navigation/FrostedTopBarTest.kt
8. app/src/test/java/com/music/aurelis/ui/navigation/NavigationIntegrationTest.kt
9. NOTICE
10. docs/migration/beta-local/LICENSE_AUDIT.md
11. docs/migration/beta-local/FILE_MAPPING.md
12. docs/migration/beta-local/F003_TOP_BAR.md
13. docs/migration/beta-local/PROGRESS_TRACKER.md
14. docs/migration/beta-local/FUNCTION_BLOCKS_40_10.md

## Procedencia

Clon BitChord de solo lectura, commit `b690dc146009d4815825fa4a20815ef2e4ee8898`,
GPLv3 según LICENSE, sin atribuirle automáticamente la cláusula «or later».
Contribuyentes y condiciones originales se mantienen. Las cabeceras Aurelis
identifican la adaptación; no reemplazan las obligaciones de las partes copiadas.

| Fuente BitChord | SHA256 |
|---|---|
| app/src/main/java/com/music/bitchord/ui/components/FrostedTopBar.kt | 58aefbaabffadaf172faee21b4e6b94ec1b48c782651fd2913eb9dc3d04f83c9 |
| app/src/main/java/com/music/bitchord/MainActivity.kt | 90e254be457f57c70166321ca9bac3bc5e42eba4e6bdf577b48235ea75c200e2 |
| sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/components/SquirclePillShape.kt | b2c1388c27b79976ee39d17df51e6e202d68f612a5f5dc33396db7cc6b4c8f90 |
| sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/components/SearchField.kt | 23ceb4d3ef6a1ee5c7f0352552e3e10f2dafdb5d8ed23d3f278c01265c8afc25 |
| sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/components/Common.kt | 9e6847c462b52f4c94a53b24a297fe9aba1fa5a0f29accde2746e7a6c075b04e |
| sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/components/SongText.kt | 2579d2a9eb577c04de023e7dc1ce9ae00e9ec22477fc7706e135ff1e8e4ac79c |
