package com.music.aurelis

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.music.aurelis.playback.smart.AudioDecoder
import com.music.aurelis.playback.smart.LocalAudioSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
