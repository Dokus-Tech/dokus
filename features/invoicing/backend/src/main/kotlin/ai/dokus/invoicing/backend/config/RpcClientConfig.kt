package ai.dokus.invoicing.backend.config

import ai.dokus.foundation.ktor.services.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import kotlinx.rpc.RpcClient
import kotlinx.rpc.krpc.ktor.client.Krpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import org.koin.dsl.module

val rpcClientModule = module {
    // Shared HTTP client with Krpc plugin
    single<HttpClient> {
        HttpClient(CIO) {
            install(Krpc)
        }
    }

    // Shared RPC client configured once
    single<RpcClient> {
        val httpClient = get<HttpClient>()
        httpClient.rpc {
            url {
                protocol = URLProtocol.HTTP
                host = System.getenv("DATABASE_SERVICE_HOST") ?: "localhost"
                port = System.getenv("DATABASE_SERVICE_PORT")?.toIntOrNull() ?: 9070
                path("/api/rpc")
            }
            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }

    // Service proxies using shared RPC client (only services registered in database service)
    single<InvoiceService> { get<RpcClient>().withService() }
    single<ClientService> { get<RpcClient>().withService() }
    single<ExpenseService> { get<RpcClient>().withService() }
    single<PaymentService> { get<RpcClient>().withService() }
}
