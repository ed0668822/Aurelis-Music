// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult

/** Service-owned controls shared by every UI controller through the single session. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class LocalPlaybackCoordinator(private val player: Player) : Player.Listener {
    companion object {
        val COMMAND = SessionCommand("com.music.aurelis.PLAYBACK_CONTROL", Bundle.EMPTY)
    }
    private val exo get() = player as? androidx.media3.exoplayer.ExoPlayer
    private fun highResolutionOutput(): Boolean = unsupportedControlPcm(exo?.audioFormat?.pcmEncoding)
    init { player.addListener(this) }
    override fun onEvents(player: Player, events: Player.Events) {
        if (highResolutionOutput()) {
            if (player.playbackParameters.speed != 1f) player.setPlaybackSpeed(1f)
            if (exo?.skipSilenceEnabled == true) exo?.skipSilenceEnabled = false
        }
        publish()
    }
    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) { publish() }
    private var profileBinding: ProfilePlaybackBinding? = null
    private var historyDrain: kotlinx.coroutines.Job? = null
    private var historyMessage: String? = null
    private fun historyStatus(status: com.music.aurelis.data.stats.ListeningCaptureStatus) {
        historyMessage = when {
            status.issue == com.music.aurelis.data.stats.ListeningStoreIssue.NEWER_SCHEMA -> "El historial usa una versión más reciente; no se registran nuevas escuchas."
            status.issue == com.music.aurelis.data.stats.ListeningStoreIssue.CORRUPT -> "No se puede registrar el historial. Sus datos se conservan para recuperarlos."
            status.issue == com.music.aurelis.data.stats.ListeningStoreIssue.PROFILE_READ_ONLY -> "El historial está en modo de solo lectura."
            status.missed > 0 -> "Algunos intervalos de escucha no se pudieron guardar."
            status.issue != null -> "Hay escuchas pendientes que no se han podido guardar."
            else -> null
        }
        publish()
    }
    fun bindProfile(context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope,
        songs: (suspend () -> List<com.music.aurelis.data.model.LocalSong>)? = null) {
        historyDrain = profileBinding?.close() ?: historyDrain
        profileBinding = if (songs == null) ProfilePlaybackBinding(context, player, scope, captureStatus = ::historyStatus)
            else ProfilePlaybackBinding(context, player, scope, captureStatus = ::historyStatus, songs = songs)
    }
    fun onUserQueueCommand() { profileBinding?.cancelRestore() }
    private var autoplay: LocalAutoplayBinding? = null
    private var autoplayStatus: String? = null
    fun bindAutoplay(scope: kotlinx.coroutines.CoroutineScope, load: suspend () -> List<com.music.aurelis.data.model.LocalSong>) {
        autoplay?.close()
        autoplay = LocalAutoplayBinding(player, scope, com.music.aurelis.data.SettingsRepository.autoplay, load) {
            autoplayStatus = it; publish()
        }
        publish()
    }
    private var session: MediaSession? = null
    private val timer = SleepTimer(player, onChange = { publish() })
    fun bind(session: MediaSession) { this.session = session; publish() }
    private fun publish() {
        session?.setSessionExtras(Bundle().apply {
            putLong("sleep_deadline", timer.deadlineMs)
            putLong("control_observed_at", android.os.SystemClock.elapsedRealtime())
            putBoolean("sleep_after_track", timer.afterTrack)
            putString("listening_history_message", historyMessage)
            putBoolean("autoplay", com.music.aurelis.data.SettingsRepository.autoplay.value)
            putBoolean("autoplay_available", autoplay != null && !com.music.aurelis.data.ProfileRepository.state.value.readOnly)
            putString("autoplay_status", autoplayStatus)
            putFloat("speed", player.playbackParameters.speed)
            putBoolean("skip_silence", exo?.skipSilenceEnabled == true)
            putBoolean("speed_available", player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH) && !highResolutionOutput())
            putBoolean("silence_available", exo != null && !highResolutionOutput())
            putString("control_restriction", if (highResolutionOutput()) "No disponible con salida PCM de alta resolución." else null)
        })
    }
    fun execute(args: Bundle): SessionResult {
        val success = when (args.getString("action")) {
            "autoplay" -> {
                if (autoplay == null || com.music.aurelis.data.ProfileRepository.state.value.readOnly) false
                else { com.music.aurelis.data.SettingsRepository.setAutoplay(args.getBoolean("enabled")); true }
            }
            "speed" -> {
                val speed = args.getFloat("speed", Float.NaN)
                if (!speed.isFinite() || speed !in 0.5f..2f ||
                    !player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH) || (speed != 1f && highResolutionOutput())) false
                else { player.setPlaybackSpeed(speed); true }
            }
            "silence" -> {
                val enabled = args.getBoolean("enabled")
                if (exo == null || (enabled && highResolutionOutput())) false
                else { exo?.skipSilenceEnabled = enabled; true }
            }
            "sleep" -> timer.start(args.getLong("duration"))
            "sleep_track" -> timer.startAfterTrack()
            "sleep_cancel" -> { timer.cancel(); true }
            else -> false
        }
        publish()
        return if (success) SessionResult(SessionResult.RESULT_SUCCESS)
        else SessionResult(androidx.media3.session.SessionError.ERROR_BAD_VALUE)
    }
    fun close(): kotlinx.coroutines.Job? {
        historyDrain = profileBinding?.close() ?: historyDrain
        profileBinding = null; autoplay?.close(); autoplay = null; player.removeListener(this); timer.close(); session = null
        return historyDrain
    }
}

/** The existing sink uses its float path for these encodings, bypassing Sonic/silence. */
internal fun unsupportedControlPcm(encoding: Int?): Boolean = when (encoding) {
    androidx.media3.common.C.ENCODING_PCM_FLOAT, androidx.media3.common.C.ENCODING_PCM_24BIT,
    androidx.media3.common.C.ENCODING_PCM_32BIT -> true
    else -> false
}
