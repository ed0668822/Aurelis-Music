// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.Manifest
import org.robolectric.Shadows.shadowOf
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.R
import com.music.aurelis.*
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier
import com.music.aurelis.ui.screens.queueSectionsPreservePlaybackOrder
import com.music.aurelis.playback.*
import com.music.aurelis.ui.navigation.*
import com.music.aurelis.ui.theme.AurelisTheme
import java.io.File
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "es-rES-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayerIntegrationTest {
    @get:org.junit.Rule(order = 0) val registerActivity = com.music.aurelis.ui.RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val store = ViewModelStore()
    private lateinit var vm: PlaybackViewModel
    private lateinit var player: RecordingPlayer
    private fun song(id: String) = LocalSong(id, "Canción local $id", "Artista local", "Álbum local",
        "content://media/external/audio/media/$id", null, 60_000, null, 1, 2026)
    @After fun cleanup() { compose.runOnIdle {
        store.clear()
        SettingsRepository.setLastPlayerScreen(LastPlayerScreen.MAIN)
    } }
    private fun start(
        fontScale: Float? = null,
        playerScreen: LastPlayerScreen = LastPlayerScreen.MAIN,
    ) {
        SettingsRepository.setLastPlayerScreen(playerScreen)
        SettingsRepository.setReduceAnimation(true)
        SettingsRepository.setShowQueueHistory(false)
        SettingsRepository.lyricsSources.value = emptySet()
        player = RecordingPlayer()
        vm = PlaybackViewModel(app) { player }; store.put("player", vm)
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val library = LibraryViewModel(app, LocalLibrarySource { listOf(song("1"), song("2"), song("3")) }); store.put("library", library)
        compose.setContent { AurelisTheme {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, fontScale ?: density.fontScale)) {
                AurelisNavigationContent(rememberAppNavigator(), vm, library)
            }
        } }
        compose.runOnIdle { library.loadLibrary(); vm.playSongs(listOf(song("1"), song("2"), song("3"))) }
        compose.waitForIdle()
        compose.runOnIdle { player.pause() }
    }
    /** On the BitChord-style full-screen lyric pane, the bottom-half tap restores
     * controls before translation/search buttons become interactive. */
    private fun revealFullScreenLyricsControls() {
        val list = compose.onNodeWithTag("lyrics-list")
        val bounds = list.fetchSemanticsNode().boundsInRoot
        // TouchInjectionScope does not expose PointerInputScope.size; use the
        // actual measured Compose node to pick a point in its bottom half.
        list.performTouchInput {
            click(androidx.compose.ui.geometry.Offset(bounds.width / 2f, bounds.height * 0.75f))
        }
        compose.waitForIdle()
    }

    private fun assertPrimaryTransportInsidePlayer(tag: String) {
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        for (key in listOf(R.string.player_play, R.string.player_next, R.string.player_previous)) {
            val node = compose.onNodeWithContentDescription(compose.activity.getString(key)).assertIsDisplayed()
            val control = node.fetchSemanticsNode().boundsInRoot
            assertTrue("Transport must fit the initial viewport: $key", control.top >= bounds.top && control.bottom <= bounds.bottom && control.left >= bounds.left && control.right <= bounds.right)
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = "es-rES-w320dp-h568dp-mdpi", application = AurelisApplication::class)
    fun compactPortraitWithLargeTextKeepsTransportVisibleAndUsesTheExistingQueue() {
        start(fontScale = 1.5f)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_open)).performClick()
        assertPrimaryTransportInsidePlayer("player-portrait")
        val viewport = compose.onNodeWithTag("player-portrait").fetchSemanticsNode().boundsInRoot
        val cover = compose.onNodeWithTag("player-artwork").fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.width, cover.width, 1f)
        val queue = vm.state.value.queue.map { it.queueEntryId }
        val replacements = player.replacements
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_next)).performClick()
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id) }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_previous)).performClick()
        compose.runOnIdle {
            assertEquals("1", vm.state.value.currentSong?.id)
            assertEquals(queue, vm.state.value.queue.map { it.queueEntryId })
            assertEquals(replacements, player.replacements)
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = "es-rES-w640dp-h320dp-land-mdpi", application = AurelisApplication::class)
    fun shortLandscapeKeepsPrimaryTransportVisibleWithLargeText() {
        start(fontScale = 1.5f)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_open)).performClick()
        assertPrimaryTransportInsidePlayer("player-landscape")
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_play)).performClick()
        compose.runOnIdle { assertTrue(player.playWhenReady) }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_pause)).performClick()
        compose.runOnIdle { assertFalse(player.playWhenReady); assertEquals("1", vm.state.value.currentSong?.id) }
    }

    @Test fun rapidSkipsAndSameSongDuplicateQueueDoNotReopenThePlayer() {
        start()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_open)).performClick()
        val original = vm.state.value.queue.map { it.queueEntryId }
        compose.runOnIdle {
            vm.next()
            vm.previous()
            vm.next()
            vm.previous()
        }
        compose.waitForIdle()
        compose.onNodeWithTag("player-portrait").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("1", vm.state.value.currentSong?.id)
            assertEquals(original, vm.state.value.queue.map { it.queueEntryId })
            vm.playNext(listOf(song("1")))
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(4, vm.state.value.queue.size)
            assertEquals("1", vm.state.value.queue[1].id)
            assertNotEquals(vm.state.value.queue[0].queueEntryId, vm.state.value.queue[1].queueEntryId)
            vm.next()
        }
        compose.waitForIdle()
        compose.onNodeWithTag("player-portrait").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("1", vm.state.value.currentSong?.id)
            assertEquals(vm.state.value.queue[1].queueEntryId, vm.state.value.currentSong?.queueEntryId)
            assertEquals(4, player.mediaItemCount)
        }
    }

    @Test fun playerHeaderHandlesMissingBlankAndNamedAlbumsAndKeepsTheCorrectSong() {
        start()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_open)).performClick()
        for (album in listOf(null, "", "   ", "Álbum 100% local")) {
            compose.runOnIdle { vm.playSongs(listOf(song("1").copy(album = album))) }
            // Tags alone cannot prove the source of the queue.
            compose.onNodeWithText(compose.activity.getString(
                R.string.player_playing_album, compose.activity.getString(R.string.queue))).assertIsDisplayed()
            compose.runOnIdle { assertEquals("1", vm.state.value.currentSong?.id) }
        }
        compose.runOnIdle { vm.playSongs(listOf(song("1").copy(
            playbackSource = "Mi lista local",
            playbackSourceType = com.music.aurelis.data.model.PlaybackSourceType.BROWSE,
            playbackSourceId = "test-source",
        ))) }
        compose.onNodeWithText(compose.activity.getString(
            R.string.player_playing_album, "Mi lista local")).assertIsDisplayed()
    }

    @Test fun hidingVolumePreservesTheControlDeckAndTransportPlacement() {
        val previous = SettingsRepository.hideVolumeBar.value
        try {
            SettingsRepository.setHideVolumeBar(false)
            start()
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_open)).performClick()
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_volume)).assertExists()
            val before = compose.onNodeWithTag("player-control-deck").fetchSemanticsNode().boundsInRoot
            compose.runOnIdle { SettingsRepository.setHideVolumeBar(true) }
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_volume)).assertDoesNotExist()
            assertEquals(before, compose.onNodeWithTag("player-control-deck").fetchSemanticsNode().boundsInRoot)
            assertPrimaryTransportInsidePlayer("player-portrait")
        } finally { compose.runOnIdle { SettingsRepository.setHideVolumeBar(previous) } }
    }

    @Test fun volumeSliderChangesTheRealMusicStreamAndReadsItsActualLevel() {
        val audio = app.getSystemService(android.media.AudioManager::class.java)
        val maximum = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        assertTrue(maximum > 0)
        audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 1, 0)
        compose.setContent { AurelisTheme { com.music.aurelis.ui.components.PlayerVolumeRow() } }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_volume))
            .assertIsEnabled().performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        compose.runOnIdle { assertEquals((maximum * 0.5f + 0.5f).toInt(), audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)) }
    }

    @Test fun exploreUnknownGenreResolvesRealSongsAndBulkFavorites() {
        start()
        compose.onNode(hasText(app.getString(R.string.explore)) and isSelectable()).performClick()
        compose.onNodeWithText("Sin clasificar").performClick()
        compose.onNodeWithText("Seleccionar").performClick()
        compose.onNodeWithText("Todas").performClick()
        compose.onNodeWithText("Favoritos").performClick()
        compose.runOnIdle { assertEquals(setOf("1", "2", "3"), app.userLibrary.state.value.favorites) }
        compose.onNodeWithText("Cancelar selección").performClick()
        compose.onNodeWithText(app.getString(R.string.collection_play)).performClick()
        compose.runOnIdle { assertEquals(3, player.mediaItemCount); assertEquals("1", vm.state.value.currentSong?.id) }
    }

    @Test fun localSearchOpensAnAlbumAndRecordsTheSubmittedQuery() {
        start()
        compose.onNode((hasText(app.getString(R.string.search)) or hasContentDescription(app.getString(R.string.search))) and isSelectable()).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Álbum")
        val songResult = hasText("Canción local 1") and
            hasAnyAncestor(hasTestTag("local-search-results"))
        compose.onNode(songResult).assertExists()
        val albumsTab = hasText("Álbumes") and isSelectable()
        compose.onAllNodes(albumsTab).assertCountEquals(1)
        compose.onNode(albumsTab).performClick()
        compose.onNode(songResult).assertDoesNotExist()
        compose.onNodeWithTag("mini-player").assertTextContains("Canción local 1")
        compose.runOnIdle { assertEquals("1", vm.state.value.currentSong?.id) }
        compose.onNodeWithText("Todo").performClick()
        compose.onNode(songResult).assertExists()
        compose.onAllNodes(albumsTab).assertCountEquals(1)
        compose.onNode(albumsTab).performClick()
        compose.onNode(hasText("Álbum local") and hasClickAction()).performClick()
        compose.onNodeWithText(app.getString(R.string.collection_play)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("Álbum"), LocalSearchHistory(app).entries) }
    }

    @Test fun playbackToolsExposeTimerSpeedAndSilenceActionsWhenServiceSupportsThem() {
        var speed = 1f
        var silence = false
        var autoplay = false
        var duration = 0L
        var afterTrack = false
        compose.setContent { AurelisTheme {
            com.music.aurelis.ui.components.PlaybackToolsDialog(
                PlaybackState(canUsePlaybackTools = true, canSetPlaybackSpeed = true, canSkipSilence = true, canSetAutoplay = true, listeningHistoryMessage = "Algunos intervalos de escucha no se pudieron guardar."),
                { millis, after -> duration = millis; afterTrack = after }, { speed = it }, { silence = it }, { autoplay = it }, {})
        } }
        compose.onNodeWithText("Algunos intervalos de escucha no se pudieron guardar.").assertExists()
        compose.onNodeWithText("Activar continuación local").performScrollTo().performClick()
        compose.onNodeWithText("1.5×").performScrollTo().performClick()
        compose.onNodeWithText("Activar salto de silencio").performScrollTo().performClick()
        compose.onNodeWithText("30 min").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1.5f, speed); assertTrue(silence); assertTrue(autoplay); assertEquals(1_800_000L, duration); assertFalse(afterTrack) }
        compose.onNodeWithText("Al terminar esta canción").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(afterTrack) }
        compose.onNodeWithText("Cancelar temporizador").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0L, duration); assertFalse(afterTrack) }
    }

    @Test fun timerDialogExplainsUnavailableServiceWithoutChangingPlayback() {
        start()
        val current = vm.state.value.currentSong?.queueEntryId
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        // UI-R11 moved playback tools into the song menu: exercise the real
        // route, not a removed redundant button.
        compose.onNodeWithContentDescription(app.getString(R.string.player_song_actions)).performClick()
        compose.onNodeWithText("Herramientas de reproducción").performScrollTo()
            .assertIsEnabled().performClick()
        compose.onNodeWithText("El servicio conectado no ofrece este control.").assertExists()
        compose.onNodeWithText("1.5×").assertIsNotEnabled()
        compose.onNodeWithText("Activar salto de silencio").assertIsNotEnabled()
        compose.onNodeWithText("15 min").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Al terminar esta canción").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Cancelar temporizador").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Cerrar").performClick()
        compose.runOnIdle { assertEquals(current, vm.state.value.currentSong?.queueEntryId); assertFalse(player.playWhenReady) }
    }

    @Test fun queueSectionsOnlyGroupWhenTheyPreserveRealPlaybackOrder() {
        val songs = listOf(
            song("1"),
            song("2").copy(queueTier = QueueTier.USER_QUEUE),
            song("3").copy(queueTier = QueueTier.CONTEXT),
            song("4").copy(queueTier = QueueTier.AUTOPLAY),
        )
        assertTrue(queueSectionsPreservePlaybackOrder(songs, listOf(0, 1, 2, 3), 0))
        assertFalse(queueSectionsPreservePlaybackOrder(songs, listOf(0, 2, 1, 3), 0))
        assertFalse(queueSectionsPreservePlaybackOrder(songs, listOf(0, 1, 1, 3), 0))
    }

    @Test fun portraitNowPlayingHasNoRedundantToolsRowAndLyricsSourcesRemainAvailable() {
        start()
        val initialEntries = compose.runOnIdle { vm.state.value.queue.map { it.queueEntryId } }
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onAllNodesWithContentDescription(app.getString(R.string.player_sources))
            .assertCountEquals(0)
        compose.onNodeWithContentDescription(app.getString(R.string.player_song_actions))
            .performClick()
        compose.onNodeWithText(app.getString(R.string.player_sources)).performScrollTo()
            .assertIsEnabled().performClick()
        compose.onNodeWithTag("lyrics-sources-dialog").assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.lyrics_sources_close)).performClick()
        compose.runOnIdle {
            assertEquals(initialEntries, vm.state.value.queue.map { it.queueEntryId })
            assertEquals("1", vm.state.value.currentSong?.id)
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = "es-rES-w360dp-h720dp-mdpi", application = AurelisApplication::class)
    fun mediumPhoneWithLargeTextKeepsTransportAccessible() {
        start(fontScale = 1.5f)
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        assertPrimaryTransportInsidePlayer("player-portrait")
        compose.onNodeWithContentDescription(app.getString(R.string.player_queue)).performClick()
        compose.onNodeWithTag("player-queue-shared-deck").assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.player_next)).assertIsDisplayed()
    }

    @Test fun queueShowsSingleClearControlForUpcomingUserSongs() {
        start()
        compose.runOnIdle { vm.playNext(listOf(song("2"))) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.player_queue)).performClick()
        compose.onAllNodesWithText(app.getString(R.string.queue_clear_user)).assertCountEquals(1)
        compose.onAllNodesWithText(app.getString(R.string.clear)).assertCountEquals(0)
        compose.onNodeWithText(app.getString(R.string.queue_clear_user)).performClick()
        compose.runOnIdle { assertEquals(listOf("1", "2", "3"), vm.state.value.queue.map { it.id }) }
    }

    @Test fun queueUsesSongSleeveAndTheSameTransportDeckAsThePlayer() {
        start()
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.player_queue)).performClick()
        compose.onNodeWithTag("player-queue-collapsed-header").assertIsDisplayed()
        compose.onNodeWithTag("player-queue-shared-deck").assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.player_seek)).assertIsDisplayed()
        compose.onNodeWithTag("player-mode-pill").assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.now_playing)).assertIsDisplayed()
        compose.onNodeWithContentDescription("Cerrar").performClick()
        compose.onNodeWithTag("player-queue-collapsed-header").assertDoesNotExist()
        compose.onNodeWithTag("player-portrait").assertIsDisplayed()
    }

    @Test fun flatQueueCanClearOnlyAddedFutureOccurrencesWithoutChangingPlayback() {
        start()
        compose.runOnIdle {
            player.seekTo(12_345)
            vm.playNext(listOf(song("1"), song("3")))
        }
        compose.waitForIdle()
        val current = vm.state.value.currentSong?.queueEntryId
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_queue)).performClick()
        compose.onNodeWithText(app.getString(R.string.queue_clear_user)).performClick()
        compose.runOnIdle {
            assertEquals(listOf("1", "2", "3"), vm.state.value.queue.map { it.id })
            assertEquals(current, vm.state.value.currentSong?.queueEntryId)
            assertEquals(12_345L, vm.state.value.positionMs)
            assertFalse(player.playWhenReady)
        }
        compose.onNodeWithText(app.getString(R.string.queue_clear_user)).assertDoesNotExist()
    }

    @Test fun shuffleQueueKeepsSelectionAndDisablingItDoesNotReplacePlayback() {
        start()
        compose.runOnIdle { vm.toggleShuffle() }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_queue)).performClick()
        compose.onNodeWithText(app.getString(R.string.queue_shuffle_reorder)).assertIsDisplayed()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions) and
            hasAnyAncestor(hasTestTag("playback-queue"))).assertDoesNotExist()
        compose.onNode(hasText("Canción local 2") and hasAnyAncestor(hasTestTag("playback-queue"))).performClick()
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id); player.pause() }
        val current = vm.state.value.currentSong
        val position = vm.state.value.positionMs
        val prepares = player.prepares; val replacements = player.replacements
        compose.onNodeWithContentDescription("Cerrar").performClick()
        // BitChord shows shuffle/repeat/autoplay in the queue's center capsule.
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_queue)).performClick()
        compose.onNodeWithTag("player-mode-pill").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_shuffle)).performClick()
        compose.runOnIdle {
            assertFalse(vm.state.value.shuffleEnabled)
            assertEquals(listOf(0, 1, 2), vm.state.value.queuePlaybackOrder)
            assertEquals(current, vm.state.value.currentSong); assertEquals(position, vm.state.value.positionMs)
            assertEquals(prepares, player.prepares); assertEquals(replacements, player.replacements)
            assertFalse(player.playWhenReady)
        }
    }

    @Test fun lyricDeckAutoHidesOnlyDuringPlaybackAndReturnsOnTap() {
        start()
        importTranslationFixture()
        compose.runOnIdle { vm.resume() }
        compose.waitUntil(5000) { compose.runOnIdle { vm.state.value.isPlaying } }
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithTag("lyrics-overlay").assertIsDisplayed()
        compose.onNodeWithTag("lyrics-floating-transport").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(4_800)
        compose.onNodeWithTag("lyrics-overlay").assertIsDisplayed()
        compose.onNodeWithTag("lyrics-floating-transport").assertDoesNotExist()
        revealFullScreenLyricsControls()
        compose.onNodeWithTag("lyrics-floating-transport").assertIsDisplayed()
    }

    @Test fun playbackStartAutomaticallyOpensFullScreenLyricsAndDoesNotReopenOverQueue() {
        start()
        val entries = compose.runOnIdle { vm.state.value.queue.map { it.queueEntryId } }
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithTag("lyrics-overlay").assertDoesNotExist()
        compose.onNodeWithContentDescription(app.getString(R.string.player_play)).performClick()
        compose.waitUntil(5000) { compose.runOnIdle { vm.state.value.isPlaying } }
        compose.onNodeWithTag("lyrics-overlay").assertIsDisplayed()
        compose.onNodeWithTag("lyrics-floating-transport").assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.player_next)).performClick()
        compose.onNodeWithTag("lyrics-overlay").assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.player_queue)).performClick()
        compose.onNodeWithTag("player-queue-pane").assertIsDisplayed()
        // Once the listener chooses the queue, track changes must not steal it.
        compose.onNodeWithContentDescription(app.getString(R.string.player_previous)).performClick()
        compose.onNodeWithTag("player-queue-pane").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(entries, vm.state.value.queue.map { it.queueEntryId })
            assertEquals(LastPlayerScreen.QUEUE, SettingsRepository.lastPlayerScreen.value)
        }
    }

    @Test fun expandedPlayerAlreadyPlayingStartsOnFullScreenLyricsWithoutReplacingQueue() {
        start()
        val entries = compose.runOnIdle { vm.state.value.queue.map { it.queueEntryId } }
        compose.runOnIdle { vm.resume() }
        compose.waitUntil(5000) { compose.runOnIdle { vm.state.value.isPlaying } }
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithTag("lyrics-overlay").assertIsDisplayed()
        compose.onNodeWithTag("lyrics-floating-transport").assertIsDisplayed()
        compose.runOnIdle { assertEquals(entries, vm.state.value.queue.map { it.queueEntryId }) }
        compose.onNodeWithContentDescription(app.getString(R.string.lyrics_ui_close)).performClick()
        compose.onNodeWithTag("lyrics-overlay").assertDoesNotExist()
        compose.onNodeWithTag("player-portrait").assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.player_next)).performClick()
        compose.onNodeWithTag("lyrics-overlay").assertDoesNotExist()
    }

    @Test fun miniPlayerHorizontalSwipeChangesOneTrackAndCancelledSwipeDoesNotChangeQueue() {
        start()
        val entries = compose.runOnIdle { vm.state.value.queue.map { it.queueEntryId } }
        compose.onNodeWithTag("mini-player").performTouchInput {
            swipe(center, center + androidx.compose.ui.geometry.Offset(-225f, 0f))
        }
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id) }
        compose.onNodeWithTag("mini-player").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(-200f, 0f))
            cancel()
        }
        compose.runOnIdle {
            assertEquals("2", vm.state.value.currentSong?.id)
            assertEquals(entries, vm.state.value.queue.map { it.queueEntryId })
        }
        compose.onNodeWithTag("mini-player").performTouchInput {
            swipe(center, center + androidx.compose.ui.geometry.Offset(225f, 0f))
        }
        compose.runOnIdle { assertEquals("1", vm.state.value.currentSong?.id) }
    }

    @Test fun miniPlayerSwipeOpensAndHeaderSwipeClosesWithoutChangingTheQueue() {
        start()
        compose.onNodeWithTag("mini-player").performTouchInput {
            down(center); moveBy(androidx.compose.ui.geometry.Offset(0f, -160f)); cancel()
        }
        compose.onNodeWithTag("player-portrait").assertDoesNotExist()
        compose.onNodeWithTag("mini-player").performTouchInput {
            swipe(center, center + androidx.compose.ui.geometry.Offset(0f, -600f))
        }
        compose.onNodeWithTag("player-portrait").assertIsDisplayed()
        compose.onNodeWithTag("player-handle").performTouchInput {
            swipe(center, center + androidx.compose.ui.geometry.Offset(0f, 600f))
        }
        compose.onNodeWithTag("player-portrait").assertDoesNotExist()
        compose.runOnIdle { assertEquals(3, player.mediaItemCount); assertEquals("1", vm.state.value.currentSong?.id) }
    }

    @Test
    @Config(sdk = [33], qualifiers = "es-rES-w800dp-h360dp-land-mdpi", application = AurelisApplication::class)
    fun landscapeControlsUseTheSameQueueAndQualityIsExplicitlyInputOnly() {
        start()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithTag("player-landscape").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_next)).performClick()
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id); assertEquals(3, player.mediaItemCount) }
        // An unknown source must not be mislabelled Lossless. The pipeline
        // remains accessible from the actual Android output control.
        compose.onNodeWithTag("player-source-quality").assertDoesNotExist()
        compose.onAllNodesWithContentDescription(app.getString(R.string.audio_output))
            .onFirst().performClick()
        compose.onNodeWithTag("audio-output-pipeline").performScrollTo().performClick()
        compose.onNodeWithTag("audio-pipeline-dialog").assertIsDisplayed()
        compose.onNode(hasText("Salida del dispositivo: no medida", substring = true)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.pipeline_route_not_confirmed)).assertExists()
        compose.onNodeWithText(app.getString(R.string.pipeline_stage_decoder)).assertExists()
        compose.onNodeWithText(app.getString(R.string.pipeline_stage_processing)).assertExists()
        for (index in 0..4) {
            compose.onNodeWithTag("audio-pipeline-stage-$index").assertExists()
        }
        compose.onNodeWithText("Cerrar").performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_close)).performClick()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [33], qualifiers = "es-rES-w800dp-h360dp-land-mdpi", application = AurelisApplication::class)
    fun landscapeQueueAndLyricsUseSidePanesAndTheSameController() {
        start()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        val thirdEntry = vm.state.value.queue[2].queueEntryId
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_queue)).performClick()
        compose.onNodeWithTag("landscape-queue").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_close)).assertIsDisplayed()
        compose.onNode(hasText("Canción local 2") and hasAnyAncestor(hasTestTag("playback-queue"))).performClick()
        compose.onNode(hasContentDescription(app.getString(R.string.remove_from_queue)) and
            hasAnyAncestor(hasTestTag("queue-entry-$thirdEntry")), useUnmergedTree = true)
            .performScrollTo().performClick()
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id); assertEquals(listOf("1", "2"), vm.state.value.queue.map { it.id }) }
        val prepares = player.prepares
        val replacements = player.replacements
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_lyrics)).performClick()
        compose.onNodeWithTag("landscape-lyrics").assertIsDisplayed()
        compose.onNodeWithTag("landscape-queue").assertDoesNotExist()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_close)).assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("player-landscape").assertIsDisplayed()
        compose.onNodeWithTag("landscape-lyrics").assertDoesNotExist()
        compose.runOnIdle { assertEquals(prepares, player.prepares); assertEquals(replacements, player.replacements) }
    }

    @Test
    @Config(sdk = [33], qualifiers = "es-rES-w800dp-h360dp-land-mdpi", application = AurelisApplication::class)
    fun landscapeImportedLyricsSeekWithTheExistingOffsetWithoutReplacingTheQueue() {
        start()
        val previousOffset = SettingsRepository.lyricsOffsetMs.value
        val previousSync = SettingsRepository.syncedLyrics.value
        try {
            compose.runOnIdle {
                SettingsRepository.setLyricsOffsetMs(500)
                SettingsRepository.setSyncedLyrics(true)
            }
            val lrc = File(app.cacheDir, "landscape-lyrics.lrc").apply {
                writeText("[00:00.000]Primera frase horizontal\n[00:01.000]Segunda frase horizontal")
            }
            compose.runOnIdle { vm.importLyrics(android.net.Uri.fromFile(lrc)) }
            compose.waitUntil(5000) { compose.runOnIdle { vm.state.value.lyrics !is LyricsState.Loading } }
            compose.runOnIdle { assertTrue(vm.state.value.lyrics is LyricsState.Loaded) }
            val queue = vm.state.value.queue.map { it.id to it.queueEntryId }
            val prepares = player.prepares
            val replacements = player.replacements
            compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
            compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_lyrics)).performClick()
            compose.onNodeWithTag("landscape-lyrics").assertIsDisplayed()
            compose.runOnIdle { assertTrue("Media3 must offer real seeking", vm.state.value.canSeek) }
            val seeksBeforeTap = player.seeks
            // In landscape the lyric transport is a floating bottom overlay (360dp high
            // window: it starts around y=236dp). The next line sits under it, so a
            // coordinate tap would hit the play/pause button instead of the lyric.
            // Invoke the line's own click action; seek behaviour is what is under test.
            compose.onNodeWithTag("lyrics-line-1").performScrollTo().assertIsEnabled()
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitUntil(5_000) {
                compose.runOnIdle { player.seeks > seeksBeforeTap }
            }
            compose.waitUntil(5_000) {
                compose.runOnIdle { vm.state.value.positionMs == 1500L }
            }
            compose.runOnIdle {
                assertEquals(1500L, vm.state.value.positionMs)
                assertEquals(queue, vm.state.value.queue.map { it.id to it.queueEntryId })
                assertEquals(prepares, player.prepares); assertEquals(replacements, player.replacements)
            }
            compose.onNodeWithContentDescription("Cerrar letras").performClick()
            compose.onNodeWithTag("landscape-lyrics").assertDoesNotExist()
            compose.onNodeWithTag("player-landscape").assertIsDisplayed()
        } finally { compose.runOnIdle {
            SettingsRepository.setLyricsOffsetMs(previousOffset)
            SettingsRepository.setSyncedLyrics(previousSync)
        } }
    }

    @Test fun changingSongsNeverRevealsTheAlreadyOpenPlayerAgain() {
        start()
        compose.runOnIdle { SettingsRepository.setReduceAnimation(false) }
        try {
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_open)).performClick()
            compose.mainClock.advanceTimeBy(2_000)
            val initial = compose.onNodeWithTag("player-portrait").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val entries = compose.runOnIdle { vm.state.value.queue.map { it.queueEntryId } }
            compose.mainClock.autoAdvance = false
            compose.runOnIdle { vm.next() }
            repeat(12) {
                compose.mainClock.advanceTimeByFrame()
                val bounds = compose.onNodeWithTag("player-portrait").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertEquals(initial.top, bounds.top, 1f)
                assertEquals(initial.bottom, bounds.bottom, 1f)
            }
            compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id); assertEquals(entries, vm.state.value.queue.map { it.queueEntryId }) }
        } finally {
            compose.mainClock.autoAdvance = true
            compose.runOnIdle { SettingsRepository.setReduceAnimation(true) }
        }
    }

    @Test fun cancellingAPartialHeaderDragPreservesTheOpenPlayerAndQueue() {
        start()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithTag("player-handle").performTouchInput {
            down(center); moveBy(androidx.compose.ui.geometry.Offset(0f, 120f)); cancel()
        }
        compose.onNodeWithTag("player-portrait").assertIsDisplayed()
        compose.runOnIdle { assertEquals(3, player.mediaItemCount); assertEquals("1", vm.state.value.currentSong?.id) }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_close)).performClick()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
    }

    @Test fun editingTheSingleProfilePreservesPlayerNavigationAndCollections() {
        start()
        compose.runOnIdle { app.userLibrary.toggleFavorite("1") }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_close)).assertIsDisplayed()
        compose.runOnIdle { ProfileRepository.rename("Nombre nuevo") }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_close)).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(setOf("1"), app.userLibrary.state.value.favorites)
            assertEquals("1", vm.state.value.currentSong?.id)
            assertEquals(3, player.mediaItemCount)
        }
    }

    @Test fun playerScrubberClampsSeekingAndKeepsTheSameQueue() {
        start()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_open)).performClick()
        val queueBefore = compose.runOnIdle { vm.state.value.queue.map { it.queueEntryId } }
        val seek = compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_seek))
        seek.assertIsDisplayed().assertIsEnabled()
        seek.performSemanticsAction(SemanticsActions.SetProgress) { it(-2f) }
        compose.runOnIdle { assertEquals(0L, vm.state.value.positionMs) }
        seek.performSemanticsAction(SemanticsActions.SetProgress) { it(1.25f) }
        compose.runOnIdle {
            assertEquals(60_000L, vm.state.value.positionMs)
            assertEquals(queueBefore, vm.state.value.queue.map { it.queueEntryId })
        }
    }

    @Test fun miniPlayerOpensRealControlsAndQueueActionsStaySynchronized() {
        start()
        val previousEntry = vm.state.value.queue.first().queueEntryId
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_close)).assertIsDisplayed()
        // Main player presents output in the center; queue presents playback modes.
        compose.onNodeWithTag("player-mode-pill").assertDoesNotExist()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_queue)).performClick()
        compose.onNodeWithTag("player-mode-pill").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_shuffle)).performClick()
        compose.runOnIdle { assertTrue(player.shuffleModeEnabled) }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_repeat)).performClick()
        compose.runOnIdle { assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode) }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_queue)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_seek))
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(0.5f) }
        compose.runOnIdle { assertEquals(30_000, vm.state.value.positionMs) }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_queue)).performClick()
        compose.onNodeWithText("Canción local 2").performClick()
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id); player.pause() }
        val playingEntry = vm.state.value.currentSong!!.queueEntryId
        val pausedPosition = vm.state.value.positionMs
        // History is hidden by default; the first remove button belongs to song3.
        compose.onNodeWithTag("queue-entry-$previousEntry").assertDoesNotExist()
        compose.runOnIdle { SettingsRepository.setShowQueueHistory(true) }
        compose.onNode(hasScrollToIndexAction() and hasAnyAncestor(hasTestTag("playback-queue")))
            .performScrollToIndex(0)
        compose.onNodeWithTag("queue-entry-$previousEntry").performScrollTo().assertIsDisplayed()
        compose.onNode(hasContentDescription(app.getString(R.string.remove_from_queue)) and
            hasAnyAncestor(hasTestTag("queue-entry-$previousEntry")), useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(listOf("2", "3"), vm.state.value.queue.map { it.id }) }
        compose.runOnIdle {
            assertEquals(playingEntry, vm.state.value.currentSong?.queueEntryId)
            assertEquals(0, vm.state.value.currentIndex)
            assertEquals(pausedPosition, vm.state.value.positionMs)
            assertFalse(player.playWhenReady)
        }
        compose.onNodeWithContentDescription("Cerrar").performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_close)).performClick()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
        compose.onNodeWithTag("mini-player").assertTextContains("Canción local 2")
    }
    @Test fun playerHasUsableArtworkTimeControlsAndCompactBarAt360dp() {
        start()
        capture("mini-player")
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_lyrics)).assertIsDisplayed()
        compose.onNodeWithTag("player-mode-pill").assertDoesNotExist()
        compose.onNodeWithText("0:00").assertIsDisplayed()
        compose.onNodeWithText("-1:00").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_next)).assertIsDisplayed()
        capture("now-playing")
    }
    @Test
    @Config(qualifiers = "es-rES-w640dp-h360dp-mdpi")
    fun landscapeKeepsTransportVisibleWithoutScrolling() {
        start()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_queue)).assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_next)).performClick()
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id) }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_close)).performClick()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
    }
    @Test fun favoriteStarReflectsRealLocalStateAndNeverReplacesTheQueue() {
        start()
        val entries = compose.runOnIdle { vm.state.value.queue.map { it.queueEntryId } }
        val replacements = compose.runOnIdle { player.replacements }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_add_favorite))
            .assertIsOff().performClick()
        compose.runOnIdle { assertEquals(setOf("1"), app.userLibrary.state.value.favorites) }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_remove_favorite))
            .assertIsOn().performClick()
        compose.runOnIdle {
            assertTrue(app.userLibrary.state.value.favorites.isEmpty())
            assertEquals(entries, vm.state.value.queue.map { it.queueEntryId })
            assertEquals(replacements, player.replacements)
        }
    }

    @Test fun songMenuOpensOnlyResolvedLocalAlbumWithoutRebuildingPlayback() {
        start()
        val originalQueue = compose.runOnIdle { vm.state.value.queue.map { it.queueEntryId } }
        val originals = compose.runOnIdle { player.replacements }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_song_actions)).performClick()
        compose.onNodeWithText("Abrir álbum").performScrollTo().performClick()
        compose.onNodeWithTag("collection-tracks").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("1", vm.state.value.currentSong?.id)
            assertEquals(originalQueue, vm.state.value.queue.map { it.queueEntryId })
            assertEquals(originals, player.replacements)
        }
    }

    @Test fun audioOutputOpensOwnDrawerWithoutClaimingAnUnconfirmedDevice() {
        start()
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onAllNodesWithContentDescription(app.getString(R.string.audio_output)).onFirst().performClick()
        compose.onNodeWithTag("audio-output-sheet").assertIsDisplayed()
        compose.onNodeWithTag("audio-output-system-picker").assertIsDisplayed()
        // This is a real bottom sheet with a vertically scrolling content area.
        // On small phones the volume and pipeline rows start below the fold.
        // PlayerDrawer's content column is clickable, so it merges the descendants that
        // are not clickable themselves; the volume wrapper only exists unmerged.
        compose.onNodeWithTag("audio-output-volume", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("audio-output-pipeline").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("audio-output-sheet").assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.audio_pipeline)).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("1", vm.state.value.currentSong?.id)
            assertFalse(player.playWhenReady)
        }
    }

    @Test fun songMenuInfoAndTimerUseNestedPagesWithoutChangingPlayback() {
        start()
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.player_song_actions)).performClick()
        compose.onNodeWithText("Información de la canción").performScrollTo().performClick()
        compose.onAllNodesWithText("Canción local 1").onLast().assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        // RecordingPlayer is not a MediaSession controller. A timer command
        // cannot be sent to this test double, and must not be shown as usable.
        compose.onNodeWithText("Temporizador").assertDoesNotExist()
        compose.onNodeWithText("Desplazamiento de letras").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0L, vm.state.value.sleepRemainingMs) }
    }

    @Test fun songActionsPersistFavoritePlaylistAndAppendWithoutRestartingQueue() {
        start()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_song_actions)).performClick()
        compose.onNodeWithText("Añadir a favoritos").performClick()
        compose.runOnIdle { assertEquals(setOf("1"), app.userLibrary.state.value.favorites) }
        compose.onNodeWithText("Nueva playlist").assertDoesNotExist()
        compose.onNodeWithText("Añadir a playlist").performClick()
        compose.onNodeWithText("Nueva playlist").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("Mi lista")
        compose.onNodeWithText("Crear playlist con esta canción").performClick()
        compose.runOnIdle { assertEquals(listOf("1"), app.userLibrary.state.value.playlists.single().songIds) }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_song_actions)).performClick()
        compose.onNodeWithText("Añadir a la cola").performClick()
        compose.runOnIdle { assertEquals(listOf("1", "2", "3", "1"), vm.state.value.queue.map { it.id }) }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_queue)).performClick()
        val playingEntry = vm.state.value.currentSong!!.queueEntryId
        val actions = compose.onNodeWithTag("queue-entry-$playingEntry")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle {
            assertTrue(actions.single { it.label == "Bajar en la cola" }.action())
        }
        compose.runOnIdle {
            assertEquals(listOf("2", "1", "3", "1"), vm.state.value.queue.map { it.id })
            assertEquals("1", vm.state.value.currentSong?.id); assertEquals(1, vm.state.value.currentIndex)
        }
    }
    @Test fun replayShowsActualHistoryAndRequiresConfirmationToClearIt() {
        val store = app.userLibrary
        store.recordListening(song("1"), 5000, true); store.flush()
        store.toggleFavorite("1"); store.createPlaylist("Conservar", listOf("1"))
        compose.setContent { AurelisTheme { com.music.aurelis.ui.screens.ReplayScreen(
            store = store, songs = listOf(song("1")), onPlay = {}) } }
        compose.onNodeWithText(compose.activity.getString(R.string.replay_plays_total, 1L)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(5000L, store.state.value.listening.getValue("1").listenedMs) }
        compose.onNodeWithText("Borrar historial").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(1, store.state.value.listening.size) }
        compose.onNodeWithText("Borrar historial").performClick()
        compose.onAllNodesWithText("Borrar historial").onLast().performClick()
        compose.runOnIdle {
            assertTrue(store.state.value.listening.isEmpty())
            assertEquals(setOf("1"), store.state.value.favorites)
            assertEquals(1, store.state.value.playlists.size)
        }
    }
    @Test fun importedLyricsHighlightAndSeekWithOffsetInTheRealPlayer() {
        start()
        val previousOffset = SettingsRepository.lyricsOffsetMs.value
        try {
            compose.runOnIdle { SettingsRepository.setLyricsOffsetMs(500) }
            val lrc = File(app.cacheDir, "import-test.lrc").apply { writeText("[00:00.000]Primera frase propia\n[00:01.000]Segunda frase propia") }
            compose.runOnIdle { vm.importLyrics(android.net.Uri.fromFile(lrc)) }
            compose.waitUntil(5000) { compose.runOnIdle { vm.state.value.lyrics !is LyricsState.Loading } }
            compose.runOnIdle { assertTrue("Expected imported lyrics, got ${vm.state.value.lyrics}", vm.state.value.lyrics is LyricsState.Loaded) }
            val originalQueue = vm.state.value.queue.map { it.id to it.queueEntryId }
            val playingSong = vm.state.value.currentSong
            val prepares = player.prepares
            val replacements = player.replacements
            compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
            compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_lyrics)).performClick()
            // BitChord opens lyrics with the transport and source shortcut visible.
            compose.onNodeWithTag("lyrics-overlay").assertIsDisplayed()
            compose.onNode(hasText("Archivo local") and hasAnyAncestor(hasTestTag("lyrics-overlay"))).assertIsDisplayed()
            compose.onNodeWithText("Primera frase propia").assertIsSelected()
            val verseBounds = compose.onNodeWithTag("lyrics-line-1").fetchSemanticsNode().boundsInRoot
            val deckBounds = compose.onNodeWithTag("lyrics-floating-transport").fetchSemanticsNode().boundsInRoot
            assertTrue("The tap-to-seek verse must not be covered by the floating scrubber",
                verseBounds.bottom < deckBounds.top)
            compose.onNodeWithText("Segunda frase propia").performClick()
            // Positive delays lyrics: media1500 - offset500 = lyric1000.
            compose.runOnIdle { assertEquals(1500L, vm.state.value.positionMs) }
            compose.onNodeWithText("Segunda frase propia").assertIsSelected()
            val positionBeforeSharing = vm.state.value.positionMs
            compose.onNodeWithTag("lyrics-line-0").performScrollTo().performTouchInput { longClick() }
            compose.onNodeWithTag("lyrics-line-0").assertIsSelected()
            compose.onNodeWithTag("lyrics-line-1").performClick().assertIsSelected()
            compose.runOnIdle { assertEquals(positionBeforeSharing, vm.state.value.positionMs) }
            compose.onNodeWithText(compose.activity.getString(R.string.share)).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.lyrics_share_title)).assertIsDisplayed()
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithText("Segunda frase propia").assertIsSelected()
            compose.runOnIdle { assertEquals(positionBeforeSharing, vm.state.value.positionMs) }
            capture("synced-lyrics")
            compose.runOnIdle { SettingsRepository.setLyricsOffsetMs(-500) }
            compose.onNodeWithText("Segunda frase propia").performClick()
            // Negative advances lyrics: media500 - offset(-500) = lyric1000.
            compose.runOnIdle { assertEquals(500L, vm.state.value.positionMs) }
            compose.onNodeWithText("Segunda frase propia").assertIsSelected()
            compose.runOnIdle {
                assertEquals(originalQueue, vm.state.value.queue.map { it.id to it.queueEntryId })
                assertEquals(playingSong, vm.state.value.currentSong)
                assertEquals(prepares, player.prepares)
                assertEquals(replacements, player.replacements)
                assertFalse(player.playWhenReady)
            }
            compose.runOnIdle { vm.next() }
            compose.runOnIdle { vm.importLyrics(android.net.Uri.fromFile(lrc), "1") }
            compose.runOnIdle { assertFalse(vm.state.value.lyrics is LyricsState.Loaded) }
        } finally {
            compose.runOnIdle { SettingsRepository.setLyricsOffsetMs(previousOffset) }
        }
    }
    @Test fun restoredLyricsPaneShowsTransportAndSurvivesTrackChange() {
        start(playerScreen = LastPlayerScreen.LYRICS)
        val before = compose.runOnIdle { vm.state.value.queue.map { it.queueEntryId } }
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithTag("lyrics-overlay").assertIsDisplayed()
        compose.onNodeWithTag("lyrics-floating-transport").assertIsDisplayed()
        compose.onNodeWithTag("player-queue-pane").assertDoesNotExist()
        compose.runOnIdle { assertEquals(LastPlayerScreen.LYRICS, SettingsRepository.lastPlayerScreen.value) }
        compose.onNodeWithContentDescription(app.getString(R.string.player_next)).performClick()
        compose.onNodeWithTag("lyrics-overlay").assertIsDisplayed()
        compose.onNodeWithTag("lyrics-floating-transport").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("2", vm.state.value.currentSong?.id)
            assertEquals(before, vm.state.value.queue.map { it.queueEntryId })
            assertEquals(LastPlayerScreen.LYRICS, SettingsRepository.lastPlayerScreen.value)
        }
    }

    @Test fun restoredQueuePaneAndBackPersistMainWithoutChangingPlayback() {
        start(playerScreen = LastPlayerScreen.QUEUE)
        val before = compose.runOnIdle { vm.state.value.queue.map { it.queueEntryId } }
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithTag("player-queue-pane").assertIsDisplayed()
        compose.onNodeWithTag("player-mode-pill").assertIsDisplayed()
        compose.runOnIdle { assertEquals(LastPlayerScreen.QUEUE, SettingsRepository.lastPlayerScreen.value) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("player-queue-pane").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(LastPlayerScreen.MAIN, SettingsRepository.lastPlayerScreen.value)
            assertEquals(before, vm.state.value.queue.map { it.queueEntryId })
        }
    }

    @Test fun lyricsReturnToFullReadingPaneWithoutChangingPlaybackState() {
        start(); importTranslationFixture()
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.player_lyrics)).performClick()
        compose.onNodeWithTag("lyrics-overlay").assertIsDisplayed()
        val queueBefore = compose.runOnIdle { vm.state.value.queue.map { it.queueEntryId } }
        val trackBefore = compose.runOnIdle { vm.state.value.currentSong?.queueEntryId }
        val positionBefore = compose.runOnIdle { vm.state.value.positionMs }
        compose.onNodeWithTag("lyrics-list").performTouchInput { swipeUp() }
        compose.onNodeWithTag("lyrics-list").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(trackBefore, vm.state.value.currentSong?.queueEntryId)
            assertEquals(positionBefore, vm.state.value.positionMs)
            assertEquals(queueBefore, vm.state.value.queue.map { it.queueEntryId })
        }
    }

    @Test fun fullScreenLyricsNeverShrinkTheirViewportWhenTransportIsRevealed() {
        start(); importTranslationFixture()
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.player_lyrics)).performClick()
        compose.onNodeWithTag("lyrics-overlay").assertIsDisplayed()
        val before = compose.onNodeWithTag("lyrics-list").fetchSemanticsNode().boundsInRoot
        // Match BitChord: opening lyrics reveals a collapsed sleeve and transport.
        compose.onNodeWithTag("lyrics-floating-transport").assertIsDisplayed()
        compose.onNodeWithTag("lyrics-deck-mask").assertIsDisplayed()
        revealFullScreenLyricsControls()
        compose.onNodeWithTag("lyrics-floating-transport").assertIsDisplayed()
        compose.onNodeWithTag("lyrics-deck-mask").assertIsDisplayed()
        val after = compose.onNodeWithTag("lyrics-list").fetchSemanticsNode().boundsInRoot
        assertEquals("Showing controls must not shrink the lyric viewport", before.height, after.height, 1f)
        assertEquals(before.top, after.top, 1f)
        compose.onNodeWithContentDescription(app.getString(R.string.player_seek)).assertIsDisplayed()
    }

    @Test fun translationDialogCancelNeverSendsAndBilingualViewKeepsOriginal() {
        start(); importTranslationFixture()
        var requests = 0
        vm.translateRequest = { _, original, _ -> requests++
            com.music.aurelis.data.lyrics.LyricsTranslation.Result.Translated(original.mapIndexed { index, line -> line.copy(text = "Traducción de prueba $index") }, "en", false) }
        SettingsRepository.setTranslationService("https://translation.example", "en")
        SettingsRepository.setTranslationLanguage("es")
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_lyrics)).performClick()
        // BitChord opens the lyric sleeve with the controls available.
        compose.onNodeWithContentDescription("Traducir").assertIsDisplayed()
        compose.onNodeWithContentDescription("Traducir").assertIsDisplayed().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(0, requests) }
        compose.onNodeWithContentDescription("Traducir").performClick()
        compose.onNodeWithText("Traducir").performClick()
        compose.onNodeWithText("Source line A").assertIsDisplayed()
        compose.onNodeWithText("Traducción de prueba 0").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, requests) }
        capture("bilingual-lyrics")
        compose.onNodeWithText("Ocultar traducción").performClick()
        compose.onNodeWithText("Traducción de prueba 0").assertDoesNotExist()
        compose.onNodeWithText("Source line A").assertIsDisplayed()
    }
    @Test fun aLateTranslationCannotOverwriteLyricsOfAnotherTrack() {
        start(); importTranslationFixture()
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        vm.translateRequest = { _, lines, _ ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { gate.await() }
            com.music.aurelis.data.lyrics.LyricsTranslation.Result.Translated(lines.map { it.copy(text = "Late fixture") }, "en", false)
        }
        try {
            compose.runOnIdle { vm.translateLyrics("es") }
            compose.runOnIdle { vm.next() }
            importTranslationFixture()
            compose.runOnIdle { gate.complete(Unit) }
            compose.runOnIdle {
                assertEquals("2", vm.state.value.currentSong?.id)
                val loaded = vm.state.value.lyrics as LyricsState.Loaded
                assertEquals(TranslationState.None, loaded.translation)
                assertEquals("Source line A", loaded.lines.first().text)
            }
        } finally { gate.complete(Unit) }
    }
    @Test fun lyricsSourceSelectionPersistsWithoutSendingAnyRequest() {
        start()
        var requests = 0
        vm.remoteLyricsRequest = { requests++; error("Selecting a provider must not send metadata") }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        // Providers remain reachable via ••• after removal of the duplicate
        // playback toolbar; changing a switch must not trigger any request.
        compose.onNodeWithContentDescription(app.getString(R.string.player_song_actions)).performClick()
        compose.onNodeWithText(app.getString(R.string.player_sources)).performScrollTo()
            .assertIsEnabled().performClick()
        compose.onNodeWithTag("lyrics-provider-LRCLIB").performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithTag("lyrics-provider-BETTER_LYRICS-disabled").assertExists()
        compose.onNodeWithTag("lyrics-provider-BINI_LYRICS-disabled").assertExists()
        compose.onNodeWithTag("lyrics-provider-LRC_RED-disabled").assertExists()
        capture("lyrics-source-registry")
        compose.onNodeWithText("Cerrar").performClick()
        compose.runOnIdle { SettingsRepository.init(app); assertEquals(setOf(com.music.aurelis.data.lyrics.LyricsSource.LRCLIB), SettingsRepository.lyricsSources.value); assertEquals(0, requests) }
        // Reopen through the same user-visible entry point after persisting.
        compose.onNodeWithContentDescription(app.getString(R.string.player_song_actions)).performClick()
        compose.onNodeWithText(app.getString(R.string.player_sources)).performScrollTo()
            .assertIsEnabled().performClick()
        compose.onNodeWithTag("lyrics-provider-LRCLIB").performScrollTo().assertIsOn()
        compose.onNodeWithText("Cerrar").performClick()
    }
    @Test fun disabledOnlineSearchKeepsImportedLyricsAndDoesNotSendMetadata() {
        start(); importTranslationFixture()
        var requests = 0
        vm.remoteLyricsRequest = { requests++; error("Disabled lookup must not send metadata") }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_lyrics)).performClick()
        val lookupLabel = compose.activity.getString(R.string.lyrics_search_lrclib)
        compose.onNodeWithText(lookupLabel).assertDoesNotExist()
        // The tools shortcut is part of the initially visible BitChord-style deck.
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.lyrics_ui_tools))
            .assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(0, requests) }
        compose.onNodeWithText(lookupLabel).assertIsDisplayed().performClick()
        compose.onNodeWithText("Source line A").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, requests)
            assertTrue(vm.state.value.lyricsLookup is LyricsLookupState.Failed)
            assertFalse(player.playWhenReady)
        }
    }
    private fun importTranslationFixture() {
        val file = File(app.cacheDir, "translation-source.lrc").apply { writeText("[00:00.000]Source line A\n[00:01.000]Source line B") }
        compose.runOnIdle { vm.importLyrics(android.net.Uri.fromFile(file)) }
        compose.waitUntil(5000) { compose.runOnIdle { vm.state.value.lyrics !is LyricsState.Loading } }
        compose.runOnIdle { assertTrue(vm.state.value.lyrics is LyricsState.Loaded) }
    }
    @Test fun homeAutoMixKeepsCurrentItemAndPositionWhileReplacingUpcomingSongs() {
        start()
        val original = compose.runOnIdle { player.seekTo(12_345); player.shuffleModeEnabled = true; player.currentMediaItem }
        // Top Picks precedes the hero shelf. Reach the actual AutoMix card in
        // both scroll axes before injecting a click; its old coordinates may
        // now belong to a recommendation or the floating MiniPlayer.
        compose.onNodeWithTag("local-home").performScrollToKey("hero")
        val autoMix = hasText(compose.activity.getString(R.string.home_automix))
        compose.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("local-home")) and
            hasAnyDescendant(hasTestTag("home-leading-hero"))).performScrollToNode(autoMix)
        compose.onNode(autoMix).assertIsDisplayed().assertIsEnabled().performClick()
        compose.waitUntil(5000) { compose.runOnIdle {
            !vm.state.value.isCreatingMix && !player.shuffleModeEnabled
        } }
        compose.runOnIdle {
            assertNull(vm.state.value.mixError)
            assertSame(original, player.currentMediaItem)
            assertEquals("1", vm.state.value.currentSong?.id)
            assertEquals(12_345L, vm.state.value.positionMs)
            assertEquals(setOf("1", "2", "3"), vm.state.value.queue.map { it.id }.toSet())
            assertFalse(player.shuffleModeEnabled); assertTrue(player.playWhenReady)
        }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_queue)).performClick()
        compose.onNodeWithText("Canción local 2").assertIsDisplayed()
        capture("automix-queue")
    }
    @Test fun emptyAutoMixShowsAnErrorAndDoesNotClearTheCurrentQueue() {
        start()
        compose.runOnIdle { vm.startAutoMix(emptyList(), app.userLibrary.state.value) }
        compose.waitUntil(5000) { compose.runOnIdle { !vm.state.value.isCreatingMix } }
        // The error is a lazy item below the hero, so it need not be composed
        // until the list is explicitly scrolled to its stable key.
        compose.onNodeWithTag("local-home").performScrollToKey("mix-error")
        compose.onNodeWithText("No hay canciones locales disponibles para crear una mezcla.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf("1", "2", "3"), vm.state.value.queue.map { it.id }) }
    }
    private fun capture(name: String) {
        val bitmap = compose.runOnIdle {
            val view = compose.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        val file = File("build/reports/b07-visual/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
    @Test fun menuStartsLocalRadioWithRealLibraryKeepingCurrentTrackAndPosition() {
        start()
        val original = compose.runOnIdle { player.currentMediaItem }
        compose.runOnIdle { player.seekTo(12_345) }
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.player_song_actions)).performClick()
        compose.onNodeWithText("Iniciar radio local").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(5000) {
            compose.runOnIdle { !vm.state.value.isCreatingMix && vm.state.value.queue.isNotEmpty() }
        }
        compose.runOnIdle {
            assertNull(vm.state.value.mixError)
            assertSame(original, player.currentMediaItem)
            assertEquals("1", vm.state.value.currentSong?.id)
            assertEquals(12_345L, vm.state.value.positionMs)
            assertEquals(setOf("1", "2", "3"), vm.state.value.queue.map { it.id }.toSet())
        }
    }

    @Test fun menuMixOverviewReportsActualLocalQueueWithoutClaimingBpm() {
        start()
        compose.onNodeWithContentDescription(app.getString(R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.player_song_actions)).performClick()
        compose.onNodeWithText("Resumen de AutoMix").performScrollTo().performClick()
        compose.onNodeWithText("Recomendaciones de música local").assertIsDisplayed()
        compose.onNodeWithText("Sin análisis de mezcla en curso").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Copiar diagnóstico").performScrollTo().assertIsEnabled()
    }

    @Test fun songMenuPlayNextInsertsADuplicateAndKeepsThePausedPlayerAndQueueInSync() {
        start()
        compose.runOnIdle { player.seekTo(12_345) }
        val originalEntry = vm.state.value.currentSong!!.queueEntryId
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_song_actions)).performClick()
        compose.onNodeWithText("Reproducir después").performScrollTo().performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf("1", "1", "2", "3"), vm.state.value.queue.map { it.id })
            assertEquals(originalEntry, vm.state.value.currentSong?.queueEntryId)
            assertEquals(12_345L, vm.state.value.positionMs)
            assertFalse(player.playWhenReady)
        }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_queue)).performClick()
        compose.onNodeWithTag("playback-queue").assertIsDisplayed()
        // Lazy queue rows are virtualized; validate the real occurrence instead.
        val duplicateEntry = vm.state.value.queue[1].queueEntryId
        compose.onNodeWithTag("queue-entry-$duplicateEntry").performScrollTo().assertIsDisplayed()
        compose.onNode(hasContentDescription(app.getString(R.string.remove_from_queue)) and
            hasAnyAncestor(hasTestTag("queue-entry-$duplicateEntry")), useUnmergedTree = true).performClick()
        compose.runOnIdle {
            assertEquals(listOf("1", "2", "3"), vm.state.value.queue.map { it.id })
            assertEquals(originalEntry, vm.state.value.currentSong?.queueEntryId)
            assertEquals(12_345L, vm.state.value.positionMs)
        }
    }

    @Test fun draggingAQueueOccurrenceUsesTheExistingPlayerWithoutSeekingOrRestartingIt() {
        start()
        compose.runOnIdle { player.seekTo(12_345) }
        val current = vm.state.value.currentSong!!.queueEntryId
        val dragged = vm.state.value.queue[1].queueEntryId
        val target = vm.state.value.queue[2].queueEntryId
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_queue)).performClick()
        compose.onNodeWithTag("queue-drag-$dragged", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("queue-drag-$target", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        val from = compose.onNodeWithTag("queue-drag-$dragged", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.center
        val to = compose.onNodeWithTag("queue-drag-$target", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.center
        val queueBounds = compose.onNodeWithTag("playback-queue").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("playback-queue").performTouchInput {
            swipe(from - queueBounds.topLeft, to - queueBounds.topLeft, durationMillis = 600)
        }
        compose.runOnIdle {
            assertEquals(listOf("1", "3", "2"), vm.state.value.queue.map { it.id })
            assertEquals(current, vm.state.value.currentSong?.queueEntryId)
            assertEquals(12_345L, vm.state.value.positionMs)
            assertFalse(player.playWhenReady)
            assertEquals(1, player.replacements)
        }
    }

    @Test fun shuffleExplainsWhyPlayNextIsUnavailableAndAppendRemainsAvailable() {
        start()
        compose.runOnIdle { player.seekTo(12_345); player.shuffleModeEnabled = true }
        val current = vm.state.value.currentSong?.queueEntryId
        val before = Triple(player.replacements, player.prepares, player.seeks)
        compose.runOnIdle { assertFalse(vm.state.value.canEditShuffleQueue) }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_song_actions)).performClick()
        compose.onNodeWithText("Reproducir después").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Desactiva el modo aleatorio para elegir la siguiente canción.").assertExists()
        compose.onNodeWithText("Añadir a la cola").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertTrue(player.shuffleModeEnabled); assertEquals(4, player.mediaItemCount)
            assertEquals(current, vm.state.value.currentSong?.queueEntryId)
            assertEquals(12_345L, vm.state.value.positionMs)
            assertFalse(player.playWhenReady)
            assertEquals(before, Triple(player.replacements, player.prepares, player.seeks))
            assertNotEquals(current, player.getMediaItemAt(3).queueEntryId)
            assertNull(vm.state.value.errorMessage)
        }
    }

}
