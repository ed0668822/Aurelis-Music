# Auditoría inicial de licencias

## Evidencia y límites

Destino `9bf3fd3db8f5870f942f5167dca5bc77e3c48edd`; origen
`b690dc146009d4815825fa4a20815ef2e4ee8898`. Se leyeron el LICENSE de BitChord,
su declaración GPLv3 en README, cabeceras seleccionadas y el NOTICE de Aurelis.
Esto no equivale a una certificación legal ni a una auditoría exhaustiva de dependencias.

Identidad solicitada para trabajo propio: Aurelis © 2026. GPL-3.0-or-later.
No se encontró `Formato de licencias.csv` en el workspace durante la auditoría.
**El usuario debe incorporarlo antes de aplicar cabeceras definitivas.** B01 deja
los documentos sin cabeceras legales definitivas y no modifica avisos existentes.

| Elemento | Evidencia | Tratamiento |
|---|---|---|
| Aurelis | LICENSE GPLv3; NOTICE declara versión 3 o posterior y adaptaciones de ed0668822 | Conservar LICENSE/NOTICE y atribuir únicamente aportes propios |
| BitChord general | LICENSE GPLv3; README declara GPLv3; NOTICE destino declara BitChord GPL-3.0-or-later | Reutilización candidata bajo GPLv3; comprobar permiso «o posterior» y excepciones por archivo antes de etiquetar adaptaciones |
| AutoMix Android compartido | TrackAnalysis, TransitionPolicy y TrackFeatures origen declaran copyright SFG545 y Kushagra Singh, procedencia Orchard y AGPLv3 o posterior | No relabelar como GPL exclusivo. Registrar textos y obligaciones AGPL, y revisar combinación/distribución antes de nuevas copias |
| DSP ya incluido en Aurelis | NOTICE, sección Orchard, enumera analizadores y JNI bajo AGPL-3.0-or-later | Preservar procedencia; contrastar lista con rutas reales `app/src/main/cpp/` en revisión específica. NOTICE por sí solo no demuestra cumplimiento completo |
| AutoMix web BitChord | `web/app/src/playback/automix/LICENSE` contiene AGPLv3 | Fuera de la migración Android; no copiar |
| GlassNavBar origen | Cabecera atribuye estructura a EchoMusicApp/Echo-Music GPL-3.0 y FloatingTabBar vendorizado | Conservar avisos y rastrear la licencia del componente incluido antes de adaptarlo |
| Signalsmith | Licencias MIT en `native/third_party/signalsmith-linear/LICENSE.txt` y `signalsmith-stretch/LICENSE.txt` | Preservar textos/copyright si se incorpora; su existencia no obliga a importar esas dependencias |
| Fuentes SF Pro e iconos/modelos | Theme origen referencia SF Pro; no se verificó autorización de redistribución de todos los recursos | No copiar recursos no aclarados. Evaluar alternativas con licencia explícita en B03 sin prometer fidelidad tipográfica idéntica |
| Proveedores de letras/traducción | Código contiene endpoints de terceros | Licencia del cliente no concede derechos sobre contenidos/API. Revisar condiciones, privacidad y permisos del servicio antes de habilitar nuevas integraciones |

## Política de reutilización

GPLv3 y AGPLv3 permiten ciertas combinaciones según sus cláusulas; no concluir que
AGPL es automáticamente incompatible ni que todo componente se convierte en GPL.
Documentar obligaciones concretas de las partes combinadas, suministro de fuentes,
avisos y, cuando aplique, interacción remota. La interpretación de NOTICE existente
debe comprobarse, no copiarse como conclusión jurídica.

Antes de cada adaptación, registrar archivo y commit original, autores, licencia,
dependencias vendorizadas, modificaciones y avisos que deben acompañar la distribución.
Si no se pueden establecer condiciones compatibles con la distribución prevista,
no copiar ese componente; dejar pendiente o especificar una alternativa independiente.
Nunca eliminar atribuciones ni atribuir a Aurelis código de terceros.
No añadir comentarios a formatos que no los permiten ni cabeceras que invaliden XML.

## Pendientes explícitos

1. Incorporar el CSV para fijar el formato de cabeceras; no es necesario para conservar este inventario.
2. Rastrear el alcance real de código Orchard ya incluido y los avisos por archivo.
3. Verificar autorización de fuentes, iconos, imágenes, modelos ONNX y sus datos asociados antes de importarlos.
4. Auditar licencias de dependencias nuevas por bloque; B01 no añade ninguna.
5. Conservar la identidad GPL-3.0-or-later del trabajo propio sin borrar obligaciones diferentes de terceros.
