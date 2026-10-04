import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.CommonExtension
import surge.gradle.extensions.alias
import surge.gradle.extensions.android
import surge.gradle.extensions.api
import surge.gradle.extensions.debugApi
import surge.gradle.extensions.implementation
import surge.gradle.extensions.libs
import surge.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("UNUSED")
class PluginComposeAndroid : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.kotlin.compose.compiler)
        }

        android {
            buildFeatures {
                compose = true
            }
        }

        dependencies {
            implementation(platform(libs.androidx.compose.bom))

            // Compose @Preview tooling
            api(libs.androidx.compose.uiToolingPreview)
            debugApi(libs.androidx.compose.uiTooling)
        }
    }
}

private fun CommonExtension.buildFeatures(block: BuildFeatures.() -> Unit) {
    buildFeatures.apply(block)
}
