/*
 * Modeled on Orchard's own AudioDecoder (https://github.com/SFG5453/Orchard),
 * scoped down to the platform MediaCodec path — Orchard prefers a native
 * libopus decode with the platform decoder as its documented fallback; this
 * only needs the fallback, since Aurelis has no reason to carry a second
 * Opus decoder purely for background analysis.
 *
 * Copyright (C) 2026 Kushagra Singh
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.music.aurelis.playback.smart

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/**
 * Decodes a region of a cached track to mono float PCM, for [TrackAnalyzer].
 *
 * Only a region: a transition only ever reads the tail of the outgoing track
 * and the head of the incoming one, not either track in full, and decoding a
 * whole album's worth of audio to analyse thirty seconds of it would cost
 * battery for nothing.
 *
 * Everything here is best-effort. A codec that will not configure, a
 * container Android cannot parse, a region past the end — all return null,
 * and the caller falls back to no analysis, which the transition policy
 * already handles as its bottom rung.
 */
object AudioDecoder {

    private const val TAG = "AurelisAudioDecoder"
    private const val TIMEOUT_US = 10_000L

    /** Decoded mono PCM at the container's own sample rate; the caller resamples. */
    data class Pcm(val samples: FloatArray, val sampleRate: Double)

    /**
     * Decoded planar stereo PCM at the container's own sample rate.
     *
     * Planar rather than interleaved because the only consumer is the vocal
     * front end, which wants one array per channel; a mono source is widened
     * by sharing the same samples on both sides, which is what the model was
     * trained to see for centre-panned material anyway.
     */
    data class StereoPcm(val left: FloatArray, val right: FloatArray, val sampleRate: Double)

    /**
     * Reads the audio duration a fully-cached container advertises, without
     * decoding it. Queue metadata isn't always trustworthy, so this is the
     * fallback [TrackAnalyzer] reaches for when a track's own duration is
     * missing or non-finite.
     */
    fun containerDurationSeconds(source: MediaDataSource): Double? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(source)
            (0 until extractor.trackCount)
                .mapNotNull { index ->
                    val format = extractor.getTrackFormat(index)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: return@mapNotNull null
                    if (!mime.startsWith("audio/") || !format.containsKey(MediaFormat.KEY_DURATION)) {
                        return@mapNotNull null
                    }
                    format.getLong(MediaFormat.KEY_DURATION).takeIf { it > 0 }?.div(1_000_000.0)
                }
                .maxOrNull()
                ?.takeIf { it.isFinite() && it > 0 }
        } catch (error: Exception) {
            Log.w(TAG, "Could not read duration from cached media", error)
            null
        } finally {
            runCatching { extractor.release() }
        }
    }

    /**
     * Decodes [startSeconds] to [endSeconds] of [source], downmixed to mono at
     * the container's native rate.
     *
     * The extractor seeks to the closest sync sample at or before the
     * requested start, so a little more audio than asked for may come back at
     * the front; the caller is given the real start via the returned offset
     * so frame indices still map to true track times.
     *
     * [abort] is polled once per pass of the decode loop; answering true
     * abandons the decode as if it had failed. A whole-track decode is the
     * longest single thing analysis does, and it holds tens of megabytes of
     * PCM while it runs, so the caller that no longer wants the answer wants
     * it stopped rather than merely ignored.
     */
    fun decodeRegion(
        source: MediaDataSource,
        startSeconds: Double,
        endSeconds: Double,
        abort: () -> Boolean = { false },
    ): Pair<Pcm, Double>? {
        val chunks = ArrayList<FloatArray>()
        val decoded = decodeRaw(source, startSeconds, endSeconds, abort) { buffer, info, channels, _, encoding ->
            chunks += toMono(buffer, info, channels, encoding)
        } ?: return null
        return Pcm(flatten(chunks), decoded.first) to decoded.second
    }

    /**
     * As [decodeRegion], but keeping the two channels apart.
     *
     * Only the vocal front end needs this: open-unmix was trained on stereo,
     * and handing it a duplicated mono mix throws away the very stereo
     * information it uses to tell a centred vocal from the instruments
     * around it.
     */
    fun decodeRegionStereo(
        source: MediaDataSource,
        startSeconds: Double,
        endSeconds: Double,
        abort: () -> Boolean = { false },
    ): Pair<StereoPcm, Double>? {
        val left = ArrayList<FloatArray>()
        val right = ArrayList<FloatArray>()
        val decoded = decodeRaw(source, startSeconds, endSeconds, abort) { buffer, info, channels, _, encoding ->
            toStereo(buffer, info, channels, encoding, left, right)
        } ?: return null
        return StereoPcm(flatten(left), flatten(right), decoded.first) to decoded.second
    }

    /**
     * As [decodeRegion], but handing each decoded mono chunk to [onChunk] instead
     * of keeping it, so a caller that only reduces the audio never holds the
     * region. Returns the output rate paired with the region's real start.
     */
    fun streamRegion(
        source: MediaDataSource,
        startSeconds: Double,
        endSeconds: Double,
        abort: () -> Boolean = { false },
        onChunk: (samples: FloatArray, atSeconds: Double, sampleRate: Int) -> Unit,
    ): Pair<Double, Double>? =
        decodeRaw(source, startSeconds, endSeconds, abort) { buffer, info, channels, rate, encoding ->
            onChunk(toMono(buffer, info, channels, encoding), info.presentationTimeUs / 1_000_000.0, rate)
        }

    /**
     * Streams stereo PCM to a reducer without retaining the decoded song.
     * The existing decode loop still owns codec/extractor cleanup and cancellation.
     */
    fun streamRegionStereo(
        source: MediaDataSource,
        startSeconds: Double,
        endSeconds: Double,
        abort: () -> Boolean = { false },
        onChunk: (left: FloatArray, right: FloatArray, sampleRate: Int, channels: Int) -> Unit,
    ): Pair<Double, Double>? =
        decodeRaw(source, startSeconds, endSeconds, abort) { buffer, info, channels, rate, encoding ->
            if (channels !in 1..2 || rate <= 0) return@decodeRaw
            val left = ArrayList<FloatArray>(1)
            val right = ArrayList<FloatArray>(1)
            toStereo(buffer, info, channels, encoding, left, right)
            if (left.isNotEmpty()) onChunk(left[0], right[0], rate, channels)
        }

    /** Concatenates the decoded chunks into one contiguous buffer. */
    private fun flatten(chunks: List<FloatArray>): FloatArray {
        val samples = FloatArray(chunks.sumOf { it.size })
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(samples, offset)
            offset += chunk.size
        }
        return samples
    }

    /**
     * Runs the decode loop, handing each output buffer to [onBuffer], and
     * returns the output sample rate paired with the region's real start.
     *
     * Shared by the mono and stereo entry points so there is one dequeue loop
     * to get right rather than two that can drift apart; all that differs
     * between them is how a buffer is reduced, which is what [onBuffer] owns.
     */
    private fun decodeRaw(
        source: MediaDataSource,
        startSeconds: Double,
        endSeconds: Double,
        abort: () -> Boolean,
        onBuffer: (ByteBuffer, MediaCodec.BufferInfo, Int, Int, Int) -> Unit,
    ): Pair<Double, Double>? {
        if (endSeconds <= startSeconds) return null
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(source)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

            val startUs = (startSeconds * 1_000_000).toLong()
            val endUs = (endSeconds * 1_000_000).toLong()
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            if (mime == "audio/raw") {
                return readPcm(extractor, format, endUs, abort, onBuffer)
            }

            codec = runCatching { MediaCodec.createDecoderByType(mime) }
                .onFailure { Log.w(TAG, "No decoder for $mime", it) }
                .getOrNull() ?: return null
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var outputChannels = format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 1
            var outputRate = format.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 0
            var outputEncoding = AudioFormat.ENCODING_PCM_16BIT
            var actualStartSeconds = -1.0
            var sawFirstSample = false
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                // Polled at the top of the loop rather than only around the
                // model passes that follow: everything decoded so far goes with
                // it, and on a long track that is most of what the analysis
                // costs in both time and heap. `finally` still tears the codec
                // and extractor down.
                if (abort()) return null
                // Every free input slot is filled, and every ready output
                // drained, before anything waits. Feeding one packet and then
                // blocking for its frame left the loop waiting on the codec's
                // latency once per 20 ms Opus frame: about 6 ms each, which is
                // most of what decoding a region cost. It only blocks now when
                // neither side could move.
                var progressed = false
                while (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(0L)
                    if (inputIndex < 0) break
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: break
                    progressed = true
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    val sampleTimeUs = extractor.sampleTime
                    if (sampleSize < 0 || (sampleTimeUs in 0..Long.MAX_VALUE && sampleTimeUs > endUs)) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTimeUs, 0)
                        extractor.advance()
                    }
                }

                var timeout = if (progressed) 0L else TIMEOUT_US
                while (!outputDone) {
                    val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeout)
                    if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                    timeout = 0L
                    when (outputIndex) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val newFormat = codec.outputFormat
                            outputRate = newFormat.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: outputRate
                            outputChannels = newFormat.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: outputChannels
                            outputEncoding = newFormat.intOrNull(MediaFormat.KEY_PCM_ENCODING) ?: outputEncoding
                        }
                        MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                        else -> if (outputIndex >= 0) {
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }
                            if (bufferInfo.size > 0) {
                                if (!sawFirstSample) {
                                    actualStartSeconds = bufferInfo.presentationTimeUs / 1_000_000.0
                                    sawFirstSample = true
                                }
                                codec.getOutputBuffer(outputIndex)?.let { output ->
                                    onBuffer(output, bufferInfo, outputChannels, outputRate, outputEncoding)
                                }
                                if (bufferInfo.presentationTimeUs > endUs) outputDone = true
                            }
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }
            }

            if (!sawFirstSample || outputRate <= 0) return null
            return outputRate.toDouble() to actualStartSeconds
        } catch (error: Exception) {
            Log.w(TAG, "Region decode failed", error)
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun readPcm(
        extractor: MediaExtractor,
        format: MediaFormat,
        endUs: Long,
        abort: () -> Boolean,
        onBuffer: (ByteBuffer, MediaCodec.BufferInfo, Int, Int, Int) -> Unit,
    ): Pair<Double, Double>? {
        val rate = format.intOrNull(MediaFormat.KEY_SAMPLE_RATE)?.takeIf { it > 0 } ?: return null
        val channels = format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT)?.takeIf { it > 0 } ?: return null
        val encoding = format.intOrNull(MediaFormat.KEY_PCM_ENCODING) ?: AudioFormat.ENCODING_PCM_16BIT
        val capacity = (format.intOrNull(MediaFormat.KEY_MAX_INPUT_SIZE) ?: 64 * 1024)
            .coerceIn(64 * 1024, 4 * 1024 * 1024)
        val buffer = ByteBuffer.allocateDirect(capacity)
        val info = MediaCodec.BufferInfo()
        var firstTime = -1L
        while (!abort()) {
            val time = extractor.sampleTime
            if (time < 0 || time > endUs) break
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            if (size > capacity) return null
            if (size > 0) {
                if (firstTime < 0) firstTime = time
                info.set(0, size, time, 0)
                onBuffer(buffer, info, channels, rate, encoding)
            }
            extractor.advance()
        }
        return if (firstTime < 0 || abort()) null else rate.toDouble() to firstTime / 1_000_000.0
    }

    private class PcmSamples(buffer: ByteBuffer, info: MediaCodec.BufferInfo, private val encoding: Int) {
        private val bytes = buffer.duplicate().apply {
            clear()
            position(info.offset)
            limit(info.offset + info.size)
            order(ByteOrder.LITTLE_ENDIAN)
        }

        val count: Int = bytes.remaining() / when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
            AudioFormat.ENCODING_PCM_32BIT, AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> throw IllegalArgumentException("Unsupported PCM encoding $encoding")
        }

        fun read(): Float = when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> ((bytes.get().toInt() and 0xff) - 128) / 128f
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
                val low = bytes.get().toInt() and 0xff
                val middle = bytes.get().toInt() and 0xff
                val high = bytes.get().toInt()
                (low or (middle shl 8) or (high shl 16)) / 8_388_608f
            }
            AudioFormat.ENCODING_PCM_32BIT -> bytes.int / 2_147_483_648f
            AudioFormat.ENCODING_PCM_FLOAT -> bytes.float.takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f
            else -> bytes.short / 32_768f
        }
    }

    private fun toMono(buffer: ByteBuffer, info: MediaCodec.BufferInfo, channels: Int, encoding: Int): FloatArray {
        val safeChannels = max(1, channels)
        val samples = PcmSamples(buffer, info, encoding)
        return FloatArray(samples.count / safeChannels) {
            var sum = 0f
            repeat(safeChannels) { sum += samples.read() }
            sum / safeChannels
        }
    }

    /** Keeps the first two channels for the stereo vocal model. */
    private fun toStereo(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        channels: Int,
        encoding: Int,
        left: MutableList<FloatArray>,
        right: MutableList<FloatArray>,
    ) {
        val safeChannels = max(1, channels)
        val samples = PcmSamples(buffer, info, encoding)
        val frames = samples.count / safeChannels
        val leftChunk = FloatArray(frames)
        val rightChunk = FloatArray(frames)
        for (index in 0 until frames) {
            leftChunk[index] = samples.read()
            rightChunk[index] = if (safeChannels > 1) samples.read() else leftChunk[index]
            repeat((safeChannels - 2).coerceAtLeast(0)) { samples.read() }
        }
        left += leftChunk
        right += rightChunk
    }

    private fun MediaFormat.intOrNull(key: String): Int? = if (containsKey(key)) getInteger(key) else null
}
