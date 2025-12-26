package ai.dokus.cashflow.backend.config

import ai.dokus.ai.config.AIConfig
import ai.dokus.cashflow.backend.database.CashflowTables
import ai.dokus.cashflow.backend.repository.ChatRepository
import ai.dokus.cashflow.backend.repository.DocumentChunksRepository
import ai.dokus.cashflow.backend.service.BillService
import ai.dokus.cashflow.backend.service.CashflowOverviewService
import ai.dokus.cashflow.backend.service.DocumentStorageService
import ai.dokus.cashflow.backend.service.ExpenseService
import ai.dokus.cashflow.backend.service.InvoiceService
import ai.dokus.foundation.database.di.repositoryModules
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.config.MinioConfig
import ai.dokus.foundation.ktor.crypto.AesGcmCredentialCryptoService
import ai.dokus.foundation.ktor.crypto.CredentialCryptoService
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.security.JwtValidator
import ai.dokus.foundation.ktor.storage.MinioStorage
import ai.dokus.foundation.ktor.storage.ObjectStorage
import ai.dokus.peppol.config.PeppolModuleConfig
import ai.dokus.peppol.mapper.PeppolMapper
import ai.dokus.peppol.provider.PeppolProviderFactory
import ai.dokus.peppol.providers.recommand.RecommandCompaniesClient
import ai.dokus.peppol.service.PeppolConnectionService
import ai.dokus.peppol.service.PeppolService
import ai.dokus.peppol.validator.PeppolValidator
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory
import ai.dokus.foundation.ktor.storage.DocumentStorageService as MinioDocumentStorageService

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    install(Koin) {
        modules(
            coreModule(appConfig),
            databaseModule,
            repositoryModules,
            storageModule(appConfig),
            serviceModule,
            peppolModule(appConfig),
            chatModule(appConfig)
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

    // HTTP Client for inter-service communication and AI calls
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            engine {
                requestTimeout = 120_000 // 2 minutes for AI calls
            }
        }
    }

    // Credential encryption service (used by Peppol settings)
    single<CredentialCryptoService> {
        val encryptionKey = System.getenv("ENCRYPTION_KEY")
            ?: appConfig.jwt.secret // Fall back to JWT secret if ENCRYPTION_KEY not set
        AesGcmCredentialCryptoService(encryptionKey)
    }
}

/**
 * Database module - provides database factory and connection
 */
val databaseModule = module {
    single {
        DatabaseFactory(get(), "cashflow-pool").apply {
            runBlocking {
                connect()
                CashflowTables.initialize()
            }
        }
    }
}


/**
 * Storage module - object storage configuration
 */
fun storageModule(appConfig: AppBaseConfig) = module {
    val logger = LoggerFactory.getLogger("StorageModule")

    // MinIO Object Storage (when available)
    single<ObjectStorage> {
        val minioConfig = MinioConfig.loadOrNull(appConfig)
        requireNotNull(minioConfig)
        val publicUrl = appConfig.storage.publicUrl
        logger.info("MinIO storage configured: endpoint=${minioConfig.endpoint}, bucket=${minioConfig.bucket}, publicUrl=$publicUrl")
        MinioStorage.create(minioConfig, publicUrl)
    }

    // MinIO Document Storage Service (high-level API)
    single<MinioDocumentStorageService> {
        val objectStorage = get<ObjectStorage>()
        MinioDocumentStorageService(objectStorage)
    }
}

/**
 * Service module - business logic services
 */
val serviceModule = module {
    // Legacy document storage service (local filesystem fallback)
    single {
        DocumentStorageService(
            storageBasePath = "./storage/documents",
            maxFileSizeMb = 10
        )
    }

    // Business logic services
    single { InvoiceService(get()) }
    single { ExpenseService(get()) }
    single { BillService(get()) }
    single { CashflowOverviewService(get(), get(), get()) }
}

/**
 * Peppol module - e-invoicing services
 */
fun peppolModule(appConfig: AppBaseConfig) = module {
    val logger = LoggerFactory.getLogger("PeppolModule")

    // Peppol module configuration from HOCON
    single {
        PeppolModuleConfig.fromConfig(appConfig.config).also {
            logger.info("Peppol module configured: defaultProvider=${it.defaultProvider}, pollingEnabled=${it.inbox.pollingEnabled}")
        }
    }

    // Provider factory - creates provider instances based on provider ID
    single {
        PeppolProviderFactory(get(), get<PeppolModuleConfig>())
    }

    // Recommand companies client - used for company discovery and setup
    single {
        RecommandCompaniesClient(get(), get<PeppolModuleConfig>())
    }

    // Peppol mapper - converts domain models to Peppol format
    single { PeppolMapper() }

    // Peppol validator - validates invoices for Peppol compliance
    single { PeppolValidator() }

    // Peppol connection service - handles Recommand company matching/creation
    single {
        PeppolConnectionService(
            settingsRepository = get(),
            recommandCompaniesClient = get()
        )
    }

    // Main Peppol service - orchestrates all Peppol operations
    single {
        PeppolService(
            settingsRepository = get(),
            transmissionRepository = get(),
            providerFactory = get(),
            mapper = get(),
            validator = get()
        )
    }
}

/**
 * Chat module - AI chat and RAG services for document Q&A.
 * Provides repositories and configuration for the chat API endpoints.
 */
fun chatModule(appConfig: AppBaseConfig) = module {
    val logger = LoggerFactory.getLogger("ChatModule")

    // AI Configuration - uses local Ollama by default, can be overridden via HOCON
    single<AIConfig> {
        val aiConfig = AIConfig.fromConfigOrNull(appConfig.config)
        if (aiConfig != null) {
            logger.info("Loaded AI config from HOCON: provider=${aiConfig.defaultProvider}")
            aiConfig
        } else {
            logger.info("No AI config in HOCON, using local Ollama default")
            AIConfig.localDefault()
        }
    }

    // Chat message repository for conversation persistence
    single { ChatRepository() }

    // Document chunks repository for vector embeddings and similarity search
    single { DocumentChunksRepository() }
}
