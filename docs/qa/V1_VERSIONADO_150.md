# V1 — Versionado después de 150 compilaciones exitosas

Se conserva la serie visible anterior: el último APK previo al ancla #101 tenía versionName **1.9.101** y versionCode **10101**. La primera compilación con la política nueva tendrá como mínimo **1.9.102** y versionCode 10000 + GITHUB_RUN_NUMBER. No se reinicia el patch a cero al activar V1.

| Pipelines B01–B06 SUCCESS acumulados después del #101 | Versión en la siguiente compilación |
|---|---|
| 0 | 1.9.102 |
| 1 | 1.9.103 |
| 149 | 1.9.251 |
| 150 | **1.10.0** |
| 300 | **1.11.0** |

Se cuentan una sola vez los runs realmente terminados con SUCCESS de B01–B06. Cancelados, fallidos, skipped e in-progress no incrementan el contador. GitHub REST (actions:read) reconstruye éxitos anteriores desde #101; B01 fija versionName/versionCode y B04 verifica exactamente el mismo manifiesto y SHA. F1 protegido reutiliza el versionado del informe B06 verde del SHA aprobado.

versionCode siempre se incrementa por número de ejecución (10000 + GITHUB_RUN_NUMBER), incluso cuando la versión visible no cambia por fallos. No se hace push ni se publica un Release por subir versionName. En caso de borrar metadatos históricos de GitHub Actions, el contador deja de ser reconstructible: preservar historial o crear checkpoint auditado antes de eliminarlos. El envío a Google Play y la instalación en Android son procesos independientes protegidos por F1.

Validación requerida: tests de transición 149→150, controles de SHA y run, y ejecución completa B01–B06 del commit final.
