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
import sys
from urllib.parse import unquote, urlparse
import xml.etree.ElementTree as ET
import zipfile

TECHNICAL = ('init', 'helpers', 'java', 'gradle', 'android', 'signing', 'compile', 'lint', 'tests', 'build', 'verify', 'cleanup')
BUILD_PHASES = ('compile', 'lint', 'tests', 'build')
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


def inventory(root: Path) -> list[dict]:
    records = []
    for name in sorted(set(git(root, 'ls-files', '-z').split('\0'))):
        p = Path(name)
        resource = 'res' in p.parts or 'assets' in p.parts
        if 'src' not in p.parts or (p.suffix.lower() not in LANGUAGES and not resource) or not (root / p).is_file():
            continue
        index = p.parts.index('src')
        source_set = p.parts[index + 1] if len(p.parts) > index + 1 else 'unknown'
        records.append({'path': name, 'module': '/'.join(p.parts[:index]) or '.',
                        'source_set': source_set, 'kind': 'test' if source_set.lower().startswith(('test', 'androidtest')) else 'production',
                        'language': LANGUAGES.get(p.suffix.lower(), 'Asset' if 'assets' in p.parts else 'Resource'),
                        'sha256': hashlib.sha256((root / p).read_bytes()).hexdigest() if not (root / p).is_symlink() else None,
                        'individual_compilation': 'NOT_ESTABLISHED'})
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
    tasks, diagnostics = {}, []
    for line in ANSI.sub('', text).splitlines():
        match = TASK.match(line)
        if match:
            tasks[match[1]] = {'phase': phase, 'task': match[1], 'state': match[2] or 'EXECUTED'}
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
        elif re.match(r'^(e: |error:|fatal error:|FAILURE: Build failed)', line):
            diagnostics.append({'phase': phase, 'severity': 'error', 'file': None, 'line': None, 'column': None, 'message': redact(line)})
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


def native_libraries(archive: zipfile.ZipFile) -> list[str]:
    libs = [n for n in archive.namelist() if n.startswith('lib/') and n.endswith('.so')]
    if not libs or {n.split('/')[1] for n in libs} != {'arm64-v8a'}:
        raise ValueError('Unexpected/missing native ABI')
    for name in libs:
        with archive.open(name) as handle:
            header = handle.read(20)
        if len(header) < 20 or header[:4] != b'\x7fELF' or header[4] != 2 or header[5] != 1:
            raise ValueError('Invalid ELF64 header: ' + name)
        if int.from_bytes(header[18:20], 'little' if header[5] == 1 else 'big') != 183:
            raise ValueError('Native library is not AArch64: ' + name)
    return libs


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
                manifest = subprocess.run([str(tool / 'aapt2'), 'dump', 'badging', str(apk)], capture_output=True, text=True, check=True).stdout
                package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']*)'", manifest)
                if not package or package[1] != 'com.music.aurelis':
                    raise ValueError(f'Unexpected application identity: {apk.name}')
                versions.add((package[2], package[3]))
                with zipfile.ZipFile(apk) as archive:
                    if archive.testzip() is not None:
                        raise ValueError(f'Corrupt ZIP: {apk.name}')
                    libs = native_libraries(archive)
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
                results.append({'variant': variant, 'file': apk.name, 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
                    'bytes': apk.stat().st_size, 'package': package[1], 'version_code': package[2], 'version_name': package[3],
                    'signature_verified': True, 'signer_certificate_sha256': certs, 'zip_verified': True,
                    'abis': ['arm64-v8a'], 'native_libraries': len(libs), 'legal_texts_verified': len(legal)})
        if len(versions) != 1:
            raise ValueError('APK variant versions do not match')
        # No partial set is offered for publication after any failed check.
        target.mkdir(exist_ok=True)
        for record in results:
            shutil.copyfile(root / f"app/build/outputs/apk/{record['variant']}/{record['file']}", target / record['file'])
        shutil.copyfile(out / 'metadata.json', target / 'source-metadata.json')
        save(target / 'apk-manifest.json', results)
        (target / 'SHA256SUMS.txt').write_text(''.join(f"{r['sha256']}  {r['file']}\n" for r in results))
        save(out / 'apk-verification.json', {'status': 'success', 'apks': results})
    except Exception as error:
        save(out / 'apk-verification.json', {'status': 'failure', 'error': redact(str(error)), 'verified_before_failure': results})
        raise


def parse_inputs(text: str, phase: str, tasks: list) -> tuple[list, list]:
    states = {task['task']: task['state'] for task in tasks}
    observations, errors = [], []
    for line in ANSI.sub('', text).splitlines():
        if not line.startswith(('AURELIS_TASK_INPUT ', 'AURELIS_INPUT_ERROR ')):
            continue
        try:
            kind, payload = line.split(' ', 1)
            data = json.loads(payload)
            if not isinstance(data, dict) or not isinstance(data.get('task'), str):
                raise ValueError('Invalid task observation')
            if kind == 'AURELIS_INPUT_ERROR':
                errors.append(f"{phase}/{data['task']}: task input inspection failed")
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


def report(root: Path, out: Path, annotations: bool = False) -> int:
    out.mkdir(exist_ok=True, parents=True)
    state = phases()
    tasks, diagnostics, collector_errors, inputs = [], [], [], []
    for phase in BUILD_PHASES:
        path = out / f'{phase}.log'
        if path.exists():
            text = path.read_text(encoding='utf-8', errors='replace')
            t, d = parse_log(text, phase, root);tasks.extend(t);diagnostics.extend(d)
            observed, errors = parse_inputs(text, phase, t);inputs.extend(observed);collector_errors.extend(errors)
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
    save(out / 'source-diagnostics.json', sources)
    save(out / 'task-inputs.json', inputs)
    save(out / 'task-states.json', tasks)
    save(out / 'diagnostics.json', diagnostics)
    for name, fields, rows in [
        ('source-inventory', ('module', 'source_set', 'kind', 'language', 'path', 'individual_compilation'), sources),
        ('task-states', ('phase', 'task', 'state'), tasks),
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
    lines += ['', f'Inventario: {len(sources)} fuentes. Tareas observadas: {len(tasks)}. Diagnósticos únicos capturados: {len(diagnostics)}.',
              'Detalle completo: source-inventory.json, changed-files.json, source-diagnostics.json, task-states.json y diagnostics.json.',
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
        'NO_TASK_INPUT_EVIDENCE significa desconocido, no excluido ni compilado. Java puede ser entrada de Kotlin solo para resolver símbolos.']
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
    print('Technical checks passed:', data['technical_checks_passed'])
    return 1 if collector_errors else 0


def md(value: str) -> str:
    return html.escape(value).replace('|', '&#124;').replace('`', '&#96;').replace('\n', ' ')


def command_escape(text: str, property_value: bool = False) -> str:
    text = text.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    return text.replace(',', '%2C').replace(':', '%3A') if property_value else text


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
