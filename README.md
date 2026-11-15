# ONNXCloudGaming — Phase 2 Native Engine Integration

This branch is an isolated native-streaming integration experiment for the Onn 4K Plus. The `main` branch remains untouched.

## What Phase 2 changes

- Builds the pinned XStreaming Android application and its native WebRTC, controller, video-decoder, and rendering components into one Android APK.
- Changes the installed Android package ID to `com.nick1234.onnxcloudgaming`.
- Changes the Android launcher label to **ONNXCloudGaming**.
- Preserves the upstream React Native streaming implementation and its controller/native modules rather than routing gameplay through the Kotlin WebView screen.
- Keeps the XStreaming source pinned as a Git submodule so the upstream origin and license notices remain traceable.

The existing ONNXCloudGaming Kotlin dashboard and WebView game screen are not yet merged into this React Native host. This phase establishes a single ONNXCloudGaming package containing the proven native streaming stack. The custom ONNX dashboard and any migration of settings/diagnostics are later integration work; this build must be tested before those changes.

## Build

1. Open the **Actions** tab.
2. Select **Build ONNXCloudGaming Native (XStreaming)**.
3. Run it on `phase-9-native-webrtc-experiment`.
4. Download `onnxcloudgaming-phase-2-native-engine`.

The workflow checks out the pinned submodule, installs Node/Yarn, Java, Android NDK and Rust targets, applies CI-only Linux toolchain fixes, then applies the ONNXCloudGaming Android package ID and launcher label before building the native release APK.

## Test checklist on the Onn 4K Plus

- Install the APK and verify Android identifies it as ONNXCloudGaming.
- Launch and sign in.
- Start Forza and confirm video, audio, controller input, and reconnect behavior.
- Compare stability against the 40-minute XStreaming baseline. Do not treat the baseline run as proof that this newly packaged build has passed.

## Upstream license and attribution

XStreaming is licensed under **GNU Affero General Public License v3.0 (AGPL-3.0)**. This project is based on that source; preserve license notices and comply with the applicable AGPL obligations when distributing modified binaries or offering network access to modified functionality. See [XStreaming's LICENSE](https://github.com/Geocld/XStreaming/blob/06ef1c7ee01d09cf9bb2a97fc4895da85e8488a8/LICENSE).

ONNXCloudGaming is an independent project and is not affiliated with Microsoft or Xbox.
