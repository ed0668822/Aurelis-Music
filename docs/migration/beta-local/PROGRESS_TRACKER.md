<!-- Aurelis © 2026. GPL-3.0-or-later -->
# PROGRESS_TRACKER — beta local

### CI196 — Android validado; publicación de artefactos al reintentar (2026-11-17)

SHA8da645ba, run44021559186. Intento1 falló antes de ejecutar tareas por HTTP429
de Maven Central al resolver Kover0.9.11; B02–B05 omitidos. Se reintentaron los
trabajos fallidos del mismo SHA, sin otra modificación de Android.

Intento2: B01, B02, B03, B04 y B05 SUCCESS.506/506 casos Debug y506/506 Release
PASS; profileAndLibrarySettingsSurviveRecreation y
profileAndLibrarySettingsPreserveRealPlayback PASS en API36;8/8 casos Media3
PASS en API35, incluidos WAV y continuous. Los tres timeouts de CI195 quedan
superados en esta ejecución. El paso del emulador B05 duró283s frente a815s en195.
No acredita prueba física en HyperOS ni igualdad visual completa.

B06 consolidó/subió el informe y aprobó Enforce all phases and report publication,
pero falló después al limpiar: Duplicate artifact names; refusing cleanup.
La API confirmó dos b01-report-196 y dos Aurelis-Informes-B01-B06-196, activos,
provenientes de ambos intentos. No se declara verde global para196.

Corrección de infraestructura acotada antes de editar:9 rutas de máximo10,
sin reiniciar contadores funcionales. Se añade overwrite:true a las12 subidas
de los seis workflows B01–B06: el productor reemplaza su artefacto homónimo en
la misma ejecución. Se conservan nombres, rutas, dependencias, orden, permisos,
retenciones, firmas, ABI y todas las puertas. El limpiador sigue rechazando
duplicados/desconocidos y nunca limpia una validación fallida. No se eliminan
manualmente artefactos históricos ni se crea una Release oficial.

Una regresión consume la configuración real de los seis productores, simula
dos intentos y ejecuta el limpiador original: conserva solo los tres artefactos
finales del intento más reciente. Al retirar overwrite en memoria, reproduce
el rechazo original, sin alterar archivos.15 pruebas de contrato y9 de grafo CI
PASS; actionlint de los seis workflows y diff --check PASS. Dos revisiones del
mismo agente: contrato por ejecución y rechazos seguros; después grafo/scope/
procedencia y conservación del código Android. Nueva ejecución pendiente.

Rutas: seis workflows de B01–B06, test_b06_delivery_contract.py y los dos
registros FUNCTION_BLOCKS_40_10/PROGRESS_TRACKER. F004.a8/10, F004.b9/10 y
F005–F00611/40 se conservan; la reparación CI ocupa9/10. Entrega acumulada27
rutas propias,30 contando los tres iconos externos. No se inicia F007; avisar
del envío y esperar al usuario antes de continuar.

### CI195 — bienvenida animada y esperas del harness Android (2026-11-17)

El ZIP original aportado por el usuario es válido:311 entradas. SHAa76abcf4,
run44017612090: B01/B02 SUCCESS;506/506 Debug y506/506 Release aprobados,
incluidas las reparaciones CI193. B03 falla exclusivamente por el primer caso
de navegación API36, TIMEOUT150s; el segundo caso con audio PASS. B05 registra
6/8 PASS: local y continuous terminan124 por timeout125s. B04 SKIPPED y B06
FAILURE mantienen la puerta correcta. No se certifican fases omitidas.

Las trazas de continuous muestran READY, más de12s de avance, seek pausado,
reanudación y stop/abandono de foco; no hay resultado JUnit final. Local no llega
a registrar el punto posterior a ActivityScenario.launch/onActivity. En API36
se observa RESUMED, pero no aparece la primera evidencia de Perfil antes del
timeout. No hay stack de hilos que permita afirmar un deadlock nativo o un fallo
de decodificación. Se leyó el fuente oficial ActivityScenario1.6.1: espera
waitForIdleSync en launch, transición de estado, onActivity y cierre. La nueva
bienvenida continúa animándose y el harness de audio nunca la completaba.

La integración de pruebas deja de depender de inactividad global mientras esa
bienvenida está visible: el caso nativo la abre con un Intent normal y pulsa
Siguiente/Empezar por accesibilidad, comprueba persistencia y perfil intacto,
y después usa ActivityScenario para todos los recorridos/recreaciones originales.
El fixture B05 marca solo la bienvenida completada con la app detenida, conserva
las demás claves app_meta y verifica el booleano leído del archivo. No cambia
ajustes de movimiento, Glass, foco, decodificador o las aserciones de audio.
Esta preparación de B05 no cuenta como prueba de la bienvenida; la prueba real
sigue en B03. No se amplían timeouts ni se añaden reintentos que oculten fallos.

Se imprimen los logs de los casos incompletos y etapas del harness en Actions,
además de conservar sus artefactos. Cinco fixtures Python nuevos comprueban
preservación de metadatos, instalación nueva y rechazo de XML/readback/error de
dispositivo.86 pruebas Python aprobadas (62 capturador,7 atestación B05,17 B03),
sintaxis Bash/Kotlin y diff --check; Android solo en Actions, aún pendiente.
Dos revisiones del mismo agente: flujo/lifecycle/persistencia y contratos de
evidencia/scope/compatibilidad. No se declara resuelto físicamente sin el nuevo CI.

Seis rutas en esta reparación, solo b05-emulator-smoke.sh nueva en el acumulado:
F004.a8/10, F004.b9/10, F005–F00611/40;20 rutas propias y23 con los tres iconos
externos preservados. No se inicia F007. Tras push avisar y esperar al usuario.

### CI193 — corrección de las pruebas de Inicio (2026-11-17)

Run44015753195, SHA16040c91: B01 SUCCESS. Los logs originales verificados
registran506 casos por variante,503 aprobados y3 fallidos tanto Debug como
Release. HomeLocalTest aún buscaba el rótulo/botón visual legacy; la tarjeta
portrait conserva Continuar en su semántica y se activa tocando la tarjeta.
Los otros dos fallos son SDK33/35 de HomeShelvesIntegrationTest: el reloj de
Media3 reanudado pasó de12345 a12354ms, invalidando una igualdad temporal exacta.

Se corrigen únicamente las pruebas: recorrer la lista y el carrusel reales,
validar la semántica Continuar y sus callbacks; pausar antes del seek de prueba,
comprobar posición exacta pausada y, tras reanudar, que no hubo reemplazo de cola,
prepare ni seek adicionales, sin exigir que el reloj en marcha quede congelado.
No se desactiva ninguna aserción funcional ni se cambian producción o workflows.

El resultado global de193 es CANCELLED, no un fallo de compilación Kotlin.
El push posterior8f7e1b1a, tres iconos ajenos conservados mediante fast-forward,
inició CI194 y la concurrencia canceló193. B02/B03/B05 quedaron cancelados,
B04 omitido y la puerta B06 falló correctamente por evidencias incompletas.
Lint y emulador no quedan aprobados ni tienen otra causa raíz demostrada aquí.

Reparación de4 rutas: las dos pruebas y los dos registros de progreso/plan.
Solo HomeLocalTest añade una ruta al alcance anterior: F005–F00611/40,
F004.a8/10 y F004.b8/10 sin cambios; entrega propia19 rutas distintas.
Incluidos los tres iconos externos preservados, el intervalo completo suma22.
Dos revisiones del mismo agente: (1) trazas, recorridos y reloj/comandos Media3;
(2) selectores Compose, alcance, conservación de cambios ajenos y diff.
Parser Kotlin y diff --check locales; pruebas Android solo Actions y pendientes
para el nuevo SHA. Avisar del push y esperar al usuario antes de F007.

### F004 y F005–F006 — siguientes dos bloques autorizados (2026-11-17)

El usuario pide «Continúa con los siguientes dos bloques». Base3a015702;
[CI192](https://github.com/ed0668822/Aurelis-Music/actions/runs/44006598772)
terminó SUCCESS en B01–B06 y CodeQL. Ese verde cierra las reparaciones de F003
para ese SHA; no acredita la entrega nueva ni validación física completa.

F004 activa bienvenida, nombre y permiso local sobre el perfil único existente.
La finalización se guarda en app_meta y solo cierra tras commit confirmado;
si falla, revierte también la bandera en memoria y permite reintentar. Mantiene
nombre/foto, biblioteca y pila pendientes; no importa Google ni otros perfiles.
La prueba real de ajustes atraviesa la bienvenida; solo las capturas comparativas
de fixtures la marcan completada, igual que hacen con la bienvenida de BitChord.

Una dependencia descubierta antes de exceder el presupuesto exigía adaptar el
capturador. Se subdivide F004 antes de editar esas rutas nuevas: F004.a8/10,
presentación/persistencia/pruebas; F004.b8/10, arranque y harness. Doce rutas
distintas en el padre; no se reinicia ningún contador de correcciones.

F005–F006 conserva cabecera/métricas/colas ya correctas y adapta la tarjeta
destacada a ancho70% con techo320dp, proporción0,92 y esquinas18dp. Los carruseles
usan ShelfRow original y su encaje. Se preservan acciones locales y sobrecargas
legacy; artista usa tarjeta150dp y contador localizado.10 rutas de40.

Entrega conjunta18 rutas distintas, dentro del techo60. Se añaden16 métodos de
regresión Android para SDK33/35; los existentes se conservan. Compilación, Lint,
Android16 y comparación real solo en Actions. Nuevo CI PENDIENTE; fidelidad de
píxeles y prueba en Redmi/HyperOS3 pendientes. Dos revisiones del mismo agente;
sintaxis, XML y fixtures Python locales. Validación local:74 pruebas Python
aprobadas, actionlint, diez Kotlin sin errores de parser, XML y diff --check;
compilación/tipos y Android pendientes de Actions. Listas y hashes en
[FUNCTION_BLOCKS_40_10](FUNCTION_BLOCKS_40_10.md). Tras el envío se avisa y se
espera el resultado del usuario antes de F007; no se inicia B09.

### F003 — cabeceras, perfil y acceso a ajustes (2026-11-17)

**CI191: B03 aprobado; reparación B02.** SHA3463388e: B01/B03/B05 SUCCESS,
B02/B06 FAILURE y B04 SKIPPED.474 casos Debug y474 Release SUCCESS; ambos
casos de navegación/recreación/reproducción en Android16 PASS. La mejora B03
anterior queda validada, sin declarar éxito global ni validación física.

B02 falló antes de ejecutar Lint por HTTP429 de Maven al resolver Kover0.9.11.
Se amplía únicamente la recuperación pre-tarea existente: máximo tres intentos
para ese429 exacto, esperas15/30s y caché conservada. No reintenta errores reales
ni modifica SDK, dependencias o umbrales. Cinco rutas de reparación B02 (máximo10),
F003 acumulado21/40; cinco recursos ajenos de iconos conservados aparte.
20 pruebas Python B02 aprobadas (nueve nuevas), bash -n y diff --check.
Dos revisiones del mismo agente; nuevo CI PENDIENTE. Detalle/hashes en
F003_TOP_BAR.md. F004 sin iniciar; avisar del envío y esperar al usuario.

**CI185 y mejora autorizada de pruebas:** SHA1ca0093a, run43999110484:
B01/B02/B05 SUCCESS, B03/B06 FAILURE y B04 SKIPPED.457 casos por variante,
456 SUCCESS y1 FAILURE, ambos en la aserción final de Replay→perfil→ajustes.
La prueba confundía raíz de Biblioteca con raíz de Inicio: canGoBack sigue
siendo true para volver a Inicio. Se conserva AppNavigator y se comprueban pila
vacía, retorno final a Inicio y restauración en Ajustes/Replay.

El usuario autoriza «Mejora eso y corrige el error». Navegación, cabecera y
estados de Home amplían Robolectric a SDK33/35. B03 compila Kotlin/Java de ambas
variantes antes de ejecutarlas, conserva independencia y cobertura, y reutiliza
los APK B01 verificados para dos pruebas reales en emuladorAPI36. Un informe
probado con fixtures distingue compilación/dependencias, aserciones, timeout,
proceso terminado y falta de evidencia. No se convierte exit137 en timeout
demostrado ni success de un paso continue-on-error en éxito de pruebas.

Mejora de dificultad alta:10 rutas de10, todas necesarias para código de pruebas,
workflow, scripts/fixtures y registros. F003 acumulado19/40, incluidas todas las
reparaciones; producción, SDK, ABI, dependencias y orden B01–B06 intactos.
Dos revisiones del mismo agente. Ver F003_TOP_BAR.md; nuevo CI PENDIENTE.
Validación local:149 fixtures/regresiones Python aprobadas, actionlint, bash -n,
sintaxis de tres Kotlin y diff --check. No se ejecutó Gradle/Android localmente.
Base de publicación actualizada por avance directo a69480b41: se preservan
cinco commits ajenos de iconos; esta reparación modifica10 rutas y no los edita.
F004 sin iniciar; esperar resultado del usuario después de avisar del envío.

**Reparación CI184:** el commitca3316af aprobó B01/B02/B05, pero B03 no compiló
las pruebas Debug/Release: FrostedTopBarTest:132 encadenaba performImeAction()
sobre el Unit devuelto por performTextInput(). No se ejecutaron pruebas en
ninguna variante. Kover quedó omitido; F5 rechazó COVERAGE_XML_UNAVAILABLE como
consecuencia. B04 SKIPPED y B06 FAILURE. Se separan las dos llamadas, conservando
acciones y aserciones. Tres archivos de reparación, todos dentro de las14 rutas
acumuladas de F003; máximo40. Dos revisiones del mismo agente y sintaxis/diff
locales; nueva compilación y pruebas solo en Actions, resultado PENDIENTE.
Logs CRC válidos, SHA256 y diagnóstico completo en F003_TOP_BAR.md.

Autorizado con «Continúa con el siguiente bloque», base9d203d13. Se adaptan
los helpers originales de cabecera y SquirclePillShape de BitChord b690dc14:
controles44dp, avatar28dp, gutter16dp y gaps/insets originales. Buscar toma la
altura de su superficie; Replay añade foto→ajustes con retorno a su página.
Se preservan perfil único, callbacks, fuente de captura aislada, servicio y
funciones locales. El título pequeño permanece oculto como en la referencia.

Se añaden seis regresiones para Actions. Dos revisiones del mismo agente;
validaciones estáticas locales, sin Gradle/Android local. CI nuevo PENDIENTE,
sin heredar el resultado de CI183.14 rutas de40, incluidos el plan40/10 y este
registro preparados previamente. Detalle, lista y hashes: [F003_TOP_BAR](F003_TOP_BAR.md).
Avisar al enviar y esperar resultado del usuario antes de F004, aún sin iniciar.

### Organización vigente por dificultad (2026-11-17)

El usuario establece **máximo40 archivos distintos por bloque fácil/medio y10
por bloque difícil**, incluidas pruebas, documentación, avisos y correcciones.
Se conserva el techo previo de60 rutas para una entrega conjunta. Distribución
completa F001–F200, autorizaciones y exclusiones en
[FUNCTION_BLOCKS_40_10.md](FUNCTION_BLOCKS_40_10.md).
Los60 de los registros anteriores no son el límite vigente por bloque.
Esta reorganización es documental, no implementa ni aprueba funciones pendientes.

F001 y F002 se conservan. CI183/run43970204682, SHA9d203d13, terminó SUCCESS
en B01–B06, comprobado después de los registros históricos de abajo. F003
(cabeceras/perfil/ajustes, media, máximo40) se entrega en el registro superior;
F004 (bienvenida local, difícil, máximo10) está autorizado y sin implementar.
No se inicia F005 ni B09.

### F002 — barra inferior y Buscar separado (2026-11-16)

**Corrección CI182:** `44005ca7` ejecutó450 casos por variante: Debug449/450,
Release450/450. Solo falla la captura software de Home; la ventana acelerada de
Robolectric no describe el Canvas(Bitmap). Ese caso declara LocalGlassHardware=false,
conserva sus recorridos/aserciones y añade comprobación de imagen no vacía.
La regresión de poses/captura se amplía a SDK33 y35. No cambia producción ni se
desactivan animaciones de la app. CI181 no ejecutó tests por HTTP429 de Maven;
CI182 fue una ejecución completa, no una repetición parcial. Detalle y hashes
en F002_NAVIGATION_BAR.md. Cuatro archivos en esta reparación,18 acumulados sobre
la base de F002, máximo60. Dos revisiones del mismo agente; nuevo CI PENDIENTE.
Esperar resultado del usuario antes de F003.

**Reparación posterior CI180:** `ab09f2c5` falló en B03: 449 pruebas por variante,
443 SUCCESS y6 FAILURE. Las seis nuevas de F002 aprobaron; los fallos son capturas
software de Home/player: SharedBoundsNode.draw usa drawRenderNode sin soporte.
B01/B02/B05 SUCCESS, B04 SKIPPED, B06 FAILURE. F5 carece de XML por ese fallo;
no se reduce el umbral ni se omiten pruebas. Logs originales CRC válidos y SHA256
en F002_NAVIGATION_BAR.md. Los ocho sharedElement y el overlay ahora requieren
hardware y movimiento; se conservan Glass, poses, interacción y animaciones
compartidas en la ruta soportada. Se añade regresión de dibujo software para ambas
poses y preferencias, sin cambiar las capturas originales ni Gradle/CI.
Cuatro archivos de reparación, todos dentro de las17 rutas de F002; máximo60.
Dos revisiones del mismo agente y comprobaciones estáticas; nuevo CI PENDIENTE.


Autorizado después de F001, base `4b60c176`. Se conecta la barra vendorizada real
BitChord `b690dc14`, su plegado por scroll y accesorio de dos densidades, y la
barra normal de cuatro pestañas. Se adaptan los cuatro vectores originales,
métricas/insets, máximo 440dp, selección accesible, RTL y movimiento reducido.
AppNavigator, MiniPlayer normal, reproducción y motor estable de Glass se
conservan. Sin cambiar dependencias, ABI, SDK, fuentes o servicios remotos.

Se añaden seis regresiones de scroll y UI, destinadas solo a Actions. Dos
revisiones del mismo agente; comprobaciones estáticas locales. CI nuevo y
validación física/visual pendientes; no se hereda el verde de la base.
Detalle, fuentes, licencias y límites: [F002_NAVIGATION_BAR](F002_NAVIGATION_BAR.md).
Después de enviar se avisa al usuario y se espera su resultado y autorización
para F003. Máximo 60 archivos distintos, incluidas correcciones.


### F001 — navegación por entradas: conservar y validar (2026-11-16)

El usuario aprueba el inventario F001–F014 por funciones y autoriza únicamente
F001 mediante «Implement the proposed plan». Los IDs F001–F200 del nuevo
inventario no sustituyen los bloques históricos B00–B13 ni las fases CI B01–B06.
La entrega F001 modifica exclusivamente este registro: un archivo, dentro del
máximo de 60 archivos vigente. No inicia F002 ni vuelve a aplicar cambios anteriores.

**Resultado: CONSERVAR.** Base auditada y rama/PR1 remotos:
`4b5a14d9c08bfa32a854a713aef0d42fae9af680`. Se comparan AppNavigator y el patrón
pageStates/pageKeysSeen de MainActivity del clon BitChord de solo lectura,
referencia `b690dc146009d4815825fa4a20815ef2e4ee8898`, con AppNavigator,
AppNavigationHost y sus consumidores activos de Aurelis. No se identifica un
defecto nuevo que justifique modificar la implementación de F001.

Se conservan RouteEntry e IDs, claves route:ID y compatibilidad de claves v1,
snapshots v1/v2, estado independiente de pestañas, retirada de páginas cerradas,
orden de Atrás y página subyacente a reproductor/cola. No se cambia ninguna API,
ruta, clase de producción, prueba, servicio, recurso, dependencia ni workflow.
Las rutas declaradas pero no conectadas no se presentan como nuevas funciones
habilitadas. La fidelidad de barras/pantallas corresponde a las otras funciones.

**Evidencia automatizada existente, ejecutada en GitHub Actions:**

| Evidencia | Resultado y alcance real |
|---|---|
| [Run178, intento 2](https://github.com/ed0668822/Aurelis-Music/actions/runs/43928400200/attempts/2), mismo SHA | B03 SUCCESS; logs originales de sus pasos Debug/Release leídos y ZIP comprobado por CRC. El pipeline global de este intento fue FAILURE por B04; no se presenta como un verde completo. |
| AppNavigatorTest | 13/13 SUCCESS en Debug y 13/13 en Release. |
| NavigationEntryStateTest | 6/6 SUCCESS en Debug y 6/6 en Release. |
| NavigationIntegrationTest | 6/6 SUCCESS en Debug y 6/6 en Release. |
| Total de estas tres clases | 25/25 por variante, 50 eventos SUCCESS; métodos contrastados con el código base, sin faltantes, inesperados, duplicados ni omitidos. Las tareas testDebugUnitTest/testReleaseUnitTest ejecutaron; el Kover posterior UP-TO-DATE no se cuenta como otra ejecución. |
| [Run179 completo](https://github.com/ed0668822/Aurelis-Music/actions/runs/43934048144), mismo SHA | API: SUCCESS en las seis fases B01–B06. El desglose individual de arriba procede del intento 2 de run178, no se atribuye al ZIP179. |

Los casos acreditan visitas repetidas independientes, IDs tras recreación,
descartar estados cerrados, conservar pestañas/búsqueda, compatibilidad v1/v2,
rechazo de snapshots malformados, Atrás en páginas anidadas y cola → reproductor
→ página/pestaña anterior, incluida restauración sin canción seleccionada.

Procedencia de los logs: archivo externo al checkout
`43928400200-attempt2-logs.zip`, SHA256
`daaf8be2ff38c9c3bbd449980f6013791d20ef460df7ab27b743343a9c8f0ca4`.
El checkout de B03 dentro del log y la API del intento coinciden con el SHA base;
se leen los pasos individuales 8/9, sin duplicar sus copias del log agregado.
La descarga del informe179 (artefacto 14260443599) falla con 403 Forbidden;
`gh run view 43934048144 --repo ed0668822/Aurelis-Music --job 133537151687 --log`
devuelve salida vacía, lo que no acredita
ausencia de errores ni permite afirmar que se leyó su informe individual.

Dos revisiones realizadas por el mismo agente, no independientes:

1. Modelo, restauración, limpieza de estados, consumidores y referencia BitChord.
2. CRC/procedencia de logs, correspondencia de métodos por variante y diff
   limitado a este documento, comprobado con git diff --check.

No se ejecutan Gradle ni pruebas Android localmente ni se añaden pruebas
redundantes. El verde179 acredita el código base, no una ejecución nueva del
commit documental. Los filtros paths de b06-unified-pipeline.yml y
ui-parity-comparison.yml no incluyen este archivo de documentación; este push
no inicia automáticamente esas compilaciones y no se solicita otra ejecución.
La comprobación física en Redmi Note 14 Pro 5G con Android 16/HyperOS 3,
la fidelidad visual completa y los bloques posteriores siguen
pendientes. Después del push se avisa al usuario de los archivos enviados y
del resultado real de GitHub; se espera su confirmación y autorización expresa
antes de F002. Un fallo de la nueva entrega se investiga antes de avanzar.

> **Límite vigente solicitado:** máximo **30 archivos por bloque**, contando toda la entrega y todos sus commits, incluidos pruebas, recursos, documentación, configuración y eliminaciones. Esta regla posterior prevalece sobre el techo35 histórico de [PLAN_CONTINUATION](PLAN_CONTINUATION.md); los subbloques menores siguen permitidos. [PLAN_30_FILES](PLAN_30_FILES.md) conserva su registro histórico. No se cierran retrospectivamente B06/B07 ni se inicia B09 por registrar el plan.

> **Excepción posterior para estas correcciones UI:** máximo **60 archivos distintos**, incluyendo sus correcciones, pruebas y documentación. El usuario pidió esperar y posteriormente autorizó «Continúa y termina». La excepción se aplica a esta entrega; no altera el alcance ni acredita paridad visual por compilación.

> **Ampliación autorizada posterior:** máximo **62 archivos distintos para estas correcciones UI**, incluidos todos los commits de reparación. Sustituye el techo60 de esta entrega; el límite general de otros bloques sigue en30. El usuario autorizó expresamente aplicar el parche de los dos contratos de idioma y entregar contexto detallado.

> **Última autorización — solo límite:** máximo **63 archivos distintos para esta corrección UI**, contando el historial completo de la entrega. El usuario autoriza únicamente ampliar el límite de62 a63 y pide explicar el parche. La aplicación de código adicional, commits, push/publicación y el inicio funcional de B07 quedan en pausa hasta una instrucción expresa posterior. Se conservan los borradores locales existentes.


> **Autorización posterior de implementación:** el usuario autoriza terminar e implementar el parche explicado de Biblioteca/galería, con commit y push a feature/bitchord-local-migration/PR1. Máximo63 rutas distintas en toda esta corrección. Esta instrucción posterior levanta la pausa de implementación/publicación; la paridad visual, prueba física y cierre de B07 no se dan por aprobados al aplicar el código.

> **Aclaración vigente del usuario:** máximo **60 archivos distintos por mensaje/entrega**
> para este trabajo, pudiendo modificar todos los archivos necesarios. Sustituye la
> interpretación histórica de63 acumulados. Cuenta código/tests/docs/config/renombres/
> eliminaciones de la entrega; múltiples commits no reinician su presupuesto.
> El usuario autoriza continuar con la primera entrega del plan B05_UI_EVIDENCE_PLAN.

### Investigación exhaustiva UI y dos regresiones reportadas — base30e78cf4

ZIP177 CRC válido: B01–B06 PASS y439 por variante; ese verde pertenece a la base.
UI31/38980bfc incompleta en referencia,24/36 por material, NOT_REVIEWED. Imágenes
nuevas bloqueadas403; no se inventa revisión ni causa de ADB. Usuario confirma cierre
de TODA la app al tocar minutos. Defecto de autorreferencia de grabación/cabecera de
Replay corregido, con regresiones GL de API35/36. Reapertura de player vinculada a
song.id en reveal: efecto cambia a presentación/disponibilidad; prueba de frames con
animaciones y Next/Previous nativos. No desactivar Glass ni animaciones para ocultar.

Se adaptan trazados y componentes reales de BitChord fijo, header/controles de letras
compartidos,34/41sp+30/38sp, gutter30/duet44, puntos instrumentales y foco con primer
vocal, controles por gesto20dp, multiselección real y labels ES/EN. Ver inventario,
fuentes/hashes, discrepancias y pendientes en UI_PLAYER_DEEP_AUDIT.md. No afirmar
copia completa de todos los módulos ni identidad visual; partículas/glow/collapse
exacto, períodos Replay y prueba Redmi16/HyperOS3 siguen pendientes. B09 no iniciado.
CI nuevo pendiente, Android solo Actions. Dos revisiones del mismo agente.
Tras push avisar y ESPERAR resultado del usuario. Máximo60 por entrega/correcciones.

### Player/letras y parpadeo — base d181c97a

Nueva petición autoriza presentación activa del reproductor/letras y transición
entre canciones. Portada retenida en player/mini y paleta sin reset intermedio;
protección de peticiones obsoletas. Cabecera/fondo/controles de letras, foco34/41sp,
manual y sincronización preservados. Compartir una línea real con PNG ajustado y
FileProvider limitado, Atrás y cambio de canción.3 pruebas de estado y1 de PNG/URI,
integración de letras ampliada, SOLO Actions. No certificar paridad ni cierre B07;
partículas/glow y multiselección de versos pendientes. Ver PLAYER_LYRICS_TRANSITIONS.md.
Tras push ESPERAR resultado del usuario. Dos revisiones del mismo agente; B09 sin iniciar.

### Continuación por capturas — player, Replay y ajustes

Base1da11b0f. Arte vertical de borde a borde y créditos/cabecera; slider restante y
volumen fino real. Replay con tarjetas/ranking y suma común saturada con Biblioteca;
ajustes con geometría de grupos y jerarquía adaptadas. Dos pruebas de agregación,
regresión de ancho de arte y pruebas de navegación/borrado conservadas.
CI nuevo PENDIENTE; Android solo Actions. Dos revisiones del mismo agente.
No simular períodos semanales/mensuales/anuales del histórico legado. No cerrar
B05/B11 ni certificar fidelidad visual/física. Ver PLAYER_REPLAY_SETTINGS_FOLLOWUP.md.
Tras push esperar resultado que comunique usuario; no avanzar por iniciativa propia.

### B05 — pendientes confirmados del ZIP: cápsulas y textos ES/EN

Corrección posterior: Main171/43816340050 de4eb1ef89 FAILURE. B01 sí compiló;
B03 ejecutó432 por variante y falló un caso en ambas: ChromeContractTest esperaba
el antiguo literal de búsqueda sin resultados. Logs originales/CRC leídos antes
de editar. Aserciones actualizadas a los recursos localizados, conservando el
contrato backdrop/campo excluido/cuerpo no vacío. 19 archivos distintos acumulados,
máximo60, sin cambios de producción ni supresión de pruebas. Dos revisiones del
mismo agente y comprobaciones estáticas; no Android local. Nuevo CI pendiente,
ESPERAR respuesta del usuario después del push. Detalles en B05_LOCAL_UI_FOLLOWUP.md.

El usuario dice «Continúa» tras indicar que esperemos su resultado de GitHub; se
continúa con entrega5 del plan, sin repetir MiniPlayer/reproductor. Base0f639624:
Main170 SUCCESS B01–B06,429 por variante y90,35% confirmados en logs del turno
anterior; UI26 FAILURE en referencia, Aurelis18/18 por material, casos nativos35/36
PASS. No declarar paridad total ni trasladar ese verde al nuevo SHA.

Cápsulas50/max420/gap14 adaptadas de ActionPill de BitChord fijo, Play relleno y
Shuffle con borde, mismos callbacks y conjunto de canciones; no muestreo del propio
backdrop ni funciones remotas. Se localizan Música local y búsqueda: títulos,
subtítulos, metadatos/plurales, pestañas, acciones, vacíos, historial, filtros y
etiquetas de destinos. Helper UI para SortOption sin cambiar valores persistidos.
Inicio usa plural real y etiquetas de carga/hero accesibles ES/EN; datos musicales,
perfiles y consultas permanecen originales. Se conserva navegación/estado/backdrop.

Tres regresiones Android/JVM preparadas: cápsulas EN50dp con Play/Shuffle/orden y
nombres reales preservados; búsqueda EN con categorías/consulta100% e historial;
Home ES singular/plural y Favoritos conectado. Tests previos de scroll actualizan
solo título localizado. No ejecutados localmente; compilación/Lint/APK/tests Android
SOLO GitHub Actions. Dos revisiones del mismo agente, no independientes. Ver
B05_LOCAL_UI_FOLLOWUP.md. B07/B09 sin iniciar y prueba física pendiente.

Regla vigente de trabajo: tras push, avisar y ESPERAR que el usuario comunique si
GitHub compiló correctamente o falló. No vigilar conclusiones por iniciativa propia
ni iniciar otra entrega. Se permite dejar CI iniciado y sus enlaces para revisión.
«Continúa» del usuario confirma avanzar; si informa error, leer evidencia real y
reparar la misma entrega antes de otro bloque. Máximo60 archivos distintos por
mensaje/entrega, incluidos sus commits de corrección. Sin nueva Release oficial.

### B05 — corrección de compilación del primer intento del player

Run169/43799755349/e0fc7cad: fallo en compileDebugKotlin por acceso a favorites
privado del store y falta de import semantics.contentDescription. Se usa el estado
público colectado del mismo store y se añade import; logs oficiales CRC válido.
Se respeta hideVolumeBar reservando40dp y sincronización deshabilitada para preview.
Cinco pruebas JVM nuevas esperadas429; la adicional comprueba que ocultar volumen
conserve la geometría del deck y controles visibles. Dos revisiones del mismo agente,
corrección dentro de las mismas24 rutas; Android nuevo pendiente de Actions.

### B05 — MiniPlayer y reproductor vertical/horizontal, implementación autorizada

El usuario autoriza terminar MiniPlayer y pantalla de reproducción en esta entrega,
incluyendo ambas orientaciones; máximo60 archivos distintos en el mensaje, sin
arrastrar inventarios históricos. Base18950a40: Main168/43794130240 SUCCESS y CodeQL
SUCCESS. UI24/43794130375 FAILURE: cuatro player Aurelis fuera de vista, con su
nuevo gate, y pérdida de ADB en referencia. Ambos recorridos API35/API36 PASS.
No se cierra la brecha por el verde principal ni se atribuye causa JIT/GPU/OOM sin traza.

MiniPlayer adapta fila, dos líneas, arte40/radio8/pill y glyph next real. Apertura
por tarjeta/gesto, elimina flecha extra y línea visual de formato; mantiene formato
y progreso accesibles por semántica y calidad en player. Pausa permanece accionable
durante buffering; gestos/capacidades/dock actuales conservados. Reproductor asigna
deck fijo al pie y espacio restante a arte/créditos: transporte sin scroll. Aplica
márgenes30/max560, SVG/XML originales de transporte, barra fina/tiempos extremos,
calidad, favorito real, volumen STREAM_MUSIC, cápsula shuffle/repeat/autoplay local,
letras/cola y herramientas. Horizontal conserva paneles y estado guardado; transportes
y cambio de panel se mantienen accesibles al mostrar cola/letras. Letras conserva
su núcleo/panel actual: efectos/gestos/compartir de B07 siguen pendientes.

Textos de controles ES/EN; selectores de tests usan recursos y dejan de desplazar
controles que ahora deben caber de inicio. Cinco nuevos tests JVM: retrato320×568
con fuente1,5, horizontal640×320 fuente1,5, volumen Android real y pausa durante carga.
Prueba nativa con reproducción añade player, pausa, giro, reanudar, retorno y cierre;
conserva pruebas Perfil/Biblioteca/Ajustes/Search/renderer/Glass, sin alargar timeouts.

Dos revisiones del mismo agente, no independientes. Kotlin parser/XML/diff/Python
permitidos localmente; Gradle/Lint/APK/Android exclusivamente Actions. Android nuevo
PENDIENTE al preparar el commit; no declarar que429 tests o giros aprobaron. Ver
B05_PLAYER_DELIVERY.md y LICENSE_AUDIT para alcance/procedencia/limitaciones.
Fidelidad visual completa pendiente de capturas y prueba Redmi/Android16/HyperOS3;
plain23 se pidió al usuario y sigue inaccesible por proxy. No iniciar B07/B09 aquí.

### B05 — primera entrega: evidencia y aceptación real del transporte

Se conserva a5089741/run167 B01–B06 SUCCESS,424 tests por variante,90,24% de
cobertura; no trasladar ese verde al nuevo SHA. UI23 FAILURE. ZIP glass23 aportado
por el usuario: CRC válido,18 capturas Aurelis y6 BitChord; revisión visual parcial
con diferencias confirmadas. Controles del player fuera de vista inicial en ambas
orientaciones, Play/Shuffle locales sin cápsulas, textos ES con locale EN y MiniPlayer
diferente. No cerrar B05 por estas capturas. Plain23 sigue bloqueado: HTTP302 a
productionresultssa16.blob.core.windows.net, CONNECT403. No ZIP local inventado.

Separación en resultados: panel_opened, transporte visible y verificación de acciones.
El estado exige título/control propio dentro del viewport, sesión propia PAUSED y
Play/Next/Previous con capacidad/habilitación coherentes. Anterior deshabilitado se
conserva si coincide con su capacidad real. El PNG se guarda ANTES de tocar controles.
No desplazar para ocultar un defecto. Después, prueba por UI Play→Pause→Next→Previous,
con dumps reales, cola de3 fixtures, cambio a02/03 y retorno01. No basta un tap ni
una sesión ajena. Letras exige también sesión pausada propia de01 tras esas acciones.
El criterio de abrir panel sigue separado para recoger letras aunque falle transporte.

Fixture XML deriva de la jerarquía original de UI23, con SHA de procedencia; solo
se añaden comentarios de cabecera. Regresión conserva evidencia y rechaza REACHED
con título/letras pero sin transporte. Se añaden casos de controles deshabilitados,
capacidades, viewport/paquete, acción inefectiva/sesión ajena/cola distinta e integridad
del resultado. Monitor toma memoria/procesos sin argumentos/cgroup y estado ADB
DURANTE la captura, con historial acotado; kernel antes/después aún dentro del paso.
Añade buffers Android system/events y aísla stdout sintético de los tests para
no confundirlo con captura real. Telemetría no certifica causa ni sustituye gate;
no cores, entorno, tokens, GPU/ABI/SDK/firmas ni referencia modificados.

57 regresiones Python PASS por material; dos revisiones del mismo agente, no
independientes. Comprobaciones locales estáticas/Python/diff; Android solo Actions.
No se modifica todavía UI de producto: esa adaptación es la siguiente entrega.
El nuevo gate debe detectar la brecha actual; un comparador rojo por ese defecto
es evidencia pendiente de reparación, no éxito visual. Causa final de ADB closed
sin demostrar; mejorar datos no equivale a resolverla. Ver B05_UI_EVIDENCE_PLAN.md.

## Continuación actual — primera entrega documental

### Run166 aprobado; preparación y diagnóstico de emuladores de comparación22

efd7598a/run166/43785856046: B01–B06 SUCCESS. Los logs oficiales acreditan424
SUCCESS Debug y424 Release, tareas ejecutadas y cobertura de líneas cambiadas
90,24%≥80%. Pasan creación/apertura de lista, preview y selector visual/cancelación.
Esto valida la compilación/pruebas del parche Biblioteca/galería, no paridad visual.

Comparación22/43785856052 FAILURE: glass alcanza36/36, capture_complete=true y
NOT_REVIEWED; API36 pasa ambos recorridos. Plain supera offline y falla al copiar
la primera FLAC («remote couldn't create file: Operation not permitted»), antes de
instalar apps. API35 falla ambos casos en la primera búsqueda de Perfil, visible=[],
antes de abrir Ajustes. El mismo código de app/test había pasado ambos casos en
API35/API36 de comparación21/43785229427; ese workflow se canceló después para
sustituir las comparaciones restantes por22. No atribuir estos fallos a una causa
de app, almacenamiento, bloqueo o ventana concreta sin la evidencia correspondiente.

Se establece pantalla despierta/keyguard descartado y se exige RUNNING_UNLOCKED
del usuario actual, máximo15 lecturas con historial; comando contrastado con AOSP15.
La regresión nativa guarda la jerarquía previa y aplica la política ya existente de
recuperar una sola ANR verificada de Pixel Launcher. ANR de app/desconocida/repetida
conserva fallo; no hay UiAutomation paralelo a instrumentación. Si falta un control,
el diagnóstico incluye paquete/clase raíz, ventana, pantalla/keyguard/desbloqueo.

Transferencias conservan bytes originales y verifican SHA256 remoto antes de scan.
Solo el error exacto de creación observado admite hasta3 intentos con pausa1s;
se registran todos los errores. Persistencia, dispositivo perdido o hash diferente
fallan. No cambian permisos, SDK, ABI, firma, material, aserciones ni timeouts nativos.
50 pruebas Python PASS por material; seis regresiones nuevas. Dos revisiones del
mismo agente, no independientes; parser estático Kotlin y diff correctos. Cuatro
rutas ya contadas; total63. Android nuevo exclusivamente Actions y aún pendiente.
Prueba física Redmi/Android16/HyperOS3 y revisión visual de imágenes siguen pendientes.

### Comparación20 — cristal completo; comprobación offline con espera acotada

43783063417 de02a3cb1c: glass recoge36/36 estados alcanzados, incluidos ambos
player horizontales. visual_verdict sigue NOT_REVIEWED; recoger imágenes no acredita
paridad visual, widgets, movimiento o HyperOS. Plain falla antes de instalar/abrir
apps en offline_configuration: no se verifica «Active default network: none» tras
la espera fija2s. Logs ZIP oficiales válidos; no se inventa otro diagnóstico.

La comprobación conserva las órdenes de modo avión/WiFi/datos y exige observar
el mismo estado desconectado. Espera hasta15 lecturas con intervalos1s, guardando
siempre el último dumpsys; conexión persistente o salida desconocida conservan
fallo. Dos regresiones cubren desconexión tardía y rechazo acotado, sin relajar gate.
44 pruebas Python PASS en cada material, doble revisión del mismo agente y diff
correcto. Tres rutas ya incluidas; total63. Sin Android local; nuevo CI pendiente.

### Reparación de run164 — tamaño del diálogo Nueva lista

02a3cb1c/run164/43783063441: B01/B02/B05 SUCCESS, B03 FAILURE, B04 SKIPPED y
B06 FAILURE por la puerta final. Debug yRelease ejecutan424 tests cada uno; solo
falla libraryCreatesAndOpensAPlaylistInTheExistingLocalStore con AppNotIdleException
al abrir el diálogo, antes de performTextInput (línea147). Kover no se genera tras
ese fallo; cobertura no disponible no se interpreta como causa primaria.
Los demás casos, incluido selector de fotos/cancelación y previews, pasan.
Los dos recorridos reales settings-navigation también PASS en API35/API36.

El diálogo nuevo usaba el ancho por defecto de plataforma. Los editores existentes
de perfil/listas usan ancho320dp y usePlatformDefaultWidth=false tras la reparación
del bucle de medida documentada en PROGRESS/BUILD_STATUS. Se aplica ese mismo
contrato al diálogo nuevo, conservando todas las aserciones y el timeout original.
Ambos recorridos Android añaden abrir/cancelar Nueva lista, con/sin reproducción,
antes de Buscar y su recreación. No se cambia renderer, Glass, SDK/ABI ni puertas.

Dos revisiones del mismo agente, no independientes; revisión de consumidores,
contrato de tamaño/ciclo de vida/cancelación y parser/diff. Tres rutas ya incluidas;
acumulado63. Sin Gradle/Lint/APK/Android local. Validación nueva pendiente de Actions.

### Propuesta posterior del usuario — Biblioteca y selector visual de fotos

Nueva instrucción de UI: quitar Ajustes de la lista de Biblioteca y entrar por la
foto; ajustar la estructura a BitChord y abrir galería para la foto. Cambios y
regresiones preparados, sin publicar: foto PickVisualMedia/ImageOnly, cancelación
sin alterar perfil, prueba de intent ACTION_PICK_IMAGES y navegación por la foto
con Atrás/restauración/servicio conservados. Nueva estructura con enlace Música
local, previews reales de álbumes/listas (máximo5 contando Nueva lista), creación
real y Replay total histórico si hay escucha registrada. Sin estadísticas falsas.

La ruta LibraryScreen.kt no pertenece a las62 ya entregadas: acumulado previsto63.
La ampliación a63 y la implementación/publicación están ahora autorizadas. Se aplica
el parche revisado; la validación de este código nuevo queda pendiente de Actions. No reiniciar
el conteo, reescribir historial ni añadir dependencias/SDK/ABI. Dos revisiones del
mismo agente; XML/parser/contratos/diff locales. Android nuevo aún NO EJECUTADO;
compilación/Lint/APK/tests exclusivamente Actions para el SHA de esta entrega.

Run163/43763766092 del6c8653bf: B01–B06 SUCCESS;421/421 por variante en logs.
Comparación19/43763766083 FAILURE, con ambos casos API35/API36 SUCCESS. Aurelis
alcanza sus18 estados en cada material, incluido horizontal. La referencia no
alcanza horizontal desde biblioteca fría; plain pierde ADB en player claro. La
siguiente captura preparada inicia/pausa la pista en vertical y gira MiniPlayer
antes de expandir, igual para ambas apps; exige lock1, sesión propia PAUSED,
título/controles y PNG horizontal. No certifica iniciar desde biblioteca fría en
horizontal ni rotar continuamente con player abierto.42 regresiones Python PASS
por material. ZIP de imágenes siguen inaccesibles; sin certificación de paridad.

El usuario comunicó modelo/Android/HyperOS y nuevas preferencias, no un resultado
de prueba física. Mantener esa validación pendiente. B07 conserva preparación;
no iniciarlo funcionalmente ni B09 mientras quede abierta la puerta de UI.

### Resultado de fe942152 — compilación y navegación aprobadas; botón fuera de vista en horizontal

Run162/43761495777 SUCCESS en B01–B06. Logs oficiales:421 SUCCESS Debug y421
Release, tareas ejecutadas y cobertura90,13%≥80%. Comparación18/43761495814:
ambos casos Android PASS en API35 yAPI36, incluyendo Buscar/recreación/Atrás y
reproducción. El usuario confirma Redmi Note14 Pro5G, Android16/HyperOS3; se ofrece
el artefacto ARM64 de Actions para prueba física, respuesta pendiente.

Plain/glass recogen36 estados cada uno,32 alcanzados: Home/Biblioteca/Ajustes/
Buscar/música local/MiniPlayer/player vertical/letras de ambas apps y temas. Solo
fallan los cuatro player-landscape por material: PNG ahora horizontal correcto,
foreground de la app correcto, pero sigue en la colección local y Play queda fuera
de la vista inicial. No hay timeout ADB en este run. No equivale a paridad visual.
La pulsación de Play admite desplazar el contenedor real de accesibilidad antes
de verificar sesión PLAYING→PAUSED y expandir. Se mantienen ambas comprobaciones,
título/controles reales, orientación y36 estados; no se declara player por ver la
colección. Regresión reproduce el botón oculto y comprueba gesto y sesión propia.

41 pruebas Python PASAN por material y diff correcto; dos revisiones del mismo
agente, no independientes. Tres rutas ya incluidas; acumulado62. Sin cambios app/
pruebas Android respecto a fe942152; nueva captura y CI del SHA nuevo pendientes.
ZIP de imágenes todavía bloqueados por dominios Azure variables; se solicitó
permitirlos y se explicó el patrón de subdominios. B07 conserva preparación,
sin declarar cerradas revisión visual/widgets/gestos ni prueba física de HyperOS.

### Dispositivo confirmado — regresión de Ajustes en Android 15 y 16

El usuario confirma Redmi Note 14 Pro 5G, Android16 y HyperOS3. Se amplía solo el
job independiente settings-navigation a API35/API36, fail-fast false y artefactos
distintos por API. Los APK inmutables proceden del B01 del mismo SHA; la prueba
rechaza una versión Android diferente antes de instalar o ejecutar casos y guarda
API esperada/real, modelo, ABI y fingerprint. Conserva ambos recorridos, recreación,
perfil y reproducción real con hardware/Liquid Glass. El emulador no acredita
HyperOS ni sustituye la prueba física del Redmi. Comparación visual conserva API35
idéntica para ambas apps; no se alteran SDK/ABI/firmas de la app.

Run161/43758688762 de d2e84316 SUCCESS en B01–B06: logs oficiales acreditan420
SUCCESS Debug y420 Release, tareas realmente ejecutadas y cobertura90,17%≥80%.
Job settings-navigation de comparación43758688750 también SUCCESS. Sus capturas
plain/glass FALLAN:26/33 estados recogidos, ambas alcanzan letras y player vertical
de Aurelis en ambos temas. No equivalen a26/33 estados aprobados. En plain la fila
de canción queda debajo de la cabecera local inicial; en glass Buscar vuelve al
launcher; horizontal conserva dimensiones verticales. BitChord termina en timeout
ADB en horizontal oscuro/plain y player vertical claro/glass. ZIP de logs válido;
ambos ZIP de imágenes devuelven403. Sin causa host/GPU/OOM ni del cierre de Buscar
demostrada por esos logs, no se inventa una traza.

La fuente de Buscar estaba en la columna de filtros, vacía/altura0 sin consulta,
dejando la lista fuera del backdrop. Se graba todo el cuerpo con peso y la cabecera
fuera. Regresión de estructura para vacío/resultados y ampliación de ambos casos
Android con Buscar/recreación/retorno y reproducción preservada. La captura imprime
exit-info/buffer crash si pierde foreground, antes del siguiente force-stop.
La orientación usa comandos WindowManager documentados y exige lectura lock0/1,
además de dimensiones PNG correctas. Misma política estática para ambas apps, sin
certificar rotación continua. La colección local se acredita por título de fixture
visible o cabecera local con conteo exacto3, transporte y categorías; no se desplaza
la vista inicial para esconder diferencias de altura. Las tres pistas ya fueron
verificadas en MediaStore. No basta una tarjeta resumen de Biblioteca.

40 regresiones Python PASAN por material; actionlint con ShellCheck, parser Kotlin
de los tres archivos y diff correctos. Nueva validación Android/API36 y UI pendiente
de Actions. Dos revisiones del mismo agente, no independientes: fuente/estado/API
y comandos/gates/consumidores/casos/artifactos/rutas. Siete rutas ya incluidas;
acumulado62. Sin pruebas Android locales. B07 preparado con sus huecos reales;
la puerta visual todavía no se declara cerrada.

### Cierre UI autorizado antes de B07 — escenarios de captura independientes

El usuario autoriza completar los pendientes de UI, cerrar esta corrección y abrir
B07 después. Techo62 vigente, sin nuevas rutas. 23f8ba18/run160/43755440162 SUCCESS
en las seis fases,420/420 Debug y420/420 Release, cobertura modificada90,17%.

Comparación43754259741: settings-navigation SUCCESS en ambos recorridos reales;
plain/glass alcanzan ocho estados de BitChord oscuro, incluidos player vertical/
horizontal. Fallan en el paso de letras tras volver a vertical: closed/timeout
ADB. Artefactos ZIP bloqueados por proxy. No se inventa causa GPU/OOM ni se edita
la referencia. Se separan capturas estáticas de orientación: fuerza stop antes de
girar, reinicia datos sintéticos por orientación y reproduce/pausa la misma pista
antes de acreditar el player horizontal. Letras se captura en vertical antes.
Aurelis se captura primero para conservar evidencia aunque falle el origen.
Se mantienen36 estados exigidos por material; rotación continua queda expresamente
fuera de esta captura, además de gestos/movimiento/dispositivo físico/identidad.
El diagnóstico del kernel usa grep disponible en lugar de rg ausente en el runner.

36 regresiones Python aprobadas en plain/glass; actionlint con ShellCheck y diff
correctos. Dos revisiones del mismo agente. Nueva validación Actions pendiente.
Se solicitó modelo/Android para la comprobación física; no se certifica sin prueba.
Esta autorización no convierte todavía en PASS la puerta visual ni cierra B07.

### Parche de contratos de idioma autorizado — techo62

El usuario concede explícitamente62 archivos. Se aplica el parche ya revisable a
LibraryUpdatesTest y PlaybackIntegrationTest. La compilación de varios artistas
se compara con library_various_artists del idioma activo; el error de archivo
se compara exactamente con playback_file_missing. Se conservan número de álbumes,
clave MediaStore, artistas, cola, preparación y recuperación. No se modifica
producción ni se fuerza español para pasar estas dos pruebas.

7343104d: job Android settings-navigation132935852132 del workflow43754259741
SUCCESS. Sus dos casos verifican ambos accesos, Atrás/recreación y el perfil,
con/sin WAV en la sesión real; exige renderer hardware, Liquid Glass activo y
blur no reducido. Este verde sí acredita esos recorridos en el emulador, no una
prueba física de todos los teléfonos ni la equivalencia visual completa.
La corrección de mensajes se comprobará en Debug/Release por Actions en el SHA
nuevo. Dos revisiones estáticas del mismo agente; XML, parser Kotlin y diff
permitidos localmente. No Gradle/Lint/APK/pruebas Android locales. Total62 rutas
contando toda la entrega desde7da39774^ y sus reparaciones, sin otra rama/Release.


### Validación de 02bf90a5 — cierre nativo superado en Perfil; selectores y dos contratos de idioma

Principal43752674100: B01/B02/B05 SUCCESS, B03 FAILURE, B04 SKIPPED y B06 FAILURE.
Debug/Release ejecutan420 tests cada uno; tres fallos iguales: LibraryUpdatesTest
espera «Varios artistas» fijo, PlaybackIntegrationTest espera un fragmento español
del error de archivo, y el test nuevo de búsqueda confunde Tema con el EditText
que contiene la consulta. Kover queda omitido por esas suites; no se atribuye el
fallo primario a cobertura. Se distingue el control Tema de la consulta sin retirar
assertions. Las otras dos correcciones están propuestas fuera del checkout:
requieren dos archivos adicionales a las60 rutas ya entregadas, pendientes de
autorización para62. No se cambia producción para ocultar el idioma en las pruebas.

Settings Android43752808707: ambas pruebas abren Perfil, recrean y regresan a Home;
no hay APP CRASH(NATIVE), solo finalización del runner tras AssertionError.
Falla la pulsación de Library: el texto existe como hijo del nodo seleccionable.
El selector usa ahora la acción del ancestro pulsable real, acotado al paquete,
sin coordenadas inventadas. Mantiene comprobaciones de estado/perfil/playback y
exige renderer hardware, Liquid Glass activo y blur no reducido. Se repite antes
de acreditar Biblioteca→Ajustes. El run UI se canceló después del job Settings para
retirar las comparaciones pendientes; no equivale a PASS. Las anotaciones muestran
primero el fallo JUnit y un diagnóstico resumido, respetando su límite4096.

34 pruebas Python y comprobaciones estáticas correctas; dos revisiones del mismo
agente. Ninguna ruta nueva; acumulado60. Nueva validación real pendiente.

### Causa del cierre reproducido — ciclo de RenderNode en el fondo de cristal

299cdcd7/43751201636: ambos casos Settings fallan. exit-info registra APP CRASH(NATIVE),
incluido status11. La traza de libhwui repite RenderNode::prepareTreeImpl y
SkiaDisplayList::prepareListAndChildren hasta los frames449/450. Evidencia oficial
guardada. El run se cancela después de terminar Settings para retirar comparaciones
ya sustituidas; no se presenta esa cancelación como PASS ni como fallo de compilación.

Inspección: AppNavigationHost grababa toda SettingsScreen en appBackdrop. Su
FrostedTopBar.liquidGlass volvía a dibujar ese mismo GraphicsLayer desde un hijo:
el RenderNode de la fuente contenía un lector de sí mismo. Local Music y Search
tenían la misma disposición. Se conserva el motor de cristal y se traslada la
fuente a sus cuerpos/listas; la cabecera queda como hermana fuera de la grabación.
El host no envuelve otra vez las páginas que graban su propio cuerpo. Se mantienen
Haze, refraction, identidad de capas, reproducción y ABI/SDK. El perfil de solo
lectura también publica su cuerpo sin introducir un lector en la fuente.

Regresión de estructura comprueba que el cuerpo incluye Nombre y excluye Atrás,
y que el host no crea una segunda fuente envolvente. Pruebas Android reales
mantienen los dos accesos, recreación y playback; el acceso a Ajustes desde la
lista usa desplazamiento de accesibilidad y verifica que Atrás retire el perfil.
Dos revisiones del mismo agente, parser/diff/actionlint estáticos; 60 rutas
acumuladas, ninguna nueva en esta corrección. Validación Android del nuevo SHA
pendiente antes de declarar cerrado el bug o la fidelidad visual completa.

### Ajustes — búsqueda por control e idioma, cierre real aún en investigación

221bebf5/43749820078 reproduce Process crashed en las dos pruebas Android reales
de Perfil/Biblioteca→Ajustes, con/sin playback. No hay stack Java en el filtro
AndroidRuntime; no se atribuye aún a un shader, biblioteca o permiso. 299cdcd7
amplía a buffers nativos y dumpsys activity exit-info. Se publica el diagnóstico
como anotación cuando falla, además de logs/artefactos, para leerlo sin ZIP.

La adaptación preparada filtra controles individuales, omite grupos sin resultados,
muestra vacío explícito y reinicia scroll al cambiar consulta sin borrar la
restauración. Conserva categorías/acciones reales. Ajustes/perfil/selector de idioma
usan recursos; inglés/español. 398 entradas de recursos existentes y nuevos tienen
contrapartida inglesa, tipos/plurales y placeholders comprobados. 183 etiquetas
compatibles proceden del commit BitChord fijado; NOTICE/LICENSE_AUDIT registran la
adaptación. Otros idiomas declarados conservan fallback; no se certifica traducción
integral de todos los textos hardcoded de otras pantallas o diálogos.

Pruebas históricas declaran español y las nuevas cubren búsqueda por fila, acentos,
vacío e inglés explícito. El helper de Activity respeta idioma indicado por el test;
el contrato MediaStore declara español para su fallback de artista. No se retiran
assertions ni se ejecuta Android local. Parser estático sin errores nuevos salvo
limitación ya presente en SettingsScreen; XML/diff y actionlint+ShellCheck correctos.
Dos revisiones del mismo agente. Unión de toda la entrega: 60 rutas distintas de 60.
No se declara el cierre corregido ni la UI idéntica hasta validar la excepción y
los casos Android. B07 sigue detenido en esta puerta.

### Cierre Perfil/Ajustes reportado — reproducción Android antes de atribuir causa

El usuario autoriza «Empezamos, corrige todo eso». Ambos accesos llaman openSettings
y SettingsScreen. Principal154/d36124c7 SUCCESS en seis fases; no acredita esos
accesos en hardware. Sin excepción del teléfono todavía; no se inventa causa.
Se prepara un job independiente de BitChord con MainActivity real, accesibilidad
Android, dos entradas, Atrás/recreación y perfil conservado, sin/con reproducción
Media3 de WAV original. No mocks de Activity/player. Reutiliza ambos APK B01 del
mismo SHA, hashes verificados; falla por crash/timeout/identidad JUnit incorrecta.
Conserva trazas AndroidRuntime y capturas de etapas. Android solo Actions.

Comparación11/43742940458: plain tiene textos reales pero todos los foreground
fallan; glass falla al leer XML inexistente. Se consulta dumpsys window completo
para obtener foco por display, guarda actividad/ventana y publica diagnósticos.
El dump elimina XML anterior y detecta errores de stdout aunque adb termine0;
reintenta solo los transitorios conocidos. No rebaja checks ni declara paridad.
33 pruebas Python pasan en plain/glass; actionlint con ShellCheck, diff --check y
parser Kotlin estático correctos. Dos revisiones del mismo agente. Ejecución
Android nueva pendiente; la reparación de Ajustes continúa con su resultado.

### Reparación del preflight153 — ShellCheck SC2024

cb83c974/run43742631853 falla en B01, «Enforce actionlint and record zizmor
findings»; no llega a Gradle. B02/B03/B04/B05 SKIPPED, B06 FAILURE por puerta final.
Logs oficiales verificados; reporte ZIP bloqueado por proxy. El actionlint local
anterior no tenía ShellCheck disponible. Con actionlint1.7.12 y ShellCheck0.10.0
se reproduce SC2024 en la nueva redirección de `sudo dmesg`. Se reemplaza por un
pipe a tee sin supresión; conserva stdout/stderr del kernel. actionlint completo
con ShellCheck, ShellCheck del script de auditoría, bash -n y diff --check pasan.
F5 ahora imprime su actionlint.log al fallar, conservando archivo y salida1.
No se rebajan puertas ni se declara validado Android con estas comprobaciones.
Dos revisiones del mismo agente; cuatro rutas, 55 acumuladas de 60. CI nuevo pendiente.

El usuario aporta además Aurelis-BitChord-UI-3.zip: CRC válido, SHA256
`00f979fe9d0ffca7a1a2e56b583ed7c9199fde8d1872375b02525a6e3cbae7e3`.
Procede de958b8c96/run principal146/43683456015, anterior a las correcciones UI.
Se conserva como evidencia histórica; no se reaplican sus diferencias como si
fueran nuevas ni se usa ese archivo para certificar el estado de cb83c974.

### Reparación de comparación UI9 — diálogo del launcher y diagnósticos

0786e3bc/run43739187894 (principal152): B01/B02/B03/B04/B05/B06 SUCCESS,
verificados por API. Comparación43739187866 falla en ambos materiales. ZIP glass
aportado por el usuario, CRC válido, SHA256
`95199fc9e3d68bdb2c1f9751222d5ba20e693516b7bf5cfc498fec3fa0e8f111`.
Las 36 capturas están bloqueadas por «Pixel Launcher isn't responding»; cero
estados alcanzados. No son evidencia comparable de las pantallas. El resumed
activity seguía perteneciendo a la app detrás del diálogo; no acredita foco.
Plain alcanza seis estados, después pierde ADB al abrir player; puerto5554
rechazado. ZIP plain bloqueado por proxy. No se atribuye a GPU/OOM sin evidencia.

El capturador retiene XML/PNG/eventos del diálogo y admite una sola recuperación
del ANR exacto de Pixel Launcher, verificando también el HOME predeterminado y
el botón del sistema. Relee la jerarquía; error persistente/recurrente o de
Aurelis/BitChord/desconocido conserva fallo y nunca se cierra automáticamente.
La pantalla exige actividad resumed y ventana con foco de su propio paquete.
Las pruebas Python aíslan GITHUB_STEP_SUMMARY: sus fixtures ya no insertan
resúmenes como «Capture never produced results» en la ejecución real. Se guardan
memoria, procesos, disco y kernel del host tras el emulador para la desconexión.

31 pruebas Python pasan en plain/glass; comprobada también la integridad de un
summary real preexistente. actionlint, py_compile y diff --check correctos. Dos
revisiones del mismo agente; cinco rutas de reparación, 54 acumuladas de 60.
No SDK/ABI/GPU, configuración de producción ni código de BitChord modificados.
Nueva validación Android/captura pendiente; no se declara arreglada la desconexión
ni cerrada la fidelidad visual. B07 no avanza.

### Pre-B07 — run151 aprobado; captura estática separada por material

b60c9d5e/run43726486169 SUCCESS: B01/B02/B03/B04/B05/B06 aprobados, verificados
por API. Se resuelven los fallos anteriores de pruebas; no se inicia B07 por ello.
Comparación 43726486047: logs ZIP oficiales leídos y CRC correcto. Por primera vez
MiniPlayer y reproductores vertical/horizontal del origen REACHED; el siguiente
dump de accesibilidad durante el paso de letras agota 60 s. Eso no demuestra una
caída del emulador ni identifica por sí solo la causa del bloqueo.

Se separan dos capturas con material plain/glass y reducción de animación activa
en ambas apps. Es configuración del emulador desechable, no producción ni edición
de BitChord. Cada modo conserva 36 estados exigidos, JSON, galería y resultado real;
fail-fast=false permite guardar la otra evidencia sin convertir un fallo en éxito.
No se certifican gestos/animaciones ni equivalencia por reducir movimiento. El timeout
ADB conserva también su salida parcial y la etapa distingue player/letras.
27 tests Python aprobados bajo ambos entornos de material, actionlint, py_compile y
diff --check; dos revisiones del mismo agente. Cinco archivos, 54 rutas acumuladas
de 60. Nueva comparación y CI pendientes; no Gradle/Android local.

### Reparación UI2 — run150 y comparación7

0a7dc77c/run43697636368: B01/B02/B05 SUCCESS; B03/B06 FAILURE, B04 SKIPPED.
ZIP de logs oficial descargado/verificado. Debug y Release ejecutaron414 tests,
cinco fallos en cada suite. Las ocho regresiones nuevas UI2 sí aprobaron ambas
variantes. Fallos: acceso antiguo a carpetas desde Settings raíz, búsqueda global
solo por texto, búsqueda local con etiqueta ambigua, resultados vacíos y pulsación
larga de favoritos después de aparecer el MiniPlayer. Se actualizan los accesos
sin retirar las comprobaciones, y el gesto físico exige un objetivo fuera del dock.
La búsqueda local tiene cabecera compacta y etiqueta «Buscar en esta lista»;
artistas/álbumes vacíos muestran Sin resultados. Se añade una regresión de ventana
baja. B03 imprime excepciones completas para leer el fallo real sin el ZIP de XML.

Comparación43697636356: MiniPlayer origen alcanzado; uiautomator termina255 sin
stderr. Este run no acredita por sí mismo muerte del emulador. adb omitía stdout
en su error: ahora lo conserva, con dos regresiones Python; árbol reintenta solo
errores transitorios de idle/raíz y falla ante desconexión u otros errores.
ZIP de capturas/XML bloqueado por proxy. No cambiar GPU, SDK, ABI, app de referencia
ni puertas por una causa no demostrada.25 tests Python aprobados, doble revisión
del mismo agente; ninguna ejecución Gradle/Android local. Validación nueva pendiente.
La unión completa permanece bajo60 rutas. No se inicia otra entrega funcional.

### Pre-B07 UI2 — páginas locales, categorías, iconografía y widgets

«Continúa y termina» autoriza completar las correcciones de la auditoría. Base
baedcf57: run43692222152 SUCCESS; comparación43692222178 FAILURE, logs leídos:
MiniPlayer sí alcanzado, player no verificado y ADB desconectado. Se corrige el
fallback que elegía el título más bajo de toda la lista: usa el título de la banda
de transporte verificada. No se gira una página que no sea el player; un estado
omitido sigue fallando. Logcat continuo conserva/imprime errores fatales si aparecen.
No se atribuye la caída del emulador a una causa que sus logs no demuestran.

Cabecera local responsive dentro de la lista, cinco pestañas/categorías con datos
existentes, duración total, acciones por pista, imágenes y filas accesibles.
Settings activo con búsqueda y rutas de categorías restaurables; perfil único,
preferencias/errores conservados. Home adapta el espacio de cuenta a perfil/datos
locales y tarjetas3:4; búsqueda separada de la cápsula en modo cristal; glifos
semánticos corregidos sin incorporar SF Symbols restringidos. Widgets Square,
Wide y Pill registrados, snapshot desde el mismo player, IO serializado y rechazo
de renders tardíos; sus botones abren el panel/controlan la sesión existente.
Ocho regresiones Android nuevas preparadas y pruebas existentes ajustadas al
flujo real, sin suprimirlas.23 tests Python aprobados y dos revisiones del mismo
agente. No Gradle/Lint/Android local. Nuevo CI y revisión visual pendientes;
no declarar paridad completa ni B12/B07 cerrados. Detalle PRE_B07_UI2_LOCAL.md.

### Corrección de UI1 — run148 y comparación5

Límite posterior del usuario:60 archivos distintos para estas correcciones.
Run43689886654/9aeefaaa: B01/B02/B05 SUCCESS, B03/B06 FAILURE y B04 SKIPPED.
ZIP oficial de logs descargado y CRC verificado; Debug y Release ejecutan406
tests cada uno, con un fallo en ambos: NavigationIntegrationTest.kt:54 espera
«Perfil del dispositivo», pero SettingsGroup usa uppercase(Locale.ROOT).
Se corrige el texto esperado y se exige además la ruta Settings; no se elimina
ni desactiva el test. Kover se omitió por fallar las suites, no causa raíz probada.
Comparación43689886636 alcanza MiniPlayer pausado del origen, pero no verifica
player y pierde ADB al girar: error closed y puerto5554 connection refused.
ZIP UI5 bloqueado por proxy; no se atribuye la caída a GPU/RAM ni a Aurelis.
Se añade logcat continuo conservado aun si muere el emulador; es diagnóstico,
no una reparación demostrada de esa caída.22 pruebas Python aprobadas, revisión
doble del mismo agente y diff --check; Android nuevo pendiente en Actions.
Tres archivos de reparación,15 distintos de UI1 y26 de la comparación acumulada.
No se inicia el siguiente grupo visual ni se declara toda la UI corregida.

### Pre-B07 UI1 — corrección de estructura común tras revisar UI-4

Autorización «Entonces soluciona todo eso». Auditoría real de36 capturas/XML del
ZIP UI-4 sobre d4fea913; API confirma principal43685163410 SUCCESS y UI43685163414
FAILURE. Cabecera/insets conectados, perfil local en cabecera, chrome persistente
en rutas, MiniPlayer encima de pestañas y altura inferior medida/teclado. El player
retira semántica y bloquea toques a controles subyacentes conservados para el dock.
Settings usa su glifo correcto; búsqueda recibe foco sin borrar consulta guardada.
Captura reinicia datos desechables por app/tema, iguala cristal/locale y verifica
transporte + sesión propia pausada; rechaza los cuatro falsos MiniPlayers de UI-4.
Veintidós tests Python aprobados, cuatro regresiones Compose preparadas para
Actions, sin Gradle/Android local. Dos revisiones del mismo agente. Quince rutas de
UI1,26 distintas al sumar toda la comparación≤30. Nueva validación Android/visual
pendiente. Ajustes completos, páginas, glifos/widgets y resto de contraste abiertos;
no cierre de toda la UI ni inicio de otra entrega B07. Ver PRE_B07_UI1_LAYOUT.md.

### Pre-B07 — compilación verificada; corrección de navegación de captura

Run43683456015/958b8c96 SUCCESS: seis fases B01–B06 aprobadas. Comparación43683456052
compila ambas apps y genera36 intentos, con19 pantallas no verificadas y error null.
Logs oficiales leídos: fallo de cobertura de navegación, no build. Lector corregido
para EditText vacíos, texto/descripción simultáneos y nodos agrupados; espera de
controles/scroll acotados, acceso perfil y etiquetas reales del player/letras.
Diecisiete tests UI pasan, py_compile/diff correctos, dos revisiones del mismo agente.
Dieciséis rutas acumuladas, sin cambios de app/SDK/ABI/firma. Validación Android
nueva pendiente; no afirmar paridad ni cerrar B07. Detalle PRE_B07_COMPARISON_RUN.

### Pre-B07 — diagnóstico de captura y activación B01–B06

Confirmado: run43679804645/db9c2207 SUCCESS en las seis fases. La comparación del
mismo SHA compila ambas apps y falla después en captura, no compilación. Logs ZIP
leídos; results.json bloqueado por proxy. Causa exacta aún no visible. Se imprime
etapa/error en logs/resumen; filtro del principal incluye el workflow UI para que
cambios solo en ese archivo también generen APK del mismo SHA. No se debilitan
pruebas/gates. Dieciséis rutas acumuladas; trece tests UI y nueve de grafo pasan,
actionlint/diff correctos; dos revisiones del mismo agente. Reproducción pendiente.

### Pre-B07 — reparación de comparación, nuevo CI pendiente

Run43679318406 falló en la preparación: V1 recibió el contador1 del workflow nuevo;
sdkmanager falló antes de Gradle. Logs ZIP oficial leído; sdk.log original no accesible
por proxy. La comparación reutiliza el APK B01 del pipeline principal del mismo SHA,
con run/intento/tamaños/hashes verificados, sin modificar V1. Catálogo oficial no
ofrece android-37 y sí android-37.0; nombre de instalación corregido, compileSdk37
intacto y salida SDK visible con tee/pipefail. No se afirma causa única del SDK.
Doce tests Python aprobados y dos revisiones del mismo agente, actionlint/py_compile/
diff correctos; quince archivos distintos acumulados. Gradle/Android solo Actions.
No B07, no capturas aún validadas. Detalle en PRE_B07_COMPARISON_RUN.md.

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
