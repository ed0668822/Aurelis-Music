# CI14 F3 — Handoff seguro de compilación (contrato)

Estado: implementación F3 en la misma rama, únicamente ámbito CI.

El pipeline crea una sola vez el APK Debug y el APK de instrumentación en B01. B04 descarga y valida el Debug; B05 descarga ambos, comprueba SHA-256, `GITHUB_SHA`, `GITHUB_RUN_ID`, `GITHUB_RUN_ATTEMPT`, `GITHUB_RUN_NUMBER` y ABI antes de ejecutar Android 15. Nunca usa un APK de un SHA distinto. El artefacto temporal `b01-shared-apks-<run_number>` se elimina **solo tras PASS de B06**, igual que los otros temporales; si el CI falla se conserva como evidencia.

**Límite de seguridad:** un Release firmado con certificado real NO se exporta como artefacto temporal a jobs sin environment protegido. B04 lo ensambla una vez en su propio job y realiza la auditoría completa junto al Debug importado. Tampoco publica el Release oficial mientras F1 carezca de un environment `release` con revisores obligatorios. B04 es la única fase que entrega el Debug verificado.

**Límite técnico:** lint y pruebas JVM siguen compilando las tareas auxiliares que exigen Gradle y Robolectric. Ningún APK compilado sustituye las clases o recursos que esas tareas necesitan. La promesa de «compilar una vez» se limita a cada APK de producción/instrumentación; no se falsea el número de ejecuciones de compiladores de Kotlin/Java.

La procedencia y los hashes se comprueban también en tests sin red. No se modifica `app/`, formatos nativos, arquitectura, permisos ni lógica de audio.
