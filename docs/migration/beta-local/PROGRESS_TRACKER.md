# PROGRESS_TRACKER — beta local

Base 01c1ba511b9eb8ecb3eb40c98afb46d8d17a017a; rama existente; autorización recibida
para ejecutar plan por unidades. B01 completado; B02 implementado y comprobado localmente. Ver últimas entradas y evidencias.
Los bloques anteriores siguen en ../PROGRESS.md sin ser reiniciados o sobrescritos.

| Bloque | Dependencias | Complejidad | Estado nuevo plan |
|---|---|---|---|
| B00 | Aprobación recibida | Media | Completado y publicado |
| B01 | B00 | Alta | B01.1/.2/.3 implementados y publicados |
| B02 | B01 | Alta | B02.1/.2/.3 comprobados localmente; hardware/CI pendientes |
| B03 | B02 | Media-alta | B03.1/.2/.3 implementados y verificados localmente; GPU/CI entrega pendientes |
| B04 | B03 | Alta | Pendiente de autorización/previas |
| B05 | B04 | Alta | Pendiente de autorización/previas |
| B06 | B05 | Muy alta | Autorizado; B06.1 implementado pendiente CI/Android; .2/.3/.4 pendientes |
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


## Checkpoint de entrega hasta B02

Código final: `ccb75126b0b0361212e9c7369265e0f803812ee6`.
Todos los SHA reales, revisión/compilación, PR y límites en B02_CLOSEOUT.md.
Entrega conjunta de commits B02 a la misma rama/PR; CI del SHA de entrega pendiente
de consulta. B01.2/.3 ya publicados; no repetir esas tareas. B03 sin autorización.


## Complemento QA00/QA01 — regresión B02

El usuario reportó cierre al abrir en Redmi Note14Pro5G. La prueba nueva reproduce
Size is unspecified en onAttach/apply/lens; se corrigió el cálculo antes de medida
sin eliminar Liquid Glass.171 pruebas por variante y builds/lint correctos.
Ver [QA](../../qa/PROGRESS.md) y QA01_EVIDENCE/RED/GREEN para evidencia y límites.
Arranque físico pendiente: no trasladar el éxito automatizado al Redmi. Plan y
orden B00–B13 intactos; B03 no iniciado. QA02 necesita autorización independiente.


## B03 — reanudación del principal

Última instrucción «Mejor continúa con el plan ... pero si prueba todo lo que
puedas» se aplica al siguiente B03, sin iniciar QA02 ni B04. QA00/01 conserva
pruebas/CI de b8b8910 correctas, Android real pendiente.
B03.1: host local inicializado y píldora real conectados;14 casos dirigidos
correctos, dos revisiones del mismo agente. Ver B03_1_NAVIGATION. R al cierre.


B03.1 código: e79f32a. B03.2: secciones reales y reproducción por sección,
HeroShelf conectado, recientes en páginas de4 y playlists con IDs disponibles.
17 casos dirigidos correctos; dos revisiones separadas del mismo agente. Ver
B03_2_HOME; release/APK/lint y suite completa en R al cierre. B03.3 autorizado.


## Cierre local B03

B03.1 e79f32a; B03.2 6b4bded; B03.3 recuperación/capturas verificadas con R.
BUILD SUCCESSFUL in 5m 6s;179 pruebas debug +179 release,0 fallos/errores/omitidos.
Lint:224/223 advertencias,0 errores. Ambos APK firmados/ZIP correctos,
10 textos legales íntegros y12 .so ARM64. Dos revisiones del mismo agente en
pasadas separadas. B03_1/2/3 y B03_EVIDENCE registran archivos/resultados/límites.

Capturas Robolectric en screenshots/b03; no Android real ni paridad GPU medida.
Fuente local, estados, colas y navegación conectados, sin contenido de ejemplo
en producción. Plan maestro/bloques, QA01, identidad, ABI y workflow intactos.
Explore queda expresamente para B04, no implementado aquí. No iniciar B04 ni QA02.


Entrega B03: código final `2e91a09f8c8c4ab621a54ffe4992dd3280ba5eaa`, commits por unidad y resultados
en B03_CLOSEOUT.md/B03_EVIDENCE.json. Misma rama/PR1 sin renombrar ni merge.
CI del SHA publicado pendiente de consulta; no reutilizar QA127. B04 no iniciado,
requiere autorización. QA02 permanece sin iniciar; Android real pendiente.


## B04 — entrega incremental; autorización B04–B05

B04.1: Explore/géneros reales y Sin clasificar conectados, estados de carga/error/
permisos/vacío; B04.2: búsqueda local de cinco tipos e historial persistente;
B04.3: directorios/restauración/lista-cuadrícula y selección múltiple de colecciones.
Cambios, archivos, referencias y límites en B04_EXPLORE_SEARCH_LIBRARY.md.
Dos revisiones estáticas del mismo agente. Seis pruebas nuevas y cinco rutas de
restauración adicionales preparadas: ejecución PENDIENTE en GitHub, no aprobadas.
No compilación local por instrucción del usuario; no Android real/Logcat.
Sin cambios de Liquid Glass/QA01, firmas, ABI, Gradle, licencias ni plan principal.

Estado: B04 implementado, validación en GitHub y paridad visual en dispositivo
pendientes. Search incluye filtros explícitos por tipo. No declarar B04 terminado
solo por publicar.
B05 autorizado por «Continúa con B04 - B05», pendiente de la puerta de CI y cierre
de B04. Mantener indicación del usuario: publicar, avisar y esperar resultado de
GitHub antes de continuar; no pedir de nuevo autorización B05. Misma rama/PR1.


## B05 — continuación autorizada tras entrega B04

«Autorización concedida» permite iniciar esta entrega, sin dar por aprobado CI de
B04. B05.1: calidad tipada real de archivo/pista, MiniPlayer cápsula/etiqueta;
B05.2: entrada/cierre/gestos con cancelación segura, panel/dock/morph pendientes;
B05.3: layout horizontal existente conectado a controles compartidos y fondo que
respeta efectos reducidos. Rotación real y fidelidad visual pendientes.
Ver B05_PLAYER.md/.json para archivos, revisiones, referencias, pruebas y límites.
Seis pruebas nuevas preparadas, NO EJECUTADAS. Compilación/lint exclusivamente en
GitHub, pendientes. Sin Android físico/sonido/Logcat verificados. Dos revisiones
estáticas del mismo agente. Firma/ABI/servicio/QA01/GLASS01/plan preservados.
Misma rama/PR1; publicar/avisar y esperar resultado del usuario. No B06 ni QA02.
No declarar B05 cerrado: siguiente trabajo será completar B05.2 y validaciones.


## B05.2 — panel progresivo / dock, continuación autorizada

Base a5e06bc. PlayerSheetMotion existente conectado al host activo, sin segundo
motor; arrastre/cancelación/velocidad, dock medido del mismo track y carátula
superpuesta durante movimiento portrait. Fondo y MiniPlayer conservados debajo
con semántica aislada. Sin medidas/animación reducida/landscape: slide normal.
Detalles, archivos, revisiones y límites en B05_2_PANEL_DOCK.md/.json.
Siete pruebas nuevas y regresión de swipe actualizada: NO EJECUTADAS aquí.
Dos revisiones estáticas separadas del mismo agente; API inspeccionada con javap.
Compilación/lint/pruebas solo en GitHub, pendientes de resultado del usuario.
No Android físico, capturas GPU, Logcat ni sonido verificados. Servicio, calidad,
B04, QA01, GLASS01, firmas/ABI/workflow/licencias íntegras y plan principal intactos.
No cierre total B05 ni B06/QA02: paridad visual y validación siguen pendientes.

Entrega de código B05.2: `ed6d44b82e055eeb5ca224bba21e0a766bee93bd`. PR1 existente, misma rama; checkpoint
documental posterior conserva idéntico árbol de aplicación. CI y Android real
pendientes; no se consulta Actions automáticamente ni se inicia B06/QA02.


## Corrección CI02 — puerta antes de B06

Run135 de0ec482b falló por acceso implícito a maxWidth dentro de Column
(NowPlayingScreen.kt:384). Se captura la medida en BoxWithConstraints, sin cambiar
el panel. La revisión estática anterior no equivale a compilación real: validación
nueva pendiente de GitHub. Ver ../../qa/CI02_BUILD_FAILURE.md/.json y CI_WORKFLOW.md.
Workflow/reportes mejorados con compile previo, entradas de tareas y estados por
archivo, sin falsos éxitos ni checks desactivados.21 pruebas de utilidades correctas;
no Gradle/Android local. No avanzar B06/QA02 hasta resolver CI. Publicar/avisar y
esperar resultado del usuario; mantener B05 y paridad visual pendientes de validar.


## CI03 — corrección de compilación del archivo de pruebas

Run42232423184: código principal y lint pasaron, falló compilar pruebas por
R.string en PlayerIntegrationTest. Gate/APK bloqueados correctamente. Import
explícito del R de Aurelis, sin tocar pruebas/producción/workflow/plan/licencias.
Ver ../../qa/CI03_TEST_RESOURCES.md/.json. Dos revisiones estáticas; compilación y
pruebas nuevas pendientes de GitHub, no ejecutadas aquí. No B06/QA02. Esperar
resultado del usuario antes de continuar; B05 no se declara verificado en Android.


## CI04 — registro recibido, fallo de aserción localizado

Run137 ejecutó200 pruebas por variante y falló la misma prueba de búsqueda en
línea75. Se restringe assertDoesNotExist a resultados en lugar de toda la pantalla,
y se verifica que la canción existe con Todo, desaparece con Álbumes y continúa
en MiniPlayer/estado. Colección e historial siguen comprobados. No se cambia la
lógica de búsqueda o reproducción: solo etiqueta de test y matcher reforzado.
JUnit se publica como anotación además del artefacto.23 pruebas Python correctas;
prueba UI y nueva compilación pendientes de GitHub, no ejecutadas localmente.
Ver ../../qa/CI04_INVESTIGATION.md/.json. Doble revisión estática, sin cierre Android
ni avance B06/QA02. Publicar y esperar resultado comunicado por el usuario.


## CI05 — aislamiento de PlaybackIntegrationTest

Reintento autorizado de registros: API Forbidden, páginas públicas/anotaciones
JUnit disponibles. Run138: carrera entre uso de Main y su sustitución en pruebas.
Se registra/espera el Job de cada ViewModel creado y reemplazado antes de resetMain.
Los diez casos existentes y sus asserts permanecen; regresión IO/finalizador Main
preparada. Detalles en ../../qa/CI05_MAIN_DISPATCHER.md/.json. Revisión doble estática,
sin ejecución Android local ni cambio de app/workflow/plan. Nuevo CI pendiente.
B06/QA02 detenidos; publicar/avisar y esperar resultado que comunique el usuario.


## B06.1 — coordinador y cola, autorizado

Base cefe533 (SHA completo en B06_1_QUEUE.json). CI05 anterior: run139 SUCCESS
del HEAD cefe533dd1e256c67a720a97a241e95789c1fa9d, confirmado mediante páginas
públicas de GitHub; no acredita Android real. QueueCoordinator existente deja
de ser stub; menús → Play Next/adición, QueueScreen → InlineQueue/arrastre/acciones
accesibles. UUID por ocurrencia y edición Media3 en sitio; misma sesión/motor.
Adiciones FIFO separadas del último playSongs. Detalles/archivos/21 tests nuevos
en B06_1_QUEUE.md/.json; preparados, NO EJECUTADOS localmente. Dos revisiones
estáticas del mismo agente; compilación/lint/tests exclusivamente en GitHub.
Play Next con shuffle deshabilitado y explicado, política de sucesor pendiente.
B06.2/.3/.4 y validación real/paridad pendientes. Plan/QA01/Glass/ABI/servicio
preservados. Publicar en mismo PR1, avisar y esperar resultado del usuario.


Entrega B06.1 — commit de código `4fbb660ec7f0441fe99aa21d8cd517eb86612ba5`, identidad local
ed0668822 <ed0668822@gmail.com>. Destino: rama existente y PR1
https://github.com/ed0668822/Aurelis-Music/pull/1, título conservado.
Checkpoint documental posterior sin cambiar el árbol de aplicación. CI/21 tests
y Android real pendientes; siguiente B06.2 espera resultado del usuario.
