<!-- Aurelis © 2026. GPL-3.0-or-later -->
# CI16 — revisión de ui/audit, ui/components y ui/haptics

**Estado: revisión de evidencia completada para 113 fuentes y 226 pares archivo/variante.**
No es cierre de CI12.3 ni validación funcional/física. Esta entrega publica
documentación de la revisión; no modifica fuentes ni configuración de CI.

## Autorización, base y presupuesto

La autorización inicial cubrió sincronizar por avance rápido y preparar esta
revisión con máximo6 archivos. Tras revisar el resultado, el usuario autorizó
continuar con este bloque documental, incluidos commit y push a la misma rama.
Base revisada: `780fe6e3b054d1a7f52ee0864132684f817da67b`, rama
`feature/bitchord-local-migration`. Avance rápido desde c1bc7359: 52 commits
remotos conservados, sin conflictos, cambios pendientes o reescritura de historial.

Manifiesto previo y real: este MD, CI16_THREE_FOLDERS_REVIEW.json y
../migration/beta-local/PROGRESS_TRACKER.md. Son3 archivos de un máximo6.
Los metadatos y logs auxiliares de trabajo están fuera del checkout; no forman parte
de la entrega. Los cambios remotos incorporados no son cambios nuevos de CI16.

Base de publicación: `eeb1c1bc78ea1c3bc79ae7f593d8a390520239b6`.
Se reconfirmaron los hashes de las113 fuentes contra el informe; siguen intactas.
El run109 es la evidencia histórica identificada por SHA, no el CI del commit
documental nuevo. La autorización de cada bloque incluye commit/push; después
se espera el resultado comunicado por el usuario y autorización para el siguiente.

## Evidencia actual y límites

[Run #109](https://github.com/ed0668822/Aurelis-Music/actions/runs/43339311481)
corresponde exactamente al SHA revisado y terminó success. La API confirma
success en las seis fases B01–B06, incluidos en B01 los pasos Compile and
assemble Debug APK, Verify every tracked Debug Kotlin/Java task input y
Package and test Debug compiler class evidence. Es evidencia global de CI;
no se usa para inventar resultados individuales ni comportamiento físico.

Artefacto revisado: Aurelis-Informes-B01-B06-109, id13999063083; no expirado al
consultar sus metadatos durante la revisión inicial.
Digest de archivo ZIP declarado por GitHub:
`sha256:125c7055571e55f8bac10da819828a3adb739e011186c90461d12f5294687943`.
Verificado localmente contra el ZIP adjuntado por el usuario, con integridad ZIP
correcta. No es el hash de un APK. Se leyeron los JSON directamente del archivo,
sin extraer rutas ni ejecutar contenido. El ZIP no contiene APK.

La descarga de red devolvía Forbidden en productionresultssa10. El ZIP aportado
por el usuario elimina ese bloqueo para esta revisión. No se solicitan credenciales
ni se publican URLs firmadas; la configuración activa de red no se modifica aquí.

Se contrastaron las226 filas del run109, únicas por archivo/variante, con los hashes
SHA-256 de las113 fuentes locales. Coinciden el SHA de origen del inventario,
matriz general, matriz F4, trazas APK, entradas Debug y gate de integridad.
No se usaron resultados de c5ff687d como evidencia actual, aunque las fuentes no
cambiaron. Las226 filas tienen input de tarea confirmado, estado SUCCESS y cero
errores atribuidos. Hay224 COMPILADO_EN_APK y2 SIN_CLASES_ESPERADAS; en Release
se observó mapping R8. Esto es evidencia cruzada, no una invocación aislada del
compilador por cada archivo ni prueba de integración funcional.

## Inventario y revisión estática

| Carpeta de producción | Archivos versionados | Resultado estático | Matriz actual Debug/Release |
|---|---:|---|---|
| app/src/main/java/com/music/aurelis/ui/audit | 5 | Kotlin bajo src/main, con declaraciones; comentarios antiguos engañosos | 5 COMPILADO_EN_APK por variante |
| app/src/main/java/com/music/aurelis/ui/components | 107 | Kotlin bajo src/main; LocalAppBackdrop.kt no emite declaración propia | 106 COMPILADO_EN_APK y1 SIN_CLASES_ESPERADAS por variante |
| app/src/main/java/com/music/aurelis/ui/haptics | 1 | Haptics.kt contiene enum/clase/función; permiso VIBRATE declarado | 1 COMPILADO_EN_APK por variante |

No se encontraron exclusiones específicas en app/build.gradle.kts, build.gradle.kts
o settings.gradle.kts. La única configuración explícita sourceSets observada
agrega assets legales generados, no excluye código. Se contrastó también la
evidencia de entradas efectivas por variante; la configuración por sí sola no
habría bastado. El gate F4 reporta integridad con límites de enlace nativo;
no se infiere enlace objeto-a-APK ni comportamiento Android por este inventario.

### Hallazgo único — comentarios históricos inducen a interpretar exclusión

**Severidad: Low.** Escenario reproducible: leer la auditoría como descripción
del build Android actual hace parecer que estos archivos se excluyen, aunque
sus comentarios no configuran Gradle. Afecta la interpretación de la evidencia,
no demuestra un fallo del compilador ni autoriza eliminar código.

| Archivo bajo ui/audit | Línea | Texto ambiguo |
|---|---:|---|
| AnimationAudit.kt | 14 | estático: no se compila |
| ImportsAudit.kt | 8 | no se compila en PolyDev |
| LayoutAudit.kt | 12 | estático: no se compila |
| LocalSongMappingAudit.kt | 20 | estático: no se compila |
| UiAudit.kt | 6 | no se compila en PolyDev |

Corrección mínima propuesta, aún no aplicada: identificar las tablas como
referencia histórica y aclarar que pertenencia al source set, compilación,
presencia DEX e integración funcional son comprobaciones diferentes. Son5
archivos, no4 como decía el plan anterior. Una entrega posterior de aclaraciones
podría usar esos5 archivos más1 registro, sin exceder6; requiere autorización.
No actualizar automáticamente estados CREATED/ADAPTED/PENDING como si fueran
resultados de CI o pruebas funcionales.

### LocalAppBackdrop.kt no debe forzarse a emitir una clase

El archivo contiene solo package/comentarios (líneas2–5). La declaración real
`val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }` está en
ui/components/LiquidGlass.kt:48. No hay una clase independiente que deba existir
por cada .kt. Copiar la declaración produciría una redeclaración.

La matriz F4 del artefacto clasifica el archivo como SIN_CLASES_ESPERADAS en
Debug y Release, con task_input=true, SUCCESS y cero errores. Su traza dice
NO_STANDALONE_DECLARATION_HINT. La capa previa conserva una etiqueta de presencia
de clase incompleta; F4 la explica por ausencia de declaración, no por fallo.
LiquidGlass.kt tiene su propia evidencia de clases/DEX en ambas variantes.

### Uso en runtime

Una búsqueda estática fuera de ui/audit no encontró referencias a sus cinco
objetos. Esto no prueba eliminación R8 ni ausencia de reflexión y no justifica
borrarlos. Haptics.kt y los componentes existentes requieren pruebas de sus
consumidores activos; compilar o aparecer en DEX no demuestra vibración física,
interacciones de UI ni paridad visual. El alcance es la evidencia de compilación
de las tres carpetas; no una nueva auditoría funcional exhaustiva de los107
componentes ni una certificación de ausencia de defectos de la app.

## Comprobaciones ejecutadas y dos revisiones

Solo fixtures Python existentes, sin Gradle/Android ni dependencias nuevas:

| Suite | Casos | Resultado |
|---|---:|---|
| test_file_compile_matrix.py | 5 | PASS |
| test_f4_debug_inputs.py | 4 | PASS |
| test_f4_integrity_gate.py | 5 | PASS |
| test_p2_inventory.py | 3 | PASS |
| test_p2_compiler_evidence.py | 6 | PASS |
| test_p2_apk_trace.py | 5 | PASS |
| test_apk_audit.py | 18 | PASS |
| Total | 46 | PASS; cero fallos |

Revisión1 del mismo agente: inventario113, hashes locales, ubicación de
declaraciones/comentarios, source sets y procedencia SHA/run. Revisión2 del mismo
agente: presupuestos, consistencia MD/JSON, distinción evidencia esperada/observada,
datos de fixtures frente a Android y ausencia de cambios funcionales. No son dos
revisores independientes. Se verificaron también el ZIP completo contra el digest
de GitHub, los hashes locales y la procedencia/pareado de todas las filas. La
validación documental final queda registrada en JSON.

En la preparación de publicación se reconfirmaron digest e integridad del ZIP,
113 hashes,226 pares y sus clasificaciones, ubicaciones de comentarios y las
declaraciones de LocalAppBackdrop. Se comprobaron alcance de tres rutas,
consistencia MD/JSON/tracker y conservación de código/CI. Las46 pruebas anteriores
siguen registradas como ejecución inicial; no se repiten por publicar documentos.
Dos pasadas finales del mismo agente: evidencia/procedencia y alcance/consistencia.
No se ejecutaron Gradle ni pruebas Android locales.

## Entrega observada: APK firmado y artefactos

El mismo informe B04 registra Release Aurelis-Music-V1.9.103.apk,57134014 bytes,
versionCode10109, signature_verified=true y modo configured_release. Certificado
configurado/verificado:7274bf97a78adce7920aed4b784fa3e8f1f679ab2597805434b7d5dd4465aa3b.
SHA-256 del APK reportado:b0b329feae4c78e9eceb2c24e4c10f0ce0d1f2654316125a55c2eb9eb126d05c.
Son datos del informe; el binario Release no está en el ZIP ni se verificó aquí.

En el run109, pipeline-results.json declara release_publish_expected=false y
expected_final_artifacts=2. Por tanto falta el descargable Release solicitado por
el usuario, pero no falló su compilación/firma según los informes: la política
separa la publicación oficial en un workflow protegido para main/tags. No se
cambió esa política durante la revisión inicial, no se sustituyó firma ni se
lanzó publicación. Este hallazgo histórico fue atendido después por CI17, commit
`bb417f39a0d8e76307977d52b1fa88587d67ae51`, documentado en
[CI17_SIGNED_RELEASE_CANDIDATE](CI17_SIGNED_RELEASE_CANDIDATE.md).
CI17 habilita el candidato Release firmado de pruebas en la rama de trabajo;
su resultado Android no se certifica con el ZIP del run109 ni con esta entrega.

## Próximos bloques pendientes

Publicar únicamente los tres documentos de este manifiesto en PR1 y comunicar
el commit. El push puede activar los workflows existentes; no se solicita un
workflow manual. Después, esperar el resultado comunicado por el usuario y su
autorización para el siguiente bloque. Las aclaraciones de cinco comentarios
siguen sin aplicar; CI12.3 funcional y B09 permanecen pendientes, sin cierre
retroactivo.
