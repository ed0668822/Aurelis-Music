# CI10 — expectativa anterior del offset en prueba integrada

Base/HEAD fallido: `ee5c3afd8ca4419fd278dca3a0767e78311bb4db`.
Run146: https://github.com/ed0668822/Aurelis-Music/actions/runs/42298701818
Job: https://github.com/ed0668822/Aurelis-Music/actions/runs/42298701818/job/128170546459

## Evidencia y causa

Páginas públicas recuperadas con enlace al commit y anotaciones JUnit. REST/gh
--log-failed devuelve Forbidden; no se descargaron todos los registros. Las
anotaciones muestran la prueba importedLyricsHighlightAndSeekWithOffsetInTheRealPlayer
con `expected:<500> but was:<1500>`, repetida en el job. Es un fallo ejecutando la
prueba, no un error Kotlin de compilación. Gate: tests=failure, build/verify/upload
skipped; los APK permanecen bloqueados correctamente. No se modifica ese control.

B07.2 unificó la convención de BitChord: positivo retrasa letras, posición visible
=player-offset y seek=line+offset, con saturación. La prueba anterior conservaba
line-offset. Para línea1000 y offset+500 corresponde player1500; el valor obtenido
es consistente con el contrato nuevo. Debió actualizarse la prueba al cambiarlo.

## Corrección mínima y comprobaciones conservadas

Solo se modifica el cuerpo de esa prueba en PlayerIntegrationTest.kt:

- Offset+500: espera1500 y la segunda frase seleccionada.
- Offset-500: espera500 y la misma frase seleccionada; no se elimina la aserción
  anterior, se usa en el escenario al que ahora corresponde.
- Verifica canción completa, cola por id/UUID, contadores prepare/reemplazo y pausa.
- Conserva etiqueta de fuente importada, selección inicial, captura de pantalla y
  rechazo de importar una letra para una canción que ya cambió.
- Cambios de preferencias en UI thread y restauración del valor previo en finally.

No se calculan resultados esperados usando las funciones de producción, ni se
revierten offset/renderizado para hacer coincidir una expectativa obsoleta. Todos
los métodos de prueba siguen presentes; el resto del archivo queda idéntico.

## Dos revisiones y límites

Revisión1 estática del mismo agente: logs/commit, contrato de referencia, host activo,
helpers/prueba unitaria y matemática inversa. Revisión2 separada del mismo agente:
estado/restauración de preferencias, aserciones conservadas/reforzadas, resto de
métodos intacto y alcance protegido. `git diff --check` correcto.

Sin cambios en producción, servicio, Glass, parsers/caché, dependencias/SDK/ABI,
modelos, workflow/gates, licencias o plan principal. No Gradle, APK ni tests Android
locales. Corrección VALIDACIÓN PENDIENTE DE GITHUB; no arranque/audio/GPU verificados.
Las anotaciones disponibles no acreditan por sí solas toda la suite como aprobada.

Publicar misma rama/PR1 y esperar resultado que comunique el usuario. Mantener la
autorización de continuar hacia B08, sin iniciarlo mientras esta puerta falle;
no pedir otra aprobación del alcance vigente ni empezar B09.
