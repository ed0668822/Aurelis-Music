package com.music.aurelis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A rejected beatmatch must be shown as a safe fallback, not an AutoMix error. */
class AutomixOriginalDisplayTest {
    @Test fun incompatibleTempoKeepsCountdownButExplainsSafeFallback() {
        val raw = "Automix · crossfading in 152 s · 4 s · automix:incompatibleTempo" +
            " · no loop: outgoing: 92 loops: 10 quiet, 82 sung (lyrics)"
        val displayed = automixDetailForDisplay(raw)
        assertTrue(displayed.contains("crossfading in 152 s · 4 s"))
        assertTrue(displayed.contains("different tempos (safe blend, no speed change)"))
        assertTrue(displayed.contains("no suitable loop (using blend)"))
        assertFalse(displayed.contains("automix:incompatibleTempo"))
        assertFalse(displayed.contains("92 loops"))
    }

    @Test fun fallbackLabelsDoNotHideOtherMusicOrSourceDetails() {
        val raw = "Automix · mixing · 116 BPM · automix:unreliableBeats"
        assertEquals(
            "Automix · mixing · 116 BPM · beat detection uncertain (safe blend)",
            automixDetailForDisplay(raw),
        )
        assertEquals(
            "Automix · filter mix in 4 s · beat analysis unavailable (safe blend)",
            automixDetailForDisplay("Automix · filter mix in 4 s · automix:analysisUnavailable"),
        )
        assertEquals("", automixDetailForDisplay(""))
        assertEquals("mezcla · 102 BPM", automixDetailForDisplay("mezcla · 102 BPM"))
    }
}
