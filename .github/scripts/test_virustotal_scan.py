#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Offline regression tests: API credentials are never required or used."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import virustotal_scan as vt


class VirusTotalAuditTests(unittest.TestCase):
    def test_no_key_never_claims_clean(self):
        with tempfile.TemporaryDirectory() as base:
            file = Path(base) / "app-release.apk"
            file.write_bytes(b"sample")
            with patch.object(vt, "get_api", side_effect=AssertionError("network forbidden")):
                report = vt.report_for(file, hashlib.sha256(b"sample").hexdigest(), "")
            self.assertEqual(report["status"], "NOT_CONFIGURED")
            self.assertEqual(report["last_analysis_stats"], {})

    def test_zero_detections_not_from_missing_stats(self):
        self.assertEqual(vt.classification({}), "PENDING")
        self.assertEqual(vt.classification({"malicious": 0, "suspicious": 0, "undetected": 0}), "PENDING")

    def test_positive_detection_needs_review(self):
        self.assertEqual(vt.classification({"malicious": 1, "undetected": 50}), "REVIEW_REQUIRED")
        self.assertEqual(vt.classification({"suspicious": 2, "harmless": 5}), "REVIEW_REQUIRED")

    def test_negative_or_malformed_counts_never_clean(self):
        self.assertEqual(vt.classification({"malicious": -1, "undetected": 10}), "ERROR")
        self.assertEqual(vt.classification({"malicious": "0", "undetected": 10}), "ERROR")

    def test_zero_reported_detections_requires_some_engines(self):
        self.assertEqual(vt.classification({"malicious": 0, "suspicious": 0, "undetected": 70}), "NO_DETECTIONS_REPORTED")

    def test_existing_report_is_explicitly_labelled_and_dated(self):
        with tempfile.TemporaryDirectory() as base:
            file = Path(base) / "app-release.apk"
            file.write_bytes(b"sample")
            digest = hashlib.sha256(b"sample").hexdigest()
            sample = {"data": {"attributes": {"sha256": digest, "last_analysis_date": 1609459200,
                                              "last_analysis_stats": {"malicious": 0, "undetected": 50}}}}
            with patch.object(vt, "get_api", return_value=sample), patch.object(vt, "upload", side_effect=AssertionError("should reuse hash")):
                result = vt.report_for(file, digest, "dummy-local-key")
            self.assertEqual(result["report_origin"], "EXISTING_HASH_REPORT")
            self.assertEqual(result["status"], "NO_DETECTIONS_REPORTED")
            self.assertTrue(result["analysis_updated_at"].startswith("2021-01-01"))

    def test_invalid_verified_release_fails_closed(self):
        with tempfile.TemporaryDirectory() as base:
            root = Path(base)
            (root / "ci-approved-apks").mkdir()
            (root / "ci-artifacts").mkdir()
            (root / "ci-approved-apks/apk-manifest.json").write_text("[]")
            (root / "ci-artifacts/apk-verification.json").write_text('{"status":"success"}')
            (root / "ci-artifacts/signing.json").write_text('{"mode":"configured_release"}')
            with self.assertRaises(ValueError):
                vt.verified_release(root)


if __name__ == "__main__":
    unittest.main()
