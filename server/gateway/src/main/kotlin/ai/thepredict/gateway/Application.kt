package ai.thepredict.gateway

import ai.thepredict.common.embeddedServer
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.contacts.api.registerContactsRemoteServices
import ai.thepredict.documents.api.registerDocumentsRemoteServices
import ai.thepredict.identity.api.registerIdentityRemoteServices
import ai.thepredict.prediction.api.registerPredictionRemoteServices
import ai.thepredict.simulation.api.registerSimulationRemoteServices
import io.ktor.server.routing.Routing
import kotlinx.rpc.krpc.ktor.server.rpc

fun main() {
    embeddedServer(endpoint = ServerEndpoint.Gateway(), routing = Routing::configureRouting).start(
        wait = true
    )
}

private fun Routing.configureRouting() {
    rpc {
        configure()
        registerIdentityRemoteServices()
        registerDocumentsRemoteServices()
        registerContactsRemoteServices()
        registerPredictionRemoteServices()
        registerSimulationRemoteServices()
    }
}