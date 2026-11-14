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


## CI136 — Lint y espera de pruebas

Run43643160015 (6a27b20e) compiló B01 y pasó B05. Lint Debug y Release fallaron por
WrongConstant en LocalPlaybackCoordinator.kt:33: constructor SessionResult con alias
SessionResult.RESULT_ERROR_BAD_VALUE, pero el contrato exige SessionError.ERROR_BAD_VALUE.
Se usa esa constante explícita, sin supresión ni cambio del código de error.

B03 compiló pruebas Debug correctamente y quedó en testDebugUnitTest desde21:37:42Z
hasta cancelar a22:08:06Z (unos30 minutos). No había progreso por nombre de test;
no se puede atribuir el atasco a una prueba concreta con ese log. Release no comenzó.
Se canceló el run ya no aprobable por Lint; el ZIP oficial de logs se descargó y
verificó. Cancelación/ausencia de resultados no equivale a aprobación.

Corrección de ciclo de vida confirmada estáticamente: se elimina el sondeo periódico
indefinido de controles del ViewModel. PlayerConnection comunica cambios de extras/
comandos; al conectar se lee el estado y los eventos de Player también sincronizan.
Solo el servicio con timer activo publica ticks. No se afirma todavía que el sondeo
sea la causa única del atasco de CI: debe validarse con la ejecución siguiente.
Prueba de sesión extendida verifica notificación de cancelación desde servicio sin
RPC de UI; regresión adicional exige que una cola pausada deje inactivo el scheduler.

Diagnóstico CI: script b03-test-diagnostics.gradle registra START/END por prueba.
Timeout GNU de12min por variante, QUIT para diagnóstico JVM y KILL tras30s si no
termina. Timeout produce fallo real; se mantienen suites, asserts y puertas de
cobertura/resultado. No se omite Release por fallo de Debug. Sin Android local.

Rutas adicionales dentro del mismo presupuesto: PlayerConnection.kt,
PlaybackIntegrationTest.kt, .github/scripts/b03-test-diagnostics.gradle,
.github/workflows/b03-unit-tests.yml. El presupuesto acumulado A pasa a18 rutas
(no reinicia al corregir). Se incluyen además informe/tracker ya existentes.
Los commits locales autorizados B06.2-b b0fc7710 y B06.2-c5ef41907 permanecen en
historial, sin rebase ni reescritura. Próximo push lleva ambos y esta corrección;
contar también la unión completa de rutas desdec6ad3dd2 contra máximo30.
Dos revisiones del mismo agente: log/intdef/ciclo de vida; después callbacks,
regresiones, YAML/Bash y diff --check. Nuevo CI y efecto físico pendientes.


Unión comprobada de B06.2-a/-b/-c y todas sus correcciones:24 rutas distintas desde
c6ad3dd2, dentro del máximo30 incluso contando todo como una entrega acumulada.
La fixture Autoplay preparada también bombea los eventos Media3 del main Looper,
como las pruebas de integración existentes, además de su scheduler de coroutines.
Sin relajar expectativas. YAML parseado y scripts de Debug/Release pasan bash -n;
no se ejecutan los comandos Gradle localmente. No atribuir el atasco a una prueba
concreta hasta que el log de progreso de la siguiente ejecución lo identifique.
