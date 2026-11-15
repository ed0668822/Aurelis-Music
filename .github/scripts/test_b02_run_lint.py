#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later
"""Fake CLI fixtures only: no Gradle, Android, network or coverage execution."""
import json
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest

SCRIPT = Path(__file__).with_name("b02-run-lint.sh").resolve()
MISSING = """A problem occurred configuring root project 'Aurelis Music'.
Could not find org.jetbrains.kotlinx:kover-features-jvm:0.9.11.
Could not find org.jetbrains.kotlinx:kover-jvm-agent:0.9.11.
"""


class LintRetryTests(unittest.TestCase):
    def run_fixture(self, responses):
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
            result = subprocess.run(["bash", str(SCRIPT)], cwd=root, capture_output=True, text=True, timeout=15)
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


if __name__ == "__main__":
    unittest.main()
