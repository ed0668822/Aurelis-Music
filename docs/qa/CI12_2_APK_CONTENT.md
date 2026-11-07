# CI12.2 — contenido real del APK

Base f36d44f73378240bb8e98a3b04d4651edad26b84, misma rama y PR1.
Run150 SUCCESS: https://github.com/ed0668822/Aurelis-Music/actions/runs/42325372151.
Job128259289859 enlaza el SHA exacto; CodeQL1319 SUCCESS observado. Esto valida
CI12.1 y sus nombres/reportes, no los cambios de esta entrega.

## Implementación y requisitos

`.github/scripts/apk_audit.py` se ejecuta dentro de verify, antes de copiar los
APK para publicación. Reutiliza las compilaciones existentes y librerías estándar
Python; no cambia app, Gradle, CMake, ABI, R8, firmas, modelos o workflow.
`.github/apk-audit-policy.json` declara los requisitos, sin forzar inclusión:

- DEX y manifiesto fusionado: AurelisApplication, MainActivity y el único servicio
  AurelisPlaybackService. Un componente obligatorio eliminado por R8 sigue siendo
  un faltante; un registro usage no prueba que exista.
- Recursos esenciales: nombre de app, fuente Vazirmatn e iconos de lanzamiento.
- Ambos modelos ONNX de main, por bytes/SHA256, y la fuente por bytes exactos.
  Una ruta de fuente renombrada por AAPT se resuelve por símbolo/entrada real.
- Bibliotecas ARM64: análisis, DSD, DST, FFmpeg JNI y los dos binarios ONNX Runtime.
  La verificación ELF/ZIP16KB/firma existente permanece obligatoria.
- Entradas base AndroidManifest.xml, resources.arsc y classes.dex; multidex sin
  duplicar definiciones. No se acepta ZIP con entradas duplicadas.

Un faltante de esta lista bloquea publicación; el publicador limpia resultados
parciales y conserva informes. No se cambian clases/keep ni se borran funciones
para hacer pasar el verificador. Nombres Aurelis-Music-V<versionName>.apk y
-debug.apk se conservan, con bytes originales y manifiesto/checksums de entrega.

## Evidencias por archivo

`apk-content-debug.json/.tsv` y `apk-content-release.json/.tsv`, incluidos por el
upload de reportes actual. JSON guarda SHA del APK, identidad source/checkout/run,
SHA de política, detalle de clases/recursos/assets/nativos y límites. TSV resume
path/kind/state. compilation-audit.json/.tsv incorpora el estado APK por variante;
no mejora el estado de compilación individual ni el de integración UI.

- Clases: leer class_def/type_id/string_id de DEX; leer nombre/SourceFile de
  outputs JVM declarados por tareas de compilador terminadas/reutilizadas.
  No usar globs de outputs antiguos como prueba ni SourceFile del DEX renombrado
  por R8. Correlacionar source package/nombre con metadata antes de R8; fuentes
  homónimas quedan ambiguas. No exigir una clase por .kt: inline/typealias y
  desugaring pueden impedir una correspondencia directa.
- R8: mapping.txt y usage.txt disponibles se identifican por SHA. Renombrado con
  clase final presente es evidencia de presencia; usage por sí solo no lo es.
  Clases auxiliares ausentes quedan incompletas, no fallo automático de app.
  Presencia de clases no prueba equivalencia de fuentes ni conexión funcional.
- Recursos: AAPT2 dump resources, símbolos (incluidos estilos con puntos) y rutas
  reales de archivos. XML compilado no se compara literalmente. Presencia de un
  símbolo no acredita todos sus valores/configuraciones/idiomas.
- Manifiesto: AAPT2 xmltree del manifiesto fusionado, componentes esenciales;
  permisos/atributos restantes no se presentan como verificados funcionalmente.
- Assets/avisos/precompilados: contraste de bytes cuando corresponde. ABI x86_64
  versionada en el repo se registra excluida por la política ARM64 actual.
- Nativo: compile_commands.json disponible por configuración, origen y SHA.
  Solo acredita comando declarado, no ejecución individual, enlace de cada unidad
  ni equivalencia fuente-binario. ffmpeg_jni.cc sin comando no demuestra que
  falte FFmpeg: el proyecto también incluye un binario precompilado.
- Pruebas y archivos de soporte excluidos por política no se presentan como
  faltantes. Symlink/contenido no inspeccionable y casos desconocidos conservan
  estados explícitos. No hay certificación genérica de que todo se copió.

## Protección contra falsos resultados

Estados generales: REQUIRED_CONTENT_VERIFIED_WITH_COVERAGE_LIMITS,
REQUIRED_CONTENT_MISSING y EVIDENCE_ERROR. Error de parser/herramienta no equivale
a error de Kotlin ni a todos los recursos ausentes. DEX estándar035/037/038/039/040
soportado; otro formato requiere adaptar el auditor y bloquea su aprobación,
sin afirmar que la app esté rota. Formatos JVM soportan constant pool moderno.
Nunca se publican valores de constantes, cuerpos de métodos ni comandos nativos.
Informes de contenido previos se eliminan al verificar; report rechaza identidad
de build obsoleta. Un APK no se ofrece si falla cualquier requisito/gate previo.

## Validación realizada y pendiente

64 métodos de pruebas Python PASAN:47 previos conservados y17 nuevos, con varios
fixtures por método. DEX/JVM/ZIP/ELF son metadata sintética, no APK instalables.
Cubren renombrado/usage R8, multidex classes10, truncamiento, modelos/fuentes y
bibliotecas faltantes o corruptas, manifiesto, valores XML, exclusión ABI/tests,
homonimia, clases sin outputs, constantes privadas, rechazo de reportes obsoletos
y bloqueo real del publicador. Resuelve fuente renombrada por AAPT sin falso fallo.

AAPT2 real del SDK35 leyó android-36/android.jar existente para contrastar formato
de tabla/estilos; no se compiló ni instaló nada. Primera revisión estática de
arquitectura/evidencia/alcance; segunda pasada de regresión/formatos/privacidad por
el mismo agente. No son revisores independientes. Diff --check correcto.

Compilación y ejecución de Gradle hook, auditoría de los APK nuevos de Aurelis y
suites Android: PENDIENTES de GitHub exclusivamente. Instalación/arranque/audio/
GPU/integración UI NO EJECUTADOS. No declarar verificado un APK real con fixtures.
Publicar, avisar y esperar resultado del usuario. CI12.3 no iniciado; autorización
conservada. Plan de migración intacto; no B09.
