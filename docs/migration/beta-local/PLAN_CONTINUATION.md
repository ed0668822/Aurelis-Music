# Plan de continuación — Aurelis Music / BitChord

## Aprobación, base y alcance

Selección del usuario: todas las 31 adaptaciones de la lista de continuidad.
Plan aprobado mediante «Implement the proposed plan». La ejecución conserva
B00–B13 y sus subunidades; los sufijos -a, -b, etc. son entregas independientes,
no bloques principales nuevos. Planificar todo no declara B09 iniciado.

- Rama única: `feature/bitchord-local-migration`; PR1
  [Aurelis Music Beta UI and components](https://github.com/ed0668822/Aurelis-Music/pull/1).
- Base de esta entrega documental: `f6309d2961384ca9a07197b51eb7c016d11f0b8d`.
  Incorporada por avance rápido desde c5ff687d; se conservan 09b848df y el plan
  documental remoto. No había cambios pendientes ni commits locales exclusivos.
- BitChord base: `b690dc146009d4815825fa4a20815ef2e4ee8898`, solo lectura.
- Delta seleccionado: contraste de controles del reproductor desde
  `acf219a7e4a7f52f026d4336d5c7ac5cf7bfafcc`; no actualizar automáticamente
  la referencia completa ni importar las otras novedades de main.
- [PLAN_30_FILES](PLAN_30_FILES.md) se conserva íntegro como historial. Su límite
  queda sustituido por el contrato 35/6 de este documento. Sus anexos conservan
  su alcance previo: esta aprobación de 31 adaptaciones no aprueba automáticamente
  propuestas NO, MÁS TARDE o no respondidas de otros anexos.

Este documento gobierna la continuación junto a [DECISIONS](DECISIONS.md),
[FILE_MAPPING](FILE_MAPPING.md) y [IMPLEMENTATION_BLOCKS](IMPLEMENTATION_BLOCKS.md).
Las observaciones de base y los estados antiguos no sustituyen evidencia por SHA.
No se repiten B00–B05, B06.1, parsers/temporización ni B08 ya implementados:
se conservan y se completan únicamente las brechas verificadas.

## Contrato de entrega 35/6

| Dificultad de la entrega | Máximo de rutas distintas |
|---|---:|
| Fácil o media | 35 |
| Difícil, muy difícil o con riesgo de datos/audio | 6 |

El presupuesto incluye código, pruebas, recursos, documentación, configuración y
eliminaciones. Un renombrado cuenta origen y destino. Repetir un archivo o dividir
una entrega en commits no reinicia el conteo. Antes de editar, registrar SHA inicial,
objetivo, dificultad y manifiesto de rutas. Validar el diff acumulado al cerrar.
Si no cabe, revisar la división antes de implementar; cada hijo necesita función,
dependencias, aceptación y cierre propios. No quitar pruebas ni atribuciones para
cumplir el límite. Los padres permanecen abiertos mientras tengan hijos pendientes.

Mantener identidad Aurelis/com.music.aurelis, perfil único local, una sesión pública,
AurelisPlaybackService, Liquid Glass estable, ARM64, firmas, secretos y atribuciones.
BitChord no se modifica. No copiar módulos completos ni reescribir autorías.
No main, ramas remotas adicionales, rebase/force push, merge automático ni Release
final automático. Autoría nueva local al repositorio: ed0668822 y el correo
ed0668822@gmail.com, verificado con GitHub antes de esta entrega.

Excluidos streaming, Cast, WebDAV, SMB, escuchar juntos, servidor compartido,
fuentes JavaScript, TV y Wear. Vídeos, carátulas animadas externas y Discord aplazados.
Android Auto pertenece a B06.4 y no implica Android TV.

## Correspondencia de las 31 adaptaciones seleccionadas

| Selección | Capacidad | Continuación |
|---|---|---|
| 01 | Fidelidad de Home | B03.2-a |
| 02 | Navegación y MiniPlayer | B03.1-a; B05.1-a |
| 03 | Reproductor vertical y dock | B05.2-a |
| 04 | Reproductor horizontal | B05.3-a/-b |
| 05 | Menús y hápticos | B02.3-a/-b |
| 06 | Contraste reciente de BitChord | B05.2-b |
| 07 | Shuffle visible y reversible | B06.1-a/-b |
| 08 | Controles y temporizador | B06.2-a/-b/-c |
| 09 | Sesión, restauración y eventos | B06.3-a/-b/-c/-d |
| 10 | Android Auto | B06.4-a/-b |
| 11 | Efectos de letras | B07.2-a/-b |
| 12 | Gestos y offset | B07.2-c |
| 13 | Compartir letras | B07.3-a/-b |
| 14 | Evaluación de otros proveedores | B08.2-a y entregas por proveedor autorizado |
| 15 | DSP medible | B09.1-a/-b/-c/-d |
| 16 | Origen/salida y compatibilidad nativa | B09.2-a/-b |
| 17 | Bluetooth y rutas | B09.3-a/-b |
| 18 | USB y bit-perfect | B09.4-a/-b/-c/-d |
| 19 | Análisis real, caché y modos 1/2/3 | B10.1-a/-b/-c/-d/-e/-f |
| 20 | Planificación AutoMix | B10.2-a/-b |
| 21 | Crossfade y decks | B10.3-a/-b/-c |
| 22 | LoopMix | B10.4-a/-b/-c |
| 23 | Ajustes organizados y aplicables | B11.1-a/-b |
| 24 | Idiomas, rendimiento y almacenamiento | B11.2-a/-b/-c |
| 25 | Replay | B11.3-a/-b/-c/-d |
| 26 | Respaldo recuperable | B11.4-a/-b/-c |
| 27 | Widgets | B12.1-a/-b/-c |
| 28 | Carátulas/metadatos remotos opcionales | B12.2-a/-b |
| 29 | Last.fm | B12.3-a/-b/-d |
| 30 | ListenBrainz | B12.3-a/-c/-d |
| 31 | Archivos y etiquetas locales | B12.4-a/-b/-c/-d |

## Orden ejecutable

Cada sufijo de una fila es una entrega separada con el máximo indicado; no un
permiso de gastar ese presupuesto varias veces en la misma entrega. Conservar
dependencias y aceptación de las fichas originales, estrechadas por estas filas.

### Preparación y calidad complementaria

| Entrega | Máximo | Trabajo y aceptación |
|---|---:|---|
| Preparación | Sin código | Cuenta/identidad/estado/remotos; preservar trabajo; avance rápido. Detenerse ante conflictos no preservables automáticamente. |
| Registro del plan | 6 | Esta entrega: nuevo plan, reglas, referencias, decisiones y progreso; conservar historial. |
| Revisión de las tres carpetas | 6 | Contrastar ui/audit, ui/components y ui/haptics por variante y SHA; explicar LocalAppBackdrop sin inventar un fallo de compilación. |
| Aclaraciones de auditoría | 6 | Corregir cuatro comentarios «no se compila»/PolyDev y registrar evidencia; no forzar clases ni eliminar legacy. |
| CI12.3-a | 6 | Función → entrada activa → acción → prueba → resultado; reutilizar matrices y distinguir simulación/Robolectric/emulador/hardware. |
| CI12.3-b | 6 | Cubrir conexiones faltantes de Home/biblioteca/navegación/MiniPlayer; acciones observables. |
| CI12.3-c | 6 | Cubrir reproductor/cola/letras; reutilizar los siete casos Media3 existentes, sin duplicarlos ni llamarlos prueba física. |

### B02–B05: brechas visuales sobre integración existente

| Entrega | Máximo | Trabajo y aceptación |
|---|---:|---|
| B02.3-a / -b | 6 | Primero anclaje/retorno de menús; después hápticos/accesibilidad en rutas activas, preferencias y objetivos táctiles. |
| B03.1-a | 35 | Navegación inferior, estados, Atrás y restauración. |
| B03.2-a | 35 | Home: cabeceras, tarjetas, carruseles y espaciado con datos locales reales. |
| B03.3-a | 35 | Permiso/carga/vacío/error y recuperación, sin datos ficticios. |
| B04: comprobación incremental | 35 | Conservar búsqueda/géneros/carpetas/selección; corregir solo diferencias reproducidas por matriz funcional. |
| B05.1-a | 6 | MiniPlayer/progreso/transiciones y calidad veraz. |
| B05.2-a / -b | 6 | Primero gestos/panel/dock; después contraste de acf219a7, respetando accesibilidad. |
| B05.3-a / -b | 6 | Primero letras/cola en paneles horizontales; después rotación/fondos y continuidad del mismo estado. |

### B06: reproducción y cola

| Entrega | Máximo | Trabajo y aceptación |
|---|---:|---|
| B06.1-a / -b | 6 | Primero shuffle visible/reversible; después Play Next/edición, respetando ocurrencias, canción/posición y selección manual. |
| B06.2-a | 6 | Temporizador del servicio: vencimiento y cancelación coherentes. |
| B06.2-b | 6 | Velocidad/salto de silencio con capacidad, estado y restricciones reales. |
| B06.2-c | 6 | Continuación automática con catálogo local, sin streaming. |
| B06.3-a | 6 | Restaurar cola/posición ante reinicio, ausentes y comandos concurrentes. |
| B06.3-b | 6 | Foco/auriculares/notificación/bloqueo mediante la misma sesión. |
| B06.3-c / -d | 6 | Primero almacenamiento versionado de eventos; después registro desde servicio y deduplicación/seek/recuperación. |
| B06.4-a / -b | 6 | Primero catálogo/comandos Android Auto en servicio existente; después controladores autorizados y navegación local. |

### B07–B08: letras

| Entrega | Máximo | Trabajo y aceptación |
|---|---:|---|
| B07.1 | Sin reimplementación | Conservar parsers/caché/temporización como regresiones. |
| B07.2-a / -b | 6 | Primero efectos visuales; después foco/pausas/seguimiento con reducción de movimiento/desenfoque. |
| B07.2-c | 6 | Lectura manual/retorno/offset sin romper seek. |
| B07.3-a | 6 | Selección y composición de tarjeta real en lugar del bitmap vacío. |
| B07.3-b | 6 | URI segura, texto/carátula/identidad, cancelación/permisos y limpieza temporal. |
| B08.1 / B08.3 | Sin reimplementación | Conservar LRCLIB, búsqueda explícita, prioridad local/caché y traducción configurable/cancelable. |
| B08.2-a | 35 | Evaluación individual de condiciones y contratos; registrar autorizado o bloqueado. |
| B08.2-b y sucesivos por proveedor autorizado | 6 | Integración individual, errores/límites/respuestas tardías; los no acreditados siguen deshabilitados. |

### B09: DSP y audio avanzado

Incluido en el plan, sin iniciar su implementación hasta cumplir dependencias y
la autorización de ejecución correspondiente. No confundirlo con correcciones
PCM previas ya confirmadas en el historial.

| Entrega | Máximo | Trabajo y aceptación |
|---|---:|---|
| B09.1-a | 6 | Contratos PCM y regresiones existentes de frames incompletos/formatos. |
| B09.1-b | 6 | EQ/balance medidos con señales conocidas. |
| B09.1-c | 6 | Normalización/limitador sin clipping ni procesamiento duplicado. |
| B09.1-d | 6 | Espacialización/controles e incompatibilidades explícitas. |
| B09.2-a / -b | 6 | Primero origen/salida; después formato efectivo/JNI/ZIP/ELF16KB. |
| B09.3-a / -b | 6 | Primero permisos/observación Bluetooth; después rutas/desconexiones físicas. |
| B09.4-a | 6 | Procedencia/licencias/contratos de transporte USB. |
| B09.4-b / -c | 6 | Primero transporte exclusivo; después política bit-perfect/liberación/recuperación. |
| B09.4-d | 6 | DAC físico: frecuencia/profundidad/restricciones; sin hardware no hay cierre bit-perfect verificado. |

### B10: AutoMix Beta

| Entrega | Máximo | Trabajo y aceptación |
|---|---:|---|
| B10.1-a | 6 | Procedencia/exportación/operadores y runtime de modelos; incompatible bloquea inferencia, sin sustitución silenciosa. |
| B10.1-b | 6 | Lectura/decodificación local y características medidas. |
| B10.1-c / -d | 6 | Ritmo y voz reales en entregas separadas; reemplazar constantes/vacíos. |
| B10.1-e | 6 | Modos1/2/3, Balanceado2, interop1 y configuración de sesión observable. |
| B10.1-f | 6 | Caché invalidable, una pista en análisis, cancelación y prioridad playback. |
| B10.2-a / -b | 6 | Primero planes medidos; después fallback crossfade/reproducción normal con motivo. |
| B10.3-a | 6 | Dos decks internos coordinados, sesión pública estable. |
| B10.3-b | 6 | Preparación/ganancias sin clipping ni saltos de estado. |
| B10.3-c | 6 | Handoff/cancelación por seek, cola, pausa, ruta o fallo. |
| B10.4-a / -b / -c | 6 | Procedencia/contrato nativo; procesamiento LoopMix; integración coordinada como entregas separadas. |
| B10.5-a / -b | 6 | Primero rendimiento/contención; después transiciones audibles en dispositivo. Mantener Beta. |

### B11: ajustes, Replay y respaldo

| Entrega | Máximo | Trabajo y aceptación |
|---|---:|---|
| B11.1-a / -b | 35 / 6 | Primero categorías/búsqueda local; después capacidades/controles reales con motivo si no disponibles. |
| B11.2-a | 35 | Idiomas/recursos con fuentes autorizadas. |
| B11.2-b | 6 | Preferencias visuales/frecuencias soportadas. |
| B11.2-c | 6 | Canciones/tamaños conocidos, desconocidos explícitos; caché no es música. |
| B11.3-a | 6 | Consultas temporales sobre eventos de B06.3. |
| B11.3-b | 35 | Tarjetas/clasificaciones de Replay. |
| B11.3-c / -d | 6 | Historias y exportación de póster por separado; sin fechas antiguas inventadas. |
| B11.4-a | 6 | Exportación versionada, sin música/secretos. |
| B11.4-b / -c | 6 | Primero validación/resumen; después staging/journal/restauración recuperable. |

### B12: funciones adicionales

| Entrega | Máximo | Trabajo y aceptación |
|---|---:|---|
| B12.1-a | 35 | Registro/recursos de widgets reutilizando clases existentes. |
| B12.1-b / -c | 6 | Primero acciones de sesión; después actualización/recuperación con UI cerrada. |
| B12.2-a / -b | 6 | Primero consentimiento/cliente/caché; después overlays reversibles sin escribir etiquetas locales. |
| B12.3-a | 6 | Credenciales protegidas/outbox idempotente y eventos existentes. |
| B12.3-b / -c | 6 | Last.fm y ListenBrainz independientes. |
| B12.3-d | 6 | Reintentos/revocación/desconexión sin duplicar envíos. |
| B12.4-a | 6 | Operaciones SAF/permisos locales. |
| B12.4-b | 6 | Lectura y matriz explícita de formatos de taggers revisados. |
| B12.4-c | 6 | Copia escrita y verificada mediante reproducción/metadatos. |
| B12.4-d | 6 | Reemplazo solo confirmado, autorizado por Android y recuperable. |

### B13: cierre

| Entrega | Máximo | Trabajo y aceptación |
|---|---:|---|
| B13.1-a / -b | 6 | Regresión funcional y comparación visual como cierres separados. |
| B13.2-a | 6 | ARM64/instalación/JNI/audio prolongado/interrupciones/segundo plano. |
| B13.2-b | 6 | USB/Bluetooth/rendimiento/consumo/temperatura. |
| B13.3-a | 6 | Procedencia/atribuciones/modelos/fuentes correspondientes/textos empaquetados. |
| B13.3-b | 6 | APK/versiones/certificados/hashes/informes/CI; no Release final automático. |

## Contratos de implementación

- UI adaptada detrás de rutas activas, sin segundo estado musical. Conservar
  coordinador y timeline; cola visible igual al orden reproducido. Ocurrencias
  duplicadas conservan identidad al editar.
- Android Auto extiende el servicio existente con contratos de biblioteca,
  conservando su nombre y propietario de reproducción.
- SQLiteOpenHelper para nuevos eventos/Replay, sin Room/KSP. Deduplicación y seek
  correctos; conservar totales antiguos sin inventar cronología.
- Letras: un modelo/repositorio temporal activo para efectos y exportación;
  no duplicar búsqueda, parsers ni cachés.
- DSP/salida: capacidades explícitas; bit-perfect excluye mezcla, DSP o velocidad
  incompatibles. Separar datos de archivo, decodificación y salida efectiva.
- AutoMix separa análisis/plan/ejecución. Datos ausentes no producen BPM/voz
  ficticios. Intraop1/2/3 e interop1 no anuncian el total de hilos UI/audio.
- Servicios opcionales con autorización individual y credenciales fuera de APK,
  respaldos y logs. Sin backend compartido ni endpoints legacy reactivados.
- Respaldo/etiquetas: validación, confirmación y recuperación; ninguna sustitución
  silenciosa. Mantener GPL de Aurelis y términos/autoría de cada componente.

## Validación, revisiones y puerta entre entregas

Local: inspección/diffs, fixtures Python y sintaxis estática sin Gradle/Android.
Exclusivamente GitHub: Gradle, compilación, lint, suites Debug/Release, APK y
pruebas Android instrumentadas. Los comandos U/N/D/R históricos se interpretan
solo en GitHub; no usar las antiguas instrucciones de activación local para compilar.

No confundir B00–B13 de desarrollo con fases técnicas B01–B06 de Actions.
Conservar sus gates e informes por SHA exacto; distinguir ejecución/caché/skipped,
fallo/ausencia de prueba y por archivo/variante. Entrada de tarea y presencia DEX
no certifican compilación individual ni conexión de un botón.

Pruebas pertinentes: claro/oscuro, orientación/tamaños/texto grande/accesibilidad,
reducción de efectos, corrupción/revocación/interrupción/recuperación, PCM/cola/
seek/foco/rutas/continuidad. Referencia visual con SHA, datos y medidas comparables;
capturas aisladas no cierran fidelidad. Conservar los siete casos Media3 existentes.
Emulador no acredita sonido físico, bit-perfect, gapless ni DSP nativo correcto.

Revisión1: arquitectura/lógica/integración; revisión2: comportamiento/licencias/
regresiones. Si son del mismo agente, decirlo. Cada cierre registra objetivo,
dependencias, SHA inicial/final, rutas previstas/reales, referencia/procedencia,
pruebas nuevas y existentes/resultados/bloqueos y ambas revisiones.

Publicar cada entrega en el mismo PR, avisar y esperar el resultado de GitHub
comunicado por el usuario antes de la siguiente. Documentación sola requiere
validación documental y referencia al código comprobado; no atribuirle un build
nuevo ni forzar una compilación extra. Ausencia de hardware, credenciales autorizadas
o procedencia bloquea la capacidad afectada y su cierre, sin éxito ficticio.

## Primera entrega: manifiesto y alcance

Presupuesto conservador: 6, aun siendo exclusivamente documental. Rutas previstas:

1. docs/migration/beta-local/PLAN_CONTINUATION.md
2. docs/migration/beta-local/MASTER_PLAN.md
3. docs/migration/beta-local/IMPLEMENTATION_BLOCKS.md
4. docs/migration/beta-local/PROGRESS_TRACKER.md
5. docs/migration/beta-local/DECISIONS.md
6. docs/migration/beta-local/FILE_MAPPING.md

Únicamente registro, enlaces, reglas y selección; fuentes, tests Android, Gradle,
workflows, firma y licencias intactos. PLAN_30_FILES conserva sus bytes. Registrar
las revisiones y resultados concretos en PROGRESS_TRACKER. La siguiente entrega
es la revisión de las tres carpetas, no la implementación de B09.
