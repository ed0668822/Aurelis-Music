# Entrega 4 — B06 + B07: exportación de letras y AutoMix visual

Fecha: 21 de noviembre de 2026. Rama: `Aurelis-Music-Redesign`.

## B06: Vídeos de letras
- El selector Vídeo escoge la presentación vertical 9:16 de forma predeterminada; el formato de imagen mantiene su ajuste independiente.
- La exportación MP4 usa un lienzo fijo de 720 × 1280, recortando al centro sin deformar la carátula.
- Cada ejecución utiliza archivos temporales exclusivos; un exportador no puede sustituir el fondo que otro todavía decodifica.
- Elimina los archivos temporales incompletos en caso de fallo o cancelación.
- Conserva el comportamiento explícito: sólo incorpora audio local accesible; para fuentes remotas sin permiso de lectura exporta MP4 silencioso.

## B07: AutoMix visual
- Cuando la carátula saliente no está disponible, la entrante se muestra directamente en vez de aparecer sobre negro.
- Mantiene el progreso sincronizado con `CrossfadeController`, las preferencias de animación y los dos reproductores de interfaz.
- No altera `PlaybackService`, MediaSession, los dos decks existentes ni los modos manuales.

## Verificación
- Revisar `AndroidLyricsShareSheet`, `LyricsShareVideo` y `AutoMixArtworkOverlay` en dispositivo.
- Ejecutar compilación, pruebas y Lint en CI; prueba manual del MP4 con y sin audio local y de carátulas ausentes.
- No confundir un CI correcto con una validación de reproducción en dispositivo real.
