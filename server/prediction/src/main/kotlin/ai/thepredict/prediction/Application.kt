package ai.thepredict.prediction

import ai.thepredict.common.configureRpc
import ai.thepredict.common.embeddedServer
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.prediction.api.registerPredictionRemoteServices
import io.ktor.server.routing.Routing
import kotlinx.rpc.krpc.ktor.server.rpc

fun main() {
    embeddedServer(ServerEndpoint.Prediction(), Routing::configureRouting).start(wait = true)
}

private fun Routing.configureRouting() {
    rpc {
        configureRpc()
        registerPredictionRemoteServices()
    }
}