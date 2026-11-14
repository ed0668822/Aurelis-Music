<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B05.2-a — cancelación del gesto de cabecera

Autorización: «Continúa con los dos siguientes bloques», base 48b72fae22b5478f3ddfdb9270c94326fed391bd.
Misma rama/PR1. Resultado anterior aceptado por el usuario; sin descargar informes.
Máximo vigente 30 archivos acumulados; manifiesto de seis rutas:
- app/src/main/java/com/music/aurelis/ui/player/PlayerSwipeTrigger.kt
- app/src/test/java/com/music/aurelis/ui/PlayerHandleGestureTest.kt
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/B05_2A_HANDLE.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

Se conserva el panel/dock ya implementado. El detector vertical notificaba
cancelación de gesto, pero no garantizaba cancelar el pull al retirarse/reiniciarse
pointerInput durante un arrastre. Ahora try/finally cancela ese pull; los caminos
normales de cancelación/liberación desactivan accepted para evitar duplicados.
No se modifican umbrales, velocidad, resorte, geometría, rutas ni servicio.
Es una corrección propia Aurelis sobre el contrato MiniPlayerPull existente;
no se vuelve a portar PlayerSheetMotion/PlayerDock ni se reclama paridad completa.

Dos regresiones para Actions: retirada durante arrastre y reapertura/liberación;
cancelación explícita seguida de retirada sin repetir cancel. Rule ui correcta.
Dos revisiones del mismo agente: ciclo del detector/cancelación y diff completo,
callbacks, imports, atribuciones y presupuesto. git diff --check local.
Sin Gradle/Lint/tests Android locales. Validación nueva pendiente de Actions;
OEM, multitouch, GPU y fidelidad física siguen pendientes. Segundo bloque autorizado:
B05.2-b, independiente de este cambio, se prepara antes del envío conjunto.
