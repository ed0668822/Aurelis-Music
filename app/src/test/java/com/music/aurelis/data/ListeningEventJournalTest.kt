// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.stats.*
import java.io.File
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
class ListeningEventJournalTest {
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private fun journal() = ListeningEventJournal(app, ProfileRepository.profileId)
    private fun event(sequence: Long = 0) = ListeningEvent("session", sequence, "song", "copy", 1234, 1000, 60_000)
    @Test fun atomicRoundTripPreservesFullWidthFieldsAndUsesPrivateNoBackupStorage() {
        val journal = journal()
        assertTrue(journal.read().isEmpty()); assertFalse(journal.file.exists())
        val edge = event(Long.MAX_VALUE).copy(observedAtMs = Long.MAX_VALUE, durationMs = Long.MAX_VALUE)
        journal.save(listOf(event(), edge))
        assertEquals(listOf(event(), edge), journal().read())
        assertTrue(journal.file.canonicalPath.startsWith(app.noBackupFilesDir.canonicalPath + File.separator))
        journal.save(emptyList()); assertTrue(journal().read().isEmpty())
    }
    @Test fun legacyAtomicBackupIsRecoveredBeforeReplay() {
        val journal = journal(); journal.save(listOf(event()))
        val original = journal.file.readBytes()
        File(journal.file.path + ".bak").writeBytes(original)
        journal.file.writeText("interrupted replacement")
        assertEquals(listOf(event()), journal.read())
        assertArrayEquals(original, journal.file.readBytes())
    }
    @Test fun futureAndMalformedSnapshotsAreRejectedWithoutReplacingTheirBytes() {
        val journal = journal(); journal.file.parentFile!!.mkdirs()
        val snapshots = listOf(
            "{\"version\":2,\"events\":[]}".toByteArray() to NewerListeningJournal::class.java,
            "{\"version\":1,\"events\":[],\"unknown\":true}".toByteArray() to CorruptListeningJournal::class.java,
            "{\"version\":\"1\",\"events\":[]}".toByteArray() to CorruptListeningJournal::class.java,
            byteArrayOf(0xc3.toByte(), 0x28) to CorruptListeningJournal::class.java)
        for ((bytes, expected) in snapshots) {
            journal.file.writeBytes(bytes)
            try { journal.read(); fail("Snapshot should be rejected") }
            catch (error: java.io.IOException) { assertTrue(error.toString(), expected.isInstance(error)) }
            assertArrayEquals(bytes, journal.file.readBytes())
        }
    }
    @Test fun writerRecoversOriginalEventsFromARealJournalAndNativeDatabaseWithoutDoubleCounting() = runBlocking {
        val journal = journal(); val original = listOf(event(), event(1))
        journal.save(original)
        val first = ListeningEventStore(app)
        try { assertTrue(first.append(original) is ListeningStoreResult.Success<*>) } finally { first.close() }
        // Model process death after SQLite COMMIT, before acknowledging the private journal.
        val writer = ListeningEventWriter(ListeningEventStore(app), journal(), { true })
        withTimeout(10_000) { writer.close().join() }
        assertTrue(journal().read().isEmpty())
        val reopened = ListeningEventStore(app)
        try {
            val rows = reopened.events() as ListeningStoreResult.Success<List<ListeningEvent>>
            assertEquals(original, rows.value)
            val total = reopened.totalsBySong() as ListeningStoreResult.Success<List<ListeningSongTotal>>
            assertEquals(listOf(ListeningSongTotal("song", 2000, 2)), total.value)
        } finally { reopened.close() }
    }
}
