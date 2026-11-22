// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.data.NerdStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Icons copied back from BitChord 1.10 must stay available to the player. */
class PlayerIconParityTest {
    @Test fun outputAirPlayHasOriginalVectorDimensions() {
        val icon = AurelisIcons.AirPlay
        assertEquals("bc_airplay", icon.name)
        assertEquals(24f, icon.defaultWidth.value, 0.001f)
        assertTrue(icon.defaultHeight.value > 23f)
    }

    @Test fun dolbyDetectionAndLosslessCodecAreDistinct() {
        assertTrue(NerdStats.isDolbyAtmosMime("audio/eac3-joc"))
        assertTrue(!NerdStats.isLosslessMime("audio/eac3-joc"))
    }
}
