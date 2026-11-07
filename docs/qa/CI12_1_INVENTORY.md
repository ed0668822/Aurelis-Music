# CI12.1 — inventario y nombres de entrega

Base dca9532f58b9f9ab1f9d9a117f3b4211c9d71f36, misma rama/PR1.
Base con run149 SUCCESS ya observado en Actions y job128211015242; CodeQL1318
SUCCESS observado. No es evidencia para los cambios de esta entrega.

Archivos de implementación: .github/scripts/android_ci.py,
.github/scripts/task-inputs.gradle, .github/scripts/tests/test_android_ci.py.
Workflow existente ya ejecuta ambos helpers y publica ci-artifacts completo;
no se cambia el YAML ni se añaden compilaciones.

El inventario anterior contaba439 fuentes/recursos. La inspección de esta base
cubre575 archivos versionados, incluidos .so y CMakeLists antes omitidos. No es
un número fijo: GitHub incluirá también los nuevos documentos de esta entrega.
Tipos/expectativas no equivalen a pertenencia efectiva a un sourceSet ni a APK.
Configuración sensible conocida no se lee/hash; symlink no se sigue; archivo
versionado faltante permanece como evidencia incompleta en el inventario.

Se añaden source-roots.json y compilation-audit.json/.tsv; este último distingue
por variante entradas observadas con tarea terminal correcta/reutilizada/fallida,
omisión, referencia desde otra tarea y ausencia de evidencia. Raíces/entradas no
son compilación individual. Casos nativos, APK/DEX/R8 y conexiones UI seguirán
requiriendo CI12.2/.3; el reporte no los marca aprobados. Excepciones/recolección
malformada/contradicciones no se silencian. Rutas no invalidan evidencia real de
sourceSets personalizados. Pruebas/documentos no se presentan como faltantes APK.

Nombres: Aurelis-Music-V2.1.apk y Aurelis-Music-V2.1-debug.apk son ejemplos,
NO una versión impuesta. Se usa versionName del APK verificado, permitiendo
componentes ASCII alfanuméricos, punto, guion, guion bajo y +, hasta100 caracteres.
Una versión que no pueda representarse con esa política requiere revisión;
no se cambia silenciosamente el número. Colisiones de APK divididos también
requieren política explícita; no se sobrescriben. build_file referencia el archivo
Gradle original, file el publicado; SHA256SUMS usa el nombre publicado.
La copia no modifica bytes/firma y los fallos retiran cualquier conjunto parcial.

Verificación:47 pruebas Python de fixtures PASAN (31 previas y16 nuevas); no
invocan Gradle/SDK ni compilan Android. Incluyen reporte integrado, caché/omisiones,
Java usado por Kotlin, tipos, symlink/faltantes, resultados contradictorios,
referencias a nombres/hashes, igualdad de bytes y fallos de publicación.
Primera revisión estática: clasificación/estado/API y límites. Segunda revisión
separada por el mismo agente: fixtures/regresiones/privacidad/publicación/alcance.
Corregido que evidencia de tarea tiene prioridad sobre convención de carpeta.
Diff --check correcto; plan, app y gates de workflow intactos.

Gradle hook NO EJECUTADO aquí, Kotlin/Java/CMake/Android/CI nuevos PENDIENTES de
GitHub. Fixtures ZIP/ELF sintéticos no acreditan un APK real o arranque Android.
Tras publicar: avisar y esperar resultado del usuario. CI12.2/.3 ya autorizados,
pero no iniciados en esta entrega. No B09.
