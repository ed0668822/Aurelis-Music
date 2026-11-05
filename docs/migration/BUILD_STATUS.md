# Estado de compilación e integración

## Línea base verificable

Commit destino validado en onboarding: `9bf3fd3db8f5870f942f5167dca5bc77e3c48edd`.
Los resultados siguientes corresponden a esa línea base, no a un nuevo build del
commit documental B01 ni a GitHub Actions.

| Comprobación | Resultado | Evidencia / límite |
|---|---|---|
| Debug | Correcto | `assembleDebug`; incluye compilación Kotlin/Java y C++ ARM64 |
| Lint | Correcto con 209 advertencias, sin errores | XML de lint del onboarding |
| Unitarias | `NO-SOURCE` | No se ejecutaron casos; no equivale a pruebas aprobadas |
| APK | Firma v2, integridad ZIP y tres bibliotecas Aurelis ARM64 verificadas | No demuestra reproducción funcional |
| Reinstalación | Script reutilizable terminó correctamente | Dependencias existentes reconocidas |
| Dispositivo | No ejecutado | Pendientes reproducción, permisos, interfaz, notificación y bloqueo |
| Release | No ejecutado en onboarding | No afirmar publicación/firma release verificadas |
| BitChord | No compilado ni ejecutado | Solo inspección de referencia en lectura |
| Actions / permisos API | Bloqueado: `Forbidden` | Lectura Git funciona; acceso API no confirmado |
| Entrega Git B01 | Rama publicada | Push confirmado; consultar PROGRESS para commit |
| Creación PR B01 | Bloqueada | `gh pr create` recibió Forbidden de api.github.com/graphql; no hay PR confirmado |

Comando final del onboarding (exit 0, `BUILD SUCCESSFUL in 13s`, 52 tareas):

```bash
cd /workspace/Aurelis-Music
source /workspace/toolchains/activate.sh
bash gradlew assembleDebug lintDebug testDebugUnitTest --no-daemon --max-workers=4
```

Evidencia local retenida, no requisito para clonar el repositorio:
`/workspace/toolchains/validation-final.log`, `/workspace/toolchains/reinstall.log`,
`app/build/reports/lint-results-debug.xml` y
`app/build/outputs/apk/debug/app-debug.apk` (117781656 bytes al verificarlo).
La primera validación se solapó con otro build y falló al empaquetar por un
intermedio DEX ausente. Ambas ejecuciones terminaron; la repetición secuencial fue
correcta. Ejecutar Gradle secuencialmente en este checkout.

## Entorno reutilizable

- JDK Temurin 17.0.17; la imagen original solo aportaba runtime Java 21, sin javac.
- Gradle wrapper 8.11.1; invocar con `bash gradlew`, sin cambiar el modo del archivo.
- SDK 36, build-tools 35.0.0, NDK 26.1.10909125 y CMake 3.22.1.
- Activación local: `/workspace/toolchains/activate.sh`. Conserva proxy y truststore
  del sistema con verificación TLS; no contiene credenciales.
- `install_script` y `start_skill` se guardaron en el borrador durante onboarding.
  Eso no demuestra publicación ni restauración en una nueva tarea.
- No hay servicio de desarrollo que deba permanecer vivo para compilar.
- La ejecución interactiva requiere Android API 33+ y ARM64 compatible.

## CI: riesgos observados, sin cambios B01

`.github/workflows/android.yml` instala explícitamente plataforma 35 mientras el
módulo compila con 36. Podría autoinstalarse la requerida; no se observó una ejecución
remota que lo confirme. Lint y unitarias usan `continue-on-error`; no inferir que un
workflow verde demuestra que ambas pasaron. Release tiene un paso final que marca
fallo si falló la compilación. No modificar el workflow para ocultar resultados.

## Validación B01

Requerida: rutas y enlaces de los documentos, consistencia de SHAs y estados,
`git diff --check` y diff limitado a los cinco Markdown. No cambia Gradle, recursos,
código ni dependencias: no se repite un build completo sin justificación.
Resultados y comprobaciones de entrega se registran en PROGRESS.

## B02 — Validación de arquitectura base

Cambios respecto a `e6f58f937a1f84166236705c7f8b9a3a2947f571`;
consultar PROGRESS para commits de implementación y entrega. La base histórica
NO-SOURCE anterior permanece documentada; B02 sí introduce y ejecuta pruebas.

```bash
cd /workspace/Aurelis-Music
source /workspace/toolchains/activate.sh
bash gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4
# Tras añadir la prueba de una canción local (solo cambió código de test):
bash gradlew testDebugUnitTest lintDebug --no-daemon --max-workers=4
```

- Primera ejecución: exit 0, BUILD SUCCESSFUL en 2m 17s, debug generado y 10 pruebas correctas.
- Ejecución final: exit 0, BUILD SUCCESSFUL en 27s; 11 pruebas, 0 fallos,
  0 errores y 0 omitidas. ProfileStorageTest: 5; LibraryViewModelTest: 6.
- Lint: sin errores, 209 advertencias (misma cantidad que la línea base).
- Pruebas Robolectric/API 33: conservación de preferencias y claves desconocidas,
  inicialización repetida, aislamiento entre cinco áreas y tres perfiles, ID estable,
  perfil no heredado, avatar y arranque de Application; permiso denegado/reintento,
  carga y reescaneo, canción local/grupos/búsqueda, cancelación/error y constructor Android.
- Informes: `app/build/test-results/testDebugUnitTest/TEST-*.xml`,
  `app/build/reports/tests/testDebugUnitTest/index.html` y XML de lint.
- Registros de esta instancia: `/workspace/toolchains/b02-validation.log` y
  `/workspace/toolchains/b02-validation-final.log`.
- Se probaron SharedPreferences Android bajo Robolectric y reapertura de ámbitos;
  no se simula un reinicio real de proceso/dispositivo ni ejecución física de MediaStore.
  Perfil activo múltiple, listas/historial funcionales, reproducción en dispositivo,
  release y CI remoto permanecen fuera de estos resultados.
- Entrega: commit de implementación `2a10e63fd89db9993ebfd1ef21920cc761aec886`
  publicado; firma APK verificada de nuevo tras la validación. PR y consulta de
  Actions siguen bloqueados por Forbidden en GraphQL/REST de GitHub. No se modifica
  el workflow para eludir errores. Consultar PROGRESS para enlace de creación manual.

## B03 — Sistema visual

Base `d1006710aa1b300a3e6aa862a0bdca26a13884a8`; commits finales en PROGRESS.
Verificación final:

```bash
cd /workspace/Aurelis-Music
source /workspace/toolchains/activate.sh
bash gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4
```

- Exit 0, BUILD SUCCESSFUL en 36s; 57 tareas (8 ejecutadas, 49 up-to-date).
- 16 pruebas, 0 fallos, 0 errores y 0 omitidas. Se mantienen las 11 de B02;
  ThemeContrastTest añade 2 y VisualSystemTest añade 3.
- Lint sin errores y 208 advertencias; no se deshabilitan comprobaciones.
- Debug generado para los cambios de producción; firma comprobada con apksigner.
- Contraste >=4.5:1 para los pares semánticos de texto probados; opacidad correcta.
  No equivale a una auditoría de todas las pantallas o de cada color dinámico de artwork.
- Compose/Robolectric API 33 comprueba cambios persistidos de tema/movimiento,
  selección, switch único, controles deshabilitados, buscar/borrar con áreas >=48 dp
  y texto de switch completo al escalar. No usa un motor de UI paralelo ni mocks
  de los componentes visuales.
- Capturas reales de una galería de componentes de prueba (360×800, escala 1/1.5),
  generadas por dibujo de la vista Android con gráficos nativos de Robolectric:
  [claro](screenshots/light.png), [oscuro](screenshots/dark.png),
  [claro ampliado](screenshots/light-large.png), [oscuro ampliado](screenshots/dark-large.png).
  Se revisaron visualmente y contienen contenido renderizado, no superficies vacías.
  Son evidencia de componentes, no capturas de toda la aplicación ni de un dispositivo físico.
- Al inicio, PixelCopy no recibía el callback de forceRedraw bajo Robolectric;
  se diagnosticó y se cambió a dibujo nativo de la vista. La revisión visual encontró
  truncación en texto ampliado; se corrigió y se repitieron las comprobaciones.
- Log final local: `/workspace/toolchains/b03-validation-final.log`.
  XML: `app/build/test-results/testDebugUnitTest/TEST-*.xml`.
  Los PNG también se regeneran en `app/build/reports/b03-visual/`.
- Entrega: implementación `7f747a32e920940b25e7a3ea3553ccf9949021dd` publicada.
  PR #1 identificado por referencia Git y confirmado OPEN por HTML de GitHub,
  con el título «Aurelis Music Beta UI and components». La rama remota confirmó
  el SHA nuevo; la referencia de lectura del PR aún mostraba el anterior al consultar.
  La consulta API sigue denegada; estado de Actions pendiente de comprobación remota.
- Pendientes: revisión en dispositivo (incluidos efectos GPU/barras de sistema y
  reproducción), comparación visual completa y cobertura de animaciones de B13.

## B04 — Navegación

Base `309c732e5051f80f198e533ac6752c3902678f64`; commits de entrega en PROGRESS.

```bash
cd /workspace/Aurelis-Music
source /workspace/toolchains/activate.sh
bash gradlew compileDebugKotlin --no-daemon --max-workers=4
bash gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4
# Tras ampliar solo el caso de prueba de restauración de búsqueda local:
bash gradlew testDebugUnitTest lintDebug --no-daemon --max-workers=4
# Tras corregir la entrada a Canciones sin permiso y ampliar el test del grafo:
bash gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4
```

- Compilación incremental inicial: exit 0 en 26s.
- Primera suite/debug/lint: exit 0 en 1m 42s; APK generado.
- Ejecución intermedia de tests/lint: exit 0 en 32s; 38 tareas (5 ejecutadas, 33 up-to-date).
- Ejecución final tras corrección de permisos: exit 0 en 1m 38s;
  57 tareas (18 ejecutadas, 39 up-to-date). Debug y firma verificados de nuevo.
- 28 pruebas, 0 fallos, 0 errores, 0 omitidas. Se mantienen las 16 previas;
  AppNavigatorTest añade 8 y NavigationIntegrationTest añade 4.
- Lint sin errores, 205 advertencias; firma APK comprobada con apksigner.
- Pruebas de pila: rutas consecutivas sin duplicación, pestaña de origen, retorno a
  Inicio raíz, cola→reproductor, claves/versiones de estado y orden de detalles previo.
- Integración Robolectric/API 33: barra real con selección, Biblioteca→Ajustes→Equalizer
  y vuelta mediante dispatcher Android; Settings conserva scroll al restaurar; Replay
  vuelve a Biblioteca; consulta de búsqueda sobrevive a pestañas/rutas/restauración;
  búsqueda local restaurada consume Atrás antes que su padre; cola restaurada vuelve
  al estado de reproductor sin canción y permite regresar a la pestaña original.
  La entrada a música local con permiso denegado muestra el Error del ViewModel
  existente, en lugar de permanecer cargando. No se añade un sistema de permisos paralelo.
- Se ejecuta AurelisNavigationContent, la misma composición usada por MainActivity,
  con sus pantallas y ViewModels reales; no se inician actualizador, conexiones Media3
  ni solicitudes de permisos. No se prueban acciones de audio o estadísticas mediante
  una implementación simulada que sustituya servicios.
- StateRestorationTester emula guardado/restauración de estado Compose; no se afirma
  reinicio real de proceso/dispositivo ni restauración de la sesión multimedia.
- El modal de actualización y los diálogos nativos se revisaron en código; su
  interacción de prioridad/ventanas con Atrás en dispositivo sigue pendiente.
- Logs locales: `/workspace/toolchains/b04-compile.log`,
  `/workspace/toolchains/b04-validation.log`, `/workspace/toolchains/b04-validation-final.log`.
  Informes XML actuales en `app/build/test-results/testDebugUnitTest/`.
- Release, dispositivo físico y Actions remoto quedan fuera de estos resultados;
  no se deshabilitan comprobaciones ni se alteran workflows.
- Entrega: implementación inicial `7e477befcd547966f06ba1bc0c777a1f290586b5` y
  corrección `462ecb25294694ff685aa31f2b0fa05ab550482a` publicadas en la rama
  del PR #1. La consulta REST de Actions volvió a responder Forbidden; no se conoce
  su resultado remoto. El registro de entrega se conserva en otro commit documental.

## B05 — Home local

Base `2e82e1a2d87fc8f11452da2e5f0b0a31f597da09`; misma rama y PR #1.

```bash
cd /workspace/Aurelis-Music
source /workspace/toolchains/activate.sh
bash gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4
```

- Ejecución final: exit 0, BUILD SUCCESSFUL en 1m 34s, 57 tareas (17 ejecutadas).
- 38 pruebas: 0 fallos, 0 errores, 0 omitidas. Se mantienen las anteriores y se añaden
  casos de datos privados, Home, rutas de colecciones y flujo real favoritos/playlists.
- Debug compilado, firma válida; lint sin errores y 207 advertencias.
- Se prueban datos conservados al reabrir, aislamiento por perfil, formatos corruptos
  sin sobrescritura, deltas/umbrales reales, repetición de sesiones y colas sin IDs ausentes.
- UI nativa Robolectric/API 33: carga/permiso/error/vacío, continuar sin reemplazar cola,
  callbacks de selección local, crear/añadir playlist, favorito y eliminación cancelada/
  confirmada. Se recorren Home→álbum→acciones con catálogo de prueba inyectado; no se
  simula un motor de audio ni se afirma reproducción física a partir del fixture.
- Se corrigió el bucle de medida del diálogo con ancho explícito y contenido acotado;
  las mismas aserciones se ejecutan con gráficos nativos, sin saltar ni omitir la prueba.
- Capturas de componentes reales con datos solo de prueba:
  [Home clara](screenshots/home-light.png), [Home oscura](screenshots/home-dark.png).
  Se revisó el fondo semántico del host y de la captura para evitar muestras de tema
  claro sobre el fondo negro por defecto de la Activity de prueba.
- Log local: `/workspace/toolchains/b05-validation-final.log`; informes XML habituales.
- No se acredita reproducción/segundo plano en dispositivo, rendimiento de grandes
  catálogos ni análisis/transiciones avanzados de AutoMix. B08/B11/B14 conservan esos trabajos.

## B06 — Verificación final

- Base B05: `7426157e00bc0187c7068fc6eda08aad7c531636`; JDK/SDK existentes, sin nuevas dependencias.
- Comando: `bash gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4`.
- Resultado final: exit 0, BUILD SUCCESSFUL en 1m 37s; 57 tareas, 17 ejecutadas.
- Pruebas XML: **53**, 0 fallos, 0 errores, 0 omitidas. Incluye las 38 de B05
  y 15 de catálogo, concurrencia, permisos, metadatos e integración Compose.
- Lint: **0 errores, 209 advertencias**. No se deshabilitan comprobaciones;
  recomendaciones KTX y advertencias del proyecto siguen visibles.
- APK debug: generado y `apksigner verify` exit 0. Sin firma/publicación release.
- Recursos: XML de strings válido y sin claves duplicadas; `git diff --check` correcto.
- Evidencia local: `/workspace/toolchains/b06-validation-final.log`, XML de pruebas
  y lint en `app/build`; no son requisitos de una futura clonación.
- Primera suite de 51 casos detectó expectativa errónea de búsqueda acentuada;
  corregida para respetar la función prevista. Posteriormente 53 correctas.
  Revisión final añadió aserciones del mensaje sin resultados en grupos y volvió
  a ejecutar las tres tareas completas correctamente.
- Actions: consulta de runs bloqueada por proxy (CONNECT 403); estado remoto pendiente.
- Robolectric SDK33 usa proveedor controlado y Compose real; no demuestra reproducción
  física, comportamiento de proveedores OEM, invalidación de imagen sin cambios de
  metadatos ni rendimiento en catálogos masivos. B07 sigue sin autorización.

## B07 — Verificación final

- Base: `15582e8139173802bc01ba7046a38d461e7ae03c`; toolchain existente.
- `bash gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4`:
  **exit 0, BUILD SUCCESSFUL en 1m 51s**, 57 tareas (18 ejecutadas).
- XML: **66 pruebas**, 0 fallos, 0 errores, 0 omitidas. Incluye las 53 de B06
  más 9 de comandos/conexión/eventos, 1 de sesión/controlador y 3 de UI.
- Lint: **0 errores y 208 advertencias**, sin deshabilitar verificaciones.
- APK debug generado; `apksigner verify` exit 0. Sin firma/publicación release.
- XML de strings válido sin claves duplicadas; `git diff --check` correcto.
- Evidencia local: `/workspace/toolchains/b07-validation-final.log`, XML de tests
  y lint en `app/build`; capturas conservadas en `docs/migration/screenshots/`
  (`mini-player.png`, `now-playing.png`) e inspeccionadas visualmente.
- Primera suite completa: 65 casos, aserción ambigua por texto repetido en Home
  y barra; selector acotado a la barra. La suite final completa de 66 pasó.
- Media3 usa SimpleBasePlayer con estado de audio controlado en pruebas; el puente
  MediaSession↔MediaController es real en proceso. Se verifican comandos externos,
  metadatos locales y que liberar el controlador no libera al reproductor de sesión.
- UI real Compose bajo Robolectric SDK33, gráficos nativos, 360×800 y 640×360.
  Se prueban apertura/cierre, seek accesible, modos y acciones de cola. No se
  reproduce audio ni se validan DSP, auriculares, foco, notificación visible o
  pantalla de bloqueo física. Mantener comprobaciones en dispositivo pendientes.
- Actions: API bloqueada por proxy CONNECT 403; no se afirma CI verde.

## B08 — Verificación final

`testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4`: exit 0
en 1m 49s, 57 tareas (18 ejecutadas). XML: 73 casos, 0 fallos/errores/omitidos.
Lint: 0 errores, 209 advertencias. APK debug generado y apksigner verify exit 0.
Log local `/workspace/toolchains/b08-validation-final.log`. Primera suite detectó
comparación de tipos numéricos incorrecta en una aserción; corregida sin cambiar
el comportamiento esperado. Pruebas de servicio usan eventos Media3 controlados,
no decodifican audio ni verifican ejecución física prolongada. Actions bloqueado
por proxy CONNECT 403; comprobaciones remotas pendientes.

## B09 — Verificación final

- Base: `75d2d6bc3faf00dd0c79f46128f9af30438889f9`.
- `testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4`: exit 0
  en 1m 43s, 57 tareas (18 ejecutadas). XML: **85 pruebas**, 0 fallos/errores/omitidas.
- Lint: **0 errores y 210 advertencias**, sin desactivar comprobaciones.
- APK debug y `apksigner verify` correctos; XML de strings válido y sin duplicados,
  git diff --check correcto. Sin firma/build release ni publicación de APK.
- Log local: `/workspace/toolchains/b09-validation-final.log`; XML de pruebas/lint
  en app/build. Captura `docs/migration/screenshots/synced-lyrics.png`, inspeccionada.
- Suite inicial: 85 casos, un timeout de prueba esperando callback IO sin procesar
  el looper Android. Se aisló y corrigió el test para drenar el looper y comprobar
  el estado real. La captura detectó etiqueta con contraste incorrecto y contenido
  subyacente accesible; overlay convertido en Surface opaca con color de contenido
  y semántica aislada. Suite completa repetida tras esa corrección y aprobada.
- Se comprueban LRC (marcas, offset, huecos, texto plano), ID3/FLAC con fixtures,
  UTF-16, prioridad local/caché, cancelación, parámetros Unicode y selección de
  grabación remota, resaltado/seguimiento y seek con offset en el grafo real.
- No hay peticiones de letras reales en pruebas. Fixtures propios solo en tests.
  LRCLIB en vivo pendiente por política de red; API Actions bloqueada por proxy
  CONNECT 403. No afirmar que una búsqueda real ni CI remoto hayan sido aprobados.
- Pendiente dispositivo físico: SAF/proveedores OEM, etiquetas en formatos no
  cubiertos, sincronización percibida y continuidad de audio/controles del sistema.

## B10 — Verificación final

`testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4`: exit 0
en 1m 55s, 57 tareas (15 ejecutadas). XML: 93 pruebas, sin fallos, errores u
omisiones. Lint: 0 errores, 210 advertencias. APK debug y apksigner verify correctos.
Log local `/workspace/toolchains/b10-validation.log`; captura documental
`bilingual-lyrics.png`, con originales/traducciones de fixture, inspeccionada.
Se usa interceptor OkHttp de pruebas, sin DNS/red, para validar contrato real
de petición/respuesta, caché y errores; no representa traducción remota en vivo.
La operación requiere configurar un servidor compatible autorizado; ninguno
se despliega ni se presupone disponible. Actions bloqueado por proxy CONNECT 403.
Pruebas físicas, accesibilidad con lector real y servicio remoto siguen pendientes.

## B11 — Verificación final

`testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4`: exit 0
en 1m 53s, 57 tareas (15 ejecutadas). **101 pruebas**, 0 fallos/errores/omitidas.
Lint: **0 errores y 210 advertencias**. APK debug y apksigner verify correctos;
git diff --check correcto. Sin build/firma release ni publicación de APK.
Log local `/workspace/toolchains/b11-validation.log`; captura `automix-queue.png`
inspeccionada. Selección pura con metadatos controlados y flujo Compose real con
Media3 controlado: semilla y posición conservadas, cola vacía no borra la actual.
Incluye todos los casos anteriores de traducción, letras, colecciones y navegación.
No se ejecuta red, decodificación/PCM ni crossfade en estas pruebas. Catálogo
masivo, batería y audio físico siguen pendientes; no se atribuyen BPM al catálogo.
Actions consultado: proxy CONNECT 403, estado remoto pendiente. No se alteran CI
ni sus verificaciones para aparentar éxito.

## B13 — Verificación final

`testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4`: exit 0
en 2m 3s, 57 tareas (17 ejecutadas). XML: **114 casos, 0 fallos/errores/omitidos**.
Lint: 0 errores y 211 advertencias. APK debug y apksigner verify correctos.
Log local `/workspace/toolchains/b13-validation-final.log`; capturas settings-light,
settings-dark y equalizer en docs/migration/screenshots, inspeccionadas.
Pruebas DSP procesan PCM real generado (campana, estantes, bypass, balance, memoria
entre bloques, flush y baja frecuencia de muestreo). Wiring de preferencias al
procesador probado sin recrear reproductor; pantallas reales Compose y persistencia.
Suite inicial: 114 casos, fallo de shader AGSL al dibujar cristal habilitado.
Se reemplazó el reflejo activo por borde de gradiente, conservando la prueba nativa.
Tras corregir también contraste oscuro, suite completa repetida y aprobada.
No prueba hardware, escucha subjetiva, latencia real ni ausencia de transitorios
al cambiar curvas; no afirma medición de consumo físico. B12 continúa pendiente.

## B14 — Integración local (cierre global pendiente)

- Pipeline Gradle: `testDebugUnitTest testReleaseUnitTest assembleDebug assembleRelease
  lintDebug lintRelease :app:dependencies --configuration releaseRuntimeClasspath
  --no-daemon --max-workers=4`. Exit 0 en **3m 1s**, 120 tareas (38 ejecutadas).
- Debug: **114** casos; release: **114** casos; en ambas, 0 fallos/errores/omitidos.
  Son 114 casos por variante, no 228 pruebas funcionales distintas. R8 release correcto.
- Lint debug: 0 errores/211 advertencias; release: 0 errores/210 advertencias.
- Primera ejecución de release falló 28 casos Compose por actividad de test ausente.
  Registro solo en memoria con regla ordenada antes del escenario, manteniendo
  todas las aserciones. No se añade dependencia/actividad de pruebas a release.
- Después solo se completaron textos legales/NOTICE, sin cambiar código de app:
  `assembleDebug assembleRelease` exit 0 en **51s**, 90 tareas (14 ejecutadas).
- Ambos APK: ZIP íntegro, firmas verificadas, nueve textos de licencia comparados
  byte por byte con fuentes, doce .so exclusivamente ARM64. Release sin actividad
  ComponentActivity de pruebas ni descriptor RobolectricActivityRule en DEX.
- Identidad: com.music.aurelis; min33/target35/compile36; v1.9.0-dev, versionCode1.
  Firma local de release: **Android Debug**, fallback existente, sin cambiar claves
  ni configuración. **No es firma/publicación de producción**.
- Hashes/tamaños: APK_EVIDENCE.json. Logs locales b14-validation-final.log y
  b14-legal-packaging-final.log bajo /workspace/toolchains.
- CI estático: actionlint 1.7.7 oficial (checksum verificado), exit0; YAML y bash -n
  válidos. SDK36, lint ambas variantes y gate final de lint/tests/build; PRs
  sustituidos se cancelan a favor del último, main/manual independientes.
- Dependencias: 175 módulos/constraints/BOMs de runtime, POMs/herencia, inventario TSV.
  No es auditoría de vulnerabilidades ni certificación de cada binario.
- Rendimiento indicativo en JVM17 del host, heap máximo512MiB, catálogo sintético
  10 000 temas/800 artistas, salida20: calentamiento494.62ms, siguientes311.24ms y
  194.72ms. IDs únicos/tamaño verificados. No son mediciones de Android ni batería.
- CI remoto: la API sigue bloqueada, pero la vista web pública permite consultar
  runs. Estado exacto del commit de entrega se registra al publicarlo; no se
  confunde éxito histórico con el actual. Dispositivo/servicios en vivo no probados.
- No cerrar B14 global hasta resolver B12, CSV, procedencia/suministro de fuentes
  de binarios y pruebas físicas/CI. Ver RELEASE_READINESS.md.

Consulta web tras publicar B14: run #116 en curso, #115 histórico figura exitoso.
No se acreditó el SHA exacto desde la página específica y no se afirma CI verde
del commit final/documental; consultar PROGRESS/Actions al retomar.


## B12 y continuidad B13 — 2026-11-05

Base `2ecce6e96d15f6e033a9ee407fa9f0958504c911` más cambios de perfiles.
Compilación real debug/release, R8, pruebas y lint de ambas variantes: exit 0,
`BUILD SUCCESSFUL in 5m 39s`.

```bash
source /workspace/toolchains/activate.sh
bash gradlew :app:testDebugUnitTest :app:testReleaseUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease --no-daemon --max-workers=4
```

- Debug: 126 casos, 0 fallos/errores/omitidos; lint 0 errores,
  218 advertencias.
- Release: 126 casos, 0 fallos/errores/omitidos; lint 0 errores,
  217 advertencias.
- Selección inicial: 31 casos correctos. Errores de imports y fixtures corregidos
  antes de la validación completa; imágenes inválidas usan decoder nativo real.
- Dos pasadas separadas del mismo agente: código/arquitectura y funcionamiento/
  integración; UI Compose nativa, persistencia, corrupción/borrado, restore Media3,
  navegación y acciones pendientes, además de toda la suite existente.
- ZIP íntegro, nueve avisos legales idénticos y doce .so ARM64 en cada APK.
  Firma verificada por apksigner; mismo fallback original de desarrollo.
- APK_EVIDENCE.json actualizado para estos artefactos; no reutilizar hashes B14.
- Logs locales: /workspace/toolchains/b12-targeted.log, b12-final.log y
  b12-debug-signature.log / b12-release-signature.log. No son archivos del repo.
- No se han probado dispositivos físicos; servicios elegidos y CI del commit
  publicado se registran aparte. Sin cambios de secretos, firma ni applicationId.


Código entregado: `30569f194d8f05ebe728caa3b51406a27ccbf96b` (mismo código de la validación precommit;
solo este checkpoint documental es posterior). Las siete advertencias adicionales
son StaticFieldLeak del singleton que guarda únicamente applicationContext,
sin Activity/Service, y recomendaciones UseKtx en operaciones commit cuyo resultado
se comprueba para no anunciar persistencia ante un fallo. No se suprimen reglas.
CI del nuevo commit no acreditado: el listado público aún mostraba #117 anterior.


## Beta-local B01.1 — perfil único

Base b2bf434 más cambios de perfil. BUILD SUCCESSFUL in 5m 12s.
137 pruebas debug y137 release sin fallos/errores/omitidos;47 casos seleccionados
previos. assemble/lint de ambas variantes correctos y R8 verificado. Evidencia
actual de APK y resultados en [B01_1_EVIDENCE](beta-local/B01_1_EVIDENCE.json).
No trasladar hashes históricos a esta entrega. Falta hardware/SAF real y CI por SHA.
No cambia ORT ni resuelve el hallazgo ELF16KB de B00.
