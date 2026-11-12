"""Aurelis GPL-3.0-or-later. APK presence evidence, never Android runtime validation."""
from __future__ import annotations

import collections
import csv
import hashlib
import json
from pathlib import Path
import re
import struct
import subprocess
import zipfile
import xml.etree.ElementTree as ET


def sha_file(file: Path) -> str:
    with file.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def bounded(data: bytes, offset: int, count: int) -> bytes:
    if offset < 0 or count < 0 or offset + count > len(data):
        raise ValueError('Truncated binary evidence')
    return data[offset:offset + count]


def mutf8(value: bytes) -> str:
    text = value.replace(b'\xc0\x80', b'\0').decode('utf-8', 'surrogatepass')
    return text.encode('utf-16-le', 'surrogatepass').decode('utf-16-le', 'surrogatepass')


def dex_classes(data: bytes) -> set[str]:
    """Read only class_def -> type_id -> string_id descriptors, not bytecode/method bodies."""
    if len(data) < 112 or data[:4] != b'dex\n' or data[4:8] not in (b'035\0', b'037\0', b'038\0', b'039\0', b'040\0'):
        raise ValueError('Unsupported or invalid DEX evidence format')
    word = lambda at: struct.unpack('<I', bounded(data, at, 4))[0]
    if word(32) != len(data) or word(36) != 112 or word(40) != 0x12345678:
        raise ValueError('Invalid DEX header bounds/endian')
    strings, string_off, types, type_off = word(56), word(60), word(64), word(68)
    count, class_off = word(96), word(100)
    for size, offset, width in ((strings, string_off, 4), (types, type_off, 4), (count, class_off, 32)):
        if size and offset < 112:
            raise ValueError('Invalid DEX table offset')
        bounded(data, offset, size * width)
    classes = set()
    for index in range(count):
        type_index = word(class_off + index * 32)
        if type_index >= types:
            raise ValueError('Invalid DEX class type index')
        string_index = word(type_off + type_index * 4)
        if string_index >= strings:
            raise ValueError('Invalid DEX descriptor index')
        offset = word(string_off + string_index * 4)
        utf16_length = 0
        for shift in range(0, 35, 7):
            byte = bounded(data, offset, 1)[0]; offset += 1
            if shift == 28 and byte > 15:
                raise ValueError('Invalid DEX string length')
            utf16_length |= (byte & 127) << shift
            if not byte & 128:
                break
        else:
            raise ValueError('Invalid DEX string length')
        end = data.find(b'\0', offset, min(len(data), offset + 65536))
        if end < 0:
            raise ValueError('Unterminated DEX descriptor')
        descriptor = mutf8(data[offset:end])
        if len(descriptor.encode('utf-16-le', 'surrogatepass')) // 2 != utf16_length or not descriptor.startswith('L') or not descriptor.endswith(';'):
            raise ValueError('Invalid DEX class descriptor')
        name = descriptor[1:-1]
        if not name or name in classes:
            raise ValueError('Duplicate/empty DEX class definition')
        classes.add(name)
    return classes


def class_metadata(data: bytes) -> tuple[str, str | None]:
    """Read class name and SourceFile only. Never serialize constants or field values."""
    offset = 0
    def take(size):
        nonlocal offset
        value = bounded(data, offset, size); offset += size
        return value
    def u2(): return struct.unpack('>H', take(2))[0]
    def u4(): return struct.unpack('>I', take(4))[0]
    if take(4) != b'\xca\xfe\xba\xbe':
        raise ValueError('Invalid JVM class magic')
    take(4); pool = [None] * u2(); index = 1
    while index < len(pool):
        tag = take(1)[0]
        if tag == 1: pool[index] = mutf8(take(u2()))
        elif tag in (7, 8, 16, 19, 20): pool[index] = u2()
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): take(4)
        elif tag in (5, 6): take(8); index += 1
        elif tag == 15: take(3)
        else: raise ValueError('Unsupported JVM constant pool entry')
        index += 1
    u2(); this_class = u2(); u2()
    name = pool[pool[this_class]]
    take(u2() * 2)
    def skip_attributes():
        for _ in range(u2()): u2(); take(u4())
    for _ in range(2):
        for _ in range(u2()): take(6); skip_attributes()
    source = None
    for _ in range(u2()):
        attribute = pool[u2()]; length = u4()
        if attribute == 'SourceFile' and length == 2: source = pool[u2()]
        else: take(length)
    if not isinstance(name, str) or (source is not None and not isinstance(source, str)):
        raise ValueError('Invalid JVM class metadata')
    return name, source


def r8_evidence(root: Path, variant: str) -> tuple[dict, set, dict]:
    mapping, usage, files = {}, set(), {}
    directory = root / 'app/build/outputs/mapping' / variant
    for name in ('mapping.txt', 'usage.txt'):
        file = directory / name
        if not file.is_file(): continue
        files[name] = {'path': str(file.relative_to(root)), 'sha256': sha_file(file)}
        with file.open(encoding='utf-8', errors='strict') as stream:
            for line in stream:
                if name == 'mapping.txt':
                    match = re.fullmatch(r'([^\s#]+) -> ([^\s:]+):\s*', line)
                    if match: mapping[match[1].replace('.', '/')] = match[2].replace('.', '/')
                elif line and not line[0].isspace() and not line.startswith('#'):
                    value = line.strip()
                    if re.fullmatch(r'[\w.$]+', value): usage.add(value.replace('.', '/'))
    return mapping, usage, files


def resource_symbols(text: str) -> set[str]:
    return set(re.findall(r'\bresource\s+0x[0-9a-fA-F]+\s+([\w]+/[\w.]+)', text))


def resource_files(text: str) -> dict[str, list[str]]:
    result, current = collections.defaultdict(list), None
    for line in text.splitlines():
        symbol = re.search(r'\bresource\s+0x[0-9a-fA-F]+\s+([\w]+/[\w.]+)', line)
        if symbol: current = symbol[1]
        file = re.search(r'\(file\)\s+(\S+)', line)
        if current and file: result[current].append(file[1])
    return dict(result)


def manifest_components(text: str, application_id: str) -> set[str]:
    android_uri = 'http://schemas.android.com/apk/res/android'
    aliases = {'android': android_uri}
    components, element = set(), ''
    for line in text.splitlines():
        namespace = re.match(r'\s*N:\s+([^=\s]+)=([^\s]+)', line)
        if namespace: aliases[namespace[1]] = namespace[2]
        match = re.match(r'\s*E:\s+([\w-]+)', line)
        if match: element = match[1]
        match = re.search(r'A:\s+([^\s(=]+)(?:\([^)]*\))?="([^"]+)"', line)
        if not match or element not in ('application', 'activity', 'service', 'provider', 'receiver'):
            continue
        attribute = match[1].rsplit(':', 1)
        if len(attribute) != 2 or attribute[1] != 'name':
            continue
        if attribute[0] != android_uri and aliases.get(attribute[0]) != android_uri:
            continue
        name = match[2]
        if name.startswith('.'): name = application_id + name
        elif '.' not in name: name = application_id + '.' + name
        components.add(name.replace('.', '/'))
    return components


def expected_resources(file: Path) -> set[str]:
    folder = file.parent.name.split('-')[0]
    if folder != 'values':
        return {folder + '/' + file.name.split('.')[0]}
    result = set()
    for item in ET.parse(file).getroot():
        kind = item.get('type') if item.tag == 'item' else item.tag
        if kind in ('public', 'declare-styleable', 'eat-comment', 'skip') or not item.get('name'): continue
        if kind in ('string-array', 'integer-array'): kind = 'array'
        result.add(str(kind) + '/' + item.get('name'))
    return result


def load_policy(root: Path) -> dict:
    policy = json.loads((root / '.github/apk-audit-policy.json').read_text())
    if policy.get('schema_version') != 1 or policy.get('abi') != 'arm64-v8a':
        raise ValueError('Unsupported APK audit policy')
    for key in ('required_dex_classes', 'required_manifest_classes', 'required_resources', 'required_assets', 'byte_exact_resources', 'required_native_libraries'):
        if not isinstance(policy.get(key), list) or any(not isinstance(v, str) or not v or '..' in Path(v).parts or Path(v).is_absolute() for v in policy[key]):
            raise ValueError('Invalid APK audit requirements')
    return policy


def compiler_classes(root: Path, out: Path, variant: str) -> tuple[list, list]:
    """Use paths declared by successful/reused compiler tasks, never stale glob guesses."""
    roots, errors, result = set(), [], []
    pattern = re.compile(r':compile' + variant.title() + r'(?:Kotlin|JavaWithJavac)$')
    for phase in ('compile', 'lint', 'tests', 'build'):
        log = out / (phase + '.log')
        if not log.is_file(): continue
        for line in log.read_text(errors='replace').splitlines():
            if not line.startswith('AURELIS_CLASS_OUTPUT '): continue
            value = json.loads(line.split(' ', 1)[1])
            if not pattern.search(value['task']): continue
            path = (root / value['path']).resolve()
            if not path.is_relative_to(root.resolve()) or '/build/' not in path.as_posix():
                raise ValueError('Compiler output outside project build directory')
            roots.add(path)
    seen = set()
    for directory in sorted(roots):
        if not directory.exists(): errors.append('DECLARED_CLASS_OUTPUT_MISSING'); continue
        if not directory.is_dir():
            errors.append('CLASS_OUTPUT_FORMAT_UNSUPPORTED'); continue
        for file in sorted(directory.rglob('*.class')):
            if file.is_symlink() or not file.resolve().is_relative_to(root.resolve()):
                errors.append('CLASS_OUTPUT_SYMLINK_NOT_INSPECTED'); continue
            if file in seen: continue
            seen.add(file)
            name, source = class_metadata(file.read_bytes())
            result.append({'class': name, 'source_file': source, 'output': str(file.relative_to(root))})
    return result, sorted(set(errors))


def native_commands(root: Path, variant: str) -> list:
    result = []
    for file in sorted((root / 'app/.cxx').glob('**/compile_commands.json')):
        lowered = file.relative_to(root).as_posix().lower()
        selected = '/debug/' in lowered if variant == 'debug' else any('/' + token + '/' in lowered for token in ('release', 'relwithdebinfo'))
        if not selected: continue
        command_sha = sha_file(file)
        for command in json.loads(file.read_text()):
            source = Path(command['file'])
            if not source.is_absolute(): source = Path(command['directory']) / source
            source = source.resolve()
            if source.is_relative_to(root.resolve()):
                result.append({'source': str(source.relative_to(root.resolve())), 'command_file': str(file.relative_to(root)), 'command_file_sha256': command_sha, 'evidence': 'COMPILE_COMMAND_DECLARED_NOT_EXECUTION_PROOF'})
    return list({r['source']: r for r in result}.values())


def audit(root: Path, out: Path, sdk: Path, apk: Path, variant: str) -> dict:
    report = {'schema_version': 1, 'variant': variant, 'apk_sha256': sha_file(apk),
              'status': 'EVIDENCE_ERROR', 'rows': [], 'required_failures': [], 'evidence_errors': [],
              'ui_integration': 'NOT_AUDITED', 'android_runtime': 'NOT_EXECUTED'}
    rows = report['rows']
    def row(path, kind, state, **details): rows.append({'path': path, 'kind': kind, 'state': state, **details})
    try:
        metadata_file = out / 'metadata.json'
        report['build_identity'] = json.loads(metadata_file.read_text()) if metadata_file.is_file() else {'state': 'NO_BUILD_METADATA'}
        policy = load_policy(root)
        report['policy_sha256'] = sha_file(root / '.github/apk-audit-policy.json')
        with zipfile.ZipFile(apk) as archive:
            names = archive.namelist(); entries = set(names)
            if len(entries) != len(names): raise ValueError('Duplicate APK ZIP entries')
            for required in ('AndroidManifest.xml', 'resources.arsc', 'classes.dex'):
                if required not in entries: report['required_failures'].append('APK_ENTRY_MISSING:' + required)
            classes = set()
            for name in sorted(entries):
                if re.fullmatch(r'classes(?:[2-9]|[1-9][0-9]+)?\.dex', name):
                    found = dex_classes(archive.read(name))
                    if classes & found: raise ValueError('Duplicate class definitions across DEX files')
                    classes.update(found)
            report['dex_class_count'] = len(classes)
            mapping, usage, report['r8'] = r8_evidence(root, variant)
            def class_state(name):
                if name in classes: return 'DEX_CLASS_PRESENT', name
                mapped = mapping.get(name)
                if mapped in classes: return 'R8_MAPPED_CLASS_PRESENT', mapped
                if name in usage: return 'R8_USAGE_REPORTED_NOT_PRESENCE', mapped
                return 'CLASS_NOT_OBSERVED_IN_DEX', mapped
            for name in policy['required_dex_classes']:
                state, final = class_state(name); row(name, 'REQUIRED_CLASS', state, final_class=final)
                if state not in ('DEX_CLASS_PRESENT', 'R8_MAPPED_CLASS_PRESENT'):
                    report['required_failures'].append('REQUIRED_CLASS_NOT_PRESENT:' + name)
            tool = sdk / 'build-tools/35.0.0/aapt2'
            output = subprocess.run([str(tool), 'dump', 'resources', str(apk)], capture_output=True, text=True, check=True).stdout
            symbols = resource_symbols(output)
            if not symbols: raise ValueError('No parseable AAPT resource table evidence')
            resource_paths = resource_files(output)
            report['resource_symbol_count'] = len(symbols)
            xmltree = subprocess.run([str(tool), 'dump', 'xmltree', str(apk), '--file', 'AndroidManifest.xml'],
                capture_output=True, text=True, check=True).stdout
            components = manifest_components(xmltree, policy['application_id'])
            if not components: raise ValueError('No parseable merged manifest component evidence')
            report['manifest_components'] = sorted(components)
            for name in policy['required_manifest_classes']:
                final = class_state(name)[1] or name
                if final not in components: report['required_failures'].append('REQUIRED_MANIFEST_COMPONENT_NOT_PRESENT:' + name)
            for symbol in policy['required_resources']:
                if symbol not in symbols: report['required_failures'].append('REQUIRED_RESOURCE_NOT_PRESENT:' + symbol)
            for name in policy['required_native_libraries']:
                path = 'lib/' + policy['abi'] + '/' + name
                row(path, 'REQUIRED_NATIVE', 'APK_ENTRY_PRESENT' if path in entries else 'APK_ENTRY_MISSING')
                if path not in entries: report['required_failures'].append('REQUIRED_NATIVE_NOT_PRESENT:' + path)
            for relative in policy['required_assets']:
                file = root / 'app/src/main/assets' / relative; path = 'assets/' + relative
                match = file.is_file() and not file.is_symlink() and path in entries and sha_file(file) == hashlib.sha256(archive.read(path)).hexdigest()
                if not match: report['required_failures'].append('REQUIRED_ASSET_MISSING_OR_DIFFERENT:' + path)
            for path in policy['byte_exact_resources']:
                file = root / 'app/src/main' / path
                candidates = {path}
                if file.is_file() and not file.is_symlink():
                    for symbol in expected_resources(file): candidates.update(resource_paths.get(symbol, []))
                match = file.is_file() and not file.is_symlink() and any(
                    candidate in entries and sha_file(file) == hashlib.sha256(archive.read(candidate)).hexdigest() for candidate in candidates)
                if not match: report['required_failures'].append('REQUIRED_RESOURCE_BYTES_MISSING_OR_DIFFERENT:' + path)
            inventory = json.loads((out / 'source-inventory.json').read_text())
            compiled, class_errors = compiler_classes(root, out, variant)
            report['compiler_output_errors'] = class_errors
            report['compiler_class_count'] = len(compiled)
            report['class_presence'] = [{**item, 'state': class_state(item['class'])[0], 'final_class': class_state(item['class'])[1]} for item in compiled]
            by_source = collections.defaultdict(list)
            source_candidates = collections.Counter()
            source_keys = {}
            for item in inventory:
                file = root / item['path']
                if item.get('role') == 'MANAGED_SOURCE' and item.get('kind') != 'test' and not file.is_symlink() and file.is_file():
                    source_text = file.read_text(encoding='utf-8')
                    # A package keyword inside a block comment is not a declaration.
                    source_text = re.sub(r'/\*.*?\*/', '', source_text, flags=re.S)
                    source_text = re.sub(r'(?m)^\s*//[^\n]*
                    key = (package[1].replace('.', '/') if package else '', file.name)
                    source_keys[item['path']] = key
                    source_candidates[key] += 1
            for item in report['class_presence']:
                if item['source_file']:
                    package = item['class'].rsplit('/', 1)[0] if '/' in item['class'] else ''
                    by_source[(package, item['source_file'])].append(item)
            commands = native_commands(root, variant)
            report['native_units'] = commands
            for source in inventory:
                path = source['path']; role = source.get('role'); file = root / path
                if source.get('kind') == 'test' or source.get('app_packaging_expected') is False:
                    row(path, role, 'NOT_EXPECTED_IN_APP_BY_POLICY'); continue
                key = source_keys.get(path)
                if source.get('source_set') in ('debug', 'release') and source['source_set'] != variant and not by_source.get(key):
                    row(path, role, 'OTHER_VARIANT_BY_PATH_POLICY'); continue
                if file.is_symlink() or not file.is_file():
                    row(path, role, 'SOURCE_CONTENT_NOT_INSPECTED'); continue
                if role == 'MANAGED_SOURCE':
                    observed = by_source.get(key, [])
                    state = 'COMPILER_CLASSES_MAPPED_TO_DEX' if observed and all(v['state'] in ('DEX_CLASS_PRESENT', 'R8_MAPPED_CLASS_PRESENT') for v in observed) else 'CLASS_PRESENCE_EVIDENCE_INCOMPLETE'
                    row(path, role, 'AMBIGUOUS_SOURCE_CLASS_MAPPING' if source_candidates[key] > 1 else state,
                        classes=observed, source_equivalence='NOT_PROVEN_BY_CLASS_PRESENCE')
                elif role == 'ASSET':
                    target = 'assets/' + path.split('/assets/', 1)[1]
                    digest = sha_file(file)
                    state = 'APK_BYTES_MATCH' if target in entries and digest == hashlib.sha256(archive.read(target)).hexdigest() else 'APK_ENTRY_MISSING_OR_DIFFERENT'
                    row(path, role, state, apk_entry=target, source_sha256=digest)
                elif role == 'ANDROID_RESOURCE':
                    expected = expected_resources(file)
                    missing = sorted(expected - symbols)
                    row(path, role, 'RESOURCE_SYMBOLS_PRESENT_CONFIGURATION_NOT_PROVEN' if expected and not missing else 'RESOURCE_EVIDENCE_INCOMPLETE', symbols=sorted(expected), unobserved_symbols=missing)
                elif role == 'PREBUILT_NATIVE':
                    relative = path.split('/jniLibs/', 1)[1]
                    if relative.split('/')[0] != policy['abi']:
                        row(path, role, 'EXCLUDED_BY_ABI_POLICY'); continue
                    target = 'lib/' + relative
                    row(path, role, 'APK_BYTES_MATCH' if target in entries and sha_file(file) == hashlib.sha256(archive.read(target)).hexdigest() else 'NATIVE_BYTES_NOT_MATCHED', apk_entry=target)
                elif role == 'NATIVE_SOURCE':
                    row(path, role, 'COMPILE_COMMAND_DECLARED_NOT_EXECUTION_PROOF' if any(c['source'] == path for c in commands) else 'NO_NATIVE_UNIT_EVIDENCE')
                elif role == 'NATIVE_HEADER': row(path, role, 'HEADER_NOT_A_STANDALONE_BINARY')
                elif role == 'MANIFEST': row(path, role, 'APK_MANIFEST_COMPONENTS_CHECKED_OTHER_ATTRIBUTES_NOT_AUDITED')
                elif role == 'LEGAL_TEXT':
                    target = 'assets/licenses/' + (path[len('LICENSES/'):] if path.startswith('LICENSES/') else path)
                    row(path, role, 'APK_BYTES_MATCH' if target in entries and sha_file(file) == hashlib.sha256(archive.read(target)).hexdigest() else 'APK_ENTRY_MISSING_OR_DIFFERENT', apk_entry=target)
                else: row(path, role or 'UNKNOWN', 'NOT_AUDITED')
            report['status'] = 'REQUIRED_CONTENT_MISSING' if report['required_failures'] else 'REQUIRED_CONTENT_VERIFIED_WITH_COVERAGE_LIMITS'
    except Exception as error:
        # Errors of the auditor are not classified as application compilation errors.
        report['status'] = 'EVIDENCE_ERROR'
        report['evidence_errors'].append({'type': type(error).__name__, 'reason': str(error)[:160] if type(error) is ValueError else 'Auditor input/tool error; no application compilation conclusion'})
    out.mkdir(parents=True, exist_ok=True)
    (out / f'apk-content-{variant}.json').write_text(json.dumps(report, ensure_ascii=True, indent=2) + '\n')
    with (out / f'apk-content-{variant}.tsv').open('w') as stream:
        writer = csv.DictWriter(stream, fieldnames=('path', 'kind', 'state'), delimiter='\t', extrasaction='ignore')
        writer.writeheader(); writer.writerows(rows)
    if report['status'] != 'REQUIRED_CONTENT_VERIFIED_WITH_COVERAGE_LIMITS':
        details = '; '.join(error['type'] + ': ' + error['reason'] for error in report['evidence_errors']) if report['evidence_errors'] else '; '.join(report['required_failures'][:5])
        raise ValueError('APK content audit ' + report['status'] + ' (' + details + '); see apk-content-' + variant + '.json')
    return report
, '', source_text)
                    package = re.search(r'(?m)^\s*package\s+([\w.]+)', source_text)
                    key = (package[1].replace('.', '/') if package else '', file.name)
                    source_keys[item['path']] = key
                    source_candidates[key] += 1
            for item in report['class_presence']:
                if item['source_file']:
                    package = item['class'].rsplit('/', 1)[0] if '/' in item['class'] else ''
                    by_source[(package, item['source_file'])].append(item)
            commands = native_commands(root, variant)
            report['native_units'] = commands
            for source in inventory:
                path = source['path']; role = source.get('role'); file = root / path
                if source.get('kind') == 'test' or source.get('app_packaging_expected') is False:
                    row(path, role, 'NOT_EXPECTED_IN_APP_BY_POLICY'); continue
                key = source_keys.get(path)
                if source.get('source_set') in ('debug', 'release') and source['source_set'] != variant and not by_source.get(key):
                    row(path, role, 'OTHER_VARIANT_BY_PATH_POLICY'); continue
                if file.is_symlink() or not file.is_file():
                    row(path, role, 'SOURCE_CONTENT_NOT_INSPECTED'); continue
                if role == 'MANAGED_SOURCE':
                    observed = by_source.get(key, [])
                    state = 'COMPILER_CLASSES_MAPPED_TO_DEX' if observed and all(v['state'] in ('DEX_CLASS_PRESENT', 'R8_MAPPED_CLASS_PRESENT') for v in observed) else 'CLASS_PRESENCE_EVIDENCE_INCOMPLETE'
                    row(path, role, 'AMBIGUOUS_SOURCE_CLASS_MAPPING' if source_candidates[key] > 1 else state,
                        classes=observed, source_equivalence='NOT_PROVEN_BY_CLASS_PRESENCE')
                elif role == 'ASSET':
                    target = 'assets/' + path.split('/assets/', 1)[1]
                    digest = sha_file(file)
                    state = 'APK_BYTES_MATCH' if target in entries and digest == hashlib.sha256(archive.read(target)).hexdigest() else 'APK_ENTRY_MISSING_OR_DIFFERENT'
                    row(path, role, state, apk_entry=target, source_sha256=digest)
                elif role == 'ANDROID_RESOURCE':
                    expected = expected_resources(file)
                    missing = sorted(expected - symbols)
                    row(path, role, 'RESOURCE_SYMBOLS_PRESENT_CONFIGURATION_NOT_PROVEN' if expected and not missing else 'RESOURCE_EVIDENCE_INCOMPLETE', symbols=sorted(expected), unobserved_symbols=missing)
                elif role == 'PREBUILT_NATIVE':
                    relative = path.split('/jniLibs/', 1)[1]
                    if relative.split('/')[0] != policy['abi']:
                        row(path, role, 'EXCLUDED_BY_ABI_POLICY'); continue
                    target = 'lib/' + relative
                    row(path, role, 'APK_BYTES_MATCH' if target in entries and sha_file(file) == hashlib.sha256(archive.read(target)).hexdigest() else 'NATIVE_BYTES_NOT_MATCHED', apk_entry=target)
                elif role == 'NATIVE_SOURCE':
                    row(path, role, 'COMPILE_COMMAND_DECLARED_NOT_EXECUTION_PROOF' if any(c['source'] == path for c in commands) else 'NO_NATIVE_UNIT_EVIDENCE')
                elif role == 'NATIVE_HEADER': row(path, role, 'HEADER_NOT_A_STANDALONE_BINARY')
                elif role == 'MANIFEST': row(path, role, 'APK_MANIFEST_COMPONENTS_CHECKED_OTHER_ATTRIBUTES_NOT_AUDITED')
                elif role == 'LEGAL_TEXT':
                    target = 'assets/licenses/' + (path[len('LICENSES/'):] if path.startswith('LICENSES/') else path)
                    row(path, role, 'APK_BYTES_MATCH' if target in entries and sha_file(file) == hashlib.sha256(archive.read(target)).hexdigest() else 'APK_ENTRY_MISSING_OR_DIFFERENT', apk_entry=target)
                else: row(path, role or 'UNKNOWN', 'NOT_AUDITED')
            report['status'] = 'REQUIRED_CONTENT_MISSING' if report['required_failures'] else 'REQUIRED_CONTENT_VERIFIED_WITH_COVERAGE_LIMITS'
    except Exception as error:
        # Errors of the auditor are not classified as application compilation errors.
        report['status'] = 'EVIDENCE_ERROR'
        report['evidence_errors'].append({'type': type(error).__name__, 'reason': str(error)[:160] if type(error) is ValueError else 'Auditor input/tool error; no application compilation conclusion'})
    out.mkdir(parents=True, exist_ok=True)
    (out / f'apk-content-{variant}.json').write_text(json.dumps(report, ensure_ascii=True, indent=2) + '\n')
    with (out / f'apk-content-{variant}.tsv').open('w') as stream:
        writer = csv.DictWriter(stream, fieldnames=('path', 'kind', 'state'), delimiter='\t', extrasaction='ignore')
        writer.writeheader(); writer.writerows(rows)
    if report['status'] != 'REQUIRED_CONTENT_VERIFIED_WITH_COVERAGE_LIMITS':
        details = '; '.join(error['type'] + ': ' + error['reason'] for error in report['evidence_errors']) if report['evidence_errors'] else '; '.join(report['required_failures'][:5])
        raise ValueError('APK content audit ' + report['status'] + ' (' + details + '); see apk-content-' + variant + '.json')
    return report
