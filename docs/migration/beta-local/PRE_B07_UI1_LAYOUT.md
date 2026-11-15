<!-- Aurelis © 2026. GPL-3.0-or-later -->
# Pre-B07 UI1 — estructura común y verificación de capturas

Autorización posterior del usuario: «Entonces soluciona todo eso», referida a la
auditoría del ZIP Aurelis-BitChord-UI-4. Esta es la primera entrega verificable de
esas correcciones, no el cierre de toda la interfaz ni una nueva implementación B07.
Se conserva la espera del resultado después de enviar cada entrega.

## Evidencia de partida

Aurelis d4fea9137e3f21f28e2e56d65798322bcf5fd64d; BitChord fijo
b690dc146009d4815825fa4a20815ef2e4ee8898. ZIP aportado por el usuario, SHA256
9656cf3ad627b57754778423a580c0a910c94226e7190c18f725f4f70c0a7502,
94 entradas conservadas,36 PNG y36 XML revisados. No se descarga otro ZIP para
repetir esta evidencia. Informe de trabajo externo:
/workspace/aurelis-validation/ui-parity-run4/AUDITORIA_UI.md.

Hallazgos observados: título Biblioteca y24px frente a y328px del origen; Buscar
sin reserva superior; barras ocultas en páginas locales; pestañas que suben198px
cuando se añade el MiniPlayer debajo. Cuatro flags MiniPlayer incorrectos y dos
Settings del origen falsamente no alcanzados. El panel completo/letras no se
abrieron en aquel run. Su ausencia en las imágenes no prueba ausencia de código.
API confirma run43685163410 SUCCESS y comparación43685163414 FAILURE del SHA base.

## Cambios de esta entrega

- Se conecta FrostedTopBar a las pestañas y se completan sus acciones/accesorio y
  botón Atrás. El acceso de perfil abre los ajustes del único perfil local; usa su
  foto si existe. No incorpora cuenta remota ni selector de perfiles.
- Inicio, Explore y Biblioteca reciben topBarContentPadding(), con inset y altura
  del componente existente. Buscar coloca campo/perfil en la cabecera, solicita
  foco al pulsar su pestaña y conserva la consulta guardada de esa pestaña.
- AppNavigationHost mantiene el chrome global en páginas apiladas. Las rutas
  reservan su altura medida y consumen el inset inferior para no sumarlo otra vez.
  Detrás del player/cola se conserva la composición para el dock, pero se retiran
  sus acciones de accesibilidad y se bloquean toques que atravesarían el panel.
- MiniPlayer queda encima de las pestañas; añadir/quitar canción no cambia el
  anclaje inferior de estas. La altura del conjunto, barra del sistema y teclado
  alimentan la reserva de contenido; se retira el valor fijo180dp de las pestañas.
- Salir de Buscar por pestañas/perfil o abrir el player retira el teclado. El enlace
  Ajustes de Biblioteca usa Settings, manteniendo temporalmente ese acceso además
  del botón de perfil.
- La captura reinicia solo los datos desechables de cada app/tema y repone permisos;
  fija Liquid Glass=true y locale en en mediante API del emulador. Conserva la
  diferencia autorizada de fuente y no cambia preferencias de producción.
- El verificador Settings reconoce la pantalla raíz real. MiniPlayer exige título
  junto a controles de transporte y la sesión activa del paquete correcto, pista
  de fixture y PAUSED. Un tap/una fila de biblioteca ya no acreditan ese estado.
  El inicio usa Play visible, con dos intentos acotados, y comprueba PLAYING antes de
  pausar. XML y sesión se comprueban otra vez al guardar la captura.

No cambia reproducción/servicio, SDK/ABI/firma, dependencias, fuentes, orden del
pipeline B01→B02/B03/B05→B04→B06 ni puertas de CI. La comparación sigue separada y
se activa por el script modificado. Un fallo de captura conserva su fallo real;
`visual_verdict` sigue NOT_REVIEWED hasta examinar las nuevas imágenes.

## Manifiesto y licencias

Quince rutas de esta entrega, incluyendo pruebas y documentación:

1. app/src/main/java/com/music/aurelis/MainActivity.kt
2. app/src/main/java/com/music/aurelis/ui/navigation/AppNavigationHost.kt
3. app/src/main/java/com/music/aurelis/ui/components/FrostedTopBar.kt
4. app/src/main/java/com/music/aurelis/ui/components/GlassNavBar.kt
5. app/src/main/java/com/music/aurelis/ui/components/MiniPlayer.kt
6. app/src/main/java/com/music/aurelis/ui/screens/SearchScreen.kt
7. app/src/test/java/com/music/aurelis/ui/navigation/GlassNavigationTest.kt
8. app/src/test/java/com/music/aurelis/ui/navigation/NavigationIntegrationTest.kt
9. app/src/test/java/com/music/aurelis/ui/navigation/ChromeContractTest.kt
10. .github/scripts/ui_parity.py
11. .github/scripts/test_ui_parity.py
12. NOTICE
13. docs/migration/beta-local/LICENSE_AUDIT.md
14. docs/migration/beta-local/PROGRESS_TRACKER.md
15. docs/migration/beta-local/PRE_B07_UI1_LAYOUT.md

La unión con las16 rutas de la comparación anterior suma26 rutas distintas≤30;
no se reinicia su presupuesto para ocultar las correcciones del capturador.
Procedencia y SHA256 de las fuentes consultadas en LICENSE_AUDIT. Se adapta el
patrón de insets, acciones, persistencia de chrome y orden de barras del origen;
no se copian archivos completos ni se cambian atribuciones/licencias existentes.

## Comprobaciones

Localmente:22 tests Python de comparación aprobados, cinco nuevos; py_compile y
git diff --check. Se contrastan los nuevos selectores con los XML/sesiones reales
de UI-4: rechazan los cuatro falsos MiniPlayers, reconocen ambos Settings del
origen y detectan el MiniPlayer presente en Inicio claro de Aurelis. La sesión del
origen no se confunde con la sesión PLAYING de Aurelis.

Cuatro regresiones Compose/Robolectric nuevas preparadas para Actions: anclaje al
añadir/quitar canción; Biblioteca bajo cabecera y apertura del perfil; reserva y
accesibilidad/toques del chrome en rutas/player; acciones reales de la cabecera.
Se actualiza la expectativa anterior que exigía ocultar pestañas en Settings.
No se ejecutan esas pruebas ni compilación/Lint/Android localmente.

Dos revisiones estáticas del mismo agente: primero consumidores, geometría,
estado/restauración y ausencia de otro motor; después callbacks, insets/teclado,
accesibilidad, datos desechables, procedencia, falsos positivos y presupuesto.
No son revisiones independientes. Resultado Android de este cambio pendiente;
no declarar paridad, funcionamiento físico ni éxito de las pruebas nuevas.

## Continuación autorizada pendiente

| Grupo de la auditoría | Estado tras preparar UI1 |
|---|---|
| UI-01/02/03 | Corregido el contrato básico; validación Android/visual pendiente. Plegado/acoplamiento Liquid Glass exacto aún por contrastar. |
| UI-04/05 | Cabecera local responsive en el mismo scroll, cinco accesos locales, filas/acciones/duración: pendientes. |
| UI-06/07 | Perfil y acceso Settings corregidos; estructura/búsqueda/orden de Ajustes e inventario de glifos: pendientes. |
| UI-08/09 | Cabecera y foco de búsqueda conectados; composición de Inicio y resto de páginas: pendientes. |
| UI-10 | Registro, conexión y comparación de widgets en host: pendientes, identificados con B12.1. |
| QA-01/02/03 | Verificadores/configuración corregidos; reproducción real en nueva captura pendiente. Textos hardcoded no se traducen por fijar locale. |
| Player/letras/carátulas/menús | Obtener pantallas reales y revisar; no cerrar por existencia de clases. |

Completar los grupos por entregas pequeñas después del resultado de UI1. No
reiniciar trabajo funcional ya hecho ni introducir autenticación/streaming,
Android Auto/TV/Wear. No se avanza B09 ni se da por terminada la puerta visual.
