package com.music.aurelis.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.music.aurelis.*
import com.music.aurelis.data.*
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
class LocalCollectionsIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun realHomeAlbumActionsCreateFavoritesAndPersistentPlaylistMembership() {
        val app = RuntimeEnvironment.getApplication() as AurelisApplication
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val song = LocalSong("1", "Fixture song", "Fixture artist", "Fixture album",
            "content://media/external/audio/media/1", null, 60_000, null, 1, 2026, 1)
        val library = LibraryViewModel(app, LocalLibrarySource { listOf(song) })
        val playback = PlaybackViewModel(app)
        compose.setContent { AurelisTheme {
            AurelisNavigationContent(rememberAppNavigator(), playback, library)
        } }
        compose.runOnIdle { library.loadLibrary() }
        compose.onNode(hasText("Playlists") and hasClickAction()).performClick()
        compose.onNodeWithText("Crear playlist").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Mi playlist")
        compose.onNodeWithText("Guardar").performClick()
        compose.runOnIdle { assertEquals("Mi playlist", app.userLibrary.state.value.playlists.single().name) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("local-home").performScrollToNode(hasText("Fixture album"))
        compose.onNodeWithText("Fixture album").performClick()
        compose.onNodeWithContentDescription("Cambiar favorito: Fixture song").performClick()
        compose.onNodeWithContentDescription("Añadir a playlist: Fixture song").performClick()
        compose.onNodeWithText("Mi playlist").performClick()
        compose.runOnIdle {
            val saved = LocalUserLibrary(app).state.value
            assertEquals(setOf("1"), saved.favorites)
            assertEquals(listOf("1"), saved.playlists.single().songIds)
        }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("local-home").performScrollToIndex(0)
        compose.onNode(hasText("Playlists") and hasClickAction()).performClick()
        compose.onNodeWithText("Mi playlist").performClick()
        compose.onNodeWithText("Fixture song").assertIsDisplayed()
        compose.onNodeWithText("Eliminar playlist").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(1, app.userLibrary.state.value.playlists.size) }
        compose.onNodeWithText("Eliminar playlist").performClick()
        compose.onNode(hasText("Eliminar playlist") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle {
            assertTrue(app.userLibrary.state.value.playlists.isEmpty())
            assertEquals(setOf("1"), app.userLibrary.state.value.favorites)
        }
    }
}
