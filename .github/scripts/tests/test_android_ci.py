"""Offline fixtures for reporting/gates; never invoke Gradle or compile Android."""
import base64
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import struct
import tempfile
import types
import unittest
from unittest.mock import patch
import zipfile

SPEC = importlib.util.spec_from_file_location('android_ci', Path(__file__).resolve().parents[1] / 'android_ci.py')
ci = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(ci)


class ReportsTest(unittest.TestCase):
    def test_kotlin_java_native_and_resources_keep_file_line_and_column(self):
        text = '\n'.join([
            'e: file:///repo/app/src/main/Foo%20Bar.kt:12:8 Unresolved reference',
            '/repo/app/src/main/Foo.java:18: error: cannot find symbol',
            'C/C++: /repo/app/src/main/cpp/dsp.cpp:4:2: fatal error: missing header',
            '/repo/app/src/main/res/layout/home.xml:9: warning: invalid value',
            'w: /repo/app/src/main/Old.kt: (7, 3): deprecated',
            'FAILURE: Build failed with an exception.'])
        _, d = ci.parse_log(text, 'build', Path('/repo'))
        self.assertEqual(len(d), 6)
        self.assertEqual((d[0]['file'], d[0]['line'], d[0]['column']), ('app/src/main/Foo Bar.kt', 12, 8))
        self.assertEqual((d[1]['line'], d[1]['severity']), (18, 'error'))
        self.assertEqual((d[2]['column'], d[2]['severity']), (2, 'error'))
        self.assertEqual(d[3]['severity'], 'warning')
        self.assertEqual((d[4]['line'], d[4]['column']), (7, 3))
        self.assertIsNone(d[5]['file'])

    def test_input_evidence_retains_failed_and_cached_tasks_without_claiming_file_compilation(self):
        log = '\n'.join([
            'AURELIS_TASK_INPUT ' + json.dumps({'task': ':app:compileDebugKotlin', 'path': 'app/src/main/One.kt'}),
            '> Task :app:compileDebugKotlin FAILED',
            'AURELIS_TASK_INPUT ' + json.dumps({'task': ':app:compileReleaseKotlin', 'path': 'app/src/main/One.kt'}),
            '> Task :app:compileReleaseKotlin FROM-CACHE'])
        tasks, _ = ci.parse_log(log, 'compile', Path('/repo'))
        inputs, errors = ci.parse_inputs(log, 'compile', tasks)
        self.assertEqual(errors, [])
        self.assertEqual([v['task_state'] for v in inputs], ['FAILED', 'FROM-CACHE'])
        self.assertTrue(all('not individual compiler success' in v['evidence'] for v in inputs))

    def test_malformed_or_failed_task_input_inspection_is_reported_not_silently_ignored(self):
        for line in ['AURELIS_TASK_INPUT invalid-json',
            'AURELIS_TASK_INPUT ' + json.dumps({'task': ':app:compile', 'path': '../outside.kt'}),
            'AURELIS_INPUT_ERROR ' + json.dumps({'task': ':app:compile'})]:
            inputs, errors = ci.parse_inputs(line, 'compile', [])
            self.assertEqual(inputs, [])
            self.assertTrue(errors)

    def test_compile_failure_produces_file_evidence_and_does_not_turn_skipped_checks_green(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out=root/'reports';out.mkdir()
            ci.save(out/'source-inventory.json', [
                {'path': 'app/src/main/One.kt', 'module': 'app', 'language': 'Kotlin', 'kind': 'production', 'source_set': 'main', 'individual_compilation': 'NOT_ESTABLISHED'},
                {'path': 'app/src/main/Two.kt', 'module': 'app', 'language': 'Kotlin', 'kind': 'production', 'source_set': 'main', 'individual_compilation': 'NOT_ESTABLISHED'}])
            states={k:{'outcome':'skipped'} for k in ci.TECHNICAL};states['compile']={'outcome':'failure'}
            diagnostic='e: file:///repo/app/src/main/One.kt:384:48 maxWidth cannot be called with an implicit receiver'
            log='AURELIS_TASK_INPUT '+json.dumps({'task': ':app:compileDebugKotlin', 'path': 'app/src/main/One.kt'})
            (out/'compile.log').write_text(log+'\n> Task :app:compileDebugKotlin FAILED\n'+diagnostic+'\n'+diagnostic+'\n')
            with patch.dict(os.environ, {'CI_STEPS_JSON': json.dumps(states), 'GITHUB_STEP_SUMMARY': ''}), contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(ci.report(Path('/repo'), out), 0)
            sources=json.loads((out/'source-diagnostics.json').read_text())
            self.assertEqual(sources[0]['diagnostic_state'], 'ERROR_DIAGNOSTICS')
            self.assertEqual(sources[0]['task_input_evidence'][0]['task_state'], 'FAILED')
            self.assertEqual(sources[1]['task_input_state'], 'NO_TASK_INPUT_EVIDENCE')
            self.assertEqual(len(json.loads((out/'diagnostics.json').read_text())), 1)
            data=json.loads((out/'report.json').read_text())
            self.assertFalse(data['technical_checks_passed'])
            self.assertEqual(data['phases']['tests'], 'skipped')
            self.assertTrue((out/'file-status.tsv').exists())
            self.assertIn('maxWidth', (out/'summary.md').read_text())

    def test_multi_failure_gradle_summary_is_not_lost(self):
        _, diagnostics = ci.parse_log('FAILURE: Build completed with 2 failures.', 'tests', Path('/repo'))
        self.assertEqual(len(diagnostics), 1)
        self.assertEqual(diagnostics[0]['severity'], 'error')
        self.assertIsNone(diagnostics[0]['file'])

    def test_junit_annotation_is_identifiable_redacted_and_cannot_inject_commands(self):
        with patch.dict(os.environ, {'STORE_PASSWORD': 'private-password'}):
            annotation = ci.junit_annotation({'variant': 'debug', 'class': 'PlayerTest', 'test': 'filters',
                'type': 'AssertionError', 'message': 'private-password\n::warning::injected 100%'})
        self.assertTrue(annotation.startswith('::error title=JUnit debug::PlayerTest.filters [AssertionError]:'))
        self.assertIn('[REDACTED]%0A::warning::injected 100%25', annotation)
        self.assertNotIn('private-password', annotation)
        self.assertNotIn('\n', annotation)

    def test_task_reuse_and_late_failure_are_not_per_file_success(self):
        text = '\n'.join(['> Task :app:compileDebugKotlin', '> Task :app:compileDebugKotlin FAILED',
            '> Task :app:testDebugUnitTest FROM-CACHE', '> Task :app:mergeResources UP-TO-DATE',
            '> Task :app:compileJava NO-SOURCE', '> Task :app:assemble SKIPPED', '> Task :app:packageRelease'])
        tasks, _ = ci.parse_log(text, 'build', Path('/repo'))
        self.assertEqual([x['state'] for x in tasks], ['FAILED', 'FROM-CACHE', 'UP-TO-DATE', 'NO-SOURCE', 'SKIPPED', 'EXECUTED'])
        self.assertFalse(any('file' in x for x in tasks))

    def test_changed_files_preserve_rename_delete_spaces_and_unicode(self):
        data = 'R100\0old name.kt\0nuevo ñ.kt\0D\0deleted.hpp\0M\0main.xml\0'
        result = ci.changes_from_null(data)
        self.assertEqual(result[0], {'status': 'R100', 'path': 'nuevo ñ.kt', 'previous_path': 'old name.kt'})
        self.assertEqual(result[1]['status'], 'D')
        self.assertEqual(ci.changes_from_null(''), [])

    def test_every_required_non_success_blocks_publication(self):
        states = {k: 'success' for k in (*ci.TECHNICAL, 'report', 'upload_report', 'upload_apk')}
        self.assertTrue(ci.approval(states, publishing=True))
        for key in states:
            for status in ('failure', 'skipped', 'cancelled', 'not_executed'):
                with self.subTest(key=key, status=status):
                    self.assertFalse(ci.approval({**states, key: status}, publishing=True))

    def test_continue_on_error_conclusion_does_not_hide_failure(self):
        with patch.dict(os.environ, {'CI_STEPS_JSON': json.dumps({'lint': {'outcome': 'failure', 'conclusion': 'success'}})}):
            self.assertEqual(ci.phases()['lint'], 'failure')
            self.assertFalse(ci.approval(ci.phases()))

    def test_secrets_and_annotation_commands_are_escaped(self):
        with patch.dict(os.environ, {'STORE_PASSWORD': 'p<&secret', 'KEY_PASSWORD': 'another-private-value'}):
            text = ci.redact('p<&secret p&lt;&amp;secret another-private-value')
            self.assertNotIn('secret', text)
        value = ci.command_escape('x%\n::error,file=y', property_value=True)
        self.assertNotIn('\n', value)
        self.assertIn('%25', value)
        self.assertIn('%2C', value)

    def test_no_diagnostic_does_not_mean_compiled_and_no_tests_cannot_pass(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp);out = root / 'reports';out.mkdir()
            ci.save(out / 'source-inventory.json', [{'path': 'app/src/test/NotInRelease.kt', 'module':'app', 'language':'Kotlin', 'kind':'test', 'individual_compilation': 'NOT_ESTABLISHED'}])
            states = {k: {'outcome': 'success'} for k in ci.TECHNICAL}
            for phase in ci.BUILD_PHASES:(out / f'{phase}.log').write_text('')
            with patch.dict(os.environ, {'CI_STEPS_JSON': json.dumps(states), 'GITHUB_STEP_SUMMARY': ''}), contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(ci.report(root, out), 1)
            source = json.loads((out / 'source-diagnostics.json').read_text())[0]
            self.assertEqual(source['individual_compilation'], 'NOT_ESTABLISHED')
            self.assertEqual(source['diagnostic_state'], 'NO_DIAGNOSTICS_CAPTURED')
            report = json.loads((out / 'report.json').read_text())
            self.assertFalse(report['technical_checks_passed'])
            self.assertEqual(report['android_startup'], 'NOT_EXECUTED')

    def test_skipped_phases_remain_skipped_not_failed_compilation_or_passed_tests(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out=root/'reports';out.mkdir()
            ci.save(out/'source-inventory.json', [])
            states = {k: {'outcome': 'skipped'} for k in ci.TECHNICAL}
            states['java'] = {'outcome': 'failure'}
            with patch.dict(os.environ, {'CI_STEPS_JSON': json.dumps(states), 'GITHUB_STEP_SUMMARY': ''}), contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(ci.report(root,out),0)
            report=json.loads((out/'report.json').read_text())
            self.assertFalse(report['technical_checks_passed'])
            self.assertEqual(report['phases']['build'],'skipped')

    def test_results_require_non_skipped_cases_and_honest_cache_status(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out=root/'reports';out.mkdir()
            ci.save(out/'source-inventory.json',[{'path':'app/src/main/One.kt','module':'app','language':'Kotlin','kind':'production','individual_compilation':'NOT_ESTABLISHED'}])
            states={k:{'outcome':'success'} for k in ci.TECHNICAL}
            for phase in ci.BUILD_PHASES:(out/f'{phase}.log').write_text('')
            (out/'tests.log').write_text('> Task :app:testDebugUnitTest FROM-CACHE\n> Task :app:testReleaseUnitTest\n')
            for variant in ('debug','release'):
                lint=root/f'app/build/reports/lint-results-{variant}.xml';lint.parent.mkdir(parents=True,exist_ok=True);lint.write_text('<issues/>')
                result=root/f'app/build/test-results/test{variant.title()}UnitTest/TEST-unit.xml';result.parent.mkdir(parents=True);result.write_text('<testsuite tests="2" failures="0" errors="0" skipped="0"/>')
            with patch.dict(os.environ,{'CI_STEPS_JSON':json.dumps(states),'GITHUB_STEP_SUMMARY':''}),contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(ci.report(root,out),0)
            data=json.loads((out/'report.json').read_text())
            self.assertTrue(data['technical_checks_passed'])
            self.assertEqual(data['unit_test_results'][0]['task_state'],'FROM-CACHE')
            self.assertEqual(data['unit_test_results'][1]['task_state'],'EXECUTED')
            self.assertEqual(json.loads((out/'source-diagnostics.json').read_text())[0]['individual_compilation'],'NOT_ESTABLISHED')
            self.assertTrue((out/'task-states.tsv').exists())
            for variant in ('debug','release'):
                result=root/f'app/build/test-results/test{variant.title()}UnitTest/TEST-unit.xml'
                result.write_text('<testsuite tests="2" failures="0" errors="0" skipped="2"/>')
            with patch.dict(os.environ,{'CI_STEPS_JSON':json.dumps(states),'GITHUB_STEP_SUMMARY':''}),contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(ci.report(root,out),1)
            self.assertFalse(json.loads((out/'report.json').read_text())['technical_checks_passed'])


    def test_junit_failures_are_diagnosed_separately_from_compiler_errors(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out=root/'reports';out.mkdir()
            ci.save(out/'source-inventory.json',[])
            states={k:{'outcome':'success'} for k in ci.TECHNICAL};states['tests']={'outcome':'failure'}
            for phase in ci.BUILD_PHASES:(out/f'{phase}.log').write_text('')
            for variant in ('debug','release'):
                lint=root/f'app/build/reports/lint-results-{variant}.xml';lint.parent.mkdir(parents=True,exist_ok=True);lint.write_text('<issues/>')
                f=root/f'app/build/test-results/test{variant.title()}UnitTest/TEST-unit.xml';f.parent.mkdir(parents=True)
                f.write_text('<testsuite tests="1" failures="1"><testcase name="attaches" classname="GlassTest"><failure type="IllegalStateException" message="Size is unspecified">frame1</failure></testcase></testsuite>')
            with patch.dict(os.environ,{'CI_STEPS_JSON':json.dumps(states),'GITHUB_STEP_SUMMARY':''}),contextlib.redirect_stdout(io.StringIO()) as console:
                self.assertEqual(ci.report(root,out,annotations=True),0)
            self.assertIn('::error title=JUnit debug::GlassTest.attaches', console.getvalue())
            self.assertIn('::error title=JUnit release::GlassTest.attaches', console.getvalue())
            data=json.loads((out/'report.json').read_text())
            self.assertFalse(data['technical_checks_passed'])
            self.assertEqual(data['test_failures'][0]['message'],'Size is unspecified')
            self.assertEqual(json.loads((out/'diagnostics.json').read_text()),[])

    def test_lint_xml_preserves_location_and_identifier(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);p=root/'lint.xml'
            p.write_text('<issues><issue id="MissingTranslation" severity="Warning" message="missing"><location file="app/src/main/res/values/strings.xml" line="8" column="2"/></issue></issues>')
            record=ci.lint_diagnostics(p,root,'debug')[0]
            self.assertEqual((record['line'],record['column'],record['id']),(8,2,'MissingTranslation'))

    def test_real_git_inventory_includes_headers_resources_tests_and_deletions(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            def git(*args):
                return subprocess.check_output(['git','-C',str(root),'-c','user.name=CI fixture','-c','user.email=ci@example.invalid','-c','commit.gpgsign=false',*args],text=True)
            git('init','-q')
            for name in ['app/src/main/cpp/audio.hpp','app/src/test/Foo Test.kt','app/src/main/res/layout/main.xml','app/src/main/res/drawable/icon.png']:
                p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text('fixture')
            git('add','.');git('commit','-qm','base');base=git('rev-parse','HEAD').strip()
            (root/'app/src/main/cpp/audio.hpp').unlink()
            git('add','-A');git('commit','-qm','delete');head=git('rev-parse','HEAD').strip()
            with patch.dict(os.environ, {'CI_SOURCE_SHA': head,'CI_BASE_SHA': base,'GITHUB_EVENT_NAME':'pull_request'}), contextlib.redirect_stdout(io.StringIO()):
                ci.prepare(root,root/'reports')
            inv=json.loads((root/'reports/source-inventory.json').read_text())
            self.assertEqual(len(inv),3)
            self.assertEqual(next(x for x in inv if x['path'].endswith('.kt'))['kind'],'test')
            changed=json.loads((root/'reports/changed-files.json').read_text())
            self.assertEqual(changed['files'],[{'status':'D','path':'app/src/main/cpp/audio.hpp'}])


class ApkAndSigningTest(unittest.TestCase):
    @staticmethod
    def elf(machine=183, alignment=16384):
        header=bytearray(120);header[:7]=b'\x7fELF\x02\x01\x01'
        struct.pack_into('<HHIQQQIHHHHHH',header,16,3,machine,1,0,64,0,0,64,56,1,0,0,0)
        struct.pack_into('<IIQQQQQQ',header,64,1,5,0,0,0,120,120,alignment)
        return bytes(header)

    def test_actual_elf_machine_is_checked_not_only_arm_directory_name(self):
        for machine in (183,62):
            with self.subTest(machine=machine), tempfile.TemporaryDirectory() as tmp:
                p=Path(tmp)/'libs.zip'
                with zipfile.ZipFile(p,'w') as z:z.writestr('lib/arm64-v8a/libtest.so',self.elf(machine))
                with zipfile.ZipFile(p) as z:
                    if machine==183:self.assertEqual(len(ci.native_libraries(z)),1)
                    else:
                        with self.assertRaises(ValueError):ci.native_libraries(z)

    def test_all_load_segments_require_16kb_alignment(self):
        data=bytearray(self.elf());data.extend(bytes(56))
        struct.pack_into('<H',data,56,2)
        struct.pack_into('<IIQQQQQQ',data,120,1,4,0,0,0,176,176,4096)
        with self.assertRaisesRegex(ValueError,'not 16KB compatible'):ci.elf_load_segments(bytes(data),'two-loads.so')

    def test_4kb_loads_are_rejected_even_in_the_correct_abi_directory(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)/'libs.zip'
            with zipfile.ZipFile(p,'w') as z:z.writestr('lib/arm64-v8a/libonnxruntime.so',self.elf(alignment=4096))
            with zipfile.ZipFile(p) as z:
                with self.assertRaisesRegex(ValueError,'not 16KB compatible'):ci.native_libraries(z)

    def test_16kb_and_64kb_load_alignment_are_accepted_and_reported(self):
        for alignment in (16384,65536):
            with self.subTest(alignment=alignment):
                segments=ci.elf_load_segments(self.elf(alignment=alignment),'compatible.so')
                self.assertEqual([s['alignment'] for s in segments],[alignment])

    def test_missing_or_truncated_program_headers_cannot_pass(self):
        for change in ('truncated','offset','entry-size','count','no-load','bounds'):
            data=bytearray(self.elf())
            if change=='truncated':data=data[:30]
            elif change=='offset':struct.pack_into('<Q',data,32,4096)
            elif change=='entry-size':struct.pack_into('<H',data,54,8)
            elif change=='count':struct.pack_into('<H',data,56,0)
            elif change=='no-load':struct.pack_into('<I',data,64,0)
            else:struct.pack_into('<Q',data,96,121)
            with self.subTest(change=change),self.assertRaises(ValueError):ci.elf_load_segments(bytes(data),'invalid.so')

    def test_misaligned_offset_and_non_power_of_two_alignment_cannot_pass(self):
        for change in ('offset','alignment'):
            data=bytearray(self.elf())
            if change=='offset':struct.pack_into('<Q',data,80,1)
            else:struct.pack_into('<Q',data,112,24576)
            with self.subTest(change=change),self.assertRaisesRegex(ValueError,'not 16KB compatible'):
                ci.elf_load_segments(bytes(data),'invalid-alignment.so')

    def fixture(self,root):
        out=root/'reports';out.mkdir()
        ci.save(out/'metadata.json',{'source_sha':'a'*40})
        ci.save(out/'signing.json',{'mode':'debug_fallback'})
        for variant in ('debug','release'):
            p=root/f'app/build/outputs/apk/{variant}/app-{variant}.apk';p.parent.mkdir(parents=True)
            with zipfile.ZipFile(p,'w') as z:
                z.writestr('lib/arm64-v8a/libtest.so',self.elf())
                z.writestr('lib/arm64-v8a/libonnxruntime.so',self.elf())
                z.writestr('lib/arm64-v8a/libonnxruntime4j_jni.so',self.elf())
                for name in ('LICENSE','NOTICE'):
                    (root/name).write_text(name);z.writestr('assets/licenses/'+name,name)
        return out

    @staticmethod
    def tool(args,**kwargs):
        if str(args[0]).endswith('apksigner'):
            return types.SimpleNamespace(returncode=0,stdout='Signer #1 certificate SHA-256 digest: '+'a'*64,stderr='')
        return types.SimpleNamespace(returncode=0,stdout="package: name='com.music.aurelis' versionCode='1' versionName='1.9'",stderr='')

    def test_verified_apks_have_exact_hashes_source_metadata_and_manifest(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out=self.fixture(root)
            with patch.object(ci.subprocess,'run',side_effect=self.tool):ci.verify_apks(root,out,root/'sdk')
            manifest=json.loads((out/'apk-verification.json').read_text())
            self.assertEqual(manifest['status'],'success');self.assertEqual(len(manifest['apks']),2)
            for apk in manifest['apks']:
                self.assertTrue(apk['zip_16kb_alignment_verified']);self.assertTrue(apk['native_16kb_alignment_verified'])
                self.assertEqual(apk['native_elf']['lib/arm64-v8a/libtest.so']['load_segments'][0]['alignment'],16384)
            self.assertTrue((root/'ci-approved-apks/source-metadata.json').is_file())
            self.assertEqual(len((root/'ci-approved-apks/SHA256SUMS.txt').read_text().splitlines()),2)

    def test_missing_onnx_jni_library_blocks_publication(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out=self.fixture(root)
            p=root/'app/build/outputs/apk/debug/app-debug.apk'
            with zipfile.ZipFile(p) as z:entries={n:z.read(n) for n in z.namelist() if not n.endswith('libonnxruntime4j_jni.so')}
            with zipfile.ZipFile(p,'w') as z:
                for name,data in entries.items():z.writestr(name,data)
            with patch.object(ci.subprocess,'run',side_effect=self.tool):
                with self.assertRaisesRegex(ValueError,'Missing ONNX Runtime native libraries'):ci.verify_apks(root,out,root/'sdk')
            self.assertFalse((root/'ci-approved-apks').exists())

    def test_zip_alignment_failure_blocks_publication(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out=self.fixture(root)
            def tool(args,**kwargs):
                if str(args[0]).endswith('zipalign'):return types.SimpleNamespace(returncode=1,stdout='',stderr='bad ZIP alignment')
                return self.tool(args,**kwargs)
            with patch.object(ci.subprocess,'run',side_effect=tool):
                with self.assertRaisesRegex(ValueError,'ZIP is not 16KB aligned'):ci.verify_apks(root,out,root/'sdk')
            self.assertFalse((root/'ci-approved-apks').exists())
            self.assertEqual(json.loads((out/'apk-verification.json').read_text())['status'],'failure')

    def test_elf_alignment_failure_cannot_publish_partial_or_stale_apks(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out=self.fixture(root)
            p=root/'app/build/outputs/apk/release/app-release.apk'
            with zipfile.ZipFile(p,'w') as z:z.writestr('lib/arm64-v8a/libonnxruntime.so',self.elf(alignment=4096))
            target=root/'ci-approved-apks';target.mkdir();(target/'stale.apk').write_text('old')
            with patch.object(ci.subprocess,'run',side_effect=self.tool):
                with self.assertRaisesRegex(ValueError,'not 16KB compatible'):ci.verify_apks(root,out,root/'sdk')
            self.assertFalse(target.exists())
            result=json.loads((out/'apk-verification.json').read_text())
            self.assertEqual(result['status'],'failure');self.assertEqual(len(result['verified_before_failure']),1)

    def test_wrong_signer_cannot_publish_partial_or_stale_apks(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out=self.fixture(root)
            ci.save(out/'signing.json',{'mode':'configured_release','configured_certificate_sha256':'b'*64})
            target=root/'ci-approved-apks';target.mkdir();(target/'stale.apk').write_text('old')
            with patch.object(ci.subprocess,'run',side_effect=self.tool):
                with self.assertRaises(ValueError):ci.verify_apks(root,out,root/'sdk')
            self.assertFalse(target.exists())
            self.assertEqual(json.loads((out/'apk-verification.json').read_text())['status'],'failure')

    def test_missing_keystore_retains_existing_fallback_without_creating_private_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            with patch.dict(os.environ,{'RUNNER_TEMP':tmp,'GITHUB_ENV':str(root/'env'),'KEYSTORE_BASE64':''}), contextlib.redirect_stdout(io.StringIO()):ci.signing(root)
            self.assertEqual(json.loads((root/'signing.json').read_text())['mode'],'debug_fallback')
            self.assertFalse((root/'aurelis-ci-release.jks').exists())

    def test_configured_signing_keeps_private_bytes_private_and_records_public_cert(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);public=':'.join(['AB']*32)
            env={'RUNNER_TEMP':tmp,'GITHUB_ENV':str(root/'env'),'KEYSTORE_BASE64':base64.b64encode(b'fixture-key').decode(),
                 'STORE_PASSWORD':' private-fixture-pass\n','KEY_ALIAS':' fixture-alias ','KEY_PASSWORD':'private-key-pass '}
            tool=types.SimpleNamespace(returncode=0,stdout='SHA256: '+public,stderr='')
            capture=io.StringIO()
            with patch.dict(os.environ,env),patch.object(ci.subprocess,'run',return_value=tool) as mocked,contextlib.redirect_stdout(capture):ci.signing(root)
            self.assertEqual(mocked.call_args.kwargs['env']['STORE_PASSWORD'],'private-fixture-pass')
            self.assertEqual(mocked.call_args.args[0][-1],'fixture-alias')
            data=json.loads((root/'signing.json').read_text())
            self.assertEqual(data['configured_certificate_sha256'],'ab'*32)
            self.assertEqual((root/'aurelis-ci-release.jks').stat().st_mode & 0o777,0o600)
            self.assertNotIn('private',capture.getvalue())

    def test_incomplete_signing_never_silently_falls_back(self):
        with tempfile.TemporaryDirectory() as tmp:
            with patch.dict(os.environ,{'RUNNER_TEMP':tmp,'GITHUB_ENV':str(Path(tmp)/'env'),'KEYSTORE_BASE64':base64.b64encode(b'fixture').decode(),'STORE_PASSWORD':'','KEY_ALIAS':'','KEY_PASSWORD':''}):
                with self.assertRaises(ValueError):ci.signing(Path(tmp))


if __name__=='__main__':unittest.main()
