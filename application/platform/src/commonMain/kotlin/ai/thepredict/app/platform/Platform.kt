package ai.thepredict.app.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform