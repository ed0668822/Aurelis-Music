<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B06.1-b — Play Next y edición con shuffle

Base80dcae7a1b4868593b0f01b0355a573eb20c9f0c. El usuario confirma la reparación
anterior mediante «Continúa con el siguiente bloque». Misma rama/PR1. Riesgo audio;
máximo vigente30 archivos distintos acumulados, sustituyendo límites históricos.
No descargar informes previos, no iniciar B06.2 ni B09.

## Manifiesto previo y aceptación

1. playback/ShuffleQueueEdits.kt (nuevo)
2. playback/AurelisPlaybackService.kt
3. playback/PlaybackViewModel.kt
4. playback/QueueCoordinator.kt
5. ui/screens/QueueScreen.kt
6. ui/screens/NowPlayingScreen.kt
7. ui/player/PlayerQueue.kt
8. res/values/strings.xml
9. test/playback/ShuffleQueueEditsTest.kt (nuevo)
10. test/playback/QueueCoordinatorTest.kt
11. NOTICE
12. docs/migration/beta-local/LICENSE_AUDIT.md
13. docs/migration/beta-local/PROGRESS_TRACKER.md
14. test/ui/PlayerIntegrationTest.kt
15. este documento

Las rutas Kotlin son relativas a app/src/main/java/com/music/aurelis o
app/src/test/java/com/music/aurelis según corresponda. Recursos en app/src/main.

La sesión existente recibe comandos privados para insertar y mover el orden
aleatorio. Sin segundo reproductor, sin reemplazar cola, prepare o seek. Nuevas
ocurrencias USER_QUEUE se fijan antes del contexto futuro; Play Next delante de
las ya pendientes, añadir detrás de ellas. Mover modifica la permutación visible;
al apagar shuffle se conserva el orden físico original con inserciones al final.
No se promete restauración canónica de BitChord: se conserva el enfoque nativo
Media3 aprobado en B06.1-a. Selección manual establece el prefijo del ciclo visible.
Vaciar elimina USER_QUEUE futuras en ese ciclo, nunca actual ni prefijo.

Referencia solo lectura BitChord b690dc146009d4815825fa4a20815ef2e4ee8898,
playback/QueueShuffle.kt: sections/restore (USER_QUEUE antes del contexto, identidad
por ocurrencia). No copiar contexto global/crossfade. Firma APIs inspeccionada con
javap de dependencias existentes; no compilación local. Dos revisiones del mismo
agente y pruebas nuevas para Actions; CI/dispositivo pendientes hasta evidencia.


## Contrato integrado y límites

Comando EDIT_SHUFFLE_QUEUE solo para paquete propietario y UID del proceso. Los
otros controladores conservan transporte estándar y no reciben este comando.
Inserciones admiten content/file locales, entre1 y200 canciones por operación;
rechazo visible en UI si falla/queda obsoleto/no hay capacidad. Sin fuentes de red.
Los MediaItem viajan con configuración local y la sesión asigna queueEntryId nuevo.
Movimientos resuelven IDs de origen/destino y current otra vez al ejecutar; si ya
no existen o cambió la canción actual, fallan sin mover otra ocurrencia.
Vaciar con shuffle se calcula en el servicio al ejecutar, no contra índices remotos.
La cola plana incorpora botón para quitar solo añadidas pendientes; el actual y el
prefijo del ciclo visible se conservan. Accesibilidad del actual usa vecinos del
orden mostrado, no index±1 físico. Sin capacidad permanece bloqueo explicado.

Se reordenan las USER_QUEUE actualmente futuras al insertar (next delante/append
detrás); no se recrean canciones del prefijo ni se inventa historial de escucha.
Apagar/reencender shuffle sigue siendo operación nativa Media3; no se persiste la
permutación ni el pinning entre reinicios (B06.3). Sin contexto canónico de BitChord,
ni promesa de crossfade, prueba física o cierre global B06.

## Validación de la entrega

Nueve regresiones nuevas para Actions:
- Play Next repetido y append: prioridad, duplicados y toggle sin reinicio.
- Movimiento visible/selección manual/eliminación: índices e identidad correctos.
- Solicitudes obsoletas: IDs desaparecidos/current distinto no modifican otra fila.
- Payload vacío/exceso/URI de red/shuffle apagado: rechazo previo a mutación.
- Cola vacía: añadir duplicados no prepara ni inicia audio.
- Otro paquete: no recibe comando privado ni puede editar por esa ruta.
- Vaciar servicio: futuro USER_QUEUE físicamente anterior al actual se retira;
  prefijo, actual y contexto sobreviven con posición intacta.
- Coordinador sin permiso de Timeline: vaciar no modifica ocurrencias.
- UI plana: botón conectado quita añadidas futuras y preserva posición/pausa.

Dos revisiones estáticas del mismo agente: primera, índices físicos/permutación,
identidad/capacidades/propietario/límites; segunda, firmas de API/imports/call sites,
UI/accessibilidad, fixtures, manifiesto de15 y atribuciones. XML parseado y
`git diff --check` correcto. javap solo inspecciona dependencias existentes1.11.1;
sin Gradle/Lint/compilación/APK/pruebas Android locales. Todo eso queda para Actions.
Tras push avisar con SHA y enlace; esperar respuesta antes de otro bloque.


## Reparación de run133

Run43637377039 del commit0febb9096b063e3e1665401a5958600c3c982ea3 falló en B01,
compileDebugKotlin. B02/B03/B04/B05 SKIPPED, B06 FAILURE por puerta final.
Los informes de artefactos se bloquean en productionresultssa7.blob.core.windows.net;
el ZIP oficial de logs del run sí pudo descargarse y verificarse íntegro.
En B01/14_Compile and assemble Debug APK.txt:539–540 constan dos errores Kotlin en
ShuffleQueueEdits.kt:59 y117: SessionResult.Code no admite member function ni value
parameter; sus destinos son type usage/expression. Se mueven ambas anotaciones al
Int de retorno/parámetro y se anota también el tipo del resultado local del callback.
Se conserva el contrato de códigos y no se elimina ni suprime la comprobación.
Sin cambios al algoritmo, permisos, límites, pruebas ni workflow.

Tres archivos ya incluidos (código, este informe, tracker); quince distintos
acumulados de máximo30. Dos revisiones estáticas del mismo agente: diagnóstico y
metadatos de la anotación en dependencia; después diff/call sites/alcance. Pasa
`git diff --check`. Sin Gradle/Lint/compilación ni pruebas Android locales. Nueve
regresiones y nuevo CI pendientes, no se afirma aprobación de fases omitidas.
