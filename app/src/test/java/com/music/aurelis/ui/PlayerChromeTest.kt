// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.components.PlayerControls
import com.music.aurelis.ui.player.playerControlTint
import com.music.aurelis.ui.player.playerAdditiveChrome
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayerChromeTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private fun contrast(a: Color, b: Color): Float {
        val one = a.luminance(); val two = b.luminance()
        return (maxOf(one, two) + 0.05f) / (minOf(one, two) + 0.05f)
    }

    @Test fun solidFallbackPreservesThemeAndDifferentiatesUnavailableControls() {
        assertSame(Modifier, Modifier.playerAdditiveChrome(false))
        for ((ink, background) in listOf(Color.White to Color.Black, Color.Black to Color.White)) {
            val active = playerControlTint(ink, true, false)
            val disabled = playerControlTint(ink, false, false)
            assertEquals(ink, active)
            assertTrue(contrast(active.compositeOver(background), background) >= 3f)
            assertTrue(contrast(active.compositeOver(background), background) > contrast(disabled.compositeOver(background), background))
        }
        // The neutral blended ink is intended for the player's dark artwork backdrop.
        val active = playerControlTint(Color.Red, true, true)
        val disabled = playerControlTint(Color.Red, false, true)
        assertTrue(contrast(active.compositeOver(Color.Black), Color.Black) >= 3f)
        assertTrue(active.alpha > disabled.alpha)
    }

    @Test fun disabledTransportStillRejectsCommandsAndCentralPlayKeepsItsAction() {
        var next = 0; var previous = 0; var play = 0
        compose.setContent { AurelisTheme {
            PlayerControls(false, false, { play++ }, { next++ }, { previous++ },
                canNext = false, canPrevious = false, reduceAnimation = true)
        } }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_next)).assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_previous)).assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_play)).performClick()
        compose.runOnIdle { assertEquals(0, next); assertEquals(0, previous); assertEquals(1, play) }
    }

    @Test fun enabledChromeAlsoDrawsOnASoftwareBitmapCanvas() {
        var draws = 0
        compose.setContent {
            Box(Modifier.size(48.dp).playerAdditiveChrome(true).drawBehind {
                draws++
                drawRect(Color.White.copy(alpha = 0.58f))
            })
        }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bitmap)
                assertFalse(canvas.isHardwareAccelerated)
                val before = draws
                view.draw(canvas)
                assertTrue("Software capture must draw the enabled chrome", draws > before)
            } finally {
                bitmap.recycle()
            }
        }
    }

}
