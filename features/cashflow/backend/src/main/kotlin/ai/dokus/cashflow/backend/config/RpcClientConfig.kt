package ai.dokus.cashflow.backend.config

import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.domain.rpc.AuthValidationRemoteService
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

    // RPC client for Auth Service (uses internal host for inter-service communication)
    single<RpcClient>(named("authClient")) {
        val httpClient = get<HttpClient>()
        val endpoint = DokusEndpoint.Auth
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

    // Service proxies using named RPC clients
    single<AuthValidationRemoteService> { get<RpcClient>(named("authClient")).withService() }
}
