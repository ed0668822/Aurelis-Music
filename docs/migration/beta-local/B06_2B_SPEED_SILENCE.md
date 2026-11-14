<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B06.2-b — velocidad y salto de silencio

Autorizado dentro de «termina B06», base6a27b20e, misma rama/PR1. Máximo30 rutas.
Manifiesto previo (diez): LocalPlaybackCoordinator.kt, PlaybackViewModel.kt,
PlaybackToolsDialog.kt, NowPlayingScreen.kt, PlaybackToolsTest.kt nuevo,
PlayerIntegrationTest.kt, NOTICE, LICENSE_AUDIT.md, PROGRESS_TRACKER.md, este informe.

Velocidad0.5–2 por comando privado al servicio; salto de silencio en el ExoPlayer
existente. Estado publicado desde propiedades reales y capacidades del reproductor.
La ruta float/PCM24/PCM32 no ofrece estos procesamientos: motivo visible y retorno
a1x/sin salto al detectarla. No modificar sink, ABI/SDK o DSP para conseguir efecto.
No afirmar medición de salida/efecto audible por setters; hardware pendiente.
Referencia BitChord b690dc1 PlaybackService.kt: setters reales de velocidad/silencio,
sin copiar infraestructura multi-deck ni colectores globales. Dos revisiones propias.


Implementado en el coordinador del servicio. Parámetros inválidos/NaN/infinito se
rechazan. Ante formato float/PCM24/PCM32 detectado se restablecen1x/sin salto, ya que
el sink actual usa salida float y no su cadena Sonic/silencio. Sin formato conocido
se acepta configuración nativa; esto no acredita procesamiento audible. No existe
modo bit-perfect activo en B06; no inventarlo ni iniciarlo aquí. Preferencias
pertenecen a la sesión actual; al reiniciarse vuelve configuración por defecto.

Cuatro regresiones nuevas preparadas: API real ExoPlayer sin prepare/seek, errores
sin mutación, matriz de PCM y callbacks UI disponibles; el caso previo de control
indisponible se conserva y extiende. Dos revisiones estáticas del mismo agente:
capacidad/restricción/ciclo de vida; después API/imports/UI/fixtures/manifiesto10.
Pasa diff --check. javap verifica getters/eventos existentes, no compila. Solo Actions
compila/ejecuta; escucha física y formatos/rutas de dispositivos pendientes.
