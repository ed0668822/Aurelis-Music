// Aurelis © 2026. GPL-3.0-or-later.
// P3.2: two generated WAVs through the real MediaSessionService and decoder.
// This tests queue operations; it does NOT claim sample-exact gapless playback.
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
import kotlin.math.PI
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class RealQueueTransitionsInstrumentedTest {
    @Test
    fun twoOriginalWavsNextPreviousAndQueueRemovalUseActualMediaSession() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val a = File(context.cacheDir, "p3-queue-a.wav")
        val b = File(context.cacheDir, "p3-queue-b.wav")
        writeOriginalWav(a, 440.0)
        writeOriginalWav(b, 659.25)
        val activity = ActivityScenario.launch(MainActivity::class.java)
        val connection = MediaController.Builder(
            context, SessionToken(context, ComponentName(context, AurelisPlaybackService::class.java)),
        ).buildAsync()
        try {
            assertEquals("Foreground audio focus is required on Android 15",
                Lifecycle.State.RESUMED, activity.state)
            val player = connection.get(30, TimeUnit.SECONDS)
            fun onMain(block: () -> Unit) {
                instrumentation.runOnMainSync { block() }
            }
            fun state(): String {
                var description = ""
                onMain {
                    description = "state=" + player.playbackState +
                        " playing=" + player.isPlaying +
                        " playWhenReady=" + player.playWhenReady +
                        " index=" + player.currentMediaItemIndex +
                        " count=" + player.mediaItemCount +
                        " mediaId=" + player.currentMediaItem?.mediaId +
                        " posMs=" + player.currentPosition +
                        " error=" + player.playerError?.errorCodeName
                }
                return description
            }
            fun awaitState(name: String, timeoutMs: Long = 15_000, accept: (MediaController) -> Boolean) {
                val limit = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
                var lastLog = 0L
                while (System.nanoTime() < limit) {
                    var ok = false
                    onMain { ok = accept(player) }
                    if (ok) return
                    val now = System.nanoTime()
                    if (now - lastLog > TimeUnit.SECONDS.toNanos(2)) {
                        Log.d("P3Media3Test", "queue wait " + name + ": " + state())
                        lastLog = now
                    }
                    Thread.sleep(100)
                }
                throw AssertionError("P3.2 " + name + " timed out: " + state())
            }
            fun media(file: File, id: String) = MediaItem.Builder()
                .setMediaId(id)
                .setUri(Uri.fromFile(file))
                .setMimeType("audio/wav")
                .build()
            onMain {
                player.setMediaItems(listOf(media(a, "p3-first"), media(b, "p3-second")))
                player.prepare()
            }
            awaitState("initial WAV READY") {
                it.playbackState == Player.STATE_READY &&
                    it.mediaItemCount == 2 &&
                    it.currentMediaItemIndex == 0 &&
                    it.duration in 3_500L..4_500L &&
                    it.playerError == null
            }
            onMain { player.play() }
            awaitState("first WAV playback") {
                it.currentMediaItem?.mediaId == "p3-first" &&
                    it.isPlaying && it.currentPosition > 250
            }
            onMain { player.seekToNextMediaItem() }
            awaitState("next track") {
                it.currentMediaItemIndex == 1 &&
                    it.currentMediaItem?.mediaId == "p3-second" &&
                    it.playbackState == Player.STATE_READY &&
                    it.isPlaying && it.currentPosition > 250
            }
            onMain { player.pause() }
            awaitState("pause second track") {
                !it.isPlaying && it.currentMediaItemIndex == 1
            }
            // Previous must select the preceding media item rather than merely
            // seeking within the same one. Test immediately after next (<3 s).
            onMain { player.seekToPreviousMediaItem() }
            awaitState("previous track") {
                it.currentMediaItemIndex == 0 &&
                    it.currentMediaItem?.mediaId == "p3-first" &&
                    it.mediaItemCount == 2
            }
            onMain { player.removeMediaItem(0) }
            awaitState("current item removal preserves second track") {
                it.mediaItemCount == 1 &&
                    it.currentMediaItemIndex == 0 &&
                    it.currentMediaItem?.mediaId == "p3-second"
            }
            onMain { player.play() }
            awaitState("resume after queue edit") {
                it.isPlaying &&
                    it.currentMediaItem?.mediaId == "p3-second" &&
                    it.currentPosition > 250
            }
            onMain {
                player.stop()
                player.clearMediaItems()
            }
        } finally {
            instrumentation.runOnMainSync { MediaController.releaseFuture(connection) }
            activity.close()
            a.delete()
            b.delete()
        }
    }

    private fun writeOriginalWav(file: File, frequency: Double) {
        val sampleRate = 22_050
        val frames = sampleRate * 4
        val dataSize = frames * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
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
        buffer.putInt(dataSize)
        repeat(frames) { i ->
            buffer.putShort((sin(2.0 * PI * frequency * i / sampleRate) * 2_000).toInt().toShort())
        }
        file.outputStream().use { it.write(buffer.array()) }
        assertTrue("Generated local WAV must be readable", file.canRead() && file.length() == (44 + dataSize).toLong())
    }
}
