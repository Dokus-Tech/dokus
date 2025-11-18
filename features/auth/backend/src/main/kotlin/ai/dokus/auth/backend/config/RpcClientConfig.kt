package ai.dokus.auth.backend.config

import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.ktor.services.ClientService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.rpc.RpcClient
import kotlinx.rpc.krpc.ktor.client.Krpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import org.koin.core.qualifier.named
import org.koin.dsl.module

val rpcClientModule = module {
    // Shared HTTP client with WebSockets and Krpc plugins for calling other services
    single<HttpClient> {
        HttpClient(CIO) {
            install(WebSockets)
            install(Krpc)
        }
    }

    // RPC client for Cashflow Service (uses internal host for inter-service communication)
    single<RpcClient>(named("cashflowClient")) {
        val httpClient = get<HttpClient>()
        val endpoint = DokusEndpoint.Cashflow
        httpClient.rpc {
            url {
                protocol = URLProtocol.WS
                host = endpoint.internalHost
                port = endpoint.internalPort
                path("/rpc")
            }
            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }

    // Service proxies for other backends
    single<ClientService> { get<RpcClient>(named("cashflowClient")).withService() }
}
