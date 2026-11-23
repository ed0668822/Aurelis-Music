package com.music.aurelis.data.innertube

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MostReplayedTest {
    private fun next(heat: List<Float>): kotlinx.serialization.json.JsonObject {
        val markers = heat.mapIndexed { index, value ->
            """{"startMillis":"${index * 2000}","durationMillis":"2000","intensityScoreNormalized":$value}"""
        }.joinToString(",")
        return Json.parseToJsonElement(
            """{"frameworkUpdates":{"entityBatchUpdate":{"mutations":[{"payload":{"macroMarkersListEntity":{"markersList":{"markerType":"MARKER_TYPE_HEATMAP","markers":[$markers]}}}}]}}}"""
        ) as kotlinx.serialization.json.JsonObject
    }

    @Test fun peakUsesRealCurveInsteadOfOpeningPopularity() {
        val heat = List(100) { i ->
            when {
                i < 4 -> 0.95f
                i in 48..55 -> 0.9f
                else -> 0.24f
            }
        }
        val curve = MostReplayed.parse(next(heat))!!
        val chosen = MostReplayed.peakMs(curve, 200_000L)
        assertTrue("Expected peak around the chorus, got $chosen", chosen != null && chosen in 86_000L..112_000L)
    }

    @Test fun absentOrFlatCurveNeverCreatesFakePopularity() {
        assertNull(MostReplayed.parse(next(List(7) { 0.5f })))
        assertNull(MostReplayed.peakMs(MostReplayed.parse(next(List(100) { 0.5f }))!!, 200_000L))
    }

    @Test fun localAndAddonIdsAreNotYouTubeVideos() {
        assertTrue(MostReplayed.isYouTubeId("abc1234567_"))
        assertTrue(!MostReplayed.isYouTubeId("local:123"))
        assertTrue(!MostReplayed.isYouTubeId("not a video id"))
    }
}
