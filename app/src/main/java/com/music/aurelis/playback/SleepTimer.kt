// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.os.Handler
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

/** Monotonic deadline owned by the service, independent of the player screen. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class SleepTimer(
    private val player: Player,
    private val handler: Handler = Handler(player.applicationLooper),
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val onChange: () -> Unit = {},
) : Player.Listener {
    var deadlineMs: Long = 0L; private set
    var afterTrack: Boolean = false; private set
    private var track: String? = null
    private var closed = false
    private var previousPauseAtEnd = false
    private val exo get() = player as? androidx.media3.exoplayer.ExoPlayer
    private val check = object : Runnable {
        override fun run() {
            if (closed || deadlineMs == 0L) return
            val remaining = deadlineMs - clock()
            if (remaining <= 0) expire() else {
                onChange()
                handler.postDelayed(this, remaining.coerceAtMost(1000L))
            }
        }
    }
    init { player.addListener(this) }

    fun start(durationMs: Long): Boolean {
        if (closed || durationMs !in 1..43_200_000L) return false
        cancel()
        deadlineMs = clock() + durationMs
        handler.postDelayed(check, durationMs.coerceAtMost(1000L))
        onChange()
        return true
    }
    fun startAfterTrack(): Boolean {
        if (closed || player.currentMediaItem == null) return false
        cancel(); afterTrack = true; track = player.currentMediaItem?.queueEntryId
        previousPauseAtEnd = exo?.pauseAtEndOfMediaItems ?: false
        exo?.pauseAtEndOfMediaItems = true
        onChange(); return true
    }
    fun cancel() {
        handler.removeCallbacks(check)
        if (afterTrack) exo?.pauseAtEndOfMediaItems = previousPauseAtEnd
        deadlineMs = 0L; afterTrack = false; track = null
        onChange()
    }
    private fun expire() {
        cancel()
        player.pause()
    }
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (!afterTrack) return
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) expire()
        else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK || mediaItem?.queueEntryId != track) cancel()
    }
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (afterTrack && !playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) expire()
    }
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (afterTrack && playbackState == Player.STATE_ENDED) expire()
    }
    fun close() {
        if (closed) return
        closed = true; cancel(); player.removeListener(this)
    }
}
