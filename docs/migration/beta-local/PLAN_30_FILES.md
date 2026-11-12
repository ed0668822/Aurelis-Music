# Plan reorganizado de ejecución — Aurelis Music

**Regla actualizada el 11 de noviembre de 2026:** máximo **30 rutas de archivo modificadas por bloque ejecutable/entrega**.

## Contrato de máximo 30 archivos

1. Cada subbloque, como `B06.3`, puede afectar **hasta 30 rutas distintas** en toda su ejecución, sumando todos los commits. Incluye archivos creados, modificados, eliminados, pruebas, recursos, documentación y GitHub Actions. Un renombrado cuenta las rutas de origen y destino.
2. Un archivo cambiado varias veces dentro del mismo subbloque cuenta una vez; hacer varios commits **no reinicia** el presupuesto. Antes de trabajar, enumerar rutas previstas y comprobar las rutas reales del diff acumulado antes de cerrar. Objetivo operativo recomendado de 8–20 rutas, con **30 como máximo absoluto**.
3. Si el alcance necesita más de 30 rutas, dividir antes de editar en `Bxx.y-a`, `Bxx.y-b`, etc. Cada hijo tiene máximo 30 rutas, dependencias, pruebas, dos revisiones y un cierre propio; el ID padre permanece abierto hasta que los hijos estén completos. No esconder archivos ni saltar controles para cumplir el máximo.
4. Verificar por SHA exacto compilación, lint, tests y CI, con fallos documentados; el hecho de que un archivo esté en Git o DEX no certifica su compilación individual. No aprobar audio físico, gapless o USB solo por emulador.
5. Mantener `feature/bitchord-local-migration`, una app/MediaSession, único perfil local, ARM64, contratos de licencia, firma/secretos y exclusiones. Sin force-push, cambio de `main`, streaming ni publicación Beta sin puertas completas.

## Bloques B00–B13 reorganizados por dependencias

La secuencia conserva todos los **14 bloques padre y sus 48 subunidades originales**; no renumera ni invalida el historial. Se audita el estado real antes de repetir una función ya implementada. Cada subunidad de la tabla tiene presupuesto **<= 30 archivos**.

| Etapa | Área | Orden de subbloques |
|---|---|---|
| 00 | B00: Auditoría y dependencias | B00.1 Arquitectura; B00.2 Licencias; B00.3 Base documental |
| 01 | B01: Perfil local y biblioteca | B01.1 Perfil y respaldo; B01.2 MediaStore/metadatos; B01.3 SAF/carpetas |
| 02 | B02: Sistema visual | B02.1 Tokens/fuentes; B02.2 Cristal/desenfoque; B02.3 Gestos/componentes |
| 03 | B03: Navegación y Home | B03.1 Navegación; B03.2 Home/carruseles; B03.3 Estados/recuperación |
| 04 | B04: Explore, Search y Library | B04.1 Géneros/Explore; B04.2 Búsqueda/historial; B04.3 Biblioteca/selección |
| 05 | B05: MiniPlayer y reproductor | B05.1 MiniPlayer/calidad; B05.2 Vertical/panel; B05.3 Horizontal/transiciones |
| 06 | B06: Playback y cola | B06.1 Cola; B06.2 Controles/timer; B06.3 Sesión/restore/eventos; B06.4 Android Auto |
| 07 | B07: Letras visuales | B07.1 Parser/tiempo; B07.2 Efectos/gestos; B07.3 Compartir |
| 08 | B08: Proveedores y traducción | B08.1 Registro/LRCLIB; B08.2 Revisión legal de proveedores; B08.3 Traducción/caché |
| 09 | B09: DSP y audio avanzado | B09.1 DSP; B09.2 Precisión/rutas; B09.3 Bluetooth; B09.4 USB/bit-perfect |
| 10 | B10: AutoMix Beta | B10.1 Inferencia/caché; B10.2 Planificación; B10.3 Crossfade/decks; B10.4 LoopMix; B10.5 Rendimiento/audio real |
| 11 | B11: Ajustes y Replay | B11.1 Controles; B11.2 Idiomas/rendimiento/Storage; B11.3 Replay; B11.4 Exportar/importar |
| 12 | B12: Funciones adicionales | B12.1 Widgets; B12.2 Carátulas/metadatos; B12.3 Last.fm/ListenBrainz; B12.4 Archivos/etiquetas |
| 13 | B13: Validación final | B13.1 Regresiones/fidelidad; B13.2 Hardware/rendimiento; B13.3 Licencias/artefactos/CI |

**Orden de ejecución actual:** (a) confirmar CI del SHA más reciente y corregir fallos; (b) conciliar hechos implementados contra PROGRESS_TRACKER y cerrar brechas de B01–B08 sin duplicarlas; (c) continuar B09 DSP/USB, B10 AutoMix, B11 Ajustes/Replay, B12 extras y B13 aceptación final. Una prueba preparada no equivale a una prueba ejecutada.

**Puertas condicionadas:** B06.4 Android Auto exige compatibilidad real; B08.2 requiere condiciones de uso/API; B09.3/4 y B10.3/5 precisan hardware o pruebas de audio reales; B12.2/3 exige consentimiento y licencias; B12.4 requiere copia, SAF y recuperación. Los bloqueos no se consideran aprobación ficticia.

## Anexo A00–A15, sin duplicar bloques

Las ampliaciones A se implementan dentro del B correspondiente, consumiendo su presupuesto de archivos y sin modificar la identidad del bloque.

| Ampliaciones | Bloques de integración | Alcance |
|---|---|---|
| A00/A15 | B00/B13 | Auditoría y QA |
| A01/A10/A11 | B09, B10.3, B13.2 | Gapless, normalización, DAC y pruebas |
| A02 | B10.1/B10.2/B10.5 | BPM, AutoMix, rendimiento |
| A03/A04/A05 | B01, B12.4 | Metadatos, M3U8, duplicados |
| A06/A14 | B07/B08/B04 | Letras |
| A07/A08 | B03/B04/B05/B06 | Home y reproductor |
| A09 | B06.3/B11.3 | Escucha y Replay |
| A12 | B11.4/B12.4 | Respaldo y recuperación |
| A13 | B12.1 | Widgets |

## Distinción obligatoria de CI y Anexo M

- **B00–B13**: bloques de desarrollo. **B01–B06 de GitHub Actions**: fases técnicas distintas (compilar, lint, pruebas JVM, APK, emulador, reportes/entrega). **P2/P3 y QA00–QA05**: evidencias complementarias, no duplicar ni confundir sus numeraciones.
- Las **141 decisiones SÍ del Anexo M** indican interés/planificación; sus casos condicionados requieren verificación técnica/legal y nueva revisión de alcance antes de implementar. Las `NO`, `MÁS TARDE` y no respondidas no quedan aprobadas por esta reorganización.
- Los complementos de Mornye que no encajen en B00–B13 permanecen como anexo posterior a B13 o unidad separada, siempre con tope de 30 rutas por entrega.

## Registro obligatorio al cerrar cualquier subbloque

| Dato | Evidencia |
|---|---|
| ID, objetivo y dependencias | `Bxx.y` o hijo dividido |
| SHA inicial y final | Rama exacta comprobada |
| Archivos previstos y reales | Listas deduplicadas; **máximo 30** rutas |
| Cambios y riesgos | Sin violar exclusiones o licencias |
| Revisión 1 / revisión 2 | Arquitectura y regresiones sin atribuir a revisores distintos |
| CI y pruebas | Resultados por SHA y fallos reales |
| Bloqueos y siguientes tareas | Hardware, permisos, servicios o datos faltantes |

Referencias: [MASTER_PLAN](MASTER_PLAN.md), [IMPLEMENTATION_BLOCKS](IMPLEMENTATION_BLOCKS.md), [PROGRESS_TRACKER](PROGRESS_TRACKER.md), [FILE_MAPPING](FILE_MAPPING.md), [DECISIONS](DECISIONS.md), [TEST_STRATEGY](TEST_STRATEGY.md).
