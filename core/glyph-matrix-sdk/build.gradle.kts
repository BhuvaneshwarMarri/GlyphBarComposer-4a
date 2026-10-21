plugins {
    id("com.android.library")
}

android {
    namespace = "com.nothing.cardsdk"
    compileSdk = 36
}

dependencies {
    api(mapOf("name" to "glyph-matrix-sdk-2.0", "ext" to "aar"))
}
