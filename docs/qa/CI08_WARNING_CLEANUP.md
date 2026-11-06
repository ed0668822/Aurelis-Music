# CI08 — advertencias, entrega1 de3

Base `c4a627d0b6d91c9ce25d2ab29992a8f93d723cba`; misma rama y PR1 «Aurelis Music Beta UI and components».
Autorización recibida para la primera entrega propuesta: limpieza de código.
No adelantar ONNX/SDK/dependencias ni alterar el plan principal. B07 detenido.

## Referencia y objetivo

Run142 https://github.com/ed0668822/Aurelis-Music/actions/runs/42271015823
finalizó SUCCESS en esta base, observado en la revisión anterior. El usuario
proporcionó los primeros20 diagnósticos. Esta entrega atiende9 entradas; solo
el nuevo lint/compile de GitHub podrá confirmar su desaparición. La aprobación
anterior de CI no verifica Android físico ni los cambios de esta entrega.

## Cambios concretos

| Entradas | Archivo/función | Tratamiento |
|---|---|---|
|4|ui/theme/Haze.kt:aurelisHazeEffect; ui/components/LiquidGlass.kt:liquidGlass|Opt-in localizado al contrato experimental de Haze Materials; mismos materiales/efectos.|
|1|ui/player/PlayerQueue.kt:queueMotion|Extensión de Modifier, LazyItemScope explícito y conservación del receptor en ambas ramas; seis llamadas actualizadas.|
|2|ui/components/SettingsComponents.kt:SettingsSubRow; ui/components/Skeletons.kt:SongRowSkeleton|modifier como primer argumento opcional; llamadas comprobadas.|
|1|playback/AudioRouting.kt:formOf|when con else y misma alternativa nombre→clase→UNKNOWN.|
|1|ui/player/PlayerPlatformUi.kt:OverlayBack|Retirar RequiresApi33 redundante e imports asociados; comentarios coherentes con minSdk33.|

Lista exacta de siete archivos y hashes en JSON adyacente. No se cambia el valor
de minSdk, la apariencia o los tiempos de animación. No supresión global de Lint
ni desactivación de comprobaciones. OptIn declara aceptación explícita del contrato
experimental existente; no transforma una API experimental en estable.

## Revisión1 — arquitectura, lógica y API

Pasada estática del mismo agente: javap confirma que ExperimentalHazeMaterialsApi
es RequiresOptIn WARNING y que animateItem es una extensión de Modifier dentro de
LazyItemScope. El helper recibe ese ámbito explícitamente, usa this@queueMotion
para preservar el receptor y mantiene fade/tween/motion reducido. Se revisaron
las seis llamadas dentro de item/items y los usos de los componentes con parámetros
reordenados: los requeridos conservan posición y los opcionales ya son nombrados.
AudioRouting conserva las dos clasificaciones especiales y el orden de fallback;
no agrega rutas/dispositivos o capacidades ficticias.

## Revisión2 — preservación, interfaz y licencias

Pasada estática separada del mismo agente. Se comparó Glass/Haze con la base
excluyendo únicamente import y OptIn: cuerpos de renderizado idénticos. Se comparó
PlayerPlatformUi excluyendo comentarios, anotación e imports retirados: mismo
código ejecutable de registro y liberación del callback. El diff de cola solo
cambia helper y seis conexiones; drag, estado, semántica y constantes iguales.
Tests, backdrop/guard QA01, servicio, workflow/gates, dependencias, firma, ABI,
manifest, código nativo y plan maestro permanecen intactos. Atribuciones originales
conservadas; sin código/recursos/dependencias de terceros nuevos ni cambios legales.
`git diff --check` correcto. Estos son controles estáticos, no ejecución Android.

## Compilación, pruebas y límites

Por instrucción del usuario, no se ejecutan Gradle/APK/tests Android localmente.
El workflow existente deberá compilar debug/release, ejecutar suites unitarias
completas y lint antes de permitir los APK. Cobertura pertinente existente:
GlassRenderingTest, VisualSystemTest, SharedGesturesTest, PlayerIntegrationTest,
QueueCoordinatorTest y SettingsIntegrationTest. No se agregan pruebas que repliquen
cambios de declaraciones; se conservan todas las regresiones actuales.

Estado nuevo: COMPILACIÓN/LINT/TESTS PENDIENTES DE GITHUB. Instalación, arranque,
Logcat, audio y GPU Android reales NO EJECUTADOS. No afirmar nueve advertencias
eliminadas hasta observar la nueva evidencia.

## Pendientes y continuación

Quedan11 entradas del listado original fuera de esta entrega:2 de ONNX16KB,6 de
AndroidX,1 de Gradle,1 de targetSdk y1 de ChromeOS/x86_64. El defecto ELF de ONNX
sigue abierto: zipalign no lo resuelve. No eliminar bibliotecas, cambiar filtros
ABI ni actualizar versiones únicamente para ocultar avisos.

Publicar en misma rama/PR1, avisar y esperar resultado del usuario. Tras recibirlo,
la siguiente entrega propuesta trata ONNX con comprobación ELF/modelos y requiere
su autorización. B07/B06.2/.3/.4/QA02 no se inician en esta entrega complementaria.
