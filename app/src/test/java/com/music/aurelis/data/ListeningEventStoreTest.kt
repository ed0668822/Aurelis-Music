// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.stats.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ListeningEventStoreTest {
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private fun event(session: String = "session", sequence: Long = 0, at: Long = 1000) =
        ListeningEvent(session, sequence, "song", "occurrence", at, 1000, 60_000)
    private fun <T> value(result: ListeningStoreResult<T>): T {
        assertTrue(result.toString(), result is ListeningStoreResult.Success<*>)
        return (result as ListeningStoreResult.Success<T>).value
    }
    @Test fun legacyTotalsRemainUntouchedAndEmptyReadsDoNotCreateOrInventEvents() = runBlocking {
        val library = LocalUserLibrary(app)
        val song = LocalSong("legacy", "Tema", "Artista", null, "content://media/1", null, 60_000, null, null, null)
        library.startListeningSession(song.id); library.recordListening(song, 5000, false); library.flush()
        val prefs = ProfileStorage(app).open(ProfileRepository.profileId, ProfileDataArea.HISTORY)
        val before = prefs.getString("listening_v1", null)
        val store = ListeningEventStore(app)
        try {
            assertFalse(store.databaseFile.exists())
            assertTrue(value(store.events()).isEmpty()); assertTrue(value(store.totalsBySong()).isEmpty())
            assertFalse(store.databaseFile.exists())
            assertEquals(before, prefs.getString("listening_v1", null))
            assertEquals(5000L, LocalUserLibrary(app).state.value.listening["legacy"]?.listenedMs)
        } finally { store.close() }
    }
    @Test fun appendIsIdempotentAcrossReopenAndPreservesFullWidthIntegerFields() = runBlocking {
        val first = event()
        val edge = event("edge", Long.MAX_VALUE, Long.MAX_VALUE).copy(durationMs = Long.MAX_VALUE)
        val store = ListeningEventStore(app)
        try {
            assertEquals(ListeningAppendResult(2, 1), value(store.append(listOf(first, edge, first))))
        } finally { store.close() }
        val reopened = ListeningEventStore(app)
        try {
            assertEquals(ListeningAppendResult(0, 2), value(reopened.append(listOf(first, edge))))
            assertEquals(listOf(first, edge), value(reopened.events()))
            assertEquals(listOf(first), value(reopened.events(untilExclusiveMs = Long.MAX_VALUE)))
        } finally { reopened.close() }
    }
    @Test fun conflictingRetriesRollBackTheWholeBatchAndNeverOverwritePriorIdentity() = runBlocking {
        val store = ListeningEventStore(app)
        val original = event()
        try {
            value(store.append(listOf(original)))
            assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.CONFLICT), store.append(listOf(
                event("other"), original.copy(listenedMs = 2000))))
            assertEquals(listOf(original), value(store.events()))
            assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.CONFLICT), store.append(listOf(
                original.copy(sequence = 1, songId = "different"))))
            assertEquals(listOf(original), value(store.events()))
        } finally { store.close() }
    }
    @Test fun temporalPagingHasNoGapsAtEqualTimestampsAndTotalsUseHalfOpenWindows() = runBlocking {
        val store = ListeningEventStore(app)
        val a0 = event("a", 0, 100)
        val a1 = event("a", 1, 100).copy(listenedMs = 2000)
        val b0 = event("b", 0, 100).copy(songId = "other", queueEntryId = "duplicate-copy", listenedMs = 3000)
        val c0 = event("c", 0, 200)
        val a2 = event("a", 2, 300)
        try {
            value(store.append(listOf(a2, c0, b0, a1, a0)))
            val pages = mutableListOf<ListeningEvent>()
            var after: ListeningEventCursor? = null
            do {
                val page = value(store.events(limit = 2, after = after))
                pages += page
                after = page.lastOrNull()?.let { ListeningEventCursor(it.observedAtMs, it.sessionId, it.sequence) }
            } while (page.isNotEmpty())
            assertEquals(listOf(a0, a1, b0, c0, a2), pages)
            assertEquals(listOf(a0, a1, b0), value(store.events(100, 200)))
            assertEquals(listOf(ListeningSongTotal("other", 3000, 1), ListeningSongTotal("song", 3000, 2)), value(store.totalsBySong(100, 200)))
            assertTrue(value(store.events(200, 200)).isEmpty())
        } finally { store.close() }
    }
    @Test fun concurrentRetryInsertsExactlyOnceAndRunsDatabaseWorkOffTheMainThread() = runBlocking {
        val caller = Thread.currentThread()
        val ioObserved = java.util.concurrent.atomic.AtomicBoolean(false)
        val store = ListeningEventStore(app, writeAllowed = {
            ioObserved.set(Thread.currentThread() !== caller); true
        })
        try {
            val replies = coroutineScope { List(12) { async(Dispatchers.Default) { value(store.append(listOf(event()))) } }.awaitAll() }
            assertEquals(1, replies.sumOf { it.inserted }); assertEquals(11, replies.sumOf { it.duplicates })
            assertEquals(listOf(event()), value(store.events()))
            assertTrue(ioObserved.get())
        } finally { store.close() }
    }
    @Test fun invalidIntervalsAndQueriesNeverCreateAFileOrTurnSeekDistanceIntoListening() = runBlocking {
        val store = ListeningEventStore(app)
        try {
            for (bad in listOf(event().copy(listenedMs = 50_000), event().copy(observedAtMs = -1),
                event().copy(sequence = -1), event().copy(queueEntryId = ""), event().copy(sessionId = "bad\u0000id"))) {
                assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.INVALID_EVENT), store.append(listOf(bad)))
            }
            assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.INVALID_QUERY), store.events(-1))
            assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.INVALID_QUERY), store.events(200, 100))
            assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.INVALID_QUERY), store.totalsBySong(limit = 1001))
            assertFalse(store.databaseFile.exists())
        } finally { store.close() }
    }
}
