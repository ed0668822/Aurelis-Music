package com.music.aurelis

import com.music.aurelis.playback.smart.AutomixAnalysisSource
import com.music.aurelis.playback.QualityUpgrade
import com.music.aurelis.data.sources.StreamFormat
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class AutomixAnalysisSourceTest {
    @Test
    fun `source recording results never reuse a YouTube match's analysis`() {
        val sourceKey = "addon-1|track-42#stereo"
        val scoped = AutomixAnalysisSource.storageKey("source-track-42", sourceKey)
        assertTrue(scoped.contains(sourceKey))
        assertFalse(scoped == AutomixAnalysisSource.storageKey("source-track-42", null))
        assertFalse(scoped == AutomixAnalysisSource.storageKey("source-track-42", "other-addon|track-42"))
        assertTrue(AutomixAnalysisSource.storageKey("yt123", null) == "yt123")
    }

    @Test
    fun `YouTube analysis accepts only its canonical Opus cache key`() {
        assertTrue(AutomixAnalysisSource.isCanonicalYouTubeRendition("video", "video"))
        assertFalse(AutomixAnalysisSource.isCanonicalYouTubeRendition("video", "video#alt"))
        assertFalse(AutomixAnalysisSource.isCanonicalYouTubeRendition("video", "video#hifi"))
    }

    @Test
    fun `analysis URI asks the resolver to bypass playback substitutions`() {
        assertTrue(AutomixAnalysisSource.opusUri("video").contains("v=video&automix_opus=1"))
        assertTrue(AutomixAnalysisSource.requestsYouTubeOpus("1"))
        assertFalse(AutomixAnalysisSource.requestsYouTubeOpus(null))
        assertFalse(AutomixAnalysisSource.requestsYouTubeOpus("0"))
    }

    @Test
    fun `second quality upgrade gets a new rendition marker`() {
        val jio = QualityUpgrade.upgradedUri("aurelis://watch?v=video")
        val lossless = QualityUpgrade.upgradedUri(jio)
        assertTrue(jio.endsWith("q=hifi"))
        assertTrue(lossless.endsWith("q=hifi-2"))
    }
}
