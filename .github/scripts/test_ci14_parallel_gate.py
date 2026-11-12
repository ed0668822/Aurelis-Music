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

    def test_b04_requires_both_lint_and_unit_tests(self):
        body = job(self.flow, "b04")
        self.assertIn("needs: [b02, b03]", body)
        self.assertIn("needs.b02.result == 'success' && needs.b03.result == 'success'", body)
        self.assertIn("secrets: inherit", body)

    def test_b05_requires_b04(self):
        body = job(self.flow, "b05")
        self.assertRegex(body, r"(?m)^    needs: b04$")
        self.assertIn("needs.b04.result == 'success'", body)

    def test_b06_collects_all_and_preserves_failure_evidence(self):
        body = job(self.flow, "b06")
        self.assertIn("needs: [b01, b02, b03, b04, b05]", body)
        self.assertIn("if: ${{ always() }}", body)
        self.assertIn("actions: write", body)


if __name__ == "__main__":
    unittest.main()
