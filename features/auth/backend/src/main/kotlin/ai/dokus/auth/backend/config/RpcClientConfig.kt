package ai.dokus.auth.backend.config

import ai.dokus.foundation.ktor.services.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import kotlinx.rpc.krpc.ktor.client.Krpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import org.koin.dsl.module

val rpcClientModule = module {
    single<HttpClient> {
        HttpClient(CIO) {
            install(Krpc)
        }
    }

    single<TenantService> {
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
        }.withService()
    }

    single<UserService> {
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
        }.withService()
    }

    single<ClientService> {
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
        }.withService()
    }

    single<InvoiceService> {
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
        }.withService()
    }

    single<ExpenseService> {
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
        }.withService()
    }

    single<PaymentService> {
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
        }.withService()
    }
}
