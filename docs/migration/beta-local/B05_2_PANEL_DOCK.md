# B05.2 — Panel progresivo y transición de carátula al dock

Base: a5e06bc; rama existente feature/bitchord-local-migration.
PR1: https://github.com/ed0668822/Aurelis-Music/pull/1; título conservado.
Autorización: «autorización concedida» tras entrega incremental B05. Se continúa
B05, sin B06/QA02, sin asumir que CI de B04 o B05 anterior pasó. El contrato de
16 campos y el orden del plan principal no cambian.

## Implementación

Se conecta el PlayerSheetMotion existente al host activo mediante inlineMode:
una instancia recordada en AurelisNavigationContent dirige desplazamiento,
arrastre, liberación, cancelación y cierre. AppNavigator conserva la propiedad de
la ruta; el servicio/controlador/cola no cambian. El resorte existente conserva
stiffness195, damping sin rebote y umbral0.5px. No se superpone un segundo slide de
AnimatedVisibility. Aperturas externas/restauradas usan reveal; sin canción el
Back conserva salida inmediata del estado vacío. Scope y movimiento se limpian
al retirar el panel/host, sin tareas globales ni cambios en la Window activa.

MiniPlayerPull incorpora inicio y cancelación con implementaciones por defecto
para mantener compatibilidad. PlayerSwipeTrigger reenvía deltas/velocidad al
movimiento compartido. Un arrastre sigue el dedo, no solo dispara al soltar.
Al soltar se decide por mitad de recorrido o fling configurado en900dp/s; al
cancelar vuelve al estado de origen. Sin pull conectado conserva el trigger
simple de48dp, usado por composables aislados. ReduceAnimation evita el resorte
y la carátula volante; el arrastre manual conserva respuesta directa.

Se conserva la página previa bajo NowPlaying/Queue. La barra permanece compuesta
cuando el player se abrió desde una pestaña, de modo que no se pierde la ruta del
puntero ni el extremo de destino. Página/barra bajo overlay no se exponen a
accesibilidad y un fondo modal intercepta nuevas pulsaciones detrás del panel.
Sus controles mantienen callbacks; no se duplica la navegación ni la reproducción.

PlayerDock registra host, panel y ambas imágenes como LayoutCoordinates con ID
de canción; libera referencias al retirar los extremos. Mini se mide respecto
al host; carátula grande respecto al viewport interior del panel, evitando restar
una matriz animada que pudiera corresponder al frame anterior. Ambos viewports
comparten tamaño y origen de reposo en este host inline. No se usa este esquema
para ventanas separadas. Geometría transformada, sin recorte de ancestros, validada
finita/no vacía antes de dibujar; cambios de canción no reutilizan un dock antiguo.

DockedArtworkOverlay dibuja una sola imagen adicional durante movimiento portrait,
interpolando medidas y esquinas8→24dp. Tamaño de decodificación estable durante la
transición; desplazamiento/escala en graphicsLayer, sin pedir otro bitmap por frame.
El origen físico es TopLeft incluso en RTL. Los extremos conservan su layout y
ceden solo el dibujo. Sin medidas válidas, con animación reducida, en horizontal o
sin MiniPlayer en la página previa se conserva slide normal. El fondo del panel
se desvanece mientras vuela la carátula. Liquid Glass y sus defensas QA01 intactos.

## Correspondencia / archivos

Referencia consultada, solo lectura: BitChord b690dc146009d4815825fa4a20815ef2e4ee8898:
app/src/main/java/com/music/bitchord/ui/player/PlayerSheetMotion.kt y
sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerDock.kt.
Se adapta su infraestructura ya portada; no se importa otro renderer, fuente,
modelo, binario ni dependencia. NOTICE/cabeceras existentes conservan autoría.

Modificados: MainActivity, GlassNavBar, MiniPlayer, AppNavigationHost,
PlayerDock, PlayerSheetMotion, PlayerSwipeTrigger, NowPlayingScreen y
PlayerIntegrationTest. Nuevo DockedArtworkOverlay; pruebas nuevas
PlayerDockGeometryTest y PlayerPanelMotionTest. Nombres completos/hashes en JSON.

No tocar: servicio/controlador/DSP/calidad de B05.1, biblioteca de B04, nativo/ABI,
manifiesto, Gradle/workflow, secretos/firma, shaders/defensas QA01, LICENSE/LICENSES,
plan maestro y definición de bloques. Comparación contra la base sin cambios.

## Revisiones

1. Pasada estática de arquitectura: continuidad del puntero, un solo movimiento y
   ruta, medidas del mismo track, descarte de NaN/Inf/vacíos, recursos/lifecycle,
   restauración sin medios y compatibilidad de API. Se añadió limpieza explícita
   al cancelar antes del primer frame: riesgo de carrera detectado por análisis,
   no fallo reproducido en un dispositivo. Release sin fling positivo usa distancia.
2. Segunda pasada del mismo agente: semántica/Back, cancelación desde cerrado y
   abierto, desacoplamiento de coordenadas del panel, RTL/ventanas pequeñas,
   esquinas/renderizado, cambios de canción, licencias y regresiones protegidas.
   Firmas AbsoluteAlignment.TopLeft, localBoundingBoxOf, VelocityTracker y
   BroadcastFrameClock.sendFrame contrastadas con javap en los JAR existentes.
   No es compilación ni revisión de un segundo ingeniero. git diff --check correcto.

## Pruebas preparadas y pendientes

Siete casos nuevos NO EJECUTADOS localmente:

- PlayerDockGeometryTest: interpolación/extremos/clamp y rechazo de medidas
  vacías, invertidas, NaN/Inf/desbordamiento.
- PlayerPanelMotionTest: cancelación antes del primer frame con reapertura;
  cancelar desde abierto; entradas no finitas; apertura/cierre del resorte con
  BroadcastFrameClock controlado. Los primeros casos usan efectos reducidos.
- PlayerIntegrationTest: cancelar arrastre de cabecera conserva player/cola.

La regresión previa de swipe abre/cierra ahora recorre600px para probar el panel
progresivo y no el trigger de48dp. Se conservan pruebas de restauración sin medios,
quality, navegación y QA01. Ninguna se marca aprobada sin ejecución. El workflow
existente de GitHub ejecutará testDebugUnitTest/testReleaseUnitTest, lintDebug/
lintRelease y assembleDebug/assembleRelease, con evidencias/artifacts ya configurados.
No se ejecutaron Gradle, Android ni APK aquí.

Pendientes: resultados de CI, capturas del morph y de sus extremos, rotación real,
multitouch/cancelación OEM, RTL/texto grande/TalkBack, GPU Redmi, Logcat y audio.
Robolectric y el reloj controlado no certifican esas capacidades. Si falla CI o
hay regresión de reproducción/persistencia se detiene avance y se corrige con
cambios mínimos; no se ocultan fallos ni se desactivan comprobaciones.

## Continuidad

B05.2 panel/dock conectados en código, cierre verificable pendiente. La beta no
se declara idéntica a BitChord: configuración del host inline difiere del modal
upstream, siguen pendientes cola/letras laterales y validación visual total.
Publicar en misma rama/PR y esperar resultado comunicado por el usuario. B06 y
QA02 siguen sin iniciar. Priorizar errores de esta entrega antes de nuevos bloques.
