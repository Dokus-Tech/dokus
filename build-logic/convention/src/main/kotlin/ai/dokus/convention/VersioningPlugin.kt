package ai.dokus.convention

import ai.dokus.utils.AppVersion
import ai.dokus.utils.BundleIds
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin that exposes version info as project extensions.
 *
 * Apply this plugin to access version constants in build scripts:
 *   plugins { id("dokus.versioning") }
 *
 *   val versionCode = appVersion.codeBase
 *   val versionName = appVersion.name
 */
class VersioningPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Create extensions for version access
        target.extensions.create("appVersion", AppVersionExtension::class.java)
        target.extensions.create("bundleIds", BundleIdsExtension::class.java)
    }
}

/**
 * Extension exposing AppVersion constants to build scripts.
 */
open class AppVersionExtension {
    val major: Int = AppVersion.MAJOR
    val minor: Int = AppVersion.MINOR
    val build: Int = AppVersion.BUILD
    val name: String = AppVersion.NAME
    val code: Int = AppVersion.CODE
}

/**
 * Extension exposing BundleIds constants to build scripts.
 */
open class BundleIdsExtension {
    val android: String = BundleIds.ANDROID
    val ios: String = BundleIds.IOS
    val macos: String = BundleIds.MACOS
}
