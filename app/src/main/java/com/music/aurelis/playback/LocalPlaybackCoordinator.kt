// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult

/** Service-owned controls shared by every UI controller through the single session. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class LocalPlaybackCoordinator(private val player: Player) {
    companion object {
        val COMMAND = SessionCommand("com.music.aurelis.PLAYBACK_CONTROL", Bundle.EMPTY)
    }
    private var session: MediaSession? = null
    private val timer = SleepTimer(player, onChange = { publish() })
    fun bind(session: MediaSession) { this.session = session; publish() }
    private fun publish() {
        session?.setSessionExtras(Bundle().apply {
            putLong("sleep_deadline", timer.deadlineMs)
            putBoolean("sleep_after_track", timer.afterTrack)
        })
    }
    fun execute(args: Bundle): SessionResult {
        val success = when (args.getString("action")) {
            "sleep" -> timer.start(args.getLong("duration"))
            "sleep_track" -> timer.startAfterTrack()
            "sleep_cancel" -> { timer.cancel(); true }
            else -> false
        }
        return if (success) SessionResult(SessionResult.RESULT_SUCCESS)
        else SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
    }
    fun close() { timer.close(); session = null }
}
