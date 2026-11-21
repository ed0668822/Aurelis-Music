#!/usr/bin/env bash
# SPDX-License-Identifier: LGPL-3.0-or-later
# Provision an authentic Discord Social SDK AAR obtained under Discord's terms.
set -euo pipefail
aar="app/libs/discord_partner_sdk.aar"
if [[ ! -f "$aar" ]]; then
    if [[ -z "${DISCORD_SDK_AAR_URL:-}" ]]; then
        echo "::notice title=Discord Social SDK::No official Android AAR: preview-only build."
        exit 0
    fi
    if [[ ! "$DISCORD_SDK_AAR_URL" =~ ^https:// ]]; then
        echo "::error title=Discord SDK::Expected direct HTTPS AAR download."
        exit 1
    fi
    if [[ ! "${DISCORD_SDK_AAR_SHA256:-}" =~ ^[0-9a-fA-F]{64}$ ]]; then
        echo "::error title=Discord SDK::Set the official AAR SHA-256 digest."
        exit 1
    fi
    mkdir -p app/libs
    tmp="$(mktemp)"
    trap 'rm -f "$tmp"' EXIT
    curl --fail --silent --show-error --location --retry 2 --max-time 180 \
      --proto '=https' --proto-redir '=https' \
      --output "$tmp" "$DISCORD_SDK_AAR_URL"
    if ! printf '%s  %s\n' "$DISCORD_SDK_AAR_SHA256" "$tmp" | sha256sum --check --status; then
        echo "::error title=Discord SDK::AAR SHA-256 did not match."
        exit 1
    fi
    mv "$tmp" "$aar"
fi
python3 - "$aar" <<'PY'
import sys
from zipfile import ZipFile, BadZipFile
try:
    with ZipFile(sys.argv[1]) as archive:
        files = archive.namelist()
        if "prefab/prefab.json" not in files:
            raise ValueError("Prefab manifest missing")
        if not any(p.startswith("prefab/modules/discord_partner_sdk/") for p in files):
            raise ValueError("Discord Prefab module missing")
        if not any("arm64-v8a" in p and p.endswith(".so") for p in files):
            raise ValueError("ARM64 native SDK missing")
        if "classes.jar" not in files:
            raise ValueError("Android classes.jar missing")
except (BadZipFile, ValueError) as exc:
    sys.exit(f"::error title=Discord SDK::Invalid Android SDK AAR: {exc}")
print("Discord Android SDK structure verified (Prefab + ARM64 + classes)")
PY
