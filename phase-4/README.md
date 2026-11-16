# Phase 4 — Settings Integration

This phase replaces the Phase 3 settings screen with a settings experience connected to XStreaming's existing settings system.

## What is included

- Reads the live settings state from XStreaming's `getSettings()` store.
- Saves toggles through `saveSettings()`, which persists the settings in the native MMKV-backed store and emits the existing settings-changed event.
- Opens XStreaming's existing `SettingDetail` screen for resolution, cloud bitrate, codec, audio bitrate, theme, and accent color. Those screens provide the supported options and save through the same native settings store.
- Displays the currently selected resolution, cloud bitrate, and codec.
- Adds explicit focus styling and a D-pad selection map for the Settings screen.
- Keeps the existing native XStreaming streaming engine and its supported settings; it does not invent unsupported controls or force a codec.

## Supported streaming choices

The upstream settings metadata provides 720p, 1080p, and 1080p HQ/1440p resolution choices, plus Auto and H.264 High/Medium/Low codec preferences. Actual resolution and codec selection remain subject to Xbox Cloud Gaming and stream negotiation. Auto remains the default codec.

## Test checklist

1. Open Settings and use the controller D-pad to move through the menu.
2. Change resolution and codec, save each choice, then return to Settings and confirm the displayed values update.
3. Restart the app and confirm the selections remain saved.
4. Toggle Stereo Sound, Controller Vibration, and IPv6; reopen Settings and confirm the values persist.
5. Launch a cloud game and confirm streaming still connects and controller/audio behavior remains normal.

This is a test build. A successful GitHub Actions build does not replace on-device validation.
