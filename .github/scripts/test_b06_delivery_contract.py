#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.5: three-artifact contract, VirusTotal-in-ZIP and cleanup guardrails."""
import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

import b06_bundle_reports as bundle
import b06_cleanup_artifacts as clean

COMMIT = "a" * 40
RELEASE = "b" * 64

class DeliveryTests(unittest.TestCase):
    def test_default_virustotal_is_in_report_tree_and_never_claims_missing_scan_clean(self):
        with TemporaryDirectory() as tmp:
            output = Path(tmp) / "b06-package"
            with patch.object(bundle, "OUTPUT", output):
                report = bundle.create_virustotal_delivery(
                    True, {"apks": [{"variant": "release", "sha256": RELEASE}]}, COMMIT)
            self.assertEqual(report["virus_total_status"], "REPORT_UNVERIFIED")
            self.assertEqual(report["antivirus_review"], "NOT_CLEARED")
            self.assertTrue((output / "VirusTotal/virustotal.json").is_file())
            self.assertTrue((output / "VirusTotal/virustotal.md").is_file())
            self.assertFalse((Path(tmp) / "b06-virustotal").exists())

    def test_error_source_is_preserved_inside_vt_without_cleaning(self):
        with TemporaryDirectory() as tmp:
            output = Path(tmp)
            original = output / "B04/Verificacion/virustotal.json"
            original.parent.mkdir(parents=True)
            original.write_text(json.dumps({
                "source_sha": COMMIT, "sha256": RELEASE,
                "status": "ERROR", "error_code": "VT_HTTP_429",
            }))
            with patch.object(bundle, "OUTPUT", output):
                report = bundle.create_virustotal_delivery(
                    True, {"apks": [{"variant": "release", "sha256": RELEASE}]}, COMMIT)
            self.assertEqual(report["virus_total_status"], "ERROR")
            self.assertEqual(report["error_code"], "VT_HTTP_429")
            self.assertFalse(report["antivirus_review"] == "NO_DETECTIONS_REPORTED_RECENT")
            self.assertTrue(original.is_file())

    def fake_env(self, tmp):
        result = Path(tmp) / "b06-package/B06/pipeline-results.json"
        result.parent.mkdir(parents=True)
        result.write_text(json.dumps({"status": "PASS"}))
        return {
            "GITHUB_API_URL": "https://api.github.com",
            "GITHUB_REPOSITORY": "sample/repo",
            "GITHUB_RUN_ID": "111",
            "GITHUB_RUN_NUMBER": "23",
            "GITHUB_TOKEN": "test-never-used",
            "GITHUB_STEP_SUMMARY": str(Path(tmp) / "summary.md"),
        }

    @staticmethod
    def artifacts_for_run():
        names = [
            "Aurelis-Informes-B01-B06-23",
            "Aurelis-Pruebas-ARM64-23",
            "Aurelis-Release-ARM64-23",
            "b01-report-23", "b01-shared-apks-23", "b01-compiler-outputs-23",
            "b02-lint-report-23", "b03-debug-tests-23",
            "b03-release-tests-23", "b03-summary-23",
            "b04-verification-report-23", "b05-android-emulator-23",
        ]
        return [{"id": i + 1, "name": x, "expired": False} for i, x in enumerate(names)]

    def test_cleanup_leaves_exactly_three_final_names(self):
        with TemporaryDirectory() as tmp:
            records = self.artifacts_for_run()
            deletes = []
            def fake_api(method, resource):
                nonlocal records
                if method == "DELETE":
                    id_ = int(resource.rsplit("/", 1)[-1])
                    deletes.append(id_)
                    records = [x for x in records if x["id"] != id_]
                return None
            with patch.dict(os.environ, self.fake_env(tmp)), \
                 patch.object(clean, "artifacts", side_effect=lambda: list(records)), \
                 patch.object(clean, "api", side_effect=fake_api):
                old = os.getcwd()
                try:
                    os.chdir(tmp)
                    self.assertEqual(clean.main(), 0)
                finally:
                    os.chdir(old)
            self.assertEqual(len(deletes), 9)
            self.assertEqual({x["name"] for x in records}, {
                "Aurelis-Informes-B01-B06-23", "Aurelis-Pruebas-ARM64-23",
                "Aurelis-Release-ARM64-23",
            })

    def test_feature_branch_keeps_two_final_artifacts(self):
        with TemporaryDirectory() as tmp:
            records = [r for r in self.artifacts_for_run()
                       if r["name"] != "Aurelis-Release-ARM64-23"]
            deletes = []
            def fake_api(method, resource):
                nonlocal records
                if method == "DELETE":
                    ident = int(resource.rsplit("/", 1)[-1])
                    deletes.append(ident)
                    records = [r for r in records if r["id"] != ident]
                return None
            env = {**self.fake_env(tmp), "RELEASE_PUBLISH_EXPECTED": "false"}
            with patch.dict(os.environ, env), \
                 patch.object(clean, "artifacts", side_effect=lambda: list(records)), \
                 patch.object(clean, "api", side_effect=fake_api):
                old = os.getcwd()
                try:
                    os.chdir(tmp)
                    self.assertEqual(clean.main(), 0)
                finally:
                    os.chdir(old)
            self.assertEqual(len(deletes), 9)
            self.assertEqual({r["name"] for r in records}, {
                "Aurelis-Informes-B01-B06-23", "Aurelis-Pruebas-ARM64-23",
            })

    def test_release_policy_strict_by_default_and_invalid_values_rejected(self):
        with patch.dict(os.environ, {}, clear=True):
            self.assertTrue(clean.release_expected())
        with patch.dict(os.environ, {"RELEASE_PUBLISH_EXPECTED": "false"}, clear=True):
            self.assertFalse(clean.release_expected())
        with patch.dict(os.environ, {"RELEASE_PUBLISH_EXPECTED": "unexpected"}, clear=True):
            with self.assertRaisesRegex(RuntimeError, "RELEASE_PUBLISH_EXPECTED"):
                clean.release_expected()

    def test_feature_branch_refuses_unexpected_release_without_deleting(self):
        with TemporaryDirectory() as tmp:
            env = {**self.fake_env(tmp), "RELEASE_PUBLISH_EXPECTED": "false"}
            deletes = []
            with patch.dict(os.environ, env), \
                 patch.object(clean, "artifacts", return_value=self.artifacts_for_run()), \
                 patch.object(clean, "api", side_effect=lambda *args: deletes.append(args)):
                old = os.getcwd()
                try:
                    os.chdir(tmp)
                    with self.assertRaisesRegex(RuntimeError, "Unexpected artifacts"):
                        clean.main()
                finally:
                    os.chdir(old)
            self.assertEqual(deletes, [])

    def test_cleanup_refuses_missing_final_or_unrecognized_artifact_without_deleting(self):
        for scenario in ("fourth-VT", "missing-debug", "missing-release", "missing-shared", "missing-compiler"):
            with self.subTest(scenario=scenario), TemporaryDirectory() as tmp:
                records = self.artifacts_for_run()
                if scenario == "fourth-VT":
                    records.append({"id": 100, "name": "Aurelis-VirusTotal-23", "expired": False})
                elif scenario == "missing-debug":
                    records = [r for r in records if r["name"] != "Aurelis-Pruebas-ARM64-23"]
                elif scenario == "missing-release":
                    records = [r for r in records if r["name"] != "Aurelis-Release-ARM64-23"]
                elif scenario == "missing-shared":
                    records = [r for r in records if r["name"] != "b01-shared-apks-23"]
                else:
                    records = [r for r in records if r["name"] != "b01-compiler-outputs-23"]
                deletions = []
                with patch.dict(os.environ, self.fake_env(tmp)), \
                     patch.object(clean, "artifacts", return_value=records), \
                     patch.object(clean, "api", side_effect=lambda *args: deletions.append(args)), \
                     patch.object(clean.time, "sleep", return_value=None):
                    old = os.getcwd()
                    try:
                        os.chdir(tmp)
                        with self.assertRaises(RuntimeError):
                            clean.main()
                    finally:
                        os.chdir(old)
                self.assertEqual(deletions, [])

    def test_failed_b06_never_deletes_temporary_evidence(self):
        with TemporaryDirectory() as tmp:
            env = self.fake_env(tmp)
            result = Path(tmp) / "b06-package/B06/pipeline-results.json"
            result.write_text('{"status":"FAIL"}')
            with patch.dict(os.environ, env), patch.object(clean, "api") as api:
                old = os.getcwd()
                try:
                    os.chdir(tmp)
                    with self.assertRaisesRegex(RuntimeError, "not passed"):
                        clean.main()
                finally:
                    os.chdir(old)
                api.assert_not_called()


if __name__ == "__main__":
    unittest.main()
