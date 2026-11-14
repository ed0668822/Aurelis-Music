<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B06.2-c — continuación automática local

Autorizado por «termina B06». Baseb0fc7710 local; rama única/PR1. Máximo30 rutas.
Manifiesto previo: Autoplay.kt, LocalAutoplayBinding.kt nuevo,
LocalPlaybackCoordinator.kt, AurelisPlaybackService.kt, PlaybackViewModel.kt,
PlaybackToolsDialog.kt, NowPlayingScreen.kt, LocalAutoplayTest.kt nuevo,
PlayerIntegrationTest.kt, NOTICE, LICENSE_AUDIT.md, PROGRESS_TRACKER.md, este informe13.

Solo al terminar una cola con playWhenReady true y repeatOFF, si autoplay está activo.
Catálogo MediaStore permitido; sin streaming, modelos ni similares ficticios. Orden
local determinista por artista/álbum/género e ID; excluye toda canción ya en cola.
Carga fuera de audio y valida de nuevo cola/ocurrencia/pausa/opción antes de aplicar.
Cancelar al pausar/cambiar cola/desactivar/cerrar. Nunca arranca una restauración
pausada. Marcado AUTOPLAY y nuevas identidades; extensión limitada a diez canciones.
No repetir auditorías o parsers; no iniciar B09/AutoMix. Pruebas solo en Actions.


Implementación propia, sin copiar motor remoto de BitChord. Estado/preferencia se
publican en la misma sesión y el selector de controles ofrece habilitar/deshabilitar.
El perfil de solo lectura rechaza cambios de opción. Error/vacío se informa sin
simular canciones; no se reintenta continuamente la misma cola agotada. Las URI que
no sean content/file se excluyen. El temporizador y la pausa prevalecen sobre autoplay.

Cinco nuevas regresiones para Actions: selección/ranking/exclusión/límite local;
final natural con IDs nuevos/primer tema; pausa/restore/repeat/opción desactivada;
respuesta tardía no cooperativa tras reemplazo; catálogo vacío/error sin bucle.
UI existente extendida para el callback. Dos revisiones propias: condiciones y
cancelación/permisos; después cola/índices/imports/fixtures y manifiesto13.
git diff --check correcto. Sin Gradle/Android locales. CI y escucha real pendientes.
