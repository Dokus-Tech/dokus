package ai.thepredict.documents

import ai.thepredict.common.configureRpc
import ai.thepredict.common.embeddedServer
import ai.thepredict.configuration.ServerEndpoint
import io.ktor.server.routing.Routing
import kotlinx.rpc.krpc.ktor.server.rpc

fun main() {
    embeddedServer(ServerEndpoint.Documents, Routing::configureRouting).start(wait = true)
}

private fun Routing.configureRouting() {
    rpc {
        configureRpc()
    }
}