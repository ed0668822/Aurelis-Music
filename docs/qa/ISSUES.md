# Incidencias QA

## QA-001 — Size is unspecified en Liquid Glass

Estado: reproducido en Robolectric nativo SDK35 y corregido en código;
validación física pendiente. Reportado por el usuario en Redmi Note14
Pro5G; Android/HyperOS y SHA/APK exactos del teléfono no proporcionados. No atribuir
el fallo al fabricante ni asumir que se ha reproducido en un dispositivo de nube.

Registro proporcionado (fragmento, no generado por las pruebas):

```text
java.lang.IllegalStateException: Size is unspecified
  LensKt.getCornerRadii
  LensKt.lens
  LiquidGlassKt.liquidGlass
  BackdropEffectScopeImpl.apply
  DrawBackdropNode.updateEffects
  DrawBackdropNode.observeEffects
  DrawBackdropNode.onAttach
```

Código observado: BackdropEffectScopeImpl inicia size en Size.Unspecified;
onAttach llama observeEffects/updateEffects/apply; lens consulta minDimension
antes de la primera actualización desde DrawScope. Hipótesis a verificar: el
efecto se ejecuta antes de disponer de dimensiones válidas. La prueba nueva debe
reproducir esa misma ruta y verificar la lente después de medir, sin desactivarla.

## QA-002 — ejecución Android no disponible

Entorno x86_64, APK ARM64, sin KVM/emulador/imágenes ni dispositivos ADB.
Instalación/arranque Android pendientes; no inferir éxito de Robolectric.

## Riesgos heredados, sin nueva corrección autorizada aquí

Dos ELF ONNX4KB, procedencia de modelos/binarios y pruebas GPU/SAF/TalkBack/hardware:
ver B02_CLOSEOUT y auditoría B00. No son nuevas regresiones reproducidas por QA01;
se conservan para sus bloques originales B09/B13 sin cambiar ABI/dependencias.


### Evidencia red/green QA-001

QA01_RED.json:7 casos,5 fallos con Size is unspecified, incluidos el nodo Compose
real y la cadena original onAttach/apply/lens. Dos casos válidos ya pasaban antes.
QA01_GREEN.json:9 casos dirigidos correctos tras la corrección, incluidos ambos
RegressionTest y GlassRenderingTest con las cuatro fuentes AGSL exactas.

Cambio mínimo: apply limpia efectos previos y espera tamaño especificado, finito y
positivo; lens comprueba también dimensiones antes de radios/uniformes; el primer
dibujo medido registra las lecturas de estado de efectos mediante observeEffects.
No se desactiva Glass, no se cambian shaders ni se capturan excepciones generales.
Pruebas positivas conservan RenderEffect real después de medir/redimensionar/reset.
Esto verifica el defecto en el entorno de prueba; no equivale al arranque del APK
en el Redmi ni a medir fps/corrección gráfica en su GPU.
