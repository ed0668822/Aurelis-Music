package com.music.aurelis.playback.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartCastRankingTest {
    private fun cast(id: String, name: String) = SmartCastRanking.AdvertisedDevice(id, name)
    private fun dlna(id: String, name: String) = SmartCastRanking.AdvertisedDevice(id, name)

    @Test fun exactUniqueNamesGroupAsOneTVWithSafeFallback() {
        val results = SmartCastRanking.choose(
            listOf(cast("cast-a", "Living Room TV")),
            listOf(dlna("dlna-a", "living room tv")),
            customReceiverEnabled = true,
        )
        assertEquals(1, results.size)
        val only = results.single()
        assertEquals("cast-a", only.castId)
        assertEquals("dlna-a", only.dlnaId)
        assertEquals(SmartCastRanking.Protocol.CAST, only.preferred)
        assertEquals(SmartCastRanking.Protocol.DLNA, only.fallback)
        assertTrue(only.lyricsOnTv)
    }

    @Test fun withoutCustomReceiverCastAudioStillTakesPriorityButNoTVLyricsClaim() {
        val choice = SmartCastRanking.choose(
            listOf(cast("c", "Bedroom TV")),
            listOf(dlna("d", "Bedroom TV")),
            customReceiverEnabled = false,
        ).single()
        assertEquals(SmartCastRanking.Protocol.CAST, choice.preferred)
        assertFalse(choice.lyricsOnTv)
    }

    @Test fun unrelatedReceiversAreNeverAssumedToBeTheSameTelevision() {
        val results = SmartCastRanking.choose(
            listOf(cast("c", "Family TV")),
            listOf(dlna("d", "Family Room TV")),
            customReceiverEnabled = true,
        )
        assertEquals(2, results.size)
        assertEquals(0, results.count { it.fallback != null })
    }

    @Test fun duplicateFriendlyNamesStaySeparateToAvoidWrongTelevision() {
        val results = SmartCastRanking.choose(
            listOf(cast("c1", "TV"), cast("c2", "TV")),
            listOf(dlna("d1", "TV")),
            customReceiverEnabled = true,
        )
        assertEquals(3, results.size)
        assertTrue(results.none { it.fallback != null })
        assertEquals(2, results.count { it.castId != null })
    }

    @Test fun nonCastTelevisionStillHasDLNAOutput() {
        val choice = SmartCastRanking.choose(emptyList(),
            listOf(dlna("living", "Living Room")), customReceiverEnabled = false).single()
        assertNull(choice.castId)
        assertEquals("living", choice.dlnaId)
        assertEquals(SmartCastRanking.Protocol.DLNA, choice.preferred)
        assertNull(choice.fallback)
    }
}
