#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.6/P3: separate observed runtime evidence from unexecuted functional tests.

This script cannot label Compose navigation or music/audio playback PASS unless
an independent, recognized functional test suite has actually run.
"""
import hashlib
import json
from pathlib import Path

UI = (
    "home_navigation", "local_library", "artists_albums", "mini_player_now_playing",
    "settings_menus", "lyrics_synchronization", "empty_loading_error_states",
)
AUDIO = (
    "load_local_track", "play_pause_resume", "next_previous", "queue_management",
    "gapless_transition", "crossfade", "audio_output_route",
)


def read(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def report(package: Path, sha: str):
    b04 = package / "B04" / "Verificacion"
    b05 = package / "B05" / "Emulador"
    output_ui = package / "P2.6"
    output_audio = package / "P3"
    output_ui.mkdir(parents=True, exist_ok=True)
    output_audio.mkdir(parents=True, exist_ok=True)
    source = read(b04 / "source-evidence.json")
    verified = read(b04 / "apk-verification.json")
    runtime = read(b05 / "results.json")
    screenshot = b05 / "screen.png"
    screenshot_bytes = screenshot.read_bytes() if screenshot.is_file() and not screenshot.is_symlink() else b""
    screenshot_verified = screenshot_bytes.startswith(b"\x89PNG\r\n\x1a\n") and len(screenshot_bytes) > 64
    foreground = (
        runtime.get("commit") == sha
        and runtime.get("pass") is True
        and runtime.get("coverage") == "INSTALL_LAUNCH_PROCESS_SMOKE_ONLY"
    )
    ui = {
        "schema_version": 1,
        "commit": sha,
        "scope": "P2.6 Compose UI and navigation",
        "runtime_smoke_observed": foreground,
        "screenshot_png_evidence": screenshot_verified,
        "screenshot_sha256": hashlib.sha256(screenshot_bytes).hexdigest() if screenshot_verified else None,
        "app_surface_observation": "SCREENSHOT_AND_SMOKE_OBSERVED" if foreground and screenshot_verified else "NOT_VERIFIED",
        "functional_test_status": "NOT_VERIFIED",
        "functional_scenarios": {x: "NOT_VERIFIED" for x in UI},
        "missing_proof": "No actual Compose interaction assertions, UI test event stream or verified per-screen results.",
    }
    audio = {
        "schema_version": 1,
        "commit": sha,
        "scope": "P3 actual playback/audio testing and public-release readiness",
        "ci_source_identity_verified": source.get("source_sha") == sha and source.get("status") == "PASS_VERIFIED_GATES",
        "apk_integrity_verified": verified.get("status") == "success",
        "emulator_smoke_observed": foreground,
        "audio_functional_test_status": "NOT_VERIFIED",
        "audio_scenarios": {x: "NOT_VERIFIED" for x in AUDIO},
        "public_release_readiness": "NOT_APPROVED",
        "public_release_created": False,
        "release_artifact_vs_public_release": (
            "An Actions artifact named Release is not a published GitHub Release. "
            "Signed APK verification does not substitute for audio and UI functional tests."
        ),
        "missing_proof": [
            "A licensed local audio fixture and automated playback assertions",
            "MediaSession state and audible-output test evidence",
            "Verified Compose navigation and error-state tests",
        ],
    }
    (output_ui / "ui-functional-coverage.json").write_text(json.dumps(ui, indent=2) + "\n", encoding="utf-8")
    (output_audio / "audio-and-publication-readiness.json").write_text(json.dumps(audio, indent=2) + "\n", encoding="utf-8")
    ui_lines = ["# P2.6 — Evidencia UI Compose", "",
                "- Inicio en emulador observado: " + str(foreground),
                "- Captura PNG verificada: " + str(screenshot_verified),
                "- Navegación y pantallas: **NOT_VERIFIED**", ""]
    ui_lines += ["- " + name + ": NOT_VERIFIED" for name in UI]
    audio_lines = ["# P3 — Audio y publicación", "",
                   "- Verificación de integridad APK: " + str(audio["apk_integrity_verified"]),
                   "- Tests reales de audio: **NOT_VERIFIED**",
                   "- Publicación pública aprobada: **NO**", ""]
    audio_lines += ["- " + name + ": NOT_VERIFIED" for name in AUDIO]
    (output_ui / "Resumen.md").write_text("\n".join(ui_lines) + "\n", encoding="utf-8")
    (output_audio / "Resumen.md").write_text("\n".join(audio_lines) + "\n", encoding="utf-8")
    return ui, audio
