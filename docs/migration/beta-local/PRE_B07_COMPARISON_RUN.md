<!-- Aurelis © 2026. GPL-3.0-or-later -->
# Comparación ejecutable previa a B07

## Condición de captura vigente — después de run151

El principal 43726486169/b60c9d5e terminó SUCCESS en las seis fases. La comparación
43726486047 sí alcanzó ambos reproductores del origen, pero su siguiente lectura
de accesibilidad durante letras agotó 60 s. Logs oficiales verificados. No hay
evidencia suficiente para atribuir el timeout a GPU, memoria o muerte del emulador.

La comparación ejecuta ahora dos jobs independientes `plain` y `glass`, con
`reduce_animation=true` en las preferencias desechables de ambas apps. El origen
usa esa misma clave; su modo high_performance está desactivado por defecto y no
anula esta preferencia. Material, tema, locale y reducción quedan en results.json.
Se conservan SDK/ABI/GPU, APKs originales y configuración de producción. No se
modifica el checkout de BitChord ni se suprimen estados. Cada job exige 36 estados;
si cualquiera falla, el workflow conserva el fallo. No certifica movimiento/hápticos
ni el aspecto del estado animado. Es una prueba de diseño estático en ambos materiales.

Artefactos: `Aurelis-BitChord-UI-<n>-plain` y `Aurelis-BitChord-UI-<n>-glass`.
`fail-fast=false` permite terminar y conservar ambas galerías. ADB guarda stdout/
stderr parcial en timeouts y registra la etapa exacta. No se supone que esta nueva
condición resuelva el bloqueo hasta comprobar el siguiente run. 27 regresiones
Python pasan tanto bajo plain como glass; actionlint/py_compile/diff correctos,
dos revisiones estáticas del mismo agente, 54 archivos acumulados ≤ 60.

Autorización «Continúa» tras propuesta de compilar/instalar BitChord y Aurelis en
Actions para compararlos. Base Aurelis b8ee8147; run43670279287 SUCCESS confirmado.
No implementación B07, Android Auto/TV/Wear excluidos. Incorporar los cambios de
alcance/documentación ya preparados; no reabrir otras entregas.

Manifiesto previsto, máximo30 rutas acumuladas: siete documentos locales previos
(DECISIONS, IMPLEMENTATION_BLOCKS, MASTER_PLAN, PLAN_CONTINUATION, PROGRESS_TRACKER,
UI_PARITY_CHECKLIST, PRE_B07_UI_PARITY); este informe; workflow ui-parity-comparison;
scripts ui_parity.py, ui_parity_fixtures.py, test_ui_parity.py; NOTICE y LICENSE_AUDIT.
Catorce rutas previstas. Ajustar manifiesto antes de ampliar alcance.

Proceso separado del pipeline B01–B06; referencia de solo lectura fijada a
b690dc146009d4815825fa4a20815ef2e4ee8898. Sin secretos/firma de producción, sin
publicar Release ni modificar referencia. Compilar solo módulo app, sin bundleWear
mediante propiedad soportada por el origen. Conservar SDK/ABI declarados de ambas.
Seleccionar salida ARM64 original del origen; Aurelis usa APK inmutable de B01.

Mismo emulador API35, tamaño/densidad/escala y música sintética con carátula/letras.
Evidencias deben distinguir pantalla alcanzada, intento bloqueado y revisión visual
pendiente. Diferencias de marca, fuentes autorizadas y alcance no se ocultarán en
imágenes. Capturas comparables no equivalen a identidad ni validación física.
No afirmar paridad si el build del origen, arranque, navegación o recursos fallan.

## Implementación y validación

Workflow ui-parity-comparison.yml se activa por cambios de sus scripts/workflow en
la rama autorizada, o manualmente una vez disponible en GitHub. No altera B05 ni la
puerta B06. La referencia se compila con :app:assembleProdDebug, bundleWear=false,
sin cambiar SDK/filtros ABI/firma; el selector usa output-metadata y verifica ZIP,
paquete y ABI. SHA del checkout y APK registrados. APK referencia retenido tres días
como transferencia entre jobs; galería final catorce días, sin Release oficial.

Nueve estados por app/tema (36 intentos): inicio, biblioteca, ajustes, búsqueda,
lista local, MiniPlayer, player vertical/horizontal y letras locales visibles.
Cada navegación usa texto/descripción del árbol real, sin coordenadas adivinadas.
El gesto se ejecuta en el centro del nodo encontrado; XML/PNG y estado se conservan.
Etiquetas/accessibilidad distintas pueden bloquear la automatización: un estado
no alcanzado no demuestra por sí mismo un defecto visual de la app. El proceso
falla si falta algún estado; un éxito solo acredita captura, nunca paridad visual.

Fixtures FLAC elegidos porque ambos lectores actuales admiten Vorbis LYRICS. Tres
canciones,60s, portadas600px originales y LRC incrustado/mismo archivo lateral,
indexación MediaStore verificada. Capturas con red por defecto desconectada,
onboarding remoto omitido solo en preferencias desechables de la referencia.
Temas explícitos; fuentes por defecto de cada app sin ocultar la excepción de
licencia/tipografía autorizada. Estado/posición real de MediaSession retenidos:
no se supone que los tiempos de playback o carga de imágenes sean idénticos.

Limitaciones: no instala widgets en el launcher ni valida animaciones/gestos,
accesibilidad o sonido físico. Solo captura una viewport por estado; no revisa el
orden completo de todas las filas de ajustes ni todas las pantallas de la app.
El alcance global pedido por el usuario sigue abierto; este es el primer conjunto
de evidencia ejecutable, no su cierre. Un fallo al compilar la referencia deja su
log y bloquea la comparación, sin sustituirla por un APK de otra versión.

Validación local: nueve tests Python (selección/hash/ABI/rutas, XML, foreground,
PNG y evidencias incompletas), py_compile, actionlint1.7.12 y git diff --check.
Fixtures reales generados con FFmpeg; ffprobe confirmó FLAC, imagen PNG adjunta,
título/artista/álbum y LYRICS. No Gradle/Lint/Android local. Dos revisiones estáticas
del mismo agente: primero identidad/artefactos/permisos/aislamiento de la referencia;
después navegación/evidencia, errores, límites y documentación. Resultado de la
nueva ejecución GitHub pendiente; no publicar afirmación de identidad.


## Reparación de la primera ejecución de comparación

Run43679318406/7da39774 FAILURE: Aurelis/B01 falla en V1 con «Invalid green-build
count or run number»: el workflow nuevo tiene run_number1, por debajo del ancla101
válida del pipeline principal. No se altera V1. La comparación localiza el B01 del
pipeline principal con mismo SHA/rama/workflow y copia su artefacto antes de limpieza.
Verifica SHA, run, intento, formatos, ZIP, ABI, tamaños y hashes, conserva bytes y
registra la identidad primaria en el resultado. No recompila Aurelis en este proceso.
Un nuevo script ui_parity_handoff.py aumenta el manifiesto acumulado a15 rutas≤30.

La referencia falla antes de Gradle en sdkmanager. sdk.log queda en el artefacto;
su descarga está bloqueada por proxy a productionresultssa15.blob.core.windows.net.
El ZIP de logs completo sí se descargó y validó, pero no contiene el texto redirigido
a sdk.log. El catálogo oficial repository2-3.xml consultado confirma que no existe
el paquete platforms;android-37 solicitado y sí platforms;android-37.0, API37.0 base.
Se usa ese nombre oficial, sin editar compileSdk37 del origen, filtros ABI o firmas.
Se muestra sdkmanager mediante tee con pipefail para conservar diagnóstico y fallo
real. No se afirma que esta sea la única causa SDK sin leer su log original.

Tres regresiones Python nuevas de transferencia entre runs: contador UI1 ajeno al
primario144, identidad incorrecta y bytes alterados. Doce tests totales aprobados,
actionlint, py_compile y diff --check correctos. Dos revisiones estáticas del mismo
agente: identidad/transferencia/retención; después workflow, errores y documentación.
No Gradle/Android local. Nueva comparación y build de referencia pendientes de CI;
no declarar similitud o pantallas verificadas con estos cambios de preparación.


## Segunda reparación — diagnóstico visible y activación del pipeline

Run43679804645/db9c2207 SUCCESS, seis fases B01–B06; no falta compilación de ese SHA.
Run43679318341 anterior CANCELLED tras nuevo push, conforme a concurrency del principal.
Run43679804648 de comparación: aurelis/reference SUCCESS; compare FAILURE en captura.
El emulador arrancó y el script salió1 tras5s; causa guardada solo en results.json,
sin imprimirla. Informe bloqueado por proxy, logs ZIP oficial sí leído/verificado.
No atribuirlo a GPU/adb/red ni a navegación sin leer el error concreto.

Se añade diagnóstico de etapa/error a stdout y resumen Actions, manteniendo JSON y
fallo real; stderr de comandos ADB limitado. Se corrige además el filtro de paths
principal para que un cambio solo en ui-parity-comparison.yml también lance B01–B06:
la comparación necesita un APK del mismo SHA. No se cambian gates ni versiones.
Dieciséis rutas distintas acumuladas≤30. Trece tests Python UI y nueve de grafo
aprobados; actionlint/diff correctos. Dos revisiones estáticas del mismo agente.
Esta preparación permite reproducir y leer la causa de captura sin depender del ZIP;
no representa aún una reparación demostrada de ese error Android ni paridad visual.


### Reproducción con diagnóstico y corrección de selectores

Run43683456015/958b8c96 SUCCESS en B01–B06, confirmado por API. La compilación
principal sí se ejecuta. Run43683456052: ambas apps compilan, captura realiza36
intentos (17 alcanzados/19 no verificados), error global null y capture_complete=false.
Ya no es un fallo de build/preparación en esta reproducción. Los logs ZIP oficiales
fueron descargados y verificados; ZIP de imágenes/XML sigue bloqueado por el proxy.
No se ha visto ni revisado visualmente ninguna captura desde este entorno.

Defectos estáticos confirmados en el lector de UI: descarta EditText vacíos al exigir
label y pierde content-desc cuando hay text; los nodos de MiniPlayer con texto
agrupado no pueden coincidir con un título individual. El flujo firmado fuera de
cuenta de BitChord abre Settings directamente desde Switch account; no requiere
pulsar otra vez el título. Aurelis abre letras mediante «Ver Letras» y el reproductor
origen expone «Up next». Estos nombres se contrastaron en los archivos reales.

Corrección: conservar ambos atributos y sus líneas semánticas (sin coincidencias
parciales arbitrarias), mantener EditText vacíos, esperar controles habilitados,
buscar dentro de contenedores scrollables (hasta cuatro observaciones), acceso de
perfil directo en referencia y marcadores/acción de letras ajustados. Los gestos de
scroll se calculan dentro de bounds del contenedor real; no hay taps a posiciones
fijas. La captura conserva su requisito de36 estados, orientación/foreground y
letras locales visibles. Fallos siguen rojos. Se incluyen etiquetas de estados no
alcanzados en los logs para diagnóstico sin ZIP; no son verificación de fidelidad.

Diecisiete pruebas Python UI aprobadas, cuatro nuevas de accesibilidad/scroll/espera;
py_compile/diff correctos. Dos revisiones estáticas del mismo agente, primero
semántica y consumidores, después límites/errores/evidencia. Dieciséis rutas distintas
acumuladas≤30. Android solo Actions; nueva validación pendiente. No se inicia B07.
La causa exacta del resultado inicial run43679804648 no se ha recuperado del ZIP;
no atribuirla retrospectivamente a estos defectos ni afirmar que se reprodujo igual.
