// Aurelis © 2026. GPL-3.0-or-later.
// P3.3: natural end-of-track transition and repeat-one under real Android Media3.
// Does not measure sample continuity (gapless), crossfade, or audible hardware output.
package com.music.aurelis.playback

import android.content.ComponentName
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
class RealAutoRepeatInstrumentedTest {
    @Test
    fun naturalTrackEndAdvancesAndRepeatOneLoopsOnRealMediaSession() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val first = File(context.cacheDir, "p3-auto-a.wav")
        val second = File(context.cacheDir, "p3-auto-b.wav")
        makeOriginalWav(first, 440.0)
        makeOriginalWav(second, 550.0)
        val foreground = ActivityScenario.launch(MainActivity::class.java)
        val future = MediaController.Builder(
            context, SessionToken(context, ComponentName(context, AurelisPlaybackService::class.java))
        ).buildAsync()
        try {
            assertEquals("P3.3 requires top app to obtain audio focus on Android 15",
                Lifecycle.State.RESUMED, foreground.state)
            val controller = future.get(30, TimeUnit.SECONDS)
            val autoEvents = AtomicInteger(0)
            val repeatEvents = AtomicInteger(0)
            val listener = object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    Log.d("P3Media3Test", "P3.3 transition reason=" + reason +
                        " id=" + mediaItem?.mediaId)
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) autoEvents.incrementAndGet()
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) repeatEvents.incrementAndGet()
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("P3Media3Test", "P3.3 failure: " + error.errorCodeName, error)
                }
            }
            fun onMain(action: () -> Unit) {
                instrumentation.runOnMainSync(action)
            }
            fun describe(): String {
                var s = ""
                onMain {
                    s = "state=" + controller.playbackState + " playing=" + controller.isPlaying +
                        " playWhenReady=" + controller.playWhenReady +
                        " index=" + controller.currentMediaItemIndex +
                        " id=" + controller.currentMediaItem?.mediaId +
                        " positionMs=" + controller.currentPosition +
                        " autoEvents=" + autoEvents.get() + " repeatEvents=" + repeatEvents.get() +
                        " error=" + controller.playerError?.errorCodeName
                }
                return s
            }
            fun until(name: String, timeoutMs: Long = 20_000, predicate: (MediaController) -> Boolean) {
                val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
                var lastLog = 0L
                while (System.nanoTime() < deadline) {
                    var ok = false
                    onMain { ok = predicate(controller) }
                    if (ok) return
                    val now = System.nanoTime()
                    if (now - lastLog >= TimeUnit.SECONDS.toNanos(2)) {
                        Log.d("P3Media3Test", "P3.3 wait " + name + ": " + describe())
                        lastLog = now
                    }
                    Thread.sleep(100)
                }
                throw AssertionError("P3.3 " + name + " not reached: " + describe())
            }
            fun item(file: File, id: String) = MediaItem.Builder()
                .setMediaId(id).setUri(Uri.fromFile(file)).setMimeType("audio/wav").build()
            onMain {
                controller.addListener(listener)
                controller.repeatMode = Player.REPEAT_MODE_OFF
                controller.shuffleModeEnabled = false
                controller.setMediaItems(listOf(item(first, "auto-a"), item(second, "auto-b")))
                controller.prepare()
            }
            until("ready first WAV") {
                it.playbackState == Player.STATE_READY &&
                    it.mediaItemCount == 2 &&
                    it.currentMediaItem?.mediaId == "auto-a" &&
                    it.duration in 2_500L..3_500L &&
                    it.playerError == null
            }
            onMain { controller.play() }
            until("first song advancing") {
                it.currentMediaItem?.mediaId == "auto-a" &&
                    it.isPlaying && it.currentPosition > 150
            }
            // No manual seek/next: an AUTO event must actually be emitted.
            until("natural transition from first to second WAV", 20_000) {
                it.currentMediaItem?.mediaId == "auto-b" &&
                    it.currentMediaItemIndex == 1 &&
                    autoEvents.get() >= 1 &&
                    it.playerError == null
            }
            until("second WAV ends without playlist repeat", 20_000) {
                it.playbackState == Player.STATE_ENDED &&
                    !it.isPlaying && it.currentMediaItem?.mediaId == "auto-b"
            }
            // Repeat-one must cause a true MEDIA_ITEM_TRANSITION_REASON_REPEAT
            // after natural completion, not merely stay on the same media id.
            onMain {
                controller.repeatMode = Player.REPEAT_MODE_ONE
                controller.seekTo(1, 0)
                controller.prepare()
            }
            until("repeat-one WAV ready") {
                it.playbackState == Player.STATE_READY &&
                    it.currentMediaItem?.mediaId == "auto-b" &&
                    it.repeatMode == Player.REPEAT_MODE_ONE
            }
            onMain { controller.play() }
            until("repeat-one playback advancing") {
                it.isPlaying && it.currentPosition > 150
            }
            until("repeat-one automatic loop event", 20_000) {
                repeatEvents.get() >= 1 &&
                    it.repeatMode == Player.REPEAT_MODE_ONE &&
                    it.currentMediaItem?.mediaId == "auto-b" &&
                    it.playbackState != Player.STATE_ENDED &&
                    it.playerError == null
            }
            onMain {
                controller.pause()
                controller.repeatMode = Player.REPEAT_MODE_OFF
                controller.removeListener(listener)
                controller.stop()
                controller.clearMediaItems()
            }
        } finally {
            instrumentation.runOnMainSync { MediaController.releaseFuture(future) }
            foreground.close()
            first.delete()
            second.delete()
        }
    }

    private fun makeOriginalWav(file: File, frequency: Double) {
        val sampleRate = 22_050
        val frames = sampleRate * 3
        val bytes = frames * 2
        val wav = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
        wav.put("RIFF".toByteArray(Charsets.US_ASCII))
        wav.putInt(36 + bytes)
        wav.put("WAVE".toByteArray(Charsets.US_ASCII))
        wav.put("fmt ".toByteArray(Charsets.US_ASCII))
        wav.putInt(16)
        wav.putShort(1)
        wav.putShort(1)
        wav.putInt(sampleRate)
        wav.putInt(sampleRate * 2)
        wav.putShort(2)
        wav.putShort(16)
        wav.put("data".toByteArray(Charsets.US_ASCII))
        wav.putInt(bytes)
        repeat(frames) { frame ->
            wav.putShort((sin(2.0 * PI * frequency * frame / sampleRate) * 2_000).toInt().toShort())
        }
        file.outputStream().use { it.write(wav.array()) }
        assertTrue("Generated WAV fixture must be complete", file.canRead() && file.length() == (44 + bytes).toLong())
    }
}
