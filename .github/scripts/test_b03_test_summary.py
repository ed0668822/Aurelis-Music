#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later
"""Fixtures for honest B03 classification and propagation of failed commands."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
from tempfile import TemporaryDirectory
import unittest
import xml.etree.ElementTree as ET

import b03_test_summary as mod

SHA = "a" * 40
SCRIPTS = Path(__file__).resolve().parent


class ReportTests(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.output = self.root / "ci-b03"
        self.output.mkdir()
        for variant in mod.VARIANTS:
            self.xml(variant)
        self.android()

    def xml(self, variant, problem=None):
        folder = self.root / f"app/build/test-results/test{variant.title()}UnitTest"
        folder.mkdir(parents=True, exist_ok=True)
        suite = ET.Element("testsuite", name="com.music.aurelis.Tests")
        case = ET.SubElement(suite, "testcase", classname="com.music.aurelis.Tests", name="localRoute", time="0.1")
        if problem:
            error = ET.SubElement(case, problem, message="Expected return to Home")
            error.text = "java.lang.AssertionError\n at com.music.aurelis.Tests.localRoute(Tests.kt:107)"
        ET.ElementTree(suite).write(folder / "TEST-fixture.xml")

    def android(self, **overrides):
        evidence = {"source_sha": SHA, "passed": True,
                    "device": {"api": "36", "expected_api": "36"},
                    "cases": [{"method": name, "passed": True, "problems": []}
                              for name in sorted(mod.ANDROID_CASES)]}
        evidence.update(overrides)
        folder = self.root / "ci-ui-comparison"
        folder.mkdir(exist_ok=True)
        (folder / "settings-navigation.json").write_text(json.dumps(evidence))

    def report(self, outcomes=None, compilations=None, android="success"):
        return mod.summarize(self.root, self.output,
                             outcomes or dict.fromkeys(mod.VARIANTS, "success"),
                             compilations or dict.fromkeys(mod.VARIANTS, "success"), android, SHA)

    def test_green_requires_two_nonempty_suites_and_verified_android_runtime(self):
        result = self.report()
        self.assertEqual([], mod.gate(result))
        self.assertEqual(1, result["debug"]["passed"])
        self.assertTrue((self.output / "junit/debug/TEST-fixture.xml").is_file())
        self.assertTrue((self.output / "android16/settings-navigation.json").is_file())
        self.assertIn("- Commit: `" + SHA + "`", (self.output / "summary.md").read_text())

    def test_added_runtime_fields_preserve_b06_junit_attestation(self):
        import b06_junit_attestation
        self.report()
        package = self.root / "package"
        summary = package / "B03/Resumen"
        summary.mkdir(parents=True)
        for name in ("summary.md", "results.json"):
            shutil.copy2(self.output / name, summary / name)
        for variant in mod.VARIANTS:
            target = package / "B03" / variant.title() / "junit" / variant
            target.mkdir(parents=True)
            shutil.copy2(self.output / "junit" / variant / "TEST-fixture.xml",
                         target / "TEST-com.music.aurelis.Tests.xml")
        result = b06_junit_attestation.attest_cases(package, SHA,
                    {"route": ("com.music.aurelis.Tests", "localRoute")})
        self.assertTrue(result["b03_tests_verified"])
        self.assertEqual("ROBOLECTRIC_JUNIT_PASS", result["states"]["route"])

    def test_compile_failure_is_primary_even_with_stale_passing_xml(self):
        (self.output / "compile-debug.log").write_text("e: file:///Tests.kt:132:72 receiver type mismatch\n")
        result = self.report({"debug": "skipped", "release": "success"},
                             {"debug": "failure", "release": "success"})
        self.assertIn("debug", mod.gate(result))
        self.assertEqual("COMPILATION_FAILED", result["debug"]["first_failure"]["phase"])
        self.assertIn("132:72", result["debug"]["first_failure"]["message"])
        self.assertTrue(result["release"]["passed_gate"])

    def test_junit_failure_includes_case_and_application_source_location(self):
        self.xml("debug", "failure")
        result = self.report({"debug": "failure", "release": "success"})
        failure = result["debug"]["first_failure"]
        self.assertEqual("TEST_FAILURE", failure["phase"])
        self.assertEqual("localRoute", failure["test"])
        self.assertIn("Tests.kt:107", failure["source"])
        self.assertFalse(result["debug"]["passed_gate"])
        self.assertIn("FAILED", (self.output / "test-cases.tsv").read_text())

    def test_timeout_records_the_unfinished_test_without_certifying_old_xml(self):
        (self.output / "debug.log").write_text(
            "AURELIS_TEST_START class=Tests test=finished\n"
            "AURELIS_TEST_END class=Tests test=finished result=SUCCESS\n"
            "AURELIS_TEST_START class=Tests test=still running\n"
            "AURELIS_PHASE_EXIT phase=debug exit_code=124\n")
        result = self.report({"debug": "failure", "release": "success"})
        failure = result["debug"]["first_failure"]
        self.assertEqual("TEST_TIMEOUT", failure["phase"])
        self.assertEqual("Tests#still running", failure["active_test"])
        self.assertFalse(result["debug"]["passed_gate"])

    def test_missing_empty_or_malformed_xml_never_pass(self):
        folder = self.root / "app/build/test-results/testDebugUnitTest"
        path = folder / "TEST-fixture.xml"
        for contents in (None, "<testsuite/>", "<broken>", "<unrelated/>"):
            with self.subTest(contents=contents):
                if contents is None:
                    path.unlink(missing_ok=True)
                else:
                    path.write_text(contents)
                self.assertIn("debug", mod.gate(self.report()))

    def test_skipped_and_error_cases_are_not_counted_as_passes(self):
        for tag in ("skipped", "error"):
            with self.subTest(tag=tag):
                self.xml("debug", tag)
                result = self.report()
                self.assertEqual(0, result["debug"]["passed"])
                self.assertIn("debug", mod.gate(result))

    def test_configuration_failure_and_not_run_are_separate_from_assertions(self):
        (self.output / "compile-release.log").write_text("Could not resolve kover: HTTP429 Too Many Requests\n")
        result = self.report({"debug": "success", "release": "skipped"},
                             {"debug": "success", "release": "failure"})
        self.assertIn("HTTP429", result["release"]["first_failure"]["message"])
        self.assertEqual("DEPENDENCY_RESOLUTION_FAILED", result["release"]["first_failure"]["phase"])
        result = self.report(compilations={"debug": "skipped", "release": "skipped"})
        self.assertEqual("COMPILATION_NOT_RUN", result["debug"]["first_failure"]["phase"])

    def test_wrong_commit_api_or_missing_case_cannot_pass_android(self):
        variants = [{"source_sha": "b" * 40}, {"device": {"api": "35", "expected_api": "36"}},
                    {"cases": []}, {"cases": [{"method": "other", "passed": True}]},
                    {"passed": False}, {"cases": [None, None]}]
        for override in variants:
            with self.subTest(override=override):
                self.android(**override)
                self.assertIn("android16_navigation", mod.gate(self.report()))

    def test_android_missing_evidence_or_failed_action_still_fails(self):
        self.assertIn("android16_navigation", mod.gate(self.report(android="failure")))
        (self.root / "ci-ui-comparison/settings-navigation.json").unlink()
        self.assertIn("android16_navigation", mod.gate(self.report()))

    def test_gate_rejects_forged_positive_flag_with_bad_counters(self):
        result = self.report()
        result["debug"]["failed"] = 1
        self.assertIn("debug", mod.gate(result))
        result = self.report()
        result["release"]["compile_outcome"] = "failure"
        self.assertIn("release", mod.gate(result))
        result = self.report()
        result["android16_navigation"]["cases"] = []
        self.assertIn("android16_navigation", mod.gate(result))

    def test_failed_test_command_does_not_pass_with_successful_xml(self):
        result = self.report({"debug": "failure", "release": "success"})
        self.assertEqual("TEST_EXECUTION_FAILED", result["debug"]["first_failure"]["phase"])
        self.assertIn("debug", mod.gate(result))

    def test_killed_worker_is_not_reported_as_a_proven_timeout(self):
        (self.output / "debug.log").write_text("AURELIS_PHASE_EXIT phase=debug exit_code=137\n")
        result = self.report({"debug": "failure", "release": "success"})
        self.assertEqual("TEST_KILLED", result["debug"]["first_failure"]["phase"])
        self.assertIn("debug", mod.gate(result))

    def test_gate_cli_fails_for_absent_report(self):
        run = subprocess.run([sys.executable, str(SCRIPTS / "b03_test_summary.py"), "--gate",
                              "--output", str(self.root / "absent")], capture_output=True, text=True)
        self.assertEqual(1, run.returncode)
        self.assertIn("missing or invalid", run.stdout)


class PhaseAndWorkflowTests(unittest.TestCase):
    def test_command_exit_codes_and_timeout_codes_are_preserved(self):
        with TemporaryDirectory() as temp:
            for status in (0, 7, 124):
                with self.subTest(status=status):
                    run = subprocess.run(["bash", str(SCRIPTS / "b03-phase.sh"), "debug",
                                          sys.executable, "-c", f"print('fixture');raise SystemExit({status})"],
                                         cwd=temp, capture_output=True, text=True)
                    self.assertEqual(status, run.returncode, run.stderr)
                    self.assertIn(f"exit_code={status}", (Path(temp) / "ci-b03/debug.log").read_text())

    def test_log_writer_failure_is_not_hidden_by_successful_command(self):
        with TemporaryDirectory() as temp:
            folder = Path(temp) / "bin"
            folder.mkdir()
            tee = folder / "tee"
            tee.write_text("#!/bin/sh\nexit 9\n")
            tee.chmod(0o755)
            env = dict(os.environ, PATH=str(folder) + os.pathsep + os.environ["PATH"])
            for command_status, expected in ((0, 9), (7, 7)):
                with self.subTest(command_status=command_status):
                    run = subprocess.run(["bash", str(SCRIPTS / "b03-phase.sh"), "debug",
                                          sys.executable, "-c", f"raise SystemExit({command_status})"],
                                         cwd=temp, env=env, capture_output=True, text=True)
                    self.assertEqual(expected, run.returncode)

    def test_maven_central_429_retries_and_recovers(self):
        with TemporaryDirectory() as temp:
            counter = Path(temp) / "attempts"
            script = (
                "from pathlib import Path; import sys; "
                "p=Path('attempts'); n=int(p.read_text())+1 if p.exists() else 1; "
                "p.write_text(str(n)); "
                "print('Could not GET https://repo.maven.apache.org/maven2/example.pom. "
                "Received status code 429 from server: Too Many Requests' if n==1 else 'SUCCESS'); "
                "sys.exit(1 if n==1 else 0)"
            )
            env = dict(os.environ, AURELIS_MAVEN_RETRY_DELAY_OVERRIDE="0")
            run = subprocess.run(["bash", str(SCRIPTS / "b03-phase.sh"), "compile-release",
                                  sys.executable, "-c", script],
                                 cwd=temp, env=env, capture_output=True, text=True)
            self.assertEqual(0, run.returncode, run.stdout + run.stderr)
            self.assertEqual("2", counter.read_text())
            self.assertIn("attempts=2", (Path(temp) / "ci-b03/compile-release.log").read_text())

    def test_maven_central_429_exhaustion_remains_failure(self):
        with TemporaryDirectory() as temp:
            script = ("print('Could not GET https://repo.maven.apache.org/maven2/example.pom. "
                      "Received status code 429 from server: Too Many Requests'); "
                      "raise SystemExit(1)")
            env = dict(os.environ, AURELIS_MAVEN_RETRY_DELAY_OVERRIDE="0")
            run = subprocess.run(["bash", str(SCRIPTS / "b03-phase.sh"), "compile-debug",
                                  sys.executable, "-c", script],
                                 cwd=temp, env=env, capture_output=True, text=True)
            self.assertEqual(1, run.returncode)
            self.assertIn("attempts=3", (Path(temp) / "ci-b03/compile-debug.log").read_text())

    def test_workflow_keeps_variants_independent_and_reuses_immutable_apks(self):
        flow = (SCRIPTS.parent / "workflows/b03-unit-tests.yml").read_text()
        self.assertLess(flow.index('id: compile_release'), flow.index('id: debug\n'))
        release = flow.split('id: release\n', 1)[1].split('      - name:', 1)[0]
        self.assertIn("steps.compile_release.outcome == 'success'", release)
        self.assertNotIn('steps.debug.outcome', release)
        self.assertIn('name: b01-shared-apks-${{ github.run_number }}', flow)
        self.assertIn('ci14_handoff.py restore --variant all', flow)
        self.assertIn('api-level: 36', flow)
        self.assertIn('ui_parity.py settings-navigation', flow)
        self.assertIn('b03_test_summary.py --gate', flow)
        self.assertIn('f5_coverage_gate.py ci-b03/diff-coverage.json', flow)


if __name__ == "__main__":
    unittest.main()
