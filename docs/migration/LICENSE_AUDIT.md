# Auditoría inicial de licencias

## Evidencia y límites

Destino `9bf3fd3db8f5870f942f5167dca5bc77e3c48edd`; origen
`b690dc146009d4815825fa4a20815ef2e4ee8898`. Se leyeron el LICENSE de BitChord,
su declaración GPLv3 en README, cabeceras seleccionadas y el NOTICE de Aurelis.
Esto no equivale a una certificación legal ni a una auditoría exhaustiva de dependencias.

Identidad solicitada para trabajo propio: Aurelis © 2026. GPL-3.0-or-later.
No se encontró `Formato de licencias.csv` en el workspace durante la auditoría.
**El usuario debe incorporarlo antes de aplicar cabeceras definitivas.** B01 deja
los documentos sin cabeceras legales definitivas y no modifica avisos existentes.

| Elemento | Evidencia | Tratamiento |
|---|---|---|
| Aurelis | LICENSE GPLv3; NOTICE declara versión 3 o posterior y adaptaciones de ed0668822 | Conservar LICENSE/NOTICE y atribuir únicamente aportes propios |
| BitChord general | LICENSE GPLv3; README declara GPLv3; NOTICE destino declara BitChord GPL-3.0-or-later | Reutilización candidata bajo GPLv3; comprobar permiso «o posterior» y excepciones por archivo antes de etiquetar adaptaciones |
| AutoMix Android compartido | TrackAnalysis, TransitionPolicy y TrackFeatures origen declaran copyright SFG545 y Kushagra Singh, procedencia Orchard y AGPLv3 o posterior | No relabelar como GPL exclusivo. Registrar textos y obligaciones AGPL, y revisar combinación/distribución antes de nuevas copias |
| DSP ya incluido en Aurelis | NOTICE, sección Orchard, enumera analizadores y JNI bajo AGPL-3.0-or-later | Preservar procedencia; contrastar lista con rutas reales `app/src/main/cpp/` en revisión específica. NOTICE por sí solo no demuestra cumplimiento completo |
| AutoMix web BitChord | `web/app/src/playback/automix/LICENSE` contiene AGPLv3 | Fuera de la migración Android; no copiar |
| GlassNavBar origen | Cabecera atribuye estructura a EchoMusicApp/Echo-Music GPL-3.0 y FloatingTabBar vendorizado | Conservar avisos y rastrear la licencia del componente incluido antes de adaptarlo |
| Signalsmith | Licencias MIT en `native/third_party/signalsmith-linear/LICENSE.txt` y `signalsmith-stretch/LICENSE.txt` | Preservar textos/copyright si se incorpora; su existencia no obliga a importar esas dependencias |
| Fuentes SF Pro e iconos/modelos | Theme origen referencia SF Pro; no se verificó autorización de redistribución de todos los recursos | No copiar recursos no aclarados. Evaluar alternativas con licencia explícita en B03 sin prometer fidelidad tipográfica idéntica |
| Proveedores de letras/traducción | Código contiene endpoints de terceros | Licencia del cliente no concede derechos sobre contenidos/API. Revisar condiciones, privacidad y permisos del servicio antes de habilitar nuevas integraciones |

## Política de reutilización

GPLv3 y AGPLv3 permiten ciertas combinaciones según sus cláusulas; no concluir que
AGPL es automáticamente incompatible ni que todo componente se convierte en GPL.
Documentar obligaciones concretas de las partes combinadas, suministro de fuentes,
avisos y, cuando aplique, interacción remota. La interpretación de NOTICE existente
debe comprobarse, no copiarse como conclusión jurídica.

Antes de cada adaptación, registrar archivo y commit original, autores, licencia,
dependencias vendorizadas, modificaciones y avisos que deben acompañar la distribución.
Si no se pueden establecer condiciones compatibles con la distribución prevista,
no copiar ese componente; dejar pendiente o especificar una alternativa independiente.
Nunca eliminar atribuciones ni atribuir a Aurelis código de terceros.
No añadir comentarios a formatos que no los permiten ni cabeceras que invaliden XML.

## Pendientes explícitos

1. Incorporar el CSV para fijar el formato de cabeceras; no es necesario para conservar este inventario.
2. Rastrear el alcance real de código Orchard ya incluido y los avisos por archivo.
3. Verificar autorización de fuentes, iconos, imágenes, modelos ONNX y sus datos asociados antes de importarlos.
4. Auditar licencias de dependencias nuevas por bloque; B01 no añade ninguna.
5. Conservar la identidad GPL-3.0-or-later del trabajo propio sin borrar obligaciones diferentes de terceros.

## Registro B02

ProfileId, ProfileStorage, LocalLibrarySource y sus pruebas son implementación propia
para Aurelis; no se copiaron nuevas piezas de BitChord, Orchard ni recursos visuales.
Se conservaron avisos de archivos existentes. Los nuevos archivos no llevan cabeceras
definitivas mientras falta el CSV, siguiendo la decisión de B01; su incorporación
sigue pendiente y no justifica borrar atribuciones existentes.
Las dependencias añadidas son solo de pruebas: JUnit 4.13.2 (EPL-1.0), Robolectric
4.14.1 (MIT) y kotlinx-coroutines-test 1.9.0 (Apache-2.0), según sus licencias
upstream, contrastadas con los POM descargados por Gradle. No se incorporan estas
herramientas al APK de producción. No se resuelven
en B02 los pendientes jurídicos de componentes ya incluidos.

## Registro B03

Se conserva el aviso previo de los archivos modificados y las atribuciones en NOTICE.
La escala/estilo de Theme.kt ya estaba adaptada en Aurelis; se conecta al tema activo
y se añade interlineado/fuente del sistema. Se consultó el tema de BitChord en el SHA
fijo de B01 para comparar la paleta neutra. No se importa código de otros módulos,
iconos, imágenes, modelos ni archivos de fuente de BitChord. No se redistribuye SF Pro;
la constante de compatibilidad previa apunta a la familia sans del sistema.

Las nuevas herramientas Compose UI de pruebas se distribuyen bajo Apache-2.0,
contrastado con los POM resueltos en Gradle. ui-test-manifest se limita a debug y
ui-test-junit4 a tests; ninguna se añade a release. Las pruebas y ajustes nuevos
son aportes propios. Sigue faltando el CSV para las cabeceras definitivas; no se
eliminan atribuciones ni se declara resuelta la auditoría de recursos pendientes.
Las cuatro capturas documentales se generan con componentes reales, texto de ejemplo
de la prueba y gráficos nativos de Robolectric; no contienen carátulas ni material
gráfico copiado de BitChord. No son pantallas de una biblioteca real de usuario.

## Registro B04

Se extiende AppNavigator ya adaptado en el destino y se conservan avisos y NOTICE.
AppNavigationHost, Saver, integración y pruebas nuevas son aportes propios; no se
incorpora nuevo código, recursos ni dependencias de BitChord u otros proyectos.
Se mantiene el alcance legal ya documentado para la adaptación previa. Nuevos archivos
siguen sin cabeceras definitivas hasta recibir el CSV; no se borran avisos existentes.

## Registro B05

HomeHero/carruseles se reutilizan desde Aurelis, conservando avisos y NOTICE. Nuevos
datos de Home, persistencia y pantallas de colecciones son aportes propios. La cola
de selección usa QueueBuilder existente, sin copiar análisis ni transiciones AGPL
adicionales. No hay fuentes, carátulas, datos musicales o traducciones descargadas
para simular bibliotecas. Los fixtures de pruebas solo existen bajo `app/src/test`.
No se añaden dependencias; las cabeceras definitivas siguen pendientes del CSV.

## Registro B06

Se modifica el catálogo MediaStore y la integración local existentes, conservando
avisos originales. LocalSearch y las pruebas nuevas son aportes propios. No se
importan componentes de BitChord, recursos, fuentes, servicios ni dependencias
adicionales. Fixtures exclusivamente en pruebas. LICENSE/NOTICE se conservan.
El archivo «Formato de licencias.csv» sigue ausente; las cabeceras definitivas de
archivos nuevos quedan pendientes, sin eliminar avisos de archivos modificados.

## Registro B07

Se adaptan el reproductor, componentes visuales y puente Media3 existentes en
Aurelis, conservando avisos. No se importan nuevos archivos ni recursos de BitChord,
Orchard u otros proyectos. Iconos y carátulas usan dependencias existentes y URIs
locales; capturas de prueba con texto ficticio y fallback propio, sin portadas
de terceros. Pruebas nuevas originales; sin nuevas dependencias ni cambios de
LICENSE/NOTICE. Sigue pendiente «Formato de licencias.csv» para cabeceras definitivas.

## Registro B08

Aportes nuevos propios sobre almacenamiento privado, servicio y UI existentes.
Sin código ni recursos nuevos de BitChord, nuevas dependencias o cambios en avisos
y LICENSE/NOTICE. Cabeceras definitivas pendientes del CSV, preservando las existentes.

## Registro B09

Se reutilizan y amplían parser, panel y cliente LRCLIB ya incluidos en Aurelis;
LocalLyricsRepository y pruebas son aportes propios. No se copian nuevos archivos
de BitChord ni recursos/SDK de terceros, ni se añaden dependencias. Avisos y
LICENSE/NOTICE conservados; CSV de cabeceras sigue pendiente.

Se consultó en lectura el README público de
https://github.com/tranxuanthang/lrclib (raw master/README.md), que describe LRCLIB
como servicio libre con API para buscar/aportar letras sincronizadas. No se copia
código del servidor. Esto no constituye autorización general sobre el contenido
de las letras ni certificación jurídica de todos los proveedores legacy.
Se conserva el endpoint público de búsqueda existente, con User-Agent del proyecto,
TLS, límites de timeout y sin credenciales. La búsqueda B09 requiere acción explícita
y LRCLIB habilitado; envía metadatos de la canción. Otros proveedores no se activan
automáticamente en esta ruta. Caché privada no se redistribuye en el repositorio.

Fixtures y captura usan texto original de pruebas, no letras atribuidas a artistas
reales ni traducciones simuladas. La consulta de LRCLIB en vivo permanece sin verificar
por la política de red del entorno; no se afirma haber descargado letras reales.

## Registro B10

Se amplía el cliente existente y sus componentes, preservando avisos; sin nuevas
dependencias ni código de servidor copiado. Se leyeron README y ruta /translate
de https://github.com/LibreTranslate/LibreTranslate (main) como documentación del
protocolo: servicio autoalojable, q array y translatedText; README declara AGPLv3.
No se incorpora ni distribuye ese servidor. No se afirma una certificación legal
de servidores ajenos ni que la licencia del programa conceda derechos sobre letras.

Los endpoints Google/Input Tools heredados no acreditaban condiciones de uso en
B01; quedan fuera del flujo de traducción activo. No se inventa un endpoint público
ni se redistribuyen modelos propietarios. El usuario debe configurar un servicio
autorizado compatible sin clave, aceptar su privacidad/condiciones y confirmar
cada traducción. No se despliega servicio ni se envían letras durante las pruebas.
Datos y traducciones de fixtures son propios y solo de pruebas; captura identificada
como fixture. LICENSE/NOTICE conservados; CSV de cabeceras sigue pendiente.

## Registro B11

Revisados en lectura, mismo SHA de B01: QueueBuilder jvmSharedMain y
TransitionPolicy/AutomixAnalysisSource commonMain de BitChord. QueueBuilder
aporta deduplicación/caps ya adaptados en Aurelis; se preservan avisos existentes.
TransitionPolicy atribuye SFG545/Orchard y Kushagra Singh bajo AGPLv3 o posterior;
no se copia ni relabela esa política. AutomixAnalysisSource está ligada a
YouTube Opus, excluida de la ruta local. No se modifica ni publica el origen.

LocalAutoMix es aporte propio basado en metadatos y almacenamiento existentes;
no se añaden bibliotecas, recursos, modelos ni código DSP de terceros. Se conservan
LICENSE/NOTICE. Pendientes previos sobre distribución de DSP ya incluido y CSV
no se declaran resueltos por implementar recomendaciones. Captura usa fixtures
de catálogo propios; no contiene canciones ni portadas copiadas.

## Registro B13

Ajustes y wiring se amplían desde Aurelis conservando avisos y NOTICE. Filtros
y snapshot nuevos son implementación propia de secciones IIR matemáticas, sin
importar código DSP de BitChord/Orchard ni nuevas dependencias. El borde activo
ligero evita shaders y no cambia avisos ni fuentes vendorizadas de backdrop.
Fixtures son señales sinusoidales propias; las capturas no incluyen recursos
de terceros. B12 no implementado ni atribuido como terminado. CSV aún ausente;
no se aplican cabeceras definitivas nuevas ni se eliminan avisos existentes.

## Registro B14

Se contrastaron los POMs de releaseRuntimeClasspath y sus padres: 175 módulos
incluyendo BOMs/aliases/restricciones, no 175 librerías empaquetadas. Inventario
`DEPENDENCIES_RELEASE.tsv`; todas las filas tienen licencia declarada resuelta.
Esto no sustituye una auditoría de contenido ni vulnerabilidades de cada binario.
NextLib-media3ext 1.11.1-0.16.0 declara GNU GPLv3 en su POM: se corrige el Apache-2.0
incorrecto de NOTICE sin atribuir su código a Aurelis ni conceder «o posterior»
no acreditado. Checker-qual 3.48.3 declara MIT, también corregido.

LICENSES conserva textos íntegros: AGPLv3 (SPDX license-list-data), Apache-2.0
(texto estándar), MIT Beat This!/Open-Unmix desde upstream, ONNX Runtime v1.19.2,
Jsoup jsoup-1.18.1 y checker-qual del JAR exacto. Se mantienen copyrights originales.
Gradle empaqueta estos siete archivos junto con LICENSE/NOTICE (nueve entradas),
verificadas byte por byte. No se agregan cabeceras propias a textos de terceros.

Beat This!: README oficial CPJKU/beat_this declara MIT para código y pesos, con
advertencia sobre datos de entrenamiento. Open-Unmix distingue umxl (CC BY-NC-SA
4.0) de umxhq; la app declara umxhq, por lo que no se traslada automáticamente la
restricción de umxl a ese archivo. Se conserva la declaración MIT upstream y su
copyright; falta registrar la procedencia/exportación cuantizada exacta. No se
reemplazan ni descargan pesos. Assets existentes y hashes:

- beat_this_int8.onnx: 4 533 252 bytes, SHA-256 `9dc29f1fcd713d18f48a2755109fce01429ba6d1639607af8ae5c7449b47070f`.
- vocals_umxhq_int8.onnx: 9 054 206 bytes, SHA-256 `a2be987b55a29bc149d3a6ae99b08175d81f85ee292a8ea21f96c3a473bc94cb`.

Los doce .so ARM64 de release se inventariaron sin cambios. No se pudo determinar
la configuración completa FFmpeg por cadenas enable/disable-gpl del binario; no
se infiere una licencia distinta por ausencia de esas cadenas. Las obligaciones
de suministro de fuentes correspondientes de esos binarios siguen pendientes
de acreditación; links/NOTICE no se declaran suficientes como certificación.

«Formato de licencias.csv» sigue ausente en repo/workspace. Por la regla del usuario
no se aplican cabeceras definitivas propias. La revisión legal final permanece
pendiente junto con esos puntos de procedencia; no se declara cumplimiento total
ni publicación lista. No se han versionado archivos de firma/configuración local
según revisión de patrones, ni se han leído o modificado claves privadas.

Textos upstream consultados: CPJKU/beat_this main/LICENSE y README,
sigsep/open-unmix-pytorch master/LICENSE y README, Microsoft/onnxruntime
v1.19.2/LICENSE, jhy/jsoup jsoup-1.18.1/LICENSE, checker-qual 3.48.3
META-INF/LICENSE.txt del JAR y SPDX license-list-data AGPL-3.0-or-later.
Son avisos íntegros, no componentes de ejecución nuevos; no se cambian modelos,
dependencias ni copyright de contribuciones anteriores.
