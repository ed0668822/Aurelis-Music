# CI15 — conflicto del PR1 y activación de compilación

## Alcance y presupuesto

Corrección autorizada mediante «Soluciona eso antes del siguiente bloque».
No implementa nuevas fases CI14 ni adelanta revisión de carpetas, CI12.3 o B09.
Conserva la fase F1 de CI14 llegada concurrentemente desde el remoto.
Rama: feature/bitchord-local-migration; PR1 y título conservados.
Presupuesto difícil: máximo 6 rutas distintas; manifiesto previsto y real: 5.

| Ruta | Motivo |
|---|---|
| .github/workflows/b01-compile-debug.yml | Resolver add/add conservando la etapa reutilizable y aclarar su contrato |
| AGENTS.md | Incorporar sin cambios las instrucciones existentes de main |
| SECURITY.md | Incorporar sin cambios las instrucciones existentes de main |
| docs/qa/CI15_PR_CONFLICT.md | Evidencia y límites de esta corrección |
| docs/migration/beta-local/PROGRESS_TRACKER.md | Registro de entrega y puerta antes del siguiente bloque |

## Diagnóstico confirmado

- HEAD remoto inicial: c373cfdc9b4e82cdaa91d1d2c2509b245092ebc2, incorporado
  por avance rápido desde 8685ae7f, conservando el plan CI14 de otra entrega.
- Durante la comprobación previa llegó dcc8aafd75c39d57289516b54b31cbbfa4bf92d0
  (F1 de CI14). Se guardó y verificó la resolución propia fuera del checkout,
  se canceló solo el intento de integración propio, se incorporó F1 por avance
  rápido y se reaplicó la resolución. Base final de esta entrega: dcc8aafd.
  Ninguno de sus seis archivos de seguridad se revierte o sobrescribe.
- main: 1d0d26a860b58c5cb83a440b1b6c39c63f32b9aa; merge-base: 9bf3fd3d.
- Las ramas añadieron independientemente b01-compile-debug.yml. Git reprodujo
  CONFLICT (add/add); GitHub informó mergeable=false/dirty para PR1.
- La versión main tenía push a main y subida de APK Debug sin el gate B04;
  la versión de migración usa workflow_call y deja la entrega de APK a B04.
- El pipeline canónico de rama B06 filtra paths y no incluye docs/**. Por eso
  los commits documentales 8685ae7f/c373cfdc no iniciaron B01–B06. Sus controles
  CodeQL no eran compilación Android.
- android.yml escucha pull_request, pero los eventos de PR no lanzan ese
  workflow mientras el PR tenga conflictos. No se confirmó fallo Kotlin/Gradle.

## Resolución

Integrar en la rama de trabajo los tres commits existentes de main mediante un
commit de integración con ambos padres, resolviendo manualmente el archivo en
conflicto. No fusionar PR1 hacia main, hacer squash/rebase/force push ni modificar
main. Conservar historial de ambos lados y todos los cambios funcionales previos.

Para B01 se conserva la semántica de la rama de migración: workflow_call y
workflow_dispatch, versiones y comandos intactos, informes B01 y publicación de
APK exclusivamente después de verificación B04. Únicamente se aclara el comentario
de cabecera. AGENTS.md y SECURITY.md son byte por byte los de main.

La modificación de .github/workflows/b01-compile-debug.yml coincide con el filtro
.github/workflows/b0*.yml del pipeline canónico: el push de esta resolución debe
activarlo. No se abre el filtro a todos los cambios documentales ni se modifica
el comportamiento general de CI. Al quitar el conflicto también se desbloquea
el evento pull_request de android.yml; no se lanza además una ejecución manual
duplicada si el pipeline canónico ya arrancó.

## Revisión 1 — resolución, contratos y pruebas

Primera pasada del mismo agente: único conflicto esperado, etapa B01 conserva
workflow_call, B06 la invoca y no reaparece la subida de APK sin verificar. Un
contraste YAML de la versión resuelta contra HEAD confirma igualdad semántica;
los cambios de B01 son comentarios. Instrucciones de main byte-idénticas.

Comprobaciones locales ejecutadas sin Gradle/Android:

- unittest discover en .github/scripts: 54 pruebas correctas tras incorporar F1.
- unittest discover en .github/scripts/tests: 85 pruebas correctas.
- Total: 139 fixtures de utilidades CI, no pruebas Android de la app.
- git diff --cached --check y parsing YAML correctos.

## Revisión 2 — preservación y activación

Segunda pasada del mismo agente, no revisor independiente: comprobar manifiesto
de 5 rutas, ausencia de entradas sin resolver, archivo B01 incluido en el filtro
push de B06 y ausencia de cambios de app/Gradle/firmas/secretos/ABI/gates. Los tres
commits de main se conservan por ascendencia, sin cambiar main. Esta reparación
no implementa ni revierte la fase F1 de CI14 incorporada desde otra entrega.

### Inconsistencia adicional observada en F1

F1 restringe release_apk de B04 a main/tags, pero el gate final B04 todavía exige
RELEASE_APK_OUTCOME=success y la limpieza B06 exige tres artefactos, incluido
Release. Por inspección, en la rama feature la omisión intencionada de Release
chocará con esos gates si llega a esas etapas. Este diagnóstico es distinto del
add/add y no es un fallo de compilador. No se revierte la restricción de seguridad
ni se relajan gates silenciosamente; requiere reconciliar el contrato de entrega
antes de declarar verde el pipeline completo. B01 puede arrancar sin esa decisión.

## Puerta remota

Tras el push: verificar SHA remoto, PR fusionable y arranque del pipeline B01–B06
para ese SHA. No presentar queued/in_progress como compilación terminada ni
como siete pruebas Android aprobadas. No se ejecuta Gradle localmente. El nuevo
resultado completo de GitHub queda pendiente de su ejecución y de comunicación
por el usuario antes del siguiente bloque. En el aviso de entrega se informa el
SHA y enlace real de la ejecución; no se atribuye un verde anterior a la corrección.
