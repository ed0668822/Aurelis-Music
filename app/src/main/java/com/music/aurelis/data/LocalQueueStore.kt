package com.music.aurelis.data

import android.content.Context
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.ProfileId
import org.json.JSONArray
import org.json.JSONObject

/** Only IDs and playback preferences are persisted, never file paths or auto-play intent. */
data class SavedLocalQueue(val ids: List<String>, val index: Int, val positionMs: Long, val repeat: Int, val shuffle: Boolean) {
    fun resolve(songs: List<LocalSong>): Pair<List<LocalSong>, Int> {
        val byId = songs.associateBy { it.id }
        val available = ids.mapIndexedNotNull { i, id -> byId[id]?.let { i to it } }
        val target = available.indexOfFirst { it.first >= index }.takeIf { it >= 0 } ?: available.lastIndex.coerceAtLeast(0)
        return available.map { it.second } to target
    }
}
class LocalQueueStore(context: Context, profileId: ProfileId = ProfileId.DEFAULT) {
    private val prefs = ProfileStorage(context).open(profileId, ProfileDataArea.QUEUE)
    var readOnly = false; private set
    fun read(): SavedLocalQueue? = try {
        prefs.getString("queue_v1", null)?.let { raw ->
            val obj = JSONObject(raw); require(obj.getInt("version") == 1)
            val rows = obj.getJSONArray("ids")
            val ids = (0 until rows.length()).map { rows.getString(it).also { id -> require(id.isNotBlank()) } }
            val index = obj.getInt("index"); val position = obj.getLong("position")
            val repeat = obj.getInt("repeat"); require(repeat in 0..2 && position >= 0 && (ids.isEmpty() || index in ids.indices))
            SavedLocalQueue(ids, index, position, repeat, obj.getBoolean("shuffle"))
        }
    } catch (_: Exception) { readOnly = true; null }
    init { read() }
    fun save(queue: SavedLocalQueue) {
        if (readOnly) return
        require(queue.repeat in 0..2 && queue.positionMs >= 0 && (queue.ids.isEmpty() || queue.index in queue.ids.indices))
        prefs.edit().putString("queue_v1", JSONObject().put("version", 1).put("ids", JSONArray(queue.ids))
            .put("index", queue.index).put("position", queue.positionMs).put("repeat", queue.repeat)
            .put("shuffle", queue.shuffle).toString()).apply()
    }
}
