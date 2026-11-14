# CI17 — APK Release firmado descargable para pruebas

## Autorización y separación de entregas

El usuario pidió corregir la ausencia del APK firmado y autorizó preparar el
ajuste para entregarlo desde la rama de migración, sin publicación oficial.
Antes de commit/push o workflow se presenta el diff y se solicita autorización.
No se inicia otro bloque de desarrollo mientras se resuelve esta entrega.

Base: 780fe6e3b054d1a7f52ee0864132684f817da67b.
Rama/PR1: feature/bitchord-local-migration / Aurelis Music Beta UI and components.
Los tres documentos CI16 preexistentes permanecen intactos y pendientes de su
propia autorización. No se mezclarán con esta corrección en un commit/entrega.
Su preservación se verifica mediante hashes contra el manifiesto previo.

Manifiesto CI17, dificultad alta, máximo6 rutas distintas:

1. .github/workflows/b04-verify-apks.yml
2. .github/workflows/b06-unified-pipeline.yml
3. .github/scripts/b06_bundle_reports.py
4. .github/scripts/test_b06_delivery_contract.py
5. .github/scripts/test_virustotal_scan.py
6. docs/qa/CI17_SIGNED_RELEASE_CANDIDATE.md

## Causa confirmada y comportamiento nuevo

El ZIP del run109 aportado por el usuario coincide con el digest GitHub y registra
Release1.9.103 construido, firmado con el certificado configurado y verificado.
Pero release_publish_expected=false/expected_final_artifacts=2 impedía entregarlo
como descargable. El ZIP de informes no contiene ni permite reconstruir ese APK.

Solo push/workflow_dispatch sobre refs/heads/feature/bitchord-local-migration
esperarán ahora tres artefactos Actions:

- Aurelis-Informes-B01-B06-N: informes y VirusTotal advisory dentro del ZIP.
- Aurelis-Pruebas-ARM64-N: APK Debug verificado.
- Aurelis-Release-ARM64-N: APK Release firmado verificado, candidato de pruebas.

N es el run nuevo. Se conserva dentro el nombre real del APK leído/verificado
por B04, sin renombrar su versión, alterar sus bytes o volver a firmarlo. No se
presenta un build nuevo como recuperación del binario exacto del run109.

Otras referencias/eventos conservan el contrato de dos artefactos en el pipeline
ordinario. La publicación oficial sigue en b04-protected-release.yml: confirmación
PUBLICAR, main/tags v* y environment release. Ese archivo no se modifica. No hay
GitHub Release, tag, merge a main, cambios de claves, permisos o entorno protegido.

## Gates conservados y coherencia de entrega

B04 sube el candidato solo si stage_apks, evidence y report terminaron success y
el job no se canceló. El APK procede únicamente de ci-b04-delivery/release,
previamente validado contra manifest, tamaño, hash, firma configurada, identidad,
ARM64 y alineación. Sin clave Release configurada no se sustituye por Debug.

El gate final exige success de esa subida cuando se espera el candidato. Si
corresponde omitirlo, exige skipped; política ausente/inválida y resultados
failure/skipped/cancelled/missing donde se esperaba éxito fallan. No se desactiva
ningún gate de compilación, pruebas, integridad o evidencia.

La expresión de alcance de B04 y las dos políticas B06 (informes y limpieza)
coinciden. La limpieza existente ya soporta tres artefactos y conserva diagnósticos
si falta alguno, hay un extra inesperado o B06 no aprobó; no requirió cambios.
pipeline-results.json añade release_artifact_purpose=SIGNED_TESTING_CANDIDATE y
official_release_published=false para no confundir descarga con publicación oficial.
La aprobación de producto/audio físico permanece independiente.

VirusTotal sigue hash-only con VIRUSTOTAL_ALLOW_UPLOAD=false, sin habilitar una
subida pública por variable del repositorio. El fixture anterior mezclaba esa
restricción con una prohibición global del nombre del APK Release; se acota al
paso de VirusTotal, conservando sus protecciones y las pruebas de red negativas.

## Validación local y revisiones

- Suite de contrato de entrega: 14 métodos correctos, con casos positivos y
  negativos de los scripts Python inline reales del workflow B04.
- Suites completas .github/scripts: 129 pruebas correctas.
- Suites completas .github/scripts/tests: 86 pruebas correctas.
- Total completo:215 (los14 dirigidos están incluidos, no se suman otra vez).
- Fixtures sintéticos, no APK reales, certificados reales ni compilación Android.
- Workflows: actionlint oficial1.7.12, archivo verificado por SHA-256 fijado en
  f5_workflow_audit.sh, sin desactivar TLS/checksums.

Revisión1 del mismo agente: alcance de referencia/evento, upload desde staging,
firma/hash/ABI y gate que falla ante ausencia de candidato. Las pruebas ejecutan
los guards B04 con archivos sintéticos y verifican copia sin cambios de bytes.
Revisión2 del mismo agente: presupuesto6, hashes de los3 documentos CI16,
workflow oficial intacto, política B06 coherente, distinción candidato/oficial y
VirusTotal hash-only. No son revisores humanos independientes.

No Gradle, APK ni pruebas Android locales. No se han creado commits, publicado
cambios o lanzado workflows en esta preparación. La validación Android y los
descargables nuevos requieren publicar el cambio y ejecutar GitHub con autorización.
No se declara la entrega remota resuelta antes de observar esos resultados.
