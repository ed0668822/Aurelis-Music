#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.2: compiler task/input evidence, not a per-file compilation certificate.

For Kotlin/Java, only a matching compiler task input counts as selected input.
For C/C++, compile_commands.json proves declaration, not invocation or success.
"""
from __future__ import annotations

import argparse
import collections
import csv
import hashlib
import json
import os
from pathlib import Path
import sys

from android_ci import git, parse_inputs, parse_log

VARIANTS = ("debug", "release")
SOURCES = {"MANAGED_SOURCE", "NATIVE_SOURCE"}
TERMINAL = {"SUCCESS", "FAILED", "FROM-CACHE", "UP-TO-DATE", "SKIPPED", "NO-SOURCE"}
FIELDS = (
    "path", "variant", "role", "source_set", "language", "task", "task_state",
    "task_input_observed", "native_command_declared", "native_command_evidence",
    "compiler_evidence", "attributed_errors", "individual_compilation"
)


def load_json(path: Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def native_commands(root: Path, variant: str, issues: list[str]) -> dict:
    """Read only source paths and command-file hashes; never print command lines."""
    observed: dict[str, list[dict]] = collections.defaultdict(list)
    cxx = root / "app" / ".cxx"
    if not cxx.exists():
        return {}
    for path in sorted(cxx.rglob("compile_commands.json")):
        if path.is_symlink() or not path.is_file():
            issues.append("NATIVE_COMMAND_FILE_UNSAFE:" + path.name)
            continue
        relative = path.relative_to(root).as_posix()
        parts = [part.lower() for part in path.relative_to(cxx).parts]
        eligible = "debug" in parts if variant == "debug" else any(part in ("release", "relwithdebinfo") for part in parts)
        if not eligible:
            continue
        raw = path.read_bytes()
        try:
            commands = json.loads(raw)
        except (UnicodeError, ValueError):
            issues.append("MALFORMED_COMPILE_COMMANDS:" + relative)
            continue
        if not isinstance(commands, list):
            issues.append("INVALID_COMPILE_COMMANDS_TYPE:" + relative)
            continue
        checksum = hashlib.sha256(raw).hexdigest()
        for entry in commands:
            if not isinstance(entry, dict) or not isinstance(entry.get("file"), str) or not isinstance(entry.get("directory"), str):
                issues.append("INVALID_COMPILE_COMMAND_ENTRY:" + relative)
                continue
            source = Path(entry["file"])
            if not source.is_absolute():
                source = Path(entry["directory"]) / source
            resolved = source.resolve()
            if not resolved.is_relative_to(root.resolve()):
                continue
            key = resolved.relative_to(root.resolve()).as_posix()
            observed[key].append({"command_file": relative, "sha256": checksum})
    return dict(observed)


def compiler_state(state: str, has_input: bool) -> str:
    if not has_input:
        return "NO_MATCHING_COMPILER_TASK_INPUT"
    return {
        "SUCCESS": "COMPILER_TASK_SUCCEEDED_WITH_INPUT",
        "FROM-CACHE": "COMPILER_TASK_REUSED_WITH_INPUT",
        "UP-TO-DATE": "COMPILER_TASK_REUSED_WITH_INPUT",
        "FAILED": "COMPILER_TASK_FAILED_SIN_ERROR_ATRIBUIDO",
        "SKIPPED": "COMPILER_TASK_DID_NOT_COMPILE_INPUT",
        "NO-SOURCE": "COMPILER_TASK_DID_NOT_COMPILE_INPUT",
    }.get(state, "INPUT_OBSERVED_WITHOUT_TERMINAL_RESULT")


def native_state(states: list[str], declared: bool) -> str:
    if not declared:
        return "NO_NATIVE_COMPILE_COMMAND_EVIDENCE"
    if "FAILED" in states:
        return "NATIVE_COMMAND_DECLARED_BUILD_TASK_FAILED"
    if "SUCCESS" in states:
        return "NATIVE_COMMAND_DECLARED_BUILD_TASK_SUCCEEDED"
    if any(value in ("FROM-CACHE", "UP-TO-DATE") for value in states):
        return "NATIVE_COMMAND_DECLARED_BUILD_TASK_REUSED"
    if any(value in ("SKIPPED", "NO-SOURCE") for value in states):
        return "NATIVE_COMMAND_DECLARED_BUILD_TASK_NOT_EXECUTED"
    return "NATIVE_COMMAND_DECLARED_TASK_RESULT_UNKNOWN"


def audit(root: Path, out: Path, expected_sha: str) -> dict:
    inventory = load_json(out / "source-inventory.json")
    metadata = load_json(out / "metadata.json")
    issues: list[str] = []
    if not isinstance(inventory, list) or not inventory:
        issues.append("MISSING_OR_EMPTY_INVENTORY")
        inventory = []
    if not isinstance(metadata, dict) or metadata.get("checkout_sha") != expected_sha or metadata.get("source_sha") != expected_sha:
        issues.append("SOURCE_COMMIT_MISMATCH")
    build = out / "build.log"
    tasks, inputs, diagnostics = [], [], []
    if not build.is_file():
        issues.append("MISSING_BUILD_LOG")
    else:
        text = build.read_text(encoding="utf-8", errors="replace")
        tasks, diagnostics = parse_log(text, "build", root)
        inputs, errors = parse_inputs(text, "build", tasks)
        issues.extend(errors)
    tasks_by_name = {row["task"]: row for row in tasks}
    by_task_path = collections.defaultdict(list)
    for item in inputs:
        by_task_path[(item["task"], item["path"])].append(item)
    error_counts = collections.Counter(
        d["file"] for d in diagnostics if d.get("severity") == "error" and d.get("file")
    )
    native = {variant: native_commands(root, variant, issues) for variant in VARIANTS}
    seen = set()
    rows = []
    for entry in sorted((x for x in inventory if isinstance(x, dict) and x.get("role") in SOURCES),
                        key=lambda x: x.get("path", "")):
        path, role = entry.get("path"), entry.get("role")
        if not isinstance(path, str) or not path:
            issues.append("INVALID_SOURCE_PATH")
            continue
        if path in seen:
            issues.append("DUPLICATE_SOURCE_PATH:" + path)
        seen.add(path)
        module = entry.get("module", "app")
        source_set = entry.get("source_set", "")
        language = entry.get("language", "")
        for variant in VARIANTS:
            task = None
            state = "NOT_APPLICABLE"
            input_seen = False
            native_declared = False
            command_evidence = []
            if entry.get("kind") == "test":
                result = "TEST_SOURCE_OUT_OF_PRODUCTION_BUILD"
            elif source_set in ("debug", "release") and source_set != variant:
                result = "OTHER_VARIANT_SOURCE_SET"
            elif source_set not in ("main", variant):
                result = "SOURCE_SET_SELECTION_UNKNOWN"
            elif role == "MANAGED_SOURCE":
                suffix = "Kotlin" if language == "Kotlin" else "JavaWithJavac" if language == "Java" else None
                if suffix is None:
                    result = "LANGUAGE_COMPILER_UNKNOWN"
                else:
                    task = ":" + str(module).replace("/", ":") + ":compile" + variant.title() + suffix
                    observed = tasks_by_name.get(task)
                    state = observed["state"] if observed and observed.get("terminal_result_observed") else "NOT_OBSERVED"
                    input_seen = bool(by_task_path.get((task, path)))
                    result = compiler_state(state, input_seen)
            else:
                command_evidence = native[variant].get(path, [])
                native_declared = bool(command_evidence)
                prefix = ":" + str(module).replace("/", ":") + ":"
                native_tasks = [
                    row for name, row in tasks_by_name.items()
                    if name.startswith(prefix)
                    and (name[len(prefix):].startswith("externalNativeBuild" + variant.title())
                         or name[len(prefix):].startswith("buildCMake" + variant.title()))
                ]
                native_states = [
                    item["state"] for item in native_tasks if item.get("terminal_result_observed")
                ]
                state = ";".join(sorted(set(native_states))) if native_states else "NOT_OBSERVED"
                task = ";".join(sorted({item["task"] for item in native_tasks})) or None
                result = native_state(native_states, native_declared)
            rows.append({
                "path": path, "variant": variant, "role": role, "source_set": source_set,
                "language": language, "task": task, "task_state": state,
                "task_input_observed": input_seen, "native_command_declared": native_declared,
                "native_command_evidence": command_evidence, "compiler_evidence": result,
                "attributed_errors": error_counts[path], "individual_compilation": "NOT_ESTABLISHED",
            })
    if len(rows) != len(seen) * len(VARIANTS):
        issues.append("SOURCE_VARIANT_CARDINALITY_MISMATCH")
    if not tasks:
        issues.append("NO_BUILD_TASK_OBSERVATIONS")
    if any(t.get("result_conflict") for t in tasks):
        issues.append("CONFLICTING_TERMINAL_TASK_RESULTS")
    if any(r["compiler_evidence"] == "COMPILER_TASK_SUCCEEDED_WITH_INPUT"
           and r["task_state"] != "SUCCESS" for r in rows):
        issues.append("INCONSISTENT_TASK_CLASSIFICATION")
    counts = collections.Counter((r["variant"], r["compiler_evidence"]) for r in rows)
    report = {
        "schema_version": 1, "source_sha": expected_sha,
        "status": "EVIDENCE_RECORDED" if not issues else "INCOMPLETE_EVIDENCE",
        "source_files": len(seen), "variant_rows": len(rows),
        "terminal_task_results": sum(bool(t.get("terminal_result_observed")) for t in tasks),
        "per_variant": {
            variant: {name: count for (v, name), count in sorted(counts.items()) if v == variant}
            for variant in VARIANTS
        },
        "issues": sorted(set(issues)), "individual_compilation_proven": False,
        "interpretation": (
            "Task outcomes apply to Gradle tasks, not each file. CMake commands are declarations, "
            "not evidence each source was compiled. No diagnostic is not proof of compilation."
        ),
    }
    out.mkdir(exist_ok=True, parents=True)
    (out / "p2-compiler-evidence.json").write_text(
        json.dumps({"summary": report, "rows": rows}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    with (out / "p2-compiler-evidence.tsv").open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=FIELDS, delimiter="\t", extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            flatten = dict(row)
            flatten["native_command_evidence"] = ";".join(
                item["command_file"] + ":" + item["sha256"] for item in row["native_command_evidence"]
            )
            writer.writerow(flatten)
    lines = [
        "# P2.2 — Evidencia del compilador", "",
        "- Commit: " + expected_sha,
        "- Estado: " + report["status"],
        "- Fuentes: " + str(len(seen)),
        "- Registros Debug/Release: " + str(len(rows)),
        "- Compilación individual: NO ESTABLECIDA", "",
        "## Categorías por variante",
    ]
    for variant in VARIANTS:
        lines += ["", "### " + variant]
        lines.extend("- " + name + ": " + str(count) for name, count in report["per_variant"][variant].items())
    lines += ["", "## Errores de recolección"]
    lines.extend("- " + issue for issue in report["issues"])
    if not report["issues"]:
        lines += ["- Ninguno."]
    lines += ["", report["interpretation"]]
    (out / "p2-compiler-summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    return report


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--root", type=Path, default=Path("."))
    p.add_argument("--out", type=Path, default=Path("ci-artifacts"))
    p.add_argument("--sha", default=os.environ.get("GITHUB_SHA", ""))
    args = p.parse_args()
    result = audit(args.root.resolve(), args.out.resolve(), args.sha)
    print(result["status"], "sources=", result["source_files"], "rows=", result["variant_rows"])
    sys.exit(0 if result["status"] == "EVIDENCE_RECORDED" else 1)
