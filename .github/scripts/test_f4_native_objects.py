#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
import f4_native_objects as f4

class NativeEvidenceTests(unittest.TestCase):
    def test_declared_command_requires_actual_object(self):
        with TemporaryDirectory() as d:
            root=Path(d)
            src=root/"app/src/main/cpp/test.cpp"
            src.parent.mkdir(parents=True)
            src.write_text("int f(){return 1;}")
            folder=root/"app/.cxx/tools/release/arm64-v8a"
            folder.mkdir(parents=True)
            obj=folder/"CMakeFiles/dsp.dir/test.cpp.o"
            entry={"directory":str(folder),"file":str(src),"command":"clang++ -c "+str(src)+" -o "+str(obj)}
            (folder/"compile_commands.json").write_text(json.dumps([entry]))
            first=f4.collect(root,"a"*40)
            self.assertEqual(first["rows"][0]["status"],"NO_OBJECT_EVIDENCE")
            obj.parent.mkdir(parents=True)
            obj.write_bytes(b"fixture-object")
            second=f4.collect(root,"a"*40)
            self.assertEqual(second["rows"][0]["status"],"OBJECT_SHA256_OBSERVED")
            self.assertFalse(second["rows"][0]["apk_link_proven"])
            self.assertEqual(len(second["rows"][0]["object_sha256"]),64)
    def test_output_outside_native_build_is_rejected(self):
        with TemporaryDirectory() as d:
            root=Path(d)
            src=root/"app/src/main/cpp/test.cpp"
            src.parent.mkdir(parents=True)
            src.write_text("source")
            f=root/"app/.cxx/tools/debug/arm64-v8a"
            f.mkdir(parents=True)
            (f/"compile_commands.json").write_text(json.dumps([{
                "directory":str(f),"file":str(src),"output":str(root/"untrusted.o")}]))
            self.assertEqual(f4.collect(root,"a"*40)["status"],"INVALID_EVIDENCE")

if __name__=="__main__":
    unittest.main()
