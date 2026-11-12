#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Offline regression tests for dual-variant Android Lint reports."""
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
import b02_lint_summary as lint


class LintSummaryTests(unittest.TestCase):
    def write(self, root, variant, content):
        xml = root / f"app/build/reports/lint-results-{variant}.xml"
        xml.parent.mkdir(parents=True, exist_ok=True)
        xml.write_text(content, encoding="utf-8")

    def test_both_variant_counts_and_tsv_rows(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.write(root, "debug", '<issues><issue id="D" severity="Warning" message="d"/></issues>')
            self.write(root, "release", '<issues><issue id="R" severity="Warning" message="r"/></issues>')
            result = lint.summarize(root, root / "reports", "success")
            self.assertTrue(result["passed_gate"])
            self.assertEqual(result["variants"]["release"]["warnings"], 1)
            self.assertEqual(result["variants"]["debug"]["warnings"], 1)
            self.assertIn("release", (root / "reports/issues.tsv").read_text())
            self.assertTrue((root / "reports/lint-results-release.xml").is_file())

    def test_missing_release_fails(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.write(root, "debug", "<issues/>")
            result = lint.summarize(root, root / "reports", "success")
            self.assertFalse(result["passed_gate"])
            self.assertFalse(result["variants"]["release"]["xml_parsed"])

    def test_malformed_xml_fails_without_stale_output(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            output = root / "reports"
            output.mkdir()
            (output / "lint-results-release.xml").write_text("stale")
            self.write(root, "debug", "<issues/>")
            self.write(root, "release", "<issues><issue>")
            result = lint.summarize(root, output, "success")
            self.assertFalse(result["passed_gate"])
            self.assertFalse((output / "lint-results-release.xml").exists())

    def test_release_errors_fail_even_with_gradle_success(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.write(root, "debug", "<issues/>")
            self.write(root, "release", '<issues><issue severity="Error"/></issues>')
            result = lint.summarize(root, root / "reports", "success")
            self.assertFalse(result["passed_gate"])
            self.assertEqual(result["variants"]["release"]["errors"], 1)

    def test_gradle_failure_never_becomes_green(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            for variant in lint.VARIANTS:
                self.write(root, variant, "<issues/>")
            result = lint.summarize(root, root / "reports", "failure")
            self.assertFalse(result["passed_gate"])


if __name__ == "__main__":
    unittest.main()
