// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import android.content.ComponentName
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
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
        listOf(node.text, node.contentDescription).any { it?.toString()?.equals(text, ignoreCase = true) == true }

    private fun await(label: String, click: Boolean = false) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            val node = rows().firstOrNull { labeled(it, label) && (!click || it.isClickable && it.isEnabled) }
            if (node != null && (!click || node.performAction(AccessibilityNodeInfo.ACTION_CLICK))) return
            SystemClock.sleep(150)
        }
        throw AssertionError("Settings navigation: missing " + label + "; visible=" +
            rows().map { "${it.text}|${it.contentDescription}" })
    }

    private fun evidence(stage: String) {
        val folder = File(context.getExternalFilesDir(null), "settings-navigation").apply { mkdirs() }
        File(folder, "$stage.txt").writeText(rows().joinToString("\n") { "${it.text}|${it.contentDescription}" })
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(folder, "$stage.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun onMain(action: () -> Unit) = instrumentation.runOnMainSync { action() }

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
            await("Perfil", click = true)
            await(settings)
            await(before.name)
            evidence("$prefix-profile")
            activity.recreate()
            await(settings)
            await(before.name)
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            await(library, click = true)
            await(settings, click = true)
            await(before.name)
            evidence("$prefix-library-settings")
            activity.recreate()
            await(before.name)
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            await(library)
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
