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

## Rutas de la entrega (14 de40)

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
