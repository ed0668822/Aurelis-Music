<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B03.1-a — estado por entrada y restauración de navegación

## Autorización, base y alcance

«Continúa» autoriza esta entrega del plan B03–B05, incluidos commit/push en
feature/bitchord-local-migration, mismo PR1 y título. Base
49ba012a9e868fd382a3e9422fd1d229dc4e138d; checkout limpio y remoto coincidente
tras fetch. Cuenta/identidad local ed0668822 / ed0668822@gmail.com, verificado.
Run118/43520405511 de esa base terminó success en las seis fases B01–B06.
La reparación CI18 se conserva, sin rehacerla ni trasladar su resultado al código
nuevo. Presupuesto vigente: 30; esta entrega incluye ocho archivos distintos.

| Archivo | Trabajo |
|---|---|
| app/src/main/java/com/music/aurelis/ui/navigation/AppNavigator.kt | Identidad persistente por entrada y codec v1/v2. |
| app/src/main/java/com/music/aurelis/ui/navigation/AppNavigationHost.kt | Proveedores por entrada y retirada del estado cerrado. |
| app/src/test/java/com/music/aurelis/ui/navigation/AppNavigatorTest.kt | Cuatro regresiones de identidad/compatibilidad/validación. |
| app/src/test/java/com/music/aurelis/ui/navigation/NavigationEntryStateTest.kt | Cinco regresiones del host Compose y recreación. |
| NOTICE | Origen y cambios de la adaptación. |
| docs/migration/beta-local/LICENSE_AUDIT.md | Registro de procedencia. |
| docs/migration/beta-local/B03_1A_ENTRY_STATE.md | Informe y límites de esta entrega. |
| docs/migration/beta-local/PROGRESS_TRACKER.md | Progreso y puerta entre bloques. |

## Diferencia verificada respecto a BitChord

Referencia fija b690dc146009d4815825fa4a20815ef2e4ee8898, solo lectura.
AppNavigator.kt upstream documenta que dos visitas a la misma ruta no comparten
estado y AppDestination.Route usa route:entry.id. MainActivity.kt mantiene
pageStates/pageKeysSeen y elimina claves que ya no corresponden a páginas vivas.
Se consultaron esos archivos y GlassNavBar.kt; rutas/hashes están en NOTICE.

El host Aurelis anterior usaba page.nombre-de-ruta en
lugar de la identidad de RouteEntry. Por ejemplo, Settings → Equalizer → Settings
utilizaba la misma clave para ambas visitas a Settings. Tampoco retiraba el
estado de páginas extraídas de la pila. Así se podían reutilizar scroll/drafts
de una visita ya cerrada y conservar sus valores en el siguiente Bundle.

Se adapta la identidad y limpieza upstream, manteniendo el grafo local existente.
No se añaden destinos de streaming/cuentas/TV/Wear ni otro controlador paralelo.

## Implementación

Cada visita nueva usa route:ID. AppNavigationHost conserva la RouteEntry completa
de la página subyacente al reproductor/cola y la usa como clave del proveedor,
en vez de colapsarla a la ruta. Atrás mantiene las entradas inferiores; un tab
seleccionado conserva su comportamiento de volver a raíz. Las cuatro claves
tab.NOMBRE existentes siguen reteniendo búsqueda/listas de cada pestaña.

El conjunto de claves de páginas retenidas es guardable como lista de strings.
Después de aplicar la composición se retira solo el estado de claves que ya no
están en la pila. También se conserva el conjunto a través de recreación para
poder retirar claves antiguas. No se guardan canciones, imágenes, callbacks ni
objetos de repositorio en el Bundle; las entradas contienen IDs/rutas y flags.

El snapshot de navegación v2 guarda tab, siguiente contador y tripletas
ID/ruta/flag-legacy. Restaurar conserva IDs para que las claves del host coincidan
con el estado guardado. El contador no retrocede al cerrar páginas. Se rechazan
tripletas incompletas, IDs negativos/repetidos/desordenados, contadores inválidos,
flags desconocidos y rutas no reconocidas, sin restauración parcial.

El lector v1 se conserva. Sus entradas mantienen temporalmente las claves page.
anteriores; ese indicador se conserva al guardar/restaurar v2. Al cerrar una
entrada antigua se retira su clave y la próxima visita usa una identidad nueva.
La compatibilidad del codec no certifica una actualización física entre APKs con
versiones distintas de Compose; se comprueba el contrato mediante fixtures.
Los destinos sin contrato local y los callbacks externos no se serializan.

## Pruebas y dos revisiones

Cuatro casos nuevos de modelo: visitas iguales con claves distintas/IDs estables;
contador después de cerrar/recrear; compatibilidad v1 → v2; rechazo de identidades
y snapshots malformados. Se conservan los nueve casos anteriores del modelo.

Cinco casos nuevos Compose/Robolectric, SDK33 y host real con campos ligeros:

- Dos visitas a Settings separadas por Equalizer tienen valores independientes,
  conservados al recrear y volver con Atrás.
- Una página cerrada desaparece del payload de SaveableStateRegistry y la query
  de su tab se conserva, también después de recrear.
- Cambiar de tab limpia páginas cerradas y conserva el contenido de otros tabs.
- Una entrada v1 conserva su clave durante recreación y se descarta al cerrarse.
- Reproductor/cola restaurados mantienen la identidad de la página subyacente.

Los tests leen el estado guardado real, sin inspeccionar campos privados del
SaveableStateHolder. La suite existente NavigationIntegrationTest conserva los
casos del grafo de MainActivity, Atrás Android, búsqueda y reproductor sin medios;
GlassNavigationTest conserva selección/objetivos táctiles/Glass. No se reescriben.

Revisión 1 del mismo agente: referencia, consumidores, identidad y ciclo de vida,
compatibilidad, pestañas y orden de Atrás. Revisión 2: formato guardado y APIs
Compose, casos positivos/negativos, atribuciones y ocho rutas dentro de 30.
Solo revisión estática/diff locales; sin Gradle/Lint/tests Android locales.
Las nueve pruebas nuevas se ejecutarán con Debug/Release en GitHub; su resultado
no se declara aprobado durante la preparación.

## Pendientes y verificación remota

Comprobar B01–B06 tras publicar. El resultado nuevo se comunicará por SHA/run.
Esta entrega aborda estado/navegación; no acredita paridad visual física ni
cierra B03 completo. Home/estados B03.2–B03.3 y las diferencias de menús/captura/
retorno visual integradas en B03–B05 siguen abiertas. Reproducción/servicio,
firmas, permisos, ABI y los workflows de CI se conservan. CI12.3 y comentarios
CI16 permanecen pendientes; B09 sigue sin iniciar. Esperar resultado y nueva
autorización antes del siguiente bloque.
