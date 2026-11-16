<!-- Aurelis © 2026. GPL-3.0-or-later -->
# Reproductor, ajustes y minutos — continuación autorizada por capturas

Base: 1da11b0f. El usuario pide mejorar estas tres áreas con sus capturas.
Máximo 60 archivos por entrega. Se conserva la sesión local, servicio Aurelis,
perfil único, permisos, ABI, SDK y firmas. No se incorpora Cast, Spotify Canvas,
streaming, TV, Auto ni Wear. La primera captura orienta la presentación vertical
con arte de borde a borde; la fidelidad final necesita prueba visual real.

## Cambios

- Player vertical: carátula sin margen ni esquinas, fundida hacia el fondo,
  asa y álbum en cabecera; créditos en negrita, estrella de favorito y menú.
  Tiempos transcurrido/restante; información de calidad y herramientas siguen
  accesibles. Volumen real STREAM_MUSIC con pista fina, conservando observer,
  límites y errores. Los modos locales siguen disponibles. Horizontal conserva
  el layout y una sola sesión. Transporte anclado, sin depender de scroll.
- Replay: sustituye lista cronológica básica por título, tarjeta de minutos,
  tarjeta de canción principal y ranking con carátulas, artista, minutos y plays.
  Orden real por tiempo acumulado, desempate estable por ID. Play usa canciones
  disponibles; medios ausentes conservan sus datos sin habilitar reproducción.
  La tarjeta de minutos lleva al ranking. Borrado conserva confirmación y los
  favoritos/listas. No inventa fecha de alta ni períodos del histórico heredado.
- Biblioteca y Replay usan la MISMA suma saturada de milisegundos. Se redondea
  a minutos después de sumar, no canción por canción. Etiqueta «Minutos escuchados».
  No se cambia la política de medición ni se añaden minutos ficticios.
- Ajustes: título/espaciado y búsqueda ajustados; grupos de radio 14 dp y divisor
  según referencia, respetando espaciado solicitado. Se mantiene el orden compatible
  de categorías ya adaptado, perfil local y PhotoPicker, sin opciones excluidas.

## Pruebas y revisión

Dos pruebas JVM nuevas para suma/rounding después de sumar, historial de medios
no disponibles, límite Long, orden por duración y no por última visita. Se amplía
la prueba de player compacto/fuente grande para comprobar ancho completo del arte.
Pruebas de navegación y borrado de Replay adaptadas a la UI actual conservando
verificación de datos, cancelar/confirmar y preservación de favoritos/playlists.
Android/Gradle/Lint/APK SOLO GitHub Actions; no ejecutados localmente.
Dos revisiones estáticas del mismo agente, no independientes. Parser Kotlin
sin errores nuevos salvo limitación ya presente de catch/finally en SettingsScreen
(mismo fallo del parser en HEAD, cambios revisados manualmente). Recursos ES/EN,
formatos y diff-check comprobados. Esto no acredita compilación ni igualdad visual.

## Pendientes explícitos

El histórico agregado disponible solo representa todo el tiempo. Los selectores
semanal/mensual/anual de la captura no se simulan ni se calculan desde lastPlayedAt:
requieren integración de eventos por período y tratamiento de legado en B11.
Continúan pendientes historias/compartir, rankings adicionales y reproducción de
la lista Replay completa; la acción actual dice explícitamente «canción más escuchada».
La disposición local mantiene modos y herramientas adicionales permitidos y no
copia Cast ni servidores/Canvas. No certificar igualdad total con estas capturas.
Comparación UI completa y prueba Redmi Note 14 Pro 5G Android16/HyperOS3 pendientes.
Después del push avisar y ESPERAR que el usuario diga si compiló o falló.
No iniciar otro bloque ni vigilar conclusiones por iniciativa propia.

## Procedencia

BitChord GPLv3, referencia fija b690dc146009d4815825fa4a20815ef2e4ee8898,
checkout solo lectura. Se adaptan patrones y disposición a componentes reales de
Aurelis; no se reemplazan módulos enteros ni autorías de terceros.

- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/NowPlayingScreen.kt
  SHA256: 1d5ceae1f1c045881a49601f3fa535ecd4cf8efe45837fc97990045e3ed9024e
- sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/replay/ReplayCard.kt
  SHA256: 0ba2cd399159aaa1722f28ab59e2f98a79fe0afb97754f6f005f8a5846f60b87
- app/src/main/java/com/music/bitchord/ui/replay/ReplayScreen.kt
  SHA256: 310d8d9f23e834b81b68d4a0f9661b839f36ed280004dad15acec5e9454b53a5
- app/src/main/java/com/music/bitchord/ui/screens/SettingsSheet.kt
  SHA256: b77bd33f94c1ac83bcdb17da34908a563139efa9fe8cc018645db96a6569fe35

## Inventario de entrega: 18 archivos

- NOTICE
- app/src/main/java/com/music/aurelis/MainActivity.kt
- app/src/main/java/com/music/aurelis/ui/components/PlayerSlider.kt
- app/src/main/java/com/music/aurelis/ui/components/PlayerVolumeRow.kt
- app/src/main/java/com/music/aurelis/ui/components/SettingsComponents.kt
- app/src/main/java/com/music/aurelis/ui/replay/LocalReplaySummary.kt
- app/src/main/java/com/music/aurelis/ui/screens/LibraryScreen.kt
- app/src/main/java/com/music/aurelis/ui/screens/NowPlayingScreen.kt
- app/src/main/java/com/music/aurelis/ui/screens/ReplayScreen.kt
- app/src/main/java/com/music/aurelis/ui/screens/SettingsScreen.kt
- app/src/main/res/values-en/strings.xml
- app/src/main/res/values/strings.xml
- app/src/test/java/com/music/aurelis/ui/PlayerIntegrationTest.kt
- app/src/test/java/com/music/aurelis/ui/navigation/NavigationIntegrationTest.kt
- app/src/test/java/com/music/aurelis/ui/replay/LocalReplaySummaryTest.kt
- docs/migration/beta-local/LICENSE_AUDIT.md
- docs/migration/beta-local/PLAYER_REPLAY_SETTINGS_FOLLOWUP.md
- docs/migration/beta-local/PROGRESS_TRACKER.md
