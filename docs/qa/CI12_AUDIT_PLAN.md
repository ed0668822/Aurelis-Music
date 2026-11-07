# AURELIS ANDROID AUTOMATED TESTING & QUALITY ASSURANCE — CI12

Extensión autorizada del CI, sin sustituir el plan B00–B13 ni avanzar B09.
Misma rama feature/bitchord-local-migration y PR1. Compilación y suites Android
exclusivamente GitHub. Mantener autorización para las tres entregas; entre ellas
publicar, avisar y esperar el resultado comunicado por el usuario.

| Entrega | Alcance | Estado |
|---|---|---|
| CI12.1 | Inventario completo, entradas/raíces/resultados Gradle, reporte por variante y nombres de APK | CI150 SUCCESS; no Android real |
| CI12.2 | Presencia en APK: clases/DEX/R8, recursos, assets/modelos y bibliotecas nativas | Implementado; APK nuevos/GitHub pendientes |
| CI12.3 | Matriz funcional, conexiones de navegación y pruebas de interacción | Autorizado, pendiente tras CI12.2; Android real según disponibilidad |

## Reglas de evidencia

Inventario y convenciones de rutas no prueban selección por variante. Una entrada
de tarea tampoco prueba compilación individual. Java puede ser referencia de
Kotlin; headers no son unidades independientes. Las tareas reutilizadas conservan
su estado de caché, sin fingir recompilación. Resultados terminales contradictorios
son error del recolector y bloquean aprobación técnica, no se ocultan como éxito.
Un fallo de tarea no atribuye un error de código a todos sus archivos.

Primera entrega en modo observación: desconocidos no se convierten en faltantes
obligatorios ni en aprobados. Los gates existentes continúan vigentes. Después de
revisar evidencias reales se podrán hacer obligatorias las comprobaciones cuyo
resultado esperado esté acreditado. No usar reglas keep adicionales para aparentar
integración ni desactivar comprobaciones existentes.

## CI12.1 — criterios y límites

- Inventariar todos los archivos versionados; clasificar fuente administrada,
  fuente/header/CMake nativos, recursos, assets, .so, pruebas, configuración,
  avisos legales y documentación. Conservar entradas faltantes/symlink sin leer
  su destino; no leer/hash de configuración sensible conocida.
- Capturar raíces declaradas Android/Kotlin y entradas efectivas de tareas
  observadas, incluidos directorios que contengan archivos versionados. No
  recorrer recursivamente cachés externas o directorios generados completos.
- Registrar afterTask SUCCESS/FAILED/FROM-CACHE/UP-TO-DATE/SKIPPED/NO-SOURCE.
  Una línea de inicio sin resultado terminal conserva estado desconocido.
- compilation-audit.json/.tsv: fila por archivo y variante debug/release, alcance,
  expectativa, evidencia de tareas, estado APK e integración UI. Resultados
  desconocidos NOT_AUDITED; pruebas/documentación no esperadas en APK por política.
- Publicar el APK release como Aurelis-Music-V<versionName>.apk y debug como
  Aurelis-Music-V<versionName>-debug.apk. Versión leída del APK por aapt2 después de
  validar identidad. Mantener build_file original en manifiesto de entrega.
  Copiar bytes sin modificar/reempaquetar/firmar otra vez. Actualizar manifiesto,
  SHA256SUMS y resumen. Colisiones o versión insegura bloquean publicación.
- Conservar app, Gradle, workflows, ABI, firmas, modelos, Glass, servicio y plan.
  Usar las mismas compilaciones/artefactos/tareas, sin una compilación extra.

## Continuación CI12.2 y CI12.3

CI12.2 contrastará outputs de compilador con DEX y transformaciones R8 sin exigir
una clase por .kt ni comparación literal de XML compilado. Recursos mediante
tabla AAPT, assets/modelos por contenido, nativos mediante artefactos/ABI/ELF y
unidades CMake acreditadas. Distinguir exclusiones previstas (p.ej. ABI x86_64
fuera de la distribución ARM64 actual) y evidencias incompletas. Guardar metadatos
por source/checkout SHA y APK SHA256. Esta capacidad se implementa en CI12.2; ver CI12_2_APK_CONTENT.md/.json.

CI12.3 mantendrá matriz función → entrada activa → acción → prueba. Búsqueda
estática solo como indicio; pruebas de interacción para confirmar comportamiento.
No declarar conectado lo que solo existe en DEX. Reutilizar suites existentes;
emulador/dispositivo ARM64, audio/GPU/hardware y servicios autorizados permanecen
pendientes cuando no estén disponibles. No activar servicios de pago ni cambiar
ABIs para aparentar compatibilidad.

La auditoría se valida con fixtures de faltantes, exclusiones, caché y resultados
contradictorios. Dos revisiones separadas por entrega; indicar si son del mismo
agente. Gradle/Android/GitHub pendientes no se presentan como pasados por Python.
