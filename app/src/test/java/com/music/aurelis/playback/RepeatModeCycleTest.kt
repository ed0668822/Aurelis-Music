package com.music.aurelis.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatModeCycleTest {
    @Test fun repeatCyclesOffAllOneOff() {
        assertEquals(Player.REPEAT_MODE_ALL, nextRepeatMode(Player.REPEAT_MODE_OFF))
        assertEquals(Player.REPEAT_MODE_ONE, nextRepeatMode(Player.REPEAT_MODE_ALL))
        assertEquals(Player.REPEAT_MODE_OFF, nextRepeatMode(Player.REPEAT_MODE_ONE))
    }
}
