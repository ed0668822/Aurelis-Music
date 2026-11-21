# Discord Social SDK: Aurelis Music

The application ID is `1573536052935794802`. Discord Social SDK **1.10+**
supports native Android Rich Presence via unauthenticated RPC. Discord must
be installed and signed in on the Android device. There is **no user token,
bot token, or API key** to input in Aurelis Music.

## Official SDK acquisition

The Android binary **cannot be generated from the Application ID**. Log in to
your own Discord Developer Portal application at
https://discord.com/developers/applications/1573536052935794802,
open **Discord Social SDK > Downloads**, obtain the official Android
`discord_partner_sdk.aar` (SDK 1.10 or later), and review its separate
license. Do not commit or redistribute the standalone SDK without authorization.

For local Android builds, put it at `app/libs/discord_partner_sdk.aar`.
The repository ignores this file. Run
`bash scripts/install-discord-sdk.sh` to check Prefab/ARM64/classes.

## Direct ZIP from Discord Developer Portal

The general `DiscordSocialSdk-1.11.24259.zip` contains Android and desktop
libraries. For an authorized **short-lived signed** download URL, create a
private repository Actions secret named `DISCORD_SDK_ZIP_URL` containing the
exact full HTTPS Google Storage download link. Do **not** use it as
`DISCORD_SDK_AAR_URL`; that older setting expects a direct .aar.

Optionally set `DISCORD_SDK_ZIP_SHA256` to the ZIP checksum. The installer
restricts ZIP downloads to the official Discord SDK distribution bucket,
extracts only the Android release AAR, and validates Prefab and ARM64.
GitHub Actions handles the extraction for Debug and signed Release independently.

Signed URLs expire (typically in one hour). Generate a fresh one from the
Developer Portal if necessary. Never commit a signed URL or proprietary SDK
archive to GitHub.

## GitHub Actions, automatic and private

To include that **official AAR** in both APKs, host a private, authorized
HTTPS copy that GitHub Actions can fetch and configure two repo Actions
secrets:

- `DISCORD_SDK_AAR_URL`: direct signed HTTPS URL for the AAR file
  (not the Developer Portal web page).
- `DISCORD_SDK_AAR_SHA256`: 64-hex SHA-256 digest of the AAR.

Both Debug and signed Release jobs run `scripts/install-discord-sdk.sh`
before invoking Gradle, verify the digest, and package their APKs as
**separate artifacts**. A missing URL produces an ordinary APK, with
Discord's switch disabled and its local preview available. An invalid
configured URL, digest, or AAR fails the build.

## Native integration

Gradle conditionally includes the AAR, enables Prefab and builds the
`aurelis_discord` JNI library. `MainActivity.onCreate` invokes the official
`com.discord.socialsdk.DiscordSocialSdkInit.setEngineActivity(this)`
by reflection (so builds without the licensed AAR compile). The
`PlaybackService` waits for SDK initialization and opt-in, then calls
`Client::SetApplicationId`, `UpdateRichPresence`, and `RunCallbacks`.
It sends the song and artist and clears activity on pause/stop.
The large image defaults to the icon configured for Aurelis Music in
the Discord Developer Portal, not the Android launcher bitmap.

The SDK-enabled ARM64 path still needs a real AAR and **physical device
test**: building successfully without the file does not validate SDK mode.

Official docs:
- https://discord.com/developers/docs/discord-social-sdk/development-guides/account-linking-on-mobile
- https://discord.com/developers/docs/discord-social-sdk/development-guides/setting-rich-presence
