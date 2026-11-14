# CI14 F1 - Activacion de Release protegido

El flujo oficial esta preparado en la rama de trabajo, pero NO esta aprobado para publicar hasta configurar el environment release en GitHub.

1. Settings > Environments: crear release, exigir revisor y restringir main y etiquetas v*. Revisar politica prevent self-review.
2. Guardar KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS y KEY_PASSWORD como environment secrets. VIRUSTOTAL_API_KEY solo si se autoriza el uso manual.
3. Configurar en el environment las variables RELEASE_SIGNER_SHA256 con huella real del certificado (64 caracteres hexadecimales) y RELEASE_PROTECTION_CONFIRMED=true SOLO despues de revisar administrativamente la proteccion.
4. Migrar LISTEN_TOGETHER_SERVER a Actions variable. El valor es publico porque se incorpora al APK; retirar el secret antiguo cuando no se use.
5. Incorporar mediante revision el workflow y B06 en main: workflow_dispatch requiere archivo en rama predeterminada; no se hace merge automaticamente.
6. Obtener run B01-B06 verde del mismo SHA de main o tag v*, seleccionar PUBLICAR en despacho manual, decidir VirusTotal explicita e independientemente y aprobar el job desde environment release.

El flujo verifica misma identidad SHA de un run verde, certificado de firma aprobado, paquete de app, ABI ARM64, hashes y alineacion ZIP16. VirusTotal publico es opt-in. Sin aprobacion administrativa real no puede afirmarse que el flujo esta protegido.

Limite: el conector de GitHub no expone escritura/verificacion de environments, reviewers ni variables de Actions. El CI ordinario aun usa credenciales de verificacion de Release heredadas en repo. Moverlas exclusivamente a environment sin un reemplazo verificable romperia B04, por ello esa migracion se difiere a decision administrativa y tecnica especifica.