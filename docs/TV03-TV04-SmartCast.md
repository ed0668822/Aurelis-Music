# TV03–TV04: Aurelis Music Custom Cast Web Receiver + synchronized lyrics

Target branch: `Aurelis-Music-Redesign`. This work extends TV01–TV02; it
does not introduce a second ExoPlayer, MediaSession, audio service or audio engine.

## TV03 — Custom Web Receiver

- Hosted receiver source: `receiver/index.html`, `receiver/receiver.js`,
  `receiver/styles.css`.
- Uses Google Cast Application Framework (CAF v3) for audio/media control,
  status and queue; the custom web layer renders cover, title, artist, lyric
  rows and progress over the receiver-managed media player.
- The Android sender config is in
  `app/src/main/java/com/music/aurelis/playback/cast/CastOptionsProvider.kt`.
- Default resource `app/src/main/res/values/cast_receiver.xml` deliberately
  leaves `cast_custom_receiver_id` **empty**. This preserves the existing
  Google Default Media Receiver and normal audio casting. Without a valid
  configured ID the custom receiver does not launch and lyrics do not appear on TV.

### Steps required to activate it

1. Publish the **three receiver/** assets together to a public HTTPS URL
   accessible from Chromecast/Google TV (e.g. `https://your-domain.example/cast/index.html`).
   Do not put credentials or signed local-media URLs in this repository.
2. In the [Google Cast SDK Developer Console](https://cast.google.com/publish/),
   register a **Custom Receiver** using the hosted HTTPS URL. Register a device
   for development if necessary and wait for registration/approval.
3. Copy the 8-character hex Cast application ID into
   `<string name="cast_custom_receiver_id" translatable="false">XXXXXXXX</string>`
   in `app/src/main/res/values/cast_receiver.xml`, then rebuild the APK.
4. Connect a registered receiver and confirm Cast playback, queue, pause, seek,
   album metadata and lyrics. Only then declare TV03 functionally verified.
   Keep this resource empty until those prerequisites are actually satisfied.

The receiver framework is loaded from the official Google Cast SDK CDN.
No external server is hosted or registered by adding source files to GitHub.

## TV04 — synchronized lyrics without duplicating playback

- The existing `PlaybackService.loadLyricsForCurrentTrack` continues to fetch
  local embedded lyrics or `LyricsRepository` results. It publishes their
  resolved `LyricLine`/word timings to `CastPlayback.updateLyrics`.
- On new track, an empty timeline is published first. Completed asynchronous
  lookups are discarded when the current mediaId no longer matches.
- Only when a custom receiver is configured and active does `CastPlayback`
  send lyrics through
  `urn:x-cast:com.music.aurelis.lyrics`, with the existing receiver mediaId,
  bounded payloads, revision IDs, and optional timing offset.
- The TV reads `cast.framework.PlayerManager.getCurrentTimeSec()` for its
  current audio position. No playback timer runs independently on the phone.
- The TV highlights active lines and word spans when word timing is available.
  For lines without word timings, it highlights the complete line. Background
  vocal text is included in message payloads for future presentation support.
- Changes to lyrics offset are republished without another network lyric lookup.
- The receiver must ignore payloads whose mediaId is not the currently playing
  song to avoid showing lyrics from a previous track.

### TV04 limits

- Word/syllable visual effects on TV are simpler than the full Compose
  `PlayerLyrics` animations; this is not a 1:1 visual clone.
- Translation/romanization are not yet delivered to the TV. Original lyrics
  and their line/word timings are delivered.
- Android Cast sends custom messages only to the registered custom receiver.
  The standard/default receiver keeps playing audio with no custom lyric UI.
- Cast metadata artwork is available when it has a TV-accessible HTTP(S) URI;
  TV02 does not expose arbitrary local content artwork.
- Sender messages are best-effort; receiver reconnection and packet loss must
  be verified on hardware.

## Verification gates

- JVM/Gradle: `:app:testDevDebugUnitTest`, `:app:testProdReleaseUnitTest`,
  `:shared:jvmTest`, `:sharedUi:jvmTest`, `:app:lintDevDebug`,
  `:app:assembleDevDebugAndroidTest`, and existing APK build tasks.
- JavaScript static syntax: `node --check receiver/receiver.js` in CI.
- Instrumentation test: `CastLyricsPacketsInstrumentedTest` parses real
  `org.json` packet payloads and checks timings, clearing and chunking.
  Compiling an AndroidTest APK does not execute instrumentation.
- Hardware: test real Cast device on the registered custom app, line/word
  sync (including pause/seek/skip), local MP3/FLAC and remote streams,
  reconnection, screen-off playback and fallback when custom ID is absent.

Avoid claiming the receiver is deployed or registration/device testing has
been completed merely because Android CI succeeds.
