# LICENSE_AUDIT — beta local

## Alcance de B00

Auditoría documental/estática y reutilización de la evidencia histórica en
[LICENSE_AUDIT anterior](../LICENSE_AUDIT.md) y
[DEPENDENCIES_RELEASE.tsv anterior](../DEPENDENCIES_RELEASE.tsv). No certificación
legal, auditoría de vulnerabilidades ni revisión semántica de los1.396 archivos.
No se cambia LICENSE ni NOTICE en B00: no se incorpora código de terceros nuevo.
NOTICE sí deberá actualizarse cuando cada componente se adapte de verdad.

| Elemento | Evidencia | Tratamiento / bloqueo |
|---|---|---|
| Aurelis | LICENSE GPLv3 y NOTICE3-or-later | Mantener GPL-3.0-or-later; copyright propio solo en aportes propios |
| BitChord | LICENSE y avisos seleccionados | Conservar autor, ref fija y modificaciones por archivo |
| Orchard | TrackAnalysis/TransitionPolicy explican AGPL3-or-later y combinación GPL | No relabelar; conservar obligaciones AGPL y fuentes |
| FFmpeg/NextLib | Auditoría previa POM NextLib GPLv3; configuración completa del binario no acreditada | No asumir todo LGPL; acreditar fuente correspondiente/versiones/build antes de nueva distribución final |
| DST | COPYING.LGPL-2.1 y NOTICE upstream | Conservar textos y condiciones |
| Mornye bit-perfect | README nativo origen declara LGPL-3.0-only, copyright zarzet2026 | Examinar cabeceras por archivo y obligaciones de combinación/distribución |
| libusb | Origen1.0.27 LGPL2.1-or-later | Fuentes, textos y enlace/distribución verificables |
| Oboe | Origen1.11.0 Apache2.0 | Pin/checksum, licencia y avisos aplicables |
| Signalsmith | stretch1.3.2/linear0.6.6, MIT | Textos íntegros y copyright |
| ONNX Runtime | Actual1.19.2/origen1.28.0, avisos MIT | Comprobar runtime/modelos y avisos de terceros |
| Modelos ONNX | SHA previos y archivos existentes; exportación exacta pendiente | Bloquea declarar análisis/redistribución final completos; nombre no certifica permiso |
| SF Pro y otras fuentes restringidas | No hay autorización de redistribución acreditada | No copiar; conservar fuentes autorizadas |
| Vazirmatn | OFL-Vazirmatn.txt presente en origen | Mantener OFL/autoría al incorporar |
| Iconos/imágenes | Condiciones por recurso | Revisar fuente real, marcas y permiso; usar identidad Aurelis |
| APIs/letras | Licencia del cliente no concede derechos de contenidos/API | Verificar individualmente antes de habilitar proveedor |

## Procedimiento por port

Registrar origen/ruta/ref, autores, licencia/SPDX, modificaciones y dependencias;
retener cabeceras originales y añadir solo atribución propia aplicable. Actualizar
NOTICE/LICENSES y bundle legal, comparar textos empaquetados. No escribir comentarios
en formatos que no los admiten. Preservar LICENSE y configuración de firma.

El inventario de dependencias identifica metadatos POM/BOM/constraints, no cantidad
de binarios dentro del APK. No sustituye auditoría del contenido enlazado.
«Formato de licencias.csv» sigue pendiente para el formato definitivo de cabeceras
según la instrucción anterior; no impide conservar avisos ni documentar el plan.

## Puertas de cierre

No declarar cumplimiento completo ni beta distribuible hasta acreditar modelos,
fuentes de binarios y condiciones de componentes nuevos. Una incidencia local de
16KB se registra en B00 y se corrige en B09/B13; no se esconde mediante avisos.
Servicios opcionales sin permisos/credenciales permanecen indisponibles honestamente,
sin activar clientes legacy. Last.fm no autoriza embeber un secreto en el APK.


Hallazgo estático B00: ORT1.19.2 incluye dos ELF con LOAD4KB. Ver B00_EVIDENCE.
La solución de compatibilidad deberá conservar licencias y acreditar modelos/ABI;
ZIP alineado no certifica ELF.175 dependencias sin cambios vs auditoría histórica.
