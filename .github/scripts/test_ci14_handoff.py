#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Offline F3 handoff regression tests: source/provenance, ABI, tamper and clean restore."""
import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
import zipfile

import ci14_handoff as h

ENV = {
    "GITHUB_SHA": "a" * 40, "GITHUB_RUN_ID": "900",
    "GITHUB_RUN_ATTEMPT": "1", "GITHUB_RUN_NUMBER": "77",
}


def apk(path: Path, abi="arm64-v8a"):
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w") as output:
        output.writestr("AndroidManifest.xml", b"manifest")
        if abi:
            output.writestr(f"lib/{abi}/libonnxruntime.so", b"arm64 native fixture")
        output.writestr("classes.dex", b"dex test data")


class HandoffTests(unittest.TestCase):
    def setUp(self):
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        for variant, (_, source) in h.FILES.items():
            apk(self.root / source, None if variant == "instrumentation" else "arm64-v8a")

    def test_verified_pack_and_restore(self):
        with patch.dict(os.environ, ENV):
            payload = h.pack(self.root)
            self.assertEqual(payload["abi"], "arm64-v8a")
            for _, (_, source) in h.FILES.items():
                (self.root / source).unlink()
            h.restore(self.root, "all")
            self.assertTrue(all((self.root / source).is_file() for _, source in h.FILES.values()))

    def test_wrong_source_or_run_rejected(self):
        with patch.dict(os.environ, ENV):
            h.pack(self.root)
            with patch.dict(os.environ, {"GITHUB_SHA": "b" * 40}):
                with self.assertRaisesRegex(ValueError, "Wrong source"):
                    h.verify(self.root)
            with patch.dict(os.environ, {"GITHUB_RUN_ID": "901"}):
                with self.assertRaisesRegex(ValueError, "Wrong source"):
                    h.verify(self.root)

    def test_tampered_apk_rejected(self):
        with patch.dict(os.environ, ENV):
            h.pack(self.root)
            with (self.root / h.HANDOFF / h.FILES["debug"][0]).open("ab") as output:
                output.write(b"tamper")
            with self.assertRaisesRegex(ValueError, "SHA-256"):
                h.verify(self.root)

    def test_extra_files_fail_closed(self):
        with patch.dict(os.environ, ENV):
            h.pack(self.root)
            (self.root / h.HANDOFF / "untrusted.apk").write_text("unexpected")
            with self.assertRaisesRegex(ValueError, "Unknown"):
                h.verify(self.root)

    def test_missing_instrumentation_refused(self):
        with patch.dict(os.environ, ENV):
            h.pack(self.root)
            (self.root / h.HANDOFF / h.FILES["instrumentation"][0]).unlink()
            with self.assertRaises(ValueError):
                h.verify(self.root)

    def test_wrong_abi_rejected(self):
        with patch.dict(os.environ, ENV):
            apk(self.root / h.FILES["debug"][1], "x86_64")
            with self.assertRaisesRegex(ValueError, "no ARM64"):
                h.pack(self.root)

    def test_no_provenance_is_not_accepted(self):
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaisesRegex(ValueError, "provenance"):
                h.pack(self.root)


if __name__ == "__main__":
    unittest.main()
