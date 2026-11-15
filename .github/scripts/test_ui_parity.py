# Aurelis © 2026. GPL-3.0-or-later
"""Evidence integrity and navigation selectors; no Android success is inferred."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile
import ui_parity as parity
import ui_parity_handoff as external
import ci14_handoff as handoff
import os
import io
from contextlib import redirect_stdout


class ParityEvidenceTest(unittest.TestCase):
    def test_fixture_session_requires_this_packages_state_and_metadata(self):
        # Format observed in UI-4; another package's session must not leak into it.
        dump = '''      package=com.music.bitchord
      active=true
      state=PlaybackState {state=NONE(0), position=0}
      metadata: null
      package=com.music.aurelis
      active=true
      state=PlaybackState {state=PAUSED(2), position=12321}
      metadata: size=28, description=Parity Track 01, Parity Artist, Parity Album 1
'''
        self.assertFalse(parity.fixture_session(dump, "com.music.bitchord"))
        self.assertTrue(parity.fixture_session(dump, "com.music.aurelis", (2,)))
        self.assertFalse(parity.fixture_session(dump, "com.music.aurelis", (3,)))
        self.assertFalse(parity.fixture_session(dump.replace("Parity Track 01", "Parity Track 02"), "com.music.aurelis"))

    def test_song_row_and_distant_header_transport_are_not_a_miniplayer(self):
        rows = parity.nodes('''<hierarchy>
          <node text="Play" bounds="[200,1470][400,1560]"/>
          <node text="Parity Track 01" bounds="[246,1847][551,1905]"/>
        </hierarchy>''')
        self.assertEqual([], parity.mini_player_rows(rows))
        rows += parity.nodes('<hierarchy><node content-desc="Next" bounds="[820,1470][960,1560]"/></hierarchy>')
        self.assertEqual([], parity.mini_player_rows(rows))
        rows += parity.nodes('<hierarchy><node text="Parity Track 01&#10;Parity Artist" bounds="[180,1470][700,1560]"/></hierarchy>')
        self.assertEqual(1, len(parity.mini_player_rows(rows)))

    def test_settings_root_recognizes_reference_above_the_fold_without_appearance(self):
        rows = parity.nodes('''<hierarchy><node text="Settings" bounds="[48,328][500,450]"/>
          <node class="android.widget.EditText" bounds="[60,480][1000,630]"/>
          <node text="Account &amp; integrations" bounds="[60,700][900,800]"/></hierarchy>''')
        self.assertTrue(parity.settings_root(rows))
        self.assertFalse(parity.settings_root(parity.nodes('<hierarchy><node text="Settings" bounds="[0,20][90,60]"/></hierarchy>')))

    def test_capture_rechecks_miniplayer_instead_of_trusting_a_previous_tap(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, "OUT", Path(temp)), \
             patch.object(parity, "tree", return_value='<hierarchy><node text="Parity Track 01" bounds="[246,1847][551,1905]"/></hierarchy>'), \
             patch.object(parity, "foreground", return_value=True), \
             patch.object(parity, "screenshot", return_value=[1080, 1920]), \
             patch.object(parity, "shell", return_value="package=com.music.bitchord\n active=true\n state=PlaybackState {state=NONE(0)}\n metadata: null\n"):
            result = {"screens": []}
            parity.record_state(result, "bitchord", "dark", "miniplayer", True)
            self.assertFalse(result["screens"][0]["reached"])
            self.assertIn("paused fixture session", result["screens"][0]["reason"])

    def test_reset_uses_same_glass_and_fresh_data_for_each_app_and_theme(self):
        def response(*args, **kwargs):
            if args[:2] == ("pm", "clear"): return "Success"
            if args[:3] == ("cmd", "locale", "get-app-locales"): return "Locales are [en]"
            return ""
        with patch.object(parity, "shell", side_effect=response) as shell:
            for app in parity.PACKAGES:
                for theme in ("dark", "light"):
                    prefs = parity.seed_preferences(app, theme)
                    self.assertTrue(prefs["reset_app_data"])
                    self.assertTrue(prefs["liquid_glass"])
            bodies = [call.kwargs["input_data"] for call in shell.call_args_list if "input_data" in call.kwargs]
            self.assertEqual(4, len(bodies))
            self.assertTrue(all(b'name="liquid_glass" value="true"' in body for body in bodies))
            self.assertEqual(4, sum(call.args[:2] == ("pm", "clear") for call in shell.call_args_list))

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

    def test_report_prints_actual_capture_error_when_artifact_is_unavailable(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, "OUT", Path(temp)):
            result = {"stage": "offline_configuration", "error": "actual fixture failure", "screens": []}
            (Path(temp) / "results.json").write_text(json.dumps(result))
            output = io.StringIO()
            with redirect_stdout(output), patch.dict(os.environ, {}, clear=True): parity.report()
            self.assertIn("actual fixture failure", output.getvalue())
            self.assertIn("offline_configuration", output.getvalue())
            self.assertEqual("NOT_REVIEWED", json.loads((Path(temp) / "results.json").read_text())["visual_verdict"])

    def test_blank_editable_field_remains_available_for_search_verification(self):
        rows = parity.nodes('<hierarchy><node text="" class="android.widget.EditText" bounds="[0,20][100,60]"/>'
                            '<node text="" class="android.view.View" bounds="[0,20][100,60]"/></hierarchy>')
        self.assertEqual(1, len(rows)); self.assertTrue(rows[0]["class"].endswith("EditText"))
        self.assertEqual([], parity.matches(rows, ("Search",)))

    def test_text_description_and_multiline_merged_rows_can_all_be_matched(self):
        rows = parity.nodes('<hierarchy><node text="Parity Track 01&#10;Parity Artist" content-desc="Open player" bounds="[0,20][100,60]"/></hierarchy>')
        for target in ("Parity Track 01", "Parity Artist", "Open player"):
            self.assertEqual(1, len(parity.matches(rows, (target,))))
        self.assertEqual([], parity.matches(rows, ("Parity",)))

    def test_tap_scrolls_within_actual_container_then_taps_the_visible_target(self):
        first = '<hierarchy><node class="android.widget.ScrollView" scrollable="true" bounds="[10,100][210,500]"/></hierarchy>'
        second = '<hierarchy><node text="Songs" bounds="[20,200][100,240]"/></hierarchy>'
        with patch.object(parity, "tree", side_effect=[first, second]), \
             patch.object(parity, "shell") as shell, patch.object(parity.time, "sleep"):
            self.assertTrue(parity.tap(("Songs",), scroll=True))
            self.assertEqual(("input", "swipe", 110, 400, 110, 200, 350), shell.call_args_list[0].args)
            self.assertEqual(("input", "tap", 60, 220), shell.call_args_list[1].args)

    def test_waits_for_an_enabled_control_without_tapping_a_disabled_one(self):
        disabled = '<hierarchy><node text="Open player" enabled="false" bounds="[0,0][40,40]"/></hierarchy>'
        enabled = '<hierarchy><node text="Open player" bounds="[0,0][40,40]"/></hierarchy>'
        with patch.object(parity, "tree", side_effect=[disabled, enabled]), \
             patch.object(parity, "shell") as shell, patch.object(parity.time, "sleep"):
            self.assertTrue(parity.tap(("Open player",)))
            shell.assert_called_once_with("input", "tap", 20, 20)

    def external_fixture(self, root):
        sha = "a" * 40
        for variant, (_, source) in handoff.FILES.items():
            path = root / source; path.parent.mkdir(parents=True, exist_ok=True)
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("AndroidManifest.xml", b"test fixture")
                if variant == "debug": archive.writestr("lib/arm64-v8a/test.so", b"fixture")
        with patch.dict(os.environ, {"GITHUB_SHA": sha, "GITHUB_RUN_ID": "900", "GITHUB_RUN_NUMBER": "144", "GITHUB_RUN_ATTEMPT": "1"}):
            handoff.pack(root)
        run = {"id": 900, "run_number": 144, "run_attempt": 1, "head_sha": sha,
               "head_branch": external.BRANCH, "event": "push", "path": ".github/workflows/" + external.WORKFLOW}
        (root / "primary-run.json").write_text(json.dumps(run))
        return sha, run

    def test_cross_run_handoff_keeps_primary_identity_without_using_ui_run_number(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); sha, _ = self.external_fixture(root)
            with patch.dict(os.environ, {"GITHUB_RUN_ID": "901", "GITHUB_RUN_NUMBER": "1"}):
                data = external.verify(root, sha)
            self.assertEqual("144", data["run_number"])
            self.assertEqual("900", data["run_id"])

    def test_cross_run_rejects_other_workflow_branch_sha_and_attempt(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); sha, original = self.external_fixture(root)
            for key, wrong in (("path", ".github/workflows/other.yml"), ("head_branch", "main"),
                               ("head_sha", "b" * 40), ("run_attempt", 2)):
                (root / "primary-run.json").write_text(json.dumps({**original, key: wrong}))
                with self.assertRaises(ValueError): external.verify(root, sha)

    def test_cross_run_rejects_modified_apk_bytes(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); sha, _ = self.external_fixture(root)
            apk = root / "ci-f3-handoff/debug/app-debug.apk"
            with zipfile.ZipFile(apk, "a") as archive: archive.writestr("changed.txt", b"changed")
            with self.assertRaisesRegex(ValueError, "checksum/size"):
                external.verify(root, sha)

    def test_report_without_capture_is_explicitly_incomplete(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, "OUT", Path(temp)):
            parity.report()
            result = json.loads((Path(temp) / "results.json").read_text())
            self.assertIn("never produced", result["error"])
            self.assertEqual("NOT_REVIEWED", result["visual_verdict"])
            self.assertIn("Capture complete: False", (Path(temp) / "REVIEW_PENDING.md").read_text())


if __name__ == "__main__":
    unittest.main()
