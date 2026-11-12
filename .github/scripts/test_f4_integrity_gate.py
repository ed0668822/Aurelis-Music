#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""No-network F4 gate regression: reject missing hashes, false success, stale SHA."""
import copy
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

import f4_integrity_gate as gate

SHA = "a" * 40
NATIVE = {
    "schema_version": 1, "source_sha": SHA, "status": "PASS",
    "declarations": 1, "object_files_observed": 1, "errors": [],
    "rows": [{"variant": "release", "path": "app/src/main/cpp/lib.cpp",
              "status": "OBJECT_SHA256_OBSERVED", "object_sha256": "b" * 64,
              "object_bytes": 72, "apk_link_proven": False}],
}
ROWS = [
    {"variant": variant, "path": "app/src/main/java/example/Foo.kt", "role": "MANAGED_SOURCE",
     "verdict": "COMPILADO_EN_APK", "individual_compilation_proven": True}
    for variant in ("debug", "release")
] + [
    {"variant": variant, "path": "app/src/main/cpp/lib.cpp", "role": "NATIVE_SOURCE",
     "verdict": "NO_EVIDENCIA_SUFICIENTE", "individual_compilation_proven": False}
    for variant in ("debug", "release")
]
MATRIX = {"summary": {"source_sha": SHA, "status": "CLASSIFIED_WITH_LIMITATIONS",
                      "total_rows": 4, "release_pending_evidence": 1}, "files": ROWS}
DEBUG = {"source_sha": SHA, "status": "PASS", "expected_source_count": 1,
         "matched_source_count": 1, "missing": [], "problems": []}


class IntegrityTests(unittest.TestCase):
    def stage(self, folder, matrix=MATRIX, debug=DEBUG, native=NATIVE):
        for name, data in (
            ("00-Resumen/file-compile-matrix.json", matrix),
            ("B01/Compilacion/f4-debug-inputs.json", debug),
            ("B04/Verificacion/native-object-evidence.json", native),
        ):
            path = Path(folder) / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(data), encoding="utf-8")

    def test_valid_reports_are_consistent(self):
        with TemporaryDirectory() as folder:
            self.stage(folder)
            report = gate.inspect(Path(folder), SHA)
            self.assertEqual(report["native_objects_with_sha256"], 1)
            self.assertFalse(report["native_apk_linkage_proven"])

    def test_missing_and_stale_sources_fail(self):
        with TemporaryDirectory() as folder:
            self.stage(folder)
            with self.assertRaisesRegex(ValueError, "source_sha"):
                gate.inspect(Path(folder), "c" * 40)
            (Path(folder) / "B01/Compilacion/f4-debug-inputs.json").unlink()
            with self.assertRaisesRegex(ValueError, "Missing"):
                gate.inspect(Path(folder), SHA)

    def test_declared_object_must_exist(self):
        with TemporaryDirectory() as folder:
            obj = copy.deepcopy(NATIVE)
            obj["object_files_observed"] = 0
            obj["rows"][0]["status"] = "NO_OBJECT_EVIDENCE"
            self.stage(folder, native=obj)
            with self.assertRaisesRegex(ValueError, "declared native"):
                gate.inspect(Path(folder), SHA)

    def test_native_object_is_not_apk_link_proof(self):
        with TemporaryDirectory() as folder:
            altered = copy.deepcopy(MATRIX)
            altered["files"][3]["verdict"] = "COMPILADO_EN_APK"
            self.stage(folder, matrix=altered)
            with self.assertRaisesRegex(ValueError, "Native source claimed"):
                gate.inspect(Path(folder), SHA)

    def test_unproven_managed_classes_fail(self):
        with TemporaryDirectory() as folder:
            altered = copy.deepcopy(MATRIX)
            altered["files"][0]["individual_compilation_proven"] = False
            self.stage(folder, matrix=altered)
            with self.assertRaisesRegex(ValueError, "Unproven managed"):
                gate.inspect(Path(folder), SHA)


if __name__ == "__main__":
    unittest.main()
