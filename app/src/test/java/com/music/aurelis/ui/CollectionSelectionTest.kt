// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.navigation.AppRoute
import com.music.aurelis.ui.navigation.LocalCollectionKind
import com.music.aurelis.ui.screens.LocalCollectionScreen
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CollectionSelectionTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val rows = listOf("a" to "Árbol", "c" to "Cielo", "z" to "Zorro").map { (id, title) ->
        LocalSong(id, title, "Artista", "Álbum", "content://media/external/audio/media/$id",
            null, 60000, null, 1, 2026)
    }
    private var library: LibraryState by mutableStateOf(LibraryState.Success(rows))
    private var played = emptyList<String>()
    private var startIndex = -1

    private fun start(): StateRestorationTester {
        rows.forEach { if (it.id !in app.userLibrary.state.value.favorites) app.userLibrary.toggleFavorite(it.id) }
        val restoration = StateRestorationTester(compose)
        restoration.setContent { AurelisTheme {
            LocalCollectionScreen(AppRoute.Collection(LocalCollectionKind.FAVORITES), library,
                app.userLibrary, null, false, onBack = {}, onOpen = {},
                onPlay = { songs, index -> played = songs.map { it.id }; startIndex = index })
        } }
        return restoration
    }

    private fun playSelection() {
        // The first action plays the whole collection; the second plays only marked rows.
        val buttons = compose.onAllNodes(hasText("Reproducir") and hasClickAction())
        buttons.assertCountEquals(2)
        buttons[1].performClick()
    }

    @Test fun bulkPlaybackAndPlaylistAppendUseVisibleOrderWithoutReorderingSavedTracks() {
        val target = app.userLibrary.createPlaylist("Destino", listOf("c"))
        start()
        compose.onNodeWithText("Título Z–A").performClick()
        compose.onNodeWithText("Seleccionar").performClick()
        compose.onNodeWithContentDescription("Seleccionar Árbol").performClick()
        compose.onNodeWithContentDescription("Seleccionar Zorro").performClick()
        playSelection()
        compose.runOnIdle { assertEquals(listOf("z", "a"), played); assertEquals(0, startIndex) }
        compose.onNodeWithText("Añadir selección a playlist").performClick()
        compose.onNodeWithText("Destino").performClick()
        compose.runOnIdle {
            assertEquals(listOf("c", "z", "a"), app.userLibrary.state.value.playlists.single { it.id == target }.songIds)
            assertEquals(setOf("a", "c", "z"), app.userLibrary.state.value.favorites)
        }
        compose.onNodeWithText("Orden original").performClick()
        playSelection()
        compose.runOnIdle {
            assertEquals(listOf("a", "z"), played)
            assertEquals(listOf("c", "z", "a"), app.userLibrary.state.value.playlists.single { it.id == target }.songIds)
        }
    }

    @Test fun markedIdsAndVisibleSortSurviveRecreationAndCancelClearsThem() {
        val restoration = start()
        compose.onNodeWithText("Título Z–A").performClick()
        compose.onNodeWithText("Seleccionar").performClick()
        compose.onNodeWithContentDescription("Seleccionar Árbol").performClick()
        compose.onNodeWithContentDescription("Seleccionar Zorro").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Seleccionar Árbol").assertIsOn()
        compose.onNodeWithContentDescription("Seleccionar Cielo").assertIsOff()
        compose.onNodeWithContentDescription("Seleccionar Zorro").assertIsOn()
        compose.onNodeWithText("Orden original").assertExists()
        playSelection()
        compose.runOnIdle { assertEquals(listOf("z", "a"), played) }
        compose.onNodeWithText("Cancelar selección").performClick()
        compose.onNodeWithText("Seleccionar").performClick()
        rows.forEach { compose.onNodeWithContentDescription("Seleccionar ${it.title}").assertIsOff() }
    }

    @Test fun restoredSelectionResolvesOnlyAvailableMediaAndDisablesEmptyActions() {
        val restoration = start()
        compose.onNodeWithText("Seleccionar").performClick()
        compose.onNodeWithText("Todas").performClick()
        compose.runOnIdle { library = LibraryState.Success(rows.filter { it.id == "c" }) }
        restoration.emulateSavedInstanceStateRestore()
        playSelection()
        compose.runOnIdle { assertEquals(listOf("c"), played) }
        compose.runOnIdle { library = LibraryState.Success(emptyList()) }
        compose.onAllNodes(hasText("Reproducir") and hasClickAction())[1].assertIsNotEnabled()
        compose.onNodeWithText("Añadir selección a playlist").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(setOf("a", "c", "z"), app.userLibrary.state.value.favorites) }
    }
}
