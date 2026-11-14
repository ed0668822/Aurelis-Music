#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""No-network F4 classification fixtures."""
import unittest
import file_compile_matrix as f4

BASE = {"path":"app/src/main/java/demo/A.kt","variant":"release","role":"MANAGED_SOURCE",
        "source_set":"main","kind":"production","compiler_task_state":"SUCCESS",
        "compiler_task_input":True,"compiler_file_errors":0,"gradle_diagnostic_errors":0,
        "apk_state":"COMPILER_CLASSES_MAPPED_TO_DEX","mapped_class_count":1}
TRACE = {"class_evidence":"R8_OR_DEX_CLASSES_OBSERVED","declaration_hint":"OTHER_OR_UNKNOWN_SOURCE_TEXT"}


class VerdictTests(unittest.TestCase):
    def test_only_full_evidence_can_prove_compilation(self):
        self.assertEqual(f4.classify(BASE,TRACE,True)[0],"COMPILADO_EN_APK")
        self.assertEqual(f4.classify(BASE,TRACE,False)[0],"NO_EVIDENCIA_SUFICIENTE")
        self.assertEqual(f4.classify({**BASE,"compiler_task_input":False},TRACE,True)[0],
                         "NO_EVIDENCIA_SUFICIENTE")

    def test_error_and_task_failure_are_separate(self):
        self.assertEqual(f4.classify({**BASE,"compiler_file_errors":1},TRACE,True)[0],"ERROR_ATRIBUIDO")
        self.assertEqual(f4.classify({**BASE,"compiler_task_state":"FAILED"},TRACE,True)[0],"NO_EJECUTADO")

    def test_only_imports_and_ambiguity_are_explicit(self):
        self.assertEqual(f4.classify({**BASE,"mapped_class_count":0},
            {**TRACE,"declaration_hint":"ONLY_PACKAGE_IMPORT_COMMENT_TEXT"},True)[0],"SIN_CLASES_ESPERADAS")
        self.assertEqual(f4.classify({**BASE,"apk_state":"AMBIGUOUS_SOURCE_CLASS_MAPPING"},TRACE,True)[0],"AMBIGUO")

    def test_native_command_is_not_object_proof(self):
        self.assertEqual(f4.classify({**BASE,"role":"NATIVE_SOURCE"},TRACE,True)[0],
                         "NO_EVIDENCIA_SUFICIENTE")

    def test_tests_are_not_production_apk_sources(self):
        self.assertEqual(f4.classify({**BASE,"kind":"test"},TRACE,True)[0],"FUERA_DE_PRODUCCION")


if __name__ == "__main__":
    unittest.main()
