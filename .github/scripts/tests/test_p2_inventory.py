#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
import hashlib
import json
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import android_ci as ci
import p2_inventory as p2

SHA="a"*40
FILENAME="app/src/main/java/com/music/aurelis/ui/haptics/Haptics.kt"

class InventoryTests(unittest.TestCase):
    def fixture(self,root):
        file=root/FILENAME
        file.parent.mkdir(parents=True)
        file.write_text("package com.music.aurelis.ui.haptics\n")
        report=root/"ci-artifacts"
        ci.save(report/"source-inventory.json",[{
            "path":FILENAME,**ci.file_policy(Path(FILENAME)),"file_state":"FILE",
            "sha256":hashlib.sha256(file.read_bytes()).hexdigest(),"language":"Kotlin"
        }])
        ci.save(report/"metadata.json",{"source_sha":SHA,"checkout_sha":SHA})
        (report/"build.log").write_text(
            'AURELIS_SOURCE_ROOT {"project":":app","source_set":"main","kind":"java","path":"app/src/main/java"}\n'
            'AURELIS_TASK_INPUT {"task":":app:compileReleaseKotlin","path":"'+FILENAME+'","origin":"tracked"}\n'
            'AURELIS_TASK_RESULT {"task":":app:compileReleaseKotlin","state":"SUCCESS"}\n')
        return report

    def test_valid_inventory_never_certifies_per_file_compilation(self):
        with TemporaryDirectory() as tmp:
            root=Path(tmp);report=self.fixture(root)
            with patch.object(p2,"git",return_value=FILENAME+"\0"):
                result=p2.audit(root,report,SHA)
            self.assertEqual(result["status"],"PASS_INVENTORY")
            self.assertEqual(result["tracked_files"],1)
            self.assertFalse(result["individual_compilation_proven"])
            self.assertIn("DECLARED_ROOT_OBSERVED",(report/"p2-inventory-files.tsv").read_text())
            self.assertIn("NOT_ESTABLISHED",(report/"p2-inventory-files.tsv").read_text())

    def test_hash_mismatch_is_not_hidden(self):
        with TemporaryDirectory() as tmp:
            root=Path(tmp);report=self.fixture(root)
            (root/FILENAME).write_text("changed")
            with patch.object(p2,"git",return_value=FILENAME+"\0"):
                result=p2.audit(root,report,SHA)
            self.assertEqual(result["status"],"INCOMPLETE_EVIDENCE")
            self.assertTrue(any(x.startswith("FILE_HASH_OR_TYPE_MISMATCH") for x in result["issues"]))

    def test_missing_gradle_log_is_unknown(self):
        with TemporaryDirectory() as tmp:
            root=Path(tmp);report=self.fixture(root)
            (report/"build.log").unlink()
            with patch.object(p2,"git",return_value=FILENAME+"\0"):
                result=p2.audit(root,report,SHA)
            self.assertIn("MISSING_BUILD_LOG",result["issues"])
            self.assertIn("NOT_OBSERVED",(report/"p2-inventory-files.tsv").read_text())

if __name__=="__main__":
    unittest.main()
