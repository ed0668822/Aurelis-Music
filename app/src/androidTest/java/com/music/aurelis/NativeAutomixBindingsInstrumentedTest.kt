package com.music.aurelis

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.aurelis.playback.loopmix.LoopMixRenderer
import com.music.aurelis.playback.smart.MelSpectrogram
import com.music.aurelis.playback.smart.NativeAnalysisLibrary
import com.music.aurelis.playback.smart.TrackFeatures
import com.music.aurelis.playback.smart.VocalSpectrogram
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runtime JNI smoke test: catches UnsatisfiedLinkError that JVM/unit tests and
 * a successful Gradle assemble cannot detect after Android package renames.
 */
@RunWith(AndroidJUnit4::class)
class NativeAutomixBindingsInstrumentedTest {
    @Test fun beatAndVocalAnalyzerNativeMethodsResolve() {
        assertTrue("Native Automix analyzer .so is unavailable", NativeAnalysisLibrary.available)
        assertTrue("Native tempo analyzer JNI symbol missing", TrackFeatures.sampleRate > 0.0)
        assertTrue("Native mel JNI symbol missing", MelSpectrogram.mels > 0)
        assertTrue("Native mel sample rate is invalid", MelSpectrogram.sampleRate > 0)
        assertTrue("Native vocal JNI symbol missing", VocalSpectrogram.bins > 0)
        assertTrue("Native vocal FFT JNI symbol missing", VocalSpectrogram.fftSize > 0)
    }

    @Test fun loopMixRendererNativeMethodsResolve() {
        assertTrue("Native loop mix .so is unavailable", LoopMixRenderer.available)
        LoopMixRenderer(44_100).use { renderer ->
            assertTrue("Native loop renderer failed to create a deck mixer", renderer.isValid)
        }
    }

    @Test fun optionalDsdAndBitPerfectLibrariesLoad() {
        for (library in listOf("aurelis_dsd", "aurelis_dst", "aurelis_exclusive", "aurelis_usb")) {
            System.loadLibrary(library)
        }
    }
}
