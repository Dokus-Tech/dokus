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
 *
 * Version name can be overridden by setting:
 *   - System property: -Dapp.version.name=1.2.3
 *   - Gradle property: -Papp.version.name=1.2.3
 *   - Environment variable: APP_VERSION_NAME=1.2.3
 *
 * This allows CI/CD to set the version from a git tag during releases.
 */
class VersioningPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Check for version name override from various sources
        val versionNameOverride = resolveVersionName(target)

        // Create extensions for version access
        target.extensions.create("appVersion", AppVersionExtension::class.java, versionNameOverride)
        target.extensions.create("bundleIds", BundleIdsExtension::class.java)
    }

    /**
     * Resolves version name from (in order of priority):
     * 1. Gradle project property (-Papp.version.name=x.y.z)
     * 2. System property (-Dapp.version.name=x.y.z)
     * 3. Environment variable (APP_VERSION_NAME=x.y.z)
     * 4. Default from AppVersion.NAME
     */
    private fun resolveVersionName(project: Project): String {
        // Check Gradle project property
        val gradleProp = project.findProperty("app.version.name") as? String
        if (!gradleProp.isNullOrBlank()) {
            project.logger.lifecycle("Using version name from Gradle property: $gradleProp")
            return gradleProp
        }

        // Check system property
        val sysProp = System.getProperty("app.version.name")
        if (!sysProp.isNullOrBlank()) {
            project.logger.lifecycle("Using version name from system property: $sysProp")
            return sysProp
        }

        // Check environment variable
        val envVar = System.getenv("APP_VERSION_NAME")
        if (!envVar.isNullOrBlank()) {
            project.logger.lifecycle("Using version name from environment: $envVar")
            return envVar
        }

        // Default to AppVersion.NAME
        return AppVersion.NAME
    }
}

/**
 * Extension exposing AppVersion constants to build scripts.
 *
 * Version name can be overridden via system property, Gradle property,
 * or environment variable (see VersioningPlugin for details).
 *
 * Note: Individual major/minor/patch components are not exposed separately
 * because the version name from git tags is the single source of truth.
 * Use `name` for the full version string (e.g., "1.2.3").
 */
open class AppVersionExtension(private val versionNameOverride: String? = null) {
    val name: String = versionNameOverride ?: AppVersion.NAME
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
