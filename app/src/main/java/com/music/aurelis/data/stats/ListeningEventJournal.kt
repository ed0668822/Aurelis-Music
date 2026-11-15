// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.stats

import android.content.Context
import android.util.AtomicFile
import com.music.aurelis.data.model.ProfileId
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

internal interface ListeningJournal {
    val storageKey: String
    fun read(): List<ListeningEvent>
    fun save(events: List<ListeningEvent>)
}
internal class NewerListeningJournal : IOException("Newer listening journal")
internal class CorruptListeningJournal : IOException("Invalid listening journal")

/** IO worker only. Unknown/corrupt snapshots must be preserved, never replaced with empty. */
internal class ListeningEventJournal(context: Context, profileId: ProfileId) : ListeningJournal {
    private val app = context.applicationContext
    internal val file: File by lazy { File(app.noBackupFilesDir, "listening-events/${profileId.value}.json") }
    override val storageKey get() = file.absolutePath
    companion object { const val MAX_EVENTS = 512; private const val MAX_BYTES = 8 * 1024 * 1024 }
    override fun read(): List<ListeningEvent> {
        if (!file.exists() && !File(file.path + ".bak").exists()) return emptyList()
        val bytes = AtomicFile(file).openRead().use { it.readNBytes(MAX_BYTES + 1) }
        if (bytes.size > MAX_BYTES) throw CorruptListeningJournal()
        try {
            val decoded = Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            val root = JSONObject(decoded)
            val version = integer(root.get("version"))
            if (version > 1) throw NewerListeningJournal()
            require(version == 1L && keys(root) == setOf("version", "events"))
            val rows = root.getJSONArray("events"); require(rows.length() <= MAX_EVENTS)
            return (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                require(keys(row) == setOf("session", "sequence", "song", "entry", "at", "ms", "duration"))
                fun text(key: String): String { val value = row.get(key); require(value is String); return value }
                ListeningEvent(text("session"), integer(row.get("sequence")), text("song"), text("entry"),
                    integer(row.get("at")), integer(row.get("ms")), integer(row.get("duration"))).also { require(it.valid()) }
            }
        } catch (future: NewerListeningJournal) { throw future }
          catch (_: Exception) { throw CorruptListeningJournal() }
    }
    override fun save(events: List<ListeningEvent>) {
        require(events.size <= MAX_EVENTS && events.all { it.valid() })
        val rows = JSONArray()
        events.forEach { rows.put(JSONObject().put("session", it.sessionId).put("sequence", it.sequence)
            .put("song", it.songId).put("entry", it.queueEntryId).put("at", it.observedAtMs)
            .put("ms", it.listenedMs).put("duration", it.durationMs)) }
        val encoded = Charsets.UTF_8.newEncoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
            .encode(java.nio.CharBuffer.wrap(JSONObject().put("version", 1).put("events", rows).toString()))
        val bytes = ByteArray(encoded.remaining()).also { encoded.get(it) }
        if (bytes.size > MAX_BYTES) throw IOException("Listening journal capacity exceeded")
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        var finished = false
        try {
            stream.write(bytes); stream.fd.sync(); atomic.finishWrite(stream); finished = true
            val committed = atomic.openRead().use { it.readNBytes(MAX_BYTES + 1) }
            if (!committed.contentEquals(bytes)) throw IOException("Listening journal commit not verified")
        } catch (error: IOException) { if (!finished) atomic.failWrite(stream); throw error }
    }
    private fun integer(value: Any): Long { require(value is Int || value is Long); return (value as Number).toLong() }
    private fun keys(value: JSONObject): Set<String> = value.keys().asSequence().toSet()
}
