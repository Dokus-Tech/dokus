package ai.thepredict.shared

enum class ServerEndpoints(val port: Int) {
    Gateway(8080),
    Contacts(8081),
    Documents(8082),
    Identity(8083),
    Prediction(8084),
    Simulation(8085)
}