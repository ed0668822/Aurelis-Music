# B07.1 — parsers y persistencia temporal, primera entrega

Estado: implementado, compilación/pruebas PENDIENTES DE GITHUB. B07 completo no se
cierra. Base `e499ad4f2f5613864cc34e6e13add077aec30270`, rama existente y PR1 con título conservado.

1. **Objetivo:** conectar formatos locales enriquecidos y conservar sus tiempos;
   reutilizar la posición real de reproducción para el seguimiento existente.
2. **Dependencias:** infraestructura actual de B06.1/servicio y B05. Autorización
   explícita del usuario para B07; B06.2/.3/.4 siguen pendientes y no se marcan
   completados. CI09 previo: run144 SUCCESS del mismo HEAD, confirmado por páginas
   públicas/enlace de commit. Esto no acredita un dispositivo Android real.
3. **Referencias:** F10, BitChord b690dc146009d4815825fa4a20815ef2e4ee8898:
   shared/src/jvmSharedMain/kotlin/com/music/bitchord/data/lyrics/EnhancedLrc.kt y
   TtmlLyrics.kt. De sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/
   PlayerLyrics.kt solo referencias dirigidas de posición/reloj; no port completo.
   Copia solo lectura, avisos originales conservados, sin importar proveedores.
4. **Existentes modificados:** data/lyrics/{Lyrics,EnhancedLrc,TtmlLyrics,
   LocalLyricsRepository}.kt; ui/screens/NowPlayingScreen.kt y pruebas
   LocalLyricsTest/PlaybackIntegrationTest. Rutas completas/hashes en JSON.
5. **Nuevos:** data/lyrics/LyricsCacheCodec.kt y test data/lyrics/RichLyricsTest.kt;
   esta evidencia MD/JSON y ampliación de registros/NOTICE.
6. **No tocar:** servicio/estado de playback/cola, shaders/Glass/QA01, ONNX/modelos,
   Gradle/SDK/ABI, workflows, manifiesto/permisos, firma/LICENSE y plan maestro.
7. **Arquitectura:** un punto Lyrics.parse; reutiliza parsers/modelo LyricLine,
   LyricWord/LyricSyllable y wordsFromRuns existentes. Caché privada version2 sin
   pérdida temporal, lector compatible con version1. Importación guarda texto
   original en la ruta privada existente, sin migrar archivos del usuario.
8. **Funciones conectadas:** picker existente acepta application/ttml+xml y XML;
   importación/incrustadas → parser compartido → estado del ViewModel → panel y
   PlaybackPosition existentes. Caché offline conserva palabras/sílabas/alineación,
   final, fondo y timingSource. No otro motor/reloj o sesión; no red nueva.
9. **Riesgos de compilación:** DOM namespaces y SAM/error handlers, JSON nullable
   overloads/modelos, corrutinas en prueba de importación. Revisión estática hecha;
   compilación real pendiente de CI, sin afirmar éxito por inspección.
10. **Licencias:** cambios a parsers ya adaptados de BitChord y glue propio;
    procedencia ampliada en NOTICE. Atribuciones originales, dependencias y LICENSE
    conservados. Fixtures breves originales de prueba, no letras comerciales.
11. **Pruebas:**16 nuevas preparadas:13 formatos/offset/repeticiones/sílabas/entidades/
    TTML/caché y corrupción;2 repositorio (reapertura/offline rico);1 integración
    con Player real de Media3 bajo RecordingPlayer para pausa/seek sin reemplazo.
    XXE usa un archivo local de prueba existente, no una ruta inexistente que pueda
    fallar por otro motivo. Métodos/aserciones anteriores no se debilitan.
12. **Compilación:** exclusivamente GitHub mediante workflow android.yml vigente:
    compile/lint/testDebugUnitTest/testReleaseUnitTest y assemble debug/release,
    más verificación APK/ZIP/ELF existente. No Gradle ni tests Android locales.
13. **Aceptación:** formatos generan modelo válido con tiempos conservados; caché
    rica reabre sin aplanar; v1 legible; datos inválidos/DTD no se presentan como
    letras; importación/seek no alteran cola, pausa o canción. Confirmar en CI.
14. **Parada:** fallo de CI/regresión, pérdida de importación, licencia no acreditada
    o activación de servicios no autorizada. Publicar, avisar y esperar resultado
    del usuario antes de la siguiente entrega. No B08 automático.
15. **Resultado:** base temporal conectada. Pendientes del alcance B07.1: conformance
    ampliada y reloj por frame; B07.2 efectos/gestos/offset y B07.3 compartir siguen
    pendientes. No afirmar fidelidad avanzada por palabra solo por conservar datos.
16. **Complejidad:** alta; esta entrega limita cambios a formatos/persistencia y
    validación del recorrido existente, sin portar el PlayerLyrics completo.

## Detalles y límites

Enhanced LRC: fracciones1–3 dígitos, timestamps repetidos y offset aplicado una vez
a línea/palabras; inválidos o desordenados no generan intervalos negativos.
Entidades Unicode suplementarias sin truncar a Char; overflow/surrogates/NUL no
se convierten en texto binario. Se limita entrada a1MiB/10000 líneas y expansión
a20000 runs. El último run conserva el fallback previo hasta siguiente línea o
800ms si no hay marca terminal: no es medición del audio.

TTML: namespaces prefijados o sin namespace y raíz tt admitida; begin/end absolutos
con reloj o ms/s/m/h, palabras/sílabas/fondo. Límites1MiB/10000 p/50000 nodos/64 de
profundidad; DTD/entidades externas rechazadas también si faltan flags del parser.
No soporta aún frame/tick notation ni temporización relativa/heredada de contenedores.
Las partes sin datos suficientes no reciben tiempos inventados. XML ajeno/inválido
no cae al modo texto mostrando su marcado. Lectura manual/efectos son otra entrega.

Caché2: persiste el modelo temporal con límites de profundidad/cantidad/texto y
validación de intervalos y rangos de caracteres. JSON inválido causa cache miss;
fallar al guardar no descarta una respuesta válida del proveedor. V1 conserva sus
tiempos por línea, sin inventar palabras perdidas. Importaciones y caché siguen
siendo atómicas y la cancelación continúa propagándose. Límites/tamaño/LRU previos
conservados; no cambios al almacenamiento del perfil ni permisos de archivos.

## Dos revisiones

Revisión1 estática del mismo agente: flujo único, contrato de tiempos, timestamps
repetidos, intervalos/namespaces/entidades, límites y compatibilidad de caché.
Revisión2 separada del mismo agente: picker/estado/posición conectados, pruebas
anteriores idénticas, falsa recuperación de palabras evitada, licencias y diff.
`git diff --check` correcto; áreas protegidas comparadas con base, sin cambios.
No se atribuyen revisiones a personas externas ni se presentan como tests ejecutados.

Las16 pruebas nuevas NO SE EJECUTARON localmente; ejecución pendiente de GitHub.
Sin instalación/arranque, gestos/GPU/sonido/Logcat ni pruebas en teléfono. Entrega
misma rama/PR1 y espera resultado del usuario. Actualizaciones de advertencias,
B06 restante y el problema ConvInteger de Beat This continúan en sus registros.
