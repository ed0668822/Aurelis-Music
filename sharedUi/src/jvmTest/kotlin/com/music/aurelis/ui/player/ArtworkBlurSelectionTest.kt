// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtworkBlurSelectionTest {
    @Test fun missingArtworkNeverShowsThePreviousCover() {
        assertNull(selectArtworkBlur<String>(null, "previous", "previous-blur", null))
    }

    @Test fun oldCoverIsHeldOnlyWhileAnotherCoverLoads() {
        assertEquals("previous-blur", selectArtworkBlur("next", "previous", "previous-blur", null))
        assertEquals("next-blur", selectArtworkBlur("next", "previous", "previous-blur", "next-blur"))
    }

    @Test fun failedCurrentCoverClearsStaleBackdrop() {
        assertNull(selectArtworkBlur<String>("broken", "broken", null, null))
    }

    @Test fun cachedCoverIsUsedWhenAlreadySelected() {
        assertEquals("cached", selectArtworkBlur("song", "older", null, "cached"))
        assertEquals("current", selectArtworkBlur("song", "song", "current", "cached"))
    }
}
