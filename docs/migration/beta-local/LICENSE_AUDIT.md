# LICENSE_AUDIT — beta local

## Alcance de B00

Auditoría documental/estática y reutilización de la evidencia histórica en
[LICENSE_AUDIT anterior](../LICENSE_AUDIT.md) y
[DEPENDENCIES_RELEASE.tsv anterior](../DEPENDENCIES_RELEASE.tsv). No certificación
legal, auditoría de vulnerabilidades ni revisión semántica de los1.396 archivos.
No se cambia LICENSE ni NOTICE en B00: no se incorpora código de terceros nuevo.
NOTICE sí deberá actualizarse cuando cada componente se adapte de verdad.

| Elemento | Evidencia | Tratamiento / bloqueo |
|---|---|---|
| Aurelis | LICENSE GPLv3 y NOTICE3-or-later | Mantener GPL-3.0-or-later; copyright propio solo en aportes propios |
| BitChord | LICENSE y avisos seleccionados | Conservar autor, ref fija y modificaciones por archivo |
| Orchard | TrackAnalysis/TransitionPolicy explican AGPL3-or-later y combinación GPL | No relabelar; conservar obligaciones AGPL y fuentes |
| FFmpeg/NextLib | Auditoría previa POM NextLib GPLv3; configuración completa del binario no acreditada | No asumir todo LGPL; acreditar fuente correspondiente/versiones/build antes de nueva distribución final |
| DST | COPYING.LGPL-2.1 y NOTICE upstream | Conservar textos y condiciones |
| Mornye bit-perfect | README nativo origen declara LGPL-3.0-only, copyright zarzet2026 | Examinar cabeceras por archivo y obligaciones de combinación/distribución |
| libusb | Origen1.0.27 LGPL2.1-or-later | Fuentes, textos y enlace/distribución verificables |
| Oboe | Origen1.11.0 Apache2.0 | Pin/checksum, licencia y avisos aplicables |
| Signalsmith | stretch1.3.2/linear0.6.6, MIT | Textos íntegros y copyright |
| ONNX Runtime | Actual1.22.0 desde CI09 (B00:1.19.2); origen1.28.0, MIT | ELF16KB del AAR comprobado; APK/Android y modelos pendientes; avisos upstream añadidos |
| Modelos ONNX | SHA previos y archivos existentes; exportación exacta pendiente | Bloquea declarar análisis/redistribución final completos; nombre no certifica permiso |
| SF Pro y otras fuentes restringidas | No hay autorización de redistribución acreditada | No copiar; conservar fuentes autorizadas |
| Vazirmatn | OFL-Vazirmatn.txt presente en origen | Mantener OFL/autoría al incorporar |
| Iconos/imágenes | Condiciones por recurso | Revisar fuente real, marcas y permiso; usar identidad Aurelis |
| APIs/letras | Licencia del cliente no concede derechos de contenidos/API | Verificar individualmente antes de habilitar proveedor |

## Procedimiento por port

Registrar origen/ruta/ref, autores, licencia/SPDX, modificaciones y dependencias;
retener cabeceras originales y añadir solo atribución propia aplicable. Actualizar
NOTICE/LICENSES y bundle legal, comparar textos empaquetados. No escribir comentarios
en formatos que no los admiten. Preservar LICENSE y configuración de firma.

El inventario de dependencias identifica metadatos POM/BOM/constraints, no cantidad
de binarios dentro del APK. No sustituye auditoría del contenido enlazado.
«Formato de licencias.csv» sigue pendiente para el formato definitivo de cabeceras
según la instrucción anterior; no impide conservar avisos ni documentar el plan.

## Puertas de cierre

No declarar cumplimiento completo ni beta distribuible hasta acreditar modelos,
fuentes de binarios y condiciones de componentes nuevos. Una incidencia local de
16KB se registra en B00 y se corrige en B09/B13; no se esconde mediante avisos.
Servicios opcionales sin permisos/credenciales permanecen indisponibles honestamente,
sin activar clientes legacy. Last.fm no autoriza embeber un secreto en el APK.


Hallazgo estático B00: ORT1.19.2 incluye dos ELF con LOAD4KB. Ver B00_EVIDENCE.
La solución de compatibilidad deberá conservar licencias y acreditar modelos/ABI;
ZIP alineado no certifica ELF.175 dependencias sin cambios vs auditoría histórica.


## B01.1

Migración/backup propios sobre APIs Android/Java ya presentes. Sin dependencias,
recursos, modelos o código nuevo de terceros. Avisos existentes intactos; archivo
SingleProfileMigration sin cabecera Aurelis definitiva mientras falta CSV.
No cambios de LICENSE/NOTICE/firma ni actualización ORT/ABI; mantiene pendientes B00.


## B02 — incorporaciones comprobadas

Vazirmatn regular y OFL íntegros del origen fijo: hashes en B02_1_EVIDENCE,
NOTICE y LICENSES/Vazirmatn-OFL.txt, incluidos en bundle legal. No SF Pro.
IosSwitch: GPLv3 según README/LICENSE upstream, sin copyright individual añadido
ficticiamente. Conserva atribución BitChord y SHA/ruta en NOTICE; cambios propios
de integración/accesibilidad identificados. No se relabela el original or-later.
El proyecto conserva LICENSE e identificación propia GPL-3.0-or-later; la
distribución conjunta debe respetar también los términos de cada componente.
Avisos Kyant0/Apache y Ahmed Gamil/MIT preservados en los efectos existentes.
Sin dependencias/binarios/modelos nuevos; siguen los pendientes legales B00.


## Mejora documental autorizada antes de B04

Se reorganiza NOTICE con índice, explicación inicial en español, rutas actuales
de los analizadores/JNI y una sección de distribución y fuentes pendientes.
Se conserva la identidad propia GPL-3.0-or-later y se aclara que el README del
BitChord fijo declara GPLv3, sin presumir permiso «o posterior» para sus archivos.
Se corrige el resumen GPL/AGPL conforme a las secciones 13 de ambos textos:
las partes conservan sus términos y las exigencias especiales de interacción
por red de AGPL se aplican a la combinación. No se cambia ninguna licencia.

LICENSES/README.md añade una guía informativa en español y un índice de textos.
El empaquetado existente incluye esta guía sin modificar Gradle. Los avisos MIT,
Apache, OFL, autores, hashes y referencias previas se conservan. LICENSE y todos
los textos íntegros de terceros permanecen byte por byte iguales a f19fcf6.
No se aplican cabeceras definitivas; el CSV sigue pendiente. Los pendientes de
fuentes FFmpeg/NextLib y exportaciones ONNX no se declaran resueltos.

Doble revisión documental y evidencia: docs/qa/LEGAL_TEXT_REVIEW.md.
No se ejecuta Gradle localmente: la compilación y verificación de los textos en
los nuevos APK corresponden a GitHub. B04 no iniciado; plan principal intacto.


## GLASS01 — adaptación del material antes de B04

Referencia leída del archivo local BitChord-b690dc1.tar.gz, commit completo
b690dc146009d4815825fa4a20815ef2e4ee8898: LiquidGlass.kt, GlassNavBar.kt,
GlassSelectionPill.kt, Shaders.kt y fragmento de MainActivity.kt (paintBackdrop).
Se restaura la procedencia declarada EchoMusicApp/Echo-Music (GPL-3.0) del
pegamento Liquid Glass, vía BitChord, en comentario existente y NOTICE.
No se inventa copyright individual ni permiso «o posterior» del código upstream.
Los avisos Kyant0/Apache y Ahmed Gamil/MIT permanecen intactos. No se importan
binarios, fuentes, iconos, modelos ni dependencias nuevos. LICENSE y LICENSES/
sin cambios. Persisten los pendientes legales y el CSV para cabeceras definitivas.
Comparación y límites en docs/qa/GLASS01_MATERIAL_PARITY.md y evidencia JSON.


## B04 — entrega incremental

Nuevos agrupador de géneros/historial y wiring propios sobre catálogo, preferencias
y pantallas existentes. Referencia consultada: Explore/Search/Library sharedUi de
b690dc1; no se copian código nuevo de terceros, fuentes, imágenes ni binarios.
Avisos de los componentes existentes intactos, igual que LICENSE/NOTICE/LICENSES.
No dependencias nuevas. Pendientes legales y CSV conservados; no cabeceras propias
definitivas. Esta revisión documental no certifica distribución final.


## B05 — reproductor local, entrega incremental

Referencia BitChord b690dc1: MiniPlayer y LandscapePlayer (métricas/disposición),
SourceQualityBadge (auditoría de etiquetas). Se reutiliza el layout vendorizado
existente y se conectan controles propios. Modelado de calidad y gesto propios
sobre Media3/Compose existentes. NOTICE amplía procedencia, conserva autores y
no incorpora símbolos Dolby ni recursos remotos/restringidos. Sin nuevas licencias,
dependencias o binarios; LICENSE/LICENSES intactos. Pendientes y CSV conservados.


## B05.2 — panel/dock

Se consultan PlayerSheetMotion (Android) y PlayerDock (sharedUi) del SHA fijo
b690dc1. Reutilización/adaptación de infraestructura previamente portada, con
procedencia GPLv3 y autores originales conservados en NOTICE y comentarios.
Geometría/host inline/overlay nuevos para Aurelis; no atribuyen a Aurelis los
derechos de terceros ni amplían permiso a versiones futuras no acreditadas.
Sin nuevas dependencias, fuentes, iconos, modelos ni binarios. LICENSE/LICENSES
íntegros; pendientes legales y CSV permanecen abiertos. No certificación legal.


## B06.1 — cola activa

PlayerQueue existente se conecta al flujo local; referencia exacta de renderer,
QueueCoordinator y SongActionsMenu en NOTICE y B06_1_QUEUE. Conservada atribución
BitChord GPLv3; integración/test de Aurelis no reetiqueta derechos de terceros.
Sin dependencias/recursos/binarios nuevos ni cambio a LICENSE. Pendientes de
redistribución nativa/modelos/CSV anteriores siguen abiertos.


## CI09 — runtime1.22.0 y avisos upstream

Actualización limitada al runtime nativo16KB autorizada antes de B07. Microsoft
MIT coincide con texto conservado; ThirdPartyNotices v1.22.0 íntegro añadido
a LICENSES y referenciado en NOTICE/índice. POM/AAR oficial y hashes en
../../qa/CI09_ONNX_16KB.json. No sustitución de LICENSE ni autoría de terceros.
AAR nativo alineado no cierra redistribución/modelos ni prueba hardware. Beat This
falla al cargar ConvInteger en CPU host en runtime previo y nuevo; voces carga
en ambos. Modelos intactos, procedencia/exportación y validación ARM64 pendientes.
