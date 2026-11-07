#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Bundle CI report artifacts as one B01-B06 directory tree, without APK binaries."""
from __future__ import annotations

import collections
import json
import os
from pathlib import Path
import shutil

INPUT = Path("b06-input")
OUTPUT = Path("b06-package")
SOURCES = {
    "B01": [("Compilacion", "b01-report-")],
    "B02": [("Lint", "b02-lint-report-")],
    "B03": [("Debug", "b03-debug-tests-"), ("Release", "b03-release-tests-"), ("Resumen", "b03-summary-")],
    "B04": [("Verificacion", "b04-verification-report-")],
    "B05": [("Emulador", "b05-android-emulator-")],
}
NO_BINARIES = {".apk", ".aab", ".jks", ".keystore", ".p12"}


def read_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8")) if path.is_file() else {}
    except (OSError, ValueError):
        return {}


def source_coverage() -> dict:
    path = OUTPUT / "B04" / "Verificacion"
    raw = read_json(path / "source-inventory.json")
    result = {"tracked_repository_files": len(raw) if isinstance(raw, list) else None,
              "individual_compilation_of_every_file_proven": False,
              "variants": {}, "limitations": [
                  "Not every repository file is a compilable source file.",
                  "A declaration-free Kotlin source may emit no separate class.",
                  "Presence of compiled classes in DEX is not a one-to-one compilation certificate.",
                  "An unused C/C++ file need not be selected by CMake.",
              ]}
    for variant in ("debug", "release"):
        evidence = read_json(path / f"apk-content-{variant}.json")
        rows = evidence.get("rows") or []
        counts = collections.Counter(row.get("state", "UNKNOWN") for row in rows)
        unresolved = [{"path": row.get("path"), "state": row.get("state")}
                      for row in rows if row.get("state") in
                      ("CLASS_PRESENCE_EVIDENCE_INCOMPLETE", "NO_NATIVE_UNIT_EVIDENCE",
                       "AMBIGUOUS_SOURCE_CLASS_MAPPING")]
        result["variants"][variant] = {
            "content_status": evidence.get("status", "NOT_RECORDED"),
            "observed_states": dict(counts),
            "unresolved_evidence": unresolved,
            "required_failures": evidence.get("required_failures", []),
        }
    (OUTPUT / "B06" / "source-coverage.json").write_text(
        json.dumps(result, indent=2) + "\n", encoding="utf-8")
    lines = ["# Evidencia de archivos compilados",
             f"- Archivos rastreados: {result['tracked_repository_files']}",
             "- El pipeline comprueba la compilación completa mediante Gradle, los tests y el APK.",
             "- NO demuestra una compilación individual de cada archivo del repositorio.",
             "- Las clases pueden estar optimizadas, fusionadas o ausentes si el archivo no declara ninguna.",
             "- Los archivos nativos solo se compilan si forman parte de las tareas CMake.",
             ""]
    for variant, value in result["variants"].items():
        lines.append(f"## {variant}: {value['content_status']}")
        for item in value["unresolved_evidence"]:
            lines.append(f"- `{item['path']}`: {item['state']}")
        if not value["unresolved_evidence"]:
            lines.append("- Sin observaciones pendientes en las categorías revisadas.")
    (OUTPUT / "B06" / "source-coverage.md").write_text(
        "\n".join(lines) + "\n", encoding="utf-8")
    return result


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / "B06").mkdir(parents=True, exist_ok=True)
    all_present = True
    index = []
    missing = []
    for phase, folders in SOURCES.items():
        for folder, prefix in folders:
            candidates = sorted(x for x in INPUT.glob(prefix + "*") if x.is_dir())
            if len(candidates) != 1:
                missing.append(f"{phase}/{folder}: expected 1 artifact, got {len(candidates)}")
                all_present = False
                continue
            source = candidates[0]
            dest = OUTPUT / phase / folder
            dest.mkdir(parents=True, exist_ok=True)
            copied = []
            for file in sorted(source.rglob("*")):
                if not file.is_file() or file.is_symlink() or file.suffix.lower() in NO_BINARIES:
                    continue
                relative = file.relative_to(source)
                target = dest / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(file, target)
                copied.append(str(target.relative_to(OUTPUT)))
            if not copied:
                missing.append(f"{phase}/{folder}: no reports")
                all_present = False
            index.append({"source_artifact": source.name, "phase": phase, "files": copied})

    stages_json = json.loads(os.environ.get("STAGES_JSON", "{}"))
    stages = {f"B0{i}": stages_json.get(f"b0{i}", {}).get("result", "not_executed")
              for i in range(1, 6)}
    b04 = read_json(OUTPUT / "B04" / "Verificacion" / "apk-verification.json")
    b03 = read_json(OUTPUT / "B03" / "Resumen" / "results.json")
    b05 = read_json(OUTPUT / "B05" / "Emulador" / "results.json")
    coverage = source_coverage()
    trusted_evidence = read_json(OUTPUT / "B04" / "Verificacion" / "source-evidence.json")
    evidence_file = OUTPUT / "B04" / "Verificacion" / "source-evidence.md"
    checks = {
        "source_evidence_reconciliation_passed": (
            trusted_evidence.get("status") == "PASS_VERIFIED_GATES" and
            trusted_evidence.get("source_sha") == os.environ.get("GITHUB_SHA") and
            evidence_file.is_file()),
        "stage_jobs_success": all(x == "success" for x in stages.values()),
        "report_artifacts_downloaded": os.environ.get("DOWNLOAD_OUTCOME") == "success",
        "required_report_folders_present": all_present,
        "apk_verification_success": b04.get("status") == "success",
        "debug_tests_success": b03.get("debug", {}).get("passed_gate") is True,
        "release_tests_success": b03.get("release", {}).get("passed_gate") is True,
        "emulator_smoke_success": b05.get("pass") is True,
    }
    passed = all(checks.values())
    report = {
        "schema_version": 2,
        "commit": os.environ.get("GITHUB_SHA"),
        "run_id": os.environ.get("GITHUB_RUN_ID"),
        "branch": os.environ.get("GITHUB_REF_NAME"),
        "status": "PASS" if passed else "FAIL",
        "phases": stages,
        "checks": checks,
        "missing_reports": missing,
        "report_index": index,
        "source_compilation_evidence": coverage,
        "source_evidence_reconciliation": {
            "status": trusted_evidence.get("status", "MISSING_EVIDENCE"),
            "source_coverage_status": trusted_evidence.get("source_coverage_status", "UNKNOWN"),
            "mandatory_problems": trusted_evidence.get("problems", []),
            "per_file_compilation_proven": False,
        },
        "apk_files_in_bundle": False,
        "note": "APK Debug and Release are separate GitHub Actions artifacts.",
    }
    (OUTPUT / "B06" / "pipeline-results.json").write_text(
        json.dumps(report, indent=2) + "\n", encoding="utf-8")
    (OUTPUT / "B06" / "artifact-index.json").write_text(
        json.dumps(index, indent=2) + "\n", encoding="utf-8")
    lines = ["# Aurelis Music — Informes B01-B06",
             f"- Commit: `{report['commit']}`",
             f"- Resultado: **{report['status']}**",
             "- Las APK se entregan aparte (Debug y Release).",
             "- La compilación individual de cada archivo no está totalmente demostrada.",
             "- Auditoría independiente B04: " + trusted_evidence.get("status", "MISSING_EVIDENCE"),
             "",
             "| Fase | Resultado |", "|---|---|"]
    lines.extend(f"| {name} | {state} |" for name, state in stages.items())
    lines += ["", "## Carpetas e informes"]
    for entry in index:
        lines.append(f"- {entry['phase']}: {entry['source_artifact']} ({len(entry['files'])} archivos)")
    for issue in missing:
        lines.append(f"- Informe faltante: {issue}")
    lines += ["", "Véase B04/Verificacion/source-evidence.md y B06/source-coverage.md para las limitaciones de la evidencia por archivo.",
              "El emulador solo valida instalación, apertura y proceso; no la reproducción musical completa."]
    summary = "\n".join(lines) + "\n"
    (OUTPUT / "B06" / "summary.md").write_text(summary, encoding="utf-8")
    with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as fp:
        fp.write(summary)
    print(f"B06 bundled {len(index)} report artifacts; status={report['status']}")


if __name__ == "__main__":
    main()
