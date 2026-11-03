// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback.smart

import android.media.MediaDataSource
import com.music.aurelis.playback.smart.AudioDecoder.Pcm
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Fuente de análisis local de pista para transiciones suaves y emparejamiento de tempo.
 *
 * Lee una región del archivo local con [AudioDecoder] y extrae métricas simples
 * (energía RMS, tempo aproximado) sin depender de red ni de modelos de ML remotos.
 */
class AutomixAnalysisSource {

    /** Resultado del análisis de una pista local. */
    data class TrackFeatures(
        /** Energía RMS normalizada (0.0 = silencio, 1.0 = escala completa). */
        val energy: Float,
        /** Tempo estimado en BPM (muy aproximado, solo para ordenar pistas similares). */
        val tempo: Float,
    ) {
        companion object {
            val empty = TrackFeatures(energy = 0f, tempo = 120f)
        }
    }

    /**
     * Analiza [filePath] leyendo una región representativa del archivo local.
     * Retorna [TrackFeatures.empty] si el archivo no se puede decodificar.
     */
    fun analyzeTrack(filePath: String): TrackFeatures {
        val source = FileMediaDataSource(filePath)
        return runCatching {
            val region = AudioDecoder.decodeRegion(
                source = source,
                startSeconds = 30.0,
                endSeconds = 60.0,
            ) ?: return@runCatching TrackFeatures.empty
            val (pcm, sampleRate) = region
            val energy = rmsEnergy(pcm)
            val tempo = estimateTempo(pcm, sampleRate.toFloat())
            TrackFeatures(energy = energy, tempo = tempo)
        }.getOrDefault(TrackFeatures.empty)
    }

    private fun rmsEnergy(pcm: Pcm): Float {
        val samples = pcm.samples
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (s in samples) sum += s * s
        return sqrt(sum / samples.size).toFloat().coerceIn(0f, 1f)
    }

    /** Estimación muy aproximada de tempo por autocorrelación simple del envelope. */
    /** MediaDataSource concreta que lee de un archivo local. */
    private class FileMediaDataSource(private val path: String) : MediaDataSource() {
        private val raf = java.io.RandomAccessFile(java.io.File(path), "r")

        override fun readAt(position: Long, buffer: ByteArray?, offset: Int, size: Int): Int {
            if (buffer == null) return -1
            raf.seek(position)
            return raf.read(buffer, offset, size)
        }

        override fun getSize(): Long = raf.length()

        override fun close() {
            raf.close()
        }
    }

    private fun estimateTempo(pcm: Pcm, sampleRate: Float): Float {
        val samples = pcm.samples
        if (samples.size < 1024) return 120f
        val frameSize = (sampleRate * 0.05).toInt() // 50 ms
        val envelope = FloatArray(samples.size / frameSize)
        for (i in envelope.indices) {
            var sum = 0f
            for (j in 0 until frameSize) {
                val idx = i * frameSize + j
                if (idx < samples.size) sum += abs(samples[idx])
            }
            envelope[i] = sum / frameSize
        }
        // Busca el pico de autocorrelación entre 60 y 180 BPM
        val minLag = (sampleRate * 60 / 180 / frameSize).toInt().coerceAtLeast(1)
        val maxLag = (sampleRate * 60 / 60 / frameSize).toInt().coerceAtMost(envelope.size - 1)
        var bestLag = minLag
        var bestCorr = 0f
        for (lag in minLag..maxLag) {
            var corr = 0f
            var count = 0
            for (i in lag until envelope.size) {
                corr += envelope[i] * envelope[i - lag]
                count++
            }
            if (count > 0 && corr / count > bestCorr) {
                bestCorr = corr / count
                bestLag = lag
            }
        }
        val bpm = sampleRate * 60 / (bestLag * frameSize)
        return bpm.coerceIn(60f, 180f)
    }
}
