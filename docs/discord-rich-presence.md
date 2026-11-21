# Discord Rich Presence: Aurelis Music

Official application ID: `1573536052935794802`.

The previous Kizzy user-token transport and token-harvesting WebView login have
been retired. The settings page has a local preview with the real launcher
foreground, track name and artist. No API key, bot token or account token is
required. Sharing is off by default.

## Optional official Android integration

Discord Social SDK 1.10+ supports unauthenticated Android RPC when Discord is
installed and signed in. To make an SDK-enabled build, obtain the official
Android `discord_partner_sdk.aar` for this registered application from
https://discord.com/developers/applications/1573536052935794802
and place it in `app/libs/discord_partner_sdk.aar` (do not commit the licensed
artifact). Gradle then enables Prefab and builds `aurelis_discord`; without the
AAR the UI correctly displays preview-only and its switch is disabled.

The Android playback service feeds title as details, artist as state, and uses
the **registered Discord application icon** as the large image by not overriding
it with external artwork. Playback stop/pause clears the activity. Configure the
Aurelis Music icon in the Discord Developer Portal: Android drawable assets do
not automatically upload to Discord. No local filesystem paths are published.

Validate an SDK-enabled ARM64 release build and live playback on a real Android
device signed in to the Discord app before promising it works end-to-end.

Docs:
https://discord.com/developers/docs/discord-social-sdk/development-guides/setting-rich-presence
https://discord.com/developers/docs/social-sdk/release_notes.html
