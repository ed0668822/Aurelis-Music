#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.2: test task/source distinction, failures, caching and native declarations."""
import json
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import android_ci as ci
import p2_compiler_evidence as p2

SHA = "a" * 40
KT = "app/src/main/java/com/music/aurelis/ui/haptics/Haptics.kt"
JAVA = "app/src/main/java/com/music/aurelis/Legacy.java"
CPP = "app/src/main/cpp/analyzer/audio_analysis.cpp"
TEST = "app/src/test/java/com/music/aurelis/AudioTest.kt"


class CompilerEvidenceTests(unittest.TestCase):
    def fixture(self, root, kotlin="SUCCESS", java="NO-SOURCE", native="SUCCESS"):
        out = root / "ci-artifacts"
        source = []
        for path, language in ((KT, "Kotlin"), (JAVA, "Java"), (CPP, "C++"), (TEST, "Kotlin")):
            row = ci.file_policy(Path(path))
            source.append({"path": path, **row, "language": language})
        ci.save(out / "source-inventory.json", source)
        ci.save(out / "metadata.json", {"source_sha": SHA, "checkout_sha": SHA})
        lines = [
            'AURELIS_TASK_INPUT {"task":":app:compileReleaseKotlin","path":"' + KT + '","origin":"tracked"}',
            'AURELIS_TASK_INPUT {"task":":app:compileReleaseKotlin","path":"' + JAVA + '","origin":"tracked"}',
            'AURELIS_TASK_INPUT {"task":":app:compileReleaseJavaWithJavac","path":"' + JAVA + '","origin":"tracked"}',
            'AURELIS_TASK_RESULT {"task":":app:compileReleaseKotlin","state":"' + kotlin + '"}',
            'AURELIS_TASK_RESULT {"task":":app:compileReleaseJavaWithJavac","state":"' + java + '"}',
            'AURELIS_TASK_RESULT {"task":":app:externalNativeBuildRelease","state":"' + native + '"}',
        ]
        (out / "build.log").write_text("\n".join(lines) + "\n", encoding="utf-8")
        native_file = root / "app/.cxx/Release/abc/arm64-v8a/compile_commands.json"
        native_file.parent.mkdir(parents=True, exist_ok=True)
        native_file.write_text(json.dumps([
            {"directory": str(root), "file": CPP, "command": "clang++ -DAUTH_KEY_SHOULD_NOT_BE_LOGGED"}
        ]), encoding="utf-8")
        return out

    def rows(self, root, **options):
        output = self.fixture(root, **options)
        summary = p2.audit(root, output, SHA)
        result = json.loads((output / "p2-compiler-evidence.json").read_text())
        return summary, result["rows"], output

    def test_task_success_does_not_certify_any_individual_file(self):
        with TemporaryDirectory() as tmp:
            summary, rows, output = self.rows(Path(tmp))
            self.assertEqual(summary["status"], "EVIDENCE_RECORDED")
            self.assertEqual((summary["source_files"], summary["variant_rows"]), (4, 8))
            kotlin = next(r for r in rows if r["path"] == KT and r["variant"] == "release")
            self.assertEqual(kotlin["compiler_evidence"], "COMPILER_TASK_SUCCEEDED_WITH_INPUT")
            self.assertEqual(kotlin["individual_compilation"], "NOT_ESTABLISHED")
            java = next(r for r in rows if r["path"] == JAVA and r["variant"] == "release")
            self.assertEqual(java["compiler_evidence"], "COMPILER_TASK_DID_NOT_COMPILE_INPUT")
            self.assertEqual(java["task_state"], "NO-SOURCE")
            self.assertEqual(next(r for r in rows if r["path"] == TEST)["compiler_evidence"],
                             "TEST_SOURCE_OUT_OF_PRODUCTION_BUILD")
            self.assertEqual(next(r for r in rows if r["path"] == KT and r["variant"] == "debug")["compiler_evidence"],
                             "NO_MATCHING_COMPILER_TASK_INPUT")
            self.assertNotIn("AUTH_KEY_SHOULD_NOT_BE_LOGGED", (output / "p2-compiler-evidence.json").read_text())

    def test_native_declaration_is_not_individual_compilation_proof(self):
        with TemporaryDirectory() as tmp:
            _, rows, _ = self.rows(Path(tmp))
            native = next(r for r in rows if r["path"] == CPP and r["variant"] == "release")
            self.assertTrue(native["native_command_declared"])
            self.assertEqual(native["compiler_evidence"], "NATIVE_COMMAND_DECLARED_BUILD_TASK_SUCCEEDED")
            self.assertEqual(native["individual_compilation"], "NOT_ESTABLISHED")
            self.assertEqual(next(r for r in rows if r["path"] == CPP and r["variant"] == "debug")["compiler_evidence"],
                             "NO_NATIVE_COMPILE_COMMAND_EVIDENCE")

    def test_failure_is_not_attributed_as_file_error(self):
        with TemporaryDirectory() as tmp:
            _, rows, _ = self.rows(Path(tmp), kotlin="FAILED")
            kotlin = next(r for r in rows if r["path"] == KT and r["variant"] == "release")
            self.assertEqual(kotlin["compiler_evidence"], "COMPILER_TASK_FAILED_SIN_ERROR_ATRIBUIDO")
            self.assertEqual(kotlin["attributed_errors"], 0)

    def test_reuse_is_separate_from_executed_success(self):
        with TemporaryDirectory() as tmp:
            _, rows, _ = self.rows(Path(tmp), kotlin="FROM-CACHE")
            kotlin = next(r for r in rows if r["path"] == KT and r["variant"] == "release")
            self.assertEqual(kotlin["compiler_evidence"], "COMPILER_TASK_REUSED_WITH_INPUT")

    def test_missing_build_log_is_incomplete_not_a_pass(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            out = self.fixture(root)
            (out / "build.log").unlink()
            result = p2.audit(root, out, SHA)
            self.assertEqual(result["status"], "INCOMPLETE_EVIDENCE")
            self.assertIn("MISSING_BUILD_LOG", result["issues"])

    def test_conflicting_terminal_outcomes_cannot_pass(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            out = self.fixture(root)
            with (out / "build.log").open("a") as f:
                f.write('AURELIS_TASK_RESULT {"task":":app:compileReleaseKotlin","state":"FAILED"}\n')
            result = p2.audit(root, out, SHA)
            self.assertEqual(result["status"], "INCOMPLETE_EVIDENCE")
            self.assertIn("CONFLICTING_TERMINAL_TASK_RESULTS", result["issues"])


if __name__ == "__main__":
    unittest.main()
