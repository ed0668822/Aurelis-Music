// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.theme

import android.view.MotionEvent
import android.view.View
import android.os.VibrationAttributes
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.LocalUserLibrary
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.Haptics
import com.music.aurelis.ui.haptics.rememberHaptics
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowVibrator
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

    private fun withHapticDevice(block: (Vibrator, ShadowVibrator) -> Unit) {
        val context = RuntimeEnvironment.getApplication()
        val vibrator = checkNotNull(context.getSystemService(VibratorManager::class.java)).defaultVibrator
        val shadow = Shadows.shadowOf(vibrator)
        val original = Settings.System.getString(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED)
        val originalHardware = vibrator.hasVibrator()
        ShadowVibrator.reset()
        Settings.System.putInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
        try {
            block(vibrator, shadow)
        } finally {
            vibrator.cancel()
            ShadowVibrator.reset()
            shadow.setHasVibrator(originalHardware)
            Settings.System.putString(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, original)
        }
    }

    @Test fun hapticsFollowAndroidSettingWithoutRecreatingTheRememberedHandle() = withHapticDevice { _, shadow ->
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        lateinit var handle: Haptics
        compose.setContent { handle = rememberHaptics() }
        compose.runOnIdle {
            Settings.System.putInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
            Haptic.entries.forEach(handle::play)
            assertNull(shadow.vibrationAttributesFromLastVibration)
            Settings.System.putInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
            handle.play(Haptic.Tap)
            val attributes = shadow.vibrationAttributesFromLastVibration as VibrationAttributes
            assertEquals(VibrationAttributes.USAGE_TOUCH, attributes.usage)
            ShadowVibrator.reset()
            Settings.System.putInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
            handle.play(Haptic.Select)
            assertNull(shadow.vibrationAttributesFromLastVibration)
        }
    }

    @Test fun hapticsSkipMissingMotorWithoutBlockingTheCaller() = withHapticDevice { vibrator, shadow ->
        Haptic.entries.forEach(Haptics(null)::play)
        shadow.setHasVibrator(false)
        Haptic.entries.forEach(Haptics(vibrator)::play)
        assertNull(shadow.vibrationAttributesFromLastVibration)
    }

    @Test fun switchFeedbackBelongsOnlyToItsEnabledUserAction() = withHapticDevice { _, shadow ->
        var value by mutableStateOf(false)
        var changes = 0
        compose.setContent { AurelisTheme {
            Column {
                IosSwitch(value, { value = it; changes++ }, Modifier.testTag("haptic-switch"))
                IosSwitch(value, null, Modifier.testTag("drawing-only-switch"))
                IosSwitch(value, { changes++ }, Modifier.testTag("disabled-haptic-switch"), enabled = false)
            }
        } }
        compose.runOnIdle { assertNull(shadow.vibrationAttributesFromLastVibration); value = true }
        compose.waitForIdle()
        compose.runOnIdle { assertNull(shadow.vibrationAttributesFromLastVibration) }
        compose.onNodeWithTag("disabled-haptic-switch").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithTag("drawing-only-switch").performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, changes); assertNull(shadow.vibrationAttributesFromLastVibration) }
        compose.onNodeWithTag("haptic-switch").performClick().assertIsOff()
        compose.runOnIdle { assertEquals(1, changes); assertNotNull(shadow.vibrationAttributesFromLastVibration) }
    }

    @Test fun settingsRowOwnsOneAccessibleToggleAndDisabledRowIsInert() = withHapticDevice { _, shadow ->
        var checked by mutableStateOf(false)
        var changes = 0
        compose.setContent { AurelisTheme {
            Column {
                SettingsSubRow("Acción única", checked, { checked = it; changes++ }, Modifier.testTag("haptic-row"))
                SettingsSubRow("Deshabilitada", false, { changes++ }, Modifier.testTag("disabled-haptic-row"), enabled = false)
            }
        } }
        compose.onAllNodes(isToggleable()).assertCountEquals(2)
        compose.onNodeWithTag("haptic-row").assertHeightIsAtLeast(48.dp).assertIsOff().performClick().assertIsOn()
        compose.runOnIdle {
            assertEquals(1, changes); assertNotNull(shadow.vibrationAttributesFromLastVibration)
            ShadowVibrator.reset(); checked = false
        }
        compose.waitForIdle()
        compose.runOnIdle { assertNull(shadow.vibrationAttributesFromLastVibration) }
        compose.onNodeWithTag("disabled-haptic-row").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, changes); assertNull(shadow.vibrationAttributesFromLastVibration) }
    }

    @Test fun menuFollowsTheHeldRowAndRespectsLogicalTrailingEdge() {
        val anchor = IntRect(80, 100, 300, 148)
        val window = IntSize(360, 800)
        val panel = IntSize(200, 240)
        assertEquals(IntOffset(100, 156), heldMenuPosition(anchor, window, panel, LayoutDirection.Ltr, 16, 8))
        assertEquals(IntOffset(80, 156), heldMenuPosition(anchor, window, panel, LayoutDirection.Rtl, 16, 8))
    }

    @Test fun menuFlipsAboveBottomRowAndClampsBothWindowEdges() {
        val window = IntSize(360, 800)
        val panel = IntSize(200, 240)
        assertEquals(IntOffset(140, 482), heldMenuPosition(
            IntRect(240, 730, 340, 778), window, panel, LayoutDirection.Ltr, 16, 8))
        assertEquals(IntOffset(16, 20), heldMenuPosition(
            IntRect(0, 0, 12, 12), window, panel, LayoutDirection.Ltr, 16, 8))
        assertEquals(IntOffset(144, 156), heldMenuPosition(
            IntRect(350, 100, 360, 148), window, panel, LayoutDirection.Rtl, 16, 8))
    }

    @Test fun missingOrOffscreenAnchorKeepsCenteredFallbackInSmallWindows() {
        val window = IntSize(360, 800)
        val panel = IntSize(200, 240)
        for (anchor in listOf(null, IntRect(0, 0, 0, 0), IntRect(0, 900, 100, 948))) {
            assertEquals(IntOffset(80, 280), heldMenuPosition(anchor, window, panel, LayoutDirection.Ltr, 16, 8))
        }
        assertEquals(IntOffset.Zero, heldMenuPosition(
            null, IntSize(100, 120), IntSize(120, 150), LayoutDirection.Ltr, 16, 8))
        assertEquals(IntOffset(16, 84), heldMenuPosition(
            IntRect(40, 170, 140, 210), IntSize(360, 400), IntSize(200, 300), LayoutDirection.Ltr, 16, 8))
    }

    @Test fun openerIsConsumedOnceAndCannotLeakAcrossHostsOrDetachedRows() {
        val first = HeldMenuState()
        val second = HeldMenuState()
        val opener = HeldMenuAnchor().also { it.attached = true }
        first.pendingAnchor = opener
        assertNull(second.takeAnchor())
        assertSame(opener, first.takeAnchor())
        assertNull(first.takeAnchor())
        opener.attached = false
        first.pendingAnchor = opener
        assertNull(first.takeAnchor())
        assertNull(first.pendingAnchor)
    }

    @Test fun dismissingHeldMenuRestoresFocusWithoutPlayingOrKeepingStaleAnchor() {
        var plays = 0
        lateinit var menus: HeldMenuState
        lateinit var inputMode: InputModeManager
        compose.setContent { AurelisTheme {
            menus = checkNotNull(LocalHeldMenuState.current)
            inputMode = LocalInputModeManager.current
            var open by remember { mutableStateOf(false) }
            Column {
                Box(Modifier.testTag("held-opener").size(48.dp)
                    .heldContextMenuInteractions({ plays++ }, { open = true })) {
                    androidx.compose.material3.Text("Origen")
                }
                if (open) HeldContextMenu({ open = false }) {
                    androidx.compose.material3.TextButton({ open = false }) {
                        androidx.compose.material3.Text("Cerrar anclado")
                    }
                }
            }
        } }
        compose.onNodeWithTag("held-opener").performTouchInput { longClick() }
        compose.onNode(isPopup()).assertExists()
        compose.runOnIdle {
            assertNull(menus.pendingAnchor); assertEquals(0, plays)
            assertEquals(InputMode.Touch, inputMode.inputMode)
        }
        compose.onNodeWithText("Cerrar anclado").performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.onNodeWithTag("held-opener").assertIsFocused()
        compose.runOnIdle {
            assertNull(menus.restoreAnchor); assertEquals(0, plays)
            assertEquals(InputMode.Touch, inputMode.inputMode)
        }
    }

    @Test fun removingTheOpenerBeforeDismissalDoesNotRequestFocusOnDetachedRow() {
        lateinit var menus: HeldMenuState
        compose.setContent { AurelisTheme {
            menus = checkNotNull(LocalHeldMenuState.current)
            var open by remember { mutableStateOf(false) }
            var visible by remember { mutableStateOf(true) }
            Column {
                if (visible) Box(Modifier.testTag("removed-opener").size(48.dp)
                    .heldContextMenuInteractions({}, { open = true })) {
                    androidx.compose.material3.Text("Fila temporal")
                }
                if (open) HeldContextMenu({ open = false }) {
                    androidx.compose.material3.TextButton({ visible = false }) {
                        androidx.compose.material3.Text("Retirar origen")
                    }
                    androidx.compose.material3.TextButton({ open = false }) {
                        androidx.compose.material3.Text("Cerrar sin origen")
                    }
                }
            }
        } }
        compose.onNodeWithTag("removed-opener").performTouchInput { longClick() }
        compose.onNodeWithText("Retirar origen").performClick()
        compose.onNodeWithTag("removed-opener").assertDoesNotExist()
        compose.onNodeWithText("Cerrar sin origen").performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertNull(menus.restoreAnchor); assertNull(menus.pendingAnchor) }
    }

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
            var open by remember { mutableStateOf(false) }
            Column {
                Box(Modifier.testTag("back-opener").size(48.dp)
                    .heldContextMenuInteractions({}, { open = true })) {
                    androidx.compose.material3.Text("Abrir acciones")
                }
                if (open) IosMenu({ open = false; dismissals++ }) { androidx.compose.material3.Text("Acciones locales") }
            }
        } }
        compose.onNodeWithTag("back-opener").performTouchInput { longClick() }
        compose.onNode(isPopup()).assertExists()
        compose.runOnIdle {
            val global = ReflectionHelpers.callStaticMethod<Any>(Class.forName("android.view.WindowManagerGlobal"), "getInstance")
            val views = ReflectionHelpers.getField<List<View>>(global, "mViews")
            val popup = views.last { it.javaClass.simpleName == "PopupLayout" }
            popup.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_BACK))
            popup.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_BACK))
        }
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.onNodeWithTag("back-opener").assertIsFocused()
        compose.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test fun outsideEventDismissesTheActualPopup() {
        var dismissals = 0
        compose.setContent { AurelisTheme {
            var open by remember { mutableStateOf(false) }
            Column {
                Box(Modifier.testTag("outside-opener").size(48.dp)
                    .heldContextMenuInteractions({}, { open = true })) {
                    androidx.compose.material3.Text("Abrir acciones")
                }
                if (open) IosMenu({ open = false; dismissals++ }) { androidx.compose.material3.Text("Acciones locales") }
            }
        } }
        compose.onNodeWithTag("outside-opener").performTouchInput { longClick() }
        compose.onNode(isPopup()).assertExists()
        compose.runOnIdle {
            val global = ReflectionHelpers.callStaticMethod<Any>(Class.forName("android.view.WindowManagerGlobal"), "getInstance")
            val views = ReflectionHelpers.getField<List<View>>(global, "mViews")
            val popup = views.last { it.javaClass.simpleName == "PopupLayout" }
            val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_OUTSIDE, -1f, -1f, 0)
            try { popup.dispatchTouchEvent(event) } finally { event.recycle() }
        }
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.onNodeWithTag("outside-opener").assertIsFocused()
        compose.runOnIdle { assertEquals(1, dismissals) }
    }
}
