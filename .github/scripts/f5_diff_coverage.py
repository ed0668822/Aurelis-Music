#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""F5: report JaCoCo line coverage on changed production lines only.

Missing merge-base, XML, or unresolvable source is NOT_MEASURED, never 100%.
Does not treat unit-test line coverage as Android instrumentation coverage.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET


def changed(root: Path, base: str) -> dict[str, set[int]]:
    diff = subprocess.run(
        ["git", "diff", "--unified=0", "--no-ext-diff", f"{base}...HEAD", "--", "app/src/main/java"],
        cwd=root, capture_output=True, text=True, check=True,
    ).stdout
    found: dict[str, set[int]] = {}
    current = ""
    for line in diff.splitlines():
        if line.startswith("+++ b/"):
            current = line[6:]
            if current.endswith((".kt", ".java")):
                found.setdefault(current, set())
            else:
                current = ""
        elif current and line.startswith("@@"):
            match = re.search(r"\+(\d+)(?:,(\d+))?", line)
            if match:
                start, count = int(match.group(1)), int(match.group(2) or 1)
                found[current].update(range(start, start + count))
    return found


def coverage(xml_files: list[Path]) -> dict[str, dict[int, bool]]:
    output = {}
    for path in xml_files:
        tree = ET.parse(path)
        if tree.getroot().tag != "report":
            raise ValueError("Not a JaCoCo coverage XML file")
        for package in tree.findall(".//package"):
            name = package.get("name", "")
            for source in package.findall("sourcefile"):
                namepart = source.get("name")
                if not namepart:
                    continue
                key = f"app/src/main/java/{name}/{namepart}"
                lines = output.setdefault(key, {})
                for line in source.findall("line"):
                    nr = int(line.attrib["nr"])
                    # Any instruction covered is a covered executable line.
                    lines[nr] = lines.get(nr, False) or int(line.get("ci", 0)) > 0
    return output


def analyze(root: Path, reports: Path, base: str, threshold: int = 80) -> dict:
    if not (root / ".git").exists():
        return {"status": "NOT_MEASURED", "reason": "GIT_HISTORY_UNAVAILABLE"}
    try:
        files = changed(root, base)
    except (subprocess.CalledProcessError, OSError):
        return {"status": "NOT_MEASURED", "reason": "MERGE_BASE_UNAVAILABLE"}
    xmls = sorted(reports.rglob("*.xml")) if reports.is_dir() else []
    if not xmls:
        return {"status": "NOT_MEASURED", "reason": "COVERAGE_XML_UNAVAILABLE"}
    try:
        available = coverage(xmls)
    except (ET.ParseError, KeyError, ValueError):
        return {"status": "NOT_MEASURED", "reason": "COVERAGE_XML_INVALID"}
    executable = covered = 0
    missing = []
    for file, modified in sorted(files.items()):
        if file not in available:
            if modified:
                missing.append(file)
            continue
        for line in modified:
            if line in available[file]:
                executable += 1
                covered += int(available[file][line])
    if missing:
        return {"status": "NOT_MEASURED", "reason": "CHANGED_SOURCE_MISSING_FROM_XML",
                "unmatched_sources": missing, "observed_executable_lines": executable}
    if executable == 0:
        return {"status": "NO_EXECUTABLE_LINES", "changed_files": len(files),
                "covered_lines": 0, "executable_lines": 0}
    percentage = round(100 * covered / executable, 2)
    return {
        "status": "MEASURED", "covered_lines": covered,
        "executable_lines": executable, "coverage_percent": percentage,
        "suggested_threshold": threshold, "threshold_met": percentage >= threshold,
        "gate": "ADVISORY_UNTIL_PR_TRIGGER_AND_BASELINE_APPROVED",
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--reports", type=Path, default=Path("ci-b03/coverage"))
    parser.add_argument("--base", default="origin/main")
    parser.add_argument("--output", type=Path, default=Path("ci-b03/diff-coverage.json"))
    args = parser.parse_args()
    result = analyze(args.root, args.reports, args.base)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print("F5 changed-line coverage:", result["status"], result.get("coverage_percent", result.get("reason", "")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
