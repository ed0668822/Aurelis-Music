# F015–F029 · Biblioteca local (BitChord → Aurelis)

Referencia: BitChord `b690dc146009d4815825fa4a20815ef2e4ee8898`, comparado con el código de Aurelis en `feature/bitchord-local-migration`.
Este bloque conserva MediaStore, LibraryViewModel y la reproducción local. No importa streaming, autenticación ni otro escáner.

| Fase | Estado de este bloque | Evidencia / alcance |
|---|---|---|
| F015 | Corregido y pruebas añadidas | `LocalMediaRepository`: duración 0/desconocida exige etiquetas válidas; tolerancia al rechazo de `DATA`; se preservan exclusión de pendientes/papelera y MediaStore como fuente única |
| F016 | Reforzado con pruebas | `LocalMetadataReader`: lecturas vacías no se cachean, pista/disco válidos, cancelación y permiso propagados, LRU acotado; MediaStore sigue siendo propietario del catálogo |
| F017 | Reforzado; pendiente CI | `LocalLibrarySource` observa avisos legacy/URI de MediaStore sin polling; `LibraryViewModel` refresca en primer plano, deja de hacerlo en segundo plano y vuelve a registrarse al regresar; pruebas nuevas |
| F018 | Reforzado; pendiente CI | `LocalFolderRepository`: eliminar sólo árboles realmente seleccionados, no revocar permisos SAF ajenos, mantener vacío el alcance tras eliminar la última carpeta; pruebas de eliminación idempotente y permisos cruzados |
| F019 | Mejorado | `LocalGroups`: orden de álbum por disco y pista, con prueba de regresión |
| F020 | Parcial | `LocalMusicContent`: cinco pestañas conectadas; paridad completa de vista cuadrícula pendiente |
| F021 | Conservado | `LocalGenres`: sólo géneros procedentes de los metadatos reales |
| F022 | Parcial | `LibraryScreen`: tarjetas/enlaces locales conectados; quedan diferencias visuales del origen |
| F023 | Mejorado | `LocalCollectionScreen`: cabecera con portada y controles de álbum |
| F024 | Mejorado | `LocalCollectionScreen`: cabecera de artista sobre los datos locales |
| F025 | Mejorado | `LocalCollectionScreen`: cabecera de carpeta sin alterar permisos ni claves |
| F026 | Mejorado | Claves de fila por posición e ID, y operaciones sobre índices originales de playlist |
| F027 | Corregido parcialmente | Fecha de modificación real; conservar orden manual de playlist; resta unificar controles de orden/vista entre pantallas |
| F028 | Conservado | `AudioPermission`, escaneo y recuperación de permisos existentes; faltan pruebas de revocación durante reproducción |
| F029 | Integrado en colecciones | `BrowseActionsMenu`: acciones locales reales, sin botones de relleno; falta comparar geometría y gestos del menú original |

No equivale a dar por terminada la fidelidad visual ni las pruebas instrumentadas. Validar Gradle B01–B06 y dispositivos antes de cerrar las fases parciales.

## Licenciamiento
Código nuevo original con cabeceras `SPDX-License-Identifier: LGPL-3.0-or-later`, conforme a la tabla de Aurelis. Los archivos adaptados de BitChord u otros terceros mantienen sus avisos y licencias aplicables (incluido `NOTICE`/`LICENSES`). No se eliminan atribuciones originales sin verificar derechos de relicenciamiento.
