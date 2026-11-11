#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""No false UI/audio green status from smoke-only Android evidence."""
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
import b06_quality_readiness as quality

SHA="a"*40


def put(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value) + "\n")


class QualityReadinessTests(unittest.TestCase):
    def create(self, package):
        put(package/"B04/Verificacion/source-evidence.json",
            {"status":"PASS_VERIFIED_GATES","source_sha":SHA})
        put(package/"B04/Verificacion/apk-verification.json",{"status":"success"})
        put(package/"B05/Emulador/results.json",
            {"commit":SHA,"pass":True,"coverage":"INSTALL_LAUNCH_PROCESS_SMOKE_ONLY"})
        image=package/"B05/Emulador/screen.png"
        image.write_bytes(b"\x89PNG\r\n\x1a\n"+b"\x00"*100)

    def test_smoke_and_screenshot_do_not_pass_ui_or_audio(self):
        with TemporaryDirectory() as tmp:
            root=Path(tmp);self.create(root)
            ui,audio=quality.report(root,SHA)
            self.assertEqual(ui["app_surface_observation"],"SCREENSHOT_AND_SMOKE_OBSERVED")
            self.assertEqual(ui["functional_test_status"],"NOT_VERIFIED")
            self.assertEqual(audio["audio_functional_test_status"],"NOT_VERIFIED")
            self.assertEqual(audio["public_release_readiness"],"NOT_APPROVED")
            self.assertTrue(all(x=="NOT_VERIFIED" for x in ui["functional_scenarios"].values()))
            self.assertTrue(all(x=="NOT_VERIFIED" for x in audio["audio_scenarios"].values()))
            self.assertTrue((root/"P3/Resumen.md").is_file())

    def test_missing_screenshot_does_not_fabricate_ui_success(self):
        with TemporaryDirectory() as tmp:
            root=Path(tmp);self.create(root)
            (root/"B05/Emulador/screen.png").unlink()
            ui,_=quality.report(root,SHA)
            self.assertEqual(ui["app_surface_observation"],"NOT_VERIFIED")

    def test_mismatched_commit_not_smoke_pass(self):
        with TemporaryDirectory() as tmp:
            root=Path(tmp);self.create(root)
            ui,audio=quality.report(root,"b"*40)
            self.assertFalse(ui["runtime_smoke_observed"])
            self.assertFalse(audio["ci_source_identity_verified"])
            self.assertEqual(audio["public_release_readiness"],"NOT_APPROVED")

    def test_untrusted_apk_signature_does_not_pass_public_gate(self):
        with TemporaryDirectory() as tmp:
            root=Path(tmp);self.create(root)
            put(root/"B04/Verificacion/apk-verification.json",{"status":"failure"})
            _,audio=quality.report(root,SHA)
            self.assertFalse(audio["apk_integrity_verified"])
            self.assertEqual(audio["public_release_readiness"],"NOT_APPROVED")


if __name__=="__main__":
    unittest.main()
