#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.1: reconcile tracked files and Gradle inputs without assuming per-file compilation."""
import argparse
import collections
import csv
import hashlib
import json
import os
from pathlib import Path
import sys
from android_ci import git, parse_log, parse_inputs, parse_source_roots

FIELDS = ("path","role","kind","source_set","file_state","sha256",
          "root_evidence","task_input_evidence","observed_tasks","individual_compilation")

def read(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None

def audit(root, output, sha):
    source = read(output / "source-inventory.json")
    meta = read(output / "metadata.json")
    issues = []
    if not isinstance(source,list) or not source:
        source = []
        issues.append("MISSING_OR_EMPTY_INVENTORY")
    if not isinstance(meta,dict) or meta.get("checkout_sha") != sha or meta.get("source_sha") != sha:
        issues.append("SOURCE_COMMIT_MISMATCH")
    tracked = set(git(root,"ls-files","-z").split("\0")) - {""}
    paths = [r.get("path") for r in source if isinstance(r,dict)]
    if len(paths) != len(source) or len(paths) != len(set(paths)) or set(paths) != tracked:
        issues.append("INVENTORY_CHECKOUT_MISMATCH")
    build = output / "build.log"
    roots, inputs, errors = [], [], []
    if build.is_file():
        text = build.read_text(encoding="utf-8",errors="replace")
        tasks, _ = parse_log(text,"build",root)
        roots, root_errors = parse_source_roots(text,"build")
        inputs, input_errors = parse_inputs(text,"build",tasks)
        errors = root_errors + input_errors
    else:
        issues.append("MISSING_BUILD_LOG")
    if errors: issues.append("COLLECTION_ERRORS")
    if not roots: issues.append("NO_SOURCE_ROOT_OBSERVATIONS")
    if not inputs: issues.append("NO_TASK_INPUT_OBSERVATIONS")
    by_path = collections.defaultdict(list)
    for item in inputs: by_path[item["path"]].append(item)
    rows = []
    for record in sorted((r for r in source if isinstance(r,dict) and isinstance(r.get("path"),str)),
                         key=lambda r:r["path"]):
        path = record["path"]
        state = record.get("file_state")
        file = root / path
        if state == "FILE" and record.get("role") != "SENSITIVE_CONFIGURATION":
            if file.is_symlink() or not file.is_file() or hashlib.sha256(file.read_bytes()).hexdigest() != record.get("sha256"):
                issues.append("FILE_HASH_OR_TYPE_MISMATCH:"+path)
        elif state == "DIRECTORY_OR_MISSING":
            issues.append("TRACKED_FILE_MISSING:"+path)
        elif state == "SYMLINK_NOT_FOLLOWED" and not file.is_symlink():
            issues.append("SYMLINK_CHANGED:"+path)
        elif state not in ("FILE","SYMLINK_NOT_FOLLOWED"):
            issues.append("UNRECOGNIZED_FILE_STATE:"+path)
        module = record.get("module")
        matches = [r for r in roots if r["source_set"] == record.get("source_set")
                   and r["project"].lstrip(":").replace(":","/") == module
                   and (r["path"] == "." or path == r["path"] or path.startswith(r["path"].rstrip("/")+"/"))]
        requires_root = record.get("role") in ("MANAGED_SOURCE","ANDROID_RESOURCE","ASSET","MANIFEST","PREBUILT_NATIVE")
        selected = by_path.get(path,[])
        rows.append({
            "path":path,"role":record.get("role"),"kind":record.get("kind"),
            "source_set":record.get("source_set"),"file_state":state,"sha256":record.get("sha256"),
            "root_evidence":("DECLARED_ROOT_OBSERVED" if matches else "NO_ROOT_EVIDENCE") if requires_root else "NOT_APPLICABLE",
            "task_input_evidence":"OBSERVED" if selected else "NOT_OBSERVED",
            "observed_tasks":";".join(sorted({x["task"]+"="+x["task_state"] for x in selected})),
            "individual_compilation":"NOT_ESTABLISHED",
        })
    result = {
        "schema_version":1,"source_sha":sha,"tracked_files":len(tracked),"inventory_files":len(source),
        "indexed_files":len(rows),"declared_roots":len(roots),"observed_task_inputs":len(inputs),
        "source_roles":dict(sorted(collections.Counter(r.get("role","UNKNOWN") for r in source if isinstance(r,dict)).items())),
        "issues":sorted(set(issues)),"collector_errors":errors,
        "individual_compilation_proven":False,
        "meaning":"Gradle roots and task inputs are observations, NOT per-file compilation certificates.",
    }
    result["status"] = "PASS_INVENTORY" if not issues else "INCOMPLETE_EVIDENCE"
    output.mkdir(parents=True,exist_ok=True)
    (output/"p2-inventory-coverage.json").write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    with (output/"p2-inventory-files.tsv").open("w",encoding="utf-8",newline="") as f:
        writer = csv.DictWriter(f,fieldnames=FIELDS,delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)
    (output/"p2-inventory-summary.md").write_text(
        "# P2.1 — Cobertura del inventario\n\n"
        + f"- Commit: {sha}\n- Resultado: {result['status']}\n"
        + f"- Git: {len(tracked)}; inventario: {len(source)}; entradas Gradle: {len(inputs)}\n"
        + "- Compilación individual: no demostrada.\n\n"
        + "\n".join("- "+issue for issue in result["issues"])+"\n",encoding="utf-8")
    return result

if __name__ == "__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--root",type=Path,default=Path("."))
    parser.add_argument("--out",type=Path,default=Path("ci-artifacts"))
    parser.add_argument("--sha",default=os.environ.get("GITHUB_SHA",""))
    args=parser.parse_args()
    result=audit(args.root.resolve(),args.out.resolve(),args.sha)
    print(result["status"],result["indexed_files"])
    sys.exit(0 if result["status"]=="PASS_INVENTORY" else 1)
