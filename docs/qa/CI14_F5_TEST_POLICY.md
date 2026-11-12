# CI14 F5 — test reliability

Seven isolated real Media3 cases must each pass once. Failure in any case always fails B05. No automatic retry may convert a red test into green.

Flaky means the same case failed and then passed on the identical SHA. Three such incidents within 14 days require a GitHub issue and human quarantine review; no quarantine may bypass the mandatory B05 gate.

Kover XML for Debug and Release is JVM-only. Until a trusted merge-base and modified-line hit count are available, diff coverage is NOT_MEASURED, not 100%.

Actionlint and zizmor supply-chain controls, code style baselines and mandatory diff coverage remain subjects of F5 integration/verification rather than assumed PASS.

## CI14 F5 enforcing changed-source coverage

B03 now enforces >=80 percent of observed changed executable lines when the JaCoCo/Kover XML and merge-base are valid. A missing or inconsistent report is FAIL, not zero/100 coverage. A diff with no production Java/Kotlin files is explicitly NO_EXECUTABLE_LINES and passes; a changed source that cannot be resolved is NOT_MEASURED and fails. Existing real Media3 7/7 gates remain unchanged. Baseline run #88: 88.86% of 3969 observed executable lines.
