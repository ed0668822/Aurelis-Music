# Aurelis Music — Security Review Instructions

This file guides security review; it is not a vulnerability reporting contact.

## Review checklist
- Secrets: never commit signing keys, keystores, passwords or API tokens. Check GitHub Actions, Gradle properties, logs and generated BuildConfig. Values bundled in an APK are not secret.
- Android: least-privilege media permissions, safe permission denial/revocation, scoped storage, URI validation, exported components, intents and deep links.
- Media: treat filenames, tags, artwork, lyrics and metadata as untrusted; handle malformed files and inaccessible URIs without crashes or disclosure.
- Network: HTTPS with normal certificate validation, timeouts, response size limits, cancellation, safe URL handling and offline fallbacks. Avoid transmitting local library data unnecessarily.
- Native code: examine JNI and C++ for buffer bounds, memory lifetime, integer overflow and unsafe parsing.
- Dependencies/CI: assess dependency vulnerabilities and licensing; keep credentials out of logs and build artifacts.

## Reporting
For each security finding give severity, affected file/line, credible attack preconditions, impact and a minimal mitigation. Distinguish proven vulnerabilities from speculative risks. Avoid posting credentials or actionable exploit payloads publicly; advise private disclosure for serious issues.

Severity: Critical (severe exploitable compromise), High (unauthorized access or major exposure), Medium (meaningful conditional weakness), Low (limited hardening issue).
