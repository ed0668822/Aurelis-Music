#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""F4: prove every tracked main Kotlin/Java source was a Debug task input."""
from __future__ import annotations
import argparse
import json
from pathlib import Path

TASKS = {"Kotlin": ":app:compileDebugKotlin",
         "Java": ":app:compileDebugJavaWithJavac"}
GOOD = {"SUCCESS", "FROM-CACHE", "UP-TO-DATE"}

def audit(inventory: list, log: str, sha: str) -> dict:
    expected: set[tuple[str,str]] = set()
    for row in inventory:
        if (row.get("role") == "MANAGED_SOURCE" and row.get("source_set") == "main"
                and row.get("module") == "app" and row.get("language") in TASKS
                and row.get("kind") != "test"):
            expected.add((TASKS[row["language"]], row["path"]))
    inputs = set()
    states = {}
    errors = []
    for line in log.splitlines():
        if not line.startswith(("AURELIS_TASK_INPUT ", "AURELIS_TASK_RESULT ", "AURELIS_INPUT_ERROR ")):
            continue
        prefix, data = line.split(" ", 1)
        try:
            item = json.loads(data)
        except ValueError:
            errors.append("MALFORMED_GRADLE_OBSERVATION")
            continue
        task = item.get("task")
        if prefix == "AURELIS_TASK_INPUT" and item.get("origin") in ("tracked", "tracked-directory-input"):
            inputs.add((task, item.get("path")))
        elif prefix == "AURELIS_TASK_RESULT" and task in TASKS.values():
            if task in states and states[task] != item.get("state"):
                errors.append("CONFLICTING_TASK_RESULT:" + task)
            states[task] = item.get("state")
        elif prefix == "AURELIS_INPUT_ERROR" and task in TASKS.values():
            errors.append("TASK_INPUT_INSPECTION_FAILED:" + str(task))
    missing = sorted(expected - inputs)
    if not expected:
        errors.append("NO_EXPECTED_TRACKED_MAIN_SOURCES")
    if missing:
        errors.append("TRACKED_DEBUG_SOURCE_INPUTS_MISSING")
    for task in {task for task, _ in expected}:
        if states.get(task) not in GOOD:
            errors.append("NO_SUCCESSFUL_DEBUG_TASK_RESULT:" + task)
    return {
        "schema": 1, "source_sha": sha,
        "status": "PASS" if not errors else "FAIL",
        "expected_source_count": len(expected),
        "matched_source_count": len(expected & inputs),
        "missing": [path for _, path in missing],
        "task_results": states,
        "problems": sorted(set(errors)),
        "meaning": "Observed Gradle task inputs are not by themselves proof of emitted classes or APK presence.",
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--inventory", type=Path, default=Path("ci-artifacts/source-inventory.json"))
    parser.add_argument("--log", type=Path, default=Path("ci-b01/gradle-build.log"))
    parser.add_argument("--output", type=Path, default=Path("ci-b01/f4-debug-inputs.json"))
    parser.add_argument("--sha", required=True)
    args = parser.parse_args()
    result = audit(json.loads(args.inventory.read_text(encoding="utf-8")),
                   args.log.read_text(encoding="utf-8", errors="replace"), args.sha)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"F4 Debug inputs: {result['matched_source_count']}/{result['expected_source_count']} ({result['status']})")
    if result["status"] != "PASS":
        raise SystemExit("F4 FAIL: " + ",".join(result["problems"]))

if __name__ == "__main__":
    main()
