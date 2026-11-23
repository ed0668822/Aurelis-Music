package com.music.aurelis

import android.content.ContextWrapper
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.music.aurelis.playback.AudioCache
import com.music.aurelis.playback.smart.AnalysisStore
import com.music.aurelis.playback.smart.AudioDecoder
import com.music.aurelis.playback.smart.AutomixAnalysisSource
import com.music.aurelis.playback.smart.LocalAudioSource
import com.music.aurelis.playback.smart.TrackAnalysis
import com.music.aurelis.playback.smart.TrackAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@UnstableApi
@RunWith(AndroidJUnit4::class)
class LocalAutomixAudioInstrumentedTest {
    @Test fun localWavCanBeReadAndReplacementGetsANewRevision() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File.createTempFile("automix-test", ".dir", context.cacheDir).apply {
            check(delete() && mkdir())
        }
        try {
            val audio = File(directory, "track.wav").apply { writeBytes(wave(8_000)) }
            val uri = Uri.fromFile(audio)
            val firstRevision = LocalAudioSource.revision(context.contentResolver, uri)
            assertNotNull(firstRevision)

            LocalAudioSource.Recording(context, uri) { false }.use { recording ->
                val pcm = checkNotNull(recording.open()).use { source ->
                    checkNotNull(AudioDecoder.decodeRegion(source, 0.0, 1.0)).first
                }
                assertEquals(8_000.0, pcm.sampleRate, 0.0)
                assertEquals(8_000, pcm.samples.size)
                assertTrue(pcm.samples.any { it > 0.2f })
            }

            val replacement = File(directory, "replacement.wav").apply { writeBytes(wave(4_000)) }
            check(audio.delete() && replacement.renameTo(audio))
            assertNotEquals(firstRevision, LocalAudioSource.revision(context.contentResolver, uri))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun savedPairRestoresWhileTheDecoderIsBusy() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File.createTempFile("automix-restore", ".dir", context.cacheDir).apply {
            check(delete() && mkdir())
        }
        val analysisContext = object : ContextWrapper(context) {
            override fun getFilesDir(): File = directory
        }
        val executor = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
        val decoderStarted = CountDownLatch(1)
        val finishDecode = CountDownLatch(1)
        val analyzer = TrackAnalyzer(analysisContext, AudioCache, executor)
        try {
            val store = AnalysisStore { directory }
            val recordings = mapOf("outgoing" to 112.0, "incoming" to 120.0).mapValues { (id, bpm) ->
                val audio = File(directory, "$id.wav").apply { writeBytes(wave(8_000)) }
                val uri = Uri.fromFile(audio)
                val revision = checkNotNull(LocalAudioSource.revision(context.contentResolver, uri))
                val key = AutomixAnalysisSource.localStorageKey(id, uri.toString(), revision)
                store.save(key, TrackAnalysis(status = TrackAnalysis.STATUS_READY, duration = 1.0, bpm = bpm))
                uri
            }
            executor.submit {
                decoderStarted.countDown()
                finishDecode.await()
            }
            assertTrue(decoderStarted.await(5, TimeUnit.SECONDS))

            val outgoing = TrackAnalyzer.Request("outgoing", recordings.getValue("outgoing"), 1.0)
            val incoming = TrackAnalyzer.Request("incoming", recordings.getValue("incoming"), 1.0)
            val deadline = SystemClock.elapsedRealtime() + 5_000
            while (recordings.any { (id, uri) -> !analyzer.analysisFor(id, uri).isUsable } &&
                SystemClock.elapsedRealtime() < deadline
            ) {
                analyzer.requestPair(outgoing, incoming, allowParallel = true)
                SystemClock.sleep(10)
            }

            assertEquals(112.0, analyzer.analysisFor("outgoing", recordings.getValue("outgoing")).bpm, 0.0)
            assertEquals(120.0, analyzer.analysisFor("incoming", recordings.getValue("incoming")).bpm, 0.0)
            analyzer.requestPair(outgoing, incoming, allowParallel = true)
            assertTrue("A cache hit must not queue decoding", executor.queue.isEmpty())
        } finally {
            finishDecode.countDown()
            analyzer.release()
            directory.deleteRecursively()
        }
    }

    private fun wave(frames: Int): ByteArray {
        val dataSize = frames * Short.SIZE_BYTES
        return ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(8_000)
            putInt(16_000)
            putShort(2)
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
            repeat(frames) { putShort((if (it % 16 < 8) 10_000 else -10_000).toShort()) }
        }.array()
    }
}
