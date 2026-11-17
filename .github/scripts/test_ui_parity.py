# Aurelis © 2026. GPL-3.0-or-later
"""Evidence integrity and navigation selectors; no Android success is inferred."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile
import ui_parity as parity
import ui_parity_host as host
import ui_parity_handoff as external
import ci14_handoff as handoff
import os
import io
from contextlib import redirect_stdout


class ParityEvidenceTest(unittest.TestCase):
    def test_audio_startup_fixture_preserves_metadata_and_replaces_only_welcome(self):
        existing = ('<map><int name="last_version" value="42"/><string name="other">kept &amp; safe</string>'
                    '<boolean name="local_onboarding_complete" value="false"/>'
                    '<string name="local_onboarding_complete">invalid duplicate</string></map>')
        written = []
        def shell(*args, **kwargs):
            self.assertIn('com.music.aurelis', args)
            if kwargs.get('input_data') is not None:
                written.append(kwargs['input_data'])
            if args[-1] == 'shared_prefs/app_meta.xml':
                return written[-1].decode()
            return existing
        with patch.object(parity, 'shell', side_effect=shell) as adb:
            result = parity.seed_audio_startup_fixture()
        root = parity.ET.fromstring(written[0])
        self.assertEqual('42', root.find("int[@name='last_version']").get('value'))
        self.assertEqual('kept & safe', root.find("string[@name='other']").text)
        flags = root.findall("*[@name='local_onboarding_complete']")
        self.assertEqual([('boolean', 'true')], [(f.tag, f.get('value')) for f in flags])
        self.assertEqual(('am', 'force-stop', 'com.music.aurelis'), adb.call_args_list[0].args)
        self.assertEqual(5, adb.call_count)
        self.assertTrue(result['welcome_completed'])
        for key in ('welcome_ui_tested', 'motion_settings_changed', 'audio_policy_changed'):
            self.assertFalse(result[key])

    def test_audio_startup_fixture_supports_missing_metadata_without_clearing_app_data(self):
        writes = []
        def shell(*args, **kwargs):
            self.assertNotIn('clear', args)
            if 'input_data' in kwargs:
                writes.append(kwargs['input_data'])
            return writes[-1].decode() if args[-1] == 'shared_prefs/app_meta.xml' else '<map/>'
        with patch.object(parity, 'shell', side_effect=shell):
            self.assertTrue(parity.seed_audio_startup_fixture()['welcome_completed'])
        self.assertEqual(1, len(parity.ET.fromstring(writes[0])))

    def test_audio_startup_fixture_does_not_overwrite_malformed_existing_metadata(self):
        for xml in ('broken XML', '<other/>'):
            with self.subTest(xml=xml), patch.object(parity, 'shell', return_value=xml) as adb:
                with self.assertRaises((ValueError, parity.ET.ParseError)):
                    parity.seed_audio_startup_fixture()
                self.assertEqual(3, adb.call_count)
                self.assertTrue(all('input_data' not in call.kwargs for call in adb.call_args_list))

    def test_audio_startup_fixture_requires_exact_persisted_boolean_readback(self):
        for observed in ('<map/>', '<map><boolean name="local_onboarding_complete" value="false"/></map>',
                         '<map><string name="local_onboarding_complete" value="true"/></map>',
                         '<map>' + '<boolean name="local_onboarding_complete" value="true"/>' * 2 + '</map>',
                         '<wrong><boolean name="local_onboarding_complete" value="true"/></wrong>', 'invalid'):
            with self.subTest(observed=observed), patch.object(parity, 'shell',
                    side_effect=['', '', '<map/>', '', observed]):
                with self.assertRaises((RuntimeError, parity.ET.ParseError)):
                    parity.seed_audio_startup_fixture()

    def test_audio_startup_fixture_propagates_device_and_write_failures(self):
        for fail_at in range(5):
            steps = ['', '', '<map/>', '', '<map/>']
            steps[fail_at] = RuntimeError('device or private preference write failed')
            with self.subTest(fail_at=fail_at), patch.object(parity, 'shell', side_effect=steps):
                with self.assertRaisesRegex(RuntimeError, 'device or private preference write failed'):
                    parity.seed_audio_startup_fixture()

    def session(self, title='Parity Track 01', state=2, actions=52, package='com.music.aurelis', queue=3):
        return f'package={package}\nactive=true\nstate=PlaybackState {{state={state}, actions={actions}}}\nmetadata: description={title}, Artist, Album\nqueueTitle=null, size={queue}\n'

    def player_xml(self, previous_enabled=True):
        return '<hierarchy>' + ''.join(
            f'<node package="com.music.aurelis" text="{label}" enabled="{enabled}" bounds="[{10+i*80},50][{70+i*80},110]"/>'
            for i, (label, enabled) in enumerate([('Parity Track 01','true'),('Play','true'),
                                                  ('Next','true'),('Previous',str(previous_enabled).lower())])) + '</hierarchy>'

    def test_uploaded_ui23_player_cannot_pass_without_visible_transport(self):
        xml = (Path(__file__).parent / 'fixtures/ui23-player-without-transport.xml').read_text()
        self.assertTrue(parity.matches(parity.nodes(xml), ('Parity Track 01',)))
        self.assertTrue(parity.matches(parity.nodes(xml), ('Ver Letras',)))
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, 'OUT', Path(temp)), \
             patch.object(parity, 'tree', return_value=xml), patch.object(parity, 'foreground', return_value=True), \
             patch.object(parity, 'screenshot', return_value=[1080,1920]), \
             patch.object(parity, 'shell', return_value=self.session()), patch.object(parity, 'exercise_transport') as exercise:
            result={'screens':[]}
            parity.record_state(result, 'aurelis', 'dark', 'player-portrait', True)
            row=result['screens'][0]
            self.assertTrue(row['panel_opened'])
            self.assertFalse(row['reached'])
            self.assertIn('transport: play',row['reason'])
            self.assertEqual('NOT_REVIEWED',row['visual_verdict'])
            exercise.assert_not_called()
            self.assertEqual(xml,(Path(temp)/'aurelis/dark/player-portrait/hierarchy.xml').read_text())

    def test_player_visibility_preserves_disabled_previous_and_checks_real_capabilities(self):
        xml=self.player_xml(previous_enabled=False)
        self.assertTrue(parity.player_transport(xml,self.session(actions=36),'com.music.aurelis',[1080,1920])['visible'])
        self.assertFalse(parity.player_transport(xml,self.session(actions=52),'com.music.aurelis',[1080,1920])['visible'])
        for bad in (xml.replace('bounds="[90,50][150,110]"','bounds="[90,1921][150,1980]"'),
                    xml.replace('text="Play"','text="Play" visible-to-user="false"'),
                    xml.replace('package="com.music.aurelis"','package="com.other"')):
            self.assertFalse(parity.player_transport(bad,self.session(actions=36),'com.music.aurelis',[1080,1920])['visible'])
        for dump in (self.session(state=3),self.session(package='com.other'),self.session(title='Other song')):
            self.assertFalse(parity.player_transport(xml,dump,'com.music.aurelis',[1080,1920])['visible'])

    def test_transport_probe_requires_real_play_pause_next_previous_session_changes(self):
        xml=self.player_xml().replace('text="Play"','text="Play" content-desc="Pause"')
        def run_probe(dumps):
            iterator=iter(dumps)
            def shell(*args):
                return next(iterator) if args==('dumpsys','media_session') else ''
            with tempfile.TemporaryDirectory() as temp, patch.object(parity,'tree',return_value=xml), \
                 patch.object(parity,'shell',side_effect=shell) as calls, patch.object(parity.time,'sleep'):
                result=parity.exercise_transport('com.music.aurelis',Path(temp))
                self.assertEqual(result,json.loads((Path(temp)/'transport-results.json').read_text()))
                self.assertFalse(any(c.args[:2]==('input','swipe') for c in calls.call_args_list))
                return result
        result=run_probe([self.session(state=3),self.session(),self.session(title='Parity Track 02'),self.session()])
        self.assertTrue(result['passed'])
        self.assertEqual(['play','pause','next','previous'],[s['step'] for s in result['steps']])
        for dump in (self.session(),self.session(state=3,package='com.other'),self.session(state=3,queue=1)):
            self.assertFalse(run_probe([dump]*6)['passed'])

    def test_failed_transport_interaction_cannot_become_a_reached_player(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity,'OUT',Path(temp)), \
             patch.object(parity,'tree',return_value=self.player_xml()), patch.object(parity,'foreground',return_value=True), \
             patch.object(parity,'screenshot',return_value=[1080,1920]), patch.object(parity,'shell',return_value=self.session()), \
             patch.object(parity,'exercise_transport',return_value={'passed':False,'steps':[]}):
            result={'screens':[]}
            parity.record_state(result,'aurelis','dark','player-portrait',True)
            self.assertFalse(result['screens'][0]['reached'])
            self.assertTrue(result['screens'][0]['transport']['visible'])

    def test_host_telemetry_is_bounded_and_never_certifies_a_cause(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(host,'command',return_value={'exit_code':0,'stdout':'sample'}):
            monitor=host.HostMonitor(temp)
            for _ in range(40): monitor.sample()
            evidence=json.loads((Path(temp)/'during-capture.json').read_text())
            self.assertEqual(36,len(evidence['samples']))
            self.assertEqual('UNCONFIRMED',evidence['cause_verdict'])
        import subprocess
        with patch.object(host.subprocess,'run',side_effect=subprocess.TimeoutExpired('adb',3)):
            self.assertEqual({'error':'TimeoutExpired'},host.command(['adb','get-state']))

    def test_host_diagnostic_failure_is_recorded_without_becoming_an_app_verdict(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(host,'command',side_effect=PermissionError):
            monitor=host.HostMonitor(temp)
            monitor.loop()
            result=json.loads((Path(temp)/'during-capture.json').read_text())
            self.assertEqual('PermissionError',result['error'])
            self.assertEqual('UNCONFIRMED',result['cause_verdict'])

    def test_host_monitor_stops_before_teardown_and_keeps_both_kernel_snapshots(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(host,'command',return_value={'exit_code':0,'stdout':'mock telemetry'}):
            monitor=host.HostMonitor(temp)
            monitor.start()
            self.assertTrue(monitor.stop()['stopped'])
            for stage in ('before','after'):
                self.assertEqual('mock telemetry',json.loads((Path(temp)/(stage+'-kernel.json')).read_text())['stdout'])

    def test_device_preparation_requires_observed_user_unlock(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, 'OUT', Path(temp)), \
             patch.object(parity, 'shell', side_effect=['', '', '', '0', 'RUNNING_UNLOCKING', 'RUNNING_UNLOCKED']) as shell, \
             patch.object(parity.time, 'sleep') as delay:
            parity.prepare_device('fixture')
            self.assertEqual([('svc', 'power', 'stayon', 'true'), ('input', 'keyevent', 'KEYCODE_WAKEUP'),
                              ('wm', 'dismiss-keyguard'), ('am', 'get-current-user'),
                              ('am', 'get-started-user-state', '0'), ('am', 'get-started-user-state', '0')],
                             [call.args for call in shell.call_args_list])
            self.assertEqual({'user': '0', 'observations': ['RUNNING_UNLOCKING', 'RUNNING_UNLOCKED']},
                             json.loads((Path(temp) / 'fixture-user-state.json').read_text()))
            delay.assert_called_once_with(1)

    def test_device_preparation_never_accepts_a_locked_or_unknown_user_state(self):
        for state in ('RUNNING_LOCKED', 'unknown'):
            with self.subTest(state=state), tempfile.TemporaryDirectory() as temp, \
                 patch.object(parity, 'OUT', Path(temp)), \
                 patch.object(parity, 'shell', side_effect=['', '', '', '0'] + [state] * 15), \
                 patch.object(parity.time, 'sleep') as delay:
                with self.assertRaisesRegex(RuntimeError, 'not RUNNING_UNLOCKED'):
                    parity.prepare_device('fixture')
                self.assertEqual(14, delay.call_count)
                self.assertEqual([state] * 15, json.loads((Path(temp) / 'fixture-user-state.json').read_text())['observations'])
        with patch.object(parity, 'shell', side_effect=['', '', '', 'not a user id']):
            with self.assertRaisesRegex(RuntimeError, 'Cannot verify emulator current user'):
                parity.prepare_device('fixture')

    def test_fixture_transfer_retains_create_error_and_verifies_original_bytes(self):
        error = RuntimeError("remote couldn't create file: Operation not permitted")
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, 'OUT', Path(temp)), \
             patch.object(parity, 'adb', side_effect=[error, 'copied']) as adb, \
             patch.object(parity, 'shell') as shell, patch.object(parity.time, 'sleep') as delay:
            path = Path(temp) / 'original.flac'
            path.write_bytes(b'original fixture bytes')
            shell.return_value = parity.digest(path) + '  /sdcard/Music/Parity/original.flac'
            parity.push_fixture(path, '/sdcard/Music/Parity/original.flac')
            self.assertEqual(2, adb.call_count)
            delay.assert_called_once_with(1)
            self.assertEqual([{'file': path.name, 'attempt': 1, 'error': str(error)}],
                             json.loads((Path(temp) / 'fixture-transfer-errors.json').read_text()))

    def test_fixture_transfer_rejects_persistent_create_error_without_verifying_or_indexing(self):
        error = RuntimeError("remote couldn't create file: Operation not permitted")
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, 'OUT', Path(temp)), \
             patch.object(parity, 'adb', side_effect=error) as adb, patch.object(parity, 'shell') as shell, \
             patch.object(parity.time, 'sleep') as delay:
            with self.assertRaisesRegex(RuntimeError, 'Operation not permitted'):
                parity.push_fixture(Path('original.flac'), '/sdcard/Music/Parity/original.flac')
            self.assertEqual(3, adb.call_count)
            self.assertEqual(2, delay.call_count)
            self.assertEqual(3, len(json.loads((Path(temp) / 'fixture-transfer-errors.json').read_text())))
            shell.assert_not_called()

    def test_fixture_transfer_never_retries_dead_device_or_differing_bytes(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, 'OUT', Path(temp)), \
             patch.object(parity, 'adb', side_effect=RuntimeError('device offline')) as adb, \
             patch.object(parity, 'shell') as shell, patch.object(parity.time, 'sleep') as delay:
            with self.assertRaisesRegex(RuntimeError, 'device offline'):
                parity.push_fixture(Path('original.flac'), '/sdcard/Music/Parity/original.flac')
            adb.assert_called_once()
            delay.assert_not_called()
            shell.assert_not_called()
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, 'adb') as adb, \
             patch.object(parity, 'shell', return_value='wrong  /sdcard/original.flac'):
            path = Path(temp) / 'original.flac'
            path.write_bytes(b'original')
            with self.assertRaisesRegex(RuntimeError, 'Fixture bytes differ'):
                parity.push_fixture(path, '/sdcard/original.flac')
            adb.assert_called_once()

    def test_offline_configuration_waits_for_observed_disconnect_and_keeps_the_dump(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, 'OUT', Path(temp)), \
             patch.object(parity, 'shell', side_effect=['Active default network: 100', 'Active default network: none']) as shell, \
             patch.object(parity.time, 'sleep') as delay:
            self.assertEqual('Active default network: none', parity.wait_offline_fixture())
            self.assertEqual(2, shell.call_count)
            delay.assert_called_once_with(1)
            self.assertEqual('Active default network: none', (Path(temp) / 'connectivity.txt').read_text())

    def test_offline_configuration_never_accepts_connected_or_unknown_state(self):
        for observed in ('Active default network: 100', 'Unknown connectivity state'):
            with self.subTest(observed=observed), tempfile.TemporaryDirectory() as temp, \
                 patch.object(parity, 'OUT', Path(temp)), patch.object(parity, 'shell', return_value=observed) as shell, \
                 patch.object(parity.time, 'sleep') as delay:
                with self.assertRaisesRegex(RuntimeError, '15 connectivity snapshots') as failure:
                    parity.wait_offline_fixture()
                self.assertIn(observed if observed.startswith('Active default network:') else 'default network status missing', str(failure.exception))
                self.assertEqual(15, shell.call_count)
                self.assertEqual(14, delay.call_count)
                self.assertEqual(observed, (Path(temp) / 'connectivity.txt').read_text())

    def test_static_scenario_stops_before_rotation_and_resets_both_apps_equally(self):
        for app in parity.PACKAGES:
            calls = []
            def shell(*args):
                calls.append(args)
                return 'lock 1' if args == ('wm', 'user-rotation') else ''
            with patch.object(parity, 'shell', side_effect=shell), \
                 patch.object(parity, 'seed_preferences', side_effect=lambda app, theme: calls.append(('seed',app,theme)) or {'theme':theme}), \
                 patch.object(parity, 'launch', side_effect=lambda package: calls.append(('launch',package))):
                self.assertEqual({'theme':'dark','rotation':1,'window_rotation_lock':'lock 1'},parity.launch_scenario(app,'dark',1))
            package=parity.PACKAGES[app]
            self.assertEqual([('am','force-stop',package),('seed',app,'dark'),
                              ('wm','fixed-to-user-rotation','enabled'),('wm','user-rotation','lock','1'),
                              ('wm','user-rotation'),('launch',package)],calls)
        with patch.object(parity,'shell') as shell:
            with self.assertRaises(ValueError):parity.launch_scenario('aurelis','dark',3)
            shell.assert_not_called()

    def test_offscreen_play_is_scrolled_into_view_before_real_session_verification(self):
        top = '<hierarchy><node scrollable="true" bounds="[20,40][220,240]"/></hierarchy>'
        play = '<hierarchy><node content-desc="Play" bounds="[80,100][160,180]"/></hierarchy>'
        pause = '<hierarchy><node content-desc="Pause" bounds="[80,180][160,240]"/></hierarchy>'
        with patch.object(parity, 'tree', side_effect=[top, play, pause]), \
             patch.object(parity, 'shell') as shell, patch.object(parity.time, 'sleep'), \
             patch.object(parity, 'wait_fixture_session', return_value=True) as session:
            self.assertTrue(parity.pause_fixture('com.music.aurelis', True))
            self.assertEqual(('input', 'swipe', 120, 190, 120, 90, 350), shell.call_args_list[0].args)
            self.assertEqual([('com.music.aurelis', (3,)), ('com.music.aurelis', (2,))],
                             [call.args for call in session.call_args_list])

    def test_landscape_expands_only_after_rotation_and_the_same_apps_paused_session(self):
        with patch.object(parity, 'shell') as shell, patch.object(parity, 'expand_fixture') as expand:
            self.assertFalse(parity.expand_landscape_fixture('com.music.aurelis', False))
            shell.assert_not_called(); expand.assert_not_called()
        for session_ok in (False, True):
            with patch.object(parity, 'shell', side_effect=['', 'lock 1']), \
                 patch.object(parity, 'wait_fixture_session', return_value=session_ok) as session, \
                 patch.object(parity, 'expand_fixture', return_value=True) as expand:
                self.assertEqual(session_ok, parity.expand_landscape_fixture('com.music.aurelis', True))
                session.assert_called_once_with('com.music.aurelis', (2,))
                self.assertEqual(int(session_ok), expand.call_count)
        with patch.object(parity, 'shell', side_effect=['', 'lock 0']), patch.object(parity, 'expand_fixture') as expand:
            with self.assertRaisesRegex(RuntimeError, 'Landscape WindowManager lock was not verified'):
                parity.expand_landscape_fixture('com.music.aurelis', True)
            expand.assert_not_called()

    def test_rotation_readback_failure_does_not_launch_or_certify_an_orientation(self):
        with patch.object(parity, 'shell', return_value='free'), \
             patch.object(parity, 'seed_preferences', return_value={}), patch.object(parity, 'launch') as launch:
            with self.assertRaisesRegex(RuntimeError, 'rotation lock was not verified'):
                parity.launch_scenario('aurelis', 'dark', 1)
            launch.assert_not_called()

    def test_local_header_requires_fixture_count_transport_and_categories(self):
        def rows(labels):
            return [{'label': label, 'labels': [label]} for label in labels]
        header = ['Local Music', 'Play', 'Shuffle', 'Artistas', 'Álbumes']
        self.assertTrue(parity.local_library_fixture(rows(header + ['3 canciones · 3 min'])))
        self.assertTrue(parity.local_library_fixture(rows(['Parity Track 01'])))
        self.assertFalse(parity.local_library_fixture(rows(header + ['2 canciones · 2 min'])))
        self.assertFalse(parity.local_library_fixture(rows(['Local Music', '3 songs'])))
        self.assertFalse(parity.local_library_fixture(rows(['Play', 'Shuffle', '3 songs'])))

    def test_player_opening_requires_paused_session_title_and_panel_marker(self):
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
        self.enterContext(redirect_stdout(io.StringIO()))
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
                bodies = [call.kwargs["input_data"] for call in shell.call_args_list
                          if "input_data" in call.kwargs and "app_meta.xml" not in str(call.args)]
                self.assertEqual(2, len(bodies))
                self.assertTrue(all(b'name="reduce_animation" value="true"' in body for body in bodies))
                welcome = [call for call in shell.call_args_list if "app_meta.xml" in str(call.args)]
                self.assertEqual(1, len(welcome))
                self.assertEqual("com.music.aurelis", welcome[0].args[1])
                self.assertEqual(b'<map><boolean name="local_onboarding_complete" value="true"/></map>',
                                 welcome[0].kwargs["input_data"])
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
             patch.dict(os.environ, UI_SETTINGS_ANDROID_API="35"), \
             patch.object(parity, "verify_apk"), patch.object(parity, "adb"), \
             patch.object(parity, "prepare_device"), patch.object(parity, "tree", return_value='<hierarchy/>'), \
             patch.object(parity, "shell", side_effect=lambda *args: "35" if args == ("getprop", "ro.build.version.sdk") else ""), \
             patch.object(parity.subprocess, "run", return_value=failed), \
             patch.object(parity, "seed_preferences") as seed, patch.object(host, "HostMonitor") as monitor, redirect_stdout(io.StringIO()):
            self.assertEqual(1, parity.settings_navigation())
            seed.assert_not_called()
            result = json.loads((Path(temp) / 'settings-navigation.json').read_text())
            self.assertFalse(result['passed'])
            self.assertEqual(2, len(result['cases']))
            self.assertTrue(all('ANDROID_RUNNER_REPORTED_FAILURE' in row['problems'] for row in result['cases']))
            monitor.return_value.start.assert_called_once()
            monitor.return_value.stop.assert_called_once()
            self.assertIn('host_monitor', result)
            stages = json.loads((Path(temp) / 'settings-navigation-stages.json').read_text())['events']
            self.assertEqual(2, sum(e['stage'] == 'instrumentation_started' for e in stages))
            self.assertEqual('failed', stages[-1]['stage'])

    def test_settings_runner_never_instruments_over_a_blocking_app_error(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, 'OUT', Path(temp)), \
             patch.dict(os.environ, UI_SETTINGS_ANDROID_API='35'), \
             patch.object(parity, 'verify_apk'), patch.object(parity, 'adb'), \
             patch.object(parity, 'shell', side_effect=lambda *args: '35' if args == ('getprop', 'ro.build.version.sdk') else ''), \
             patch.object(parity, 'prepare_device'), \
             patch.object(parity, 'tree', side_effect=RuntimeError("Blocking system error: Aurelis Music isn't responding")), \
             patch.object(parity.subprocess, 'run') as instrument, redirect_stdout(io.StringIO()):
            self.assertEqual(1, parity.settings_navigation())
            result = json.loads((Path(temp) / 'settings-navigation.json').read_text())
            self.assertFalse(result['passed'])
            self.assertEqual([], result['cases'])
            self.assertIn("Aurelis Music isn't responding", result['error'])
            instrument.assert_not_called()
            monitor.return_value.start.assert_called_once()
            monitor.return_value.stop.assert_called_once()
            stages = json.loads((Path(temp) / "settings-navigation-stages.json").read_text())["events"]
            self.assertEqual("preflight", stages[-2]["stage"])
            self.assertEqual("failed", stages[-1]["stage"])

    def test_settings_runner_rejects_wrong_android_before_install_or_instrumentation(self):
        for expected, actual in (("35", "36"), ("36", "35")):
            with self.subTest(expected=expected), tempfile.TemporaryDirectory() as temp, \
                 patch.object(parity, "OUT", Path(temp)), \
                 patch.dict(os.environ, UI_SETTINGS_ANDROID_API=expected), \
                 patch.object(parity, "shell", return_value=actual), \
                 patch.object(parity, "verify_apk") as verify, patch.object(parity, "adb") as adb, \
                 patch.object(parity.subprocess, "run") as instrument, patch.object(host, "HostMonitor") as monitor, redirect_stdout(io.StringIO()):
                self.assertEqual(1, parity.settings_navigation())
                result = json.loads((Path(temp) / "settings-navigation.json").read_text())
                self.assertEqual({"expected_api": expected, "api": actual}, result["device"])
                self.assertFalse(result["passed"])
                self.assertEqual([], result["cases"])
                self.assertIn("requires Android API " + expected, result["error"])
                verify.assert_not_called()
                adb.assert_not_called()
                instrument.assert_not_called()

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
            bodies = [call.kwargs["input_data"] for call in shell.call_args_list
                      if "input_data" in call.kwargs and "app_meta.xml" not in str(call.args)]
            self.assertEqual(4, len(bodies))
            self.assertTrue(all(b'name="liquid_glass" value="true"' in body for body in bodies))
            self.assertEqual(2, sum("app_meta.xml" in str(call.args) for call in shell.call_args_list))
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

    def test_foreground_failure_preserves_exit_diagnostics_without_becoming_reached(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(parity, "OUT", Path(temp)), \
             patch.object(parity, "tree", return_value="<hierarchy/>"), \
             patch.object(parity, "foreground", return_value=False), \
             patch.object(parity, "screenshot", return_value=[1080, 1920]), \
             patch.object(parity, "shell", side_effect=lambda *args: "APP CRASH(NATIVE)" if args[:2] == ("dumpsys", "activity") else "Fatal signal 11") as shell, \
             redirect_stdout(io.StringIO()):
            result = {"screens": []}
            parity.record_state(result, "aurelis", "dark", "search", True)
            self.assertFalse(result["screens"][0]["reached"])
            folder = Path(temp) / "aurelis/dark/search"
            self.assertEqual("APP CRASH(NATIVE)", (folder / "exit-info.txt").read_text())
            self.assertEqual("Fatal signal 11", (folder / "crash-buffer.txt").read_text())
            self.assertEqual(("dumpsys", "activity", "exit-info", "com.music.aurelis"), shell.call_args_list[0].args)

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
