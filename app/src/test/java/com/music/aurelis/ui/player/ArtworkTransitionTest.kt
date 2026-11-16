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
    @Test fun missingOrFailedArtworkClearsTheOldFrameAndRejectsLateSuccess() {
        val state = ArtworkTransition<String?>("artwork A")
        val old = state.begin()
        val missing = state.begin()
        state.complete(missing, null)
        state.complete(old, "stale artwork")
        assertNull(state.value)
    }
}
