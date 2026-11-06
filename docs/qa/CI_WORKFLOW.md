# CI01 — mejora de GitHub Actions antes de B04

Autorización: «Aplica los cambios antes de B04». Complemento de CI, no implementación
de B04, QA02 ni cambios a funciones de la aplicación. Se conserva la rama y PR1
«Aurelis Music Beta UI and components», sin merge/force push ni cambios de firma,
identidad, dependencias de la app, ABI o plan maestro.

## Regla vigente de ejecución

El usuario indicó que la compilación de la app se realizará exclusivamente en
GitHub. Al publicar se informa al usuario y se espera a que comunique el resultado;
no se continúa el plan ni se modifican fallos de CI sin ese aviso/autorización.
Las comprobaciones locales de esta entrega son pruebas Python de las utilidades,
actionlint y sintaxis de Bash; no se ejecutó Gradle, compilación Android ni APK nuevo.
La primera ejecución real del workflow modificado queda pendiente de GitHub.

## Flujo conservado y actualizado

- Eventos push a main, PR a main y ejecución manual; concurrencia/cancelación de
  PRs anteriores conservadas. Ubuntu24.04, Java17, wrapper8.11.1, SDK36,
  build-tools35.0.0, NDK26.1.10909125 y CMake3.22.1. Se admite el sufijo oficial
  de Android `3.22.1-g37088a8`; no se acepta otra versión como3.22.10.
- Acciones fijadas a commits verificados y manifests Node24. Cache de Gradle
  reutilizada y build-cache habilitada; PR/ramas fuera de main solo leen cache.
  Máximo2 workers para limitar presión de memoria. No builds duplicados por aviso
  manual: publicar la rama activa el evento PR existente.
- Fases visibles: inventario, pruebas de helpers, Java, Gradle, SDK, firma, lint,
  pruebas de ambas variantes, APK debug/release, verificación, limpieza e informes.
- `continue-on-error` permite recoger diagnósticos, no aprobar el resultado: la
  publicación y el gate final exigen `outcome == success` en todas las fases
  obligatorias. Failure/skipped/cancelled/ausente bloquean el APK aprobado.
- Contents:read como único permiso. Se sustituyen Reviewdog/comentarios externos
  por anotaciones nativas del job y GITHUB_STEP_SUMMARY; no escritura en issues,
  comentarios o checks mediante token adicional. Credenciales de checkout no persistidas.

## Registros que podrá consultar el usuario

En `compilation-report-N` (30 días):

| Archivo | Qué demuestra |
|---|---|
| metadata.json | Commit fuente/PR, checkout real (merge en PR), tree y ejecución |
| changed-files.json | Cambios frente a base/merge-base; renombres/eliminaciones y paths |
| source-inventory.json/.tsv | Fuentes, cabeceras, XML/recursos/assets por módulo/source set/lenguaje; producción/pruebas |
| source-diagnostics.json y file-status.txt | Diagnósticos asociados; sin mensajes no significa compilado |
| task-states.json/.tsv | Tareas por fase: EXECUTED, FROM-CACHE, UP-TO-DATE, SKIPPED, NO-SOURCE, FAILED |
| diagnostics.json/.tsv, errors.txt, warnings.txt | Mensajes de Kotlin/Java/C++/XML/lint cuando tienen ubicación, conservando línea/columna; también errores sin archivo |
| tests-debug/, tests-release/, report.json | XML, conteos y fallos de pruebas separados de errores de compilación |
| lint-debug.xml/lint-release.xml y logs | Diagnósticos completos disponibles, con secretos conocidos redactados |
| apk-verification.json | Firma, certificado público, identidad, versión, integridad ZIP, ABI/ELF y avisos legales |
| summary.md | Resumen legible de fases, inventario, tareas y diagnósticos |

EXECUTED significa línea de tarea observada sin marca de reutilización; no prueba
qué archivos individuales recompiló Kotlin/Gradle. Los resultados XML pueden venir
de cache y se acompañan del estado observado de su tarea. Los archivos sin mensajes
se etiquetan NO_DIAGNOSTICS_CAPTURED y su compilación individual NOT_ESTABLISHED.
Los headers y recursos de inventario no se presentan como unidades compiladas.

En `Aurelis Music-N` (14 días), únicamente si todo está aprobado: APK debug/release,
SHA256SUMS.txt, apk-manifest.json y source-metadata.json. Los SHA corresponden a cada
APK; el digest generado por upload-artifact corresponde al archivo del artefacto,
no al APK interior. Se verifican package com.music.aurelis, firmas, versión común,
ZIP, bibliotecas ELF64 AArch64 y LICENSE/NOTICE/LICENSES íntegros. No se deduce de
ello carga JNI, alineación ELF16KB o ejecución real: pendientes previos B09/B13
se conservan, sin cambiar bibliotecas para aprobar este workflow.

## Firma y datos sensibles

Se reutilizan exclusivamente KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS,
KEY_PASSWORD y LISTEN_TOGETHER_SERVER existentes. No se cambian sus valores.
Keystore temporal en RUNNER_TEMP, modo600, eliminado antes de artefactos; no se
sube la clave privada ni su base64. Se verifica alias/certificado con keytool sin
contraseña en los argumentos; se compara el certificado público del release.
Se conserva el trim de credenciales utilizado por Gradle. Si no hay keystore se
conserva el fallback debug existente y se indica expresamente; eso no equivale a
firma oficial de publicación. Keystore configurado pero inválido/incompleto falla
sin sustituir silenciosamente la identidad. Logs e informes redactan secretos
conocidos antes de la subida; nunca se usa --debug ni se vuelca el entorno.

## Procedencia de acciones

Referencias resueltas mediante git ls-remote y manifests leídos por SHA:

| Acción | Referencia verificada | Commit fijado |
|---|---|---|
| actions/checkout | v5 | fbc6f3992d24b796d5a048ff273f7fcc4a7b6c09 |
| actions/setup-java | v5 | b6effb05e454b25005698d916606bdc6ffcbf961 |
| gradle/actions/setup-gradle | v5 (commit del tag anotado) | 0723195856401067f7a2779048b490ace7a47d7c |
| actions/upload-artifact | v6 | b7c566a772e6b6bfb58ed0dc250532a479d7789f |

Se preservan caché, nombres de artefactos y retención. La herramienta es stdlib
Python, sin nuevas dependencias del proyecto. Dos revisiones del mismo agente:
precisión/gates/firma, después fixtures/validación estática/preservación del plan.
18 pruebas offline de utilidades correctas; actionlint sin diagnósticos y11 bloques
Bash válidos. Evidencia en CI_WORKFLOW_STATIC_EVIDENCE.json. Instalación, arranque,
GPU/audio y pruebas instrumentadas Android continúan NO EJECUTADOS.

## CI02 — fallo confirmado y mejoras posteriores a B05.2

Run135 (42228085464), commit0ec482b, falló con diagnóstico Kotlin concreto en
NowPlayingScreen.kt:384: maxWidth del BoxWithConstraints exterior no es accesible
implícitamente dentro de Column por el marcador DSL de Compose. El mismo error se
repitió en debug/release y en las tres fases. La corrección calcula
portraitArtworkSize en el scope de BoxWithConstraints y pasa un Dp normal a Column.
Se conservan panel, dock, Liquid Glass y defensas QA01. No se ha compilado Android
localmente; corrección pendiente de confirmación en la nueva ejecución de GitHub.

Antes de lint/pruebas/APK hay una fase compile obligatoria (Kotlin, Java, recursos,
debug/release). Si falla, esas etapas quedan skipped y la puerta final falla.
Si compile pasa, lint y pruebas pueden recopilar sus diagnósticos independientes;
APK requiere ambos correctos. Evita tres invocaciones repitiendo un fallo de Kotlin
sin omitir comprobaciones necesarias ni publicar APK incompletos.

Se añade task-inputs.gradle mediante --init-script en las cuatro fases Gradle.
Lee exclusivamente las entradas declaradas de tareas de Kotlin/Java/recursos/
assets/shaders/CMake y registra paths del inventario o fuentes generadas locales;
no imprime contenidos, propiedades de firma, classpath completo ni secretos.
No se habilita --debug/--info global ni se añaden plugins/dependencias/build scans.
El callback beforeTask requiere configuración tradicional de Gradle8.11.1; este
workflow fuerza --no-configuration-cache para garantizar esa compatibilidad. La
build-cache continúa habilitada.

Nuevos archivos del artefacto compilation-report-N:

| Archivo | Lectura correcta |
| --- | --- |
| compile.log | Primera compilación previa y sus errores |
| task-inputs.json/.tsv | Fase, tarea, path, origen tracked/generated y estado de tarea |
| file-status.tsv | Archivo, source set, diagnósticos, presencia de evidencia y estados de tareas observadas |

Estas entradas NO son una lista de archivos compilados exitosamente. Java puede
ser entrada de Kotlin solo para resolver símbolos; headers se incluyen en unidades
nativas; recursos/assets pueden procesarse o empaquetarse sin compilarse como código.
Cache/UP-TO-DATE no significan compilación ejecutada otra vez. Fallar una tarea no
significa que todos sus archivos tengan errores. NO_TASK_INPUT_EVIDENCE significa
que falta prueba, nunca que el archivo se excluyó o compiló. Todos conservan
individual_compilation=NOT_ESTABLISHED; los fallos con archivo/línea se identifican
separadamente. Las fuentes generadas se detallan en task-inputs, aunque no estén en
el inventario Git. No se obtiene trazabilidad individual universal de compiladores
con solo el estado de una tarea Gradle.

Se priorizan errores con ubicación en GITHUB_STEP_SUMMARY, se colapsan mensajes
idénticos por fase y se conservan logs completos y XML. La pérdida/malformación de
telemetría se registra como error del recolector, no como éxito silencioso.

Se mantienen mecanismos nativos documentados por GitHub: anotaciones, resumen,
artefactos incluso tras fallo, permisos mínimos, SHA de acciones, secretos efímeros
y aprobación por outcome. Referencias oficiales:
https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-commands
https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions
La consulta web de documentación en este entorno devolvió403; no se afirma una
auditoría/certificación de GitHub ni haber leído una revisión remota inaccesible.
Las anotaciones públicas del run sí pudieron leerse; la API gh devolvió Forbidden.

Pruebas locales de utilidades:21 casos correctos, actionlint sin diagnósticos,
12 bloques Bash con sintaxis válida y sintaxis Groovy comprobada con compilador
standalone de las bibliotecas existentes. No se ejecutó Gradle ni compilación,
APK, prueba Android o emulador. La integración real del init script y la corrección
Kotlin deben verificarse en GitHub. B06 y QA02 no iniciados.
