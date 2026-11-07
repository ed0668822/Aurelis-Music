#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Independent, fail-closed APK evidence reconciliation.

PASS applies only to mandatory integrity/signature/build-identity checks.
Individual source compilation is NEVER inferred from absence of errors,
CMake command declarations, class presence, or a successful Gradle task.
"""
from __future__ import annotations

import argparse
import collections
import csv
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import zipfile

EXPECTED_AUDIT = "REQUIRED_CONTENT_VERIFIED_WITH_COVERAGE_LIMITS"
EXPECTED_ABI = "arm64-v8a"
EXPECTED_PACKAGE = "com.music.aurelis"
VARIANTS = ("debug", "release")
KNOWN_TASK_STATES = {"SUCCESS", "FROM-CACHE", "UP-TO-DATE", "SKIPPED", "NO-SOURCE", "FAILED"}
TASK_RESULT = re.compile(r"^AURELIS_TASK_RESULT (\{.*\})$")


def sha256(path: Path) -> str:
    with path.open("rb") as handle:
        digest = hashlib.sha256()
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
        return digest.hexdigest()


def load_json(path: Path, expected_type, problems: list[str]):
    if not path.is_file():
        problems.append("MISSING_EVIDENCE: " + path.name)
        return expected_type()
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        problems.append("MALFORMED_EVIDENCE: " + path.name)
        return expected_type()
    if not isinstance(value, expected_type):
        problems.append("WRONG_EVIDENCE_TYPE: " + path.name)
        return expected_type()
    return value


def safe_apk(root: Path, file_name: object) -> Path | None:
    if not isinstance(file_name, str) or not file_name or PurePosixPath(file_name).name != file_name:
        return None
    file = root / "ci-approved-apks" / file_name
    if file.is_symlink() or not file.is_file():
        return None
    return file


def parse_tasks(text: str) -> dict[str, str]:
    statuses: dict[str, str] = {}
    for line in text.splitlines():
        match = TASK_RESULT.fullmatch(line.strip())
        if not match:
            continue
        try:
            task = json.loads(match.group(1))
        except ValueError:
            continue
        path, state = task.get("task"), task.get("state")
        if isinstance(path, str) and isinstance(state, str) and state in KNOWN_TASK_STATES:
            statuses[path] = state
    return statuses


def source_evidence(source: dict, row: dict | None) -> tuple[str, str]:
    role = source.get("role")
    if source.get("kind") == "test":
        return "OUT_OF_APK_SCOPE", "Test source is assessed by B03, not packaged in production APK"
    if source.get("app_packaging_expected") is False:
        return "OUT_OF_APK_SCOPE", "Inventory policy excludes this file from standalone APK packaging"
    if row is None:
        return "EVIDENCE_UNAVAILABLE", "No corresponding row in the APK content audit"
    state = row.get("state", "UNCLASSIFIED")
    if role == "MANAGED_SOURCE":
        if state == "COMPILER_CLASSES_MAPPED_TO_DEX":
            return "CLASS_MAPPING_OBSERVED", "Generated classes linked to DEX; individual source compilation NOT proven"
        if state == "OTHER_VARIANT_BY_PATH_POLICY":
            return "OTHER_BUILD_VARIANT", "Not expected in this APK variant by source-set path policy"
        return "CLASS_MAPPING_INCONCLUSIVE", "No reliable one-to-one source-to-DEX proof; not a compiler failure"
    if role == "NATIVE_SOURCE":
        if state == "COMPILE_COMMAND_DECLARED_NOT_EXECUTION_PROOF":
            return "NATIVE_COMMAND_DECLARED", "CMake command declaration observed; execution per source NOT proven"
        return "NATIVE_EXECUTION_UNVERIFIED", "No observed native build command for this source"
    if role == "PREBUILT_NATIVE":
        if state == "APK_BYTES_MATCH":
            return "PREBUILT_PACKAGED_EXACT", "Original prebuilt library bytes match the APK"
        if state == "EXCLUDED_BY_ABI_POLICY":
            return "OUT_OF_ABI_SCOPE", "Not targeted by ARM64 APK policy"
        return "PREBUILT_PACKAGING_INCONCLUSIVE", "No exact packaged bytes match observed"
    if role in ("ASSET", "LEGAL_TEXT") and state == "APK_BYTES_MATCH":
        return "PACKAGED_BYTES_MATCH", "Source bytes match APK entry"
    if role == "ANDROID_RESOURCE" and state == "RESOURCE_SYMBOLS_PRESENT_CONFIGURATION_NOT_PROVEN":
        return "RESOURCE_SYMBOL_PRESENT", "Symbol observed; specific resource configuration not proven"
    return "OTHER_APK_EVIDENCE", "See raw APK audit state: " + str(state)


def evaluate(root: Path, out: Path, expected_sha: str) -> dict:
    problems: list[str] = []
    inventory = load_json(out / "source-inventory.json", list, problems)
    metadata = load_json(out / "metadata.json", dict, problems)
    policy = load_json(root / ".github/apk-audit-policy.json", dict, problems)
    verification = load_json(out / "apk-verification.json", dict, problems)
    signing = load_json(out / "signing.json", dict, problems)
    manifest = load_json(root / "ci-approved-apks/apk-manifest.json", list, problems)

    if not re.fullmatch(r"[0-9a-f]{40}|[0-9a-f]{64}", expected_sha):
        problems.append("INVALID_EXPECTED_COMMIT_SHA")
    if metadata.get("source_sha") != expected_sha or metadata.get("checkout_sha") != expected_sha:
        problems.append("BUILD_COMMIT_IDENTITY_MISMATCH")
    if policy.get("schema_version") != 1 or policy.get("abi") != EXPECTED_ABI or policy.get("application_id") != EXPECTED_PACKAGE:
        problems.append("UNEXPECTED_APK_POLICY")
    if verification.get("status") != "success":
        problems.append("APK_VERIFICATION_NOT_SUCCESS")
    if signing.get("mode") != "configured_release":
        problems.append("RELEASE_SIGNING_CERTIFICATE_NOT_CONFIGURED")
    expected_certificate = signing.get("configured_certificate_sha256")
    if not isinstance(expected_certificate, str) or not re.fullmatch(r"[0-9a-fA-F]{64}", expected_certificate):
        problems.append("RELEASE_CERTIFICATE_FINGERPRINT_MISSING")

    by_variant: dict[str, dict] = {}
    for record in manifest:
        variant = record.get("variant") if isinstance(record, dict) else None
        if variant not in VARIANTS or variant in by_variant:
            problems.append("INVALID_OR_DUPLICATE_MANIFEST_VARIANT")
            continue
        by_variant[variant] = record
    if set(by_variant) != set(VARIANTS):
        problems.append("INCOMPLETE_DEBUG_RELEASE_MANIFEST")
    verifications = {r.get("variant"): r for r in verification.get("apks", []) if isinstance(r, dict)}
    if set(verifications) != set(VARIANTS):
        problems.append("INCOMPLETE_APK_VERIFICATION_RECORDS")

    if not (out / "build.log").is_file():
        problems.append("MISSING_GRADLE_BUILD_LOG")
        task_states = {}
    else:
        task_states = parse_tasks((out / "build.log").read_text(errors="replace"))
        if any(state == "FAILED" for state in task_states.values()):
            problems.append("FAILED_GRADLE_TASK_IN_SUCCESSFUL_APK_REPORT")
    task_observations: dict[str, dict] = {}
    for variant in VARIANTS:
        tasks = [f":app:compile{variant.title()}Kotlin", f":app:compile{variant.title()}JavaWithJavac"]
        task_observations[variant] = {task: task_states.get(task, "NOT_OBSERVED") for task in tasks}
    seen_sources: set[str] = set()
    for item in inventory:
        name = item.get("path") if isinstance(item, dict) else None
        if not isinstance(name, str) or name in seen_sources or not name:
            problems.append("INVALID_OR_DUPLICATE_SOURCE_INVENTORY")
        else:
            seen_sources.add(name)

    source_rows: list[dict] = []
    coverage: dict[str, dict] = {}
    for variant in VARIANTS:
        audit = load_json(out / f"apk-content-{variant}.json", dict, problems)
        entry = by_variant.get(variant, {})
        record = verifications.get(variant, {})
        file = safe_apk(root, entry.get("file"))
        digest = sha256(file) if file else None
        if file is None or digest != entry.get("sha256") or digest != record.get("sha256"):
            problems.append(f"{variant.upper()}_APK_SHA256_MISMATCH_OR_MISSING")
        if file is not None and (file.stat().st_size != entry.get("bytes") or file.stat().st_size != record.get("bytes")):
            problems.append(f"{variant.upper()}_APK_SIZE_MISMATCH")
        for flag in ("signature_verified", "zip_verified", "zip_16kb_alignment_verified",
                     "native_16kb_alignment_verified"):
            if record.get(flag) is not True:
                problems.append(f"{variant.upper()}_{flag.upper()}_NOT_VERIFIED")
        if record.get("abis") != [EXPECTED_ABI] or record.get("package") != EXPECTED_PACKAGE:
            problems.append(f"{variant.upper()}_ABI_OR_PACKAGE_MISMATCH")
        if expected_certificate not in record.get("signer_certificate_sha256", []):
            problems.append(f"{variant.upper()}_SIGNER_CERTIFICATE_MISMATCH")
        if audit.get("status") != EXPECTED_AUDIT or audit.get("required_failures") or audit.get("evidence_errors"):
            problems.append(f"{variant.upper()}_APK_CONTENT_AUDIT_NOT_VERIFIED")
        if audit.get("apk_sha256") != digest or audit.get("policy_sha256") != sha256(root / ".github/apk-audit-policy.json"):
            problems.append(f"{variant.upper()}_APK_CONTENT_OR_POLICY_HASH_MISMATCH")
        if audit.get("build_identity") != metadata:
            problems.append(f"{variant.upper()}_AUDIT_COMMIT_IDENTITY_MISMATCH")
        if audit.get("compiler_output_errors"):
            problems.append(f"{variant.upper()}_COMPILER_EVIDENCE_CORRUPT")

        # This tests byte equality, not runtime JNI symbol resolution or audio decoding.
        prebuilt = root / "app/src/main/jniLibs/arm64-v8a/libffmpegJNI.so"
        if not prebuilt.is_file() or prebuilt.is_symlink() or file is None:
            problems.append(f"{variant.upper()}_FFMPEG_PREBUILT_OR_APK_MISSING")
        else:
            try:
                with zipfile.ZipFile(file) as apk:
                    binary = apk.read("lib/arm64-v8a/libffmpegJNI.so")
                if hashlib.sha256(binary).hexdigest() != sha256(prebuilt):
                    problems.append(f"{variant.upper()}_FFMPEG_PREBUILT_BYTES_DIFFER")
            except (OSError, KeyError, zipfile.BadZipFile):
                problems.append(f"{variant.upper()}_FFMPEG_PREBUILT_NOT_READABLE")

        rows = audit.get("rows") or []
        by_path = collections.defaultdict(list)
        for row in rows:
            if isinstance(row, dict):
                by_path[row.get("path")].append(row)
        findings = collections.Counter()
        unresolved = []
        for item in inventory:
            if not isinstance(item, dict) or not isinstance(item.get("path"), str):
                continue
            path = item["path"]
            matched = by_path.get(path, [])
            if len(matched) != 1:
                problems.append(f"{variant.upper()}_SOURCE_AUDIT_CARDINALITY_ERROR: {path}")
            state, reason = source_evidence(item, matched[0] if len(matched) == 1 else None)
            findings[state] += 1
            if state in ("CLASS_MAPPING_INCONCLUSIVE", "NATIVE_EXECUTION_UNVERIFIED",
                         "PREBUILT_PACKAGING_INCONCLUSIVE", "EVIDENCE_UNAVAILABLE"):
                unresolved.append({"path": path, "classification": state})
            source_rows.append({
                "variant": variant, "path": path, "role": item.get("role", ""),
                "classification": state, "raw_audit_state": matched[0].get("state") if len(matched) == 1 else "MISSING",
                "meaning": reason, "individual_compilation": "NOT_PROVEN",
            })
        coverage[variant] = {
            "states": dict(findings),
            "unresolved": unresolved,
            "individual_source_compilation_proven": False,
        }

    return {
        "schema_version": 1,
        "source_sha": expected_sha,
        "status": "PASS_VERIFIED_GATES" if not problems else "FAIL_VERIFICATION",
        "source_coverage_status": "PARTIAL_OBSERVATION_NOT_PER_FILE_PROOF",
        "problems": sorted(set(problems)),
        "gradle_task_observations": task_observations,
        "coverage": coverage,
        "source_rows": source_rows,
        "scope": {
            "proven": ["APK checksum and size", "APK signature/certificate match", "ARM64 packaging",
                       "B04 content audit and commit identity", "FFmpeg prebuilt binary exact bytes"],
            "not_proven": ["each file individually compiled", "FFmpeg JNI methods resolve at runtime",
                           "playback correctness", "all UI routes", "complete functional coverage"],
        },
    }


def write_report(out: Path, result: dict) -> None:
    out.mkdir(parents=True, exist_ok=True)
    (out / "source-evidence.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    with (out / "source-evidence.tsv").open("w", encoding="utf-8", newline="") as f:
        fields = ("variant", "path", "role", "classification", "raw_audit_state", "meaning", "individual_compilation")
        writer = csv.DictWriter(f, delimiter="\t", fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(result["source_rows"])
    lines = [
        "# B04 | Evidence reconciliation",
        "",
        f"- Mandatory verification gates: **{result['status']}**",
        f"- Source coverage: **{result['source_coverage_status']}**",
        f"- Commit: {result['source_sha']}",
        "- Every source is classified using observed evidence, not presumed compilation.",
        "- A source without mapped classes is **inconclusive**, not automatically failed.",
        "- Packaged prebuilt FFmpeg is verified byte-for-byte, not claimed to have been rebuilt.",
        "- FFmpeg playback and JNI invocation require a separate runtime test.",
        "",
        "## Mandatory evidence issues",
    ]
    lines += ["- " + problem for problem in result["problems"]] or ["- None detected"]
    for variant in VARIANTS:
        info = result["coverage"][variant]
        lines += ["", f"## {variant.title()} — source classifications", "", "| Classification | Count |",
                  "|---|---:|"]
        lines += [f"| {status} | {count} |" for status, count in sorted(info["states"].items())]
        lines += ["", f"Unresolved source evidence (not automatic build failures): {len(info['unresolved'])}"]
        lines += [f"- {item['path']}: {item['classification']}" for item in info["unresolved"][:40]]
        if len(info["unresolved"]) > 40:
            lines.append("- Remaining entries in source-evidence.json/.tsv")
    (out / "source-evidence.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--root", type=Path, default=Path("."))
    p.add_argument("--out", type=Path, default=Path("ci-artifacts"))
    p.add_argument("--sha", default=os.environ.get("GITHUB_SHA", ""))
    args = p.parse_args()
    result = evaluate(args.root.resolve(), args.out.resolve(), args.sha)
    write_report(args.out, result)
    print(f"Evidence integrity: {result['status']}; source files are NOT individually certified")
    for issue in result["problems"]:
        print("ERROR: " + issue)
    return 0 if result["status"] == "PASS_VERIFIED_GATES" else 1


if __name__ == "__main__":
    raise SystemExit(main())
