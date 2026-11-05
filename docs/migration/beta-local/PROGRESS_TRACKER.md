# PROGRESS_TRACKER — beta local

Base 01c1ba511b9eb8ecb3eb40c98afb46d8d17a017a; rama existente; autorización recibida
para ejecutar plan por unidades. Esta entrega comprende B00. B01 aún no iniciado.
Los bloques anteriores siguen en ../PROGRESS.md sin ser reiniciados o sobrescritos.

| Bloque | Dependencias | Complejidad | Estado nuevo plan |
|---|---|---|---|
| B00 | Aprobación recibida | Media | Completado localmente; publicación pendiente |
| B01 | B00 | Alta | Pendiente de autorización/previas |
| B02 | B01 | Alta | Pendiente de autorización/previas |
| B03 | B02 | Media-alta | Pendiente de autorización/previas |
| B04 | B03 | Alta | Pendiente de autorización/previas |
| B05 | B04 | Alta | Pendiente de autorización/previas |
| B06 | B05 | Muy alta | Pendiente de autorización/previas |
| B07 | B06 | Alta | Pendiente de autorización/previas |
| B08 | B07 | Media-alta | Pendiente de autorización/previas |
| B09 | B06 y B08 | Muy alta | Pendiente de autorización/previas |
| B10 | B09 | Muy alta | Pendiente de autorización/previas |
| B11 | B10 | Alta | Pendiente de autorización/previas |
| B12 | B11 | Alta | Pendiente de autorización/previas |
| B13 | B12 y todos los componentes incluidos resueltos | Alta | Pendiente de autorización/previas |

## B00 — auditoría y dependencias

- B00.1: rutas/propietarios/stubs comprobados; fuente fija e inventario completo.
- B00.2: comparación de175 módulos resueltos con inventario previo, sin cambios de
  versión; licencias reutilizadas como metadatos, no certificación de binarios.
- B00.3: ocho documentos guardados, validación real debug/release y suites con
  --rerun; evidencia y entrega se registran al finalizar las dos revisiones.
- Revisión1: arquitectura/correspondencias. Se corrigieron rutas del plan: LocalAutoMix
  está en playback; PlayerLyrics es nuevo en destino; LrcRed ya existe. No cambios
  a código de app ni archivos de firma/identidad/licencia/workflow.
- Revisión2: comprobaciones funcionales de base, inventarios/documentos/ZIP/ELF y
  workflow. Resultado/commit/PR al cierre, sin declarar fidelidad o hardware probados.
- Bloqueo descubierto: ORT1.19.2 aporta libonnxruntime.so y libonnxruntime4j_jni.so
  con LOAD4KB. ZIP16KB correcto no resuelve esta incompatibilidad ELF. Tratar en
  B09/B10 antes de cerrar B13; no cambiar binarios en B00.
- Pendientes: modelos/fuentes de binarios, condiciones de APIs, CSV, referencia
  visual y dispositivos. Ver LICENSE_AUDIT/TEST_STRATEGY/B00_AUDIT.

## Registro por unidad futura

Guardar base, archivos creados/modificados, pruebas realmente ejecutadas/cacheadas,
revisión1/revisión2, build debug/release, CI por SHA, paridad/limitaciones, commit/PR
reales y próximo subbloque autorizable. No repetir árbol completo ni auditorías
ya acreditadas; leer solo filas/archivos de la unidad al reanudar.


- Cierre local: revisión1 y2 completadas en pasadas separadas del mismo agente.
  180 correspondencias verificadas;14 fichas x16campos y48 subunidades.
  Inventario1396 y dependencias175; ambos builds y252 tests ejecutados correctos.
  Firmas/ZIP/actionlint correctos; hallazgo ELF16KB registrado sin ocultarlo.
- B00_AUDIT y B00_EVIDENCE contienen evidencia/limitaciones. La entrega no afirma
  B01, funciones futuras, paridad ni hardware aprobados. SHA/PR al publicar.
