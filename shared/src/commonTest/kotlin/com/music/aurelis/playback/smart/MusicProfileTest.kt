package com.music.aurelis.playback.smart

import kotlin.test.Test
import kotlin.test.assertEquals

class MusicProfileTest {
    private fun lowCurve(small: Int, big: Int): List<EnergySample> =
        List(small + big) { i -> EnergySample(i.toDouble(), if (i < small) 0.05 else 1.0) }

    @Test
    fun steadyKickAtDanceTempoIsDance() {
        val analysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 128.0,
            beatConfidence = 0.9,
            lowEnergyCurve = List(40) { EnergySample(it.toDouble(), 1.0) },
        )
        assertEquals(MusicKind.DANCE, musicKindOf(analysis))
    }

    @Test
    fun sparseLowEndIsCalm() {
        val analysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 80.0,
            beatConfidence = 0.7,
            lowEnergyCurve = lowCurve(small = 34, big = 6),
        )
        assertEquals(MusicKind.CALM, musicKindOf(analysis))
    }

    @Test
    fun sungTrackWithoutDriveIsVocal() {
        val analysis = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            bpm = 90.0,
            beatConfidence = 0.6,
            vocalProbability = 0.8,
        )
        assertEquals(MusicKind.VOCAL, musicKindOf(analysis))
    }

    @Test
    fun unanalysedTrackIsGeneral() {
        assertEquals(MusicKind.GENERAL, musicKindOf(TrackAnalysis()))
    }
}
