// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.ui.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.exoplayer.ExoPlayer
import com.music.aurelis.playback.AutoMixVisualFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/** Current, preloaded and retiring Canvas players share this limit. Main only. */
internal object CanvasDecoders {
    private const val MAX_DECODERS = 2
    private const val DEFERRED_RELEASE_MS = 1_000L
    private val inUse = MutableStateFlow(0)
    private val mainHandler = Handler(Looper.getMainLooper())

    suspend fun open(context: Context): Lease? {
        while (inUse.value >= MAX_DECODERS) {
            inUse.first { it < MAX_DECODERS }
        }
        val player = runCatching { newCanvasDecoder(context) }.getOrNull() ?: return null
        inUse.value++
        return Lease(player) { inUse.value-- }
    }

    class Lease(val player: ExoPlayer, private val returnSlot: () -> Unit) {
        var closed = false
            private set

        /** Set once the deferred release has run; a surface destroyed after that is not ours to hold. */
        var released = false
            private set

        /**
         * Stop loading immediately and keep the slot until the player and its
         * surface are released. Delay blocking teardown to give the reveal time
         * to settle, with a second delay at most if the mix is still active.
         */
        fun close(afterRelease: () -> Unit = {}) {
            if (closed) return
            closed = true
            runCatching { player.stop() }
            val release = Runnable {
                try {
                    runCatching { player.release() }
                    released = true
                    afterRelease()
                } finally {
                    returnSlot()
                }
            }
            mainHandler.postDelayed({
                if (AutoMixVisualFeed.state.value?.mixActive == true) {
                    mainHandler.postDelayed(release, DEFERRED_RELEASE_MS)
                } else {
                    release.run()
                }
            }, DEFERRED_RELEASE_MS)
        }
    }
}
