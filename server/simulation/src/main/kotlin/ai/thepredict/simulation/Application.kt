package ai.thepredict.simulation

import ai.thepredict.common.configureRpc
import ai.thepredict.common.embeddedServer
import ai.thepredict.configuration.ServerEndpoint
import io.ktor.server.routing.Routing
import kotlinx.rpc.krpc.ktor.server.rpc

fun main() {
    embeddedServer(ServerEndpoint.Simulation, Routing::configureRouting).start(wait = true)
}

private fun Routing.configureRouting() {
    rpc {
        configureRpc()
    }
}