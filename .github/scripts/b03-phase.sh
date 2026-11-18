#!/usr/bin/env bash
# Aurelis © 2026. GPL-3.0-or-later
# Retry only genuine Maven artifact HTTP 429 (including Robolectric SDK mirrors); preserve timeouts, test assertions and tee failures.
set -euo pipefail
phase="${1:?Expected a B03 phase}"
shift
case "$phase" in compile-debug|compile-release|debug|release) ;; *) exit 2 ;; esac
test "$#" -gt 0
mkdir -p ci-b03
attempt=1
max_attempts=3
status=1
while [ "$attempt" -le "$max_attempts" ]; do
  attempt_log="ci-b03/$phase-attempt-$attempt.log"
  set +e
  timeout --signal=QUIT --kill-after=30s 12m "$@" 2>&1 | tee "$attempt_log"
  phase_status=("${PIPESTATUS[@]}")
  set -e
  status="${phase_status[0]}"
  if [ "$status" -eq 0 ]; then status="${phase_status[1]}"; fi
  if ! cp "$attempt_log" "ci-b03/$phase.log"; then
    echo "AURELIS_PHASE_LOG_COPY_FAILED phase=$phase attempt=$attempt" >&2
    exit 1
  fi
  # Never retry timeouts, test assertions, compilation errors, or failed log writers.
  if [ "${phase_status[0]}" -eq 1 ] && [ "${phase_status[1]}" -eq 0 ] &&
     [ "$attempt" -lt "$max_attempts" ] &&
     grep -Eq 'repo.maven.apache.org/maven2|gcp.mirrors.maven.org/maven2|maven-central-eu.storage-download.googleapis.com/maven2' "$attempt_log" &&
     grep -Eq 'Received status code 429|HTTP response code: 429' "$attempt_log"; then
    delay=$((20 * attempt))
    echo "AURELIS_MAVEN_429_RETRY phase=$phase attempt=$attempt delay_seconds=$delay" | tee -a "ci-b03/$phase.log"
    sleep "${AURELIS_MAVEN_RETRY_DELAY_OVERRIDE:-$delay}"
    attempt=$((attempt + 1))
    continue
  fi
  break
done
set +e
printf '\nAURELIS_PHASE_EXIT phase=%s exit_code=%s command_exit_code=%s log_exit_code=%s attempts=%s\n' \
  "$phase" "$status" "${phase_status[0]}" "${phase_status[1]}" "$attempt" | tee -a "ci-b03/$phase.log"
marker_status=("${PIPESTATUS[@]}")
set -e
if [ "$status" -eq 0 ]; then
  status="${marker_status[1]}"
  if [ "$status" -eq 0 ]; then status="${marker_status[0]}"; fi
fi
exit "$status"
