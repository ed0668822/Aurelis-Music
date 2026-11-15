// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import android.database.sqlite.SQLiteDatabase
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.ProfileId
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
class ListeningDatabaseRecoveryTest {
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val event get() = ListeningEvent("session", 0, "song", "entry", 1000, 1000, 60_000)
    private fun <T> value(result: ListeningStoreResult<T>): T {
        assertTrue(result.toString(), result is ListeningStoreResult.Success<*>)
        return (result as ListeningStoreResult.Success<T>).value
    }
    @Test fun profileScopeReadOnlyAndClosedStatesCannotMutateAnotherHistory() = runBlocking {
        val allowed = java.util.concurrent.atomic.AtomicBoolean(true)
        val store = ListeningEventStore(app, writeAllowed = { allowed.get() })
        val other = ListeningEventStore(app, ProfileId("archived"), writeAllowed = { true })
        try {
            value(store.append(listOf(event)))
            assertTrue(value(other.events()).isEmpty())
            value(other.append(listOf(event.copy(songId = "different"))))
            assertEquals(listOf(event), value(store.events()))
            allowed.set(false)
            assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.PROFILE_READ_ONLY), store.append(listOf(event.copy(sequence = 1))))
            assertEquals(listOf(event), value(store.events()))
            store.close()
            assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.CLOSED), store.events())
            assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.CLOSED), store.append(listOf(event)))
        } finally { store.close(); other.close() }
    }
    @Test fun futureSchemaAndItsPayloadSurviveRejectedReadsAndWrites() = runBlocking {
        val store = ListeningEventStore(app)
        withContext(Dispatchers.IO) {
            store.databaseFile.parentFile!!.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(store.databaseFile, null).use { db ->
                db.execSQL("CREATE TABLE future_payload(value TEXT)")
                db.execSQL("INSERT INTO future_payload VALUES (?)", arrayOf("keep-me"))
                db.version = 99
            }
        }
        try {
            assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.NEWER_SCHEMA), store.events())
            assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.NEWER_SCHEMA), store.append(listOf(event)))
        } finally { store.close() }
        withContext(Dispatchers.IO) {
            SQLiteDatabase.openDatabase(store.databaseFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                assertEquals(99, db.version)
                db.rawQuery("SELECT value FROM future_payload", null).use { row -> assertTrue(row.moveToFirst()); assertEquals("keep-me", row.getString(0)) }
            }
        }
    }
    @Test fun corruptDatabaseIsPreservedInsteadOfDeletedAndRecreatedEmpty() = runBlocking {
        val store = ListeningEventStore(app)
        val bytes = ByteArray(512) { (it % 127).toByte() }
        withContext(Dispatchers.IO) { store.databaseFile.parentFile!!.mkdirs(); store.databaseFile.writeBytes(bytes) }
        try {
            val result = store.events()
            assertTrue(result is ListeningStoreResult.Failure)
            assertTrue((result as ListeningStoreResult.Failure).issue in setOf(ListeningStoreIssue.CORRUPT, ListeningStoreIssue.IO_ERROR))
            assertTrue(store.databaseFile.exists())
            assertArrayEquals(bytes, store.databaseFile.readBytes())
        } finally { store.close() }
    }
    @Test fun unavailableDatabaseIsReportedAsFailureRatherThanEmptySuccess() = runBlocking {
        val store = ListeningEventStore(app)
        store.databaseFile.mkdirs() // A directory cannot be opened as a SQLite file.
        try {
            assertEquals(ListeningStoreResult.Failure(ListeningStoreIssue.IO_ERROR), store.events())
            assertTrue(store.databaseFile.isDirectory)
        } finally { store.close() }
    }
}
