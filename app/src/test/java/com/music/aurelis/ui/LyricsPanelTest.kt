package com.music.aurelis.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.lyrics.LyricLine
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
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
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
}
