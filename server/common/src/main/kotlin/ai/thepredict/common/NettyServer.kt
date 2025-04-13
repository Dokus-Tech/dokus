package ai.thepredict.common

import ai.thepredict.configuration.ServerEndpoint
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.KrpcConfigBuilder
import kotlinx.rpc.krpc.ktor.server.Krpc

inline fun embeddedServer(
    endpoint: ServerEndpoint,
    crossinline routing: Routing.() -> Unit,
    plugin: ApplicationPlugin<KrpcConfigBuilder.Server>? = Krpc,
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    return embeddedServer(
        Netty,
        port = endpoint.internalPort,
        host = endpoint.internalHost
    ) {
        plugin?.let(::install)

        routing {
            routing()

            addInfoResponse(endpoint)
        }
    }
}