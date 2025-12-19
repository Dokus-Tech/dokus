package ai.dokus.foundation.domain.config

data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val versionCode: Int,
    val versionName: String,
)

val appVersion = AppVersion(
    major = BuildKonfig.appVersionMajor,
    minor = BuildKonfig.appVersionMinor,
    patch = BuildKonfig.appVersionBuild,
    versionCode = BuildKonfig.appVersionCode,
    versionName = BuildKonfig.appVersionName
)