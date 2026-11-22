// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** The technical AutoMix text remains verbatim, as in BitChord 1.10. */
class AutomixOriginalDisplayTest {
    @Test fun keepsMixingAndBeatReasonsUnabridged() {
        val raw = "mixing · 116 BPM · filter mix in 4 s · 8 s · automix: beatAligned · no loop: analysisUnavailable"
        assertEquals(raw, automixDetailForDisplay(raw))
    }

    @Test fun preservesLocalizedOrEmptySourceMessages() {
        assertEquals("", automixDetailForDisplay(""))
        assertEquals("mezcla · 102 BPM", automixDetailForDisplay("mezcla · 102 BPM"))
    }
}
