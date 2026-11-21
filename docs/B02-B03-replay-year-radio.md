# B02 + B03: Replay Year animations and YouTube Radio

## B02
The annual story uses the exact calendar-year snapshot from `ListeningStats.year()` and
`ReplayYearStoryModel`. Optional month cards appear only with at least two
recorded monthly top songs. The progress clock now controls the large reveal,
so pausing, holding and sharing also pause the reveal. Each slide uses its
assigned song art, without starting a second playback service.

The Replay page exposes an explicit animated annual-story action. Card sharing
continues to use the existing Replay poster for the corresponding category.

## B03
Radio is the fifth bottom tab, preserving existing stored tab indices.
The YouTube Music `mixedForYouKeyed()` feed displays the localized names
and groups them by the English identity: stations, personal mixes and mood
mixes. Anonymous users see the existing sign-in prompt. All mix cards
open through Aurelis's existing browse/detail player.

## Compatibility
No duplicate Media3 player or changes to the playback service.
No change to `main`, no cloud backups, no download automation.
