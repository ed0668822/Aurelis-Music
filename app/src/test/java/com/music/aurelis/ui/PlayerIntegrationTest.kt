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
    @After fun cleanup() { compose.runOnIdle { store.clear() } }
    private fun start(fontScale: Float? = null) {
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

    @Test fun playerHeaderHandlesMissingBlankAndNamedAlbumsAndKeepsTheCorrectSong() {
        start()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.player_open)).performClick()
        for (album in listOf(null, "", "   ", "Álbum 100% local")) {
            compose.runOnIdle { vm.playSongs(listOf(song("1").copy(album = album))) }
            val expected = album?.takeIf { it.isNotBlank() } ?: compose.activity.getString(R.string.local_music)
            compose.onNodeWithText(compose.activity.getString(R.string.player_playing_album, expected)).assertIsDisplayed()
            compose.runOnIdle { assertEquals("1", vm.state.value.currentSong?.id) }
        }
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
        compose.onNodeWithText("Álbumes").performClick()
        compose.onNode(songResult).assertDoesNotExist()
        compose.onNodeWithTag("mini-player").assertTextContains("Canción local 1")
        compose.runOnIdle { assertEquals("1", vm.state.value.currentSong?.id) }
        compose.onNodeWithText("Todo").performClick()
        compose.onNode(songResult).assertExists()
        compose.onNodeWithText("Álbumes").performClick()
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
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_tools)).performClick()
        compose.onNodeWithText("El servicio conectado no ofrece este control.").assertExists()
        compose.onNodeWithText("1.5×").assertIsNotEnabled()
        compose.onNodeWithText("Activar salto de silencio").assertIsNotEnabled()
        compose.onNodeWithText("15 min").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Al terminar esta canción").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Cancelar temporizador").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Cerrar").performClick()
        compose.runOnIdle { assertEquals(current, vm.state.value.currentSong?.queueEntryId); assertFalse(player.playWhenReady) }
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
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_shuffle)).performClick()
        compose.runOnIdle {
            assertFalse(vm.state.value.shuffleEnabled)
            assertEquals(listOf(0, 1, 2), vm.state.value.queuePlaybackOrder)
            assertEquals(current, vm.state.value.currentSong); assertEquals(position, vm.state.value.positionMs)
            assertEquals(prepares, player.prepares); assertEquals(replacements, player.replacements)
            assertFalse(player.playWhenReady)
        }
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
        compose.onNodeWithText("Formato desconocido").performClick()
        compose.onNode(hasText("Salida del dispositivo: no medida", substring = true)).assertIsDisplayed()
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
        try {
            compose.runOnIdle { SettingsRepository.setLyricsOffsetMs(500) }
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
            compose.onNodeWithText("Segunda frase horizontal").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(1500L, vm.state.value.positionMs)
                assertEquals(queue, vm.state.value.queue.map { it.id to it.queueEntryId })
                assertEquals(prepares, player.prepares); assertEquals(replacements, player.replacements)
            }
            compose.onNodeWithContentDescription("Cerrar letras").performClick()
            compose.onNodeWithTag("landscape-lyrics").assertDoesNotExist()
            compose.onNodeWithTag("player-landscape").assertIsDisplayed()
        } finally { compose.runOnIdle { SettingsRepository.setLyricsOffsetMs(previousOffset) } }
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

    @Test fun miniPlayerOpensRealControlsAndQueueActionsStaySynchronized() {
        start()
        val previousEntry = vm.state.value.queue.first().queueEntryId
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_close)).assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_shuffle)).performClick()
        compose.runOnIdle { assertTrue(player.shuffleModeEnabled) }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_repeat)).performClick()
        compose.runOnIdle { assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode) }
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
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_repeat)).assertIsDisplayed()
        compose.onNodeWithText("1:00").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_next)).assertIsDisplayed()
        capture("now-playing")
    }
    @Test
    @Config(qualifiers = "es-rES-w640dp-h360dp-mdpi")
    fun landscapeKeepsTransportVisibleWithoutScrolling() {
        start()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_repeat)).assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_next)).performClick()
        compose.runOnIdle { assertEquals("2", vm.state.value.currentSong?.id) }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_close)).performClick()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
    }
    @Test fun songActionsPersistFavoritePlaylistAndAppendWithoutRestartingQueue() {
        start()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_song_actions)).performClick()
        compose.onNodeWithText("Añadir a favoritos").performClick()
        compose.runOnIdle { assertEquals(setOf("1"), app.userLibrary.state.value.favorites) }
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
            compose.onNode(hasText("Archivo local") and hasAnyAncestor(hasTestTag("lyrics-overlay"))).assertIsDisplayed()
            compose.onNodeWithText("Primera frase propia").assertIsSelected()
            compose.onNodeWithText("Segunda frase propia").performClick()
            // Positive delays lyrics: media1500 - offset500 = lyric1000.
            compose.runOnIdle { assertEquals(1500L, vm.state.value.positionMs) }
            compose.onNodeWithText("Segunda frase propia").assertIsSelected()
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
    @Test fun translationDialogCancelNeverSendsAndBilingualViewKeepsOriginal() {
        start(); importTranslationFixture()
        var requests = 0
        vm.translateRequest = { _, original, _ -> requests++
            com.music.aurelis.data.lyrics.LyricsTranslation.Result.Translated(original.mapIndexed { index, line -> line.copy(text = "Traducción de prueba $index") }, "en", false) }
        SettingsRepository.setTranslationService("https://translation.example", "en")
        SettingsRepository.setTranslationLanguage("es")
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_lyrics)).performClick()
        compose.onNodeWithContentDescription("Traducir").performClick()
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
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_sources)).performClick()
        compose.onNodeWithTag("lyrics-provider-LRCLIB").performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithTag("lyrics-provider-BETTER_LYRICS-disabled").assertExists()
        compose.onNodeWithTag("lyrics-provider-BINI_LYRICS-disabled").assertExists()
        compose.onNodeWithTag("lyrics-provider-LRC_RED-disabled").assertExists()
        capture("lyrics-source-registry")
        compose.onNodeWithText("Cerrar").performClick()
        compose.runOnIdle { SettingsRepository.init(app); assertEquals(setOf(com.music.aurelis.data.lyrics.LyricsSource.LRCLIB), SettingsRepository.lyricsSources.value); assertEquals(0, requests) }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_sources)).performClick()
        compose.onNodeWithTag("lyrics-provider-LRCLIB").performScrollTo().assertIsOn()
        compose.onNodeWithText("Cerrar").performClick()
    }
    @Test fun disabledOnlineSearchKeepsImportedLyricsAndDoesNotSendMetadata() {
        start(); importTranslationFixture()
        var requests = 0
        vm.remoteLyricsRequest = { requests++; error("Disabled lookup must not send metadata") }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_open)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_lyrics)).performClick()
        compose.onNodeWithText("Buscar en LRCLIB").performClick()
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
        compose.onNodeWithText("AutoMix · Selección local").performClick()
        compose.waitUntil(5000) { compose.runOnIdle { !vm.state.value.isCreatingMix } }
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
        compose.onAllNodesWithContentDescription(app.getString(R.string.drag_to_reorder)).assertCountEquals(3)
        val duplicateEntry = vm.state.value.queue[1].queueEntryId
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
