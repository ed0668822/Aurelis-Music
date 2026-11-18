// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapePlayerSizingTest {
    @Test fun compactPhoneReservesSpaceForLyricsAndQueue() {
        assertEquals(0.42f, landscapeArtworkWeight(568.dp), 0.001f)
        assertEquals(12.dp, landscapeSidePadding(568.dp, compact = true))
        assertTrue(landscapeArtworkWeight(568.dp) < 0.5f)
    }

    @Test fun wideScreensKeepBalancedColumnsAndSafeInsets() {
        assertEquals(0.46f, landscapeArtworkWeight(640.dp), 0.001f)
        assertEquals(0.5f, landscapeArtworkWeight(800.dp), 0.001f)
        assertEquals(20.dp, landscapeSidePadding(800.dp, compact = true))
        assertEquals(24.dp, landscapeSidePadding(800.dp, compact = false))
        assertEquals(12.dp, landscapeSidePadding(640.dp, compact = false))
    }
}
