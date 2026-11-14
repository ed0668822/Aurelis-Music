# B01.3 — carpetas y permisos

Selección de hasta16 carpetas locales mediante SAF y permiso persistente de lectura.
Solo ExternalStorageDocuments (dispositivo/SD), no proveedores cloud/remotos. El
catálogo único continúa siendo MediaStore: archivos aún no indexados por Android
no se muestran, tal como indica el selector. No se añade un crawler File ni otro
motor. El método scanLocalFolders(context, Set<URI>, filter) es ahora real y usado.

Scopes por volumen y ruta relativa normalizada, con límite de directorio (Music/
no coincide con Music2/), sin traversal. MIME de directorio y permiso se verifican
antes de guardar y al escanear. Error de carpeta/permiso revocado se muestra como
error, no catálogo vacío exitoso. Columnas de carpeta no soportadas producen error
explícito y opción de volver a todas las carpetas. No inferir paths ausentes.

Añadir verifica antes de guardar; un fallo libera el grant nuevo y conserva la
política previa. Quitar guarda antes de liberar. Quitar la última conserva modo
seleccionado vacío; no amplía implícitamente a todo el dispositivo. Volver a todas
requiere acción del usuario. Se puede quitar un URI legacy inválido sin que otros
URI inválidos bloqueen esa recuperación. ReadOnly del perfil mantiene su guarda.

La política es atómica en preferencias. Una revisión de ámbito fuerza invalidación
incluso al conceder de nuevo la misma carpeta; no depende de orden de callbacks de
ActivityResult/onResume. MediaStoreLibrarySource combina observer y cambios del
ámbito; VM mantiene debounce/cancelación y relectura al volver al primer plano.
Restore de cola usa el mismo catálogo filtrado; referencias privadas ausentes se
conservan. Cambiar filtro no detiene ni vacía una cola que ya está reproduciendo.

Settings permite elegir/quitar y ver carpetas reales con número de canciones.
La ruta Collection(FOLDER,key) abre sus canciones y las reproduce por el controlador
existente. La ruta se restaura y no requiere migrar Home/Explore/B03/B04. Carpetas
sin metadatos se agrupan explícitamente como desconocidas, sin inventar rutas.

Revisión1: ámbitos/permisos/atomicidad/identidades y caminos activos; revisión2:
provider local controlado, permisos revocados/reconcedidos, límites entre carpetas
volúmenes, columns faltantes, selección persistida, abrir/reproducir por navegación
y cancelación de observer. Mismo agente en dos pasadas. El archivo de evidencia
registra el U final. SAF real/emulador/SD física siguen pendientes; no se declara
lector de documentos no indexados ni compatibilidad con proveedores remotos.

Sin nuevas dependencias/licencias/código upstream. B01.2 conserva URI/IDs y datos
privados. No modifica perfil, DSP, firmas, LICENSE ni modelos. Autorizado junto con
B02; una vez validado B01 se continúa únicamente con los componentes visuales B02.
