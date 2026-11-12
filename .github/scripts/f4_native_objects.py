#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""F4 native evidence: pair CMake declared source with a real hashed .o.

An object observed in .cxx does not by itself prove inclusion in an APK .so.
"""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
import shlex
import sys

def sha256(path: Path) -> str:
    dig=hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda:stream.read(1024*1024),b""):
            dig.update(chunk)
    return dig.hexdigest()

def within(path: Path, folder: Path) -> bool:
    return path.resolve().is_relative_to(folder.resolve())

def parse_output(entry: dict) -> str | None:
    output=entry.get("output")
    if isinstance(output,str) and output.endswith((".o",".obj")):
        return output
    tokens=entry.get("arguments")
    if not isinstance(tokens,list):
        command=entry.get("command","")
        if not isinstance(command,str):
            return None
        try:
            tokens=shlex.split(command)
        except ValueError:
            return None
    for i, token in enumerate(tokens):
        if token=="-o" and i+1<len(tokens):
            return tokens[i+1]
        if token.startswith("-o") and token not in ("-o","-O0","-O1","-O2","-O3","-Os","-Oz"):
            return token[2:]
    return None

def collect(root: Path, sha: str) -> dict:
    native_root=root/"app/.cxx"
    tracked_root=root/"app/src/main/cpp"
    records={}
    errors=[]
    if native_root.exists():
        for manifest in sorted(native_root.rglob("compile_commands.json")):
            if manifest.is_symlink() or not manifest.is_file() or not within(manifest,native_root):
                errors.append("UNSAFE_CMAKE_MANIFEST")
                continue
            relative=manifest.relative_to(native_root).parts
            variant=next((x for x in relative if x in ("debug","release")),None)
            if variant is None or "arm64-v8a" not in relative:
                continue
            try:
                commands=json.loads(manifest.read_text(encoding="utf-8"))
            except (OSError,ValueError):
                errors.append("UNREADABLE_CMAKE_MANIFEST")
                continue
            if not isinstance(commands,list):
                errors.append("INVALID_CMAKE_MANIFEST")
                continue
            for item in commands:
                if not isinstance(item,dict) or not isinstance(item.get("file"),str) or not isinstance(item.get("directory"),str):
                    errors.append("INVALID_CMAKE_COMMAND")
                    continue
                cwd=Path(item["directory"])
                if not cwd.is_absolute():
                    cwd=manifest.parent/cwd
                source=Path(item["file"])
                if not source.is_absolute():
                    source=cwd/source
                if not within(source,tracked_root) or not source.is_file():
                    continue
                path=source.resolve().relative_to(root.resolve()).as_posix()
                key=(variant,path)
                if key in records:
                    errors.append("DUPLICATE_NATIVE_SOURCE_COMMAND:"+path)
                    continue
                out_name=parse_output(item)
                result={"path":path,"variant":variant,"declared_command_sha256":sha256(manifest),
                        "status":"NO_OBJECT_EVIDENCE","object_sha256":None,
                        "apk_link_proven":False}
                if out_name:
                    obj=Path(out_name)
                    if not obj.is_absolute():
                        obj=cwd/obj
                    if within(obj,native_root) and obj.is_file() and not obj.is_symlink() and obj.suffix in (".o",".obj"):
                        result["status"]="OBJECT_SHA256_OBSERVED"
                        result["object_sha256"]=sha256(obj)
                        result["object_bytes"]=obj.stat().st_size
                    elif not within(obj,native_root):
                        errors.append("UNSAFE_OBJECT_OUTPUT:"+path)
                records[key]=result
    rows=[records[k] for k in sorted(records)]
    report={"schema_version":1,"source_sha":sha,"status":"PASS" if not errors else "INVALID_EVIDENCE",
            "declarations":len(rows),"object_files_observed":sum(x["status"]=="OBJECT_SHA256_OBSERVED" for x in rows),
            "errors":errors,"rows":rows,
            "limitations":"Object SHA-256 and CMake declaration do not prove linker or APK inclusion."}
    return report

def main():
    import argparse
    p=argparse.ArgumentParser()
    p.add_argument("--root",type=Path,default=Path("."))
    p.add_argument("--out",type=Path,default=Path("ci-artifacts/native-object-evidence.json"))
    p.add_argument("--sha",required=True)
    a=p.parse_args()
    result=collect(a.root.resolve(),a.sha)
    a.out.parent.mkdir(parents=True,exist_ok=True)
    a.out.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print("F4 native object evidence",result["status"],result["object_files_observed"],"/",result["declarations"])
    if result["status"]!="PASS":
        sys.exit(1)

if __name__=="__main__":
    main()
