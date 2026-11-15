// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import com.music.aurelis.data.stats.*
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListeningEventWriterTest {
    private fun event(sequence: Long = 0) = ListeningEvent("session", sequence, "song", "copy", 1234, 1000, 60_000)
    private class Journal : ListeningJournal {
        override val storageKey = UUID.randomUUID().toString()
        var rows = emptyList<ListeningEvent>()
        var saves = 0
        var readError: IOException? = null
        var rejectAck = false
        var rejectWrites = false
        override fun read(): List<ListeningEvent> { readError?.let { throw it }; return rows.toList() }
        override fun save(events: List<ListeningEvent>) {
            saves++
            if (rejectWrites) throw IOException("journal unavailable")
            if (rejectAck && events.isEmpty()) throw IOException("ack unavailable")
            rows = events.toList()
        }
    }
    private class Sink : ListeningEventSink {
        val rows = linkedMapOf<Pair<String, Long>, ListeningEvent>()
        var attempts = 0
        var closes = 0
        var offline = false
        var ambiguousCommit = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun append(events: List<ListeningEvent>): ListeningStoreResult<ListeningAppendResult> {
            attempts++; gate?.await()
            if (offline) return ListeningStoreResult.Failure(ListeningStoreIssue.IO_ERROR)
            var inserted = 0
            for (event in events) {
                val key = event.sessionId to event.sequence
                val previous = rows[key]
                if (previous != null && previous != event) return ListeningStoreResult.Failure(ListeningStoreIssue.CONFLICT)
                if (previous == null) { rows[key] = event; inserted++ }
            }
            if (ambiguousCommit) return ListeningStoreResult.Failure(ListeningStoreIssue.IO_ERROR)
            return ListeningStoreResult.Success(ListeningAppendResult(inserted, events.size - inserted))
        }
        override suspend fun close() { closes++ }
    }
    private fun writer(sink: Sink, journal: Journal, dispatcher: TestDispatcher, capacity: Int = 128, limit: Int = 512,
        writable: () -> Boolean = { true }) = ListeningEventWriter(sink, journal, writable, dispatcher,
            clock = { dispatcher.scheduler.currentTime }, queueCapacity = capacity, pendingLimit = limit)

    @Test fun acceptedIntervalsDrainOnCloseWithoutChangingCaptureIdentityOrUtc() {
        val dispatcher = StandardTestDispatcher(); val sink = Sink(); val journal = Journal()
        val writer = writer(sink, journal, dispatcher)
        assertTrue(writer.record(event())); assertTrue(writer.record(event(1)))
        val done = writer.close(); dispatcher.scheduler.runCurrent()
        assertTrue(done.isCompleted); assertEquals(listOf(event(), event(1)), sink.rows.values.toList())
        assertTrue(journal.rows.isEmpty()); assertEquals(1, sink.closes)
        assertFalse(writer.record(event(2))); assertEquals(0L, writer.status.value.missed)
    }
    @Test fun transientIoIsJournaledAndRetriesAfterBackoffWithTheOriginalPayload() {
        val dispatcher = StandardTestDispatcher(); val sink = Sink().apply { offline = true }; val journal = Journal()
        val writer = writer(sink, journal, dispatcher)
        writer.record(event()); dispatcher.scheduler.runCurrent()
        assertEquals(listOf(event()), journal.rows); assertEquals(1, sink.attempts)
        assertEquals(ListeningStoreIssue.IO_ERROR, writer.status.value.issue)
        dispatcher.scheduler.advanceTimeBy(4999); dispatcher.scheduler.runCurrent(); assertEquals(1, sink.attempts)
        sink.offline = false
        dispatcher.scheduler.advanceTimeBy(1); dispatcher.scheduler.runCurrent()
        writer.close(); dispatcher.scheduler.runCurrent()
        assertEquals(listOf(event()), sink.rows.values.toList()); assertTrue(journal.rows.isEmpty())
        assertNull(writer.status.value.issue)
    }
    @Test fun uncertainCommitAndFailedAcknowledgmentReplayTheSameKeysOnlyOnce() {
        val dispatcher = StandardTestDispatcher(); val sink = Sink().apply { ambiguousCommit = true }
        val journal = Journal().apply { rejectAck = true }
        val first = writer(sink, journal, dispatcher)
        first.record(event()); dispatcher.scheduler.runCurrent(); first.close(); dispatcher.scheduler.runCurrent()
        assertEquals(listOf(event()), journal.rows); assertEquals(1, sink.rows.size)
        sink.ambiguousCommit = false
        val second = writer(sink, journal, dispatcher)
        dispatcher.scheduler.runCurrent() // SQLite is committed but the journal acknowledgment still fails.
        assertEquals(listOf(event()), journal.rows); assertEquals(1, sink.rows.size)
        journal.rejectAck = false
        second.close(); dispatcher.scheduler.runCurrent()
        assertTrue(second.completion.isCompleted); assertTrue(journal.rows.isEmpty()); assertEquals(1, sink.rows.size)
    }
    @Test fun futureOrCorruptJournalIsPreservedAndTheUnpersistedIntervalIsReported() {
        for (error in listOf(NewerListeningJournal(), CorruptListeningJournal())) {
            val dispatcher = StandardTestDispatcher(); val sink = Sink(); val journal = Journal().apply { readError = error }
            val writer = writer(sink, journal, dispatcher)
            writer.record(event()); dispatcher.scheduler.runCurrent()
            assertTrue(writer.completion.isCompleted); assertEquals(0, journal.saves); assertTrue(sink.rows.isEmpty())
            assertEquals(if (error is NewerListeningJournal) ListeningStoreIssue.NEWER_SCHEMA else ListeningStoreIssue.CORRUPT,
                writer.status.value.issue)
            assertEquals(1L, writer.status.value.missed); assertEquals(1, sink.closes)
        }
    }
    @Test fun boundedInputReportsOverflowAndStillDrainsAcceptedRows() {
        val dispatcher = StandardTestDispatcher(); val sink = Sink(); val journal = Journal()
        val writer = writer(sink, journal, dispatcher, capacity = 2)
        assertTrue(writer.record(event())); assertTrue(writer.record(event(1))); assertFalse(writer.record(event(2)))
        writer.close(); dispatcher.scheduler.runCurrent()
        assertEquals(2, sink.rows.size); assertEquals(1L, writer.status.value.missed)
    }
    @Test fun pendingLimitPreservesDurableRowsAndReportsNewIntervalsThatCannotFit() {
        val dispatcher = StandardTestDispatcher(); val sink = Sink().apply { offline = true }; val journal = Journal()
        val writer = writer(sink, journal, dispatcher, limit = 2)
        writer.record(event()); writer.record(event(1)); dispatcher.scheduler.runCurrent()
        writer.record(event(2)); dispatcher.scheduler.runCurrent(); writer.close(); dispatcher.scheduler.runCurrent()
        assertEquals(listOf(event(), event(1)), journal.rows); assertEquals(1L, writer.status.value.missed)
        assertTrue(writer.completion.isCompleted)
    }
    @Test fun overlappingCloseAndReopenSerializeTheJournalBeforeReplay() {
        val dispatcher = StandardTestDispatcher(); val gate = CompletableDeferred<Unit>()
        val sink = Sink().apply { this.gate = gate }; val journal = Journal()
        val first = writer(sink, journal, dispatcher); first.record(event()); dispatcher.scheduler.runCurrent()
        first.close()
        val nextSink = Sink(); val second = writer(nextSink, journal, dispatcher)
        second.record(event(1)); dispatcher.scheduler.runCurrent()
        assertEquals(0, nextSink.attempts); assertEquals(listOf(event()), journal.rows)
        gate.complete(Unit); dispatcher.scheduler.runCurrent(); second.close(); dispatcher.scheduler.runCurrent()
        assertTrue(first.completion.isCompleted); assertTrue(second.completion.isCompleted)
        assertEquals(listOf(event()), sink.rows.values.toList()); assertEquals(listOf(event(1)), nextSink.rows.values.toList())
        assertTrue(journal.rows.isEmpty())
    }
    @Test fun profileBecomingReadOnlyNeverCommitsAcceptedRowsOrOverwritesTheJournal() {
        val dispatcher = StandardTestDispatcher(); val sink = Sink(); val journal = Journal()
        var writable = true
        val writer = writer(sink, journal, dispatcher, writable = { writable })
        writer.record(event()); writable = false; dispatcher.scheduler.runCurrent()
        assertTrue(writer.completion.isCompleted); assertEquals(0, journal.saves); assertEquals(0, sink.attempts)
        assertEquals(ListeningStoreIssue.PROFILE_READ_ONLY, writer.status.value.issue)
        assertEquals(1L, writer.status.value.missed)
    }
    @Test fun failedCheckpointNeverCallsSqliteAndReportsIntervalsStillOnlyInMemory() {
        val dispatcher = StandardTestDispatcher(); val sink = Sink(); val journal = Journal().apply { rejectWrites = true }
        val writer = writer(sink, journal, dispatcher)
        writer.record(event()); dispatcher.scheduler.runCurrent(); writer.close(); dispatcher.scheduler.runCurrent()
        assertTrue(writer.completion.isCompleted); assertEquals(0, sink.attempts); assertTrue(journal.rows.isEmpty())
        assertEquals(ListeningStoreIssue.IO_ERROR, writer.status.value.issue); assertEquals(1L, writer.status.value.missed)
    }
    @Test fun conflictingPayloadStopsRetryAndKeepsTheJournalForRecovery() {
        val dispatcher = StandardTestDispatcher(); val sink = Sink(); val journal = Journal()
        sink.rows[event().sessionId to event().sequence] = event().copy(listenedMs = 2000)
        val writer = writer(sink, journal, dispatcher)
        writer.record(event()); dispatcher.scheduler.runCurrent()
        assertTrue(writer.completion.isCompleted); assertEquals(1, sink.attempts)
        assertEquals(ListeningStoreIssue.CONFLICT, writer.status.value.issue)
        assertEquals(listOf(event()), journal.rows); assertEquals(2000L, sink.rows.values.single().listenedMs)
    }
    @Test fun invalidCaptureIsRejectedBeforeEitherStorageLayer() {
        val dispatcher = StandardTestDispatcher(); val sink = Sink(); val journal = Journal()
        val writer = writer(sink, journal, dispatcher)
        assertFalse(writer.record(event().copy(listenedMs = 5001)))
        writer.close(); dispatcher.scheduler.runCurrent()
        assertEquals(0, sink.attempts); assertEquals(0, journal.saves); assertEquals(1L, writer.status.value.missed)
    }
}
