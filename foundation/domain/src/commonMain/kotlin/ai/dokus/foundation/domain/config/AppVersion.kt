package ai.dokus.foundation.domain.config

/**
 * App version information.
 *
 * The versionName is the full semantic version string (e.g., "1.2.3")
 * which comes from the git tag during releases.
 */
data class AppVersion(
    val versionCode: Int,
    val versionName: String,
)

val appVersion = AppVersion(
    versionCode = BuildKonfig.appVersionCode,
    versionName = BuildKonfig.appVersionName
)