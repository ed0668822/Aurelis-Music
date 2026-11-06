# Progreso QA complementario

Base b179fde, árbol inicialmente limpio; autorización recibida para QA00/QA01.
Plan principal beta-local preservado; B03 no iniciado. Dos pasadas del mismo agente.

| Unidad | Estado |
|---|---|
| QA00 | Entorno/plan documentados |
| QA01 | Corrección verificada en pruebas automatizadas; Android físico pendiente |
| QA02–QA05 | Planificados; no iniciados/no autorizados |

Pruebas Android reales: instalación/arranque no ejecutados; sin dispositivo.
Los resultados y commit/PR se completarán con evidencia, no con suposiciones.


## Primera revisión QA01

Revisión de causa/arquitectura completada: conexión antes de medida confirmada
por prueba roja del nodo real, no solo hipótesis estática. Producción limitada a
tres archivos de backdrop; guard compartido evita acceso a NaN/Unspecified/0/∞.
El primer dibujo medido mantiene la observación de preferencias; reset limpia
efectos/caché. Excepciones con dimensiones válidas siguen propagándose.
Sin dependencias, ABI, JNI, autenticación ni cambios de diseño. R en ejecución.

Segunda revisión: regresiones completas y comprobación del plan/artefactos en curso.
Mismo agente en dos pasadas; Android real no ejecutado. B03 sin iniciar.


## Cierre local QA00/QA01

R: BUILD SUCCESSFUL in 4m 56s.171 casos debug y171 release,0 fallos,
errores u omitidos. Lint:224/223 advertencias debug/release,0 errores.
Ambos APK: firma/ZIP correctos,10 textos legales idénticos, fuente íntegra y12
bibliotecas ARM64 conservadas. Hashes/firmante público en QA01_EVIDENCE.json.

Revisión1/2 completadas en pasadas separadas del mismo agente. El plan maestro,
bloques, LICENSE/NOTICE, ABI/Gradle, manifiesto y workflow son idénticos a la base
(hashes registrados). B03 sin iniciar; QA02–QA05 no implementados.

Instalación/arranque en Android y GPU Redmi: NO EJECUTADOS por falta de dispositivo.
La corrección reproduce y resuelve el defecto en Robolectric; no certifica que
toda la aplicación funcione en el teléfono ni infraestructura Android completa.
Publicación/CI y commit se registran al cerrar la entrega en el mismo PR1.


## Entrega QA00/QA01

Commit de código/evidencia: `c52d1ddab21bfb02a0c26368f3080a318fc8f294`.
Rama existente: feature/bitchord-local-migration. PR1:
https://github.com/ed0668822/Aurelis-Music/pull/1
Título: Aurelis Music Beta UI and components, sin cambios. Autor/committer
ed0668822 <ed0668822@gmail.com>, configuración exclusivamente local.

Los APK probados se construyeron desdeb179fde más los archivos con SHA guardados
en QA01_EVIDENCE, luego registrados en este commit de código. Este checkpoint es
documental y no cambia la aplicación validada. Se publican juntos para evitar
duplicar ejecuciones CI por commit; comprobar refs de rama/PR al reanudar.
CI del SHA de entrega: pendiente de consulta tras publicación, sin heredar verde
histórico. No merge, force push, otra rama, cambios de firma o servicio de pago.

Siguiente: QA02 necesita autorización y Android ARM64 compatible; instalación y
arranque físico siguen NO EJECUTADOS. Infraestructura completa QA05 no implementada.
B03 detenido; plan principal y orden de bloques intactos.


## Validación final QA00/QA01 — 2026-11-06T03:10:21.277553+00:00

Run126 de2742913 terminó Success,10m23s; incluye c52d1dd y publicó dos artefactos.
58 pruebas dirigidas nuevas ejecuciones correctas; APK locales/hashes/firmas
verificados. Ver QA01_FINAL_VALIDATION.md/.json y QA01_FINAL_DEVICE_CHECK.json.
API GitHub Forbidden y descarga web404: bytes de APK CI no inspeccionados.
Arranque, navegación/Glass/permisos/audio en Android y Logcat NO EJECUTADOS.
No hay Android operativo disponible; no confundir RecordingPlayer ni Robolectric
con audio/arranque real. No nuevos fallos reproducidos ni cambios de producción.
Plan principal intacto; QA02 y B03 no iniciados. Entrega documental en el mismo PR;
CI de esa revisión se consulta separadamente sin modificar la app probada.

CodeQL del mismo2742913 también Success (run42152988508,1m6s).


CI de la entrega documental b8b8910: Build127 Success (10m32s) y CodeQL Success
(1m11s), URLs/digests en QA01_DELIVERY_CI.json. Este resultado no acredita los
cambios posteriores de B03. El usuario indicó después «Mejor continúa con el
plan ... pero si prueba todo lo que puedas»: se retoma el principal por B03,
sin QA02; se preserva la evidencia y la limitación de Android real.


## CI01 — mejoras autorizadas antes de B04

«Aplica los cambios antes de B04»: workflow/reportes modificados, app y plan
principal intactos. Informe separa inventario/diagnósticos/tareas/cache y etapas
omitidas; solo publica APK tras todas las validaciones. Firma/ZIP/ABI-ELF/identidad/
licencias y procedencia por SHA; secretos originales conservados. Acciones Node24
fijadas por SHA y permisos mínimos. Ver CI_WORKFLOW.md y evidencia estática.

18 pruebas offline Python correctas, actionlint sin diagnósticos y11 pasos Bash
con sintaxis válida. No se ejecutó Gradle ni se compiló la app localmente.
Por preferencia vigente, publicar/avisar y esperar el resultado que comunique
el usuario desde GitHub; no esperar CI ni avanzar automáticamente. El nuevo
workflow aún no se declara aprobado en GitHub. QA02 y B04 no iniciados.


## LEGAL01 — textos legales antes de B04

Autorización: «Necesito que mejores el texto de notice y licencia antes de B04».
NOTICE reorganizado y aclarado; guía informativa LICENSES/README.md en español.
LICENSE oficial y textos íntegros/atribuciones de terceros preservados; se corrigen
resúmenes no acreditados sin modificar licencias. Auditoría actualizada en
beta-local/LICENSE_AUDIT.md; dos revisiones documentadas en LEGAL_TEXT_REVIEW.md.
No cambios de aplicación, Gradle, ABI, firma, workflow o plan principal.
Sin compilación local; empaquetado del próximo APK pendiente de GitHub.
Publicación en la misma rama/PR; esperar el resultado comunicado por el usuario.
B04 y QA02 no iniciados. Pendientes legales históricos siguen abiertos.


## GLASS01 — material BitChord antes de B04

Autorización expresa tras pedir el mismo Liquid Glass que BitChord.
Comparación real en b690dc1; parámetros y programas AGSL conservados.
Barra activa usa liquidGlass completo explícitamente, sin recorte/sobreborde adicional;
MiniPlayer deja visible el material en vez de taparlo con fondo opaco.
Captura de página opaca como referencia; página, fila y lente respetan la misma
política de desactivación/reducción de desenfoque. Ruta ligera restaurada para
fondos estables. Protecciones QA01 y regresiones de tamaño/attach intactas.

Dos pruebas añadidas para preferencias y controles: NO EJECUTADAS localmente.
Dos revisiones estáticas del mismo agente; Gradle/compilación/pruebas de Android
pendientes exclusivamente en GitHub. Evidencia en GLASS01_MATERIAL_PARITY.md/.json.
Sin dispositivo/GPU Redmi: paridad visual y arranque real pendientes, no declarados
verificados. El plegado/separación de Search y el diseño completo del MiniPlayer
siguen pendientes en los bloques principales; no se afirma paridad total.
Misma rama/PR1; esperar resultado del usuario. B04 y QA02 no iniciados.


## B04 — pruebas preparadas para CI, no ejecutadas localmente

Ver ../migration/beta-local/B04_EXPLORE_SEARCH_LIBRARY.md/.json y PROGRESS_TRACKER.
Seis casos nuevos más extensión de restauración; ejecución/compilación en GitHub
pendientes del commit publicado. Sin Android real ni sonido/Logcat verificados.
GLASS01 y QA01 conservados. No QA02. B05 autorizado, esperando resultado de B04
comunicado por usuario y cierre de su validación. No confundir preparación con
pruebas aprobadas ni cambios funcionales con paridad visual completa.


## B05 — controles compartidos/calidad; pruebas pendientes

B05_PLAYER.md/.json en migration/beta-local registra dos revisiones estáticas y
seis pruebas nuevas preparadas. NO EJECUTADAS; no Gradle local. Referencias de API
verificadas por lectura de bytecode. CI de B04 y B05 pendientes de confirmación.
No Android real/rotación/audio/Logcat. Servicio/QA01/GLASS01 intactos. Dock/morph del
panel y fidelidad visual siguen pendientes de B05; no cierre completo ni B06/QA02.


## B05.2 — movimiento/dock, entrega para GitHub

Ver ../migration/beta-local/B05_2_PANEL_DOCK.md/.json. Siete casos nuevos y
actualización del swipe anterior preparados, NO EJECUTADOS. Doble revisión
estática y lectura de API con javap; no Gradle local, APK ni dispositivo.
No se hereda verde de B04/B05 anteriores. Rotación/GPU/Logcat/audio/RTL real
pendientes. QA01/GLASS01/servicio/plan intactos. No B06 ni QA02. Publicar/avisar y
esperar resultado del usuario antes de seguir con validación/correcciones.


## CI02 — fallo run135 antes de B06

Confirmado error Kotlin maxWidth en NowPlayingScreen.kt:384 del commit0ec482b.
Corrección mínima de scope; no retirada de panel/dock/Glass. Workflow compila primero
Kotlin/Java/recursos, no repite fases dependientes si falla; reporta entradas de
tareas y estados por archivo, errores y ausencia de evidencia honestamente.
21 pruebas Python correctas, actionlint/Bash/Groovy estáticos correctos. No Gradle
ni Android local; nueva compilación/integración en GitHub PENDIENTE. Evidencia y
límites en CI02_BUILD_FAILURE.md/.json y CI_WORKFLOW.md. Dos revisiones del mismo
agente. Misma rama/PR, sin firma/ABI/servicio/plan alterados. No B06 ni QA02.

Commit de corrección/CI02: `0e36d0f2854be5a79061bc12c7dd73669d3da7a7`. Publicación en PR1; compilación
GitHub del nuevo código pendiente. No B06 hasta recibir su resultado.


## CI03 — import de recursos en PlayerIntegrationTest

Adjunto del usuario y run42232423184 del commit e687ee2: compile/lint success,
tests failure por R.string en cuatro líneas del archivo de pruebas; APK omitido.
Se añade únicamente import explícito com.music.aurelis.R, sin cambiar aserciones
ni gate/workflow/app. Dos revisiones estáticas; cuatro nombres de referencia
comprobados contra recursos existentes. No Gradle/Android local. CI nuevo pendiente.
Evidencia en CI03_TEST_RESOURCES.md/.json. No B06 ni QA02; mismo PR1 y rama.

Commit de corrección CI03: `2d7e8632e467d36d75adbd15baa800f9a036eb2a`. Mismo PR1; resultado nuevo de
GitHub pendiente, sin Gradle/Android local ni avance B06/QA02.


## CI04 — aserción de búsqueda con alcance incorrecto

Recibido log completo del paso11:200 pruebas/variante,1 fallo por variante en
localSearchOpensAnAlbumAndRecordsTheSubmittedQuery:75. La aserción global
confundía la canción del MiniPlayer con resultados de búsqueda. Matcher acotado
a la lista etiquetada, con controles positivos Todo/Álbumes y reproducción intacta;
apertura de álbum e historial siguen exigidos. No se desactiva ninguna prueba.
Anotaciones JUnit públicas y reconocimiento del resumen Gradle multi-failure.
23 pruebas de utilidades correctas; UI/Android/Gradle pendientes exclusivamente en
GitHub. Dos revisiones estáticas del mismo agente. Evidencia en CI04_INVESTIGATION.
Mismo PR1, sin workflow/gates/servicio/Glass/plan alterados. No B06/QA02.

Commit de corrección CI04: `6a57325f45a3c4f1e044c3d44a2ed05dc0b5fc80`. Misma rama/PR1; nueva
verificación Android pendiente en GitHub. No B06/QA02 ni compilación local.


## CI05 — concurrencia de Main en pruebas de playback

Run138 del commit3128d9c: dos casos release fallan con Dispatchers.Main is used
concurrently with setting it. Las anotaciones públicas de CI04 permitieron conocer
la causa sin descargar el artefacto; API REST aún Forbidden. Cleanup del harness
espera scopes actuales y reemplazados antes de resetMain, usando mismo scheduler.
Diez casos/aserciones conservados, una regresión IO→Main añadida. No retries/skips
ni cambios de producción/CI. Doble revisión estática; no Gradle/Android local.
Evidencia y límites en CI05_MAIN_DISPATCHER.md/.json. Validación nueva pendiente de
GitHub, no se declara eliminado el fallo intermitente. Mismo PR; no B06/QA02.

Commit de corrección CI05: `9d053c082958558683a9b169737ed4fb2769a190`. Mismo PR1/rama; suite nueva
pendiente de GitHub. Sin compilación local ni avance B06/QA02.


## Cierre CI05 observado al iniciar B06 autorizado

Run139 https://github.com/ed0668822/Aurelis-Music/actions/runs/42244895407
SUCCESS; job127984422069 enlaza el HEAD cefe533dd1e256c67a720a97a241e95789c1fa9d.
Evidencia: páginas públicas, estado «completed successfully» y enlace de commit.
No descarga REST de logs ni prueba física. El bloqueo CI anterior queda superado
para continuar B06 autorizado; QA02 no iniciado. B06.1 aporta21 casos preparados,
sin ejecución local; su CI nuevo pendiente del resultado comunicado por usuario.
Ver ../migration/beta-local/B06_1_QUEUE.md/.json para mantener separado el alcance.


## CI06 — compilación de pruebas B06.1

Run140/HEAD85100dd falla al compilar pruebas: token Media3 construido con tipo
incorrecto y CustomActions tratado como AccessibilityAction en vez de lista.
Anotaciones públicas recuperadas; REST Forbidden, sin logs completos descargados.
Dos tests corregidos; métodos/asserts conservados, acción en hilo UI. Doble revisión
estática del mismo agente, APIs contrastadas con javap y diff correcto. Producción,
workflow/gates, Glass/QA01 y plan sin cambios. Compilación/tests nuevos PENDIENTES
DE GITHUB; no Android local ni arranque verificado. Ver CI06_TEST_COMPILATION.md/.json.
Mismo PR1; publicar, avisar y esperar resultado del usuario. No B07 ni QA02.


## CI07 — gate causado por pruebas de cola

Run141/390c759 ejecuta pruebas, pero fallan dos casos PlayerIntegrationTest:
se elimina la fila visible incorrecta al estar oculto el historial; el tirador
solo existe en árbol unmerged. Test corregido con historial explícito, selector
por UUID y cuatro búsquedas unmerged. Aserciones originales conservadas y estado
reforzado. Solo test/documentación; gate/app/Glass/plan intactos. Evidencias en
CI07_QUEUE_TEST_TARGETS.md/.json. Doble revisión estática; tests/compilación nuevos
pendientes exclusivamente de GitHub. Publicar mismo PR y esperar aviso del usuario.
No Android físico ni inicio de B07/QA02.


## CI08 — limpieza de advertencias, entrega1 autorizada

Base c4a627d, cuyo run142 SUCCESS se observó en revisión previa. De las20 entradas
proporcionadas por el usuario se atienden9:4 Haze,3 convenciones Modifier,1 when
y1 anotación API33. Siete archivos de producción; detalle/hashes/revisiones en
CI08_WARNING_CLEANUP.md/.json. Dos pasadas estáticas del mismo agente; sin cambios
de tests, dependencias/SDK/ABI, motores, shaders/QA01, workflow/gates o plan.
Compilación/lint/tests nuevos PENDIENTES DE GITHUB, no ejecutados localmente.
ONNX16KB y otras11 entradas pendientes; no se ocultan. Publicar mismo PR y esperar
resultado del usuario antes de siguiente entrega/autorización. No B07 ni QA02.
