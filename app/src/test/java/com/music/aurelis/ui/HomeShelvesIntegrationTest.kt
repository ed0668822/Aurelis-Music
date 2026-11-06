package com.music.aurelis.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.*
import com.music.aurelis.data.*
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.*
import com.music.aurelis.ui.navigation.*
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeShelvesIntegrationTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val rows = (1..6).map { id -> LocalSong("$id", "Tema $id", "Artista $id", "Álbum",
        "content://media/external/audio/media/$id", null, 60000, null, id, 2026) }
    private val models = ViewModelStore()
    private lateinit var recording: RecordingPlayer
    private lateinit var navigator: AppNavigator
    @After fun cleanup() { compose.runOnIdle { models.clear() } }
    private fun start() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val library = LibraryViewModel(app, LocalLibrarySource { rows })
        recording = RecordingPlayer()
        val player = PlaybackViewModel(app) { recording }
        models.put("library", library);models.put("player", player)
        compose.setContent { AurelisTheme {
            navigator = rememberAppNavigator()
            AurelisNavigationContent(navigator, player, library)
        } }
        compose.runOnIdle { library.loadLibrary() }
    }
    @Test fun favoriteCardUsesFavoriteQueueAndHoldDoesNotReplaceIt() {
        app.userLibrary.toggleFavorite("3");app.userLibrary.toggleFavorite("4")
        start()
        compose.onNodeWithTag("local-home").performScrollToNode(hasTestTag("home-favorites"))
        val favorite = hasText("Tema 4") and hasAnyAncestor(hasTestTag("home-favorites"))
        compose.onNode(favorite).performClick()
        compose.runOnIdle {
            assertEquals(listOf("3", "4"), (0 until recording.mediaItemCount).map { recording.getMediaItemAt(it).mediaId })
            assertEquals("4", recording.currentMediaItem?.mediaId)
        }
        compose.onNode(favorite).performTouchInput { longClick() }
        compose.onNodeWithText("Quitar de favoritos").assertIsDisplayed()
        compose.onNodeWithText("Cerrar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("4", recording.currentMediaItem?.mediaId); assertEquals(2, recording.mediaItemCount) }
    }
    @Test fun playlistTileOpensThePersistedPlaylistAndExcludesUnavailableSongs() {
        val id = app.userLibrary.createPlaylist("Mi colección", listOf("2", "missing"))
        start()
        compose.onNodeWithTag("local-home").performScrollToNode(hasTestTag("home-playlists"))
        compose.onNode(hasText("1 canción disponible") and hasAnyAncestor(hasTestTag("home-playlists"))).assertExists()
        compose.onNode(hasText("Mi colección") and hasAnyAncestor(hasTestTag("home-playlists"))).performClick()
        compose.runOnIdle { assertEquals(AppRoute.Collection(LocalCollectionKind.PLAYLIST, id), navigator.visibleRoute()) }
        compose.onNodeWithText("Tema 2").assertIsDisplayed()
        compose.onNodeWithText("Tema 1").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("2", "missing"), app.userLibrary.state.value.playlists.single().songIds) }
    }
    private fun directHome(recent: List<LocalSong> = emptyList(), recommendations: List<LocalSong> = emptyList(),
        onQueue: (List<LocalSong>, Int) -> Unit, onHold: (LocalSong) -> Unit) {
        compose.setContent { AurelisTheme {
            com.music.aurelis.ui.screens.HomeScreen(recent, emptyList(), rows, emptyList(), emptyList(),
                androidx.compose.foundation.lazy.rememberLazyListState(), {}, onHold, {}, {}, null, false,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                libraryState = LibraryState.Success(rows), recommendations = recommendations, onPlayShelf = onQueue)
        } }
    }
    @Test fun heroShelfUsesItsRealSongsAndHoldDoesNotPlayAgain() {
        val recommended = rows.takeLast(2)
        var queue = emptyList<LocalSong>(); var index = -1; var plays = 0; var held = ""
        directHome(recommendations = recommended, onQueue = { q, i -> queue = q; index = i; plays++ }, onHold = { held = it.id })
        compose.onNodeWithTag("local-home").performScrollToNode(hasTestTag("home-recommendations"))
        val card = hasText("Tema 5") and hasAnyAncestor(hasTestTag("home-recommendations"))
        compose.onNode(card).performClick()
        compose.onNode(card).performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(recommended, queue);assertEquals(0,index);assertEquals(1,plays);assertEquals("5",held) }
    }
    @Test fun recentSecondPageKeepsItsGlobalQueueIndexAndMoreOnlyOpensActions() {
        var index = -1;var plays = 0;var held = ""
        directHome(recent = rows, onQueue = { q, i -> assertEquals(rows,q); index = i; plays++ }, onHold = { held = it.id })
        compose.onNodeWithTag("local-home").performScrollToNode(hasTestTag("home-recents"))
        compose.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("home-recents")))
            .performScrollToNode(hasText("Tema 6"))
        compose.onNode(hasText("Tema 6") and hasAnyAncestor(hasTestTag("home-recents"))).performClick()
        compose.onNodeWithContentDescription("Acciones de Tema 6").performClick()
        compose.runOnIdle { assertEquals(5,index);assertEquals(1,plays);assertEquals("6",held) }
    }

}
