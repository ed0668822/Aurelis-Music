#!/usr/bin/env bash
# Aurelis © 2026. GPL-3.0-or-later.
# F5 pinned workflow audit; initial findings are reported, not silently ignored.
set -euo pipefail
mkdir -p ci-b01
TOOL_DIR="$RUNNER_TEMP/aurelis-f5-tools"
mkdir -p "$TOOL_DIR"
VERSION="1.7.12"
ARCHIVE="actionlint_$VERSION""_linux_amd64.tar.gz"
URL="https://github.com/rhysd/actionlint/releases/download/v$VERSION/$ARCHIVE"
EXPECTED_SHA256="8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8"
curl --proto '=https' --tlsv1.2 --retry 2 --fail --silent --show-error --location \
  "$URL" --output "$TOOL_DIR/$ARCHIVE"
echo "$EXPECTED_SHA256  $TOOL_DIR/$ARCHIVE" | sha256sum --check
tar -xf "$TOOL_DIR/$ARCHIVE" -C "$TOOL_DIR" actionlint
set +e
"$TOOL_DIR/actionlint" .github/workflows/*.yml > ci-b01/actionlint.log 2>&1
ACTIONLINT_CODE=$?
set -e
python3 -m venv "$TOOL_DIR/zizmor-venv"
"$TOOL_DIR/zizmor-venv/bin/python" -m pip install --disable-pip-version-check \
  --no-cache-dir "zizmor==1.32.0" > ci-b01/zizmor-install.log 2>&1
set +e
"$TOOL_DIR/zizmor-venv/bin/zizmor" --format=json .github/workflows \
  > ci-b01/zizmor-findings.json 2> ci-b01/zizmor.log
ZIZMOR_CODE=$?
set -e
{
  echo "# CI14 F5 — workflow security audit"
  echo
  echo "- actionlint 1.7.12 exit code: $ACTIONLINT_CODE"
  echo "- zizmor 1.32.0 exit code: $ZIZMOR_CODE"
  if [ "$ACTIONLINT_CODE" -eq 0 ]; then
    echo "- actionlint obligatorio: **PASS**"
  else
    echo "- actionlint obligatorio: **FAIL**"
  fi
  if [ "$ZIZMOR_CODE" -eq 0 ]; then
    echo "- zizmor: **sin hallazgos**"
  else
    echo "- zizmor: **hallazgos para revisión; no equivale a PASS**"
  fi
  echo "- Evidencia íntegra: zizmor-findings.json, zizmor.log y actionlint.log."
} > ci-b01/workflow-audit.md
if [ "$ACTIONLINT_CODE" -ne 0 ]; then
  echo "F5 FAIL: invalid workflow detected by actionlint" >&2
  exit 1
fi
if [ "$ZIZMOR_CODE" -ne 0 ]; then
  echo "F5: advisory zizmor findings recorded, no green security claim"
fi
echo "F5 actionlint PASS"
