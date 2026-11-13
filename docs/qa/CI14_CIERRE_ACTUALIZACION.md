# Aurelis Music — Cierre técnico CI14, versionado V1 y revisión F7

**Repositorio:** `ed0668822/Aurelis-Music` · **Rama:** `feature/bitchord-local-migration` · **Fecha:** 2026-11-12.

**Alcance de entrega:** código y documentos enviados a GitHub únicamente. Sin merge con `main`, sin GitHub Release ni APK público, sin publicación en Google Play y sin subida pública del APK a VirusTotal.

## Resultado de referencia: Run #108, SHA `94c7218d2eed5af35c481f33747a2aee957f970c`

B01, B02, B03, B04, B05 y B06: **SUCCESS**. Publicados solamente dos artefactos de GitHub Actions: `Aurelis-Informes-B01-B06-108` y `Aurelis-Pruebas-ARM64-108`.

| Componente | Evidencia comprobada | Limitación |
|---|---|---|
| B02 | 228 advertencias Debug y 228 Release, cero errores | Advertencias no resueltas |
| B03 | 280/280 JVM Debug y 280/280 JVM Release | No prueba audio físico |
| Cobertura F5 | 3527 de 3969 líneas ejecutables modificadas; 88,86 % | Supera mínimo 80 % |
| B05 Media3 | 7/7 pruebas independientes | Emulador Android 15; no Android 16 físico |
| F4 matriz | 663 fuentes versionadas, 1326 filas, 338 clases mapeadas por variante, 11 objetos nativos con SHA-256 | No demuestra vínculo individual de cada `.o` con el APK |
| V1 | `versionName 1.9.102`, `versionCode 10108` | Primer ciclo: 150 éxitos posteriores al run #101 para saltar a `1.10.0` |
| VirusTotal | HASH_LOOKUP_ONLY | No significa análisis limpio |

## Correcciones finales incluidas

1. Quitar referencia al step `release_apk` eliminado del flujo B04.
2. Hacer `actionlint` un control obligatorio y conservar el análisis independiente de `zizmor`, sin atribuirle éxito falso.
3. Evitar interpolar directamente los IDs del run aprobado en el shell del workflow de Release protegido; transferirlos por `env:`.
4. Actualizar la documentación CI14 y añadir pruebas de regresión para los dos problemas de workflows.

## Estado final del trabajo y límites de publicación

- **F0–F3:** completados en CI. F3 redujo tiempo de pared de 29,25 min a 15,65 min (46,5 %) entre runs verdes #41 y #88.
- **F4:** inventario y evidencia por archivo con advertencias explícitas sobre el enlace nativo.
- **F5:** pruebas, cobertura y actionlint; Detekt/ktlint y análisis longitudinal de flakiness no integrados, sin fingir que lo están.
- **F6:** documentos consolidados en este índice, manteniendo el registro histórico.
- **F7:** revisión aplicada: `targetSdk 36`, actualizador GitHub corregido. FFmpeg y `org.json` se conservan por compatibilidad; requieren validación en Android 16 físico.
- **V1:** contador de 150 ejecuciones B01–B06 realmente exitosas; no publica ni instala ninguna versión automáticamente.
- **F1:** Release preparado en el código, **no activado administrativamente**: requiere revisores reales del environment `release`, control de secretos y aprobación antes de distribución.

**Cierre de desarrollo y envío a GitHub**, no certificación integral del producto ni autorización para distribuir APK. El commit de este cierre requiere su propio B01–B06 verde; un run de un SHA anterior no lo sustituye.

## Material de referencia

- [Ejecución #108 aprobada](https://github.com/ed0668822/Aurelis-Music/actions/runs/43335494800)
- [Plan e historial CI14](CI14_PLAN_MEJORA_CI.md)
- [Versiones V1](V1_VERSIONADO_150.md)
- [Rendimiento F3](CI14_F3_RESULTADO.md)
- [Seguridad F1](CI14_F1_RELEASE_PROTEGIDO.md)
- [Política de pruebas F5](CI14_F5_TEST_POLICY.md)
- [Revisión F7](CI14_F7_REVISION.md)
