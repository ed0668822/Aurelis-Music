// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import com.music.aurelis.data.model.LocalSong

/** Network policy for lyrics. Saved preferences cannot enable an unaudited service. */
internal object LyricsProviderRegistry {
    enum class Availability { AVAILABLE, CONDITIONS_UNVERIFIED }
    data class Provider(val source: LyricsSource, val availability: Availability)

    // Local imports/embedded lyrics are resolved before this registry is consulted.
    val providers: List<Provider> = listOf(
        Provider(LyricsSource.LRCLIB, Availability.AVAILABLE),
        Provider(LyricsSource.BETTER_LYRICS, Availability.CONDITIONS_UNVERIFIED),
        Provider(LyricsSource.BINI_LYRICS, Availability.CONDITIONS_UNVERIFIED),
        Provider(LyricsSource.LRC_RED, Availability.CONDITIONS_UNVERIFIED),
    )
    val availableSources: List<LyricsSource> = providers.filter { it.availability == Availability.AVAILABLE }.map { it.source }

    fun isAvailable(source: LyricsSource): Boolean = source in availableSources

    suspend fun lookup(song: LocalSong, source: LyricsSource = LyricsSource.LRCLIB): LyricsRepository.Result? {
        if (!isAvailable(source)) throw LyricsRequestException(LyricsFailure(LyricsFailureKind.DISABLED))
        return when (source) {
            LyricsSource.LRCLIB -> LrcLib.lookup(song.title, song.artist, song.durationMs)
            else -> throw LyricsRequestException(LyricsFailure(LyricsFailureKind.DISABLED))
        }
    }
}
