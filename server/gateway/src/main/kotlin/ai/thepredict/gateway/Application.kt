package ai.thepredict.gateway

import ai.thepredict.configuration.ServerEndpoints
import ai.thepredict.gateway.api.registerServices
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.RPC
import kotlinx.rpc.krpc.ktor.server.rpc

fun main() {
    embeddedServer(
        Netty,
        host = "0.0.0.0",
        port = ServerEndpoints.Gateway.internalPort,
    ) {
//        install(WebSockets) {
//            pingPeriod = 15.seconds
//            timeout = 15.seconds
//            maxFrameSize = Long.MAX_VALUE
//            masking = false
//        }
        module()
    }.start(wait = true)
}

private fun Application.module() {
    install(RPC)

    routing {
        rpc {
            configure()
            registerServices()
        }

        get("/info") {
            call.respondText("Gateway: Hey")
        }
    }
}