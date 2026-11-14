#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""P2.3: independently reconcile per-file APK/DEX/R8 and native evidence."""
import argparse
import collections
import csv
import json
import os
from pathlib import Path
import re
import sys

VARIANTS = ("debug", "release")
PASS = "REQUIRED_CONTENT_VERIFIED_WITH_COVERAGE_LIMITS"
FIELDS = ("path","variant","role","kind","compiler_evidence","apk_state",
          "class_evidence","packaging_evidence","class_count",
          "native_command_declared","declaration_hint",
          "individual_compilation","runtime_evidence")


def load(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def simple_source(path):
    """Only a syntax hint, never a Kotlin compilation verdict."""
    if path.suffix != ".kt" or path.is_symlink() or not path.is_file():
        return "NOT_CHECKED"
    try:
        content = path.read_text(encoding="utf-8")
    except (OSError, UnicodeError):
        return "NOT_CHECKED"
    content = re.sub(r"/\*.*?\*/", "", content, flags=re.S)
    content = re.sub(r"(?m)^\s*//.*$", "", content)
    lines = [x.strip() for x in content.splitlines() if x.strip()]
    return ("ONLY_PACKAGE_IMPORT_COMMENT_TEXT" if lines and
            all(re.fullmatch(r"(?:package|import)\s+[\w.*]+(?:\s+as\s+\w+)?", x) for x in lines)
            else "OTHER_OR_UNKNOWN_SOURCE_TEXT")


def classify(source, entry, compiler, hint, variant):
    role = source.get("role")
    state = entry.get("state", "NOT_OBSERVED")
    classes = entry.get("classes")
    classes = classes if isinstance(classes, list) else []
    mapped = [c.get("state") for c in classes if isinstance(c, dict)]
    class_evidence, packaging = "NOT_APPLICABLE", "NOT_ESTABLISHED"
    if source.get("kind") == "test" or source.get("app_packaging_expected") is False:
        packaging = "OUT_OF_PRODUCTION_APK_SCOPE"
    elif state == "OTHER_VARIANT_BY_PATH_POLICY":
        packaging = "OTHER_VARIANT"
    elif role == "MANAGED_SOURCE":
        if state == "COMPILER_CLASSES_MAPPED_TO_DEX" and mapped:
            if all(x in ("DEX_CLASS_PRESENT","R8_MAPPED_CLASS_PRESENT") for x in mapped):
                class_evidence = ("R8_OR_DEX_CLASSES_OBSERVED" if "R8_MAPPED_CLASS_PRESENT" in mapped
                                  else "DEX_CLASSES_OBSERVED")
            else:
                class_evidence = "INCONSISTENT_CLASS_MAPPING"
        elif state == "AMBIGUOUS_SOURCE_CLASS_MAPPING":
            class_evidence = "AMBIGUOUS_SOURCE_PROVENANCE"
        elif hint == "ONLY_PACKAGE_IMPORT_COMMENT_TEXT" and state == "CLASS_PRESENCE_EVIDENCE_INCOMPLETE":
            class_evidence = "NO_STANDALONE_DECLARATION_HINT"
        else:
            class_evidence = "CLASS_PRESENCE_NOT_ESTABLISHED"
    elif role == "NATIVE_SOURCE":
        packaging = ("CMAKE_COMMAND_DECLARED_NOT_EXECUTION_PROOF" if
                     state == "COMPILE_COMMAND_DECLARED_NOT_EXECUTION_PROOF"
                     else "NATIVE_COMMAND_NOT_OBSERVED")
    elif state == "APK_BYTES_MATCH":
        packaging = "APK_BYTES_VERIFIED"
    elif state == "RESOURCE_SYMBOLS_PRESENT_CONFIGURATION_NOT_PROVEN":
        packaging = "RESOURCE_SYMBOLS_ONLY"
    else:
        packaging = state
    return {"path":source["path"],"variant":variant,"role":role,"kind":source.get("kind"),
            "compiler_evidence":compiler.get("compiler_evidence", "NOT_APPLICABLE"),
            "apk_state":state,"class_evidence":class_evidence,"packaging_evidence":packaging,
            "class_count":len(classes),
            "native_command_declared":compiler.get("native_command_declared",False) if role=="NATIVE_SOURCE" else None,
            "declaration_hint":hint,"individual_compilation":"NOT_ESTABLISHED",
            "runtime_evidence":"NOT_TESTED_PER_FILE"}


def audit(root, output, sha):
    inventory = load(output/"source-inventory.json")
    metadata = load(output/"metadata.json")
    compiler = load(output/"p2-compiler-evidence.json")
    verified = load(output/"apk-verification.json")
    inventory = inventory if isinstance(inventory,list) else []
    metadata = metadata if isinstance(metadata,dict) else {}
    compiler = compiler if isinstance(compiler,dict) else {}
    verified = verified if isinstance(verified,dict) else {}
    issues = []
    if not inventory: issues.append("MISSING_INVENTORY")
    if metadata.get("source_sha") != sha or metadata.get("checkout_sha") != sha:
        issues.append("SOURCE_COMMIT_MISMATCH")
    summary = compiler.get("summary")
    if not isinstance(summary,dict) or summary.get("source_sha") != sha or summary.get("status") != "EVIDENCE_RECORDED":
        issues.append("P2_COMPILER_EVIDENCE_UNVERIFIED")
    if verified.get("status") != "success":
        issues.append("APK_VERIFICATION_UNVERIFIED")
    source_by_path = {}
    for x in inventory:
        if not isinstance(x,dict) or not isinstance(x.get("path"),str) or not x["path"]:
            issues.append("INVALID_INVENTORY_ROW"); continue
        if x["path"] in source_by_path: issues.append("DUPLICATE_INVENTORY_PATH:"+x["path"])
        source_by_path[x["path"]] = x
    compiler_by_key = {}
    for x in compiler.get("rows",[]):
        if not isinstance(x,dict): issues.append("INVALID_COMPILER_ROW"); continue
        key = (x.get("variant"),x.get("path"))
        if key in compiler_by_key: issues.append("DUPLICATE_COMPILER_ROW:"+str(key))
        compiler_by_key[key] = x
    source_paths = {p for p,v in source_by_path.items() if v.get("role") in ("MANAGED_SOURCE","NATIVE_SOURCE")}
    expected_compiler = {(variant,p) for variant in VARIANTS for p in source_paths}
    if set(compiler_by_key) != expected_compiler: issues.append("COMPILER_COVERAGE_MISMATCH")
    if isinstance(summary,dict) and (summary.get("source_files") != len(source_paths)
                                      or summary.get("variant_rows") != len(expected_compiler)):
        issues.append("COMPILER_COUNTS_MISMATCH")
    apks = {}
    for x in verified.get("apks",[]):
        if not isinstance(x,dict) or x.get("variant") not in VARIANTS:
            issues.append("INVALID_VERIFIED_APK"); continue
        if x["variant"] in apks: issues.append("DUPLICATE_VERIFIED_APK")
        apks[x["variant"]] = x
    rows = []
    totals = {}
    for variant in VARIANTS:
        content = load(output/("apk-content-"+variant+".json"))
        content = content if isinstance(content,dict) else {}
        valid = apks.get(variant,{})
        if (content.get("status") != PASS or content.get("variant") != variant
                or content.get("build_identity") != metadata or not valid
                or content.get("apk_sha256") != valid.get("sha256")
                or content.get("required_failures") or content.get("evidence_errors")
                or content.get("compiler_output_errors")):
            issues.append("APK_IDENTITY_OR_CONTENT_UNVERIFIED:"+variant)
        by_path = collections.defaultdict(list)
        for item in content.get("rows",[]):
            if isinstance(item,dict) and isinstance(item.get("path"),str):
                by_path[item["path"]].append(item)
        native = {x.get("source") for x in content.get("native_units",[]) if isinstance(x,dict)}
        states = collections.Counter()
        for path,item in sorted(source_by_path.items()):
            matches = by_path.get(path,[])
            if len(matches)!=1: issues.append("APK_SOURCE_CARDINALITY:"+variant+":"+path)
            evidence = matches[0] if len(matches)==1 else {}
            compile_row = compiler_by_key.get((variant,path),{})
            hint = simple_source(root/path) if item.get("role")=="MANAGED_SOURCE" and item.get("kind")!="test" else "NOT_CHECKED"
            row = classify(item,evidence,compile_row,hint,variant)
            rows.append(row)
            states[row["class_evidence"] if item.get("role")=="MANAGED_SOURCE" else row["packaging_evidence"]] += 1
            if evidence.get("state")=="COMPILER_CLASSES_MAPPED_TO_DEX" and row["class_count"]==0:
                issues.append("EMPTY_CLASS_MAPPING:"+variant+":"+path)
            if item.get("role")=="NATIVE_SOURCE" and bool(compile_row.get("native_command_declared")) != (path in native):
                issues.append("NATIVE_COMMAND_MISMATCH:"+variant+":"+path)
        totals[variant] = {"dex_classes":content.get("dex_class_count"),
                           "compiler_classes":content.get("compiler_class_count"),
                           "r8_artifacts":content.get("r8",{}),
                           "native_commands_declared":len(native),"states":dict(sorted(states.items()))}
    report = {"schema_version":1,"source_sha":sha,"tracked_files":len(source_by_path),
              "variant_rows":len(rows),"variants":totals,
              "status":"EVIDENCE_RECONCILED" if not issues else "INCOMPLETE_EVIDENCE",
              "issues":sorted(set(issues)),"individual_compilation_proven":False,
              "runtime_functionality_proven":False,
              "limitations":["DEX/R8 mapping is not an individual compilation certificate.",
                             "Native compile_commands.json declares commands, not proof of execution.",
                             "A package/import-only source may not emit a class.",
                             "APK inclusion is not equivalent to functional runtime testing."]}
    output.mkdir(parents=True,exist_ok=True)
    (output/"p2-apk-trace.json").write_text(json.dumps({"summary":report,"rows":rows},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    with (output/"p2-apk-trace.tsv").open("w",encoding="utf-8",newline="") as f:
        writer=csv.DictWriter(f,fieldnames=FIELDS,delimiter="\t")
        writer.writeheader();writer.writerows(rows)
    details=["# P2.3 — DEX, R8 y codigo nativo","",
             "- Commit: "+sha,"- Estado: "+report["status"],
             "- Archivos versionados: "+str(len(source_by_path)),
             "- Filas por variante: "+str(len(rows)),
             "- Compilacion individual: NO ESTABLECIDA",""]
    for variant in VARIANTS:
        v=totals[variant]
        details += ["## "+variant,"- DEX: "+str(v["dex_classes"]),
                    "- Classes: "+str(v["compiler_classes"]),
                    "- Comandos CMake declarados: "+str(v["native_commands_declared"])]
        details += ["- "+key+": "+str(value) for key,value in v["states"].items()]
    details+=["","## Problemas de evidencia"]+(["- "+x for x in report["issues"]] or ["- Ninguno."])
    (output/"p2-apk-trace.md").write_text("\n".join(details)+"\n",encoding="utf-8")
    return report


if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--root",type=Path,default=Path("."))
    p.add_argument("--out",type=Path,default=Path("ci-artifacts"))
    p.add_argument("--sha",default=os.environ.get("GITHUB_SHA",""))
    a=p.parse_args()
    result=audit(a.root.resolve(),a.out.resolve(),a.sha)
    print(result["status"],result["tracked_files"],result["variant_rows"])
    sys.exit(0 if result["status"]=="EVIDENCE_RECONCILED" else 1)
