# B02.3 — gestos y componentes compartidos

## Alcance y conexiones

IosSwitch se adapta del archivo real BitChord b690dc1, conservando las proporciones
64x28/40x24dp, colores, presión, RTL y haptics. NOTICE registra origen y SHA original
1931946478c7c4e0f81cac1f3cb89a3070d38d71366ac446bf327291a2eed9c5.
GPLv3 según LICENSE/README upstream; no se atribuye el código original a Aurelis
ni se deduce permiso «or later» del código de terceros. LICENSE se conserva.
Adaptaciones: paquetes, reducción de movimiento (snap/sin stretch), y contenedor
interactivo explícito de48dp que mantiene el dibujo a28dp. En SettingsSubRow el
control es lectura y la fila posee una única acción/rol Switch. SettingsScreen
usa esa fila; las preferencias persistentes y el tema existentes siguen activos.

HeldContextMenu deja de ser un Box sin función y usa IosMenu. SongRow conecta el
gesto compartido combinedClickable con acción accesible de pulsación larga.
SongActionsDialog abre el panel real: favoritos, playlists existentes/creación y
cola siguen usando LocalUserLibrary y PlaybackViewModel; no son acciones ficticias.
El panel es enfocable, acotado al tamaño de pantalla y desplazable. Back, fuera y
Cerrar notifican al propietario; respeta movimiento reducido y comparte cristal.
El proveedor del fondo abarca también el menú sin incluirlo en la captura fuente.

## Límites de paridad y bloques siguientes

Este bloque establece componentes e interacciones, no certifica fidelidad completa.
El panel actual es centrado: posicionamiento por ancla, snapshot levantado del ítem
y animación de retorno de HeldItem upstream no están adaptados todavía. Deben
integrarse con los consumidores de Home/Biblioteca en B03/B04 y con los menús del
reproductor en B05. No presentar este panel como un port completo del HeldContextMenu
original (el archivo upstream y sus dependencias siguen en referencia). No se
ha simplificado ni retirado el código upstream de esos bloques futuros.

## Revisiones

Revisión1 (mismo agente): estado real, ownership único del gesto, solo una acción
por fila, readOnly, bounds/scroll/foco, motion/RTL y atribuciones. Detectada y
corregida altura semántica28dp del original:48dp sin agrandar el dibujo.
Revisión2 separada: tests de tocar/mantener, favorito persistente, playlist real,
cola/cierre, Back y fuera sobre PopupLayout real, texto grande, deshabilitado y
capturas claro/oscuro/grande existentes. Se inspeccionó dark-large.png del árbol
real; no equivale a captura ejecutada de BitChord ni prueba GPU/dispositivo.
Resultados finales en B02_3_EVIDENCE; no se prueban TalkBack/fps/OEM en hardware.
