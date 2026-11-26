<!-- SPDX-FileCopyrightText: © 2026 Aurelis -->

<h1 align="center">Aurelis Music</h1>

<p align="center"><strong>A local-first music player for Android</strong></p>

<p align="center">
  <a href="#download">Download</a> ·
  <a href="#features">Features</a> ·
  <a href="#build-from-source">Build from source</a> ·
  <a href="#contributing">Contributing</a> ·
  <a href="#credits">Credits</a> ·
  <a href="#license">License</a>
</p>

Aurelis Music brings your on-device music library, synced lyrics, and audio controls together in one player. Browse your albums and artists, build playlists, and listen to your local files offline.

## Features

| Playback | Library |
| --- | --- |
| Play audio files stored on your Android device, including in the background. | Browse songs, albums, and artists. |
| Blend tracks with AutoMix (beta) and choose an analysis performance mode. | Save favorites, create playlists, and find duplicate songs. |

| Now playing | Audio controls |
| --- | --- |
| Follow word-synced lyrics when available. | Adjust tone and balance with a seven-band equalizer. |
| Enable the Liquid Glass navigation effect on Android 12 or later. | Skip silence and set a sleep timer. |

## Download

The standard APK targets **64-bit ARM devices (`arm64-v8a`)** running **Android 8.0 or later**.

1. Open [GitHub Releases](https://github.com/ed0668822/Aurelis-Music/releases) and download the APK from the release's **Assets** section.
2. Open the APK on your Android device and follow the installation prompts.
3. Allow access to audio files when Aurelis Music asks, so it can read your local library.

## Build from source

Use **JDK 17**, the **Android SDK Platform 37**, and **CMake 3.22.1**. Set your Android SDK path in `local.properties` as `sdk.dir`, or through `ANDROID_HOME`.

```sh
git clone https://github.com/ed0668822/Aurelis-Music.git
cd Aurelis-Music
./gradlew :app:assembleDevDebug
```

On Windows, use `gradlew.bat :app:assembleDevDebug`. Find the APK under `app/build/outputs/apk/dev/debug/`.

## Contributing

Open a pull request with a short explanation of the change and how you tested it. Include screenshots for visual changes and the Android version you used for device testing.

## Credits

- [BitChord](https://github.com/kushagrasinghx/BitChord) by Kushagra Singh and contributors is the upstream project Aurelis Music is based on.
- [Mornye](https://github.com/mornyeapp/Mornye) served as a reference for AutoMix work.

## License

Copyright © 2026 Aurelis.

Aurelis Music is distributed under the [GNU General Public License, version 3 only](LICENSE) (`GPL-3.0-only`). Files and components with their own license notices remain subject to those notices.
