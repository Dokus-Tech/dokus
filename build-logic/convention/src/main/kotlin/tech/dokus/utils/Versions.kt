package tech.dokus.utils

/**
 * Single source of truth for app versioning.
 *
 * During releases, the version name is overridden from the git tag
 * via the APP_VERSION_NAME environment variable (see VersioningPlugin).
 *
 * The values here are development defaults only.
 */
object AppVersion {
    /** Default version name for development builds */
    const val NAME = "1.0.0"

    /** Version code: git commit count (set by CI) */
    const val CODE = 1
}

/**
 * Bundle identifiers for all platforms.
 * Keep in sync across Android, iOS, and macOS.
 */
object BundleIds {
    const val ANDROID = "vision.invoid.dokus"
    const val IOS = "vision.invoid.dokus"
    const val MACOS = "vision.invoid.dokus"
}
