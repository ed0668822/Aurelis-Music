// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapePlayerSizingTest {
    @Test fun equalColumnsMatchTheBitChordLandscapeReferenceAtAllWidths() {
        for (width in listOf(568.dp, 640.dp, 760.dp, 800.dp, 1100.dp)) {
            assertEquals(0.5f, landscapeArtworkWeight(width), 0.001f)
        }
    }

    @Test fun narrowPhonesAndLargeFontsKeepCompactTransportSpace() {
        assertTrue(landscapeIsCompact(320.dp, 1f))
        assertTrue(landscapeIsCompact(420.dp, 1f))
        assertTrue(landscapeIsCompact(550.dp, 1.5f))
        org.junit.Assert.assertFalse(landscapeIsCompact(700.dp, 1f))
        assertEquals(12.dp, landscapeTopHandlePadding(320.dp))
        assertEquals(24.dp, landscapeTopHandlePadding(500.dp))
    }

    @Test fun queueAndLyricsHaveReferenceGutterOnTabletsAndPhones() {
        for (gutter in listOf(20.dp, 30.dp)) {
            assertEquals(gutter, landscapePanePadding(PlayerPane.Main, gutter))
            assertEquals(16.dp, landscapePanePadding(PlayerPane.Lyrics, gutter))
            assertEquals(16.dp, landscapePanePadding(PlayerPane.Queue, gutter))
        }
    }

    @Test fun compactAndRoomyMarginsMatchReference() {
        for (width in listOf(568.dp, 640.dp, 800.dp)) {
            assertEquals(20.dp, landscapeSidePadding(width, compact = true))
            assertEquals(30.dp, landscapeSidePadding(width, compact = false))
        }
    }
}
