// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis

import com.music.aurelis.playback.queuedDurationMillis
import org.junit.Assert.assertEquals
import org.junit.Test

class QueuedDurationTest {
    @Test
    fun `a streamed track keeps the runtime its URI carries`() {
        assertEquals(225_000L, queuedDurationMillis("225", "9:99"))
    }

    @Test
    fun `a device file with no d parameter falls back to its queue row runtime`() {
        assertEquals(167_000L, queuedDurationMillis(null, "2:47"))
        assertEquals(3_723_000L, queuedDurationMillis(null, "1:02:03"))
    }

    @Test
    fun `an unusable URI value falls through to the row runtime`() {
        assertEquals(167_000L, queuedDurationMillis("abc", "2:47"))
        assertEquals(167_000L, queuedDurationMillis("0", "2:47"))
    }

    @Test
    fun `no runtime anywhere stays unknown`() {
        assertEquals(0L, queuedDurationMillis(null, null))
        assertEquals(0L, queuedDurationMillis("", "not a time"))
    }
}
