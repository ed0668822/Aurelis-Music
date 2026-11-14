<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B02.3-b1 — hápticos por acción y controles accesibles

## Autorización, base y presupuesto

El usuario autorizó solucionar el fallo del ZIP112 y continuar el siguiente
bloque. Esta entrega de B02.3-b es independiente de la reparación de foco
b02607d6eedea057c8db27df5ee6c9a29f8a98e7, que es su base de preparación. Se
publica en feature/bitchord-local-migration, mismo PR1, después de verificar la
reparación en GitHub. No se repite la aprobación entre aplicación y push.

Entrega difícil, seis rutas en total:

| Archivo | Responsabilidad |
|---|---|
| app/src/main/java/com/music/aurelis/ui/haptics/Haptics.kt | Política Android, motor disponible y atributos de interacción. |
| app/src/main/java/com/music/aurelis/ui/components/IosSwitch.kt | Feedback solo desde la acción propia habilitada. |
| app/src/main/java/com/music/aurelis/ui/components/SettingsComponents.kt | Una acción/háptico por fila, estado deshabilitado accesible. |
| app/src/test/java/com/music/aurelis/ui/theme/SharedGesturesTest.kt | Cuatro regresiones de política, hardware y consumidores reales. |
| docs/migration/beta-local/B02_3B_HAPTIC_ACTIONS.md | Evidencias, revisiones y pendientes. |
| docs/migration/beta-local/PROGRESS_TRACKER.md | Seguimiento de esta entrega. |

## Referencia y problemas observados

BitChord, solo lectura, b690dc146009d4815825fa4a20815ef2e4ee8898:
sharedUi/src/androidMain/kotlin/com/music/bitchord/ui/haptics/Haptics.android.kt.
Se consultó su política de habilitación Android y clasificación USAGE_TOUCH.
La implementación local conserva sus patrones existentes; no se copia el motor
upstream de composición de primitivas ni se declara equivalencia física.
IosSwitch conserva sus avisos originales y el origen registrado en NOTICE. La
cabecera Aurelis añadida delimita las contribuciones propias, no relicencia el
código BitChord GPLv3. No se eliminan atribuciones ni licencias anteriores.

Antes, Haptics utilizaba vibrate(effect) sin comprobar HAPTIC_FEEDBACK_ENABLED ni
hasVibrator. Los consumidores podían solicitar feedback con el ajuste de Android
apagado. Además IosSwitch vibraba desde LaunchedEffect(checked): un cambio externo
de preferencia, incluso en un switch decorativo/deshabilitado, disparaba feedback
sin acción del usuario. SettingsSubRow delegaba su feedback en ese efecto del
control dibujado, aunque la fila era la dueña de la interacción.

## Comportamiento y accesibilidad

rememberHaptics utiliza applicationContext. Cada acción lee la preferencia de
Android, por lo que un cambio externo se aplica sin recrear el handle. El servicio
ausente o un dispositivo sin motor omiten el feedback. Las vibraciones habilitadas
se envían con VibrationAttributes.USAGE_TOUCH para que Android aplique la política
de interacción. minSdk33 ya permite esta API; no se alteran SDK, permisos o ABI.
Los patrones Tick/Tap/Select/toggle/reproducción conservan sus valores anteriores.

IosSwitch emite feedback desde su callback habilitado después de ejecutar la
acción. La composición inicial, los cambios externos y el modo decorativo
(onCheckedChange=null) quedan silenciosos. SettingsSubRow posee el callback y
emite su propio háptico una vez; el switch secundario conserva solo la función
visual. No se añaden vibraciones al gesto largo que ya gestiona Compose.

SettingsSubRow admite enabled=false: desactiva la acción semántica de la fila y
atenúa texto/control. Su rol Switch, valor y objetivo mínimo48dp permanecen en un
único destino; no se duplican callbacks en el dibujo secundario. El parámetro
nuevo es opcional y al final, por lo que los consumidores existentes conservan
su contrato. No cambian colores, formas, iconos o animaciones del switch.

## Puerta de reparación comprobada en GitHub

[Run113](https://github.com/ed0668822/Aurelis-Music/actions/runs/43479948669),
SHA b02607d6eedea057c8db27df5ee6c9a29f8a98e7: completed/success según API.
B01, B02, B03, B04, B05 y B06 terminaron success. B03 aprobó Debug/Release,
generación Kover, gate de cobertura80% y ambas suites; B04 y B06 también
completaron sus contratos de verificación/entrega. Esta evidencia corresponde
a la reparación, no al código nuevo de hápticos preparado después. No se
descargó/verificó un APK local ni se acredita hardware físico por esos estados.

## Validación y dos revisiones

Cuatro pruebas nuevas, preparadas para Debug/Release en GitHub:

- El mismo handle respeta apagado/encendido/apagado del ajuste de Android y
  registra USAGE_TOUCH en Robolectric.
- Un motor ausente/null omite todos los patrones sin bloquear al consumidor.
- Cambios externos, switch decorativo y switch deshabilitado no vibran; la acción
  habilitada cambia el estado y registra feedback.
- La fila ofrece un destino de toggle, altura48dp y una acción; el modo
  deshabilitado no cambia el valor ni genera feedback, ni el cambio externo.

Las verificaciones usan ShadowVibrator4.14.1, restablecen preferencias/hardware y
observan atributos persistentes en vez de depender de cuánto dure el pulso.
La suite conserva las14 pruebas anteriores, incluidas las tres aserciones de foco
de la reparación. Contiene18 casos preparados; no ejecutados localmente.

Revisión1 del mismo agente: dueños reales de eventos, preferencias Android,
contexto, compatibilidad API y conservación visual. Revisión2 del mismo agente:
presupuesto, API de shadows consultada en su versión exacta, aislamiento de
pruebas, semántica única y atribuciones. Comprobaciones estáticas/diff locales;
sin Gradle/APK/tests Android locales, ni cambios a workflow, firma o gates.
Resultado Android de esta entrega pendiente de GitHub; no se declara PASS por
compilación ni sensación háptica física por evidencia Robolectric.

## Límites y continuación

Este es B02.3-b1: política y acciones únicas. Las primitivas/adaptación de motor
de BitChord, un ajuste propio adicional en Aurelis y pruebas TalkBack/hardware
siguen pendientes; no se declara paridad física global ni B02 cerrado. Las
capturas y animaciones de retorno de HeldItem permanecen pendientes de integrar
con Home/biblioteca/reproductor en B03–B05, como registra B02.3-a.
CI12.3/comentarios CI16 siguen abiertos; B09 sin iniciar. Tras esta entrega se
espera el resultado comunicado por el usuario y autorización para la siguiente.
