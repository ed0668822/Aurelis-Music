#!/usr/bin/env python3
# Aurelis © 2026. GPL-3.0-or-later.
"""Optional, evidence-driven VirusTotal scan of the already verified ARM64 Release APK.

A missing API key never masquerades as a clean scan. Any report is advisory:
VirusTotal findings need human review, and a zero-detection result is not proof of safety.
Public uploads share the submitted APK with VirusTotal's security ecosystem.
"""
from __future__ import annotations

import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

BASE = "https://www.virustotal.com/api/v3"
VALID_STATES = {
    "NOT_CONFIGURED", "NO_DETECTIONS_REPORTED", "REVIEW_REQUIRED",
    "PENDING", "ERROR",
}
MAX_DIRECT_UPLOAD = 32 * 1024 * 1024
POLL_COUNT = 7
POLL_INTERVAL_SECONDS = 21


def sha256(file: Path) -> str:
    digest = hashlib.sha256()
    with file.open("rb") as handle:
        for part in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(part)
    return digest.hexdigest()


def verified_release(root: Path) -> tuple[Path, str]:
    manifest = json.loads((root / "ci-approved-apks/apk-manifest.json").read_text())
    verification = json.loads((root / "ci-artifacts/apk-verification.json").read_text())
    signing = json.loads((root / "ci-artifacts/signing.json").read_text())
    if verification.get("status") != "success" or signing.get("mode") != "configured_release":
        raise ValueError("B04 Release signing/integrity verification was not approved")
    release = [item for item in manifest if item.get("variant") == "release"]
    if len(release) != 1:
        raise ValueError("Expected exactly one verified Release manifest entry")
    record = release[0]
    name = record.get("file")
    if not isinstance(name, str) or Path(name).name != name:
        raise ValueError("Unsafe APK name")
    file = root / "ci-approved-apks" / name
    if not file.is_file() or file.is_symlink():
        raise ValueError("Verified Release APK not found")
    digest = sha256(file)
    if digest != record.get("sha256") or file.stat().st_size != record.get("bytes"):
        raise ValueError("Release SHA-256 or size mismatch")
    v = [r for r in verification.get("apks", []) if r.get("variant") == "release"]
    if len(v) != 1 or v[0].get("sha256") != digest or v[0].get("signature_verified") is not True:
        raise ValueError("Release APK not matched to signature-verification evidence")
    if record.get("abis") != ["arm64-v8a"]:
        raise ValueError("Only ARM64 release APK may be submitted")
    return file, digest


def get_api(path: str, apikey: str):
    request = Request(BASE + path, headers={
        "x-apikey": apikey,
        "accept": "application/json",
        "User-Agent": "aurelis-ci-virustotal-audit",
    }, method="GET")
    with urlopen(request, timeout=65) as response:
        return json.load(response)


def upload(file: Path, apikey: str) -> str:
    if file.stat().st_size >= MAX_DIRECT_UPLOAD:
        raise ValueError("Release APK too large for direct upload; use approved large-file endpoint")
    boundary = "AurelisBoundary" + secrets.token_hex(18)
    body = (
        ("--" + boundary + "\r\n"
         + 'Content-Disposition: form-data; name="file"; filename="aurelis-release.apk"\r\n'
         + "Content-Type: application/vnd.android.package-archive\r\n\r\n").encode()
        + file.read_bytes()
        + ("\r\n--" + boundary + "--\r\n").encode()
    )
    req = Request(BASE + "/files", data=body, headers={
        "x-apikey": apikey,
        "Accept": "application/json",
        "Content-Type": "multipart/form-data; boundary=" + boundary,
        "User-Agent": "aurelis-ci-virustotal-audit",
    }, method="POST")
    with urlopen(req, timeout=120) as response:
        result = json.load(response)
    analysis_id = result.get("data", {}).get("id")
    if not isinstance(analysis_id, str) or not analysis_id:
        raise ValueError("VirusTotal did not provide an analysis identifier")
    return analysis_id


def classification(stats: dict) -> str:
    if not isinstance(stats, dict):
        return "PENDING"
    counts = [stats.get(k, 0) for k in ("malicious", "suspicious", "harmless", "undetected", "timeout", "failure")]
    if any(not isinstance(v, int) or v < 0 for v in counts):
        return "ERROR"
    if counts[0] + counts[1] > 0:
        return "REVIEW_REQUIRED"
    if sum(counts[:4]) == 0:
        return "PENDING"
    return "NO_DETECTIONS_REPORTED"


def timestamp_to_iso(value):
    if isinstance(value, int):
        return dt.datetime.fromtimestamp(value, tz=dt.timezone.utc).isoformat()
    return None


def selected_detections(attrs: dict) -> list[dict]:
    result = []
    for name, value in (attrs.get("last_analysis_results") or {}).items():
        if not isinstance(value, dict) or value.get("category") not in ("malicious", "suspicious"):
            continue
        result.append({
            "engine": str(name)[:80],
            "category": value.get("category"),
            "result": str(value.get("result") or "")[:200],
        })
    return result[:50]


def report_for(file: Path, digest: str, key: str) -> dict:
    report = {
        "schema_version": 1,
        "sha256": digest,
        "file_name": file.name,
        "bytes": file.stat().st_size,
        "source_sha": os.environ.get("GITHUB_SHA", ""),
        "status": "NOT_CONFIGURED",
        "report_origin": None,
        "analysis_updated_at": None,
        "last_analysis_stats": {},
        "detections": [],
        "url": "https://www.virustotal.com/gui/file/" + digest + "/detection",
        "error_code": None,
        "limitations": [
            "VirusTotal results are advisory; no antivirus scan proves a file is harmless.",
            "Zero reported detections do not certify a clean application.",
            "Play Protect may separately scan sideloaded apps.",
            "Results for an existing hash can be older than this GitHub Actions execution.",
        ],
    }
    if not key:
        return report
    try:
        try:
            existing = get_api("/files/" + digest, key)
        except HTTPError as err:
            if err.code != 404:
                raise
            existing = None
        if existing is not None:
            attrs = existing.get("data", {}).get("attributes", {})
            if attrs.get("sha256", digest) != digest:
                raise ValueError("VirusTotal hash does not match submitted release")
            stats = attrs.get("last_analysis_stats") or {}
            report.update(
                report_origin="EXISTING_HASH_REPORT",
                analysis_updated_at=timestamp_to_iso(attrs.get("last_analysis_date")),
                last_analysis_stats=stats,
                detections=selected_detections(attrs),
                status=classification(stats),
            )
            return report

        analysis_id = upload(file, key)
        report["report_origin"] = "NEW_APK_UPLOAD"
        for _ in range(POLL_COUNT):
            time.sleep(POLL_INTERVAL_SECONDS)
            a = get_api("/analyses/" + analysis_id, key)
            attrs = a.get("data", {}).get("attributes", {})
            if attrs.get("status") != "completed":
                continue
            stats = attrs.get("stats") or {}
            report.update(last_analysis_stats=stats, status=classification(stats),
                          analysis_updated_at=dt.datetime.now(dt.timezone.utc).isoformat())
            detections = attrs.get("results") or {}
            report["detections"] = selected_detections({"last_analysis_results": detections})
            break
        else:
            report["status"] = "PENDING"
        return report
    except HTTPError as err:
        report.update(status="ERROR", error_code=f"VT_HTTP_{err.code}")
        return report
    except (URLError, TimeoutError):
        report.update(status="ERROR", error_code="VT_NETWORK_ERROR")
        return report
    except (ValueError, TypeError, KeyError, OSError):
        report.update(status="ERROR", error_code="VT_RESPONSE_OR_UPLOAD_ERROR")
        return report


def write_reports(out: Path, report: dict):
    out.mkdir(exist_ok=True, parents=True)
    (out / "virustotal.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    safe = lambda s: str(s).replace("|", "\\|").replace("\n", " ")
    lines = [
        "# B04 — VirusTotal (auditoría opcional)",
        "",
        f"- Estado: **{report['status']}**",
        f"- APK Release: `{report.get('file_name') or 'no_disponible'}`",
        f"- SHA-256: `{report.get('sha256') or 'no_disponible'}`",
        f"- Fuente del análisis: `{report.get('report_origin') or 'NO_API_QUERY'}`",
        f"- Fecha del análisis: `{report.get('analysis_updated_at') or 'no_disponible'}`",
        f"- URL del informe: {report.get('url') or 'no_disponible'}",
        "",
        "## Detecciones y advertencias",
    ]
    if report["status"] == "NOT_CONFIGURED":
        lines.append("- Falta el secreto `VIRUSTOTAL_API_KEY` en GitHub Actions: **NO ANALIZADO**.")
    elif report["status"] == "PENDING":
        lines.append("- El análisis está pendiente o aún no dispone de resultados suficientes.")
    elif report["status"] == "ERROR":
        lines.append(f"- Consulta incompleta: {report.get('error_code') or 'UNKNOWN'}. No equivale a 0 detecciones.")
    elif report["status"] == "NO_DETECTIONS_REPORTED":
        lines.append("- No se reportaron detecciones en los motores consultados; esto no demuestra inocuidad.")
    else:
        lines.append("- Hay detecciones que **requieren revisión humana** antes de distribuir.")
    lines += ["", "| Motor | Clasificación | Detección |", "|---|---|---|"]
    for item in report.get("detections") or []:
        lines.append(f"| {safe(item['engine'])} | {safe(item['category'])} | {safe(item['result'])} |")
    lines += ["", "## Recuento de motores", ""]
    for key, n in (report.get("last_analysis_stats") or {}).items():
        lines.append(f"- {safe(key)}: {n}")
    lines += ["", "VirusTotal no reemplaza la auditoría local ni la verificación de Play Protect.",
              "La subida pública puede hacer que el APK esté disponible para investigadores y socios de VirusTotal."]
    (out / "virustotal.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    if "GITHUB_STEP_SUMMARY" in os.environ:
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as fp:
            fp.write("\n".join(lines) + "\n")


def main() -> int:
    root = Path.cwd()
    out = root / "ci-artifacts"
    key = os.environ.get("VIRUSTOTAL_API_KEY", "").strip()
    try:
        file, digest = verified_release(root)
        report = report_for(file, digest, key)
    except (OSError, ValueError, TypeError, KeyError, json.JSONDecodeError):
        report = {
            "schema_version": 1, "source_sha": os.environ.get("GITHUB_SHA", ""),
            "status": "ERROR", "error_code": "RELEASE_NOT_VERIFIED_OR_UNAVAILABLE",
            "last_analysis_stats": {}, "detections": [], "sha256": None, "url": None,
        }
    write_reports(out, report)
    print("VirusTotal audit status:", report["status"])
    print("The antivirus result is advisory and not proof of a clean app.")
    return 0 if report["status"] in VALID_STATES else 1


if __name__ == "__main__":
    sys.exit(main())
