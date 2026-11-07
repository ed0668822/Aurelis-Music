# B01.2 — MediaStore y metadatos

Conserva IDs numéricos y URI external de MediaStore para no invalidar favoritos,
playlists, cola o cachés/importaciones de letras por añadir información de volumen.
Añade volumen, ruta relativa, nombre, MIME, modificación, tamaño conocido y origen
del género, con ida/vuelta Media3. Identificadores contradictorios entre volúmenes
fallan explícitamente en vez de descartar un archivo o vincular datos incorrectos.

MediaStore tiene prioridad. Si falta género u otro dato básico, se intenta leer
la etiqueta real con MediaMetadataRetriever, fuera del hilo UI. Fallo por formato
no inventa valores; permisos denegados se propagan. Género ausente sigue null y
B04 lo presentará como Sin clasificar. No se infieren géneros de artista/nombre.

Cache LRU2048 en memoria; clave URI/tamaño/modificación; no reutiliza si versión
faltante ni cachea fallos transitorios. Primera lectura de biblioteca grande puede
necesitar lectura de tags; rendimiento físico pendiente. Se conserva duración
mínima500ms; duración nula requiere etiqueta válida. Columns opcionales rechazadas
permiten query mínima. No hay nuevo motor ni dependencia. Se elimina el método
File vacío scanLocalFolders, sin callers; B01.3 añadirá el filtro de carpetas real.

Revisión1 (mismo agente, pasada aparte): fuentes, caché, cancelación, identidad y
permisos. Se corrigió constructor Kotlin y se conservaron URI legacy al comprobar
que la clave de letras incluye URI. Revisión2:144 pruebas debug sin errores/fallos/
omisiones; assembleDebug y lintDebug correctos. Tests de tags/cache/volúmenes y
proyección, observador/revocación/movimientos y Media3. No dispositivo ni release
nuevos en esta unidad; evidencia B01_2_EVIDENCE. Autorización vigente permite B01.3
antes de B02; no se adelanta Explore/Home ni cambios de reproductor.
