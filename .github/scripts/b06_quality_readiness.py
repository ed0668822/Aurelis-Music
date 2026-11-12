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
        and runtime.get("coverage") in (
            "INSTALL_LAUNCH_PROCESS_SMOKE_ONLY",
            "INSTALL_LAUNCH_PROCESS_AND_LOCAL_WAV_INSTRUMENTATION",
        )
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
            "Physical-device audible audio capture and output evidence",
            "MediaSession state and audible-output test evidence",
            "Verified Compose navigation and error-state tests",
        ],
    }
    # Count only actual passing Robolectric JUnit cases in both variants and the same commit.
    # Explicitly keep physical Android UI and audible output NOT_VERIFIED.
    from b06_junit_attestation import UI_CASES, CONTROLLER_CASES, attest_cases
    compose = attest_cases(package, sha, UI_CASES)
    controls = attest_cases(package, sha, CONTROLLER_CASES)
    ui["functional_scenarios"].update(compose["states"])
    ui["junit_evidence"] = compose
    ui["functional_test_status"] = (
        "ROBOLECTRIC_COMPOSE_CASES_VERIFIED"
        if all(v == "ROBOLECTRIC_JUNIT_PASS" for v in compose["states"].values())
        else "NOT_VERIFIED"
    )
    ui["physical_android_ui_verification"] = "NOT_VERIFIED"
    audio["mocked_controller_evidence"] = controls
    audio["mocked_controller_status"] = (
        "ROBOLECTRIC_MOCK_PLAYER_CASES_VERIFIED"
        if all(v == "ROBOLECTRIC_JUNIT_PASS" for v in controls["states"].values())
        else "NOT_VERIFIED"
    )
    for key, value in controls["states"].items():
        audio["audio_scenarios"][key] = (
            "MOCK_PLAYER_BEHAVIOR_VERIFIED" if value == "ROBOLECTRIC_JUNIT_PASS"
            else "NOT_VERIFIED"
        )
    # Only a successful real AndroidJUnitRunner test in the same commit may
    # establish local-WAV playback through AurelisPlaybackService/Media3.
    wav = (
        foreground
        and runtime.get("coverage") == "INSTALL_LAUNCH_PROCESS_AND_LOCAL_WAV_INSTRUMENTATION"
        and isinstance(runtime.get("checks"), dict)
        and runtime["checks"].get("p3_local_wav_instrumentation_passed") is True
        and isinstance(runtime.get("p3_test"), dict)
        and runtime["p3_test"].get("passed") is True
        and runtime["p3_test"].get("instrumentation_class")
        == "com.music.aurelis.playback.RealLocalPlaybackInstrumentedTest"
    )
    audio["local_wav_media3_instrumentation"] = (
        "REAL_MEDIA3_WAV_PLAYBACK_OBSERVED" if wav else "NOT_VERIFIED"
    )
    if wav:
        audio["audio_functional_test_status"] = "MEDIA3_LOCAL_WAV_PLAYBACK_VERIFIED_IN_EMULATOR"
        audio["audio_scenarios"]["load_local_track"] = "EMULATOR_REAL_WAV_DECODE_OBSERVED"
        audio["audio_scenarios"]["play_pause_resume"] = "EMULATOR_PLAY_PAUSE_SEEK_RESUME_OBSERVED"
    # A second named AndroidJUnitRunner case must be present in B05 before
    # queue transitions are promoted from mocked to real-media evidence.
    queue = (
        wav
        and runtime.get("checks", {}).get("p3_queue_transitions_instrumentation_passed") is True
        and runtime["p3_test"].get("test_count") == 2
        and runtime["p3_test"].get("queue_transitions_passed") is True
        and runtime["p3_test"].get("queue_test_class")
        == "com.music.aurelis.playback.RealQueueTransitionsInstrumentedTest"
    )
    audio["real_media3_queue_transitions"] = (
        "MEDIA3_EMULATOR_QUEUE_TRANSITIONS_VERIFIED" if queue else "NOT_VERIFIED"
    )
    if queue:
        audio["audio_scenarios"]["next_previous"] = "EMULATOR_REAL_MEDIA3_NEXT_PREVIOUS_VERIFIED"
        audio["audio_scenarios"]["queue_management"] = "EMULATOR_REAL_MEDIA3_QUEUE_EDIT_VERIFIED"
    audio["audible_physical_output_verified"] = False
    # Real emulated playback cannot prove audible physical output or native DSP.

    (output_ui / "ui-functional-coverage.json").write_text(json.dumps(ui, indent=2) + "\n", encoding="utf-8")
    (output_audio / "audio-and-publication-readiness.json").write_text(json.dumps(audio, indent=2) + "\n", encoding="utf-8")
    ui_lines = ["# P2.6 — Evidencia UI Compose", "",
                "- Inicio en emulador observado: " + str(foreground),
                "- Captura PNG verificada: " + str(screenshot_verified),
                "- Navegación Compose bajo Robolectric: **" + ui["functional_test_status"] + "**",
                "- Funcionalidad UI física: **NOT_VERIFIED**", ""]
    ui_lines += ["- " + name + ": " + ui["functional_scenarios"][name] for name in UI]
    audio_lines = ["# P3 — Audio y publicación", "",
                   "- Verificación de integridad APK: " + str(audio["apk_integrity_verified"]),
                   "- Controles con reproductor simulado: **" + audio["mocked_controller_status"] + "**",
                   "- WAV original a través de Media3 en emulador: **" + audio["local_wav_media3_instrumentation"] + "**",
                   "- Cambio de pista y cola con Media3 real: **" + audio["real_media3_queue_transitions"] + "**",
                   "- Audio audible en dispositivo físico: **NOT_VERIFIED**",
                   "- Publicación pública aprobada: **NO**", ""]
    audio_lines += ["- " + name + ": " + audio["audio_scenarios"][name] for name in AUDIO]
    (output_ui / "Resumen.md").write_text("\n".join(ui_lines) + "\n", encoding="utf-8")
    (output_audio / "Resumen.md").write_text("\n".join(audio_lines) + "\n", encoding="utf-8")
    return ui, audio
