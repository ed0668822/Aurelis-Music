#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
import unittest
import v1_versioning as v1

P = {"schema_version": 1, "repo": v1.REPO, "workflow": v1.WORKFLOW,
     "anchor_run_number": 101, "base_major": 1, "base_minor": 9,
     "successes_per_minor": 150, "anchor_visible_patch": 101,
     "version_code_offset": 10000}


class VersionTests(unittest.TestCase):
    def test_policy_rejects_unsafe_anchor_and_code_resets(self):
        import json
        import tempfile
        from pathlib import Path
        for field, value in (("anchor_run_number", 1), ("version_code_offset", 1),
                             ("anchor_visible_patch", 0)):
            with self.subTest(field=field), tempfile.TemporaryDirectory() as d:
                path = Path(d) / "version-policy.json"
                path.write_text(json.dumps({**P, field: value}), encoding="utf-8")
                with self.assertRaisesRegex(ValueError, "Invalid V1 policy"):
                    v1.read_policy(path)

    def test_exact_150_success_cycle(self):
        for green, run, result in [
            (0, 102, "1.9.102"), (149, 251, "1.9.251"),
            (150, 252, "1.10.0"), (300, 402, "1.11.0")]:
            self.assertEqual(v1.compute(green, run, P)[0], result)
            self.assertEqual(v1.compute(green, run, P)[1], 10000 + run)

    def test_bad_counters_fail(self):
        for count, run in [(-1, 110), (150, 151), (1, 101)]:
            with self.assertRaises(ValueError):
                v1.compute(count, run, P)

    def test_immutable_source_and_run_identity(self):
        m = v1.manifest(P, "a" * 40, 8000, 102, 0)
        self.assertEqual(v1.validate(m, P, "a" * 40, 8000, 102), m)
        for sha, ident, number in [("b" * 40, 8000, 102),
                                   ("a" * 40, 8001, 102),
                                   ("a" * 40, 8000, 103)]:
            with self.assertRaises(ValueError):
                v1.validate(m, P, sha, ident, number)
        changed = dict(m, version_name="1.10.0")
        with self.assertRaises(ValueError):
            v1.validate(changed, P, "a" * 40, 8000, 102)


if __name__ == "__main__":
    unittest.main()
