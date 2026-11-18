// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui

import androidx.compose.ui.unit.dp
import com.music.aurelis.ui.screens.portraitArtworkSize
import org.junit.Assert.assertEquals
import org.junit.Test

class PortraitPlayerSizingTest {
    @Test fun phonePortraitBoundsArtworkByAvailableHeightAndTitleControls() {
        assertEquals(320.dp, portraitArtworkSize(320.dp, 568.dp))
        assertEquals(280.dp, portraitArtworkSize(320.dp, 540.dp))
        assertEquals(360.dp, portraitArtworkSize(360.dp, 800.dp))
        assertEquals(280.dp, portraitArtworkSize(360.dp, 540.dp))
    }

    @Test fun tabletPortraitKeepsReferenceMaxWidthAndCentresTheArt() {
        assertEquals(560.dp, portraitArtworkSize(800.dp, 1200.dp))
        assertEquals(540.dp, portraitArtworkSize(1000.dp, 800.dp))
        assertEquals(0.dp, portraitArtworkSize((-10).dp, 900.dp))
        assertEquals(0.dp, portraitArtworkSize(360.dp, (-5).dp))
    }
}
