plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.smaarig.glyphbarcomposer.core.common"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
