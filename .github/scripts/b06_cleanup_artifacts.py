#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""After B06 PASS, delete ONLY verified same-run temporary CI artifacts.

Retain reports with VirusTotal and Debug ARM64 in all successful runs.
Only main and tags publish the third artifact, a signed Release ARM64 APK.
An absent policy flag defaults to the strict three-artifact contract.
Never delete temporary reports on a failed pipeline.
"""
from __future__ import annotations
import json
import os
from pathlib import Path
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def api(method: str, resource: str):
    base = os.environ["GITHUB_API_URL"].rstrip("/")
    token = os.environ["GITHUB_TOKEN"]
    url = base + resource
    req = Request(url, method=method, headers={
        "Accept": "application/vnd.github+json",
        "Authorization": "Bearer " + token,
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "aurelis-b06-artifact-pruner",
    })
    with urlopen(req, timeout=30) as response:
        return json.load(response) if method == "GET" else None


def artifacts() -> list[dict]:
    repo = os.environ["GITHUB_REPOSITORY"]
    run_id = os.environ["GITHUB_RUN_ID"]
    collected = []
    for page in range(1, 11):
        data = api("GET", f"/repos/{repo}/actions/runs/{run_id}/artifacts?per_page=100&page={page}")
        batch = data.get("artifacts", [])
        collected.extend(batch)
        if len(batch) < 100:
            break
    else:
        raise RuntimeError("Artifact pagination beyond limit; refusing cleanup")
    if len({a.get("id") for a in collected}) != len(collected):
        raise RuntimeError("Duplicate artifact ids; refusing cleanup")
    return collected


def release_expected() -> bool:
    """Use B04's Release policy and reject invalid settings."""
    value = os.environ.get("RELEASE_PUBLISH_EXPECTED", "true")
    if value not in ("true", "false"):
        raise RuntimeError("RELEASE_PUBLISH_EXPECTED must be 'true' or 'false'")
    return value == "true"


def main() -> int:
    summary = Path("b06-package/B06/pipeline-results.json")
    if not summary.is_file() or json.loads(summary.read_text()).get("status") != "PASS":
        raise RuntimeError("B06 not passed; temporary evidence will be preserved")
    if not os.environ.get("GITHUB_TOKEN"):
        raise RuntimeError("Missing GITHUB_TOKEN with actions:write")
    n = os.environ["GITHUB_RUN_NUMBER"]
    final = {
        f"Aurelis-Informes-B01-B06-{n}",
        f"Aurelis-Pruebas-ARM64-{n}",
    }
    if release_expected():
        final.add(f"Aurelis-Release-ARM64-{n}")
    temporary = {
        f"b01-report-{n}",
        f"b01-shared-apks-{n}",
        f"b01-compiler-outputs-{n}",
        f"b02-lint-report-{n}",
        f"b03-debug-tests-{n}",
        f"b03-release-tests-{n}",
        f"b03-summary-{n}",
        f"b04-verification-report-{n}",
        f"b05-android-emulator-{n}",
    }
    # Do not remove any unrecognized artifacts. Never delete from another run.
    for retry in range(8):
        existing = artifacts()
        names = {a["name"] for a in existing}
        if final <= names and temporary <= names and all(
                a.get("expired") is False for a in existing if a["name"] in final):
            break
        if retry == 7:
            raise RuntimeError("Final or temporary artifacts are not yet visible; refusing cleanup: " +
                               repr(sorted((final | temporary) - names)))
        time.sleep(3)
    # A duplicated final name would invalidate the expected final-artifact contract, even
    # if the set comparison passed.
    if len(names) != len(existing):
        raise RuntimeError("Duplicate artifact names; refusing cleanup")
    unexpected = names - final - temporary
    if unexpected:
        raise RuntimeError("Unexpected artifacts in this run; refusing deletion: " + repr(sorted(unexpected)))
    for a in existing:
        if a["name"] in temporary:
            api("DELETE", f"/repos/{os.environ['GITHUB_REPOSITORY']}/actions/artifacts/{a['id']}")
            print("Removed temporary artifact:", a["name"])
    for retry in range(8):
        remaining = artifacts()
        if {x["name"] for x in remaining} == final and len(remaining) == len(final):
            count = len(final)
            print(f"B06 SUCCESS: exactly {count} final downloadable artifacts remain")
            with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as f:
                f.write("\n## Entrega final\n")
                detail = ("informes B01-B06 con VirusTotal, Debug ARM64 y Release ARM64 firmado"
                          if count == 3 else
                          "informes B01-B06 con VirusTotal y Debug ARM64; Release no publicado en esta referencia")
                f.write(f"Se publicaron {count} artefactos: {detail}.\n")
            return 0
        if retry == 7:
            raise RuntimeError("Cleanup not yet confirmed: " + repr([x["name"] for x in remaining]))
        time.sleep(3)
    return 1


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, KeyError, HTTPError, URLError, RuntimeError) as error:
        print("B06 artifact cleanup verification failed:", error, file=sys.stderr)
        raise SystemExit(1)
