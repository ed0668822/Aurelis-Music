// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.stats

/** One measured interval, not seek distance or a reconstructed historical play. */
internal data class ListeningEvent(
    val sessionId: String,
    val sequence: Long,
    val songId: String,
    val queueEntryId: String,
    val observedAtMs: Long,
    val listenedMs: Long,
    val durationMs: Long = 0L,
) {
    internal fun valid(): Boolean = validKey(sessionId, 128) && validKey(songId, 4096) &&
        validKey(queueEntryId, 4096) && sequence >= 0 && observedAtMs >= 0 &&
        listenedMs in 1L..5000L && durationMs >= 0
}
internal fun validKey(value: String, maxLength: Int): Boolean =
    value.isNotBlank() && value.length <= maxLength && '\u0000' !in value

internal data class ListeningEventCursor(val observedAtMs: Long, val sessionId: String, val sequence: Long)
internal data class ListeningAppendResult(val inserted: Int, val duplicates: Int)
internal data class ListeningSongTotal(val songId: String, val listenedMs: Long, val segments: Long)
internal enum class ListeningStoreIssue { CLOSED, PROFILE_READ_ONLY, NEWER_SCHEMA, CORRUPT, IO_ERROR, INVALID_EVENT, INVALID_QUERY, CONFLICT }
internal sealed interface ListeningStoreResult<out T> {
    data class Success<T>(val value: T) : ListeningStoreResult<T>
    data class Failure(val issue: ListeningStoreIssue) : ListeningStoreResult<Nothing>
}


internal interface ListeningEventSink {
    suspend fun append(events: List<ListeningEvent>): ListeningStoreResult<ListeningAppendResult>
    suspend fun close()
}
