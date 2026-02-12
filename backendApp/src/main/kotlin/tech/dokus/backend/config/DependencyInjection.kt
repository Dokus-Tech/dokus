package tech.dokus.backend.config

import com.typesafe.config.Config
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
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
import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.backend.services.cashflow.CashflowOverviewService
import tech.dokus.backend.services.cashflow.CreditNoteService
import tech.dokus.backend.services.cashflow.ExpenseService
import tech.dokus.backend.services.cashflow.InvoiceService
import tech.dokus.backend.services.contacts.ContactMatchingService
import tech.dokus.backend.services.contacts.ContactNoteService
import tech.dokus.backend.services.contacts.ContactService
import tech.dokus.backend.services.documents.AutoConfirmPolicy
import tech.dokus.backend.services.documents.ContactResolutionService
import tech.dokus.backend.services.documents.StorageDocumentFetcher
import tech.dokus.backend.services.documents.confirmation.CreditNoteConfirmationService
import tech.dokus.backend.services.documents.confirmation.DocumentConfirmationDispatcher
import tech.dokus.backend.services.documents.confirmation.InvoiceConfirmationService
import tech.dokus.backend.services.documents.confirmation.ReceiptConfirmationService
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
import tech.dokus.database.repository.peppol.PeppolRegistrationRepository
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.aiModule
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.foundation.backend.cache.RedisClient
import tech.dokus.foundation.backend.cache.RedisNamespace
import tech.dokus.foundation.backend.cache.redis
import tech.dokus.foundation.backend.config.AppBaseConfig
import tech.dokus.foundation.backend.config.AuthConfig
import tech.dokus.foundation.backend.config.CachingConfig
import tech.dokus.foundation.backend.config.DatabaseConfig
import tech.dokus.foundation.backend.config.FlywayConfig
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.config.MinioConfig
import tech.dokus.foundation.backend.config.StorageConfig
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
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.mapper.PeppolMapper
import tech.dokus.peppol.provider.PeppolProviderFactory
import tech.dokus.peppol.provider.client.RecommandCompaniesClient
import tech.dokus.peppol.provider.client.RecommandProvider
import tech.dokus.peppol.service.PeppolConnectionService
import tech.dokus.peppol.service.PeppolCredentialResolver
import tech.dokus.peppol.service.PeppolCredentialResolverImpl
import tech.dokus.peppol.service.PeppolRegistrationService
import tech.dokus.peppol.service.PeppolService
import tech.dokus.peppol.service.PeppolTransferPollingService
import tech.dokus.peppol.service.PeppolVerificationService
import tech.dokus.peppol.validator.PeppolValidator

/**
 * Koin setup for the modular monolith server.
 */
fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val logger = loggerFor("ServerDI")

    install(Koin) {
        modules(
            // Core config
            configureConfigDi(appConfig),

            // Shared infrastructure
            databaseModule,
            httpClientModule,
            storageModule,

            // Cross-cutting crypto
            cryptoModule,

            // Repositories (shared, monolith)
            repositoryModules,

            // Feature services
            authModule(),
            cashflowModule(),
            contactsModule,
            documentProcessingModule(),
            aiModule()
        )
    }

    logger.info("Koin modules installed")
}

private val databaseModule = module {
    single {
        DatabaseFactory(get<DatabaseConfig>(), get<FlywayConfig>(), "server-pool").apply {
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
        val minioConfig = get<MinioConfig>()
        val publicUrl = get<StorageConfig>().publicUrl
        MinioStorage.create(minioConfig, publicUrl)
    }

    single { DocumentUploadValidator() }
    single { DocumentStorageService(get<ObjectStorage>()) }
    single { AvatarStorageService(get<ObjectStorage>()) }
}

private val cryptoModule = module {
    single<PasswordCryptoService> { PasswordCryptoService4j() }

    single<CredentialCryptoService> {
        val config = get<JwtConfig>()
        val encryptionKey = System.getenv("ENCRYPTION_KEY") ?: config.secret
        AesGcmCredentialCryptoService(encryptionKey)
    }

    singleOf(::JwtGenerator)
    singleOf(::JwtValidator)
}

private fun authModule() = module {
    single<EmailService> {
        val emailConfig = get<EmailConfig>()
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
        val cachingConfig = get<CachingConfig>()
        redis {
            config = cachingConfig.redis
            namespace = RedisNamespace.Auth
        }
    }

    single<RateLimitServiceInterface> {
        val authConfig = get<AuthConfig>()
        RedisRateLimitService(
            redisClient = get(),
            maxAttempts = authConfig.maxLoginAttempts,
            attemptWindowMinutes = (authConfig.rateLimit.windowSeconds / 60).toLong(),
            lockoutDurationMinutes = authConfig.lockDurationMinutes.toLong()
        )
    }
    singleOf(::RedisTokenBlacklistService) bind TokenBlacklistService::class
    singleOf(::RateLimitCleanupWorker)

    single {
        val authConfig = get<AuthConfig>()
        AuthService(
            userRepository = get(),
            jwtGenerator = get(),
            refreshTokenRepository = get(),
            rateLimitService = get(),
            emailVerificationService = get(),
            passwordResetService = get(),
            tokenBlacklistService = get(),
            maxConcurrentSessions = authConfig.maxConcurrentSessions
        )
    }

    single { TeamService(get(), get(), get()) }

    single {
        val config = get<Config>()
        val cbeApiSecret = if (config.hasPath("cbe.apiSecret")) config.getString("cbe.apiSecret") else ""
        CbeApiClient(get(), cbeApiSecret)
    }
}

private fun cashflowModule() = module {
    single { InvoiceService(get(), get()) }
    single { ExpenseService(get()) }
    single { CreditNoteService(get(), get(), get()) }
    single { CashflowEntriesService(get()) }
    single { CashflowOverviewService(get(), get(), get()) }
    single { InvoiceConfirmationService(get(), get(), get()) }
    single { ReceiptConfirmationService(get(), get(), get()) }
    single { CreditNoteConfirmationService(get(), get(), get(), get(), get(), get()) }
    single { DocumentConfirmationDispatcher(get(), get(), get()) }

    // PDF Preview
    single { PdfPreviewService(get<ObjectStorage>(), get<DocumentStorageService>()) }

    // Peppol
    single { PeppolModuleConfig.fromConfig(get<Config>()) }
    single { PeppolProviderFactory(get()) }
    single { RecommandCompaniesClient(get()) }
    single { RecommandProvider(get()) } // For directory lookups
    single { PeppolMapper() }
    single { PeppolValidator() }
    // Centralized credential resolver - ALL Peppol operations use this
    single<PeppolCredentialResolver> { PeppolCredentialResolverImpl(get(), get()) }
    single { PeppolConnectionService(get(), get(), get()) }
    single { PeppolService(get(), get(), get(), get(), get(), get()) }

    // PEPPOL Directory Cache - resolves recipients via cache-first lookup
    single { PeppolRecipientResolver(get(), get(), get(), get()) }

    // PEPPOL Registration State Machine (Phase B)
    single { PeppolRegistrationRepository() }
    single { PeppolVerificationService(get(), get()) }
    single { PeppolRegistrationService(get(), get(), get(), get(), get(), get(), get()) }
    single { PeppolTransferPollingService(get(), get()) }

    // Peppol Polling Worker
    singleOf(::PeppolPollingWorker)
}

private val contactsModule = module {
    // NOTE: ContactService takes optional PeppolDirectoryCacheRepository for cache invalidation
    single { ContactService(get(), getOrNull()) }
    single { ContactNoteService(get()) }
    single { ContactMatchingService(get()) }
}

private fun documentProcessingModule() = module {
    // Bridge: backendApp's DocumentFetcher implementation for the AI module
    single<DocumentFetcher> { StorageDocumentFetcher(get(), get()) }

    // Contact resolution (deterministic post-processing)
    singleOf(::ContactResolutionService)
    singleOf(::AutoConfirmPolicy)

    // Document Processing Worker
    singleOf(::DocumentProcessingWorker)
}
