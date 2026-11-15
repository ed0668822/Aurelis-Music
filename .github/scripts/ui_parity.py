# Aurelis © 2026. GPL-3.0-or-later
"""Capture real app states; never turn capture success into a parity verdict."""
from __future__ import annotations
import argparse
import hashlib
import html
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import struct
import time
import xml.etree.ElementTree as ET
import zipfile

REFERENCE_SHA = "b690dc146009d4815825fa4a20815ef2e4ee8898"
OUT = Path("ci-ui-comparison")
PACKAGES = {"bitchord": "com.music.bitchord", "aurelis": "com.music.aurelis"}
LABELS = {
    "home": ("Home", "Inicio", "Listen Now", "Escuchar ahora"),
    "library": ("Library", "Biblioteca"),
    "settings": ("Settings", "Ajustes", "Configuración"),
    "profile": ("Profile", "Account", "Perfil", "Cuenta", "Your profile", "Open account", "Account and settings", "Cuenta y ajustes", "Switch account", "Cambiar cuenta"),
    "songs": ("Local Music", "Música local", "Songs", "Canciones"),
    "search": ("Search", "Buscar"),
    "lyrics": ("Lyrics", "Letras", "Show lyrics", "Mostrar letras", "Ver Letras", "Abrir letras", "Open lyrics"),
}


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def abis(path):
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise ValueError("Invalid APK ZIP")
        return {p.split("/")[1] for p in archive.namelist() if p.startswith("lib/") and p.endswith(".so")}


def select_reference(base, out):
    metadata = json.loads((base / "output-metadata.json").read_text())
    if metadata.get("applicationId") != PACKAGES["bitchord"]:
        raise ValueError("Reference package does not match")
    candidates = [e for e in metadata["elements"] if e.get("filters") == [{"filterType": "ABI", "value": "arm64-v8a"}]]
    if len(candidates) != 1:
        raise ValueError("Expected exactly one original ARM64 split")
    source = (base / candidates[0]["outputFile"]).resolve()
    if source.parent != base.resolve() or abis(source) != {"arm64-v8a"}:
        raise ValueError("Unsafe path or unexpected APK ABIs")
    out.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, out / "reference.apk")
    (out / "reference.json").write_text(json.dumps({
        "commit": REFERENCE_SHA, "package": PACKAGES["bitchord"],
        "version_name": candidates[0].get("versionName"),
        "original_output": source.name, "sha256": digest(source),
        "abi": "arm64-v8a", "source_modified": False, "bundle_wear": False,
    }, indent=2) + "\n")


def adb(*args, binary=False, input_data=None):
    result = subprocess.run(["adb", *map(str, args)], input=input_data,
                            capture_output=True, timeout=60)
    if result.returncode:
        raise RuntimeError("adb command failed (exit " + str(result.returncode) + "): " +
                           " ".join(map(str, args[:3])) + ": " + result.stderr.decode("utf-8", errors="replace")[-4000:])
    return result.stdout if binary else result.stdout.decode("utf-8", errors="replace").strip()


def shell(*args, **kwargs):
    return adb("shell", shlex.join(map(str, args)), **kwargs)


def nodes(xml):
    root = ET.fromstring(xml)
    result = []
    for node in root.iter("node"):
        labels = list(dict.fromkeys(value for value in (node.get("text", ""), node.get("content-desc", "")) if value))
        label = labels[0] if labels else ""
        editable = node.get("class", "").endswith("EditText")
        scrollable = node.get("scrollable") == "true"
        bounds = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
        if bounds and (label or editable or scrollable) and node.get("enabled", "true") == "true":
            left, top, right, bottom = map(int, bounds.groups())
            if right > left and bottom > top:
                result.append({"label": label, "bounds": [left, top, right, bottom], "class": node.get("class", ""), "labels": labels, "scrollable": scrollable})
    return result


def normalized(label):
    return " ".join(label.casefold().split())


def matches(rows, labels):
    wanted = {normalized(x) for x in labels}
    return [row for row in rows if any(normalized(value) in wanted
            for label in row.get("labels", [row["label"]]) for value in [label, *label.splitlines()])]


def fixture_session(dump, package, states=(2, 3)):
    """Never borrow another package's active session or merely accept a tap."""
    records = re.finditer(r"^\s*package=(\S+)[ \t]*\n(.*?)(?=^\s*package=|\Z)", dump, re.M | re.S)
    for record in records:
        if record[1] != package:
            continue
        body = record[2]
        state = re.search(r"state=PlaybackState \{state=(?:[A-Z_]+\()?(\d+)", body)
        title = re.search(r"metadata:.*?description=([^,\n]+)", body)
        if (state and int(state[1]) in states and title and title[1].strip() == "Parity Track 01"
                and re.search(r"^\s*active=true\s*$", body, re.M)):
            return True
    return False


def wait_fixture_session(package, states):
    for attempt in range(6):
        if fixture_session(shell("dumpsys", "media_session"), package, states):
            return True
        if attempt < 5:
            time.sleep(0.75)
    return False


def mini_player_rows(rows):
    """A library title is insufficient: require adjacent transport controls."""
    titles = matches(rows, ("Parity Track 01",))
    transport = matches(rows, ("Play", "Pause", "Reproducir", "Pausar", "Pausa"))
    next_controls = matches(rows, ("Next", "Siguiente"))
    def overlaps(a, b):
        return a["bounds"][1] < b["bounds"][3] and b["bounds"][1] < a["bounds"][3]
    return [title for title in titles if any(overlaps(title, control) for control in transport)
            and any(overlaps(title, control) for control in next_controls)]


def settings_root(rows):
    return bool(matches(rows, LABELS["settings"]) and (
        any(row["class"].endswith("EditText") for row in rows)
        or matches(rows, ("Account & integrations", "Perfil del dispositivo"))))


def tree():
    # XML is inspected and retained: no guessed screen coordinates.
    shell("uiautomator", "dump", "/sdcard/ui-parity.xml")
    return shell("cat", "/sdcard/ui-parity.xml")


def scroll_view(rows):
    containers = [row for row in rows if row.get("scrollable")]
    if not containers:
        return False
    row = max(containers, key=lambda n: (n["bounds"][2] - n["bounds"][0]) * (n["bounds"][3] - n["bounds"][1]))
    left, top, right, bottom = row["bounds"]
    shell("input", "swipe", (left + right) // 2, top + (bottom - top) * 3 // 4,
          (left + right) // 2, top + (bottom - top) // 4, 350)
    return True


def inspect_until(predicate, scroll=False):
    rows = []
    for attempt in range(4 if scroll else 3):
        rows = nodes(tree())
        if predicate(rows):
            return rows
        if attempt < (3 if scroll else 2):
            if scroll and not scroll_view(rows):
                break
            time.sleep(0.75)
    return rows


def tap(labels, bottom=False, scroll=False):
    rows = inspect_until(lambda current: bool(matches(current, labels)), scroll=scroll)
    found = matches(rows, labels)
    if not found:
        return False
    found.sort(key=lambda n: n["bounds"][1], reverse=bottom)
    left, top, right, end = found[0]["bounds"]
    shell("input", "tap", (left + right) // 2, (top + end) // 2)
    time.sleep(1.5)
    return True


def foreground(package):
    state = shell("dumpsys", "activity", "activities")
    return any(package + "/" in line for line in state.splitlines()
               if "mResumedActivity" in line or "topResumedActivity" in line)


def screenshot(folder):
    content = adb("exec-out", "screencap", "-p", binary=True)
    if len(content) < 24 or not content.startswith(b"\x89PNG\r\n\x1a\n") or content[12:16] != b"IHDR":
        raise ValueError("No valid screenshot returned")
    (folder / "screen.png").write_bytes(content)
    return list(struct.unpack(">II", content[16:24]))


def record_state(result, app, theme, name, reached, reason=""):
    folder = OUT / app / theme / name
    folder.mkdir(parents=True, exist_ok=True)
    try:
        xml = tree()
        (folder / "hierarchy.xml").write_text(xml)
        labels = [n["label"] for n in nodes(xml)]
        current = foreground(PACKAGES[app])
        dimensions = screenshot(folder)
        orientation_ok = dimensions[0] > dimensions[1] if name == "player-landscape" else dimensions[0] < dimensions[1]
        if not orientation_ok:
            reason = "Requested orientation was not verified in screenshot dimensions"
        if name in {"miniplayer", "player-portrait", "player-landscape", "lyrics"}:
            session = shell("dumpsys", "media_session")
            (folder / "media-session.txt").write_text(session)
            if name == "miniplayer" and not (mini_player_rows(nodes(xml)) and fixture_session(session, PACKAGES[app], (2,))):
                reached = False
                reason = "Requires visible MiniPlayer transport and this app's paused fixture session"
        result["screens"].append({"app": app, "theme": theme, "state": name,
            "reached": bool(reached and current and orientation_ok), "foreground": current, "dimensions": dimensions,
            "reason": reason if reached and current else reason or "Target navigation/foreground was not verified",
            "labels": labels, "image": str((folder / "screen.png").relative_to(OUT)),
            "visual_verdict": "NOT_REVIEWED"})
    except Exception as error:
        result["screens"].append({"app": app, "theme": theme, "state": name,
            "reached": False, "reason": str(error), "visual_verdict": "NOT_REVIEWED"})
    row = result["screens"][-1]
    print(f"UI state {app}/{theme}/{name}: {'REACHED' if row['reached'] else 'NOT_REACHED'}; {row.get('reason', '')}", flush=True)
    save(result)


def save(result):
    (OUT / "results.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")


def seed_preferences(app, theme):
    # Synthetic, disposable emulator data only. Original app source is unchanged.
    package = PACKAGES[app]
    shell("am", "force-stop", package)
    if shell("pm", "clear", package) != "Success":
        raise RuntimeError("Disposable comparison app data could not be reset")
    shell("pm", "grant", package, "android.permission.READ_MEDIA_AUDIO")
    shell("pm", "grant", package, "android.permission.POST_NOTIFICATIONS")
    shell("cmd", "locale", "set-app-locales", package, "--user", "0", "--locales", "en")
    locale = shell("cmd", "locale", "get-app-locales", package, "--user", "0")
    if "[en]" not in locale:
        raise RuntimeError("Comparison app locale was not verified: " + locale)
    if app == "bitchord":
        filename = "bitchord_settings.xml"
        body = f'<map><boolean name="onboarding_complete" value="true"/><boolean name="liquid_glass" value="true"/><string name="theme_mode">{theme.upper()}</string></map>'
    else:
        filename = "aurelis_settings.xml"
        body = f'<map><boolean name="liquid_glass" value="true"/><int name="theme_mode" value="{1 if theme == "light" else 2}"/></map>'
    shell("run-as", package, "mkdir", "-p", "shared_prefs")
    shell("run-as", package, "sh", "-c", f"cat > shared_prefs/{filename}", input_data=body.encode())
    return {"reset_app_data": True, "liquid_glass": True, "locale": locale, "theme": theme,
            "font_exception": "Reference default; Aurelis authorized system font"}


def launch(package):
    shell("am", "start", "-W", "-f", "0x10008000", "-n", package + "/.MainActivity")
    time.sleep(4)


def verify_apk(path, package):
    if abis(path) != {"arm64-v8a"}:
        raise ValueError("APK must contain only ARM64 native libraries")
    sdk = Path(os.environ["ANDROID_HOME"])
    tools = sorted(sdk.glob("build-tools/*/aapt"))
    if not tools:
        raise ValueError("Missing aapt for APK package verification")
    badging = subprocess.run([str(tools[-1]), "dump", "badging", str(path)], capture_output=True, text=True, check=True).stdout
    if not re.search(r"^package: name='" + re.escape(package) + "'", badging, re.MULTILINE):
        raise ValueError("APK manifest package mismatch")
    return {"sha256": digest(path), "package": package, "abi": "arm64-v8a"}


def capture():
    OUT.mkdir(exist_ok=True)
    result = {"schema": 1, "aurelis_commit": os.environ.get("GITHUB_SHA"), "reference_commit": REFERENCE_SHA,
        "visual_verdict": "NOT_REVIEWED", "screens": [], "capture_complete": False, "stage": "provenance",
        "not_tested": ["widgets on a launcher", "gestures and animations in motion", "physical device", "pixel identity"],
        "font_policy": "Original reference default; Aurelis authorized system font. Difference remains visible.",
        "fixture_preferences": "Disposable emulator only: reset each app/theme; explicit theme, Liquid Glass and locale; skip reference remote onboarding",
        "scenario_preferences": {},
        "scope": "Evidence gathering only; Android Auto/TV/Wear excluded"}
    save(result)
    try:
        reference = Path("comparison-reference/reference.apk")
        provenance = json.loads(Path("comparison-reference/reference.json").read_text())
        if provenance.get("commit") != REFERENCE_SHA or provenance.get("sha256") != digest(reference):
            raise ValueError("Reference provenance mismatch")
        apks = {"bitchord": reference, "aurelis": Path("app/build/outputs/apk/debug/app-debug.apk")}
        primary = json.loads(Path("comparison-aurelis/primary-run.json").read_text())
        result["aurelis_build_source"] = {key: primary[key] for key in ("id", "run_number", "run_attempt", "head_sha", "path")}
        result["apks"] = {app: verify_apk(path, PACKAGES[app]) for app, path in apks.items()}
        result["stage"] = "device_configuration"
        if shell("getprop", "ro.build.version.sdk") != "35":
            raise ValueError("Expected API35")
        shell("wm", "size", "1080x1920"); shell("wm", "density", "480")
        shell("settings", "put", "system", "font_scale", "1.0")
        shell("settings", "put", "system", "accelerometer_rotation", "0")
        shell("settings", "put", "system", "user_rotation", "0")
        result["stage"] = "offline_configuration"
        # No remote credentials/content are used. Internet access is disabled for captures.
        shell("cmd", "connectivity", "airplane-mode", "enable")
        shell("svc", "wifi", "disable"); shell("svc", "data", "disable")
        time.sleep(2)
        network = shell("dumpsys", "connectivity")
        (OUT / "connectivity.txt").write_text(network)
        if "Active default network: none" not in network:
            raise RuntimeError("Offline fixture conditions were not verified")
        result["device"] = {"size": shell("wm", "size"), "density": shell("wm", "density"),
            "font_scale": shell("settings", "get", "system", "font_scale"),
            "locale": shell("getprop", "persist.sys.locale"), "api": "35",
            "abis": shell("getprop", "ro.product.cpu.abilist"), "hardware_audio_verified": False}
        result["stage"] = "media_indexing"
        fixtures = Path("comparison-media")
        result["fixtures"] = json.loads((fixtures / "manifest.json").read_text())
        media = "/sdcard/Music/Parity"
        shell("mkdir", "-p", media)
        for path in sorted(fixtures.glob("*")):
            if path.suffix in {".flac", ".lrc"}:
                adb("push", path, media + "/" + path.name)
                shell("am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", "file://" + media + "/" + path.name)
        for _ in range(20):
            catalog = shell("content", "query", "--uri", "content://media/external/audio/media", "--projection", "_display_name")
            if all(f"Parity Track {i:02}.flac" in catalog for i in range(1, 4)):
                break
            time.sleep(1)
        else:
            raise RuntimeError("The three identical fixture songs were not indexed")
        (OUT / "media-catalog.txt").write_text(catalog)
        for app, path in apks.items():
            result["stage"] = "install:" + app
            package = PACKAGES[app]
            adb("install", "-r", path)
            for theme in ("dark", "light"):
                result["stage"] = "capture:" + app + "/" + theme
                shell("settings", "put", "system", "user_rotation", "0")
                result["scenario_preferences"][app + "/" + theme] = seed_preferences(app, theme)
                launch(package)
                labels = nodes(tree())
                home = bool(matches(labels, LABELS["home"]) and matches(labels, LABELS["library"]))
                record_state(result, app, theme, "home", home)
                library = tap(LABELS["library"])
                record_state(result, app, theme, "library", library and bool(matches(nodes(tree()), LABELS["songs"])))
                settings = tap(LABELS["profile"])
                if not settings:
                    settings = tap(LABELS["settings"], scroll=True)
                inspected = inspect_until(settings_root)
                record_state(result, app, theme, "settings", settings and settings_root(inspected))
                # Reset navigation through a force-stop/launch without resetting library/preferences.
                shell("am", "force-stop", package); launch(package)
                search = tap(LABELS["search"])
                search_nodes = inspect_until(lambda rows: any(n["class"].endswith("EditText") for n in rows))
                record_state(result, app, theme, "search", search and any(n["class"].endswith("EditText") for n in search_nodes))
                shell("am", "force-stop", package); launch(package)
                local = tap(LABELS["library"]) and tap(LABELS["songs"], scroll=True)
                record_state(result, app, theme, "local-songs", local and bool(matches(nodes(tree()), ("Parity Track 01",))))
                playing = False
                for _ in range(2 if local else 0):
                    # Use the visible header action, not a row clipped under floating bars.
                    if tap(("Play", "Reproducir")) and wait_fixture_session(package, (3,)):
                        playing = True
                        break
                paused = playing and tap(("Pause", "Pausar", "Pausa"), bottom=True) and wait_fixture_session(package, (2,))
                mini = paused and bool(mini_player_rows(nodes(tree())))
                record_state(result, app, theme, "miniplayer", mini)
                expanded = mini and (tap(("Abrir reproductor", "Open player", "Expand player")) or tap(("Parity Track 01",), bottom=True))
                player_markers = LABELS["lyrics"] + ("Queue", "Cola", "Abrir cola", "Up next", "Cerrar reproductor")
                player_labels = inspect_until(lambda rows: bool(matches(rows, player_markers)))
                verified = bool(matches(player_labels, player_markers))
                record_state(result, app, theme, "player-portrait", expanded and verified)
                shell("settings", "put", "system", "user_rotation", "1"); time.sleep(2)
                record_state(result, app, theme, "player-landscape", expanded and verified and bool(matches(nodes(tree()), ("Parity Track 01",))) and bool(matches(nodes(tree()), player_markers)))
                shell("settings", "put", "system", "user_rotation", "0"); time.sleep(2)
                lyrics = expanded and tap(LABELS["lyrics"], scroll=True)
                lyric_nodes = inspect_until(lambda rows: any("Parity line" in n["label"] for n in rows))
                text = [n["label"] for n in lyric_nodes]
                record_state(result, app, theme, "lyrics", lyrics and any("Parity line" in label for label in text),
                    "Requires visible local fixture lyrics; a lyrics button alone is insufficient")
            shell("am", "force-stop", package)
        (OUT / "widget-registration.txt").write_text(shell("dumpsys", "appwidget"))
        result["capture_complete"] = len(result["screens"]) == 36 and all(row["reached"] for row in result["screens"])
    except Exception as error:
        result["error"] = str(error)
        print(f"UI capture FAILED at {result['stage']}: {error}", flush=True)
    finally:
        save(result)
    return 0 if result["capture_complete"] else 1


def report():
    OUT.mkdir(exist_ok=True)
    path = OUT / "results.json"
    result = json.loads(path.read_text()) if path.exists() else {"error": "Capture never produced results", "screens": []}
    result["visual_verdict"] = "NOT_REVIEWED"
    save(result)
    summary = {key: result.get(key) for key in ("stage", "error", "capture_complete", "visual_verdict")}
    summary["captured_states"] = len(result.get("screens", []))
    summary["unreached"] = [{key: row.get(key) for key in ("app", "theme", "state", "reason", "labels")}
                            for row in result.get("screens", []) if not row.get("reached")]
    diagnostic = json.dumps(summary, ensure_ascii=False, indent=2)
    print("UI comparison diagnostic:\n" + diagnostic, flush=True)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as stream:
            stream.write("## UI comparison — review pending\n\n```json\n" + diagnostic + "\n```\n")
    parts = ['<!doctype html><meta charset="utf-8"><title>BitChord / Aurelis</title>',
        '<style>body{font:16px system-ui;background:#eee;color:#111}section{margin:24px}.pair{display:flex;gap:16px}.pair figure{flex:1;min-width:0;margin:0}img{max-width:100%;vertical-align:top}pre{white-space:pre-wrap}</style>',
        '<h1>BitChord / Aurelis — visual review pending</h1>',
        '<p>Capture success is not visual equivalence. Unreached targets are labeled. Brand, permitted fonts and local-only scope remain visible.</p>',
        '<pre>' + html.escape(json.dumps({k: v for k, v in result.items() if k != "screens"}, ensure_ascii=False, indent=2)) + '</pre>']
    rows = {(r["app"], r["theme"], r["state"]): r for r in result.get("screens", [])}
    for theme, state in sorted({(r["theme"], r["state"]) for r in result.get("screens", [])}):
        parts.append('<section><h2>' + html.escape(theme + ' / ' + state) + '</h2><div class="pair">')
        for app in PACKAGES:
            row = rows.get((app, theme, state), {})
            parts.append('<figure><figcaption>' + html.escape(app + ': ' + ('REACHED — review pending' if row.get('reached') else 'NOT REACHED') + ' ' + row.get('reason', '')) + '</figcaption>')
            image = row.get("image", "")
            if image and (OUT / image).is_file():
                parts.append('<img alt="' + app + '" src="' + html.escape(image, quote=True) + '">')
            parts.append('</figure>')
        parts.append('</div></section>')
    (OUT / "index.html").write_text('\n'.join(parts), encoding="utf-8")
    (OUT / "REVIEW_PENDING.md").write_text(
        "# UI comparison\n\nVisual equivalence: NOT REVIEWED.\nCapture complete: " + str(result.get("capture_complete", False)) +
        "\nSee index.html, results.json and per-state PNG/XML.\nWidgets, motion, accessibility and physical validation remain pending.\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["select-reference", "capture", "report"])
    parser.add_argument("paths", nargs="*")
    args = parser.parse_args()
    if args.command == "select-reference":
        select_reference(*map(Path, args.paths))
    elif args.command == "capture":
        raise SystemExit(capture())
    else:
        report()
