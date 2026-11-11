#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.6/P3 JUnit provenance must fail closed."""
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
import xml.etree.ElementTree as ET
import b06_junit_attestation as mod
SHA = "a" * 40

def setup(package):
    p = package / "B03/Resumen"
    p.mkdir(parents=True)
    (p / "summary.md").write_text("- Commit: " + chr(96) + SHA + chr(96) + "\n")
    (p / "results.json").write_text(json.dumps({
        v: {"passed_gate": True, "outcome": "success", "tests": 273,
            "failed": 0, "errors": 0, "skipped": 0}
        for v in ("debug", "release")}))
    required = {}
    for clazz, case in list(mod.UI_CASES.values()) + list(mod.CONTROLLER_CASES.values()):
        required.setdefault(clazz, set()).add(case)
    for v in ("debug", "release"):
        for clazz, cases in required.items():
            suite = ET.Element("testsuite", name=clazz, tests=str(len(cases)),
                               failures="0", errors="0", skipped="0")
            for case in cases:
                ET.SubElement(suite, "testcase", classname=clazz, name=case)
            target = package / "B03" / v.title() / "junit" / v / ("TEST-" + clazz + ".xml")
            target.parent.mkdir(parents=True)
            ET.ElementTree(suite).write(target, encoding="utf-8", xml_declaration=True)

class Tests(unittest.TestCase):
    def test_debug_and_release_real_cases_verified(self):
        with TemporaryDirectory() as d:
            root = Path(d); setup(root)
            for cases in (mod.UI_CASES, mod.CONTROLLER_CASES):
                result = mod.attest_cases(root, SHA, cases)
                self.assertTrue(result["b03_tests_verified"])
                self.assertTrue(all(state == "ROBOLECTRIC_JUNIT_PASS" for state in result["states"].values()))
                self.assertTrue(all(x["variants"]["release"]["sha256"]
                                    for x in result["cases"].values()))

    def test_missing_release_xml_fails_only_affected_case(self):
        with TemporaryDirectory() as d:
            root = Path(d); setup(root)
            clazz, _ = mod.UI_CASES["home_navigation"]
            (root / "B03/Release/junit/release" / ("TEST-" + clazz + ".xml")).unlink()
            result = mod.attest_cases(root, SHA, mod.UI_CASES)
            self.assertEqual(result["states"]["home_navigation"], "NOT_VERIFIED")
            self.assertEqual(result["states"]["artists_albums"], "ROBOLECTRIC_JUNIT_PASS")

    def test_wrong_commit_rejects_all(self):
        with TemporaryDirectory() as d:
            root = Path(d); setup(root)
            result = mod.attest_cases(root, "b" * 40, mod.UI_CASES)
            self.assertFalse(result["b03_tests_verified"])
            self.assertTrue(all(x == "NOT_VERIFIED" for x in result["states"].values()))

    def test_failed_or_skipped_case_never_counts_as_pass(self):
        for element in ("failure", "skipped"):
            with self.subTest(element=element), TemporaryDirectory() as d:
                root = Path(d); setup(root)
                clazz, case = mod.CONTROLLER_CASES["play_pause_resume"]
                path = root / "B03/Debug/junit/debug" / ("TEST-" + clazz + ".xml")
                tree = ET.parse(path)
                found = next(x for x in tree.iter("testcase") if x.get("name") == case)
                ET.SubElement(found, element)
                tree.write(path, encoding="utf-8")
                result = mod.attest_cases(root, SHA, mod.CONTROLLER_CASES)
                self.assertEqual(result["states"]["play_pause_resume"], "NOT_VERIFIED")

    def test_missing_results_or_invalid_xml_never_pass(self):
        with TemporaryDirectory() as d:
            root = Path(d); setup(root)
            (root / "B03/Resumen/results.json").unlink()
            result = mod.attest_cases(root, SHA, mod.CONTROLLER_CASES)
            self.assertFalse(result["b03_tests_verified"])
            self.assertTrue(all(x == "NOT_VERIFIED" for x in result["states"].values()))
        with TemporaryDirectory() as d:
            root = Path(d); setup(root)
            clazz, _ = mod.UI_CASES["local_library"]
            (root / "B03/Debug/junit/debug" / ("TEST-" + clazz + ".xml")).write_text("<invalid")
            result = mod.attest_cases(root, SHA, mod.UI_CASES)
            self.assertEqual(result["states"]["local_library"], "NOT_VERIFIED")

if __name__ == "__main__":
    unittest.main()
