package com.music.aurelis.ui.theme

import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SharedGesturesTest {
    @get:Rule(order = 0) val registerActivity = com.music.aurelis.ui.RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val song = LocalSong("shared-1", "Canción de prueba", "Artista local", null,
        "content://media/external/audio/media/1", null, 180000, null, null, null)

    @Test fun iosSwitchKeepsTouchTargetDisabledStateAndSingleAction() {
        var changes = 0
        compose.setContent { AurelisTheme {
            var checked by remember { mutableStateOf(false) }
            Column {
                IosSwitch(checked, { checked = it; changes++ }, Modifier.testTag("switch"))
                IosSwitch(false, { changes++ }, Modifier.testTag("disabled"), enabled = false)
            }
        } }
        compose.onNodeWithTag("switch").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .assertIsOff().performClick().assertIsOn()
        compose.onNodeWithTag("disabled").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, changes) }
    }

    @Test fun tapPlaysOnceHoldOpensRealActionsAndEnqueueDismisses() {
        val store = LocalUserLibrary(RuntimeEnvironment.getApplication())
        var played = 0; var enqueued = 0
        compose.setContent { AurelisTheme {
            var open by remember { mutableStateOf(false) }
            SongRow(song, { played++ }, { open = true }, false, false)
            if (open) SongActionsDialog(song, store, { enqueued++ }, { open = false }, onPlayNext = {})
        } }
        compose.onNodeWithText(song.title).performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, played) }
        compose.onNodeWithText(song.title).performTouchInput { longClick() }
        compose.onNodeWithText("Añadir a favoritos").performClick()
        compose.runOnIdle {
            assertEquals(1, played)
            assertTrue(song.id in LocalUserLibrary(RuntimeEnvironment.getApplication()).state.value.favorites)
        }
        compose.onNodeWithText("Añadir a la cola").performScrollTo().performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, enqueued); assertEquals(1, played) }
    }

    @Test fun playNextDispatchesOnceAndDismissesTheRealMenu() {
        val store = LocalUserLibrary(RuntimeEnvironment.getApplication())
        var next = 0; var enqueued = 0
        compose.setContent { AurelisTheme {
            var open by remember { mutableStateOf(true) }
            if (open) SongActionsDialog(song, store, { enqueued++ }, { open = false }, onPlayNext = { next++ })
        } }
        compose.onNodeWithText("Reproducir después").performScrollTo().performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, next); assertEquals(0, enqueued) }
    }

    @Test fun disconnectedQueueActionsAreDisabledWithoutDisablingLocalFavorites() {
        val store = LocalUserLibrary(RuntimeEnvironment.getApplication())
        compose.setContent { AurelisTheme {
            SongActionsDialog(song, store, {}, {}, onPlayNext = {}, queueActionsEnabled = false)
        } }
        compose.onNodeWithText("Reproducir después").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Añadir a la cola").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Añadir a favoritos").performScrollTo().assertIsEnabled()
    }

    @Test fun menuCreatesPersistentPlaylistAndDismisses() {
        val store = LocalUserLibrary(RuntimeEnvironment.getApplication())
        compose.setContent { AurelisTheme {
            var open by remember { mutableStateOf(true) }
            if (open) SongActionsDialog(song, store, {}, { open = false }, onPlayNext = {})
        } }
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("Local compartida")
        compose.onNodeWithText("Crear playlist con esta canción").performScrollTo().performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle {
            val playlist = LocalUserLibrary(RuntimeEnvironment.getApplication()).state.value.playlists.single()
            assertEquals("Local compartida", playlist.name); assertEquals(listOf(song.id), playlist.songIds)
        }
    }

    @Test fun closeButtonDismissesAndLargeTextRemainsScrollable() {
        compose.setContent { AurelisTheme(darkTheme = true) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, 1.5f)) {
                var open by remember { mutableStateOf(true) }
                if (open) SongActionsDialog(song, LocalUserLibrary(RuntimeEnvironment.getApplication()), {}, { open = false }, onPlayNext = {})
            }
        } }
        compose.onNodeWithText("Cerrar").performScrollTo().assertIsDisplayed().performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
    }

    @Test fun backKeyDismissesTheActualPopup() {
        var dismissals = 0
        compose.setContent { AurelisTheme {
            var open by remember { mutableStateOf(true) }
            if (open) IosMenu({ open = false; dismissals++ }) { androidx.compose.material3.Text("Acciones locales") }
        } }
        compose.onNode(isPopup()).assertExists()
        compose.runOnIdle {
            val global = ReflectionHelpers.callStaticMethod<Any>(Class.forName("android.view.WindowManagerGlobal"), "getInstance")
            val views = ReflectionHelpers.getField<List<View>>(global, "mViews")
            val popup = views.last { it.javaClass.simpleName == "PopupLayout" }
            popup.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_BACK))
            popup.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_BACK))
        }
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test fun outsideEventDismissesTheActualPopup() {
        var dismissals = 0
        compose.setContent { AurelisTheme {
            var open by remember { mutableStateOf(true) }
            if (open) IosMenu({ open = false; dismissals++ }) { androidx.compose.material3.Text("Acciones locales") }
        } }
        compose.onNode(isPopup()).assertExists()
        compose.runOnIdle {
            val global = ReflectionHelpers.callStaticMethod<Any>(Class.forName("android.view.WindowManagerGlobal"), "getInstance")
            val views = ReflectionHelpers.getField<List<View>>(global, "mViews")
            val popup = views.last { it.javaClass.simpleName == "PopupLayout" }
            val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_OUTSIDE, -1f, -1f, 0)
            try { popup.dispatchTouchEvent(event) } finally { event.recycle() }
        }
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, dismissals) }
    }
}
