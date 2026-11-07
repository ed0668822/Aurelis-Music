package com.music.aurelis.ui.components.backdrop

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.music.aurelis.ui.components.backdrop.backdrops.layerBackdrop
import com.music.aurelis.ui.components.backdrop.effects.lens
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackdropAttachRegressionTest {
    @get:Rule(order = 0) val registerActivity = com.music.aurelis.ui.RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun actualBackdropNodeAttachesResizesAndReattachesWithoutUnspecifiedSizeCrash() {
        var visible by mutableStateOf(true)
        var side by mutableIntStateOf(120)
        compose.setContent {
            val backdrop = rememberLayerBackdrop()
            val shape = remember { RoundedCornerShape(20.dp) }
            Column {
                Box(Modifier.size(200.dp).layerBackdrop(backdrop))
                if (visible) Box(Modifier.size(side.dp).testTag("lens")
                    .drawPlainBackdrop(backdrop, shape = { shape }, effects = {
                        lens(12f, 8f, depthEffect = true, chromaticAberration = true)
                    }))
            }
        }
        compose.onNodeWithTag("lens").assertIsDisplayed().assertWidthIsEqualTo(120.dp)
        compose.runOnIdle { side = 180 }
        compose.onNodeWithTag("lens").assertWidthIsEqualTo(180.dp)
        compose.runOnIdle { visible = false }
        compose.onNodeWithTag("lens").assertDoesNotExist()
        compose.runOnIdle { visible = true }
        compose.onNodeWithTag("lens").assertIsDisplayed().assertWidthIsEqualTo(180.dp)
    }
}
