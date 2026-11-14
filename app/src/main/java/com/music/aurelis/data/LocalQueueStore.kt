// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import android.content.Context
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.ProfileId
import com.music.aurelis.data.model.QueueTier
import org.json.JSONArray
import org.json.JSONObject

/** IDs/preferences only: no file paths, metadata payloads or automatic playback intent. */
data class SavedLocalQueue(
    val ids: List<String>, val index: Int, val positionMs: Long, val repeat: Int, val shuffle: Boolean,
    val entryIds: List<String> = emptyList(),
    val tiers: List<QueueTier> = emptyList(),
    val shuffleOrder: List<Int> = emptyList(),
) {
    internal fun validate() {
        require(ids.all { it.isNotBlank() } && repeat in 0..2 && positionMs >= 0)
        require(if (ids.isEmpty()) index == 0 else index in ids.indices)
        require(entryIds.isEmpty() || (entryIds.size == ids.size && entryIds.all { it.isNotBlank() } && entryIds.toSet().size == ids.size))
        require(tiers.isEmpty() || tiers.size == ids.size)
        require(shuffleOrder.isEmpty() || (shuffleOrder.size == ids.size && shuffleOrder.toSet() == ids.indices.toSet()))
    }
    fun resolve(songs: List<LocalSong>): Pair<List<LocalSong>, Int> {
        val byId = songs.associateBy { it.id }
        val available = ids.mapIndexedNotNull { i, id -> byId[id]?.let { song ->
            i to song.copy(queueEntryId = entryIds.getOrNull(i), queueTier = tiers.getOrNull(i) ?: QueueTier.CONTEXT)
        } }
        val target = available.indexOfFirst { it.first >= index }.takeIf { it >= 0 } ?: available.lastIndex.coerceAtLeast(0)
        return available.map { it.second } to target
    }
    fun resolveShuffleOrder(songs: List<LocalSong>): List<Int>? {
        if (shuffleOrder.isEmpty()) return null
        val available = songs.map { it.id }.toSet()
        val mapping = ids.indices.filter { ids[it] in available }.withIndex().associate { it.value to it.index }
        return shuffleOrder.mapNotNull { mapping[it] }
    }
}
class LocalQueueStore(context: Context, profileId: ProfileId = ProfileRepository.profileId) {
    private val prefs = ProfileStorage(context).open(profileId, ProfileDataArea.QUEUE)
    var readOnly = false; private set
    fun read(): SavedLocalQueue? = try {
        (prefs.getString("queue_v2", null) ?: prefs.getString("queue_v1", null))?.let { raw ->
            val obj = JSONObject(raw)
            fun integer(value: Any): Long {
                require(value is Int || value is Long)
                return (value as Number).toLong()
            }
            fun int(key: String): Int {
                val value = integer(obj.get(key)); require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
                return value.toInt()
            }
            val version = int("version"); require(version in 1..2)
            require(obj.get("shuffle") is Boolean)
            fun strings(key: String): List<String> {
                val rows = obj.getJSONArray(key)
                return (0 until rows.length()).map { val value = rows.get(it); require(value is String); value }
            }
            val order = if (version == 2) obj.getJSONArray("order") else JSONArray()
            SavedLocalQueue(strings("ids"), int("index"), integer(obj.get("position")),
                int("repeat"), obj.getBoolean("shuffle"),
                if (version == 2) strings("entries") else emptyList(),
                if (version == 2) strings("tiers").map { QueueTier.valueOf(it) } else emptyList(),
                (0 until order.length()).map { integer(order.get(it)).also { value -> require(value in 0L..Int.MAX_VALUE.toLong()) }.toInt() }).also { it.validate() }
        }
    } catch (_: Exception) { readOnly = true; null }
    init { read() }
    fun save(queue: SavedLocalQueue) {
        if (readOnly || ProfileRepository.state.value.readOnly) return
        queue.validate()
        prefs.edit().putString("queue_v2", JSONObject().put("version", 2).put("ids", JSONArray(queue.ids))
            .put("index", queue.index).put("position", queue.positionMs).put("repeat", queue.repeat)
            .put("shuffle", queue.shuffle).put("entries", JSONArray(queue.entryIds))
            .put("tiers", JSONArray(queue.tiers.map { it.name })).put("order", JSONArray(queue.shuffleOrder)).toString()).apply()
        // Keep the previous v1 value intact as a migration source; v2 takes precedence.
    }
}
