# B01.1 — perfil único y respaldo privado

Base b2bf434531dae4c174411f1afb515a5a61b64248. Autorización expresa B01.1.
No implementa B01.2/B01.3, rediseño de Settings, Dolby, DSP, ONNX ni nuevos motores.

## Comportamiento

Una LocalProfileState contiene un único perfil (ID interno, nombre y avatar).
Se eliminan APIs create/switch/delete y ProfileSessionListener, su selector y las
acciones de múltiples cuentas. Solo editar nombre/importar/cambiar/quitar foto.
Application mantiene un repositorio de biblioteca, no cambia por renombrar. Los
hooks anteriores en MainActivity/PlaybackViewModel/servicio dejan de existir.
El binding del servicio conserva restauración pausada y cancelación al cerrar;
no recrea sesión/player ni borra la cola al editar el nombre.

## Protocolo de migración

1. Si existe recibo v2 válido, verificar SHA del ZIP, hash del manifiesto e ID
   seleccionado; conservar ese ID aunque se cambie el registro legacy después.
2. Sin recibo, interpretar profiles_v1 con versión1/IDs válidos/activo incluido.
   Ausencia de registro selecciona default. Tipos/versiones inválidos no se resetearán.
3. Capturar el registro completo y las seis áreas por cada ID conocido: PROFILE,
   PREFERENCES, FAVORITES, PLAYLISTS, HISTORY y QUEUE. Incluir default y tombstones
   de borrados antiguos; no continuar limpieza destructiva.
4. Copiar avatares privados regulares de filesDir/profiles/ID, sin seguir symlinks
   ni abrir contenido externo de URIs legacy. Esas URI se conservan como metadata.
5. Crear ZIP mediante AtomicFile, cerrar ZIP sin cerrar prematuramente el stream
   atómico y hacer fsync. Verificar manifiesto/entradas/longitudes/hashes/tipos.
6. Solo entonces confirmar selection-v2.json atómicamente. Una interrupción entre
   ZIP y recibo reutiliza el mismo ZIP si la instantánea no cambió. Una escritura
   .new sin recibo confirmado no cuenta como selección completa.
7. Reabrir preferencias/datos del ID elegido sin copiarlos, fusionarlos ni borrar
   archivos antiguos. El ámbito permanece inmutable en el flujo de la aplicación.

La migración ocurre antes de inicializar ajustes/biblioteca/servicio. Es trabajo
acotado de arranque, no del hilo de audio; límite de64MiB para la instantánea
(manifiesto + avatares). Superarlo bloquea la confirmación y conserva originales.
No se acredita tiempo de arranque en dispositivo físico; medir bibliotecas/avatares
reales sigue pendiente. No se analizan canciones ni se respaldan audio o cachés.

## Formato y privacidad

Dentro de `context.noBackupFilesDir/single-profile-migration/`:

- `legacy-<SHA256 del manifiesto>.zip`: manifest.json y avatars/ID/ruta.
- `selection-v2.json`: version2, profileId, backup y archiveSha256.

Manifiesto v1: active, ids, registry, areas y avatars (entry/bytes/sha256).
Preferencias codificadas por tipo: string/int/long/boolean/stringSet/floatBits.
Float usa bits para preservar NaN/infinito/cero firmado; sets se ordenan para
checksum determinista. Se conservan claves desconocidas, no se descartan.
`SingleProfileMigration.decodePreferences` permite reconstruir valores tipados;
pruebas verifican igualdad con originales y bytes de avatares en ZIP.

Respaldo privado, fuera de caché, Auto Backup y FileProvider. Puede contener valores
privados legacy, incluidos tokens si existían: no subir ZIP/manifiesto a Git,
logs ni telemetría. Los fixtures solo usan datos inventados de prueba.
No hay selector de cuentas ni botón ficticio de restauración/exportación.
El respaldo existe para recuperación técnica y la importación de datos de B11,
pendiente. En build debug un técnico puede inspeccionar el directorio con run-as;
no asumir acceso equivalente en release. Desinstalar/borrar datos elimina también
este respaldo privado; no sustituye una exportación externa futura.

## Fallos y recuperación

Si falla lectura/validación/escritura o el respaldo confirmado desaparece/se altera,
ProfileRepository publica readOnly y un error, sin sobrescribir originales,
registro o respaldo. Settings bloquea edición; LocalUserLibrary/QueueStore y ajustes
mantienen sus guardas de escritura. Se puede volver a intentar en el siguiente
inicio tras resolver un fallo transitorio de almacenamiento. No regenerar un
respaldo corrupto encima del anterior ni elegir otro perfil automáticamente.

El recibo y el ZIP se excluyen juntos de Auto Backup. Una restauración Android
sin noBackup vuelve a ejecutar la selección desde el registro legacy conservado
y respalda los datos restaurados antes de confirmar. Las referencias externas de
fotos legacy dependen del permiso del proveedor; las nuevas fotos son PNG privados.

## Verificación y alcance de revisiones

Revisión1: código/arquitectura, tipos de preferencias, orden respaldar/verificar/
confirmar, AtomicFile, cancelación, paths/symlinks, ID persistido y borrados no
reanudados. Se actualizaron conexiones necesarias F04/F08 únicamente para retirar
los callbacks multiusuario; no se adelanta navegación/reproductor de esos bloques.

Revisión2: selección de47 casos pasada. Dos primeras aserciones UI se corrigieron
porque el nombre aparece en cabecera y fila: se verifica una aparición visible,
sin retirar la comprobación de edición. Suite completa/build/lint y entrega se
registran en B01_1_EVIDENCE y PROGRESS_TRACKER al finalizar. Ambas revisiones son
pasadas separadas del mismo agente, no revisores humanos independientes.

Pruebas: instalación nueva, legado único sin default, múltiples ámbitos, seis
áreas/tipos/avatares, música/cachés intactos, registry inválido, tombstones,
interrupción y .new parcial, ZIP corrupto, recibo desconocido/ID inconsistente,
límite de tamaño, reinicio y metadata posterior. UI sin crear/cambiar/eliminar
perfil; foto/nombre persistidos; edición no altera cola/navegación/playback.

Prueba física, permisos SAF reales y arranque tras actualización en dispositivo
pendientes. Límite ELF16KB de ONNX permanece como hallazgo anterior para B09/B13;
no se cambian bibliotecas nativas en esta unidad.


Validación final: BUILD SUCCESSFUL in 5m 12s;137 pruebas por variante sin fallos/errores/omitidos;
assemble/lint debug y release correctos, R8/firma/ZIP/avisos verificados.
B01_1_EVIDENCE.json fija hashes y advertencias de estos artefactos, no los de B00.
