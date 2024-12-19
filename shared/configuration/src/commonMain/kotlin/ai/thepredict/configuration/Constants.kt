package ai.thepredict.configuration

private const val DEFAULT_PORT = 8080

enum class ServerEndpoints(val internalPort: Int, val externalPort: Int) {
    Gateway(8080, 8080),
    Contacts(8081, 8081),
    Documents(8082, 8082),
    Identity(8083, 8083),
    Prediction(8084, 8084),
    Simulation(8085, 8085)
}