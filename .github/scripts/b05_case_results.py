#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Attest seven isolated Media3 AndroidJUnitRunner invocations, never infer a pass."""
from __future__ import annotations

import json
from pathlib import Path
import re
import sys

PREFIX = "com.music.aurelis.playback."
CASES = (
    ("local", "RealLocalPlaybackInstrumentedTest",
     "generatedPcmWavPlaysPausesSeeksAndResumesThroughMediaSession", "p3-local-wav-pass.txt"),
    ("queue", "RealQueueTransitionsInstrumentedTest",
     "twoOriginalWavsNextPreviousAndQueueRemovalUseActualMediaSession", "p3-queue-transitions-pass.txt"),
    ("auto", "RealAutoRepeatInstrumentedTest",
     "naturalTrackEndAdvancesAndRepeatOneLoopsOnRealMediaSession", "p3-auto-repeat-pass.txt"),
    ("recovery", "RealErrorRecoveryInstrumentedTest",
     "missingLocalWavReportsErrorThenSameSessionRecoversToValidWav", "p3-error-recovery-pass.txt"),
    ("recreation", "RealSessionResilienceInstrumentedTest",
     "activityRecreationRetainsMediaSessionQueueAndAdvancingPosition", "p3-activity-recreation-pass.txt"),
    ("repeat-all", "RealSessionResilienceInstrumentedTest",
     "repeatAllCyclesFromSecondOriginalWavBackToFirst", "p3-repeat-all-pass.txt"),
    ("continuous", "RealSessionResilienceInstrumentedTest",
     "continuousTwelveSecondDecodeAndSeekRemainStable", "p3-continuous-playback-pass.txt"),
)


def verdict(log: str, klass: str, method: str, code: str) -> tuple[bool, list[str]]:
    failures: list[str] = []
    if code != "0":
        failures.append("INSTRUMENTATION_EXIT_NONZERO_OR_NOT_RUN")
    if not re.search(r"(?m)^OK \(1 test\)\s*$", log):
        failures.append("ONE_TEST_JUNIT_PASS_MISSING")
    if not re.search(r"(?m)^INSTRUMENTATION_CODE: -1\s*$", log):
        failures.append("ANDROID_RUNNER_SUCCESS_MISSING")
    class_lines = re.findall(r"(?m)^INSTRUMENTATION_STATUS: class=(\S+)", log)
    if not class_lines or set(class_lines) != {PREFIX + klass}:
        failures.append("CLASS_IDENTITY_UNVERIFIED")
    methods = re.findall(r"(?m)^INSTRUMENTATION_STATUS: test=(\S+)", log)
    if not methods or set(methods) != {method}:
        failures.append("TEST_IDENTITY_UNVERIFIED")
    if re.search(r"(?m)^FAILURES!!!", log) or "INSTRUMENTATION_STATUS_CODE: -2" in log or "INSTRUMENTATION_FAILED" in log:
        failures.append("ANDROID_RUNNER_REPORTED_FAILURE")
    return not failures, failures


def summarize(folder: Path) -> dict:
    results = {}
    tests = folder / "p3-tests"
    for marker in ("p3-instrumentation-pass.txt", *(case[3] for case in CASES)):
        (folder / marker).unlink(missing_ok=True)
    for slug, klass, method, marker in CASES:
        logpath = tests / f"{slug}.log"
        exitpath = tests / f"{slug}.exit"
        log = logpath.read_text(errors="replace") if logpath.is_file() else ""
        exit_code = exitpath.read_text().strip() if exitpath.is_file() else "NOT_RUN"
        passed, problems = verdict(log, klass, method, exit_code)
        results[slug] = {
            "test": PREFIX + klass + "#" + method, "passed": passed,
            "exit_code": exit_code, "problems": problems,
            "log": f"p3-tests/{slug}.log",
            "logcat": f"p3-tests/{slug}-logcat.txt",
        }
        if passed:
            (folder / marker).write_text(f"PASS: isolated {slug} Media3 test\n")
    all_passed = len(results) == 7 and all(row["passed"] for row in results.values())
    if all_passed:
        (folder / "p3-instrumentation-pass.txt").write_text("PASS: all seven isolated real Media3 tests\n")
    summary = {
        "schema_version": 1, "test_count": len(results),
        "passed_count": sum(1 for row in results.values() if row["passed"]),
        "all_passed": all_passed, "tests": results,
    }
    (folder / "p3-case-results.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return summary


def main() -> int:
    if sys.argv[1:] == ["list"]:
        for slug, klass, method, _ in CASES:
            print(f"{slug}|{PREFIX}{klass}#{method}")
        return 0
    if len(sys.argv) == 3 and sys.argv[1] == "summarize":
        result = summarize(Path(sys.argv[2]))
        print(f"B05 per-case results: {result['passed_count']}/7 PASS")
        return 0 if result["all_passed"] else 1
    print("Usage: b05_case_results.py list | summarize <folder>", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main())
