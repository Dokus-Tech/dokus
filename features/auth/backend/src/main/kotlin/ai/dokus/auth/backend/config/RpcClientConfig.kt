package ai.dokus.auth.backend.config

import ai.dokus.foundation.ktor.services.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.dsl.module

val rpcClientModule = module {
    single {
        val databaseServiceUrl = System.getenv("DATABASE_SERVICE_URL") ?: "http://localhost:9070"

        HttpClient(CIO) {
            installRPC()
        }.rpc("$databaseServiceUrl/api/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }

    single<TenantService> {
        get<kotlinx.rpc.krpc.StreamRPCClient>().withService()
    }

    single<UserService> {
        get<kotlinx.rpc.krpc.StreamRPCClient>().withService()
    }

    single<ClientService> {
        get<kotlinx.rpc.krpc.StreamRPCClient>().withService()
    }

    single<InvoiceService> {
        get<kotlinx.rpc.krpc.StreamRPCClient>().withService()
    }

    single<ExpenseService> {
        get<kotlinx.rpc.krpc.StreamRPCClient>().withService()
    }

    single<PaymentService> {
        get<kotlinx.rpc.krpc.StreamRPCClient>().withService()
    }
}
