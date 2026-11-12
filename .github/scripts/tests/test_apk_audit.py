"""Offline binary/policy fixtures; no Gradle, compilation or Android execution."""
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import types
import unittest
from unittest.mock import patch
import zipfile
from apk_fixture import dex, jvm, policy, write_policy, RESOURCES, MANIFEST

SPEC=importlib.util.spec_from_file_location('apk_audit',Path(__file__).resolve().parents[1]/'apk_audit.py')
audit=importlib.util.module_from_spec(SPEC);SPEC.loader.exec_module(audit)


class ContentAuditTest(unittest.TestCase):
    def fixture(self, root, names=('fixture/App',), files=None, inventory=None, selected_policy=None):
        out=root/'reports';out.mkdir();(out/'metadata.json').write_text(json.dumps({'source_sha':'a'*40,'checkout_sha':'b'*40}))
        (out/'source-inventory.json').write_text(json.dumps(inventory or []))
        write_policy(root,selected_policy)
        apk=root/'fixture.apk'
        entries={'classes.dex':dex(names),'resources.arsc':b'fixture','AndroidManifest.xml':b'fixture'}
        entries.update(files or {})
        with zipfile.ZipFile(apk,'w') as archive:
            for name,value in entries.items():archive.writestr(name,value)
        return out,apk
    def tool(self,args,**kwargs):
        return types.SimpleNamespace(stdout=RESOURCES if args[2]=='resources' else MANIFEST,returncode=0)
    def run_audit(self,root,out,apk,variant='debug'):
        with patch.object(audit.subprocess,'run',side_effect=self.tool):return audit.audit(root,out,root/'sdk',apk,variant)

    def test_minimal_required_content_is_verified_without_runtime_claims(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out,apk=self.fixture(root)
            result=self.run_audit(root,out,apk)
            self.assertEqual(result['status'],'REQUIRED_CONTENT_VERIFIED_WITH_COVERAGE_LIMITS')
            self.assertEqual(result['required_failures'],[])
            self.assertEqual(result['ui_integration'],'NOT_AUDITED')
            self.assertEqual(result['android_runtime'],'NOT_EXECUTED')
            self.assertTrue((out/'apk-content-debug.tsv').is_file())

    def test_missing_required_class_blocks_and_saves_precise_evidence(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out,apk=self.fixture(root,('fixture/Other',))
            with self.assertRaises(ValueError):self.run_audit(root,out,apk)
            result=json.loads((out/'apk-content-debug.json').read_text())
            self.assertIn('REQUIRED_CLASS_NOT_PRESENT:fixture/App',result['required_failures'])
            self.assertEqual(result['status'],'REQUIRED_CONTENT_MISSING')

    def test_r8_rename_is_present_but_usage_is_not_presence(self):
        for removed in (False,True):
            with tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp);out,apk=self.fixture(root,('a/b',))
                folder=root/'app/build/outputs/mapping/release';folder.mkdir(parents=True)
                (folder/'mapping.txt').write_text('fixture.App -> a.b:\n')
                (folder/'usage.txt').write_text('fixture.App\n')
                manifest=MANIFEST.replace('fixture.App','a.b')
                def tool(args,**kwargs):return types.SimpleNamespace(stdout=RESOURCES if args[2]=='resources' else manifest,returncode=0)
                if removed:(folder/'mapping.txt').write_text('fixture.App -> x.y:\n')
                with patch.object(audit.subprocess,'run',side_effect=tool):
                    if removed:
                        with self.assertRaises(ValueError):audit.audit(root,out,root/'sdk',apk,'release')
                        result=json.loads((out/'apk-content-release.json').read_text())
                        self.assertEqual(result['rows'][0]['state'],'R8_USAGE_REPORTED_NOT_PRESENCE')
                    else:
                        result=audit.audit(root,out,root/'sdk',apk,'release')
                        self.assertEqual(result['rows'][0]['state'],'R8_MAPPED_CLASS_PRESENT')

    def test_required_assets_fonts_and_native_libraries_cannot_be_missing_or_corrupted(self):
        for kind in ('asset','font','native'):
            with tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp);value=policy()
                if kind=='asset':value['required_assets']=['model.onnx'];file=root/'app/src/main/assets/model.onnx'
                elif kind=='font':value['byte_exact_resources']=['res/font/fixture.ttf'];file=root/'app/src/main/res/font/fixture.ttf'
                else:value['required_native_libraries']=['libfixture.so'];file=None
                if file:file.parent.mkdir(parents=True);file.write_bytes(b'original')
                files={'assets/model.onnx':b'corrupt','res/font/fixture.ttf':b'corrupt'}
                out,apk=self.fixture(root,files=files,selected_policy=value)
                with self.assertRaises(ValueError):self.run_audit(root,out,apk)
                result=json.loads((out/'apk-content-debug.json').read_text())
                self.assertTrue(result['required_failures'])

    def test_missing_resource_and_missing_manifest_component_are_distinct(self):
        for resources,manifest,expected in [(RESOURCES.replace('string/app_name','string/other'),MANIFEST,'REQUIRED_RESOURCE_NOT_PRESENT'),
                                            (RESOURCES,MANIFEST.replace('fixture.App','fixture.Other'),'REQUIRED_MANIFEST_COMPONENT_NOT_PRESENT')]:
            with tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp);out,apk=self.fixture(root)
                def tool(args,**kwargs):return types.SimpleNamespace(stdout=resources if args[2]=='resources' else manifest,returncode=0)
                with patch.object(audit.subprocess,'run',side_effect=tool),self.assertRaises(ValueError):audit.audit(root,out,root/'sdk',apk,'debug')
                result=json.loads((out/'apk-content-debug.json').read_text())
                self.assertTrue(any(v.startswith(expected) for v in result['required_failures']))

    def test_unsupported_or_malformed_evidence_is_an_auditor_error(self):
        for invalid in (b'bad DEX',dex().replace(b'039\0',b'041\0',1)):
            with tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp);out,apk=self.fixture(root,files={'classes.dex':invalid})
                with self.assertRaises(ValueError):self.run_audit(root,out,apk)
                result=json.loads((out/'apk-content-debug.json').read_text())
                self.assertEqual(result['status'],'EVIDENCE_ERROR');self.assertTrue(result['evidence_errors'])

    def test_multidex_including_classes10_is_read_and_duplicate_definitions_rejected(self):
        for duplicate in (False,True):
            with tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp);out,apk=self.fixture(root,files={'classes10.dex':dex(('fixture/App' if duplicate else 'fixture/Extra',))})
                if duplicate:
                    with self.assertRaises(ValueError):self.run_audit(root,out,apk)
                else:self.assertEqual(self.run_audit(root,out,apk)['dex_class_count'],2)

    def test_jvm_metadata_maps_source_to_present_classes_without_source_equivalence_claim(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);source=root/'app/src/main/java/fixture/App.kt';source.parent.mkdir(parents=True);source.write_text('package fixture\nclass App')
            output=root/'app/build/tmp/kotlin-classes/debug';output.mkdir(parents=True);(output/'App.class').write_bytes(jvm())
            inventory=[{'path':str(source.relative_to(root)),'kind':'production','role':'MANAGED_SOURCE','source_set':'main'}]
            out,apk=self.fixture(root,inventory=inventory)
            (out/'compile.log').write_text('AURELIS_CLASS_OUTPUT '+json.dumps({'task':':app:compileDebugKotlin','path':str(output.relative_to(root))}))
            result=self.run_audit(root,out,apk)
            row=next(r for r in result['rows'] if r['path'].endswith('App.kt'))
            self.assertEqual(row['state'],'COMPILER_CLASSES_MAPPED_TO_DEX')
            self.assertEqual(row['source_equivalence'],'NOT_PROVEN_BY_CLASS_PRESENCE')


    def test_block_and_line_comments_do_not_spoof_source_package(self):
        fixtures = (
            ('/*\npackage wrong.block\n*/\n// package wrong.line\npackage fixture\nclass App',
             'COMPILER_CLASSES_MAPPED_TO_DEX'),
            ('/* package fixture */\n// package fixture\npackage other\nclass App',
             'CLASS_PRESENCE_EVIDENCE_INCOMPLETE'),
        )
        for source_text, expected_state in fixtures:
            with self.subTest(expected_state=expected_state), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                source = root / 'app/src/main/java/fixture/App.kt'
                source.parent.mkdir(parents=True)
                source.write_text(source_text, encoding='utf-8')
                output = root / 'app/build/tmp/kotlin-classes/debug'
                output.mkdir(parents=True)
                (output / 'App.class').write_bytes(jvm())
                inventory = [{'path': str(source.relative_to(root)), 'kind': 'production',
                              'role': 'MANAGED_SOURCE', 'source_set': 'main'}]
                out, apk = self.fixture(root, inventory=inventory)
                (out / 'compile.log').write_text('AURELIS_CLASS_OUTPUT ' + json.dumps({
                    'task': ':app:compileDebugKotlin', 'path': str(output.relative_to(root))
                }))
                result = self.run_audit(root, out, apk)
                record = next(row for row in result['rows'] if row['path'].endswith('App.kt'))
                self.assertEqual(record['state'], expected_state)

    def test_absent_compiler_outputs_and_homonymous_sources_remain_incomplete(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);inventory=[]
            for folder in ('one','two'):
                file=root/f'app/src/main/java/{folder}/App.kt';file.parent.mkdir(parents=True);file.write_text('package fixture\nclass App')
                inventory.append({'path':str(file.relative_to(root)),'kind':'production','role':'MANAGED_SOURCE','source_set':'main'})
            out,apk=self.fixture(root,inventory=inventory)
            result=self.run_audit(root,out,apk)
            self.assertTrue(all(r['state']=='AMBIGUOUS_SOURCE_CLASS_MAPPING' for r in result['rows'] if r['path'].endswith('.kt')))
            self.assertEqual(result['compiler_class_count'],0)

    def test_other_abis_and_tests_are_excluded_without_missing_flags(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);path='app/src/main/jniLibs/x86_64/libfixture.so';file=root/path;file.parent.mkdir(parents=True);file.write_bytes(b'fixture')
            inventory=[{'path':path,'role':'PREBUILT_NATIVE','source_set':'main','kind':'production'},
                       {'path':'app/src/test/Foo.kt','role':'MANAGED_SOURCE','kind':'test','app_packaging_expected':False}]
            out,apk=self.fixture(root,inventory=inventory)
            result=self.run_audit(root,out,apk)
            states={r['path']:r['state'] for r in result['rows']}
            self.assertEqual(states[path],'EXCLUDED_BY_ABI_POLICY')
            self.assertEqual(states['app/src/test/Foo.kt'],'NOT_EXPECTED_IN_APP_BY_POLICY')

    def test_resource_values_are_checked_as_symbols_not_literal_xml(self):
        with tempfile.TemporaryDirectory() as tmp:
            file=Path(tmp)/'values/strings.xml';file.parent.mkdir();file.write_text('<resources><string name="app_name">Own fixture</string><declare-styleable name="Widget"/><string-array name="choices"/><style name="Theme.Fixture"/></resources>')
            self.assertEqual(audit.expected_resources(file),{'string/app_name','array/choices','style/Theme.Fixture'})
        self.assertEqual(audit.manifest_components('E: activity\n A: android:name=".Main"\n','fixture'),{'fixture/Main'})

    def test_renamed_font_path_is_resolved_through_aapt_not_declared_missing(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);file=root/'app/src/main/res/font/fixture.ttf';file.parent.mkdir(parents=True);file.write_bytes(b'original-font-fixture')
            value=policy();value['byte_exact_resources']=['res/font/fixture.ttf']
            out,apk=self.fixture(root,files={'res/a.ttf':file.read_bytes()},selected_policy=value)
            resources=RESOURCES+' resource 0x7f020001 font/fixture\n  () (file) res/a.ttf\n'
            def tool(args,**kwargs):return types.SimpleNamespace(stdout=resources if args[2]=='resources' else MANIFEST,returncode=0)
            with patch.object(audit.subprocess,'run',side_effect=tool):result=audit.audit(root,out,root/'sdk',apk,'debug')
            self.assertEqual(result['required_failures'],[])

    def test_unparseable_resource_table_is_evidence_error_not_missing_every_resource(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out,apk=self.fixture(root)
            with patch.object(audit.subprocess,'run',return_value=types.SimpleNamespace(stdout='unrecognized output',returncode=0)),self.assertRaises(ValueError):
                audit.audit(root,out,root/'sdk',apk,'debug')
            result=json.loads((out/'apk-content-debug.json').read_text())
            self.assertEqual(result['status'],'EVIDENCE_ERROR')
            self.assertFalse(any('RESOURCE_NOT_PRESENT' in v for v in result['required_failures']))

    def test_native_units_are_declared_commands_not_execution_claims(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);directory=root/'app/.cxx/Debug/hash/arm64-v8a';directory.mkdir(parents=True)
            file=directory/'compile_commands.json';file.write_text(json.dumps([{'file':str(root/'app/src/main/cpp/own.cpp'),'directory':str(root),'command':'not emitted in report'}]))
            values=audit.native_commands(root,'debug')
            self.assertEqual(values[0]['source'],'app/src/main/cpp/own.cpp')
            self.assertEqual(values[0]['evidence'],'COMPILE_COMMAND_DECLARED_NOT_EXECUTION_PROOF')
            self.assertNotIn('command',values[0])
            self.assertEqual(audit.native_commands(root,'release'),[])

    def test_manifest_accepts_actual_aapt_uri_short_prefix_and_declared_alias(self):
        uri='http://schemas.android.com/apk/res/android'
        for prefix,declaration in [(uri,''),('android',''),('a','N: a='+uri+'\n')]:
            text=declaration+'E: manifest\n E: application\n  A: '+prefix+':name(0x01010003)=".App" (Raw: ".App")\n'
            text+=' E: activity\n  A: '+prefix+':name(0x01010003)="fixture.Main"\n'
            self.assertEqual(audit.manifest_components(text,'fixture'),{'fixture/App','fixture/Main'})
        for text in ['E: application\n A: tools:name="fixture.Wrong"\n',
                     'N: android=http://unrelated.example/ns\nE: application\n A: android:name="fixture.Wrong"\n',
                     'E: uses-permission\n A: '+uri+':name="fixture.Wrong"\n']:
            self.assertEqual(audit.manifest_components(text,'fixture'),set())

    def test_auditor_exception_exposes_safe_reason_and_report_location(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out,apk=self.fixture(root,files={'classes.dex':b'invalid'})
            with self.assertRaisesRegex(ValueError,'EVIDENCE_ERROR.*Unsupported or invalid DEX evidence format.*apk-content-debug.json'):
                self.run_audit(root,out,apk)

    def test_binary_bounds_and_class_metadata_never_include_constant_values(self):
        self.assertEqual(audit.class_metadata(jvm(private_text='private-fixture-value')),('fixture/App','App.kt'))
        for data in (b'',jvm()[:14]):
            with self.assertRaises((ValueError,IndexError)):audit.class_metadata(data)
        bad=bytearray(dex());struct.pack_into('<I',bad,100,1)
        with self.assertRaises(ValueError):audit.dex_classes(bytes(bad))
