// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.components.PlayerControls
import com.music.aurelis.ui.theme.AurelisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayerButtonF056Test {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun transportRemainsClickableAfterPlayPauseStateChanges() {
        var playing by mutableStateOf(false)
        var playCalls = 0
        var nextCalls = 0
        var previousCalls = 0
        compose.setContent { AurelisTheme {
            PlayerControls(
                isPlaying = playing,
                isLoading = false,
                onPlayPause = { playCalls++; playing = !playing },
                onNext = { nextCalls++ },
                onPrevious = { previousCalls++ },
                reduceAnimation = true,
            )
        } }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_play))
            .assertIsEnabled().performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_pause))
            .assertIsEnabled().performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_next))
            .performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_previous))
            .performClick()
        compose.runOnIdle {
            assertEquals(2, playCalls)
            assertEquals(1, nextCalls)
            assertEquals(1, previousCalls)
        }
    }
    @Test fun disabledSkipsNeverTriggerEvenAcrossAvailabilityChanges() {
        var enabled by mutableStateOf(false)
        var nextCalls = 0
        compose.setContent { AurelisTheme {
            PlayerControls(false, false, {}, { nextCalls++ }, {},
                canNext = enabled, reduceAnimation = true)
        } }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_next))
            .assertIsNotEnabled()
        compose.runOnIdle { enabled = true }
        compose.onNodeWithContentDescription(compose.activity.getString(com.music.aurelis.R.string.player_next))
            .assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, nextCalls) }
    }
}
