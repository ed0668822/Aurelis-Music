# AutoMix ONNX — ARM64 runtime

The app includes both quantized, offline ONNX models. A model is always
validated by **SHA-256 and exact length** before native ORT loads it. If a
bundled file becomes missing or corrupt, `AutomixModelDepot` attempts a
background recovery download from a **pinned Aurelis GitHub source revision**.
Normal playback and fallback crossfade work while models are unavailable.

The default Maven dependency `onnxruntime-android:1.28.0` is retained in CI.
To build a smaller CPU-only runtime on Linux, run `build_arm64.sh` with the
NDK and SDK configured. The script keeps the official JNI bridge, builds
only the pinned model operators, and verifies linked runtime symbols.
Gradle uses the resulting AAR only if it has been produced explicitly.

**Release gate:** a slim runtime has not been produced or device-validated
by ordinary GitHub Actions. Do not publish one without ARM64 instrumented
checks for both BeatTracker and VocalTracker. Recalculate required operators
whenever either model changes.

For low-memory devices and thermal throttling, both models share a dynamic
CPU budget. Efficient stays one inference thread; Balanced defaults to two;
Maximum is capped by available cores and thermal/RAM constraints.
