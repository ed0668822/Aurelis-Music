# AURELIS ANDROID AUTOMATED TESTING & QUALITY ASSURANCE

Plan complementario aprobado para QA00 y QA01. No sustituye, renumera ni ejecuta
bloques del plan [beta-local](../migration/beta-local/MASTER_PLAN.md).
Base b179fde1ae59692bdfcd2d9430e03a3ca6fee802, misma rama y PR1
«Aurelis Music Beta UI and components». B03 permanece sin iniciar.

| Unidad | Objetivo y dependencias | Aceptación |
|---|---|---|
| QA00 | Registro del plan, estado/capacidad y clasificación de evidencia; autorizado | Entorno comprobado y límites registrados, sin repetir auditoría completa |
| QA01 | Regresión de conexión/medición/lente, corrección mínima B02; autorizado | Prueba falla antes y pasa después; lente real activa tras medir; compilación/regresiones; arranque físico separado |
| QA02 | Instrumentación, instalación, arranque/reapertura, screenshot y Logcat | Android ARM64 autorizado; no éxito si el proceso se cierra; aún no autorizado |
| QA03 | Compose Test/UI Automator para funciones existentes y permisos | QA02 y fixtures autorizados; rutas disponibles, no implementar B03+; aún no autorizado |
| QA04 | Reproducción, segundo plano, letras/traducción y JNI/ONNX reales | QA02/03, datos/dispositivo adecuados; hardware y APIs separados; aún no autorizado |
| QA05 | Workflow adicional android-ui-tests.yml e informes/artefactos | Al menos una ejecución Android real y registros verificables; aún no autorizado |

## Infraestructura prevista

Compilar APK debug e instrumentación una vez en Actions y reutilizarlos en un
runner de confianza con Android ARM64 autorizado. El runner puede ser x86_64 si
su dispositivo ADB es ARM64: comprobar ro.product.cpu.abilist, instalación y carga
JNI, no deducir compatibilidad por etiqueta del host. No cambiar abiFilters ni
retirar bibliotecas. Dispositivos privados nunca ejecutan automáticamente código
no confiable de PRs/forks; definir aprobaciones/condiciones al implementar QA05.

Evaluar Emulator Runner/Gradle Managed Devices solo con imagen/host compatibles;
el emulador x86_64 habitual no acredita las bibliotecas ARM64. Esta máquina carece
de KVM/emulador/imágenes. No iniciar emulación costosa sin evaluar viabilidad.
Firebase Test Lab/dispositivos remotos son alternativas sujetas a acceso autorizado,
cuota/costo comprobado y permisos; no activar servicios de pago ni introducir
credenciales. La interfaz de escritorio de nube no equivale a un Android.

QA00/01 no crean todavía workflow, runner, servicios ni instrumentación: el permiso
vigente cubre registro y corrección de B02. Infraestructura preparada/documentada
no equivale a implementada: requiere QA02/05 y una ejecución Android real.

## Contrato de corrección y revisión

Evidencia del fallo → prueba de hipótesis → corrección mínima → prueba original →
regresiones. No eliminar Liquid Glass ni silenciar excepciones generales. Separar
riesgo estático, fallo reportado por usuario, reproducción automatizada y validación
física. Compilación, firma y shader compilado no prueban arranque Android/GPU.

Primera revisión: causa, lógica, compilación y prueba original. Segunda pasada:
regresiones, interfaz/efectos, licencias, límites y preservación del plan. Mismo
agente, no atribuirlas a revisores humanos independientes. Guardar informes con
SHA/comandos, casos y limitaciones. Preservar LICENSE/NOTICE, identidad, firma,
ABI y workflow de compilación; no merge, force push, cambio de rama ni título PR.

## Archivos y comprobaciones QA01

Producción: ui/components/backdrop/BackdropEffectScope.kt, effects/Lens.kt y
DrawBackdropModifier.kt solo si lo requiere la observación del primer dibujo.
Pruebas: BackdropSizeRegressionTest y BackdropAttachRegressionTest. Registros:
PROGRESS/ISSUES/ENVIRONMENT/QA01_EVIDENCE en este directorio, enlace desde el progreso
principal sin reescribir sus bloques. Sin dependencias nuevas ni cambios nativos.

Dirigidas: testDebugUnitTest con ambos RegressionTest y GlassRenderingTest.
Cierre: assembleDebug/assembleRelease + testDebugUnitTest/testReleaseUnitTest +
lintDebug/lintRelease, --no-daemon --max-workers=4. Instalar/abrir solo cuando
exista Android compatible; en caso contrario marcar no ejecutado. Revisión de
APK/firma/textos legales y CI del SHA publicado. No reutilizar un verde histórico.
