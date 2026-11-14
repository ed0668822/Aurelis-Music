<!-- Aurelis © 2026. GPL-3.0-or-later -->
# LICENSE_AUDIT — beta local

## LEGAL01 — formato recibido y primer bloque de cabeceras propias

El usuario aportó Formato de licencias.csv y autorizó aplicarlo a archivos propios
por bloques, conservando las licencias y atribuciones de terceros. Se guarda una
copia byte por byte en [AURELIS_HEADER_FORMAT.csv](../../legal/AURELIS_HEADER_FORMAT.csv),
con sus 28 reglas. Las menciones posteriores al CSV pendiente son registros
históricos de entregas anteriores, no el estado actual de recepción del formato.

Base de preparación: bb417f39a0d8e76307977d52b1fa88587d67ae51; misma rama/PR1.
Solo se añade la línea `// Aurelis © 2026. GPL-3.0-or-later` al inicio de cuatro
pruebas propias de perfiles/ajustes; después de esa línea sus bytes no cambian.
La cabecera no se inserta en código de terceros ni sustituye avisos originales.

| Archivo propio bajo app/src/test/java/com/music/aurelis/data | Commit de creación, autor ed0668822 |
|---|---|
| SingleProfileMigrationTest.kt | bfbc81fdaeee5d9cb2086f27ae156ce75ca5c58b |
| ProfileStorageTest.kt | 2a10e63fd89db9993ebfd1ef21920cc761aec886 |
| ProfileAvatarsTest.kt | 30569f194d8f05ebe728caa3b51406a27ccbf96b |
| SettingsPersistenceTest.kt | 640b936179e95356fc58df6aa458e3d0858fd226 |

Se revisaron sus contenidos y antecedentes: pruebas de contratos locales de
Aurelis, sin avisos previos de terceros y sin coincidencia de ruta en el inventario
de la referencia BitChord. Esto delimita esta selección; no atribuye automáticamente
todos los archivos del repositorio a Aurelis por su ubicación o por el autor Git.

Manifiesto de entrega: los cuatro .kt anteriores, el CSV y este LICENSE_AUDIT.md;
6 rutas como máximo conservador, incluidos documentación y referencia. Los tres
documentos CI16 previamente pendientes se conservan sin modificar, fuera de esta
entrega. No se mezclan presupuestos ni se preparan commits conjuntos.

Aplicación futura del CSV: respetar shebang antes de comentario en scripts,
declaración XML antes de comentario y JSON sin comentarios. Los archivos propios
Markdown pueden llevar comentario HTML; aquí se aplica a este registro. El CSV
se conserva como dato original, sin anteponer una línea que altere sus columnas.
No se modifican LICENSE, NOTICE, LICENSES, textos de dependencias, binarios,
modelos ni fuentes; tampoco firmas, Gradle, CI o comportamiento de las pruebas.

Revisión1 del mismo agente: historial/contenido de la selección propia y sintaxis
exacta .kt/.md según CSV. Revisión2 del mismo agente: prefijo único/resto de bytes,
CSV íntegro, seis rutas, archivos legales y documentos CI16 intactos. No son
revisores independientes ni una certificación legal global. Comprobaciones
estáticas de preservación y diff; no tests nuevos ni Gradle/Android locales para
este cambio de comentarios. Compilación Android nueva no acreditada aquí.
El usuario autorizó aplicar y subir este bloque. En adelante, la autorización
para aplicar cada bloque incluye su commit y push a
feature/bitchord-local-migration, conservando el mismo PR. Tras publicarlo se
espera el resultado comunicado por el usuario y su autorización para el siguiente
bloque. Este es el primer bloque, no una aplicación masiva ni un cierre de todos
los pendientes de licencias/modelos/procedencia.

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
El formato definitivo de cabeceras propias está recibido y registrado en LEGAL01
al inicio de este documento. Su aplicación no elimina avisos de terceros.

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

### B02.3-b3 — adaptación del motor háptico

Fuente BitChord b690dc146009d4815825fa4a20815ef2e4ee8898, archivo
sharedUi/src/androidMain/kotlin/com/music/bitchord/ui/haptics/Haptics.android.kt;
SHA-256 verificado por lectura API del ref fijo:
074cb0c645dac155555bbd57378b0ed027b8f025771d145daaa2999c2bc683ab.
Su LICENSE es GNU GPLv3, mismo texto íntegro ya conservado en LICENSE;
SHA-256: 3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986.
Se adaptan datos de ritmos, composición y fallbacks de onda en Haptics.kt.
La cabecera primera de Aurelis queda delimitada a contribuciones locales;
comentario de procedencia y NOTICE conservan autoría/condiciones BitChord,
sin inventar permiso or-later para el material upstream.

Cambios propios: API mínima33 existente, soporte de primitivas por ritmo
(incluido LOW_TICK), caché por handle, política app/Android y conexión del gesto
largo a la acción compartida. No se incorpora el singleton/observador global del
origen. No entran dependencias, permisos, recursos, fuentes, modelos o binarios.
LICENSE y LICENSES sin cambios, avisos anteriores de NOTICE íntegros. Las dos
revisiones de esta entrega incluyen procedencia/alcance de licencia; no son una
certificación de cumplimiento completo ni de paridad física. Se conservan
los pendientes B00 de otras dependencias/modelos y distribución final.

### Incorporaciones previas de B02

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


## B03.1-a — identidad y ciclo de vida del estado de páginas

Referencia BitChord b690dc146009d4815825fa4a20815ef2e4ee8898, solo lectura:
app/src/main/java/com/music/bitchord/ui/navigation/AppNavigator.kt,
SHA-256: 9ffe029a38252ecd0e3355009c366d6e1b25f8de1837ae8d3578e13db65c0947;
app/src/main/java/com/music/bitchord/MainActivity.kt,
SHA-256: 90e254be457f57c70166321ca9bac3bc5e42eba4e6bdf577b48235ea75c200e2.
Se adaptan la identidad por RouteEntry y el patrón pageStates/pageKeysSeen para
retirar estado de páginas cerradas, en AppNavigator/AppNavigationHost locales.
NOTICE conserva origen y condiciones GPLv3 del material upstream; las cabeceras
Aurelis identifican aportes propios, sin conceder or-later al original.

Persistencia v2, compatibilidad de claves v1 y pruebas son integración propia de
Aurelis con las rutas locales existentes. GlassNavBar se consultó como referencia
de interacción; su archivo permanece sin cambios. No se incorporan dependencias,
recursos, fuentes, modelos, APIs remotas ni binarios. LICENSE/otras atribuciones,
firmas, applicationId y ABI intactos. No se cierran los pendientes legales de
modelos/binarios por esta adaptación de UI.

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


## B07.1 — parsers locales y caché temporal

EnhancedLrc/TtmlLyrics ya adaptados de BitChord, avisos originales conservados y
procedencia fija/changelog ampliados en NOTICE. Codec/glue/fixtures nuevos propios,
sin letras comerciales, fuentes/modelos/binarios o dependencias nuevas. LICENSE y
LICENSES intactos; formato definitivo CSV sigue pendiente, sin relabelar terceros.
No se habilitan proveedores nuevos ni se presume autorización de sus contenidos.


## B07.2 — reloj y texto barrido

Partes dirigidas de PlayerLyrics/LyricClock BitChord fijo, GPLv3; atribución de
contribuidores preservada en NOTICE y marcadores de los adaptadores. No se importan
fuentes/recursos/proveedores/modelos ni dependencias nuevas. LICENSE/LICENSES y
procedencias previas intactas. No se relabela código de terceros ni se afirma port
completo del renderer; fixtures propios. Ver B07_2_SWEEP para alcance y pendientes.

## B08 — proveedores y traducción

Véase B08_PROVIDERS.md (tabla individual) y ../../qa/B08_PROVIDERS.json.
Referencia F12 de BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 revisada
sin escribir en origen. Conservados LICENSE y los avisos originales; NOTICE
ampliado para identificar alcance de la adaptación y aportaciones Aurelis.
Sin nueva dependencia, fuente, modelo o binario. No se presume licencia de letras
por la licencia GPL del cliente. LRCLIB: consulta pública/manual ya aprobada en el
plan; README público consultado, sin copiar letras o código de su servidor.
BetterLyrics/Portato, BiniLyrics y lrc.red: condiciones de uso/contenido no
acreditadas; deshabilitados en flujo activo y preferencias no pueden activarlos.
BiniLyrics además requiere endurecer coincidencias/destinos antes de habilitarlo.
Otros proveedores legacy no se despachan. Traducción: solo servidor HTTPS que
configure el usuario; sin activar endpoints Google legacy ni claves embebidas.
Disponibilidad real y derechos del contenido no certificados por esta revisión.

## B03.2-a — cabeceras, tarjetas y carruseles de Home

Referencia local fija BitChord b690dc146009d4815825fa4a20815ef2e4ee8898,
GPLv3 según LICENSE; no se le atribuye automáticamente «or later».
HomeScreen.kt upstream, sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens,
SHA256 d6f6a11a322eb968823fd6abd861f3e8ebff381e71e629a2a77a55d38d66e017.
SongText.kt upstream, ui/components bajo la misma raíz, SHA256
2579d2a9eb577c04de023e7dc1ce9ae00e9ec22477fc7706e135ff1e8e4ac79c.
Se adaptan métricas, tipografía, borde y proporción del encabezado, no archivos
completos ni marcas. NOTICE registra contribuyentes/procedencia; conserva las
atribuciones originales y cabeceras Aurelis existentes. Nueva prueba y documento
propios llevan la cabecera indicada por el usuario. LICENSE/terceros no se borran.
Sin nuevas dependencias, fuentes, imágenes, modelos o binarios externos.

## B03.3-a — estados y recuperación local

Referencia local fija BitChord b690dc146009d4815825fa4a20815ef2e4ee8898, GPLv3
según LICENSE, conservando atribuciones originales y NOTICE. Se adapta el patrón
de estados en LazyColumn de sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/
ui/screens/HomeScreen.kt (SHA256
d6f6a11a322eb968823fd6abd861f3e8ebff381e71e629a2a77a55d38d66e017) y se reutiliza
MessageState ya presente en Aurelis, contrastado con ui/components/Common.kt
upstream (SHA256 9e6847c462b52f4c94a53b24a297fe9aba1fa5a0f29accde2746e7a6c075b04e).
No se copian skeletons, datos remotos ni archivos completos. El wiring del flag
isRefreshing y las pruebas controladas son aportaciones Aurelis. Prueba y documento
propios nuevos tienen la cabecera solicitada. No se eliminan licencias/avisos
ni se añaden dependencias, recursos de terceros o binarios.

## B04-a — orden visible y restauración de selección

Consulta local de BitChord b690dc146009d4815825fa4a20815ef2e4ee8898,
app/src/main/java/com/music/bitchord/ui/screens/LocalFolder.kt,
SHA256 177dd478a46ad1ec78a07a36e14d92955e412990a36001dea9eeae056b6db5db.
Se contrastan LocalFolderState/LocalFolderSelectionBar (IDs resueltos contra las
canciones de la vista). No se trasplanta el archivo ni funciones remotas/de caché.
La corrección de orden y el saver de IDs pertenecen a Aurelis; no se atribuye
al upstream esta persistencia nueva. NOTICE conserva contribuyentes y GPLv3 del
material de referencia. Archivo propio modificado, prueba y documento nuevos
llevan la cabecera Aurelis solicitada, sin retirar atribuciones de terceros.
Sin dependencias, recursos, modelos ni binarios nuevos. LICENSE intacto.

## B05.1-a — MiniPlayer y gestos de cola

Adaptación parcial de BitChord b690dc146009d4815825fa4a20815ef2e4ee8898,
app/src/main/java/com/music/bitchord/ui/components/MiniPlayer.kt,
SHA256 f294dfd1b75d1808fb86df2a93580efc87783daef5bc774f4ff9c3f6f7317b69:
miniPlayerGestures y métricas de glyph/spinner. GPLv3 según LICENSE del origen;
se conservan autores/avisos en NOTICE y el comentario de procedencia del helper.
Cabecera Aurelis en primera línea del helper, prueba y documento propios nuevos;
no sustituye derechos del material adaptado. Cambios propios: capacidades del
controller Aurelis, cancelación explícita, slop vertical preservado y progreso
finito. No se portan el lift/spring de la barra ni contratos remotos/video/party.
SourceQualityBadge.kt upstream solo consultado, SHA256
aac9316e1eeb609d799955ab6a7fbd326fda8f761dbc31c20b7fe9eaedd9592a;
sus etiquetas de streaming no se conectan. Sin nuevas dependencias/recursos/binarios.

## B05.2-a
Corrección propia de cancelación del detector de cabecera. Se conservan NOTICE
y atribuciones del panel/dock ya adaptado. Cabeceras Aurelis propias; sin
dependencias, recursos, licencias retiradas ni nueva copia de terceros.
