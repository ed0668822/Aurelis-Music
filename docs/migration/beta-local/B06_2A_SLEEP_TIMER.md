<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B06.2-a — temporizador del servicio

Base c6ad3dd2905a62c7516aed68e96401e3cb66f8f0. El usuario confirma mediante
«Continúa con el siguiente bloque y termina B06» y autoriza también los restantes
subbloques B06 en su orden; no inicia B07/B09. Máximo30 rutas distintas por entrega.

Manifiesto previo: SleepTimer.kt, LocalPlaybackCoordinator.kt nuevo,
AurelisPlaybackService.kt, ShuffleQueueEdits.kt, PlaybackViewModel.kt,
ui/components/PlaybackToolsDialog.kt nuevo, ui/screens/NowPlayingScreen.kt,
test/playback/SleepTimerTest.kt nuevo, NOTICE, LICENSE_AUDIT.md, PROGRESS_TRACKER.md
strings.xml, test/ui/PlayerIntegrationTest.kt y este documento (14). Kotlin bajo com/music/aurelis, docs bajo migration/beta-local.

Aceptación: deadline monotónico, cancelar/rearmar sin callbacks obsoletos, pausa
única al vencer o en transición automática/ENDED; cambio manual cancela after-track.
Servicio conserva propietario al cerrar UI, controlador solo envía comandos propios.
Presets15/30/45/60 y final de pista; estado real por sessionExtras. No alarma exacta,
no reinicio de reproducción ni otro servicio. Probar en Actions, no Android local.
Referencia BitChord fija b690dc1 playback/SleepTimer.kt: deadline/afterTrack, adaptados
a un objeto propiedad del servicio, sin estado global de UI. Dos revisiones propias.


Implementado con deadline elapsedRealtime y revisiones cada segundo: tras suspensión
del proceso/dispositivo pausa al poder volver a ejecutar, no promete alarma exacta.
No persiste al morir el servicio; nueva sesión empieza desarmada. After-track
pausa también repeat-one, no confunde seek manual con final natural. Cinco pruebas
nuevas preparadas para Actions, incluida sesión real y cierre de controlador.
Dos revisiones estáticas del mismo agente: primero ciclo de vida/deadline/eventos;
después comandos/capacidades/UI/imports y catorce rutas. diff --check y XML válido.
Gradle/Lint/tests Android solo Actions; validación física pendiente.

Se usa pauseAtEndOfMediaItems de ExoPlayer para detener antes del siguiente medio;
se restaura su valor anterior al cancelar. Firma inspeccionada sin compilar.
Prueba UI adicional de control indisponible; puente de sesión cubre también VM/cancel.
