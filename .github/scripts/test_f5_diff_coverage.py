#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Diff coverage fixtures: no unverified success on empty data."""
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
import f5_diff_coverage as cov


class CoverageTests(unittest.TestCase):
    def test_no_xml_is_unknown_not_success(self):
        with TemporaryDirectory() as tmp:
            p = Path(tmp)
            (p / ".git").mkdir()
            with patch.object(cov, "changed", return_value={"app/src/main/java/x/Foo.kt":{5}}):
                value = cov.analyze(p, p / "missing", "origin/main")
            self.assertEqual(value["status"], "NOT_MEASURED")

    def test_modified_lines_are_counted_not_global_coverage(self):
        with TemporaryDirectory() as tmp:
            p = Path(tmp)
            (p / ".git").mkdir()
            reports = p / "reports"
            reports.mkdir()
            (reports / "report.xml").write_text(
                '<report name="a"><package name="x"><sourcefile name="Foo.kt">'
                '<line nr="5" mi="0" ci="3"/><line nr="7" mi="2" ci="0"/>'
                '</sourcefile></package></report>')
            with patch.object(cov, "changed",
                    return_value={"app/src/main/java/x/Foo.kt":{5,7}}):
                value = cov.analyze(p, reports, "origin/main")
            self.assertEqual(value["status"], "MEASURED")
            self.assertEqual(value["executable_lines"], 2)
            self.assertEqual(value["coverage_percent"], 50)

    def test_unmatched_source_reports_unknown(self):
        with TemporaryDirectory() as tmp:
            p = Path(tmp)
            (p / ".git").mkdir()
            reports = p / "reports"
            reports.mkdir()
            (reports / "report.xml").write_text('<report name="none"/>')
            with patch.object(cov, "changed", return_value={"app/src/main/java/x/Foo.kt":{1}}):
                self.assertEqual(cov.analyze(p, reports, "origin/main")["status"], "NOT_MEASURED")


if __name__ == "__main__":
    unittest.main()
