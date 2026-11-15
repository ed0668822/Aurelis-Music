<!-- Aurelis © 2026. GPL-3.0-or-later -->
# PROGRESS_TRACKER — beta local

> **Límite vigente solicitado:** máximo **30 archivos por bloque**, contando toda la entrega y todos sus commits, incluidos pruebas, recursos, documentación, configuración y eliminaciones. Esta regla posterior prevalece sobre el techo35 histórico de [PLAN_CONTINUATION](PLAN_CONTINUATION.md); los subbloques menores siguen permitidos. [PLAN_30_FILES](PLAN_30_FILES.md) conserva su registro histórico. No se cierran retrospectivamente B06/B07 ni se inicia B09 por registrar el plan.

## Continuación actual — primera entrega documental

### Pre-B07 — comparación ejecutable preparada; resultado nuevo pendiente

Run43670279287/b8ee8147 terminó SUCCESS; B06.3-d tiene pipeline aprobado. No acredita
paridad visual/física. B06.4 excluido por decisión del usuario, no implementado.
«Continúa» autoriza ejecutar la comparación propuesta entre las dos apps. Workflow
separado: Aurelis usa B01; BitChord se compila en el commit fijo sin cambios y sin
bundleWear, conservando SDK/ABI. Mismo emulador API35, dimensiones/escala, fixtures
FLAC originales con carátulas/LRC incrustado, captura claro/oscuro y ambos layouts.
Galería HTML, PNG/XML, condiciones, hashes y estados no alcanzados explícitos.
No comparación completada ni igualdad certificada: widgets/movimiento/accesibilidad
siguen pendientes y cada captura requiere revisión. No se inicia B07. Catorce rutas,
dos revisiones estáticas del mismo agente, nueve fixtures Python aprobados, actionlint,
py_compile y diff --check correctos. Fixtures reales generados/ffprobe comprobado
localmente; ninguna compilación ni ejecución Android local. Ver PRE_B07_COMPARISON_RUN.

### Prioridad posterior — fidelidad de toda la UI antes de B07

El usuario pide verificar y adaptar la misma interfaz de referencia con música
local: iconos, letras, carátulas, widgets y ubicación/orden de ajustes incluidos.
Se abre puerta previa a B07, sin declarar cerradas las diferencias visuales por
las entregas anteriores. Contraste estático inicial en PRE_B07_UI_PARITY.md;
UI_PARITY_CHECKLIST.md registra criterios. Ajustes simplificados y registro de
widgets siguen pendientes; no se han realizado cambios de código en esta revisión.

### Cambio de alcance — Android Auto excluido

Por indicación explícita del usuario, B06.4-a/-b quedan fuera del alcance, junto
a TV y Wear. No se implementan ni se consideran completados mediante código.
No bloquean el paso a B07. B06.3-d continúa pendiente del resultado de Actions;
se mantienen las pruebas físicas pendientes de los bloques anteriores.


### B06.3-d — registro desde el servicio (CI nuevo pendiente)

«Continúa» acepta la reparación e17c6db4 de B06.3-c; no se descargan informes previos.
Dieciocho archivos de máximo30, misma rama y PR1. Un recorder mide tiempo monotónico
solo mientras isPlaying, conserva ocurrencia y crea sesión por transición/repeat;
seek, pausa, buffering y supresión de foco no añaden distancia/tiempo suspendido.
Diario AtomicFile privado antes de SQLite; reintentos con mismas claves y UTC,
replay idempotente tras commit sin confirmación. IO separado, cierre asíncrono
acotado en memoria, serialización por ruta entre cierre y reapertura. Saturación/
fallos se informan mediante extras de la sesión y herramientas del reproductor;
no detienen audio ni borran diarios desconocidos/corruptos. Totales legacy siguen
su único recorder; no se importan como eventos fechados.
Diecinueve pruebas nuevas preparadas y una comprobación UI ampliada; no ejecutadas
aquí. Dos revisiones estáticas del mismo agente y diff --check; Gradle, Lint, suites
Android y verificación APK corresponden a Actions. No afirmar éxito de esas pruebas
ni funcionamiento físico. Límites de pérdida tras muerte abrupta documentados en
[B06_3D_SERVICE_EVENTS](B06_3D_SERVICE_EVENTS.md). B06.4 está excluido por decisión posterior del usuario; cierre de implementación
de B06 pendiente del resultado de B06.3-d.

### B06.3-c — corrección aceptada por el usuario; físico pendiente

Run141: B01/B03/B05 SUCCESS, B02/B06 FAILURE, B04 SKIPPED. B02 no llegó a ejecutar
Lint: faltó resolver dos módulos Kover0.9.11 durante configuración. POM/JAR oficiales
ahora200; causa entre publicación transitoria/caché negativa no demostrada. Reparado
con un reintento acotado --refresh-dependencies solo para esa firma, conservando
ambas ejecuciones y todas las puertas XML/resultado. Sin cambios de versión/SDK/ABI.
Once tests Python B02 pasan, dos revisiones propias y sintaxis/diff correctos; nuevo
CI pendiente. Seis archivos de reparación, doce distintos acumulados del bloque.


«Continúa» confirma la correcciónf7f3dd46. Nueve archivos de máximo30, misma rama/PR1,
sin descargar informes previos. SQLiteOpenHelper privado por perfil, IO serializado
fuera de main/audio, clave sesión/secuencia idempotente y conflictos atómicos sin
sobrescribir. Ventanas temporales/paginación estable; futuro/corrupto preservados,
sin borrar ni reconstruir historial. Totales antiguos intactos; integración desde
servicio queda para B06.3-d (no afirmar registro activo aquí). Diez pruebas nativas
SQLite bajo Robolectric preparadas para Actions; dos revisiones del mismo agente y
diff --check correcto. Sin Gradle/Android locales, nuevo CI/hardware pendientes.
[B06_3C_EVENT_STORE](B06_3C_EVENT_STORE.md). B06 sigue abierto.

### B06.3-b — corrección aceptada por el usuario; físico pendiente

Run139: B01/B03/B05 SUCCESS; B02/B06 FAILURE, B04 SKIPPED. Lint UseSdkSuppress por
RequiresApi redundante en test instrumentado. Retirada esa anotación; permanece
SdkSuppress(minSdkVersion=26), guard API33 y todo el caso de notificación/foco.
Tres archivos ya incluidos; catorce acumulados. Dos revisiones del mismo agente y
diff --check, sin Android local. Nueva validación pendiente; ver informe B06_3B.


«Continúa» confirma1a14927f. Catorce archivos de máximo30, misma rama/PR1, sin ZIP.
Política de interrupciones Media3 compartida; quitar tarea no conserva servicio en
IDLE/ENDED/error, pero sí intención READY con supresión transitoria. Dos pruebas
Robolectric de receptor noisy real sin prepare; nueva octava instrumentación exige
token de notificación pause/play y foco transitorio con supresión/recuperación del
mismo Player. Gates/reportes exigen identidad del octavo caso, sin inferir físico.
18 pruebas Python de parser/informe aprobadas; YAML/Bash/diff --check correctos.
Dos revisiones del mismo agente. Tres pruebas Android nuevas pendientes de Actions;
auriculares/pantalla bloqueada/sonido físicos pendientes. B06 aún abierto.
[B06_3B_SYSTEM_CONTROLS](B06_3B_SYSTEM_CONTROLS.md).

### B06.3-a — resultado aceptado por el usuario; físico pendiente

«Continúa» confirma869ffe6a por el usuario; no descarga ZIP ni auditoría repetida.
Doce archivos de máximo30. IDs de ocurrencia/tiers/permutación en v2, lector v1 sin
borrar bytes antiguos, ausentes remapeados y restauración pausada. Vaciado real se
guarda vacío, cancelar una carga pendiente no borra snapshot recuperable. Coordinador
del servicio posee binding; comandos remotos y cambios de cola cancelan cargas tardías.
Doce regresiones nuevas para Actions (cinco codec/siete binding, incluyendo ExoPlayer
y comando MediaController real). Dos revisiones del mismo agente y diff --check;
sin Gradle/Android locales. Físico/CI nuevo pendientes; B06 sigue abierto.
[B06_3A_RESTORE](B06_3A_RESTORE.md).

### Reparación CI136 antes de B06.3

B01/B05 aprobados; B02 falló WrongConstant de SessionResult/SessionError. B03 compiló
Debug pero no terminó testDebugUnitTest en unos30min; Release no comenzó. Run
cancelado para recuperar logs oficiales, sin declarar pruebas aprobadas. Constante
corregida, sondeo infinito de controles sustituido por eventos de sesión (causa única
del atasco aún no demostrada). Regresión de scheduler pausado y notificación de
cancelación. CI registra cada prueba y limita cada variante12min con fallo real.
Dos revisiones del mismo agente, YAML/Bash/diff --check; sin Android local. No B06.3
hasta validar. Push siguiente incluye también B06.2-b/-c ya autorizados/preparados,
sin reescribir historia; unión completa de archivos debe permanecer≤30.

### B06.2-c — continuación automática local (CI pendiente)

Autorización acumulada terminar B06. Trece archivos de máximo30. Al finalizar cola
solicitada, repeatOFF y opción activa, consulta MediaStore y añade hasta diez temas
locales no presentes, con tier AUTOPLAY/IDs nuevos. Valida fingerprint y estado al
volver; pausa/cambio/opción/cierre cancelan y rechazan respuestas tardías. Catálogo
vacío/error no inicia bucle. Cinco pruebas nuevas de selector/binding y caso UI
previo extendido. Dos revisiones del mismo agente, diff --check, Android solo CI.
[B06_2C_LOCAL_AUTOPLAY](B06_2C_LOCAL_AUTOPLAY.md). Validación física pendiente.

### B06.2-b — velocidad/silencio con capacidades (CI pendiente)

Autorizado dentro de terminar B06. Diez archivos de máximo30. Setters reales sobre
el único player, estado publicado y controles0.5–2x/silencio. Salida float/PCM24/32
excluida por la ruta actual del sink; motivo visible y retorno1x/sin salto. Sin
cambios DSP/ABI/SDK. Cuatro regresiones para Actions: propiedades/identidad/posición,
valores inválidos, encodings restringidos y acciones UI. Dos revisiones del mismo
agente y diff --check; sin Android local ni afirmación de efecto audible medido.
[B06_2B_SPEED_SILENCE](B06_2B_SPEED_SILENCE.md). B06.2-a mantiene CI pendiente.

### B06.2-a — temporizador del servicio (CI pendiente)

Autorización «Continúa con el siguiente bloque y termina B06». Basec6ad3dd2,
misma rama/PR1, catorce archivos de máximo30. Deadline monotónico, after-track,
cancelación/rearmado, estado observado y controles reales en ambos layouts.
Cinco pruebas para Actions: UI sin capacidad, plazo/rearmado, cancelación/cierre/límites,
auto/repeat/ENDED/selección manual y comando de sesión tras cerrar controlador.
Dos revisiones del mismo agente, diff --check; sin Gradle/Android locales.
CI/dispositivo pendientes. [B06_2A_SLEEP_TIMER](B06_2A_SLEEP_TIMER.md).

### B06.1-b — resultado aceptado por el usuario; físico pendiente

Run134: B01/B02/B05 SUCCESS; B03/B06 FAILURE, B04 SKIPPED. Debug y Release ejecutan
344 pruebas cada uno, falla el mismo caso UI de append sin capacidad privada.
Se restaura append estándar Media3 en esa alternativa; Play Next sigue protegido y
la sesión con capacidad conserva pinning. Regresión existente reforzada sin rebajar
su expectativa. Cuatro archivos ya incluidos, quince acumulados; dos revisiones del
mismo agente y diff --check. Nuevo CI/cobertura pendientes; ver informe B06_1B.


Run133 FAILURE en compileDebugKotlin: dos SessionResult.Code colocadas en destinos
no permitidos (función/parámetro). Corregidas como anotaciones del tipo Int; también
se declara el tipo del código local. Evidencia: ZIP oficial de logs verificado;
B02/B03/B04/B05 omitidos, sin pruebas ejecutadas. Tres archivos ya incluidos, quince
distintos acumulados. Dos revisiones del mismo agente y diff --check correcto;
nueva compilación y validaciones pendientes de Actions. Detalle en informe B06_1B.


«Continúa» confirma la reparación80dcae7a y autoriza este bloque. Quince archivos
de máximo30, dos revisiones estáticas del mismo agente; sin descarga de ZIP previo.
La sesión existente aplica inserciones/movimientos a la permutación nativa y vacía
solo USER_QUEUE futuras. UI habilita según comando privado disponible; botón de
vaciar conectado también a la cola plana. Inserciones físicas al final conservan
orden original al apagar shuffle. Duplicados con identidad nueva; current/posición,
pausa y controlador único conservados sin setMediaItems/prepare/seek en la edición.
Nueve regresiones preparadas (siete Media3/sesión, una coordinador y una UI); no se
han ejecutado localmente. CI nuevo/audio físico pendientes. Detalles, límites y
manifiesto en [B06_1B_QUEUE_EDITS](B06_1B_QUEUE_EDITS.md). Esperar tras push.

### B06.1-a — reparación aceptada por el usuario; físico pendiente

Run131 falló en compileDebugUnitTestKotlin y compileReleaseUnitTestKotlin por argumento
de tipo inválido en RuntimeEnvironment.getApplication del test de MediaController.
Ambas suites: cero pruebas/XML; no se declara que aprobaron. Corregida la llamada
sin argumento genérico, conservando las seis regresiones. Tres archivos ya incluidos,
once distintos acumulados. Dos revisiones estáticas del mismo agente y diff --check;
nueva compilación/ejecución pendiente de Actions. Detalle en B06_1A_SHUFFLE_VIEW.md.


«Continúa» confirma B05.3-b y autoriza esta entrega; base2e85dc59, misma rama/PR1.
Once archivos de máximo30 según [B06_1A_SHUFFLE_VIEW](B06_1A_SHUFFLE_VIEW.md).
Se proyecta el orden real de Timeline Media3, conservando índices físicos para
jump/remove y ocurrencias duplicadas; desactivar muestra orden original sin reset.
Orden no verificable se avisa; reordenación bajo shuffle se bloquea y explica,
selección/eliminación disponibles. Algoritmo global de QueueShuffle BitChord no
copiado, solo objetivo adaptado; pinning/Play Next/sucesor quedan para B06.1-b.
Seis regresiones para Actions (incluyen ExoPlayer y MediaSession/Controller), dos
revisiones del mismo agente, diff --check y XML válido. Sin Gradle/Android locales;
nuevo CI/audio físico pendientes. Avisar tras push y esperar antes del siguiente.

### B05.3-b — resultado aceptado por el usuario; físico/GPU pendiente

«Continúa» acepta B05.3-a y autoriza esta entrega, basec5139134. Misma rama/PR1,
sin descargar ZIP. Estado de scroll compartido/guardable de cola entre lateral y
modal; no se recentra al readjuntar o cambiar efectos, sí ante cambios reales de
canción/historial. Se omite preparar paleta cuando reduceBlur omite mesh.
Dos regresiones de restricciones/restauración/efectos con mismo playback; ocho
rutas de máximo30, dos revisiones del mismo agente y diff --check. Sin Gradle ni
Android locales, nuevo CI/físico/GPU pendiente. Ver [B05_3B_CONTINUITY](B05_3B_CONTINUITY.md).
No se cierran pendientes B07 ni se inicia B06/B09; avisar tras push y esperar.

### B05.3-a — resultado aceptado por el usuario; físico/visual pendiente

«Continúa con el siguiente bloque» acepta la corrección44c0c9db y autoriza este
bloque. Sin descargar informes previos. Ocho rutas de máximo30, misma rama/PR1.
Slots horizontales existentes conectados a letras y cola reales, contenido modal/
lateral compartido, transporte/cabecera visibles y Back al principal. Mismas
capacidades/cola/lookup/offset/traducción, sin segundo controlador. Fades omitidos
con reducción de movimiento. Dos regresiones preparadas para Actions; dos
revisiones del mismo agente y diff --check. Sin Gradle/Android locales; CI, GPU y
fidelidad física pendientes. Ver [B05_3A_SIDE_PANES](B05_3A_SIDE_PANES.md).
No iniciar B05.3-b/B06/B09 hasta la siguiente autorización.

### B05.2-b — corrección aceptada por el usuario; físico/GPU pendiente

**Reparación run127 autorizada:** ZIP válido leído. Debug320/324, cuatro errores
RenderNode al capturar Canvas software; Release324/324. Se reemplaza GraphicsLayer
por Canvas.saveLayer/Plus acotado con restore en finally, sin eliminar el efecto
ni modificar tests previos/puertasCI. Regresión de captura software en memoria,
sin guardar imágenes. Cuatro rutas existentes; ocho acumuladas de este bloque,
once entre ambos. Dos revisiones del mismo agente y diff --check; nuevo CI pendiente.

Segundo bloque autorizado junto a B05.2-a, base668daa36. Adaptación parcial del
delta aprobado acf219a7: tinta aditiva limitada a iconos skip con hardware y sin
reduceBlur; fallback sólido del tema y atenuación de controles sin capacidad.
Botón central/targets/callbacks conservados. Ocho rutas, dos pruebas preparadas;
dos revisiones del mismo agente, diff --check y sin Gradle/Android locales.
Las dos entregas suman once rutas distintas y se envían juntas, cada una en su
commit. Nuevo CI/físico/GPU pendiente. Ver [B05_2B_CHROME](B05_2B_CHROME.md).

### B05.2-a — cabecera/panel (CI pendiente)

Dos bloques autorizados por el usuario, base48b72fae. Resultado previo aceptado
sin descargar ZIP. Se garantiza cancelación del pull al retirar el detector de
cabecera, sin duplicar cancel/release. Seis rutas, dos regresiones para Actions,
dos revisiones del mismo agente y diff --check; sin Gradle/Android locales.
Ver [B05_2A_HANDLE](B05_2A_HANDLE.md). Pendiente nuevo CI/físico.

### B05.1-a — resultado aceptado por el usuario; físico/visual pendiente

**Corrección run125 autorizada:** ZIP aportado y verificado. SHA3fd112c8,
B01/B02/B05 success, B03/B06 failure y B04 skipped. Por variante319/320 tests
aprobados; único fallo en la medición de tamaño de MiniPlayerInteractionTest:
confundía bounds visuales del state layer40dp con hit target48dp. Contrastadas
fuentes oficiales de las versiones existentes: ahora usa assertTouchWidthIsEqualTo
y assertTouchHeightIsEqualTo(48.dp), sin reducir tamaño ni quitar acciones/tests.
Kover omitido por el fallo; no se modifica la puerta de cobertura. Tres rutas
ya incluidas, nueve acumuladas de máximo30; dos revisiones del mismo agente y
diff --check. Sin Gradle/Android locales; nuevo CI pendiente, B05.2 sin iniciar.

«Continúa» confirma B04-a y autoriza la siguiente entrega. Base359c940e, misma
rama/PR1, referencia local BitChord fija b690dc1. Detector único MiniPlayer para
swipes de cola y pull ascendente; comandos al soltar y capacidades/callbacks
vigentes. Se conserva PlayerSheetMotion/PlayerDock, objetivos48dp y expandir;
glyph/spinner alineados con referencia y progreso finito/acotado. AudioQualityInfo
mantiene origen de entrada real/desconocido, sin marketing ni afirmación de salida.
Nueve archivos de máximo30 en [B05_1A_MINIPLAYER](B05_1A_MINIPLAYER.md), cuatro
pruebas nuevas para Actions y dos revisiones del mismo agente; diff --check local.
Sin Gradle/Android locales; nuevo CI y comprobaciones físicas/visuales pendientes.
Avisar tras push y esperar respuesta; no descargar ZIP preventivamente ni iniciar
B05.2/B05.3/B06/B09 u otro bloque.

### B04-a — resultado aceptado por el usuario; físico/visual pendiente

«Continúa» confirma el resultado bueno de B03.3-a y autoriza esta entrega;
no se descargan informes ni se afirma una nueva lectura de su CI. Base5fde06ac,
misma rama/PR1. Revisión estática de los flujos existentes de géneros/búsqueda/
carpetas/biblioteca: se conservan, sin rehacerlos. Brechas corregidas en selección:
acciones múltiples siguen el orden visible y IDs seleccionados se restauran junto
al modo/orden. Solo IDs en Bundle; medios ausentes se excluyen de las acciones,
referencias privadas y playlists conservadas. Seis rutas de máximo30, manifiesto
en [B04_A_SELECTION](B04_A_SELECTION.md); tres pruebas nuevas para Actions,
dos revisiones del mismo agente y git diff --check. Sin Gradle/Android locales.
Nuevo CI y comprobaciones físicas/visuales pendientes; avisar tras push y esperar
respuesta. No se inicia B05/B09 ni otra entrega en este bloque.

### B03.3-a — resultado aceptado por el usuario; físico/visual pendiente

«Continúa con el siguiente bloque» confirma el éxito previo y autoriza esta
entrega. Base14e4567c, misma rama/PR1; siete rutas de máximo30 según el manifiesto
[B03_3A_RECOVERY](B03_3A_RECOVERY.md). Home permite desplazar estados largos y
mantiene cabecera/padding; isRefreshing del ViewModel se conecta para distinguir
vacío de escaneo activo y mostrar progreso sin retirar canciones ya cargadas.
Callbacks de permisos/Ajustes, cancelación y escáner existentes conservados.
Tres pruebas nuevas para Actions (pantalla baja/texto grande, permiso y lecturas
controladas con ViewModel real); dos revisiones del mismo agente y diff --check.
Sin Gradle/Android locales; B01–B06 y comprobaciones físicas/visuales pendientes.
Regla nueva explícita: tras push avisar y esperar; «continúa» confirma éxito y
ordena el siguiente bloque, sin descargar ZIP. Solo investigar/descargar informes
cuando el usuario comunique fallo. No iniciar otro bloque antes de esa respuesta.

### B03.2-a — implementación entregada, run122 success; visual/físico pendiente

**Resultado posterior:** run122/43552336276, SHA fcc10d128df854ef819d786cb14d5641e9724460,
SUCCESS en B01–B06 confirmado por API. Kover, evaluación de cobertura, puerta F5
y Enforce both test suites terminaron success. El informe original no se pudo
descargar por el proxy: recuento exacto de tests/porcentaje de cobertura no leídos.
Queda resuelto el fallo de CI; implementación de esta subentrega entregada, sin
cerrar la fidelidad visual/física pendiente ni B03 completo. Siete rutas acumuladas;
cierre documental con dos revisiones del mismo agente y git diff --check. No se
traslada el verde de fcc10d12 al commit documental posterior ni se inicia otro bloque.


**Antecedente corregido:** Run121/43549705914 de da216319 terminó failure: B01/B02/B05 success,
B03/B06 failure, B04 skipped. Descargas de resultados/logs bloqueadas por proxy
al dominio productionresultssa31.blob.core.windows.net. Defecto estático
comprobado en la prueba nueva: import de RobolectricActivityRule en paquete
inexistente; se retira para resolver la clase del mismo paquete ui. Se conserva
el contenido de las dos pruebas; nuevo CI pendiente, sin declarar causa única
ni suites aprobadas sin sus evidencias. Siete rutas acumuladas, dos revisiones
del mismo agente y git diff --check; sin Gradle/Android locales.

Autorización «Continúa con el siguiente bloque». Base 43cdc0c0, misma rama/PR1;
B03.1-a CI resuelto con run120 success. Siete archivos distintos de máximo 30;
manifestado antes de editar en [B03_2A_HOME](B03_2A_HOME.md). Referencia BitChord
local fija b690dc1, solo lectura. Se adaptan gutters, cabeceras, captions,
espaciado, bordes y escala del icono; se conecta el indicador de reproducción
existente a tarjetas normales. Datos/colas y funciones locales conservados.
Dos casos nuevos para Actions sobre geometría/acciones y texto 1,5x en ambos
temas; dos revisiones del mismo agente, git diff --check local. Sin Gradle/Android
locales. B01–B06 del nuevo SHA pendientes: no se cierra B03.2 ni paridad visual
física. Esperar resultado y autorización antes de B03.3 u otro bloque.

### B03.1-a — estado por entrada, Atrás y restauración

**Reparación run119 autorizada.** Run119/43527135590 del SHA dc9a4b13 falló:
B01/B02/B05 success, B03/B06 failure, B04 skipped. XML y logs confirman por
variante 306/308 pruebas aprobadas, dos fallidas por igualdad de identidad de
SnapshotStateList en AppNavigatorTest (no por compilación). Las cinco pruebas
Compose nuevas aprobaron por variante. Kover se omitió tras fallar las suites;
F5 rechazó la falta de cobertura. Se corrigen las dos aserciones usando toList()
en ambos lados, sin cambiar producción ni rebajar CI. Tres archivos ya incluidos
en los ocho del bloque (máximo acumulado 30); dos revisiones del mismo agente y
git diff --check locales, sin ejecutar Gradle/Android.

**Resultado posterior confirmado:** b11427277569b591acb8ab6f476ee1a6171098d3,
run120/43537277492 SUCCESS en las seis fases B01–B06. Informes originales y XML
verificados: 308/308 pruebas Debug y 308/308 Release, cero fallos/errores/omitidas;
13/13 AppNavigatorTest y 5/5 NavigationEntryStateTest por variante. XML Kover
Debug/Release presentes; cobertura cambiada 89,46 % (3718/4156), puerta F5 success.
Resuelto el fallo CI de B03.1-a. El cierre documental posterior conserva ocho
rutas acumuladas y no equivale a una nueva compilación. No se acredita validación
física/visual completa ni se inicia otro bloque; esperar nueva autorización.

Autorización «Continúa», base 49ba012a; cuenta/identidad verificada, checkout
limpio y remoto coincidente. Run118 success en B01–B06 confirma CI18 antes de
este bloque. Ocho rutas de un máximo 30; manifiesto/referencia/revisiones en
[B03_1A_ENTRY_STATE](B03_1A_ENTRY_STATE.md).

Se adapta de BitChord b690dc1 la identidad por entrada y limpieza de estados
cerrados. Cada visita nueva tiene clave propia; cerrar/cambiar tab retira solo
las páginas que salen de la pila y mantiene estado de tabs. El snapshot v2
conserva IDs/contador, acepta v1 con sus claves antiguas y rechaza datos inválidos.
Los overlays conservan la entrada subyacente. Nueve pruebas nuevas para GitHub
(cuatro modelo/cinco Compose) y suites anteriores intactas. Dos revisiones
estáticas del mismo agente; sin Gradle/Android locales. Nuevo B01–B06 pendiente.
No se cierra paridad física/visual de B03 ni se inician B09 u otros bloques.

### CI18 — reparación conjunta de B02 y B06 tras run117

Autorización explícita para corregir ambas causas y comprobar B01–B06 después
del push. Base09083362; cinco archivos de un máximo 30. Manifiesto, procedencia
de evidencia, accesos intentados y revisiones en
[CI18_HAPTICS_AND_REPORT_FAILURE](../../qa/CI18_HAPTICS_AND_REPORT_FAILURE.md).

B02 comprueba todos los tipos de golpe requeridos mediante constantes SDK
explícitas en areAllPrimitivesSupported, eliminando el IntArray que Lint no
aceptaba. B06 conserva la validación estricta F4 y registra su fallo como
NOT_GENERATED cuando B04 está skipped o faltan datos; consolida los diagnósticos
con resultado global FAIL y conserva fases originales. No crea p2-apk-trace ni
certifica fuentes ausentes. Retira únicamente salidas F4 derivadas antiguas.

Reproducción local del error original confirmada. Siete regresiones nuevas,
222 pruebas Python aprobadas; dos revisiones del mismo agente. Sin Gradle ni
Android locales, cambios de firma, SDK, ABI o supresiones de Lint. Resultado
posterior confirmado: run118/43520405511 SUCCESS para 49ba012a, las seis fases
B01–B06 success y tres artefactos finales conservados. No se reutiliza ese verde
para la nueva entrega B03.1-a.

### B02.3-b3 — ritmos por motor y feedback de pulsación larga

**Reparación autorizada tras run116.** El usuario pidió revisar/corregir el error
y aportó Aurelis-Informes-B01-B06-116.zip. Base de reparación:
9dcfb964eaf082acee9ae6ee1305308bc0e473c8. ZIP íntegro, SHA-256 verificado:
317d4de9e1582ef82e26b9714d954e70ed66be6dbbd5cb65eaf2cc9e9d0f953e.
Los XML Debug/Release registran un error WrongConstant por variante en
Haptics.kt:71, argumento de Composition.addPrimitive; también229 warnings por
variante, fuera del alcance de esta reparación. B01, B03 y B05 aprobaron; B02
falló, B04 se omitió y B06 falló por la puerta global. Es un diagnóstico Lint,
no un error de compilación Kotlin ni una prueba de fallo de reproducción.
El ZIP registra299/299 pruebas aprobadas en Debug y299/299 en Release, cero
errores/skips; pertenecen al run116, no a la reparación nueva.

La propiedad Int del enum conservaba el valor correcto en runtime, pero Lint
no la reconocía como uno de los valores del @IntDef del SDK. Se expresa la
selección mediante when exhaustivo y constantes PRIMITIVE_TICK, LOW_TICK y CLICK
directamente en addPrimitive. Se conservan comprobaciones de soporte, escalas,
pausas, cache, fallback, preferencias y gesto. La suite existente de23 pruebas
cubre ya las tres primitivas y las rutas de motor; se conserva sin añadir casos
que solo repitan el cambio sintáctico. No se suprime WrongConstant ni se cambia
Lint, el umbral de cobertura, workflows, firma, SDK o ABI.

Dos rutas: Haptics.kt y este tracker, ambas dentro de las seis de B02.3-b3.
Revisión1 del mismo agente: diagnóstico de ambos XML y constantes del @IntDef
en anotaciones del SDK. Revisión2: equivalencia de las tres ramas con el enum,
datos de ritmos/pruebas/avisos intactos y presupuesto. Solo comprobaciones
estáticas/diff locales; sin Gradle ni pruebas Android locales. Resultado nuevo
de Lint/Android pendiente de GitHub; tras publicar esta reparación se espera
el resultado comunicado por el usuario antes de otro bloque.

**Autorización/base.** «Autorización concedida» para la siguiente entrega,
incluidos commit/push a feature/bitchord-local-migration, mismo PR1.
Base c3844686db58283ee70325d8bc4f44393e48599f, estado limpio y remoto coincidente
tras fetch. Cuenta/identidad ed0668822 y correo ed0668822@gmail.com verificado.
[Run115](https://github.com/ed0668822/Aurelis-Music/actions/runs/43502658583)
completed/success en esa base: las seis fases B01–B06 aprobaron. No trasladar ese
resultado al código nuevo de esta entrega.

**Manifiesto difícil: seis archivos distintos en toda la entrega.** Informe
detallado en este tracker y registro legal; sin informe adicional fuera del límite.

| Archivo | Cambio |
|---|---|
| app/src/main/java/com/music/aurelis/ui/haptics/Haptics.kt | Ritmos BitChord, tres rutas de motor y caché de efectos. |
| app/src/main/java/com/music/aurelis/ui/components/HeldContextMenu.kt | Una solicitud Select al abrir acciones por pulsación larga. |
| app/src/test/java/com/music/aurelis/ui/theme/SharedGesturesTest.kt | Cinco regresiones de motor/gesto. |
| NOTICE | Archivo/ref/hash/condiciones y cambios de la adaptación. |
| docs/migration/beta-local/LICENSE_AUDIT.md | Procedencia y conservación de avisos. |
| docs/migration/beta-local/PROGRESS_TRACKER.md | Informe, revisiones, pruebas y pendientes. |

**Adaptación concreta.** BitChord b690dc146009d4815825fa4a20815ef2e4ee8898,
sharedUi/src/androidMain/kotlin/com/music/bitchord/ui/haptics/Haptics.android.kt,
SHA-256: 074cb0c645dac155555bbd57378b0ed027b8f025771d145daaa2999c2bc683ab.
Se conservan sus ritmos Tick/Tap/Select/ToggleOn/ToggleOff/SkipNext/SkipPrevious/
Resume/Pause/Expand y las reglas de fallback. Los avisos GPLv3 del origen se
registran sin relabelar el material como propio u or-later. El primer comentario
Aurelis se delimita a cambios locales. LICENSE y avisos legales anteriores intactos.

Con primitivas soportadas se usan composición, fuerza y pausas de esos ritmos.
La comprobación se hace por las primitivas requeridas para cada ritmo, incluyendo
LOW_TICK; no basta la versión Android ni soportar Tick/Click para dar Resume por
compatible. Sin soporte completo se usa la onda con amplitudes si el motor las
admite. En un motor básico se conservan extremos del ritmo, pausas mínimas20ms y
pulsos18–40ms, como el fallback upstream; sin amplitudes ficticias. minSdk33
existente permite las APIs utilizadas sin importar compatibilidad antigua.

Los efectos inmutables se cachean por handle, con un máximo del enum de 10
patrones. App/Android y presencia de motor se comprueban antes de emitir incluso
si el efecto ya estaba cacheado. Se conserva USAGE_TOUCH; no se crea singleton
con Activity, observador global, dependencia, permiso o cambio de ABI.
El gesto compartido, ya conectado a SongRow/HomeCarousels, solicita Select una
vez después de su callback largo. Compose1.7.x no emite otro pulso desde ese
callback, según la lectura estática previa. Un toque corto conserva reproducción;
la acción larga táctil o semántica abre el mismo menú sin invocarla. Ancla/foco,
acciones de cola y modos deshabilitados existentes se conservan.

**Pruebas preparadas para GitHub.** SharedGesturesTest conserva18 casos y añade
cinco,23 en total: primitivas/fuerzas/pausas Select y Resume; soporte parcial sin
LOW_TICK elige fallback por ritmo; motor con amplitud conserva tres golpes de
SkipNext y fuerzas distintas; motor básico emite los10 ritmos sin repetición y
con duración de onda<=80ms; pantalla SongRow real comprueba toque, largo táctil,
acción semántica, apertura y supresión con cada preferencia apagada. Las pruebas
de primitivas/ondas usan ShadowVibrator4.14.1 y no acreditan fuerza percibida en
hardware. Los atributos registrados sobreviven al pulso; no se depende de que
isVibrating siga activo al terminar animaciones. Se conservan aserciones de foco,
las cuatro pruebas previas de preferencias y gates sin modificar.

**Dos revisiones del mismo agente.** Primera: comparación del origen fijo,
licencia, ritmos, capacidades por patrón, límites de fallback y política viva.
Segunda: consumidores/acciones, API Android/ShadowVibrator, persistencia de
preferencias, seis rutas y conservación de firma/ABI/reproducción/workflows.
Solo comprobaciones estáticas/diff locales; no Gradle, APK o pruebas Android
locales ni revisión independiente. Compilación/pruebas nuevas pendientes de
GitHub; el push de app/** y NOTICE activa B01–B06 en la misma rama.

**Pendientes/puerta.** No declarar paridad háptica física, TalkBack ni B02 cerrado.
Captura/elevación/retorno visual del elemento siguen pendientes de integración
B03–B05; CI12.3/comentarios CI16 abiertos, B06/B07 sin cierre retroactivo y B09
sin iniciar. Tras publicar, esperar resultado del usuario y autorización antes
de la siguiente entrega.

### B02.3-b2 — preferencia propia de respuesta háptica

**Autorización/base.** El usuario pidió «Continúa». Aplicación, commit y push en
feature/bitchord-local-migration, mismo PR1. Base1714325ed9dcade11785dc59fae7cf827f9964c0;
checkout limpio y remoto coincidente tras fetch. Cuenta/identidad local ed0668822
y ed0668822@gmail.com verificado por API. Puerta anterior:
[run114](https://github.com/ed0668822/Aurelis-Music/actions/runs/43483218982)
completed/success para ese SHA, con las seis fases B01–B06 success. El resultado
previo no acredita el código nuevo de esta entrega.

**Manifiesto difícil: seis archivos distintos en toda la entrega.** Este tracker
contiene el informe detallado para mantener el presupuesto, sin informe separado.

| Archivo | Cambio |
|---|---|
| app/src/main/java/com/music/aurelis/data/SettingsRepository.kt | Preferencia persistente y actualización tras guardar. |
| app/src/main/java/com/music/aurelis/ui/screens/SettingsScreen.kt | Fila real Respuesta háptica y error al guardar. |
| app/src/main/java/com/music/aurelis/ui/haptics/Haptics.kt | Política común app/Android para patrones y feedback Compose. |
| app/src/main/java/com/music/aurelis/ui/theme/AurelisTheme.kt | Proveedor del host real para feedback del framework. |
| app/src/test/java/com/music/aurelis/ui/SettingsIntegrationTest.kt | Cuatro regresiones funcionales nuevas. |
| docs/migration/beta-local/PROGRESS_TRACKER.md | Informe, evidencia, revisiones y pendientes. |

**Comportamiento.** Respuesta háptica se incorpora en Apariencia con el mismo
SettingsSubRow de rol Switch/objetivo48dp y un único propietario de la acción.
La preferencia haptic_feedback queda en el almacenamiento de ajustes del único
perfil local. Ausente: true, conservando el comportamiento anterior. Un tipo
desconocido usa el fallback sin sobrescribir los bytes originales. El setter
comprueba escritura permitida y commit() antes de cambiar el StateFlow: un error
conserva el valor y la pantalla muestra el fallo. El modo de solo lectura no
expone el control y también impide su modificación desde el repositorio.

La política permite feedback solo con la preferencia Aurelis activada Y el ajuste
de Android activado. Apagar Aurelis no modifica Android; activar Aurelis no
anula un ajuste de Android apagado. Los handles existentes leen la política al
usar un control, sin recrear pantalla/servicio. Se conserva el chequeo de motor
y USAGE_TOUCH para patrones propios. El proveedor LocalHapticFeedback del tema
filtra las llamadas del framework y delega una sola vez al proveedor capturado
fuera de él; no genera un pulso adicional ni se llama recursivamente.

**Referencia y límites.** Continúa la política Android revisada en BitChord
b690dc146009d4815825fa4a20815ef2e4ee8898,
sharedUi/src/androidMain/kotlin/com/music/bitchord/ui/haptics/Haptics.android.kt.
La preferencia propia y el wrapper se implementan sobre los repositorios/tema de
Aurelis; no se trasplanta código ni se elimina ninguna atribución. Se conservan
patrones, formas, iconos y animaciones de los controles. Lectura estática de la
dependencia Compose1.7.x: su combinedClickable invoca el callback largo, sin
emitir por sí mismo un pulso. Este filtro no añade ese pulso; la integración
háptica explícita del menú y primitivas/adaptación de motor siguen pendientes.
No declarar B02 cerrado ni paridad TalkBack/motor físico por este bloque.

**Pruebas preparadas para GitHub.** SettingsIntegrationTest conserva sus cuatro
casos anteriores y añade cuatro, ocho en total: pantalla real → repositorio →
reapertura y política de los mismos handles propio/Compose; preferencia
ausente/malformada sin pérdida; modo de solo lectura; fallo de commit con valor
conservado/error visible. La política se prueba con ambos ajustes activos, app
apagada y Android apagado, contando una sola delegación del tipo solicitado.
ShadowVibrator registra atributos sin depender de la duración del pulso. Cada
caso con inyección de estado restaura preferencias de Android/hardware/campos
inyectados en finally. La suite SharedGesturesTest del bloque anterior permanece
intacta; no se retiran aserciones de foco ni controles de calidad.

**Dos revisiones del mismo agente.** Primera: conexiones reales, prioridad de
ajustes, ciclo de vida, persistencia antes de publicar estado y modo de solo
lectura. Segunda: API Compose/Android, casos de error y aislamiento de pruebas,
enlaces, cabeceras, seis rutas y preservación de licencias/firma/ABI/workflows.
No son revisiones independientes. Solo comprobaciones estáticas/diff locales;
sin Gradle, APK o pruebas Android locales. El push de app/** activa el pipeline
unificado; el resultado posterior run115/43502658583 fue SUCCESS en las seis fases.
La evidencia física
y el resultado Android nuevo no se deducen de la revisión estática.

**Puerta siguiente.** Publicar esta entrega autorizada y esperar resultado del
usuario y autorización para la siguiente. Capturas/retorno visual de HeldItem
siguen pendientes de integración B03–B05; CI12.3 y comentarios CI16 abiertos;
B06/B07 no se cierran retrospectivamente y B09 permanece sin iniciar.

### B02.3-b1 — política háptica y una acción accesible por control

El usuario autorizó reparar ZIP112 y continuar. Base de preparación b02607d6;
la entrega posterior se verifica y publica por separado de la reparación.
Puerta previa confirmada: run113/43479948669 completed/success en b02607d6,
las seis fases B01–B06 success, incluidos tests Debug/Release y cobertura80%.
Ese resultado certifica los gates de la reparación, no la nueva entrega háptica.
Seis rutas: Haptics, IosSwitch, SettingsComponents, SharedGesturesTest, informe
[B02_3B_HAPTIC_ACTIONS](B02_3B_HAPTIC_ACTIONS.md) y este tracker.
Haptics respeta el ajuste de Android y motor disponible, usa applicationContext
y atributos USAGE_TOUCH. Switch/fila emiten feedback desde su acción habilitada,
sin vibraciones por cambios externos o dibujo decorativo. Fila con estado
deshabilitado, rol/valor únicos y objetivo48dp. Diseño visual conservado.

Cuatro regresiones nuevas para GitHub,18 casos en la suite, incluidos los14
anteriores y las aserciones de foco. Dos revisiones estáticas del mismo agente;
sin Gradle ni pruebas Android locales. Gates, firma, ABI y atribuciones intactos.
Resultado posterior run114/43483218982 SUCCESS en las seis fases B01–B06;
primitivas/paridad física y TalkBack siguen abiertos. La preferencia propia se
aborda en B02.3-b2. B02.3-a conserva pendiente de captura/retorno visual
en B03–B05. Tras publicar esta entrega, esperar resultado del usuario y nueva
autorización; B09 no se inicia.

### B02.3-a — primera entrega de anclaje y devolución del foco

Reparación autorizada tras ZIP112: run43470182513 compiló Debug y pasó Lint/
emulador; B03 tuvo283/286 casos aprobados por variante y tres aserciones de foco
fallidas en ambas. combinedClickable impide el foco en modo Touch por defecto.
Se explicita canFocus en el destino existente; se conservan las aserciones y se
comprueba que no cambie a modo teclado. Cobertura no medida por pruebas fallidas,
sin modificar gates. Cuatro rutas, subconjunto de las seis de la entrega original;
dos revisiones estáticas del mismo agente. Detalle y digest del ZIP en B02_3A.
Resultado posterior confirmado: run113/43479948669 SUCCESS, fases B01–B06
aprobadas. Siguiente entrega también autorizada, manteniendo reparación y
desarrollo separados. Sin Gradle/pruebas Android locales.

El usuario priorizó empezar B02–B05. Base ed977183; rama/remoto sincronizados,
cuenta ed0668822 e identidad local con correo verificado. Autorizada aplicación,
commit y push de esta entrega al mismo PR1. Presupuesto difícil: seis rutas,
incluidos tres archivos de producción, SharedGesturesTest y dos documentos.
Manifiesto y revisión en [B02_3A_ANCHOR_FOCUS](B02_3A_ANCHOR_FOCUS.md).

El gesto compartido captura el origen, el menú lo consume por host y se coloca
según ventana/tamaño/LTR/RTL. Cierre mantiene callbacks; devuelve foco tras retirar
el popup si el origen sigue montado. Filas retiradas invalidan el ancla; el host
cancela su efecto al desaparecer. Aperturas sin origen conservan el centro.
No cambia la arquitectura de reproducción, firmas, ABI ni workflow.

Dos revisiones del mismo agente y comprobaciones estáticas de API/diff/presupuesto.
Regresiones geométricas, ciclo de vida y Compose preparadas para GitHub; sin
Gradle ni pruebas Android locales. El push de app/** activa B01–B06, cuyo resultado
nuevo queda pendiente. No se acredita fidelidad visual ni hardware por este cambio.
B02.3-a sigue parcial: captura/retorno visual del elemento pendientes; después
B02.3-b y B03–B05 por entregas autorizadas. CI12.3/comentarios CI16 siguen abiertos;
B09 sin iniciar. Esperar resultado comunicado por el usuario y nueva autorización.

### CI16 — entrega documental autorizada de la revisión de las tres carpetas

Autorización inicial para sincronizar por avance rápido y preparar la revisión
con máximo6 archivos. La autorización posterior para continuar incluye commit y
push de sus tres documentos a feature/bitchord-local-migration, en el mismo PR1.
Sincronización c1bc7359 → 780fe6e3: 52 commits remotos conservados, sin conflictos.
El run109/43339311481 de ese SHA tiene B01–B06 success según API; las anteriores
inconsistencias de gates CI15 son históricas y no describen el flujo CI14 actual.

Inventario revisado: ui/audit5, ui/components107, ui/haptics1;113 fuentes sin cambios
frente a c5ff687d. LocalAppBackdrop.kt no tiene declaraciones propias; la real
está en LiquidGlass.kt. Hay5 comentarios antiguos no se compila/PolyDev, no4.
No se modifican fuentes ni se convierten estados de auditoría antiguos en CI.

46 fixtures existentes de inventario/clasificación/integridad pasaron localmente;
no se ejecutaron Gradle/APK/pruebas Android. Preparados
[informe CI16](../../qa/CI16_THREE_FOLDERS_REVIEW.md) y
[evidencia estructurada](../../qa/CI16_THREE_FOLDERS_REVIEW.json), más este tracker:
3 rutas de un máximo6, dos revisiones del mismo agente.

La descarga de red estaba bloqueada, pero el usuario aportó el ZIP del run109.
Se comprobó su digest contra GitHub e integridad ZIP, procedencia SHA y226 filas
únicas con hashes de fuente/input/tareas/DEX/R8:224 COMPILADO_EN_APK y2
SIN_CLASES_ESPERADAS (LocalAppBackdrop, ambas variantes), cero errores atribuidos.
La declaración real en LiquidGlass conserva evidencia propia. Esto no cierra
CI12.3 funcional ni acredita hardware; los cinco comentarios siguen sin modificar.

El ZIP del run109 también informa que Release1.9.103 fue construido y firmado con
la clave configurada, pero esa entrega esperaba solo2 artefactos y no lo publicó.
El binario Release no está en el ZIP de informes. El ajuste posterior CI17,
bb417f39, habilitó el candidato Release firmado de pruebas; ver
[CI17](../../qa/CI17_SIGNED_RELEASE_CANDIDATE.md). El ZIP109 no certifica su resultado.

Base de publicación documental eeb1c1bc: los113 hashes de fuentes siguen iguales
a la evidencia del run109. Reconfirmados digest/integridad del ZIP,226 pares,
clasificaciones, referencias y presupuesto de3 rutas. Dos revisiones finales del
mismo agente: evidencia/procedencia y alcance/consistencia. Las46 pruebas son la
ejecución inicial registrada; no se repiten por esta publicación documental.
Sin Gradle ni pruebas Android locales, cambios de app/CI/firma/ABI ni cierre de
B06/B07/B09. Los cinco comentarios ambiguos y CI12.3 siguen pendientes.
Tras subir este bloque se espera el resultado comunicado por el usuario y su
autorización para el siguiente; el push puede activar el CI existente.

### Corrección CI15 antes del siguiente bloque

Autorización «Soluciona eso antes del siguiente bloque». Base final dcc8aafd:
se preservan el documento CI14 y la fase F1 llegada concurrentemente. La resolución
propia se guardó/verificó fuera del checkout antes de reanudar sobre ese avance.
Se integra main 1d0d26a8 en la
rama de trabajo y se resuelve manualmente el add/add de B01 con su contrato
reutilizable actual; main/PR1 no se fusionan hacia main ni se reescribe historia.
AGENTS.md/SECURITY.md de main se conservan intactos. Único cambio semántico de
árbol fuera de documentación: ninguno; el YAML B01 conserva su estructura y solo
aclara comentarios, que además hacen coincidir el push con el filtro b0*.yml.

Manifiesto de cinco rutas y presupuesto máximo6 en
[CI15_PR_CONFLICT](../../qa/CI15_PR_CONFLICT.md). Dos revisiones del mismo agente;
139 fixtures Python y comprobaciones YAML/diff correctas; sin Gradle/Android
locales. Tras publicar, verificar PR fusionable y pipeline iniciado para el SHA
exacto. Resultado Android nuevo pendiente, sin avanzar al siguiente bloque ni
implementar nuevas fases CI14 dentro de esta reparación. La restricción remota de
Release a main/tags sigue intacta; su contradicción con gates B04/B06 queda
identificada en CI15 y no se presenta como pipeline completo aprobado.

Autorización: selección de todas las31 adaptaciones y «Implement the proposed plan».
Base `f6309d2961384ca9a07197b51eb7c016d11f0b8d`; checkout previamente en c5ff687d,
actualizado mediante avance rápido sin conflictos. Se conservaron el commit09b848df
de regresiones PCM y el plan documental f6309d29; la rama work permanece intacta.
Estado inicial limpio, sin cambios pendientes ni commits locales exclusivos.
Cuenta autenticada ed0668822 y correo ed0668822@gmail.com verificado por API;
identidad Git local coincidente. PR1 abierto, mismo título y rama.

Manifiesto previsto: PLAN_CONTINUATION, MASTER_PLAN, IMPLEMENTATION_BLOCKS,
PROGRESS_TRACKER, DECISIONS y FILE_MAPPING, todos .md en este directorio.
Presupuesto conservador6; exclusivamente documentación. PLAN_30_FILES se conserva
sin modificar. La matriz de selección enlaza las31 capacidades con sus entregas;
no añade por implicación propuestas excluidas/no respondidas de anexos anteriores.

La implementación funcional y la revisión de las tres carpetas aún no se ejecutan
en esta entrega. No se ejecuta Gradle/Android localmente ni se modifica código de
la app, pruebas, workflows, firma, ABI o modelos. Los resultados anteriores de CI
no se atribuyen a esta documentación como una compilación nueva.

### Revisión1 — estructura, referencias y presupuesto

Primera pasada del mismo agente: manifiesto comprobado contra diff más archivos
nuevos, exactamente6 rutas Markdown; selecciones01–31 completas, únicas y en
orden; enlaces locales del nuevo plan resueltos; SHAs base/referencias fijos
comprobados. PLAN_30_FILES sin cambios. `git diff --check` correcto.

### Revisión2 — alcance, arquitectura y regresiones documentales

Segunda pasada del mismo agente, no revisión humana independiente: comprobadas
las75 filas de hoja de ruta (varias contienen hijos independientes), presupuestos
6/35 y distinción entre entregas. Comprobadas puerta de espera, restricción
GitHub, sesión/servicio únicos, SQLite, modos1/2/3 e interop1, límites físicos,
proveedores condicionados y B09 sin iniciar. Se corrigió la instrucción antigua
de compilación local de IMPLEMENTATION_BLOCKS y se identificaron como históricos
los estados antiguos del master/progreso, sin borrar sus registros.

Resultado de las comprobaciones documentales: correcto; sin pruebas Android,
Gradle ni compilación nuevos en esta entrega. No se crearon tests de código para
estos cambios exclusivamente documentales. Tras publicar, avisar y esperar el
resultado comunicado por el usuario antes de revisar las tres carpetas/CI12.3.

## Registro histórico anterior a la continuación

Base 01c1ba511b9eb8ecb3eb40c98afb46d8d17a017a; rama existente; autorización recibida
para ejecutar plan por unidades. B01 completado; B02 implementado y comprobado localmente. Ver últimas entradas y evidencias.
Los bloques anteriores siguen en ../PROGRESS.md sin ser reiniciados o sobrescritos.

| Bloque | Dependencias | Complejidad | Estado nuevo plan |
|---|---|---|---|
| B00 | Aprobación recibida | Media | Completado y publicado |
| B01 | B00 | Alta | B01.1/.2/.3 implementados y publicados |
| B02 | B01 | Alta | B02.1/.2/.3 comprobados localmente; hardware/CI pendientes |
| B03 | B02 | Media-alta | B03.1/.2/.3 implementados y verificados localmente; GPU/CI entrega pendientes |
| B04 | B03 | Alta | Pendiente de autorización/previas |
| B05 | B04 | Alta | Pendiente de autorización/previas |
| B06 | B05 | Muy alta | Autorizado; B06.1 implementado pendiente CI/Android; .2/.3/.4 pendientes |
| B07 | B06 | Alta | B07.1 CI145 SUCCESS; núcleo B07.2 + CI10 CI147 SUCCESS; resto abierto |
| B08 | B07 | Media-alta | B08.1/.2/.3 implementados por autorización; CI y Android pendientes |
| B09 | B06 y B08 | Muy alta | Pendiente de autorización/previas |
| B10 | B09 | Muy alta | Pendiente de autorización/previas |
| B11 | B10 | Alta | Pendiente de autorización/previas |
| B12 | B11 | Alta | Pendiente de autorización/previas |
| B13 | B12 y todos los componentes incluidos resueltos | Alta | Pendiente de autorización/previas |

## B00 — auditoría y dependencias

- B00.1: rutas/propietarios/stubs comprobados; fuente fija e inventario completo.
- B00.2: comparación de175 módulos resueltos con inventario previo, sin cambios de
  versión; licencias reutilizadas como metadatos, no certificación de binarios.
- B00.3: ocho documentos guardados, validación real debug/release y suites con
  --rerun; evidencia y entrega se registran al finalizar las dos revisiones.
- Revisión1: arquitectura/correspondencias. Se corrigieron rutas del plan: LocalAutoMix
  está en playback; PlayerLyrics es nuevo en destino; LrcRed ya existe. No cambios
  a código de app ni archivos de firma/identidad/licencia/workflow.
- Revisión2: comprobaciones funcionales de base, inventarios/documentos/ZIP/ELF y
  workflow. Resultado/commit/PR al cierre, sin declarar fidelidad o hardware probados.
- Bloqueo descubierto: ORT1.19.2 aporta libonnxruntime.so y libonnxruntime4j_jni.so
  con LOAD4KB. ZIP16KB correcto no resuelve esta incompatibilidad ELF. Tratar en
  B09/B10 antes de cerrar B13; no cambiar binarios en B00.
- Pendientes: modelos/fuentes de binarios, condiciones de APIs, CSV, referencia
  visual y dispositivos. Ver LICENSE_AUDIT/TEST_STRATEGY/B00_AUDIT.

## Registro por unidad futura

Guardar base, archivos creados/modificados, pruebas realmente ejecutadas/cacheadas,
revisión1/revisión2, build debug/release, CI por SHA, paridad/limitaciones, commit/PR
reales y próximo subbloque autorizable. No repetir árbol completo ni auditorías
ya acreditadas; leer solo filas/archivos de la unidad al reanudar.


- Cierre local: revisión1 y2 completadas en pasadas separadas del mismo agente.
  180 correspondencias verificadas;14 fichas x16campos y48 subunidades.
  Inventario1396 y dependencias175; ambos builds y252 tests ejecutados correctos.
  Firmas/ZIP/actionlint correctos; hallazgo ELF16KB registrado sin ocultarlo.
- B00_AUDIT y B00_EVIDENCE contienen evidencia/limitaciones. La entrega no afirma
  B01, funciones futuras, paridad ni hardware aprobados. SHA/PR al publicar.


## Entrega B00 confirmada

- Commit: `b89a6b5089ac20562253697d565af934b724976e`, autor/committer ed0668822 <ed0668822@gmail.com>.
- Push confirmado desde01c1ba5; rama remota y refs/pull/1/head coinciden con SHA.
- PR: https://github.com/ed0668822/Aurelis-Music/pull/1, OPEN, título conservado
  «Aurelis Music Beta UI and components». No merge ni cambio de rama.
- GitHub Actions web muestra run119 histórico exitoso
  (https://github.com/ed0668822/Aurelis-Music/actions/runs/42101996135).
  No acredita el SHA nuevo: CI de B00/checkpoint pendiente de verificación.
  No se transfiere el verde histórico a esta entrega.
- Solo documentación, inventarios y evidencia. No modifica código/tests de app,
  Gradle/CMake, manifest, workflows, LICENSE/NOTICE ni secretos/configuración de firma.
- Nuevo checkpoint documental registra este SHA real tras publicar; el baseline
  de B00_EVIDENCE sigue siendo01c1ba5, sin cambios funcionales respecto a entrega.
- Siguiente unidad: B01.1, requiere autorización. Perfil múltiple, Dolby y demás
  cambios previstos NO se han implementado todavía.


## B01.1 — en implementación y validación

- Autorización expresa recibida; base b2bf434. Rama/PR originales.
- Perfil único; nombre/foto y ID del ámbito activo conservados. Respaldo privado
  tipado/verificado y recibo atómico antes de consolidar; sin borrar legacy.
- Modificados ProfileRepository/Storage/Avatars y SettingsScreen. Application,
  MainActivity, PlaybackViewModel y ProfilePlaybackBinding retiran únicamente
  conexiones multiusuario. No se implementan B01.2/B01.3.
- Nuevos SingleProfileMigration, su suite de pruebas y B01_1_MIGRATION. Pruebas
  de perfiles/playback/UI adaptadas a la decisión de perfil único; las pruebas
  de ámbitos de almacenamiento legacy se mantienen para recuperación.
- Revisión1 completada: respaldo/atomicidad/paths/tipos/estado y preservación.
- Revisión2 en cierre:47 casos seleccionados correctos. Suite/debug/release/lint
  corriendo; no declarar hardware ni migración en dispositivo verificados.
- Registro legal: implementación propia, sin copiar componentes nuevos de BitChord;
  avisos existentes conservados, cabecera definitiva pendiente del CSV histórico.
- Commit/PR/CI se registran tras verificaciones y publicación. Siguiente unidad
  B01.2 MediaStore/metadatos, requiere autorización.


## B01.1 — cierre local

- Estado: implementado y revisado; publicación/CI se registran a continuación.
- Compilación: BUILD SUCCESSFUL in 5m 12s. Debug/release, R8 y lint correctos.
- Suite137 debug +137 release:0 fallos/errores/omitidos;47 seleccionados previos.
- Lint advertencias debug/release: 215/214;0 errores. Firmas y ZIP válidos;9 textos legales idénticos y12 .so ARM64.
- Revisiones1/2 completadas en pasadas separadas del mismo agente. Las pruebas
  adaptadas sustituyen requisitos multiusuario retirados por invariantes de perfil
  único, preservación de datos y reproducción; no se excluyen pruebas por fallar.
- No dispositivos físicos/SAF real; no importación/exportación UI ni B01.2/3.
  B01_1_MIGRATION y B01_1_EVIDENCE documentan formato, privacidad y limitaciones.


## B01.1 — entrega publicada

- Commit: `bfbc81fdaeee5d9cb2086f27ae156ce75ca5c58b`; autor/committer ed0668822 <ed0668822@gmail.com>,
  identidad solo local. Push confirmado desdeb2bf434 a la misma rama.
- PR1 abierto: https://github.com/ed0668822/Aurelis-Music/pull/1; título intacto
  «Aurelis Music Beta UI and components». No merge ni force push.
- Actions: el listado público tras push todavía mostraba el run121 anterior
  exitoso (42123993004); no acredita el nuevo SHA. CI del código/checkpoint
  pendiente; no se traslada un verde histórico a B01.1.
- Este checkpoint documental registra el SHA real después de publicar y no
  cambia código validado. APK construido desdeb2bf434 más los cambios de esta
  entrega; B01_1_EVIDENCE registra ambas referencias para no confundir metadatos.
- Estado: B01.1 completado localmente y publicado. B01.2 MediaStore/metadatos y
  B01.3 carpetas siguen sin implementar y requieren autorización.


## B01.2 — completado localmente

Autorizado junto con B01.3/B02. Base0de7f2a; MediaStore/metadatos implementados,
144 tests debug correctos y U completo (BUILD SUCCESSFUL in 1m 50s); lint0 errores.
Dos revisiones separadas del mismo agente; ver B01_2_METADATA/EVIDENCE.
Release/dispositivo pendientes del cierre de alcance. Siguiente B01.3 autorizado.


## B01.2 y B01.3 — cierre de B01

- B01.2 código: `de8c1afe5614814593865d268e4e72ffaadaf6ef` (de8c1af),144 casos debug correctos.
- B01.3:154 casos debug correctos; assemble/lint debug correctos.
- Dos revisiones en pasadas separadas del mismo agente. Alcance indexado por
  MediaStore explícito; SAF real/SD físico pendientes. No escáner ficticio.
- B01 completo localmente; B02.1/.2/.3 autorizados por «continúa hasta B02».
- Release completo se comprobará al cerrar B02; no iniciar B03.


## B02.1 — completado localmente

156 tests debug correctos, assemble/lint correctos. Fuente del sistema y Vazirmatn
con selector real, persistencia y OFL/NOTICE/bundle. Dos revisiones separadas del
mismo agente; fuente y texto íntegros con SHA de origen. ADB sin dispositivos.
B01.3 publicado en4c8d529; B02.2/.3 autorizados, B03 no iniciado.


## B02.2 — completado localmente

158 casos debug, assemble/lint correctos (U:1m55s), cuatro programas AGSL exactos
compilados en runtime nativo V, sin transformar shaders ni cambiar dependencias.
Fuente de página conectada, superficies compartidas, preferencias y fallback
por capacidad; guards de Canvas software. Dos revisiones del mismo agente en
pasadas separadas; evidencia/limitaciones en B02_2_RENDERING/EVIDENCE.
Hardware y paridad visual final pendientes; B02.3 autorizado, B03 no iniciado.


## B02.3 y cierre acumulado hasta B02

- Fuente/interruptor y componentes/gestos conectados; no mocks de favoritos,
  playlists ni cola. Diferencias de HeldItem/ancla/retorno documentadas en B02_3.
-14 casos seleccionados correctos; R:164 debug y164 release,0 fallos/errores/omitidos.
- Debug/release/R8/lint correctos; dos revisiones del mismo agente en pasadas
  separadas. Firmas/ZIP válidos,10 textos legales idénticos y fuente íntegra.
- B02_3_EVIDENCE registra APK/hashes/resultados y base+archivos probados. No hay
  dispositivo: paridad final/GPU/SAF real pendientes. ONNX4KB/modelos/licencias
  B00 siguen abiertos para B09/B13; no se modifica el motor nativo.
- B03 no iniciado; requiere autorización. Entrega misma rama/PR1, sin renombrar,
  merge ni force push; SHA de código en checkpoint de entrega. Actions pendiente
  del SHA publicado, no confundir con ejecuciones anteriores.


## Checkpoint de entrega hasta B02

Código final: `ccb75126b0b0361212e9c7369265e0f803812ee6`.
Todos los SHA reales, revisión/compilación, PR y límites en B02_CLOSEOUT.md.
Entrega conjunta de commits B02 a la misma rama/PR; CI del SHA de entrega pendiente
de consulta. B01.2/.3 ya publicados; no repetir esas tareas. B03 sin autorización.


## Complemento QA00/QA01 — regresión B02

El usuario reportó cierre al abrir en Redmi Note14Pro5G. La prueba nueva reproduce
Size is unspecified en onAttach/apply/lens; se corrigió el cálculo antes de medida
sin eliminar Liquid Glass.171 pruebas por variante y builds/lint correctos.
Ver [QA](../../qa/PROGRESS.md) y QA01_EVIDENCE/RED/GREEN para evidencia y límites.
Arranque físico pendiente: no trasladar el éxito automatizado al Redmi. Plan y
orden B00–B13 intactos; B03 no iniciado. QA02 necesita autorización independiente.


## B03 — reanudación del principal

Última instrucción «Mejor continúa con el plan ... pero si prueba todo lo que
puedas» se aplica al siguiente B03, sin iniciar QA02 ni B04. QA00/01 conserva
pruebas/CI de b8b8910 correctas, Android real pendiente.
B03.1: host local inicializado y píldora real conectados;14 casos dirigidos
correctos, dos revisiones del mismo agente. Ver B03_1_NAVIGATION. R al cierre.


B03.1 código: e79f32a. B03.2: secciones reales y reproducción por sección,
HeroShelf conectado, recientes en páginas de4 y playlists con IDs disponibles.
17 casos dirigidos correctos; dos revisiones separadas del mismo agente. Ver
B03_2_HOME; release/APK/lint y suite completa en R al cierre. B03.3 autorizado.


## Cierre local B03

B03.1 e79f32a; B03.2 6b4bded; B03.3 recuperación/capturas verificadas con R.
BUILD SUCCESSFUL in 5m 6s;179 pruebas debug +179 release,0 fallos/errores/omitidos.
Lint:224/223 advertencias,0 errores. Ambos APK firmados/ZIP correctos,
10 textos legales íntegros y12 .so ARM64. Dos revisiones del mismo agente en
pasadas separadas. B03_1/2/3 y B03_EVIDENCE registran archivos/resultados/límites.

Capturas Robolectric en screenshots/b03; no Android real ni paridad GPU medida.
Fuente local, estados, colas y navegación conectados, sin contenido de ejemplo
en producción. Plan maestro/bloques, QA01, identidad, ABI y workflow intactos.
Explore queda expresamente para B04, no implementado aquí. No iniciar B04 ni QA02.


Entrega B03: código final `2e91a09f8c8c4ab621a54ffe4992dd3280ba5eaa`, commits por unidad y resultados
en B03_CLOSEOUT.md/B03_EVIDENCE.json. Misma rama/PR1 sin renombrar ni merge.
CI del SHA publicado pendiente de consulta; no reutilizar QA127. B04 no iniciado,
requiere autorización. QA02 permanece sin iniciar; Android real pendiente.


## B04 — entrega incremental; autorización B04–B05

B04.1: Explore/géneros reales y Sin clasificar conectados, estados de carga/error/
permisos/vacío; B04.2: búsqueda local de cinco tipos e historial persistente;
B04.3: directorios/restauración/lista-cuadrícula y selección múltiple de colecciones.
Cambios, archivos, referencias y límites en B04_EXPLORE_SEARCH_LIBRARY.md.
Dos revisiones estáticas del mismo agente. Seis pruebas nuevas y cinco rutas de
restauración adicionales preparadas: ejecución PENDIENTE en GitHub, no aprobadas.
No compilación local por instrucción del usuario; no Android real/Logcat.
Sin cambios de Liquid Glass/QA01, firmas, ABI, Gradle, licencias ni plan principal.

Estado: B04 implementado, validación en GitHub y paridad visual en dispositivo
pendientes. Search incluye filtros explícitos por tipo. No declarar B04 terminado
solo por publicar.
B05 autorizado por «Continúa con B04 - B05», pendiente de la puerta de CI y cierre
de B04. Mantener indicación del usuario: publicar, avisar y esperar resultado de
GitHub antes de continuar; no pedir de nuevo autorización B05. Misma rama/PR1.


## B05 — continuación autorizada tras entrega B04

«Autorización concedida» permite iniciar esta entrega, sin dar por aprobado CI de
B04. B05.1: calidad tipada real de archivo/pista, MiniPlayer cápsula/etiqueta;
B05.2: entrada/cierre/gestos con cancelación segura, panel/dock/morph pendientes;
B05.3: layout horizontal existente conectado a controles compartidos y fondo que
respeta efectos reducidos. Rotación real y fidelidad visual pendientes.
Ver B05_PLAYER.md/.json para archivos, revisiones, referencias, pruebas y límites.
Seis pruebas nuevas preparadas, NO EJECUTADAS. Compilación/lint exclusivamente en
GitHub, pendientes. Sin Android físico/sonido/Logcat verificados. Dos revisiones
estáticas del mismo agente. Firma/ABI/servicio/QA01/GLASS01/plan preservados.
Misma rama/PR1; publicar/avisar y esperar resultado del usuario. No B06 ni QA02.
No declarar B05 cerrado: siguiente trabajo será completar B05.2 y validaciones.


## B05.2 — panel progresivo / dock, continuación autorizada

Base a5e06bc. PlayerSheetMotion existente conectado al host activo, sin segundo
motor; arrastre/cancelación/velocidad, dock medido del mismo track y carátula
superpuesta durante movimiento portrait. Fondo y MiniPlayer conservados debajo
con semántica aislada. Sin medidas/animación reducida/landscape: slide normal.
Detalles, archivos, revisiones y límites en B05_2_PANEL_DOCK.md/.json.
Siete pruebas nuevas y regresión de swipe actualizada: NO EJECUTADAS aquí.
Dos revisiones estáticas separadas del mismo agente; API inspeccionada con javap.
Compilación/lint/pruebas solo en GitHub, pendientes de resultado del usuario.
No Android físico, capturas GPU, Logcat ni sonido verificados. Servicio, calidad,
B04, QA01, GLASS01, firmas/ABI/workflow/licencias íntegras y plan principal intactos.
No cierre total B05 ni B06/QA02: paridad visual y validación siguen pendientes.

Entrega de código B05.2: `ed6d44b82e055eeb5ca224bba21e0a766bee93bd`. PR1 existente, misma rama; checkpoint
documental posterior conserva idéntico árbol de aplicación. CI y Android real
pendientes; no se consulta Actions automáticamente ni se inicia B06/QA02.


## Corrección CI02 — puerta antes de B06

Run135 de0ec482b falló por acceso implícito a maxWidth dentro de Column
(NowPlayingScreen.kt:384). Se captura la medida en BoxWithConstraints, sin cambiar
el panel. La revisión estática anterior no equivale a compilación real: validación
nueva pendiente de GitHub. Ver ../../qa/CI02_BUILD_FAILURE.md/.json y CI_WORKFLOW.md.
Workflow/reportes mejorados con compile previo, entradas de tareas y estados por
archivo, sin falsos éxitos ni checks desactivados.21 pruebas de utilidades correctas;
no Gradle/Android local. No avanzar B06/QA02 hasta resolver CI. Publicar/avisar y
esperar resultado del usuario; mantener B05 y paridad visual pendientes de validar.


## CI03 — corrección de compilación del archivo de pruebas

Run42232423184: código principal y lint pasaron, falló compilar pruebas por
R.string en PlayerIntegrationTest. Gate/APK bloqueados correctamente. Import
explícito del R de Aurelis, sin tocar pruebas/producción/workflow/plan/licencias.
Ver ../../qa/CI03_TEST_RESOURCES.md/.json. Dos revisiones estáticas; compilación y
pruebas nuevas pendientes de GitHub, no ejecutadas aquí. No B06/QA02. Esperar
resultado del usuario antes de continuar; B05 no se declara verificado en Android.


## CI04 — registro recibido, fallo de aserción localizado

Run137 ejecutó200 pruebas por variante y falló la misma prueba de búsqueda en
línea75. Se restringe assertDoesNotExist a resultados en lugar de toda la pantalla,
y se verifica que la canción existe con Todo, desaparece con Álbumes y continúa
en MiniPlayer/estado. Colección e historial siguen comprobados. No se cambia la
lógica de búsqueda o reproducción: solo etiqueta de test y matcher reforzado.
JUnit se publica como anotación además del artefacto.23 pruebas Python correctas;
prueba UI y nueva compilación pendientes de GitHub, no ejecutadas localmente.
Ver ../../qa/CI04_INVESTIGATION.md/.json. Doble revisión estática, sin cierre Android
ni avance B06/QA02. Publicar y esperar resultado comunicado por el usuario.


## CI05 — aislamiento de PlaybackIntegrationTest

Reintento autorizado de registros: API Forbidden, páginas públicas/anotaciones
JUnit disponibles. Run138: carrera entre uso de Main y su sustitución en pruebas.
Se registra/espera el Job de cada ViewModel creado y reemplazado antes de resetMain.
Los diez casos existentes y sus asserts permanecen; regresión IO/finalizador Main
preparada. Detalles en ../../qa/CI05_MAIN_DISPATCHER.md/.json. Revisión doble estática,
sin ejecución Android local ni cambio de app/workflow/plan. Nuevo CI pendiente.
B06/QA02 detenidos; publicar/avisar y esperar resultado que comunique el usuario.


## B06.1 — coordinador y cola, autorizado

Base cefe533 (SHA completo en B06_1_QUEUE.json). CI05 anterior: run139 SUCCESS
del HEAD cefe533dd1e256c67a720a97a241e95789c1fa9d, confirmado mediante páginas
públicas de GitHub; no acredita Android real. QueueCoordinator existente deja
de ser stub; menús → Play Next/adición, QueueScreen → InlineQueue/arrastre/acciones
accesibles. UUID por ocurrencia y edición Media3 en sitio; misma sesión/motor.
Adiciones FIFO separadas del último playSongs. Detalles/archivos/21 tests nuevos
en B06_1_QUEUE.md/.json; preparados, NO EJECUTADOS localmente. Dos revisiones
estáticas del mismo agente; compilación/lint/tests exclusivamente en GitHub.
Play Next con shuffle deshabilitado y explicado, política de sucesor pendiente.
B06.2/.3/.4 y validación real/paridad pendientes. Plan/QA01/Glass/ABI/servicio
preservados. Publicar en mismo PR1, avisar y esperar resultado del usuario.


Entrega B06.1 — commit de código `4fbb660ec7f0441fe99aa21d8cd517eb86612ba5`, identidad local
ed0668822 <ed0668822@gmail.com>. Destino: rama existente y PR1
https://github.com/ed0668822/Aurelis-Music/pull/1, título conservado.
Checkpoint documental posterior sin cambiar el árbol de aplicación. CI/21 tests
y Android real pendientes; siguiente B06.2 espera resultado del usuario.


## CI06 — corrección antes de continuar B06/B07

Run140 de85100dd falló al compilar dos tests B06.1; APK/gates posteriores omitidos.
Corrección mínima: Builder recibe session.token directamente; CustomActions se lee
como lista tipada y su acción se ejecuta en compose.runOnIdle. Todas las líneas de
aserción y métodos de prueba conservados. Evidencia en ../../qa/CI06_TEST_COMPILATION.md/.json.
Dos revisiones estáticas, no compilación/tests Android locales. Validación nueva
pendiente de GitHub; publicar en misma rama/PR1 y esperar resultado del usuario.
B06.1 no verificado; B06.2/.3/.4 y B07 pendientes, sin cambio de plan ni producción.

Entrega CI06 — commit de corrección `a5d5ce3a1a4111593c56bcd39fa0dc6ae252bc9d`; misma rama y PR1.
Checkpoint documental posterior, sin alterar el árbol de tests/aplicación. Nuevo
resultado de GitHub y ejecución de pruebas pendientes del aviso del usuario.


## CI07 — corrección de selectores tras resultado del usuario

Run141 de390c759 falla ejecutando dos pruebas de cola. Historial oculto hacía
que el primer botón quitara canción3; el tag de arrastre está en árbol unmerged.
Se explicita historial, se selecciona por UUID y se usa árbol correcto en los
cuatro accesos al tirador. Expectativas [2,3]/[1,3,2] conservadas; identidad,
índice, posición y pausa comprobados. Ver ../../qa/CI07_QUEUE_TEST_TARGETS.md/.json.
Dos revisiones estáticas del mismo agente; producción/plan/Glass/gate preservados.
Nueva compilación/tests exclusivamente en GitHub, pendientes del aviso del usuario.
No cierre de B06 ni inicio de B07/B06.2/.3/.4/QA02 en esta corrección.

Entrega CI07 — corrección `fe43f5b881f3f067aa0e6cb10afb504153f341f6`; mismo PR1 y rama existente.
Checkpoint documental posterior sin cambiar tests/app. Compilación y ejecución
pendientes exclusivamente de GitHub; esperar resultado del usuario antes de avanzar.


## CI08 — entrega1 de advertencias antes de B07

Autorizada la primera entrega propuesta, con base c4a627d/run142 SUCCESS observado
en revisión anterior. OptIn Haze localizado, helper Modifier con LazyItemScope
explícito, parámetros Modifier reordenados, fallback audio exhaustivo y anotación
API33 redundante retirada. Nueve entradas atendidas, pendientes de confirmar en CI.
Ver ../../qa/CI08_WARNING_CLEANUP.md/.json para archivos/hashes y doble revisión
estática. Glass/motion/servicio/plan/pruebas/firmas/SDK/ABI intactos. Sin Android
local; compilación/lint/tests exclusivamente GitHub. Publicar mismo PR1 y esperar
resultado del usuario. Entrega2 ONNX y entrega3 actualizaciones pendientes; no B07
ni B06.2/.3/.4/QA02 en esta corrección complementaria.

Entrega CI08.1 — corrección `606eea71e8a831a3b134686dec3a392744982d57`; misma rama y PR1.
Checkpoint documental posterior sin cambiar código de aplicación. Compilación,
lint y pruebas pendientes exclusivamente de GitHub. Esperar resultado del usuario;
entrega2 ONNX/entrega3 actualizaciones y B07 permanecen pendientes.


## CI09 — entrega2 ONNX antes de B07, autorizada

Base a3229f0; dependencia1.22.0 verificada en AAR real, dos ELF ARM64 LOAD16384.
Regla keep ai.onnxruntime corregida y checks ZIP/ELF16KB/presencia ONNX añadidos
al verify existente.31 tests Python correctos,8 nuevos; dos revisiones del mismo
agente. Modelos intactos: voces carga en CPU1.19.2/1.22.0; Beat This falla igual
con ConvInteger10 en ambos. Caso pendiente específico B09/B10, no éxito simulado
ni cambio de modelos sin procedencia. Ver ../../qa/CI09_ONNX_16KB.md/.json.
Sin compilación/Android local; APK final/lint/suites/ZIP/ELF pendientes de GitHub.
Plan/Glass/servicio/ABI/SDK/Gradle/AndroidX conservados. Publicar mismo PR y esperar
resultado del usuario. Entrega3 y B07/B06.2/.3/.4/QA02 no iniciados.

Entrega CI09 — corrección `da938b44c94dd72eaf008c0df1bdb06adfcea8b5`; misma rama y PR1.
Checkpoint documental posterior sin cambiar runtime/verificador.31 tests Python
correctos; APK final/CI/Android16KB pendientes. Beat This CPU conserva fallo previo
ConvInteger; no exportación ni inferencia Android verificadas. Esperar resultado
del usuario antes de entrega3 o cualquier avance del plan; no B07 automático.


## B07.1 — primera entrega de letras, autorizada

Base e499ad4; run144 SUCCESS confirmado con enlace de commit. Se conserva estado
pendiente B06.2/.3/.4; autorización explícita permite esta unidad de datos/temporización
sin dar por completado playback. Parser común activa Enhanced/TTML en importación
local, conserva palabras/sílabas/fondo y caché2 con lector v1. Picker admite TTML.
Límites/XML seguro/offset/repeticiones;16 pruebas nuevas preparadas, NO EJECUTADAS
localmente. Dos revisiones estáticas del mismo agente; evidencia en B07_1_TIMING.md/.json.
Compilación/lint/tests exclusivamente GitHub; publicar mismo PR1 y esperar aviso.
Servicio/cola/Glass/ONNX/modelos/ABI/SDK/CI/plan intactos. B07.1 alcance restante,
B07.2 efectos/gestos, B07.3 compartir y B08 siguen pendientes. No falsa paridad o
Android real probado; entrega3 de advertencias y ConvInteger continúan registrados.

Entrega B07.1 — implementación `291db3a9fe7e1d599388a485cc63b7343456b41f`; misma rama y PR1.
Checkpoint documental posterior sin cambiar app/tests.16 pruebas nuevas preparadas
(NO EJECUTADAS localmente); compilación/lint/suites pendientes exclusivamente de
GitHub. Esperar resultado del usuario antes de la siguiente entrega; B07 completo,
B07.2/.3 y B08 no se declaran realizados.


## Continuación autorizada B07→B08 — primera entrega B07.2

El usuario autoriza continuar hasta B08. Conservar autorización para futuras
entregas sin pedirla otra vez; esperar el resultado de GitHub entre entregas como
indicó. Run145 SUCCESS de f81b6a0 confirmado mediante páginas públicas/commit.
B07.2: reloj existente por frames/foreground, barrido de dos textos con clipping
de graphemes/bidi, palabras/sílabas/voces, foco medido, lectura manual y offset
positivo=retardo con seek inverso. VM solo publica discontinuidades con posición,
sin nuevos comandos/motores.17 tests nuevos preparados, NO EJECUTADOS localmente.
Dos revisiones estáticas del mismo agente, evidencia en B07_2_SWEEP.md/.json.
Compilación/lint/suites exclusivamente GitHub; publicar mismo PR y esperar aviso.
Pendientes: efectos completos/paridad B07.2, compartir B07.3 y B08. No B09.
B06 restante, avisos entrega3 y ConvInteger siguen registrados. Plan/Glass/servicio/
ONNX/modelos/ABI/SDK/CI/caché/parsers conservados. No declarar Android real probado.

Entrega B07.2 (núcleo) — implementación `327caca25ec7433e04a77ce31391a88f1228ecf7`; mismo PR1.
Checkpoint documental posterior sin cambiar app/tests.17 pruebas nuevas preparadas
y dos revisiones estáticas; nuevo compile/lint/suites exclusivamente GitHub,
pendientes del resultado del usuario. Autorización hasta B08 conservada; no pedir
otra para el alcance vigente. Faltan resto B07/compartir y B08; no B09 automático.


## CI10 — reparar prueba de offset antes de continuar

Run146 de ee5c3af falla en PlayerIntegrationTest: expectativa antigua500 frente a
1500 correcto para línea1000 y offset+500. Se corrige solo ese cuerpo de test y se
comprueba también offset-500→500, selección, cola/canción, pausa y no reinicio.
Restaura preferencia en finally; todos los otros métodos permanecen idénticos.
Ver ../../qa/CI10_LYRICS_OFFSET.md/.json. Doble revisión estática; app/Glass/plan/
workflow intactos. Gradle/tests Android exclusivamente GitHub, nueva validación
pendiente del aviso del usuario. Autorización hasta B08 permanece, con corrección
y resultado CI antes de nuevas entregas. No avance de bloques/B09 por esta reparación.

Entrega CI10 — corrección `ec3b4b780a83576bfda2c1bfd0a52ffff9503f56`; mismo PR1.
Checkpoint documental posterior; compilación y pruebas nuevas pendientes en
GitHub. Esperar resultado comunicado por el usuario antes de continuar B07/B08.


## B08 — proveedores y traducción, entrega autorizada

Base32a40c935c7b1b05625e40c2527fe8f2cf751a24; run147 SUCCESS confirmado en
Actions/job128195419590 y enlace al SHA exacto. CI10 validado por ese CI; no Android
físico. Autorización actual para terminar B08: registro/LRCLIB, evaluación de
BetterLyrics/BiniLyrics/lrc.red (deshabilitados por condiciones sin acreditar),
cliente de traducción configurable y caché. Selector persistente conectado,
red explícita/local primero, errores HTTP/429/timeout/red/tamaño separados,
cancelación/reintento y protección de cambios de canción. Originales conservados.
18 pruebas nuevas preparadas, ninguna ejecutada aquí; dos revisiones estáticas
separadas del mismo agente. XML/recursos/alcance/diff revisados; CI nuevo pendiente
exclusivamente en GitHub. Evidencias: beta-local/B08_PROVIDERS.md y
docs/qa/B08_PROVIDERS.json según la raíz del repositorio. No se alteran plan
principal, Glass, servicio, comandos de cola, DSP, NDK, ABI, SDK, modelos o CI.
B07 visual/compartir y B06 restante siguen abiertos; esta entrega no los cierra.
Misma rama/PR1. Avisar tras publicar y esperar resultado del usuario; no B09.

Entrega B08 — implementación `524e964a4f595412f1dc5611436cef2bf88c5468`.
PR: https://github.com/ed0668822/Aurelis-Music/pull/1.
Checkpoint documental posterior; GitHub y Android pendientes para esta entrega.
Esperar resultado del usuario antes de continuar; no B09.


## CI11 — corrección del import R antes de continuar

Run148/b927f0a FAILURE, job128208313569: seis diagnósticos Unresolved reference R
sobre NowPlayingScreen; falta import com.music.aurelis.R tras B08. Corregida solo
esa línea. Gate bloquea compile y deja lint/tests/build/verify/upload sin ejecutar.
Dos revisiones estáticas separadas del mismo agente; imports/recursos de cinco
archivos B08 comprobados, resto de la pantalla idéntico. Evidencias en docs/qa/
CI11_RESOURCE_IMPORT.md/.json (rutas desde raíz). Compilación/tests exclusivamente
GitHub; nueva validación pendiente. CodeQL de la base también falla, causa propia
no confirmada en anotaciones públicas; no se modifica ni desactiva.
Plan, Glass, pruebas, motor, ABI/modelos/Gradle/workflow intactos. Mismo PR1;
esperar resultado del usuario, no B09 ni otra unidad.

Entrega CI11 — corrección `7014468ca711772b209e9076bbdc022385f2d468`; PR https://github.com/ed0668822/Aurelis-Music/pull/1.
Checkpoint documental posterior; nuevo CI pendiente del resultado del usuario.


## CI12.1 — auditoría complementaria y nombre de APK, autorizados

Primera de tres entregas complementarias: inventario completo de Git, raíces y
entradas/resultados terminales de Gradle, estados por variante en modo observación.
Desconocido no se transforma en éxito ni en faltante. APK release como
Aurelis-Music-V<versionName>.apk y debug con -debug, versión leída del APK real;
bytes/firma conservados y manifiesto/checksums actualizados.47 fixtures Python
PASAN; Gradle/Android nuevos NO EJECUTADOS aquí, validación exclusivamente GitHub.
Dos revisiones estáticas separadas del mismo agente. Ver docs/qa/CI12_AUDIT_PLAN.md
y CI12_1_INVENTORY.md/.json (rutas desde raíz). No cambios app/workflow/gates/plan.
CI12.2 presencia APK y CI12.3 integración UI ya autorizados, pendientes después de
publicar y recibir resultado CI del usuario. Base CI149 SUCCESS confirmado antes;
esto no valida esta entrega. Mismo PR1, no B09 ni otro bloque de migración.

Entrega CI12.1 — implementación `c317f4a9f65b025941e5d97e229a0bcf840fb9d9`.
PR: https://github.com/ed0668822/Aurelis-Music/pull/1.
Checkpoint documental posterior; GitHub nuevo pendiente. Esperar resultado del
usuario antes de CI12.2/.3, cuya autorización permanece. No B09.


## CI12.2 — contenido real de APK, autorizado

Base f36d44f/CI150 SUCCESS y CodeQL1319 SUCCESS confirmados; CI12.1 validado por
ese CI. Nueva política explícita: DEX/manifiesto, recursos esenciales, modelos,
fuente y nativos ARM64; faltantes obligatorios bloquean publicación. Informes
apk-content-debug/release JSON/TSV y enlaces desde compilation-audit por variante.
R8, recursos transformados, homonimia y ausencia de outputs mantienen límites;
no se convierte presencia en integración UI. Reportes obsoletos rechazados.
64 fixtures Python PASAN (47 anteriores conservados,17 nuevos). Dos revisiones
estáticas separadas del mismo agente; formato AAPT contrastado con SDK existente.
No Gradle/Android local: auditoría de APK reales y nuevo CI pendientes de GitHub.
Ver docs/qa/CI12_2_APK_CONTENT.md/.json desde raíz. App, plan, workflow, Gradle,
ABI/firmas/modelos/Glass intactos; mismo PR1. Avisar y esperar resultado; CI12.3
no iniciado, autorización conservada. No B09.

Entrega CI12.2 — implementación `29525fa5562a3bcae9feee98acf53feddaf5baf8`.
PR: https://github.com/ed0668822/Aurelis-Music/pull/1.
Checkpoint documental posterior; nuevo CI/auditoría de APK reales pendientes.
Esperar resultado comunicado por el usuario; no CI12.3 ni B09 en esta entrega.


## CI13 — fallo del auditor al leer namespace AAPT2

Run151/e360935: verify=failure, upload_apk=skipped; compilación y pruebas no son
el fallo mostrado. JSON interno no accesible (descarga API Forbidden). Defecto
reproducido con SDK35/android36 existente: parser android:name encontraba0
componentes donde URI Android completa produce84. Parser corregido acepta URI
completa/alias Android y rechaza otros namespaces; fixture ajustado a formato real.
Excepción pública incluye causa segura y ruta JSON para próximos diagnósticos.
66 pruebas Python PASAN,64 anteriores conservadas,2 nuevas; reproducción real
SDK ahora84 y conjuntos URI/abreviatura idénticos. Dos revisiones estáticas del
mismo agente. Política/gates/app/plan/workflow intactos. Evidencias desde raíz:
docs/qa/CI13_AAPT_NAMESPACE.md/.json. No Gradle/Android local; nuevo CI pendiente.
Sin JSON remoto no se descartan otros fallos; esperar resultado antes de avanzar.
Mismo PR1; no CI12.3 ni B09.

Entrega CI13 — corrección `e34f70f5a11a6b7282fe9404c3162b5bdcbb4a68`.
PR: https://github.com/ed0668822/Aurelis-Music/pull/1.
Checkpoint documental posterior; nuevo CI pendiente del resultado del usuario.
No CI12.3 ni B09 en esta corrección.
