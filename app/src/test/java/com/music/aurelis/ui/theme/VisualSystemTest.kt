package com.music.aurelis.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.R
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.ui.components.*
import java.io.File
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
class VisualSystemTest {
    @get:org.junit.Rule(order = 0) val registerActivity = com.music.aurelis.ui.RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun savedThemeAndMotionPreferencesUpdateTheComposedRoot() {
        var background: androidx.compose.ui.graphics.Color? = null
        var reduced = false
        var barsMoving = true
        compose.setContent {
            AurelisTheme {
                val color = MaterialTheme.colorScheme.background
                val motion = LocalReduceMotion.current
                val bars = LocalPlayingBarsMotion.current
                SideEffect { background = color; reduced = motion; barsMoving = bars }
                Text("Aurelis")
            }
        }
        compose.runOnIdle { SettingsRepository.setThemeMode(2) }
        compose.runOnIdle { assertEquals(DarkColorScheme.background, background) }
        compose.runOnIdle {
            SettingsRepository.setThemeMode(1)
            SettingsRepository.setReduceAnimation(true)
        }
        compose.runOnIdle {
            assertEquals(LightColorScheme.background, background)
            assertTrue(reduced)
            assertFalse(barsMoving)
        }
    }

    @Test fun sharedControlsExposeSelectionAndDoNotDuplicateSwitchActions() {
        var toggleCount = 0
        var submits = 0
        val context = RuntimeEnvironment.getApplication()
        compose.setContent {
            AurelisTheme(darkTheme = false) {
                var selected by remember { mutableIntStateOf(0) }
                var checked by remember { mutableStateOf(false) }
                var query by remember { mutableStateOf("Local") }
                Column {
                    SegmentedControl(listOf("System", "Light", "Dark"), selected, { selected = it })
                    SettingsSubRow("Reduce motion", checked, { checked = it; toggleCount++ })
                    SearchField(query, { query = it }, { submits++ })
                    SettingsRow(title = "Unavailable", enabled = false, onClick = {})
                }
            }
        }
        compose.onNodeWithText("Dark").performClick().assertIsSelected()
        compose.onNodeWithText("System").assertIsNotSelected()
        compose.onNodeWithText("Reduce motion").assertIsOff().performClick().assertIsOn()
        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        compose.runOnIdle { assertEquals(1, toggleCount) }
        compose.onNodeWithText("Unavailable").assertIsNotEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.search))
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, submits) }
        compose.onNodeWithContentDescription(context.getString(R.string.clear_search))
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("")
    }

    @Test fun capturesRealSharedComponentsInBothThemesAndLargeText() {
        var dark by mutableStateOf(false)
        var fontScale by mutableFloatStateOf(1f)
        compose.setContent {
            AurelisTheme(darkTheme = dark) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Surface(Modifier.fillMaxSize().testTag("gallery")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Aurelis Music", style = MaterialTheme.typography.headlineLarge)
                            Text("Música local", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SearchField("", {}, {}, placeholder = "Canciones, artistas, álbumes")
                            SegmentedControl(listOf("Sistema", "Claro", "Oscuro"), if (dark) 2 else 1, {})
                            SettingsGroup(header = "Apariencia") {
                                SettingsRow(title = "Biblioteca del dispositivo",
                                    subtitle = "Tu música, disponible sin conexión", onClick = {})
                                RowDivider()
                                SettingsSubRow("Reducir animaciones", true, {})
                            }
                            SongText("Una canción local", "Artista · Álbum")
                        }
                    }
                }
            }
        }
        for (large in listOf(false, true)) {
            for (night in listOf(false, true)) {
                compose.runOnIdle { dark = night; fontScale = if (large) 1.5f else 1f }
                compose.onNodeWithText("Biblioteca del dispositivo").assertIsDisplayed()
                compose.onNodeWithText("Reducir animaciones").assertIsDisplayed()
                compose.onNodeWithText("Reducir animaciones", useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayout ->
                        val layouts = mutableListOf<TextLayoutResult>()
                        assertTrue(getLayout(layouts))
                        val layout = layouts.single()
                        // Intrinsic paragraph widths may round by a fraction of a pixel.
                        // Verify actual truncation and visible characters, not that rounding.
                        val lastLine = layout.lineCount - 1
                        assertFalse("Switch label ellipsized: large=$large dark=$night",
                            layout.isLineEllipsized(lastLine))
                        assertFalse("Switch label height clipped", layout.didOverflowHeight)
                        assertEquals(layout.layoutInput.text.length,
                            layout.getLineEnd(lastLine, visibleEnd = true))
                    }
                // Robolectric does not drive PixelCopy's forceRedraw callback.
                // Draw the real, laid-out Android view with native graphics instead.
                val bitmap = compose.runOnIdle {
                    val view = compose.activity.window.decorView
                    Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
                        view.draw(Canvas(it))
                    }
                }
                assertTrue(bitmap.width > 0 && bitmap.height > 0)
                // A capture must contain rendered content, not just a blank surface.
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                assertTrue("Blank capture", pixels.toSet().size > 20)
                val file = File("build/reports/b03-visual/${if (night) "dark" else "light"}${if (large) "-large" else ""}.png")
                requireNotNull(file.parentFile).mkdirs()
                file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            }
        }
    }
}
