#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""V1: one minor version per 150 successful complete B01-B06 workflow runs."""
import argparse
import json
import os
from pathlib import Path
import re
import sys
from urllib.parse import urlencode
from urllib.request import Request, urlopen

REPO = "ed0668822/Aurelis-Music"
WORKFLOW = "b06-unified-pipeline.yml"
SHA40 = re.compile(r"[0-9a-f]{40}\Z")


def read_policy(path):
    p = json.loads(Path(path).read_text(encoding="utf-8"))
    if (p.get("schema_version") != 1 or p.get("repo") != REPO or
        p.get("workflow") != WORKFLOW or
        any(type(p.get(k)) is not int or p[k] <= 0
            for k in ("anchor_run_number", "base_major", "base_minor",
                      "successes_per_minor", "version_code_offset")) or
        p["successes_per_minor"] != 150 or
        type(p.get("anchor_visible_patch")) is not int or
        p["anchor_visible_patch"] != 101 or
        p["anchor_run_number"] != 101 or
        p["version_code_offset"] != 10000 or
        (p["base_major"], p["base_minor"]) != (1, 9)):
        raise ValueError("Invalid V1 policy")
    return p


def compute(count, number, policy):
    if type(count) is not int or count < 0 or type(number) is not int or number <= policy["anchor_run_number"]:
        raise ValueError("Invalid green-build count or run number")
    if count > number - policy["anchor_run_number"] - 1:
        raise ValueError("Impossible successful-run count")
    # Preserve previously issued 1.9.101; never restart the visible patch at 0.
    if count < policy["successes_per_minor"]:
        visible_name = f"{policy['base_major']}.{policy['base_minor']}.{policy['anchor_visible_patch'] + 1 + count}"
    else:
        minor_offset, patch = divmod(count - policy["successes_per_minor"],
                                    policy["successes_per_minor"])
        visible_name = f"{policy['base_major']}.{policy['base_minor'] + 1 + minor_offset}.{patch}"
    code = policy["version_code_offset"] + number
    if not 1 <= code <= 2100000000:
        raise ValueError("Android versionCode out of range")
    return visible_name, code


def count_completed(token, current_run_id, current_number, policy):
    if not token or current_run_id < 1:
        raise ValueError("Missing Actions read permission/token")
    count, seen, reached_anchor = 0, set(), False
    for page in range(1, 101):
        query = urlencode({"status": "success", "per_page": 100, "page": page,
                           "exclude_pull_requests": "true"})
        url = f"https://api.github.com/repos/{REPO}/actions/workflows/{WORKFLOW}/runs?{query}"
        req = Request(url, headers={"Authorization": "Bearer " + token,
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "Aurelis-V1-CI"})
        with urlopen(req, timeout=30) as response:
            result = json.load(response)
        runs = result.get("workflow_runs")
        if not isinstance(runs, list):
            raise ValueError("GitHub did not return verifiable workflow runs")
        for item in runs:
            if not isinstance(item, dict) or type(item.get("run_number")) is not int or type(item.get("id")) is not int:
                raise ValueError("Invalid workflow run identity")
            number, ident = item["run_number"], item["id"]
            if number <= policy["anchor_run_number"]:
                reached_anchor = True
                break
            if number >= current_number or ident >= current_run_id:
                continue
            if (item.get("status") == "completed" and item.get("conclusion") == "success"
                and item.get("event") in ("push", "workflow_dispatch") and ident not in seen):
                seen.add(ident)
                count += 1
        if reached_anchor or len(runs) < 100:
            return count
    raise ValueError("V1 pagination limit reached: never reset counter silently")


def manifest(policy, sha, ident, number, count):
    if not SHA40.fullmatch(sha) or ident < 1:
        raise ValueError("Invalid SHA/run identity")
    name, code = compute(count, number, policy)
    return {"schema_version": 1, "repo": REPO, "workflow": WORKFLOW,
            "source_sha": sha, "run_id": ident, "run_number": number,
            "anchor_run_number": policy["anchor_run_number"],
            "successful_prior_runs": count,
            "successes_per_minor": policy["successes_per_minor"],
            "version_name": name, "version_code": code}


def validate(meta, policy, sha, ident, number):
    if not isinstance(meta, dict):
        raise ValueError("Invalid manifest")
    expected = manifest(policy, sha, ident, number, meta.get("successful_prior_runs"))
    if meta != expected:
        raise ValueError("Version manifest conflicts with source SHA, run or version rule")
    return meta


def append_env(path, meta):
    path = Path(path)
    if not path.is_file() or path.is_symlink():
        raise ValueError("GITHUB_ENV not available")
    with path.open("a", encoding="utf-8") as stream:
        stream.write(f"AURELIS_VERSION_NAME={meta['version_name']}\n")
        stream.write(f"AURELIS_VERSION_CODE={meta['version_code']}\n")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("action", choices=("generate", "adopt"))
    p.add_argument("--policy", default=".github/version-policy.json")
    p.add_argument("--manifest", default="ci-b01/version-metadata.json")
    p.add_argument("--env-file", default=os.environ.get("GITHUB_ENV", "/dev/null"))
    opts = p.parse_args()
    policy = read_policy(opts.policy)
    sha = os.environ.get("GITHUB_SHA", "").lower()
    ident = int(os.environ.get("GITHUB_RUN_ID", "0"))
    number = int(os.environ.get("GITHUB_RUN_NUMBER", "0"))
    if opts.action == "generate":
        count = count_completed(os.environ.get("GITHUB_TOKEN", ""), ident, number, policy)
        data = manifest(policy, sha, ident, number, count)
        path = Path(opts.manifest)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    else:
        path = Path(opts.manifest)
        if path.is_symlink() or not path.is_file():
            raise ValueError("Same-run B01 version manifest missing")
        data = validate(json.loads(path.read_text(encoding="utf-8")), policy, sha, ident, number)
    append_env(opts.env_file, data)
    print(f"V1 PASS {data['version_name']} code={data['version_code']} prior_successes={data['successful_prior_runs']}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, TypeError, KeyError, json.JSONDecodeError) as error:
        print("V1 FAIL: " + str(error), file=sys.stderr)
        sys.exit(1)
