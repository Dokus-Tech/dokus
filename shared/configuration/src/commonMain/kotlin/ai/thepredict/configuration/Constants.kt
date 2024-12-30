package ai.thepredict.configuration

private const val DEFAULT_INTERNAL_HOST = "0.0.0.0"
private const val DEFAULT_EXTERNAL_HOST = "predict.local"

sealed interface ServerEndpoint {
    val internalHost: String
    val externalHost: String
    val internalPort: Int
    val externalPort: Int

    data class Website(
        override val externalHost: String = DEFAULT_EXTERNAL_HOST,
    ) : ServerEndpoint {
        override val internalHost: String = DEFAULT_INTERNAL_HOST
        override val internalPort: Int = 8080
        override val externalPort: Int = 8080
    }

    data class Gateway(
        override val externalHost: String = DEFAULT_EXTERNAL_HOST,
    ) : ServerEndpoint {
        override val internalHost: String = DEFAULT_INTERNAL_HOST
        override val internalPort: Int = 8089
        override val externalPort: Int = 8089
    }

    data class Contacts(
        override val externalHost: String = DEFAULT_EXTERNAL_HOST,
    ) : ServerEndpoint {
        override val internalHost: String = DEFAULT_INTERNAL_HOST
        override val internalPort: Int = 8081
        override val externalPort: Int = 8081
    }

    data class Documents(
        override val externalHost: String = DEFAULT_EXTERNAL_HOST,
    ) : ServerEndpoint {
        override val internalHost: String = DEFAULT_INTERNAL_HOST
        override val internalPort: Int = 8082
        override val externalPort: Int = 8082
    }

    data class Identity(
        override val externalHost: String = DEFAULT_EXTERNAL_HOST,
    ) : ServerEndpoint {
        override val internalHost: String = DEFAULT_INTERNAL_HOST
        override val internalPort: Int = 8083
        override val externalPort: Int = 8083
    }

    data class Prediction(
        override val externalHost: String = DEFAULT_EXTERNAL_HOST,
    ) : ServerEndpoint {
        override val internalHost: String = DEFAULT_INTERNAL_HOST
        override val internalPort: Int = 8084
        override val externalPort: Int = 8084
    }

    data class Simulation(
        override val externalHost: String = DEFAULT_EXTERNAL_HOST,
    ) : ServerEndpoint {
        override val internalHost: String = DEFAULT_INTERNAL_HOST
        override val internalPort: Int = 8085
        override val externalPort: Int = 8085
    }

    data object Database : ServerEndpoint {
        override val internalHost = "the-predict-database-postgres"
        override val externalHost = DEFAULT_EXTERNAL_HOST
        override val internalPort = 5432
        override val externalPort = 8090

        val connectUrl = "${internalHost}:${internalPort}"
    }
}

private val ServerEndpoint.name: String
    get() = when (this) {
        is ServerEndpoint.Website -> "Website"
        is ServerEndpoint.Gateway -> "Gateway"
        is ServerEndpoint.Contacts -> "Contacts"
        is ServerEndpoint.Documents -> "Documents"
        is ServerEndpoint.Identity -> "Identity"
        is ServerEndpoint.Prediction -> "Prediction"
        is ServerEndpoint.Simulation -> "Simulation"
        is ServerEndpoint.Database -> "Database"
    }

val ServerEndpoint.info: String get() = "The Predict module: $name. \nListening on $internalPort"