#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""F3-a: regression fixtures for CI DAG and integrity gates."""
import re
from pathlib import Path
import unittest

WORKFLOW = Path(__file__).resolve().parent.parent / "workflows/b06-unified-pipeline.yml"


def job(text, name):
    pattern = r"(?ms)^  " + re.escape(name) + r":\n(.*?)(?=^  [a-z0-9_-]+:\n|\Z)"
    match = re.search(pattern, text)
    if match is None:
        raise AssertionError(f"Missing job: {name}")
    return match.group(1)


class GraphTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.flow = WORKFLOW.read_text(encoding="utf-8")

    def test_b02_and_b03_both_depend_on_successful_b01(self):
        for name in ("b02", "b03"):
            body = job(self.flow, name)
            self.assertRegex(body, r"(?m)^    needs: b01$")
            self.assertIn("needs.b01.result == 'success'", body)

    def test_three_independent_parallel_jobs_after_b01(self):
        for name in ("b02", "b03", "b05"):
            body = job(self.flow, name)
            self.assertRegex(body, r"(?m)^    needs: b01$")
            self.assertIn("needs.b01.result == 'success'", body)

    def test_one_push_trigger_not_two_full_runs(self):
        self.assertIn("  push:", self.flow)
        self.assertNotIn("  pull_request:", self.flow)
        self.assertIn("cancel-in-progress: true", self.flow)

    def test_b04_requires_both_lint_and_unit_tests(self):
        body = job(self.flow, "b04")
        self.assertIn("needs: [b02, b03]", body)
        self.assertIn("needs.b02.result == 'success' && needs.b03.result == 'success'", body)
        self.assertNotIn("secrets: inherit", body)
        for name in ["KEYSTORE_BASE64","KEYSTORE_PASSWORD","KEY_ALIAS","KEY_PASSWORD","LISTEN_TOGETHER_SERVER","VIRUSTOTAL_API_KEY"]:
            self.assertIn(f"${{ secrets.{name} }}", body)

    def test_b05_reuses_b01_handoff_without_waiting_on_b04(self):
        body = job(self.flow, "b05")
        self.assertRegex(body, r"(?m)^    needs: b01$")
        self.assertIn("needs.b01.result == 'success'", body)

    def test_concurrency_does_not_define_duplicate_yaml_keys(self):
        for name in ("b04-verify-apks.yml", "b06-unified-pipeline.yml"):
            path = WORKFLOW.parent / name
            data = path.read_text(encoding="utf-8")
            block = data.split("concurrency:", 1)[1].split("jobs:", 1)[0]
            self.assertEqual(block.count("cancel-in-progress:"), 1)
            expected = "true" if name.startswith("b06-") else "false"
            self.assertIn(f"cancel-in-progress: {expected}", block)

    def test_b06_collects_all_and_preserves_failure_evidence(self):
        body = job(self.flow, "b06")
        self.assertIn("needs: [b01, b02, b03, b04, b05]", body)
        self.assertIn("if: ${{ always() }}", body)
        self.assertIn("actions: write", body)


if __name__ == "__main__":
    unittest.main()
