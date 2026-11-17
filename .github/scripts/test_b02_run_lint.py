#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later
"""Fake CLI fixtures only: no Gradle, Android, network or coverage execution."""
import json
import os
from pathlib import Path
import shutil
import subprocess
from tempfile import TemporaryDirectory
import unittest

SCRIPT = Path(__file__).with_name("b02-run-lint.sh").resolve()
MISSING = """A problem occurred configuring root project 'Aurelis Music'.
Could not find org.jetbrains.kotlinx:kover-features-jvm:0.9.11.
Could not find org.jetbrains.kotlinx:kover-jvm-agent:0.9.11.
"""
RATE_LIMIT = """A problem occurred configuring root project 'Aurelis Music'.
Could not resolve org.jetbrains.kotlinx:kover-jvm-agent:0.9.11.
Could not GET 'https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kover-jvm-agent/0.9.11/kover-jvm-agent-0.9.11.pom'. Received status code 429 from server: Too Many Requests
"""


class LintRetryTests(unittest.TestCase):
    def run_fixture(self, responses, tee_failure=False):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "responses.json").write_text(json.dumps(responses))
            fake = root / "gradlew"
            fake.write_text("""#!/usr/bin/env python3
import json
from pathlib import Path
import sys
calls_path = Path('calls.json')
calls = json.loads(calls_path.read_text()) if calls_path.exists() else []
response = json.loads(Path('responses.json').read_text())[len(calls)]
calls.append(sys.argv[1:])
calls_path.write_text(json.dumps(calls))
print(response[1], flush=True)
sys.exit(response[0])
""")
            fake.chmod(0o755)
            tools = root / "bin"
            tools.mkdir()
            pause = tools / "sleep"
            pause.write_text("#!/usr/bin/env python3\nimport sys\nfrom pathlib import Path\n"
                             "with Path('ci-b02/retry-sleeps.log').open('a') as fp: fp.write(sys.argv[1]+'\\n')\n")
            pause.chmod(0o755)
            if tee_failure:
                tee = tools / "tee"
                tee.write_text("#!/usr/bin/env python3\nimport subprocess,sys\n"
                               f"subprocess.run([{shutil.which('tee')!r},*sys.argv[1:]])\n"
                               "raise SystemExit(9)\n")
                tee.chmod(0o755)
            env = dict(os.environ, PATH=str(tools) + os.pathsep + os.environ["PATH"])
            result = subprocess.run(["bash", str(SCRIPT)], cwd=root, capture_output=True, text=True,
                                    timeout=15, env=env)
            calls = json.loads((root / "calls.json").read_text())
            logs = {p.name: p.read_text() for p in (root / "ci-b02").glob("*.log")}
            return result.returncode, calls, logs

    def test_first_success_runs_both_variants_without_refresh(self):
        code, calls, logs = self.run_fixture([(0, "lint complete")])
        self.assertEqual(code, 0)
        self.assertEqual(len(calls), 1)
        self.assertIn(":app:lintDebug", calls[0]); self.assertIn(":app:lintRelease", calls[0])
        self.assertNotIn("--refresh-dependencies", calls[0])
        self.assertNotIn("lint-attempt-2.log", logs)

    def test_known_configuration_failure_refreshes_once_and_preserves_both_attempts(self):
        code, calls, logs = self.run_fixture([(1, MISSING), (0, "fresh lint complete")])
        self.assertEqual(code, 0); self.assertEqual(len(calls), 2)
        self.assertNotIn("--refresh-dependencies", calls[0])
        self.assertEqual(calls[1], calls[0] + ["--refresh-dependencies"])
        self.assertIn(MISSING.strip(), logs["lint.log"])
        self.assertIn("fresh lint complete", logs["lint.log"])
        self.assertEqual(logs["lint-attempt-1.log"], MISSING + "\n")
        self.assertIn("fresh lint complete", logs["lint-attempt-2.log"])

    def test_second_failure_is_final_and_never_a_false_success(self):
        code, calls, logs = self.run_fixture([(1, MISSING), (9, MISSING)])
        self.assertEqual(code, 9); self.assertEqual(len(calls), 2)
        self.assertIn(MISSING.strip(), logs["lint-attempt-2.log"])

    def test_actual_lint_errors_are_not_retried_even_with_missing_dependency_text(self):
        output = MISSING + "\n> Task :app:lintDebug FAILED\nFile.kt: Error: defect [WrongConstant]\n"
        code, calls, _ = self.run_fixture([(7, output)])
        self.assertEqual(code, 7); self.assertEqual(len(calls), 1)

    def test_unrelated_dependency_or_version_is_not_retried(self):
        for output in (MISSING.replace("kover-", "unrelated-"), MISSING.replace("0.9.11", "0.9.10")):
            with self.subTest(output=output):
                code, calls, _ = self.run_fixture([(4, output)])
                self.assertEqual(code, 4); self.assertEqual(len(calls), 1)

    def test_missing_module_without_root_configuration_failure_is_not_retried(self):
        code, calls, _ = self.run_fixture([(3, MISSING.split("\n", 1)[1])])
        self.assertEqual(code, 3); self.assertEqual(len(calls), 1)

    def test_observed_maven429_retries_without_refresh_and_preserves_both_logs(self):
        code, calls, logs = self.run_fixture([(1, RATE_LIMIT), (0, "lint complete")])
        self.assertEqual(0, code); self.assertEqual(2, len(calls))
        self.assertEqual(calls[0], calls[1])
        self.assertNotIn("--refresh-dependencies", calls[1])
        self.assertEqual("15\n", logs["retry-sleeps.log"])
        self.assertIn(RATE_LIMIT.strip(), logs["lint-attempt-1.log"])
        self.assertIn("lint complete", logs["lint-attempt-2.log"])

    def test_two_rate_limits_back_off15_then30_and_keep_success(self):
        code, calls, logs = self.run_fixture([(1, RATE_LIMIT), (1, RATE_LIMIT), (0, "lint complete")])
        self.assertEqual(0, code); self.assertEqual(3, len(calls))
        self.assertTrue(all(call == calls[0] for call in calls))
        self.assertEqual("15\n30\n", logs["retry-sleeps.log"])
        self.assertIn("lint complete", logs["lint-attempt-3.log"])

    def test_three_rate_limits_fail_with_last_exit_code_without_a_fourth_attempt(self):
        code, calls, logs = self.run_fixture([(1, RATE_LIMIT), (7, RATE_LIMIT), (9, RATE_LIMIT)])
        self.assertEqual(9, code); self.assertEqual(3, len(calls))
        self.assertEqual("15\n30\n", logs["retry-sleeps.log"])

    def test_actual_lint_failure_after_rate_limit_stops_immediately(self):
        real_failure = "> Task :app:lintDebug FAILED\nFile.kt: Error: defect [WrongConstant]\n"
        code, calls, logs = self.run_fixture([(1, RATE_LIMIT), (7, real_failure)])
        self.assertEqual(7, code); self.assertEqual(2, len(calls))
        self.assertEqual("15\n", logs["retry-sleeps.log"])
        self.assertNotIn("lint-attempt-3.log", logs)

    def test_other_artifact_version_host_or_http_status_is_not_retried(self):
        for output in (RATE_LIMIT.replace("kover-", "other-"), RATE_LIMIT.replace("0.9.11", "0.9.10"),
                       RATE_LIMIT.replace("repo.maven.apache.org", "mirror.example"),
                       RATE_LIMIT.replace("429", "403"), RATE_LIMIT.replace("429", "503")):
            with self.subTest(output=output):
                code, calls, logs = self.run_fixture([(4, output)])
                self.assertEqual(4, code); self.assertEqual(1, len(calls))
                self.assertNotIn("retry-sleeps.log", logs)

    def test_no_retry_after_any_task_or_without_root_configuration_failure(self):
        for output in (RATE_LIMIT.split("\n", 1)[1], RATE_LIMIT + "\n> Task :app:compileDebugKotlin FAILED\n",
                       RATE_LIMIT + "\ne: Kotlin source error\n", RATE_LIMIT + "\nError: defect [WrongConstant]\n"):
            with self.subTest(output=output):
                code, calls, _ = self.run_fixture([(5, output)])
                self.assertEqual(5, code); self.assertEqual(1, len(calls))

    def test_retry_stops_if_next_failure_is_not_the_observed_rate_limit(self):
        code, calls, logs = self.run_fixture([(1, RATE_LIMIT), (8, RATE_LIMIT.replace("429", "404"))])
        self.assertEqual(8, code); self.assertEqual(2, len(calls))
        self.assertNotIn("lint-attempt-3.log", logs)

    def test_rate_limit_has_priority_over_missing_metadata_without_refreshing(self):
        code, calls, _ = self.run_fixture([(1, RATE_LIMIT + MISSING), (0, "lint complete")])
        self.assertEqual(0, code); self.assertEqual(2, len(calls))
        self.assertEqual(calls[0], calls[1])

    def test_log_writer_failure_never_passes_or_triggers_a_dependency_retry(self):
        for status, output, expected in ((0, "lint complete", 9), (1, RATE_LIMIT, 1)):
            with self.subTest(status=status):
                code, calls, logs = self.run_fixture([(status, output)], tee_failure=True)
                self.assertEqual(expected, code); self.assertEqual(1, len(calls))
                self.assertNotIn("retry-sleeps.log", logs)


if __name__ == "__main__":
    unittest.main()
