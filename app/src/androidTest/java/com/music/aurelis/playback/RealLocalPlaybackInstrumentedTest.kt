// Aurelis © 2026. GPL-3.0-or-later.
// P3: real Android audio decode through AurelisPlaybackService, not mocked Media3.
package com.music.aurelis.playback

import android.content.ComponentName
import android.net.Uri
import android.util.Log
import com.music.aurelis.MainActivity
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
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
        assertTrue(
            "P3 requires an original readable PCM WAV fixture",
            fixture.isFile && fixture.canRead() && fixture.length() == 44L + 22_050L * 4L * 2L,
        )
        // Android 15+ restricts audio focus to a top app or foreground service.
        // Starting a MediaSessionService from am instrument is not proof that
        // the target package is foreground; the prior runs received focus loss
        // (Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS).
        //
        // Keep Aurelis's real UI resumed during this test, without disabling
        // or mocking ExoPlayer's production audio-focus policy.
        val foreground = ActivityScenario.launch(MainActivity::class.java)
        val future = MediaController.Builder(
            context, SessionToken(context, ComponentName(context, AurelisPlaybackService::class.java))
        ).buildAsync()
        try {
            assertEquals("P3: Aurelis must be foreground to request audio focus on API 35",
                Lifecycle.State.RESUMED, foreground.state)
            foreground.onActivity { activity ->
                Log.d("P3Media3Test", "Aurelis MainActivity RESUMED; windowFocused=" +
                    activity.hasWindowFocus())
            }
            val controller = future.get(30, TimeUnit.SECONDS)
            val listener = object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    Log.d("P3Media3Test", "onPlayWhenReadyChanged=" + playWhenReady +
                        " reason=" + reason + " (user=1 focus=2 noisy=3 remote=4 end=5)")
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    Log.d("P3Media3Test", "onPlaybackStateChanged=" + playbackState)
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("P3Media3Test", "onPlayerError=" + error.errorCodeName, error)
                }
            }
            fun onMain(action: () -> Unit) {
                instrumentation.runOnMainSync { action() }
            }
            fun snapshot(): String {
                var state = ""
                onMain {
                    val error = controller.playerError
                    state = "state=" + controller.playbackState +
                        ", isPlaying=" + controller.isPlaying +
                        ", playWhenReady=" + controller.playWhenReady +
                        ", suppression=" + controller.playbackSuppressionReason +
                        ", positionMs=" + controller.currentPosition +
                        ", durationMs=" + controller.duration +
                        ", mediaItems=" + controller.mediaItemCount +
                        ", mediaScheme=" + controller.currentMediaItem?.localConfiguration?.uri?.scheme +
                        ", error=" + error?.errorCodeName +
                        ", message=" + error?.message +
                        ", cause=" + error?.cause?.javaClass?.simpleName +
                        ": " + error?.cause?.message
                }
                return state
            }
            fun until(timeoutMs: Long, predicate: (MediaController) -> Boolean): Boolean {
                val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
                var lastReported = 0L
                while (System.nanoTime() < deadline) {
                    var matched = false
                    onMain { matched = predicate(controller) }
                    if (matched) return true
                    val now = System.nanoTime()
                    if (now - lastReported >= TimeUnit.SECONDS.toNanos(2)) {
                        Log.d("P3Media3Test", snapshot())
                        lastReported = now
                    }
                    Thread.sleep(100)
                }
                Log.e("P3Media3Test", "Timeout waiting for playback: " + snapshot())
                return false
            }
            onMain {
                controller.addListener(listener)
                controller.setMediaItem(
                    MediaItem.Builder()
                        .setUri(Uri.fromFile(fixture))
                        .setMimeType("audio/wav")
                        .build()
                )
                controller.prepare()
            }
            // Wait for Media3 to prepare before issuing play through the remote controller.
            assertTrue(
                "P3: WAV decoder did not reach READY (see P3Media3Test logcat)",
                until(15_000) {
                    it.playbackState == Player.STATE_READY &&
                        it.duration in 3_500L..4_500L && it.playerError == null
                },
            )
            onMain { controller.play() }
            assertTrue(
                "P3: Media3 READY but play command did not advance (see reason logs)",
                until(15_000) {
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
            foreground.close()
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
