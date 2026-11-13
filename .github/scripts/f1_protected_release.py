#!/usr/bin/env python3
"""Fail-closed guard for the manual, environment-protected Aurelis Release."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys
from urllib.request import Request, urlopen
from urllib.parse import urlencode

REPO = "ed0668822/Aurelis-Music"

def digest(path):
    sha = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            sha.update(chunk)
    return sha.hexdigest()

def is_release_ref(ref):
    return ref == "refs/heads/main" or bool(re.fullmatch(r"refs/tags/v[0-9][A-Za-z0-9.+_-]*", ref))

def validate_environment(env):
    if env.get("GITHUB_REPOSITORY") != REPO or env.get("GITHUB_EVENT_NAME") != "workflow_dispatch":
        raise ValueError("Wrong repository or not manually started")
    if not is_release_ref(env.get("GITHUB_REF", "")):
        raise ValueError("Release ref must be main or v* tag")
    if env.get("RELEASE_CONFIRMATION") != "PUBLICAR":
        raise ValueError("Manual publication confirmation missing")
    if env.get("RELEASE_PROTECTION_CONFIRMED") != "true":
        raise ValueError("Protected environment configuration not confirmed")
    if not re.fullmatch("[0-9a-f]{40}", env.get("GITHUB_SHA", "").lower()):
        raise ValueError("Invalid source SHA")
    if not re.fullmatch("[0-9a-f]{64}", env.get("RELEASE_SIGNER_SHA256", "").lower()):
        raise ValueError("Missing or invalid official certificate fingerprint")
    if not env.get("GITHUB_TOKEN"):
        raise ValueError("Missing Actions read token")

def assert_prior_green(sha, token):
    url = ("https://api.github.com/repos/" + REPO +
           "/actions/workflows/b06-unified-pipeline.yml/runs?" +
           urlencode({"head_sha": sha, "per_page": 100}))
    req = Request(url, headers={
        "Authorization": "Bearer " + token,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "aurelis-protected-release",
    })
    with urlopen(req, timeout=30) as response:
        result = json.load(response)
    accepted = [x for x in result.get("workflow_runs", [])
                if isinstance(x, dict) and x.get("head_sha", "").lower() == sha
                and x.get("event") == "push" and x.get("conclusion") == "success"
                and x.get("status") == "completed"
                and type(x.get("id")) is int and type(x.get("run_number")) is int]
    if not accepted:
        raise ValueError("No successful B01-B06 push run for the exact Release SHA")
    return max(accepted, key=lambda x: x["run_number"])

def verify_and_stage(root, cert, sha):
    if not re.fullmatch("[0-9a-f]{64}", cert) or not re.fullmatch("[0-9a-f]{40}", sha):
        raise ValueError("Invalid expected fingerprint or SHA")
    artifacts = root / "ci-artifacts"
    approved = root / "ci-approved-apks"
    meta = json.loads((artifacts / "metadata.json").read_text())
    signing = json.loads((artifacts / "signing.json").read_text())
    audit = json.loads((artifacts / "apk-verification.json").read_text())
    manifest = json.loads((approved / "apk-manifest.json").read_text())
    if meta.get("source_sha") != sha or meta.get("checkout_sha") != sha:
        raise ValueError("Source provenance mismatch")
    if signing.get("mode") != "configured_release" or signing.get("configured_certificate_sha256") != cert:
        raise ValueError("Production certificate not approved")
    if audit.get("status") != "success" or not isinstance(manifest, list) or len(manifest) != 2:
        raise ValueError("APK verification has not passed")
    if {x.get("variant") for x in manifest} != {"debug", "release"}:
        raise ValueError("Expected exactly two independently verified variants")
    rel = next(x for x in manifest if x["variant"] == "release")
    verified = next((x for x in audit.get("apks", []) if x.get("variant") == "release"), None)
    if not verified:
        raise ValueError("Release audit record missing")
    if rel.get("abis") != ["arm64-v8a"] or rel.get("package") != "com.music.aurelis":
        raise ValueError("Release package/ABI mismatch")
    if cert not in rel.get("signer_certificate_sha256", []):
        raise ValueError("Release signer fingerprint mismatch")
    for key in ("signature_verified", "zip_verified", "zip_16kb_alignment_verified", "native_16kb_alignment_verified"):
        if rel.get(key) is not True or verified.get(key) is not True:
            raise ValueError("Release integrity gate missing: " + key)
    name = rel.get("file")
    if not isinstance(name, str) or name != Path(name).name or not name.endswith(".apk"):
        raise ValueError("Unsafe Release file name")
    path = approved / name
    if path.is_symlink() or not path.is_file() or digest(path) != rel.get("sha256") or digest(path) != verified.get("sha256") or path.stat().st_size != rel.get("bytes"):
        raise ValueError("APK SHA256 or size does not match audit")
    destination = root / "ci-protected-release"
    if destination.is_symlink():
        raise ValueError("Unsafe destination")
    destination.mkdir(exist_ok=True)
    shutil.copyfile(path, destination / name)
    if digest(destination / name) != digest(path):
        raise ValueError("Staged APK SHA256 mismatch")
    (destination / "SHA256SUMS.txt").write_text(digest(path) + "  " + name + "\n")
    (destination / "release-evidence.json").write_text(json.dumps({
        "source_sha": sha, "release_sha256": digest(path),
        "signer_sha256": cert, "abi": "arm64-v8a", "status": "PASS",
    }, indent=2) + "\n")

def validate_virustotal(root):
    report = json.loads((root / "ci-artifacts/virustotal.json").read_text())
    evidence = json.loads((root / "ci-protected-release/release-evidence.json").read_text())
    if (report.get("status") != "NO_DETECTIONS_REPORTED"
            or report.get("sha256") != evidence.get("release_sha256")
            or report.get("report_origin") not in ("NEW_APK_UPLOAD", "EXISTING_HASH_REPORT")):
        raise ValueError("VirusTotal analysis missing, conflicting or requiring review")

def main():
    command = sys.argv[1:]
    if command == ["preflight"]:
        validate_environment(os.environ)
        green = assert_prior_green(os.environ["GITHUB_SHA"].lower(), os.environ["GITHUB_TOKEN"])
        outputs = Path(os.environ.get("GITHUB_OUTPUT", "/nonexistent"))
        if outputs.is_symlink() or not outputs.is_file():
            raise ValueError("Cannot bind verified green run ID to protected release job")
        with outputs.open("a", encoding="utf-8") as stream:
            stream.write(f"run_id={green['id']}\nrun_number={green['run_number']}\n")
    elif command == ["stage"]:
        verify_and_stage(Path("."), os.environ["RELEASE_SIGNER_SHA256"].lower(), os.environ["GITHUB_SHA"].lower())
    elif command == ["validate-vt"]:
        validate_virustotal(Path("."))
    else:
        raise ValueError("Usage: preflight | stage | validate-vt")

if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, TypeError, json.JSONDecodeError) as error:
        print("Protected Release DENIED:", error, file=sys.stderr)
        sys.exit(1)
