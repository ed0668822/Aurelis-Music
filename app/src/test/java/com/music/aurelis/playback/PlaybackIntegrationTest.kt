package com.music.aurelis.playback

import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class PlaybackIntegrationTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private lateinit var player: RecordingPlayer
    private lateinit var vm: PlaybackViewModel
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private fun song(id: String) = LocalSong(id, "Canción $id", "Artista", "Álbum", "content://media/external/audio/media/$id",
        null, 60_000, null, 2, 2026, dateAddedMs = 10_000, sizeBytes = 1234, albumId = "9", artistId = "8", discNumber = 1)
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        SettingsRepository.lyricsSources.value = emptySet()
        player = RecordingPlayer()
        vm = PlaybackViewModel(app) { player }
        store.put("player", vm)
    }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun settle() { dispatcher.scheduler.runCurrent(); shadowOf(Looper.getMainLooper()).idle(); dispatcher.scheduler.runCurrent() }
    private fun play() { vm.playSongs(listOf(song("1"), song("2"), song("3"))); settle() }

    @Test fun switchingProfileCancelsTapWaitingForControllerAndNewTapCanConnect() {
        val gate = CompletableDeferred<Unit>()
        vm = PlaybackViewModel(app) { gate.await(); player }; store.put("player", vm)
        vm.playSongs(listOf(song("1"))); settle()
        val second = ProfileRepository.create("Segundo")
        ProfileRepository.switchTo(second)
        gate.complete(Unit); settle()
        assertEquals(0, player.mediaItemCount)
        assertNull(vm.state.value.currentSong)
        vm.playSongs(listOf(song("2"))); settle()
        assertEquals("2", vm.state.value.currentSong?.id)
    }

    @Test fun immediateTapWaitsForConnectionClampsIndexAndUsesCompleteMetadata() {
        val gate = CompletableDeferred<Unit>()
        var connections = 0
        vm = PlaybackViewModel(app) { connections++; gate.await(); player }
        store.put("player", vm)
        vm.connect(); vm.connect(); vm.playSongs(listOf(song("1"), song("2")), 999)
        settle(); assertEquals(1, connections); assertEquals(0, player.mediaItemCount)
        gate.complete(Unit); settle()
        assertTrue(vm.state.value.isConnected)
        assertEquals(1, vm.state.value.currentIndex)
        assertEquals(song("2"), vm.state.value.currentSong)
        assertEquals(60_000, vm.state.value.durationMs)
        assertFalse(vm.state.value.canNext)
    }
    @Test fun latestTapWinsAndEmptySelectionDoesNotClearTheQueue() {
        val gate = CompletableDeferred<Unit>()
        vm = PlaybackViewModel(app) { gate.await(); player }; store.put("player", vm)
        vm.playSongs(listOf(song("1"))); settle()
        vm.playSongs(listOf(song("2"))); settle(); gate.complete(Unit); settle()
        vm.playSongs(emptyList()); settle()
        assertEquals(listOf("2"), vm.state.value.queue.map { it.id })
    }
    @Test fun externalCommandsSynchronizeRepeatShuffleSeekAndPause() {
        play()
        player.repeatMode = Player.REPEAT_MODE_ALL; player.shuffleModeEnabled = true
        player.seekTo(12_345); player.pause(); settle()
        assertEquals(Player.REPEAT_MODE_ALL, vm.state.value.repeatMode)
        assertTrue(vm.state.value.shuffleEnabled); assertFalse(vm.state.value.isPlaying)
        assertEquals(12_345, vm.state.value.positionMs)
        vm.cycleRepeat(); settle(); assertEquals(Player.REPEAT_MODE_ONE, player.repeatMode)
        vm.cycleRepeat(); settle(); assertEquals(Player.REPEAT_MODE_OFF, player.repeatMode)
        vm.cycleRepeat(); settle(); assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode)
        vm.toggleShuffle(); settle(); assertFalse(player.shuffleModeEnabled)
    }
    @Test fun queueRemovalIsAppliedOnceAndInvalidIndexesDoNothing() {
        play(); vm.seekToIndex(1); settle()
        vm.removeFromQueue(-1); vm.removeFromQueue(99); vm.removeFromQueue(1); settle()
        assertEquals(3, vm.state.value.queue.size)
        vm.removeFromQueue(0); settle()
        assertEquals(listOf("2", "3"), vm.state.value.queue.map { it.id })
        assertEquals(0, vm.state.value.currentIndex); assertEquals("2", vm.state.value.currentSong?.id)
        vm.next(); settle(); assertEquals("3", vm.state.value.currentSong?.id)
        vm.seekTo(90_000); settle(); assertEquals(60_000, vm.state.value.positionMs)
        vm.seekTo(-1); settle(); assertEquals(0, vm.state.value.positionMs)
    }
    @Test fun disconnectDisablesControlsAndReconnectsWithoutAnExtraEngine() {
        var disconnected: () -> Unit = {}
        var attempts = 0
        val replacement = RecordingPlayer()
        vm = PlaybackViewModel(app) { callback -> disconnected = callback; if (++attempts == 1) player else replacement }
        store.put("player", vm)
        play(); disconnected(); settle()
        assertFalse(vm.state.value.isConnected); assertFalse(vm.state.value.canSeek)
        assertFalse(vm.state.value.isPlayRequested); assertTrue(player.released)
        vm.retry(); settle(); assertTrue(vm.state.value.isConnected); assertEquals(2, attempts)
    }
    @Test fun clearingWhileConnectingCancelsPendingPlaybackRequest() {
        val gate = CompletableDeferred<Unit>()
        var supplied = false
        vm = PlaybackViewModel(app) { gate.await(); supplied = true; player }; store.put("player", vm)
        vm.playSongs(listOf(song("1"))); settle(); store.clear(); gate.complete(Unit); settle()
        assertFalse(supplied); assertEquals(0, player.mediaItemCount)
    }
    @Test fun bufferingCanBePausedAndEndedCanResumeFromStart() {
        play(); player.publish(player.snapshot.buildUpon().setPlaybackState(Player.STATE_BUFFERING).build()); settle()
        assertTrue(vm.state.value.isPlayRequested)
        vm.togglePlayPause(); settle(); assertFalse(player.playWhenReady)
        player.publish(player.snapshot.buildUpon().setPlaybackState(Player.STATE_ENDED).setContentPositionMs(60_000).build()); settle()
        vm.resume(); settle(); assertEquals(0, vm.state.value.positionMs); assertTrue(player.playWhenReady)
    }
    @Test fun failedFileIsVisibleAndRetryPreparesSameQueue() {
        play(); player.publish(player.snapshot.buildUpon().setPlaybackState(Player.STATE_IDLE)
            .setPlayerError(PlaybackException("test missing", null, PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)).build()); settle()
        assertTrue(vm.state.value.errorMessage!!.contains("ya no está disponible"))
        val before = player.prepares; vm.retry(); settle()
        assertEquals(before + 1, player.prepares); assertNull(vm.state.value.errorMessage)
        assertEquals(3, vm.state.value.queue.size)
    }
    @Test fun connectionFailureCanRetryAndClearingReleasesOnlyController() {
        var attempts = 0
        vm = PlaybackViewModel(app) { if (++attempts == 1) error("test connection") else player }; store.put("player", vm)
        vm.connect(); settle(); assertFalse(vm.state.value.isConnected); assertNotNull(vm.state.value.errorMessage)
        vm.retry(); settle(); assertTrue(vm.state.value.isConnected)
        store.clear(); assertTrue(player.released)
    }
}
