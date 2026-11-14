<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B03.3-a — estados accesibles y recuperación local

## Autorización y manifiesto previo

Autorización «Continúa con el siguiente bloque», confirmando éxito de la entrega
anterior. Base 14e4567c7b4746f08cf4d098f3ce540f226a45f9, misma rama
feature/bitchord-local-migration y PR1. No se descarga ZIP del bloque anterior:
regla del usuario, «continúa» confirma éxito; informes solo al comunicar un fallo.
Referencia BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 local y solo lectura.
Dificultad media; máximo acumulado 30 archivos distintos.

Manifiesto previsto (siete archivos, contando correcciones posteriores):
- app/src/main/java/com/music/aurelis/ui/screens/HomeScreen.kt
- app/src/main/java/com/music/aurelis/MainActivity.kt
- app/src/test/java/com/music/aurelis/ui/HomeStateRecoveryTest.kt
- NOTICE
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/B03_3A_RECOVERY.md
- docs/migration/beta-local/PROGRESS_TRACKER.md

## Brechas verificadas y enfoque

La rama de carga/error/vacío de Home era una Column sin scroll: mensajes largos,
pantallas bajas o texto grande pueden dejar inaccesible el botón de recuperación.
Usaba además un padding superior distinto del Home poblado. Se adapta el patrón
upstream HomeScreen (cabecera y mensaje dentro de LazyColumn; UiState.Error →
MessageState con acción real), conservando textos/estados locales y sin skeletons
que simulen contenido remoto. Se reutiliza MessageState ya existente en Aurelis.

LibraryViewModel ya distingue isRefreshing y conserva el Success durante refresh.
Home no recibía ese flag: biblioteca vacía podía mostrarse vacía mientras escaneaba,
y Home poblado no indicaba el escaneo en curso. Se conecta el estado existente;
no se reescribe el escáner ni la política de cancelación/observadores/permisos.

Fuentes exactas consultadas, bajo sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord:
- ui/screens/HomeScreen.kt, SHA256
  d6f6a11a322eb968823fd6abd861f3e8ebff381e71e629a2a77a55d38d66e017.
- ui/components/Common.kt (MessageState), SHA256
  9e6847c462b52f4c94a53b24a297fe9aba1fa5a0f29accde2746e7a6c075b04e.

## Implementación y contratos

HomeScreen añade isRefreshing=false al final de sus parámetros para mantener
compatibilidad con consumidores existentes. MainActivity conecta el flag real
de LibraryUiState. Biblioteca vacía durante refresh muestra carga sin ofrecer
otra acción de escaneo; Home poblado conserva canciones y muestra progreso.
No se inventan tiempos de espera, porcentajes ni resultados. El ViewModel existente
controla éxito, cancelación, fallos, pérdida de permisos y lecturas pendientes.

Carga, error, permiso y vacío están en una LazyColumn con scroll independiente
del feed poblado; comparten el ajuste superior de 28dp y el padding inferior
real del host. La acción sigue al mensaje sin límite fijo de altura, por lo que
puede alcanzarse desplazando. MessageState existente proporciona texto y botón;
los cambios se anuncian como live region polite. No se añade captura automática
de errores ni se alteran permisos, escáner, archivos, reproducción o datos privados.
El callback existente distingue pedir permiso/abrir Ajustes y el ciclo ON_RESUME
ya vuelve a consultar el catálogo al retornar; no se reimplementa ese flujo.

## Pruebas y revisiones

Tres pruebas nuevas en HomeStateRecoveryTest, preparadas para Actions:
1. Error largo y acción real de reintento accesible en pantalla 640x360dp, texto
   1,5x y ambos temas; al recuperar el estado vacío desaparece el error previo.
2. Mensaje largo de permiso con acción accesible; el callback de reintento no se
   invoca en lugar del de permiso. No es prueba del diálogo Android físico.
3. ViewModel real con LocalLibrarySource y lecturas suspendidas/controladas:
   vacío → refresh/carga → canciones reales → refresh conservando contenido →
   vacío, sin botón duplicado de refresh ni canción/progreso obsoletos al terminar.

Se conservan las suites existentes de permisos/revocación/errores/cancelación de
LibraryViewModel, LibraryUpdates y HomeShelvesIntegration. No se repite aquella
entrega ni se añaden datos ficticios al producto; fixtures solo en src/test.

Revisión 1 del mismo agente: árbol/scroll/espaciado de estados, fuente del flag,
consumidores, callbacks y ausencia de alteraciones de escáner/reproducción.
Revisión 2 del mismo agente: diff completo, tipos/imports, regla Robolectric en
su paquete real ui, scopes de coroutines, atribuciones y manifiesto de siete
archivos. git diff --check local; sin Gradle/Lint/APK/tests Android locales.
B01–B06 y las tres pruebas nuevas quedan pendientes del SHA publicado: no se
certifica ejecución, paridad visual ni permisos/dispositivo físico por revisión
estática. No se inicia B04/B05/CI12.3/B09 ni otro bloque.

Tras enviar el commit, avisar al usuario. Su «continúa» confirma resultado bueno
y autoriza empezar el siguiente bloque sin descargar ZIP; ante fallo comunicado,
investigar los informes reales antes de corregir. No crear Release oficial.
