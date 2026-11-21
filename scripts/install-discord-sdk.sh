#!/usr/bin/env bash
# SPDX-License-Identifier: LGPL-3.0-or-later
# Import the separately licensed Discord Social SDK Android AAR.
set -euo pipefail
aar="app/libs/discord_partner_sdk.aar"
mkdir -p app/libs
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

if [[ ! -f "$aar" && -n "${DISCORD_SDK_ZIP_URL:-}" ]]; then
    # A signed official Google Storage URL; never print it or commit it.
    python3 - <<'PY'
import os, sys
from urllib.parse import urlsplit
url = urlsplit(os.environ["DISCORD_SDK_ZIP_URL"])
if (url.scheme != "https"
    or url.hostname != "storage.googleapis.com"
    or not url.path.startswith("/discord-slayer-sdk-artifacts/discord_partner_sdk/")
    or "/release/DiscordSocialSdk-" not in url.path
    or not url.path.endswith(".zip")
    or "X-Goog-Signature=" not in url.query):
    sys.exit("::error title=Discord SDK::Use an official signed ZIP download URL from Discord Developer Portal")
PY
    echo "Downloading the official Discord Social SDK archive..."
    curl --fail --silent --show-error --location --retry 2 \
        --connect-timeout 30 --max-time 1200 \
        --proto '=https' --proto-redir '=https' \
        --output "$tmpdir/discord-sdk.zip" "$DISCORD_SDK_ZIP_URL"
    if [[ -n "${DISCORD_SDK_ZIP_SHA256:-}" ]]; then
        if [[ ! "$DISCORD_SDK_ZIP_SHA256" =~ ^[0-9a-fA-F]{64}$ ]]; then
            echo "::error title=Discord SDK::ZIP SHA-256 must be 64 hexadecimal characters."
            exit 1
        fi
        if ! printf '%s  %s\n' "$DISCORD_SDK_ZIP_SHA256" "$tmpdir/discord-sdk.zip" | sha256sum --check --status; then
            echo "::error title=Discord SDK::ZIP SHA-256 mismatch."
            exit 1
        fi
    fi
    python3 - "$tmpdir/discord-sdk.zip" "$tmpdir/discord_partner_sdk.aar" <<'PY'
import shutil, sys
from zipfile import ZipFile, BadZipFile
try:
    with ZipFile(sys.argv[1]) as archive:
        matches = [m for m in archive.infolist()
                   if not m.is_dir() and m.filename.endswith("/discord_partner_sdk.aar")
                   and "/release/" in m.filename.lower()]
        if len(matches) != 1:
            raise ValueError(f"expected one Android release AAR, found {len(matches)}")
        if matches[0].file_size > 1_500_000_000:
            raise ValueError("AAR exceeds safe extraction size")
        with archive.open(matches[0]) as src, open(sys.argv[2], "wb") as dst:
            shutil.copyfileobj(src, dst, 1024 * 1024)
        print("Extracted official SDK Android release AAR.")
except (BadZipFile, ValueError, OSError, EOFError) as exc:
    sys.exit(f"::error title=Discord SDK::Cannot extract release AAR: {exc}")
PY
    mv "$tmpdir/discord_partner_sdk.aar" "$aar"
fi

# Existing route: separately hosted, integrity-checked Android AAR.
if [[ ! -f "$aar" && -n "${DISCORD_SDK_AAR_URL:-}" ]]; then
    if [[ ! "$DISCORD_SDK_AAR_URL" =~ ^https:// ]] ||
       [[ ! "${DISCORD_SDK_AAR_SHA256:-}" =~ ^[0-9a-fA-F]{64}$ ]]; then
        echo "::error title=Discord SDK::AAR requires HTTPS URL and SHA-256."
        exit 1
    fi
    curl --fail --silent --show-error --location --retry 2 \
        --connect-timeout 30 --max-time 1200 \
        --proto '=https' --proto-redir '=https' \
        --output "$tmpdir/discord_partner_sdk.aar" "$DISCORD_SDK_AAR_URL"
    if ! printf '%s  %s\n' "$DISCORD_SDK_AAR_SHA256" "$tmpdir/discord_partner_sdk.aar" | sha256sum --check --status; then
        echo "::error title=Discord SDK::AAR SHA-256 mismatch."
        exit 1
    fi
    mv "$tmpdir/discord_partner_sdk.aar" "$aar"
fi

if [[ ! -f "$aar" ]]; then
    echo "::notice title=Discord Social SDK::No official AAR supplied: preview-only build."
    exit 0
fi

python3 - "$aar" <<'PY'
import sys
from zipfile import ZipFile, BadZipFile
try:
    with ZipFile(sys.argv[1]) as sdk:
        files = sdk.namelist()
        if "classes.jar" not in files:
            raise ValueError("Android classes.jar missing")
        if "prefab/prefab.json" not in files:
            raise ValueError("Prefab manifest missing")
        if not any(f.startswith("prefab/modules/discord_partner_sdk/") for f in files):
            raise ValueError("Discord Prefab module missing")
        if not any("arm64-v8a" in f and f.endswith(".so") for f in files):
            raise ValueError("ARM64 native SDK missing")
except (BadZipFile, ValueError, OSError) as error:
    sys.exit(f"::error title=Discord SDK::Invalid Android AAR: {error}")
print("Discord SDK AAR: Prefab, ARM64 and Android classes verified.")
PY
