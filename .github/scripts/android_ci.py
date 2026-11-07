#!/usr/bin/env python3
"""CI evidence and APK checks. An inventory/diagnostic is never a per-file build result."""
from __future__ import annotations

import argparse
import base64
import collections
import csv
import hashlib
import html
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import struct
import sys
from urllib.parse import unquote, urlparse
import xml.etree.ElementTree as ET
import zipfile

TECHNICAL = ('init', 'helpers', 'java', 'gradle', 'android', 'signing', 'compile', 'lint', 'tests', 'build', 'verify', 'cleanup')
BUILD_PHASES = ('compile', 'lint', 'tests', 'build')
TERMINAL_STATES = {'SUCCESS', 'FAILED', 'FROM-CACHE', 'UP-TO-DATE', 'SKIPPED', 'NO-SOURCE'}
LANGUAGES = {'.kt': 'Kotlin', '.kts': 'Kotlin', '.java': 'Java', '.c': 'C', '.cc': 'C++',
             '.cpp': 'C++', '.cxx': 'C++', '.h': 'Header', '.hpp': 'Header', '.hh': 'Header',
             '.hxx': 'Header', '.xml': 'XML'}
TASK = re.compile(r'^> Task (:\S+?)(?: (UP-TO-DATE|FROM-CACHE|SKIPPED|NO-SOURCE|FAILED))?\s*$')
LOCATION = re.compile(
    r'^(?:(?P<level>[ew]):\s*)?(?P<path>.+?\.(?:kt|kts|java|c|cc|cpp|cxx|h|hpp|hxx|xml)):'
    r'\s*(?:(?P<line>\d+):(?:(?P<col>\d+):?)?|\((?P<line2>\d+),\s*(?P<col2>\d+)\):?)\s*'
    r'(?:(?P<severity>fatal error|error|warning|note):\s*)?(?P<message>.*)$')
ANSI = re.compile(r'\x1b\[[0-?]*[ -/]*[@-~]')


def redact(text: str) -> str:
    for key in ('KEYSTORE_BASE64', 'STORE_PASSWORD', 'KEY_PASSWORD', 'LISTEN_TOGETHER_SERVER'):
        value = os.environ.get(key, '')
        if value:
            values = {value, value.strip()} - {''}
            forms = {form for v in values for form in (v, html.escape(v), html.escape(v, quote=False))}
            for form in sorted(forms, key=len, reverse=True):
                text = text.replace(form, '[REDACTED]')
    return text


def save(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=True, indent=2) + '\n', encoding='utf-8')


def git(root: Path, *args: str) -> str:
    return subprocess.check_output(['git', '-C', str(root), *args], text=True)


def file_policy(path: Path) -> dict:
    """Expected transformation, not proof that Gradle selected or packaged the file."""
    parts = path.parts
    source_set = parts[parts.index('src') + 1] if 'src' in parts and len(parts) > parts.index('src') + 1 else ''
    module = '/'.join(parts[:parts.index('src')]) if source_set else '.'
    test = source_set.lower().startswith(('test', 'androidtest'))
    sensitive = (path.name in ('local.properties', 'keystore.properties') or path.name.startswith('.env')
                 or path.suffix.lower() in ('.jks', '.keystore', '.p12', '.pem', '.key'))
    if sensitive:
        role, expectation = 'SENSITIVE_CONFIGURATION', 'CONTENT_NOT_INSPECTED'
    elif parts[0] == 'LICENSES' or str(path) in ('LICENSE', 'NOTICE'):
        role, expectation = 'LEGAL_TEXT', 'GENERATED_LICENSE_ASSET'
    elif source_set and 'assets' in parts:
        role, expectation = 'ASSET', 'COPIED_ASSET'
    elif source_set and 'res' in parts:
        role, expectation = 'ANDROID_RESOURCE', 'PROCESSED_RESOURCE'
    elif source_set and 'jniLibs' in parts:
        role, expectation = 'PREBUILT_NATIVE', 'PREBUILT_NATIVE'
    elif path.name == 'AndroidManifest.xml' and source_set:
        role, expectation = 'MANIFEST', 'MERGED_MANIFEST'
    elif path.name == 'CMakeLists.txt' or path.suffix == '.cmake':
        role, expectation = 'NATIVE_BUILD_CONFIGURATION', 'NOT_A_COMPILATION_UNIT'
    elif source_set and path.suffix in ('.kt', '.java'):
        role, expectation = 'MANAGED_SOURCE', 'TRANSLATED_TO_DEX'
    elif source_set and path.suffix.lower() in ('.c', '.cc', '.cpp', '.cxx', '.s'):
        role, expectation = 'NATIVE_SOURCE', 'LINKED_NATIVE'
    elif source_set and path.suffix in ('.h', '.hpp', '.hh', '.hxx'):
        role, expectation = 'NATIVE_HEADER', 'NATIVE_DEPENDENCY_NOT_STANDALONE'
    elif source_set:
        role, expectation = 'UNCLASSIFIED_SOURCE_INPUT', 'REVIEW_REQUIRED'
    elif parts[0] in ('docs', '.codex') or path.suffix.lower() in ('.md', '.csv'):
        role, expectation = 'DOCUMENTATION', 'NOT_AN_APP_INPUT'
    elif parts[0] == '.github':
        role, expectation = 'CI_CONFIGURATION', 'NOT_AN_APP_INPUT'
    elif parts[0] == 'gradle' or path.name in ('gradlew', 'gradlew.bat', 'gradle.properties') or path.suffix in ('.gradle', '.kts', '.pro'):
        role, expectation = 'BUILD_CONFIGURATION', 'NOT_AN_APP_INPUT'
    else:
        role, expectation = 'REPOSITORY_SUPPORT', 'NOT_AN_APP_INPUT'
    return {'module': module or '.', 'source_set': source_set,
            'kind': 'test' if test else 'production' if source_set else 'support',
            'role': role, 'artifact_expectation': expectation,
            'expectation_basis': 'PATH_POLICY; actual task/source-root evidence reported separately',
            'app_packaging_expected': False if test or expectation in ('NOT_AN_APP_INPUT', 'NOT_A_COMPILATION_UNIT', 'CONTENT_NOT_INSPECTED') else None}


def inventory(root: Path) -> list[dict]:
    records = []
    for name in sorted(set(git(root, 'ls-files', '-z').split('\0')) - {''}):
        path = Path(name)
        policy = file_policy(path)
        file = root / path
        # Do not follow symlinks, read secrets, or silently drop missing tracked entries.
        state = 'SYMLINK_NOT_FOLLOWED' if file.is_symlink() else 'FILE' if file.is_file() else 'DIRECTORY_OR_MISSING'
        digest = hashlib.sha256(file.read_bytes()).hexdigest() if state == 'FILE' and policy['role'] != 'SENSITIVE_CONFIGURATION' else None
        language = LANGUAGES.get(path.suffix.lower(), policy['role'])
        records.append({'path': name, **policy, 'language': language, 'file_state': state,
                        'sha256': digest, 'individual_compilation': 'NOT_ESTABLISHED'})
    return records


def changes_from_null(data: str) -> list[dict]:
    tokens = data.rstrip('\0').split('\0') if data else []
    result = []
    while tokens:
        status = tokens.pop(0)
        old = tokens.pop(0)
        if status.startswith(('R', 'C')):
            result.append({'status': status, 'path': tokens.pop(0), 'previous_path': old})
        else:
            result.append({'status': status, 'path': old})
    return result


def prepare(root: Path, out: Path) -> None:
    checkout = git(root, 'rev-parse', 'HEAD').strip()
    source = os.environ.get('CI_SOURCE_SHA') or checkout
    base = os.environ.get('CI_BASE_SHA', '')
    for sha in (source, base):
        if sha and not re.fullmatch(r'[0-9a-fA-F]{40}|[0-9a-fA-F]{64}', sha):
            raise ValueError('Invalid commit reference')
    subprocess.run(['git', '-C', str(root), 'merge-base', '--is-ancestor', source, checkout], check=True)
    comparison = None
    if base and set(base) != {'0'}:
        comparison = git(root, 'merge-base', base, source).strip() if os.environ.get('GITHUB_EVENT_NAME') == 'pull_request' else base
    else:
        parent = subprocess.run(['git', '-C', str(root), 'rev-parse', '--verify', source + '^'], capture_output=True, text=True)
        if parent.returncode == 0:
            comparison = parent.stdout.strip()
    if comparison:
        diff = git(root, 'diff', '--find-renames', '--name-status', '-z', comparison, source)
    else:
        diff = git(root, 'diff-tree', '--root', '--no-commit-id', '-r', '--name-status', '-z', source)
    save(out / 'source-inventory.json', inventory(root))
    save(out / 'changed-files.json', {'comparison_base': comparison, 'source_sha': source, 'files': changes_from_null(diff)})
    save(out / 'metadata.json', {'source_sha': source, 'checkout_sha': checkout,
         'checkout_tree': git(root, 'rev-parse', 'HEAD^{tree}').strip(), 'event': os.environ.get('GITHUB_EVENT_NAME', 'local'),
         'repository': os.environ.get('GITHUB_REPOSITORY'), 'run_id': os.environ.get('GITHUB_RUN_ID'),
         'run_number': os.environ.get('GITHUB_RUN_NUMBER'), 'python': platform.python_version()})
    print(f'Source {source}; checkout {checkout}. Inventory is not individual compilation evidence.')


def signing(out: Path) -> None:
    key = Path(os.environ['RUNNER_TEMP']) / 'aurelis-ci-release.jks'
    payload = os.environ.get('KEYSTORE_BASE64', '').strip()
    mode = 'debug_fallback'
    certificate = None
    if payload:
        credentials = {k: os.environ.get(k, '').strip() for k in ('STORE_PASSWORD', 'KEY_ALIAS', 'KEY_PASSWORD')}
        if not all(credentials.values()):
            raise ValueError('Keystore configured but signing credentials incomplete; no silent signature fallback')
        decoded = base64.b64decode(''.join(payload.split()), validate=True)
        if not decoded:
            raise ValueError('Empty keystore')
        fd = os.open(key, os.O_WRONLY | os.O_CREAT | os.O_TRUNC | os.O_NOFOLLOW, 0o600)
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, 'wb') as handle:
            handle.write(decoded)
        check = subprocess.run(['keytool', '-J-Duser.language=en', '-list', '-v', '-keystore', str(key),
            '-storepass:env', 'STORE_PASSWORD', '-alias', credentials['KEY_ALIAS']],
            capture_output=True, text=True, env={**os.environ, **credentials})
        fingerprint = re.search(r'SHA256:\s*([A-Fa-f0-9:]{64,95})', check.stdout)
        if check.returncode or not fingerprint:
            raise ValueError('Configured keystore/alias could not be validated; signing will not silently fall back')
        certificate = fingerprint[1].replace(':', '').lower()
        mode = 'configured_release'
    else:
        key.unlink(missing_ok=True)
        print('::warning::No keystore configured; existing Gradle debug-signature fallback retained.')
    with open(os.environ['GITHUB_ENV'], 'a', encoding='utf-8') as handle:
        handle.write(f'STORE_FILE={key}\n')
    save(out / 'signing.json', {'mode': mode, 'private_key_published': False, 'configured_certificate_sha256': certificate})


def relative_path(value: str, root: Path) -> str:
    value = unquote(urlparse(value).path) if value.startswith('file://') else value
    p = Path(value)
    if p.is_absolute():
        try:
            return str(p.relative_to(root))
        except ValueError:
            return str(p)
    return str(p)


def parse_log(text: str, phase: str, root: Path) -> tuple[list, list]:
    tasks, diagnostics, conflicts = {}, [], set()
    for line in ANSI.sub('', text).splitlines():
        match = TASK.match(line)
        if match:
            previous = tasks.get(match[1], {})
            if previous.get('terminal_result_observed'):
                if match[2] == 'FAILED' and previous['state'] != 'FAILED':
                    conflicts.add(match[1])
            elif previous.get('state') != 'FAILED':
                tasks[match[1]] = {'phase': phase, 'task': match[1], 'state': match[2] or 'EXECUTED'}
        if line.startswith('AURELIS_TASK_RESULT '):
            try:
                value = json.loads(line.split(' ', 1)[1])
                if isinstance(value.get('task'), str) and value.get('state') in TERMINAL_STATES:
                    previous = tasks.get(value['task'], {})
                    if ((previous.get('terminal_result_observed') and previous['state'] != value['state'])
                            or (previous.get('state') == 'FAILED' and value['state'] != 'FAILED')):
                        conflicts.add(value['task'])
                    tasks[value['task']] = {'phase': phase, 'task': value['task'], 'state': value['state'], 'terminal_result_observed': True}
            except (ValueError, TypeError, AttributeError):
                pass  # parse_inputs records malformed observations as collector errors.
        line = re.sub(r'^C/C\+\+:\s*', '', line)
        match = LOCATION.match(line)
        if match:
            data = match.groupdict()
            severity = data['severity'] or {'e': 'error', 'w': 'warning'}.get(data['level'])
            if severity:
                diagnostics.append({'phase': phase, 'severity': 'error' if severity == 'fatal error' else severity,
                    'file': relative_path(data['path'], root), 'line': int(data['line'] or data['line2']),
                    'column': int(data['col'] or data['col2']) if data['col'] or data['col2'] else None,
                    'message': redact(data['message'])})
        elif re.match(r'^(e: |error:|fatal error:|FAILURE: Build (?:failed|completed with \d+ failures))', line):
            diagnostics.append({'phase': phase, 'severity': 'error', 'file': None, 'line': None, 'column': None, 'message': redact(line)})
    for task in conflicts:
        tasks[task].update(state='CONFLICTING_RESULTS', terminal_result_observed=False, result_conflict=True)
    return list(tasks.values()), diagnostics


def lint_diagnostics(path: Path, root: Path, variant: str) -> list:
    result = []
    for issue in ET.parse(path).getroot().findall('issue'):
        severity = issue.get('severity', '').lower()
        severity = 'error' if severity == 'fatal' else severity
        for loc in issue.findall('location') or [None]:
            result.append({'phase': 'lint/' + variant, 'severity': severity, 'id': issue.get('id'),
                'file': relative_path(loc.get('file', ''), root) if loc is not None else None,
                'line': int(loc.get('line')) if loc is not None and loc.get('line') else None,
                'column': int(loc.get('column')) if loc is not None and loc.get('column') else None,
                'message': redact(issue.get('message', ''))})
    return result


def phases() -> dict:
    raw = json.loads(os.environ.get('CI_STEPS_JSON', '{}'))
    return {key: raw.get(key, {}).get('outcome') or 'not_executed' for key in (*TECHNICAL, 'report', 'upload_report', 'upload_apk')}


def approval(states: dict, publishing: bool = False) -> bool:
    required = (*TECHNICAL, 'report', 'upload_report', 'upload_apk') if publishing else TECHNICAL
    return all(states.get(k) == 'success' for k in required)


def elf_load_segments(data: bytes, name: str) -> list[dict]:
    """Validate actual AArch64 ELF PT_LOAD alignment, independent of ZIP layout."""
    if len(data) < 64 or data[:7] != b'\x7fELF\x02\x01\x01':
        raise ValueError('Invalid ELF64 little-endian header: ' + name)
    elf_type, machine, version = struct.unpack_from('<HHI', data, 16)
    if machine != 183:
        raise ValueError('Native library is not AArch64: ' + name)
    if elf_type != 3 or version != 1:
        raise ValueError('Invalid shared-library ELF type/version: ' + name)
    phoff = struct.unpack_from('<Q', data, 32)[0]
    ehsize, phentsize, phnum = struct.unpack_from('<HHH', data, 52)
    if ehsize != 64 or phentsize != 56 or phnum == 0 or phoff < 64 or phoff + phentsize * phnum > len(data):
        raise ValueError('Invalid ELF program-header table: ' + name)
    segments = []
    for index in range(phnum):
        kind, _, offset, vaddr, _, filesz, memsz, alignment = struct.unpack_from('<IIQQQQQQ', data, phoff + index * phentsize)
        if kind != 1:  # PT_LOAD, not a section or an unrelated program header.
            continue
        if filesz > memsz or offset + filesz > len(data):
            raise ValueError('Invalid ELF LOAD bounds: ' + name)
        if alignment < 16384 or alignment & (alignment - 1) or offset % alignment != vaddr % alignment:
            raise ValueError(f'ELF LOAD segment {index} is not 16KB compatible: {name} (alignment={alignment})')
        segments.append({'index': index, 'offset': offset, 'virtual_address': vaddr,
                         'file_size': filesz, 'memory_size': memsz, 'alignment': alignment})
    if not segments:
        raise ValueError('ELF has no LOAD segments: ' + name)
    return segments


def native_libraries(archive: zipfile.ZipFile, details: dict | None = None) -> list[str]:
    libs = [n for n in archive.namelist() if n.startswith('lib/') and n.endswith('.so')]
    if not libs or {n.split('/')[1] for n in libs} != {'arm64-v8a'}:
        raise ValueError('Unexpected/missing native ABI')
    for name in libs:
        data = archive.read(name)
        segments = elf_load_segments(data, name)
        if details is not None:
            details[name] = {'sha256': hashlib.sha256(data).hexdigest(), 'load_segments': segments}
    return libs


def apk_delivery_name(version: str, variant: str) -> str:
    if variant not in ('debug', 'release') or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._+\-]{0,99}', version):
        raise ValueError('Unsupported APK version/variant for safe delivery filename')
    suffix = '-debug' if variant == 'debug' else ''
    return f'Aurelis-Music-V{version}{suffix}.apk'


def verify_apks(root: Path, out: Path, sdk: Path) -> None:
    tool = sdk / 'build-tools/35.0.0'
    results = []
    target = root / 'ci-approved-apks'
    if target.exists():
        shutil.rmtree(target)
    try:
        versions = set()
        for variant in ('debug', 'release'):
            candidates = sorted((root / f'app/build/outputs/apk/{variant}').glob('*.apk'))
            if not candidates:
                raise ValueError(f'Missing {variant} APK')
            for apk in candidates:
                signature = subprocess.run([str(tool / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)], capture_output=True, text=True)
                if signature.returncode:
                    raise ValueError(f'Invalid signature: {apk.name}')
                alignment = subprocess.run([str(tool / 'zipalign'), '-c', '-P', '16', '-v', '4', str(apk)], capture_output=True, text=True)
                if alignment.returncode:
                    raise ValueError(f'APK ZIP is not 16KB aligned: {apk.name}')
                manifest = subprocess.run([str(tool / 'aapt2'), 'dump', 'badging', str(apk)], capture_output=True, text=True, check=True).stdout
                package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']*)'", manifest)
                if not package or package[1] != 'com.music.aurelis':
                    raise ValueError(f'Unexpected application identity: {apk.name}')
                versions.add((package[2], package[3]))
                with zipfile.ZipFile(apk) as archive:
                    if archive.testzip() is not None:
                        raise ValueError(f'Corrupt ZIP: {apk.name}')
                    native_details = {}
                    libs = native_libraries(archive, native_details)
                    required_onnx = {'lib/arm64-v8a/libonnxruntime.so', 'lib/arm64-v8a/libonnxruntime4j_jni.so'}
                    if not required_onnx.issubset(libs):
                        raise ValueError('Missing ONNX Runtime native libraries: ' + apk.name)
                    legal = {'LICENSE': root / 'LICENSE', 'NOTICE': root / 'NOTICE'}
                    legal.update({str(p.relative_to(root / 'LICENSES')): p for p in (root / 'LICENSES').rglob('*') if p.is_file()})
                    for name, source in legal.items():
                        if archive.read('assets/licenses/' + name) != source.read_bytes():
                            raise ValueError(f'Legal text mismatch: {apk.name}: {name}')
                certs = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]+)', signature.stdout)
                if not certs:
                    raise ValueError(f'Missing signer metadata: {apk.name}')
                certs = [c.lower() for c in certs]
                signing_info = json.loads((out / 'signing.json').read_text())
                expected = signing_info.get('configured_certificate_sha256')
                if variant == 'release' and expected and expected not in certs:
                    raise ValueError('Release APK does not match the configured signing certificate')
                results.append({'variant': variant, 'build_file': apk.name, 'file': apk_delivery_name(package[3], variant), 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
                    'bytes': apk.stat().st_size, 'package': package[1], 'version_code': package[2], 'version_name': package[3],
                    'signature_verified': True, 'signer_certificate_sha256': certs, 'zip_verified': True,
                    'zip_16kb_alignment_verified': True, 'native_16kb_alignment_verified': True,
                    'native_elf': native_details, 'abis': ['arm64-v8a'], 'native_libraries': len(libs), 'legal_texts_verified': len(legal)})
        if len(versions) != 1:
            raise ValueError('APK variant versions do not match')
        if len({r['file'] for r in results}) != len(results):
            raise ValueError('Duplicate delivery APK name; split APKs require an explicit naming policy')
        # No partial set is offered for publication after any failed check.
        target.mkdir(exist_ok=True)
        for record in results:
            shutil.copyfile(root / f"app/build/outputs/apk/{record['variant']}/{record['build_file']}", target / record['file'])
        shutil.copyfile(out / 'metadata.json', target / 'source-metadata.json')
        save(target / 'apk-manifest.json', results)
        (target / 'SHA256SUMS.txt').write_text(''.join(f"{r['sha256']}  {r['file']}\n" for r in results))
        save(out / 'apk-verification.json', {'status': 'success', 'apks': results})
    except Exception as error:
        if target.exists():
            shutil.rmtree(target)
        save(out / 'apk-verification.json', {'status': 'failure', 'error': redact(str(error)), 'verified_before_failure': results})
        raise


def parse_inputs(text: str, phase: str, tasks: list) -> tuple[list, list]:
    states = {task['task']: task['state'] for task in tasks}
    observations = []
    errors = [f"{phase}/{task['task']}: conflicting task result observations" for task in tasks if task.get('result_conflict')]
    for line in ANSI.sub('', text).splitlines():
        if not line.startswith(('AURELIS_TASK_INPUT ', 'AURELIS_INPUT_ERROR ', 'AURELIS_TASK_RESULT ')):
            continue
        try:
            kind, payload = line.split(' ', 1)
            data = json.loads(payload)
            if (not isinstance(data, dict) or not isinstance(data.get('task'), str)
                    or not data['task'].startswith(':') or any(c.isspace() for c in data['task'])):
                raise ValueError('Invalid task observation')
            if kind == 'AURELIS_INPUT_ERROR':
                errors.append(f"{phase}/{data['task']}: task input inspection failed")
                continue
            if kind == 'AURELIS_TASK_RESULT':
                if data.get('state') not in TERMINAL_STATES:
                    raise ValueError('Invalid terminal outcome')
                continue
            path = data.get('path')
            if not isinstance(path, str) or not path or Path(path).is_absolute() or '..' in Path(path).parts:
                raise ValueError('Invalid observed input path')
            observations.append({'phase': phase, 'task': data['task'], 'path': path,
                'origin': data.get('origin', 'unknown'), 'task_state': states.get(data['task'], 'NOT_OBSERVED'),
                'evidence': 'DECLARED_TASK_INPUT; not individual compiler success'})
        except (ValueError, TypeError):
            errors.append(f'{phase}: malformed task input observation')
    unique = {json.dumps(value, sort_keys=True): value for value in observations}
    return list(unique.values()), errors


def parse_source_roots(text: str, phase: str) -> tuple[list, list]:
    roots, errors = [], []
    for line in ANSI.sub('', text).splitlines():
        if not line.startswith('AURELIS_SOURCE_ROOT '):
            continue
        try:
            value = json.loads(line.split(' ', 1)[1])
            if (not isinstance(value, dict) or not all(isinstance(value.get(k), str) and value[k] for k in ('project', 'source_set', 'kind', 'path'))
                    or Path(value['path']).is_absolute() or '..' in Path(value['path']).parts):
                raise ValueError('Invalid root')
            roots.append({**value, 'phase': phase, 'evidence': 'DECLARED_SOURCE_ROOT; not variant selection or file compilation'})
        except (ValueError, TypeError):
            errors.append(f'{phase}: malformed source root observation')
    return roots, errors


def compilation_audit(sources: list, inputs: list, tasks: list, roots: list) -> list:
    """Keep task outcomes separate from file diagnostics and unknown package/runtime status."""
    task_map = {(t['phase'], t['task']): t for t in tasks}
    by_file = collections.defaultdict(list)
    for value in inputs:
        by_file[value['path']].append(value)
    result = []
    for source in sources:
        path = source['path']
        role = source.get('role', 'UNCLASSIFIED_SOURCE_INPUT')
        for variant in ('debug', 'release'):
            # Only compiler tasks for this language/source set are compiler evidence.
            token = variant.title()
            if source.get('kind') == 'test':
                token += 'AndroidTest' if source.get('source_set', '').lower().startswith('androidtest') else 'UnitTest'
            suffix = token + ('Kotlin' if source.get('language') == 'Kotlin' else 'JavaWithJavac')
            compiler_inputs = [v for v in by_file[path] if v['task'].endswith(':compile' + suffix)]
            terminal = [task_map[(v['phase'], v['task'])] for v in compiler_inputs
                        if (v['phase'], v['task']) in task_map and task_map[(v['phase'], v['task'])].get('terminal_result_observed')]
            outcomes = {v['state'] for v in terminal}
            source_set = source.get('source_set', '')
            if role != 'MANAGED_SOURCE':
                status = 'NOT_A_STANDALONE_MANAGED_COMPILATION' if role != 'UNCLASSIFIED_SOURCE_INPUT' else 'REVIEW_REQUIRED'
            elif source.get('file_state', 'FILE') != 'FILE':
                status = 'NO_FILE_CONTENT_EVIDENCE'
            elif 'FAILED' in outcomes:
                status = 'DECLARED_INPUT_TASK_FAILED'
            elif 'SUCCESS' in outcomes:
                status = 'DECLARED_INPUT_TASK_SUCCEEDED'
            elif outcomes & {'FROM-CACHE', 'UP-TO-DATE'}:
                status = 'DECLARED_INPUT_TASK_REUSED'
            elif outcomes & {'SKIPPED', 'NO-SOURCE'}:
                status = 'DECLARED_INPUT_TASK_DID_NOT_COMPILE'
            elif compiler_inputs:
                status = 'INPUT_OBSERVED_WITHOUT_TERMINAL_RESULT'
            elif source_set in ('debug', 'release') and source_set != variant:
                status = 'OTHER_VARIANT_BY_PATH_POLICY'
            elif by_file[path]:
                status = 'OTHER_TASK_INPUT_ONLY'
            else:
                status = 'NO_TASK_INPUT_EVIDENCE'
            matching = [r for r in roots if r['source_set'] == source_set and
                        (r['path'] == '.' or path == r['path'] or path.startswith(r['path'].rstrip('/') + '/'))]
            result.append({'path': path, 'variant': variant, 'role': role, 'source_set': source_set,
                'kind': source.get('kind', 'unknown'), 'app_packaging_expected': source.get('app_packaging_expected'),
                'artifact_expectation': source.get('artifact_expectation', 'REVIEW_REQUIRED'),
                'compilation_evidence': status, 'source_root_evidence': matching,
                'compiler_task_evidence': compiler_inputs, 'observed_task_evidence': by_file[path],
                'terminal_task_evidence': terminal,
                'individual_compilation': 'NOT_ESTABLISHED',
                'apk_presence': 'NOT_EXPECTED_IN_APP_BY_POLICY' if source.get('app_packaging_expected') is False else 'NOT_AUDITED',
                'ui_integration': 'NOT_APPLICABLE_BY_POLICY' if source.get('app_packaging_expected') is False else 'NOT_AUDITED'})
    return result


def report(root: Path, out: Path, annotations: bool = False) -> int:
    out.mkdir(exist_ok=True, parents=True)
    state = phases()
    tasks, diagnostics, collector_errors, inputs, roots = [], [], [], [], []
    for phase in BUILD_PHASES:
        path = out / f'{phase}.log'
        if path.exists():
            text = path.read_text(encoding='utf-8', errors='replace')
            t, d = parse_log(text, phase, root);tasks.extend(t);diagnostics.extend(d)
            observed, errors = parse_inputs(text, phase, t);inputs.extend(observed);collector_errors.extend(errors)
            observed_roots, errors = parse_source_roots(text, phase);roots.extend(observed_roots);collector_errors.extend(errors)
            path.write_text(redact(text), encoding='utf-8')
        elif state[phase] in ('success', 'failure'):
            collector_errors.append(f'Missing log for {phase}')
    tests, test_failures = [], []
    for variant in ('debug', 'release'):
        lint_file = root / f'app/build/reports/lint-results-{variant}.xml'
        if lint_file.exists():
            try:
                diagnostics.extend(lint_diagnostics(lint_file, root, variant))
            except ET.ParseError:
                collector_errors.append(f'Invalid lint XML: {variant}')
            destination = out / f'lint-{variant}.xml'
            destination.write_text(redact(lint_file.read_text(errors='replace')), encoding='utf-8')
        elif state['lint'] == 'success':
            collector_errors.append(f'Missing lint XML: {variant}')
        xml_files = sorted((root / f'app/build/test-results/test{variant.title()}UnitTest').glob('TEST-*.xml'))
        counts = collections.Counter(tests=0, failures=0, errors=0, skipped=0)
        for path in xml_files:
            try:
                suite = ET.parse(path).getroot()
                counts.update({k: int(suite.get(k, '0')) for k in counts})
                for case in suite.findall('testcase'):
                    for failure in case.findall('failure') + case.findall('error'):
                        test_failures.append({'variant': variant, 'class': case.get('classname', suite.get('name')),
                            'test': case.get('name'), 'type': failure.get('type'),
                            'message': redact(failure.get('message', '')),
                            'stack_excerpt': redact((failure.text or '')[:4000])})
            except (ET.ParseError, ValueError):
                collector_errors.append(f'Invalid test XML: {variant}/{path.name}')
            destination = out / f'tests-{variant}' / path.name
            destination.parent.mkdir(exist_ok=True)
            destination.write_text(redact(path.read_text(errors='replace')), encoding='utf-8')
        if state['tests'] == 'success' and (counts['tests'] <= counts['skipped'] or counts['failures'] or counts['errors']):
            collector_errors.append(f'Missing/failing/fully-skipped test results despite successful task: {variant}')
        tests.append({'variant': variant, **dict(counts), 'task_state': next((t['state'] for t in tasks if t['phase'] == 'tests' and t['task'] == f':app:test{variant.title()}UnitTest'), 'NOT_OBSERVED')})
    if state['lint'] == 'success' and any(d['phase'].startswith('lint/') and d['severity'] == 'error' for d in diagnostics):
        collector_errors.append('Lint errors despite successful task')
    inv_path = out / 'source-inventory.json'
    sources = json.loads(inv_path.read_text()) if inv_path.exists() else []
    if not inv_path.exists():
        collector_errors.append('Missing source inventory')
    # Keep distinct phase/location/severity while collapsing exact repeated messages.
    diagnostics = list({json.dumps(d, sort_keys=True): d for d in diagnostics}.values())
    observed_by_file = collections.defaultdict(list)
    for item in inputs:
        observed_by_file[item['path']].append(item)
    by_file = collections.defaultdict(list)
    for d in diagnostics:
        if d['file']:
            by_file[d['file']].append(d)
    for source in sources:
        source['task_input_evidence'] = observed_by_file[source['path']]
        source['observed_task_states'] = '; '.join(sorted({f"{item['phase']}/{item['task']}={item['task_state']}" for item in source['task_input_evidence']}))
        source['task_input_state'] = 'DECLARED_TASK_INPUT_OBSERVED' if source['task_input_evidence'] else 'NO_TASK_INPUT_EVIDENCE'
        source['diagnostics'] = dict(collections.Counter(d['severity'] for d in by_file[source['path']]))
        source['diagnostic_state'] = 'ERROR_DIAGNOSTICS' if source['diagnostics'].get('error') else 'WARNING_DIAGNOSTICS' if source['diagnostics'].get('warning') else 'OTHER_DIAGNOSTICS' if source['diagnostics'] else 'NO_DIAGNOSTICS_CAPTURED'
    roots = list({json.dumps(r, sort_keys=True): r for r in roots}.values())
    audit = compilation_audit(sources, inputs, tasks, roots)
    save(out / 'compilation-audit.json', audit)
    save(out / 'source-roots.json', roots)
    save(out / 'source-diagnostics.json', sources)
    save(out / 'task-inputs.json', inputs)
    save(out / 'task-states.json', tasks)
    save(out / 'diagnostics.json', diagnostics)
    for name, fields, rows in [
        ('source-inventory', ('module', 'source_set', 'kind', 'language', 'path', 'role', 'artifact_expectation', 'file_state', 'sha256', 'individual_compilation'), sources),
        ('compilation-audit', ('path', 'variant', 'role', 'source_set', 'kind', 'app_packaging_expected', 'artifact_expectation', 'compilation_evidence', 'individual_compilation', 'apk_presence', 'ui_integration'), audit),
        ('task-states', ('phase', 'task', 'state', 'terminal_result_observed'), tasks),
        ('task-inputs', ('phase', 'task', 'path', 'origin', 'task_state', 'evidence'), inputs),
        ('file-status', ('path', 'source_set', 'diagnostic_state', 'task_input_state', 'observed_task_states', 'individual_compilation'), sources),
        ('diagnostics', ('phase', 'severity', 'file', 'line', 'column', 'message'), diagnostics),
    ]:
        with (out / (name + '.tsv')).open('w', encoding='utf-8', newline='') as handle:
            writer = csv.DictWriter(handle, fieldnames=fields, delimiter='\t', extrasaction='ignore')
            writer.writeheader();writer.writerows(rows)
    (out / 'file-status.txt').write_text(''.join(
        f"{s['diagnostic_state']} | {s['task_input_state']} | {s['path']} | individual_compilation=NOT_ESTABLISHED\n" for s in sources), encoding='utf-8')
    for level in ('error', 'warning'):
        (out / (level + 's.txt')).write_text(''.join(
            f"[{d['phase']}] {d['file'] or '(sin archivo)'}:{d['line'] or ''}:{d['column'] or ''} {d['message']}\n"
            for d in diagnostics if d['severity'] == level), encoding='utf-8')

    data = {'phases': {k: state[k] for k in TECHNICAL}, 'technical_checks_passed': approval(state) and not collector_errors,
            'compilation_audit_mode': 'OBSERVATION_ONLY; no APK or UI coverage claim',
            'compilation_audit_states': dict(collections.Counter(r['compilation_evidence'] for r in audit)),
            'source_root_observations': len(roots),
            'task_input_observations': len(inputs), 'collector_errors': collector_errors, 'unit_test_results': tests, 'test_failures': test_failures,
            'individual_file_compilation': 'NOT_ESTABLISHED; no diagnostic is not proof of compilation',
            'android_installation': 'NOT_EXECUTED', 'android_startup': 'NOT_EXECUTED', 'android_instrumentation': 'NOT_EXECUTED'}
    save(out / 'report.json', data)
    lines = ['# Aurelis Music — informe CI', '', '| Fase | Resultado |', '|---|---|']
    lines.extend(f'| {k} | {state[k]} |' for k in TECHNICAL)
    lines += ['', '**Sin diagnóstico no significa compilado.** El inventario incluye producción/pruebas, no ejecución individual.',
              'Los estados EXECUTED/FROM-CACHE/UP-TO-DATE/SKIPPED/NO-SOURCE/FAILED describen tareas por fase.',
              'EXECUTED identifica una tarea observada sin marca de reutilización; no certifica archivos individuales.', '', '## Pruebas unitarias (resultados XML)']
    lines.extend(f"- {t['variant']}: {t['tests']} casos; {t['failures']} fallos; {t['errors']} errores; {t['skipped']} omitidos; tarea {t['task_state']}." for t in tests)
    lines += ['', f'Inventario completo: {len(sources)} archivos versionados. Tareas observadas: {len(tasks)}. Diagnósticos únicos capturados: {len(diagnostics)}.',
              'Detalle completo: source-inventory.json/.tsv (todos los archivos), compilation-audit.json/.tsv (por variante), source-roots.json, source-diagnostics.json y diagnostics.json.',
              'APK/SHA-256/firma/ABI: apk-verification.json. Source SHA, checkout/merge SHA y tree: metadata.json.',
              '', '**Android real: instalación, arranque, audio, GPU y pruebas instrumentadas NO EJECUTADOS.**', '',
              'Aprobación técnica: ' + ('APROBADA (publicación separada)' if data['technical_checks_passed'] else 'NO APROBADA; consultar fases y registros.')]
    causes = [d for d in diagnostics if d['severity'] == 'error' and d.get('file')]
    lines += ['', '## Errores con ubicación (prioridad)', '']
    lines += [f"- {md(d['file'])}:{d['line'] or ''}:{d['column'] or ''}: {md(d['message'])} ({md(d['phase'])})" for d in causes[:10]]
    if not causes:
        lines += ['No se capturaron errores con ubicación; esto no equivale a éxito. Revisar errores sin archivo, fases y XML.']
    lines += ['', '## Evidencia por archivo', '',
        f'{len(inputs)} entradas declaradas observadas de tareas; ver task-inputs.json/.tsv y file-status.tsv.',
        'El estado de la tarea (incluido cache/failure) se conserva junto a la entrada. No demuestra que cada archivo se compiló.',
        'NO_TASK_INPUT_EVIDENCE significa desconocido, no excluido ni compilado. Java puede ser entrada de Kotlin solo para resolver símbolos.',
        'compilation-audit.json/.tsv separa SUCCESS terminal, reutilización, fallo y resultado desconocido por variante.',
        'Esta primera entrega observa; los faltantes no demostrados no bloquean como errores de app. APK e integración UI: NOT_AUDITED.',
        'Las raíces declaradas no prueban selección por variante. Documentación, headers, assets y pruebas no son clases de producción individuales.']
    lines += ['', '## Auditoría de compilación por variante (observación)', '', '| Variante | Evidencia | Archivos |', '|---|---|---:|']
    counts = collections.Counter((row['variant'], row['compilation_evidence']) for row in audit)
    lines += [f'| {variant} | {evidence} | {count} |' for (variant, evidence), count in sorted(counts.items())]
    verification_file = out / 'apk-verification.json'
    if verification_file.exists():
        verification = json.loads(verification_file.read_text())
        if verification.get('status') == 'success':
            lines += ['', '## APK verificados para entrega', '', '| Variante | Archivo | Versión |', '|---|---|---|']
            lines += [f"| {md(a['variant'])} | {md(a['file'])} | {md(a['version_name'])} |" for a in verification.get('apks', [])]
    if test_failures:
        lines += ['', 'Fallos de pruebas (primeros10; detalle XML/JSON completo):']
        lines += [f"- {md(f['variant'])}: {md(str(f['class']))} / {md(str(f['test']))}: {md(f['message'])}" for f in test_failures[:10]]
    if collector_errors:
        lines += ['', 'Errores de recopilación:', *('- ' + md(e) for e in collector_errors)]
    signing_file = out / 'signing.json'
    if signing_file.exists():
        lines += ['', 'Modo de firma: ' + json.loads(signing_file.read_text())['mode']]
    lines += ['', '## Inventario por módulo/lenguaje/tipo', '', '| Módulo | Lenguaje | Tipo | Entradas |', '|---|---|---|---:|']
    counts = collections.Counter((s['module'], s['language'], s['kind']) for s in sources)
    lines += [f'| {md(module)} | {md(language)} | {kind} | {count} |' for (module, language, kind), count in sorted(counts.items())]
    lines += ['', '## Tareas observadas (primeras30)', '', '| Fase | Tarea | Estado |', '|---|---|---|']
    lines += [f"| {t['phase']} | {md(t['task'])} | {t['state']} |" for t in tasks[:30]]
    lines += ['', '## Diagnósticos (primeros20; errores primero)', '', '| Fase | Nivel | Archivo:línea:columna | Mensaje |', '|---|---|---|---|']
    ordered = sorted(diagnostics, key=lambda d: d['severity'] != 'error')
    lines += [f"| {md(d['phase'])} | {md(d['severity'])} | {md(str(d['file'] or '(sin archivo)') + ':' + str(d['line'] or '') + ':' + str(d['column'] or ''))} | {md(d['message'])} |" for d in ordered[:20]]
    lines += ['', 'Listas completas en TSV/JSON y file-status.txt. No equivalen a compilación individual confirmada.']
    summary = '\n'.join(lines) + '\n'

    (out / 'summary.md').write_text(summary, encoding='utf-8')
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as handle:
            handle.write(summary)
    if annotations:
        for item in [d for d in ordered if d['severity'] in ('error', 'warning')][:30]:
            props = []
            for k, v in [('file', item['file']), ('line', item['line']), ('col', item['column'])]:
                if v is not None:
                    props.append(f'{k}=' + command_escape(str(v), property_value=True))
            print('::' + item['severity'] + (' ' + ','.join(props) if props else '') + '::' + command_escape(item['message']))
    if annotations:
        for failure in test_failures[:10]:
            print(junit_annotation(failure))
    print('Technical checks passed:', data['technical_checks_passed'])
    return 1 if collector_errors else 0


def md(value: str) -> str:
    return html.escape(value).replace('|', '&#124;').replace('`', '&#96;').replace('\n', ' ')


def command_escape(text: str, property_value: bool = False) -> str:
    text = text.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    return text.replace(',', '%2C').replace(':', '%3A') if property_value else text


def junit_annotation(failure: dict) -> str:
    title = command_escape('JUnit ' + str(failure.get('variant', 'unknown')), property_value=True)
    message = redact(f"{failure.get('class')}.{failure.get('test')} [{failure.get('type')}]: {failure.get('message', '')}")
    return '::error title=' + title + '::' + command_escape(message[:2000])


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=('prepare', 'signing', 'verify', 'report', 'gate'))
    parser.add_argument('--root', type=Path, default=Path.cwd())
    parser.add_argument('--output', type=Path, default=Path('ci-artifacts'))
    parser.add_argument('--annotations', action='store_true')
    args = parser.parse_args()
    root = args.root.resolve();out = args.output.resolve()
    if args.command == 'prepare':prepare(root, out)
    elif args.command == 'signing':signing(out)
    elif args.command == 'verify':verify_apks(root, out, Path(os.environ['ANDROID_HOME']))
    elif args.command == 'report':return report(root, out, args.annotations)
    elif args.command == 'gate':
        states = phases()
        if not approval(states, publishing=True):
            print('::error::CI no aprobado: ' + ', '.join(k + '=' + v for k, v in states.items() if v != 'success'))
            return 1
        print('All required CI phases and artifact uploads succeeded. Android runtime remains unverified.')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except Exception as error:
        print('::error::' + command_escape(redact(str(error))), file=sys.stderr)
        sys.exit(1)
