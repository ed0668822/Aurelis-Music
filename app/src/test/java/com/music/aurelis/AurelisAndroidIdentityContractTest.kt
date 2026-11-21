package com.music.aurelis

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A003: checks first-party Android identity and safe intent routing.
 * Android can compile successfully while using the wrong package, launcher
 * component or unverified foreign-brand HTTPS domain.
 */
class AurelisAndroidIdentityContractTest {

    private fun projectFile(path: String): File {
        val file = sequenceOf(
            File(path),
            File("app/$path"),
            File("../app/$path"),
        ).firstOrNull { it.isFile }
        return file ?: error("Missing project file: $path")
    }

    @Test fun appVariantsUseAurelisNamespace() {
        val gradle = projectFile("build.gradle.kts").readText()
        assertTrue(gradle.contains("namespace = \"com.music.aurelis\""))
        assertTrue(gradle.contains("applicationId = \"com.music.aurelis\""))
        assertTrue(gradle.contains("applicationId = \"com.dev.aurelis\""))
        assertFalse(gradle.contains("applicationId = \"com.music.bitchord\""))
        assertFalse(gradle.contains("namespace = \"com.music.bitchord\""))
    }

    @Test fun launcherAndCustomLinksBelongToAurelis() {
        val manifest = projectFile("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:name=\".AurelisApplication\""))
        assertTrue(manifest.contains("android:theme=\"@style/Theme.Aurelis\""))
        assertTrue(manifest.contains("android:scheme=\"aurelis\""))
        assertFalse("Do not register BitChord's website as an Aurelis App Link",
            manifest.contains("android:host=\"bitchord.kushagrasingh.in\""))
    }

    @Test fun releaseNativeBuildMatchesKotlinPackage() {
        val cmake = projectFile("src/main/cpp/CMakeLists.txt").readText()
        val exports = projectFile("src/main/cpp/bitperfect/exports.map").readText()
        assertTrue(cmake.contains("add_library(aurelis_analysis SHARED"))
        assertTrue(cmake.contains("add_library(aurelis_loopmix SHARED"))
        assertFalse(cmake.contains("add_library(bitchord_analysis SHARED"))
        assertTrue(exports.contains("Java_com_music_aurelis_playback_audio_bitperfect_*"))
    }
}
