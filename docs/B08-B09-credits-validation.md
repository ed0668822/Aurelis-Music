# Entrega 5 — B08 + B09: créditos y cierre de regresión

Fecha: 21 de noviembre de 2026. Rama: `Aurelis-Music-Redesign`.

## B08 — Créditos musicales
- Añadido `SongCredit` y lector de créditos de YouTube Music con búsqueda directa por identificador de canción, y un segundo intento a través del menú de la canción en la cola.
- El analizador sólo acepta roles que aparezcan explícitamente en los datos. Puede recuperar autores, productores, compositores, intérpretes y otros roles sin suponer nombres.
- Si YouTube Music no devuelve créditos, se conservan los créditos de la sección musical del vídeo cuando están presentes.
- `SongInfoDialog` muestra créditos remotos y mantiene los créditos de etiquetas locales existentes.
- Los fallos de consulta de créditos no impiden consultar los demás metadatos. Las peticiones se realizan al abrir Song Info, no durante la reproducción.
- Pruebas para páginas de diálogo, filas de YouTube Music, asociación del endpoint a la canción y metadatos públicos.

## B09 — Matriz de verificación

| Área | Validación automatizada | Validación adicional necesaria |
| --- | --- | --- |
| Replay Year | `ReplayYearStoryModelTest`, `ReplayYearProjectionTest` | Animaciones y pausa en móvil |
| Radio | `YouTubeRadioSeedTest`, `AppNavigatorTest` | Cola y sesiones reales |
| Best Part | `MostReplayedTest`, `BestPartTimingTest` | Heatmap real de vídeos |
| YouTube Premium | pruebas del resolvedor/compilación | Cuenta Premium y calidad real |
| Letras en vídeo | `LyricsShareTimelineTest`, Debug/Release | Abrir MP4 con/sin audio en dispositivo |
| AutoMix visual | `AutoMixArtworkStateTest`, `AutoMixArtworkTuningTest` | Reproducción, carátulas ausentes y memoria |
| Créditos | `SongCreditsParserTest`, `SongInfoParserTest` | Respuestas reales de YouTube Music |
| Estabilidad | GitHub Actions: Debug, Release, Lint y unit tests | Instrumentadas ejecutadas en emulador |

La compilación de pruebas instrumentadas no equivale a su ejecución. Una ejecución verde de CI tampoco certifica una sesión Premium ni una reproducción real de AutoMix.

## Límites deliberados
- Sin nuevos reproductores: Media3/MediaSession existentes se conservan.
- No se habilitan descargas automáticas de YouTube ni Google Drive.
- No se modifican otros módulos, `main` ni el archivo de licencias.
- La discrepancia de licencias GPL/LGPL de los archivos heredados requiere revisión independiente antes de redistribución.
