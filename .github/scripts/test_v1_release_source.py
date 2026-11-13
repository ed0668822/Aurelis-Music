#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from v1_release_source import verify_apk_versions
import v1_versioning as v1

P = {"anchor_run_number": 101, "base_major": 1, "base_minor": 9,
     "successes_per_minor": 150, "anchor_visible_patch": 101,
     "version_code_offset": 10000}
INFO = v1.manifest(P, "a" * 40, 8000, 102, 0)


class ReleaseVersionTests(unittest.TestCase):
    def test_apk_debug_and_release_must_share_verified_name_and_code(self):
        with TemporaryDirectory() as tmp:
            base = Path(tmp)
            items = [{"variant": v, "version_name": "1.9.102", "version_code": "10102"}
                     for v in ("debug", "release")]
            m, v = base / "manifest.json", base / "verification.json"
            m.write_text(json.dumps(items))
            v.write_text(json.dumps({"status": "success", "apks": items}))
            self.assertTrue(verify_apk_versions(INFO, m, v))
            items[1]["version_name"] = "1.10.0"
            v.write_text(json.dumps({"status": "success", "apks": items}))
            with self.assertRaisesRegex(ValueError, "version drift"):
                verify_apk_versions(INFO, m, v)


if __name__ == "__main__":
    unittest.main()
