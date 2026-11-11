// Aurelis © 2026. GPL-3.0-or-later.
// Regression for Media3's shared zero-length input buffers in the DSP pipeline.
package com.music.aurelis.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.music.aurelis.AurelisApplication
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

    private fun processor() = DspChainAudioProcessor(DspChain()).also {
        it.configure(AudioProcessor.AudioFormat(22_050, 1, C.ENCODING_PCM_16BIT))
        it.flush()
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
    fun nonEmptyPartialPcmFrameIsForwardedRatherThanLost() {
        val processor = processor()
        try {
            val partial = ByteBuffer.allocateDirect(1)
            partial.put(0x35.toByte())
            partial.flip()
            processor.queueInput(partial)
            val output = processor.getOutput()
            assertEquals(1, output.remaining())
            assertEquals(0x35, output.get().toInt() and 0xFF)
            assertFalse(partial.hasRemaining())
        } finally {
            processor.reset()
        }
    }
}
