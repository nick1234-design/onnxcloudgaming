# ONNXCloudGaming — Phase 9 Native Experiment

This branch is an isolated native-streaming experiment for the Onn 4K Plus. The existing `main` branch remains the WebView-based fallback and is not changed by this experiment.

## Phase 9.1: XStreaming native foundation

The `vendor/XStreaming` directory is a pinned Git submodule referencing the upstream [Geocld/XStreaming](https://github.com/Geocld/XStreaming) project. It provides the native React Native + WebRTC Xbox cloud-streaming implementation, controller integration, and Android rendering engines.

This phase builds the upstream Android TV-capable app as a baseline APK. It does **not** yet apply ONNXCloudGaming branding or claim that the native build has been tested on the Onn 4K Plus.

## Build

1. Open the **Actions** tab in this repository.
2. Select **Build ONNXCloudGaming Native (XStreaming)**.
3. Choose **Run workflow** on branch `phase-9-native-webrtc-experiment`.
4. When it completes, download the artifact named `onnxcloudgaming-native-phase-9-1`.

The workflow checks out the pinned submodule, installs Node/Yarn, Java, Android NDK and Rust targets, then builds the upstream `devDebug` Android variant. A small build-time patch selects the Linux NDK toolchain when the build runs on GitHub's Ubuntu runner; it does not modify the pinned upstream source.

## Upstream license and attribution

XStreaming is licensed under **GNU Affero General Public License v3.0 (AGPL-3.0)**. Its original source and license notices are preserved in the submodule. This experiment is based on that project; modifications and distribution must follow the applicable AGPL terms. See [XStreaming's LICENSE](https://github.com/Geocld/XStreaming/blob/main/LICENSE).

ONNXCloudGaming is an independent project and is not affiliated with Microsoft or Xbox.
