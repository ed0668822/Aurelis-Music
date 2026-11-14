#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Summarize Debug and Release Android Lint without inventing missing evidence."""
from __future__ import annotations

import collections
import json
import os
from pathlib import Path
import shutil
import sys
import xml.etree.ElementTree as ET

VARIANTS = ("debug", "release")


def summarize(root: Path, output: Path, outcome: str) -> dict:
    output.mkdir(parents=True, exist_ok=True)
    variant_reports = {}
    rows = []
    for variant in VARIANTS:
        source = root / f"app/build/reports/lint-results-{variant}.xml"
        target = output / f"lint-results-{variant}.xml"
        target.unlink(missing_ok=True)
        issues = []
        diagnostics = []
        if not source.is_file():
            diagnostics.append(f"Missing {variant} lint XML")
        else:
            try:
                tree = ET.parse(source)
                if tree.getroot().tag != "issues":
                    raise ValueError("Expected <issues> root")
                for issue in tree.findall("issue"):
                    loc = issue.find("location")
                    issues.append({
                        "variant": variant,
                        "severity": issue.get("severity", "Unknown"),
                        "id": issue.get("id", "Unknown"),
                        "file": loc.get("file", "") if loc is not None else "",
                        "line": loc.get("line", "") if loc is not None else "",
                        "message": issue.get("message", ""),
                    })
                shutil.copyfile(source, target)
            except (ET.ParseError, ValueError, OSError) as error:
                diagnostics.append(f"{variant} invalid XML: {error}")
        rows.extend(issues)
        counts = collections.Counter(item["severity"] for item in issues)
        variant_reports[variant] = {
            "xml_parsed": not diagnostics,
            "issues": len(issues),
            "errors": counts["Error"] + counts["Fatal"],
            "warnings": counts["Warning"],
            "information": counts["Information"],
            "diagnostics": diagnostics,
        }

    fields = ("variant", "severity", "id", "file", "line", "message")
    def safe(value):
        return str(value).replace("\t", " ").replace("\r", " ").replace("\n", " ")

    with (output / "issues.tsv").open("w", encoding="utf-8") as fp:
        fp.write("\t".join(fields) + "\n")
        for row in rows:
            fp.write("\t".join(safe(row[key]) for key in fields) + "\n")

    passed = outcome == "success" and all(
        info["xml_parsed"] and info["errors"] == 0
        for info in variant_reports.values()
    )
    result = {
        "schema_version": 1, "gradle_outcome": outcome,
        "passed_gate": passed, "variants": variant_reports,
    }
    (output / "lint-results.json").write_text(
        json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    lines = [
        "# B02 — Android Lint: Debug and Release",
        f"- Gradle: **{outcome}**",
        f"- Per-variant checks: **{'PASS' if passed else 'FAIL'}**",
        "",
        "| Variant | Parsed XML | Issues | Errors | Warnings | Info |",
        "|---|---|---:|---:|---:|---:|",
    ]
    for variant, info in variant_reports.items():
        lines.append(
            f"| {variant} | {'yes' if info['xml_parsed'] else 'no'} | "
            f"{info['issues']} | {info['errors']} | {info['warnings']} | {info['information']} |"
        )
        lines += [f"- {err}" for err in info["diagnostics"]]
    lines += ["", "Reports preserve both variants. Lint is not runtime or per-file compilation proof."]
    content = "\n".join(lines) + "\n"
    (output / "summary.md").write_text(content, encoding="utf-8")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as fp:
            fp.write(content)
    return result


def main():
    result = summarize(Path.cwd(), Path("ci-b02"), os.getenv("LINT_OUTCOME", "not_started"))
    return 0 if result["passed_gate"] else 1


if __name__ == "__main__":
    sys.exit(main())
