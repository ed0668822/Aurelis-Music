#!/usr/bin/env bash
# Aurelis © 2026. GPL-3.0-or-later
# Preserve the command AND log-writer exit statuses, including timeout failures.
set -euo pipefail
phase="${1:?Expected a B03 phase}"
shift
case "$phase" in compile-debug|compile-release|debug|release) ;; *) exit 2 ;; esac
test "$#" -gt 0
mkdir -p ci-b03
set +e
timeout --signal=QUIT --kill-after=30s 12m "$@" 2>&1 | tee "ci-b03/$phase.log"
phase_status=("${PIPESTATUS[@]}")
status="${phase_status[0]}"
if [ "$status" -eq 0 ]; then status="${phase_status[1]}"; fi
printf '\nAURELIS_PHASE_EXIT phase=%s exit_code=%s command_exit_code=%s log_exit_code=%s\n' \
  "$phase" "$status" "${phase_status[0]}" "${phase_status[1]}" | tee -a "ci-b03/$phase.log"
marker_status=("${PIPESTATUS[@]}")
set -e
if [ "$status" -eq 0 ]; then
  status="${marker_status[1]}"
  if [ "$status" -eq 0 ]; then status="${marker_status[0]}"; fi
fi
exit "$status"
