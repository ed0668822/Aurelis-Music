// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import androidx.media3.common.Player
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Extends only a naturally exhausted, still-requested local session. */
internal class LocalAutoplayBinding(
    private val player: Player,
    private val scope: CoroutineScope,
    private val enabled: StateFlow<Boolean>,
    private val load: suspend () -> List<LocalSong>,
    private val status: (String?) -> Unit = {},
) : Player.Listener {
    private var job: Job? = null
    private var applying = false
    private var closed = false
    private var lastAttempt: List<String>? = null
    private val preferenceJob: Job
    init {
        player.addListener(this)
        preferenceJob = scope.launch { enabled.collect { lastAttempt = null; status(null); evaluate() } }
    }
    private fun eligible() = !closed && enabled.value && player.mediaItemCount > 0 &&
        player.playWhenReady && player.playbackState == Player.STATE_ENDED &&
        player.playerError == null && player.repeatMode == Player.REPEAT_MODE_OFF &&
        !player.hasNextMediaItem() && player.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS) &&
        player.isCommandAvailable(Player.COMMAND_GET_TIMELINE) &&
        player.isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM) &&
        player.isCommandAvailable(Player.COMMAND_PREPARE) && player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)

    private fun fingerprint(): List<String> = (0 until player.mediaItemCount).map {
        player.getMediaItemAt(it).queueEntryId ?: "$it:${player.getMediaItemAt(it).mediaId}"
    } + "current:${player.currentMediaItemIndex}"

    override fun onEvents(player: Player, events: Player.Events) { evaluate() }
    private fun evaluate() {
        if (applying) return
        if (!eligible()) {
            job?.cancel(); job = null
            if (!player.playWhenReady || player.playbackState != Player.STATE_ENDED) lastAttempt = null
            return
        }
        val token = fingerprint()
        if (job?.isActive == true || lastAttempt == token) return
        val current = player.currentMediaItem?.toLocalSong() ?: return
        val excluded = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }.toSet()
        lastAttempt = token
        job = scope.launch {
            try {
                status(null)
                val candidates = AutoplayEngine().suggestNextTracks(current, load(), excluded)
                currentCoroutineContext().ensureActive()
                if (!eligible() || token != fingerprint()) return@launch
                if (candidates.isEmpty()) { status("Sin más canciones locales disponibles."); return@launch }
                applying = true
                try {
                    val next = player.mediaItemCount
                    player.addMediaItems(candidates.map { it.copy(queueTier = QueueTier.AUTOPLAY).toQueueMediaItem() })
                    player.seekToDefaultPosition(next)
                    player.prepare()
                    player.play()
                } finally { applying = false }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { status("No se pudo consultar el catálogo local.") }
        }
    }
    fun close() {
        closed = true; job?.cancel(); preferenceJob.cancel(); player.removeListener(this)
    }
}
