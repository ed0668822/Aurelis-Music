// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
    }
    dependencies {
        /*
         * Keep the tested D8/R8 version during the AGP upgrade. InnerTubeX
         * and other dependencies publish Kotlin 2.4 metadata, which requires
         * R8 9.1.29 or newer. Older D8 8.10.x also produced a VerifyError in
         * NowPlayingScreen.
         * Verify both Debug playback and signed Release after upgrades.
         */
        classpath("com.android.tools:r8:9.1.31")
    }
}
/*
 * Shared phone / watch release version: v3.10.
 *
 * GitHub Actions sets -Paurelis.buildNumber to github.run_number, so every
 * workflow run generates a monotonically increasing Android versionCode.
 * Debug and signed Release share the release version name.
 * Local builds default to 41, one higher than the previously published 40.
 *
 * Keep the literal "v" out of Android's versionName: the in-app updater
 * compares dot-separated numeric versions. Release tags can use "v3.10".
 */
val requestedBuildNumber = providers.gradleProperty("aurelis.buildNumber").orNull
val aurelisBuildNumber = requestedBuildNumber?.toIntOrNull()
    ?: if (requestedBuildNumber == null) 41
    else error("Invalid aurelis.buildNumber: expected a positive integer")
require(aurelisBuildNumber in 41..2_100_000_000) {
    "aurelis.buildNumber must be >= 41 and valid as Android versionCode"
}
extra["aurelisVersionCode"] = aurelisBuildNumber
extra["aurelisVersionName"] = "3.10"

/*
 * Bump this by hand before cutting each sideloaded test build ("beta2",
 * "beta3", ...) and blank it out before cutting the real release. Marks the
 * versionName above as a pre-release: AppUpdateChecker.isNewer() treats any
 * "-suffix" as older than a clean release of the same number, so testers
 * still get the update prompt once the matching tag is actually published.
 *
 * Applied to release builds as well as debug ones, and that is the whole
 * point of it. A sideloaded beta is a *release* build — signed with the real
 * key, installed over the real package — so leaving the marker off it is
 * exactly the case that strands a tester: their build calls itself 1.6.1,
 * the published 1.6.1 then matches it, isNewer() says no, and no prompt ever
 * comes. Blanking this line is the one step that turns a beta into a release,
 * so it is the one place to get right.
 *
 * The watch app carries the same marker. It needs no prompt logic of its own:
 * the phone compares the watch's installed APK with the one it carries, so a
 * beta watch app shows "Update available" as soon as the phone has the release.
 */
extra["aurelisBetaSuffix"] = ""

plugins {
    id("com.android.application") version "9.1.1" apply false
    id("com.android.library") version "9.1.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.3.20" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
}
