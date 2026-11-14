#!/usr/bin/env bash
# Aurelis © 2026. GPL-3.0-or-later.
# B05: execute under Bash; android-emulator-runner invokes its script field with /bin/sh.
set -euo pipefail

APK="app/build/outputs/apk/debug/app-debug.apk"
PKG="com.music.aurelis"
OUT="ci-b05"
mkdir -p "$OUT"

# Always collect evidence, including failed APK installation or app startup.
collect_diagnostics() {
  local status=$?
  trap - EXIT
  set +e
  adb devices -l > "$OUT/adb-devices-final.txt" 2>&1
  adb logcat -d -s AndroidRuntime:E > "$OUT/android-runtime-errors.log" 2>&1
  # On failure, collect only Media3/audio tags to avoid unrelated private logs.
  adb logcat -d -v threadtime -s \
    P3Media3Test:D P3Media3Service:D MediaFocusControl:D AudioManager:D \
    ExoPlayerImplInternal:E AudioTrack:E \
    MediaCodecAudioRenderer:E DefaultAudioSink:E AudioFlinger:E \
    AurelisDspChain:D '*:S' > "$OUT/p3-media3-logcat.txt" 2>&1
  adb shell dumpsys activity activities > "$OUT/activity.txt" 2>&1
  adb shell dumpsys audio > "$OUT/audio-focus-state.txt" 2>&1
  adb shell screencap -p /sdcard/ci-b05.png >/dev/null 2>&1
  adb pull /sdcard/ci-b05.png "$OUT/screen.png" >/dev/null 2>&1
  printf 'exit_code=%s\n' "$status" > "$OUT/script-exit.txt"
  exit "$status"
}
trap collect_diagnostics EXIT

printf 'B05: Android 15 (API 35) emulator / arm64-v8a APK smoke test\n'
test -s "$APK" || { echo 'B05 error: APK file missing'; exit 1; }

# The application MUST stay ARM64-only. Do not create/publish x86 APKs.
unzip -Z1 "$APK" > "$OUT/apk-entries.txt"
grep -Eq '^lib/arm64-v8a/[^/]+[.]so$' "$OUT/apk-entries.txt" || {
  echo 'B05 error: APK contains no ARM64 native libraries'; exit 1;
}
if grep -Eq '^lib/(x86|x86_64|armeabi-v7a)/' "$OUT/apk-entries.txt"; then
  echo 'B05 error: unexpectedly contains another native ABI'; exit 1
fi
echo "arm64-v8a only" > "$OUT/apk-abi.txt"

adb devices -l > "$OUT/adb-devices.txt"
adb shell getprop ro.build.version.sdk | tr -d '\r' > "$OUT/device-api.txt"
adb shell getprop ro.product.cpu.abilist | tr -d '\r' > "$OUT/device-abis.txt"
adb shell getprop ro.dalvik.vm.native.bridge | tr -d '\r' > "$OUT/native-bridge.txt"
echo "Device ABIs: $(cat "$OUT/device-abis.txt")"
echo "Native bridge: $(cat "$OUT/native-bridge.txt")"

test "$(cat "$OUT/device-api.txt")" = "35" || {
  echo 'B05 error: unexpected emulator API level'; exit 1;
}

# Installation is the definitive compatibility gate for this Google APIs x86_64
# emulator, which may translate ARM64 code. Never assume native ARM64 execution.
if ! adb install -r "$APK" > "$OUT/install.log" 2>&1; then
  echo 'B05 error: APK installation failed (see install.log)'
  cat "$OUT/install.log"
  exit 1
fi
grep -q 'Success' "$OUT/install.log" || {
  echo 'B05 error: adb did not confirm installation'; exit 1;
}
adb shell pm path "$PKG" > "$OUT/package-path.txt"
grep -q 'package:' "$OUT/package-path.txt" || {
  echo 'B05 error: package not found after install'; exit 1;
}

if ! adb shell am start -W -n "$PKG/.MainActivity" > "$OUT/start.log" 2>&1; then
  echo 'B05 error: MainActivity failed to start'
  cat "$OUT/start.log"
  exit 1
fi
cat "$OUT/start.log"
grep -q 'Status: ok' "$OUT/start.log" || {
  echo 'B05 error: ActivityManager did not confirm launch'; exit 1;
}

sleep 15
adb shell pidof "$PKG" | tr -d '\r' > "$OUT/process.txt"
test -s "$OUT/process.txt" || {
  echo 'B05 error: app process is not running after 15 seconds'; exit 1;
}

echo 'PASS: ARM64-only APK installed, activity launched, and process survived 15 seconds' \
  > "$OUT/smoke-pass.txt"

# P3: verify actual Media3 session decoding with an original local PCM WAV fixture.
TEST_APK="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
test -s "$TEST_APK" || { echo 'P3: missing instrumentation APK'; exit 1; }
if ! adb install -r "$TEST_APK" > "$OUT/p3-install.log" 2>&1; then
  echo 'P3: Android instrumented-test installation failed'
  cat "$OUT/p3-install.log"
  exit 1
fi
# MainActivity requests READ_MEDIA_AUDIO on first launch. Grant only for this
# automated test so the Android permission dialog cannot replace the foreground
# app while Media3 requests audio focus (Android 15, targetSdk 35).
if ! adb shell pm grant "$PKG" android.permission.READ_MEDIA_AUDIO \
    > "$OUT/p3-permission-grant.log" 2>&1; then
  echo 'P3: READ_MEDIA_AUDIO permission setup failed'
  cat "$OUT/p3-permission-grant.log"
  exit 1
fi
# Seven separate AndroidJUnitRunner commands, all run even if one fails.
mkdir -p "$OUT/p3-tests"
mapfile -t CASE_SELECTORS < <(python3 .github/scripts/b05_case_results.py list)
if [ "${#CASE_SELECTORS[@]}" -ne 7 ]; then
  echo 'P3: expected seven independent test selectors'
  exit 1
fi
: > "$OUT/p3-instrumentation.log"
for entry in "${CASE_SELECTORS[@]}"; do
  IFS='|' read -r case_id class_selector <<< "$entry"
  echo "P3 isolated case: $case_id -> $class_selector"
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/.MainActivity" \
    > "$OUT/p3-tests/${case_id}-start.log" 2>&1 || true
  adb logcat -c >/dev/null 2>&1 || true
  if timeout 125s adb shell am instrument -w -r \
     -e class "$class_selector" \
     com.music.aurelis.test/androidx.test.runner.AndroidJUnitRunner \
     > "$OUT/p3-tests/${case_id}.log" 2>&1; then
    printf '0\n' > "$OUT/p3-tests/${case_id}.exit"
  else
    printf '%s\n' "$?" > "$OUT/p3-tests/${case_id}.exit"
  fi
  printf '\n===== P3 CASE: %s =====\n' "$case_id" >> "$OUT/p3-instrumentation.log"
  cat "$OUT/p3-tests/${case_id}.log" >> "$OUT/p3-instrumentation.log"
  adb logcat -d -v threadtime -s \
    P3Media3Test:D P3Media3Service:D MediaFocusControl:I \
    ExoPlayerImplInternal:E AudioTrack:E AndroidRuntime:E '*:S' \
    > "$OUT/p3-tests/${case_id}-logcat.txt" 2>&1 || true
done
# Failing any one case fails B05 after preserving the other six.
python3 .github/scripts/b05_case_results.py summarize "$OUT"

echo 'B05: seven real Media3 Android tests passed (WAV, queue, repeat modes, recovery, recreation, continuous decode).'
