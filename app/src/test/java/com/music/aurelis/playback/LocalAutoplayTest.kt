// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LocalAutoplayTest {
    private fun song(id: String, artist: String = "Artista") = LocalSong(id, "Tema $id", artist, "Álbum",
        "content://media/$id", null, 1000L, null, null, null)
    private fun fixture(load: suspend () -> List<LocalSong>, test: (RecordingPlayer, MutableStateFlow<Boolean>, kotlinx.coroutines.test.TestCoroutineScheduler) -> Unit) {
        val dispatcher = StandardTestDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val player = RecordingPlayer()
        val enabled = MutableStateFlow(true)
        PlayerConnection.playSongs(player, listOf(song("1")))
        val binding = LocalAutoplayBinding(player, scope, enabled, load)
        dispatcher.scheduler.runCurrent()
        try { test(player, enabled, dispatcher.scheduler) }
        finally { binding.close(); scope.cancel(); dispatcher.scheduler.runCurrent(); player.release() }
    }
    private fun end(player: RecordingPlayer) = player.publish(player.snapshot.buildUpon().setPlaybackState(Player.STATE_ENDED).build())

    @Test fun suggestionsAreBoundedDeterministicLocalAndExcludeQueueDuplicates() {
        val current = song("1")
        val candidates = listOf(song("3", "Otro"), song("2"), song("1"), song("2"), song("4"), song("remote").copy(contentUri = "https://example.com/audio"))
        assertEquals(listOf("2", "4"), AutoplayEngine().suggestNextTracks(current, candidates, setOf("1"), 2).map { it.id })
        assertTrue(AutoplayEngine().suggestNextTracks(current, candidates, setOf("1", "2", "3", "4")).isEmpty())
    }
    @Test fun naturalEndExtendsOnceWithFreshAutoplayOccurrencesAndStartsTheFirst() = fixture({ listOf(song("1"), song("2"), song("3")) }) { player, _, scheduler ->
        val first = player.currentMediaItem!!.queueEntryId
        end(player); scheduler.runCurrent()
        assertEquals(3, player.mediaItemCount); assertEquals("2", player.currentMediaItem!!.mediaId)
        assertTrue(player.playWhenReady)
        assertEquals(QueueTier.AUTOPLAY, player.currentMediaItem!!.queueTier)
        assertNotEquals(first, player.currentMediaItem!!.queueEntryId)
        assertEquals(1, player.replacements); assertEquals(2, player.prepares)
        scheduler.runCurrent(); assertEquals(3, player.mediaItemCount)
    }
    @Test fun pausedRestoreRepeatAndDisabledAutoplayNeverStartContinuation() = fixture({ listOf(song("2")) }) { player, enabled, scheduler ->
        player.pause(); end(player); scheduler.runCurrent(); assertEquals(1, player.mediaItemCount)
        player.repeatMode = Player.REPEAT_MODE_ALL; player.play(); scheduler.runCurrent()
        assertEquals(1, player.mediaItemCount)
        enabled.value = false; scheduler.runCurrent()
        player.repeatMode = Player.REPEAT_MODE_OFF; end(player); scheduler.runCurrent()
        assertEquals(1, player.mediaItemCount)
    }
    @Test fun lateCatalogResultCannotOverridePauseOrAReplacementQueue() {
        val release = CompletableDeferred<Unit>()
        fixture({ withContext(NonCancellable) { release.await() }; listOf(song("2")) }) { player, _, scheduler ->
            end(player); scheduler.runCurrent()
            player.pause()
            PlayerConnection.playSongs(player, listOf(song("new")))
            player.pause(); release.complete(Unit); scheduler.runCurrent()
            assertEquals(1, player.mediaItemCount); assertEquals("new", player.currentMediaItem!!.mediaId)
            assertFalse(player.playWhenReady)
        }
    }
    @Test fun exhaustedOrFailingCatalogDoesNotLoopOrReplaceTheQueue() {
        var calls = 0
        fixture({ calls++; emptyList() }) { player, _, scheduler ->
            end(player); scheduler.runCurrent(); end(player); scheduler.runCurrent()
            assertEquals(1, calls); assertEquals(1, player.mediaItemCount)
        }
        fixture({ error("provider unavailable") }) { player, _, scheduler ->
            end(player); scheduler.runCurrent(); assertEquals(1, player.mediaItemCount)
        }
    }
}
