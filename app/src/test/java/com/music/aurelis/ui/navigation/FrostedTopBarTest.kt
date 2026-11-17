// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.navigation

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.R
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.ui.RobolectricActivityRule
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.theme.AurelisTheme
import com.music.aurelis.ui.theme.LocalReduceMotion
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "es-rES-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FrostedTopBarTest {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private fun bounds(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
        .fetchSemanticsNode().boundsInRoot
    private fun profile() = compose.onNodeWithContentDescription(compose.activity.getString(R.string.settings_profile_action))

    @Test fun profileAndBackKeepSourceGeometryAcrossMaterialsThemesAndScrolling() {
        var glass by mutableStateOf(true)
        var night by mutableStateOf(false)
        var scrolled by mutableStateOf(false)
        var backs = 0
        var profiles = 0
        val oldBlur = SettingsRepository.reduceDynamicBlur.value
        try {
            compose.setContent { AurelisTheme(darkTheme = night) {
                CompositionLocalProvider(LocalGlassHardware provides false,
                    LocalLiquidGlassEnabled provides glass, LocalReduceMotion provides true) {
                    FrostedTopBar("Small title stays hidden", { scrolled },
                        transparentBackdrop = true, artworkPageChrome = true,
                        onBack = { backs++ }, actions = { LocalProfileButton { profiles++ } })
                }
            } }
            for (dark in listOf(false, true)) {
                for ((enabled, blur) in listOf(true to false, false to false, true to true)) {
                    compose.runOnIdle { night = dark; glass = enabled; scrolled = !scrolled; SettingsRepository.setReduceDynamicBlur(blur) }
                    val back = bounds("top-bar-back")
                    val actions = bounds("top-bar-actions")
                    assertEquals(44f, back.width, 0.5f)
                    assertEquals(44f, back.height, 0.5f)
                    assertEquals(44f, actions.width, 0.5f)
                    assertEquals(44f, actions.height, 0.5f)
                    assertEquals(16f, back.left, 0.5f)
                    assertEquals(344f, actions.right, 0.5f)
                    assertEquals(back.center.y, actions.center.y, 0.5f)
                    assertEquals(28f, bounds("top-bar-avatar").width, 0.5f)
                    compose.onNodeWithText("Small title stays hidden").assertDoesNotExist()
                    compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
                    profile().performClick()
                }
            }
            compose.runOnIdle { assertEquals(6, backs); assertEquals(6, profiles) }
        } finally { SettingsRepository.setReduceDynamicBlur(oldBlur) }
    }

    @Test fun actionsExpandToTheLeftWithSourceGapsAndCollapseToOneCircle() {
        var extra by mutableIntStateOf(0)
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalGlassHardware provides false, LocalReduceMotion provides true) {
                FrostedTopBar("", { false }, transparentBackdrop = true, artworkPageChrome = true,
                    actions = {
                        repeat(extra) { IconButton(onClick = {}) { Icon(AurelisIcons.Search, null) } }
                        LocalProfileButton {}
                    })
            }
        } }
        // The source rounds each 1.5dp end inset to2px at mdpi.
        for ((count, width) in listOf(0 to 44f, 1 to 102f, 2 to 156f, 0 to 44f)) {
            compose.runOnIdle { extra = count }
            val pill = bounds("top-bar-actions")
            assertEquals(width, pill.width, 0.5f)
            assertEquals(44f, pill.height, 0.5f)
            assertEquals(344f, pill.right, 0.5f)
            profile().assertIsDisplayed()
        }
    }

    @Test fun searchSharesControlHeightAndPreservesSubmitClearAndProfileInRtl() {
        var query by mutableStateOf("Local")
        var submissions = 0
        var profiles = 0
        compose.setContent { AurelisTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalGlassHardware provides false, LocalReduceMotion provides true,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, 1.5f)) {
                FrostedTopBar("", { false }, transparentBackdrop = true, artworkPageChrome = true,
                    accessory = { SearchField(query, { query = it }, { submissions++ }) },
                    actions = { LocalProfileButton { profiles++ } })
            }
        } }
        val accessory = bounds("top-bar-accessory")
        val actions = bounds("top-bar-actions")
        assertEquals(44f, accessory.height, 0.5f)
        assertEquals(44f, actions.height, 0.5f)
        assertEquals(16f, actions.left, 0.5f)
        assertEquals(8f, accessory.left - actions.right, 0.5f)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.search)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.clear_search)).performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("").assertIsFocused()
        compose.onNode(hasSetTextAction()).performTextInput("Canción")
        compose.onNode(hasSetTextAction()).performImeAction()
        profile().performClick()
        compose.runOnIdle { assertEquals("Canción", query); assertEquals(2, submissions); assertEquals(1, profiles) }
        assertEquals(accessory, bounds("top-bar-accessory"))
    }

    @Test fun softwareCaptureKeepsLocalPhotoAndFallbackInteractive() {
        val previous = ProfileRepository.userPhotoUri
        val id = ProfileRepository.profileId
        var profiles = 0
        var glass by mutableStateOf(true)
        val image = java.io.File(compose.activity.filesDir, "f003-profile-fixture.png")
        val fixture = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        fixture.eraseColor(android.graphics.Color.GREEN)
        image.outputStream().use { assertTrue(fixture.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        fixture.recycle()
        try {
            compose.setContent { AurelisTheme {
                CompositionLocalProvider(LocalGlassHardware provides false,
                    LocalLiquidGlassEnabled provides glass, LocalReduceMotion provides true) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        FrostedTopBar("", { false }, transparentBackdrop = true, artworkPageChrome = true,
                            onBack = {}, actions = { LocalProfileButton { profiles++ } })
                    }
                }
            } }
            for (photo in listOf(image.toURI().toString(), null)) {
                for (enabled in listOf(true, false)) {
                    compose.runOnIdle { ProfileRepository.userPhotoUri = photo; glass = enabled }
                    profile().assertIsDisplayed().performClick()
                    assertEquals(28f, bounds("top-bar-avatar").width, 0.5f)
                    compose.runOnIdle {
                        val view = compose.activity.window.decorView
                        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                        try {
                            val canvas = Canvas(bitmap)
                            assertFalse(canvas.isHardwareAccelerated)
                            view.draw(canvas)
                            val pixels = IntArray(bitmap.width * bitmap.height)
                            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                            assertTrue("Header capture must contain rendered content", pixels.toSet().size > 20)
                        } finally { bitmap.recycle() }
                        assertEquals(id, ProfileRepository.profileId)
                        assertEquals(photo, ProfileRepository.userPhotoUri)
                    }
                }
            }
            compose.runOnIdle { assertEquals(4, profiles) }
        } finally {
            compose.runOnIdle { ProfileRepository.userPhotoUri = previous }
            image.delete()
        }
    }

    @Test fun changingActionWidthWithMotionEnabledSettlesAtTheSameAnchor() {
        var extra by mutableStateOf(false)
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalGlassHardware provides false, LocalReduceMotion provides false) {
                FrostedTopBar("", { false }, transparentBackdrop = true, artworkPageChrome = true,
                    actions = {
                        if (extra) IconButton(onClick = {}) { Icon(AurelisIcons.Search, null) }
                        LocalProfileButton {}
                    })
            }
        } }
        val original = bounds("top-bar-actions")
        compose.runOnIdle { extra = true }
        compose.waitForIdle()
        assertEquals(102f, bounds("top-bar-actions").width, 0.5f)
        assertEquals(original.right, bounds("top-bar-actions").right, 0.5f)
        compose.runOnIdle { extra = false }
        compose.waitForIdle()
        assertEquals(original, bounds("top-bar-actions"))
    }
}
