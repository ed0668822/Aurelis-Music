#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.4 regression tests for complete, truthful cross-report inventory."""
import csv
import json
from pathlib import Path
import tempfile
import unittest

import b06_file_matrix as matrix

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

if __name__ == "__main__":
    unittest.main()
