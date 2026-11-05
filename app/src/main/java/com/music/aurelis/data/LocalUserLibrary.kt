package com.music.aurelis.data

import android.content.Context
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.ProfileId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LocalPlaylist(val id: String, val name: String, val songIds: List<String>)
data class LocalListening(val listenedMs: Long = 0, val playCount: Int = 0, val lastPlayedAt: Long = 0)
data class LocalUserLibraryState(
    val favorites: Set<String> = emptySet(),
    val playlists: List<LocalPlaylist> = emptyList(),
    val listening: Map<String, LocalListening> = emptyMap(),
    val readOnly: Boolean = false,
)

/** Small, profile-private collections. Missing media IDs remain saved for removable storage. */
class LocalUserLibrary(
    context: Context,
    profileId: ProfileId = ProfileId.DEFAULT,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val storage = ProfileStorage(context)
    private val favorites = storage.open(profileId, ProfileDataArea.FAVORITES)
    private val playlists = storage.open(profileId, ProfileDataArea.PLAYLISTS)
    private val history = storage.open(profileId, ProfileDataArea.HISTORY)
    private val mutableState = MutableStateFlow(read())
    val state = mutableState.asStateFlow()
    private val pendingListening = mutableState.value.listening.toMutableMap()
    private var sessionId: String? = null
    private var sessionMs = 0L
    private var counted = false
    private var dirty = false
    private var lastFlush = 0L

    private fun read(): LocalUserLibraryState = runCatching {
        val savedPlaylists = playlists.getString("playlists_v1", null)?.let(::JSONObject)
        val savedHistory = history.getString("listening_v1", null)?.let(::JSONObject)
        require(savedPlaylists == null || savedPlaylists.getInt("version") == 1)
        require(savedHistory == null || savedHistory.getInt("version") == 1)
        val list = savedPlaylists?.getJSONArray("items") ?: JSONArray()
        val parsedLists = (0 until list.length()).map { index ->
            val item = list.getJSONObject(index)
            val ids = item.getJSONArray("songs")
            LocalPlaylist(item.getString("id"), item.getString("name"),
                (0 until ids.length()).map { ids.getString(it) }.distinct())
        }
        require(parsedLists.all { it.id.isNotBlank() && it.name.isNotBlank() })
        require(parsedLists.map { it.id }.distinct().size == parsedLists.size)
        val tracks = savedHistory?.getJSONObject("tracks") ?: JSONObject()
        val stats = tracks.keys().asSequence().associateWith { id ->
            val item = tracks.getJSONObject(id)
            LocalListening(item.getLong("ms"), item.getInt("plays"), item.getLong("last")).also {
                require(it.listenedMs >= 0 && it.playCount >= 0 && it.lastPlayedAt >= 0)
            }
        }
        LocalUserLibraryState(favorites.getStringSet("song_ids", emptySet()).orEmpty().toSet(), parsedLists, stats)
    }.getOrElse {
        // Unknown/corrupt versions are preserved on disk, never overwritten with empty data.
        LocalUserLibraryState(readOnly = true)
    }

    private fun writable() { check(!state.value.readOnly) { "Local collections could not be read" } }

    @Synchronized fun toggleFavorite(songId: String) {
        writable(); require(songId.isNotBlank())
        val ids = state.value.favorites.toMutableSet()
        if (!ids.add(songId)) ids.remove(songId)
        favorites.edit().putStringSet("song_ids", ids.toSet()).apply()
        mutableState.value = state.value.copy(favorites = ids.toSet())
    }

    @Synchronized fun createPlaylist(name: String, songIds: List<String> = emptyList()): String {
        writable()
        val title = name.trim().also { require(it.isNotEmpty() && it.length <= 100) }
        require(songIds.all { it.isNotBlank() })
        val id = UUID.randomUUID().toString()
        savePlaylists(state.value.playlists + LocalPlaylist(id, title, songIds.distinct()))
        return id
    }

    @Synchronized fun renamePlaylist(id: String, name: String) {
        writable()
        val title = name.trim().also { require(it.isNotEmpty() && it.length <= 100) }
        require(state.value.playlists.any { it.id == id })
        savePlaylists(state.value.playlists.map { if (it.id == id) it.copy(name = title) else it })
    }

    @Synchronized fun deletePlaylist(id: String) {
        writable(); savePlaylists(state.value.playlists.filterNot { it.id == id })
    }

    @Synchronized fun setPlaylistSongs(id: String, songIds: List<String>) {
        writable(); require(songIds.all { it.isNotBlank() })
        require(state.value.playlists.any { it.id == id })
        savePlaylists(state.value.playlists.map { if (it.id == id) it.copy(songIds = songIds.distinct()) else it })
    }

    @Synchronized fun movePlaylistSong(id: String, from: Int, to: Int) {
        writable()
        val playlist = state.value.playlists.firstOrNull { it.id == id } ?: return
        if (from !in playlist.songIds.indices || to !in playlist.songIds.indices) return
        val ids = playlist.songIds.toMutableList(); ids.add(to, ids.removeAt(from))
        setPlaylistSongs(id, ids)
    }

    @Synchronized fun clearHistory() {
        writable()
        pendingListening.clear(); dirty = true; flush()
        // Keep the current session counted so clearing never fabricates another play.
    }

    private fun savePlaylists(items: List<LocalPlaylist>) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("songs", JSONArray(it.songIds))) }
        playlists.edit().putString("playlists_v1", JSONObject().put("version", 1).put("items", array).toString()).apply()
        mutableState.value = state.value.copy(playlists = items)
    }

    @Synchronized fun startListeningSession(songId: String?) {
        flush()
        sessionId = songId; sessionMs = 0; counted = false
    }

    @Synchronized fun recordListening(song: LocalSong, deltaMs: Long, completed: Boolean) {
        if (state.value.readOnly || song.id.isBlank() || deltaMs !in 1..5000) return
        if (sessionId != song.id) startListeningSession(song.id)
        sessionMs = (sessionMs.coerceAtMost(Long.MAX_VALUE - deltaMs) + deltaMs)
        val previous = pendingListening[song.id] ?: LocalListening()
        val threshold = if (song.durationMs > 0) minOf(30_000L, (song.durationMs / 2).coerceAtLeast(1L)) else 30_000L
        val qualifies = !counted && (completed || sessionMs >= threshold)
        counted = counted || qualifies
        val now = clock().coerceAtLeast(0)
        pendingListening[song.id] = LocalListening(
            previous.listenedMs.coerceAtMost(Long.MAX_VALUE - deltaMs) + deltaMs,
            if (qualifies && previous.playCount < Int.MAX_VALUE) previous.playCount + 1 else previous.playCount,
            now,
        )
        dirty = true
        if (previous.lastPlayedAt == 0L || qualifies || now - lastFlush >= 30_000 || now < lastFlush) flush()
    }

    @Synchronized fun flush() {
        if (!dirty || state.value.readOnly) return
        val tracks = JSONObject()
        pendingListening.forEach { (id, value) -> tracks.put(id, JSONObject()
            .put("ms", value.listenedMs).put("plays", value.playCount).put("last", value.lastPlayedAt)) }
        history.edit().putString("listening_v1", JSONObject().put("version", 1).put("tracks", tracks).toString()).apply()
        mutableState.value = state.value.copy(listening = pendingListening.toMap())
        lastFlush = clock(); dirty = false
    }
}
