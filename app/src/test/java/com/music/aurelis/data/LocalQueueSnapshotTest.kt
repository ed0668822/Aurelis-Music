// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.ProfileId
import com.music.aurelis.data.model.QueueTier
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LocalQueueSnapshotTest {
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val prefs get() = ProfileStorage(app).open(ProfileRepository.profileId, ProfileDataArea.QUEUE)
    private fun song(id: String) = LocalSong(id, id, "Artist", null, "content://media/$id", null, 60_000L, null, null, null)
    @Before fun clear() { prefs.edit().clear().commit() }

    @Test fun versionTwoRoundTripPreservesDuplicateOccurrenceIdentityTierAndPermutation() {
        val saved = SavedLocalQueue(listOf("1", "2", "1"), 2, 12345, 2, true,
            listOf("first", "second", "third"), listOf(QueueTier.CONTEXT, QueueTier.USER_QUEUE, QueueTier.AUTOPLAY), listOf(2, 0, 1))
        LocalQueueStore(app).save(saved)
        val loaded = requireNotNull(LocalQueueStore(app).read())
        assertEquals(saved, loaded)
        val (resolved, current) = loaded.resolve(listOf(song("1"), song("2")))
        assertEquals(2, current); assertEquals("third", resolved[current].queueEntryId)
        assertEquals(QueueTier.AUTOPLAY, resolved[current].queueTier)
        assertNotEquals(resolved[0].queueEntryId, resolved[2].queueEntryId)
        assertEquals(listOf(2, 0, 1), loaded.resolveShuffleOrder(listOf(song("1"), song("2"))))
        assertNull(LocalQueueStore(app, ProfileId("other")).read())
    }
    @Test fun missingSongsRemapThePermutationAndKeepTheNearestSurvivingOccurrence() {
        val saved = SavedLocalQueue(listOf("gone", "1", "2", "1"), 2, 12345, 0, true,
            listOf("gone", "first", "missing-current", "last"), emptyList(), listOf(2, 3, 0, 1))
        val (resolved, current) = saved.resolve(listOf(song("1")))
        assertEquals(listOf("first", "last"), resolved.map { it.queueEntryId })
        assertEquals(1, current)
        assertEquals(listOf(1, 0), saved.resolveShuffleOrder(listOf(song("1"))))
    }
    @Test fun legacySnapshotRemainsReadableAndMigrationKeepsItsOriginalBytes() {
        val raw = """{"version":1,"ids":["1","1"],"index":1,"position":3456,"repeat":1,"shuffle":false}"""
        prefs.edit().putString("queue_v1", raw).commit()
        val store = LocalQueueStore(app)
        val saved = requireNotNull(store.read())
        assertEquals(SavedLocalQueue(listOf("1", "1"), 1, 3456, 1, false), saved)
        store.save(saved)
        assertEquals(raw, prefs.getString("queue_v1", null))
        assertEquals(saved, LocalQueueStore(app).read())
    }
    @Test fun malformedAndFutureSnapshotsAreReadOnlyWithoutFallbackOrOverwrite() {
        val base = """{"version":2,"ids":["1","1"],"index":1,"position":3456,"repeat":1,"shuffle":true,"entries":["a","b"],"tiers":["CONTEXT","USER_QUEUE"],"order":[1,0]}"""
        val invalid = listOf(base.replace("\"version\":2", "\"version\":99"),
            base.replace("[\"a\",\"b\"]", "[\"a\",\"a\"]"), base.replace("[1,0]", "[1,1]"),
            base.replace("[1,0]", "[1.0,0]"), base.replace("\"index\":1", "\"index\":8"),
            base.replace("\"USER_QUEUE\"", "\"FUTURE_TIER\""), base.replace("3456", "-1"),
            base.replace("[\"a\",\"b\"]", "[\"a\"]"), base.replace("\"shuffle\":true", "\"shuffle\":\"true\""))
        for (raw in invalid) {
            prefs.edit().putString("queue_v2", raw).commit()
            val store = LocalQueueStore(app)
            assertTrue(raw, store.readOnly); assertNull(store.read())
            store.save(SavedLocalQueue(emptyList(), 0, 0, 0, false))
            assertEquals(raw, prefs.getString("queue_v2", null))
        }
    }
    @Test fun anExplicitEmptySnapshotTakesPrecedenceOverAnOlderQueue() {
        val raw = """{"version":1,"ids":["1"],"index":0,"position":10,"repeat":0,"shuffle":false}"""
        prefs.edit().putString("queue_v1", raw).commit()
        LocalQueueStore(app).save(SavedLocalQueue(emptyList(), 0, 0, 0, false))
        assertTrue(requireNotNull(LocalQueueStore(app).read()).ids.isEmpty())
        assertEquals(raw, prefs.getString("queue_v1", null))
    }
}
