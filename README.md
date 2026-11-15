# ONNXCloudGaming — Phase 3 Custom Dashboard and Settings

This branch is an isolated native-streaming integration experiment for the Onn 4K Plus. The `main` branch remains untouched.

## What Phase 3 changes

- Builds the pinned XStreaming Android application and its native WebRTC, controller, video-decoder, and rendering components into one Android APK.
- Changes the installed Android package ID to `com.nick1234.onnxcloudgaming`.
- Changes the Android launcher label to **ONNXCloudGaming**.
- Preserves the upstream React Native streaming implementation and its controller/native modules rather than routing gameplay through the Kotlin WebView screen.
- Keeps the XStreaming source pinned as a Git submodule so the upstream origin and license notices remain traceable.
- Replaces the upstream Settings screen at build time with the ONNXCloudGaming black-and-red, TV-oriented dashboard layout: left navigation, two-column settings cards, red focus treatment, and app branding.
- Connects visible controls to existing XStreaming settings storage for resolution, bitrate, codec, vibration, stereo audio, IPv6, theme, and accent color; routes advanced options to the upstream setting detail screens.
- Keeps this UI override in `phase-3/Settings.tsx`; the pinned upstream submodule itself is not modified.

The existing Kotlin WebView app is still not merged into this React Native host. This phase adds the first ONNXCloudGaming-branded dashboard/settings experience inside the native XStreaming host. The broader Series X-style home screen and any additional diagnostics remain future work. Settings controls must be tested on the Onn 4K Plus; options that route to upstream setting-detail screens retain upstream behavior.

## Build

1. Open the **Actions** tab.
2. Select **Build ONNXCloudGaming Native (XStreaming)**.
3. Run it on `phase-9-native-webrtc-experiment`.
4. Download `onnxcloudgaming-phase-3-dashboard`.

The workflow checks out the pinned submodule, installs Node/Yarn, Java, Android NDK and Rust targets, applies CI-only Linux toolchain fixes, applies the ONNXCloudGaming Android package ID and launcher label, copies the Phase 3 settings screen into the XStreaming host, then builds the native release APK.

## Test checklist on the Onn 4K Plus

- Install the APK and verify Android identifies it as ONNXCloudGaming.
- Launch and sign in.
- Start Forza and confirm video, audio, controller input, and reconnect behavior.
- Compare stability against the 40-minute XStreaming baseline. Do not treat the baseline run as proof that this newly packaged build has passed.

## Upstream license and attribution

XStreaming is licensed under **GNU Affero General Public License v3.0 (AGPL-3.0)**. This project is based on that source; preserve license notices and comply with the applicable AGPL obligations when distributing modified binaries or offering network access to modified functionality. See [XStreaming's LICENSE](https://github.com/Geocld/XStreaming/blob/06ef1c7ee01d09cf9bb2a97fc4895da85e8488a8/LICENSE).

ONNXCloudGaming is an independent project and is not affiliated with Microsoft or Xbox.
