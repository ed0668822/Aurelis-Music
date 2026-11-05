# Estado de compilación e integración

## Línea base verificable

Commit destino validado en onboarding: `9bf3fd3db8f5870f942f5167dca5bc77e3c48edd`.
Los resultados siguientes corresponden a esa línea base, no a un nuevo build del
commit documental B01 ni a GitHub Actions.

| Comprobación | Resultado | Evidencia / límite |
|---|---|---|
| Debug | Correcto | `assembleDebug`; incluye compilación Kotlin/Java y C++ ARM64 |
| Lint | Correcto con 209 advertencias, sin errores | XML de lint del onboarding |
| Unitarias | `NO-SOURCE` | No se ejecutaron casos; no equivale a pruebas aprobadas |
| APK | Firma v2, integridad ZIP y tres bibliotecas Aurelis ARM64 verificadas | No demuestra reproducción funcional |
| Reinstalación | Script reutilizable terminó correctamente | Dependencias existentes reconocidas |
| Dispositivo | No ejecutado | Pendientes reproducción, permisos, interfaz, notificación y bloqueo |
| Release | No ejecutado en onboarding | No afirmar publicación/firma release verificadas |
| BitChord | No compilado ni ejecutado | Solo inspección de referencia en lectura |
| Actions / permisos API | Bloqueado: `Forbidden` | Lectura Git funciona; acceso API no confirmado |
| Entrega Git B01 | Rama publicada | Push confirmado; consultar PROGRESS para commit |
| Creación PR B01 | Bloqueada | `gh pr create` recibió Forbidden de api.github.com/graphql; no hay PR confirmado |

Comando final del onboarding (exit 0, `BUILD SUCCESSFUL in 13s`, 52 tareas):

```bash
cd /workspace/Aurelis-Music
source /workspace/toolchains/activate.sh
bash gradlew assembleDebug lintDebug testDebugUnitTest --no-daemon --max-workers=4
```

Evidencia local retenida, no requisito para clonar el repositorio:
`/workspace/toolchains/validation-final.log`, `/workspace/toolchains/reinstall.log`,
`app/build/reports/lint-results-debug.xml` y
`app/build/outputs/apk/debug/app-debug.apk` (117781656 bytes al verificarlo).
La primera validación se solapó con otro build y falló al empaquetar por un
intermedio DEX ausente. Ambas ejecuciones terminaron; la repetición secuencial fue
correcta. Ejecutar Gradle secuencialmente en este checkout.

## Entorno reutilizable

- JDK Temurin 17.0.17; la imagen original solo aportaba runtime Java 21, sin javac.
- Gradle wrapper 8.11.1; invocar con `bash gradlew`, sin cambiar el modo del archivo.
- SDK 36, build-tools 35.0.0, NDK 26.1.10909125 y CMake 3.22.1.
- Activación local: `/workspace/toolchains/activate.sh`. Conserva proxy y truststore
  del sistema con verificación TLS; no contiene credenciales.
- `install_script` y `start_skill` se guardaron en el borrador durante onboarding.
  Eso no demuestra publicación ni restauración en una nueva tarea.
- No hay servicio de desarrollo que deba permanecer vivo para compilar.
- La ejecución interactiva requiere Android API 33+ y ARM64 compatible.

## CI: riesgos observados, sin cambios B01

`.github/workflows/android.yml` instala explícitamente plataforma 35 mientras el
módulo compila con 36. Podría autoinstalarse la requerida; no se observó una ejecución
remota que lo confirme. Lint y unitarias usan `continue-on-error`; no inferir que un
workflow verde demuestra que ambas pasaron. Release tiene un paso final que marca
fallo si falló la compilación. No modificar el workflow para ocultar resultados.

## Validación B01

Requerida: rutas y enlaces de los documentos, consistencia de SHAs y estados,
`git diff --check` y diff limitado a los cinco Markdown. No cambia Gradle, recursos,
código ni dependencias: no se repite un build completo sin justificación.
Resultados y comprobaciones de entrega se registran en PROGRESS.

## B02 — Validación de arquitectura base

Cambios respecto a `e6f58f937a1f84166236705c7f8b9a3a2947f571`;
consultar PROGRESS para commits de implementación y entrega. La base histórica
NO-SOURCE anterior permanece documentada; B02 sí introduce y ejecuta pruebas.

```bash
cd /workspace/Aurelis-Music
source /workspace/toolchains/activate.sh
bash gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=4
# Tras añadir la prueba de una canción local (solo cambió código de test):
bash gradlew testDebugUnitTest lintDebug --no-daemon --max-workers=4
```

- Primera ejecución: exit 0, BUILD SUCCESSFUL en 2m 17s, debug generado y 10 pruebas correctas.
- Ejecución final: exit 0, BUILD SUCCESSFUL en 27s; 11 pruebas, 0 fallos,
  0 errores y 0 omitidas. ProfileStorageTest: 5; LibraryViewModelTest: 6.
- Lint: sin errores, 209 advertencias (misma cantidad que la línea base).
- Pruebas Robolectric/API 33: conservación de preferencias y claves desconocidas,
  inicialización repetida, aislamiento entre cinco áreas y tres perfiles, ID estable,
  perfil no heredado, avatar y arranque de Application; permiso denegado/reintento,
  carga y reescaneo, canción local/grupos/búsqueda, cancelación/error y constructor Android.
- Informes: `app/build/test-results/testDebugUnitTest/TEST-*.xml`,
  `app/build/reports/tests/testDebugUnitTest/index.html` y XML de lint.
- Registros de esta instancia: `/workspace/toolchains/b02-validation.log` y
  `/workspace/toolchains/b02-validation-final.log`.
- Se probaron SharedPreferences Android bajo Robolectric y reapertura de ámbitos;
  no se simula un reinicio real de proceso/dispositivo ni ejecución física de MediaStore.
  Perfil activo múltiple, listas/historial funcionales, reproducción en dispositivo,
  release y CI remoto permanecen fuera de estos resultados.
