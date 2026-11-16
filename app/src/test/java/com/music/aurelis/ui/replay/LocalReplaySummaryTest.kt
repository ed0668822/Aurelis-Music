// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.replay

import com.music.aurelis.data.LocalListening
import org.junit.Assert.*
import org.junit.Test

class LocalReplaySummaryTest {
    @Test fun minutesAreRoundedAfterSummingIntervalsAndMissingMediaRemainsInHistory() {
        val history = mapOf("b" to LocalListening(30_000, 1), "a" to LocalListening(30_000, 2))
        val summary = localReplaySummary(history, emptyList())
        assertEquals(60_000L, summary.totalMs)
        assertEquals("1", formatMinutes(summary.totalMs))
        assertEquals(3L, summary.totalPlays)
        assertEquals(listOf("a", "b"), summary.rows.map { it.key })
        assertEquals(listOf(1, 2), summary.rows.map { it.rank })
        assertEquals(summary.totalMs, totalListeningMs(history))
    }
    @Test fun totalsDoNotOverflowAndRankUsesListeningTimeRatherThanMostRecentVisit() {
        val history = mapOf("long" to LocalListening(Long.MAX_VALUE, 4, 1), "recent" to LocalListening(60_000, 1, 100))
        val summary = localReplaySummary(history, emptyList())
        assertEquals(Long.MAX_VALUE, summary.totalMs)
        assertEquals("long", summary.rows.first().key)
        assertEquals(5L, summary.totalPlays)
        assertEquals(0L, totalListeningMs(emptyMap()))
    }
}
