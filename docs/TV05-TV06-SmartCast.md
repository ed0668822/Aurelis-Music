# TV05 / TV06 — DLNA and browser TV transport

Work branch: `Aurelis-Music-Redesign`. This follows TV01–TV04.

## Architecture

Aurelis already has exactly one `PlaybackService`, one Android MediaSession,
the phone's queue, and an abstract `RemoteSpeaker`. TV05 and TV06 implement
that existing interface: **no additional ExoPlayer or MediaSession is created**.
The source resolver, local-file permissions and lyric repository stay in the
playback service.

New remote speakers are mutually exclusive with Cast and the watch route in
the standard output picker (TV07 will extend automatic protocol ranking).

## TV05 — DLNA/UPnP MediaRenderer

`DlnaController` discovers UPnP AV MediaRenderers via SSDP M-SEARCH on
239.255.255.250:1900 when the receiver picker is opened. It acquires a short
Wi-Fi multicast lock while scanning. Replies are parsed and checked for
same-host, local IPv4 HTTP device descriptions. Untrusted UPnP XML DTDs and
external entities are disallowed.

`DlnaProtocol` reads the device's description and AVTransport service;
ConnectionManager GetProtocolInfo is used when available to filter audio MIME
formats. SOAP AVTransport commands support SetAVTransportURI, Play, Pause, Seek,
Stop, GetPositionInfo and GetTransportInfo. RenderingControl SetVolume is
attempted on devices exposing that service.

`DlnaPlayback` keeps the phone queue, sends the current media URL, pauses the
phone *after* the remote successfully begins, and polls the remote's position.
After confirmed STOPPED state, it advances to the next queued song.
When the listener disconnects, the phone can resume at the last reported
remote position. UPnP HTTP/1.1 clients vary considerably: some devices reject
codecs, require DIDL-Lite variants or implement only parts of AVTransport.
No full DLNA compliance is claimed.

DLNA typically transmits audio/metadata, **not an Aurelis lyrics UI**.

## TV06 — browser receiver

`LocalTvWebServer` serves a bundled HTML/CSS/JS player directly from the
phone, bound to one Wi-Fi/Ethernet IPv4 address. When the user taps
"Open Aurelis Music in your TV browser" in the output picker it displays an
unguessable, short-lived local URL to open or copy.

The TV browser plays music with an HTML5 audio element. Browser code polls
`/state`, fetches `/lyrics` when the lyrics revision changes, animates
timed lines and words against `HTMLAudioElement.currentTime`, and submits
play/pause/progress/end events through `POST /event`.

`WebTvPlayback` remains in the existing `RemoteSpeaker` model. The phone
keeps ownership of the queue; the browser owns the playback clock. The phone
is paused only after the browser reports that audio began. The user must
explicitly press Play inside the TV browser to satisfy autoplay restrictions.

The server uses a random session token in every route, accepts bounded
request headers/bodies, refuses arbitrary paths and checks cross-origin POST
requests. It is closed when the output session ends. It never serves a whole
filesystem or introduces an externally hosted account.

Because the browser page is served over plain HTTP on the private LAN, it
must be treated as an **untrusted-network feature**: HTTP and temporary
media URLs are visible to someone who can inspect local traffic.
Do not expose the listener through router port forwarding. TV07 can add stronger
pairing, identity and automatic protocol selection.

## Platform and operational limits

- Android targetSdk is 36; SSDP multicast is subject to Android's evolving
  local-network permission enforcement when targetSdk is raised to 37.
- Only private IPv4 LAN UPnP renderers and Wi-Fi/Ethernet addresses are covered.
- The TV and phone must be mutually reachable (client/AP isolation blocks it).
- Local music requires a seekable MediaStore/SAF provider, a format supported
  by the receiver, and a phone whose media service stays alive.
- Some online streams need headers/auth that generic DLNA/HTML5 devices cannot
  provide. Those streams may remain unsupported.
- Browser TV needs a modern browser with HTML5 audio and Fetch API.
  The browser cannot automatically open itself on a generic Smart TV.
- The phone's DSP/AutoMix processing is not automatically reproduced by a
  remote device decoding the original audio URL.
- Sender/device formats, remote controls, discovery timing, pause/resume and
  lyric sync must be validated on *real hardware*.

## Test plan and verification

```bash
./gradlew :app:assembleDevDebug :app:assembleProdRelease \
  :app:testDevDebugUnitTest :app:testProdReleaseUnitTest \
  :app:lintDevDebug :app:assembleDevDebugAndroidTest
python3 scripts/verify_webtv_receiver.py
```

Added JVM regression tests for XML description parsing, SOAP escaping,
protocol/MIME decisions, URL protection and the browser pairing server's
GET/POST endpoints. CI validates JavaScript syntax but does not emulate a
physical Samsung/LG/Google TV.

**Next stage:** TV07 automatic ranking, observed capabilities, and robust
handoff/fallback; TV08 mirroring alternatives and recovery; TV09 hardware QA.
