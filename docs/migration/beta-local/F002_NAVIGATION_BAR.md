<!-- Aurelis © 2026. GPL-3.0-or-later -->
# F002 — Barra inferior y Buscar separado

## Autorización y base

El usuario autoriza F002 después de F001, función por función. Base Aurelis
`4b60c1769fb59b9857850be959b4d2dd3df2f4c3`, rama
`feature/bitchord-local-migration`, PR1 con título conservado. Máximo 60 archivos
por entrega, incluidas correcciones. F003 y posteriores requieren autorización.
F002 es el ID del nuevo inventario; no renumera bloques B00–B13 ni fases CI.

BitChord se consulta en el clon de solo lectura `/workspace/BitChord`, commit
`b690dc146009d4815825fa4a20815ef2e4ee8898`. No se actualiza desde main ni se
copia su Gradle. Se conservan identidad Aurelis, perfil local, biblioteca,
AppNavigator, servicio/sesión, GlassSelectionPill y correcciones del backdrop.

## Diferencias comprobadas y adaptación

- La barra activa tenía altura fija de 56dp, margen de 16dp, elevación inferior
  de 8dp y glifos de 24dp. La referencia comparte margen 19dp, elevación 6dp,
  inset 4dp, padding vertical de pestaña 6,5dp, glifos 28dp y etiquetas 10sp.
- FloatingTabBar era una simplificación desconectada: no tenía el builder,
  transición de poses o estado de scroll del archivo original. Se adapta la
  implementación vendorizada real y se conecta nestedScroll en el host activo.
- Glass conserva tres pestañas y Buscar circular separado. Su pose compacta
  sigue el scroll del usuario; pulsar la pestaña compacta vuelve a expandirla.
  Los desplazamientos programáticos no cambian la pose ni se consume el scroll.
- El accesorio Glass usa las dos densidades del GlassNowPlaying original:
  carátula 30,4dp, esquinas 5,7dp, título 13sp y artista 12sp, transporte compacto
  y separación inline de 14dp. Se adapta a LocalSong, carátula local retenida,
  capacidades reales y callbacks existentes. El MiniPlayer normal no se modifica.
- Sin Glass se utiliza la FloatingBottomBar original adaptada, con cuatro
  pestañas en una cápsula y el MiniPlayer existente encima. Se conserva la
  reducción de blur y movimiento. El área de las barras tiene máximo 440dp.
- Se copian solo Home, TabExplore, TabLibrary, TabSearch y su helper de los
  vectores originales. No se incorporan SfIcons, fuentes SF Pro ni servicios
  remotos. El aviso MIT de cupertino_icons se conserva íntegro.
- Los insets quedan en los componentes originales: barra del sistema con suelo
  de 15dp y separación de 6dp; se retira el segundo navigationBarsPadding del
  contenedor padre. Su altura medida sigue reservando espacio en las listas.

## Adaptaciones de integración y estabilidad

Compose de Aurelis permanece en BOM 2024.12.01. Se inspeccionó, sin compilar,
SharedTransitionScope.kt del JAR fuente oficial animation 1.7.6: sharedElement
recibe `state`, no el nombre de parámetro nuevo `sharedContentState`. Se pasa
ese argumento por posición y se conservan las APIs disponibles de shared bounds,
lookahead y overlay. No se añaden ni actualizan dependencias.

Los bounds compartidos y tintes respetan movimiento reducido. Las pestañas
conservan Role.Tab/selected y callbacks actualizados; la pose saliente pierde
semántica durante la transición. El indicador conserva el motor Glass de Aurelis;
la captura de la fila y la lente se limitan a hardware/Glass/blur disponibles,
sin grabar el propio lector en su fuente. La barra normal adapta también el
sentido físico del arrastre y la posición del indicador para RTL.

La fuente sigue siendo sistema/Vazirmatn autorizadas. Se elimina únicamente la
altura de línea explícita heredada por los textos de esta barra para respetar
las métricas naturales del original; no se cambia el tema global. La ausencia
de metadatos explícitos en LocalSong no se disfraza con una insignia inventada.
No se declara identidad de píxeles ni validación física completa.

## Pruebas y evidencia

Se añaden seis regresiones, destinadas a GitHub Actions:

- NavigationBarScrollTest: umbral y cambio de sentido sin consumir gesto,
  exclusión de scroll programático y políticas explícita/inversa.
- NavigationBarLayoutTest: Buscar circular fuera del grupo y cuatro pestañas
  en modo normal; scroll de una lista real que pliega y vuelve a expandir el
  accesorio; arrastre RTL desde Buscar con selección lógica correcta.

Se conservan GlassNavigationTest, navegación por entradas y pruebas del MiniPlayer.
Las nuevas pruebas UI usan el fallback de software y no certifican GPU o fidelidad
visual. Compilación, Lint y pruebas Android SOLO en Actions; no se ejecutan aquí.
Comprobaciones locales: diff --check, procedencia/trazados, alcance de archivos
y análisis de sintaxis de los diez Kotlin cambiados con Tree-sitter aislado en
/tmp. Este parser no valida tipos ni sustituye compilación o pruebas Android.
Dos revisiones del mismo agente: (1) APIs/integración/estado/capacidades;
(2) layouts/gestos/semántica/reducción de efectos/licencias y diff final.

CI del nuevo commit: PENDIENTE. El resultado anterior de F001/CodeQL y run179
no se atribuye a esta entrega. Tras el push se comunica SHA, archivos y ejecución
real; se espera el resultado del usuario antes de F003. La prueba en Redmi Note
14 Pro 5G con Android 16/HyperOS 3 y la comparación visual completa siguen pendientes.

## Reparación posterior — CI180

El usuario comunica el fallo y autoriza su investigación y reparación dentro de
F002. Commit fallido `ab09f2c54e871859366c012523a8e4151f40b8a6`,
[run180/43958082003](https://github.com/ed0668822/Aurelis-Music/actions/runs/43958082003).
B01, B02 y B05 SUCCESS; B03 FAILURE, B04 SKIPPED y B06 FAILURE.
Los APK Debug e instrumentación sí compilaron. No se acredita APK Release
verificado porque B04 no se ejecutó.

Los logs originales se descargan fuera del checkout y pasan CRC:
`run180-logs.zip`, SHA256
`c8b2e76b1e2f1490092ba4499fe10f93cae4cb02203722b228e9a8e75e0d28f5`.
La descarga del informe consolidado (artefacto 14273561194) falla con403;
la causa se obtiene del ZIP válido de logs, no de un informe inexistente.

| Variante | Pruebas ejecutadas | SUCCESS | FAILURE |
|---|---:|---:|---:|
| Debug | 449 | 443 | 6 |
| Release | 449 | 443 | 6 |

Las seis regresiones nuevas de F002 sí aprueban en ambas variantes. Los fallos
son el caso loadingPermissionEmptyFailureAndRetryRecoverToRealHome de
HomeShelvesIntegrationTest y cinco capturas de PlayerIntegrationTest:
lyricsSourceSelectionPersistsWithoutSendingAnyRequest,
homeAutoMixKeepsCurrentItemAndPositionWhileReplacingUpcomingSongs,
translationDialogCancelNeverSendsAndBilingualViewKeepsOriginal,
playerHasUsableArtworkTimeControlsAndCompactBarAt360dp e
importedLyricsHighlightAndSeekWithOffsetInTheRealPlayer.

Todos registran IllegalArgumentException: Software rendering doesn't support
drawRenderNode, desde SharedBoundsNode.draw → GraphicsLayer → Canvas.drawRenderNode.
La barra instalaba sharedElement incluso sin hardware y con movimiento reducido;
esos nodos requieren RenderNode también en reposo. Las capturas llaman View.draw
sobre Canvas(Bitmap), que no dispone de GPU. No es un error de tipos ni se
identifica un fallo de los parsers, AutoMix o reproducción en esos casos.
Kover quedó omitido después de las suites fallidas; F5 rechazó
COVERAGE_XML_UNAVAILABLE. No se atribuye el origen a un porcentaje bajo de cobertura.

Corrección: los ocho puntos compartidos pasan por navigationSharedElement, que
conserva el modificador original solo con LocalGlassHardware y sin movimiento
reducido. El overlay compartido utiliza la misma condición. En las otras rutas
se mantienen composiciones, geometría, selección, gestos, accesorio y transición
normal sin nodos compartidos; no se desactiva Glass globalmente ni se toca el
motor estable del backdrop. En hardware con movimiento permanecen los shared
bounds originales. No cambian Gradle, SDK, ABI, secretos, workflows ni umbrales.

Se añade una regresión con View.draw(Canvas(Bitmap)), sin capturar excepciones,
que prueba ambas poses, con/sin canción, software con movimiento y movimiento
reducido con la bandera de hardware habilitada; verifica después navegación.
Se conservan las seis regresiones iniciales y todas las capturas que fallaron.
Son siete casos añadidos por F002 en total; el caso nuevo queda pendiente de
Actions y no acredita una GPU real por simular una bandera de composición.

Dos revisiones del mismo agente: causa/trazas/cobertura de todos los puntos
compartidos y, después, fallback/interacción/regresión/sintaxis/diff. Compilación
y pruebas nuevas exclusivamente en GitHub Actions. La reparación modifica cuatro
archivos ya incluidos en F002: implementación, prueba, este informe y tracker;
el acumulado sigue en17 rutas distintas sobre la base4b60c176, máximo60.
Nuevo CI de la reparación PENDIENTE. Esperar resultado del usuario antes de F003.

## Corrección de captura — CI181 y CI182

CI181 (`43961700531`, SHA `44005ca7`) no ejecutó pruebas: Maven Central
respondió HTTP429 al resolver kover-features-jvm y kover-jvm-agent0.9.11.
Los logs pasan CRC, SHA256
`de644a8dd764f64156ee4d0b68a2f60eaee8f1a7362b5526476be837577cc034`.
Se inició una ejecución completa del mismo SHA, sin cambiar dependencias ni
reutilizar parcialmente las fases: [CI182](https://github.com/ed0668822/Aurelis-Music/actions/runs/43963880770).

CI182: B01/B02/B05 SUCCESS, B03/B06 FAILURE y B04 SKIPPED. Debug ejecutó450
casos (449 SUCCESS,1 FAILURE); Release ejecutó450, todos SUCCESS. Los siete
casos nuevos de F002 aprueban en ambas variantes. El único fallo restante es
HomeShelvesIntegrationTest.loadingPermissionEmptyFailureAndRetryRecoverToRealHome,
con Software rendering doesn't support drawRenderNode desde SharedBoundsNode.draw.
F5 sin XML es consecuencia del fallo, no un porcentaje bajo demostrado.
ZIP de logs original `run182-logs.zip`, CRC válido, SHA256
`d2d64524b2bab693ae713e096d38de05c8095b17a6fa1d109750625fe96803bc`.

La condición añadida en44005ca7 es necesaria, pero no convierte un RenderNode
en contenido software: la ventana de Robolectric puede anunciar hardware mientras
la captura usa Canvas(Bitmap). Home heredaba esa bandera del tema. Este caso
configura ahora LocalGlassHardware=false en su proveedor de captura, junto a su
densidad existente. Mantiene movimiento y todos los recorridos de permiso,
vacío, error, reintento, modo claro/oscuro y fuente grande; no se suprime ninguna
excepción ni se omite el dibujo. Se comprueba que el canvas no tiene GPU y que
la imagen contiene más de20 colores. PixelCopy no es sustituto en este entorno:
VisualSystemTest documenta que Robolectric no conduce su callback forceRedraw.

La regresión de captura de NavigationBarLayoutTest se ejecutará en SDK33 (el
del fallo) y35, con ambas poses, con/sin canción y las dos políticas soportadas.
No se cambia código de producción, animaciones en hardware, dependencias,
umbrales, workflows ni APIs Android. La fidelidad GPU requiere la comparación
en emulador/dispositivo; una captura software no la acredita.

Dos revisiones del mismo agente: (1) logs de ambas variantes y correspondencia
entre canvas y proveedor; (2) preservación de aserciones, aislamiento del cambio,
matriz SDK y diff final. Comprobaciones locales limitadas a sintaxis Kotlin y
git diff --check; Gradle/pruebas Android solo en Actions. Cuatro archivos en
esta corrección;18 rutas distintas acumuladas desde4b60c176, dentro del máximo60.
Resultado del nuevo CI PENDIENTE; se avisará del push y se esperará al usuario
antes de F003.

## Fuentes y SHA256

| Fuente BitChord fijada | SHA256 |
|---|---|
| `app/src/main/java/com/music/bitchord/ui/components/GlassNavBar.kt` | `73e76c658ad723955b6ab2ec6c2a55ee7e01371896de801d4cf6b4546a7fe191` |
| `app/src/main/java/com/music/bitchord/ui/components/FloatingBottomBar.kt` | `aae3eb1e1e15b9face290c7ae2525aaabc4f6ff9661a04af67925acacee4209b` |
| `app/src/main/java/com/music/bitchord/ui/components/floatingtabbar/FloatingTabBar.kt` | `39f482b1f3cb168cbd37dbd6d74dc87a04be08184f5676aedd0180a33466ec3a` |
| `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/icons/BitChordIcons.kt` | `f08d862396d3c1d067f13e01feef3a9e7ce10914d5418b96dccc5f926ca3a202` |
| `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/components/SongText.kt` | `2579d2a9eb577c04de023e7dc1ce9ae00e9ec22477fc7706e135ff1e8e4ac79c` |
| `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/components/Common.kt` | `9e6847c462b52f4c94a53b24a297fe9aba1fa5a0f29accde2746e7a6c075b04e` |
| `app/src/main/java/com/music/bitchord/MainActivity.kt` | `90e254be457f57c70166321ca9bac3bc5e42eba4e6bdf577b48235ea75c200e2` |
| `app/src/main/java/com/music/bitchord/ui/theme/Theme.kt` | `8a0fe2f0118da55fe1448443344160c1a9e20cfda2d9c1acf7286c0e17fba900` |

FloatingTabBar conserva el aviso de Elyes Mansour/Apache-2.0 y modificaciones
Echo-Music/BitChord. GlassNavBar conserva la atribución Echo-Music/GPL-3.0.
Los trazados Cupertino conservan MIT, copyright 2016 Vladimir Kharlampidi;
texto original consultado en flutter/packages, blob de licencia
`581f84ccf02b284313f5f2c75433d3798990a352`. Se registran los cambios en NOTICE y
LICENSE_AUDIT; LICENSE y los textos legales anteriores permanecen intactos.
