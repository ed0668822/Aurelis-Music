# Aurelis © 2026. GPL-3.0-or-later
"""Reuse same-SHA B01 APKs without applying a different workflow's version counter."""
import json
import os
from pathlib import Path
import shutil
import time
from urllib.parse import urlencode
from urllib.request import Request, urlopen
import ci14_handoff as handoff

REPO = "ed0668822/Aurelis-Music"
WORKFLOW = "b06-unified-pipeline.yml"
BRANCH = "feature/bitchord-local-migration"
ROOT = Path("comparison-aurelis")


def api(path):
    req = Request("https://api.github.com/repos/" + REPO + "/" + path, headers={
        "Authorization": "Bearer " + os.environ["GH_TOKEN"], "Accept": "application/vnd.github+json"})
    with urlopen(req, timeout=30) as response:
        return json.load(response)


def eligible(run, sha):
    return (run.get("head_sha") == sha and run.get("head_branch") == BRANCH
            and run.get("event") == "push" and run.get("path") == ".github/workflows/" + WORKFLOW
            and all(type(run.get(k)) is int and run[k] > 0 for k in ("id", "run_number", "run_attempt")))


def resolve():
    sha = os.environ["GITHUB_SHA"]
    if not handoff.SHA.fullmatch(sha):
        raise ValueError("Invalid current source SHA")
    for _ in range(40):
        query = urlencode({"branch": BRANCH, "head_sha": sha, "event": "push", "per_page": 20})
        runs = api("actions/workflows/" + WORKFLOW + "/runs?" + query).get("workflow_runs", [])
        for run in sorted((r for r in runs if eligible(r, sha)), key=lambda r: r["id"], reverse=True):
            name = "b01-shared-apks-" + str(run["run_number"])
            items = api(f"actions/runs/{run['id']}/artifacts?per_page=100").get("artifacts", [])
            if any(a.get("name") == name and not a.get("expired", True) for a in items):
                ROOT.mkdir(exist_ok=True)
                (ROOT / "primary-run.json").write_text(json.dumps(run, indent=2) + "\n")
                with open(os.environ["GITHUB_OUTPUT"], "a") as output:
                    output.write(f"run_id={run['id']}\nartifact={name}\n")
                return
        time.sleep(30)  # Actions only; bounded wait for the actual B01 artifact.
    raise RuntimeError("No same-SHA primary B01 handoff available within 20 minutes")


def verify(root, sha):
    run = json.loads((root / "primary-run.json").read_text())
    if not handoff.SHA.fullmatch(sha) or not eligible(run, sha):
        raise ValueError("Primary pipeline identity mismatch")
    folder = root / "ci-f3-handoff"
    manifest = folder / "manifest.json"
    if folder.is_symlink() or manifest.is_symlink():
        raise ValueError("Symlinked handoff")
    data = json.loads(manifest.read_text())
    expected = {"source_sha": sha, "run_id": str(run["id"]), "run_number": str(run["run_number"]), "run_attempt": str(run["run_attempt"])}
    if any(data.get(k) != v for k, v in expected.items()):
        raise ValueError("Handoff SHA/run/attempt mismatch")
    if data.get("schema") != 1 or data.get("abi") != "arm64-v8a" or data.get("distribution") != "debug_only":
        raise ValueError("Invalid debug handoff format")
    if not isinstance(data.get("files"), dict) or set(data["files"]) != set(handoff.FILES):
        raise ValueError("Incomplete handoff")
    allowed = {"manifest.json"}
    for variant, (relative, _) in handoff.FILES.items():
        record = data["files"][variant]
        if not isinstance(record, dict) or record.get("path") != relative:
            raise ValueError("Unexpected handoff path")
        file = folder / relative
        handoff.checked_apk(file, variant == "debug")
        if file.stat().st_size != record.get("bytes") or handoff.digest(file) != record.get("sha256"):
            raise ValueError("Handoff APK checksum/size mismatch")
        allowed.add(relative)
    found = {p.relative_to(folder).as_posix() for p in folder.rglob("*") if p.is_file() or p.is_symlink()}
    if found != allowed:
        raise ValueError("Unknown or symlinked handoff files")
    return data


def restore():
    data = verify(ROOT, os.environ["GITHUB_SHA"])
    for variant, (relative, destination) in handoff.FILES.items():
        target = Path(destination)
        if target.is_symlink():
            raise ValueError("Symlinked APK destination")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ROOT / "ci-f3-handoff" / relative, target)
        if handoff.digest(target) != data["files"][variant]["sha256"]:
            raise ValueError("Restored APK checksum mismatch")


if __name__ == "__main__":
    import sys
    if sys.argv[1] == "resolve": resolve()
    elif sys.argv[1] == "verify": verify(ROOT, os.environ["GITHUB_SHA"])
    elif sys.argv[1] == "restore": restore()
    else: raise ValueError("Unknown mode")
