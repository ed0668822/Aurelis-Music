#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.3: synthetic evidence fixtures, no Android/Gradle required."""
import json
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import p2_apk_trace as p2

SHA="a"*40
HASH="b"*64
BLANK="app/src/main/java/com/music/aurelis/ui/components/LocalAppBackdrop.kt"
PLAYER="app/src/main/java/com/music/aurelis/ui/player/Player.kt"
CPP="app/src/main/cpp/ffmpeg/ffmpeg_jni.cc"

def write(path,content):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(content)+"\n")

class APKTraceTests(unittest.TestCase):
    def fixture(self,root):
        out=root/"ci-artifacts"
        for filename,content in ((BLANK,"// empty\npackage com.music.aurelis.ui.components\n"),
                                 (PLAYER,"package com.music.aurelis.ui.player\nclass Player\n"),
                                 (CPP,"int fake();\n")):
            file=root/filename
            file.parent.mkdir(parents=True,exist_ok=True)
            file.write_text(content)
        inv=[{"path":BLANK,"role":"MANAGED_SOURCE","kind":"production"},
             {"path":PLAYER,"role":"MANAGED_SOURCE","kind":"production"},
             {"path":CPP,"role":"NATIVE_SOURCE","kind":"production"}]
        meta={"source_sha":SHA,"checkout_sha":SHA}
        write(out/"source-inventory.json",inv)
        write(out/"metadata.json",meta)
        write(out/"apk-verification.json",{"status":"success","apks":[{"variant":v,"sha256":HASH} for v in p2.VARIANTS]})
        compile_rows=[{"path":x["path"],"variant":v,"native_command_declared":False,
                       "compiler_evidence":"COMPILER_TASK_SUCCEEDED_WITH_INPUT"}
                      for v in p2.VARIANTS for x in inv]
        write(out/"p2-compiler-evidence.json",{"summary":{"status":"EVIDENCE_RECORDED",
              "source_sha":SHA,"source_files":3,"variant_rows":6},"rows":compile_rows})
        for v in p2.VARIANTS:
            write(out/("apk-content-"+v+".json"),{
                "status":p2.PASS,"variant":v,"build_identity":meta,"apk_sha256":HASH,
                "required_failures":[],"evidence_errors":[],"compiler_output_errors":[],
                "dex_class_count":19,"compiler_class_count":2,"native_units":[],
                "r8":{"mapping.txt":{"sha256":HASH}} if v=="release" else {},
                "rows":[{"path":BLANK,"state":"CLASS_PRESENCE_EVIDENCE_INCOMPLETE"},
                        {"path":PLAYER,"state":"COMPILER_CLASSES_MAPPED_TO_DEX",
                         "classes":[{"state":"R8_MAPPED_CLASS_PRESENT" if v=="release" else "DEX_CLASS_PRESENT"}]},
                        {"path":CPP,"state":"NO_NATIVE_UNIT_EVIDENCE"}]})
        return out
    def test_declaration_free_and_r8_are_distinct(self):
        with TemporaryDirectory() as t:
            out=self.fixture(Path(t))
            result=p2.audit(Path(t),out,SHA)
            self.assertEqual(result["status"],"EVIDENCE_RECONCILED")
            rows=json.loads((out/"p2-apk-trace.json").read_text())["rows"]
            self.assertEqual(result["variant_rows"],6)
            self.assertEqual(next(r for r in rows if r["path"]==BLANK)["class_evidence"],"NO_STANDALONE_DECLARATION_HINT")
            self.assertEqual(next(r for r in rows if r["path"]==PLAYER and r["variant"]=="release")["class_evidence"],"R8_OR_DEX_CLASSES_OBSERVED")
            self.assertTrue(all(r["individual_compilation"]=="NOT_ESTABLISHED" for r in rows))
    def test_hash_mismatch_is_not_green(self):
        with TemporaryDirectory() as t:
            out=self.fixture(Path(t));path=out/"apk-content-release.json"
            content=json.loads(path.read_text());content["apk_sha256"]="c"*64;write(path,content)
            self.assertIn("APK_IDENTITY_OR_CONTENT_UNVERIFIED:release",p2.audit(Path(t),out,SHA)["issues"])
    def test_missing_apk_row_is_detected(self):
        with TemporaryDirectory() as t:
            out=self.fixture(Path(t));path=out/"apk-content-debug.json"
            content=json.loads(path.read_text());content["rows"].pop();write(path,content)
            self.assertTrue(any(x.startswith("APK_SOURCE_CARDINALITY:debug") for x in p2.audit(Path(t),out,SHA)["issues"]))
    def test_native_command_disagreement_is_detected(self):
        with TemporaryDirectory() as t:
            out=self.fixture(Path(t));path=out/"p2-compiler-evidence.json"
            content=json.loads(path.read_text())
            content["rows"][-1]["native_command_declared"]=True
            write(path,content)
            self.assertIn("NATIVE_COMMAND_MISMATCH:release:"+CPP,p2.audit(Path(t),out,SHA)["issues"])
    def test_compiler_report_wrong_commit_cannot_pass(self):
        with TemporaryDirectory() as t:
            out=self.fixture(Path(t));path=out/"p2-compiler-evidence.json"
            content=json.loads(path.read_text());content["summary"]["source_sha"]="c"*40;write(path,content)
            self.assertEqual(p2.audit(Path(t),out,SHA)["status"],"INCOMPLETE_EVIDENCE")

if __name__=="__main__":
    unittest.main()
