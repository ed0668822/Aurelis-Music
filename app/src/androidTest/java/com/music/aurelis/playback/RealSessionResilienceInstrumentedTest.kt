// Aurelis © 2026. GPL-3.0-or-later.
// P3.5–P3.7: actual Android Media3 session resilience, without mock players.
// The 12-second run is a bounded stability check, not a long-run hardware burn-in.
package com.music.aurelis.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.music.aurelis.MainActivity
import org.junit.Assert.assertEquals
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
class RealSessionResilienceInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    private fun onMain(action: () -> Unit) {
        instrumentation.runOnMainSync(action)
    }

    private fun state(controller: MediaController): String {
        var snapshot = ""
        onMain {
            snapshot = "state=" + controller.playbackState +
                " isPlaying=" + controller.isPlaying +
                " playWhenReady=" + controller.playWhenReady +
                " id=" + controller.currentMediaItem?.mediaId +
                " index=" + controller.currentMediaItemIndex +
                " count=" + controller.mediaItemCount +
                " pos=" + controller.currentPosition +
                " duration=" + controller.duration +
                " error=" + controller.playerError?.errorCodeName
        }
        return snapshot
    }

    private fun until(
        controller: MediaController, name: String, timeoutMs: Long = 15_000,
        condition: (MediaController) -> Boolean,
    ) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        var lastLogged = 0L
        while (System.nanoTime() < deadline) {
            var success = false
            onMain { success = condition(controller) }
            if (success) return
            val now = System.nanoTime()
            if (now - lastLogged >= TimeUnit.SECONDS.toNanos(2)) {
                Log.d("P3Media3Test", "Resilience " + name + ": " + state(controller))
                lastLogged = now
            }
            Thread.sleep(100)
        }
        throw AssertionError(name + " timeout: " + state(controller))
    }

    private fun withRealSession(
        block: (MediaController, ActivityScenario<MainActivity>) -> Unit,
    ) {
        val activity = ActivityScenario.launch(MainActivity::class.java)
        val future = MediaController.Builder(
            context, SessionToken(context, ComponentName(context, AurelisPlaybackService::class.java)),
        ).buildAsync()
        try {
            assertEquals("API 35 requires the app foreground for audio focus",
                Lifecycle.State.RESUMED, activity.state)
            val controller = future.get(30, TimeUnit.SECONDS)
            try {
                block(controller, activity)
            } finally {
                onMain {
                    controller.pause()
                    controller.repeatMode = Player.REPEAT_MODE_OFF
                    controller.shuffleModeEnabled = false
                    controller.stop()
                    controller.clearMediaItems()
                }
            }
        } finally {
            onMain { MediaController.releaseFuture(future) }
            activity.close()
        }
    }

    private fun track(file: File, id: String) = MediaItem.Builder()
        .setMediaId(id)
        .setUri(Uri.fromFile(file))
        .setMimeType("audio/wav")
        .build()

    @Test
    fun activityRecreationRetainsMediaSessionQueueAndAdvancingPosition() {
        val wav = File(context.cacheDir, "p3-recreation.wav")
        originalWav(wav, 12, 392.0)
        try {
            withRealSession { controller, activity ->
                onMain {
                    controller.setMediaItem(track(wav, "p3-recreation"))
                    controller.prepare()
                }
                until(controller, "initial READY") {
                    it.playbackState == Player.STATE_READY &&
                        it.currentMediaItem?.mediaId == "p3-recreation" &&
                        it.duration in 11_500L..12_500L &&
                        it.playerError == null
                }
                onMain { controller.play() }
                until(controller, "initial playback") {
                    it.isPlaying && it.currentPosition > 500
                }
                var before = 0L
                onMain { before = controller.currentPosition }
                activity.recreate()
                assertEquals("MainActivity recreated and resumed", Lifecycle.State.RESUMED, activity.state)
                until(controller, "same session after Activity.recreate", 12_000) {
                    it.isPlaying &&
                        it.currentMediaItem?.mediaId == "p3-recreation" &&
                        it.mediaItemCount == 1 &&
                        it.currentPosition > before + 300L &&
                        it.playerError == null
                }
            }
        } finally {
            wav.delete()
        }
    }

    @Test
    fun repeatAllCyclesFromSecondOriginalWavBackToFirst() {
        val first = File(context.cacheDir, "p3-repeat-all-a.wav")
        val second = File(context.cacheDir, "p3-repeat-all-b.wav")
        originalWav(first, 3, 330.0)
        originalWav(second, 3, 660.0)
        try {
            withRealSession { controller, _ ->
                val wrapEvents = AtomicInteger(0)
                val observer = object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                            mediaItem?.mediaId == "repeat-all-first"
                        ) wrapEvents.incrementAndGet()
                    }
                }
                onMain {
                    controller.addListener(observer)
                    controller.repeatMode = Player.REPEAT_MODE_ALL
                    controller.setMediaItems(listOf(
                        track(first, "repeat-all-first"),
                        track(second, "repeat-all-second"),
                    ))
                    controller.prepare()
                }
                until(controller, "repeat-all ready") {
                    it.playbackState == Player.STATE_READY &&
                        it.mediaItemCount == 2 && it.playerError == null
                }
                onMain { controller.play() }
                until(controller, "first repeat-all track advancing") {
                    it.isPlaying && it.currentMediaItemIndex == 0 &&
                        it.currentPosition > 100L
                }
                // A seek may leave Media3 IDLE while a decoder is switching.
                // Prepare explicitly, then demand actual resumed second-track
                // playback before measuring the natural repeat-all wrap.
                onMain {
                    controller.seekTo(1, 0L)
                    controller.prepare()
                }
                until(controller, "second repeat-all track READY") {
                    it.playbackState == Player.STATE_READY &&
                        it.currentMediaItem?.mediaId == "repeat-all-second" &&
                        it.playerError == null
                }
                onMain { controller.play() }
                until(controller, "second repeat-all track advancing") {
                    it.playbackState == Player.STATE_READY &&
                        it.isPlaying &&
                        it.currentMediaItem?.mediaId == "repeat-all-second" &&
                        it.currentPosition > 250L
                }
                // No seek-to-first: a natural Media3 AUTO transition must wrap.
                until(controller, "repeat-all natural wrap", 15_000) {
                    wrapEvents.get() > 0 &&
                        it.currentMediaItem?.mediaId == "repeat-all-first" &&
                        it.currentMediaItemIndex == 0 &&
                        it.isPlaying &&
                        it.playerError == null
                }
                onMain { controller.removeListener(observer) }
            }
        } finally {
            first.delete()
            second.delete()
        }
    }

    @Test
    fun continuousTwelveSecondDecodeAndSeekRemainStable() {
        val wav = File(context.cacheDir, "p3-continuous.wav")
        originalWav(wav, 18, 523.25)
        try {
            withRealSession { controller, _ ->
                onMain {
                    controller.setMediaItem(track(wav, "p3-continuous"))
                    controller.prepare()
                }
                until(controller, "continuous WAV READY") {
                    it.playbackState == Player.STATE_READY &&
                        it.duration in 17_500L..18_500L &&
                        it.playerError == null
                }
                onMain { controller.play() }
                until(controller, "first samples advance") {
                    it.isPlaying && it.currentPosition > 300
                }
                // Observe at least 12 s of actual decoder + DSP + AudioSink progress.
                until(controller, "12-second continuous decode", 17_000) {
                    it.isPlaying &&
                        it.currentMediaItem?.mediaId == "p3-continuous" &&
                        it.currentPosition > 12_000 &&
                        it.playerError == null
                }
                onMain {
                    controller.pause()
                    controller.seekTo(5_000L)
                }
                until(controller, "paused seek preserved") {
                    !it.isPlaying &&
                        it.currentPosition in 4_800L..5_300L &&
                        it.playerError == null
                }
                onMain { controller.play() }
                until(controller, "resume after long decode and seek") {
                    it.isPlaying && it.currentPosition > 5_400
                }
            }
        } finally {
            wav.delete()
        }
    }

    private fun originalWav(file: File, seconds: Int, frequency: Double) {
        val sampleRate = 22_050
        val frames = sampleRate * seconds
        val pcmSize = frames * 2
        val buffer = ByteBuffer.allocate(44 + pcmSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + pcmSize)
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
        buffer.putInt(pcmSize)
        repeat(frames) { i ->
            buffer.putShort((sin(2.0 * PI * frequency * i / sampleRate) * 2_000).toInt().toShort())
        }
        file.outputStream().use { it.write(buffer.array()) }
        assertTrue("PCM fixture must exist", file.isFile && file.canRead() &&
            file.length() == (44 + pcmSize).toLong())
    }
}
