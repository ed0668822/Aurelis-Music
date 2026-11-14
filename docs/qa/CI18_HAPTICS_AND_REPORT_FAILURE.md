<!-- Aurelis © 2026. GPL-3.0-or-later -->
# CI18 — Run117: constantes hápticas y consolidación de fallos

## Autorización, base y presupuesto

El usuario autorizó corregir ambas causas, subir a la misma rama y comprobar
el resultado real de B01–B06 después del push. Base
090833626b014d74d68451c176df5407c6eb6cd1; rama
feature/bitchord-local-migration, PR1 sin cambio de título. Fetch y estado
inicial limpio, sin commits locales exclusivos. Cuenta/identidad local
ed0668822 / ed0668822@gmail.com, correo verificado por API.

Máximo solicitado:30 archivos por bloque. Esta entrega tiene cinco rutas:

| Archivo | Responsabilidad |
|---|---|
| app/src/main/java/com/music/aurelis/ui/haptics/Haptics.kt | Comprobación de soporte con constantes del SDK. |
| .github/scripts/b06_bundle_reports.py | Consolidar fallos de evidencia sin certificar fuentes ausentes. |
| .github/scripts/test_b06_file_matrix.py | Siete regresiones nuevas, ejecutadas por el paso B06 existente. |
| docs/qa/CI18_HAPTICS_AND_REPORT_FAILURE.md | Diagnóstico, verificaciones y resultado. |
| docs/migration/beta-local/PROGRESS_TRACKER.md | Estado y límite vigente de esta entrega. |

## Evidencia y acceso

Run117:43515196097, commit09083362. API: B01/B03/B05 success; B02 failure;
B04 skipped; B06 failure. Los dos errores concretos fueron aportados por el
usuario y contrastados con el código. No se afirma haber leído logs completos.

Se ejecutaron gh auth status, gh run view --log-failed, gh run download del
artefacto consolidado y gh run view --job --log para B02/B06. La cuenta activa
es ed0668822. Descargas CLI/API de los artefactos14079664586 y14079784365:
Forbidden. Los comandos gh run view devolvieron contenido vacío; los endpoints
API de logs de jobs132197321758 y132199658052 devolvieron Forbidden. Credenciales
y URLs firmadas no forman parte del informe. La reparación continúa sobre el
diagnóstico aportado y reproducción local de fixtures, sin depender del ZIP.

## B02 — causa y corrección

La reparación09083362 corrigió addPrimitive, pero dejó
areAllPrimitivesSupported(*primitives) con un IntArray construido desde una
propiedad Int. Esa llamada todavía incumplía la comprobación estática WrongConstant
de Lint. No es prueba de incompatibilidad física ni fallo de compilación Kotlin.

Se comprueban los tipos de golpe distintos mediante when exhaustivo y llamadas
con PRIMITIVE_TICK, PRIMITIVE_LOW_TICK y PRIMITIVE_CLICK explícitos. Se exige
soporte de todos los tipos utilizados antes de componer; el rechazo de cualquiera
mantiene los fallbacks de onda/amplitud. Se elimina el campo Int redundante del
enum. Los mismos valores del SDK se conservan en addPrimitive. No hay Suppress,
NoInspection, baseline nueva ni cambio de SDK/ABI/firmas/gates. Ritmos, fuerzas,
pausas, caché y preferencias siguen intactos. Los23 casos existentes de
SharedGesturesTest ya cubren soporte completo, parcial y ausente; no se duplican.

## B06 — origen, validación y fallo secundario

p2-apk-trace.json es evidencia producida por p2_apk_trace.py en B04 y publicada
en el artefacto b04-verification-report. El consolidado la copia a
B04/Verificacion. B04 exige B02 y B03 correctos: un fallo de Lint deja B04 skipped,
por lo que ese JSON no existe. No corresponde obtenerlo de BitChord ni crearlo
desde resultados supuestos: debe proceder del APK y del commit que se verifican.

file_compile_matrix.generate valida archivo seguro, procedencia SHA, filas y
pareado de trazas. Su llamada en B06 estaba fuera del manejo de errores, antes
de generar pipeline-results.json. Reproducción con el script original y un
fixture de B04 omitido: ValueError exacto
Missing or unsafe evidence: p2-apk-trace.json.

La reparación conserva ese generador y su validación estricta. Si no puede
generarse F4, el consolidado escribe únicamente un diagnóstico NOT_GENERATED,
con error observado, SHA esperado y resultado original de B04. No crea trazas,
matrices de clases vacías presentadas como completas ni resultados aprobados.
Retira solo tres salidas F4 derivadas antiguas, evitando publicar una matriz
exitosa residual. La comprobación de integridad queda FAIL y el nuevo check
f4_matrix_generated impide PASS. Se conservan B04=skipped y los demás estados.

La consolidación puede terminar para publicar el diagnóstico y el índice. Su
resultado global sigue siendo FAIL; la puerta final del workflow conserva el
fallo y no permite publicar una entrega aprobada. En entradas completas se usa
el generador original y las comprobaciones existentes; no se relaja la evidencia.
Los workflows, el productor B04 y sus condiciones no cambian.

## Verificaciones y dos revisiones

- Reproducción del fallo con el código original: confirmada usando fixtures
  sintéticos; no ejecución Android ni sustitución de evidencia de producción.
- Siete pruebas nuevas: B04 skipped; job success sin evidencia; JSON malformado;
  enlace simbólico; matriz derivada antigua; generación válida posterior a un
  fallo; SHA de traza incorrecto. Los casos incompletos conservan FAIL.
- Suite raíz Python:136 pruebas PASS. Suite scripts/tests:86 pruebas PASS.
  Total222; incluye las13 de test_b06_file_matrix. Ejecuciones sin red/Gradle.
- Revisión1 del mismo agente: contratos IntDef y soporte real, productor B04,
  reproducción de la excepción y preservación de fases/gates.
- Revisión2 del mismo agente: casos negativos/positivo, alcance de limpieza,
  sintaxis/diff, cinco rutas y conservación de tests Android/licencias/firmas/ABI.

No son revisiones independientes. No se ejecutan Gradle, Lint ni pruebas Android
localmente. Se espera el resultado real del nuevo CI después del push, según la
autorización actual. B09 y siguientes bloques funcionales permanecen sin iniciar.

## Resultado remoto

Pendiente de publicar y comprobar el run del commit de reparación. No se declara
éxito de B01–B06 hasta que GitHub Actions confirme sus seis resultados.
