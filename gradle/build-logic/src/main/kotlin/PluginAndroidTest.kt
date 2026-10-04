import surge.gradle.extensions.alias
import surge.gradle.extensions.libs
import surge.gradle.extensions.surgex
import surge.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidTest : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.test)
            alias(surgex.plugins.android.base)
        }
    }
}
