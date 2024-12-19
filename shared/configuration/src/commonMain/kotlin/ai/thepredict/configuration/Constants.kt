package ai.thepredict.configuration

private const val DEFAULT_PORT = 8080

enum class ServerEndpoints(val internalPort: Int, val externalPort: Int) {
    Gateway(DEFAULT_PORT, 8080),
    Contacts(DEFAULT_PORT, 8081),
    Documents(DEFAULT_PORT, 8082),
    Identity(DEFAULT_PORT, 8083),
    Prediction(DEFAULT_PORT, 8084),
    Simulation(DEFAULT_PORT, 8085)
}