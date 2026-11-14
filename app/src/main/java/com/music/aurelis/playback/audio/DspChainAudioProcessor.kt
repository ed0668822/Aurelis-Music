// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The bridge between ExoPlayer's ByteBuffer world and Aurelis's Float32 DSP.
 *
 * ExoPlayer feeds AudioProcessor.queueInput with interleaved 16-bit PCM in a
 * ByteBuffer. Aurelis's DspChain (equalizer, spatial, transition, loudness)
 * works on AudioBlock — Float32 in-place, with reverb tails and filter state
 * that have to survive between blocks. So this decodes, runs, and re-encodes:
 * one AudioBlock reused across calls, one DspChain instance for the lifetime
 * of the renderer.
 *
 * Not an AudioSink: the sink stays DefaultAudioSink, so the whole existing
 * audio pipeline (AudioTrack, float output, offload rejection) is unchanged.
 * This just sits inside setAudioProcessors.
 */
@UnstableApi
class DspChainAudioProcessor(
    private val chain: DspChain,
) : BaseAudioProcessor() {

    // AudioBlock is not thread-safe and doesn't grow; it's allocated once here
    // sized for the largest block ExoPlayer can hand us.
    private var block: AudioBlock = AudioBlock()
    private var inputEncoding: PcmEncoding = PcmEncoding.PCM_16BIT
    private var outputEncoding: PcmEncoding = PcmEncoding.PCM_16BIT
    private var configured = false
    // PCM chunk boundaries need not coincide with complete audio frames.
    // Carry at most one partial frame between calls, allocated only at configure.
    private var pendingFrameBytes = ByteArray(0)
    private var pendingByteCount = 0
    private var frameScratch: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.LITTLE_ENDIAN)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val encoding = when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> PcmEncoding.PCM_16BIT
            C.ENCODING_PCM_24BIT -> PcmEncoding.PCM_24BIT_PACKED
            C.ENCODING_PCM_32BIT -> PcmEncoding.PCM_32BIT
            C.ENCODING_PCM_FLOAT -> PcmEncoding.PCM_FLOAT
            else -> return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount <= 0 || inputAudioFormat.sampleRate <= 0) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        inputEncoding = encoding
        outputEncoding = encoding
        val bytesPerFrame = encoding.bytesPerFrame(inputAudioFormat.channelCount)
        pendingFrameBytes = ByteArray(bytesPerFrame)
        frameScratch = ByteBuffer.allocateDirect(bytesPerFrame).order(ByteOrder.LITTLE_ENDIAN)
        pendingByteCount = 0
        if (!configured || block.channelCount != inputAudioFormat.channelCount) {
            block = AudioBlock(channelCount = inputAudioFormat.channelCount)
        }
        chain.configure(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        configured = true
        // Same format out as in: the DSP chain changes sample values, not their
        // container, and forcing Float32 out would fight the sink's own
        // negotiation (which already prefers float where the device supports it).
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        // The Android Media3 pipeline may send its shared EMPTY_BUFFER.
        // Never attempt to copy that singleton into itself.
        if (!inputBuffer.hasRemaining()) return

        val bytesPerFrame = inputEncoding.bytesPerFrame(block.channelCount)
        if (bytesPerFrame <= 0) {
            replaceOutputBuffer(inputBuffer.remaining()).put(inputBuffer).flip()
            return
        }

        val completeFrames = (pendingByteCount + inputBuffer.remaining()) / bytesPerFrame
        if (completeFrames == 0) {
            // PCM fragments smaller than a full frame must NOT reach AudioTrack.
            // Consume them into the carry, so the Media3 pipeline can advance
            // to the next input buffer rather than repeatedly presenting a tail.
            val bytes = inputBuffer.remaining()
            inputBuffer.get(pendingFrameBytes, pendingByteCount, bytes)
            pendingByteCount += bytes
            return
        }

        val dst = replaceOutputBuffer(completeFrames * bytesPerFrame)
        dst.order(ByteOrder.LITTLE_ENDIAN)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        if (pendingByteCount != 0) {
            val needed = bytesPerFrame - pendingByteCount
            inputBuffer.get(pendingFrameBytes, pendingByteCount, needed)
            frameScratch.clear()
            frameScratch.put(pendingFrameBytes, 0, bytesPerFrame)
            frameScratch.flip()
            PcmBoundary.decode(frameScratch, inputEncoding, block, 1)
            chain.process(block)
            PcmBoundary.encode(block, outputEncoding, dst, framesToEncode = block.frameCount)
            pendingByteCount = 0
        }

        var remainingFrames = inputBuffer.remaining() / bytesPerFrame
        while (remainingFrames > 0) {
            val chunkFrames = minOf(remainingFrames, block.capacityFrames)
            val chunkBytes = chunkFrames * bytesPerFrame
            val slice = inputBuffer.slice().order(ByteOrder.LITTLE_ENDIAN)
            slice.limit(chunkBytes)
            PcmBoundary.decode(slice, inputEncoding, block, chunkFrames)
            chain.process(block)
            PcmBoundary.encode(block, outputEncoding, dst, startFrame = 0, framesToEncode = block.frameCount)
            inputBuffer.position(inputBuffer.position() + chunkBytes)
            remainingFrames -= chunkFrames
        }
        // Save any final partial frame, regardless of PCM sample format or
        // channel count. Media3 owns the original buffer; fully consume it.
        val tail = inputBuffer.remaining()
        if (tail != 0) {
            inputBuffer.get(pendingFrameBytes, 0, tail)
            pendingByteCount = tail
        }
        dst.flip()
    }

    override fun onQueueEndOfStream() {
        // Incomplete PCM frames are invalid: never pass partial samples to the
        // sink or carry them into the next stream.
        pendingByteCount = 0
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onFlush() {
        pendingByteCount = 0
        // A seek, a track change, or a format change: the filters' tails would
        // otherwise carry across the boundary and bleed one track into the next.
        chain.flush()
        chain.onStreamBoundary()
    }

    override fun onReset() {
        chain.reset()
        configured = false
        pendingByteCount = 0
        pendingFrameBytes = ByteArray(0)
        frameScratch = ByteBuffer.allocateDirect(0).order(ByteOrder.LITTLE_ENDIAN)
        block = AudioBlock()
    }
}
