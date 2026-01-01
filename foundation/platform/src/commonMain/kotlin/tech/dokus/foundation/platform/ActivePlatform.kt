package tech.dokus.foundation.platform

enum class ActivePlatform {
    ANDROID,
    IOS,
    WEB,
    DESKTOP
}

expect val activePlatform: ActivePlatform

val ActivePlatform.isWeb: Boolean get() = this == ActivePlatform.WEB
val ActivePlatform.isDesktop: Boolean get() = this == ActivePlatform.DESKTOP
val ActivePlatform.isMobile: Boolean get() = this == ActivePlatform.ANDROID || this == ActivePlatform.IOS