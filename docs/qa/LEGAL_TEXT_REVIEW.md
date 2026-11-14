# LEGAL01 — revisión de textos legales antes de B04

Fecha: 2026-11-06. Base: f19fcf6099e3672e171394f04f573de581d36bef.
Rama existente: feature/bitchord-local-migration.
PR existente: https://github.com/ed0668822/Aurelis-Music/pull/1.

## Alcance

Mejora autorizada de NOTICE y de la explicación de las licencias, sin cambiar
sus condiciones: índice y presentación, guía en español LICENSES/README.md,
rutas reales y aclaración de permisos y obligaciones. LICENSE oficial íntegro.
Se actualizan el registro QA y la auditoría vigente; no se altera el plan principal.

## Revisión 1 — preservación y trazabilidad

Pasada documental/estática completada. Comparación byte por byte contra la base:
LICENSE y los ocho textos existentes en LICENSES/ sin cambios. SHA-256 de LICENSE:
`3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986`.
Los bloques originales de Kyant0/backdrop, liquid_glass_easy y modelos ONNX
permanecen íntegros. Se conservan las líneas originales de copyright, el hash
de IosSwitch y el commit de referencia. Las ocho rutas nativas y los enlaces
locales de la guía existen. git diff --check sin incidencias.

## Revisión 2 — contenido e integración

Segunda pasada del mismo agente; no se presenta como revisión de otro ingeniero.
README/LICENSE leídos directamente del archivo de referencia de BitChord
b690dc146009d4815825fa4a20815ef2e4ee8898: GPLv3, sin presumir «o posterior».
Resumen de combinación contrastado con LICENSE, sección 13, y
LICENSES/AGPL-3.0-or-later.txt, sección 13. Se conservan la licencia propia
GPL-3.0-or-later y las obligaciones distintas de las partes de terceros.

Inspección de app/build.gradle.kts: bundleLicenseTexts copia también el nuevo
README a assets/licenses/. La verificación existente de android_ci.py enumera
LICENSES/ dinámicamente y compara bytes de cada entrada, incluida la nueva guía.
No se cambia Gradle ni el workflow. Los enlaces Markdown se documentan como
enlaces del repositorio, dado que el APK reúne los textos en un solo directorio.

Aplicación, configuración Gradle, ABI, firma, workflow y plan maestro sin cambios
respecto a la base. Ningún código, dependencia ni recurso nuevo de terceros.

## Límites y continuidad

No se ejecutaron Gradle, compilación Android ni pruebas de dispositivo localmente.
Empaquetado efectivo y comparación de textos en los nuevos APK: pendientes de
la compilación en GitHub. No se consultará su resultado automáticamente: esperar
el resultado que comunique el usuario. La revisión documental no certifica
cumplimiento jurídico completo ni funcionamiento Android.

Fuentes/configuración exacta de FFmpeg/NextLib y procedencia de exportaciones
ONNX siguen pendientes, así como el CSV para cabeceras definitivas. No se
eliminan esas limitaciones ni se aplican nuevas cabeceras definitivas.
B04 y QA02 no iniciados; conservar el plan y esperar autorización.
