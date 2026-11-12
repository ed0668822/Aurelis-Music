# CI14 F5 — test reliability

Seven isolated real Media3 cases must each pass once. Failure in any case always fails B05. No automatic retry may convert a red test into green.

Flaky means the same case failed and then passed on the identical SHA. Three such incidents within 14 days require a GitHub issue and human quarantine review; no quarantine may bypass the mandatory B05 gate.

Kover XML for Debug and Release is JVM-only. Until a trusted merge-base and modified-line hit count are available, diff coverage is NOT_MEASURED, not 100%.

Actionlint and zizmor supply-chain controls, code style baselines and mandatory diff coverage remain subjects of F5 integration/verification rather than assumed PASS.
