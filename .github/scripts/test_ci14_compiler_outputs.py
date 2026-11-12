#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""F3 compiler handoff regression tests; no Gradle or network used."""
import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

import ci14_compiler_outputs as comp

ENV = {
    "GITHUB_SHA": "d" * 40,
    "GITHUB_RUN_ID": "123",
    "GITHUB_RUN_NUMBER": "42",
    "GITHUB_RUN_ATTEMPT": "1",
}


class CompilerHandoffTest(unittest.TestCase):
    def setUp(self):
        self.directory = TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        path = self.root / comp.ALLOWED[0] / "com/music/aurelis/Test.class"
        path.parent.mkdir(parents=True)
        path.write_bytes(b"class fixture; not production bytecode")
        log = self.root / "ci-b01/gradle-build.log"
        log.parent.mkdir(parents=True)
        log.write_text('AURELIS_CLASS_OUTPUT ' + json.dumps({
            "task": ":app:compileDebugKotlin",
            "path": comp.ALLOWED[0],
        }) + "\n")

    def test_pack_verify_restore(self):
        with patch.dict(os.environ, ENV):
            comp.pack(self.root)
            comp.verify(self.root)
            classes = self.root / comp.ALLOWED[0]
            import shutil
            shutil.rmtree(classes)
            result = comp.restore(self.root)
            self.assertEqual(result["class_files"], 1)
            self.assertTrue((classes / "com/music/aurelis/Test.class").is_file())

    def test_hash_tamper_is_rejected(self):
        with patch.dict(os.environ, ENV):
            comp.pack(self.root)
            with (self.root / comp.OUT / comp.ARCHIVE).open("ab") as file:
                file.write(b"tamper")
            with self.assertRaisesRegex(ValueError, "SHA-256"):
                comp.verify(self.root)

    def test_wrong_commit_or_run_is_rejected(self):
        with patch.dict(os.environ, ENV):
            comp.pack(self.root)
            with patch.dict(os.environ, {"GITHUB_RUN_ID": "124"}):
                with self.assertRaisesRegex(ValueError, "provenance"):
                    comp.verify(self.root)

    def test_invalid_log_path_is_rejected(self):
        log = self.root / "ci-b01/gradle-build.log"
        log.write_text('AURELIS_CLASS_OUTPUT ' + json.dumps({
            "task": ":app:compileDebugKotlin", "path": "../../sneaky",
        }) + "\n")
        with patch.dict(os.environ, ENV):
            with self.assertRaisesRegex(ValueError, "Unapproved"):
                comp.pack(self.root)

    def test_missing_classes_is_rejected(self):
        (self.root / comp.ALLOWED[0] / "com/music/aurelis/Test.class").unlink()
        with patch.dict(os.environ, ENV):
            with self.assertRaisesRegex(ValueError, "zero class"):
                comp.pack(self.root)


if __name__ == "__main__":
    unittest.main()
