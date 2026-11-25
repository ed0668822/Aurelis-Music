// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis

import com.music.aurelis.data.AppUpdateChecker
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A010: update links may never install an APK from a foreign repository. */
class AurelisOfficialUpdateLinksTest {
    @Test fun officialReleaseApkIsAccepted() {
        assertTrue(AppUpdateChecker.isOfficialReleaseAssetUrl(
            "https://github.com/ed0668822/Aurelis-Music/releases/download/v2.0.0/Aurelis-v2.0.0_arm64-v8a.apk"
        ))
    }

    @Test fun displayNamedReleaseApkIsOnlyOfferedToArm64Devices() {
        val base = "https://github.com/ed0668822/Aurelis-Music/releases/download/v3.11/"
        val arm64Url = base + "Aurelis.Music.V3.11.apk"
        val x86Url = base + "Aurelis-v3.11_x86_64.apk"
        val release = buildJsonObject {
            put("assets", buildJsonArray {
                for ((name, url) in listOf(
                    "Aurelis.Music.V3.11.apk" to arm64Url,
                    "Aurelis-v3.11_x86_64.apk" to x86Url,
                )) {
                    add(buildJsonObject {
                        put("name", name)
                        put("browser_download_url", url)
                        put("state", "uploaded")
                    })
                }
            })
        }

        assertEquals(arm64Url, AppUpdateChecker.apkAssetUrl(release, arrayOf("arm64-v8a")))
        assertEquals(x86Url, AppUpdateChecker.apkAssetUrl(release, arrayOf("x86_64")))
        assertNull(AppUpdateChecker.apkAssetUrl(release, arrayOf("armeabi-v7a")))
    }

    @Test fun foreignAndImpersonatedReleaseAssetsAreRejected() {
        val foreign = listOf(
            "https://github.com/kushagrasinghx/BitChord/releases/download/v2/test.apk",
            "https://github.com/other/Aurelis-Music/releases/download/v2/test.apk",
            "https://github.com.evil.example/ed0668822/Aurelis-Music/releases/download/v2/test.apk",
            "http://github.com/ed0668822/Aurelis-Music/releases/download/v2/test.apk",
            "https://evil.example/test.apk",
            "not-a-url",
        )
        for (url in foreign) assertFalse(url, AppUpdateChecker.isOfficialReleaseAssetUrl(url))
    }
}
