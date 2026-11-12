# IMPLEMENTATION_BLOCKS

Nuevo plan beta-local B00–B13; no equivale a bloques anteriores homónimos.
Cada ficha contiene los16 campos requeridos y cada subunidad hereda exactamente
la ficha del padre, con alcance y aceptación estrechados en su propia fila.
Referencias Fxx resuelven rutas exactas en [FILE_MAPPING](FILE_MAPPING.md).
No copiar familias enteras: editar solo archivos necesarios para el alcance de la
subunidad. Autorizar un bloque no autoriza el siguiente; subunidades terminan con
informe y solo avanzan si están incluidas en la autorización vigente.

**Regla vigente:** máximo **35 rutas en entregas fáciles/medias y 6 en difíciles**,
acumuladas entre todos sus commits; incluye código, pruebas, recursos, documentación,
configuración y eliminaciones. Renombrar cuenta origen/destino. El manifiesto se
declara antes de editar y el diff acumulado se comprueba al cerrar. Véanse los hijos,
dependencias y aceptación de [PLAN_CONTINUATION](PLAN_CONTINUATION.md); conservan las
48 subunidades históricas y los IDs B00–B13. [PLAN_30_FILES](PLAN_30_FILES.md) queda
como historial, con su límite sustituido. Si una entrega no cabe, se revisa su
división antes de implementar; no se elude el máximo repartiendo commits.

## Contrato común

LOCK: LICENSE, firma/secretos/claves/configuración de publicación, applicationId,
namespace e identidad oficial; BitChord íntegro y solo lectura; historial previo.
No importar módulos excluidos. B00 edita exclusivamente documentación.

Revisión1=arquitectura/lógica/integración/compilación. Revisión2=UI/comportamiento/
licencias/regresiones. Dos pasadas del mismo agente se describen honestamente;
no atribuirlas a humanos independientes. Registrar tests nuevos vs UP-TO-DATE.
Una compilación o captura no demuestra comportamiento.

**Compilación y suites Android exclusivamente en GitHub.** Los siguientes comandos
U/N/D/R son referencias para el runner, no instrucciones de ejecución local. Localmente
solo inspección, comprobaciones estáticas y fixtures Python; no Gradle ni APK.

- U: `bash gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --max-workers=4`
- N: U más `bash gradlew :app:externalNativeBuildDebug --no-daemon --max-workers=4`
- D: `bash gradlew :app:connectedDebugAndroidTest --no-daemon --max-workers=4` con dispositivo.
- R: `bash gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:lintRelease --no-daemon --max-workers=4`

Firmas/publicación originales; solo cambios de la unidad autorizada se entregan
misma rama/PR, autor local ed0668822, sin force push/merge ni renombrar PR.


## B00 — Auditoría y dependencias

1. **Objetivo:** Auditoría y dependencias: Ocho documentos, referencia/inventario, dependencias y evidencia reproducible; bloqueos explícitos.
2. **Dependencias previas:** Aprobación recibida
3. **Referencias BitChord:** F00; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F00; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F00; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Grafo de rutas/estado/almacenamiento/JNI; sin implementar funciones.
8. **Funciones conectadas:** Ocho documentos, referencia/inventario, dependencias y evidencia reproducible; bloqueos explícitos.
9. **Riesgos de compilación:** Versiones/ABI/ONNX, dependencias y build base.
10. **Riesgos de licencias:** Auditoría documental; no copiar código ni retirar avisos.
11. **Pruebas necesarias:** Comprobar rutas e inventarios; base debug/release, suites reales, ZIP/ELF, actionlint.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** R + actionlint + ZIP/ELF (tests --rerun en B00); comandos exactos definidos arriba
13. **Criterios de aceptación:** Ocho documentos, referencia/inventario, dependencias y evidencia reproducible; bloqueos explícitos. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Media

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B00.1 | Arquitectura y rutas activas | F00 | Rutas activas, stubs y propietarios identificados con evidencia. |
| B00.2 | Licencias y dependencias | F00 | Origen fijo y175 módulos comparados; límites legales registrados. |
| B00.3 | Base y entrega documental | F00 | Dos revisiones, suites/build y entrega misma rama/PR sin código de app. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B01 — Perfil local y biblioteca

1. **Objetivo:** Perfil local y biblioteca: Solo un perfil visible; datos antiguos recuperables; canciones/carpetas/géneros reales.
2. **Dependencias previas:** B00
3. **Referencias BitChord:** F01 F02; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F01 F02; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F01 F02; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Consolidación no destructiva a perfil único; catálogo MediaStore/SAF y metadatos con identidad conservada.
8. **Funciones conectadas:** Solo un perfil visible; datos antiguos recuperables; canciones/carpetas/géneros reales.
9. **Riesgos de compilación:** Migración, URI revocadas y volúmenes extraíbles.
10. **Riesgos de licencias:** Perfil propio; parsers de etiquetas según avisos/permiso upstream.
11. **Pruebas necesarias:** Migración0/1/varios perfiles, corrupción/reinicio, permisos, filtros/volúmenes y carpetas.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** Solo un perfil visible; datos antiguos recuperables; canciones/carpetas/géneros reales. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B01.1 | Perfil único y respaldo | F01 | Crear/cambiar/selector de usuarios fuera del flujo; nombre/foto y datos activos preservados. |
| B01.2 | MediaStore y metadatos | F02 | Catálogo correcto ante cambios/revocación; géneros leídos y desconocidos explícitos. |
| B01.3 | Carpetas y permisos | F02 | Cada carpeta seleccionada abre sus canciones; ningún método vacío accesible. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B02 — Componentes visuales

1. **Objetivo:** Componentes visuales: Efectos activos con contraste/estabilidad y fuentes autorizadas; diferencias documentadas.
2. **Dependencias previas:** B01
3. **Referencias BitChord:** F03; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F03; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F03; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Tokens/tema/preferencias gráficas compartidos; efectos compatibles, gestos y accesibilidad.
8. **Funciones conectadas:** Efectos activos con contraste/estabilidad y fuentes autorizadas; diferencias documentadas.
9. **Riesgos de compilación:** AGSL/Haze/Compose y consumo gráfico.
10. **Riesgos de licencias:** No SF Pro; OFL y atribuciones de componentes preservadas.
11. **Pruebas necesarias:** Capturas claro/oscuro/texto grande, motion/blur reducido, renderer y gestos.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** Efectos activos con contraste/estabilidad y fuentes autorizadas; diferencias documentadas. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B02.1 | Tokens, fuentes e iconos | F03 | Identidad Aurelis, Roboto/Vazirmatn y recursos autorizados. |
| B02.2 | Cristal y desenfoque | F03 | Sin crash de shader; preferencias afectan al árbol activo. |
| B02.3 | Gestos y componentes | F03 | Menús/toggles/formas funcionan y respetan accesibilidad. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B03 — Navegación y Home

1. **Objetivo:** Navegación y Home: Home y barra inferior fieles, todos los destinos funcionales y sin ejemplos.
2. **Dependencias previas:** B02
3. **Referencias BitChord:** F04; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F04; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F04; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** AppUi local inicializado; navegación guardada y carruseles de LocalHomeData reales.
8. **Funciones conectadas:** Home y barra inferior fieles, todos los destinos funcionales y sin ejemplos.
9. **Riesgos de compilación:** Host no instalado, rutas y callbacks remotos legacy.
10. **Riesgos de licencias:** Componentes GPL y avisos existentes.
11. **Pruebas necesarias:** Back/restauración, abrir cada tarjeta, vacíos/errores/permisos.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** Home y barra inferior fieles, todos los destinos funcionales y sin ejemplos. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Media-alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B03.1 | Navegación y barra inferior | F04 | Rutas existentes preservadas y Back/restauración correctos. |
| B03.2 | Home y carruseles locales | F04 | Cada tarjeta abre datos reales de la colección indicada. |
| B03.3 | Estados y recuperación | F04 | Permiso/error/vacío no producen cargas infinitas ni datos ficticios. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B04 — Explore, Search y Library

1. **Objetivo:** Explore, Search y Library: Explore sustituye marcador; Sin clasificar funciona; Library incluye todas las secciones aprobadas.
2. **Dependencias previas:** B03
3. **Referencias BitChord:** F02 F05; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F02 F05; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F02 F05; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Géneros reales a rutas; búsqueda de todos los tipos y selección/orden coherentes.
8. **Funciones conectadas:** Explore sustituye marcador; Sin clasificar funciona; Library incluye todas las secciones aprobadas.
9. **Riesgos de compilación:** Metadatos ausentes y listas grandes.
10. **Riesgos de licencias:** Reutilización GPL; no clasificación musical inventada.
11. **Pruebas necesarias:** Acentos, sin resultados, género vacío, filtros/orden y multiselección.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** Explore sustituye marcador; Sin clasificar funciona; Library incluye todas las secciones aprobadas. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B04.1 | Géneros y Explore | F02 F05 | Tarjetas de género navegan a sus canciones, incluida Sin clasificar. |
| B04.2 | Search e historial | F05 | Canciones/álbumes/artistas/playlists/géneros locales buscables. |
| B04.3 | Library y selección | F05 | Lista/cuadrícula/carpetas/favoritos/historial y acciones múltiples funcionan. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B05 — MiniPlayer y reproductor

1. **Objetivo:** MiniPlayer y reproductor: Dos layouts activos, controles reales y calidad veraz sin Dolby.
2. **Dependencias previas:** B04
3. **Referencias BitChord:** F06 F07; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F06 F07; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F06 F07; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Un estado de controlador para mini/vertical/horizontal/panel; calidad tipada origen/salida.
8. **Funciones conectadas:** Dos layouts activos, controles reales y calidad veraz sin Dolby.
9. **Riesgos de compilación:** Duplicación de UI/estado, rotación y formatos desconocidos.
10. **Riesgos de licencias:** Fondos/componentes GPL y fuentes autorizadas.
11. **Pruebas necesarias:** Abrir/cerrar/rotar, controles, gestos, formatos conocidos/desconocidos y capturas.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** Dos layouts activos, controles reales y calidad veraz sin Dolby. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B05.1 | MiniPlayer y calidad | F06 F07 | Badge proviene del archivo/decoder; desconocidos sin inventar bitrate/profundidad. |
| B05.2 | Vertical y panel | F06 | Apertura/cierre/dock/controles en posiciones previstas, estado único. |
| B05.3 | Horizontal, fondos y transiciones | F06 | Rotación conserva reproducción y controles, fondos respetan reducción de movimiento. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B06 — Playback y cola

1. **Objetivo:** Playback y cola: Una sesión/servicio, cola coherente, restore pausado y registro real sin contar seek.
2. **Dependencias previas:** B05
3. **Referencias BitChord:** F08 F09 F18; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F08 F09 F18; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F08 F09 F18; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** LocalPlaybackCoordinator y Player lógico estable; cola canónica; registro temporal SQLite fuera de audio.
8. **Funciones conectadas:** Una sesión/servicio, cola coherente, restore pausado y registro real sin contar seek.
9. **Riesgos de compilación:** Comandos/restauración, eventos duplicados, foco y futuras transiciones.
10. **Riesgos de licencias:** Adaptación GPL y API Media3; revisar contratos usados.
11. **Pruebas necesarias:** Play Next/drag/duplicates/seek/pause/restore; timers/speed/silence/autoplay; notificación y bloqueo.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** Una sesión/servicio, cola coherente, restore pausado y registro real sin contar seek. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Muy alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B06.1 | Coordinador y cola | F08 F09 | Play Next inserta después de actual; reordenar conserva item/posición. |
| B06.2 | Controles y temporizador | F08 | Timers/velocidad/silencio/autoplay local ejecutan su efecto. |
| B06.3 | Sesión, restauración y eventos | F08 F18 | Registro nuevo con histórico previo; foco/notificación/bloqueo y restore coherentes. |
| B06.4 | Android Auto compatible | F08 | Contrato de biblioteca/controladores autorizado y probado, no solo manifest. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B07 — Letras visuales

1. **Objetivo:** Letras visuales: Seguimiento/animación correctos; ausencia de letras no bloquea audio.
2. **Dependencias previas:** B06
3. **Referencias BitChord:** F10 F11; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F10 F11; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F10 F11; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Parser/modelo temporal/reloj real/panel/gestos/exportación conectados a una sola canción.
8. **Funciones conectadas:** Seguimiento/animación correctos; ausencia de letras no bloquea audio.
9. **Riesgos de compilación:** Timestamps inválidos, rendimiento y resultados antiguos.
10. **Riesgos de licencias:** Atribuciones del sistema de letras y condiciones de compartir contenido.
11. **Pruebas necesarias:** Fixtures LRC/enhanced/TTML, palabra/sílaba, seek/offset/manual y exportación.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** Seguimiento/animación correctos; ausencia de letras no bloquea audio. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B07.1 | Parsers y temporización | F10 | LRC/enhanced/TTML producen modelo válido y sincronizan con posición real. |
| B07.2 | Efectos y gestos | F10 F11 | Foco/blur/pausas/offset y lectura manual correctos. |
| B07.3 | Compartir letras | F11 | Imagen con fragmento elegido y URI segura, sin acceso amplio al filesystem. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B08 — Proveedores y traducción

1. **Objetivo:** Proveedores y traducción: LRCLIB prioritario; otros clientes solo habilitados con condiciones verificadas.
2. **Dependencias previas:** B07
3. **Referencias BitChord:** F12; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F12; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F12; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Registro extensible; local/caché primero; búsquedas explícitas y cliente de traducción permitido.
8. **Funciones conectadas:** LRCLIB prioritario; otros clientes solo habilitados con condiciones verificadas.
9. **Riesgos de compilación:** Resultados equivocados, límites, cancelación y secretos.
10. **Riesgos de licencias:** Autorización de servicios por separado; licencia de cliente no concede contenido.
11. **Pruebas necesarias:** Offline/timeout/404/429, cancelación/cambio de canción y caché.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** LRCLIB prioritario; otros clientes solo habilitados con condiciones verificadas. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Media-alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B08.1 | Registro y LRCLIB | F12 | Lookup explícito, prioridad y errores reales. |
| B08.2 | Evaluación de BetterLyrics, BiniLyrics y LRC Red | F12 | Cada proveedor queda autorizado y probado o deshabilitado con motivo documentado. |
| B08.3 | Traducción y caché | F12 | Cliente configurable existente, original/traducido, límites y cancelación. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B09 — DSP y audio avanzado

1. **Objetivo:** DSP y audio avanzado: Controles medibles; no anunciar bit-perfect sin prueba; resolver incompatibilidad ELF16KB de ONNX antes de beta final.
2. **Dependencias previas:** B06 y B08
3. **Referencias BitChord:** F13 F14; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F13 F14; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F13 F14; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Capacidades y rutas reales; DSP con propietario único; bit-perfect excluye procesamiento incompatible.
8. **Funciones conectadas:** Controles medibles; no anunciar bit-perfect sin prueba; resolver incompatibilidad ELF16KB de ONNX antes de beta final.
9. **Riesgos de compilación:** JNI/CMake/USB/formatos/ELF16KB y clipping.
10. **Riesgos de licencias:** Mornye LGPL3-only, libusb LGPL, Oboe Apache y avisos DSP.
11. **Pruebas necesarias:** PCM conocido, EQ/balance/normalización/limitador, conexión/desconexión y hardware.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U, N y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** Controles medibles; no anunciar bit-perfect sin prueba; resolver incompatibilidad ELF16KB de ONNX antes de beta final. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Muy alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B09.1 | DSP | F13 | Curvas/normalización/limitador/espacialización tienen efecto medido sin duplicarse. |
| B09.2 | Precisión, rutas y compatibilidad nativa | F13 F14 | Origen/salida conocidos; formato real y ZIP/ELF compatibles. |
| B09.3 | Bluetooth | F14 | Dispositivos/permisos/desconexión manejados sin propiedades inventadas. |
| B09.4 | USB y bit-perfect | F14 | Salida y restricciones verificadas en hardware o prueba pendiente explícita que bloquea cierre. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B10 — AutoMix Beta

1. **Objetivo:** AutoMix Beta: Balanceado2; sin stubs; transiciones locales reales; indicador Beta.
2. **Dependencias previas:** B09
3. **Referencias BitChord:** F15 F16; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F15 F16; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F15 F16; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Análisis persistente → plan → deck entrante → mezcla → handoff; una sesión pública.
8. **Funciones conectadas:** Balanceado2; sin stubs; transiciones locales reales; indicador Beta.
9. **Riesgos de compilación:** Modelos/ORT, memoria, sincronización y saturación.
10. **Riesgos de licencias:** Orchard AGPL, modelos con procedencia pendiente y Signalsmith MIT.
11. **Pruebas necesarias:** Modo1/2/3 efectivo, BPM conocido, caché, seek/pause/fin corto, mezcla escuchada y consumo.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U, N y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** Balanceado2; sin stubs; transiciones locales reales; indicador Beta. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Muy alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B10.1 | Inferencia y caché | F15 | Analizador real; modos1/2/3, default2, sesiones liberadas y caché invalidable. |
| B10.2 | Planificación | F15 | Planes desde características medidas; fallback explícito sin datos inventados. |
| B10.3 | Crossfade y decks | F16 | Comandos/handoff/ganancias coherentes; no segundo servicio/sesión. |
| B10.4 | LoopMix | F16 | Procesamiento nativo con avisos y licencias acreditados; prueba real. |
| B10.5 | Rendimiento y audio real | F15 F16 | Inferencia prioriza playback y no se declara completa sin transiciones escuchadas. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B11 — Ajustes y Replay

1. **Objetivo:** Ajustes y Replay: Sin Dolby/icon selector/exclusiones; Storage solo canciones/tamaño; Replay sin fechas inventadas.
2. **Dependencias previas:** B10
3. **Referencias BitChord:** F17 F18 F19; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F17 F18 F19; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F17 F18 F19; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Una pantalla activa, preferencias con consumidores; eventos por períodos; respaldo versionado recuperable.
8. **Funciones conectadas:** Sin Dolby/icon selector/exclusiones; Storage solo canciones/tamaño; Replay sin fechas inventadas.
9. **Riesgos de compilación:** Pantallas duplicadas/importación destructiva y pasado no reconstruible.
10. **Riesgos de licencias:** Fuentes/traducciones GPL/OFL y avisos de exportación.
11. **Pruebas necesarias:** Cada ajuste/idioma/plural, datos de período, export/import fallido/reinicio.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** Sin Dolby/icon selector/exclusiones; Storage solo canciones/tamaño; Replay sin fechas inventadas. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B11.1 | Categorías y controles | F17 | Secciones aplicables fieles, búsqueda y cada control con efecto real. |
| B11.2 | Idiomas, rendimiento y Storage | F17 F19 | Idioma persistido, frecuencia soportada, canciones/tamaño real; sin caché visible. |
| B11.3 | Replay completo | F18 | Tarjetas/historias/póster/compartir basados en eventos disponibles. |
| B11.4 | Exportar e importar | F18 | Secretos excluidos, validación/confirmación/backup/journal y recuperación. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B12 — Funciones adicionales

1. **Objetivo:** Funciones adicionales: Widgets reales; conexiones opcionales; guardar copia de tags por defecto.
2. **Dependencias previas:** B11
3. **Referencias BitChord:** F20 F21 F22 F23; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F20 F21 F22 F23; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F20 F21 F22 F23; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Widgets mediante sesión; enriquecimiento opt-in; outbox de eventos; URI autorizadas para etiquetas.
8. **Funciones conectadas:** Widgets reales; conexiones opcionales; guardar copia de tags por defecto.
9. **Riesgos de compilación:** Credenciales/reintentos, permisos y daño a originales.
10. **Riesgos de licencias:** API/carátulas/contenido y taggers por licencia; no streaming.
11. **Pruebas necesarias:** App cerrada/offline/token revocado, envíos idempotentes, salida etiquetada reproducible.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** U y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** Widgets reales; conexiones opcionales; guardar copia de tags por defecto. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B12.1 | Widgets | F20 | Reproducción/acciones desde widget siguen estado real incluso con UI cerrada. |
| B12.2 | Carátulas y metadatos remotos | F21 | Opt-in; overlay no sobrescribe datos locales ni agrega reproducción remota. |
| B12.3 | Last.fm y ListenBrainz | F22 | Autorización oficial/credenciales protegidas; sin secretos APK ni falso éxito sin claves. |
| B12.4 | Archivos y etiquetas | F23 | Copia validada por defecto; permisos y recuperación antes de sustituir originales. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.

## B13 — Validación final

1. **Objetivo:** Validación final: APK verificable, evidencia por función, sin bloqueos legales/datos/audio críticos.
2. **Dependencias previas:** B12 y todos los componentes incluidos resueltos
3. **Referencias BitChord:** F00; columnas origen de FILE_MAPPING
4. **Existentes a modificar:** F00; columna destino; limitar al alcance autorizado
5. **Nuevos previstos:** F00; columna nuevos y pruebas necesarias; no creados hasta implementar su unidad
6. **Archivos que no deben tocarse:** LOCK y cualquier archivo ajeno a las filas/alcance de esta unidad
7. **Cambios de arquitectura:** Verificación extremo a extremo y ausencia de exclusiones; fixes solo por defecto demostrado.
8. **Funciones conectadas:** APK verificable, evidencia por función, sin bloqueos legales/datos/audio críticos.
9. **Riesgos de compilación:** R8/ABI/páginas16KB, hardware/rendimiento y pipeline.
10. **Riesgos de licencias:** Modelos/fuentes de binarios y avisos completos antes de distribución.
11. **Pruebas necesarias:** R/D, matrices visuales, audio prolongado, hardware, accesibilidad y CI por SHA.; dos revisiones y TEST_STRATEGY
12. **Comando de compilación:** R y D; comandos exactos definidos arriba
13. **Criterios de aceptación:** APK verificable, evidencia por función, sin bloqueos legales/datos/audio críticos. Debe pasar verificación pertinente y registrar pendientes reales.
14. **Condiciones de parada:** Pérdida de datos, build roto, regresión crítica, licencia/procedencia no acreditada del componente o nueva dependencia sin justificación. Hardware ausente bloquea declarar esa capacidad completa.
15. **Resultado esperado:** Unidad conectada, documentada, verificable y entregada; no simular ni confundir compilación con funcionamiento.
16. **Complejidad:** Alta

### Subunidades

| ID | Alcance | Filas de archivos heredadas | Aceptación específica adicional |
|---|---|---|---|
| B13.1 | Regresiones y fidelidad | F00 | Matriz visual y funcional acreditada; diferencias autorizadas registradas. |
| B13.2 | Hardware y rendimiento | F00 | USB/Bluetooth/ARM64/16KB/audio largo y consumo probados o cierre bloqueado. |
| B13.3 | Licencias, artefactos y CI | F00 | Textos/fuentes/procedencia y firmas/ZIP/CI exacto; no merge automático. |

Cada subunidad hereda los16 campos anteriores y LOCK; sus filas estrechan las
referencias/existentes/nuevos del padre, y alcance/aceptación limitan qué se edita.
