# Progreso de migración

## Reanudación

Leer MASTER_PLAN, este registro, COMPONENT_MAP, BUILD_STATUS y LICENSE_AUDIT.
Comprobar rama, HEAD, cambios locales y último commit antes de actuar. No repetir
la auditoría completa; verificar solo referencias cambiadas y componentes del bloque
autorizado. B02 no está autorizado.

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

B02 — Arquitectura base, únicamente tras autorización explícita del usuario.
Su primera tarea será trazar persistencia y llamadas existentes, fijar contratos
locales y asociación de datos a perfiles, y definir migraciones sin pérdida de datos.

## Verificaciones documentales B01

- Rutas de destino del mapa contrastadas con el checkout.
- Enlaces Markdown relativos de esta carpeta comprobados.
- Catorce bloques con dificultad y aceptación; sin autorización implícita de B02.
- `git diff --cached --check` requerido antes de cada commit documental.
- No se repitió Gradle: no hubo cambios de código, recursos o dependencias.
- Las dos revisiones son pasadas separadas; los resultados no sustituyen una
  revisión externa de licencias ni una validación de audio en dispositivo.
