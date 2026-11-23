# BitChord 1.10 → Aurelis: B00/B01

## Scope
Branch: `Aurelis-Music-Redesign`. No edits to `main`.
Reference: BitChord tag `v1.10` (`7448b92e032af7e5666c93b0e04c6b9fa3abf898`).

This delivery prepares annual Replay data only. It does not add cloud backup,
replace the player, or claim that the B02 animated year stories are completed.

## B00: compatibility
- Keep the existing single Media3 playback service and the local listening profile.
- Reuse `ListeningStats` and its persisted per-month buckets; no database migration.
- Avoid new binary models or dependencies.
- Respect the existing notice/licence requirements; review third-party copying independently.

## B01: year-specific model
- `ListeningStats.year(year)` reads the requested calendar year, including past years.
- Recorded day totals and monthly winning songs are projected from the same buckets.
- The busiest-day clip is selected only when a track has actual day-level evidence.
- `ReplayYearStoryModel` allocates clips to the existing annual card types and
  reserves the leading song for its reveal and recap.
- No activity or artwork is fabricated for an empty or legacy bucket.

## Follow-up B02
Wire this snapshot into the animated story screen, add dedicated monthly and
discovery cards, gesture/navigation/audio timing, and export/share actions.

## Verification
Tests: `ReplayYearProjectionTest`, `ReplayYearStoryModelTest`.
CI: Android Debug/Release and Android Lint on the target branch.
