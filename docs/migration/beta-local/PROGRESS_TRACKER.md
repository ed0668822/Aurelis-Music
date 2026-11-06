# PROGRESS_TRACKER — beta local

Base 01c1ba511b9eb8ecb3eb40c98afb46d8d17a017a; rama existente; autorización recibida
para ejecutar plan por unidades. B01 completado; B02 implementado y comprobado localmente. Ver últimas entradas y evidencias.
Los bloques anteriores siguen en ../PROGRESS.md sin ser reiniciados o sobrescritos.

| Bloque | Dependencias | Complejidad | Estado nuevo plan |
|---|---|---|---|
| B00 | Aprobación recibida | Media | Completado y publicado |
| B01 | B00 | Alta | B01.1/.2/.3 implementados y publicados |
| B02 | B01 | Alta | B02.1/.2/.3 comprobados localmente; hardware/CI pendientes |
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


## Entrega B00 confirmada

- Commit: `b89a6b5089ac20562253697d565af934b724976e`, autor/committer ed0668822 <ed0668822@gmail.com>.
- Push confirmado desde01c1ba5; rama remota y refs/pull/1/head coinciden con SHA.
- PR: https://github.com/ed0668822/Aurelis-Music/pull/1, OPEN, título conservado
  «Aurelis Music Beta UI and components». No merge ni cambio de rama.
- GitHub Actions web muestra run119 histórico exitoso
  (https://github.com/ed0668822/Aurelis-Music/actions/runs/42101996135).
  No acredita el SHA nuevo: CI de B00/checkpoint pendiente de verificación.
  No se transfiere el verde histórico a esta entrega.
- Solo documentación, inventarios y evidencia. No modifica código/tests de app,
  Gradle/CMake, manifest, workflows, LICENSE/NOTICE ni secretos/configuración de firma.
- Nuevo checkpoint documental registra este SHA real tras publicar; el baseline
  de B00_EVIDENCE sigue siendo01c1ba5, sin cambios funcionales respecto a entrega.
- Siguiente unidad: B01.1, requiere autorización. Perfil múltiple, Dolby y demás
  cambios previstos NO se han implementado todavía.


## B01.1 — en implementación y validación

- Autorización expresa recibida; base b2bf434. Rama/PR originales.
- Perfil único; nombre/foto y ID del ámbito activo conservados. Respaldo privado
  tipado/verificado y recibo atómico antes de consolidar; sin borrar legacy.
- Modificados ProfileRepository/Storage/Avatars y SettingsScreen. Application,
  MainActivity, PlaybackViewModel y ProfilePlaybackBinding retiran únicamente
  conexiones multiusuario. No se implementan B01.2/B01.3.
- Nuevos SingleProfileMigration, su suite de pruebas y B01_1_MIGRATION. Pruebas
  de perfiles/playback/UI adaptadas a la decisión de perfil único; las pruebas
  de ámbitos de almacenamiento legacy se mantienen para recuperación.
- Revisión1 completada: respaldo/atomicidad/paths/tipos/estado y preservación.
- Revisión2 en cierre:47 casos seleccionados correctos. Suite/debug/release/lint
  corriendo; no declarar hardware ni migración en dispositivo verificados.
- Registro legal: implementación propia, sin copiar componentes nuevos de BitChord;
  avisos existentes conservados, cabecera definitiva pendiente del CSV histórico.
- Commit/PR/CI se registran tras verificaciones y publicación. Siguiente unidad
  B01.2 MediaStore/metadatos, requiere autorización.


## B01.1 — cierre local

- Estado: implementado y revisado; publicación/CI se registran a continuación.
- Compilación: BUILD SUCCESSFUL in 5m 12s. Debug/release, R8 y lint correctos.
- Suite137 debug +137 release:0 fallos/errores/omitidos;47 seleccionados previos.
- Lint advertencias debug/release: 215/214;0 errores. Firmas y ZIP válidos;9 textos legales idénticos y12 .so ARM64.
- Revisiones1/2 completadas en pasadas separadas del mismo agente. Las pruebas
  adaptadas sustituyen requisitos multiusuario retirados por invariantes de perfil
  único, preservación de datos y reproducción; no se excluyen pruebas por fallar.
- No dispositivos físicos/SAF real; no importación/exportación UI ni B01.2/3.
  B01_1_MIGRATION y B01_1_EVIDENCE documentan formato, privacidad y limitaciones.


## B01.1 — entrega publicada

- Commit: `bfbc81fdaeee5d9cb2086f27ae156ce75ca5c58b`; autor/committer ed0668822 <ed0668822@gmail.com>,
  identidad solo local. Push confirmado desdeb2bf434 a la misma rama.
- PR1 abierto: https://github.com/ed0668822/Aurelis-Music/pull/1; título intacto
  «Aurelis Music Beta UI and components». No merge ni force push.
- Actions: el listado público tras push todavía mostraba el run121 anterior
  exitoso (42123993004); no acredita el nuevo SHA. CI del código/checkpoint
  pendiente; no se traslada un verde histórico a B01.1.
- Este checkpoint documental registra el SHA real después de publicar y no
  cambia código validado. APK construido desdeb2bf434 más los cambios de esta
  entrega; B01_1_EVIDENCE registra ambas referencias para no confundir metadatos.
- Estado: B01.1 completado localmente y publicado. B01.2 MediaStore/metadatos y
  B01.3 carpetas siguen sin implementar y requieren autorización.


## B01.2 — completado localmente

Autorizado junto con B01.3/B02. Base0de7f2a; MediaStore/metadatos implementados,
144 tests debug correctos y U completo (BUILD SUCCESSFUL in 1m 50s); lint0 errores.
Dos revisiones separadas del mismo agente; ver B01_2_METADATA/EVIDENCE.
Release/dispositivo pendientes del cierre de alcance. Siguiente B01.3 autorizado.


## B01.2 y B01.3 — cierre de B01

- B01.2 código: `de8c1afe5614814593865d268e4e72ffaadaf6ef` (de8c1af),144 casos debug correctos.
- B01.3:154 casos debug correctos; assemble/lint debug correctos.
- Dos revisiones en pasadas separadas del mismo agente. Alcance indexado por
  MediaStore explícito; SAF real/SD físico pendientes. No escáner ficticio.
- B01 completo localmente; B02.1/.2/.3 autorizados por «continúa hasta B02».
- Release completo se comprobará al cerrar B02; no iniciar B03.


## B02.1 — completado localmente

156 tests debug correctos, assemble/lint correctos. Fuente del sistema y Vazirmatn
con selector real, persistencia y OFL/NOTICE/bundle. Dos revisiones separadas del
mismo agente; fuente y texto íntegros con SHA de origen. ADB sin dispositivos.
B01.3 publicado en4c8d529; B02.2/.3 autorizados, B03 no iniciado.


## B02.2 — completado localmente

158 casos debug, assemble/lint correctos (U:1m55s), cuatro programas AGSL exactos
compilados en runtime nativo V, sin transformar shaders ni cambiar dependencias.
Fuente de página conectada, superficies compartidas, preferencias y fallback
por capacidad; guards de Canvas software. Dos revisiones del mismo agente en
pasadas separadas; evidencia/limitaciones en B02_2_RENDERING/EVIDENCE.
Hardware y paridad visual final pendientes; B02.3 autorizado, B03 no iniciado.


## B02.3 y cierre acumulado hasta B02

- Fuente/interruptor y componentes/gestos conectados; no mocks de favoritos,
  playlists ni cola. Diferencias de HeldItem/ancla/retorno documentadas en B02_3.
-14 casos seleccionados correctos; R:164 debug y164 release,0 fallos/errores/omitidos.
- Debug/release/R8/lint correctos; dos revisiones del mismo agente en pasadas
  separadas. Firmas/ZIP válidos,10 textos legales idénticos y fuente íntegra.
- B02_3_EVIDENCE registra APK/hashes/resultados y base+archivos probados. No hay
  dispositivo: paridad final/GPU/SAF real pendientes. ONNX4KB/modelos/licencias
  B00 siguen abiertos para B09/B13; no se modifica el motor nativo.
- B03 no iniciado; requiere autorización. Entrega misma rama/PR1, sin renombrar,
  merge ni force push; SHA de código en checkpoint de entrega. Actions pendiente
  del SHA publicado, no confundir con ejecuciones anteriores.
