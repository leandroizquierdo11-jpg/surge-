plugins {
    alias(surgex.plugins.android.library)
    alias(surgex.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "surge.core.viewmodel"
}

dependencies {
    implementation(libs.androidx.lifecycle.viewmodel)
}
