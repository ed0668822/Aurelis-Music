// Aurelis © 2026. GPL-3.0-or-later.
// P3: real Android audio decode through AurelisPlaybackService, not mocked Media3.
package com.music.aurelis.playback

import android.content.ComponentName
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class RealLocalPlaybackInstrumentedTest {
    @Test
    fun generatedPcmWavPlaysPausesSeeksAndResumesThroughMediaSession() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val fixture = File(context.cacheDir, "p3-original-tone.wav")
        originalWav(fixture)
        val future = MediaController.Builder(
            context, SessionToken(context, ComponentName(context, AurelisPlaybackService::class.java))
        ).buildAsync()
        try {
            val controller = future.get(30, TimeUnit.SECONDS)
            fun onMain(action: () -> Unit) {
                instrumentation.runOnMainSync { action() }
            }
            fun until(timeoutMs: Long, predicate: (MediaController) -> Boolean): Boolean {
                val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
                while (System.nanoTime() < deadline) {
                    var matched = false
                    onMain { matched = predicate(controller) }
                    if (matched) return true
                    Thread.sleep(100)
                }
                return false
            }
            onMain {
                controller.setMediaItem(MediaItem.fromUri(Uri.fromFile(fixture)))
                controller.prepare()
                controller.play()
            }
            assertTrue(
                "P3: local PCM WAV must reach READY and playback position must advance",
                until(20_000) {
                    it.playbackState == Player.STATE_READY &&
                        it.isPlaying && it.currentPosition > 350
                },
            )
            onMain { controller.pause() }
            assertTrue("P3: pause must stop actual Media3 playback",
                until(5_000) { !it.isPlaying })
            onMain { controller.seekTo(250) }
            assertTrue("P3: paused seek must change position",
                until(5_000) { !it.isPlaying && it.currentPosition in 150L..450L })
            onMain { controller.play() }
            assertTrue("P3: resume must advance same WAV",
                until(8_000) { it.isPlaying && it.currentPosition > 650 })
            onMain {
                controller.stop()
                controller.clearMediaItems()
            }
        } finally {
            instrumentation.runOnMainSync { MediaController.releaseFuture(future) }
            fixture.delete()
        }
    }

    private fun originalWav(file: File) {
        val rate = 22_050
        val frames = rate * 4
        val size = frames * 2
        val wav = ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN)
        wav.put("RIFF".toByteArray(Charsets.US_ASCII))
        wav.putInt(36 + size)
        wav.put("WAVE".toByteArray(Charsets.US_ASCII))
        wav.put("fmt ".toByteArray(Charsets.US_ASCII))
        wav.putInt(16)
        wav.putShort(1)
        wav.putShort(1)
        wav.putInt(rate)
        wav.putInt(rate * 2)
        wav.putShort(2)
        wav.putShort(16)
        wav.put("data".toByteArray(Charsets.US_ASCII))
        wav.putInt(size)
        repeat(frames) { frame ->
            val sample = (sin(2.0 * PI * 440.0 * frame / rate) * 2_000).toInt()
            wav.putShort(sample.toShort())
        }
        file.outputStream().use { it.write(wav.array()) }
    }
}
