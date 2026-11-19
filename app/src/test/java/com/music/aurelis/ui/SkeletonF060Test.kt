// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.components.SkeletonLoader
import com.music.aurelis.ui.components.SongRowSkeleton
import com.music.aurelis.ui.theme.AurelisTheme
import com.music.aurelis.ui.theme.LocalReduceMotion
import org.junit.Rule
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkeletonF060Test {
    @get:Rule(order = 0) val activity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun actualLoadingPlaceholderRetainsItsDimensionsWhenMotionIsReduced() {
        var reduced by mutableStateOf(true)
        compose.setContent { AurelisTheme {
            CompositionLocalProvider(LocalReduceMotion provides reduced) {
                Column {
                    SkeletonLoader(Modifier.testTag("loading-placeholder"))
                    SongRowSkeleton(index = 1, modifier = Modifier.testTag("row-placeholder"))
                }
            }
        } }
        compose.onNodeWithTag("loading-placeholder").assertExists()
            .assertHeightIsEqualTo(64.dp)
        compose.onNodeWithTag("row-placeholder").assertExists()
        compose.runOnIdle { reduced = false }
        compose.onNodeWithTag("loading-placeholder").assertExists()
            .assertHeightIsEqualTo(64.dp)
        compose.runOnIdle { reduced = true }
        compose.onNodeWithTag("loading-placeholder").assertExists()
    }
}
