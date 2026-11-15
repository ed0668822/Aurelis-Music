<!-- Aurelis © 2026. GPL-3.0-or-later -->
# Pre-B07 UI2 — correcciones de las páginas y conexiones activas

Autorización: «Continúa y termina». Límite posterior:60 rutas distintas para estas
correcciones, contando toda la comparación, entregas y reparaciones. Base
baedcf57715ec8333230fd716960f00603a37c5f. GitHub confirma run43692222152 SUCCESS;
la comparación43692222178 vuelve a fallar tras MiniPlayer. Logs oficiales leídos,
sin descargar otro ZIP. No se avanza un bloque funcional B07/B09 ni otro backend.

## Comportamiento resultante

1. Música local mantiene el ViewModel/catálogo existentes, con la presentación
   separada en LocalMusicContent. Cabecera y filas pertenecen al mismo LazyColumn;
   imagen de carpeta de76% del ancho, máximo50% del alto, radio12dp y sombra18dp.
   El contenido reserva la altura real del chrome inferior sin reducir otra vez
   la ventana usada para calcular la portada. Error/permiso/carga no requieren
   atravesar una portada grande. Canciones, Artistas, Álbumes, Playlists y Carpetas
   resuelven las colecciones locales existentes, sin catálogo o player adicional.
2. La cabecera muestra cantidad/duración reales y las filas de pista tienen menú
   conectado a SongActionsDialog. Artistas/álbumes muestran su imagen local; la
   carpeta conserva un placeholder deliberado. Imágenes recortadas a su contenedor.
3. SettingsScreen, el consumidor activo, tiene búsqueda y categorías compatibles
   en orden reproducción/apariencia/rendimiento/música local/almacenamiento/datos/
   idioma/avanzado/acerca de. No incluye descargas remotas. Letras se abre desde
   reproducción y también por búsqueda. Categorías se guardan como claves escalares
   settings-section:ENUM, con IDs por entrada y Atrás/restauración independientes.
   Búsqueda, modales y errores de guardado conservan su función. El perfil único
   local ocupa el espacio de cuenta; no hay autenticación ni selector de perfiles.
4. Se reutilizan controles reales: tema/fuente/cristal/hápticos, reducción de efectos,
   ecualizador, letras/traducción y carpetas. Almacenamiento muestra solo datos
   conocidos del catálogo. Tus datos abre Replay; no ofrece respaldo ficticio.
   Idioma usa AppCompat y los seis idiomas declarados, con metadatos de persistencia
   de AppCompat. Avanzado remite a los ajustes locales existentes; no añade DSP.
5. Home sustituye la casa genérica por la marca musical Aurelis. El espacio que
   ocupa la cuenta en la referencia se adapta al perfil/biblioteca local, con
   accesos reales. Tarjetas de selección/AutoMix3:4, ancho60% hasta280dp y radio12dp;
   se conservan carruseles, favoritos, playlists, recomendaciones y sus callbacks.
   AutoMix conserva su implementación: modificar su presentación no certifica modelos.
6. Cristal mantiene pestañas principales en una cápsula y Buscar en un círculo
   separado, como el modo del origen. Sin cristal conserva las cuatro etiquetas.
   Selección, haptics y anclaje inferior se mantienen; las pruebas buscan también
   la etiqueta accesible de Buscar cuando no tiene texto visible.
7. Se corrigen asociaciones de glifos en pestañas/Biblioteca. Cuadrícula, pila y
   chevrón son vectores geométricos originales. Otros glifos usan Material Rounded
   ya disponible. No se importan los vectores SF Symbols del origen: su propio
   archivo explica restricciones de Apple. Esta diferencia se declara, no se
   presenta como una copia literal o licencia GPL de los símbolos de Apple.
8. Manifest registra Square/Wide/Pill y su receptor privado de acciones. El servicio
   publica metadatos y estado del mismo player mediante un único escritor IO
   conflado; no hace I/O en callbacks de audio ni crea otro motor. Al cerrar publica
   estado detenido y retira el listener. La publicación final del render comprueba
   el snapshot actual bajo el mismo bloqueo que la escritura; carátulas tardías no
   pueden devolver un estado anterior. Los PendingIntents inmutables abren el panel
   y los comandos siguen usando MediaController/AurelisPlaybackService.
9. El capturador instala ambas apps y conserva el registro de widgets antes de
   navegar, para que una caída posterior no borre esa evidencia. Abre el MiniPlayer
   desde el título junto a sus controles, no desde una fila inferior de biblioteca.
   No intenta rotar la lista si el player no fue alcanzado. El fallo conserva su
   fallo real y sus capturas; no se convierte navegación omitida en paridad aprobada.

## Validación y límites reales

Local:23 pruebas Python aprobadas, py_compile y git diff --check. Regresión nueva
reproduce título de biblioteca debajo del MiniPlayer y acredita que se pulsa la
banda de transporte. XML del manifest y recursos referenciados revisados. Parser
Kotlin estático temporal para archivos nuevos/cambios: sin errores nuevos; el
parser tiene un error conocido en SettingsScreen ya presente en la base. Esto no
equivale a compilar Kotlin ni a ejecutar Android.

Ocho pruebas Android nuevas para Actions: cabecera/acciones alcanzables en ventana
baja, restauración del desplazamiento local, Carpetas a la colección correcta, búsqueda/categorías y Atrás, restauración de
rutas de categoría, registro/recursos privados de widgets, snapshot del player real
de prueba y rechazo de publicación tardía. Se mantienen y actualizan pruebas previas
de hápticos, fuentes/tema, traducción, perfiles y navegación. No se elimina una
prueba por cambiar el diseño. Android/Gradle/Lint/APK se ejecutan solo en Actions.

Dos revisiones estáticas del mismo agente: primero consumidores, estado, acciones
locales y correspondencia del origen; después APIs, permisos/receivers, ciclo de
vida/IO, carreras de render, accesibilidad y tests/documentación. No independientes.

La validación visual nueva sigue pendiente del emulador: letras/reproductores y
widgets necesitan capturas de los estados reales. Registro de proveedores no es
un widget colocado en un launcher. El plegado/acoplamiento dinámico exacto de
Liquid Glass, gestos y movimiento tampoco se certifican con una imagen estática.
No se declara toda la app idéntica ni se cierra B12/B07/B13 por estos cambios.
Nombre, fuente autorizada, datos locales y glifos permitidos son diferencias
explícitas. No se incorporan streaming, Auto/TV/Wear ni funciones aplazadas.

Referencias/hashes en LICENSE_AUDIT. Manifiesto exacto de archivos y evidencia
estática de entrega en /workspace/aurelis-validation/ui-finish. La unión conserva
los cambios de la comparación anterior y está por debajo de60 rutas; no se cuenta
solo este commit para ocultar las reparaciones anteriores.
