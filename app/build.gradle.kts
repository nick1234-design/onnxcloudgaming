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

    // Keep Java bytecode target aligned with Kotlin's JVM 17 target.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}