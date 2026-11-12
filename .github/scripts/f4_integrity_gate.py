#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Fail closed on untrustworthy F4 compiler reports; never invent native APK linkage."""
from __future__ import annotations

import collections
import json
from pathlib import Path
import re

HEX40 = re.compile(r"[a-f0-9]{40}\Z")
HEX64 = re.compile(r"[a-f0-9]{64}\Z")
GOOD_MANAGED = {"COMPILADO_EN_APK", "SIN_CLASES_ESPERADAS"}
BAD = {"ERROR_ATRIBUIDO", "NO_EJECUTADO", "AMBIGUO"}


def read_json(path: Path) -> dict:
    if path.is_symlink() or not path.is_file():
        raise ValueError("Missing or unsafe F4 evidence: " + path.name)
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError("Invalid F4 JSON document: " + path.name)
    return data


def inspect(package: Path, sha: str) -> dict:
    if not HEX40.fullmatch(sha):
        raise ValueError("Invalid source commit SHA")
    f4 = read_json(package / "00-Resumen/file-compile-matrix.json")
    debug = read_json(package / "B01/Compilacion/f4-debug-inputs.json")
    native = read_json(package / "B04/Verificacion/native-object-evidence.json")
    summary = f4.get("summary")
    if not isinstance(summary, dict) or not isinstance(f4.get("files"), list):
        raise ValueError("F4 matrix missing summary or rows")
    for key, metadata in (("file matrix", summary), ("B01 inputs", debug), ("native objects", native)):
        if metadata.get("source_sha") != sha:
            raise ValueError("Stale or conflicting F4 source_sha in " + key)
    if summary.get("status") != "CLASSIFIED_WITH_LIMITATIONS" or debug.get("status") != "PASS" or native.get("status") != "PASS":
        raise ValueError("F4 input report not certified as complete")
    if not isinstance(debug.get("expected_source_count"), int) or debug["expected_source_count"] < 1 or (
            debug.get("matched_source_count") != debug["expected_source_count"] or debug.get("missing") != [] or debug.get("problems") != []):
        raise ValueError("B01 has incomplete Debug source-to-task input evidence")
    if not isinstance(summary.get("total_rows"), int) or summary["total_rows"] != len(f4["files"]):
        raise ValueError("F4 total row count mismatch")
    seen = set()
    roles = collections.Counter()
    managed_main = collections.Counter()
    for row in f4["files"]:
        if not isinstance(row, dict):
            raise ValueError("Invalid per-file source row")
        variant, path, verdict = row.get("variant"), row.get("path"), row.get("verdict")
        if variant not in ("debug", "release") or not isinstance(path, str) or not path:
            raise ValueError("Invalid file identity")
        key = (variant, path)
        if key in seen:
            raise ValueError("Duplicate F4 source row")
        seen.add(key)
        roles[(variant, row.get("role"), verdict)] += 1
        if verdict in BAD:
            raise ValueError("Attributed error, unexecuted task or ambiguous source: " + path)
        if row.get("role") == "MANAGED_SOURCE" and verdict != "FUERA_DE_PRODUCCION":
            if verdict not in GOOD_MANAGED or row.get("individual_compilation_proven") != (verdict == "COMPILADO_EN_APK"):
                raise ValueError("Unproven managed source marked ready: " + path)
            if path.startswith("app/src/main/"):
                managed_main[variant] += 1
        if row.get("role") == "NATIVE_SOURCE" and verdict == "COMPILADO_EN_APK":
            raise ValueError("Native source claimed packaged without linker proof: " + path)
    if not managed_main["debug"] or managed_main["debug"] != debug["expected_source_count"]:
        raise ValueError("B01 Debug task input count differs from main managed source rows")
    if managed_main["debug"] != managed_main["release"]:
        raise ValueError("Main source classification differs between variants")
    rows = native.get("rows")
    if not isinstance(rows, list) or not rows or native.get("declarations") != len(rows):
        raise ValueError("Native compiler object record set missing or inconsistent")
    if native.get("object_files_observed") != len(rows) or native.get("errors") != []:
        raise ValueError("At least one declared native source lacks a verified object")
    objects = set()
    for item in rows:
        if not isinstance(item, dict):
            raise ValueError("Invalid native row")
        variant, path = item.get("variant"), item.get("path")
        if (variant, path) in objects or (variant, path) not in seen:
            raise ValueError("Native output not tied to a tracked source row")
        objects.add((variant, path))
        if (item.get("status") != "OBJECT_SHA256_OBSERVED" or not isinstance(item.get("object_bytes"), int)
                or item["object_bytes"] <= 0 or not isinstance(item.get("object_sha256"), str)
                or not HEX64.fullmatch(item["object_sha256"])):
            raise ValueError("Native object hash/size evidence missing")
        if item.get("apk_link_proven") is not False:
            raise ValueError("Native APK linkage must not be inferred from object existence")
    result = {
        "schema_version": 1, "source_sha": sha,
        "status": "PASS_EVIDENCE_INTEGRITY_WITH_LINKAGE_LIMITATIONS",
        "main_managed_sources": managed_main["debug"],
        "compiled_with_class_evidence_debug": roles[("debug", "MANAGED_SOURCE", "COMPILADO_EN_APK")],
        "compiled_with_class_evidence_release": roles[("release", "MANAGED_SOURCE", "COMPILADO_EN_APK")],
        "native_objects_with_sha256": len(objects), "native_apk_linkage_proven": False,
        "release_sources_without_individual_APK_linkage": summary.get("release_pending_evidence"),
        "note": "Certifies evidence integrity, not audio behavior or native object-to-APK linkage.",
    }
    (package / "00-Resumen/f4-integrity-gate.json").write_text(
        json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return result
