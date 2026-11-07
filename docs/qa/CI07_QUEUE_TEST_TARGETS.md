# CI07 — objetivos concretos en pruebas de cola

Base/HEAD fallido: `390c759b2f18c6a47fc2ae0200d4c6e66797c268`.
Run141: https://github.com/ed0668822/Aurelis-Music/actions/runs/42268479543
Job: https://github.com/ed0668822/Aurelis-Music/actions/runs/42268479543/job/128066550781

## Evidencia y causa

Se consultaron anotaciones públicas y enlace de commit. REST/gh --log-failed
continúa Forbidden, sin descarga de logs completos. El gate comunica resultados
`tests=failure, build=skipped, verify=skipped, upload_apk=skipped`: detiene los APK
porque fallan pruebas, sin ser el origen del fallo. No se cambia gate/workflow.
Run141 ejecutó pruebas de interfaz; los errores Kotlin anteriores de CI06 ya no
aparecen en las anotaciones. Esto no acredita un APK nuevo ni un dispositivo real.

1. PlayerIntegrationTest.miniPlayerOpensRealControlsAndQueueActionsStaySynchronized:
   esperaba [2,3], obtuvo [1,2]. Tras saltar a canción2, el historial está oculto por
   defecto (SettingsRepository.showQueueHistory=false). El botón [0] visible borra
   canción3; no canción1 histórica como presupone la prueba. Se fija el estado de
   preferencias en el fixture, comprueba la ausencia de la fila histórica, habilita
   historial explícitamente, lleva la lista de cola al índice0 para componer la
   fila en LazyColumn y busca eliminación dentro del ancestro de su UUID.
   Se conserva [2,3] y se exige además mismo UUID actual, índice0, posición y pausa.
2. PlayerIntegrationTest.draggingAQueueOccurrenceUsesTheExistingPlayerWithoutSeekingOrRestartingIt:
   Compose informa que el tag existe solo en el árbol unmerged. La fila clickable
   combina semántica de sus descendientes, incluido el Box del tirador. Se usa
   useUnmergedTree=true en los cuatro accesos al tirador (scroll/visibilidad y
   geometría inicial/final). El swipe sigue siendo real dentro de la prueba,
   conservando orden esperado [1,3,2], identidad actual, posición, pausa y ausencia
   de reinicio. No se sustituye por invocación directa al ViewModel.

## Dos revisiones

Revisión1 estática del mismo agente: contrastar anotaciones, estado inicial y
jerarquía Row.clickable/Box.testTag/Modifier.pointerInput. Selectores por ocurrencia,
no orden accidental de iconos. Historial solo se cambia desde esta prueba, sin
alterar preferencias predeterminadas de producción ni forzar que la app lo muestre.

Revisión2 estática separada del mismo agente: todos los métodos de test y expresiones
de aserción previas conservados (dos prefijos de selector pasan a unmerged). Nuevas
comprobaciones de visibilidad y estado. Diff correcto; solo PlayerIntegrationTest.kt
y documentación cambian. App/servicio/cola/Glass/QA01, ABI/JNI, dependencias, firmas,
LICENSE/NOTICE, workflow/gates y plan intactos. No reintentos, ignores o exclusiones.

## Estado

Corrección preparada; compilación y ejecución nuevas PENDIENTES DE GITHUB. No se
ha ejecutado Gradle, APK ni tests Android localmente por instrucción del usuario.
Sin instalación, arranque, Logcat, GPU ni audio físico comprobados. Estas revisiones
no certifican que el arrastre pase: antes fallaba al localizar el nodo, por lo que
la ejecución completa del gesto sigue pendiente del nuevo CI.

Publicar en misma rama/PR1, avisar y esperar el resultado del usuario. No B07,
QA02 ni otras unidades B06 en esta corrección. Evidencias/hashes en JSON adyacente.
