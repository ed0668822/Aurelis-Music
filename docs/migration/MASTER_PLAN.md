# Migración BitChord → Aurelis Music

## Alcance y referencias

B01: auditoría y planificación exclusivamente. No autoriza B02 ni cambios de aplicación.
Documentación redactada el 4 de noviembre de 2026 (America/El_Salvador).

- Destino: `ed0668822/Aurelis-Music`, base `9bf3fd3db8f5870f942f5167dca5bc77e3c48edd`.
- Origen: `kushagrasinghx/BitChord`, referencia fija `b690dc146009d4815825fa4a20815ef2e4ee8898`.
- BitChord se consultó exclusivamente en lectura mediante Git HTTPS y archivos de GitHub; no se modificó ni publicó nada allí.
- Público: usuarios de música almacenada en su dispositivo y futuros mantenedores de Aurelis.
- Éxito final: experiencia visual cercana a BitChord, reproducción local sin Internet, funciones reales, datos persistentes y sin regresiones demostradas.

Consultar [mapa](COMPONENT_MAP.md), [licencias](LICENSE_AUDIT.md),
[validación](BUILD_STATUS.md) y [progreso](PROGRESS.md) antes de continuar.
Cada tarea cloud ya está aislada: utilizar el checkout existente, sin worktrees salvo petición expresa.

## Arquitectura y límites

Conservar `com.music.aurelis`, las firmas, publicación y funciones existentes compatibles.
Mantener `AurelisPlaybackService` (MediaSessionService + ExoPlayer) y su cadena DSP;
conectar las pantallas a su controlador, sin añadir otro motor de reproducción.
Los modelos `LocalSong`, `LocalAlbum` y `LocalArtist`, MediaStore y repositorios actuales
son el punto de partida. No sustituirlos solo para imitar la estructura del origen.

Flujo previsto: permisos → MediaStore → repositorio local → estado de biblioteca →
Home/biblioteca/búsqueda → controlador Media3 → servicio y controles Android.
Favoritos, listas e historial alimentarán Home y recomendaciones. B02 definirá un
identificador de perfil estable para esos datos; B12 implementará su gestión visual.
No elegir una nueva base de datos sin comprobar primero la persistencia existente y
su migración: esa decisión corresponde a la especificación autorizada de B02.

Letras: archivo LRC e incrustadas → caché → proveedor externo permitido; traducción
separada, cancelable y cacheada. La disponibilidad de un endpoint no acredita permiso
de uso. Reproducción local no dependerá de proveedores de letras o traducción.
AutoMix separará selección local, análisis y transiciones; conservará una cola
reproducible cuando no existan metadatos o características musicales suficientes.

No importar módulos de streaming, autenticación remota, TV, Wear OS o escritorio.
No eliminar funciones originales en esta migración sin autorización. No copiar
recursos o código con procedencia no resuelta. No introducir credenciales locales
para perfiles salvo requisito posterior; nunca contraseñas en texto plano.

## Dependencias y compatibilidad

| Área | Aurelis base | BitChord inspeccionado | Decisión |
|---|---|---|---|
| Estructura | módulo Android `app` | `shared`, `sharedUi`, app y otras plataformas | Adaptar piezas; no importar el monorepo |
| AGP / Gradle | 8.10.1 / wrapper 8.11.1 | AGP 8.10.1 | Conservar toolchain destino inicialmente |
| Kotlin | 2.2.21 | 2.3.20 | Adaptar APIs antes de proponer actualización |
| SDK compile/min/target | 36 / 33 / 35 | teléfono 37 / 26 / 36 | Conservar identidad y compatibilidad destino |
| Media3 | 1.11.1 | 1.11.0 | No degradar ni copiar dependencias de streaming |
| Compose BOM | 2024.12.01 | mismo BOM más Foundation 1.10.0 explícita | Revisar cada API visual importada |
| Nativo destino | NDK 26.1.10909125, CMake 3.22.1, ARM64 | DSP y componentes con licencias individuales | Auditar procedencia antes de adaptar |

## Bloques y aceptación

El orden B01–B14 se mantiene. Cada fila requiere autorización explícita independiente;
no representa permiso para ejecutarla. Dificultad relativa, sin promesas de tiempo o créditos.

| Bloque | Trabajo y áreas previstas | Dificultad / riesgo | Aceptación y comprobación |
|---|---|---|---|
| B01 | Los cinco documentos de esta carpeta | Media: precisión de evidencias/licencias | Dos revisiones; diff documental; entrega trazable |
| B02 | `data`, modelos, servicios y persistencia existentes | Alta: pérdida de datos y duplicación | Contratos locales y perfil definidos; migración probada con datos previos; build |
| B03 | Temas, componentes compartidos y recursos autorizados | Media: APIs Compose y derechos de recursos | Claro/oscuro, tipografía accesible, capturas y compilación |
| B04 | Navegación existente, barra inferior y rutas | Media: pilas de retorno | Atrás, restauración, pestañas y apertura del reproductor sin duplicaciones |
| B05 | Home y componentes `home` | Alta: datos vacíos o simulados | Continuar, recientes, más escuchadas, álbumes, artistas, favoritos, listas, añadidas, recomendaciones y AutoMix con datos reales; carga/vacío/error/permisos |
| B06 | MediaStore, permisos, biblioteca y búsqueda | Alta: URI obsoleta y coste de escaneo | Revocación, búsqueda/orden/filtros, metadatos/carátulas, altas/bajas/movidos y actualización incremental |
| B07 | Mini/reproductor completo y host Media3 existentes | Alta: regresión de audio | Play/pausa, anterior/siguiente, seek, repetición, aleatorio, cola, fondo, notificación y bloqueo en dispositivo |
| B08 | Persistencia de favoritos/listas/historial y cola | Alta: integridad de relaciones | Reinicio conserva datos, acciones idempotentes y referencias a canciones eliminadas seguras |
| B09 | Repositorio, parsers y paneles de letras | Alta: sincronización y proveedores | LRC/incrustadas/caché antes de red; seek, resaltado, scroll y ausencia de letras con fixtures |
| B10 | Traducción, caché y presentación bilingüe | Media: endpoint y privacidad | Original intacto, idioma y caché correctos; offline, timeout y cancelación explícitos |
| B11 | Selección local y análisis/transiciones existentes | Alta: licencias, DSP y batería | Cola exclusivamente local, sin repeticiones excesivas; fallback sin BPM/género; límites y consumo documentados |
| B12 | Perfiles y asociaciones preparadas en B02 | Alta: aislamiento y migración | Crear/cambiar/borrar con confirmación; avatar opcional; favoritos/listas/historial separados sin Internet |
| B13 | Ajustes, animaciones y personalización | Media: rendimiento y accesibilidad | Persistencia, reducción de movimiento, fluidez y regresiones medidas |
| B14 | Integración, optimización y auditoría final | Alta: compatibilidad completa | Debug/release, pruebas relevantes, dispositivo, licencias y CI con estados independientes |

Dividir un bloque grande en subbloques antes de implementarlo: cada uno tendrá diff
acotado, criterios, revisiones y commit. B09 y B10 son prioridad alta dentro del orden
solicitado; no adelantarlos sin autorización. B11 depende de biblioteca, historial y
cola; B12 depende del diseño de aislamiento de B02. No simular una función pendiente.

## Protocolo de trabajo y entrega

1. Leer esta carpeta, comprobar Git y ejecutar únicamente el bloque autorizado.
2. Inspeccionar solo componentes afectados; actualizar mapa y riesgos con evidencia.
3. Implementar sin cambios de identidad, secretos, firmas o dependencias innecesarias.
4. Revisión 1: arquitectura, imports, recursos, licencias, duplicaciones y preservación.
5. Revisión 2 separada: comportamiento, integración, build/pruebas pertinentes y CI disponible.
6. Corregir hallazgos y repetir solo verificaciones afectadas; registrar pendientes reales.
7. Actualizar documentos, commit y PR sin force push, merge automático ni borrado de ramas ajenas.
8. Consultar Actions si existe acceso; no alterar workflows para ocultar fallos.
9. Parar antes del siguiente bloque. Mantener un punto seguro de reanudación.

Para B01, el diff es exclusivamente documental: `git diff --check`, enlaces/rutas y
coherencia de referencias son las comprobaciones requeridas. La compilación previa
sirve de línea base, no de prueba funcional ni de CI del futuro commit.
No repetir builds completos sin cambios que los justifiquen.
