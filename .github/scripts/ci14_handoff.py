#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""F3: immutable same-run Debug + instrumentation APK handoff, fail closed.

Only the unsigned-for-distribution (debug-signed) APKs are handed off.
The private release keystore and signed release APK never enter this artifact.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys
import zipfile

HANDOFF = Path("ci-f3-handoff")
FILES = {
    "debug": ("debug/app-debug.apk", "app/build/outputs/apk/debug/app-debug.apk"),
    "instrumentation": ("instrumentation/app-debug-androidTest.apk",
                        "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"),
}
SHA = re.compile(r"[0-9a-f]{40}\Z")
OTHER_ABIS = {"armeabi", "armeabi-v7a", "x86", "x86_64", "mips", "mips64", "riscv64"}


def provenance() -> dict:
    sha = os.environ.get("GITHUB_SHA", "").lower()
    run = os.environ.get("GITHUB_RUN_ID", "")
    attempt = os.environ.get("GITHUB_RUN_ATTEMPT", "")
    number = os.environ.get("GITHUB_RUN_NUMBER", "")
    if not SHA.fullmatch(sha) or not all(x.isdecimal() and int(x) > 0
                                           for x in (run, attempt, number)):
        raise ValueError("Missing or invalid immutable GitHub source/run provenance")
    return {"source_sha": sha, "run_id": run, "run_attempt": attempt, "run_number": number}


def digest(path: Path) -> str:
    sha = hashlib.sha256()
    with path.open("rb") as fp:
        for chunk in iter(lambda: fp.read(1024 * 1024), b""):
            sha.update(chunk)
    return sha.hexdigest()


def checked_apk(path: Path, require_arm64: bool) -> None:
    if path.is_symlink() or not path.is_file() or path.stat().st_size < 50:
        raise ValueError(f"Missing, symlinked or invalid APK: {path}")
    if not zipfile.is_zipfile(path):
        raise ValueError(f"Not an APK ZIP: {path}")
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise ValueError(f"Corrupt APK: {path}")
        entries = archive.namelist()
        libs = [name.split("/")[1] for name in entries
                if name.startswith("lib/") and name.endswith(".so") and len(name.split("/")) == 3]
        if require_arm64 and "arm64-v8a" not in libs:
            raise ValueError("Debug app APK has no ARM64 native libraries")
        if OTHER_ABIS.intersection(libs):
            raise ValueError("Unexpected non-ARM64 native ABI in APK")


def pack(root: Path) -> dict:
    metadata = provenance()
    handoff = root / HANDOFF
    if handoff.is_symlink():
        raise ValueError("Handoff destination must not be a symlink")
    if handoff.exists():
        shutil.rmtree(handoff)
    handoff.mkdir(parents=True)
    items = {}
    for variant, (relative, source) in FILES.items():
        original = root / source
        checked_apk(original, variant == "debug")
        target = handoff / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(original, target)
        items[variant] = {"path": relative, "sha256": digest(target),
                          "bytes": target.stat().st_size}
    report = {"schema": 1, "abi": "arm64-v8a", "distribution": "debug_only",
              **metadata, "files": items}
    (handoff / "manifest.json").write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return report


def verify(root: Path) -> dict:
    handoff = root / HANDOFF
    if handoff.is_symlink() or not handoff.is_dir():
        raise ValueError("Missing or symlinked handoff directory")
    manifest_file = handoff / "manifest.json"
    if not manifest_file.is_file() or manifest_file.is_symlink():
        raise ValueError("Missing or symlinked handoff manifest")
    data = json.loads(manifest_file.read_text(encoding="utf-8"))
    expected = provenance()
    if any(data.get(key) != value for key, value in expected.items()):
        raise ValueError("Wrong source SHA, GitHub run, attempt, or run number")
    if data.get("schema") != 1 or data.get("abi") != "arm64-v8a" or data.get("distribution") != "debug_only":
        raise ValueError("Wrong artifact format or ABI")
    if not isinstance(data.get("files"), dict) or set(data["files"]) != set(FILES):
        raise ValueError("Incomplete or unexpected artifact manifest")
    allowed = {"manifest.json"}
    for variant, (relative, _) in FILES.items():
        record = data["files"][variant]
        if not isinstance(record, dict) or record.get("path") != relative:
            raise ValueError(f"Unexpected APK path for {variant}")
        file = handoff / relative
        checked_apk(file, variant == "debug")
        if file.stat().st_size != record.get("bytes") or digest(file) != record.get("sha256"):
            raise ValueError(f"{variant} APK size or SHA-256 mismatch")
        allowed.add(relative)
    found = {p.relative_to(handoff).as_posix() for p in handoff.rglob("*") if p.is_file() or p.is_symlink()}
    if found != allowed:
        raise ValueError("Unknown, extra or symlinked handoff files")
    return data


def restore(root: Path, variant: str) -> dict:
    data = verify(root)
    chosen = FILES if variant == "all" else {variant: FILES[variant]}
    for name, (relative, dest) in chosen.items():
        target = root / dest
        if target.is_symlink():
            raise ValueError("Destination APK must not be a symlink")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(root / HANDOFF / relative, target)
        if digest(target) != data["files"][name]["sha256"]:
            raise ValueError("Restored APK checksum mismatch")
        print("F3 verified source and restored", name, "APK")
    return data


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("pack", "verify", "restore"))
    parser.add_argument("--variant", choices=("debug", "instrumentation", "all"), default="all")
    args = parser.parse_args()
    root = Path.cwd()
    if args.mode == "pack":
        pack(root)
    elif args.mode == "verify":
        verify(root)
    else:
        restore(root, args.variant)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, TypeError, json.JSONDecodeError, zipfile.BadZipFile) as error:
        print("F3 FAIL:", error, file=sys.stderr)
        sys.exit(1)
