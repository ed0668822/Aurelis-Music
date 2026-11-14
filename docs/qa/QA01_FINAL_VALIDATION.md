# Validación final QA00/QA01

Estado: corrección y controles automatizados aprobados; arranque Android NO
VERIFICADO. No nuevos defectos reproducidos en estas comprobaciones. Sin cambios
nuevos de producción, funciones, efectos, dependencias ni workflow.

## CI y procedencia

[Build Release APK126](https://github.com/ed0668822/Aurelis-Music/actions/runs/42152992285)
terminó **Success**,10m23s, para2742913. CodeQL también terminó Success
(1m6s, run42152988508) para el mismo SHA. Git confirma que incluye c52d1dd y que
el cambio posterior es documental: ningún archivo app difiere. El PR1 conserva
«Aurelis Music Beta UI and components». No heredar un verde de otro SHA.

GitHub publica Aurelis Music-126 y compilation-report-126 con digests de archivo
de artefacto en QA01_FINAL_VALIDATION.json. Esos digests NO son el SHA del APK
interior. La API devuelve Forbidden y la descarga web del artefacto devolvió404:
procedencia del artefacto confirmada por run/commit/metadatos, inspección de sus
bytes pendiente. No se ha usado un espejo ni eludido restricciones.

Los APK locales debug/release conservan SHA081cff27…/ba55a60f… de QA01_EVIDENCE;
firma e integridad ZIP verificadas de nuevo. Fuente probada idéntica a c52d1dd
incluida en2742913, comprobada por hashes de cinco archivos y evidencia de build
previa. No afirmar identidad binaria entre los APK locales y CI (versionado/firma
pueden diferir). No recompilar por cambios exclusivamente documentales.

## Pruebas ejecutadas y límites

58 casos dirigidos,0 fallos/errores/omitidos; BUILD SUCCESSFUL in29s.
Incluyen conexión/medición/redimensionado/reattachment del nodo, programas AGSL
exactos, navegación, biblioteca, carpetas, preferencias y controles de reproducción.
El reproductor de pruebas es RecordingPlayer: valida comandos/estado/cola, no
reproduce ni decodifica un archivo musical real. Permisos y proveedores son
simulados por Robolectric; no se han pulsado diálogos reales de Android.
Las capturas generadas por estas pruebas son del árbol Robolectric, no del Redmi.

| Comprobación en Android instalado | Resultado y motivo |
|---|---|
| Instalación, apertura limpia, reinicio y reapertura | No ejecutadas: ningún dispositivo/emulador ADB |
| Navegación y permisos del sistema | Pendientes; solo comprobaciones Robolectric ejecutadas |
| Liquid Glass en GPU/OEM | Pendiente; compilar shaders y probar el ciclo no certifica la imagen/fps |
| Música local, decodificación y audio real | Pendientes; RecordingPlayer no sustituye Media3/dispositivo |
| Logcat y excepciones Android | No recopilados: ADB get-state informa no devices/emulators found |

Host x86_64 sin /dev/kvm, /dev/dri, emulador, QEMU ni imágenes/AVD instalados.
APK solo ARM64. No hay una vía Android ARM64 operativa comprobada en este entorno;
no se arranca un AVD x86_64 suponiendo traducción JNI compatible ni se cambia ABI.
No se provisionan dispositivos externos/servicios de pago ni se inicia QA02.
La ausencia de Logcat no significa que Android esté libre de excepciones.

## Revisión final y continuidad

Dos pasadas del mismo agente: CI/procedencia/regresiones; después límites de
pruebas, integridad de plan/Glass/ABI/identidad y consistencia de evidencia.
Hashes del plan maestro, bloques, código de cristal/shaders, Gradle, manifiesto,
LICENSE/NOTICE y workflow continúan idénticos a QA01. B03 y QA02 no iniciados.
La ejecución CI de una entrega documental posterior se distingue del run126
validado aquí; su resultado se consulta por su propio SHA antes del informe final.
