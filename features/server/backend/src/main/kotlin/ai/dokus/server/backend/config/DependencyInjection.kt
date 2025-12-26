package ai.dokus.server.backend.config

import ai.dokus.ai.config.AIConfig
import ai.dokus.auth.backend.jobs.RateLimitCleanupJob
import ai.dokus.auth.backend.services.AuthService
import ai.dokus.auth.backend.services.DisabledEmailService
import ai.dokus.auth.backend.services.EmailConfig
import ai.dokus.auth.backend.services.EmailService
import ai.dokus.auth.backend.services.EmailVerificationService
import ai.dokus.auth.backend.services.PasswordResetService
import ai.dokus.auth.backend.services.RateLimitService
import ai.dokus.auth.backend.services.RateLimitServiceInterface
import ai.dokus.auth.backend.services.RedisRateLimitService
import ai.dokus.auth.backend.services.SmtpEmailService
import ai.dokus.auth.backend.services.TeamService
import ai.dokus.cashflow.backend.repository.ChatRepository
import ai.dokus.cashflow.backend.repository.DocumentChunksRepository
import ai.dokus.cashflow.backend.service.BillService
import ai.dokus.cashflow.backend.service.CashflowOverviewService
import ai.dokus.cashflow.backend.service.ExpenseService
import ai.dokus.cashflow.backend.service.InvoiceService
import ai.dokus.cashflow.backend.service.DocumentStorageService as LocalDocumentStorageService
import ai.dokus.contacts.backend.service.ContactMatchingService
import ai.dokus.contacts.backend.service.ContactNoteService
import ai.dokus.contacts.backend.service.ContactService
import ai.dokus.foundation.database.di.repositoryModules
import ai.dokus.foundation.database.repository.auth.PasswordResetTokenRepository
import ai.dokus.foundation.database.repository.auth.RefreshTokenRepository
import ai.dokus.foundation.database.repository.auth.UserRepository
import ai.dokus.foundation.database.schema.DokusSchema
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redis
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.config.MinioConfig
import ai.dokus.foundation.ktor.crypto.AesGcmCredentialCryptoService
import ai.dokus.foundation.ktor.crypto.CredentialCryptoService
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService4j
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.lookup.CbeApiClient
import ai.dokus.foundation.ktor.security.InMemoryTokenBlacklistService
import ai.dokus.foundation.ktor.security.JwtGenerator
import ai.dokus.foundation.ktor.security.JwtValidator
import ai.dokus.foundation.ktor.security.RedisTokenBlacklistService
import ai.dokus.foundation.ktor.security.TokenBlacklistService
import ai.dokus.foundation.ktor.storage.AvatarStorageService
import ai.dokus.foundation.ktor.storage.DocumentStorageService
import ai.dokus.foundation.ktor.storage.MinioStorage
import ai.dokus.foundation.ktor.storage.ObjectStorage
import ai.dokus.peppol.config.PeppolModuleConfig
import ai.dokus.peppol.mapper.PeppolMapper
import ai.dokus.peppol.provider.PeppolProviderFactory
import ai.dokus.peppol.providers.recommand.RecommandCompaniesClient
import ai.dokus.peppol.service.PeppolConnectionService
import ai.dokus.peppol.service.PeppolService
import ai.dokus.peppol.validator.PeppolValidator
import ai.dokus.processor.backend.extraction.ExtractionProviderFactory
import ai.dokus.processor.backend.worker.DocumentProcessingWorker
import ai.dokus.processor.backend.worker.WorkerConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Koin setup for the modular monolith server.
 */
fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val logger = LoggerFactory.getLogger("ServerDI")

    install(Koin) {
        modules(
            // Core config
            module { single { appConfig } },

            // Shared infrastructure
            databaseModule,
            httpClientModule,
            storageModule,

            // Cross-cutting crypto
            cryptoModule,

            // Repositories (shared, monolith)
            repositoryModules,

            // Feature services
            authModule,
            cashflowModule(appConfig),
            contactsModule,
            processorModule(appConfig),
        )
    }

    logger.info("Koin modules installed")
}

private val databaseModule = module {
    single {
        DatabaseFactory(get(), "server-pool").apply {
            runBlocking {
                connect()
                DokusSchema.initialize()
            }
        }
    }
}

private val httpClientModule = module {
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
                requestTimeout = 120_000 // 2 minutes for AI / external calls
            }
        }
    }
}

private val storageModule = module {
    single<ObjectStorage> {
        val appConfig = get<AppBaseConfig>()
        val minioConfig = MinioConfig.loadOrNull(appConfig)
        requireNotNull(minioConfig) { "MinIO config missing. Ensure 'minio { ... }' exists in application config." }
        val publicUrl = appConfig.storage.publicUrl
        MinioStorage.create(minioConfig, publicUrl)
    }

    single { DocumentStorageService(get<ObjectStorage>()) }
    single { AvatarStorageService(get<ObjectStorage>()) }
}

private val cryptoModule = module {
    single<PasswordCryptoService> { PasswordCryptoService4j() }

    single<CredentialCryptoService> {
        val appConfig = get<AppBaseConfig>()
        val encryptionKey = System.getenv("ENCRYPTION_KEY") ?: appConfig.jwt.secret
        AesGcmCredentialCryptoService(encryptionKey)
    }

    single { JwtGenerator(get<AppBaseConfig>().jwt) }
    single { JwtValidator(get<AppBaseConfig>().jwt) }
}

private val authModule = module {
    single<EmailService> {
        val appConfig = get<AppBaseConfig>()
        val emailConfig = EmailConfig.load(appConfig)
        if (emailConfig.enabled && emailConfig.provider == "smtp") {
            SmtpEmailService(emailConfig)
        } else {
            DisabledEmailService()
        }
    }

    single { EmailVerificationService(get<UserRepository>(), get<EmailService>()) }

    single {
        PasswordResetService(
            get<UserRepository>(),
            get<PasswordResetTokenRepository>(),
            get<RefreshTokenRepository>(),
            get<EmailService>(),
            getOrNull<TokenBlacklistService>()
        )
    }

    // Redis is optional in monolith: use it only when CACHE_TYPE=redis
    single {
        val appConfig = get<AppBaseConfig>()
        if (appConfig.caching.type.lowercase() == "redis") {
            redis {
                config = appConfig.caching.redis
                namespace = RedisNamespace.Auth
            }
        } else {
            null
        }
    }

    single<RateLimitServiceInterface> {
        val redisClient = get<Any?>() as? ai.dokus.foundation.ktor.cache.RedisClient
        if (redisClient != null) RedisRateLimitService(redisClient) else RateLimitService()
    }

    single<TokenBlacklistService> {
        val redisClient = get<Any?>() as? ai.dokus.foundation.ktor.cache.RedisClient
        if (redisClient != null) RedisTokenBlacklistService(redisClient) else InMemoryTokenBlacklistService()
    }

    single { RateLimitCleanupJob(get<RateLimitServiceInterface>()) }

    single {
        val appConfig = get<AppBaseConfig>()
        AuthService(
            userRepository = get(),
            jwtGenerator = get(),
            refreshTokenRepository = get(),
            rateLimitService = get(),
            emailVerificationService = get(),
            passwordResetService = get(),
            tokenBlacklistService = get(),
            maxConcurrentSessions = appConfig.auth.maxConcurrentSessions
        )
    }

    single { TeamService(get(), get(), get()) }

    single {
        val appConfig = get<AppBaseConfig>()
        val cbeApiSecret = if (appConfig.config.hasPath("cbe.apiSecret")) appConfig.config.getString("cbe.apiSecret") else ""
        CbeApiClient(get(), cbeApiSecret)
    }
}

private fun cashflowModule(appConfig: AppBaseConfig) = module {
    single { InvoiceService(get()) }
    single { ExpenseService(get()) }
    single { BillService(get()) }
    single { CashflowOverviewService(get(), get(), get()) }

    // Legacy local-file validation helper still used by routes
    single { LocalDocumentStorageService(storageBasePath = "./storage/documents", maxFileSizeMb = 10) }

    // Peppol
    single { PeppolModuleConfig.fromConfig(appConfig.config) }
    single { PeppolProviderFactory(get(), get()) }
    single { RecommandCompaniesClient(get(), get()) }
    single { PeppolMapper() }
    single { PeppolValidator() }
    single { PeppolConnectionService(get(), get()) }
    single { PeppolService(get(), get(), get(), get(), get()) }

    // Chat / RAG
    single<AIConfig> {
        AIConfig.fromConfigOrNull(appConfig.config) ?: AIConfig.localDefault()
    }
    single { ChatRepository() }
    single { DocumentChunksRepository() }
}

private val contactsModule = module {
    single { ContactService(get()) }
    single { ContactNoteService(get()) }
    single { ContactMatchingService(get()) }
}

private fun processorModule(appConfig: AppBaseConfig) = module {
    // Worker config from environment with sensible defaults
    single {
        WorkerConfig(
            pollingInterval = appConfig.config.getConfig("processor").getLong("pollingInterval"),
            batchSize = appConfig.config.getConfig("processor").getInt("batchSize"),
            maxAttempts = appConfig.config.getConfig("processor").getInt("maxAttempts"),
        )
    }

    single {
        val processorConfig = appConfig.config.getConfig("processor")
        val aiConfig = processorConfig.getConfig("ai")

        ai.dokus.processor.backend.extraction.AIConfig(
            defaultProvider = aiConfig.getString("defaultProvider"),
            openaiApiKey = aiConfig.getString("openai.apiKey"),
            openaiModel = aiConfig.getString("openai.model"),
            openaiBaseUrl = aiConfig.getString("openai.baseUrl"),
            anthropicApiKey = aiConfig.getString("anthropic.apiKey"),
            anthropicModel = aiConfig.getString("anthropic.model"),
            anthropicBaseUrl = aiConfig.getString("anthropic.baseUrl"),
            localBaseUrl = aiConfig.getString("local.baseUrl"),
            localModel = aiConfig.getString("local.model"),
        )
    }

    single { ExtractionProviderFactory(get<HttpClient>(), get()) }

    single {
        DocumentProcessingWorker(
            processingRepository = get(),
            documentStorage = get<DocumentStorageService>(),
            providerFactory = get(),
            config = get(),
            // RAG chunking/embedding is intentionally disabled for MVP
            chunkingService = null,
            embeddingService = null,
            chunkRepository = null,
        )
    }
}
