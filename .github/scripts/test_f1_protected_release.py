#!/usr/bin/env python3
"""Offline negative tests for protected Release guard."""
import unittest
from unittest.mock import patch
import f1_protected_release as mod

GOOD = {
    "GITHUB_REPOSITORY": mod.REPO,
    "GITHUB_EVENT_NAME": "workflow_dispatch",
    "GITHUB_REF": "refs/heads/main",
    "RELEASE_CONFIRMATION": "PUBLICAR",
    "RELEASE_PROTECTION_CONFIRMED": "true",
    "GITHUB_SHA": "a" * 40,
    "RELEASE_SIGNER_SHA256": "b" * 64,
    "GITHUB_TOKEN": "test",
}

class ProtectedReleaseTests(unittest.TestCase):
    def test_explicit_allow_policy(self):
        mod.validate_environment(GOOD)
        self.assertTrue(mod.is_release_ref("refs/tags/v1.2.0"))
        self.assertFalse(mod.is_release_ref("refs/heads/feature/bitchord-local-migration"))
        for field, bad in (
            ("GITHUB_REPOSITORY", "other/repo"),
            ("GITHUB_EVENT_NAME", "push"),
            ("GITHUB_REF", "refs/heads/feature/bitchord-local-migration"),
            ("RELEASE_CONFIRMATION", "NO"),
            ("RELEASE_PROTECTION_CONFIRMED", ""),
            ("GITHUB_SHA", "bad"),
            ("RELEASE_SIGNER_SHA256", ""),
            ("GITHUB_TOKEN", ""),
        ):
            with self.subTest(field=field), self.assertRaises(ValueError):
                mod.validate_environment({**GOOD, field: bad})

    def test_unsigned_or_missing_source_cannot_be_staged(self):
        from pathlib import Path
        from tempfile import TemporaryDirectory
        import json
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "ci-artifacts").mkdir()
            (root / "ci-approved-apks").mkdir()
            (root / "ci-artifacts/metadata.json").write_text(json.dumps({
                "source_sha": GOOD["GITHUB_SHA"], "checkout_sha": GOOD["GITHUB_SHA"]}))
            (root / "ci-artifacts/signing.json").write_text(json.dumps({
                "mode": "debug_fallback", "configured_certificate_sha256": "b" * 64}))
            (root / "ci-artifacts/apk-verification.json").write_text('{"status":"success"}')
            (root / "ci-approved-apks/apk-manifest.json").write_text("[]")
            with self.assertRaisesRegex(ValueError, "certificate"):
                mod.verify_and_stage(root, "b" * 64, GOOD["GITHUB_SHA"])
            self.assertFalse((root / "ci-protected-release").exists())

    def test_virus_total_missing_or_review_requires_block(self):
        from pathlib import Path
        from tempfile import TemporaryDirectory
        import json
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "ci-artifacts").mkdir()
            (root / "ci-protected-release").mkdir()
            (root / "ci-protected-release/release-evidence.json").write_text(json.dumps({
                "release_sha256": "a"*64}))
            for status in ["NOT_CONFIGURED", "HASH_LOOKUP_ONLY", "REVIEW_REQUIRED", "ERROR"]:
                (root / "ci-artifacts/virustotal.json").write_text(json.dumps({
                    "status":status,"sha256":"a"*64,"report_origin":"EXISTING_HASH_REPORT"}))
                with self.assertRaises(ValueError):
                    mod.validate_virustotal(root)

if __name__ == "__main__":
    unittest.main()
