<!-- Aurelis © 2026. GPL-3.0-or-later -->
# Puerta de fidelidad de interfaz antes de B07

Solicitud: misma interfaz de BitChord adaptada a música local, incluidos iconos,
letras, carátulas, widgets y ubicación/orden de ajustes. Nueva prioridad anterior
al siguiente B07; no declarar equivalencia visual por compilación o archivos.

Referencia local verificada: /workspace/BitChord,
b690dc146009d4815825fa4a20815ef2e4ee8898. Aurelis base b8ee8147, con cambios locales
posteriores de exclusión de Android Auto. Sin copiar nuevos archivos en esta revisión.

## Hallazgos iniciales confirmados por lectura

1. MainActivity.kt:332 usa AurelisIcons.Home para el acceso etiquetado Ajustes;
   AurelisIcons.kt también define Settings. MainActivity.kt:407 abre SettingsScreen.
   Hay que contrastar acceso/posición/iconografía con el flujo de referencia y
   corregir el acceso activo; no basta cambiar un componente que nadie consume.
2. ui/screens/SettingsScreen.kt activo tiene grupos locales simplificados:
   Audio local, Apariencia, Letras, Biblioteca, etc. El SettingsSheet.kt del origen
   usa búsqueda y grupos de reproducción/apariencia/rendimiento/música local/datos,
   entre otros. No coincide la organización. Adaptar solo opciones compatibles,
   conservando su orden relativo; no copiar streaming/descargas/cuentas remotas.
3. Existen widget/MediaWidget*.kt locales, pero AndroidManifest.xml no contiene
   receptores MediaWidget ni APPWIDGET. No presentar widgets como disponibles por
   la presencia de esas clases. Registro, recursos y conexión son trabajo pendiente.

Estos tres hallazgos no constituyen una auditoría exhaustiva de todas las pantallas.
No se han ejecutado aplicaciones ni comparado capturas en esta revisión.

## Matriz a cerrar por entregas pequeñas

| Familia | Contraste requerido | Estado |
|---|---|---|
| Iconos | Glifos, variante, tamaños, posición, targets y acción en rutas activas | Pendiente; acceso Ajustes observado |
| Textos/tipografía | Jerarquía, pesos, tamaños, truncado, idioma, fuentes autorizadas | Pendiente de comparación integral |
| Carátulas | Proporción, recorte, radios, placeholder, fondo y transiciones | Pendiente de comparación visual |
| Inicio/biblioteca/búsqueda | Disposición, filas, cabeceras, menús y navegación con MediaStore | Implementaciones previas; contraste final pendiente |
| MiniPlayer y reproductor | Vertical/horizontal, controles, dock, cola y continuidad | Implementaciones previas; contraste final pendiente |
| Ajustes | Entrada, categorías, búsqueda, tarjetas y orden compatible | Diferencias confirmadas; adaptación pendiente |
| Letras | Controles, foco, desenfoque, seguimiento, offset y compartir | Núcleo previo; fidelidad/gestos/compartir abiertos |
| Widgets | Registro y diseño cuadrado/ancho, carátula y comandos de sesión | Registro no acreditado; implementación/validación pendientes |
| Estados/accesibilidad | Permisos, vacío/error, claro/oscuro, texto grande y reducción de efectos | Comparación integral pendiente |

## Aceptación

Por pantalla: archivo origen/commit, archivo destino y consumidor activo, elementos
adaptados, datos locales reales y diferencias autorizadas. Pruebas de interacción
en Actions y comparación visual verificable bajo mismos tamaños, tema y escala.
Ver UI_PARITY_CHECKLIST.md para anchos, orientaciones y accesibilidad. Conservar
Aurelis, perfil único, servicio/sesión, Liquid Glass estable y atribuciones GPL.

Resolver por bloques de máximo30 rutas y dos revisiones; registrar cada cierre.
B06.3-d conserva su validación Actions pendiente por separado. Android Auto/TV/Wear
siguen excluidos. No iniciar otra entrega B07 ni declarar la puerta completada
mientras persistan diferencias sin resolver/evidencias visuales pendientes.
