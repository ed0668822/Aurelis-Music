// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data.stats

import com.music.aurelis.data.model.Song
import java.time.LocalDate
import java.time.YearMonth

/** The most listened-to track in one recorded month of an annual Replay. */
data class ReplayMonthTop(val month: YearMonth, val song: RankedSong)

/**
 * Immutable annual snapshot. Unknown daily facts and absent month rankings
 * stay absent, rather than being estimated from lifetime totals.
 */
data class ReplayYearData(
    val year: Int,
    val summary: ReplaySummary,
    val dayMs: Map<String, Long>,
    val monthTops: List<ReplayMonthTop>,
    val busiestDaySong: Song?,
) {
    val monthsWithListening: Int get() = monthTops.size
    val isEmpty: Boolean get() = summary.isEmpty
}

/**
 * A pure, deterministic projection of the persisted local listening buckets.
 * It never merges another calendar year, guesses unrecorded daily activity, or
 * mutates an existing bucket. The summary is calculated by ListeningStats.
 */
internal object ReplayYearProjection {
    private data class Count(
        val entry: TrackEntry,
        var ms: Long,
        var plays: Int,
    )

    fun fromBuckets(year: Int, summary: ReplaySummary, buckets: List<StoredBucket>): ReplayYearData {
        require(year in 1970..9999)
        val daily = sortedMapOf<String, Long>()
        val monthly = sortedMapOf<YearMonth, MutableMap<String, Count>>()
        val perTrackDay = HashMap<String, MutableMap<String, Long>>()

        for (bucket in buckets) {
            val month = runCatching { YearMonth.parse(bucket.month) }.getOrNull()
                ?.takeIf { it.year == year } ?: continue
            val counts = monthly.getOrPut(month) { LinkedHashMap() }

            for ((day, ms) in bucket.days) {
                if (ms <= 0L) continue
                val date = runCatching { month.atDay(day) }.getOrNull() ?: continue
                daily[date.toString()] = (daily[date.toString()] ?: 0L) + ms
            }

            for (entry in bucket.tracks) {
                if (entry.id.isBlank()) continue
                val count = counts[entry.id]
                if (count == null) {
                    counts[entry.id] = Count(entry, entry.ms.coerceAtLeast(0L), entry.plays.coerceAtLeast(0))
                } else {
                    count.ms += entry.ms.coerceAtLeast(0L)
                    count.plays += entry.plays.coerceAtLeast(0)
                }

                for ((date, stats) in entry.days) {
                    if (stats.ms <= 0L) continue
                    val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: continue
                    if (YearMonth.from(parsed) != month) continue
                    val dayTotals = perTrackDay.getOrPut(date) { HashMap() }
                    dayTotals[entry.id] = (dayTotals[entry.id] ?: 0L) + stats.ms
                }
            }
        }

        val tops = monthly.mapNotNull { (month, tracks) ->
            val winner = tracks.values
                .asSequence()
                .filter { it.ms > 0L }
                .sortedWith(
                    compareByDescending<Count> { it.ms }
                        .thenByDescending { it.plays }
                        .thenBy { it.entry.id },
                ).firstOrNull() ?: return@mapNotNull null
            val song = summary.songs.firstOrNull { it.song.videoId == winner.entry.id }?.song
                ?: winner.entry.toReplaySong()
            ReplayMonthTop(month, RankedSong(song, winner.ms, winner.plays))
        }

        val busiestDay = daily.entries
            .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
            .firstOrNull()
        val busiestSongId = busiestDay?.key?.let { date ->
            perTrackDay[date]?.entries
                ?.sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
                ?.firstOrNull()?.key
        }
        val busiestSong = summary.songs.firstOrNull { it.song.videoId == busiestSongId }?.song
        return ReplayYearData(
            year = year,
            summary = summary.copy(
                label = year.toString(),
                busiestDay = busiestDay?.key,
                busiestDayMs = busiestDay?.value ?: 0L,
            ),
            dayMs = daily.toMap(),
            monthTops = tops,
            busiestDaySong = busiestSong,
        )
    }

    private fun TrackEntry.toReplaySong(): Song = Song(
        videoId = id,
        title = title,
        artist = artist,
        thumbnailUrl = art,
        artistId = artistId,
        albumId = albumId,
        albumName = album,
    )
}
