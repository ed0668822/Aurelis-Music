// Aurelis © 2026. GPL-3.0-or-later.
// Regression for Media3's shared zero-length input buffers in the DSP pipeline.
package com.music.aurelis.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.AurelisApplication
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class DspChainAudioProcessorRegressionTest {

    private fun processor(channels: Int = 1, encoding: Int = C.ENCODING_PCM_16BIT) =
        DspChainAudioProcessor(DspChain()).also {
            it.configure(AudioProcessor.AudioFormat(22_050, channels, encoding))
            it.flush()
        }

    private fun direct(bytes: ByteArray): ByteBuffer =
        ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(bytes)
            flip()
        }

    @Test
    fun emptyInputCanBeQueuedRepeatedlyWithoutSelfCopy() {
        val processor = processor()
        try {
            repeat(4) {
                // AudioProcessor.EMPTY_BUFFER is shared by BaseAudioProcessor;
                // the old implementation called put() with this very same buffer.
                processor.queueInput(AudioProcessor.EMPTY_BUFFER)
                assertFalse(processor.getOutput().hasRemaining())
            }
            processor.queueInput(ByteBuffer.allocateDirect(0))
            assertFalse(processor.getOutput().hasRemaining())
        } finally {
            processor.reset()
        }
    }

    @Test
    fun emptyInputDoesNotDiscardSubsequentPcmFrames() {
        val processor = processor()
        try {
            processor.queueInput(AudioProcessor.EMPTY_BUFFER)
            assertFalse(processor.getOutput().hasRemaining())

            val input = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN)
            repeat(4) { input.putShort((1_000 + it * 100).toShort()) }
            input.flip()
            processor.queueInput(input)
            assertEquals(8, input.position())
            assertEquals(8, processor.getOutput().remaining())

            processor.queueInput(AudioProcessor.EMPTY_BUFFER)
            assertFalse(processor.getOutput().hasRemaining())
        } finally {
            processor.reset()
        }
    }

    @Test
    fun partialPcmFrameWaitsUntilSecondInputAndNeverLeaksToAudioSink() {
        val processor = processor()
        try {
            val first = direct(byteArrayOf(0x35))
            processor.queueInput(first)
            assertFalse(first.hasRemaining())
            assertFalse("A single byte is not a PCM16 frame", processor.getOutput().hasRemaining())

            val second = direct(byteArrayOf(0x12))
            processor.queueInput(second)
            assertFalse(second.hasRemaining())
            assertEquals("A complete PCM16 mono frame must be emitted", 2,
                processor.getOutput().remaining())
        } finally {
            processor.reset()
        }
    }

    @Test
    fun arbitraryChunkBoundariesPreservePacked24BitStereoAudioFrames() {
        val processor = processor(channels = 2, encoding = C.ENCODING_PCM_24BIT)
        try {
            val bytes = byteArrayOf(
                0x05, 0x00, 0x00, 0x06, 0x00, 0x00,
                0x07, 0x00, 0x00, 0x08, 0x00, 0x00,
                0x09, 0x00, 0x00, 0x0A, 0x00, 0x00,
                0x0B, 0x00, 0x00, 0x0C, 0x00, 0x00,
            )
            val chunks = listOf(1, 2, 3, 4, 5, 9)
            var offset = 0
            var totalOutput = 0
            val output = ByteArrayOutputStream()
            chunks.forEach { size ->
                val input = direct(bytes.copyOfRange(offset, offset + size))
                processor.queueInput(input)
                assertFalse("All input bytes should be consumed", input.hasRemaining())
                val produced = processor.getOutput()
                assertEquals("No partial PCM24 stereo frames may escape the DSP", 0,
                    produced.remaining() % 6)
                val chunk = ByteArray(produced.remaining())
                produced.get(chunk)
                totalOutput += chunk.size
                output.write(chunk)
                offset += size
            }
            assertEquals(bytes.size, offset)
            assertEquals(bytes.size, totalOutput)
            assertEquals(bytes.size, output.toByteArray().size)
        } finally {
            processor.reset()
        }
    }

    @Test
    fun flushAndEndOfStreamDiscardIncompletePcmFrames() {
        val processor = processor()
        try {
            processor.queueInput(direct(byteArrayOf(0x35)))
            assertFalse(processor.getOutput().hasRemaining())
            processor.flush()
            processor.queueInput(direct(byteArrayOf(0x46, 0x12)))
            assertEquals(2, processor.getOutput().remaining())

            processor.queueInput(direct(byteArrayOf(0x77)))
            assertFalse(processor.getOutput().hasRemaining())
            processor.queueEndOfStream()
            assertFalse(processor.getOutput().hasRemaining())
            processor.flush()
            processor.queueInput(direct(byteArrayOf(0x56, 0x34)))
            assertEquals(2, processor.getOutput().remaining())
        } finally {
            processor.reset()
        }
    }
}
