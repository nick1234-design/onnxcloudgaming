plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.nick1234.onnxcloudgaming"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nick1234.onnxcloudgaming"
        minSdk = 23
        targetSdk = 28
        versionCode = 1
        versionName = "1.0.0"
    }
}