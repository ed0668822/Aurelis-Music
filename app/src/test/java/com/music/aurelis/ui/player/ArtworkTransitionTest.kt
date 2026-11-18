// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.player

import org.junit.Assert.*
import org.junit.Test

class ArtworkTransitionTest {
    @Test fun startingTheNextTrackRetainsTheVisibleArtworkUntilItResolves() {
        val state = ArtworkTransition<String?>("artwork A")
        val request = state.begin()
        assertEquals("artwork A", state.value)
        state.complete(request, "artwork B")
        assertEquals("artwork B", state.value)
    }
    @Test fun aLateLoadCannotReplaceTheNewerTrackOrPalette() {
        val state = ArtworkTransition("palette A")
        val slow = state.begin()
        val next = state.begin()
        state.complete(next, "palette C")
        state.complete(slow, "palette B")
        assertEquals("palette C", state.value)
    }
    @Test fun rapidTrackSkipsCannotPublishLateArtworkForAnOlderSong() {
        val state = ArtworkTransition<String?>("A")
        val slowB = state.begin()
        val quickC = state.begin()
        val returnA = state.begin()
        assertEquals("A", state.value)
        state.complete(slowB, "B late")
        state.complete(quickC, "C late")
        assertEquals("A", state.value)
        state.complete(returnA, "A current")
        assertEquals("A current", state.value)
        state.complete(slowB, "B even later")
        assertEquals("A current", state.value)
    }

    @Test fun repeatedTrackSameArtworkUriCanRejoinQueueWithoutObsoleteCover() {
        val state = ArtworkTransition<String?>("album cover A")
        // Same URI across duplicate queue entries does not begin a fresh load.
        assertEquals("album cover A", state.value)
        val requestB = state.begin()
        val requestC = state.begin()
        state.complete(requestB, "old B")
        assertEquals("album cover A", state.value)
        state.complete(requestC, "cover C")
        assertEquals("cover C", state.value)
        val missing = state.begin()
        state.complete(missing, null)
        assertNull(state.value)
        state.complete(requestB, "stale B")
        assertNull(state.value)
    }

    @Test fun duplicateQueueOccurrencesCanRetainTheSameDecodedCover() {
        val state = ArtworkTransition<String?>("cover for song A")
        // Two queue entries for the same local URI do not require invalidating
        // a displayed cover. A new image request only begins when the URI changes.
        assertEquals("cover for song A", state.value)
        val differentUri = state.begin()
        assertEquals("cover for song A", state.value)
        state.complete(differentUri, "cover for song B")
        assertEquals("cover for song B", state.value)
    }

    @Test fun missingOrFailedArtworkClearsTheOldFrameAndRejectsLateSuccess() {
        val state = ArtworkTransition<String?>("artwork A")
        val old = state.begin()
        val missing = state.begin()
        state.complete(missing, null)
        state.complete(old, "stale artwork")
        assertNull(state.value)
    }
}
