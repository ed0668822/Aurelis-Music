# B03.2 — secciones locales y colas

Se conecta HeroShelf existente, con medidas de Common compartidas (70%/máximo320dp,
ratio0.92); se retiran únicamente las dos definiciones duplicadas en home para
usar la fuente común ya funcional. La cabecera/continuar/Aurelis se conservan.
Recientes adapta RecentShelf/CompactTrackRow del HomeScreen upstream:4 filas por
columna, ancho88%/máximo400dp, portada48dp y acciones reales. Se mantienen tags,
modelos y servicio del destino; no fuentes de streaming ni contenido de muestra.

Playlists pasan de lista de textos a carrusel de portadas de canciones disponibles,
ID persistente y plural de canciones realmente presentes. IDs ausentes permanecen
guardados (SD/archivo retirado). Canciones de cada sección encolan su propia lista,
no el catálogo entero. Recomendaciones usan el HeroShelf ya portado, favoritos/
most-played/added conservan tarjetas; mantener abre las mismas acciones locales.
El hero actual se limita al catálogo visible, con fallback a una canción real.
La barra deja de sumar los márgenes del host a los16dp/8dp de su propio componente.

Referencia b690dc1: sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/
HomeScreen.kt, RecentShelf/CompactTrackRow/HeroShelf; Common.kt para métricas.
NOTICE registra procedencia/modificaciones y conserva derechos/avisos originales;
LICENSE, dependencias, identidad/firma, motor y almacenamiento se mantienen.

Revisión1: colas/índices, datos/identidad, accesibilidad, key únicas y atribuciones.
Revisión2 separada del mismo agente:17 casos dirigidos correctos en31s, incluidos
4 nuevos de Home y11 del reproductor. Cola real del controller de prueba para
favoritos; playlist real/IDs ausentes; HeroShelf, mantener y sexta canción de la
segunda página mantienen lista/índice5 sin reproducir al pulsar Más.
No se afirma decodificación/audio físico (RecordingPlayer), GPU ni paridad medida
contra BitChord ejecutable. R y capturas adicionales al cierre B03/B03.3.
