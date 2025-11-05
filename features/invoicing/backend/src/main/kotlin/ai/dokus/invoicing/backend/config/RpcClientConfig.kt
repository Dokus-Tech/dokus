package ai.dokus.invoicing.backend.config

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

    // RPC client for Auth Service (for TenantService)
    single<RpcClient>(named("authClient")) {
        val httpClient = get<HttpClient>()
        val endpoint = DokusEndpoint.Auth
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

    // RPC client for Audit Service
    single<RpcClient>(named("auditClient")) {
        val httpClient = get<HttpClient>()
        val endpoint = DokusEndpoint.Audit
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

    // RPC client for Expense Service
    single<RpcClient>(named("expenseClient")) {
        val httpClient = get<HttpClient>()
        val endpoint = DokusEndpoint.Expense
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

    // RPC client for Payment Service
    single<RpcClient>(named("paymentClient")) {
        val httpClient = get<HttpClient>()
        val endpoint = DokusEndpoint.Payment
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
    single<TenantService> { get<RpcClient>(named("authClient")).withService() }
    single<AuditService> { get<RpcClient>(named("auditClient")).withService() }
    single<ExpenseService> { get<RpcClient>(named("expenseClient")).withService() }
    single<PaymentService> { get<RpcClient>(named("paymentClient")).withService() }
}
