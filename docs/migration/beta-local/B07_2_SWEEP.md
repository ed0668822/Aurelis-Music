# B07.2 — barrido, reloj y foco, primera entrega

Estado: implementado, pruebas/compilación nuevas PENDIENTES DE GITHUB. No cierre
total de B07.2/B07. Base `f81b6a03228e3d5e6d03fb04b75cf7de2afa437e`, misma rama/PR1, título conservado.
Autorización vigente: continuar B07 y llegar a B08. Conservarla entre entregas;
solo esperar el resultado de GitHub que comunique el usuario antes de la siguiente.

1. **Objetivo:** hacer visible la temporización rica mediante barrido continuo,
   foco/lectura manual y offset coherentes con posición real.
2. **Dependencias:** primera entrega B07.1 publicada; run145 SUCCESS con enlace al
   mismo HEAD, confirmado en páginas públicas. B06 restante no se da por cerrado.
3. **Referencia:** F10/F11, BitChord b690dc146009d4815825fa4a20815ef2e4ee8898:
   sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerLyrics.kt
   (secciones offset, rememberLyricClock, SweptLyricLine y sweepTo) y LyricClock.kt.
   Referencia de solo lectura; adaptación dirigida, no copia del archivo completo.
4. **Existentes modificados:** ui/player/SyncedLyricsPanel.kt;
   ui/screens/NowPlayingScreen.kt; playback/PlaybackViewModel.kt;
   res/values/strings.xml. Pruebas LyricsPanelTest/PlaybackIntegrationTest.
5. **Nuevos:** ui/player/{LyricsPlayback,LyricSweep}.kt y test
   ui/player/LyricTimingTest.kt; estas evidencias MD/JSON.
6. **No tocar:** servicio/motor/comandos de playback/cola, parsers/caché B07.1,
   shaders/Glass/QA01, ONNX/modelos/JNI/ABI, SDK/dependencias/workflow/gates,
   firma/LICENSE y plan principal. Avisos/progreso ampliados sin reemplazar autorías.
7. **Arquitectura:** LyricClock existente, un adaptador Compose por panel. Recibe
   PlaybackPosition real con sample time y token de discontinuidad. El VM publica
   ese token en el mismo syncState que posición/canción, sin emitir comandos.
   Identidad del panel usa queueEntryId para distinguir repeticiones de una canción.
8. **Funciones conectadas:** datos palabra/sílaba → revealedChars → recorte de dos
   copias del mismo Text con idéntico layout. Conserva shaping/wrapping/bidi; overlay
   sin semántica duplicada. Foco admite voces solapadas/fondo reales. Arrastre pausa
   seguimiento; botón/click en línea lo reanudan. CenterFocus usa medidas reales.
9. **Riesgos de compilación:** TextLayoutResult/Path/clip y LifecycleOwner, medidas
   lazy y callbacks Media3. Firmas contrastadas con javap; compilación pendiente.
10. **Licencias:** wrapper/painter adaptados de BitChord GPLv3, atribución en fuente
    y NOTICE; helper/algoritmo existentes reutilizados. Sin dependencias/recursos
    nuevos de terceros, letras comerciales ni cambio de LICENSE.
11. **Pruebas:**17 casos nuevos preparados:9 reloj/offset/graphemes/sílabas/foco,
    7 Compose (semántica única/seek, offset, motion reducido, voces, lectura manual,
    centrado de texto envuelto, RTL) y1 Media3 de discontinuidades/cola/pausa.
    Casos anteriores intactos. Consultas de nodos fuera de runOnIdle para evitar
    esperas UI anidadas. Nada ejecutado localmente como prueba Android.
12. **Compilación:** exclusivamente en GitHub mediante workflow vigente; compile,
    lint y unit suites debug/release, assemble y verificación APK/ZIP/ELF existentes.
13. **Aceptación:** barrido usa tiempos reales y no solo colores por palabra;
    mismos glyphs/layout en ambas capas, un texto accesible, seek/offset inversos,
    pausa y saltos coherentes, scroll manual sin secuestro y retorno explícito al
    foco. Confirmar con nuevo CI y pruebas gráficas Android.
14. **Parada:** fallo CI/regresión, callback duplicado, datos fuera de rango o
    pérdida de playback/lectura. Publicar y esperar resultado del usuario;
    correcciones primero, no encadenar entregas antes de esa puerta.
15. **Resultado:** núcleo visual activo. Faltan glow/feather/lift/growth/partículas,
    gestos completos de controles y validación de paridad/hardware; no se declara
    copia visual completa. Compartir B07.3 y proveedores/traducción B08 pendientes.
16. **Complejidad:** alta; se conserva una entrega verificable y la autorización
    de continuación hasta B08, sin empezar B09 ni reordenar el plan.

## Detalles de integración y rendimiento

Un valor positivo retrasa las letras: posición visible=max(player-offset,0);
seek=max(line+offset,0), con saturación superior para evitar overflow. Corrige la
inversión previa del host activo y coincide con la referencia y adaptador lateral
existente. La descripción del ajuste usa su recurso actual, sin un control ficticio.

El reloj usa samples en System.nanoTime y frames del mismo origen cuando coinciden;
se detiene al ocultarse/pararse/reducir movimiento y al perder foreground. Lecturas
paradas/seek pausado son directas; no se extrapola la pausa. Reinicio fresco al volver.
No se reescribe el algoritmo LyricClock ni se crea un segundo estado de reproducción.

Barrido: prefijo completo y fracción del grapheme actual se recortan sobre la copia
brillante; dirección tomada del run bidi del TextLayoutResult, no de heurística del
idioma. Las dos copias miden el mismo texto/estilo/ancho. Texto tenue conserva
semántica y overlay se excluye; el progreso se lee en draw. Fases antes/después evitan
redibujar sin necesidad una línea terminada. No se reclama glow/lift ya implementado.

El foco solo recalcula las voces al cruzar eventos de inicio/final; no recorre toda
la lista sesenta veces por segundo. CenterFocus permite centrar primera/última fila
con padding medido; scroll de lectura nunca se interpreta como seek. Motion reducido
usa posición reportada y scroll directo. El audio no se pausa al pausar seguimiento.

## Dos revisiones y límites

Revisión1 estática del mismo agente: contrato real de position/discontinuity,
interpolación/pausa/lifecycle, offsets inversos, rangos de graphemes y geometría.
Revisión2 separada del mismo agente: aislamiento de semántica, manual scroll y foco,
compatibilidad de API, conservación de métodos de prueba, XML/legal y áreas protegidas.
Comparación del VM verifica que solo añade publicación del contador, no cambios de
comandos. `git diff --check` y XML correctos; firmas reales inspeccionadas con javap.

Las17 pruebas son PREPARADAS/NO EJECUTADAS. No Gradle/APK/tests Android locales.
Nuevo CI pendiente; teléfono, GPU, Logcat, efectos a60Hz, consumo y capturas de
paridad no verificados. Tampoco se cierra con esta entrega conformance TTML completa,
B06 pendiente, entrega3 de advertencias o el fallo ConvInteger de Beat This.

Publicar misma rama/PR1 y avisar. Esperar resultado del usuario, conservar autorización
para la continuación B07→B08; no solicitarla de nuevo para el alcance ya concedido.
