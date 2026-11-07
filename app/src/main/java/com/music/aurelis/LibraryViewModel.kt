// Aurelis © 2026. GPL-3.0-or-later

package com.music.aurelis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music.aurelis.data.AudioPermission
import com.music.aurelis.data.LocalSearch
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.LocalLibrarySource
import com.music.aurelis.data.MediaStoreLibrarySource
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.SortOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class LibraryUiState(
    val libraryState: LibraryState = LibraryState.Loading,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.TITLE_ASC,
    val filteredSongs: List<LocalSong> = emptyList(),
    val isRefreshing: Boolean = false,
)

class LibraryViewModel @JvmOverloads constructor(
    application: Application,
    private val librarySource: LocalLibrarySource = MediaStoreLibrarySource(application),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var allSongs: List<LocalSong> = emptyList()

    @Volatile
    private var scanned: Boolean = false

    private var scanJob: Job? = null
    private var observerJob: Job? = null
    private var pendingRefresh = false
    private var scanVersion = 0L

    private fun permissionError() = LibraryState.Error(
        "Permiso de acceso a música no concedido. " +
            "Concédelo en Ajustes para escanear el dispositivo.", permissionRequired = true,
    )

    private fun clearCatalogue(error: LibraryState.Error) {
        allSongs = emptyList()
        _uiState.update { it.copy(libraryState = error, filteredSongs = emptyList(), isRefreshing = false) }
    }

    /** Observe only while a UI owner is foreground; the first event follows registration. */
    @OptIn(FlowPreview::class)
    fun onForeground() {
        if (!AudioPermission.isGranted(getApplication<Application>())) {
            onBackground()
            scanned = false
            clearCatalogue(permissionError())
            return
        }
        if (observerJob?.isActive == true) { refresh(); return }
        observerJob = viewModelScope.launch {
            var first = true
            merge(librarySource.changes, SettingsRepository.filterNonMusicAudio.drop(1).map { Unit })
                .debounce { if (first) { first = false; 0L } else 350L }
                .catch { refresh() }
                .collect { refresh() }
        }
    }

    fun onBackground() {
        observerJob?.cancel(); observerJob = null
        scanVersion++
        scanJob?.cancel(); scanJob = null
        pendingRefresh = false
        if (_uiState.value.libraryState !is LibraryState.Success) scanned = false
        _uiState.update { it.copy(isRefreshing = false) }
    }

    fun loadLibrary(force: Boolean = false) {
        if (scanJob?.isActive == true) { if (force) pendingRefresh = true; return }
        if (scanned && !force) return
        scanned = true
        val version = ++scanVersion
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            _uiState.update { it.copy(
                libraryState = if (it.libraryState is LibraryState.Success) it.libraryState else LibraryState.Loading,
                isRefreshing = true,
            ) }
            try {
                val context = getApplication<Application>()
                if (!AudioPermission.isGranted(context)) throw SecurityException()
                val rows = librarySource.songs(SettingsRepository.filterNonMusicAudio.value)
                currentCoroutineContext().ensureActive()
                if (version != scanVersion) return@launch
                if (!AudioPermission.isGranted(context)) throw SecurityException()
                val previous = allSongs.associateBy { it.id }
                val result = rows.distinctBy { it.id }.map { row -> previous[row.id]?.takeIf { it == row } ?: row }
                val old = _uiState.value.libraryState as? LibraryState.Success
                val snapshot = if (result == allSongs && old != null) old else {
                    val (albums, artists, recent) = deriveGroupings(result)
                    LibraryState.Success(result, albums, artists, recent)
                }
                allSongs = snapshot.songs
                _uiState.update { it.copy(libraryState = snapshot) }
                applyFilterAndSort()
            } catch (cancelled: CancellationException) {
                if (version == scanVersion) scanned = false
                throw cancelled
            } catch (error: SecurityException) {
                if (version == scanVersion) { scanned = false; clearCatalogue(permissionError()) }
            } catch (error: Exception) {
                if (version == scanVersion) {
                    scanned = false
                    clearCatalogue(LibraryState.Error(error.message ?: "Error desconocido al escanear la biblioteca."))
                }
            } finally {
                if (version == scanVersion) {
                    scanJob = null
                    _uiState.update { it.copy(isRefreshing = false) }
                    val again = pendingRefresh
                    pendingRefresh = false
                    if (again && viewModelScope.isActive) loadLibrary(force = true)
                }
            }
        }
        scanJob = job
        job.start()
    }

    fun refresh() = loadLibrary(force = true)

    override fun onCleared() { onBackground(); super.onCleared() }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilterAndSort()
    }

    fun onSortOptionChanged(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applyFilterAndSort()
    }

    private fun deriveGroupings(
        songs: List<LocalSong>,
    ): Triple<List<LocalAlbum>, List<LocalArtist>, List<LocalSong>> {
        val albums = songs
            .filter { !it.album.isNullOrBlank() }
            .groupBy { it.albumId?.let { id -> "media:$id" } ?: "${it.album.orEmpty().length}:${it.album}${it.artist}" }
            .map { (_, list) ->
                LocalAlbum(
                    name = list.first().album.orEmpty(),
                    artist = list.map { it.artist }.distinct().singleOrNull()
                        ?: getApplication<Application>().getString(R.string.library_various_artists),
                    mediaId = list.first().albumId,
                    artworkUri = list.firstNotNullOfOrNull { it.artworkUri },
                    songCount = list.size,
                    songs = list.sortedWith(compareBy<LocalSong> { it.discNumber ?: 0 }.thenBy { it.trackNumber ?: Int.MAX_VALUE }),
                )
            }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }

        val artists = songs
            .groupBy { it.artistId?.let { id -> "media:$id" } ?: it.artist.lowercase(Locale.ROOT) }
            .map { (_, list) ->
                LocalArtist(
                    name = list.first().artist,
                    mediaId = list.first().artistId,
                    artworkUri = list.firstNotNullOfOrNull { it.artworkUri },
                    songCount = list.size,
                    albumCount = list.filter { !it.album.isNullOrBlank() }.map {
                        it.albumId?.let { id -> "media:$id" } ?: "${it.album.orEmpty().length}:${it.album}${it.artist}"
                    }.distinct().size,
                    songs = list,
                )
            }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }

        val recent = songs
            .filter { it.dateAddedMs > 0L }
            .sortedByDescending { it.dateAddedMs }
            .take(20)

        return Triple(albums, artists, recent)
    }

    private fun applyFilterAndSort() {
        val state = _uiState.value
        val query = LocalSearch.normalize(state.searchQuery)
        val sort = state.sortOption

        var result = allSongs

        if (query.isNotBlank()) {
            result = result.filter { song -> LocalSearch.matchesNormalized(song, query) }
        }

        result = LocalSearch.sort(result, sort)

        _uiState.update { it.copy(filteredSongs = result) }
    }
}
