# Progreso de migración

## Reanudación

Leer MASTER_PLAN, este registro, COMPONENT_MAP, BUILD_STATUS y LICENSE_AUDIT.
Comprobar rama, HEAD, cambios locales y último commit antes de actuar. No repetir
la auditoría completa; verificar solo referencias cambiadas y componentes del bloque
autorizado. B04 fue autorizado expresamente; B05 y siguientes no están autorizados.

PR actual: [Aurelis Music Beta UI and components](https://github.com/ed0668822/Aurelis-Music/pull/1).
Creado y nombrado por el usuario. Referencia `refs/pull/1/head` coincidente con la
rama publicada `feature/bitchord-local-migration`, comprobada al iniciar B03.
Después del push B03, la página HTML de GitHub confirmó título y estado OPEN
(5 commits en ese momento), aunque la referencia de lectura del PR aún mostraba
el SHA anterior. La rama remota sí confirmó el SHA nuevo; no se reescribió ninguna referencia.
Los errores de creación API de B01/B02 registrados abajo son históricos y no
significan que actualmente no exista el PR.

## B01 — Auditoría y planificación

- Estado: auditoría y documentación completadas; rama publicada; creación de PR bloqueada por acceso API.
- Rama: `feature/bitchord-local-migration`.
- Base Aurelis: `9bf3fd3db8f5870f942f5167dca5bc77e3c48edd`.
- Origen BitChord: `b690dc146009d4815825fa4a20815ef2e4ee8898`.
- Acceso: ambos repositorios legibles; BitChord solo referencia, sin cambios.
- Creados: `MASTER_PLAN.md`, `PROGRESS.md`, `COMPONENT_MAP.md`, `BUILD_STATUS.md`,
  `LICENSE_AUDIT.md`, todos dentro de `docs/migration/`.
- Archivos existentes modificados: ninguno; no migración UI ni B02.
- Revisión 1 — arquitectura/procedencia: completada en una pasada separada.
  Se verificaron 24 rutas destino, versiones Gradle/Kotlin/SDK/Media3, stubs,
  servicio Media3, rutas de origen seleccionadas y avisos AGPL. Se mantuvieron
  explícitos los límites de la inspección; no se certificó el árbol completo.
- Revisión 2 — integración/verificabilidad: completada en una segunda pasada
  del mismo agente, con objetivo distinto; no intervino un segundo revisor humano.
  Se contrastaron los registros reales de build y NO-SOURCE, enlaces locales,
  cobertura B01–B14 y diff documental. Pruebas funcionales/CI siguen pendientes.
- Compilación: línea base debug/lint correcta; unitarias NO-SOURCE. No nuevo build
  por documentación; ejecución funcional y release pendientes.
- GitHub Actions: consulta API denegada (`Forbidden`); estado remoto desconocido.
- Commit de documentos: `04c28c90a07c3dcc668d232fe2c2dbb8d24779cc`.
  Este registro se completa en un commit posterior de trazabilidad; consultar
  `git log -2 --oneline` para ambos commits, sin referencias circulares a su propio SHA.
- Publicación: `git push --set-upstream origin feature/bitchord-local-migration`
  terminó con exit 0 y confirmó la nueva rama remota. El commit posterior de
  trazabilidad requiere su propio push y verificación remota.
- Pull Request: no creado. `gh pr create --base main --head
  feature/bitchord-local-migration` falló con
  `Post https://api.github.com/graphql: Forbidden`. No se recibió número ni URL de PR.
- Enlace de creación devuelto por GitHub (no es un PR existente):
  https://github.com/ed0668822/Aurelis-Music/pull/new/feature/bitchord-local-migration
- Título sugerido: `B01: auditoría y plan de migración local de BitChord`.
  Descripción: cinco documentos de auditoría/planificación; sin cambios de aplicación;
  dos revisiones documentales; línea base debug/lint correcta, unitarias NO-SOURCE;
  CSV, dispositivo y consulta de Actions pendientes.

## Hallazgos y bloqueos

- Ya existen componentes adaptados; preservar el servicio Media3 y evitar duplicación.
- Playlists locales y escaneo de carpetas tienen puntos de entrada vacíos; perfil
  actual único. No afirmar cobertura funcional completa a partir de nombres de archivo.
- Origen multiplataforma y versiones distintas requieren adaptaciones selectivas.
- AutoMix incluye procedencia AGPL/Orchard; nuevas copias requieren revisar condiciones.
- Falta `Formato de licencias.csv`: incorporarlo antes de cabeceras definitivas.
- Lectura y escritura Git HTTPS confirmadas en Aurelis. GitHub API devuelve
  Forbidden tanto al consultar permisos como al crear el PR; acceso de creación
  de PR y consulta de Actions no disponible. No se deduce que falte un token.
  Se necesita acceso autorizado a la API de GitHub o crear el PR con el enlace
  anterior. No solicitar credenciales en chat ni cambiar secretos para resolverlo.

## Próximo bloque

B05 — Home local, únicamente tras nueva autorización explícita del usuario
y cierre de B04. No iniciar automáticamente.

## Verificaciones documentales B01

- Rutas de destino del mapa contrastadas con el checkout.
- Enlaces Markdown relativos de esta carpeta comprobados.
- Catorce bloques con dificultad y aceptación; sin autorización implícita de B02.
- `git diff --cached --check` requerido antes de cada commit documental.
- No se repitió Gradle: no hubo cambios de código, recursos o dependencias.
- Las dos revisiones son pasadas separadas; los resultados no sustituyen una
  revisión externa de licencias ni una validación de audio en dispositivo.

## B02 — Arquitectura base

- Autorización: concedida expresamente por el usuario tras configurar su identidad Git local.
- Estado: implementación y revisiones completadas; rama publicada; PR bloqueado por API.
- Base: `e6f58f937a1f84166236705c7f8b9a3a2947f571`; misma rama de migración.
- Añadidos: ProfileId, ProfileStorage, LocalLibrarySource; ProfileStorageTest y
  LibraryViewModelTest en los paquetes correspondientes de `app/src/test/`.
- Modificados: AurelisApplication, ProfileRepository, SettingsRepository,
  LibraryViewModel y dependencias de pruebas de `app/build.gradle.kts`;
  documentos de esta carpeta actualizados.
- Migración: ámbitos default apuntan a los mismos archivos/claves, sin copia ni
  borrado; identidad independiente del nombre. Aislamiento preparado para B08/B12.
- Funcionamiento existente corregido: inicialización de perfil antes de Ajustes;
  cancelación de biblioteca propagada, sin convertirla en error visible.
- Revisión 1 (arquitectura/código): completada como pasada separada del mismo agente.
  Se contrastaron las llamadas de Ajustes, archivos previos, inicialización Application,
  límites de perfil, constructor Android, imports y ausencia de duplicación de escáner/motor.
  Sin cambios de firmas, identificadores de app ni versiones de producción.
- Revisión 2 (funcionamiento/integración): completada como segunda pasada del mismo
  agente. Se añadió cobertura de una canción local y se ejecutaron las pruebas finales;
  conservación de datos/aislamiento y reintentos contrastados con resultados XML.
  No equivale a una revisión de otro humano ni a pruebas en dispositivo.
- Compilación: assembleDebug correcto. Unitarias finales: 11 correctas, 0 fallos,
  0 errores, 0 omitidas. Lint sin errores y 209 advertencias; no se deshabilitan checks.
- Commit de implementación: `2a10e63fd89db9993ebfd1ef21920cc761aec886`.
  Autor/committer: `ed0668822 <ed0668822@gmail.com>`, verificados antes del commit.
  El commit posterior de documentación registra la entrega; consultar `git log -2`.
- Publicación: push confirmado de `e6f58f9` a `2a10e63` en
  `feature/bitchord-local-migration`; registro de entrega se publica en otro commit.
- Pull Request: intento de creación B01–B02 falló con
  `Post https://api.github.com/graphql: Forbidden`. No hay URL de PR confirmada.
  Se conserva la rama y todo el trabajo. Enlace de creación previamente devuelto
  por GitHub: https://github.com/ed0668822/Aurelis-Music/pull/new/feature/bitchord-local-migration
  Título: `B01–B02: auditoría y arquitectura local por perfil`.
- GitHub Actions: consulta de runs de esta rama falló con Forbidden en api.github.com;
  estado remoto desconocido, sin afirmar checks exitosos ni modificar workflows.
- Revisión documental final: enlaces relativos y `git diff --check` correctos;
  manifiesto confirma AurelisApplication como punto de inicio y APK con firma válida.
- Límites: sin UI de perfiles ni funcionalidades de B08; sin nuevo motor Media3.
- Cabeceras definitivas pendientes del CSV; no se alteran atribuciones de terceros.
- Próximo bloque: B03, pendiente de autorización. Ningún cambio visual implementado.

## B03 — Sistema visual

- Autorización: concedida expresamente; PR existente nombrado por el usuario.
- Base: `d1006710aa1b300a3e6aa862a0bdca26a13884a8`; misma rama y PR #1.
- Estado: implementación y dos revisiones completadas; rama publicada para el PR #1.
- Modificados: temas raíz/dinámico, ColorSchemes y Theme; SearchField, SearchBar,
  SettingsComponents, Skeletons y configuración/dependencias de pruebas Compose.
- Añadidos: ThemeContrastTest y VisualSystemTest; documentación y cuatro PNG en
  `docs/migration/screenshots/` (claro/oscuro, escala de texto 1 y 1.5).
- Revisión 1 (arquitectura/recursos/licencias): completada como pasada separada
  del mismo agente. Se contrastaron preferencia persistida, llamadas del tema,
  formas, fuente de sistema, alias compatibles, semántica de controles y POM de
  dependencias. No hay nuevas fuentes, iconos o imágenes de terceros importados.
- Revisión 2 (funcionamiento/integración): completada como segunda pasada del
  mismo agente. Interacción real Compose, contraste y las 11 pruebas previas
  correctas. Las capturas iniciales mostraron una etiqueta recortada al ampliar
  texto: se corrigió a dos líneas y se comprobó ausencia de elipsis/recorte vertical
  y presencia de todos los caracteres, además de revisión visual de los PNG finales.
- Compilación: assembleDebug correcto, firma APK verificada. Suite final: 16 pruebas,
  0 fallos, 0 errores, 0 omitidas. Lint sin errores y 208 advertencias.
  Se corrigió una llamada de test a la API de Robolectric; la captura PixelCopy
  no recibía callback y se sustituyó por dibujo real de la vista con gráficos
  nativos. Se afinó la comprobación de texto para no confundir redondeo de ancho
  subpíxel con truncación. No se deshabilitaron pruebas ni se cambiaron workflows.
- Commit de implementación: `7f747a32e920940b25e7a3ea3553ccf9949021dd`.
  Autor/committer `ed0668822 <ed0668822@gmail.com>`, verificados antes del commit.
- Publicación: push de `d100671` a `7f747a3` confirmado en la rama existente;
  el registro final se publica en otro commit documental (consultar `git log -2`).
- PR: https://github.com/ed0668822/Aurelis-Music/pull/1,
  «Aurelis Music Beta UI and components», confirmado OPEN por la página de GitHub.
  Se conserva su título y no se crea otro PR ni se sobrescribe una descripción de usuario desconocida.
- Actions: la consulta de runs de la rama devuelve Forbidden en api.github.com;
  estado remoto sin verificar. No se afirma CI verde ni se modifican workflows.
- Límites: sin cambios de navegación, Home, motor musical ni diseño de reproductor.
  SF Pro no se copia; validación física y cabeceras definitivas siguen pendientes.
- Próximo bloque B04 requiere autorización nueva.

## B04 — Navegación

- Autorización: concedida expresamente por el usuario tras B03.
- Base: `309c732e5051f80f198e533ac6752c3902678f64`; misma rama y PR #1.
- Estado: implementación y dos revisiones completadas; entrega en curso.
- Modificados: MainActivity, AppNavigator, GlassNavBar, LocalMusicScreen,
  ReplayScreen, EqualizerScreen, strings y documentación.
- Añadidos: AppNavigationHost, AppNavigatorTest, NavigationIntegrationTest.
- Integración: pila única y pestañas tipadas; enlaces de Biblioteca y Ajustes;
  regreso cola→reproductor→origen, Back Android y estado por pestaña/página guardable.
  Se mantiene el motor Media3 y se evita el mini reproductor duplicado.
- Revisión 1 (arquitectura/preservación): completada como pasada separada del mismo
  agente. Se revisaron rutas/callbacks, constructor del navegador, orden de detalles,
  estados guardables pequeños, ausencia de doble controlador activo y preservación
  del arranque/Media3. Se verificaron XML y enlaces documentales; sin dependencias nuevas.
- Revisión 2 (funcionamiento/integración): completada como segunda pasada del mismo
  agente. Se ejercitó el grafo real Compose con enlaces y pantallas existentes, Atrás,
  scroll y consultas restauradas, selección accesible y retorno de cola. Se amplió el
  caso de búsqueda local para incluir texto y restauración antes de cerrar con Atrás.
  No equivale a una revisión humana independiente ni a una prueba física del servicio.
- Compilación incremental: compileDebugKotlin correcto en 26s.
- Debug: correcto; firma APK verificada. Suite final: 28 pruebas correctas,
  0 fallos, 0 errores y 0 omitidas. Lint sin errores y 205 advertencias.
- Commit, publicación y Actions: pendientes de entrega.
- Límites: sin Home nueva, detalles álbum/artista, estadísticas ni controles EQ nuevos.
  Replay/Equalizer se exponen como estructuras existentes incompletas; no funciones simuladas.
- Próximo bloque: B05, pendiente de nueva autorización.
