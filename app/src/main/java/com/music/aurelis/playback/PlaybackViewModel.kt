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
import com.music.aurelis.data.lyrics.LocalLyricsRepository
import android.net.Uri
import com.music.aurelis.data.lyrics.LyricsSource
import com.music.aurelis.data.lyrics.LyricsFailure
import com.music.aurelis.data.lyrics.LyricsFailureKind
import com.music.aurelis.data.lyrics.LyricsProviderRegistry
import com.music.aurelis.data.lyrics.LyricsRequestException
import com.music.aurelis.data.lyrics.LyricsTranslation
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import com.music.aurelis.data.LocalUserLibraryState
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
        val isSynced: Boolean = lines.any { it.timeMs > 0 || it.isWordSynced },
        val cached: Boolean = false,
    ) : LyricsState
    data object Missing : LyricsState
    data class Failed(val message: String) : LyricsState
}

sealed interface TranslationState {
    data object None : TranslationState
    data object Loading : TranslationState
    data class Done(val lines: List<LyricLine>, val language: String = "", val cached: Boolean = false) : TranslationState
    data class SameLanguage(val language: String) : TranslationState
    data object Failed : TranslationState
    data class Error(val failure: LyricsFailure) : TranslationState
}

sealed interface LyricsLookupState {
    data object Idle : LyricsLookupState
    data object Loading : LyricsLookupState
    data class Complete(val source: LyricsSource, val cached: Boolean) : LyricsLookupState
    data object NotFound : LyricsLookupState
    data class Failed(val failure: LyricsFailure) : LyricsLookupState
}

data class PlaybackState(
    val currentSong: LocalSong? = null,
    val sourceQuality: AudioQualityInfo = AudioQualityInfo(),
    val isPlaying: Boolean = false,
    val isPlayRequested: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0L,
    val positionDiscontinuity: Int = 0,
    val durationMs: Long = 0L,
    val queue: List<LocalSong> = emptyList(),
    val currentIndex: Int = -1,
    val isConnected: Boolean = false,
    val canEditQueue: Boolean = false,
    val lyrics: LyricsState = LyricsState.None,
    val lyricsLookup: LyricsLookupState = LyricsLookupState.Idle,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val canSeek: Boolean = false,
    val canNext: Boolean = false,
    val canPrevious: Boolean = false,
    val errorMessage: String? = null,
    val isCreatingMix: Boolean = false,
    val mixError: String? = null,
)

class PlaybackViewModel internal constructor(
    application: Application,
    private val connector: suspend (() -> Unit) -> Player,
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, { disconnected ->
        PlayerConnection.connect(application, disconnected)
    })

    private var controller: Player? = null
    private val queueCoordinator = QueueCoordinator { controller }
    private var queueEditJob: Job? = null
    private var queueEditGeneration = 0L
    private var connectionJob: Job? = null
    private var playRequestJob: Job? = null
    private var mixJob: Job? = null
    private var mixVersion = 0L
    private var positionJob: Job? = null
    private var lyricsJob: Job? = null
    private var translationJob: Job? = null
    private var translationGeneration = 0L
    internal var translateRequest: suspend (String, List<LyricLine>, String) -> LyricsTranslation.Result = { id, lines, target ->
        LyricsTranslation.translate(id, lines, target)
    }
    private var lastLyricsSongId: String? = null
    private var lyricsGeneration = 0L
    internal var remoteLyricsRequest: suspend (LocalSong) -> LyricsRepository.Result? = { LyricsProviderRegistry.lookup(it) }
    private val localLyrics = LocalLyricsRepository(application) { remoteLyricsRequest(it) }

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncState()
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionPolling()
            else stopPositionPolling()
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isLoading = playbackState == Player.STATE_BUFFERING) }
            if (playbackState == Player.STATE_READY) syncDuration()
        }
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) = syncState(discontinuous = true)
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
                    _state.update { it.copy(isConnected = false, canEditQueue = false, isPlaying = false, isPlayRequested = false,
                        isLoading = false, canSeek = false, canNext = false, canPrevious = false,
                        errorMessage = getApplication<Application>().getString(R.string.playback_connection_error)) }
                }
                controller = player
                player.addListener(playerListener)
                _state.update { it.copy(isConnected = true, errorMessage = null) }
                syncState()
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
        cancelMix()
        invalidateQueueEdits()
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
        if (queueCoordinator.removeItem(index)) syncState()
    }

    fun moveQueueItem(from: Int, to: Int) {
        if (queueCoordinator.moveItem(from, to)) syncState()
    }

    fun clearUserQueue() {
        if (queueCoordinator.clearUserQueue()) syncState()
    }

    fun enqueue(songs: List<LocalSong>) = editQueue(songs, next = false)
    fun playNext(songs: List<LocalSong>) = editQueue(songs, next = true)

    private fun editQueue(songs: List<LocalSong>, next: Boolean) {
        if (songs.isEmpty()) return
        cancelMix()
        val selection = songs.toList()
        val generation = queueEditGeneration
        val previous = queueEditJob
        queueEditJob = viewModelScope.launch {
            previous?.join()
            connect(); connectionJob?.join()
            if (generation != queueEditGeneration) return@launch
            if (next) queueCoordinator.playNext(selection) else queueCoordinator.append(selection)
            syncState()
        }
    }

    private fun invalidateQueueEdits() {
        queueEditGeneration++
        queueEditJob?.cancel()
    }

    private fun cancelMix() {
        mixVersion++; mixJob?.cancel()
        _state.update { it.copy(isCreatingMix = false, mixError = null) }
    }

    fun startAutoMix(songs: List<LocalSong>, user: LocalUserLibraryState) {
        cancelMix(); invalidateQueueEdits(); playRequestJob?.cancel()
        val version = mixVersion
        val seedId = _state.value.currentSong?.id
        _state.update { it.copy(isCreatingMix = true) }
        mixJob = viewModelScope.launch {
            try {
                val mix = withContext(Dispatchers.Default) { LocalAutoMix.build(songs, user, seedId, variation = version) }
                currentCoroutineContext().ensureActive()
                if (mix.isEmpty()) { _state.update { it.copy(mixError = "No hay canciones locales disponibles para crear una mezcla.") }; return@launch }
                connect(); connectionJob?.join()
                val player = controller ?: return@launch
                if (seedId != player.currentMediaItem?.mediaId) {
                    _state.update { it.copy(mixError = "La canción cambió mientras se creaba la mezcla. Vuelve a intentarlo.") }; return@launch
                }
                player.shuffleModeEnabled = false // The recommender has already ordered this queue.
                if (player.currentMediaItem?.mediaId == mix.first().id && player.currentMediaItem?.localConfiguration?.uri.toString() == mix.first().contentUri) {
                    val index = player.currentMediaItemIndex
                    if (index + 1 < player.mediaItemCount) player.removeMediaItems(index + 1, player.mediaItemCount)
                    if (index > 0) player.removeMediaItems(0, index)
                    player.addMediaItems(mix.drop(1).map { it.toQueueMediaItem() })
                    resume()
                } else PlayerConnection.playSongs(player, mix)
                syncState()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (version == mixVersion) _state.update { it.copy(mixError = "No se pudo crear la mezcla local. Vuelve a intentarlo.") } }
            finally { if (version == mixVersion) _state.update { it.copy(isCreatingMix = false) } }
        }
    }

    fun translateLyrics(targetLanguageTag: String) {
        val loaded = _state.value.lyrics as? LyricsState.Loaded ?: return
        val song = _state.value.currentSong ?: return
        val version = ++translationGeneration
        val lyricsVersion = lyricsGeneration
        translationJob?.cancel()
        _state.update { it.copy(lyrics = loaded.copy(translation = TranslationState.Loading)) }
        translationJob = viewModelScope.launch {
            val result = try { translateRequest(song.id, loaded.lines, targetLanguageTag) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { LyricsTranslation.Result.Unavailable }
            if (version != translationGeneration || lyricsVersion != lyricsGeneration || _state.value.currentSong?.id != song.id) return@launch
            _state.update { current ->
                val now = current.lyrics as? LyricsState.Loaded ?: return@update current
                val translated = when (result) {
                    is LyricsTranslation.Result.Translated -> TranslationState.Done(result.lines, targetLanguageTag, result.fromCache)
                    is LyricsTranslation.Result.SameLanguage -> TranslationState.SameLanguage(result.language)
                    LyricsTranslation.Result.Unavailable -> TranslationState.Failed
                    is LyricsTranslation.Result.Failed -> TranslationState.Error(result.failure)
                }
                current.copy(lyrics = now.copy(translation = translated))
            }
        }
    }

    fun clearTranslation() {
        translationGeneration++
        translationJob?.cancel()
        _state.update { current ->
            val now = current.lyrics as? LyricsState.Loaded ?: return@update current
            current.copy(lyrics = now.copy(translation = TranslationState.None))
        }
    }

    private fun syncState(discontinuous: Boolean = false) {
        val ctrl = controller ?: return
        val currentIndex = ctrl.currentMediaItemIndex
        val mediaItem = ctrl.currentMediaItem
        val song = mediaItem?.toLocalSong()
        val queueSongs = (0 until ctrl.mediaItemCount).map { ctrl.getMediaItemAt(it).toLocalSong() }

        _state.update {
            it.copy(
                sourceQuality = AudioQualityInfo.selected(song, if (ctrl.isCommandAvailable(Player.COMMAND_GET_TRACKS))
                    ctrl.currentTracks else androidx.media3.common.Tracks.EMPTY),
                currentSong = song, currentIndex = currentIndex, isPlaying = ctrl.isPlaying,
                canEditQueue = queueCoordinator.canEdit,
                isPlayRequested = ctrl.playWhenReady && ctrl.playbackState != Player.STATE_ENDED && ctrl.playerError == null,
                isLoading = ctrl.playbackState == Player.STATE_BUFFERING,
                positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                positionDiscontinuity = it.positionDiscontinuity + if (discontinuous) 1 else 0,
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
            lyricsGeneration++
            lyricsJob?.cancel()
            translationJob?.cancel()
            _state.update { it.copy(lyrics = LyricsState.None, lyricsLookup = LyricsLookupState.Idle) }
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

    fun searchLyrics() {
        val song = _state.value.currentSong ?: return
        if (LyricsSource.LRCLIB !in SettingsRepository.lyricsSources.value) {
            _state.update { it.copy(lyricsLookup = LyricsLookupState.Failed(LyricsFailure(LyricsFailureKind.DISABLED))) }
            return
        }
        loadLyrics(song, true)
    }

    fun importLyrics(uri: Uri, requestedSongId: String? = _state.value.currentSong?.id) {
        val song = _state.value.currentSong ?: return
        if (song.id != requestedSongId || controller?.currentMediaItem?.mediaId != song.id) {
            _state.update { it.copy(lyrics = LyricsState.Failed("La canción cambió mientras elegías el archivo. Importa las letras para la canción actual.")) }
            return
        }
        val generation = ++lyricsGeneration
        lyricsJob?.cancel(); translationJob?.cancel()
        _state.update { it.copy(lyrics = LyricsState.Loading(LyricsSource.LOCAL_FILE), lyricsLookup = LyricsLookupState.Idle) }
        lyricsJob = viewModelScope.launch {
            try {
                val result = localLyrics.import(song, uri)
                if (generation == lyricsGeneration && _state.value.currentSong?.id == song.id)
                    _state.update { it.copy(lyrics = LyricsState.Loaded(result.lines, result.source, isSynced = result.isSynced)) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (generation == lyricsGeneration) _state.update {
                it.copy(lyrics = LyricsState.Failed("No se pudieron importar las letras. Usa LRC o texto UTF-8/UTF-16 de hasta 1 MiB."))
            } }
        }
    }

    fun cancelLyricsLookup() {
        if (_state.value.lyricsLookup !is LyricsLookupState.Loading) return
        lyricsGeneration++
        lyricsJob?.cancel()
        _state.update { it.copy(lyrics = (it.lyrics as? LyricsState.Loaded) ?: LyricsState.Missing,
            lyricsLookup = LyricsLookupState.Idle) }
    }

    private fun loadLyrics(song: LocalSong, allowRemote: Boolean = false) {
        lastLyricsSongId = song.id
        val generation = ++lyricsGeneration
        lyricsJob?.cancel(); translationJob?.cancel()
        val previous = if (allowRemote) (_state.value.lyrics as? LyricsState.Loaded)?.copy(translation = TranslationState.None) else null
        _state.update { it.copy(lyrics = previous ?: LyricsState.Loading(),
            lyricsLookup = if (allowRemote) LyricsLookupState.Loading else LyricsLookupState.Idle) }
        lyricsJob = viewModelScope.launch {
            try {
                val result = localLyrics.find(song, allowRemote)
                currentCoroutineContext().ensureActive()
                if (generation != lyricsGeneration || _state.value.currentSong?.id != song.id) return@launch
                _state.update { it.copy(lyrics = if (result == null) previous ?: LyricsState.Missing else
                    LyricsState.Loaded(result.lines, result.source, isSynced = result.isSynced, cached = result.cached),
                    lyricsLookup = if (!allowRemote) LyricsLookupState.Idle else if (result == null) LyricsLookupState.NotFound
                        else LyricsLookupState.Complete(result.source, result.cached)) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (generation == lyricsGeneration && _state.value.currentSong?.id == song.id) _state.update {
                    it.copy(lyrics = previous ?: LyricsState.Missing,
                        lyricsLookup = LyricsLookupState.Failed((error as? LyricsRequestException)?.failure
                            ?: LyricsFailure(LyricsFailureKind.INVALID_RESPONSE)))
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
                _state.update {
                    it.copy(
                        positionMs = currentPos,
                        durationMs = duration(ctrl),
                    )
                }
                delay(250L)
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
        mixJob?.cancel()
        positionJob?.cancel()
        lyricsJob?.cancel()
        translationJob?.cancel()
        controller?.let { ctrl ->
            ctrl.removeListener(playerListener)
            ctrl.release()
        }
        controller = null
        super.onCleared()
    }
}
