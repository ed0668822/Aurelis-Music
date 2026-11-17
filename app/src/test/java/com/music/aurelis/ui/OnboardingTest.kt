// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.music.aurelis.AurelisApplication
import com.music.aurelis.R
import com.music.aurelis.ui.components.LocalGlassHardware
import com.music.aurelis.ui.onboarding.*
import com.music.aurelis.ui.theme.AurelisTheme
import com.music.aurelis.ui.theme.LocalReduceMotion
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 35], qualifiers = "es-rES-w360dp-h800dp-mdpi", application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingTest {
    @get:Rule(order = 0) val registerActivity = RobolectricActivityRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()
    private val preferences get() = compose.activity.getSharedPreferences("f004-fixture", Context.MODE_PRIVATE)
    private fun label(id: Int) = compose.activity.getString(id)
    private fun button(id: Int) = compose.onNodeWithText(label(id))
    private fun field() = compose.onNode(hasSetTextAction())
    @Before fun resetCompletionFixture() { preferences.edit().clear().commit() }
    private fun awaitLibrary() {
        compose.waitUntil(5000) { compose.onAllNodesWithText("Local library").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(preferences.getBoolean(LOCAL_ONBOARDING_COMPLETE, false))
    }

    private fun start(prefs: SharedPreferences = preferences, granted: Boolean = false,
                      readOnly: Boolean = false, onName: (String) -> Unit = {},
                      request: () -> Unit = {}): StateRestorationTester {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { AurelisTheme(darkTheme = false) {
            CompositionLocalProvider(LocalGlassHardware provides false, LocalReduceMotion provides true) {
                LocalOnboardingGate(prefs, "Nombre existente", readOnly, onName, granted, request) {
                    Text("Local library")
                }
            }
        } }
        return restoration
    }

    @Test fun nameAndPermissionSkipPersistWithoutExposingUnderlyingNavigation() {
        preferences.edit().putString("unrelated", "keep").commit()
        var name = ""
        var requests = 0
        val restoration = start(onName = { name = it }, request = { requests++ })
        compose.runOnIdle {
            assertFalse(WindowCompat.getInsetsController(compose.activity.window,
                compose.activity.window.decorView).isAppearanceLightStatusBars)
        }
        compose.onNodeWithText("Local library").assertDoesNotExist()
        field().performTextReplacement("  Mi nombre  ")
        button(R.string.onboarding_next).performClick()
        compose.runOnIdle { assertEquals("Mi nombre", name); assertEquals(0, requests) }
        button(R.string.onboarding_grant_permission).performClick()
        button(R.string.onboarding_permission_title).assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, requests) }
        button(R.string.onboarding_skip).performClick()
        awaitLibrary()
        compose.onNodeWithText("Local library").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(WindowCompat.getInsetsController(compose.activity.window,
                compose.activity.window.decorView).isAppearanceLightStatusBars)
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("local-onboarding").assertDoesNotExist()
        compose.runOnIdle { assertEquals("keep", preferences.getString("unrelated", null)) }
    }

    @Test fun recreationRetainsDraftAndPermissionPageAndBackReturnsToTheName() {
        var chosen = ""
        val restoration = start(onName = { chosen = it })
        field().performTextReplacement("Borrador")
        restoration.emulateSavedInstanceStateRestore()
        field().assertTextEquals("Borrador")
        button(R.string.onboarding_next).performClick()
        restoration.emulateSavedInstanceStateRestore()
        button(R.string.onboarding_permission_title).assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        field().assertTextEquals("Borrador")
        compose.runOnIdle {
            assertEquals("Borrador", chosen)
            assertFalse(preferences.getBoolean(LOCAL_ONBOARDING_COMPLETE, false))
        }
    }

    @Test fun blankNameCannotAdvanceAndInputHonorsExistingProfileLimit() {
        start()
        field().performTextClearance()
        button(R.string.onboarding_next).assertIsNotEnabled()
        field().performImeAction()
        button(R.string.onboarding_permission_title).assertDoesNotExist()
        field().performTextReplacement("a".repeat(59) + "🎵")
        field().assertTextEquals("a".repeat(59))
        field().performTextClearance()
        field().performTextInput("a".repeat(80))
        field().assertTextEquals("a".repeat(60))
        button(R.string.onboarding_next).assertIsEnabled()
    }

    @Test fun profileWriteFailureKeepsWelcomeAndDoesNotAskForPermission() {
        var requests = 0
        start(onName = { throw IllegalStateException("fixture failure") }, request = { requests++ })
        button(R.string.onboarding_next).performClick()
        button(R.string.onboarding_save_error).assertIsDisplayed()
        field().assertTextEquals("Nombre existente")
        button(R.string.onboarding_permission_title).assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, requests); assertFalse(preferences.contains(LOCAL_ONBOARDING_COMPLETE)) }
    }

    @Test fun readOnlyProfileCanFinishWithoutAnyRenameOrPermissionRequest() {
        var names = 0
        var requests = 0
        start(granted = true, readOnly = true, onName = { names++ }, request = { requests++ })
        button(R.string.onboarding_next).performClick()
        button(R.string.onboarding_start).performClick()
        awaitLibrary()
        compose.onNodeWithText("Local library").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, names); assertEquals(0, requests) }
    }

    @Test fun failedCompletionCommitShowsRecoveryAndCanBeRetried() {
        var writable = false
        val actual = preferences
        val prefs = object : SharedPreferences by actual {
            override fun edit(): SharedPreferences.Editor {
                val editor = actual.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                        editor.putBoolean(key, value); return this
                    }
                    override fun remove(key: String?): SharedPreferences.Editor {
                        editor.remove(key); return this
                    }
                    override fun commit(): Boolean = if (writable) editor.commit() else false
                }
            }
        }
        start(prefs, granted = true)
        button(R.string.onboarding_next).performClick()
        button(R.string.onboarding_start).performClick()
        compose.waitUntil(5000) {
            compose.onAllNodesWithText(label(R.string.onboarding_save_error)).fetchSemanticsNodes().isNotEmpty()
        }
        assertFalse(actual.getBoolean(LOCAL_ONBOARDING_COMPLETE, false))
        compose.onNodeWithText("Local library").assertDoesNotExist()
        compose.runOnIdle { writable = true }
        button(R.string.onboarding_start).performClick()
        awaitLibrary()
        compose.onNodeWithText("Local library").assertIsDisplayed()
    }

    @Test fun completedInstallationDoesNotShowWelcomeOrModifyItsProfile() {
        preferences.edit().putBoolean(LOCAL_ONBOARDING_COMPLETE, true).commit()
        var writes = 0
        start(onName = { writes++ })
        compose.onNodeWithText("Local library").assertIsDisplayed()
        compose.onNodeWithTag("local-onboarding").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, writes) }
    }

    @Test fun failedDiskCommitDoesNotLeaveACompletedFlagInMemory() {
        val actual = preferences
        val prefs = object : SharedPreferences by actual {
            override fun edit(): SharedPreferences.Editor {
                val editor = actual.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                        editor.putBoolean(key, value); return this
                    }
                    override fun remove(key: String?): SharedPreferences.Editor {
                        editor.remove(key); return this
                    }
                    override fun commit(): Boolean { editor.apply(); return false }
                }
            }
        }
        assertFalse(persistOnboardingCompletion(prefs))
        assertFalse(actual.getBoolean(LOCAL_ONBOARDING_COMPLETE, false))
    }

    @Test fun malformedCompletionFlagRecoversWithoutAStartupCrash() {
        preferences.edit().putString(LOCAL_ONBOARDING_COMPLETE, "invalid").commit()
        start(granted = true)
        field().assertTextEquals("Nombre existente")
        button(R.string.onboarding_next).performClick()
        button(R.string.onboarding_start).performClick()
        awaitLibrary()
        compose.onNodeWithText("Local library").assertIsDisplayed()
    }

    @Test fun largeTextInShortWindowKeepsBothPagesActionsReachable() {
        var finished = 0
        compose.setContent { AurelisTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalGlassHardware provides false, LocalReduceMotion provides true,
                LocalDensity provides Density(density.density, 2f)) {
                OnboardingScreen(false, {}, { finished++ }, Modifier.height(320.dp), initialName = "Nombre")
            }
        } }
        button(R.string.onboarding_next).performScrollTo().performClick()
        button(R.string.onboarding_skip).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, finished) }
    }
}
