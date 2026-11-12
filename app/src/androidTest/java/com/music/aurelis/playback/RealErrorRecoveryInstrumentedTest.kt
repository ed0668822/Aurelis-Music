// Aurelis © 2026. GPL-3.0-or-later.
// P3.4: a missing local file must raise a real Media3 playback error; the
// same MediaSession must then recover and decode a valid, original WAV.
// Not a substitute for external storage permission or physical audio testing.
package com.music.aurelis.playback

import android.content.ComponentName
import android.net.Uri
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.music.aurelis.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class RealErrorRecoveryInstrumentedTest {
    @Test
    fun missingLocalWavReportsErrorThenSameSessionRecoversToValidWav() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val missing = File(context.cacheDir, "p3-missing-does-not-exist.wav")
        assertFalse("The missing track fixture must actually be absent", missing.exists())
        val valid = File(context.cacheDir, "p3-recovery-valid.wav")
        originalWav(valid)
        val foreground = ActivityScenario.launch(MainActivity::class.java)
        val future = MediaController.Builder(
            context, SessionToken(context, ComponentName(context, AurelisPlaybackService::class.java)),
        ).buildAsync()
        try {
            assertEquals("Android 15 requires a foreground app to request audio focus",
                Lifecycle.State.RESUMED, foreground.state)
            val controller = future.get(30, TimeUnit.SECONDS)
            val errors = AtomicInteger(0)
            val listener = object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.d("P3Media3Test", "P3.4 expected missing-file error: " +
                        error.errorCodeName + " " + error.message)
                    errors.incrementAndGet()
                }
            }
            fun main(action: () -> Unit) {
                instrumentation.runOnMainSync { action() }
            }
            fun describe(): String {
                var result = ""
                main {
                    result = "state=" + controller.playbackState +
                        " playing=" + controller.isPlaying +
                        " ready=" + controller.playWhenReady +
                        " id=" + controller.currentMediaItem?.mediaId +
                        " position=" + controller.currentPosition +
                        " error=" + controller.playerError?.errorCodeName +
                        " events=" + errors.get()
                }
                return result
            }
            fun until(stage: String, timeoutMs: Long = 15_000, predicate: (MediaController) -> Boolean) {
                val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
                var lastLogged = 0L
                while (System.nanoTime() < deadline) {
                    var match = false
                    main { match = predicate(controller) }
                    if (match) return
                    val now = System.nanoTime()
                    if (now - lastLogged >= TimeUnit.SECONDS.toNanos(2)) {
                        Log.d("P3Media3Test", "P3.4 waiting " + stage + ": " + describe())
                        lastLogged = now
                    }
                    Thread.sleep(100)
                }
                throw AssertionError("P3.4 " + stage + " timed out: " + describe())
            }
            fun item(file: File, id: String) = MediaItem.Builder()
                .setMediaId(id).setUri(Uri.fromFile(file)).setMimeType("audio/wav").build()
            main {
                controller.addListener(listener)
                controller.repeatMode = Player.REPEAT_MODE_OFF
                controller.setMediaItem(item(missing, "p3-missing"))
                controller.prepare()
            }
            // A missing on-disk file must not be counted as successfully decoded.
            until("missing local file reported a decoder/data-source error") {
                it.playerError != null && errors.get() >= 1 &&
                    it.currentMediaItem?.mediaId == "p3-missing"
            }
            main {
                // An explicit new playable selection should recover without
                // recreating the service, session or MediaController.
                controller.setMediaItem(item(valid, "p3-recovered"))
                controller.prepare()
            }
            until("valid WAV ready after local file error") {
                it.playerError == null &&
                    it.playbackState == Player.STATE_READY &&
                    it.currentMediaItem?.mediaId == "p3-recovered" &&
                    it.duration in 3_500L..4_500L
            }
            main { controller.play() }
            until("recovery WAV actually advancing") {
                it.playerError == null &&
                    it.currentMediaItem?.mediaId == "p3-recovered" &&
                    it.isPlaying && it.currentPosition > 350
            }
            main {
                controller.pause()
                controller.stop()
                controller.clearMediaItems()
                controller.removeListener(listener)
            }
        } finally {
            instrumentation.runOnMainSync { MediaController.releaseFuture(future) }
            foreground.close()
            valid.delete()
        }
    }

    private fun originalWav(file: File) {
        val sampleRate = 22_050
        val frames = sampleRate * 4
        val pcmBytes = frames * 2
        val buffer = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + pcmBytes)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(1)
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcmBytes)
        repeat(frames) { i ->
            buffer.putShort((sin(2.0 * PI * 440.0 * i / sampleRate) * 2_000).toInt().toShort())
        }
        file.outputStream().use { it.write(buffer.array()) }
        assertTrue("Valid WAV fixture must be readable", file.canRead() && file.length() == (44 + pcmBytes).toLong())
    }
}
