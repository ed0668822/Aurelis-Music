// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SongInfoMovementTest {
    @Test fun nextAndPreviousHaveOppositeDirections() {
        assertEquals(1, songInfoSlideDirection(1, 2, 4))
        assertEquals(-1, songInfoSlideDirection(2, 1, 4))
    }

    @Test fun queueWrapDoesNotFlipTransportDirection() {
        assertEquals(1, songInfoSlideDirection(3, 0, 4))
        assertEquals(-1, songInfoSlideDirection(0, 3, 4))
    }

    @Test fun invalidQueuePositionsUseStableFallback() {
        assertEquals(1, songInfoSlideDirection(-1, 1, 4))
        assertEquals(1, songInfoSlideDirection(0, 0, 1))
    }
}
