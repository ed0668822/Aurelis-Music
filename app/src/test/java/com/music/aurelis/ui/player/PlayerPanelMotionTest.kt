package com.music.aurelis.ui.player

import androidx.compose.runtime.BroadcastFrameClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class PlayerPanelMotionTest {
    @Test fun cancelledPullBeforeTheFirstPlayerFrameCanOpenAgain() = runTest {
        var shown = false
        val dock = PlayerDock().apply { sheetTravel = 800f }
        val motion = PlayerSheetMotion(this, dock, { shown }, { shown = it }, inlineMode = true, reduceMotion = { true })
        motion.beginDrag(); motion.drag(-120f)
        assertTrue(shown); assertEquals(680f, motion.position(), 0f)
        motion.cancel(); runCurrent()
        assertFalse(shown); assertFalse(motion.holding)
        motion.laidOut = true
        motion.open(); runCurrent()
        assertTrue(shown); assertEquals(0f, motion.position(), 0f)
        motion.close(); runCurrent()
        assertFalse(shown)
    }
    @Test fun cancellingAnExpandedPlayerDragReturnsToOpen() = runTest {
        var shown = true
        val dock = PlayerDock().apply { sheetTravel = 800f }
        val motion = PlayerSheetMotion(this, dock, { shown }, { shown = it }, inlineMode = true, reduceMotion = { true })
        motion.beginDrag(); motion.drag(120f)
        assertEquals(120f, motion.position(), 0f)
        motion.cancel(); runCurrent()
        assertTrue(shown); assertFalse(motion.holding); assertEquals(0f, motion.position(), 0f)
    }
    @Test fun invalidDragAndVelocityDoNotLeaveANonFinitePanelPosition() = runTest {
        var shown = true
        val dock = PlayerDock().apply { sheetTravel = 800f }
        val motion = PlayerSheetMotion(this, dock, { shown }, { shown = it }, inlineMode = true, reduceMotion = { true })
        motion.beginDrag(); motion.drag(Float.NaN); motion.drag(Float.POSITIVE_INFINITY)
        assertEquals(0f, motion.position(), 0f)
        motion.release(Float.NaN); runCurrent()
        assertTrue(shown); assertTrue(motion.position().isFinite())
    }
    @Test fun originalSpringOpensAndClosesWithAControlledFrameClock() = runTest {
        var shown = false
        val clock = BroadcastFrameClock()
        val scope = CoroutineScope(coroutineContext + clock)
        val dock = PlayerDock().apply { sheetTravel = 800f }
        val motion = PlayerSheetMotion(scope, dock, { shown }, { shown = it }, inlineMode = true)
        motion.laidOut = true
        motion.open(); runCurrent()
        assertTrue(shown); assertTrue(motion.holding)
        var frame = 0L
        repeat(120) { clock.sendFrame(++frame * 16_666_667L); runCurrent() }
        assertFalse(motion.holding); assertEquals(0f, motion.position(), 0f)
        motion.close(); runCurrent()
        repeat(120) { clock.sendFrame(++frame * 16_666_667L); runCurrent() }
        assertFalse(shown); assertFalse(motion.holding)
    }
}
