#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""B04 Gradle diagnostics: authenticated log provenance, not per-file compilation proof."""
import argparse
import hashlib
import json
import os
from pathlib import Path
from android_ci import parse_log, parse_inputs, redact


def collect(root, out, sha):
    out.mkdir(parents=True, exist_ok=True)
    dest = out / "diagnostics.json"
    dest.unlink(missing_ok=True)
    log = out / "build.log"
    meta = out / "metadata.json"
    problems, diagnostics, task_count = [], [], 0
    try:
        metadata = json.loads(meta.read_text()) if meta.is_file() and not meta.is_symlink() else {}
    except (OSError, ValueError):
        metadata = {}
    if metadata.get("source_sha") != sha or metadata.get("checkout_sha") != sha:
        problems.append("SOURCE_COMMIT_MISMATCH")
    log_sha = None
    if not log.is_file() or log.is_symlink():
        problems.append("MISSING_OR_UNSAFE_BUILD_LOG")
    else:
        raw = log.read_bytes()
        log_sha = hashlib.sha256(raw).hexdigest()
        tasks, found = parse_log(raw.decode("utf-8", errors="replace"), "build", root)
        _, input_errors = parse_inputs(raw.decode("utf-8", errors="replace"), "build", tasks)
        if input_errors:
            problems.append("MALFORMED_TASK_OBSERVATIONS")
        if any(task.get("result_conflict") for task in tasks):
            problems.append("CONFLICTING_TERMINAL_RESULTS")
        task_count = sum(task.get("terminal_result_observed") is True for task in tasks)
        if not task_count:
            problems.append("NO_TERMINAL_GRADLE_TASK_RESULTS")
        seen = set()
        for item in found:
            row = {k: item.get(k) for k in ("phase", "severity", "file", "line", "column")}
            row["message"] = redact(str(item.get("message", "")))
            key = json.dumps(row, sort_keys=True)
            if key not in seen:
                diagnostics.append(row)
                seen.add(key)
    report = {
        "schema_version": 1, "source_sha": sha,
        "status": "PASS_LOG_PARSED" if not problems else "INCOMPLETE_EVIDENCE",
        "build_log_sha256": log_sha, "terminal_gradle_tasks": task_count,
        "diagnostic_count": len(diagnostics), "diagnostics_sha256": None,
        "issues": sorted(set(problems)),
        "scope": "Recognized Gradle/compiler diagnostics; no per-file compilation claim",
    }
    if not problems:
        raw_diag = (json.dumps(diagnostics, ensure_ascii=False, indent=2) + "\n").encode()
        dest.write_bytes(raw_diag)
        report["diagnostics_sha256"] = hashlib.sha256(raw_diag).hexdigest()
    (out / "diagnostics-provenance.json").write_text(json.dumps(report, indent=2) + "\n")
    return report


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--root", type=Path, default=Path("."))
    p.add_argument("--out", type=Path, default=Path("ci-artifacts"))
    p.add_argument("--sha", default=os.environ.get("GITHUB_SHA", ""))
    a = p.parse_args()
    result = collect(a.root.resolve(), a.out.resolve(), a.sha)
    print(result["status"], "tasks=", result["terminal_gradle_tasks"],
          "diagnostics=", result["diagnostic_count"])
    raise SystemExit(0 if result["status"] == "PASS_LOG_PARSED" else 1)
