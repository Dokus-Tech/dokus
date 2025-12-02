package ai.dokus.peppol.backend.config

import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.security.JwtValidator
import ai.dokus.peppol.backend.client.RecommandClient
import ai.dokus.peppol.backend.database.tables.PeppolSettingsTable
import ai.dokus.peppol.backend.database.tables.PeppolTransmissionsTable
import ai.dokus.peppol.backend.mapper.PeppolMapper
import ai.dokus.peppol.backend.repository.PeppolSettingsRepository
import ai.dokus.peppol.backend.repository.PeppolTransmissionRepository
import ai.dokus.peppol.backend.service.CashflowServiceClient
import ai.dokus.peppol.backend.service.ICashflowService
import ai.dokus.peppol.backend.service.PeppolService
import ai.dokus.peppol.backend.validator.PeppolValidator
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    install(Koin) {
        modules(
            coreModule(appConfig),
            databaseModule,
            repositoryModule,
            clientModule,
            serviceModule
        )
    }
}

/**
 * Core module - provides base configuration and security
 */
fun coreModule(appConfig: AppBaseConfig) = module {
    single { appConfig }

    // JWT validator for local token validation
    single {
        JwtValidator(appConfig.jwt)
    }

    // HTTP Client for inter-service communication
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }
}

/**
 * Database module - provides database factory and connection
 */
val databaseModule = module {
    single {
        DatabaseFactory(get(), "peppol-pool").apply {
            runBlocking {
                init(
                    PeppolSettingsTable,
                    PeppolTransmissionsTable
                )
            }
        }
    }
}

/**
 * Repository module - data access layer
 */
val repositoryModule = module {
    single { PeppolSettingsRepository() }
    single { PeppolTransmissionRepository() }
}

/**
 * Client module - external API clients
 */
val clientModule = module {
    // Recommand API client
    single {
        val recommandBaseUrl = System.getenv("RECOMMAND_BASE_URL") ?: "https://app.recommand.eu"
        RecommandClient(get(), recommandBaseUrl)
    }

    // Cashflow service client for inter-service communication
    single<ICashflowService> {
        val cashflowServiceBaseUrl = System.getenv("CASHFLOW_SERVICE_URL") ?: "http://localhost:8000"
        CashflowServiceClient(
            httpClient = get(),
            cashflowServiceBaseUrl = cashflowServiceBaseUrl
        )
    }
}

/**
 * Service module - business logic services
 */
val serviceModule = module {
    // Mapper
    single { PeppolMapper() }

    // Validator
    single { PeppolValidator() }

    // Main Peppol service
    single {
        PeppolService(
            settingsRepository = get(),
            transmissionRepository = get(),
            recommandClient = get(),
            mapper = get(),
            validator = get()
        )
    }
}
