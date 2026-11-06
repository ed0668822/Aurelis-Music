# B03.1 — host y navegación local

Autorización vigente: «Mejor continúa con el plan ... pero si prueba todo lo que
puedas», interpretada como siguiente bloque principal B03; QA02 no iniciado.
Base b8b8910, mismo PR1. F04 y componentes F03 existentes se reutilizan.

AppUi se instala en Application después de crear la biblioteca persistente del
perfil único y antes del árbol Compose. El contrato no utilizado de descarga/
pinnedPlaylists/swipe se sustituye por el StateFlow real de bibliotecas: no había
consumidores ni implementación funcional de esos miembros. Main observa ese host;
no se agregan preferencias falsas ni funciones de descarga/streaming.

La barra conserva rutas/etiquetas/accesibilidad y MiniPlayer actuales. Activa la
píldora, lentes, refracción y movimiento GlassPillMotion ya portados (MIT Ahmed
Gamil; avisos preservados): tabRow y superficie exportada se capturan por separado,
sin incluir la propia píldora en su fuente. Reduce motion hace snap, RTL invierte
posiciones y software/no-capacidad conserva selección plana. No elimina Glass.

Revisión1: bootstrap/estado único, semántica de tabs, medidas/RTL, captura sin
recursión y conservación de QA01. Revisión2 separada del mismo agente:14 casos
seleccionados correctos en33s; navegación/back/restauración, host persistente,
selección accesible y árbol con fondo real más regresiones onAttach de QA01.
Kotlin/debug compilado. APK/release/lint y suite completa en cierre B03; GPU/fps/
Android real pendientes. No afirmar paridad pixel a pixel ni pruebas de dispositivo.
