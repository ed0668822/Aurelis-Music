#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
import json
import unittest
import f4_debug_inputs as x

SOURCES = [{"path":"app/src/main/java/demo/Main.kt","module":"app","role":"MANAGED_SOURCE",
            "source_set":"main","kind":"source","language":"Kotlin"}]
GOOD_LOG = "\n".join([
 "AURELIS_TASK_INPUT " + json.dumps({"task": ":app:compileDebugKotlin", "path": SOURCES[0]["path"],"origin":"tracked-directory-input"}),
 "AURELIS_TASK_RESULT " + json.dumps({"task": ":app:compileDebugKotlin", "state":"SUCCESS"}),
])

class DebugTaskInputTests(unittest.TestCase):
    def test_verified_tracked_debug_source(self):
        self.assertEqual(x.audit(SOURCES, GOOD_LOG, "abc")["status"], "PASS")
    def test_generated_only_is_not_source_proof(self):
        data = GOOD_LOG.replace("tracked-directory-input","generated")
        r=x.audit(SOURCES, data,"abc")
        self.assertEqual(r["status"],"FAIL")
        self.assertEqual(r["missing"],[SOURCES[0]["path"]])
    def test_missing_or_failed_task_does_not_pass(self):
        self.assertEqual(x.audit(SOURCES, "", "abc")["status"], "FAIL")
        self.assertEqual(x.audit(SOURCES, GOOD_LOG.replace('"SUCCESS"','"FAILED"'),"abc")["status"], "FAIL")
    def test_contradictory_result_fails(self):
        extra="AURELIS_TASK_RESULT "+json.dumps({"task":":app:compileDebugKotlin","state":"FAILED"})
        self.assertEqual(x.audit(SOURCES,GOOD_LOG+"\n"+extra,"abc")["status"],"FAIL")

if __name__ == "__main__":
    unittest.main()
