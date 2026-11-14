# B03.3 — estados y recuperación

Home conserva cabecera/identidad también al cargar, fallar, denegar permiso o
quedar vacío; el cuerpo muestra el estado y su acción real sin canciones ficticias.
Se reutiliza LibraryViewModel, sin reescribir el escáner ni el ciclo de permisos.
Fuente y acciones existentes recuperan el catálogo desde permiso concedido,
biblioteca vacía, IOException y reintento sin mantener el error anterior visible.

Revisión1: cada rama y callback, persistencia del header, estado del catálogo y
protección de QA01. Revisión2 del mismo agente:20 casos dirigidos correctos en36s,
con ViewModel y fuente controlada (no solo variable de presentación); regreso a
Home poblado y snapshots claro/oscuro a escalas1/1.5. Casos nav/bar/QA01 incluidos.
Se centraliza el número4 de páginas recientes; sin cambios a servicios/modelos.

Capturas en screenshots/b03 son RobolectricSDK33/native graphics con Canvas de
captura software y canciones de prueba sin carátula. No son fotos del Redmi ni
referencias ejecutadas de BitChord. No certifican GPU/blur/fps ni audio físico.
Código de reproducción de integración usa RecordingPlayer: comandos/cola/estado,
no decodificación. Permisos del sistema, apertura APK y Logcat siguen pendientes.

R completo y resultados de APK/firmas/licencias en B03_EVIDENCE al cerrar. QA02 y
B04 no iniciados; Explore conserva su marcador previsto hasta B04. B03 conecta
navegación/Home, no presenta la pantalla Explore como implementación terminada.
