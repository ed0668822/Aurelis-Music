#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
import unittest
import f5_coverage_gate as gate

class CoverageGateTests(unittest.TestCase):
    def test_measured_pass(self):
        self.assertTrue(gate.check({"status":"MEASURED","coverage_percent":88.86,"executable_lines":3969,"threshold_met":True})[0])
    def test_unmeasured_cannot_pass(self):
        for v in ["NOT_MEASURED","COVERAGE_XML_INVALID","NO_EXECUTABLE_LINES"]:
            self.assertFalse(gate.check({"status":v,"changed_files":2})[0])
    def test_no_production_source_changed(self):
        self.assertTrue(gate.check({"status":"NO_EXECUTABLE_LINES","changed_files":0})[0])
    def test_below_limit_or_inconsistent_data_fails(self):
        self.assertFalse(gate.check({"status":"MEASURED","coverage_percent":79.99,"executable_lines":10,"threshold_met":False})[0])
        self.assertFalse(gate.check({"status":"MEASURED","coverage_percent":99,"executable_lines":0,"threshold_met":True})[0])
        self.assertFalse(gate.check({"status":"MEASURED","coverage_percent":"100","executable_lines":10,"threshold_met":True})[0])

if __name__=="__main__":
    unittest.main()
