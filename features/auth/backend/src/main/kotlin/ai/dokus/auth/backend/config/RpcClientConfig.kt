package ai.dokus.auth.backend.config

import ai.dokus.foundation.ktor.services.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import kotlinx.rpc.krpc.ktor.client.KtorRpcClient
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.ktor.client.withService
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.dsl.module

val rpcClientModule = module {
    single<KtorRpcClient> {
        val httpClient = HttpClient(CIO) {
            installKrpc()
        }

        httpClient.rpc {
            url {
                host = System.getenv("DATABASE_SERVICE_HOST") ?: "localhost"
                port = System.getenv("DATABASE_SERVICE_PORT")?.toIntOrNull() ?: 9070
                encodedPath = "/api/rpc"
            }

            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }

    single<TenantService> {
        get<KtorRpcClient>().withService<TenantService>()
    }

    single<UserService> {
        get<KtorRpcClient>().withService<UserService>()
    }

    single<ClientService> {
        get<KtorRpcClient>().withService<ClientService>()
    }

    single<InvoiceService> {
        get<KtorRpcClient>().withService<InvoiceService>()
    }

    single<ExpenseService> {
        get<KtorRpcClient>().withService<ExpenseService>()
    }

    single<PaymentService> {
        get<KtorRpcClient>().withService<PaymentService>()
    }
}
