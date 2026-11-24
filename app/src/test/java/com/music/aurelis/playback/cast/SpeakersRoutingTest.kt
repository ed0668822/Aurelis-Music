// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.cast

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakersRoutingTest {
    private class FakeSpeaker(override var active: Boolean) : RemoteSpeaker {
        override var sink: CastSink? = null
        override var playWhenReady = false
        override var isPlaying = false
        override var playbackState = Player.STATE_READY
        override var positionMs = 0L
        override var durationMs = 1000L
        override var deviceVolume = 25
        var played = 0
        var paused = 0
        var queueChanges = 0
        var transitions = 0
        var repeated = 0
        var lastSeek = -1L
        var lastVolume = -1f

        override fun play() { played++ }
        override fun pause() { paused++ }
        override fun seekTo(positionMs: Long) { lastSeek = positionMs }
        override fun setVolume(fraction: Float) { lastVolume = fraction }
        override fun onLocalTransition(item: MediaItem?, reason: Int) { transitions++ }
        override fun onLocalQueueChanged() { queueChanges++ }
        override fun onLocalRepeatChanged(repeatMode: Int) { repeated++ }
        override fun onLocalStartedPlaying(player: Player) = Unit
    }

    @Test
    fun routesTransportAndClockToActiveRemoteOnly() {
        val first = FakeSpeaker(false)
        val cast = FakeSpeaker(true).apply { positionMs = 570L; deviceVolume = 70 }
        val speakers = Speakers(listOf(first, cast))
        assertTrue(speakers.active)
        assertEquals(570L, speakers.positionMs)
        assertEquals(70, speakers.deviceVolume)
        speakers.play()
        speakers.seekTo(620L)
        speakers.setVolume(0.4f)
        assertEquals(0, first.played)
        assertEquals(1, cast.played)
        assertEquals(620L, cast.lastSeek)
        assertEquals(0.4f, cast.lastVolume, 0.0001f)
        cast.active = false
        assertFalse(speakers.active)
        speakers.pause()
        assertEquals(1, first.paused)
        assertEquals(0, cast.paused)
    }

    @Test
    fun sharesSinkAndQueueChangesWithoutDuplicatingPlayback() {
        val cast = FakeSpeaker(true)
        val watch = FakeSpeaker(false)
        val speakers = Speakers(listOf(cast, watch))
        val sink = object : CastSink {
            override fun onRemotePlaybackChanged() = Unit
            override fun onRemoteRouteChanged() = Unit
            override fun onRemoteVolumeChanged() = Unit
        }
        speakers.sink = sink
        assertSame(sink, cast.sink)
        assertSame(sink, watch.sink)
        speakers.onLocalQueueChanged()
        speakers.onLocalTransition(null, 0)
        speakers.onLocalRepeatChanged(Player.REPEAT_MODE_ALL)
        assertEquals(1, cast.queueChanges)
        assertEquals(1, watch.queueChanges)
        assertEquals(1, cast.transitions)
        assertEquals(1, watch.transitions)
        assertEquals(1, cast.repeated)
        assertEquals(1, watch.repeated)
    }
}
