#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.4: consolidate all tracked files across B02/B04 evidence, without invented builds.

A complete MATRIX means consistent report coverage, NOT that each file compiled,
was included in the APK, or passed runtime checks.
"""
from __future__ import annotations

import collections
import csv
import hashlib
import json
from pathlib import Path

VARIANTS = ("debug", "release")
FIELDS = (
    "path", "variant", "module", "source_set", "kind", "language", "role",
    "file_state", "source_sha256", "root_evidence", "task_input_evidence",
    "observed_tasks", "compiler_evidence", "compiler_task_state",
    "compiler_task_input", "compiler_file_errors", "apk_state",
    "apk_class_evidence", "apk_packaging_evidence", "mapped_class_count",
    "source_evidence_classification", "lint_errors", "lint_warnings",
    "lint_information", "gradle_diagnostic_errors", "gradle_diagnostic_warnings",
    "unit_test_scope", "runtime_scope", "individual_compilation",
)


def read_json(path: Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def read_tsv(path: Path):
    if not path.is_file():
        return None
    try:
        with path.open(encoding="utf-8", newline="") as stream:
            reader = csv.DictReader(stream, delimiter="\t")
            if not reader.fieldnames or "path" not in reader.fieldnames and "file" not in reader.fieldnames:
                return None
            return list(reader)
    except (OSError, UnicodeError, csv.Error):
        return None


def keyed(data, keys, name, issues):
    if not isinstance(data, list):
        issues.append("MISSING_ROWS:" + name)
        return {}
    rows = {}
    for row in data:
        if not isinstance(row, dict) or any(not isinstance(row.get(k), str) or not row[k] for k in keys):
            issues.append("INVALID_ROW:" + name)
            continue
        key = tuple(row[k] for k in keys)
        if key in rows:
            issues.append("DUPLICATE_ROW:" + name + ":" + "/".join(key))
        rows[key] = row
    return rows


def normalize_issue_path(value, tracked):
    if not isinstance(value, str):
        return None
    raw = value.replace("\\", "/")
    candidates = [path for path in tracked if raw == path or raw.endswith("/" + path)]
    return max(candidates, key=len) if len(candidates) == 1 else None


def write_csv(path, fields, rows):
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def build_matrix(package: Path, sha: str):
    output = package / "00-Resumen"
    output.mkdir(parents=True, exist_ok=True)
    b04 = package / "B04" / "Verificacion"
    issues = []
    inventory = read_json(b04 / "source-inventory.json")
    inventory_rows = keyed(inventory, ("path",), "source-inventory", issues)
    indexed = read_tsv(b04 / "p2-inventory-files.tsv")
    inventory_details = keyed(indexed, ("path",), "p2-inventory-files", issues)
    inventory_gate = read_json(b04 / "p2-inventory-coverage.json")
    compiler = read_json(b04 / "p2-compiler-evidence.json")
    apk = read_json(b04 / "p2-apk-trace.json")
    source_gate = read_json(b04 / "source-evidence.json")
    inventory_gate = inventory_gate if isinstance(inventory_gate, dict) else {}
    compiler = compiler if isinstance(compiler, dict) else {}
    apk = apk if isinstance(apk, dict) else {}
    source_gate = source_gate if isinstance(source_gate, dict) else {}
    compiler_gate = compiler.get("summary", {})
    apk_gate = apk.get("summary", {})
    compiler_gate = compiler_gate if isinstance(compiler_gate, dict) else {}
    apk_gate = apk_gate if isinstance(apk_gate, dict) else {}

    gates = (
        ("P2_INVENTORY", inventory_gate, "PASS_INVENTORY"),
        ("P2_COMPILER", compiler_gate, "EVIDENCE_RECORDED"),
        ("P2_APK", apk_gate, "EVIDENCE_RECONCILED"),
        ("B04_SOURCE", source_gate, "PASS_VERIFIED_GATES"),
    )
    for name, report, desired in gates:
        if report.get("source_sha") != sha or report.get("status") != desired:
            issues.append("MISSING_STALE_OR_FAILED_REPORT:" + name)
    total = len(inventory_rows)
    if not total or not isinstance(inventory, list):
        issues.append("EMPTY_TRACKED_INVENTORY")
    if set(inventory_rows) != set(inventory_details):
        issues.append("P2_INVENTORY_FILE_SET_MISMATCH")
    if any(inventory_gate.get(k) != total for k in ("tracked_files", "inventory_files", "indexed_files")):
        issues.append("P2_INVENTORY_CARDINALITY_MISMATCH")

    compiler_rows = keyed(compiler.get("rows"), ("variant", "path"), "p2-compiler", issues)
    apk_rows = keyed(apk.get("rows"), ("variant", "path"), "p2-apk", issues)
    source_rows = keyed(source_gate.get("source_rows"), ("variant", "path"), "b04-source", issues)
    expected_all = {(variant, p[0]) for variant in VARIANTS for p in inventory_rows}
    managed_paths = {p[0] for p, row in inventory_rows.items()
                     if row.get("role") in ("MANAGED_SOURCE", "NATIVE_SOURCE")}
    expected_compiler = {(variant, p) for variant in VARIANTS for p in managed_paths}
    if set(compiler_rows) != expected_compiler:
        issues.append("P2_COMPILER_ROW_SET_MISMATCH")
    if set(apk_rows) != expected_all:
        issues.append("P2_APK_ROW_SET_MISMATCH")
    if set(source_rows) != expected_all:
        issues.append("B04_SOURCE_ROW_SET_MISMATCH")
    if compiler_gate.get("source_files") != len(managed_paths) or compiler_gate.get("variant_rows") != len(expected_compiler):
        issues.append("P2_COMPILER_COUNTS_MISMATCH")
    if apk_gate.get("tracked_files") != total or apk_gate.get("variant_rows") != len(expected_all):
        issues.append("P2_APK_COUNTS_MISMATCH")
    if inventory_gate.get("source_sha") != sha:
        issues.append("P2_INVENTORY_COMMIT_MISMATCH")
    for (path,), row in inventory_rows.items():
        details = inventory_details.get((path,), {})
        for field in ("file_state", "sha256", "role"):
            if field in details and str(details[field]) not in ("", str(row.get(field))):
                issues.append("INVENTORY_FIELD_CONFLICT:" + path + ":" + field)

    lint_issues = read_tsv(package / "B02" / "Lint" / "issues.tsv")
    if lint_issues is None:
        issues.append("MISSING_B02_LINT_ISSUES")
        lint_issues = []
    raw_gradle = read_json(b04 / "diagnostics.json")
    provenance = read_json(b04 / "diagnostics-provenance.json")
    provenance = provenance if isinstance(provenance, dict) else {}
    if not isinstance(raw_gradle, list):
        issues.append("MISSING_B04_GRADLE_DIAGNOSTICS")
        raw_gradle = []
    if (provenance.get("status") != "PASS_LOG_PARSED"
            or provenance.get("source_sha") != sha
            or provenance.get("schema_version") != 1
            or not isinstance(provenance.get("terminal_gradle_tasks"), int)
            or provenance.get("terminal_gradle_tasks", 0) < 1
            or provenance.get("diagnostic_count") != len(raw_gradle)):
        issues.append("UNVERIFIED_B04_GRADLE_DIAGNOSTIC_PROVENANCE")
    for file_name, expected_hash in (
        ("build.log", provenance.get("build_log_sha256")),
        ("diagnostics.json", provenance.get("diagnostics_sha256")),
    ):
        file = b04 / file_name
        if (not file.is_file() or file.is_symlink() or not isinstance(expected_hash, str)
                or hashlib.sha256(file.read_bytes()).hexdigest() != expected_hash):
            issues.append("B04_GRADLE_DIAGNOSTIC_HASH_MISMATCH:" + file_name)
    lint_by_path = collections.defaultdict(collections.Counter)
    gradle_by_path = collections.defaultdict(collections.Counter)
    diag_output = []
    for entry in lint_issues:
        mapped = normalize_issue_path(entry.get("file"), {key[0] for key in inventory_rows})
        severity = str(entry.get("severity", "")).lower()
        if severity in ("error", "fatal"):
            name = "error"
        elif severity == "warning":
            name = "warning"
        else:
            name = "information"
        if mapped:
            lint_by_path[mapped][name] += 1
        diag_output.append({
            "origin": "B02_LINT", "severity": severity, "path": mapped or (entry.get("file") or ""),
            "line": entry.get("line") or "", "code": entry.get("id") or "",
            "message": entry.get("message") or "", "mapped_to_source": mapped is not None,
        })
    for entry in raw_gradle:
        if not isinstance(entry, dict):
            issues.append("INVALID_GRADLE_DIAGNOSTIC")
            continue
        mapped = normalize_issue_path(entry.get("file"), {key[0] for key in inventory_rows})
        severity = str(entry.get("severity") or "").lower()
        if mapped and severity in ("error", "warning"):
            gradle_by_path[mapped][severity] += 1
        diag_output.append({
            "origin": "B04_GRADLE_" + str(entry.get("phase", "UNKNOWN")),
            "severity": severity, "path": mapped or (entry.get("file") or ""),
            "line": entry.get("line") or "", "code": "",
            "message": entry.get("message") or "", "mapped_to_source": mapped is not None,
        })

    rows = []
    counts = collections.Counter()
    for (path,), item in sorted(inventory_rows.items()):
        details = inventory_details.get((path,), {})
        for variant in VARIANTS:
            key = (variant, path)
            c = compiler_rows.get(key, {})
            a = apk_rows.get(key, {})
            s = source_rows.get(key, {})
            if c and (c.get("role") != item.get("role") or c.get("variant") != variant):
                issues.append("COMPILER_SOURCE_IDENTITY_CONFLICT:" + variant + ":" + path)
            if a and (a.get("role") != item.get("role") or a.get("variant") != variant):
                issues.append("APK_SOURCE_IDENTITY_CONFLICT:" + variant + ":" + path)
            if s and (s.get("role") != item.get("role") or s.get("variant") != variant):
                issues.append("B04_SOURCE_IDENTITY_CONFLICT:" + variant + ":" + path)
            row = {
                "path": path, "variant": variant, "module": item.get("module", ""),
                "source_set": item.get("source_set", ""), "kind": item.get("kind", ""),
                "language": item.get("language", ""), "role": item.get("role", ""),
                "file_state": item.get("file_state", ""), "source_sha256": item.get("sha256"),
                "root_evidence": details.get("root_evidence", "NOT_OBSERVED"),
                "task_input_evidence": details.get("task_input_evidence", "NOT_OBSERVED"),
                "observed_tasks": details.get("observed_tasks", ""),
                "compiler_evidence": c.get("compiler_evidence", "NOT_APPLICABLE_BY_ROLE" if path not in managed_paths else "MISSING_EVIDENCE"),
                "compiler_task_state": c.get("task_state", "NOT_APPLICABLE"),
                "compiler_task_input": c.get("task_input_observed"),
                "compiler_file_errors": c.get("attributed_errors", 0),
                "apk_state": a.get("apk_state", "MISSING_EVIDENCE"),
                "apk_class_evidence": a.get("class_evidence", "MISSING_EVIDENCE"),
                "apk_packaging_evidence": a.get("packaging_evidence", "MISSING_EVIDENCE"),
                "mapped_class_count": a.get("class_count"),
                "source_evidence_classification": s.get("classification", "MISSING_EVIDENCE"),
                "lint_errors": lint_by_path[path]["error"],
                "lint_warnings": lint_by_path[path]["warning"],
                "lint_information": lint_by_path[path]["information"],
                "gradle_diagnostic_errors": gradle_by_path[path]["error"],
                "gradle_diagnostic_warnings": gradle_by_path[path]["warning"],
                "unit_test_scope": "B03_AGGREGATE_NOT_PER_FILE" if item.get("kind") == "test" else "NOT_A_PER_FILE_TEST",
                "runtime_scope": "B05_SMOKE_ONLY_NOT_PER_FILE",
                "individual_compilation": "NOT_ESTABLISHED",
            }
            rows.append(row)
            counts[(variant, row["source_evidence_classification"])] += 1
    if len(rows) != total * len(VARIANTS):
        issues.append("FINAL_MATRIX_CARDINALITY_MISMATCH")
    report = {
        "schema_version": 1, "source_sha": sha,
        "status": "COMPLETE_MATRIX" if not issues else "INCOMPLETE_MATRIX",
        "tracked_files": total, "rows": len(rows), "variants": list(VARIANTS),
        "consistency_issues": sorted(set(issues)),
        "unmatched_diagnostics": sum(not x["mapped_to_source"] for x in diag_output),
        "source_state_counts": {v: {name: number for (vv, name), number in sorted(counts.items()) if vv == v}
                                for v in VARIANTS},
        "individual_compilation_proven": False,
        "runtime_functionality_proven": False,
        "coverage_note": (
            "COMPLETE_MATRIX means every tracked file has a row in both variants and the reports agree; "
            "it does not imply standalone compilation, APK inclusion or runtime correctness."
        ),
    }
    (output / "resultado-matriz.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (output / "estado-todos-los-archivos.json").write_text(
        json.dumps({"summary": report, "files": rows}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_csv(output / "estado-todos-los-archivos.csv", FIELDS, rows)
    issue_columns = ("origin", "severity", "path", "line", "code", "message", "mapped_to_source")
    write_csv(output / "errores.csv", issue_columns,
              [x for x in diag_output if x["severity"] in ("error", "fatal")])
    write_csv(output / "advertencias.csv", issue_columns,
              [x for x in diag_output if x["severity"] == "warning"])
    write_csv(output / "diagnosticos-no-asignados.csv", issue_columns,
              [x for x in diag_output if not x["mapped_to_source"]])
    lines = [
        "# Aurelis Music — P2.4 Matriz de todos los archivos", "",
        "- Commit: " + sha,
        "- Integridad de informes: **" + report["status"] + "**",
        "- Archivos versionados: " + str(total),
        "- Filas Debug/Release: " + str(len(rows)),
        "- Diagnósticos sin archivo atribuible: " + str(report["unmatched_diagnostics"]),
        "- Un reporte completo no certifica la compilación individual ni la ejecución funcional.",
        "", "## Discrepancias",
    ]
    lines.extend("- " + x for x in report["consistency_issues"])
    if not report["consistency_issues"]:
        lines.append("- Ninguna discrepancia de estructura detectada.")
    lines.extend(["", "Consulta estado-todos-los-archivos.csv/.json para cada ruta.",
                  "errores.csv y advertencias.csv diferencian los hallazgos de Lint de los diagnósticos de Gradle."])
    (output / "Resumen.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    return report


if __name__ == "__main__":
    import os
    result = build_matrix(Path(os.environ.get("B06_PACKAGE", "b06-package")),
                          os.environ.get("GITHUB_SHA", ""))
    print(result["status"], result["tracked_files"], result["rows"])
    raise SystemExit(0 if result["status"] == "COMPLETE_MATRIX" else 1)
