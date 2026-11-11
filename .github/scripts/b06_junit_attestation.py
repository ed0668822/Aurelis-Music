#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.6/P3: cross-check named JUnit tests in BOTH Debug/Release, tied to B03 commit.

Robolectric Compose test PASS != physical Android UI validation.
Media3 RecordingPlayer test PASS != decoded audio or real speaker output.
"""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

UI_CASES = {
    "home_navigation": ("com.music.aurelis.ui.navigation.NavigationIntegrationTest", "realLibraryLinksAndNestedSettingsRespectAndroidBackAndRestore"),
    "local_library": ("com.music.aurelis.ui.navigation.NavigationIntegrationTest", "localMusicSearchOwnsBackBeforeItsParentPage"),
    "artists_albums": ("com.music.aurelis.ui.LocalCollectionsIntegrationTest", "realHomeAlbumActionsCreateFavoritesAndPersistentPlaylistMembership"),
    "mini_player_now_playing": ("com.music.aurelis.ui.PlayerIntegrationTest", "miniPlayerOpensRealControlsAndQueueActionsStaySynchronized"),
    "settings_menus": ("com.music.aurelis.ui.SettingsIntegrationTest", "appearanceSettingsDriveThemeProvidersAndPersist"),
    "lyrics_synchronization": ("com.music.aurelis.ui.PlayerIntegrationTest", "importedLyricsHighlightAndSeekWithOffsetInTheRealPlayer"),
    "empty_loading_error_states": ("com.music.aurelis.ui.navigation.NavigationIntegrationTest", "restoredPlayerWithoutMediaCanReturnFromQueueWithoutATrappedBlankScreen"),
}
CONTROLLER_CASES = {
    "load_local_track": ("com.music.aurelis.playback.PlaybackIntegrationTest", "immediateTapWaitsForConnectionClampsIndexAndUsesCompleteMetadata"),
    "play_pause_resume": ("com.music.aurelis.playback.PlaybackIntegrationTest", "bufferingCanBePausedAndEndedCanResumeFromStart"),
    "next_previous": ("com.music.aurelis.playback.PlaybackIntegrationTest", "queueRemovalIsAppliedOnceAndInvalidIndexesDoNothing"),
    "queue_management": ("com.music.aurelis.playback.QueueCoordinatorTest", "movingThePlayingRowKeepsItsIdentityAndPausedPosition"),
}


def attest_cases(package: Path, sha: str, cases: dict) -> dict:
    root = package / "B03"
    summary_path = root / "Resumen/summary.md"
    summary = summary_path.read_text(encoding="utf-8") if summary_path.is_file() else ""
    try:
        counters = json.loads((root / "Resumen/results.json").read_text(encoding="utf-8"))
    except (OSError, ValueError):
        counters = {}
    identity = ("- Commit: " + chr(96) + sha + chr(96)) in summary
    valid = bool(identity and isinstance(counters, dict) and all(
        isinstance(counters.get(v), dict)
        and counters[v].get("passed_gate") is True
        and counters[v].get("outcome") == "success"
        and counters[v].get("tests", 0) > 0
        and counters[v].get("failed") == 0
        and counters[v].get("errors") == 0
        and counters[v].get("skipped") == 0
        for v in ("debug", "release")
    ))
    results = {}
    files = {}
    for scenario, (clazz, test) in cases.items():
        variants = {}
        for variant in ("debug", "release"):
            path = root / variant.title() / "junit" / variant / ("TEST-" + clazz + ".xml")
            state, digest = "NOT_VERIFIED", None
            try:
                if valid and path.is_file() and not path.is_symlink():
                    data = path.read_bytes()
                    digest = hashlib.sha256(data).hexdigest()
                    parsed = ET.fromstring(data)
                    if parsed.tag == "testsuite" and parsed.get("name") == clazz:
                        found = [x for x in parsed.iter("testcase")
                                 if x.get("classname") == clazz and x.get("name") == test]
                        if len(found) == 1 and all(found[0].find(kind) is None
                                                   for kind in ("failure", "error", "skipped")):
                            state = "ROBOLECTRIC_JUNIT_PASS"
            except (ET.ParseError, OSError, ValueError):
                state = "NOT_VERIFIED"
            variants[variant] = {"state": state, "report": str(path.relative_to(package)), "sha256": digest}
        results[scenario] = ("ROBOLECTRIC_JUNIT_PASS" if
                             all(v["state"] == "ROBOLECTRIC_JUNIT_PASS" for v in variants.values())
                             else "NOT_VERIFIED")
        files[scenario] = {"class": clazz, "test": test, "variants": variants}
    return {"commit_identity_verified": bool(identity),
            "b03_tests_verified": valid,
            "states": results, "cases": files}
