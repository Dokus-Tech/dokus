package ai.thepredict.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform