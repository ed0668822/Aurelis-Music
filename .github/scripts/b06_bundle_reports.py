#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Bundle CI report artifacts as one B01-B06 directory tree, without APK binaries."""
from __future__ import annotations

import collections
import json
import os
from pathlib import Path
import shutil

from b06_cleanup_artifacts import release_expected

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



def create_virustotal_delivery(ci_passed: bool, b04: dict, commit: str, destination: Path | None = None) -> dict:
    """Produce an independent advisory status report tied to the signed Release SHA-256."""
    import datetime as dt
    import re

    source_path = OUTPUT / "B04" / "Verificacion" / "virustotal.json"
    source = read_json(source_path)
    candidates = [r for r in b04.get("apks", [])
                  if isinstance(r, dict) and r.get("variant") == "release"]
    expected = candidates[0].get("sha256") if len(candidates) == 1 else None
    sha_valid = isinstance(expected, str) and re.fullmatch(r"[0-9a-f]{64}", expected) is not None
    matches = bool(sha_valid and source.get("sha256") == expected and source.get("source_sha") == commit)
    allowed = {"NO_DETECTIONS_REPORTED", "REVIEW_REQUIRED", "PENDING", "ERROR", "NOT_CONFIGURED", "HASH_LOOKUP_ONLY"}
    status = source.get("status") if matches else "REPORT_UNVERIFIED"
    if status not in allowed and status != "REPORT_UNVERIFIED":
        status = "REPORT_UNVERIFIED"
    stats = source.get("last_analysis_stats") if matches else None
    stats = stats if isinstance(stats, dict) else {}
    updated = source.get("analysis_updated_at")
    recent = False
    age_days = None
    if isinstance(updated, str):
        try:
            when = dt.datetime.fromisoformat(updated.replace("Z", "+00:00"))
            if when.tzinfo is not None:
                age_days = round((dt.datetime.now(dt.timezone.utc) - when).total_seconds() / 86400, 2)
                recent = -1 <= age_days <= 30
        except (ValueError, OverflowError):
            pass
    clear = status == "NO_DETECTIONS_REPORTED" and recent and ci_passed
    report = {
        "schema_version": 1, "source_sha": commit, "ci_passed": ci_passed,
        "virus_total_status": status, "virus_total_evidence_matches_release": matches,
        "release_sha256": expected if sha_valid else None,
        "last_analysis_stats": stats, "detections": source.get("detections", []) if matches else [],
        "report_origin": source.get("report_origin") if matches else None,
        "analysis_updated_at": updated if matches else None,
        "analysis_age_days": age_days if matches else None,
        "error_code": source.get("error_code") if matches else "MISSING_OR_MISMATCHED_REPORT",
        "antivirus_review": "NO_DETECTIONS_REPORTED_RECENT" if clear else "NOT_CLEARED",
        "url": "https://www.virustotal.com/gui/file/" + expected + "/detection" if sha_valid else None,
        "limitations": "No antivirus scan proves an APK is harmless; human review remains necessary.",
    }
    destination = destination if destination is not None else OUTPUT / "VirusTotal"
    destination.mkdir(parents=True, exist_ok=True)
    (destination / "virustotal.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    safe = lambda x: str(x).replace("\n", " ").replace("|", "\\|").replace("`", "'")
    lines = [
        "# Aurelis Music — VirusTotal",
        "",
        f"- Pruebas B01–B05: **{'PASS' if ci_passed else 'FAIL'}**",
        f"- Resultado VirusTotal: **{status}**",
        f"- Revisión antivirus: **{report['antivirus_review']}**",
        f"- Correspondencia con APK Release: **{'VERIFICADA' if matches else 'NO VERIFICADA'}**",
        f"- SHA-256: `{safe(report['release_sha256'] or 'no_disponible')}`",
        f"- Fecha del análisis: `{safe(report['analysis_updated_at'] or 'no_disponible')}`",
        f"- Origen del resultado: `{safe(report['report_origin'] or 'no_disponible')}`",
        f"- Error: `{safe(report['error_code'] or 'ninguno')}`",
        "",
        "## Recuento de motores",
    ]
    for category, count in sorted(stats.items()):
        lines.append(f"- {safe(category)}: {safe(count)}")
    lines += ["", "## Detecciones"]
    for item in report["detections"] if isinstance(report["detections"], list) else []:
        if isinstance(item, dict):
            lines.append(f"- {safe(item.get('engine', 'sin_motor'))}: {safe(item.get('result', 'sin_detalle'))}")
    if not isinstance(report["detections"], list) or not report["detections"]:
        lines.append("- No hay detecciones registradas en la evidencia recibida (puede no haberse completado el análisis).")
    lines += [
        "", f"- Enlace: {report['url'] or 'no_disponible'}",
        "- ERROR, PENDING, NOT_CONFIGURED y REPORT_UNVERIFIED nunca equivalen a un análisis sin detecciones.",
        "- Incluso sin detecciones reportadas, VirusTotal no certifica que el APK sea seguro.",
        "- Las subidas públicas de APK a VirusTotal pueden compartirse con sus socios de seguridad.",
        "",
    ]
    (destination / "virustotal.md").write_text("\n".join(lines), encoding="utf-8")
    return report

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
    publish_release = release_expected()
    final_count = 3 if publish_release else 2
    delivery_note = (
        "Exactly three final artifacts: B01-B06+VirusTotal report ZIP, Debug ARM64 APK and signed Release ARM64 APK."
        if publish_release else
        "Exactly two final artifacts: B01-B06+VirusTotal report ZIP and Debug ARM64 APK. "
        "B04 verifies signed Release ARM64, but it is not published outside main/tags."
    )
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
                if file.suffix.lower() in (".zip", ".7z", ".rar"):
                    missing.append(f"{phase}/{folder}: nested archive requires explicit extraction: {file.relative_to(source)}")
                    all_present = False
                    continue
                relative = file.relative_to(source)
                target = dest / relative
                if target.exists() or target.is_symlink():
                    missing.append(f"{phase}/{folder}: duplicate report path: {relative}")
                    all_present = False
                    continue
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
    from b06_file_matrix import build_matrix
    matrix = build_matrix(OUTPUT, os.environ.get("GITHUB_SHA", ""))
    # F4 evidence-only report: no false per-file approval on incomplete inputs.
    from file_compile_matrix import generate as generate_f4
    f4_matrix = generate_f4(OUTPUT, os.environ.get("GITHUB_SHA", ""))
    from f4_integrity_gate import inspect as inspect_f4
    try:
        f4_integrity = inspect_f4(OUTPUT, os.environ.get("GITHUB_SHA", ""))
    except (ValueError, OSError, TypeError, KeyError, json.JSONDecodeError) as error:
        f4_integrity = {"status": "FAIL", "source_sha": os.environ.get("GITHUB_SHA"),
                        "errors": [str(error)], "native_apk_linkage_proven": False}
        (OUTPUT / "00-Resumen" / "f4-integrity-gate.json").write_text(
            json.dumps(f4_integrity, indent=2) + "\n", encoding="utf-8")
    from b06_quality_readiness import report as quality_report
    p26, p3 = quality_report(OUTPUT, os.environ.get("GITHUB_SHA", ""))
    index.extend([
        {"source_artifact": "runtime-derived", "phase": "P2.6",
         "files": ["P2.6/ui-functional-coverage.json", "P2.6/Resumen.md"]},
        {"source_artifact": "runtime-derived", "phase": "P3",
         "files": ["P3/audio-and-publication-readiness.json", "P3/Resumen.md"]},
    ])
    trusted_evidence = read_json(OUTPUT / "B04" / "Verificacion" / "source-evidence.json")
    evidence_file = OUTPUT / "B04" / "Verificacion" / "source-evidence.md"
    checks = {
        "p2_file_matrix_consistent": matrix["status"] == "COMPLETE_MATRIX",
        "f4_evidence_integrity_verified": (
            f4_integrity["status"] == "PASS_EVIDENCE_INTEGRITY_WITH_LINKAGE_LIMITATIONS"),
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
    virustotal = create_virustotal_delivery(passed, b04, os.environ.get("GITHUB_SHA", ""))
    # VirusTotal stays inside the single report artifact, including advisory errors.
    index.append({"source_artifact": "derived-from-B04", "phase": "VirusTotal",
                  "files": ["VirusTotal/virustotal.json", "VirusTotal/virustotal.md"]})
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
        "p2_file_matrix": {"status": matrix["status"], "tracked_files": matrix["tracked_files"], "rows": matrix["rows"], "issues": matrix["consistency_issues"]},
        "f4_file_compile_matrix": f4_matrix,
        "f4_evidence_integrity": f4_integrity,
        "p2_6_ui_functional_status": p26["functional_test_status"],
        "p3_mocked_controller_test_status": p3["mocked_controller_status"],
        "p3_audio_status": p3["audio_functional_test_status"],
        "p3_public_release_readiness": p3["public_release_readiness"],
        "source_evidence_reconciliation": {
            "status": trusted_evidence.get("status", "MISSING_EVIDENCE"),
            "source_coverage_status": trusted_evidence.get("source_coverage_status", "UNKNOWN"),
            "mandatory_problems": trusted_evidence.get("problems", []),
            "per_file_compilation_proven": False,
        },
        "apk_files_in_bundle": False,
        "virustotal": virustotal,
        "release_publish_expected": publish_release,
        "expected_final_artifacts": final_count,
        "note": delivery_note,
    }
    (OUTPUT / "B06" / "pipeline-results.json").write_text(
        json.dumps(report, indent=2) + "\n", encoding="utf-8")
    (OUTPUT / "B06" / "artifact-index.json").write_text(
        json.dumps(index, indent=2) + "\n", encoding="utf-8")
    lines = ["# Aurelis Music — Informes B01-B06",
             f"- Commit: `{report['commit']}`",
             f"- Resultado: **{report['status']}**",
             (f"- Entrega final: {final_count} artefactos, informes (VirusTotal incluido) y APK Debug ARM64" +
              (", más APK Release ARM64 firmado." if publish_release else
               "; APK Release firmado verificado en B04, sin publicación en esta referencia.")),
             "- La compilación individual de cada archivo no está totalmente demostrada.",
             "- Auditoría independiente B04: " + trusted_evidence.get("status", "MISSING_EVIDENCE"),
             "- Matriz por archivo P2.4: " + matrix["status"] + " (" + str(matrix["rows"]) + " filas).",
             "- P2.6 UI Compose: " + p26["functional_test_status"] + " (no equivale a smoke).",
             "- P3 controles con reproductor simulado: " + p3["mocked_controller_status"] + ".",
             "- P3 audio/publicación pública: " + p3["audio_functional_test_status"] + " / " + p3["public_release_readiness"],
             "- VirusTotal: " + virustotal["virus_total_status"] + " (véase VirusTotal/virustotal.md; no certifica seguridad)",
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
