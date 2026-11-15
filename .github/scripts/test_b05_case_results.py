#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""No-network tests for the eight independent Media3 test verdicts."""
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

import b05_case_results as suite


def passed_log(klass, method):
    return (
        f"INSTRUMENTATION_STATUS: class={suite.PREFIX}{klass}\n"
        f"INSTRUMENTATION_STATUS: test={method}\n"
        "INSTRUMENTATION_STATUS_CODE: 1\n"
        f"INSTRUMENTATION_STATUS: test={method}\n"
        "INSTRUMENTATION_STATUS_CODE: 0\n"
        "OK (1 test)\n"
        "INSTRUMENTATION_CODE: -1\n"
    )


class CaseTests(unittest.TestCase):
    def test_eight_unique_methods(self):
        self.assertEqual(len(suite.CASES), 8)
        self.assertEqual(len({x[0] for x in suite.CASES}), 8)
        self.assertEqual(len({(x[1], x[2]) for x in suite.CASES}), 8)

    def test_missing_results_fail_without_success_markers(self):
        with TemporaryDirectory() as path:
            result = suite.summarize(Path(path))
            self.assertFalse(result["all_passed"])
            self.assertEqual(result["passed_count"], 0)
            self.assertFalse((Path(path) / "p3-instrumentation-pass.txt").exists())

    def test_eight_independent_successes(self):
        with TemporaryDirectory() as path:
            root = Path(path)
            (root / "p3-tests").mkdir()
            for slug, klass, method, marker in suite.CASES:
                (root / f"p3-tests/{slug}.log").write_text(passed_log(klass, method))
                (root / f"p3-tests/{slug}.exit").write_text("0\n")
            result = suite.summarize(root)
            self.assertTrue(result["all_passed"])
            self.assertEqual(result["passed_count"], 8)
            self.assertTrue((root / "p3-instrumentation-pass.txt").is_file())

    def test_single_failure_does_not_mask_other_seven(self):
        with TemporaryDirectory() as path:
            root = Path(path)
            (root / "p3-tests").mkdir()
            for slug, klass, method, marker in suite.CASES:
                (root / f"p3-tests/{slug}.log").write_text(passed_log(klass, method))
                (root / f"p3-tests/{slug}.exit").write_text("1\n" if slug == "queue" else "0\n")
            result = suite.summarize(root)
            self.assertFalse(result["all_passed"])
            self.assertEqual(result["passed_count"], 7)
            self.assertFalse(result["tests"]["queue"]["passed"])
            self.assertTrue(result["tests"]["continuous"]["passed"])
            self.assertFalse((root / "p3-instrumentation-pass.txt").exists())

    def test_old_seven_successes_cannot_attest_the_new_system_case(self):
        with TemporaryDirectory() as path:
            root = Path(path)
            (root / "p3-tests").mkdir()
            for slug, klass, method, _ in suite.CASES:
                if slug == "system-controls":
                    continue
                (root / f"p3-tests/{slug}.log").write_text(passed_log(klass, method))
                (root / f"p3-tests/{slug}.exit").write_text("0\n")
            result = suite.summarize(root)
            self.assertEqual(result["passed_count"], 7)
            self.assertFalse(result["all_passed"])
            self.assertFalse((root / "p3-system-controls-pass.txt").exists())
            self.assertFalse((root / "p3-instrumentation-pass.txt").exists())

    def test_runner_reports_failure_even_with_success_footer(self):
        _, klass, method, _ = suite.CASES[0]
        ok, problems = suite.verdict(passed_log(klass, method) +
                                      "INSTRUMENTATION_STATUS_CODE: -2\n", klass, method, "0")
        self.assertFalse(ok)
        self.assertIn("ANDROID_RUNNER_REPORTED_FAILURE", problems)

    def test_wrong_method_cannot_pass(self):
        _, klass, method, _ = suite.CASES[0]
        ok, _ = suite.verdict(passed_log(klass, "otherMethod"), klass, method, "0")
        self.assertFalse(ok)


if __name__ == "__main__":
    unittest.main()
