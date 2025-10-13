package ai.dokus.auth.backend.config

import ai.dokus.foundation.ktor.services.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.dsl.module

val rpcClientModule = module {
    single {
        val databaseServiceUrl = System.getenv("DATABASE_SERVICE_URL") ?: "http://localhost:9070"

        val httpClient = HttpClient(CIO) {
            installKrpc()
        }

        httpClient.rpc("$databaseServiceUrl/api/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }

    single<TenantService> {
        get<kotlinx.rpc.krpc.client.KrpcClient>().withService<TenantService>()
    }

    single<UserService> {
        get<kotlinx.rpc.krpc.client.KrpcClient>().withService<UserService>()
    }

    single<ClientService> {
        get<kotlinx.rpc.krpc.client.KrpcClient>().withService<ClientService>()
    }

    single<InvoiceService> {
        get<kotlinx.rpc.krpc.client.KrpcClient>().withService<InvoiceService>()
    }

    single<ExpenseService> {
        get<kotlinx.rpc.krpc.client.KrpcClient>().withService<ExpenseService>()
    }

    single<PaymentService> {
        get<kotlinx.rpc.krpc.client.KrpcClient>().withService<PaymentService>()
    }
}
