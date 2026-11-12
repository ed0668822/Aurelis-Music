#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Ensure the standalone VirusTotal report never turns missing evidence into a clean scan."""
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
import b06_bundle_reports as mod


class VirusTotalDeliveryTests(unittest.TestCase):
    def test_missing_evidence_never_cleared(self):
        with TemporaryDirectory() as dirname:
            with patch.object(mod, "OUTPUT", Path(dirname) / "input"):
                report = mod.create_virustotal_delivery(
                    True, {"apks": [{"variant": "release", "sha256": "a" * 64}]},
                    "b" * 40, Path(dirname) / "report")
            self.assertEqual(report["virus_total_status"], "REPORT_UNVERIFIED")
            self.assertEqual(report["antivirus_review"], "NOT_CLEARED")

    def test_hash_mismatch_never_cleared(self):
        with TemporaryDirectory() as dirname:
            path = Path(dirname) / "B04" / "Verificacion"
            path.mkdir(parents=True)
            (path / "virustotal.json").write_text(json.dumps({
                "status": "NO_DETECTIONS_REPORTED", "sha256": "c" * 64,
                "source_sha": "b" * 40, "analysis_updated_at": "2026-11-08T00:00:00+00:00"}))
            with patch.object(mod, "OUTPUT", Path(dirname)):
                report = mod.create_virustotal_delivery(
                    True, {"apks": [{"variant": "release", "sha256": "a" * 64}]},
                    "b" * 40, Path(dirname) / "out")
            self.assertEqual(report["virus_total_status"], "REPORT_UNVERIFIED")
            self.assertEqual(report["antivirus_review"], "NOT_CLEARED")

    def test_hash_lookup_only_is_never_cleared(self):
        with TemporaryDirectory() as dirname:
            path = Path(dirname) / "B04" / "Verificacion"
            path.mkdir(parents=True)
            (path / "virustotal.json").write_text(json.dumps({
                "status": "HASH_LOOKUP_ONLY", "error_code": "UPLOAD_DISABLED_BY_POLICY",
                "sha256": "a" * 64, "source_sha": "b" * 40,
                "analysis_updated_at": "2026-11-08T00:00:00+00:00"}))
            with patch.object(mod, "OUTPUT", Path(dirname)):
                report = mod.create_virustotal_delivery(
                    True, {"apks": [{"variant": "release", "sha256": "a" * 64}]},
                    "b" * 40, Path(dirname) / "out")
            self.assertEqual(report["virus_total_status"], "HASH_LOOKUP_ONLY")
            self.assertEqual(report["antivirus_review"], "NOT_CLEARED")

    def test_error_status_is_not_passed(self):
        with TemporaryDirectory() as dirname:
            path = Path(dirname) / "B04" / "Verificacion"
            path.mkdir(parents=True)
            (path / "virustotal.json").write_text(json.dumps({
                "status": "ERROR", "error_code": "VT_HTTP_429",
                "sha256": "a" * 64, "source_sha": "b" * 40}))
            with patch.object(mod, "OUTPUT", Path(dirname)):
                report = mod.create_virustotal_delivery(
                    True, {"apks": [{"variant": "release", "sha256": "a" * 64}]},
                    "b" * 40, Path(dirname) / "out")
            self.assertEqual(report["virus_total_status"], "ERROR")
            self.assertEqual(report["antivirus_review"], "NOT_CLEARED")


if __name__ == "__main__":
    unittest.main()
