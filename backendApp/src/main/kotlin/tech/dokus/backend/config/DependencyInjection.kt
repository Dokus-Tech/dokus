package tech.dokus.backend.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.runBlocking
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory
import tech.dokus.backend.services.auth.AuthService
import tech.dokus.backend.services.auth.DisabledEmailService
import tech.dokus.backend.services.auth.EmailConfig
import tech.dokus.backend.services.auth.EmailService
import tech.dokus.backend.services.auth.EmailVerificationService
import tech.dokus.backend.services.auth.PasswordResetService
import tech.dokus.backend.services.auth.RateLimitServiceInterface
import tech.dokus.backend.services.auth.RedisRateLimitService
import tech.dokus.backend.services.auth.SmtpEmailService
import tech.dokus.backend.services.auth.TeamService
import tech.dokus.backend.services.cashflow.BillService
import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.backend.services.cashflow.CashflowOverviewService
import tech.dokus.backend.services.cashflow.ExpenseService
import tech.dokus.backend.services.cashflow.InvoiceService
import tech.dokus.backend.services.contacts.ContactMatchingService
import tech.dokus.backend.services.contacts.ContactNoteService
import tech.dokus.backend.services.contacts.ContactService
import tech.dokus.backend.services.documents.DocumentConfirmationService
import tech.dokus.backend.services.pdf.PdfPreviewService
import tech.dokus.backend.services.peppol.PeppolRecipientResolver
import tech.dokus.backend.worker.DocumentProcessingWorker
import tech.dokus.backend.worker.PeppolPollingWorker
import tech.dokus.backend.worker.RateLimitCleanupWorker
import tech.dokus.database.DokusSchema
import tech.dokus.database.di.repositoryModules
import tech.dokus.database.repository.auth.PasswordResetTokenRepository
import tech.dokus.database.repository.auth.RefreshTokenRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.agents.DocumentClassificationAgent
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.features.ai.config.AIModels
import tech.dokus.features.ai.config.AIProviderFactory
import tech.dokus.features.ai.coordinator.AutonomousProcessingCoordinator
import tech.dokus.features.ai.coordinator.ProcessingConfig
import tech.dokus.features.ai.judgment.JudgmentAgent
import tech.dokus.features.ai.models.ExtractedBillData
import tech.dokus.features.ai.models.ExtractedExpenseData
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.models.ExtractedReceiptData
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.retry.FeedbackDrivenRetryAgent
import tech.dokus.features.ai.retry.RetryConfig
import tech.dokus.features.ai.service.AIService
import tech.dokus.features.ai.services.ChunkingService
import tech.dokus.features.ai.services.DocumentImageService
import tech.dokus.features.ai.services.EmbeddingService
import tech.dokus.features.ai.validation.ExtractionAuditService
import tech.dokus.foundation.backend.config.ModelPurpose
import tech.dokus.foundation.backend.cache.RedisClient
import tech.dokus.foundation.backend.cache.RedisNamespace
import tech.dokus.foundation.backend.cache.redis
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.AppBaseConfig
import tech.dokus.foundation.backend.config.DeploymentConfig
import tech.dokus.foundation.backend.config.MinioConfig
import tech.dokus.foundation.backend.crypto.AesGcmCredentialCryptoService
import tech.dokus.foundation.backend.crypto.CredentialCryptoService
import tech.dokus.foundation.backend.crypto.PasswordCryptoService
import tech.dokus.foundation.backend.crypto.PasswordCryptoService4j
import tech.dokus.foundation.backend.database.DatabaseFactory
import tech.dokus.foundation.backend.lookup.CbeApiClient
import tech.dokus.foundation.backend.security.JwtGenerator
import tech.dokus.foundation.backend.security.JwtValidator
import tech.dokus.foundation.backend.security.RedisTokenBlacklistService
import tech.dokus.foundation.backend.security.TokenBlacklistService
import tech.dokus.foundation.backend.storage.AvatarStorageService
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.foundation.backend.storage.DocumentUploadValidator
import tech.dokus.foundation.backend.storage.MinioStorage
import tech.dokus.foundation.backend.storage.ObjectStorage
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.mapper.PeppolMapper
import tech.dokus.peppol.policy.DefaultDocumentConfirmationPolicy
import tech.dokus.peppol.policy.DocumentConfirmationPolicy
import tech.dokus.peppol.provider.PeppolProviderFactory
import tech.dokus.peppol.provider.client.RecommandCompaniesClient
import tech.dokus.peppol.provider.client.RecommandProvider
import tech.dokus.peppol.service.PeppolConnectionService
import tech.dokus.peppol.service.PeppolCredentialResolver
import tech.dokus.peppol.service.PeppolCredentialResolverImpl
import tech.dokus.peppol.service.PeppolService
import tech.dokus.peppol.validator.PeppolValidator

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
            authModule(appConfig),
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
                json(json)
            }
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.ALL
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

    single { DocumentUploadValidator() }
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

private fun authModule(appConfig: AppBaseConfig) = module {
    single<EmailService> {
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

    single<RedisClient> {
        redis {
            config = appConfig.caching.redis
            namespace = RedisNamespace.Auth
        }
    }

    single<RateLimitServiceInterface> {
        RedisRateLimitService(
            redisClient = get(),
            maxAttempts = appConfig.auth.maxLoginAttempts,
            attemptWindowMinutes = (appConfig.auth.rateLimit.windowSeconds / 60).toLong(),
            lockoutDurationMinutes = appConfig.auth.lockDurationMinutes.toLong()
        )
    }
    singleOf(::RedisTokenBlacklistService) bind TokenBlacklistService::class
    singleOf(::RateLimitCleanupWorker)

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
        val cbeApiSecret =
            if (appConfig.config.hasPath("cbe.apiSecret")) appConfig.config.getString("cbe.apiSecret") else ""
        CbeApiClient(get(), cbeApiSecret)
    }
}

private fun cashflowModule(appConfig: AppBaseConfig) = module {
    single { InvoiceService(get(), get()) }
    single { ExpenseService(get()) }
    single { BillService(get(), get()) }
    single { CashflowEntriesService(get()) }
    single { CashflowOverviewService(get(), get(), get()) }
    single { DocumentConfirmationService(get(), get(), get(), get()) }

    // PDF Preview
    single { PdfPreviewService(get<ObjectStorage>(), get<DocumentStorageService>()) }

    // Deployment config (hosting mode)
    single { DeploymentConfig.fromConfig(appConfig.config) }

    // Peppol
    single { PeppolModuleConfig.fromConfig(appConfig.config) }
    single { PeppolProviderFactory(get()) }
    single { RecommandCompaniesClient(get()) }
    single { RecommandProvider(get()) } // For directory lookups
    single { PeppolMapper() }
    single { PeppolValidator() }
    // Centralized credential resolver - ALL Peppol operations use this
    single<PeppolCredentialResolver> { PeppolCredentialResolverImpl(get(), get(), get()) }
    single { PeppolConnectionService(get(), get(), get()) }
    single { PeppolService(get(), get(), get(), get(), get(), get()) }

    // PEPPOL Directory Cache - resolves recipients via cache-first lookup
    single { PeppolRecipientResolver(get(), get(), get(), get()) }
    single<DocumentConfirmationPolicy> { DefaultDocumentConfirmationPolicy() }

    // Peppol Polling Worker
    single {
        PeppolPollingWorker(
            peppolSettingsRepository = get(),
            peppolService = get(),
            documentRepository = get(),
            draftRepository = get(),
            ingestionRunRepository = get(),
            confirmationPolicy = get(),
            confirmationService = get(),
            documentStorageService = get(),
            contactRepository = get()
        )
    }

    // AI / RAG config (repositories are in repositoryModules)
    single<AIConfig> { appConfig.ai }
}

private val contactsModule = module {
    // NOTE: ContactService takes optional PeppolDirectoryCacheRepository for cache invalidation
    single { ContactService(get(), getOrNull()) }
    single { ContactNoteService(get()) }
    single { ContactMatchingService(get()) }
}

private fun processorModule(appConfig: AppBaseConfig) = module {
    // AI Service (uses Koog agents for vision-based extraction)
    single { AIService(appConfig.ai) }

    // Document Image Service (converts PDFs/images to PNG for vision processing)
    single { DocumentImageService() }

    // Optional RAG services - can be enabled when embeddings are needed
    // single { ChunkingService() }
    // single { EmbeddingService(appConfig.ai) }

    single {
        DocumentProcessingWorker(
            ingestionRepository = get(),
            documentStorage = get<DocumentStorageService>(),
            aiService = get(),
            documentImageService = get(),
            config = appConfig.processor,
            draftRepository = get(),
            contactMatchingService = get(),
            tenantRepository = get(),
            // RAG chunking/embedding - use repositories from foundation:database
            chunkingService = getOrNull<ChunkingService>(),
            embeddingService = getOrNull<EmbeddingService>(),
            chunkRepository = getOrNull<ChunkRepository>(),
        )
    }
}
