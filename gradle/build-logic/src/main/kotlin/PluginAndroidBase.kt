import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.DefaultConfig
import surge.gradle.configurations.configureKotlin
import surge.gradle.extensions.android
import surge.gradle.extensions.configureTest
import surge.gradle.extensions.coreLibraryDesugaring
import surge.gradle.extensions.libs
import surge.gradle.extensions.surgex
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("UNUSED")
class PluginAndroidBase : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        configureKotlin()
        configureTest()

        android {
            defaultConfig {
                minSdk = surgex.versions.android.sdk.min.get().toInt()
                if (this is ApplicationDefaultConfig) {
                    targetSdk = surgex.versions.android.sdk.target.get().toInt()
                }

                ndkVersion = surgex.versions.android.ndk.get()
            }

            compileSdk = surgex.versions.android.sdk.compile.get().toInt()

            compileOptions {
                isCoreLibraryDesugaringEnabled = true
            }
        }

        dependencies {
            coreLibraryDesugaring(libs.android.desugar)
        }
    }
}

private fun CommonExtension.defaultConfig(block: DefaultConfig.() -> Unit) {
    defaultConfig.apply(block)
}

private fun CommonExtension.compileOptions(block: CompileOptions.() -> Unit) {
    compileOptions.apply(block)
}
