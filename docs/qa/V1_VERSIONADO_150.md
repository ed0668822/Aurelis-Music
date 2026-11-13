# V1 — Auto-versionado tras 150 pipelines exitosos

El ancla es el run #101. Desde el siguiente build se cuenta cada B01–B06 completo SUCCESS exactamente una vez. Fallos, cancelaciones, skips y ejecuciones incompletas no cuentan.

| Éxitos anteriores | Versión siguiente |
|---|---|
| 0 | 1.9.0 |
| 149 | 1.9.149 |
| 150 | 1.10.0 |
| 300 | 1.11.0 |

B01 obtiene el conteo mediante API de GitHub Actions y genera ci-b01/version-metadata.json; B04 adopta la misma versión y code del mismo run; F1 protegido fija la versión del informe de la ejecución verde del SHA, nunca del número de su workflow manual.

versionCode = 10000 + GITHUB_RUN_NUMBER conserva la monotonía para instalaciones. Cambiar versionName NO publica en Google Play, no crea un GitHub Release y no instala el APK automáticamente en teléfonos.

Si la API no devuelve historial confiable, falta permiso o el SHA/versionName/code no coincide, el gate falla sin inventar resultados. No borrar el historial de runs sin un checkpoint auditado porque el contador reconstruido perdería precisión.

La publicación F1 exige environment release configurado y aprobado. La validación Android 16 física sigue siendo necesaria tras cambiar targetSdk a 36.
