// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.replay

import com.music.aurelis.data.LocalListening
import com.music.aurelis.data.model.LocalSong

internal fun totalListeningMs(history: Map<String, LocalListening>): Long = history.values.fold(0L) { sum, entry ->
    sum + entry.listenedMs.coerceAtLeast(0).coerceAtMost(Long.MAX_VALUE - sum)
}

internal data class LocalReplaySummary(val totalMs: Long, val totalPlays: Long, val rows: List<ReplayRow>)

/** Legacy aggregates are lifetime totals, never reconstructed calendar periods. */
internal fun localReplaySummary(history: Map<String, LocalListening>, songs: List<LocalSong>): LocalReplaySummary {
    fun total(values: Iterable<Long>) = values.fold(0L) { sum, value -> sum + value.coerceAtLeast(0).coerceAtMost(Long.MAX_VALUE - sum) }
    val byId = songs.associateBy { it.id }
    val rows = history.entries.sortedWith(compareByDescending<Map.Entry<String, LocalListening>> { it.value.listenedMs }.thenBy { it.key })
        .mapIndexed { index, (id, stats) ->
            val song = byId[id]
            ReplayRow(id, index + 1, song?.title.orEmpty(), song?.artist, song?.artworkUri,
                stats.listenedMs.coerceAtLeast(0), stats.playCount.coerceAtLeast(0))
        }
    return LocalReplaySummary(totalListeningMs(history), total(history.values.map { it.playCount.toLong() }), rows)
}
