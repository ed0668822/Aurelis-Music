#!/usr/bin/env python3
"""Aurelis CI v2: source inventory is not proof of per-file recompilation."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path.cwd()
OUT = ROOT / "ci-v2"
OUT.mkdir(exist_ok=True)

def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()

def save(name, obj):
    (OUT / name).write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n")

def inventory():
    source = os.environ.get("SOURCE_SHA") or git("rev-parse", "HEAD")
    base = os.environ.get("BASE_SHA", "")
    tracked = git("ls-files").splitlines()
    files = [p for p in tracked if p.startswith("app/src/")]
    changes = []
    if re.fullmatch(r"[0-9a-f]{40}", base or "") and set(base) != {"0"}:
        comparison = git("merge-base", base, source) if os.environ.get("EVENT") == "pull_request" else base
        changes = git("diff", "--name-status", comparison, source).splitlines()
    save("provenance.json", {
        "branch": os.environ.get("BRANCH"), "event": os.environ.get("EVENT"),
        "source_sha": source, "checkout_sha": git("rev-parse", "HEAD"),
        "tree_sha": git("rev-parse", "HEAD^{tree}"), "base_sha": base
    })
    save("sources.json", [{"path": p, "sha256": hashlib.sha256(Path(p).read_bytes()).hexdigest(),
                           "individual_compilation": "NOT_ESTABLISHED"} for p in files if Path(p).is_file()])
    (OUT / "changed-files.txt").write_text("\n".join(changes) + "\n")

def verify():
    apks = []
    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT", "/usr/local/lib/android/sdk"))
    signer = sdk / "build-tools/35.0.0/apksigner"
    aapt = sdk / "build-tools/35.0.0/aapt"
    for variant in ("debug", "release"):
        candidates = sorted((ROOT / f"app/build/outputs/apk/{variant}").glob("*.apk"))
        if not candidates:
            raise RuntimeError(f"No {variant} APK found")
        for apk in candidates:
            subprocess.run([str(signer), "verify", "--verbose", str(apk)], check=True, capture_output=True)
            badging = subprocess.check_output([str(aapt), "dump", "badging", str(apk)], text=True)
            if "package: name='com.music.aurelis'" not in badging:
                raise RuntimeError(f"Wrong application ID: {apk.name}")
            with zipfile.ZipFile(apk) as z:
                if z.testzip() is not None:
                    raise RuntimeError(f"Invalid ZIP: {apk.name}")
                abis = {p.split("/")[1] for p in z.namelist() if p.startswith("lib/") and p.endswith(".so")}
                if abis != {"arm64-v8a"}:
                    raise RuntimeError(f"Unexpected ABI: {abis}")
            apks.append({"variant": variant, "file": apk.name, "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
                         "bytes": apk.stat().st_size, "signature_valid": True, "abi": "arm64-v8a",
                         "official_release": False})
    save("apk-verification.json", apks)
    (OUT / "SHA256SUMS.txt").write_text("".join(f"{a['sha256']}  {a['file']}\n" for a in apks))

def report():
    phases = {k: os.environ.get(k + "_OUTCOME", "not_executed") for k in ("QUALITY", "BUILD", "VERIFY")}
    tasks = []
    diagnostics = []
    for name in ("quality", "build"):
        p = OUT / (name + ".log")
        if not p.exists():
            continue
        for line in p.read_text(errors="replace").splitlines():
            m = re.match(r"^> Task (:\S+)(?: (UP-TO-DATE|FROM-CACHE|SKIPPED|NO-SOURCE|FAILED))?$", line)
            if m:
                tasks.append({"phase": name, "task": m[1], "state": m[2] or "EXECUTED"})
            if re.search(r"(^e: |^w: |error:|warning:|FAILURE: Build failed)", line):
                diagnostics.append({"phase": name, "message": line[:1200]})
    save("task-states.json", tasks)
    save("diagnostics.json", diagnostics)
    save("gates.json", phases)
    summary = ["## Aurelis Music CI Professional v2", "",
               "Source inventory does not establish individual file compilation.", "",
               "| Gate | Outcome |", "|---|---|"]
    summary += [f"| {k} | {v} |" for k, v in phases.items()]
    summary += ["", f"Gradle tasks observed: {len(tasks)}", f"Diagnostic lines: {len(diagnostics)}",
                "APK artifacts are for testing only, never an official signed release.",
                "Emulator status is reported by its separate job."]
    text = "\n".join(summary) + "\n"
    (OUT / "summary.md").write_text(text)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as f:
            f.write(text)

if __name__ == "__main__":
    import sys
    {"inventory": inventory, "verify": verify, "report": report}[sys.argv[1]]()
