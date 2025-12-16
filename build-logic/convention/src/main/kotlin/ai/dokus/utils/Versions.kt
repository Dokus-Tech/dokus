package ai.dokus.utils

/**
 * Single source of truth for app versioning.
 *
 * CI updates BUILD before each build using sed.
 */
object AppVersion {
    const val MAJOR = 1
    const val MINOR = 1
    const val BUILD = 1  // CI sets this to git commit count

    /** Version string: "MAJOR.MINOR.BUILD" */
    const val NAME = "$MAJOR.$MINOR.$BUILD"

    /** Version code: just the build number (git commit count) */
    const val CODE = BUILD
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
