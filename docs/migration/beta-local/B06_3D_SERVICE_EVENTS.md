<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B06.3-d — eventos medidos desde el servicio

Base e17c6db474fb0754d60caa95e8b05b5d2c158081 confirmada por «Continúa». Sin ZIP previo.
Misma rama/PR1, máximo 30 archivos distintos por entrega. Manifiesto previo (18):
data/stats/ListeningEvent.kt, ListeningEventStore.kt, ListeningEventJournal.kt nuevo,
ListeningEventWriter.kt nuevo; playback/ServicePlaybackRecorder.kt,
ProfilePlaybackBinding.kt, LocalPlaybackCoordinator.kt, PlaybackViewModel.kt;
ui/components/PlaybackToolsDialog.kt; test/playback/ServiceListeningEventsTest.kt nuevo,
test/data/ListeningEventWriterTest.kt nuevo, ListeningEventJournalTest.kt nuevo;
test/playback/ProfilePlaybackTest.kt; test/ui/PlayerIntegrationTest.kt; NOTICE,
LICENSE_AUDIT.md, PROGRESS_TRACKER.md y este informe.

Un recorder en el servicio: reloj monotónico mientras isPlaying, sin contar distancia
de seek, pausa/buffering ni intervalos inciertos>5s. Sesión nueva por transición/repeat,
ocurrencia conservada cuando existe UUID. UTC observado al capturar, no al reintentar.
Legacy sigue su ruta única actual; SQLite solo recibe segmentos nuevos.

Escritor IO con cola acotada y diario AtomicFile privado por perfil. Diario antes de
SQLite, confirmación después del commit; replay con mismas claves y payload. Cierre
no bloquea main y drena lo aceptado; ningún worker retiene Player/Activity/servicio.
Mutex por ruta serializa cierre/reapertura del diario en el mismo proceso. Futuro/
corrupto no se sobrescribe; error de historial no interrumpe audio. Límite/backpressure
explícito, no se inventan intervalos perdidos. Fuente UI única vía extras de sesión.
Dos revisiones propias, Android/SQLite solo Actions. B06.4 sigue pendiente.

## Evidencia y límites

Diecinueve casos nuevos, preparados para Actions:
- Escritor (11): cierre y drenaje, backoff IO, commit incierto/ack fallida, diario
  futuro/corrupto, límites de entrada y pendientes, cierre/reapertura concurrentes,
  perfil de solo lectura, checkpoint fallido, conflicto e intervalo inválido.
- Diario/SQLite nativo (4): enteros de64 bits/ruta privada, backup AtomicFile,
  formatos desconocidos/UTF-8 inválido preservados y replay real sin duplicación
  después de COMMIT anterior a la confirmación del diario.
- Servicio (4): seek/pausa/buffering/foco; ocurrencias duplicadas y repeat-one;
  cambio UTC/gap incierto; binding real, drenaje a SQLite y restore pausado.
La prueba Compose existente de herramientas comprueba el aviso de pérdida.
Las pruebas anteriores del binding esperan su drenaje antes de cerrar Robolectric.

No se ejecutan Gradle/Lint/Android localmente en esta entrega. Estas pruebas son
código preparado, no resultados aprobados; Actions y el dispositivo siguen pendientes.
No hay promesa de completar intervalos que el proceso no alcanzó a medir. Una muerte
abrupta antes del checkpoint puede perder lo aceptado solo en RAM; el diario recupera
lo ya persistido. Saturación o fallo de persistencia al cierre se contabilizan como
pérdida explícita durante esa instancia; ese contador diagnóstico no sobrevive al
proceso. Gaps mayores de5s se omiten como inciertos. Un error duro/corrupción/versión
futura requiere reapertura y diagnóstico; no se borra ni se sustituye el archivo.
El cierre solicita drenaje sin bloquear main, pero Android puede terminar el proceso
antes de que finalice. No se vuelve a empaquetar ni entregar un APK local.

## Revisiones

Primera revisión estática: tipos, callbacks Media3, referencia a ocurrencia/sesión,
una sola ruta legacy, separación main/IO y ciclos de vida. Se mantiene el reloj como
último parámetro del recorder para conservar las pruebas anteriores con lambda final.
Segunda revisión estática: transacciones/journal, protección de versiones, errores,
backpressure, cierre/reapertura serializados, cancelación y referencias retenidas;
revisión de expectativas de las pruebas, manifiesto y atribuciones.
Ambas revisiones realizadas por el mismo agente; no son revisiones independientes.
Comprobación local: git diff --check, recuento de rutas y cabeceras propias.
No cambios en Gradle, workflows, SDK, ABI, firma, secretos ni RELEASE.
