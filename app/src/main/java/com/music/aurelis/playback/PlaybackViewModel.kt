// Aurelis © 2026. GPL-3.0-or-later
// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.PlaybackException
import androidx.media3.common.C
import com.music.aurelis.R
import kotlinx.coroutines.CancellationException
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.data.lyrics.LyricsRepository
import com.music.aurelis.data.lyrics.LyricsSource
import com.music.aurelis.data.lyrics.LyricsTranslation
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.stats.ListeningStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface LyricsState {
    data object None : LyricsState
    data class Loading(val source: LyricsSource? = null) : LyricsState
    data class Loaded(
        val lines: List<LyricLine>,
        val source: LyricsSource,
        val translation: TranslationState = TranslationState.None,
    ) : LyricsState
    data object Missing : LyricsState
}

sealed interface TranslationState {
    data object None : TranslationState
    data object Loading : TranslationState
    data class Done(val lines: List<LyricLine>) : TranslationState
    data object Failed : TranslationState
}

data class PlaybackState(
    val currentSong: LocalSong? = null,
    val isPlaying: Boolean = false,
    val isPlayRequested: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<LocalSong> = emptyList(),
    val currentIndex: Int = -1,
    val isConnected: Boolean = false,
    val lyrics: LyricsState = LyricsState.None,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val canSeek: Boolean = false,
    val canNext: Boolean = false,
    val canPrevious: Boolean = false,
    val errorMessage: String? = null,
)

class PlaybackViewModel internal constructor(
    application: Application,
    private val connector: suspend (() -> Unit) -> Player,
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, { disconnected ->
        PlayerConnection.connect(application, disconnected)
    })

    private var controller: Player? = null
    private var connectionJob: Job? = null
    private var playRequestJob: Job? = null
    private var positionJob: Job? = null
    private var lyricsJob: Job? = null
    private var translationJob: Job? = null
    private var lastLyricsSongId: String? = null
    private var lastTickPositionMs = 0L

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            lastTickPositionMs = 0L
            syncState()
            ListeningStats.onTrackChanged(_state.value.currentSong?.id)
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionPolling()
            else { stopPositionPolling(); ListeningStats.flush() }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isLoading = playbackState == Player.STATE_BUFFERING) }
            if (playbackState == Player.STATE_READY) syncDuration()
        }
        override fun onTimelineChanged(timeline: Timeline, reason: Int) = syncState()
        override fun onEvents(player: Player, events: Player.Events) = syncState()
    }

    fun connect() {
        if (controller != null || connectionJob?.isActive == true) return
        connectionJob = viewModelScope.launch {
            try {
                val player = connector {
                    controller?.removeListener(playerListener)
                    controller?.release()
                    controller = null
                    stopPositionPolling()
                    _state.update { it.copy(isConnected = false, isPlaying = false, isPlayRequested = false,
                        isLoading = false, canSeek = false, canNext = false, canPrevious = false,
                        errorMessage = getApplication<Application>().getString(R.string.playback_connection_error)) }
                }
                controller = player
                player.addListener(playerListener)
                _state.update { it.copy(isConnected = true, errorMessage = null) }
                syncState()
                lastTickPositionMs = player.currentPosition.coerceAtLeast(0)
                if (player.isPlaying) startPositionPolling()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(isConnected = false, isLoading = false,
                    errorMessage = getApplication<Application>().getString(R.string.playback_connection_error)) }
            }
        }
    }

    fun playSongs(songs: List<LocalSong>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        playRequestJob?.cancel()
        playRequestJob = viewModelScope.launch {
            connect()
            connectionJob?.join()
            val player = controller ?: return@launch
            PlayerConnection.playSongs(player, songs, startIndex)
            syncState()
        }
    }

    fun resume() {
        val player = controller ?: return
        if (player.mediaItemCount == 0) return
        if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition()
        if (player.playbackState == Player.STATE_IDLE || player.playerError != null) player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.playWhenReady && player.playbackState != Player.STATE_ENDED && player.playerError == null) player.pause() else resume()
    }

    fun next() { if (_state.value.canNext) controller?.seekToNextMediaItem() }
    fun previous() { if (_state.value.canPrevious) controller?.seekToPrevious() }

    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() { controller?.let {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    } }

    fun retry() { if (controller == null) connect() else resume() }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        if (!_state.value.canSeek) return
        val target = positionMs.coerceIn(0, _state.value.durationMs.coerceAtLeast(0))
        lastTickPositionMs = target
        player.seekTo(target)
        syncState()
    }

    fun seekToIndex(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.seekToDefaultPosition(index)
        resume()
        syncState()
    }

    fun removeFromQueue(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount || index == player.currentMediaItemIndex) return
        player.removeMediaItem(index)
        syncState() // Timeline callbacks may already have updated the queue; never remove twice.
    }

    fun translateLyrics(targetLanguageTag: String) {
        val loaded = _state.value.lyrics as? LyricsState.Loaded ?: return
        val song = _state.value.currentSong ?: return
        translationJob?.cancel()
        _state.update { it.copy(lyrics = loaded.copy(translation = TranslationState.Loading)) }
        translationJob = viewModelScope.launch {
            when (val res = LyricsTranslation.translate(song.id, loaded.lines, targetLanguageTag)) {
                is LyricsTranslation.Result.Translated -> {
                    _state.update { current ->
                        val now = current.lyrics as? LyricsState.Loaded ?: return@update current
                        current.copy(lyrics = now.copy(translation = TranslationState.Done(res.lines)))
                    }
                }
                is LyricsTranslation.Result.SameLanguage -> {
                    _state.update { current ->
                        val now = current.lyrics as? LyricsState.Loaded ?: return@update current
                        current.copy(lyrics = now.copy(translation = TranslationState.None))
                    }
                }
                is LyricsTranslation.Result.Unavailable -> {
                    _state.update { current ->
                        val now = current.lyrics as? LyricsState.Loaded ?: return@update current
                        current.copy(lyrics = now.copy(translation = TranslationState.Failed))
                    }
                }
            }
        }
    }

    fun clearTranslation() {
        translationJob?.cancel()
        _state.update { current ->
            val now = current.lyrics as? LyricsState.Loaded ?: return@update current
            current.copy(lyrics = now.copy(translation = TranslationState.None))
        }
    }

    private fun syncState() {
        val ctrl = controller ?: return
        val currentIndex = ctrl.currentMediaItemIndex
        val mediaItem = ctrl.currentMediaItem
        val song = mediaItem?.toLocalSong()
        val queueSongs = (0 until ctrl.mediaItemCount).map { ctrl.getMediaItemAt(it).toLocalSong() }

        _state.update {
            it.copy(
                currentSong = song, currentIndex = currentIndex, isPlaying = ctrl.isPlaying,
                isPlayRequested = ctrl.playWhenReady && ctrl.playbackState != Player.STATE_ENDED && ctrl.playerError == null,
                isLoading = ctrl.playbackState == Player.STATE_BUFFERING,
                positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                durationMs = duration(ctrl),
                queue = queueSongs,
                repeatMode = ctrl.repeatMode, shuffleEnabled = ctrl.shuffleModeEnabled,
                canSeek = ctrl.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) && duration(ctrl) > 0,
                canNext = ctrl.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) && ctrl.hasNextMediaItem(),
                canPrevious = ctrl.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS) && ctrl.mediaItemCount > 0,
                errorMessage = ctrl.playerError?.let(::errorMessage),
            )
        }

        if (song != null && song.id != lastLyricsSongId) loadLyrics(song)
        else if (song == null) {
            lastLyricsSongId = null
            lyricsJob?.cancel()
            translationJob?.cancel()
            _state.update { it.copy(lyrics = LyricsState.None) }
        }
    }

    private fun duration(player: Player): Long = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }
        ?: player.currentMediaItem?.mediaMetadata?.extras?.getLong("durationMs", 0)?.coerceAtLeast(0) ?: 0

    private fun errorMessage(error: PlaybackException): String = getApplication<Application>().getString(when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> R.string.playback_permission_error
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> R.string.playback_file_missing
        else -> R.string.playback_file_error
    })

    private fun syncDuration() {
        val ctrl = controller ?: return
        _state.update { it.copy(durationMs = duration(ctrl)) }
    }

    private fun loadLyrics(song: LocalSong) {
        lastLyricsSongId = song.id
        lyricsJob?.cancel()
        translationJob?.cancel()
        _state.update { it.copy(lyrics = LyricsState.Loading()) }
        lyricsJob = viewModelScope.launch {
            val result = runCatching {
                LyricsRepository.lyrics(
                    title = song.title,
                    artist = song.artist,
                    durationMs = song.durationMs,
                    album = song.album,
                    sources = SettingsRepository.lyricsSources.value,
                    order = SettingsRepository.lyricsSourceOrder.value,
                    prioritizeSyllableSync = SettingsRepository.prioritizeSyllableSync.value,
                )
            }.getOrNull()
            if (lastLyricsSongId != song.id) return@launch
            _state.update {
                when {
                    result == null || result.lines.isEmpty() -> it.copy(lyrics = LyricsState.Missing)
                    else -> it.copy(lyrics = LyricsState.Loaded(result.lines, result.source))
                }
            }
        }
    }

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                val ctrl = controller ?: break
                val currentPos = ctrl.currentPosition.coerceAtLeast(0L)
                val delta = currentPos - lastTickPositionMs
                if (delta in 1..5000 && _state.value.currentSong != null && ctrl.isPlaying) {
                    ListeningStats.record(_state.value.currentSong!!, delta, false)
                }
                lastTickPositionMs = currentPos
                _state.update {
                    it.copy(
                        positionMs = currentPos,
                        durationMs = duration(ctrl),
                    )
                }
                delay(1000L)
            }
        }
    }

    private fun stopPositionPolling() {
        positionJob?.cancel()
        positionJob = null
    }

    override fun onCleared() {
        connectionJob?.cancel()
        playRequestJob?.cancel()
        positionJob?.cancel()
        lyricsJob?.cancel()
        translationJob?.cancel()
        ListeningStats.flush()
        controller?.let { ctrl ->
            ctrl.removeListener(playerListener)
            ctrl.release()
        }
        controller = null
        super.onCleared()
    }
}
