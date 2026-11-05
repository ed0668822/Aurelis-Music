package com.music.aurelis.playback

import com.music.aurelis.playback.audio.AudioBlock
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class EqualizerProcessorTest {
    private fun processor(curve: EqCurve = EqCurve.FLAT, enabled: Boolean = true, balance: Float = 0f, rate: Int = 48_000) = EqualizerProcessor().apply {
        configure(rate, 2); setTuning(enabled, curve, balance)
    }
    private fun tone(frequency: Double, frames: Int = 24_000, rate: Int = 48_000) = FloatArray(frames * 2) { index -> (0.05 * sin(2 * PI * frequency * (index / 2) / rate)).toFloat() }
    private fun process(eq: EqualizerProcessor, input: FloatArray, chunk: Int = 512): FloatArray {
        val output = FloatArray(input.size); var offset = 0
        val block = AudioBlock(2, chunk)
        while (offset < input.size) {
            val frames = min(chunk, (input.size - offset) / 2)
            input.copyInto(block.samples, 0, offset, offset + frames * 2); block.reset(frames)
            eq.process(block); block.samples.copyInto(output, offset, 0, frames * 2); offset += frames * 2
        }
        return output
    }
    private fun rms(samples: FloatArray): Double = sqrt(samples.drop(8000).sumOf { it.toDouble() * it } / (samples.size - 8000))
    @Test fun disabledAndFlatAreExactPassThrough() {
        val input = tone(1000.0)
        assertArrayEquals(input, process(processor(manualCurve(List(7) { 12f }), false), input), 0f)
        assertArrayEquals(input, process(processor(), input), 0f)
    }
    @Test fun bellActuallyChangesTheTargetFrequency() {
        val curve = manualCurve(listOf(0f, 0f, 0f, 6f, 0f, 0f, 0f))
        val center = rms(process(processor(curve), tone(1000.0)))
        val low = rms(process(processor(curve), tone(60.0)))
        val differenceDb = 20 * log10(center / low)
        assertTrue("Bell response $differenceDb dB", differenceDb in 5.0..7.0)
    }
    @Test fun shelvesChangeBassAndTrebleIndependently() {
        val bass = manualCurve(listOf(6f, 0f, 0f, 0f, 0f, 0f, 0f))
        assertTrue(rms(process(processor(bass), tone(30.0))) > 1.5 * rms(process(processor(bass), tone(2000.0))))
        val treble = manualCurve(listOf(0f, 0f, 0f, 0f, 0f, 0f, 6f))
        assertTrue(rms(process(processor(treble), tone(20_000.0))) > 1.5 * rms(process(processor(treble), tone(1000.0))))
    }
    @Test fun balanceMutesTheOppositeStereoChannelAndDoesNotAmplifyTheOther() {
        val input = FloatArray(200) { 0.25f }
        val result = process(processor(balance = 1f), input)
        for (index in result.indices step 2) { assertEquals(0f, result[index], 0f); assertEquals(0.25f, result[index + 1], 0f) }
    }
    @Test fun filterHistorySurvivesBlocksAndFlushClearsIt() {
        val curve = toneCurve(2, 3, true); val input = tone(1200.0)
        assertArrayEquals(process(processor(curve), input, 256), process(processor(curve), input, 2048), 0.000001f)
        val eq = processor(curve); process(eq, input); eq.flush()
        assertArrayEquals(FloatArray(1000), process(eq, FloatArray(1000)), 0f)
    }
    @Test fun invalidSettingsAndLowSampleRatesNeverProduceNonFiniteSamples() {
        val invalid = EqCurve(floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY, 99f), floatArrayOf(Float.NaN), Float.NaN)
        assertTrue(process(processor(invalid, balance = Float.NaN, rate = 8000), tone(600.0, rate = 8000)).all { it.isFinite() })
        val inactive = manualCurve(listOf(0f, 0f, 0f, 0f, 0f, 0f, 12f))
        val input = tone(600.0, rate = 8000)
        assertArrayEquals(input, process(processor(inactive, rate = 8000), input), 0f)
    }
}
