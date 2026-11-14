#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""F5: require measurable coverage for production source changes, never fake PASS."""
import json
from pathlib import Path
import sys

THRESHOLD = 80.0

def check(report):
    state = report.get("status")
    if state == "NO_EXECUTABLE_LINES" and report.get("changed_files") == 0:
        return True, "No changed app production files"
    if state != "MEASURED":
        return False, "Coverage is unavailable: " + str(report.get("reason",state))
    measured = report.get("coverage_percent")
    lines = report.get("executable_lines")
    if not isinstance(measured,(int,float)) or isinstance(measured,bool) or not isinstance(lines,int) or lines <= 0:
        return False, "Invalid changed-line numerator or denominator"
    if measured < THRESHOLD or report.get("threshold_met") is not True:
        return False, "Changed-line coverage below required 80 percent"
    return True, f"Changed-line coverage {measured}% >= {THRESHOLD}%"

def main():
    if len(sys.argv) != 2:
        raise SystemExit("Provide path to diff-coverage.json")
    path=Path(sys.argv[1])
    if not path.is_file() or path.is_symlink():
        raise SystemExit("F5 FAIL: coverage report missing")
    try:
        report=json.loads(path.read_text(encoding="utf-8"))
    except (OSError,ValueError) as error:
        raise SystemExit("F5 FAIL: invalid coverage JSON") from error
    passed,reason=check(report)
    print(("F5 PASS: " if passed else "F5 FAIL: ")+reason)
    if not passed:
        raise SystemExit(1)

if __name__=="__main__":
    main()
