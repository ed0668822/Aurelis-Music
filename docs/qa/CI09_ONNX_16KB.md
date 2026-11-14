# CI09 — ONNX y16KB, entrega2 autorizada

Base `a3229f07512f6e98c0a60e7ce284fbbcbfeda651`; misma rama/PR1 «Aurelis Music Beta UI and components».
Alcance autorizado: corregir compatibilidad nativa de ONNX y comprobar modelos.
No actualización general de AndroidX/Gradle/SDK, no cambio de ABI y no B07.

## Cambio y evidencia binaria

Se fija `com.microsoft.onnxruntime:onnxruntime-android:1.22.0`, publicado en Maven
Central. Se inspeccionó el AAR completo oficial (28515295 bytes, SHA256 en JSON),
no solo un anuncio de soporte. MinSdk del AAR24, compatible con minSdk33 de Aurelis.

| Biblioteca ARM64 | Runtime anterior1.19.2 | Runtime1.22.0 |
|---|---|---|
|libonnxruntime.so|LOAD4096, rechazado|Tres LOAD16384, aceptado|
|libonnxruntime4j_jni.so|LOAD4096, rechazado|Dos LOAD16384, aceptado|

La nueva función de verificación se ejecutó sobre ambos AAR reales, además de
fixtures: rechaza las dos bibliotecas antiguas y acepta las nuevas. Son controles
binarios en host, no carga del código Android ni un APK nuevo compilado aquí.

El AAR no contiene reglas consumer de ProGuard. Su classes.jar usa `ai.onnxruntime`;
la regla anterior apuntaba a `com.microsoft.onnxruntime`. Se corrige esa única
regla keep para conservar los enlaces Java/JNI en release.

## Protección permanente en GitHub

El paso verify existente valida ZIP con `zipalign -c -P 16 -v 4` y todos los ELF
ARM64 del APK mediante sus encabezados de programa PT_LOAD. Comprueba límites,
alineación potencia de2>=16384 y congruencia offset/dirección virtual. Rechaza
bibliotecas corruptas, segmentos4KB o la ausencia de alguno de los dos componentes
nativos ONNX. ZIP alineado no basta para aprobar un ELF incorrecto.

El JSON de APK incorpora SHA256 y segmentos por biblioteca y flags separados de
ZIP/ELF16KB. Un fallo mantiene bloqueada la publicación y elimina conjuntos
parciales/antiguos. No cambios en workflows, gates, número de builds ni credenciales.
Se conserva ARM64 y todas las bibliotecas: no se quita ONNX para pasar el control.

## Modelos — hallazgo previo, pendiente

Modelos originales sin modificar: beat_this_int8.onnx (4533252 bytes) y
vocals_umxhq_int8.onnx (9054206 bytes), mismos hashes que B00. Ambos pasan el
checker protobuf ONNX1.17.0: IR8/opset17; voces añade dominio com.microsoft1.

Se crearon sesiones con CPUExecutionProvider en entornos Python aislados y un
hilo por runtime, usando exactamente los mismos archivos:

| Modelo | CPU host1.19.2 | CPU host1.22.0 |
|---|---|---|
|Voces|Carga PASS|Carga PASS|
|Beat This|Carga FAIL, ConvInteger(10)|Mismo FAIL, mismo nodo/error|

Error: `NOT_IMPLEMENTED: Could not find an implementation for ConvInteger(10)`
en `/m/frontend/stem/conv2d/Conv_quant`. Es un resultado reproducido en CPU host,
no evidencia de fallo en un dispositivo ARM64. No se presenta Beat This como
compatible ni se convierte/reemplaza su exportación con procedencia pendiente.
El resultado previo/posterior coincide; el problema necesita un trabajo específico
sobre exportación/operadores/proveedores en B09/B10, preservando licencias.

La inspección de app/src no encontró llamadas OrtEnvironment/OrtSession/OnnxTensor
o OrtGetApiBase; el analizador local existente es independiente. Este cambio no
activa modelos, no implementa AutoMix y no altera el motor de reproducción.

## Dos revisiones y pruebas

Revisión1 del mismo agente: publicación/minSdk, ELF reales, namespace/classes y
reglas R8, inspección de consumidores y contraste de carga de modelos con baseline.
Revisión2 separada del mismo agente: regresiones de verificador, publicación sin
parciales, legal y alcance.31 pruebas Python de herramientas CI correctas (8 nuevas),
incluyendo4KB, segmentos mixtos, tablas truncadas, congruencia, ZIP fallido, JNI
faltante y rechazo de publicación parcial. Casos anteriores conservados.

Sin Gradle, APK ni tests Android locales por instrucción del usuario. La comprobación
Python de modelos CPU no es una prueba Android ni inferencia/calidad de audio.
Compilación debug/release, suites, lint y verificación del APK final: PENDIENTES
DE GITHUB. Sin dispositivo Android/16KB, arranque, Logcat o sonido verificados.

## Licencias y preservación

Microsoft MIT de v1.22.0 coincide byte a byte con el texto ya conservado. Se añade
ThirdPartyNotices.txt upstream íntegro (SHA256 en JSON), con índice/NOTICE de versión.
Incluye avisos de diversas plataformas; no certifica que todos los componentes
citados estén enlazados en Android. Los mecanismos existentes lo empaquetan y
comparan con el APK. LICENSE GPL, atribuciones previas y modelos conservados.

Fuera de alcance: Glass/QA01, pantallas, reproducción, CMake/JNI propios, filtros
ABI, SDK, firmas, AndroidX/Gradle y plan maestro. Se actualizan únicamente runtime,
regla R8, verificador/tests y avisos/evidencias. Pendiente de validar APK real y
resolver/exportar Beat This antes de afirmar análisis ONNX/AutoMix completos.

## Entrega y continuidad

Publicar misma rama/PR, avisar y esperar resultado del usuario. No consultar
automáticamente el nuevo run ni iniciar entrega3/B07/B06.2/.3/.4/QA02. La corrección
anticipa solo el punto nativo16KB, sin dar por completados B09/B10 ni reordenar el plan.
