# TV01–TV02: Google Cast audit and local-media delivery

Reference: `Aurelis-Music-Redesign` at `df6351fb1c606957d6ade0cdbd7af6601426b2c0`.
Scope: Cast regression coverage and LAN delivery of seekable local audio.
This implementation does **not** add DLNA, a custom TV receiver or synchronized lyrics (TV03–TV09).

## TV01 — verified integration boundaries

- `CastController` owns Cast device discovery, sessions and receiver volume.
- `CastPlayback` owns Cast media loading, receiver clock and remote queue reconciliation.
- `RemoteSpeaker`/`Speakers` keep a single service-level routing abstraction for Cast and watch playback.
- `PlaybackService` owns the existing ExoPlayer, media session, queue and stream resolver.
- `CastOptionsProvider` still uses the Google Default Media Receiver, with its
  notification/media session disabled to avoid competing with Aurelis's own.
- `CastDialog` still connects/disconnects via `CastController`.
- No additional ExoPlayer, MediaSession, Cast framework or configuration is introduced.

Added `SpeakersRoutingTest` to check active-speaker dispatch and broadcasting
queue/transition changes to receivers, without doubling playback commands.

## TV02 — local audio in the existing Cast pipeline

`PlaybackService.resolveForCast` now attempts to expose an authorized, seekable
`content://` or `file://` source via `LocalCastMediaServer` before its
existing remote URL resolution. Local media must have a reliable nonzero size
and supported audio MIME type.

`LocalCastMediaServer` obtains a Wi-Fi/Ethernet IPv4 interface and creates a
`LocalCastHttpServer` bound to that address (not all interfaces). It reads
only local audio already chosen by the playback service; there is no directory
listing, free-form file path parameter or public filesystem API.

The HTTP server:
- Exposes random per-session and per-media capability URL segments, never raw file paths.
- Streams using file descriptors and bounded buffers; does not load entire songs into memory.
- Supports `GET`, `HEAD`, `Range: bytes=start-end`, open-ended and suffix ranges.
- Returns 206 for valid partial requests and 416 for unsatisfiable ranges.
- Restricts request/header sizes, concurrent clients and tracked media URLs.
- Terminates and revokes resources on Cast session release / service destruction.
- Does not add dependencies, persistent network listeners or another media engine.

Added `LocalCastHttpServerTest` covering GET, HEAD, seek ranges, invalid
ranges, unpredictable paths and parser bounds.

## Known limits and follow-on work

- This is a LAN HTTP stream, not a claim that every Cast receiver will accept
  insecure HTTP audio from all networks. Test real devices, router isolation,
  firmware codec support, buffering and Android local-network rules.
- `content://` providers must expose a seekable file descriptor and length.
  The implementation deliberately refuses unknown-size/nonseekable providers.
- Offline HLS/DASH playlists with relative segments are not shared through
  this single-file endpoint; existing remote fallback remains available.
- Receiver codec support differs; there is no transcoding in TV02.
- Cast URLs are bearer capabilities. They are temporary but plaintext over
  local HTTP. They must not be logged, published or treated as authenticated
  identity for devices on untrusted networks.
- A phone whose process is terminated cannot continue serving local audio.
  Receiver reconnection/resumption of old LAN URLs requires device testing.
- Future SmartCast protocol selection and synchronized TV lyrics belong to
  TV03–TV09; TV02 must not modify these features prematurely.

## Verification

Gradle checks to run on this branch:
```bash
./gradlew :app:testDevDebugUnitTest :app:testProdReleaseUnitTest :app:lintDevDebug :app:assembleDevDebug
```
Also test local MP3, FLAC, WAV and M4A on a real Cast receiver, seeking and
queue advance, then disconnecting and confirming the LAN port closes.

Automated JVM tests cannot certify actual TV compatibility. Record device
results separately and do not claim real-device verification from CI alone.
