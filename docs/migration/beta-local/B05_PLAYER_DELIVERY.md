<!-- Aurelis © 2026. GPL-3.0-or-later -->
# B05 — MiniPlayer y pantalla de reproducción local

## Alcance autorizado y base

El usuario autoriza completar MiniPlayer y pantalla vertical/horizontal juntos;
60 archivos distintos máximo por mensaje/entrega, incluidos todos los arreglos.
Base18950a40102045e3e3b461c7bd84bdcc426ba82f, main168 SUCCESS, UI24 FAILURE
con la brecha confirmada de transporte fuera de vista y pérdida de ADB en referencia.
No reiniciar B06 ni iniciar B07/B09. Misma rama feature/bitchord-local-migration,
PR1 y título Aurelis Music Beta UI and components; sin Release oficial.

## Implementación

- MiniPlayer: arte40dp/radio8, padding12/8, dos líneas, target visual40/glyph32 y
  next25/escalaY0,85, separación8, pill50%. Se retiran la flecha extra y la tercera
  línea; la tarjeta conserva abrir por toque/gesto, calidad/progreso accesibles por
  semántica. Se conserva el dock y cancelación/capacidades de gestos existentes.
  Pausa sigue accionable durante carga, con indicador. Hápticos respetan políticas.
- Transporte: glyphs reales de BitChord y proporciones92/76dp, play74/58, skip56dp
  target y50/44 glyph con squash0,85. Se mantiene lugar de botones deshabilitados,
  tint/contraste y animación reducida. Ningún controlador adicional.
- Vertical: cabecera48, margen30, máximo560, créditos/favorito local y deck al pie.
  Primero se mide el deck: carátula recibe espacio restante por altura y ancho;
  no fuerza320dp ni depende de desplazar para acceder a transporte. Puede reducir
  la carátula con poco espacio; texto ampliado mantiene prioridad de controles.
- Deck: vista de línea actual con el ajuste temporal existente, progreso fino y
  tiempos en extremos, calidad/origen explícitos, transporte, volumen real y fila
  de letras/modos/cola. La indicación de salida sigue «no medida», sin inventar DSP.
  Estado de temporizador y acceso a herramientas existentes en cabecera. Menú de
  canción, fuentes/importación/traducción y acciones locales conservados.
- Horizontal: patrón LandscapePlayerLayout existente a dos columnas. Créditos
  secundarios pueden desplazarse en su región pequeña; deck permanece fijo. Arte,
  controles y paneles usan un estado. Cola/letras conservan transportes y acceso a
  cambio de panel a la izquierda; lista guarda su scroll/restauración existentes.
- Volumen: STREAM_MUSIC mediante AudioManager, ContentObserver de ajustes y lectura
  posterior real; context.applicationContext, observer retirado al salir. Capacidad
  fija/ausente se deshabilita; sin instalar PlayerPlatform legacy ni simular volumen.
- Recursos ES/EN y selectores por recurso en tests. Acciones se conservan en todas
  las rutas activas. No se toca firma/ABI/SDK/Gradle/dependencias ni renderer Glass.

## Procedencia y licencias

BitChord fijo b690dc146009d4815825fa4a20815ef2e4ee8898, solo lectura. Se usan
patrones reales de MiniPlayer/TransportRow/deck de NowPlayingScreen y el layout
horizontal ya adaptado; no se reemplazan archivos completos ni se portan streaming,
party/video/TV/Wear/Auto. Cuatro vector XML con créditos upstream/GPLv3, pathData
original y tint aplicado por Compose. NOTICE y LICENSE_AUDIT registran fuentes,
hashes y adaptación. Cabeceras Aurelis aplican a aportes propios, sin cambiar la
licencia/autoría del material original. Sin limpieza masiva de licencias.

## Validación y límites

Local: parser estático Kotlin y XML, unicidad/contratos de recursos, diff y57
regresiones Python por material. Gradle, Android Lint, APK y tests Android SOLO
GitHub Actions. Dos revisiones del mismo agente, no independientes: implementación/
capacidades/lifecycle y consumidores/restauración/pruebas/recursos/presupuesto.

Cinco tests nuevos JVM preparados:320×568 fuente1,5,640×320 fuente1,5, volumen real
de AudioManager y pausa MiniPlayer durante buffering. Se conservan pruebas previas
de cola, fuentes/traducción, calidad, gestos y restauración; se retira scroll para
alcanzar controles que deben estar visibles. Se amplía el caso nativo con música a
abrir player, pausar, girar, reanudar, volver y cerrar. Conserva hardware/Glass y
recorridos Perfil/Biblioteca/Nueva lista/Buscar. Ningún timeout se alarga.

Compilación/429 tests esperados/giros del SHA nuevo PENDIENTES al preparar commit;
no afirmar aprobación antes de leer Actions. El comparador mantiene sus requisitos
de transporte real visible y cambios de sesión; no se relaja para ocultar fallos.
La referencia puede seguir perdiendo ADB; causa final aún no acreditada.

ZIP plain23 solicitado, todavía inaccesible por proxy sa16; las imágenes completas
de referencia no se han revisado. Por tanto no se declara identidad visual total,
validación HyperOS ni cierre final B05. Prueba física Redmi Note14 Pro5G Android16/
HyperOS3 sigue pendiente. B07 conserva parsers/núcleo/panel ya existente y sus
pendientes de efectos, gestos y compartir; no se da por implementado con este cambio.
Restantes hallazgos ZIP de otras pantallas conservan sus entregas del plan.

## Inventario de esta entrega

24 rutas previstas al preparar commit; máximo60 contando correcciones:

- `NOTICE`
- `app/src/androidTest/java/com/music/aurelis/playback/RealSettingsNavigationInstrumentedTest.kt`
- `app/src/main/java/com/music/aurelis/ui/components/AnimatedArtwork.kt`
- `app/src/main/java/com/music/aurelis/ui/components/MiniPlayer.kt`
- `app/src/main/java/com/music/aurelis/ui/components/PlayerControls.kt`
- `app/src/main/java/com/music/aurelis/ui/components/PlayerSlider.kt`
- `app/src/main/java/com/music/aurelis/ui/components/PlayerVolumeRow.kt`
- `app/src/main/java/com/music/aurelis/ui/screens/NowPlayingScreen.kt`
- `app/src/main/res/drawable/ic_player_next.xml`
- `app/src/main/res/drawable/ic_player_pause.xml`
- `app/src/main/res/drawable/ic_player_play.xml`
- `app/src/main/res/drawable/ic_player_previous.xml`
- `app/src/main/res/values-en/strings.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/music/aurelis/ui/HomeLocalTest.kt`
- `app/src/test/java/com/music/aurelis/ui/MiniPlayerInteractionTest.kt`
- `app/src/test/java/com/music/aurelis/ui/PlayerChromeTest.kt`
- `app/src/test/java/com/music/aurelis/ui/PlayerIntegrationTest.kt`
- `app/src/test/java/com/music/aurelis/ui/PlayerResizeContinuityTest.kt`
- `app/src/test/java/com/music/aurelis/ui/navigation/GlassNavigationTest.kt`
- `docs/migration/beta-local/B05_PLAYER_DELIVERY.md`
- `docs/migration/beta-local/B05_UI_EVIDENCE_PLAN.md`
- `docs/migration/beta-local/LICENSE_AUDIT.md`
- `docs/migration/beta-local/PROGRESS_TRACKER.md`

## Corrección del primer intento de Actions

Run169/43799755349/e0fc7cad: B01 falla en compileDebugKotlin; otras fases de app
omitidas, B06 conserva fallo real. Logs ZIP oficiales CRC válido: NowPlayingScreen
accedía a userStore.favorites (campo privado SharedPreferences) y faltaba import de
semantics.contentDescription. Se colecta userStore.state y se usa su favorites
público; acción y estado de favorito pertenecen a la misma instancia activa. Se
añade import explícito. No supresiones ni alteración de firmas/SDK/dependencias.

Se respeta hideVolumeBar existente, reservando40dp como la fila real; nueva regresión
comprueba que ocultarla no desplace transporte. La preview respeta syncedLyrics.
Cinco tests JVM nuevos preparados (429 esperados), todavía NO ejecutados aquí.
Dos revisiones del mismo agente para la corrección; nuevo CI pendiente.
