// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.data.lyrics.LyricWord
import com.music.aurelis.ui.player.adjustedLyricsSeekTarget
import com.music.aurelis.ui.theme.LocalReduceMotion
import androidx.compose.ui.semantics.SemanticsProperties
import com.music.aurelis.playback.PlaybackPosition
import com.music.aurelis.ui.player.SyncedLyricsPanel
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LyricsPanelTest {
    @get:org.junit.Rule(order = 0) val registerActivity = com.music.aurelis.ui.RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun timestampsHighlightAndManualFollowCanBePausedAndResumed() {
        val position = PlaybackPosition(); var seek = -1L
        compose.setContent { AurelisTheme { Surface { SyncedLyricsPanel(
            listOf(LyricLine(0, "Primera"), LyricLine(1000, "Segunda")), "1", position, true, false, { seek = it }) } } }
        compose.onNodeWithText("Primera").assertIsSelected()
        compose.runOnIdle { position.report(1000) }
        compose.onNodeWithText("Segunda").assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(1000L, seek) }
        compose.onNodeWithText("Pausar seguimiento").performClick()
        compose.onNodeWithText("Seguir letras").performClick()
        compose.onNodeWithText("Pausar seguimiento").assertIsDisplayed()
    }
    @Test fun plainLyricsNeverHighlightOrSeekAndDisplayAllText() {
        val position = PlaybackPosition().apply { report(60_000) }
        compose.setContent { AurelisTheme { Surface { SyncedLyricsPanel(
            listOf(LyricLine(0, "Texto A"), LyricLine(0, "Texto B")), "plain", position, true, false, { fail("Plain text must not seek") }, isSynced = false) } } }
        compose.onNodeWithText("Letra sin sincronización").assertIsDisplayed()
        compose.onNodeWithText("Texto A").assertIsNotSelected().assertIsNotEnabled()
        compose.onNodeWithText("Texto B").assertIsNotSelected().assertIsDisplayed()
        compose.onNodeWithText("Pausar seguimiento").assertDoesNotExist()
    }
    @Test fun disablingSyncKeepsTimestampedLyricsReadableWithoutHighlighting() {
        compose.setContent { AurelisTheme { Surface { SyncedLyricsPanel(
            listOf(LyricLine(0, "Línea con tiempo")), "disabled", PlaybackPosition(), true, false, {}, syncEnabled = false) } } }
        compose.onNodeWithText("Línea con tiempo").assertIsDisplayed().assertIsNotSelected()
        compose.onNodeWithText("Sincronización desactivada en ajustes").assertIsDisplayed()
        compose.onNodeWithText("Pausar seguimiento").assertDoesNotExist()
    }

    @Test fun aSweptWordHasOnlyOneAccessibleTextNodeAndRemainsSeekable() {
        val position = PlaybackPosition().apply { report(1500) }
        var seek = -1L
        val line = LyricLine(1000, "Luna", listOf(LyricWord(1000, 2000, "Luna")))
        compose.setContent { AurelisTheme { Surface { SyncedLyricsPanel(
            listOf(line), "word", position, true, false, { seek = it }) } } }
        compose.onAllNodesWithText("Luna").assertCountEquals(1)
        compose.onNodeWithText("Luna").assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(1000L, seek); position.report(2500) }
        compose.onAllNodesWithText("Luna").assertCountEquals(1)
    }

    @Test fun positiveOffsetDelaysFocusAndTheSeekUsesTheInverseOffset() {
        val position = PlaybackPosition().apply { report(2000) }
        var seek = -1L
        compose.setContent { AurelisTheme { Surface { SyncedLyricsPanel(
            listOf(LyricLine(1000, "Primera con offset"), LyricLine(3000, "Segunda con offset")),
            "offset", position, true, false, { seek = adjustedLyricsSeekTarget(it, 1000) }, offsetMs = 1000) } } }
        compose.onNodeWithText("Primera con offset").assertIsSelected()
        compose.runOnIdle { position.report(4000) }
        compose.onNodeWithText("Segunda con offset").assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(4000L, seek) }
    }

    @Test fun reducedMotionUsesReportedPositionsWithoutAnInterpolatingFrameLoop() {
        val position = PlaybackPosition().apply { report(0); advancing = true }
        compose.setContent { AurelisTheme { CompositionLocalProvider(LocalReduceMotion provides true) { Surface { SyncedLyricsPanel(
            listOf(LyricLine(0, "Movimiento A"), LyricLine(1000, "Movimiento B")),
            "reduced", position, true, true, {}) } } } }
        compose.onNodeWithText("Movimiento A").assertIsSelected()
        compose.runOnIdle { position.report(1000) }
        compose.onNodeWithText("Movimiento B").assertIsSelected()
        compose.runOnIdle { position.report(0); position.seeks++ }
        compose.onNodeWithText("Movimiento A").assertIsSelected()
    }

    @Test fun overlappingLeadAndBackingVoicesAreVisibleAndFocusedTogether() {
        val first = LyricLine(0, "Voz uno", listOf(LyricWord(0, 3000, "Voz uno")),
            background = LyricLine(0, "Eco", listOf(LyricWord(0, 3000, "Eco"))))
        val second = LyricLine(1000, "Voz dos", listOf(LyricWord(1000, 2000, "Voz dos")))
        val position = PlaybackPosition().apply { report(1500) }
        compose.setContent { AurelisTheme { Surface { SyncedLyricsPanel(
            listOf(first, second), "duet", position, true, false, {}) } } }
        compose.onNodeWithText("Voz uno").assertIsSelected()
        compose.onNodeWithText("Voz dos").assertIsSelected()
        compose.onNodeWithText("Eco").assertIsDisplayed()
    }

    @Test fun manualScrollStopsFollowingUntilTheUserResumesIt() {
        val position = PlaybackPosition().apply { report(0) }
        val lines = (0..30).map { LyricLine(it * 1000L, "Línea manual $it") }
        compose.setContent { AurelisTheme { CompositionLocalProvider(LocalReduceMotion provides true) { Surface { SyncedLyricsPanel(
            lines, "manual", position, true, false, {}) } } } }
        compose.onNodeWithTag("lyrics-list").performTouchInput { swipeUp() }
        compose.onNodeWithText("Seguir letras").assertIsDisplayed()
        var before = 0f
        val beforeRange = compose.onNodeWithTag("lyrics-list").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        compose.runOnIdle { before = beforeRange.value() }
        compose.runOnIdle { position.report(2000) }
        val afterRange = compose.onNodeWithTag("lyrics-list").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        compose.runOnIdle { assertEquals(before, afterRange.value(), 0.1f) }
        compose.onNodeWithText("Seguir letras").performClick()
        compose.onNodeWithText("Línea manual 2").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithText("Pausar seguimiento").assertIsDisplayed()
    }

    @Test fun explicitPauseAndManualReadingRemainPausedWhenPlayerIsStopped() {
        val position = PlaybackPosition().apply { report(0); advancing = false }
        var seek = -1L
        val lines = (0..25).map { LyricLine(it * 1000L, "Lectura pausada $it") }
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                Surface { SyncedLyricsPanel(lines, "paused-manual", position, true, false, { seek = it }) }
            }
        } }
        compose.onNodeWithText("Pausar seguimiento").performClick()
        compose.onNodeWithText("Seguir letras").assertIsDisplayed()
        compose.onNodeWithTag("lyrics-list").performTouchInput { swipeUp() }
        compose.runOnIdle { position.report(4000) }
        compose.onNodeWithText("Seguir letras").assertIsDisplayed()
        compose.runOnIdle { assertEquals(-1L, seek) }
        compose.onNodeWithText("Seguir letras").performClick()
        compose.onNodeWithText("Pausar seguimiento").assertIsDisplayed()
    }

    @Test fun disablingSyncAlsoDisablesTapToSeekForTimestampedVerses() {
        val position = PlaybackPosition().apply { report(1500) }
        compose.setContent { AurelisTheme { Surface { SyncedLyricsPanel(
            listOf(LyricLine(1000, "Verso sin seek")), "sync-paused",
            position, true, false, { fail("Seeking must be disabled") },
            syncEnabled = false) } } }
        compose.onNodeWithText("Verso sin seek").assertIsNotEnabled().assertIsNotSelected()
    }

    @Test fun centerFocusUsesMeasuredRowsIncludingWrappedText() {
        val lines = (0..12).map { LyricLine(it * 1000L, "Línea $it con texto suficientemente largo para ocupar más de una fila") }
        val position = PlaybackPosition().apply { report(5000) }
        compose.setContent { AurelisTheme { CompositionLocalProvider(LocalReduceMotion provides true) { Surface { SyncedLyricsPanel(
            lines, "center", position, true, false, {}, centerFocus = true) } } } }
        compose.onNodeWithTag("lyrics-row-5").assertIsDisplayed()
        val viewport = compose.onNodeWithTag("lyrics-list").fetchSemanticsNode().boundsInRoot
        val row = compose.onNodeWithTag("lyrics-row-5").fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.center.y, row.center.y, 2f)
    }

    @Test fun rightToLeftSweptLyricsRemainReadableAndSeekable() {
        val line = LyricLine(0, "مرحبا", listOf(LyricWord(0, 2000, "مرحبا")))
        val position = PlaybackPosition().apply { report(1000) }
        var seek = -1L
        compose.setContent { AurelisTheme { Surface { SyncedLyricsPanel(
            listOf(line), "rtl", position, true, false, { seek = it }) } } }
        compose.onAllNodesWithText("مرحبا").assertCountEquals(1)
        compose.onNodeWithText("مرحبا").assertIsDisplayed().assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(0L, seek) }
    }

    @Test fun instrumentalDotsRemainSeekableAndPickingVersesDoesNotSeek() {
        val position = PlaybackPosition().apply { report(0) }
        var seek = -1L
        var picking by mutableStateOf(false)
        var picks by mutableStateOf(emptySet<Int>())
        var longPresses = 0
        val lines = listOf(LyricLine(0, ""), LyricLine(5000, "Una línea para compartir"))
        compose.setContent { AurelisTheme { Surface { SyncedLyricsPanel(
            lines, "instrumental", position, true, false, { seek = it },
            onLineLongClick = { longPresses++; picking = true; picks = setOf(it) },
            picking = picking, picked = picks, onTogglePick = { index -> picks = if (index in picks) picks - index else picks + index }) } } }
        compose.onNodeWithContentDescription("Instrumental").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(0L, seek); seek = -1L }
        compose.onNodeWithTag("lyrics-line-1").performTouchInput { longClick() }
        compose.onNodeWithTag("lyrics-line-1").assertIsSelected().performClick().assertIsNotSelected()
        compose.runOnIdle { assertEquals(1, longPresses); assertEquals(-1L, seek); assertTrue(picks.isEmpty()) }
    }

    @Test fun onlyDirectReadingGesturesHideOrRevealPlaybackControls() {
        lateinit var connection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection
        var hidden = 0; var revealed = 0
        compose.setContent { AurelisTheme {
            connection = com.music.aurelis.ui.player.rememberPlayerControlsOnScroll(onReveal = { revealed++ }, onHide = { hidden++ })
            androidx.compose.material3.Text("Gesture fixture")
        } }
        compose.runOnIdle {
            connection.onPreScroll(androidx.compose.ui.geometry.Offset(0f, -100f), androidx.compose.ui.input.nestedscroll.NestedScrollSource.SideEffect)
            assertEquals(0, hidden); assertEquals(0, revealed)
            connection.onPreScroll(androidx.compose.ui.geometry.Offset(0f, -100f), androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput)
            assertEquals(1, hidden)
            connection.onPreScroll(androidx.compose.ui.geometry.Offset(0f, 100f), androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput)
            assertEquals(1, revealed)
        }
    }

}
