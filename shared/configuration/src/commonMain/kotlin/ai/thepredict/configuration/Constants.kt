package ai.thepredict.configuration

sealed interface ServerEndpoint {
    val host: String
    val port: Int?
    val isLocal: Boolean

    data object PredictCloud : ServerEndpoint {
        override val host = "api.thepredict.ai"
        override val port = null
        override val isLocal = false
    }

    data object LocalAndroid : ServerEndpoint {
        override val host = "10.0.2.2"
        override val port = 8000
        override val isLocal = true
    }

    data object Local : ServerEndpoint {
        override val host = "127.0.0.1"
        override val port = 8000
        override val isLocal = true
    }
}

private val ServerEndpoint.name: String
    get() = when (this) {
        is ServerEndpoint.PredictCloud -> "PredictCloud"
        is ServerEndpoint.LocalAndroid -> "LocalAndroid"
        is ServerEndpoint.Local -> "Local"
    }