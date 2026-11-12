# onnxcloudgaming

A simple Android TV launcher app for Xbox Cloud Gaming, aimed at the Onn 4K Plus.

## Build an APK with GitHub Actions
1. Create a GitHub repository named `onnxcloudgaming`.
2. Upload the contents of this project ZIP to the repository root.
3. Open **Actions** and select **Build onnxcloudgaming APK**.
4. Run the workflow (or push to `main` and let it run).
5. Open the successful run and download the `onnxcloudgaming-debug-apk` artifact ZIP.
6. Extract `app-debug.apk` and install it on the Onn 4K Plus.

## Important compatibility note
The app includes an embedded WebView and a Chrome fallback. Xbox Cloud Gaming officially supports selected browsers and devices; Android TV WebView compatibility is not guaranteed. If Microsoft sign-in or game streaming is blocked in the embedded view, use **OPEN IN CHROME** from the home screen. A compatible browser, supported region, eligible Game Pass plan, and supported controller may be required.

This project is an independent launcher and is not affiliated with Microsoft or Xbox.