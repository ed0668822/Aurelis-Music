#!/usr/bin/env bash
# Aurelis © 2026. GPL-3.0-or-later
# Narrow pre-task recovery for observed Kover0.9.11 missing metadata / Maven429.
set -euo pipefail
report_dir="${1:-ci-b02}"
mkdir -p "$report_dir"
gradle_args=(
  :app:lintDebug :app:lintRelease
  --init-script .github/scripts/task-inputs.gradle
  --continue --build-cache --console=plain --stacktrace
  --no-daemon --no-configuration-cache --max-workers=2
)
: > "$report_dir/lint.log"
attempt_status=0
writers_ok=1

run_attempt() {
  local attempt="$1"
  shift
  local -a statuses
  set +e
  ./gradlew "${gradle_args[@]}" "$@" 2>&1 |
    tee "$report_dir/lint-attempt-$attempt.log" | tee -a "$report_dir/lint.log"
  statuses=("${PIPESTATUS[@]}")
  set -e
  writers_ok=$((statuses[1] == 0 && statuses[2] == 0))
  attempt_status="${statuses[0]}"
  if [ "$attempt_status" -eq 0 ]; then attempt_status="${statuses[1]}"; fi
  if [ "$attempt_status" -eq 0 ]; then attempt_status="${statuses[2]}"; fi
  return 0
}

before_tasks() {
  [ "$writers_ok" -eq 1 ] &&
    grep -qF 'A problem occurred configuring root project' "$1" &&
    ! grep -qE '^[[:space:]]*> Task |^[[:space:]]*e: |Error: .*\[[A-Za-z][A-Za-z0-9]+\]' "$1"
}

rate_limited() {
  before_tasks "$1" &&
    grep -qE "https://repo[.]maven[.]apache[.]org/maven2/org/jetbrains/kotlinx/kover-(features-jvm|jvm-agent)/0[.]9[.]11/[^'[:space:]]+[.]pom'. Received status code 429 from server: Too Many Requests" "$1"
}

run_attempt 1
if [ "$attempt_status" -eq 0 ]; then exit 0; fi
if ! rate_limited "$report_dir/lint-attempt-1.log" &&
   before_tasks "$report_dir/lint-attempt-1.log" &&
   grep -qE 'Could not find org\.jetbrains\.kotlinx:kover-(features-jvm|jvm-agent):0\.9\.11\.' "$report_dir/lint-attempt-1.log"; then
  printf '%s\n' 'B02: configuration dependency resolution failed; retrying once with --refresh-dependencies.' | tee -a "$report_dir/lint.log"
  run_attempt 2 --refresh-dependencies
  exit "$attempt_status"
fi
for attempt in 2 3; do
  previous=$((attempt - 1))
  if ! rate_limited "$report_dir/lint-attempt-$previous.log"; then exit "$attempt_status"; fi
  delay=$((15 * (attempt - 1)))
  printf 'B02: Maven429 before tasks; attempt %s/3 after %ss, keeping cached dependencies.\n' \
    "$attempt" "$delay" | tee -a "$report_dir/lint.log"
  sleep "$delay"
  run_attempt "$attempt"
  if [ "$attempt_status" -eq 0 ]; then exit 0; fi
done
exit "$attempt_status"
