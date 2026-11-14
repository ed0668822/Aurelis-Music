// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.components.LocalLiquidGlassEnabled
import com.music.aurelis.ui.components.MiniPlayer
import com.music.aurelis.ui.player.MiniPlayerPull
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MiniPlayerInteractionTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private fun horizontal(distance: Float) {
        compose.onNodeWithTag("mini-player").performTouchInput {
            swipe(center, center + Offset(distance, 0f), durationMillis = 300)
        }
    }

    @Test fun horizontalReleaseUsesLatestCallbacksAndCapabilitiesWithoutOpeningThePlayer() {
        var oldNext = 0; var newNext = 0; var previous = 0; var opened = 0
        var nextAction by mutableStateOf<() -> Unit>({ oldNext++ })
        var allowedNext by mutableStateOf(true)
        var allowedPrevious by mutableStateOf(true)
        var enabled by mutableStateOf(true)
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalLiquidGlassEnabled provides false) {
                Box(Modifier.width(320.dp)) {
                    MiniPlayer("Canción", "Artista", false, {}, { opened++ }, enabled = enabled,
                        canNext = allowedNext, canPrevious = allowedPrevious,
                        onNext = nextAction, onPrevious = { previous++ })
                }
            }
        } }
        compose.onNodeWithTag("mini-player").performTouchInput { down(center); moveBy(Offset(-100f, 0f)) }
        compose.runOnIdle { nextAction = { newNext++ }; allowedNext = false }
        compose.onNodeWithTag("mini-player").performTouchInput { up() }
        compose.runOnIdle { assertEquals(0, oldNext); assertEquals(0, newNext); allowedNext = true }
        horizontal(-130f)
        horizontal(130f)
        compose.runOnIdle { assertEquals(1, newNext); assertEquals(1, previous); allowedPrevious = false }
        horizontal(130f)
        compose.runOnIdle { enabled = false }
        horizontal(-130f)
        compose.runOnIdle {
            assertEquals(0, oldNext); assertEquals(1, newNext); assertEquals(1, previous); assertEquals(0, opened)
        }
    }

    @Test fun cancelledDragsDoNotSkipOrReleaseTheDockAndAnUpwardDragChoosesOneAxis() {
        var next = 0; var previous = 0; var opened = 0
        var begun = 0; var released = 0; var cancelled = 0
        val pull = object : MiniPlayerPull {
            override fun beginDrag() { begun++ }
            override fun drag(delta: Float) { assertTrue(delta.isFinite()) }
            override fun release(velocity: Float) { assertTrue(velocity.isFinite()); released++ }
            override fun cancel() { cancelled++ }
        }
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalLiquidGlassEnabled provides false) {
                Box(Modifier.width(320.dp)) {
                    MiniPlayer("Canción", "Artista", false, {}, { opened++ }, onNext = { next++ },
                        onPrevious = { previous++ }, canPrevious = true, pull = pull)
                }
            }
        } }
        compose.onNodeWithTag("mini-player").performTouchInput { down(center); moveBy(Offset(-100f, 0f)); cancel() }
        compose.onNodeWithTag("mini-player").performTouchInput { down(center); moveBy(Offset(0f, -120f)); cancel() }
        compose.runOnIdle { assertEquals(0, next); assertEquals(0, previous); assertEquals(0, released); assertEquals(1, cancelled) }
        compose.onNodeWithTag("mini-player").performTouchInput {
            swipe(center, center + Offset(25f, -180f), durationMillis = 400)
        }
        compose.runOnIdle {
            assertEquals(2, begun); assertEquals(1, released); assertEquals(1, cancelled)
            assertEquals(0, next); assertEquals(0, previous); assertEquals(0, opened)
        }
    }

    @Test fun compactLargeTextKeepsTransportTargetsAndTapAndSwipeExpansionSeparate() {
        var plays = 0; var next = 0; var opened = 0
        compose.setContent { AurelisTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalLiquidGlassEnabled provides false,
                LocalDensity provides Density(density.density, 1.5f)) {
                Box(Modifier.width(320.dp)) {
                    MiniPlayer("Nombre largo de una canción local", "Nombre largo de artista", false,
                        { plays++ }, { opened++ }, qualityLabel = "FLAC", onNext = { next++ })
                }
            }
        } }
        for (label in listOf("Reproducir", "Siguiente", "Abrir reproductor")) {
            // Material's visual state layer is 40dp; verify the expanded hit target.
            compose.onNodeWithContentDescription(label)
                .assertTouchWidthIsEqualTo(48.dp)
                .assertTouchHeightIsEqualTo(48.dp)
        }
        compose.onNodeWithContentDescription("Reproducir").performClick()
        compose.onNodeWithContentDescription("Siguiente").performClick()
        compose.runOnIdle { assertEquals(1, plays); assertEquals(1, next); assertEquals(0, opened) }
        compose.onNodeWithContentDescription("Abrir reproductor").performClick()
        compose.onNodeWithTag("mini-player").performTouchInput {
            swipe(center, center + Offset(0f, -180f), durationMillis = 400)
        }
        compose.runOnIdle { assertEquals(2, opened); assertEquals(1, plays); assertEquals(1, next) }
    }

    @Test fun progressRemainsFiniteAndBoundedWithoutInventingQualityFromTheTitle() {
        var progress by mutableFloatStateOf(0f)
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalLiquidGlassEnabled provides false) {
                MiniPlayer("Archivo.flac", "Artista", false, {}, {}, progress = progress)
            }
        } }
        compose.onNodeWithText("FLAC").assertDoesNotExist()
        for ((input, expected) in listOf(Float.NaN to 0f, Float.POSITIVE_INFINITY to 0f,
            Float.NEGATIVE_INFINITY to 0f, -2f to 0f, 2f to 1f, 0.5f to 0.5f)) {
            compose.runOnIdle { progress = input }
            val info = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                .fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
            assertTrue(info.current.isFinite())
            assertEquals(expected, info.current, 0.001f)
        }
    }
}
