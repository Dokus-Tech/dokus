package ai.thepredict.configuration

private const val DEFAULT_INTERNAL_HOST = "0.0.0.0"

enum class ServerEndpoint(val internalHost: String, val internalPort: Int, val externalPort: Int) {
    Gateway(DEFAULT_INTERNAL_HOST, 8080, 8080),
    Contacts(DEFAULT_INTERNAL_HOST, 8081, 8081),
    Documents(DEFAULT_INTERNAL_HOST, 8082, 8082),
    Identity(DEFAULT_INTERNAL_HOST, 8083, 8083),
    Prediction(DEFAULT_INTERNAL_HOST, 8084, 8084),
    Simulation(DEFAULT_INTERNAL_HOST, 8085, 8085)
}

val ServerEndpoint.info: String get() = "The Predict module: $name. \nListening on $internalPort"