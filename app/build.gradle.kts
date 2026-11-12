// Aurelis © 2026. GPL-3.0-or-later.

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlinx.kover")
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { stream ->
            this.load(stream)
        }
    }
}

val signingProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { stream ->
            this.load(stream)
        }
    }
}

fun readValue(key: String, default: String = ""): String {
    val local = localProps.getProperty(key)?.trim()
    if (!local.isNullOrEmpty()) return local

    val env = System.getenv(key)?.trim()
    if (!env.isNullOrEmpty()) return env

    return default
}

fun readSigning(key: String): String? {
    val local = signingProps.getProperty(key)?.trim()
    if (!local.isNullOrEmpty()) return local

    return System.getenv(key)?.trim()?.takeIf { it.isNotEmpty() }
}

fun escapeForBuildConfig(value: String): String {
    val sb = StringBuilder("\"")

    for (ch in value) {
        when (ch) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> sb.append(ch)
        }
    }

    sb.append('"')
    return sb.toString()
}

val listenTogetherServer = readValue("LISTEN_TOGETHER_SERVER", "").trimEnd('/')

// A debug-signed Release is only for explicitly requested local experiments.
// CI never sets this property; official builds must use the configured release keystore.
val allowDebugSignedRelease = providers.gradleProperty("allowDebugSigned")
    .map { it == "true" }.getOrElse(false)
val configuredReleaseSigning = listOf(
    readSigning("storeFile") ?: readSigning("STORE_FILE"),
    readSigning("storePassword") ?: readSigning("STORE_PASSWORD"),
    readSigning("keyAlias") ?: readSigning("KEY_ALIAS"),
    readSigning("keyPassword") ?: readSigning("KEY_PASSWORD"),
).all { !it.isNullOrBlank() } && (
    readSigning("storeFile") ?: readSigning("STORE_FILE")
)?.let { rootProject.file(it).isFile } == true

val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val autoVersionCode = if (runNumber != null) 10000 + runNumber else 1
val autoVersionName = if (runNumber != null) "1.9.$runNumber" else "1.9.0-dev"

android {
    namespace = "com.music.aurelis"

    compileSdk = 36
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "com.music.aurelis"

        minSdk = 33
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        versionCode = autoVersionCode
        versionName = autoVersionName

        buildConfigField("String", "UPDATE_APK_PREFIX", "\"app-\"")
        buildConfigField("String", "LASTFM_API_KEY", "\"\"")
        buildConfigField("String", "LASTFM_SECRET", "\"\"")
        buildConfigField(
            "String",
            "LISTEN_TOGETHER_SERVER",
            escapeForBuildConfig(listenTogetherServer)
        )

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        val storePath = readSigning("storeFile") ?: readSigning("STORE_FILE")
        val storePassword = readSigning("storePassword") ?: readSigning("STORE_PASSWORD")
        val keyAlias = readSigning("keyAlias") ?: readSigning("KEY_ALIAS")
        val keyPassword = readSigning("keyPassword") ?: readSigning("KEY_PASSWORD")

        val store = storePath?.let { rootProject.file(it) }

        if (
            store != null &&
            store.exists() &&
            !storePassword.isNullOrEmpty() &&
            !keyAlias.isNullOrEmpty() &&
            !keyPassword.isNullOrEmpty()
        ) {
            create("release") {
                storeFile = store
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    androidResources {
        noCompress.add("onnx")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols.add("**/*.so")
        }
    }

    buildTypes {
        debug {
            // Keep Debug independently debug-signed, even when Release credentials exist.
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = false

            signingConfig = signingConfigs.findByName("release")
                ?: if (allowDebugSignedRelease) signingConfigs.getByName("debug") else null

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Fail before running Release assembly if an actual release keystore is missing.
// Release unit tests do not assemble an APK, so they remain executable without secrets.
gradle.taskGraph.whenReady {
    if (hasTask(":app:assembleRelease") &&
        !configuredReleaseSigning &&
        !allowDebugSignedRelease
    ) {
        throw GradleException(
            "Release signing requires a valid configured keystore. " +
                "No Debug signature fallback is allowed; " +
                "-PallowDebugSigned=true is for explicit local experiments only."
        )
    }
}

// Bundle verbatim project and third-party notices; never edit license text during builds.
val bundleLicenseTexts by tasks.registering(Sync::class) {
    from(rootProject.file("LICENSE"))
    from(rootProject.file("NOTICE"))
    from(rootProject.file("LICENSES"))
    into(layout.buildDirectory.dir("generated/legalAssets/licenses"))
}
android.sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/legalAssets"))
tasks.named("preBuild").configure { dependsOn(bundleLicenseTexts) }

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")

    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.json:json:20240303")

    implementation("androidx.palette:palette-ktx:1.1.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.media3:media3-common:1.11.1")

    implementation("io.github.anilbeesetti:nextlib-media3ext:1.11.1-0.16.0")

    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("dev.chrisbanes.haze:haze:1.3.1")
    implementation("dev.chrisbanes.haze:haze-materials:1.3.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.22.0")

    implementation("com.google.guava:guava:33.3.1-android")
    implementation("org.checkerframework:checker-qual:3.48.3")
    implementation("org.jsoup:jsoup:1.18.1")

    compileOnly("org.jspecify:jspecify:1.0.0")

    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

