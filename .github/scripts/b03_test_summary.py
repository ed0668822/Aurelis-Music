#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later
"""B03 evidence: distinguish compilation, tests, timeout and Android runtime."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET

VARIANTS = ("debug", "release")
ANDROID_CASES = {"profileAndLibrarySettingsSurviveRecreation",
                 "profileAndLibrarySettingsPreserveRealPlayback"}


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace") if path.is_file() else ""


def first_log_error(log: str) -> str:
    for line in log.splitlines():
        if re.search(r"(?:^|\s)e: |Could not resolve|Too Many Requests|Execution failed for task", line):
            return line.strip()[:600]
    return "No detailed error available; inspect the phase log."


def active_test(log: str) -> str | None:
    active = {}
    for line in log.splitlines():
        match = re.search(r"AURELIS_TEST_(START|END) class=(.*?) test=(.*?)(?: result=\w+)?$", line)
        if match:
            key = match[2] + "#" + match[3]
            if match[1] == "START":
                active[key] = None
            else:
                active.pop(key, None)
    return next(reversed(active), None)


def summarize_variant(root: Path, output: Path, variant: str, outcome: str,
                      compilation: str) -> tuple[dict, list[list[str]]]:
    sources = sorted((root / f"app/build/test-results/test{variant.title()}UnitTest").glob("TEST-*.xml"))
    dest = output / "junit" / variant
    dest.mkdir(parents=True, exist_ok=True)
    totals = dict.fromkeys(("tests", "failed", "errors", "skipped"), 0)
    parse_errors, rows, failures = [], [], []
    for source in sources:
        shutil.copy2(source, dest / source.name)
        try:
            parsed = ET.parse(source).getroot()
            if parsed.tag not in {"testsuite", "testsuites"}:
                raise ValueError("Invalid JUnit root: " + parsed.tag)
            suites = [parsed] if parsed.tag == "testsuite" else parsed.findall(".//testsuite")
            if not suites:
                raise ValueError("No JUnit testsuite found")
            for suite in suites:
                for case in suite.findall("testcase"):
                    totals["tests"] += 1
                    kind, problem = "PASSED", None
                    for tag, result, counter in (("failure", "FAILED", "failed"),
                                                  ("error", "ERROR", "errors"),
                                                  ("skipped", "SKIPPED", "skipped")):
                        element = case.find(tag)
                        if element is not None:
                            kind, problem = result, element
                            totals[counter] += 1
                            break
                    clazz = case.get("classname", suite.get("name", ""))
                    rows.append([variant, clazz, case.get("name", ""), kind, case.get("time", "")])
                    if kind in {"FAILED", "ERROR"}:
                        trace = problem.text or ""
                        location = re.search(r"at com\.music\.aurelis\.[^\n]+\([^\n]+:\d+\)", trace)
                        failures.append({"phase": "TEST_FAILURE", "class": clazz,
                                         "test": case.get("name", ""),
                                         "message": (problem.get("message") or trace.strip().split("\n")[0]
                                                     or problem.get("type") or kind)[:600],
                                         "source": location[0] if location else None})
        except (OSError, ET.ParseError, ValueError) as error:
            parse_errors.append(f"{source.name}: {error}")
    passed = totals["tests"] - totals["failed"] - totals["errors"] - totals["skipped"]
    test_log, compile_log = read(output / f"{variant}.log"), read(output / f"compile-{variant}.log")
    exits = re.findall(r"AURELIS_PHASE_EXIT phase=\S+ exit_code=(\d+)", test_log)
    first = None
    if compilation != "success":
        phase = "COMPILATION_NOT_RUN"
        if compilation == "failure":
            phase = "DEPENDENCY_RESOLUTION_FAILED" if "Could not resolve" in compile_log else "COMPILATION_FAILED"
        first = {"phase": phase, "message": first_log_error(compile_log)}
    elif exits and int(exits[-1]) in {124, 137}:
        first = {"phase": "TEST_TIMEOUT" if int(exits[-1]) == 124 else "TEST_KILLED",
                 "message": "Test command timed out" if int(exits[-1]) == 124 else "Test command killed; cause requires logs",
                 "active_test": active_test(test_log), "exit_code": int(exits[-1])}
    elif failures:
        first = failures[0]
    elif parse_errors:
        first = {"phase": "REPORT_INVALID", "message": parse_errors[0]}
    elif outcome != "success":
        first = {"phase": "TESTS_NOT_RUN" if outcome in {"skipped", "cancelled"} else "TEST_EXECUTION_FAILED",
                 "message": first_log_error(test_log), "active_test": active_test(test_log)}
    elif not sources or not totals["tests"]:
        first = {"phase": "TEST_RESULTS_MISSING", "message": "No nonempty JUnit evidence"}
    elif totals["skipped"]:
        first = {"phase": "TESTS_SKIPPED", "message": f"{totals['skipped']} skipped JUnit cases"}
    result = {**totals, "passed": passed, "outcome": outcome, "compile_outcome": compilation,
              "xml_files": len(sources), "parse_errors": parse_errors, "first_failure": first,
              "passed_gate": first is None and passed > 0}
    return result, rows


def android_cases_passed(cases) -> bool:
    return (isinstance(cases, list) and len(cases) == 2 and all(isinstance(c, dict) for c in cases)
            and {c.get("method") for c in cases} == ANDROID_CASES
            and all(c.get("passed") is True and not c.get("problems") for c in cases))


def summarize_android(root: Path, output: Path, outcome: str, sha: str) -> dict:
    source = root / "ci-ui-comparison"
    if source.is_dir():
        shutil.copytree(source, output / "android16", dirs_exist_ok=True)
    try:
        evidence = json.loads(read(source / "settings-navigation.json"))
        if not isinstance(evidence, dict):
            raise ValueError("Expected Android navigation object")
    except (OSError, ValueError) as error:
        evidence = {"error": "Missing or invalid Android navigation evidence: " + str(error)}
    device, cases = evidence.get("device"), evidence.get("cases")
    device = device if isinstance(device, dict) else {}
    cases = cases if isinstance(cases, list) else []
    valid_cases = android_cases_passed(cases)
    identity = bool(re.fullmatch(r"[0-9a-f]{40}", sha) and evidence.get("source_sha") == sha)
    passed = (outcome == "success" and evidence.get("passed") is True and identity
              and device.get("api") == "36" and device.get("expected_api") == "36" and valid_cases)
    return {"outcome": outcome, "passed_gate": passed, "api": device.get("api"),
            "source_sha_verified": identity, "cases": cases,
            "first_failure": None if passed else {
                "phase": "ANDROID_RUNTIME_FAILED" if cases else "ANDROID_SETUP_OR_EVIDENCE_FAILED",
                "message": evidence.get("error") or "Android16 navigation incomplete, failed or identity/API mismatch"},
            "limitations": "Android emulator; physical HyperOS and pixel parity are not certified"}


def safe(value) -> str:
    return str(value).replace("\t", " ").replace("\r", " ").replace("\n", " ").replace("|", "\\|")


def summarize(root: Path, output: Path, outcomes: dict, compilations: dict,
              android_outcome: str, sha: str) -> dict:
    output.mkdir(parents=True, exist_ok=True)
    lines = ["# B03 — Unit tests", f"- Commit: `{sha}`", "",
             "| Variant | Compile | Test command | JUnit cases | Passed | Failed | Errors | Skipped | XML files |",
             "|---|---|---|---:|---:|---:|---:|---:|---:|"]
    results, rows = {}, []
    for variant in VARIANTS:
        info, cases = summarize_variant(root, output, variant, outcomes.get(variant, "skipped"),
                                        compilations.get(variant, "skipped"))
        results[variant] = info
        rows.extend(cases)
        lines.append(f"| {variant} | {info['compile_outcome']} | {info['outcome']} | {info['tests']} | "
                     f"{info['passed']} | {info['failed']} | {info['errors']} | {info['skipped']} | {info['xml_files']} |")
    results["android16_navigation"] = summarize_android(root, output, android_outcome, sha)
    runtime = results["android16_navigation"]
    lines += ["", "## First failure by phase"]
    for name, info in results.items():
        failure = info["first_failure"]
        if failure:
            detail = {k: v for k, v in failure.items() if v is not None}
            lines.append(f"- **{name}: {failure['phase']}** — {safe(json.dumps(detail, ensure_ascii=False))}")
        else:
            lines.append(f"- {name}: PASS")
    lines += ["", f"- Android16 emulator navigation: **{'PASS' if runtime['passed_gate'] else 'FAIL / incomplete'}**.",
              "- Unit graphics captures use software fixtures; emulator tests exercise the real Android Activity.",
              "- No visual-identity or physical-device claim follows from either result.",
              "- Missing/empty/invalid XML, skipped cases and failed compilation cannot pass B03.",
              "- Coverage remains a separate required gate; missing coverage after a test failure is a consequence."]
    (output / "test-cases.tsv").write_text("variant\tclass\tname\tresult\ttime_seconds\n" +
        "".join("\t".join(safe(v) for v in row) + "\n" for row in rows), encoding="utf-8")
    (output / "results.json").write_text(json.dumps(results, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    (output / "summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    return results


def gate(results: dict) -> list[str]:
    failed = []
    for variant in VARIANTS:
        info = results.get(variant, {})
        counts = [info.get(k) for k in ("tests", "passed", "failed", "errors", "skipped", "xml_files")]
        if not (info.get("passed_gate") is True and info.get("outcome") == "success"
                and info.get("compile_outcome") == "success"
                and all(type(c) is int and c >= 0 for c in counts)
                and counts[0] == counts[1] > 0 and counts[2:5] == [0, 0, 0]
                and counts[5] > 0 and info.get("parse_errors") == [] and info.get("first_failure") is None):
            failed.append(variant)
    runtime = results.get("android16_navigation", {})
    if not (runtime.get("passed_gate") is True and runtime.get("outcome") == "success"
            and runtime.get("api") == "36" and runtime.get("source_sha_verified") is True
            and runtime.get("first_failure") is None and android_cases_passed(runtime.get("cases"))):
        failed.append("android16_navigation")
    return failed


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--output", type=Path, default=Path("ci-b03"))
    parser.add_argument("--gate", action="store_true")
    args = parser.parse_args()
    if args.gate:
        try:
            result = json.loads((args.output / "results.json").read_text(encoding="utf-8"))
            if not isinstance(result, dict):
                raise ValueError("Expected B03 object")
            failed = gate(result)
        except (OSError, ValueError, TypeError, AttributeError) as error:
            print("B03 FAIL: missing or invalid report: " + str(error))
            return 1
        print("B03 FAIL: " + ", ".join(failed) if failed else "B03 PASS: both suites and Android16 navigation verified")
        return int(bool(failed))
    summarize(args.root, args.output,
              {v: os.getenv(v.upper() + "_OUTCOME", "skipped") for v in VARIANTS},
              {v: os.getenv("COMPILE_" + v.upper() + "_OUTCOME", "skipped") for v in VARIANTS},
              os.getenv("ANDROID16_OUTCOME", "skipped"), os.getenv("GITHUB_SHA", "unknown"))
    summary = read(args.output / "summary.md")
    if os.getenv("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as fp:
            fp.write(summary)
    print(summary)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
