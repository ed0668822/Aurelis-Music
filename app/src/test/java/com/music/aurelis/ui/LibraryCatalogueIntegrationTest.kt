package com.music.aurelis.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.music.aurelis.*
import com.music.aurelis.data.LocalLibrarySource
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.ui.navigation.*
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LibraryCatalogueIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private fun song(id: String, title: String, artist: String) = LocalSong(id, title, artist, "Disco local",
        "content://media/external/audio/media/$id", null, 60_000, null, 1, 2026,
        albumId = "14", artistId = if (id == "1") "11" else "22")

    @Test fun librarySearchFiltersArtistGroupsAndRestoresMediaIdRoutes() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val rows = listOf(song("1", "Árbol", "Artista Uno"), song("2", "Mar", "Artista Dos"))
        val library = LibraryViewModel(app, LocalLibrarySource { rows })
        val player = PlaybackViewModel(app)
        lateinit var navigator: AppNavigator
        val restoration = StateRestorationTester(compose)
        restoration.setContent { AurelisTheme {
            navigator = rememberAppNavigator()
            AurelisNavigationContent(navigator, player, library)
        } }
        compose.runOnIdle { library.loadLibrary() }
        compose.onNode(isSelectable() and hasText("Biblioteca")).performClick()
        compose.onNodeWithText("Canciones").performClick()
        compose.onNodeWithContentDescription("Buscar").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("arbol")
        compose.onNode(isSelectable() and hasText("Álbumes")).performClick()
        compose.onNodeWithText("Disco local").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextReplacement("zzzz")
        compose.onNodeWithText("Sin resultados").assertIsDisplayed()
        compose.onNode(isSelectable() and hasText("Artistas")).performClick()
        compose.onNodeWithText("Sin resultados").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextReplacement("arbol")
        compose.onNode(isSelectable() and hasText("Artistas")).performClick()
        compose.onNodeWithText("Artista Dos").assertDoesNotExist()
        compose.onNodeWithText("Artista Uno").performClick()
        compose.runOnIdle { assertEquals(AppRoute.Collection(LocalCollectionKind.ARTIST, "media:11"), navigator.visibleRoute()) }
        compose.onNodeWithText("Árbol").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Árbol").assertIsDisplayed()
    }

    @Test fun removedAndMovedMediaUpdatesTheVisiblePlaylistWithoutDeletingPrivateReferences() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val one = song("1", "Árbol", "Artista Uno")
        val two = song("2", "Mar", "Artista Dos")
        var rows = listOf(one, two)
        val playlistId = app.userLibrary.createPlaylist("Guardada", listOf("1", "2"))
        app.userLibrary.toggleFavorite("1")
        val library = LibraryViewModel(app, LocalLibrarySource { rows })
        val player = PlaybackViewModel(app)
        lateinit var navigator: AppNavigator
        compose.setContent { AurelisTheme {
            navigator = rememberAppNavigator()
            AurelisNavigationContent(navigator, player, library)
        } }
        compose.runOnIdle {
            library.loadLibrary()
            navigator.push(AppRoute.Collection(LocalCollectionKind.PLAYLIST, playlistId))
        }
        compose.onNodeWithText("Árbol").assertIsDisplayed()
        compose.runOnIdle { rows = listOf(two.copy(title = "Movida", filePath = "/Music/new.mp3")); library.refresh() }
        compose.onNodeWithText("Árbol").assertDoesNotExist()
        compose.onNodeWithText("Movida").assertIsDisplayed()
        compose.onNodeWithText("Algunas canciones ya no están disponibles en el dispositivo.").assertIsDisplayed()
        compose.runOnIdle { rows = emptyList(); library.refresh() }
        compose.onNodeWithText("No hay canciones disponibles en esta colección.").assertIsDisplayed()
        compose.onNodeWithText("Reproducir").assertIsNotEnabled()
        compose.runOnIdle {
            val saved = LocalUserLibrary(app).state.value
            assertEquals(setOf("1"), saved.favorites)
            assertEquals(listOf("1", "2"), saved.playlists.single().songIds)
        }
    }
}
