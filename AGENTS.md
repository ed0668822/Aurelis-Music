# Aurelis Music — Code Review Instructions

## Project
Android app `com.music.aurelis`: Kotlin, Jetpack Compose, Gradle Kotlin DSL, Media3, CMake/C++. Prioritize local music playback, library, lyrics, translations, search, automix, and stable UI. Preserve existing features.

## Review priorities
1. Compilation: Kotlin imports/types, Compose APIs, Gradle dependency compatibility, Android resources, native CMake/NDK, GitHub Actions.
2. Playback: Media3 lifecycle, audio focus, background services, notifications, queues, shuffle/repeat, MediaStore permissions.
3. Reliability: crashes, concurrency, coroutines, lifecycle leaks, nullability, offline operation.
4. UI: Compose state, recomposition, navigation, accessibility, responsiveness, localization.
5. Security: follow SECURITY.md; review exported components, secrets, network requests and untrusted metadata.
6. Performance: main-thread I/O, artwork memory, expensive library scans and unnecessary recomposition.
7. Compatibility: respect minSdk, targetSdk, compileSdk and supported ABI declared by Gradle.

## Findings
Only report actionable issues with file and line, severity (Critical/High/Medium/Low), reproducible scenario, impact and minimal fix. Distinguish confirmed defects from untested possibilities. Review nearby call sites and avoid duplicate or purely stylistic feedback.

Do not claim a build or test passed unless actually run. If no defects are found, state remaining verification gaps. Prefer minimal corrections and regression tests; never remove a core feature just to make a build pass.

## Licensing and scope
Respect existing `Aurelis © 2026. GPL-3.0-or-later` headers and third-party notices. Do not assume third-party attribution may be replaced. Review only: do not edit or merge code without explicit authorization. Never reveal credentials or private data.
