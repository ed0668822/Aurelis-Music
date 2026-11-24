// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis

import com.music.aurelis.data.AppUpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A010: update links may never install an APK from a foreign repository. */
class AurelisOfficialUpdateLinksTest {
    @Test fun officialReleaseApkIsAccepted() {
        assertTrue(AppUpdateChecker.isOfficialReleaseAssetUrl(
            "https://github.com/ed0668822/Aurelis-Music/releases/download/v2.0.0/Aurelis-v2.0.0_arm64-v8a.apk"
        ))
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
