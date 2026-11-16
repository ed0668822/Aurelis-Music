// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.os.PowerManager
import android.os.UserManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.music.aurelis.MainActivity
import com.music.aurelis.R
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.data.SettingsRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.sin

/** Exercises the real Activity, saved state, renderer and service on Android. */
@RunWith(AndroidJUnit4::class)
class RealSettingsNavigationInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    private fun rows(): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (node.packageName?.toString() == context.packageName && node.isVisibleToUser) result.add(node)
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
        return result
    }

    private fun labeled(node: AccessibilityNodeInfo, text: String) =
        listOf(node.text, node.contentDescription).any { label ->
            label?.toString()?.lineSequence()?.any { it.equals(text, ignoreCase = true) } == true
        }

    private fun windowDiagnostic(): String {
        val root = instrumentation.uiAutomation.rootInActiveWindow
        val labels = mutableListOf<String>()
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 30 || labels.size >= 40) return
            if (node.isVisibleToUser && (!node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank())) {
                labels.add("${node.packageName}:${node.text}|${node.contentDescription}")
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let { visit(it, depth + 1) }
        }
        root?.let { visit(it, 0) }
        return "root=${root?.packageName}/${root?.className}; " +
            "interactive=${context.getSystemService(PowerManager::class.java).isInteractive}; " +
            "keyguard=${context.getSystemService(KeyguardManager::class.java).isKeyguardLocked}; " +
            "userUnlocked=${context.getSystemService(UserManager::class.java).isUserUnlocked}; " +
            "windowLabels=$labels"
    }

    private fun clickLabeledControl(node: AccessibilityNodeInfo): Boolean {
        // Compose exposes some labels as children of their clickable tab/row.
        // Use that actual ancestor's accessibility action, never guessed coordinates.
        var target: AccessibilityNodeInfo? = node
        repeat(12) {
            val current = target ?: return false
            if (current.packageName?.toString() != context.packageName || !current.isVisibleToUser) return false
            if (current.isClickable && current.isEnabled &&
                current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            target = current.parent
        }
        return false
    }

    private fun await(label: String, click: Boolean = false, scroll: Boolean = false) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            val found = rows().filter { labeled(it, label) }
            if (found.any { !click || clickLabeledControl(it) }) return
            if (scroll) rows().firstOrNull { it.isScrollable }
                ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            SystemClock.sleep(150)
        }
        throw AssertionError("Settings navigation: missing " + label + "; visible=" +
            rows().map { "${it.text}|${it.contentDescription}" } + "; " + windowDiagnostic())
    }

    private fun awaitAbsent(label: String) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (rows().none { labeled(it, label) }) return
            SystemClock.sleep(150)
        }
        throw AssertionError("Settings navigation: Back left the profile visible: " + label)
    }

    private fun awaitEditor(stage: String) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (rows().any { it.className?.toString() == "android.widget.EditText" && it.isEnabled }) return
            SystemClock.sleep(150)
        }
        throw AssertionError("Settings navigation: $stage editor missing; visible=" +
            rows().map { "${it.text}|${it.contentDescription}" } + "; " + windowDiagnostic())
    }

    private fun evidence(stage: String) {
        val folder = File(context.getExternalFilesDir(null), "settings-navigation").apply { mkdirs() }
        File(folder, "$stage.txt").writeText(windowDiagnostic() + "\n" +
            rows().joinToString("\n") { "${it.text}|${it.contentDescription}" })
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(folder, "$stage.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun onMain(action: () -> Unit) = instrumentation.runOnMainSync { action() }

    private fun rotate(activity: ActivityScenario<MainActivity>, landscape: Boolean) {
        activity.onActivity { it.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        val expected = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (context.resources.configuration.orientation != expected && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(150)
        assertEquals("Device must apply the requested player orientation", expected, context.resources.configuration.orientation)
    }

    private fun exercise(withPlayback: Boolean) {
        val fixture = File(context.cacheDir, "settings-navigation.wav")
        val before = ProfileRepository.state.value.profile
        val activity = ActivityScenario.launch(MainActivity::class.java)
        var connection: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
        var player: MediaController? = null
        try {
            assertEquals(Lifecycle.State.RESUMED, activity.state)
            if (withPlayback) {
                // Original PCM fixture, through the production MediaSession and decoder.
                val samples = 8_000 * 90
                val wav = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
                wav.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
                wav.putInt(16).putShort(1).putShort(1).putInt(8_000).putInt(16_000).putShort(2).putShort(16)
                wav.put("data".toByteArray()).putInt(samples * 2)
                repeat(samples) { wav.putShort((sin(it * 2.0 * Math.PI * 440 / 8_000) * 2_000).toInt().toShort()) }
                fixture.writeBytes(wav.array())
                val future = MediaController.Builder(context,
                    SessionToken(context, ComponentName(context, AurelisPlaybackService::class.java))).buildAsync()
                connection = future
                val controller = future.get(30, TimeUnit.SECONDS)
                player = controller
                onMain {
                    controller.setMediaItem(MediaItem.Builder().setMediaId("settings-navigation")
                        .setUri(Uri.fromFile(fixture)).setMimeType("audio/wav").build())
                    controller.repeatMode = Player.REPEAT_MODE_ONE
                    controller.prepare()
                    controller.play()
                }
                val deadline = SystemClock.elapsedRealtime() + 15_000
                var playing = false
                while (!playing && SystemClock.elapsedRealtime() < deadline) {
                    onMain { playing = controller.isPlaying && controller.currentPosition > 100 }
                    SystemClock.sleep(100)
                }
                assertTrue("Real playback must start before opening Settings", playing)
            }
            val settings = context.getString(R.string.navigation_settings)
            val library = context.getString(R.string.library)
            val prefix = if (withPlayback) "playing" else "idle"
            await(context.getString(R.string.settings_profile_action))
            activity.onActivity { assertTrue("Regression requires the hardware renderer", it.window.decorView.isHardwareAccelerated) }
            assertTrue("Regression must retain Liquid Glass", SettingsRepository.liquidGlass.value)
            assertFalse("Regression must not disable dynamic blur", SettingsRepository.reduceDynamicBlur.value)
            await(context.getString(R.string.settings_profile_action), click = true)
            await(settings)
            await(before.name)
            evidence("$prefix-profile")
            activity.recreate()
            await(settings)
            await(before.name)
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            awaitAbsent(context.getString(R.string.settings_photo_hint))
            await(library, click = true)
            await(context.getString(R.string.settings_profile_action), click = true)
            await(settings)
            await(before.name)
            evidence("$prefix-library-settings")
            activity.recreate()
            await(before.name)
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            awaitAbsent(context.getString(R.string.settings_photo_hint))
            await(library)
            await(context.getString(R.string.new_playlist), click = true, scroll = true)
            awaitEditor("New playlist")
            evidence("$prefix-new-playlist")
            await(context.getString(R.string.cancel), click = true)
            if (withPlayback) {
                await(context.getString(R.string.player_open), click = true)
                await(context.getString(R.string.player_pause))
                await(context.getString(R.string.player_next))
                await(context.getString(R.string.player_previous))
                evidence("$prefix-player-portrait")
                await(context.getString(R.string.player_pause), click = true)
                await(context.getString(R.string.player_play))
                rotate(activity, true)
                await(context.getString(R.string.player_play))
                await(context.getString(R.string.player_next))
                await(context.getString(R.string.player_previous))
                evidence("$prefix-player-landscape")
                await(context.getString(R.string.player_play), click = true)
                await(context.getString(R.string.player_pause))
                rotate(activity, false)
                await(context.getString(R.string.player_pause))
                await(context.getString(R.string.player_close), click = true)
                await(context.getString(R.string.player_open))
            }
            await(context.getString(R.string.search), click = true)
            awaitEditor("Search")
            evidence("$prefix-search")
            activity.recreate()
            awaitEditor("Search")
            await(context.getString(R.string.home), click = true)
            await(context.getString(R.string.settings_profile_action))
            assertEquals("Opening Settings must preserve the single local profile", before, ProfileRepository.state.value.profile)
            assertEquals(Lifecycle.State.RESUMED, activity.state)
            if (withPlayback) onMain {
                assertTrue("Settings and recreation must preserve playback", player!!.isPlaying)
                assertEquals("settings-navigation", player!!.currentMediaItem?.mediaId)
            }
        } finally {
            runCatching { evidence(if (withPlayback) "playing-final" else "idle-final") }
            player?.let { controller -> onMain { controller.stop(); controller.clearMediaItems() } }
            connection?.let { future -> onMain { MediaController.releaseFuture(future) } }
            activity.close()
            fixture.delete()
        }
    }

    @Test fun profileAndLibrarySettingsSurviveRecreation() = exercise(false)
    @Test fun profileAndLibrarySettingsPreserveRealPlayback() = exercise(true)
}
