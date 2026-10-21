plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.smaarig.glyphbarcomposer.core.glyphsdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }
}

dependencies {
    api(project(":core:glyph-matrix-sdk"))
    compileOnly(mapOf("name" to "glyph-matrix-sdk-2.0", "ext" to "aar"))
}
