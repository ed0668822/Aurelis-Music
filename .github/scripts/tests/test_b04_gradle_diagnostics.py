#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Verify B04 never invents an empty diagnostics report."""
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import b04_gradle_diagnostics as audit

SHA = "a" * 40
TASK = 'AURELIS_TASK_RESULT {"task":":app:compileDebugKotlin","state":"SUCCESS"}\n'


class Tests(unittest.TestCase):
    def fixture(self, root, text=TASK):
        out = root / "ci-artifacts"
        out.mkdir()
        (out / "metadata.json").write_text(json.dumps({"source_sha": SHA, "checkout_sha": SHA}))
        (out / "build.log").write_text(text)
        return out

    def test_detected_warning_and_hash_are_real(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            out = self.fixture(root, TASK + "w: app/src/main/Foo.kt:8:3: deprecated\n")
            result = audit.collect(root, out, SHA)
            records = json.loads((out / "diagnostics.json").read_text())
            self.assertEqual(result["status"], "PASS_LOG_PARSED")
            self.assertEqual(len(records), 1)
            self.assertEqual(records[0]["severity"], "warning")
            self.assertEqual(records[0]["file"], "app/src/main/Foo.kt")
            self.assertEqual(result["diagnostics_sha256"],
                             hashlib.sha256((out / "diagnostics.json").read_bytes()).hexdigest())

    def test_confirmed_no_warnings_can_be_empty(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            out = self.fixture(root)
            self.assertEqual(audit.collect(root, out, SHA)["status"], "PASS_LOG_PARSED")
            self.assertEqual(json.loads((out / "diagnostics.json").read_text()), [])

    def test_no_task_evidence_blocks_empty_result(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            out = self.fixture(root, "")
            report = audit.collect(root, out, SHA)
            self.assertIn("NO_TERMINAL_GRADLE_TASK_RESULTS", report["issues"])
            self.assertFalse((out / "diagnostics.json").exists())

    def test_missing_log_and_stale_commit_fail(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            out = self.fixture(root)
            (out / "build.log").unlink()
            report = audit.collect(root, out, "b" * 40)
            self.assertEqual(report["status"], "INCOMPLETE_EVIDENCE")
            self.assertIn("MISSING_OR_UNSAFE_BUILD_LOG", report["issues"])
            self.assertIn("SOURCE_COMMIT_MISMATCH", report["issues"])

    def test_malformed_task_observation_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            out = self.fixture(root, TASK + "AURELIS_TASK_INPUT bogus\n")
            result = audit.collect(root, out, SHA)
            self.assertIn("MALFORMED_TASK_OBSERVATIONS", result["issues"])
            self.assertFalse((out / "diagnostics.json").exists())


if __name__ == "__main__":
    unittest.main()
