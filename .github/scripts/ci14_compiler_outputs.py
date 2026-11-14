#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""F3: signed-by-provenance compiler output handoff, no source-file success claims."""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import sys
import tarfile

from ci14_handoff import provenance

OUT = Path("ci-f3-classes")
ARCHIVE = "debug-compiler-outputs.tar.gz"
MANIFEST = "manifest.json"
ALLOWED = (
    "app/build/tmp/kotlin-classes/debug",
    "app/build/kotlin/compileDebugKotlin/classpath-snapshot",
    "app/build/kotlin/compileDebugKotlin/cacheable",
    "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes",
    "app/build/generated/ap_generated_sources/debug/out",
)
MAX_MEMBERS = 100000
MAX_UNCOMPRESSED = 1_500_000_000


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as fp:
        for chunk in iter(lambda: fp.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def roots_from_log(root: Path) -> list[str]:
    log = root / "ci-b01/gradle-build.log"
    if not log.is_file():
        raise ValueError("Missing B01 compiler log")
    roots = set()
    for line in log.read_text(encoding="utf-8", errors="replace").splitlines():
        if not line.startswith("AURELIS_CLASS_OUTPUT "):
            continue
        value = json.loads(line.split(" ", 1)[1])
        if value.get("task") not in (
            ":app:compileDebugKotlin", ":app:compileDebugJavaWithJavac",
        ):
            continue
        name = value.get("path")
        if not isinstance(name, str) or name not in ALLOWED:
            raise ValueError(f"Unapproved compiler root: {name}")
        roots.add(name)
    if not roots or "app/build/tmp/kotlin-classes/debug" not in roots:
        raise ValueError("B01 did not declare Kotlin compiler class output")
    return sorted(roots)


def safe(root: Path, name: str) -> Path:
    pure = PurePosixPath(name)
    if pure.is_absolute() or ".." in pure.parts or not name.startswith("app/build/"):
        raise ValueError("Unsafe compiled-output path")
    result = root / name
    if result.is_symlink() or not result.resolve().is_relative_to(root.resolve()):
        raise ValueError("Compiler path escapes checkout")
    return result


def pack(root: Path) -> dict:
    roots = roots_from_log(root)
    destination = root / OUT
    if destination.exists():
        shutil.rmtree(destination)
    destination.mkdir(parents=True)
    count, length, class_count = 0, 0, 0
    tarpath = destination / ARCHIVE
    with tarfile.open(tarpath, "w:gz") as tar:
        for base in roots:
            directory = safe(root, base)
            if not directory.is_dir():
                raise ValueError(f"Missing declared compiler output: {base}")
            for member in [directory, *sorted(directory.rglob("*"))]:
                if member.is_symlink():
                    raise ValueError("Compiler output symlink refused")
                if not (member.is_file() or member.is_dir()):
                    raise ValueError("Nonregular compiler output refused")
                relative = member.relative_to(root).as_posix()
                safe(root, relative)
                count += 1
                if count > MAX_MEMBERS:
                    raise ValueError("Compiler output file count exceeds limit")
                if member.is_file():
                    length += member.stat().st_size
                    class_count += int(member.suffix == ".class")
                    if length > MAX_UNCOMPRESSED:
                        raise ValueError("Compiler output archive exceeds limit")
                tar.add(member, arcname=relative, recursive=False)
    if class_count == 0:
        raise ValueError("Compiler output has zero class files")
    result = {"schema": 1, **provenance(), "type": "compiler_debug_only",
              "roots": roots, "archive": ARCHIVE,
              "archive_sha256": sha256(tarpath), "archive_bytes": tarpath.stat().st_size,
              "entries": count, "uncompressed_bytes": length, "class_files": class_count}
    (destination / MANIFEST).write_text(
        json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return result


def verify(root: Path) -> dict:
    directory = root / OUT
    manifest = directory / MANIFEST
    archive = directory / ARCHIVE
    if any(p.is_symlink() for p in (directory, manifest, archive)):
        raise ValueError("Symlink in compiler handoff")
    if not manifest.is_file() or not archive.is_file():
        raise ValueError("Missing compiler handoff files")
    data = json.loads(manifest.read_text(encoding="utf-8"))
    if data.get("schema") != 1 or data.get("type") != "compiler_debug_only":
        raise ValueError("Unknown compiler handoff schema")
    if any(data.get(k) != v for k, v in provenance().items()):
        raise ValueError("Wrong source or GitHub run provenance")
    roots = data.get("roots")
    if not isinstance(roots, list) or not roots or len(roots) != len(set(roots)) or any(x not in ALLOWED for x in roots):
        raise ValueError("Wrong declared compiler roots")
    if data.get("archive") != ARCHIVE or data.get("archive_bytes") != archive.stat().st_size or data.get("archive_sha256") != sha256(archive):
        raise ValueError("Compiler archive size or SHA-256 mismatch")
    if set(p.name for p in directory.iterdir()) != {ARCHIVE, MANIFEST}:
        raise ValueError("Unexpected compiler handoff files")
    entries, size, classes = 0, 0, 0
    with tarfile.open(archive, "r:gz") as tar:
        for member in tar:
            entries += 1
            name = member.name.rstrip("/")
            if entries > MAX_MEMBERS or not (member.isfile() or member.isdir()):
                raise ValueError("Unsafe archive member")
            if not any(name == prefix or name.startswith(prefix + "/") for prefix in roots):
                raise ValueError("Compiler archive contains unapproved path")
            safe(root, name)
            if member.isfile():
                size += member.size
                classes += int(name.endswith(".class"))
                if size > MAX_UNCOMPRESSED:
                    raise ValueError("Compiler archive exceeds size limit")
    if entries != data.get("entries") or size != data.get("uncompressed_bytes") or classes != data.get("class_files") or classes == 0:
        raise ValueError("Compiler archive manifest mismatch")
    return data


def restore(root: Path) -> dict:
    data = verify(root)
    with tarfile.open(root / OUT / ARCHIVE, "r:gz") as tar:
        for member in tar:
            name = member.name.rstrip("/")
            path = safe(root, name)
            if member.isdir():
                path.mkdir(parents=True, exist_ok=True)
            else:
                path.parent.mkdir(parents=True, exist_ok=True)
                if path.is_symlink():
                    raise ValueError("Refusing to overwrite symlink")
                content = tar.extractfile(member)
                if content is None:
                    raise ValueError("Cannot extract compiler class file")
                with content, path.open("wb") as dest:
                    shutil.copyfileobj(content, dest)
    return data


def main() -> int:
    if len(sys.argv) != 2 or sys.argv[1] not in ("pack", "verify", "restore"):
        raise ValueError("Expected pack, verify, or restore")
    data = {"pack": pack, "verify": verify, "restore": restore}[sys.argv[1]](Path.cwd())
    print("F3 compiler output", sys.argv[1], "PASS:", data["class_files"], "class files")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, TypeError, tarfile.TarError) as error:
        print("F3 compiler handoff FAIL:", error, file=sys.stderr)
        sys.exit(1)
