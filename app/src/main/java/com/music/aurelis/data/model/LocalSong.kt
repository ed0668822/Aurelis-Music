// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.model

data class LocalSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val contentUri: String,
    val artworkUri: String?,
    val durationMs: Long,
    val filePath: String?,
    val trackNumber: Int?,
    val year: Int?,
    val dateAddedMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val queueTier: QueueTier = QueueTier.CONTEXT,
    val queueEntryId: String? = null,
    val playbackSource: String? = null,
    val playbackSourceType: PlaybackSourceType? = null,
    val playbackSourceId: String? = null,
    // === Fase 2 Wave 2 (Decisión A — Opción C) ===
    // Null/false hasta que NerdStats real exista en Fase 8.
    // Los rellenará el escáner local cuando se sepa decodificar el formato real;
    // por ahora sirven para que SourceQualityBadge y ExplicitBadge compilen
    // y se vean cuando alguien los popule manualmente.
    val sourceQuality: String? = null,
    val sourceHiRes: Boolean = false,
    val isExplicit: Boolean? = null,
    val albumId: String? = null,
    val artistId: String? = null,
    val discNumber: Int? = null,
    val genre: String? = null,
) {
    val videoId: String get() = id
    val thumbnailUrl: String? get() = artworkUri
    val albumName: String? get() = album
    val fromAutoplay: Boolean get() = queueTier == QueueTier.AUTOPLAY

    val durationText: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}


