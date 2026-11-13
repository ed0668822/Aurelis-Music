#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.5: three-artifact contract, VirusTotal-in-ZIP and cleanup guardrails."""
import json
import os
import re
import subprocess
import sys
import textwrap
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

    def test_disabled_candidate_policy_keeps_two_final_artifacts(self):
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

    def test_disabled_candidate_policy_refuses_unexpected_release_without_deleting(self):
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


class SignedCandidateTests(unittest.TestCase):
    """Run the real B04 inline guards using synthetic files, never real APKs/keys."""

    workflows = Path(__file__).resolve().parent.parent / "workflows"

    @classmethod
    def setUpClass(cls):
        cls.b04 = (cls.workflows / "b04-verify-apks.yml").read_text(encoding="utf-8")
        cls.b06 = (cls.workflows / "b06-unified-pipeline.yml").read_text(encoding="utf-8")

    @classmethod
    def step(cls, name):
        match = re.search(r"(?ms)^      - name: " + re.escape(name)
                          + r"\n(.*?)(?=^      - name: |\Z)", cls.b04)
        if match is None:
            raise AssertionError("Missing workflow step: " + name)
        return match.group(1)

    @classmethod
    def inline_python(cls, name):
        body = cls.step(name).split("python3 - <<'PY'\n", 1)[1]
        return textwrap.dedent(body.rsplit("\n          PY", 1)[0])

    def test_testing_upload_has_same_scope_as_report_and_cleanup(self):
        scope = ("${{ github.ref == 'refs/heads/feature/bitchord-local-migration' && "
                 "(github.event_name == 'push' || github.event_name == 'workflow_dispatch') }}")
        self.assertIn("RELEASE_CANDIDATE_EXPECTED: " + scope, self.b04)
        self.assertEqual(self.b06.count("RELEASE_PUBLISH_EXPECTED: " + scope), 2)
        step = self.step("Publish signed Release candidate for testing")
        for required in ("!cancelled()", "github.ref == 'refs/heads/feature/bitchord-local-migration'",
                         "github.event_name == 'push'", "github.event_name == 'workflow_dispatch'",
                         "steps.stage_apks.outcome == 'success'", "steps.evidence.outcome == 'success'",
                         "steps.report.outcome == 'success'", "path: ci-b04-delivery/release/*.apk",
                         "name: Aurelis-Release-ARM64-${{ github.run_number }}", "if-no-files-found: error"):
            self.assertIn(required, step)
        self.assertNotIn("continue-on-error", step)
        self.assertNotIn("secrets.", step)

    def test_official_release_guard_is_still_main_tags_and_environment_only(self):
        text = (self.workflows / "b04-protected-release.yml").read_text(encoding="utf-8")
        for required in ("inputs.confirmation == 'PUBLICAR'", "github.ref == 'refs/heads/main'",
                         "startsWith(github.ref, 'refs/tags/v')", "name: release"):
            self.assertIn(required, text)
        self.assertNotIn("release_candidate_apk", text)

    def run_gate(self, root, expected="true", outcome="success"):
        for rel, data in (("ci-artifacts/apk-verification.json", {"status": "success"}),
                          ("ci-artifacts/source-evidence.json", {"status": "PASS_VERIFIED_GATES"}),
                          ("ci-approved-apks/apk-manifest.json", [{"variant": "debug"}, {"variant": "release"}])):
            p = root / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(json.dumps(data), encoding="utf-8")
        (root / "ci-approved-apks/SHA256SUMS.txt").write_text("fixture\n", encoding="utf-8")
        for variant in ("debug", "release"):
            p = root / "ci-b04-delivery" / variant / "verified.apk"
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_bytes(b"synthetic gate fixture, not an Android APK")
        env = {**os.environ, **{k + "_OUTCOME": "success" for k in
               ("PREPARE", "SIGNING", "BUILD", "VERIFY", "SUMMARY", "STAGE", "EVIDENCE", "REPORT", "DEBUG_APK")},
               "RELEASE_CANDIDATE_OUTCOME": outcome}
        env.pop("RELEASE_CANDIDATE_EXPECTED", None)
        if expected is not None:
            env["RELEASE_CANDIDATE_EXPECTED"] = expected
        return subprocess.run([sys.executable, "-c", self.inline_python("Enforce B04 verified artifact gate")],
                              cwd=root, env=env, capture_output=True, text=True)

    def test_candidate_upload_must_succeed_when_expected(self):
        for outcome in ("success", "failure", "skipped", "cancelled", "missing"):
            with self.subTest(outcome=outcome), TemporaryDirectory() as tmp:
                result = self.run_gate(Path(tmp), outcome=outcome)
                self.assertEqual(result.returncode == 0, outcome == "success", result.stderr)

    def test_other_refs_require_skipped_candidate_and_invalid_policy_fails(self):
        for expected, outcome, ok in (("false", "skipped", True), ("false", "success", False),
                                      ("false", "failure", False), ("invalid", "success", False),
                                      (None, "success", False)):
            with self.subTest(expected=expected, outcome=outcome), TemporaryDirectory() as tmp:
                result = self.run_gate(Path(tmp), expected=expected, outcome=outcome)
                self.assertEqual(result.returncode == 0, ok, result.stderr)

    def test_staging_preserves_verified_bytes_and_rejects_bad_signer_hash_or_abi(self):
        import hashlib
        for scenario in ("valid", "debug-fallback", "wrong-signer", "wrong-hash", "wrong-abi"):
            with self.subTest(scenario=scenario), TemporaryDirectory() as tmp:
                root = Path(tmp)
                approved, evidence = root / "ci-approved-apks", root / "ci-artifacts"
                approved.mkdir(); evidence.mkdir()
                records = []
                for variant in ("debug", "release"):
                    payload = ("verified synthetic " + variant).encode()
                    name = variant + ".apk"
                    (approved / name).write_bytes(payload)
                    records.append({"variant": variant, "file": name, "bytes": len(payload),
                                    "sha256": hashlib.sha256(payload).hexdigest(), "abis": ["arm64-v8a"],
                                    "package": "com.music.aurelis", "signer_certificate_sha256": ["a" * 64],
                                    **{k: True for k in ("signature_verified", "zip_verified",
                                          "zip_16kb_alignment_verified", "native_16kb_alignment_verified")}})
                signing = {"mode": "configured_release", "configured_certificate_sha256": "a" * 64}
                if scenario == "debug-fallback": signing["mode"] = "debug_fallback"
                if scenario == "wrong-signer": records[1]["signer_certificate_sha256"] = ["b" * 64]
                if scenario == "wrong-hash": records[1]["sha256"] = "0" * 64
                if scenario == "wrong-abi": records[1]["abis"] = ["x86_64"]
                (approved / "apk-manifest.json").write_text(json.dumps(records), encoding="utf-8")
                (approved / "SHA256SUMS.txt").write_text("fixture\n", encoding="utf-8")
                (evidence / "apk-verification.json").write_text('{"status":"success"}', encoding="utf-8")
                (evidence / "signing.json").write_text(json.dumps(signing), encoding="utf-8")
                code = self.inline_python("Stage individually verified Debug and signed Release APKs")
                result = subprocess.run([sys.executable, "-c", code], cwd=root, capture_output=True, text=True)
                self.assertEqual(result.returncode == 0, scenario == "valid", result.stderr)
                if scenario == "valid":
                    self.assertEqual((root / "ci-b04-delivery/release/release.apk").read_bytes(),
                                     (approved / "release.apk").read_bytes())

    def test_signed_candidate_policy_never_claims_official_publication(self):
        candidate = bundle.artifact_delivery_policy(True)
        self.assertEqual(candidate["expected_final_artifacts"], 3)
        self.assertEqual(candidate["release_artifact_purpose"], "SIGNED_TESTING_CANDIDATE")
        self.assertFalse(candidate["official_release_published"])
        without = bundle.artifact_delivery_policy(False)
        self.assertEqual(without["expected_final_artifacts"], 2)
        self.assertEqual(without["release_artifact_purpose"], "NOT_PUBLISHED")
        self.assertFalse(without["official_release_published"])


if __name__ == "__main__":
    unittest.main()
