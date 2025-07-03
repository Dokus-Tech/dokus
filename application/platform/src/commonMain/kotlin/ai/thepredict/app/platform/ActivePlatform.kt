package ai.thepredict.app.platform

enum class ActivePlatform {
    ANDROID,
    IOS,
    WEB,
    DESKTOP
}

expect val activePlatform: ActivePlatform