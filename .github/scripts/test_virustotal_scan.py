#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Offline regression tests: API credentials are never required or used."""
import hashlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import virustotal_scan as vt


class VirusTotalAuditTests(unittest.TestCase):
    def test_b04_workflow_cannot_enable_public_upload_by_repo_variable(self):
        workflow = Path(__file__).resolve().parent.parent / "workflows/b04-verify-apks.yml"
        value = workflow.read_text(encoding="utf-8")
        self.assertIn('VIRUSTOTAL_ALLOW_UPLOAD: "false"', value)
        self.assertNotIn("VIRUSTOTAL_ALLOW_UPLOAD: ${{ vars.VIRUSTOTAL_ALLOW_UPLOAD }}", value)
        self.assertIn("if: ${{ false }}", value)


    def test_no_key_never_claims_clean(self):
        with tempfile.TemporaryDirectory() as base:
            file = Path(base) / "app-release.apk"
            file.write_bytes(b"sample")
            with patch.object(vt, "get_api", side_effect=AssertionError("network forbidden")):
                report = vt.report_for(file, hashlib.sha256(b"sample").hexdigest(), "")
            self.assertEqual(report["status"], "NOT_CONFIGURED")
            self.assertEqual(report["last_analysis_stats"], {})

    def test_upload_disabled_by_default_without_existing_report(self):
        from urllib.error import HTTPError
        with tempfile.TemporaryDirectory() as base:
            file = Path(base) / "app-release.apk"
            file.write_bytes(b"sample")
            digest = hashlib.sha256(b"sample").hexdigest()
            not_found = HTTPError("https://example.invalid", 404, "Not Found", None, None)
            with patch.object(vt, "get_api", side_effect=not_found), \
                 patch.object(vt, "upload", side_effect=AssertionError("public upload must not happen")):
                report = vt.report_for(file, digest, "dummy-local-key")
            self.assertEqual(report["status"], "HASH_LOOKUP_ONLY")
            self.assertEqual(report["error_code"], "UPLOAD_DISABLED_BY_POLICY")
            self.assertIsNone(report["report_origin"])
            self.assertIn("HASH_LOOKUP_ONLY", vt.VALID_STATES)

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
    def test_large_file_uses_one_time_endpoint(self):
        with tempfile.TemporaryDirectory() as dirname:
            path = Path(dirname) / "big-release.apk"
            path.write_bytes(b"apk")
            with patch.object(vt, "MAX_DIRECT_UPLOAD", 2), \
                 patch.object(vt, "get_api", return_value={"data": "http://www.virustotal.com/_ah/upload/one-time/"}) as get_api, \
                 patch.object(vt, "build_opener") as opener_factory:
                opener_factory.return_value.open.return_value.__enter__.return_value = io.BytesIO(b'{"data":{"id":"analysis-123"}}')
                analysis = vt.upload(path, "fake-test-key")
            self.assertEqual(analysis, "analysis-123")
            get_api.assert_called_once_with("/files/upload_url", "fake-test-key")
            req = opener_factory.return_value.open.call_args.args[0]
            self.assertEqual(req.full_url, "https://www.virustotal.com/_ah/upload/one-time/")
            self.assertIn(b"apk", req.data)

    def test_real_bigfiles_host_is_accepted_over_https(self):
        url = "https://bigfiles.virustotal.com/_ah/upload/one-use-token/?opaque=123"
        self.assertEqual(vt.large_upload_url({"data": url}), url)

    def test_bigfiles_http_is_not_allowed(self):
        with self.assertRaises(ValueError):
            vt.large_upload_url({"data": "http://bigfiles.virustotal.com/_ah/upload/token"})

    def test_upload_redirects_are_denied(self):
        self.assertIsNone(vt._NoUploadRedirect().redirect_request(
            None, None, 302, "Found", {}, "https://evil.example/upload"))

    def test_bigfiles_file_upload_uses_only_approved_host(self):
        with tempfile.TemporaryDirectory() as dirname:
            file = Path(dirname) / "big.apk"
            file.write_bytes(b"big-file-test")
            url = "https://bigfiles.virustotal.com/_ah/upload/one-time/"
            with patch.object(vt, "MAX_DIRECT_UPLOAD", 2), \
                 patch.object(vt, "get_api", return_value={"data": url}), \
                 patch.object(vt, "build_opener") as opener_factory:
                opener_factory.return_value.open.return_value.__enter__.return_value = io.BytesIO(b'{"data":{"id":"big-analysis"}}')
                result = vt.upload(file, "fake-key")
            self.assertEqual(result, "big-analysis")
            req = opener_factory.return_value.open.call_args.args[0]
            self.assertEqual(req.full_url, url)
            self.assertEqual(req.get_header("X-apikey"), "fake-key")

    def test_untrusted_upload_urls_are_rejected(self):
        for url in ("https://evil.example/_ah/upload/x",
                    "https://www.virustotal.com.evil.example/_ah/upload/x",
                    "file:///tmp/escape", "https://user:pass@www.virustotal.com/_ah/upload/x",
                    "https://bigfiles.virustotal.com.evil.example/_ah/upload/x",
                    "https://other.virustotal.com/_ah/upload/x",
                    "https://bigfiles.virustotal.com/api/v3/files/upload_url",
                    "https://bigfiles.virustotal.com:444/_ah/upload/x",
                    "https://bigfiles.virustotal.com:invalid/_ah/upload/x"):
            with self.subTest(url=url), self.assertRaises(ValueError):
                vt.large_upload_url({"data": url})

    def test_oversize_rejected_without_network(self):
        with tempfile.TemporaryDirectory() as dirname:
            path = Path(dirname) / "too-big.apk"
            path.write_bytes(b"12345678")
            with patch.object(vt, "MAX_LARGE_UPLOAD", 7), \
                 patch.object(vt, "get_api", side_effect=AssertionError("network is forbidden")):
                with self.assertRaisesRegex(ValueError, "FILE_TOO_LARGE"):
                    vt.upload(path, "fake-test-key")


if __name__ == "__main__":
    unittest.main()
