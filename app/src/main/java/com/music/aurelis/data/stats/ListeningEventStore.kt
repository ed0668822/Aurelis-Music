// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.stats

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.data.model.ProfileId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Serialized native SQLite IO. Legacy totals and playback policy are deliberately separate. */
internal class ListeningEventStore(
    context: Context,
    profileId: ProfileId = ProfileRepository.profileId,
    private val writeAllowed: () -> Boolean = { !ProfileRepository.state.value.readOnly && ProfileRepository.profileId == profileId },
) : ListeningEventSink {
    internal val databaseFile = context.applicationContext.getDatabasePath(ListeningDatabase.fileName(profileId))
    private val helper = ListeningDatabase(context.applicationContext, profileId)
    private val mutex = Mutex()
    private var closed = false
    private var blocked: ListeningStoreIssue? = null
    private class Conflict : RuntimeException()

    private suspend fun <T> access(write: Boolean = false, action: () -> T): ListeningStoreResult<T> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (closed) return@withLock ListeningStoreResult.Failure(ListeningStoreIssue.CLOSED)
                blocked?.let { return@withLock ListeningStoreResult.Failure(it) }
                if (write && !writeAllowed()) return@withLock ListeningStoreResult.Failure(ListeningStoreIssue.PROFILE_READ_ONLY)
                try { ListeningStoreResult.Success(action()) }
                catch (_: NewerListeningSchema) {
                    blocked = ListeningStoreIssue.NEWER_SCHEMA
                    ListeningStoreResult.Failure(ListeningStoreIssue.NEWER_SCHEMA)
                } catch (_: SQLiteDatabaseCorruptException) {
                    blocked = ListeningStoreIssue.CORRUPT
                    ListeningStoreResult.Failure(ListeningStoreIssue.CORRUPT)
                } catch (_: Conflict) { ListeningStoreResult.Failure(ListeningStoreIssue.CONFLICT) }
                  catch (_: SQLiteException) { ListeningStoreResult.Failure(ListeningStoreIssue.IO_ERROR) }
            }
        }

    override suspend fun append(events: List<ListeningEvent>): ListeningStoreResult<ListeningAppendResult> {
        val batch = events.toList()
        if (batch.any { !it.valid() }) return ListeningStoreResult.Failure(ListeningStoreIssue.INVALID_EVENT)
        return access(write = true) {
            if (batch.isEmpty()) return@access ListeningAppendResult(0, 0)
            val db = helper.writableDatabase
            var inserted = 0
            var duplicates = 0
            db.beginTransaction()
            try {
                for (event in batch) {
                    db.query("listening_sessions", arrayOf("song_id", "queue_entry_id"), "session_id=?",
                        arrayOf(event.sessionId), null, null, null).use { session ->
                        if (session.moveToFirst()) {
                            if (session.getString(0) != event.songId || session.getString(1) != event.queueEntryId) throw Conflict()
                        } else {
                            db.insertOrThrow("listening_sessions", null, ContentValues().apply {
                                put("session_id", event.sessionId); put("song_id", event.songId); put("queue_entry_id", event.queueEntryId)
                            })
                        }
                    }
                    val same = db.query("listening_events", arrayOf("observed_at_ms", "listened_ms", "duration_ms"),
                        "session_id=? AND sequence=?", arrayOf(event.sessionId, event.sequence.toString()), null, null, null).use { prior ->
                        if (!prior.moveToFirst()) false else {
                            if (prior.getLong(0) != event.observedAtMs || prior.getLong(1) != event.listenedMs || prior.getLong(2) != event.durationMs) throw Conflict()
                            true
                        }
                    }
                    if (same) duplicates++ else {
                        db.insertOrThrow("listening_events", null, ContentValues().apply {
                            put("session_id", event.sessionId); put("sequence", event.sequence)
                            put("observed_at_ms", event.observedAtMs); put("listened_ms", event.listenedMs); put("duration_ms", event.durationMs)
                        })
                        inserted++
                    }
                }
                db.setTransactionSuccessful()
                ListeningAppendResult(inserted, duplicates)
            } finally { db.endTransaction() }
        }
    }

    suspend fun events(fromInclusiveMs: Long = 0, untilExclusiveMs: Long? = null, limit: Int = 1000,
        after: ListeningEventCursor? = null): ListeningStoreResult<List<ListeningEvent>> {
        if (!validRange(fromInclusiveMs, untilExclusiveMs, limit) || (after != null &&
            (after.observedAtMs < 0 || after.sequence < 0 || !validKey(after.sessionId, 128))))
            return ListeningStoreResult.Failure(ListeningStoreIssue.INVALID_QUERY)
        return access {
            if (!databaseFile.exists()) return@access emptyList()
            val clauses = mutableListOf("e.observed_at_ms>=?")
            val args = mutableListOf(fromInclusiveMs.toString())
            if (untilExclusiveMs != null) { clauses += "e.observed_at_ms<?"; args += untilExclusiveMs.toString() }
            if (after != null) {
                clauses += "(e.observed_at_ms>? OR (e.observed_at_ms=? AND (e.session_id>? OR (e.session_id=? AND e.sequence>?))))"
                args += listOf(after.observedAtMs.toString(), after.observedAtMs.toString(), after.sessionId, after.sessionId, after.sequence.toString())
            }
            args += limit.toString()
            helper.writableDatabase.rawQuery("""SELECT e.session_id,e.sequence,s.song_id,s.queue_entry_id,
                e.observed_at_ms,e.listened_ms,e.duration_ms FROM listening_events e
                JOIN listening_sessions s ON s.session_id=e.session_id
                WHERE ${clauses.joinToString(" AND ")} ORDER BY e.observed_at_ms,e.session_id,e.sequence LIMIT ?""", args.toTypedArray()).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.event()) }
            }
        }
    }

    suspend fun totalsBySong(fromInclusiveMs: Long = 0, untilExclusiveMs: Long? = null, limit: Int = 1000): ListeningStoreResult<List<ListeningSongTotal>> {
        if (!validRange(fromInclusiveMs, untilExclusiveMs, limit)) return ListeningStoreResult.Failure(ListeningStoreIssue.INVALID_QUERY)
        return access {
            if (!databaseFile.exists()) return@access emptyList()
            val upper = if (untilExclusiveMs != null) " AND e.observed_at_ms<?" else ""
            val args = mutableListOf(fromInclusiveMs.toString())
            if (untilExclusiveMs != null) args += untilExclusiveMs.toString()
            args += limit.toString()
            helper.writableDatabase.rawQuery("""SELECT s.song_id,SUM(e.listened_ms),COUNT(*) FROM listening_events e
                JOIN listening_sessions s ON s.session_id=e.session_id WHERE e.observed_at_ms>=?$upper
                GROUP BY s.song_id ORDER BY SUM(e.listened_ms) DESC,s.song_id LIMIT ?""", args.toTypedArray()).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        if (cursor.isNull(0) || cursor.getType(1) != Cursor.FIELD_TYPE_INTEGER || cursor.getType(2) != Cursor.FIELD_TYPE_INTEGER)
                            throw SQLiteDatabaseCorruptException("Invalid listening total")
                        val total = ListeningSongTotal(cursor.getString(0), cursor.getLong(1), cursor.getLong(2))
                        if (!validKey(total.songId, 4096) || total.listenedMs < 0 || total.segments <= 0)
                            throw SQLiteDatabaseCorruptException("Invalid listening total")
                        add(total)
                    }
                }
            }
        }
    }

    override suspend fun close(): Unit = withContext(NonCancellable + Dispatchers.IO) {
        mutex.withLock { if (!closed) { closed = true; helper.close() } }
    }
    private fun validRange(from: Long, until: Long?, limit: Int) = from >= 0 && (until == null || until >= from) && limit in 1..1000
    private fun Cursor.event(): ListeningEvent {
        if ((0..6).any { isNull(it) } || listOf(1, 4, 5, 6).any { getType(it) != Cursor.FIELD_TYPE_INTEGER })
            throw SQLiteDatabaseCorruptException("Invalid listening record")
        return ListeningEvent(getString(0), getLong(1), getString(2), getString(3), getLong(4), getLong(5), getLong(6))
            .also { if (!it.valid()) throw SQLiteDatabaseCorruptException("Invalid listening record") }
    }
}
