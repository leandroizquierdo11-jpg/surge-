import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import surge.gradle.configurations.configureKotlin
import surge.gradle.extensions.alias
import surge.gradle.extensions.configureTest
import surge.gradle.extensions.coreLibraryDesugaring
import surge.gradle.extensions.libs
import surge.gradle.extensions.surgex
import surge.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("UNUSED")
class PluginKotlinMultiplatform : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.kmp.library)
            alias(libs.plugins.kotlin.multiplatform)
        }

        configureKotlin()
        configureTest()

        kotlin {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            applyDefaultHierarchyTemplate()

            android {
                minSdk = surgex.versions.android.sdk.min.get().toInt()
                compileSdk = surgex.versions.android.sdk.compile.get().toInt()
                enableCoreLibraryDesugaring = true
            }
        }

        dependencies {
            coreLibraryDesugaring(libs.android.desugar)
        }
    }
}

private fun Project.kotlin(block: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(block)
}

private fun KotlinMultiplatformExtension.android(block: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach(block)
}
