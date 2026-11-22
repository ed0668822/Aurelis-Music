# TV07 / TV08 — SmartCast routing, manual mirroring and recovery

Work branch: `Aurelis-Music-Redesign`. Base code: TV01–TV06.

## Goal and verified architecture

TV07 makes it possible to select a *television* instead of selecting a
protocol first. TV08 handles disconnections without unexpectedly resuming
the phone speaker and offers the **Android system's screen-casting settings**
as a strictly opt-in alternative. None of these features creates another
ExoPlayer, MediaSession, service, audio stream processor or TV video encoder.

The existing app still owns the music library and queue via PlaybackService.
The actual receiver owns its playback position; the MediaSession follows
RemoteSpeaker/Speakers. Cast, DLNA, Web TV and Wear are mutually exclusive.

## TV07 — Safe protocol choice

- `SmartCastRanking` is a pure Kotlin ranking function, with JVM tests.
- Nearby Cast and DLNA endpoints are combined **only when the normalized
  advertised names match and each protocol has exactly one such device**.
  Ambiguous same-name devices remain separate to prevent sending audio to
  someone else's TV. Never group by broad/fuzzy brand/model heuristics.
- The default order is: **registered Custom Cast** (TV lyrics), **Google
  Default Media Receiver** (audio), then **DLNA** when Cast is absent.
  A web receiver remains an *explicitly paired browser URL*, not something
  the phone can launch itself on all televisions.
- Grouped Cast+DLNA endpoints offer a single SmartCast choice and an
  explicit manual DLNA option. Cast is tried first. Only if the Cast SDK
  reports the **session startup failed** does the coordinator try DLNA
  on the *same, unambiguously matched* renderer.
- No automatic switch during playback suspension, a seek, buffering or an
  ordinary Wi-Fi interruption: a second output could start while the first
  is still playing. Choosing another active output requires disconnecting.
- DLNA's own ConnectionManager:GetProtocolInfo probe still gates formats
  in `DlnaPlayback.connect`; a discovered renderer does *not* guarantee
  support for every codec. Actual output quality isn't inferred from protocol.

This is a conservative first version. TV09 should test capability probing
and whether selected devices expose stable identifiers that enable safe,
more precise grouping.

## TV08 — Failure handling

- `RemoteRecoveryPolicy` defines **5 consecutive missed** DLNA status
  polling cycles and **15 seconds** without a TV browser heartbeat for an
  actively playing browser session. Both thresholds are JVM tested.
- DLNA timeouts revoke the remote route and preserve the last reported
  track position, but **never auto-play the phone**. The device picker
  presents an error when a renderer fails, rejects a format or loses contact.
- Browser TV sends a heartbeat via authenticated local `GET /state` polls
  and music events. On heartbeat timeout, the temporary web listener/media
  URL is shut down and the remote route clears. The browser also pauses
  itself after repeated failures to reach the phone.
- A system-initiated Cast session closes any paired Web TV server and
  disconnects DLNA before Cast playback begins.
- Browser TV owns its volume. The output drawer hides the phone's
  ineffective volume slider on that route; DLNA uses its reported/set
  volume level.
- Screen mirroring is a separate, manually chosen row using
  `android.provider.Settings.ACTION_CAST_SETTINGS`, falling back to
  `ACTION_DISPLAY_SETTINGS` if no specific Cast settings activity exists.
  This merely opens system configuration; **it does not invoke MediaProjection,
  guarantee screen mirroring availability or capture audio/video itself**.
  System/device vendors may require their own mirroring workflow and consent.

## Security and limitations

The TV06 browser and TV02 local-media URLs are short-lived bearer URLs
served over plaintext **LAN HTTP**. These are not suitable for untrusted
shared networks and must never be forwarded to the public internet.
A Web TV session should be closed when it is no longer in use.

The user must explicitly select a TV, open a Web browser URL or start a
system mirroring flow. No automatic screen capture or unsolicited playback.

The currently registered Google Cast Custom Receiver ID is still optional.
Until its application is hosted at HTTPS and registered with Google, default
Cast continues to play audio without the custom TV lyrics interface.

## Verification

The existing `.github/workflows/android.yml` checks Debug, Release,
JVM tests, Android lint, instrumentation test APK compilation, and static
syntax of Cast/Web receiver JavaScript. New JVM tests are:

- `SmartCastRankingTest`: exact-name grouping, distinct endpoints,
  ambiguous duplicate names, selection and lyrics capability.
- `RemoteRecoveryPolicyTest`: repeated DLNA failures and browser heartbeat.
- `LocalTvWebServerTest`: request-scoped heartbeat callback and access
  tokens, in addition to its pre-existing GET/POST tests.

Passing the Gradle workflow does not prove firmware-specific TV operation.
TV09 should test actual Cast, DLNA (Samsung/LG as available), Google TV,
browser pairing, format negotiation, link outages, queue transitions,
paused sessions, screen-off survival and cross-protocol recovery.
