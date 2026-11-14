// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.player.MiniPlayerPull
import com.music.aurelis.ui.player.playerSwipeTrigger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = AurelisApplication::class)
class PlayerHandleGestureTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun removingAnActiveHandleCancelsThePullExactlyOnce() {
        var attached by mutableStateOf(true)
        var begins = 0; var cancels = 0; var releases = 0
        val pull = object : MiniPlayerPull {
            override fun beginDrag() { begins++ }
            override fun drag(delta: Float) = Unit
            override fun release(velocity: Float) { releases++ }
            override fun cancel() { cancels++ }
        }
        compose.setContent {
            Box(Modifier.size(240.dp).testTag("handle").then(
                if (attached) Modifier.playerSwipeTrigger(1, {}, pull) else Modifier))
        }
        compose.onNodeWithTag("handle").performTouchInput { down(center); moveBy(Offset(0f, 70f)) }
        compose.runOnIdle { assertEquals(1, begins); attached = false }
        compose.waitForIdle()
        compose.onNodeWithTag("handle").performTouchInput { cancel() }
        compose.runOnIdle { assertEquals(1, cancels); assertEquals(0, releases); attached = true }
        compose.onNodeWithTag("handle").performTouchInput {
            swipe(center, center + Offset(0f, 80f), durationMillis = 300)
        }
        compose.runOnIdle { assertEquals(2, begins); assertEquals(1, releases); attached = false }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, cancels) }
    }

    @Test fun explicitCancellationIsNotRepeatedWhenTheHandleIsRemoved() {
        var attached by mutableStateOf(true)
        var cancels = 0
        val pull = object : MiniPlayerPull {
            override fun drag(delta: Float) = Unit
            override fun release(velocity: Float) { fail("Cancelled drag released") }
            override fun cancel() { cancels++ }
        }
        compose.setContent {
            Box(Modifier.size(240.dp).testTag("handle").then(
                if (attached) Modifier.playerSwipeTrigger(1, {}, pull) else Modifier))
        }
        compose.onNodeWithTag("handle").performTouchInput { down(center); moveBy(Offset(0f, 70f)); cancel() }
        compose.runOnIdle { assertEquals(1, cancels); attached = false }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, cancels) }
    }
}
