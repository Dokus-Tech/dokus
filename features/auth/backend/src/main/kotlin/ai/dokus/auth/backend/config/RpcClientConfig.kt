package ai.dokus.auth.backend.config

import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.ktor.services.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
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

    // RPC client for Invoicing Service (uses internal host for inter-service communication)
    single<RpcClient>(named("invoicingClient")) {
        val httpClient = get<HttpClient>()
        val endpoint = DokusEndpoint.Invoicing
        httpClient.rpc {
            url {
                protocol = URLProtocol.WS
                host = endpoint.internalHost
                port = endpoint.internalPort
                path("/api/rpc")
            }
            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }

    // Service proxies for other backends
    single<ClientService> { get<RpcClient>(named("invoicingClient")).withService() }
    single<InvoiceService> { get<RpcClient>(named("invoicingClient")).withService() }
    // TODO: Add RPC clients for expense, payment services when needed
}
