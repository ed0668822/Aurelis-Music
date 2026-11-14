<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B06.3-a — restauración de cola y concurrencia

Base869ffe6a9182a2cd3161383e4ecda22cfc48aa83, confirmada por «Continúa». Sin descargar
ZIP previo. Autorización vigente terminar B06, rama única/PR1, máximo30 rutas.

Manifiesto previo (12): data/LocalQueueStore.kt, playback/ProfilePlaybackBinding.kt,
ServicePlaybackRecorder.kt, LocalPlaybackCoordinator.kt, AurelisPlaybackService.kt,
ShuffleQueueEdits.kt, test/data/LocalQueueSnapshotTest.kt nuevo,
test/playback/ProfilePlaybackTest.kt, NOTICE, LICENSE_AUDIT.md, PROGRESS_TRACKER.md,
este documento. Kotlin bajo com/music/aurelis; documentación migration/beta-local.

Snapshot v2 conserva IDs de ocurrencia, tiers y permutación nativa, sin URI/rutas,
carátulas ni intención de autoarranque. Lee v1; rechaza futuro/malformado sin sobrescribir.
Ausentes se filtran por ID local, remapeando índices/permutación. Restaurar pausado;
ninguna carga tardía sobrescribe acciones ya solicitadas por UI/controlador.
Vaciado explícito persiste vacío; arranque sin permiso/cierre durante carga no borra
snapshot recuperable. Propietario coordinado en el servicio existente. Dos revisiones
propias y pruebas preparadas para Actions, nada Android local. No inicia B07/B09.


## Implementación y límites

queue_v2 precede a queue_v1 sin borrar este último. Tipos JSON estrictos: no coerción
de decimal/string a índice o booleano, ni restauración parcial con datos malformados.
Entry IDs deben ser únicos cuando existen; tiers/permutación deben tener forma válida.
Snapshots v1 no pueden recuperar UUID/permutación que nunca guardaron; se generan
identidades nuevas y se usa shuffle nativo. Datos futuros/corruptos quedan en modo
solo lectura, intactos. No serializar canciones completas ni intención de reproducir.

Al resolver, se conserva el orden físico y la identidad de cada duplicado; se retiran
solo IDs inexistentes y se remapea su permutación. Si falta el actual, se elige el
siguiente superviviente físico (o último disponible) y posición0. Si existe, posición
se limita a duración conocida; si duración es desconocida se conserva el dato guardado.
ExoPlayer recibe su ShuffleOrder antes de habilitar shuffle; no prepare/play.

El coordinador del servicio pasa a poseer ProfilePlaybackBinding y su cierre.
Callback de comandos mutantes cancela restore antes de ejecutarlos, incluso cuando
el Player vacío no emite evento (p. ej. cambiar repeat). También vigila cambios reales
de timeline/posición/arranque. Solo una edición real de timeline habilita guardar vacío;
pausa/cancelación/cierre/catálogo indisponible conservan snapshot recuperable si no se
cargó ninguna cola. B06.3-c/-d siguen pendientes para eventos SQLite; no se cierran aquí.

## Validación

Doce pruebas nuevas preparadas para Actions: codec v2 con duplicados, faltantes,
lectura/migración v1 preservando bytes, corrupto/futuro sin sobrescribir y vacío con
prioridad sobre antiguo; ExoPlayer restituye IDs/permutación/posición pausado, vaciar
y reiniciar, cancelar carga no cooperativa sin editar, reemplazo mientras carga,
actual ausente/catálogo vacío y comando real MediaController repeat durante carga y medio externo sin ID local que
no debe provocar crash ni sobrescribir el snapshot recuperable.
Se conservan las dos pruebas previas de binding y todas las de colecciones/perfil.

Dos revisiones del mismo agente: primero concurrencia/datos/índices/vaciado/perfil;
después firmas Media3/constructor Recorder con lambda clock existente/call sites,
fixtures, permisos y doce rutas. git diff --check correcto. Sin Gradle, Lint, APK ni
pruebas Android locales. CI nuevo y pruebas físicas pendientes; no afirmar B06 cerrado.
