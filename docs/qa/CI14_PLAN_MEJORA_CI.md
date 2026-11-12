# CI14 — Plan de mejora de CI y evidencia por archivo

Estado: **PLAN — no implementado.** Ningún workflow, script, Gradle ni código de la app se modifica en esta entrega.
Base: `feature/bitchord-local-migration` @ `f6309d2`.
Último CI verde del código: run #41 (`09b848d`, pipeline B01–B06).
Autorización: el plan debe aprobarse antes de cualquier fase. Cada fase se entrega por separado, con CI en GitHub y aviso al usuario.

---

## 1. Objetivo y no-objetivos

**Objetivo**

1. Cerrar riesgos de seguridad en CI (publicación de APK firmados, subida a VirusTotal, secretos mal clasificados).
2. Tener un único pipeline canónico, más rápido y con menos duplicación.
3. Producir una matriz por archivo que diga qué fuentes compilaron y llegaron al APK, sin falsos positivos.
4. Mejorar la calidad de las pruebas automáticas (estabilidad, cobertura útil, controles del propio workflow).
5. Reducir la documentación de evidencia a una sola fuente de verdad.

**No-objetivos**

- No cambiar la lógica de la app (playback, UI, DSP, licencias de modelos) salvo lo indicado en la fase F7, y solo tras verificación.
- No reclamar compilación individual de archivos a partir de logs de Gradle sin cruzar evidencias.
- No afirmar audio físico, gapless, USB o rendimiento a partir del emulador.
- No hacer force-push, no tocar `main`, no cambiar firmas ni identidad de la app sin autorización expresa.

---

## 2. Diagnóstico (hallazgos verificados)

| # | Hallazgo | Evidencia | Riesgo |
|---|---|---|---|
| H1 | El APK firmado de release se sube a VirusTotal en cada ejecución de B04 cuando existe el secreto. | `.github/scripts/virustotal_scan.py` (encabezado: "Public uploads share the submitted APK"); `b04-verify-apks.yml` paso `virustotal` | Alto |
| H2 | B04 publica APK firmados desde cualquier push a la rama, con `secrets: inherit`. Sin environment de protección. | `b06-unified-pipeline.yml` (`secrets: inherit`); `b04-verify-apks.yml` pasos `release_apk`/`debug_apk` | Alto |
| H3 | `buildTypes.release` cae a firma debug si no hay keystore. | `app/build.gradle.kts:169-170` | Medio (mitigado por gate de CI) |
| H4 | `LISTEN_TOGETHER_SERVER` se trata como secreto, pero se compila en `BuildConfig` y es extraíble del APK. | `app/build.gradle.kts:65,92`; 7 referencias en workflows | Bajo (mala clasificación) |
| H5 | Dos pipelines: `android.yml` (solo `main`, lint debug+release) y B01–B06 (rama, lint solo debug). | `.github/workflows/android.yml`; `b02-android-lint.yml` | Medio |
| H6 | El build se repite en B01, B02, B03, B04 y B05, en cadena secuencial, con SDK reinstalado en cada job. | Cadena `needs:` en `b06-unified-pipeline.yml` | Alto (tiempo) |
| H7 | `--no-daemon` y `--no-configuration-cache` en todos los pasos de Gradle. | 11 ocurrencias en workflows | Medio (tiempo) |
| H8 | `task-inputs.gradle` solo se usa en algunos jobs; la evidencia no es comparable entre fases. | `.github/scripts/task-inputs.gradle`; uso en android.yml y B04 | Medio |
| H9 | `ReactiveCircus/android-emulator-runner@v2.38.0` no está fijado a SHA, a diferencia del resto. | `b05-android-emulator.yml:103` | Medio (supply chain) |
| H10 | B05 hardcodea `"test_count": 7` y la lista de clases de test. | `b05-android-emulator.yml` bloque de informe | Bajo |
| H11 | `COMPILER_TASK_FAILED_WITH_INPUT_NOT_FILE_FAULT` afirma que el archivo no tiene culpa sin evidencia de error atribuido. | `.github/scripts/p2_compiler_evidence.py`, función `compiler_state` | Medio (falso positivo de etiqueta) |
| H12 | Seis capas de evidencia solapadas (`p2_inventory`, `p2_compiler_evidence`, `p2_apk_trace`, `verify_evidence`, `b04_gradle_diagnostics`, `apk_audit`). | `.github/scripts/` | Medio (mantenimiento) |
| H13 | Una espera con `Thread.sleep(5)` en un test de sesión. | `app/src/test/.../playback/MediaSessionBridgeTest.kt:82` | Medio (flakiness) |
| H14 | No hay `dependabot.yml` ni control de workflows (actionlint/zizmor). | Ausencia en `.github/` | Medio |
| H15 | Clases de Media3 FFmpeg copiadas en el código fuente, junto con la dependencia `nextlib-media3ext`. | `app/src/main/java/androidx/media3/decoder/ffmpeg/` (4 archivos); `app/build.gradle.kts:231` | Medio (posible duplicación/versión) |
| H16 | `org.json:json` como dependencia; Android ya incluye `org.json`. | `app/build.gradle.kts:215` | Bajo |
| H17 | `targetSdk = 35` con `compileSdk = 36`. | `app/build.gradle.kts:74,81` | Medio (requisitos de Play; verificar vigente) |
| H18 | La documentación está desfasada: `PROGRESS.md` termina en CI13 pendiente, con commits P3 posteriores. | `docs/qa/PROGRESS.md`; historial | Medio |

**Resultados de verificación local (sin Android SDK):**

- Suite `.github/scripts/tests`: 85 pruebas, todas OK.
- Pruebas `.github/scripts/test_*.py`: 52 pruebas, todas OK.
- Tests de app: ~280 `@Test` en 50 archivos, en su mayoría Robolectric con I/O real. No ejecutados aquí.

---

## 3. Principios de ejecución

1. **Una fase, una entrega.** Cada fase se publica en su propio commit o serie de commits, con su CI en GitHub.
2. **Límite de 30 rutas por bloque**, según la regla vigente de `PLAN_30_FILES.md`. Si una fase necesita más, se divide en `F2-a`, `F2-b`, etc. antes de editar.
3. **Sin evidencia inflada.** Todo informe conserva `individual_compilation_proven: false` hasta que la matriz de F4 lo justifique.
4. **Fallo visible.** `continue-on-error` solo para recoger diagnósticos; la aprobación exige `result == success` en las fases obligatorias.
5. **Rollback simple.** Cada fase deja el pipeline anterior operativo hasta que la nueva versión pase en CI.
6. **Cambios de app y build config requieren autorización explícita** (fase F7 y los puntos H3, H16, H17).

---

## 4. Fases

### F0 — Línea base y decisiones (sin cambios de código)

**Tareas**
- Confirmar que el último SHA de código (`09b848d`) sigue verde en CI.
- Registrar tiempos de referencia de B01–B06 (duración total y por job) desde la API o la interfaz de GitHub.
- Clasificar los archivos de `docs/qa/` en: vigente, histórico, reemplazable (ver F6).
- Registrar las decisiones de la sección 6 con respuesta del usuario.

**Archivos:** solo este documento y, si se aprueba, `docs/qa/PROGRESS.md` (una entrada).
**Criterio de salida:** tiempos base documentados y decisiones D1–D6 respondidas.

### F1 — Seguridad de CI y secretos (prioridad inmediata)

**Tareas**
1. **VirusTotal solo por hash por defecto.** `virustotal_scan.py` consulta `/files/{sha256}` sin subir. La subida real pasa a un `workflow_dispatch` con input explícito y con environment de aprobación. (H1)
2. **Environment `release`.** Crear el environment en GitHub con revisores obligatorios y ramas/tags permitidos (`main` y tags `v*`). Los pasos que usan `KEYSTORE_*` y publican APK firmados se ejecutan solo bajo ese environment. Los pushes a la rama de trabajo siguen compilando y verificando, pero sin publicar release firmado. (H2)
3. **Release sin firma debug.** En `app/build.gradle.kts`, el buildType `release` falla si no hay keystore, salvo que se pase una propiedad explícita (`-PallowDebugSigned=true`) usada solo en local. (H3) *Requiere autorización: toca build config.*
4. **`LISTEN_TOGETHER_SERVER` como variable.** Mover de `secrets.` a `vars.` en los 7 usos; actualizar la lista de `redact()` en `android_ci.py` para no ocultar una URL pública necesaria en el log. (H4)
5. **Mínimo privilegio y SHA.** Confirmar `permissions: contents: read` en todos los workflows salvo B06 (que necesita `actions: write` para limpiar artefactos; documentarlo).

**Archivos previstos (≤30):** `.github/workflows/b04-verify-apks.yml`, `b06-unified-pipeline.yml`, `b05-android-emulator.yml`, `.github/scripts/virustotal_scan.py`, `.github/scripts/android_ci.py`, `.github/scripts/test_virustotal_scan.py`, `.github/scripts/tests/test_android_ci.py`, `app/build.gradle.kts`.

**Validación:**
- Pruebas Python existentes + nuevas para el modo solo-hash y la ausencia de subida sin input.
- En GitHub: run en la rama confirma que no se publica release; run `workflow_dispatch` con environment confirma la publicación bajo aprobación.
- Local: `./gradlew :app:assembleRelease` sin keystore falla con mensaje claro.

**Rollback:** revertir commit; los environments y secretos no cambian de valor.

### F2 — Pipeline único y canónico

**Decisión previa D1:** ¿el pipeline canónico es B01–B06 (recomendado, ya tiene matriz de evidencia) o `android.yml`?

**Tareas**
1. Eliminar `android.yml` o convertirlo en un caller fino del pipeline unificado (según D1).
2. Igualar el lint: release y debug en B02 (paridad con `android.yml`).
3. Homogeneizar `task-inputs.gradle` en todos los jobs que compilan (H8).
4. Agregar `paths-ignore` coherente entre `push` y `pull_request`, para que la rama y `main` se comporten igual.

**Archivos previstos:** `.github/workflows/android.yml`, `b02-android-lint.yml`, `b01-compile-debug.yml`, `b03-unit-tests.yml`, `b04-verify-apks.yml`, `b05-android-emulator.yml`, `b06-unified-pipeline.yml`.

**Validación:** misma cobertura de fases que antes; mismo número de artefactos finales (tres, según `b06_cleanup_artifacts.py`).

### F3 — Eficiencia (tiempo y coste)

**Tareas**
1. **Construir una vez.** Un job `build` produce `app-debug.apk`, `app-release.apk` (si aplica), clases y `mapping.txt`, y los sube como artefacto intermedio. Lint, tests y emulador descargan ese artefacto en lugar de recompilar. (H6)
2. **Fan-out en paralelo** tras `build`: lint, tests JVM y emulador corren a la vez. Solo el gate final espera a todos.
3. **Caché de SDK.** Cachear `platforms;android-36`, `build-tools;35.0.0`, `ndk;26.1.10909125` y `cmake;3.22.1` con clave por versión.
4. **Gradle.** Probar `--configuration-cache` en una rama de prueba; quitar `--no-daemon` en CI si la memoria lo permite (`--max-workers=2` se mantiene). (H7)
5. **Concurrencia.** Revisar `cancel-in-progress` para que un push nuevo no cancele evidencia de un SHA ya verificado.

**Archivos previstos:** `b01-compile-debug.yml`, `b03-unit-tests.yml`, `b05-android-emulator.yml`, `b06-unified-pipeline.yml`, `gradle.properties`.

**Métrica de éxito:** tiempo total del pipeline medido contra la línea base de F0. Meta inicial: reducción de al menos 30 % en tiempo de pared. Si no se cumple, se documenta y no se avanza.

### F4 — Matriz de compilación por archivo

**Definición de "compilado y en el APK"** (todas las condiciones):
1. La compile task del archivo terminó en `SUCCESS`, `FROM-CACHE` o `UP-TO-DATE` en esta ejecución.
2. Existen clases de salida de esta ejecución cuyo `SourceFile` coincide con el archivo, en el paquete declarado.
3. Esas clases aparecen en el DEX del APK (en release, a través de `mapping.txt` de R8).
4. Para C/C++: existe el `.o` del archivo en `.cxx/.../obj/arm64-v8a/` de esta ejecución.

**Veredictos posibles:**

| Veredicto | Significado |
|---|---|
| `COMPILADO_EN_APK` | Cumple 1–3 (o 1 y 4 para nativo). |
| `ERROR_ATRIBUIDO` | Hay diagnóstico de error con ese archivo. Bloquea la aprobación. |
| `SIN_CLASES_ESPERADAS` | Archivo sin declaraciones emitibles (solo `package`/`import`/`typealias`). No es fallo. |
| `NO_EJECUTADO` | La compile task no terminó en éxito y no hay error atribuido. Bloquea la aprobación. |
| `NO_EVIDENCIA_SUFICIENTE` | Falta alguna pieza (p. ej. mapping de R8). Bloquea la aprobación en release. |
| `AMBIGUO` | Dos fuentes con el mismo nombre en el mismo paquete. Requiere revisión. |
| `FUERA_DE_PRODUCCION` | Test o fuera del alcance del APK por política. |

**Tareas**
1. Crear `.github/scripts/file_compile_matrix.py` que genere `file-compile-matrix.tsv` y `.json`, consolidando la lógica de `p2_compiler_evidence` y `apk_audit` en un único flujo.
2. Renombrar la etiqueta H11 a `COMPILER_TASK_FAILED_SIN_ERROR_ATRIBUIDO` y mapearla al veredicto `NO_EJECUTADO`.
3. Tratar `package` dentro de comentarios de bloque como no válido al extraer el paquete (la regex actual puede coincidir con texto en comentarios).
4. Leer fuentes con `encoding="utf-8"` explícito.
5. Criterio de aprobación: cero `ERROR_ATRIBUIDO` y cero `NO_EJECUTADO`; `NO_EVIDENCIA_SUFICIENTE` bloquea solo en release.
6. Retirar `p2_inventory`, `p2_compiler_evidence` y `p2_apk_trace` cuando la matriz esté verde durante dos ejecuciones consecutivas. `verify_evidence.py` y `b04_gradle_diagnostics.py` se evalúan por separado.

**Archivos previstos:** `.github/scripts/file_compile_matrix.py` (nuevo), `.github/scripts/tests/test_file_compile_matrix.py` (nuevo), `.github/scripts/p2_compiler_evidence.py`, `.github/scripts/apk_audit.py`, `.github/scripts/android_ci.py`, `.github/scripts/b06_bundle_reports.py`, `b04-verify-apks.yml`, `b06-unified-pipeline.yml`.

**Validación:** fixtures con fuentes de ejemplo (un archivo sin clases, un archivo con error, un nombre duplicado, un archivo en release con clase eliminada por R8) y casos esperados por veredicto.

### F5 — Calidad de pruebas automáticas

**Tareas**
1. Reemplazar `Thread.sleep(5)` en `MediaSessionBridgeTest.kt:82` por espera basada en `idle()` de Robolectric o en el dispatcher de test. (H13)
2. **Cobertura sobre el diff.** Añadir JaCoCo o Kover con reporte XML; umbral solo sobre líneas cambiadas en el PR, no global (evita premiar cobertura trivial).
3. **Política de flakiness.** Un test que falla y pasa en reintento se marca en el informe; tres ocurrencias en 14 días lo mueven a cuarentena con issue abierto.
4. **Parametrizar B05.** Derivar `test_count` y la lista de clases de los XML de JUnit en lugar de constantes. (H10)
5. **Pinear la acción del emulador** a SHA. (H9)
6. **Análisis estático.** Añadir detekt y ktlint con baseline inicial, para que no bloqueen código antiguo.
7. **Controles del workflow.** `actionlint` y `zizmor` en un job de `helpers`.
8. **Dependabot** para `gradle` y `github-actions`, con agrupación semanal.

**Archivos previstos:** `app/src/test/java/com/music/aurelis/playback/MediaSessionBridgeTest.kt`, `app/build.gradle.kts` (solo plugins de cobertura y lint, con autorización), `build.gradle.kts` (raíz, para detekt/ktlint), `.github/dependabot.yml` (nuevo), `.github/workflows/b05-android-emulator.yml`, `.github/workflows/b03-unit-tests.yml`, `.github/workflows/b01-compile-debug.yml`, `.github/scripts/b06_bundle_reports.py`, `.github/scripts/tests/test_android_ci.py`.

**Validación:** `./gradlew :app:testDebugUnitTest` estable en 3 ejecuciones consecutivas; `actionlint` sin errores; informe de cobertura generado.

### F6 — Documentación como fuente de verdad única

**Tareas**
1. Un solo `PROGRESS_TRACKER` por bloque en `docs/migration/beta-local/`, con estado derivado de los JSON de CI, no escrito a mano.
2. Archivar en `docs/qa/archive/` los informes CI01–CI13 cuyo contenido ya esté reflejado en el tracker. No borrar: solo mover.
3. Corregir el desfase de H18: una entrada de estado que indique el último SHA verde y las fases pendientes.
4. Reducir las reglas duplicadas (la regla de 30 archivos aparece en varios documentos) a una referencia.

**Archivos previstos:** `docs/qa/PROGRESS.md`, `docs/qa/CI_WORKFLOW.md`, `docs/migration/beta-local/PROGRESS_TRACKER.md`, `docs/migration/beta-local/PLAN_30_FILES.md`, y los archivos de `docs/qa/` que se archiven (se contarán dentro del límite de 30 rutas, por lotes).

**Nota:** esta fase solo se ejecuta cuando F4 esté estable, para no perder evidencia en medio de cambios de pipeline.

### F7 — Higiene de la app (solo con autorización explícita)

Estas tareas no son de CI. Se hacen únicamente tras verificación previa y aprobación.

1. **H15 — Media3 FFmpeg.** Comprobar si las clases copiadas en `app/src/main/java/androidx/media3/decoder/ffmpeg/` duplican las de `nextlib-media3ext` (mismo paquete y clases). Si duplican, eliminar la copia y validar que el decoder sigue cargando `libffmpegJNI.so` en el emulador. Verificación previa: comparar contra el AAR de la dependencia, sin cambiar código.
2. **H16 — `org.json`.** Eliminar la dependencia si no hay uso que requiera la versión externa; validar con pruebas.
3. **H17 — `targetSdk`.** Revisar los requisitos vigentes de Google Play antes de cambiarlo; no subir a ciegas.

**Archivos previstos:** `app/build.gradle.kts`, y los 4 archivos de `androidx/media3/decoder/ffmpeg/` si se eliminan.

---

## 5. Orden, dependencias y entregas

```
F0 ──► F1 ──► F2 ──► F3 ──► F4 ──► F5 ──► F6
                                  │
                                  └──► F7 (independiente, con autorización)
```

- F1 es independiente de F2–F4 y puede entregarse primero.
- F4 depende de F2 (un solo pipeline) y de F3 (artefacto de build compartido).
- F6 se hace al final, cuando F4 es estable.
- F7 puede hacerse en cualquier momento tras su verificación previa.

**Entregas sugeridas:** una por fase. Cada entrega incluye: SHA de inicio y fin, rutas tocadas (≤30), resultado de CI por SHA, y bloqueos.

---

## 6. Decisiones que necesito del usuario

| ID | Pregunta | Recomendación |
|---|---|---|
| D1 | ¿Pipeline canónico: B01–B06 o `android.yml`? | B01–B06 (ya tiene matriz de evidencia y es el de la rama). |
| D2 | ¿Se permite publicar APK firmados solo desde `main` y tags, bajo environment con revisor? | Sí. |
| D3 | ¿VirusTotal: solo consulta por hash por defecto, subida manual? | Sí. |
| D4 | ¿Se autoriza modificar `app/build.gradle.kts` (H3, H16, H17)? | Sí, por separado de CI. |
| D5 | ¿Se acepta eliminar la copia de Media3 FFmpeg si duplica la dependencia (H15)? | Sí, tras verificación. |
| D6 | ¿Límite de tiempo objetivo para el pipeline completo? | Meta inicial de −30 % frente a la línea base. |

---

## 7. Riesgos

| Riesgo | Mitigación |
|---|---|
| Cambiar el pipeline oculta regresiones durante la transición. | Mantener el pipeline anterior hasta que el nuevo pase dos ejecuciones verdes (F2). |
| Build compartido entre jobs introduce dependencia de artefactos corruptos. | Verificar hash de los artefactos intermedios antes de usarlos (F3). |
| Matriz por archivo marca falsos negativos en release por R8. | Usar `mapping.txt` y tratar `NO_EVIDENCIA_SUFICIENTE` como bloqueo solo en release (F4). |
| Environment de release bloquea la publicación a tiempo. | El revisor es el usuario; documentar el procedimiento de aprobación. |
| Emulador inestable en GitHub. | Política de flakiness y cuarentena (F5). |
| Cobertura por diff puede ser difícil de calcular en merges. | Comparar contra el merge-base que ya calcula `prepare` (`android_ci.py`). |

---

## 8. Registro al cerrar cada fase

| Dato | Evidencia |
|---|---|
| Fase y ID | `F1`…`F7` o subdivisión |
| SHA inicial y final | Rama `feature/bitchord-local-migration` |
| Rutas previstas y reales | Lista deduplicada, ≤30 |
| Cambios y riesgos | Sin violar exclusiones ni licencias |
| Revisión 1 / revisión 2 | Revisiones separadas |
| CI y pruebas | Resultados por SHA, fallos reales |
| Bloqueos y siguiente fase | Hardware, permisos, datos faltantes |

---

## 9. Qué no está verificado en este plan

- No se ejecutó Gradle ni el build Android en este entorno (no hay Android SDK).
- No se pudo leer el log del run #38 (fallido) porque el token de GitHub del entorno es inválido.
- Las versiones de SDK, acciones y requisitos de Play deben revalidarse en el momento de cada fase.

---

## 10. Registro de ejecución

Decisión D1 tomada por el usuario: pipeline canónico **B01–B06**.

| Fase | Estado | Commit | Notas |
|---|---|---|---|
| F0 | Línea base cerrada | (documentación F0) | API de GitHub consultada: run #41 29,3 min; #40 30,4 min; #50 27,4 min, #51 30,8 min (B05 falló). Detalles en CI14_F0_BASELINE.md. Decisiones D1-D3 y D6 incorporadas al alcance; D4 solo H3 en F1, D5 se reserva para F7. |
| F1 | Seguridad fail-closed aplicada; configuración protegida pendiente | `dcc8aaf` + entrega F1 de 2026-11-12 | B04 consulta VirusTotal solo por hash sin que una variable del repositorio habilite subidas. Gradle exige keystore para ensamblar Release y solo permite firma Debug bajo `-PallowDebugSigned=true` explícito. La publicación oficial queda bloqueada hasta disponer de un job con environment `release` y revisores verificados. La variable `LISTEN_TOGETHER_SERVER` todavía debe configurarse; se conserva la compatibilidad con secreto hasta entonces. |
| F2 | Consolidado; CI de este SHA pendiente | `2e88ebe`, `7a9c748` (SHA de emulador), siguiente commit (unificación) | Lint debug+release en B02; `task-inputs.gradle` en B01/B03/B05; B06 valida PR hacia `main`; `android.yml` retirado. |
| F3 | Pendiente | — | Build compartido y paralelización. |
| F4–F7 | Pendiente | — | Ver secciones 4 y 5. |

Desviaciones respecto al plan:
- VirusTotal: la subida se activa con variable de repo, no con input de `workflow_dispatch`, porque B04 se invoca con `workflow_call`.
- `redact()` en `android_ci.py` no se modificó: protege el log y no hace falta quitarlo.
- Publicación de release desde `main` (push) no está habilitada: falta el environment `release`. B06 aún no se dispara en push a `main`.

Efectos colaterales a comunicar:
- El check de PR ahora se llama "Aurelis | Unified B01-B06 Pipeline". Si la protección de rama exige "Build Release APK", hay que actualizar esa regla.
- Documentos históricos (B06_1, B07_1) siguen mencionando `android.yml`; no se reescriben como evidencia.

## 11. F0 actualizado — medición reproducible (2026-11-12)

Evidencia de los tiempos y decisiones de F0: [CI14_F0_BASELINE.md](CI14_F0_BASELINE.md).
El CI del PR #51 falló en B05 por el cambio a IDLE de Media3; no se hereda el verde del push #50.
La fase F1 conserva la prioridad de seguridad y no habilita publicación sin protección del environment `release`.

## 12. CI14 F1 — refuerzo de seguridad, entrega 2026-11-12

1. F1-H1: B04 fija `VIRUSTOTAL_ALLOW_UPLOAD=false`. Ninguna variable del repositorio puede publicar el binario por el flujo B01–B06. Los informes siguen distinguiendo `HASH_LOOKUP_ONLY` y `NOT_CONFIGURED` de un análisis limpio. Una futura subida manual exige un workflow independiente y protegido; no se crea uno sin autorización y protección reales.
2. F1-H3: `assembleRelease` requiere keystore válido y no se firma automáticamente con la clave Debug. Para una prueba local explícita existe `-PallowDebugSigned=true` (nunca configurado en CI); los gates de B04 siguen rechazando cualquier publicación no firmada con el certificado configurado. Debug conserva su firma Debug independiente.
3. F1-H2: publicación oficial Release desactivada de forma estricta en B04, porque los environments de GitHub se protegen por job, no por step. **No se afirma** que exista el environment `release` ni un revisor obligatorio. Se requiere configurar las reglas desde GitHub y trasladar la publicación a un job protegido antes de habilitar Release. B01–B06 sigue dando informes y Debug solamente.
4. F1-H4: `LISTEN_TOGETHER_SERVER` prioriza `vars`, pero conserva temporalmente la compatibilidad con el secreto de repositorio. No se puede afirmar que esté migrado sin observar/configurar `vars.LISTEN_TOGETHER_SERVER` en GitHub.
5. F1-H5: se mantienen permisos mínimos existentes; B06 requiere `actions: write` para borrar temporales únicamente tras PASS.

**Estado:** salvaguardas de código aplicadas. La configuración de GitHub (environment `release`, revisores, variable de repositorio y eventual workflow manual) sigue pendiente. F1 no se declarará operativamente completa hasta verificarla. Los tests y el pipeline de este commit no se presumen verdes sin observarlos.

## 13. CI14 F2 — consolidación (2026-11-12)

- `android.yml` fue retirado anteriormente. Los eventos push/PR de B06 usan el mismo conjunto explícito de paths (GitHub no permite combinar `paths` con `paths-ignore`).
- B02 ya ejecutaba lint Debug y Release. Se corrige el resumen para que incluya los hallazgos y el XML de cada variante; antes únicamente contabilizaba Debug. La utilidad `b02_lint_summary.py` y sus pruebas impiden etiquetar resultados ausentes o inválidos como PASS.
- `task-inputs.gradle` y las políticas previas de B01–B06, ARM64 y publicación segura siguen vigentes. La validación remota del nuevo SHA debe observarse antes de declarar todo verde.
