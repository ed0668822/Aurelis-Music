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
