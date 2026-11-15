<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B06.3-c — almacenamiento versionado de eventos

Basef7f3dd46bf6a9d5f3da2a493d1f3326f7e5b1616, confirmada mediante «Continúa».
Misma rama/PR1, sin descargar ZIP anterior. Máximo30 rutas distintas por entrega.
Manifiesto previo (9): data/stats/ListeningEvent.kt, ListeningDatabase.kt,
ListeningEventStore.kt nuevos; test/data/ListeningEventStoreTest.kt y
ListeningDatabaseRecoveryTest.kt nuevos; NOTICE, LICENSE_AUDIT.md, PROGRESS_TRACKER.md,
este documento. Kotlin bajo com/music/aurelis; docs migration/beta-local.

SQLiteOpenHelper nativo (sin Room/KSP/dependencias), base privada por ProfileId.
Eventos nuevos: sesión/secuencia/ocurrencia/canción, timestamp observado UTC, intervalo
monotónico escuchado y duración conocida. No convertir totales legacy a fechas.
Clave sesión+secuencia, duplicado exacto idempotente, payload/identidad conflictivos
rechazados atómicamente. IO fuera de main/audio, consultas temporales paginadas.
Base futura/corrupta no se borra ni reinicializa. Escritura en perfil solo lectura
rechazada; constructor/consulta vacía no crean base. Integración del recorder queda
explícitamente para B06.3-d; no declarar registro activo por presencia de estas clases.
Dos revisiones del mismo agente; pruebas Android solo en Actions.


## Contrato del almacén

Base de esquema1 en aurelis-listening-<ProfileId>.db, contexto applicationContext;
ProfileId existente valida nombres, sin nuevo selector multiusuario. Sesiones fijan
songId/queueEntryId; intervalos llevan sequence, observedAtMs, listenedMs y durationMs
(0 desconocida). Solo se admiten intervalos1–5000ms, IDs no vacíos/acotados y enteros
no negativos. El almacén valida datos; no demuestra que hubo audio, no mide seek ni
crea fechas: B06.3-d deberá proporcionar intervalos monotónicos medidos desde servicio.

Una transacción contiene todo el batch. Repetir la misma clave/payload no duplica;
cambiar payload o identidad de sesión rechaza y revierte el batch completo. No usa
CONFLICT_REPLACE ni convierte fallos de inserción en duplicados. Mutex por instancia y
transacciones SQLite; resultados distinguen conflicto, datos/consulta inválidos,
solo lectura, cerrado, esquema futuro, corrupción e IO. Cancelación se propaga, no se
certifica escritura fallida; reintentar el mismo payload es idempotente si el commit
ya ocurrió. Se copia el batch antes de suspender para evitar mutación del llamador.

Constructor y lecturas sin fichero no crean base. Escrituras respetan perfil activo
y modo solo lectura; lecturas de datos existentes siguen disponibles. Abrir/consultar/
escribir/cerrar SQLite se despacha a IO; cierre serializado y no cancelable. WAL se
activa solo tras verificar versión. onDowngrade rechaza futuro, onUpgrade no destruye;
DatabaseErrorHandler propio impide borrado automático ante corrupción. Futuro/corrupto
bloquean esa instancia hasta cerrar/reabrir; IO transitorio es fallo explícito, no
colección vacía ni éxito inventado. Esta entrega no implementa reparación/importación.

Consultas [desde,hasta) con límite1–1000 y cursor (timestamp,sesión,secuencia),
ordenadas igual que el predicado, sin perder eventos con fecha igual. Hasta null
significa sin límite superior; se conservan Long completos. Totales por canción solo
suman los eventos de esa ventana: no son play counts ni mezclan totales antiguos.
Inputs SQL siempre ligados; se comprueban tipos/valores de filas al decodificar.
No se guardan rutas, carátulas, letras, credenciales o objetos Player.

## Validación y dos revisiones

Diez regresiones nuevas preparadas para Actions con SQLite nativo de Robolectric:
1. Historial agregado previo intacto, cero eventos fabricados y lectura sin crear DB.
2. Duplicado exacto tras reabrir, Long.MAX_VALUE sin truncamiento y rango semiabierto.
3. Conflicto revierte batch completo y no cambia identidad de sesión existente.
4. Paginación en timestamps iguales y totales de ventana exactos.
5. Doce reintentos concurrentes insertan una vez y operan fuera de main.
6. Intervalo de seek/inválido y consulta inválida no crean datos ni fichero.
7. Perfiles aislados; solo lectura/cerrado no permiten mutaciones.
8. Esquema99 conserva versión y payload al rechazar lecturas/escrituras.
9. Fichero corrupto no se elimina/recrea vacío (se conserva contenido original).
10. Ruta no abrible se reporta como IO, no como historial vacío válido.

Dos revisiones del mismo agente: primero atomicidad/dedupe/perfil/futuro/corrupción;
después SQL ligado, tipos/cursores/cancelación/IO/límites/fixtures y nueve rutas.
Pasa git diff --check. Sin compilar ni ejecutar Gradle, Lint, APK o Android local.
Pruebas SQLite son Android/Robolectric y se ejecutarán exclusivamente en Actions.
No equivalen a prueba física ni a integración del recorder. B06.3-d/B06.4 pendientes.
