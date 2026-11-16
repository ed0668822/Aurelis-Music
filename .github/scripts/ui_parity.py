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
SYSTEM_DIALOG_EVENTS = []
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
    try:
        result = subprocess.run(["adb", *map(str, args)], input=input_data,
                                capture_output=True, timeout=60)
    except subprocess.TimeoutExpired as error:
        def partial(value):
            return (value.decode("utf-8", errors="replace") if isinstance(value, bytes) else value or "")[-4000:]
        raise RuntimeError("adb timed out after 60 seconds: " + " ".join(map(str, args[:3])) +
            "; stdout=" + partial(error.stdout) + "; stderr=" + partial(error.stderr)) from error
    if result.returncode:
        raise RuntimeError("adb command failed (exit " + str(result.returncode) + "): " +
                           " ".join(map(str, args[:3])) + ": stderr=" + result.stderr.decode("utf-8", errors="replace")[-4000:]
                           + "; stdout=" + result.stdout.decode("utf-8", errors="replace")[-4000:])
    return result.stdout if binary else result.stdout.decode("utf-8", errors="replace").strip()


def shell(*args, **kwargs):
    return adb("shell", shlex.join(map(str, args)), **kwargs)


def prepare_device(stage):
    """Boot completion alone does not establish an unlocked, interactive fixture."""
    shell("svc", "power", "stayon", "true")
    shell("input", "keyevent", "KEYCODE_WAKEUP")
    shell("wm", "dismiss-keyguard")
    user = shell("am", "get-current-user")
    if not re.fullmatch(r"\d+", user):
        raise RuntimeError("Cannot verify emulator current user: " + user)
    observations = []
    for attempt in range(15):
        state = shell("am", "get-started-user-state", user)
        observations.append(state)
        (OUT / (stage + "-user-state.json")).write_text(json.dumps(
            {"user": user, "observations": observations}, indent=2) + "\n")
        if state == "RUNNING_UNLOCKED":
            return
        if attempt < 14:
            time.sleep(1)
    raise RuntimeError("Emulator user is not RUNNING_UNLOCKED: " + state)


def push_fixture(path, remote):
    """Retain bounded create failures; never retry transport or hash failures."""
    for attempt in range(3):
        try:
            adb("push", path, remote)
            break
        except RuntimeError as error:
            events_path = OUT / "fixture-transfer-errors.json"
            events = json.loads(events_path.read_text()) if events_path.exists() else []
            events.append({"file": path.name, "attempt": attempt + 1, "error": str(error)})
            events_path.write_text(json.dumps(events, indent=2) + "\n")
            print("Fixture transfer diagnostic: " + str(error), flush=True)
            if attempt == 2 or "remote couldn't create file: Operation not permitted" not in str(error):
                raise
            time.sleep(1)
    transferred = shell("sha256sum", remote).split()
    if not transferred or transferred[0] != digest(path):
        raise RuntimeError("Fixture bytes differ after transfer: " + path.name)


def nodes(xml, include_disabled=False):
    root = ET.fromstring(xml)
    result = []
    for node in root.iter("node"):
        labels = list(dict.fromkeys(value for value in (node.get("text", ""), node.get("content-desc", "")) if value))
        label = labels[0] if labels else ""
        editable = node.get("class", "").endswith("EditText")
        scrollable = node.get("scrollable") == "true"
        bounds = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
        if (bounds and (label or editable or scrollable) and node.get("visible-to-user", "true") != "false"
                and (include_disabled or node.get("enabled", "true") == "true")):
            left, top, right, bottom = map(int, bounds.groups())
            if right > left and bottom > top:
                result.append({"label": label, "bounds": [left, top, right, bottom], "class": node.get("class", ""), "labels": labels, "scrollable": scrollable,
                               "enabled": node.get("enabled", "true") == "true", "package": node.get("package", "")})
    return result


def normalized(label):
    return " ".join(label.casefold().split())


def matches(rows, labels):
    wanted = {normalized(x) for x in labels}
    return [row for row in rows if any(normalized(value) in wanted
            for label in row.get("labels", [row["label"]]) for value in [label, *label.splitlines()])]


def session_snapshot(dump, package):
    """Read only this package's active session, including real capabilities."""
    records = re.finditer(r"^\s*package=(\S+)[ \t]*\n(.*?)(?=^\s*package=|\Z)", dump, re.M | re.S)
    for record in records:
        if record[1] != package:
            continue
        body = record[2]
        state = re.search(r"state=PlaybackState \{state=(?:[A-Z_]+\()?(\d+)", body)
        title = re.search(r"metadata:.*?description=([^,\n]+)", body)
        actions = re.search(r"\bactions=(\d+)", body)
        queue = re.search(r"queueTitle=.*?, size=(\d+)", body)
        if state and title and re.search(r"^\s*active=true\s*$", body, re.M):
            return {"state": int(state[1]), "title": title[1].strip(),
                    "actions": int(actions[1]) if actions else None,
                    "queue_size": int(queue[1]) if queue else None}
    return None


def fixture_session(dump, package, states=(2, 3)):
    snapshot = session_snapshot(dump, package)
    return bool(snapshot and snapshot["state"] in states and snapshot["title"] == "Parity Track 01")


TRANSPORT = {"play": ("Play", "Reproducir"), "pause": ("Pause", "Pausar", "Pausa"),
             "next": ("Next", "Siguiente"), "previous": ("Previous", "Anterior")}


def player_transport(xml, dump, package, dimensions):
    """Check the initial viewport; disabled controls must agree with session actions."""
    snapshot = session_snapshot(dump, package)
    if not snapshot or snapshot["state"] != 2 or snapshot["title"] != "Parity Track 01" or snapshot["actions"] is None:
        return {"visible": False, "reason": "Own paused fixture session/capabilities missing"}
    rows = [r for r in nodes(xml, include_disabled=True) if r["package"] == package
            and 0 <= r["bounds"][0] < r["bounds"][2] <= dimensions[0]
            and 0 <= r["bounds"][1] < r["bounds"][3] <= dimensions[1]]
    if not matches(rows, ("Parity Track 01",)):
        return {"visible": False, "reason": "Fixture title missing from own viewport"}
    controls = {}
    for name, bit in (("play", 4), ("next", 32), ("previous", 16)):
        found = matches(rows, TRANSPORT[name])
        if len(found) != 1 or found[0]["enabled"] != bool(snapshot["actions"] & bit):
            return {"visible": False, "reason": "Missing, ambiguous or capability-mismatched transport: " + name}
        controls[name] = found[0]
    if not controls["play"]["enabled"]:
        return {"visible": False, "reason": "Paused fixture cannot be played"}
    return {"visible": True, "controls": controls, "session": snapshot}


def exercise_transport(package, folder):
    """Exercise UI controls, retaining every observed session; never scroll to pass."""
    evidence = {"passed": False, "steps": []}
    def observe(name, titles, states):
        for attempt in range(6):
            dump = shell("dumpsys", "media_session")
            (folder / ("transport-" + name + ".txt")).write_text(dump)
            snapshot = session_snapshot(dump, package)
            if snapshot and snapshot["title"] in titles and snapshot["state"] in states and snapshot["queue_size"] == 3:
                evidence["steps"].append({"step": name, "session": snapshot})
                return snapshot
            if attempt < 5:
                time.sleep(0.5)
        raise RuntimeError("Transport did not reach expected fixture state: " + name)
    try:
        for name, titles, states in (("play", ("Parity Track 01",), (3,)),
                                     ("pause", ("Parity Track 01",), (2,)),
                                     ("next", ("Parity Track 02", "Parity Track 03"), (2,)),
                                     ("previous", ("Parity Track 01",), (2,))):
            rows = [r for r in nodes(tree()) if r["package"] == package]
            found = matches(rows, TRANSPORT[name])
            if len(found) != 1:
                raise RuntimeError("Enabled transport control missing or ambiguous: " + name)
            left, top, right, bottom = found[0]["bounds"]
            shell("input", "tap", (left + right) // 2, (top + bottom) // 2)
            observe(name, titles, states)
        evidence["passed"] = True
    except RuntimeError as error:
        evidence["error"] = str(error)
    (folder / "transport-results.json").write_text(json.dumps(evidence, indent=2) + "\n")
    return evidence


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


def expand_miniplayer():
    rows = inspect_until(lambda current: bool(mini_player_rows(current)))
    candidates = mini_player_rows(rows)
    if not candidates:
        return False
    # A song list may extend BELOW the dock. Never select the lowest title globally.
    target = min(candidates, key=lambda row: (row["bounds"][2] - row["bounds"][0]) * (row["bounds"][3] - row["bounds"][1]))
    left, top, right, bottom = target["bounds"]
    shell("input", "tap", (left + right) // 2, (top + bottom) // 2)
    time.sleep(1.5)
    return True


def settings_root(rows):
    return bool(matches(rows, LABELS["settings"]) and (
        any(row["class"].endswith("EditText") for row in rows)
        or matches(rows, ("Account & integrations", "Perfil del dispositivo"))))


def raw_tree():
    # XML is inspected and retained: no guessed screen coordinates.
    for attempt in range(3):
        try:
            shell("rm", "-f", "/sdcard/ui-parity.xml")
            output = shell("uiautomator", "dump", "/sdcard/ui-parity.xml")
            if re.search(r"error|null root|could not get", output, re.I):
                raise RuntimeError("UIAutomator snapshot failed: " + output)
            return shell("cat", "/sdcard/ui-parity.xml")
        except RuntimeError as error:
            # Retry only known transient snapshot errors. A dead device still fails.
            if attempt == 2 or not re.search(r"could not get idle state|null root node|could not get root node|ui-parity\.xml: No such file or directory", str(error), re.I):
                raise
            time.sleep(0.75)


def system_error_dialog(xml):
    rows = list(ET.fromstring(xml).iter("node"))
    close = [node for node in rows if node.get("package") == "android"
             and node.get("resource-id") == "android:id/aerr_close"]
    if not close:
        return None
    title = next((node.get("text", "") for node in rows
                  if node.get("package") == "android" and node.get("resource-id") == "android:id/alertTitle"), "")
    return {"title": title, "close": close[0]}


def tree():
    xml = raw_tree()
    dialog = system_error_dialog(xml)
    if dialog is None:
        return xml
    folder = OUT / "system-dialogs" / str(len(SYSTEM_DIALOG_EVENTS) + 1)
    folder.mkdir(parents=True, exist_ok=True)
    (folder / "hierarchy.xml").write_text(xml)
    event = {"title": dialog["title"], "resolved": False,
             "evidence": str(folder.relative_to(OUT)), "action": None}
    SYSTEM_DIALOG_EVENTS.append(event)
    def save_events():
        (OUT / "system-dialog-events.json").write_text(json.dumps(SYSTEM_DIALOG_EVENTS, indent=2) + "\n")
    save_events()
    screenshot(folder)
    # Only the confirmed disposable Pixel Launcher ANR may be dismissed, once.
    # Tested-app ANRs/crashes and unknown system errors must remain failures.
    if dialog["title"].replace("’", "'") != "Pixel Launcher isn't responding":
        raise RuntimeError("Blocking system error: " + dialog["title"])
    if len(SYSTEM_DIALOG_EVENTS) != 1:
        raise RuntimeError("Pixel Launcher ANR recurred; emulator is not healthy")
    launcher = shell("cmd", "package", "resolve-activity", "--brief", "-a", "android.intent.action.MAIN",
                     "-c", "android.intent.category.HOME")
    if not re.search(r"^com\.google\.android\.apps\.nexuslauncher/\S+$", launcher, re.M):
        raise RuntimeError("Pixel Launcher ANR owner was not verified")
    close = dialog["close"]
    bounds = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", close.get("bounds", ""))
    if not bounds or close.get("enabled") != "true":
        raise RuntimeError("Pixel Launcher close action is unavailable")
    left, top, right, bottom = map(int, bounds.groups())
    if right <= left or bottom <= top:
        raise RuntimeError("Pixel Launcher close action has invalid bounds")
    event["action"] = "close_verified_launcher_anr"
    save_events()
    print("UI environment recovery: closing verified Pixel Launcher ANR once; original evidence retained", flush=True)
    shell("input", "tap", (left + right) // 2, (top + bottom) // 2)
    time.sleep(1)
    xml = raw_tree()
    if system_error_dialog(xml) is not None:
        raise RuntimeError("System error remains after Pixel Launcher recovery")
    event["resolved"] = True
    save_events()
    return xml


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


def foreground(package, folder=None):
    state = shell("dumpsys", "activity", "activities")
    resumed = any(re.search(r"\b" + re.escape(package) + r"/", line) for line in state.splitlines()
                  if "mResumedActivity" in line or "topResumedActivity" in line)
    # The windows-only subcommand omits per-display focus on some Android builds.
    focus = shell("dumpsys", "window")
    focused = any(re.search(r"\b" + re.escape(package) + r"/", line)
                  for line in focus.splitlines() if "mCurrentFocus=" in line)
    if folder is not None:
        (folder / "activity.txt").write_text(state)
        (folder / "window.txt").write_text(focus)
    if not resumed or not focused:
        print("Foreground check: " + package + f" resumed={resumed} focused={bool(focused)}; " +
              " | ".join(line.strip() for line in focus.splitlines() if "mCurrentFocus" in line), flush=True)
    return resumed and focused


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
        current = foreground(PACKAGES[app], folder)
        if not current:
            # Preserve the tested app's exit reason before the next force-stop.
            for filename, command in (
                ("exit-info.txt", ("dumpsys", "activity", "exit-info", PACKAGES[app])),
                ("crash-buffer.txt", ("logcat", "-d", "-b", "crash", "-v", "threadtime")),
            ):
                try:
                    diagnostic = shell(*command)
                    (folder / filename).write_text(diagnostic)
                    print(f"UI foreground failure {app}/{theme}/{name} {filename}:\n" + diagnostic[-16000:], flush=True)
                except RuntimeError as error:
                    print("UI foreground diagnostic unavailable: " + str(error), flush=True)
        dimensions = screenshot(folder)
        orientation_ok = dimensions[0] > dimensions[1] if name == "player-landscape" else dimensions[0] < dimensions[1]
        if not orientation_ok:
            reason = "Requested orientation was not verified in screenshot dimensions"
        transport = None
        panel_opened = bool(reached and current and orientation_ok)
        if name in {"miniplayer", "player-portrait", "player-landscape", "lyrics"}:
            session = shell("dumpsys", "media_session")
            (folder / "media-session.txt").write_text(session)
            if name == "miniplayer" and not (mini_player_rows(nodes(xml)) and fixture_session(session, PACKAGES[app], (2,))):
                reached = False
                reason = "Requires visible MiniPlayer transport and this app's paused fixture session"
            if name == "lyrics" and not fixture_session(session, PACKAGES[app], (2,)):
                reached = False
                reason = "Lyrics require this app's own paused fixture session"
            if name in {"player-portrait", "player-landscape"} and current and orientation_ok:
                transport = player_transport(xml, session, PACKAGES[app], dimensions)
                if not transport["visible"]:
                    reached = False
                    reason = transport["reason"]
                elif reached:
                    transport["interaction"] = exercise_transport(PACKAGES[app], folder)
                    if not transport["interaction"]["passed"]:
                        reached = False
                        reason = "Visible player transport failed interaction verification"
        result["screens"].append({"app": app, "theme": theme, "state": name,
            "reached": bool(reached and current and orientation_ok), "foreground": current, "dimensions": dimensions,
            "reason": reason if reached and current else reason or "Target navigation/foreground was not verified",
            "labels": labels, "image": str((folder / "screen.png").relative_to(OUT)),
            "visual_verdict": "NOT_REVIEWED"})
        if name in {"player-portrait", "player-landscape"}:
            result["screens"][-1].update({"panel_opened": panel_opened, "transport": transport})
    except Exception as error:
        result["screens"].append({"app": app, "theme": theme, "state": name,
            "reached": False, "reason": str(error), "visual_verdict": "NOT_REVIEWED"})
    row = result["screens"][-1]
    print(f"UI state {app}/{theme}/{name}: {'REACHED' if row['reached'] else 'NOT_REACHED'}; {row.get('reason', '')}", flush=True)
    save(result)


def save(result):
    (OUT / "results.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")


def material_mode():
    mode = os.environ.get("UI_COMPARISON_MATERIAL", "glass")
    if mode not in {"plain", "glass"}:
        raise ValueError("Unknown comparison material mode")
    return mode


def seed_preferences(app, theme):
    # Synthetic, disposable emulator data only. Original app source is unchanged.
    package = PACKAGES[app]
    mode = material_mode()
    glass = mode == "glass"
    common = f'<boolean name="liquid_glass" value="{str(glass).lower()}"/><boolean name="reduce_animation" value="true"/>'
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
        body = f'<map><boolean name="onboarding_complete" value="true"/>{common}<string name="theme_mode">{theme.upper()}</string></map>'
    else:
        filename = "aurelis_settings.xml"
        body = f'<map>{common}<int name="theme_mode" value="{1 if theme == "light" else 2}"/></map>'
    shell("run-as", package, "mkdir", "-p", "shared_prefs")
    shell("run-as", package, "sh", "-c", f"cat > shared_prefs/{filename}", input_data=body.encode())
    return {"reset_app_data": True, "liquid_glass": glass, "reduce_animation": True, "material_mode": mode, "locale": locale, "theme": theme,
            "font_exception": "Reference default; Aurelis authorized system font"}


def launch(package):
    shell("am", "start", "-W", "-f", "0x10008000", "-n", package + "/.MainActivity")
    time.sleep(4)


def launch_scenario(app, theme, rotation):
    """Static screenshots: configure orientation before starting either app."""
    if rotation not in (0, 1):
        raise ValueError("Unsupported comparison orientation")
    package = PACKAGES[app]
    shell("am", "force-stop", package)
    preferences = seed_preferences(app, theme)
    shell("wm", "fixed-to-user-rotation", "enabled")
    shell("wm", "user-rotation", "lock", str(rotation))
    lock = shell("wm", "user-rotation")
    if lock != "lock " + str(rotation):
        raise RuntimeError("WindowManager rotation lock was not verified: " + lock)
    preferences["rotation"] = rotation
    preferences["window_rotation_lock"] = lock
    launch(package)
    return preferences


def local_library_fixture(rows):
    if matches(rows, ("Parity Track 01",)):
        return True
    # Preserve the initial viewport: a large header can put songs below the fold.
    # Indexing already verified all three fixtures; require the collection header,
    # its real count, transport and category controls rather than a Library summary.
    return all(matches(rows, labels) for labels in (
        ("Local Music", "Música local"), ("Play", "Reproducir"),
        ("Shuffle", "Aleatorio"), ("Artists", "Artistas"), ("Albums", "Álbumes"),
    )) and any(re.fullmatch(r"3 (?:songs|canciones)(?:\s*[·•]\s*.+)?", row["label"], re.I) for row in rows)


def pause_fixture(package, local):
    for _ in range(2 if local else 0):
        if tap(("Play", "Reproducir"), scroll=True) and wait_fixture_session(package, (3,)):
            return tap(("Pause", "Pausar", "Pausa"), bottom=True) and wait_fixture_session(package, (2,))
    return False


def expand_fixture(paused):
    # Opening alone is insufficient: record_state validates the initial transport
    # and its real interactions separately, without scrolling the screenshot.
    if not paused:
        return False
    expanded = paused and (tap(("Abrir reproductor", "Open player", "Expand player")) or expand_miniplayer())
    markers = LABELS["lyrics"] + ("Queue", "Cola", "Abrir cola", "Up next", "Cerrar reproductor")
    rows = inspect_until(lambda current: bool(matches(current, markers)))
    return expanded and bool(matches(rows, markers)) and bool(matches(rows, ("Parity Track 01",)))


def expand_landscape_fixture(package, paused):
    # Initialize transport in portrait, where both local-library pages expose it.
    # Rotate the paused MiniPlayer before opening the independent full player.
    if not paused:
        return False
    shell("wm", "user-rotation", "lock", "1")
    lock = shell("wm", "user-rotation")
    if lock != "lock 1":
        raise RuntimeError("Landscape WindowManager lock was not verified: " + lock)
    return wait_fixture_session(package, (2,)) and expand_fixture(True)


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


def wait_offline_fixture():
    # Android disconnects asynchronously after airplane mode and svc changes.
    # Preserve the last observed state and never accept an unverified connection.
    for attempt in range(15):
        network = shell("dumpsys", "connectivity")
        (OUT / "connectivity.txt").write_text(network)
        if "Active default network: none" in network:
            return network
        if attempt < 14:
            time.sleep(1)
    state = next((line.strip() for line in network.splitlines()
                  if line.strip().startswith("Active default network:")), "default network status missing")
    raise RuntimeError("Offline fixture conditions were not verified after 15 connectivity snapshots; last state: " + state[:200])


def capture():
    from ui_parity_host import HostMonitor
    OUT.mkdir(exist_ok=True)
    SYSTEM_DIALOG_EVENTS.clear()
    logcat = None
    logcat_stream = None
    monitor = HostMonitor(OUT / "host")
    result = {"schema": 1, "aurelis_commit": os.environ.get("GITHUB_SHA"), "reference_commit": REFERENCE_SHA,
        "visual_verdict": "NOT_REVIEWED", "screens": [], "capture_complete": False, "stage": "provenance",
        "not_tested": ["widgets on a launcher", "gestures and animations in motion", "continuous rotation with player open", "cold landscape library playback initiation", "physical device", "pixel identity"],
        "font_policy": "Original reference default; Aurelis authorized system font. Difference remains visible.",
        "fixture_preferences": "Disposable emulator only: reset app/theme; matched material and reduced motion, explicit locale; skip reference remote onboarding",
        "material_mode": material_mode(),
        "scenario_preferences": {},
        "scope": "Evidence gathering only; Android Auto/TV/Wear excluded"}
    save(result)
    try:
        monitor.start()
        reference = Path("comparison-reference/reference.apk")
        provenance = json.loads(Path("comparison-reference/reference.json").read_text())
        if provenance.get("commit") != REFERENCE_SHA or provenance.get("sha256") != digest(reference):
            raise ValueError("Reference provenance mismatch")
        apks = {"aurelis": Path("app/build/outputs/apk/debug/app-debug.apk"), "bitchord": reference}
        primary = json.loads(Path("comparison-aurelis/primary-run.json").read_text())
        result["aurelis_build_source"] = {key: primary[key] for key in ("id", "run_number", "run_attempt", "head_sha", "path")}
        result["apks"] = {app: verify_apk(path, PACKAGES[app]) for app, path in apks.items()}
        result["stage"] = "device_configuration"
        if shell("getprop", "ro.build.version.sdk") != "35":
            raise ValueError("Expected API35")
        prepare_device("capture")
        # Stream before navigating: a dead emulator cannot answer a final logcat dump.
        logcat_stream = (OUT / "device-logcat.txt").open("wb")
        logcat = subprocess.Popen(["adb", "logcat", "-v", "threadtime", "-b", "main", "-b", "crash", "-b", "system", "-b", "events"],
                                  stdout=logcat_stream, stderr=subprocess.STDOUT)
        shell("wm", "size", "1080x1920"); shell("wm", "density", "480")
        shell("settings", "put", "system", "font_scale", "1.0")
        shell("settings", "put", "system", "accelerometer_rotation", "0")
        shell("settings", "put", "system", "user_rotation", "0")
        result["stage"] = "offline_configuration"
        # No remote credentials/content are used. Internet access is disabled for captures.
        shell("cmd", "connectivity", "airplane-mode", "enable")
        shell("svc", "wifi", "disable"); shell("svc", "data", "disable")
        wait_offline_fixture()
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
                push_fixture(path, media + "/" + path.name)
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
        (OUT / "widget-registration.txt").write_text(shell("dumpsys", "appwidget"))
        for app in apks:
            package = PACKAGES[app]
            for theme in ("dark", "light"):
                result["stage"] = "capture:" + app + "/" + theme
                result["scenario_preferences"][app + "/" + theme] = launch_scenario(app, theme, 0)
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
                record_state(result, app, theme, "local-songs", local and local_library_fixture(nodes(tree())),
                    "Initial viewport: fixture title or local header with three indexed tracks required")
                paused = pause_fixture(package, local)
                mini = paused and bool(mini_player_rows(nodes(tree())))
                record_state(result, app, theme, "miniplayer", mini)
                result["stage"] = "capture:" + app + "/" + theme + "/player-portrait"
                expanded = expand_fixture(mini)
                record_state(result, app, theme, "player-portrait", expanded)
                result["stage"] = "capture:" + app + "/" + theme + "/lyrics"
                visible_lyrics = any("Parity line" in n["label"] for n in nodes(tree()))
                lyrics = expanded and (visible_lyrics or tap(LABELS["lyrics"], scroll=True))
                lyric_nodes = inspect_until(lambda rows: any("Parity line" in n["label"] for n in rows))
                text = [n["label"] for n in lyric_nodes]
                record_state(result, app, theme, "lyrics", lyrics and any("Parity line" in label for label in text),
                    "Requires visible local fixture lyrics; a lyrics button alone is insufficient")
                result["stage"] = "capture:" + app + "/" + theme + "/player-landscape"
                preferences = launch_scenario(app, theme, 0)
                preferences["initial_rotation"] = preferences.pop("rotation")
                preferences["initial_window_rotation_lock"] = preferences.pop("window_rotation_lock")
                preferences["capture_rotation_requested"] = 1
                preferences["player_setup"] = "Pause fixture in portrait; rotate MiniPlayer before opening full player"
                result["scenario_preferences"][app + "/" + theme + "/landscape"] = preferences
                local = tap(LABELS["library"]) and tap(LABELS["songs"], scroll=True)
                record_state(result, app, theme, "player-landscape", expand_landscape_fixture(package, pause_fixture(package, local)))
                shell("am", "force-stop", package)
            shell("am", "force-stop", package)
        (OUT / "widget-registration.txt").write_text(shell("dumpsys", "appwidget"))
        result["capture_complete"] = len(result["screens"]) == 36 and all(row["reached"] for row in result["screens"])
    except Exception as error:
        result["error"] = str(error)
        print(f"UI capture FAILED at {result['stage']}: {error}", flush=True)
    finally:
        # This executes before emulator-runner tears the VM down, even on ADB loss.
        result["host_monitor"] = monitor.stop()
        result["system_dialog_events"] = SYSTEM_DIALOG_EVENTS.copy()
        if logcat is not None:
            logcat.terminate()
            try:
                logcat.wait(timeout=5)
            except subprocess.TimeoutExpired:
                logcat.kill()
                logcat.wait(timeout=5)
        if logcat_stream is not None:
            logcat_stream.close()
            path = OUT / "device-logcat.txt"
            with path.open("rb") as stream:
                stream.seek(max(0, path.stat().st_size - 262144))
                tail = stream.read().decode("utf-8", errors="replace")
            result["device_crash_lines"] = [line for line in tail.splitlines()
                if re.search(r"Fatal signal|FATAL EXCEPTION|OutOfMemoryError|GPU.*(?:fault|hang)", line, re.I)][-40:]
            if result["device_crash_lines"]:
                print("Device crash diagnostics:\n" + "\n".join(result["device_crash_lines"]), flush=True)
        save(result)
    return 0 if result["capture_complete"] else 1


def settings_navigation():
    """Independent real-Activity regression; a broken reference cannot skip it."""
    from b05_case_results import verdict
    OUT.mkdir(exist_ok=True)
    result = {"source_sha": os.environ.get("GITHUB_SHA"), "cases": [], "passed": False}
    SYSTEM_DIALOG_EVENTS.clear()
    try:
        expected_api = os.environ.get("UI_SETTINGS_ANDROID_API", "35")
        if expected_api not in {"35", "36"}:
            raise ValueError("Unsupported Settings regression API: " + expected_api)
        result["device"] = {"expected_api": expected_api,
                            "api": shell("getprop", "ro.build.version.sdk")}
        if result["device"]["api"] != expected_api:
            raise ValueError("Settings regression requires Android API " + expected_api +
                             "; actual API: " + result["device"]["api"])
        result["device"].update({"model": shell("getprop", "ro.product.model"),
                                  "abis": shell("getprop", "ro.product.cpu.abilist"),
                                  "fingerprint": shell("getprop", "ro.build.fingerprint"),
                                  "physical_hyperos_verified": False})
        package = PACKAGES["aurelis"]
        verify_apk(Path("app/build/outputs/apk/debug/app-debug.apk"), package)
        adb("install", "-r", "app/build/outputs/apk/debug/app-debug.apk")
        adb("install", "-r", "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk")
        shell("pm", "grant", package, "android.permission.READ_MEDIA_AUDIO")
        for method in ("profileAndLibrarySettingsSurviveRecreation", "profileAndLibrarySettingsPreserveRealPlayback"):
            klass = "RealSettingsNavigationInstrumentedTest"
            command = ["adb", "shell", "am", "instrument", "-w", "-r", "-e", "class",
                       "com.music.aurelis.playback." + klass + "#" + method,
                       "com.music.aurelis.test/androidx.test.runner.AndroidJUnitRunner"]
            shell("am", "force-stop", package)
            prepare_device(method)
            # Never launch a second UiAutomation client during instrumentation.
            # Apply the existing strictly verified launcher-only ANR policy first.
            (OUT / (method + "-preflight.xml")).write_text(tree())
            shell("logcat", "-c")
            try:
                run = subprocess.run(command, capture_output=True, text=True, timeout=150)
                log, code = run.stdout + run.stderr, str(run.returncode)
            except subprocess.TimeoutExpired as error:
                def decode(value):
                    return value.decode(errors="replace") if isinstance(value, bytes) else value or ""
                log, code = decode(error.stdout) + decode(error.stderr), "TIMEOUT"
            (OUT / (method + ".log")).write_text(log)
            passed, problems = verdict(log, klass, method, code)
            result["cases"].append({"method": method, "passed": passed, "problems": problems, "exit_code": code})
            print(f"Settings navigation {method}: {'PASS' if passed else 'FAIL'}\n" + log[-16000:], flush=True)
            if not passed:
                first_error = (method + "\n" + ", ".join(problems) + "\n" + log[:3000])
                escaped = first_error.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
                print("::error title=Settings test failure::" + escaped, flush=True)
            # Keep each case's Android exception before the next process starts.
            try:
                runtime = shell("logcat", "-d", "-b", "all", "-v", "threadtime")
                (OUT / (method + "-android-runtime.txt")).write_text(runtime)
                # Instrumentation installs its own exception handler: Java stacks may
                # use MonitoringInstr/TestRunner rather than AndroidRuntime.
                relevant = "\n".join(line for line in runtime.splitlines() if re.search(
                    r"\b(?:AndroidRuntime|AndroidJUnitRunner|AndroidJUnit4|MonitoringInstr|InstrumentationResultPrinter|TestRunner|libc|DEBUG|System\.err)\s*:", line))
                print(relevant[-48000:], flush=True)
                exits = shell("dumpsys", "activity", "exit-info", package)
                (OUT / (method + "-exit-info.txt")).write_text(exits)
                print(exits[-16000:], flush=True)
                if not passed:
                    exit_lines = "\n".join(line for line in exits.splitlines() if re.search(r"process=|description=", line))
                    critical = "\n".join(line for line in relevant.splitlines() if re.search(r"Fatal signal|FATAL EXCEPTION|Abort message|Caused by:|RenderNode::prepareTreeImpl", line))
                    diagnostic = (method + "\n" + exit_lines[:1400] + "\n" + critical[:2000]).replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
                    print("::error title=Settings navigation failure::" + diagnostic, flush=True)
                adb("pull", "/sdcard/Android/data/com.music.aurelis/files/settings-navigation", OUT / "screens")
            except RuntimeError as error:
                result.setdefault("diagnostic_errors", []).append(str(error))
        result["passed"] = len(result["cases"]) == 2 and all(case["passed"] for case in result["cases"])
    except Exception as error:
        result["error"] = str(error)
    result["system_dialog_events"] = SYSTEM_DIALOG_EVENTS.copy()
    (OUT / "settings-navigation.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2), flush=True)
    return 0 if result["passed"] else 1


def report():
    OUT.mkdir(exist_ok=True)
    path = OUT / "results.json"
    result = json.loads(path.read_text()) if path.exists() else {"error": "Capture never produced results", "screens": []}
    result["visual_verdict"] = "NOT_REVIEWED"
    save(result)
    summary = {key: result.get(key) for key in ("stage", "error", "capture_complete", "visual_verdict", "system_dialog_events", "host_monitor")}
    summary["captured_states"] = len(result.get("screens", []))
    summary["unreached"] = [{key: row.get(key) for key in ("app", "theme", "state", "reason", "labels", "panel_opened", "transport")}
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
    parser.add_argument("command", choices=["select-reference", "capture", "report", "settings-navigation"])
    parser.add_argument("paths", nargs="*")
    args = parser.parse_args()
    if args.command == "select-reference":
        select_reference(*map(Path, args.paths))
    elif args.command == "capture":
        raise SystemExit(capture())
    elif args.command == "settings-navigation":
        raise SystemExit(settings_navigation())
    else:
        report()
