<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B05.1-a — MiniPlayer, gestos de cola y progreso

## Autorización y manifiesto previo

«Continúa» confirma B04-a y autoriza B05.1-a. Base
359c940e3c55dd7185bff8f9a66579aa02780146, misma rama/PR1. Sin descargar ZIP
ni atribuir al agente comprobaciones de CI anteriores. Máximo vigente del usuario
30 rutas acumuladas, posterior al límite histórico de seis de esta subunidad.

Manifiesto previsto (nueve archivos, incluidas correcciones):
- app/src/main/java/com/music/aurelis/ui/components/MiniPlayer.kt
- app/src/main/java/com/music/aurelis/ui/components/MiniPlayerGestures.kt
- app/src/main/java/com/music/aurelis/ui/components/GlassNavBar.kt
- app/src/main/java/com/music/aurelis/MainActivity.kt
- app/src/test/java/com/music/aurelis/ui/MiniPlayerInteractionTest.kt
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/B05_1A_MINIPLAYER.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

## Brecha y referencia

BitChord local fijo b690dc146009d4815825fa4a20815ef2e4ee8898, solo lectura.
app/src/main/java/com/music/bitchord/ui/components/MiniPlayer.kt:
SHA256 f294dfd1b75d1808fb86df2a93580efc87783daef5bc774f4ff9c3f6f7317b69.
miniPlayerGestures escoge eje al superar slop, favorece subir en diagonales
(bias1,5), cambia canción solo al soltar y usa umbral horizontal72dp. Glyph32dp,
spinner22dp; no se trasplanta entero el archivo ni sus fuentes remotas.

Aurelis solo tenía playerSwipeTrigger vertical: GlassNavBar recibía onPrevious
pero no lo conectaba al MiniPlayer. Se añade el gesto horizontal con capacidades
reales y callbacks actualizados sin reiniciar el detector en cada tick de playback.
El gesto vertical conserva PlayerSheetMotion/PlayerDock y cancelación explícita.
No se modifica el detector de la cabecera del reproductor ni se inicia B05.2.

Calidad contrastada con SourceQualityBadge.kt upstream, SHA256
aac9316e1eeb609d799955ab6a7fbd326fda8f761dbc31c20b7fe9eaedd9592a.
AudioQualityInfo de Aurelis ya discrimina metadatos/pista seleccionada/desconocido.
Se conserva esa fuente: sin etiquetas de marketing, inferencias por extensión,
Dolby, DSP ni afirmaciones sobre DAC/salida. B09 no se inicia.

## Implementación, compatibilidad y límites

MiniPlayer y GlassNavBar añaden canPrevious=false al final de sus parámetros;
MiniPlayer añade onPrevious vacío por defecto. MainActivity conecta canPrevious
real de PlaybackState; onPrevious existente termina en PlaybackViewModel.previous.
CanNext y enabled mantienen su origen real. El detector usa rememberUpdatedState
para callbacks/capacidades, incluso si cambian durante un arrastre; un gesto solo
elige un eje y emite como máximo un comando al levantar el dedo. Cancelaciones
horizontales no ejecutan comandos; las verticales llaman cancel al pull existente,
incluso al retirar el detector. Un arrastre descendente no abre ni cambia canción.

Umbral horizontal72dp; bias ascendente1,5. Se conserva el umbral vertical Aurelis
48dp tras slop para el fallback sin pull. Con pull se usa beginDrag/drag/release
de PlayerSheetMotion, sin animación ni segundo estado de playback. No se importa
el lift elástico de la barra upstream. Cancelar no se convierte en release(0).

Glyph play32dp, skip25dp, spinner22dp; permanecen objetivos48dp, altura mínima56dp
ampliable por texto y el tercer botón de expandir. La forma/material Liquid Glass,
carátula/dock y los controles de carga actuales se conservan. No se afirma que el
icono skip estándar sea idéntico al drawable upstream ni paridad visual completa.
Progreso válido se acota a0..1; NaN/infinito se rechazan como0 sin inventar avance.
AudioQualityInfo/sourceQuality y sus cuatro pruebas existentes se conservan.

## Pruebas y dos revisiones

Cuatro pruebas nuevas en MiniPlayerInteractionTest para Actions:
1. Swipes izquierda/derecha, callback y capacidades actualizados antes de soltar,
   sin comando al bloquear/retirar capacidad ni apertura accidental.
2. Cancelar horizontal/vertical no cambia cola ni libera el dock; subir en diagonal
   elige solo el eje vertical y conserva el contrato begin/drag/release/cancel.
3. Mini de320dp/texto1,5x mantiene objetivos de controles48dp; taps de transporte
   no expanden, y botón/gesto vertical expanden por la entrada existente.
4. Progreso finito/acotado para desconocidos y fuera de rango; la extensión del
   título no se convierte en una etiqueta de calidad.

Son pruebas de Compose/Robolectric y callbacks/pull controlados, no audio físico.
Se conservan las suites PlayerIntegrationTest, PlayerDockGeometryTest y
AudioQualityInfoTest para servicio/controlador, apertura/cierre y origen de calidad.
Revisión1 del mismo agente: fuente fija, detector de un eje/cancelación, callbacks
vigentes, capacidades y conexión única. Revisión2 del mismo agente: diff completo,
tipos/imports/API de gestos, accesibilidad/targets y atribuciones/nueve rutas.
git diff --check local; sin Gradle/Lint/APK/tests Android locales.

B01–B06 del SHA nuevo y estas pruebas están pendientes de Actions. No se acredita
fidelidad visual completa, tacto bajo TalkBack, GPU ni reproducción/rotación física.
B05.2/B05.3, B06 y B09 no se inician por esta entrega. Tras enviar, avisar y esperar:
«continúa» confirma éxito y autoriza la siguiente; investigar ZIP solo si comunica
un fallo. No crear Release oficial.


## Reparación de run125 — medir el objetivo táctil real

El usuario adjuntó Aurelis-Informes-B01-B06-125.zip y autorizó corregir el error.
ZIP íntegro verificado, SHA de la evidencia 3fd112c823bc0552833986fa87461bbc9d591449.
Run125/43566891741 terminó failure: B01/B02/B05 success, B03/B06 failure,
B04 skipped según API. results.json, XML y logs coinciden: 320 tests ejecutados
por variante, 319 aprobados, uno fallido, cero errores/omitidos. MiniPlayerInteractionTest
aprobó tres de cuatro casos por variante; solo falló
compactLargeTextKeepsTransportTargetsAndTapAndSwipeExpansionSeparate, línea115.
La generación Kover se omitió tras fallar suites; cobertura NOT_MEASURED por
COVERAGE_XML_UNAVAILABLE. La matriz incompleta no acredita un APK B04 verificado.

Causa: la prueba comparaba SemanticsNode.boundsInRoot (límites visuales) con los
48dp del objetivo táctil. Se contrastó el código fuente oficial de Material3
1.3.1 IconButton: minimumInteractiveComponentSize seguido de state layer40dp;
la zona táctil mínima se amplía a48dp sin exigir48dp al dibujo. El BOM declarado
es2024.12.01. En las fuentes oficiales ui-test-android1.7.6, BoundsAssertions.kt
expone assertTouchWidthIsEqualTo/HeightIsEqualTo y mide node.touchBoundsInRoot
con su densidad. Se usan esas APIs con48.dp para cada uno de los tres controles,
conservando texto1,5x, frame320dp y todas las acciones/aserciones restantes.
No se rebaja la dimensión exigida ni se elimina el test.

Solo se modifica la medición de la prueba y estos dos registros de entrega.
Sin cambios de producción, gestos, capacidades, Material3, SDK/ABI o puertasCI.
Tres rutas ya incluidas: nueve archivos distintos acumulados de máximo30.
Revisión1 del mismo agente: XML/logs/resultado y semántica visual frente a táctil,
API pública en versión existente. Revisión2 del mismo agente: diff, aserciones y
acciones conservadas, cabeceras y presupuesto. git diff --check local; sin Gradle
ni tests Android locales. El nuevo B01–B06 queda pendiente del SHA publicado.
B05.1-a no se cierra ni se inicia B05.2 hasta recibir el resultado del usuario.
