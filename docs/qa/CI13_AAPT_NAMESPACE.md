# CI13 — parser de namespace AAPT2

Base/fallo e3609356500a550c11f0acfae48c3b622a755ee8, misma rama/PR1.
Run151 FAILURE: https://github.com/ed0668822/Aurelis-Music/actions/runs/42328186956.
Job128268304312 enlaza ese SHA. Gate público: verify=failure, upload_apk=skipped;
compilación/lint/tests/build no figuran entre resultados fallidos/omitidos. La
anotación original dice APK content audit EVIDENCE_ERROR; ver apk-content-debug.json.
CodeQL1320 SUCCESS. Advertencias de versiones/ABI/plurales no son el fallo mostrado.

## Acceso y causa reproducida

Anotaciones públicas/commit leídos; descarga compilation-report-151 por gh devuelve
Forbidden en la API. No se leyó apk-content-debug.json ni se afirma conocer su
contenido o descartar otro error anterior del auditor. Se identificó por
reproducción un defecto concreto que produce EVIDENCE_ERROR en esta ruta.

AAPT2 real build-tools35.0.0, leyendo AndroidManifest.xml de android-36/android.jar
existente, imprime atributos como
A: http://schemas.android.com/apk/res/android:name(0x01010003)="…".
El parser esperaba literalmente android:name. Resultado ANTES:0 componentes;
abreviando solo ese namespace en la misma salida:84 componentes. Esto reproduce
el rechazo No parseable merged manifest component evidence sin compilar Android.
Los fixtures anteriores solo cubrían la forma abreviada y no detectaban el defecto.

## Corrección y verificación

Parser ahora acepta URI completa y alias declarados del namespace Android, conserva
compatibilidad android:name y rechaza atributos de namespaces distintos. Mantiene
lista de tipos de componente y resolución de nombres relativos. Requisitos DEX/
manifiesto/recursos/modelos/fuente/nativos, política y gates sin cambios.
Fixture central actualizado al formato URI real; dos métodos nuevos cubren formas,
alias, casos negativos y exposición segura de la causa/report en la excepción.
Ahora el error público incluye tipo/motivo seguro o primeros faltantes obligatorios,
además del JSON; no imprime stdout/stderr, constantes ni contenidos del APK.

66 métodos Python PASAN (64 anteriores retenidos,2 nuevos). Parser corregido sobre
la misma salida SDK real:84 componentes, conjuntos idénticos para URI/abreviatura.
Primera revisión: namespace/causa/reproducción. Segunda revisión estática separada
del mismo agente: negativos/regresiones/diagnósticos/alcance. No revisores distintos.
Diff --check correcto. App/Glass/plan/ABI/firmas/Gradle/workflow/política intactos.

Gradle, APK Aurelis, instalación y arranque locales NO EJECUTADOS por instrucción
GitHub exclusivo. Nueva ejecución debe confirmar la corrección y mostrar si existe
otro fallo; no declarar CI12.2 validado con esta reproducción ni con fixtures.
Publicar en mismo PR, avisar y esperar resultado del usuario. No CI12.3 ni B09.
