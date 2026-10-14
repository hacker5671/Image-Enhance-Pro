# Image Enhance Pro — Android starter

Native Android Kotlin project for importing, previewing, scaling, and saving images.

## Current status — read before using
This is a starter app, **not yet a true AI upscaler**:
- It has a native gallery picker, scale selector, local high-quality bitmap resampling, and PNG export.
- Offline AI needs a compatible Real-ESRGAN ONNX model plus an ONNX Runtime inference pipeline.
- Online AI mode is a UI placeholder. It needs a configured HTTPS endpoint and request/response adapter.
- Upscaling by 4× or 8× can require substantial memory; the app attempts to reduce the scale to avoid very large outputs.

## Build
Open this folder in Android Studio and build the `app` module. For a Termux build, install a compatible JDK and Android SDK/command-line tools, then run:
`gradle assembleDebug`
The Android SDK must be installed and `ANDROID_HOME` configured. A Gradle wrapper is not included in this starter.

## Suggested next implementation
1. Add ONNX Runtime Android dependency.
2. Add a compatible Real-ESRGAN model in `app/src/main/assets/`.
3. Implement tiled inference to keep RAM use manageable.
4. Implement an HTTPS API adapter for the user's chosen cloud provider. Do not hard-code API keys in the APK; use a backend proxy or user-entered key.
