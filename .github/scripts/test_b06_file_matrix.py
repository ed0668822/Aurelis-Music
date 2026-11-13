#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.4 regression tests for complete, truthful cross-report inventory."""
import csv
import json
import os
from contextlib import contextmanager
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import b06_file_matrix as matrix
import b06_bundle_reports as bundle

SHA = "a" * 40
SRC = "app/src/main/java/com/music/aurelis/ui/haptics/Haptics.kt"
TEST = "app/src/test/java/com/music/aurelis/HapticsTest.kt"
README = "README.md"
INV = [
    {"path": SRC, "role": "MANAGED_SOURCE", "kind": "production",
     "module": "app", "source_set": "main", "language": "Kotlin", "file_state": "FILE", "sha256": "b" * 64},
    {"path": TEST, "role": "MANAGED_SOURCE", "kind": "test",
     "module": "app", "source_set": "test", "language": "Kotlin", "file_state": "FILE", "sha256": "c" * 64},
    {"path": README, "role": "DOCUMENTATION", "kind": "support",
     "module": ".", "source_set": "", "language": "DOCUMENTATION", "file_state": "FILE", "sha256": "d" * 64},
]


def put(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value) + "\n", encoding="utf-8")


def tsv(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(data[0]), delimiter="\t")
        writer.writeheader()
        writer.writerows(data)


class MatrixTests(unittest.TestCase):
    def fixture(self, package):
        b04 = package / "B04" / "Verificacion"
        put(b04 / "source-inventory.json", INV)
        tsv(b04 / "p2-inventory-files.tsv", [
            {"path": x["path"], "role": x["role"], "file_state": x["file_state"],
             "sha256": x["sha256"], "root_evidence": "NOT_OBSERVED",
             "task_input_evidence": "NOT_OBSERVED", "observed_tasks": ""}
            for x in INV
        ])
        put(b04 / "p2-inventory-coverage.json", {
            "source_sha": SHA, "status": "PASS_INVENTORY",
            "tracked_files": 3, "inventory_files": 3, "indexed_files": 3,
        })
        compile_rows = [
            {"variant": v, "path": x["path"], "role": x["role"],
             "compiler_evidence": "COMPILER_TASK_SUCCEEDED_WITH_INPUT",
             "task_state": "SUCCESS", "task_input_observed": True, "attributed_errors": 0}
            for v in matrix.VARIANTS for x in INV if x["role"] == "MANAGED_SOURCE"
        ]
        put(b04 / "p2-compiler-evidence.json", {
            "summary": {"status": "EVIDENCE_RECORDED", "source_sha": SHA,
                        "source_files": 2, "variant_rows": 4},
            "rows": compile_rows,
        })
        apk_rows = [
            {"variant": v, "path": x["path"], "role": x["role"], "kind": x["kind"],
             "apk_state": "NOT_EXPECTED_IN_APP_BY_POLICY" if x["kind"] != "production"
                          else "COMPILER_CLASSES_MAPPED_TO_DEX",
             "class_evidence": "DEX_CLASSES_OBSERVED" if x["kind"] == "production" else "NOT_APPLICABLE",
             "packaging_evidence": "OUT_OF_PRODUCTION_APK_SCOPE" if x["kind"] != "production" else "NOT_ESTABLISHED",
             "class_count": 1 if x["kind"] == "production" else 0}
            for v in matrix.VARIANTS for x in INV
        ]
        put(b04 / "p2-apk-trace.json", {
            "summary": {"source_sha": SHA, "status": "EVIDENCE_RECONCILED",
                        "tracked_files": 3, "variant_rows": 6},
            "rows": apk_rows,
        })
        put(b04 / "source-evidence.json", {
            "source_sha": SHA, "status": "PASS_VERIFIED_GATES",
            "source_rows": [
                {"variant": v, "path": x["path"], "role": x["role"],
                 "classification": "CLASS_MAPPING_OBSERVED" if x["kind"] == "production" else "OUT_OF_APK_SCOPE"}
                for v in matrix.VARIANTS for x in INV
            ],
        })
        put(b04 / "diagnostics.json", [
            {"phase": "build", "severity": "warning", "file": SRC,
             "line": 25, "message": "Compiler warning"},
        ])
        import hashlib
        (b04 / "build.log").write_text('AURELIS_TASK_RESULT {"task":":app:compileDebugKotlin","state":"SUCCESS"}\\n')
        put(b04 / "diagnostics-provenance.json", {
            "schema_version": 1, "source_sha": SHA, "status": "PASS_LOG_PARSED",
            "terminal_gradle_tasks": 1, "diagnostic_count": 1,
            "build_log_sha256": hashlib.sha256((b04 / "build.log").read_bytes()).hexdigest(),
            "diagnostics_sha256": hashlib.sha256((b04 / "diagnostics.json").read_bytes()).hexdigest(),
        })
        tsv(package / "B02" / "Lint" / "issues.tsv", [
            {"severity": "Warning", "id": "ContextLeak",
             "file": "/home/runner/work/Aurelis-Music/Aurelis-Music/" + SRC,
             "line": "10", "message": "Lint warning"},
        ])
        return b04

    def test_every_path_two_variants_and_stages_separate(self):
        with tempfile.TemporaryDirectory() as temp:
            package = Path(temp)
            self.fixture(package)
            report = matrix.build_matrix(package, SHA)
            self.assertEqual(report["status"], "COMPLETE_MATRIX")
            self.assertEqual((report["tracked_files"], report["rows"]), (3, 6))
            data = json.loads((package / "00-Resumen/estado-todos-los-archivos.json").read_text())["files"]
            self.assertTrue(all(row["individual_compilation"] == "NOT_ESTABLISHED" for row in data))
            self.assertEqual(next(x for x in data if x["path"] == SRC)["lint_warnings"], 1)
            self.assertEqual(next(x for x in data if x["path"] == SRC)["gradle_diagnostic_warnings"], 1)
            self.assertEqual(next(x for x in data if x["path"] == TEST)["unit_test_scope"],
                             "B03_AGGREGATE_NOT_PER_FILE")
            self.assertEqual(next(x for x in data if x["path"] == README)["compiler_evidence"],
                             "NOT_APPLICABLE_BY_ROLE")

    def test_missing_per_variant_row_is_reported_not_marked_compiled(self):
        with tempfile.TemporaryDirectory() as temp:
            package = Path(temp)
            b04 = self.fixture(package)
            file = b04 / "p2-apk-trace.json"
            content = json.loads(file.read_text())
            content["rows"].pop()
            put(file, content)
            report = matrix.build_matrix(package, SHA)
            self.assertEqual(report["status"], "INCOMPLETE_MATRIX")
            self.assertIn("P2_APK_ROW_SET_MISMATCH", report["consistency_issues"])
            self.assertEqual(report["rows"], 6)

    def test_commit_mismatch_and_missing_inventory_evidence(self):
        with tempfile.TemporaryDirectory() as temp:
            package = Path(temp)
            b04 = self.fixture(package)
            file = b04 / "p2-inventory-coverage.json"
            content = json.loads(file.read_text())
            content["source_sha"] = "e" * 40
            put(file, content)
            report = matrix.build_matrix(package, SHA)
            self.assertEqual(report["status"], "INCOMPLETE_MATRIX")
            self.assertIn("MISSING_STALE_OR_FAILED_REPORT:P2_INVENTORY", report["consistency_issues"])

    def test_duplicate_rows_are_never_silently_overwritten(self):
        with tempfile.TemporaryDirectory() as temp:
            package = Path(temp)
            b04 = self.fixture(package)
            path = b04 / "p2-compiler-evidence.json"
            data = json.loads(path.read_text())
            data["rows"].append(data["rows"][0])
            put(path, data)
            report = matrix.build_matrix(package, SHA)
            self.assertTrue(any(x.startswith("DUPLICATE_ROW:p2-compiler:") for x in report["consistency_issues"]))

    def test_unmatched_lint_findings_remain_in_report(self):
        with tempfile.TemporaryDirectory() as temp:
            package = Path(temp)
            self.fixture(package)
            path = package / "B02/Lint/issues.tsv"
            with path.open("a", encoding="utf-8") as stream:
                stream.write("Error\tOther\t/foreign/Unknown.kt\t2\tUnmatched issue\n")
            report = matrix.build_matrix(package, SHA)
            self.assertEqual(report["unmatched_diagnostics"], 1)
            errors = (package / "00-Resumen/errores.csv").read_text()
            self.assertIn("Unknown.kt", errors)


    def test_missing_or_modified_gradle_diagnostics_is_not_green(self):
        for case in ("missing", "tampered", "wrong-commit"):
            with self.subTest(case=case), tempfile.TemporaryDirectory() as tmp:
                package = Path(tmp)
                b04 = self.fixture(package)
                if case == "missing":
                    (b04 / "diagnostics.json").unlink()
                elif case == "tampered":
                    (b04 / "diagnostics.json").write_text("[]\n")
                else:
                    path = b04 / "diagnostics-provenance.json"
                    record = json.loads(path.read_text())
                    record["source_sha"] = "e" * 40
                    put(path, record)
                report = matrix.build_matrix(package, SHA)
                self.assertEqual(report["status"], "INCOMPLETE_MATRIX")
                self.assertTrue(any("GRADLE_DIAGNOSTIC" in i for i in report["consistency_issues"]))

class BundleFailureTests(unittest.TestCase):
    """Synthetic artifact fixtures, never substituted for actual CI evidence."""

    @contextmanager
    def pipeline(self, b04_result="skipped", include_b04=False):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            incoming, output = root / "input", root / "package"
            for phase, groups in bundle.SOURCES.items():
                if phase == "B04" and not include_b04:
                    continue
                for _, prefix in groups:
                    folder = incoming / (prefix + "117")
                    folder.mkdir(parents=True)
                    (folder / "summary.txt").write_text("Synthetic artifact fixture\n")
            put(incoming / "b03-summary-117/results.json", {
                "debug": {"passed_gate": True}, "release": {"passed_gate": True},
            })
            put(incoming / "b05-android-emulator-117/results.json", {"pass": True, "commit": SHA})
            results = {f"b0{i}": {"result": "success"} for i in range(1, 6)}
            results["b04"]["result"] = b04_result
            if b04_result == "skipped":
                results["b02"]["result"] = "failure"
            env = {
                "STAGES_JSON": json.dumps(results), "GITHUB_SHA": SHA,
                "GITHUB_RUN_ID": "117", "GITHUB_REF_NAME": "feature/bitchord-local-migration",
                "GITHUB_STEP_SUMMARY": str(root / "step-summary.md"),
                "DOWNLOAD_OUTCOME": "success", "RELEASE_PUBLISH_EXPECTED": "false",
            }
            with patch.object(bundle, "INPUT", incoming), patch.object(bundle, "OUTPUT", output), patch.dict(os.environ, env):
                yield incoming, output

    def result(self, output):
        return json.loads((output / "B06/pipeline-results.json").read_text())

    def test_skipped_b04_still_bundles_failure_and_original_phases(self):
        with self.pipeline() as (_, output):
            bundle.main()
            report = self.result(output)
            self.assertEqual(report["status"], "FAIL")
            self.assertEqual(report["phases"]["B02"], "failure")
            self.assertEqual(report["phases"]["B04"], "skipped")
            self.assertEqual(report["phases"]["B03"], "success")
            self.assertTrue(report["checks"]["debug_tests_success"])
            self.assertFalse(report["checks"]["stage_jobs_success"])
            self.assertFalse(report["checks"]["f4_matrix_generated"])
            self.assertEqual(report["f4_file_compile_matrix"]["status"], "NOT_GENERATED")
            self.assertEqual(report["f4_evidence_integrity"]["status"], "FAIL")
            self.assertTrue(any("p2-apk-trace.json" in error for error in report["missing_reports"]))
            self.assertTrue((output / "B06/summary.md").is_file())
            self.assertTrue((output / "B06/artifact-index.json").is_file())
            self.assertTrue((output / "00-Resumen/f4-generation-error.json").is_file())
            self.assertFalse((output / "B04/Verificacion/p2-apk-trace.json").exists())
            self.assertFalse((output / "00-Resumen/file-compile-matrix.json").exists())
            self.assertEqual(report["p3_public_release_readiness"], "NOT_APPROVED")

    def test_successful_b04_job_without_its_evidence_does_not_pass(self):
        with self.pipeline("success", include_b04=True) as (_, output):
            bundle.main()
            report = self.result(output)
            self.assertTrue(report["checks"]["stage_jobs_success"])
            self.assertEqual(report["phases"]["B04"], "success")
            self.assertEqual(report["status"], "FAIL")
            self.assertFalse(report["checks"]["f4_matrix_generated"])
            self.assertFalse(report["checks"]["f4_evidence_integrity_verified"])

    def test_malformed_trace_remains_failure_with_diagnostic(self):
        with self.pipeline("success", include_b04=True) as (incoming, output):
            (incoming / "b04-verification-report-117/p2-apk-trace.json").write_text("{invalid")
            bundle.main()
            self.assertEqual(self.result(output)["status"], "FAIL")
            diagnostic = json.loads((output / "00-Resumen/f4-generation-error.json").read_text())
            self.assertEqual(diagnostic["status"], "NOT_GENERATED")
            self.assertTrue(diagnostic["errors"])

    def test_symlink_trace_is_not_copied_or_certified(self):
        with self.pipeline("success", include_b04=True) as (incoming, output):
            external = incoming.parent / "external.json"
            sentinel = '{"summary":{"status":"EVIDENCE_RECONCILED"}}'
            external.write_text(sentinel)
            (incoming / "b04-verification-report-117/p2-apk-trace.json").symlink_to(external)
            bundle.main()
            self.assertEqual(self.result(output)["status"], "FAIL")
            self.assertFalse((output / "B04/Verificacion/p2-apk-trace.json").exists())
            self.assertEqual(external.read_text(), sentinel)

    def test_failed_generation_removes_only_stale_derived_f4_outputs(self):
        with self.pipeline() as (_, output):
            summary = output / "00-Resumen"
            summary.mkdir(parents=True)
            names = ("file-compile-matrix.json", "file-compile-matrix.tsv", "CI14-F4.md")
            for name in names:
                (summary / name).write_text("stale successful report")
            bundle.main()
            self.assertEqual(self.result(output)["status"], "FAIL")
            self.assertTrue(all(not (summary / name).exists() for name in names))
            self.assertTrue((summary / "estado-todos-los-archivos.json").is_file())

    def test_valid_generation_is_preserved_and_clears_previous_error(self):
        with tempfile.TemporaryDirectory() as tmp:
            package = Path(tmp)
            self.assertEqual(bundle.compilation_matrix(package, SHA, "skipped")["status"], "NOT_GENERATED")
            b04 = MatrixTests().fixture(package)
            put(b04 / "apk-content-release.json", {"r8": {"mapping.txt": {"sha256": "c" * 64}}})
            self.assertEqual(matrix.build_matrix(package, SHA)["status"], "COMPLETE_MATRIX")
            report = bundle.compilation_matrix(package, SHA, "success")
            self.assertEqual(report["status"], "CLASSIFIED_WITH_LIMITATIONS")
            self.assertEqual(report["total_rows"], 6)
            self.assertTrue((package / "00-Resumen/file-compile-matrix.json").is_file())
            self.assertFalse((package / "00-Resumen/f4-generation-error.json").exists())

    def test_stale_trace_sha_is_reported_without_certification(self):
        with tempfile.TemporaryDirectory() as tmp:
            package = Path(tmp)
            b04 = MatrixTests().fixture(package)
            put(b04 / "apk-content-release.json", {})
            path = b04 / "p2-apk-trace.json"
            trace = json.loads(path.read_text())
            trace["summary"]["source_sha"] = "e" * 40
            put(path, trace)
            matrix.build_matrix(package, SHA)
            report = bundle.compilation_matrix(package, SHA, "success")
            self.assertEqual(report["status"], "NOT_GENERATED")
            self.assertIn("stale", report["errors"][0])
            self.assertFalse((package / "00-Resumen/file-compile-matrix.json").exists())


if __name__ == "__main__":
    unittest.main()
