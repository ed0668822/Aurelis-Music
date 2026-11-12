#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""F4: conservative per-file compile/DEX/R8 evidence classification."""
from __future__ import annotations
import collections
import csv
import json
from pathlib import Path

COLUMNS = ("path", "variant", "verdict", "role", "kind", "task_state",
           "task_input", "apk_state", "class_evidence", "r8_mapping",
           "attributed_errors", "individual_compilation_proven", "explanation")
GOOD = {"SUCCESS", "FROM-CACHE", "UP-TO-DATE"}
DEX = {"DEX_CLASSES_OBSERVED", "R8_OR_DEX_CLASSES_OBSERVED"}
VERDICTS = ("COMPILADO_EN_APK", "ERROR_ATRIBUIDO", "SIN_CLASES_ESPERADAS",
            "NO_EJECUTADO", "NO_EVIDENCIA_SUFICIENTE", "AMBIGUO", "FUERA_DE_PRODUCCION")


def classify(row, trace, r8_mapping=False):
    variant = row.get("variant")
    role = row.get("role")
    source_set = row.get("source_set")
    errors = int(row.get("compiler_file_errors") or 0) + int(row.get("gradle_diagnostic_errors") or 0)
    if row.get("kind") == "test" or role not in ("MANAGED_SOURCE", "NATIVE_SOURCE") or (
        source_set in ("debug", "release") and source_set != variant
    ):
        return "FUERA_DE_PRODUCCION", "Source excluded from production APK"
    if errors:
        return "ERROR_ATRIBUIDO", "Explicit Gradle compiler diagnostic for source file"
    if row.get("apk_state") == "AMBIGUOUS_SOURCE_CLASS_MAPPING" or trace.get("class_evidence") == "AMBIGUOUS_SOURCE_PROVENANCE":
        return "AMBIGUO", "Shared source-file/package identity requires manual review"
    if trace.get("declaration_hint") == "ONLY_PACKAGE_IMPORT_COMMENT_TEXT" and row.get("mapped_class_count") == 0:
        return "SIN_CLASES_ESPERADAS", "Only package/import declarations; no class expected"
    task = row.get("compiler_task_state")
    if task in ("FAILED", "SKIPPED", "NO-SOURCE"):
        return "NO_EJECUTADO", "No successful terminal compiler task"
    if role == "NATIVE_SOURCE":
        return "NO_EVIDENCIA_SUFICIENTE", "CMake declaration is not individual .o proof"
    if (task in GOOD and row.get("compiler_task_input") is True
        and row.get("apk_state") == "COMPILER_CLASSES_MAPPED_TO_DEX"
        and trace.get("class_evidence") in DEX
        and isinstance(row.get("mapped_class_count"), int) and row["mapped_class_count"] > 0
        and (variant != "release" or r8_mapping)):
        return "COMPILADO_EN_APK", "Compiler task/input, JVM SourceFile and DEX/R8 observed"
    return "NO_EVIDENCIA_SUFICIENTE", "Missing per-file task/input or DEX/R8 evidence"


def load(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("Missing or unsafe evidence: " + path.name)
    return json.loads(path.read_text(encoding="utf-8"))


def generate(package: Path, sha: str):
    summary_dir = package / "00-Resumen"
    audit_dir = package / "B04/Verificacion"
    source = load(summary_dir / "estado-todos-los-archivos.json")
    traced = load(audit_dir / "p2-apk-trace.json")
    release = load(audit_dir / "apk-content-release.json")
    if (source.get("summary", {}).get("status") != "COMPLETE_MATRIX"
        or traced.get("summary", {}).get("status") != "EVIDENCE_RECONCILED"
        or source.get("summary", {}).get("source_sha") != sha
        or traced.get("summary", {}).get("source_sha") != sha):
        raise ValueError("Incomplete or stale source evidence SHA")
    if not isinstance(source.get("files"), list) or not isinstance(traced.get("rows"), list):
        raise ValueError("Missing file evidence rows")
    trace_index = {}
    for item in traced["rows"]:
        key = (item.get("variant"), item.get("path"))
        if key in trace_index or not all(isinstance(v, str) and v for v in key):
            raise ValueError("Ambiguous/invalid APK evidence row")
        trace_index[key] = item
    r8 = release.get("r8")
    mapping = isinstance(r8, dict) and isinstance(r8.get("mapping.txt"), dict) and bool(r8["mapping.txt"].get("sha256"))
    output, counts, seen = [], collections.Counter(), set()
    for item in source["files"]:
        key = (item.get("variant"), item.get("path"))
        if key in seen or key not in trace_index or key[0] not in ("debug", "release"):
            raise ValueError("Duplicate or unpaired source row")
        seen.add(key)
        result, reason = classify(item, trace_index[key], mapping)
        out = {
            "path": key[1], "variant": key[0], "verdict": result,
            "role": item.get("role"), "kind": item.get("kind"),
            "task_state": item.get("compiler_task_state"),
            "task_input": item.get("compiler_task_input"),
            "apk_state": item.get("apk_state"),
            "class_evidence": trace_index[key].get("class_evidence"),
            "r8_mapping": mapping if key[0] == "release" else None,
            "attributed_errors": int(item.get("compiler_file_errors") or 0) + int(item.get("gradle_diagnostic_errors") or 0),
            "individual_compilation_proven": result == "COMPILADO_EN_APK",
            "explanation": reason,
        }
        output.append(out)
        counts[(key[0], result)] += 1
    if set(trace_index) != seen:
        raise ValueError("Unmatched source/APK evidence rows")
    report = {
        "schema_version": 1, "source_sha": sha, "status": "CLASSIFIED_WITH_LIMITATIONS",
        "total_rows": len(output), "r8_mapping_observed": mapping,
        "release_pending_evidence": sum(1 for x in output if x["variant"] == "release"
            and x["verdict"] in ("NO_EVIDENCIA_SUFICIENTE", "NO_EJECUTADO", "ERROR_ATRIBUIDO", "AMBIGUO")),
        "per_variant": {v: {label: counts[(v, label)] for label in VERDICTS} for v in ("debug", "release")},
        "migration_mode": "ADVISORY_UNTIL_TWO_VERIFIED_GREEN_RUNS",
        "limitations": [
            "Native individual object (.o) evidence not yet recorded",
            "Restored Debug compiler task input evidence may be missing",
            "DEX presence is not runtime correctness",
        ],
    }
    summary_dir.mkdir(parents=True, exist_ok=True)
    (summary_dir / "file-compile-matrix.json").write_text(
        json.dumps({"summary": report, "files": output}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    with (summary_dir / "file-compile-matrix.tsv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, delimiter="\t", fieldnames=COLUMNS)
        writer.writeheader()
        writer.writerows(output)
    (summary_dir / "CI14-F4.md").write_text(
        "# F4 — source compilation verdicts\n\n"
        + "Commit: " + sha + "\n"
        + "Release source rows still needing evidence: " + str(report["release_pending_evidence"]) + "\n"
        + "Old P2 gate is retained; no unverified file is called compiled.\n", encoding="utf-8")
    return report
