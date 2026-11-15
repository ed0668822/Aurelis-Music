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
    def test_static_scenario_stops_before_rotation_and_resets_both_apps_equally(self):
        for app in parity.PACKAGES:
            calls = []
            with patch.object(parity, 'shell', side_effect=lambda *args: calls.append(args)), \
                 patch.object(parity, 'seed_preferences', side_effect=lambda app, theme: calls.append(('seed',app,theme)) or {'theme':theme}), \
                 patch.object(parity, 'launch', side_effect=lambda package: calls.append(('launch',package))):
                self.assertEqual({'theme':'dark','rotation':1},parity.launch_scenario(app,'dark',1))
            package=parity.PACKAGES[app]
            self.assertEqual([('am','force-stop',package),('settings','put','system','user_rotation','1'),
                              ('seed',app,'dark'),('launch',package)],calls)
        with patch.object(parity,'shell') as shell:
            with self.assertRaises(ValueError):parity.launch_scenario('aurelis','dark',3)
            shell.assert_not_called()

    def test_player_expansion_requires_paused_session_and_visible_title_and_controls(self):
        with patch.object(parity,'tree') as tree:
            self.assertFalse(parity.expand_fixture(False))
            tree.assert_not_called()
        controls=parity.nodes('<hierarchy><node text="Lyrics" bounds="[1,1][20,20]"/></hierarchy>')
        with patch.object(parity,'tap',return_value=True),patch.object(parity,'inspect_until',return_value=controls):
            self.assertFalse(parity.expand_fixture(True))
            controls+=parity.nodes('<hierarchy><node text="Parity Track 01" bounds="[1,21][50,40]"/></hierarchy>')
            self.assertTrue(parity.expand_fixture(True))

    def setUp(self):
        # Reports made from synthetic fixtures must never pollute the real CI summary.
        self.enterContext(patch.dict(os.environ))
        os.environ.pop("GITHUB_STEP_SUMMARY", None)
        self.enterContext(patch.object(parity, "SYSTEM_DIALOG_EVENTS", []))

    def error_dialog(self, title="Pixel Launcher isn't responding", bounds="[71,916][1009,1048]", enabled="true"):
        return f'''<hierarchy><node package="android" resource-id="android:id/alertTitle"
            text="{title}" bounds="[137,808][943,875]"/>
            <node package="android" resource-id="android:id/aerr_close" text="Close app"
            enabled="{enabled}" bounds="{bounds}"/></hierarchy>'''

    def test_launcher_recovery_retains_original_evidence_and_rechecks_tree(self):
        original = self.error_dialog()
        recovered = '<hierarchy><node package="com.music.bitchord" text="Home"/></hierarchy>'
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, "OUT", Path(temp)), \
             patch.object(parity, "raw_tree", side_effect=[original, recovered]), \
             patch.object(parity, "screenshot", return_value=[1080, 1920]) as screenshot, \
             patch.object(parity, "shell", return_value="com.google.android.apps.nexuslauncher/.NexusLauncherActivity") as shell, \
             patch.object(parity.time, "sleep"):
            self.assertEqual(recovered, parity.tree())
            self.assertEqual(original, (Path(temp) / 'system-dialogs/1/hierarchy.xml').read_text())
            screenshot.assert_called_once_with(Path(temp) / 'system-dialogs/1')
            shell.assert_any_call("input", "tap", 540, 982)
            events = json.loads((Path(temp) / 'system-dialog-events.json').read_text())
            self.assertEqual("close_verified_launcher_anr", events[0]["action"])
            self.assertTrue(events[0]["resolved"])

    def test_tested_app_and_unknown_system_errors_fail_without_dismissing(self):
        for title in ("Aurelis Music isn't responding", "BitChord keeps stopping", "System UI isn't responding"):
            with self.subTest(title=title), tempfile.TemporaryDirectory() as temp, patch.object(parity, "OUT", Path(temp)), \
                 patch.object(parity, "raw_tree", return_value=self.error_dialog(title)), \
                 patch.object(parity, "screenshot", return_value=[1080, 1920]), patch.object(parity, "shell") as shell:
                with self.assertRaisesRegex(RuntimeError, "Blocking system error"):
                    parity.tree()
                shell.assert_not_called()
                self.assertTrue((Path(temp) / "system-dialog-events.json").is_file())

    def test_launcher_recovery_requires_owner_and_enabled_valid_bounds(self):
        for owner, bounds, enabled in [("other.launcher/.Home", "[0,0][10,10]", "true"),
                                      ("com.google.android.apps.nexuslauncher/.Home", "[0,0][0,0]", "true"),
                                      ("com.google.android.apps.nexuslauncher/.Home", "[0,0][10,10]", "false")]:
            with self.subTest(owner=owner, bounds=bounds, enabled=enabled), tempfile.TemporaryDirectory() as temp, \
                 patch.object(parity, "OUT", Path(temp)), patch.object(parity, "SYSTEM_DIALOG_EVENTS", []), \
                 patch.object(parity, "raw_tree", return_value=self.error_dialog(bounds=bounds, enabled=enabled)), \
                 patch.object(parity, "screenshot"), patch.object(parity, "shell", return_value=owner) as shell:
                with self.assertRaises(RuntimeError): parity.tree()
                self.assertFalse(any(call.args[:2] == ("input", "tap") for call in shell.call_args_list))

    def test_recurring_or_persistent_launcher_error_still_fails(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, "OUT", Path(temp)), \
             patch.object(parity, "raw_tree", return_value=self.error_dialog()), \
             patch.object(parity, "screenshot"), \
             patch.object(parity, "shell", return_value="com.google.android.apps.nexuslauncher/.Home") as shell, \
             patch.object(parity.time, "sleep"):
            with self.assertRaisesRegex(RuntimeError, "remains after"): parity.tree()
            self.assertFalse(parity.SYSTEM_DIALOG_EVENTS[0]["resolved"])
            with self.assertRaisesRegex(RuntimeError, "recurred"): parity.tree()
            self.assertEqual(1, sum(call.args[:2] == ("input", "tap") for call in shell.call_args_list))

    def test_timeout_preserves_partial_device_output(self):
        import subprocess
        error = subprocess.TimeoutExpired(["adb"], 60, output=b"waiting for accessibility", stderr=b"partial error")
        with patch.object(parity.subprocess, "run", side_effect=error):
            with self.assertRaisesRegex(RuntimeError, "waiting for accessibility; stderr=partial error"):
                parity.adb("shell", "uiautomator", "dump")

    def test_material_modes_seed_identical_reduced_motion_in_both_apps(self):
        def response(*args, **kwargs):
            if args[:2] == ("pm", "clear"): return "Success"
            if args[:3] == ("cmd", "locale", "get-app-locales"): return "Locales are [en]"
            return ""
        for mode in ("plain", "glass"):
            with patch.dict(os.environ, {"UI_COMPARISON_MATERIAL": mode}), patch.object(parity, "shell", side_effect=response) as shell:
                for app in parity.PACKAGES:
                    prefs = parity.seed_preferences(app, "dark")
                    self.assertEqual(mode == "glass", prefs["liquid_glass"])
                    self.assertTrue(prefs["reduce_animation"])
                bodies = [call.kwargs["input_data"] for call in shell.call_args_list if "input_data" in call.kwargs]
                self.assertEqual(2, len(bodies))
                self.assertTrue(all(b'name="reduce_animation" value="true"' in body for body in bodies))
        with patch.dict(os.environ, {"UI_COMPARISON_MATERIAL": "unknown"}), patch.object(parity, "shell") as shell:
            with self.assertRaises(ValueError): parity.seed_preferences("aurelis", "dark")
            shell.assert_not_called()

    def test_adb_failure_preserves_stdout_diagnostics(self):
        import subprocess
        failed = subprocess.CompletedProcess(["adb"], 255, b"ERROR: could not get idle state.", b"")
        with patch.object(parity.subprocess, "run", return_value=failed):
            with self.assertRaisesRegex(RuntimeError, "stdout=ERROR: could not get idle state"):
                parity.adb("shell", "uiautomator", "dump")

    def test_tree_retries_idle_snapshot_error_but_not_device_disconnect(self):
        with patch.object(parity, "shell", side_effect=["", RuntimeError("ERROR: could not get idle state"), "", "dumped", "<hierarchy/>"]) as shell, patch.object(parity.time, "sleep"):
            self.assertEqual("<hierarchy/>", parity.tree())
            self.assertEqual(5, shell.call_count)
        with patch.object(parity, "shell", side_effect=RuntimeError("device offline")) as shell:
            with self.assertRaisesRegex(RuntimeError, "device offline"): parity.tree()
            self.assertEqual(1, shell.call_count)

    def test_zero_exit_dump_error_is_retried_without_reading_stale_xml(self):
        with patch.object(parity, "shell", side_effect=["", "ERROR: null root node returned", "", "dumped", "<hierarchy/>"]) as shell, patch.object(parity.time, "sleep"):
            self.assertEqual("<hierarchy/>", parity.raw_tree())
            self.assertEqual(1, sum(call.args[:1] == ("cat",) for call in shell.call_args_list))
            self.assertEqual(2, sum(call.args[:2] == ("rm", "-f") for call in shell.call_args_list))

    def test_missing_snapshot_is_bounded_and_does_not_reuse_an_old_file(self):
        missing = RuntimeError('cat: /sdcard/ui-parity.xml: No such file or directory')
        with patch.object(parity, 'shell', side_effect=['', '', missing, '', 'dumped', '<hierarchy/>']) as shell, patch.object(parity.time, 'sleep'):
            self.assertEqual('<hierarchy/>', parity.raw_tree())
            self.assertEqual(2, sum(call.args[:2] == ('rm', '-f') for call in shell.call_args_list))
        with patch.object(parity, 'shell', side_effect=['', '', missing] * 3) as shell, patch.object(parity.time, 'sleep'):
            with self.assertRaisesRegex(RuntimeError, 'No such file'): parity.raw_tree()
            self.assertEqual(3, sum(call.args[:2] == ('rm', '-f') for call in shell.call_args_list))

    def test_settings_runner_rejects_runner_failure_even_when_adb_exits_zero(self):
        import subprocess
        failed = subprocess.CompletedProcess([], 0, 'INSTRUMENTATION_FAILED: Process crashed\n', '')
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, "OUT", Path(temp)), \
             patch.object(parity, "verify_apk"), patch.object(parity, "adb"), patch.object(parity, "shell", return_value=""), \
             patch.object(parity.subprocess, "run", return_value=failed), redirect_stdout(io.StringIO()):
            self.assertEqual(1, parity.settings_navigation())
            result = json.loads((Path(temp) / 'settings-navigation.json').read_text())
            self.assertFalse(result['passed'])
            self.assertEqual(2, len(result['cases']))
            self.assertTrue(all('ANDROID_RUNNER_REPORTED_FAILURE' in row['problems'] for row in result['cases']))

    def test_expand_uses_the_transport_band_even_if_a_library_title_is_lower(self):
        xml = '''<hierarchy>
          <node text="Parity Track 01" bounds="[200,1600][600,1700]"/>
          <node content-desc="Play" bounds="[700,1600][800,1700]"/>
          <node content-desc="Next" bounds="[850,1600][950,1700]"/>
          <node text="Parity Track 01" bounds="[200,1847][600,1905]"/>
        </hierarchy>'''
        with patch.object(parity, "tree", return_value=xml), patch.object(parity, "shell") as shell, patch.object(parity.time, "sleep"):
            self.assertTrue(parity.expand_miniplayer())
            shell.assert_called_once_with("input", "tap", 400, 1650)

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
        with patch.dict(os.environ, {"UI_COMPARISON_MATERIAL": "glass"}), patch.object(parity, "shell", side_effect=response) as shell:
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
        focus = "mCurrentFocus=Window{123 u0 com.music.aurelis/.MainActivity}"
        resumed = "mResumedActivity com.music.aurelis/.MainActivity"
        with patch.object(parity, "shell", side_effect=["ActivityRecord com.music.aurelis/.MainActivity\nmResumedActivity com.other/.Main", focus]):
            self.assertFalse(parity.foreground("com.music.aurelis"))
        with patch.object(parity, "shell", side_effect=[resumed, focus]):
            self.assertTrue(parity.foreground("com.music.aurelis"))
        for blocked in ("mCurrentFocus=Window{123 u0 Application Not Responding: com.google.android.apps.nexuslauncher}",
                        "mFocusedApp=ActivityRecord com.music.aurelis/.MainActivity", "mCurrentFocus=null"):
            with patch.object(parity, "shell", side_effect=[resumed, blocked]):
                self.assertFalse(parity.foreground("com.music.aurelis"))

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
