#!/usr/bin/env bash
# Aurelis © 2026. GPL-3.0-or-later
# Refresh once only for the observed, pre-task Kover 0.9.11 resolution failure.
set -euo pipefail
report_dir="${1:-ci-b02}"
mkdir -p "$report_dir"
gradle_args=(
  :app:lintDebug :app:lintRelease
  --init-script .github/scripts/task-inputs.gradle
  --continue --build-cache --console=plain --stacktrace
  --no-daemon --no-configuration-cache --max-workers=2
)
first_status=0
./gradlew "${gradle_args[@]}" 2>&1 | tee "$report_dir/lint.log" "$report_dir/lint-attempt-1.log" || first_status=$?
if [ "$first_status" -eq 0 ]; then
  exit 0
fi
if grep -qF 'A problem occurred configuring root project' "$report_dir/lint-attempt-1.log" &&
   grep -qE 'Could not find org\.jetbrains\.kotlinx:kover-(features-jvm|jvm-agent):0\.9\.11\.' "$report_dir/lint-attempt-1.log" &&
   ! grep -qE '^> Task :app:lint|Error: .*\[[A-Za-z][A-Za-z0-9]+\]' "$report_dir/lint-attempt-1.log"; then
  printf '%s\n' 'B02: configuration dependency resolution failed; retrying once with --refresh-dependencies.' | tee -a "$report_dir/lint.log"
  ./gradlew "${gradle_args[@]}" --refresh-dependencies 2>&1 |
    tee "$report_dir/lint-attempt-2.log" | tee -a "$report_dir/lint.log"
else
  exit "$first_status"
fi
