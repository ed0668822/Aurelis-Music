// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import com.music.aurelis.data.model.QueueTier
import com.music.aurelis.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAutoplayRankerTest {
    private fun track(id: String, artist: String): Song = Song(
        videoId = "content://media/audio/$id",
        title = "Song $id",
        artist = artist,
        thumbnailUrl = null,
        localUri = "content://media/audio/$id",
    )

    @Test fun excludesAlreadyQueuedAndSeedAndDuplicates() {
        val seed = track("1", "A")
        val queued = track("2", "B")
        val other = track("3", "C")
        val result = LocalAutoplayRanker.suggest(
            seed, listOf(seed, queued, other, other), listOf(queued),
            emptySet(), emptyMap(), 10,
        )
        assertEquals(listOf(other.localUri), result.map { it.localUri })
        assertEquals(QueueTier.AUTOPLAY, result.first().queueTier)
    }

    @Test fun prefersLikedAndPlayedWithoutNeedingBpm() {
        val seed = track("1", "A")
        val favorite = track("2", "B")
        val ordinary = track("3", "C")
        val result = LocalAutoplayRanker.suggest(
            seed, listOf(ordinary, favorite), emptyList(), setOf(favorite.localUri!!),
            mapOf(favorite.videoId to 8), 1,
        )
        assertEquals(favorite.localUri, result.first().localUri)
    }

    @Test fun cachedBpmCanPreferNearTempoCandidate() {
        val seed = track("seed", "A")
        val close = track("close", "B")
        val far = track("far", "C")
        val bpm = mapOf(seed.videoId to 126.0, close.videoId to 125.0, far.videoId to 85.0)
        val result = LocalAutoplayRanker.suggest(
            seed, listOf(far, close), emptyList(), emptySet(), emptyMap(), 1,
            bpmFor = { bpm[it] },
        )
        assertEquals(close.localUri, result.first().localUri)
    }

    @Test fun prefersVarietyRatherThanRepeatingOneArtist() {
        val seed = track("seed", "A")
        val pool = listOf(track("a", "A"), track("b", "A"), track("c", "C"))
        val result = LocalAutoplayRanker.suggest(
            seed, pool, emptyList(), emptySet(), emptyMap(), 3,
        )
        assertTrue(result.take(2).map { it.artist }.distinct().size == 2)
    }

    @Test fun noSeedLocalUriMeansNoOfflineRecommendations() {
        val remote = track("1", "A").copy(localUri = null)
        assertTrue(LocalAutoplayRanker.suggest(
            remote, listOf(track("2", "B")), emptyList(),
            emptySet(), emptyMap(), 2,
        ).isEmpty())
    }
}
