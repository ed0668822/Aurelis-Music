#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""V1: pin manual Release to version approved by the same-SHA green B01-B06 CI."""
import argparse
import json
import os
from pathlib import Path
import sys
from v1_versioning import append_env, read_policy, validate


def source_version(report_root, sha, run_id, run_number, policy_path):
    f = Path(report_root) / "B01/Compilacion/version-metadata.json"
    if f.is_symlink() or not f.is_file():
        raise ValueError("Previous green B06 artifact does not contain B01 V1 metadata")
    info = json.loads(f.read_text(encoding="utf-8"))
    return validate(info, read_policy(policy_path), sha.lower(), run_id, run_number)


def verify_apk_versions(info, manifest_path, verification_path):
    manifest = json.loads(Path(manifest_path).read_text(encoding="utf-8"))
    data = json.loads(Path(verification_path).read_text(encoding="utf-8"))
    if not isinstance(manifest, list) or len(manifest) != 2 or data.get("status") != "success":
        raise ValueError("No independently verified Debug + Release manifest")
    if not isinstance(data.get("apks"), list) or len(data["apks"]) != 2:
        raise ValueError("Invalid signed APK verification entries")
    for variant in ("debug", "release"):
        row = next((r for r in manifest if r.get("variant") == variant), None)
        verified = next((r for r in data["apks"] if r.get("variant") == variant), None)
        if not row or not verified:
            raise ValueError("Missing APK variant in version attestation")
        for record in (row, verified):
            if record.get("version_name") != info["version_name"] or \
                    str(record.get("version_code")) != str(info["version_code"]):
                raise ValueError("V1 APK version drift: " + variant)
    return True


def main():
    p = argparse.ArgumentParser()
    p.add_argument("action", choices=("pin", "check"))
    p.add_argument("--report", default="ci-prev-green")
    p.add_argument("--source-manifest", default="ci-b01-from-producer/version-metadata.json")
    p.add_argument("--output", default="ci-protected-source-version.json")
    p.add_argument("--env-file", default=os.environ.get("GITHUB_ENV", "/nonexistent"))
    p.add_argument("--policy", default=".github/version-policy.json")
    p.add_argument("--apk-manifest", default="ci-approved-apks/apk-manifest.json")
    p.add_argument("--verification", default="ci-artifacts/apk-verification.json")
    a = p.parse_args()
    sha = os.environ.get("GITHUB_SHA", "").lower()
    if a.action == "pin":
        run_id = int(os.environ.get("GREEN_SOURCE_RUN_ID", "0"))
        run_number = int(os.environ.get("GREEN_SOURCE_RUN_NUMBER", "0"))
        info = source_version(a.report, sha, run_id, run_number, a.policy)
        Path(a.output).write_text(json.dumps(info, indent=2) + "\n", encoding="utf-8")
        append_env(a.env_file, info)
        print("V1 protected Release pinned to verified B01-B06:", info["version_name"])
    else:
        if os.environ.get("GREEN_SOURCE_RUN_ID"):
            info = validate(json.loads(Path(a.output).read_text(encoding="utf-8")),
                            read_policy(a.policy), sha,
                            int(os.environ["GREEN_SOURCE_RUN_ID"]),
                            int(os.environ["GREEN_SOURCE_RUN_NUMBER"]))
        else:
            info = validate(json.loads(Path(a.source_manifest).read_text(encoding="utf-8")),
                            read_policy(a.policy), sha,
                            int(os.environ.get("GITHUB_RUN_ID", "0")),
                            int(os.environ.get("GITHUB_RUN_NUMBER", "0")))
        verify_apk_versions(info, a.apk_manifest, a.verification)
        print("V1 PASS: both signed APKs match verified source version", info["version_name"])


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, TypeError, KeyError, json.JSONDecodeError) as error:
        print("V1 SIGNED APK VERSION FAIL:", error, file=sys.stderr)
        sys.exit(1)
