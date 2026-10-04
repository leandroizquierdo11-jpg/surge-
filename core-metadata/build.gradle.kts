plugins {
    alias(surgex.plugins.android.library)
    alias(surgex.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "tachiyomi.core.metadata"
}

dependencies {
    implementation(projects.sourceApi)

    implementation(libs.bundles.serialization)
}
