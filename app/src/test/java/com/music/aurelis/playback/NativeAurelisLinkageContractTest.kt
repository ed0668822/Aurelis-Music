package com.music.aurelis.playback

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the Android package migration across Kotlin -> JNI -> CMake.
 * A build can succeed with incorrect JNI method names while playback fails
 * at runtime with UnsatisfiedLinkError. Keep the native contract explicit.
 */
class NativeAurelisLinkageContractTest {

    private fun nativeFile(relative: String): String {
        val roots = listOf(File("."), File("app"), File("../app"))
        val target = roots.map { File(it, "src/main/cpp/$relative") }
            .firstOrNull { it.isFile }
            ?: error("Missing native source: $relative")
        return target.readText()
    }

    @Test fun nativeLibraryNamesMatchKotlinLoaders() {
        val cmake = nativeFile("CMakeLists.txt")
        for (name in listOf("analysis", "loopmix", "dsd", "dst", "exclusive", "usb")) {
            assertTrue("Missing libaurelis_$name.so build target",
                cmake.contains("add_library(aurelis_${name} SHARED"))
        }
        assertFalse("Legacy native library targets are unsafe",
            Regex("""add_library\(bitchord_""").containsMatchIn(cmake))
        assertTrue("Wrong JNI export visibility",
            nativeFile("bitperfect/exports.map").contains(
                "Java_com_music_aurelis_playback_audio_bitperfect_*"))
    }

    @Test fun jniBindingsUseCurrentAndroidPackage() {
        val bridges = mapOf(
            "jni/analysis_jni.cpp" to "playback_smart_TrackFeatures_nativeAnalyze",
            "jni/mel_jni.cpp" to "playback_smart_MelSpectrogram_nativeCompute",
            "jni/vocal_jni.cpp" to "playback_smart_VocalSpectrogram_nativeCompute",
            "jni/loopmix_jni.cpp" to "playback_loopmix_LoopMixRenderer_nativeCreate",
            "dsd/dsd_decimator.cpp" to "playback_dsd_DsdBlockDecoder_create",
            "dsd/dst/jni.cpp" to "playback_dsd_DstDecoder_create",
            "bitperfect/exclusive_output.cpp" to "playback_audio_bitperfect_ExclusiveNative_",
            "bitperfect/usb_transport.cpp" to "playback_audio_bitperfect_usb_UsbDirectNative_",
        )
        for ((file, suffix) in bridges) {
            val cpp = nativeFile(file)
            assertTrue("$file still points at the wrong package",
                cpp.contains("Java_com_music_aurelis_$suffix"))
            assertFalse("$file contains legacy JNI symbol names",
                cpp.contains("Java_com_music_bitchord_"))
        }
    }
}
