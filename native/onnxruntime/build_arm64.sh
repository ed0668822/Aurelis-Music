#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Aurelis
# SPDX-License-Identifier: LGPL-3.0-or-later
#
# Optional reproducible ARM64-only ONNX Runtime package for Aurelis AutoMix.
# It retains the official Java/JNI bridge and only builds the CPU operators
# used by the pinned ONNX graphs. It does NOT run from ordinary Gradle CI.
#
# Requirements: Linux, Android NDK r27, Android SDK, Python 3 + flatbuffers,
# CMake >=3.28, Ninja, curl, unzip, zip and an ARM64 Android cross-toolchain.
#
# Usage:
#   export ANDROID_NDK=/path/to/android-ndk-r27
#   export ANDROID_SDK_ROOT=/path/to/android-sdk
#   bash native/onnxruntime/build_arm64.sh
#
# A Gradle build with app/libs/onnxruntime-android-1.28.0-automix.aar uses
# this reduced runtime. Without it, Gradle stays on the official AAR.
set -euo pipefail
VERSION=1.28.0
COMMIT=da9b5e364c465de65c49d91e696cd6485270757f
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
WORK="${WORK:-$HOME/aurelis-ort-arm64}"
NDK="${ANDROID_NDK:?Set ANDROID_NDK to NDK r27}"
SDK="${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
SOURCE="$WORK/onnxruntime"
OUTPUT="$ROOT/app/libs/onnxruntime-android-${VERSION}-automix.aar"
JOBS="${JOBS:-4}"
mkdir -p "$WORK"
if [ ! -d "$SOURCE/.git" ]; then
    git clone --depth 1 --branch "v$VERSION" https://github.com/microsoft/onnxruntime "$SOURCE"
fi
[ "$(git -C "$SOURCE" rev-parse HEAD)" = "$COMMIT" ] || {
    echo "Unexpected ORT source commit; refusing build" >&2; exit 1;
}
python3 "$SOURCE/tools/ci_build/build.py" \
  --build_dir "$WORK/build" --config Release --update --build \
  --parallel "$JOBS" --skip_tests --skip_submodule_sync \
  --android --android_abi arm64-v8a --android_api 26 \
  --android_sdk_path "$SDK" --android_ndk_path "$NDK" \
  --cmake_generator Ninja --build_shared_lib --enable_lto --use_nnapi \
  --include_ops_by_config "$HERE/required_operators.config" \
  --cmake_extra_defines onnxruntime_BUILD_UNIT_TESTS=OFF

curl -fsSL "https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/$VERSION/onnxruntime-android-$VERSION.aar" -o "$WORK/official.aar"
rm -rf "$WORK/aar"
mkdir -p "$WORK/aar"
unzip -q "$WORK/official.aar" -d "$WORK/aar"
for abi in "$WORK"/aar/jni/*; do
  if [ "$(basename "$abi")" != arm64-v8a ]; then rm -rf "$abi"; fi
done
runtime="$WORK/build/Release/libonnxruntime.so"
[ -s "$runtime" ] || { echo "Missing built ARM64 runtime" >&2; exit 1; }
cp "$runtime" "$WORK/aar/jni/arm64-v8a/libonnxruntime.so"
readelf="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
strip="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"
"$strip" --strip-unneeded "$WORK/aar/jni/arm64-v8a/libonnxruntime.so"
needed=$("$readelf" --dyn-syms -W "$WORK/aar/jni/arm64-v8a/libonnxruntime4j_jni.so" | awk '$7 == "UND" {print $8}' | sed 's/@.*//' | grep -E '^(Ort|OrtSessionOptions)' | sort -u)
exported=$("$readelf" --dyn-syms -W "$WORK/aar/jni/arm64-v8a/libonnxruntime.so" | awk '$7 != "UND" {print $8}' | sed 's/@.*//' | sort -u)
missing=$(comm -23 <(printf '%s\n' "$needed") <(printf '%s\n' "$exported"))
[ -z "$missing" ] || { echo "JNI ABI mismatch: $missing" >&2; exit 1; }
mkdir -p "$(dirname "$OUTPUT")"
(cd "$WORK/aar" && zip -q -r -X "$OUTPUT" .)
echo "Ready: $OUTPUT"
echo "Verify both models in an ARM64 device/emulator before publishing."
