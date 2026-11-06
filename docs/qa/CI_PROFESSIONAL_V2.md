# Aurelis CI Professional v2

Independent CI workflow on branch ci/professional-v2. It does not alter the Codex migration branch, PR #1, app code, dependencies, signing configuration or existing android.yml.

Runs on PRs to main and manual dispatch. Validates lint/tests for debug and release, APK signatures/package/ZIP/arm64-v8a, SHA256 hashes, and attempts emulator install and startup.

Reports: provenance.json, sources.json, changed-files.txt, task-states.json, diagnostics.json, gates.json, apk-verification.json and summary.md.

Limitations: sources.json is an inventory, not a claim that each source file was compiled. Debug-signed release fallback is explicitly NOT an official release. Emulator is x86_64 while the app filters native libraries to arm64-v8a; an emulator smoke test may fail due to ABI incompatibility and needs an ARM64-capable environment or test-only ABI build. Do not weaken the production ABI filter without approval. This workflow does not publish to GitHub Releases.
