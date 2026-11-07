# GLASS01 — material Liquid Glass antes de B04

Autorización: «Que sea el mismo que BitChord» y «autorización concedida».
Base: 1c9e843. Misma rama feature/bitchord-local-migration y PR1:
https://github.com/ed0668822/Aurelis-Music/pull/1.
Referencia fija: b690dc146009d4815825fa4a20815ef2e4ee8898, leída de la copia
local de BitChord exclusivamente de consulta. Evidencia/hash: JSON adjunto.

## Correspondencia y cambios

Las rutas de referencia de esta tabla se encuentran bajo
`app/src/main/java/com/music/bitchord/`; las de Aurelis bajo
`app/src/main/java/com/music/aurelis/`.

| Referencia BitChord | Aurelis | Cambio comprobable |
| --- | --- | --- |
| ui/components/LiquidGlass.kt | ui/components/LiquidGlass.kt | Mantiene parámetros/tintes, blur, lente, dispersión, sombra y reflejo; restaura variante ligera sin captura para fondos estables |
| ui/components/GlassNavBar.kt | ui/components/GlassNavBar.kt | Superficie activa usa liquidGlass completo como referencia; quita recorte y borde superpuestos que afectaban el contorno/sombra |
| ui/components/GlassNavBar.kt (GlassNowPlaying) | ui/components/MiniPlayer.kt | Aplica el material compartido al contenedor existente; fondo transparente y sin elevación tonal encima del cristal |
| MainActivity.kt (paintBackdrop) | MainActivity.kt | Graba primero el fondo opaco del tema y después la página, evitando transparencia en la capa muestreada |
| MainActivity.kt (glassSamplesBackdrop) | ui/components/GlassRendering.kt, MainActivity.kt, ui/components/GlassNavBar.kt | Misma política de captura para habilitado, efectos reducidos y aceleración hardware |
| ui/components/floatingtabbar/GlassSelectionPill.kt | ui/components/floatingtabbar/GlassSelectionPill.kt | Conserva física existente; lente plana cuando está deshabilitada o se reduce el desenfoque |
| ui/components/backdrop/internal/Shaders.kt | misma ruta relativa | Cinco bloques de texto AGSL idénticos a referencia; no se reescriben shaders |

La barra ya recibía el pipeline completo indirectamente a través de la función
ligera. Ahora la elección es explícita como en BitChord y la variante ligera
recupera su significado original. No se atribuye un incremento de refracción a
ese cambio de llamada: las diferencias visibles esperadas proceden del contorno,
el fondo grabado y el material del MiniPlayer. Requieren comprobación en Android.

Se conservan ocho constantes idénticas al origen: vibrancy1, blur8dp, altura y
cantidad de lente0.5 sobre48dp, opacidad0.4, escala0.33 y opacidad del indicador
oscuro0.14. Tintes claro/oscuro y programas de reflejo/refracción conservados.
Haze/superficie sólida siguen siendo alternativas para renderizadores sin
capacidad; no se fuerza AGSL sobre hardware incompatible.

## Revisiones

Primera pasada del mismo agente: lectura de fuentes reales y comparación de
constantes/programas, dependencias e imports, origen de la captura y ausencia
de modificaciones a los nodos de QA01. Se conserva el control de tamaño válido
antes de aplicar la lente. No se toca el motor ni el servicio de reproducción.

Segunda pasada del mismo agente: flujo de preferencias a página/fila/píldora,
contenido transparente del MiniPlayer, fallback sin GPU, callbacks de navegación,
pausa/siguiente/abrir existentes y atribuciones. LICENSE, LICENSES, Gradle,
workflows, ABI, manifiesto y plan principal comparados sin cambios. El JSON
registra hashes de los archivos originales de QA01 y fuentes modificadas.
`git diff --check` correcto. Estas dos pasadas son estáticas, no compilación.

## Pruebas y estado de ejecución

Se añadieron dos casos para que los ejecute el workflow existente de GitHub:

- GlassRenderingTest.pageAndLensSamplingStopsWhenEffectsAreDisabledOrReduced:
  evita que disponer de fondo/shaders ignore las preferencias del usuario.
- GlassNavigationTest.changingGlassAndReducedBlurKeepsMiniPlayerAndNavigationInteractive:
  verifica callbacks y accesibilidad al alternar cristal/efectos reducidos con
  el fallback software; utiliza una canción ficticia solo en pruebas.

Se conservan BackdropAttachRegressionTest y BackdropSizeRegressionTest de QA01,
además de PlayerIntegrationTest, pruebas de navegación y compilación de shaders.
El workflow ejecuta testDebugUnitTest, testReleaseUnitTest, lintDebug, lintRelease,
assembleDebug y assembleRelease. NO se ejecutan Gradle ni pruebas Android aquí.

| Comprobación | Estado |
| --- | --- |
| Comparación del código fuente, constantes y AGSL | Comprobada estáticamente |
| Preservación de defensas QA01 y textos legales íntegros | Comprobada por bytes |
| Compilación debug/release y pruebas JVM/Robolectric | Pendiente de GitHub |
| Instalación, arranque, navegación real y Logcat Android | No ejecutados; sin dispositivo |
| Reflejos, refracción, animación y rendimiento GPU Redmi | Pendientes de prueba en el teléfono |
| Paridad visual completa con BitChord | No verificada |

## Límites y continuación

Este complemento adapta el material de las superficies activas. No trasplanta
el contenedor FloatingTabBar completo de BitChord, el plegado por scroll, Search
separado ni el diseño/transición completos de GlassNowPlaying. Esas diferencias
siguen abiertas en el plan de navegación/reproductor; las documentadas en B02/B03
no se borran. El MiniPlayer conserva geometría, acciones y datos locales actuales.
No se presenta esta entrega como una copia visual completa ni como una app ya
verificada en Redmi. Sin dependencias ni componentes de streaming nuevos.

NOTICE conserva el origen EchoMusicApp/Echo-Music declarado por BitChord, junto
con Kyant0/backdrop y Ahmed Gamil. Pendientes legales históricos sin resolver.
Publicar en el mismo PR sin renombrarlo; avisar al usuario y esperar su resultado
de GitHub. B04 y QA02 no iniciados; no avanzar sin autorización.
