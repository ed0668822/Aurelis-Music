#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Regression tests: confidence labels must never invent source compilation."""
import json
from pathlib import Path
import tempfile
import unittest

import verify_evidence as verifier


class EvidencePolicyTests(unittest.TestCase):
    def test_no_errors_is_not_source_proof(self):
        state, _ = verifier.source_evidence(
            {"role": "MANAGED_SOURCE", "kind": "production"}, None)
        self.assertEqual(state, "EVIDENCE_UNAVAILABLE")

    def test_class_mapping_is_not_per_file_compile_proof(self):
        state, explanation = verifier.source_evidence(
            {"role": "MANAGED_SOURCE", "kind": "production"},
            {"state": "COMPILER_CLASSES_MAPPED_TO_DEX"})
        self.assertEqual(state, "CLASS_MAPPING_OBSERVED")
        self.assertIn("NOT proven", explanation)

    def test_unused_cpp_does_not_count_as_build_failure(self):
        state, _ = verifier.source_evidence(
            {"role": "NATIVE_SOURCE", "kind": "production"},
            {"state": "NO_NATIVE_UNIT_EVIDENCE"})
        self.assertEqual(state, "NATIVE_EXECUTION_UNVERIFIED")

    def test_native_command_is_not_execution_proof(self):
        state, _ = verifier.source_evidence(
            {"role": "NATIVE_SOURCE", "kind": "production"},
            {"state": "COMPILE_COMMAND_DECLARED_NOT_EXECUTION_PROOF"})
        self.assertEqual(state, "NATIVE_COMMAND_DECLARED")

    def test_tests_not_expected_in_apk(self):
        state, _ = verifier.source_evidence(
            {"role": "MANAGED_SOURCE", "kind": "test"}, None)
        self.assertEqual(state, "OUT_OF_APK_SCOPE")

    def test_prebuilt_binaries_are_not_claimed_recompiled(self):
        state, explanation = verifier.source_evidence(
            {"role": "PREBUILT_NATIVE", "kind": "production"},
            {"state": "APK_BYTES_MATCH"})
        self.assertEqual(state, "PREBUILT_PACKAGED_EXACT")
        self.assertIn("prebuilt", explanation)

    def test_task_outcome_parsing(self):
        sample = '\n'.join([
            'AURELIS_TASK_RESULT {"task":":app:compileDebugKotlin","state":"SUCCESS"}',
            'AURELIS_TASK_RESULT {"task":":app:compileReleaseKotlin","state":"FROM-CACHE"}',
            'AURELIS_TASK_RESULT {"task":":app:bad","state":"INVALID"}',
        ])
        result = verifier.parse_tasks(sample)
        self.assertEqual(result[":app:compileDebugKotlin"], "SUCCESS")
        self.assertEqual(result[":app:compileReleaseKotlin"], "FROM-CACHE")
        self.assertNotIn(":app:bad", result)

    def test_prevent_path_traversal(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.assertIsNone(verifier.safe_apk(root, "../../untrusted.apk"))
            self.assertIsNone(verifier.safe_apk(root, "/tmp/untrusted.apk"))
            self.assertIsNone(verifier.safe_apk(root, "missing.apk"))

    def test_missing_evidence_is_explicit(self):
        with tempfile.TemporaryDirectory() as folder:
            issues = []
            result = verifier.load_json(Path(folder) / "missing.json", dict, issues)
            self.assertEqual(result, {})
            self.assertTrue(any("MISSING_EVIDENCE" in issue for issue in issues))


if __name__ == "__main__":
    unittest.main()
