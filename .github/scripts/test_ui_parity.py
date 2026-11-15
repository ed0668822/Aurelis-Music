# Aurelis © 2026. GPL-3.0-or-later
"""Evidence integrity and navigation selectors; no Android success is inferred."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile
import ui_parity as parity


class ParityEvidenceTest(unittest.TestCase):
    def fixture(self, base, filters=None, output="original.apk", package="com.music.bitchord", abi="arm64-v8a"):
        base.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(base / "original.apk", "w") as archive:
            archive.writestr(f"lib/{abi}/fixture.so", b"Fixture; not a real ELF or installable APK")
        (base / "output-metadata.json").write_text(json.dumps({"applicationId": package, "elements": [{
            "filters": filters if filters is not None else [{"filterType": "ABI", "value": "arm64-v8a"}],
            "outputFile": output, "versionName": "fixture"}]}))

    def test_reference_selection_preserves_original_bytes_and_identity(self):
        with tempfile.TemporaryDirectory() as temp:
            base = Path(temp) / "input"; out = Path(temp) / "out"
            self.fixture(base)
            parity.select_reference(base, out)
            self.assertEqual((base / "original.apk").read_bytes(), (out / "reference.apk").read_bytes())
            record = json.loads((out / "reference.json").read_text())
            self.assertEqual(parity.REFERENCE_SHA, record["commit"])
            self.assertEqual(parity.digest(out / "reference.apk"), record["sha256"])
            self.assertFalse(record["source_modified"])

    def test_wrong_package_or_abi_is_rejected(self):
        for kwargs in ({"package": "other.app"}, {"abi": "x86_64"}, {"filters": []}):
            with self.subTest(kwargs=kwargs), tempfile.TemporaryDirectory() as temp:
                base = Path(temp) / "in"; self.fixture(base, **kwargs)
                with self.assertRaises(ValueError):
                    parity.select_reference(base, Path(temp) / "out")

    def test_traversal_and_ambiguous_outputs_are_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            base = Path(temp) / "in"; self.fixture(base, output="../outside.apk")
            with self.assertRaises(ValueError):
                parity.select_reference(base, Path(temp) / "out")
            data = json.loads((base / "output-metadata.json").read_text())
            data["elements"] *= 2
            (base / "output-metadata.json").write_text(json.dumps(data))
            with self.assertRaises(ValueError):
                parity.select_reference(base, Path(temp) / "out")

    def test_xml_uses_exact_text_or_description_and_rejects_empty_disabled_bounds(self):
        xml = '''<hierarchy><node text="Settings" bounds="[1,2][11,22]"/>
          <node content-desc="Open player" bounds="[2,3][12,23]"/>
          <node text="Search" bounds="[0,0][0,0]"/>
          <node text="Settings" enabled="false" bounds="[1,2][11,22]"/>
          <node text="Settings help" bounds="[1,2][11,22]"/></hierarchy>'''
        rows = parity.nodes(xml)
        self.assertEqual(3, len(rows))
        self.assertEqual(1, len(parity.matches(rows, (" settings ",))))
        self.assertEqual("Open player", parity.matches(rows, ("OPEN PLAYER",))[0]["label"])
        self.assertFalse(parity.matches(rows, ("Search",)))

    def test_foreground_requires_resumed_package_not_a_background_activity(self):
        with patch.object(parity, "shell", return_value="ActivityRecord com.music.aurelis/.MainActivity\nmResumedActivity com.other/.Main"):
            self.assertFalse(parity.foreground("com.music.aurelis"))
        with patch.object(parity, "shell", return_value="mResumedActivity com.music.aurelis/.MainActivity"):
            self.assertTrue(parity.foreground("com.music.aurelis"))

    def test_valid_png_header_is_required_before_storing_screenshot(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, "adb", return_value=b"not PNG"):
            with self.assertRaises(ValueError):
                parity.screenshot(Path(temp))
            self.assertFalse((Path(temp) / "screen.png").exists())

    def test_unreached_state_never_becomes_parity_even_with_capture_and_foreground(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, "OUT", Path(temp)), \
             patch.object(parity, "tree", return_value='<hierarchy><node text="Other screen" bounds="[0,0][10,10]"/></hierarchy>'), \
             patch.object(parity, "foreground", return_value=True), patch.object(parity, "screenshot", return_value=[1080, 1920]):
            result = {"screens": []}
            parity.record_state(result, "aurelis", "dark", "settings", False)
            row = result["screens"][0]
            self.assertFalse(row["reached"]); self.assertEqual("NOT_REVIEWED", row["visual_verdict"])
            parity.report()
            text = (Path(temp) / "index.html").read_text()
            self.assertIn("NOT REACHED", text)
            self.assertNotIn("REACHED — review pending", text)
            self.assertEqual("NOT_REVIEWED", json.loads((Path(temp) / "results.json").read_text())["visual_verdict"])

    def test_landscape_capture_requires_actual_landscape_dimensions(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, "OUT", Path(temp)), \
             patch.object(parity, "tree", return_value='<hierarchy/>'), \
             patch.object(parity, "foreground", return_value=True), \
             patch.object(parity, "screenshot", return_value=[1080, 1920]), \
             patch.object(parity, "shell", return_value="fixture session"):
            result = {"screens": []}
            parity.record_state(result, "aurelis", "dark", "player-landscape", True)
            self.assertFalse(result["screens"][0]["reached"])
            self.assertIn("orientation", result["screens"][0]["reason"])

    def test_report_without_capture_is_explicitly_incomplete(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, "OUT", Path(temp)):
            parity.report()
            result = json.loads((Path(temp) / "results.json").read_text())
            self.assertIn("never produced", result["error"])
            self.assertEqual("NOT_REVIEWED", result["visual_verdict"])
            self.assertIn("Capture complete: False", (Path(temp) / "REVIEW_PENDING.md").read_text())


if __name__ == "__main__":
    unittest.main()
