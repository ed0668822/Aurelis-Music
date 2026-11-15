<!-- Aurelis © 2026. GPL-3.0-or-later -->
# Comparación ejecutable previa a B07

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
