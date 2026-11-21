# Discord Rich Presence in Aurelis Music

- **Application name:** Aurelis Music
- **Discord Application ID:** `1573536052935794802`
- **Official project:** https://github.com/ed0668822/Aurelis-Music

## Current integration

`DiscordRPC` can publish the current track title in `details`, artist
in `state`, and track progress via timestamps. `PlaybackService`
observes track changes and clears the activity when playback stops.

Local MediaStore songs must **never** be linked to a fictitious YouTube
Music URL. Only YouTube-style 11-character track IDs may receive an
external Listen button; the project's secondary button points to Aurelis.

## IMPORTANT: Existing code is not the official Discord SDK

The current `KizzyRPC` connector uses a **Discord user session token**.
Changing the Application ID does not replace that connector, authenticate
users safely, or make it Discord Social SDK. Do not ask users to extract
or paste their Discord account tokens as a way to activate official
integration.

For the official integration, download the Discord Social SDK **1.10+**
for Android from the **Discord Developer Portal** for this application
and ensure its terms permit this music player. This distributable is not
included here; no proprietary binaries or secrets are checked into Git.

When available, replace the legacy gateway transport with a native
`discordpp::Client` adapter, call `SetApplicationId`, then publish
`Activity` updates with `UpdateRichPresence` via JNI. Android
`<queries>` already declares `com.discord` for app visibility.

Requirements: Discord installed and signed in on the device; user
explicitly enables sharing; stopped/paused playback clears activity.
Use the same Media3 event feed already observed by `PlaybackService`.
The Social SDK backend must be validated in an Android instrumented test
before replacing the legacy transport or marking the feature supported.

Official docs:
https://docs.discord.com/developers/discord-social-sdk/development-guides/setting-rich-presence
