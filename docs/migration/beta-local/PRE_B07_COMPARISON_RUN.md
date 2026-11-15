<!-- Aurelis © 2026. GPL-3.0-or-later -->
# Comparación ejecutable previa a B07


## Contexto de continuidad — ampliación a 62 autorizada

Actualización solicitada expresamente por el usuario tras autorizar el parche de
los dos contratos de idioma. Esta sección resume el estado verificado antes de
ese nuevo commit; los resultados posteriores deben consultarse en Actions por SHA.

- Repositorio: ed0668822/Aurelis-Music. Checkout activo de esta sesión:
  `/workspace/Aurelis-Music-b03-repair`; no asumir `/workspace/Aurelis-Music`.
- Única rama remota: `feature/bitchord-local-migration`; PR1 abierto, título
  `Aurelis Music Beta UI and components`. No main, merge, rebase, force push ni
  nueva rama remota/Release oficial. Commit/push del parche ya autorizados.
- Identidad local: ed0668822 / ed0668822@gmail.com; usuario, correo verificado y
  permiso push comprobados antes de esta entrega. Revalidar antes de commits futuros.
- Techo de esta corrección UI ampliado explícitamente de60 a62 rutas distintas,
  contando todo el historial de entrega desde `7da39774^`, pruebas/docs/config y
  reparaciones. Las demás entregas mantienen el límite general30 salvo autorización.
- Referencia local de solo lectura: `/workspace/BitChord`, commit fijo
  `b690dc146009d4815825fa4a20815ef2e4ee8898`. No tomar main indiscriminadamente.
- Preservar Aurelis, com.music.aurelis, perfil único local, AurelisPlaybackService,
  música local y Liquid Glass. Android Auto/TV/Wear/streaming y otros excluidos
  siguen fuera; B07 no avanza por cerrar este fallo.

### Cierre reportado por el usuario y causa demostrada

Perfil y Biblioteca→Ajustes llaman a la misma SettingsScreen. Los casos Android
reales de221bebf5/43749820078 reproducen Process crashed. El diagnóstico ampliado
299cdcd7/43751201636 registra APP CRASH(NATIVE), status11 y repetición hasta los
frames449/450 de RenderNode::prepareTreeImpl/SkiaDisplayList en libhwui.
La página se grababa completa en appBackdrop e incluía la cabecera de cristal que
volvía a leer esa capa: fuente y lector formaban un ciclo de RenderNode.

02bf90a5 coloca la fuente en el cuerpo/lista de Settings, Música local y Search,
con la cabecera como hermana fuera de la grabación. AppNavigationHost evita una
segunda fuente envolvente. No se desactiva cristal/Haze/refraction ni se cambian
SDK/ABI/firma. El perfil de solo lectura conserva una fuente segura.

El ensayo de02bf90a5 supera Perfil/recreación/Atrás sin el crash nativo, pero su
selector no podía pulsar el texto hijo Library. 7343104d usa la acción del ancestro
pulsable del mismo paquete y mantiene todas las comprobaciones. La misma entrega
distingue la etiqueta Tema del EditText que contiene esa palabra en el test.

**Evidencia positiva confirmada:** SHA7343104de98d9ef48081aa576f6be20170f87b64,
workflow43754259741, job132935852132 settings-navigation SUCCESS. Sus dos métodos
profileAndLibrarySettingsSurviveRecreation/profileAndLibrarySettingsPreserveRealPlayback
recorren ambos accesos, recreación y Atrás, preservan perfil y reproducción real
con WAV original. Exigen hardware renderer, Liquid Glass activo y blur no reducido.
No equivale a validación física en el teléfono ni a paridad visual de toda la app.

### UI, búsqueda, idioma y evidencia de comparación

38bfd26b filtra controles individuales en Ajustes, oculta grupos sin coincidencias,
expresa el vacío y reinicia scroll al cambiar consulta respetando restauración.
Categorías, perfil, nombre/foto, carpetas, letras y callbacks existentes se conservan.
Ajustes y selector de idioma usan recursos. Inglés/español:398 entradas existentes
más nuevas tienen claves/tipos/plurales/formatos coherentes;183 etiquetas inglesas
compatibles se adaptan del commit BitChord fijado. Procedencia en NOTICE y
LICENSE_AUDIT. Conservar GPL/avisos originales; no relicenciar material de terceros.
Otros idiomas declarados conservan fallback y otras pantallas todavía contienen
textos hardcoded: no dar toda la localización por terminada.

El comparador conserva modos plain/glass,36 estados exigidos por modo, reducción de
movimiento igual en ambas apps, dataset FLAC/art/LRC original, temas claro/oscuro,
locale y APK B01 inmutable del mismo SHA. No certifica animaciones en movimiento,
hápticos físicos, widgets colocados en launcher ni identidad por píxel.

Reparaciones de captura ya entregadas: errores ADB con stdout, timeouts acotados,
XML antiguo eliminado antes de cada dump y reintento solo por errores transitorios;
foco comprobado con dumpsys window completo; evidencia XML/PNG/ventana/actividad;
logcat continuo, motivos de salida y anotaciones JUnit concisas. Una recuperación
limitada del ANR confirmado de Pixel Launcher conserva la evidencia original;
ANR/crashes de Aurelis/BitChord o recurrentes conservan fallo.

ZIP UI9-glass:36 capturas tapadas por el launcher; no comparables. ZIP UI3 volvió
a aportarse, pero pertenece a958b8c96 y es histórico. No reaplicar sus diferencias.
Persisten fallos de captura en referencia/player/letras que deben comprobarse en
el run más reciente. No atribuirlos a GPU/OOM sin su evidencia concreta.

### Parche autorizado de dos pruebas y resultado que falta comprobar

Principal43754259793 de7343104d: B01/B02/B05 SUCCESS, B03 FAILURE, B04 SKIPPED y B06
FAILURE. Debug/Release seguían pendientes de aprobar el conjunto completo; el verde
del job Settings independiente no sustituye las puertas principales.

El parche nuevo cambia exactamente estos contratos:
- LibraryUpdatesTest: artista de álbum compilado se compara con el recurso
  library_various_artists del idioma activo, conservando IDs/álbumes/artistas.
- PlaybackIntegrationTest: el error de archivo se compara exactamente con
  playback_file_missing, conservando cola, retry/preparación y recuperación.

No se elimina ninguna prueba, no se fuerza idioma en producción y no se reduce
ninguna puerta. La cabecera Aurelis se añade al test propio que carecía de ella.
Ambos nuevos archivos llevan el acumulado de60 a62. PROGRESS_TRACKER también
registra la autorización; estas dos páginas ya pertenecían al inventario.

Dos revisiones del mismo agente, no independientes. Local: lectura/contratos,
parser Kotlin y git diff --check; no Android/Gradle/Lint/APK local. Las34 pruebas
Python del capturador ya habían aprobado ambos materiales; el parche no las cambia.
El siguiente paso obligatorio es comprobar Debug/Release, Kover y B01–B06 del nuevo
SHA, registrar resultado real y corregir fallos del mismo bloque antes de avanzar.
La comparación visual, gestos, widgets y validación física siguen abiertos.

### Herramientas y preservación del entorno

Metadatos API y ZIP de logs de runs completos funcionan; ZIP de artefactos y logs
individuales pueden redirigir a dominios Azure bloqueados con403. No evadir proxy/
TLS ni exponer tokens o enlaces firmados. Usar Jobs/check-runs/anotaciones; las
anotaciones tienen límite4096, de ahí el resumen JUnit al principio. Solicitar el
artefacto adecuado al usuario si es imprescindible y sigue inaccesible.

Evidencias fuera del checkout en `/workspace/aurelis-validation/settings-repair`:
propuesta original, patches revisados, logs ZIP oficiales, trazas nativas,
anotaciones, inventario y validaciones remotas. Pueden no transferirse a otro
entorno. Preservar `.gradle/`, `app/.cxx/` y `app/build/` no seguidos; no añadirlos.
Leer AGENTS/SECURITY y planes reales antes de futuras modificaciones; no repetir
entregas terminadas ni dar B07/B12/B13 por cerrados a partir de esta reparación.


## Corrección del preflight después de cb83c974

Principal153/43742631853 falla antes de Gradle en la auditoría obligatoria de
workflows. Logs oficiales confirman actionlint rechazado; el ZIP del reporte está
bloqueado por proxy. Reproducción local con actionlint1.7.12 + ShellCheck0.10.0:
SC2024 en la redirección del nuevo `sudo dmesg`. La revisión anterior solo ejecutó
actionlint porque ShellCheck no estaba instalado localmente; ese alcance era
insuficiente para reproducir el preflight de Actions.

La redirección se sustituye por `sudo dmesg --ctime |& tee ... > /dev/null`, sin
supresión ni cambio de condiciones del emulador. Stdout/stderr quedan en kernel.txt.
La auditoría F5 imprime actionlint.log si falla y conserva su salida1. Todos los
workflows pasan actionlint con ShellCheck; script F5 pasa ShellCheck y bash -n;
diff --check correcto. Dos revisiones del mismo agente; cuatro rutas de reparación,
55 acumuladas de 60. No se repiten pruebas Python sin cambios ni se ejecuta Android
local. CI y recuperación del launcher siguen pendientes del siguiente SHA.

El ZIP UI-3 vuelto a aportar es histórico: SHA958b8c96, principal43683456015,
anterior a UI1/UI2 y sus reparaciones. CRC válido, SHA256
`00f979fe9d0ffca7a1a2e56b583ed7c9199fde8d1872375b02525a6e3cbae7e3`.
Sus capturas no describen el estado actual y no justifican reaplicar cambios ya
entregados ni afirmar que reproductores/letras se comprobaron correctamente.

## Reparación posterior a UI9

Principal152/43739187894 del SHA0786e3bc: seis fases SUCCESS. Comparación9/
43739187866: ambos jobs fallidos. El ZIP glass original aportado por el usuario
abre/CRC válido; SHA256 `95199fc9e3d68bdb2c1f9751222d5ba20e693516b7bf5cfc498fec3fa0e8f111`.
Los 36 XML/PNG muestran el diálogo Android «Pixel Launcher isn't responding»
sobre las apps. No se alcanzó ningún estado. La causa del launcher no se conoce;
no se atribuye ese ANR a Aurelis o BitChord. Plain alcanza seis estados y pierde
ADB al abrir player: exit255, puerto5554 rechazado. Logs oficiales leídos; descarga
de su artefacto bloqueada por proxy, sin causa host/GPU/OOM demostrada.

El capturador conserva el XML/PNG original en `system-dialogs/<n>` y eventos en
`system-dialog-events.json`/`results.json`/resumen. Solo admite cerrar una vez el
ANR exacto de Pixel Launcher, tras verificar el HOME nexuslauncher y el botón
Android `aerr_close` habilitado, con límites válidos leídos del XML. Relee el
árbol tras cerrar. Si el diálogo persiste/reaparece, el run falla. ANR/crash de
las apps comparadas o un diálogo desconocido nunca se cierra automáticamente.
La comprobación de foreground exige también `mCurrentFocus` del paquete; una
actividad resumed detrás de otra ventana deja de contar como foreground.

Los fixtures de unittest aíslan GITHUB_STEP_SUMMARY. Los resúmenes repetidos de
«Capture never produced results» y «Other screen» procedían de esos fixtures,
no de otro intento real de captura. El informe conserva el resultado auténtico.
Un paso `always()` guarda memoria/disco/procesos/kernel del host tras la salida
del emulador para investigar la desconexión, sin cambiar su GPU/SDK/ABI ni ocultar
su fallo. Se mantienen ambos materiales, los 36 estados exigidos por modo y los
APKs originales. No se modifica producción ni el checkout de BitChord.

31 regresiones Python pasan bajo plain/glass; un summary centinela permanece
intacto durante toda la suite. actionlint, py_compile y diff --check correctos;
dos revisiones estáticas del mismo agente, no independientes. Cinco rutas de
esta reparación, 54 acumuladas de 60. Android/Gradle solo Actions. Validación del
nuevo SHA pendiente: esto no demuestra reparación de la pérdida de ADB ni paridad.

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
