package ai.dokus.peppol.backend.config

import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.crypto.AesGcmCredentialCryptoService
import ai.dokus.foundation.ktor.crypto.CredentialCryptoService
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
    val peppolConfig = PeppolConfig.fromConfig(appConfig.config)

    install(Koin) {
        modules(
            coreModule(appConfig),
            peppolConfigModule(peppolConfig),
            cryptoModule(peppolConfig),
            databaseModule,
            repositoryModule,
            clientModule(peppolConfig),
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
 * Peppol config module - provides Peppol-specific configuration
 */
fun peppolConfigModule(peppolConfig: PeppolConfig) = module {
    single { peppolConfig }
}

/**
 * Crypto module - provides credential encryption service
 */
fun cryptoModule(peppolConfig: PeppolConfig) = module {
    single<CredentialCryptoService> {
        val encryptionKey = peppolConfig.encryptionKey

        require(encryptionKey.length >= 32) {
            "Encryption key must be at least 32 characters. " +
                "Set PEPPOL_ENCRYPTION_KEY or ensure JWT_SECRET is at least 32 characters."
        }

        AesGcmCredentialCryptoService(encryptionKey)
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
    single { PeppolSettingsRepository(get()) }
    single { PeppolTransmissionRepository() }
}

/**
 * Client module - external API clients
 */
fun clientModule(peppolConfig: PeppolConfig) = module {
    // Recommand API client
    single {
        RecommandClient(get(), peppolConfig.recommand.baseUrl)
    }

    // Cashflow service client for inter-service communication
    single<ICashflowService> {
        CashflowServiceClient(
            httpClient = get(),
            cashflowServiceBaseUrl = peppolConfig.cashflowService.baseUrl
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
