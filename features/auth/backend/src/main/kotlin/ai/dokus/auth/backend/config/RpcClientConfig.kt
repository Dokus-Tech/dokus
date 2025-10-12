package ai.dokus.auth.backend.config

import ai.dokus.foundation.ktor.services.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.dsl.module

val rpcClientModule = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            installRPC()
        }
    }

    single {
        val httpClient = get<HttpClient>()
        val databaseServiceUrl = System.getenv("DATABASE_SERVICE_URL") ?: "http://localhost:9070"

        httpClient.rpc("$databaseServiceUrl/api/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }

    single<TenantService> {
        val rpcClient = get<kotlinx.rpc.krpc.RPCClient>()
        rpcClient.withService()
    }

    single<UserService> {
        val rpcClient = get<kotlinx.rpc.krpc.RPCClient>()
        rpcClient.withService()
    }

    single<ClientService> {
        val rpcClient = get<kotlinx.rpc.krpc.RPCClient>()
        rpcClient.withService()
    }

    single<InvoiceService> {
        val rpcClient = get<kotlinx.rpc.krpc.RPCClient>()
        rpcClient.withService()
    }

    single<ExpenseService> {
        val rpcClient = get<kotlinx.rpc.krpc.RPCClient>()
        rpcClient.withService()
    }

    single<PaymentService> {
        val rpcClient = get<kotlinx.rpc.krpc.RPCClient>()
        rpcClient.withService()
    }
}
