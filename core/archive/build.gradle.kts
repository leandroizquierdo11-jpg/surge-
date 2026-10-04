plugins {
    alias(surgex.plugins.android.library)
    alias(surgex.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "surge.core.archive"
}

dependencies {
    implementation(libs.jsoup)
    implementation(libs.archive)
    implementation(libs.unifile)
}
