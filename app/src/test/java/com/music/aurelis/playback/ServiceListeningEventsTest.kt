// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.stats.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@OptIn(ExperimentalCoroutinesApi::class)
@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ServiceListeningEventsTest {
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private fun song(id: String = "1") = LocalSong(id, id, "artist", "album", "content://media/external/audio/media/$id", null, 60_000, null, 1, 2026)
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private inner class Capture : AutoCloseable {
        var now = 0L
        var utc = 100_000L
        val rows = mutableListOf<ListeningEvent>()
        val library = LocalUserLibrary(app, clock = { now })
        val player = RecordingPlayer()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher())
        val recorder = ServicePlaybackRecorder(player, library, LocalQueueStore(app), scope,
            event = { rows += it }, wallClock = { utc }, clock = { now })
        fun start(songs: List<LocalSong> = listOf(song())) { PlayerConnection.playSongs(player, songs); idle(); assertTrue(player.isPlaying) }
        fun elapsed(ms: Long) { now += ms; recorder.tick() }
        override fun close() { recorder.close(); scope.cancel(); player.release() }
    }
    @Test fun measuredTimeExcludesSeekDistancePauseBufferingAndFocusSuppression() {
        Capture().use { c ->
            c.start(); c.elapsed(1000)
            c.player.seekTo(50_000); idle(); assertEquals(1000L, c.rows.sumOf { it.listenedMs })
            c.now += 250; c.player.pause(); idle(); c.elapsed(2000)
            c.player.play(); idle(); c.elapsed(1000)
            c.player.publish(c.player.snapshot.buildUpon().setPlaybackState(Player.STATE_BUFFERING).build()); idle()
            c.elapsed(3000)
            c.player.publish(c.player.snapshot.buildUpon().setPlaybackState(Player.STATE_READY).build()); idle()
            c.elapsed(500)
            c.player.publish(c.player.snapshot.buildUpon().setPlaybackSuppressionReason(Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS).build()); idle()
            c.elapsed(2000)
            c.player.publish(c.player.snapshot.buildUpon().setPlaybackSuppressionReason(Player.PLAYBACK_SUPPRESSION_REASON_NONE).build()); idle()
            c.elapsed(750); c.recorder.close(); c.recorder.close()
            assertEquals(3500L, c.rows.sumOf { it.listenedMs })
            assertEquals(3500L, c.library.state.value.listening["1"]?.listenedMs)
            assertEquals(c.rows.indices.map { it.toLong() }, c.rows.map { it.sequence })
            assertEquals(1, c.rows.map { it.sessionId }.toSet().size)
        }
    }
    @Test fun duplicateQueueOccurrencesAndRepeatHaveSeparateSessionsWithoutLosingTheirOccurrenceIds() {
        Capture().use { c ->
            c.start(listOf(song(), song())); val first = c.player.currentMediaItem!!.queueEntryId
            c.elapsed(1000); c.player.seekToNextMediaItem(); idle()
            val second = c.player.currentMediaItem!!.queueEntryId
            c.elapsed(1000)
            // Repeat-one keeps the same timeline occurrence but starts a fresh listening episode.
            c.recorder.onMediaItemTransition(c.player.currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
            c.elapsed(1000)
            c.library.flush()
            assertNotEquals(first, second)
            assertEquals(listOf(first, second, second), c.rows.map { it.queueEntryId })
            assertEquals(3, c.rows.map { it.sessionId }.toSet().size)
            assertEquals(listOf(0L, 0L, 0L), c.rows.map { it.sequence })
            assertEquals(3000L, c.library.state.value.listening["1"]?.listenedMs)
        }
    }
    @Test fun utcChangesDoNotAlterElapsedTimeAndUncertainLongGapsAreNotInvented() {
        Capture().use { c ->
            c.start(); c.elapsed(1000); c.utc = 50_000; c.elapsed(1000)
            c.elapsed(20_000); c.elapsed(1000)
            assertEquals(listOf(100_000L, 50_000L, 50_000L), c.rows.map { it.observedAtMs })
            assertEquals(3000L, c.rows.sumOf { it.listenedMs })
        }
    }
    @Test fun serviceBindingDrainsToNativeSqliteAndPausedRestoreAddsNoListening() {
        val dispatcher = StandardTestDispatcher(); val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val player = RecordingPlayer()
        val binding = ProfilePlaybackBinding(app, player, scope, songs = { listOf(song()) })
        try {
            dispatcher.scheduler.runCurrent(); PlayerConnection.playSongs(player, listOf(song())); idle()
            dispatcher.scheduler.runCurrent()
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
            dispatcher.scheduler.advanceTimeBy(1000); dispatcher.scheduler.runCurrent()
            player.pause(); idle()
        } finally {
            runBlocking { withTimeout(10_000) { binding.close().join() } }; player.release()
        }
        val reader = ListeningEventStore(app)
        val before = runBlocking {
            try { (reader.events() as ListeningStoreResult.Success<List<ListeningEvent>>).value } finally { reader.close() }
        }
        assertTrue(before.isNotEmpty()); assertEquals(1000L, before.sumOf { it.listenedMs })
        val restoredPlayer = RecordingPlayer()
        val restarted = ProfilePlaybackBinding(app, restoredPlayer, scope, songs = { listOf(song()) })
        try {
            dispatcher.scheduler.runCurrent(); idle(); assertFalse(restoredPlayer.playWhenReady)
            assertEquals("1", restoredPlayer.currentMediaItem?.mediaId)
            shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS)
            dispatcher.scheduler.advanceTimeBy(2000); dispatcher.scheduler.runCurrent()
        } finally {
            runBlocking { withTimeout(10_000) { restarted.close().join() } }; scope.cancel(); restoredPlayer.release()
        }
        runBlocking {
            val after = ListeningEventStore(app)
            try { assertEquals(before, (after.events() as ListeningStoreResult.Success<List<ListeningEvent>>).value) } finally { after.close() }
        }
    }
}
