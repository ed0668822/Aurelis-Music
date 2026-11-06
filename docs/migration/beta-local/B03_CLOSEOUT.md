# Entrega B03 — navegación y Home local

Rama: feature/bitchord-local-migration. Mismo PR1, título conservado:
https://github.com/ed0668822/Aurelis-Music/pull/1
Aurelis Music Beta UI and components.

| Unidad | Commit real |
|---|---|
| B03.1 | `e79f32a64cd0c6faf16538ee872132b6db6759e4` |
| B03.2 | `6b4bded2cd53545baea4c207a1bba9121bf209e6` |
| B03.3 | `2e91a09f8c8c4ab621a54ffe4992dd3280ba5eaa` |


Autorización: «Mejor continúa con el plan ... pero si prueba todo lo que puedas»,
retomando el siguiente B03 y preservando todo lo anterior. QA02/B04 no iniciados.

Host local conectado antes de la UI; barra con píldora/reflexión/movimiento ya
portados; Home usa carruseles reales, recientes4/columna y playlists disponibles.
Cada sección tiene su cola y sus acciones; estados vuelven al catálogo tras
permiso/vacío/error/reintento. No contenido de streaming/ejemplo en producción.

Revisión1/2: pasadas separadas del mismo agente. Dirigidas14/17/20 correctas;
R acumulado BUILD SUCCESSFUL in5m6s,179 casos debug +179 release,0 fallos,
errores u omitidos. Lint224/223 advertencias y0 errores. Firmas/ZIP correctos,
10 textos legales/fuente idénticos y12 .so ARM64. B03_EVIDENCE registra archivos,
SHA de APK, referencias y capturas. Los APK se generaron desde6b4bded más los
archivos probados, luego registrados en2e91a09; este checkpoint es documental.
No afirmar tests instrumentados sobre el APK minificado: R8 compilado y suites
Robolectric de ambas variantes son verificaciones diferentes.

Plan maestro/orden de bloques, licencia, identidad/firma/ABI/workflow y corrección
QA01 idénticos a la base. NOTICE añade procedencia sin retirar avisos originales.
Capturas son Robolectric/Canvas software con fixtures, no Redmi/GPU ni goldens de
BitChord ejecutable. No se declara paridad visual completa ni audio real probado.
Explore se mantiene para B04. Integración avanzada HeldItem/ancla/retorno mantiene
sus bloques futuros; no se finge un port completo de esos componentes.

QA anterior: Build127 y CodeQL de b8b8910 correctos, registrados en docs/qa.
CI B03: consultar SHA de esta nueva entrega tras push; no heredar verde QA127.
Los commits se publican juntos para evitar duplicación de pipelines intermedios.
Autor/committer local ed0668822 <ed0668822@gmail.com>; sin merge ni force push.

Siguiente bloque B04 requiere autorización. QA02 no se inicia por esta entrega.
Android real, instalación/arranque, Logcat/GPU/fps/TalkBack y audio físico siguen
pendientes; los límites nativos/licencias heredados continúan para B09/B13.
