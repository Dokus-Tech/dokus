package tech.dokus.backend.config

import ai.koog.prompt.executor.model.PromptExecutor
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
import tech.dokus.backend.services.documents.ContactLinkingService
import tech.dokus.backend.services.pdf.PdfPreviewService
import tech.dokus.backend.services.peppol.PeppolRecipientResolver
import tech.dokus.backend.worker.DocumentProcessingWorker
import tech.dokus.backend.worker.PeppolPollingWorker
import tech.dokus.backend.worker.RateLimitCleanupWorker
import tech.dokus.database.DokusSchema
import tech.dokus.database.di.repositoryModules
import tech.dokus.database.repository.ai.DocumentExamplesRepository
import tech.dokus.database.repository.auth.PasswordResetTokenRepository
import tech.dokus.database.repository.auth.RefreshTokenRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.repository.peppol.PeppolRegistrationRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.Name
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.ContactEvidence
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ExampleRepository
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.config.AIModels
import tech.dokus.features.ai.config.AIProviderFactory
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.toDomainType
import tech.dokus.features.ai.models.toExtractedDocumentData
import tech.dokus.features.ai.orchestrator.DocumentOrchestrator
import tech.dokus.features.ai.orchestrator.tools.CreateContactTool
import tech.dokus.features.ai.orchestrator.tools.GetDocumentImagesTool
import tech.dokus.features.ai.orchestrator.tools.LookupContactTool
import tech.dokus.features.ai.services.ChunkingService
import tech.dokus.features.ai.services.DocumentImageService
import tech.dokus.features.ai.services.EmbeddingService
import tech.dokus.foundation.backend.cache.RedisClient
import tech.dokus.foundation.backend.cache.RedisNamespace
import tech.dokus.foundation.backend.cache.redis
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.AppBaseConfig
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
import tech.dokus.peppol.service.PeppolRegistrationService
import tech.dokus.peppol.service.PeppolService
import tech.dokus.peppol.service.PeppolTransferPollingService
import tech.dokus.peppol.service.PeppolVerificationService
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
    single { CashflowOverviewService(get(), get(), get(), get()) }
    single { DocumentConfirmationService(get(), get(), get(), get()) }

    // PDF Preview
    single { PdfPreviewService(get<ObjectStorage>(), get<DocumentStorageService>()) }

    // Peppol
    single { PeppolModuleConfig.fromConfig(appConfig.config) }
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
    single<DocumentConfirmationPolicy> { DefaultDocumentConfirmationPolicy() }

    // PEPPOL Registration State Machine (Phase B)
    single { PeppolRegistrationRepository() }
    single { PeppolVerificationService(get(), get()) }
    single { PeppolRegistrationService(get(), get(), get(), get(), get(), get(), get()) }
    single { PeppolTransferPollingService(get(), get()) }

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
    // =========================================================================
    // Document Processing Services
    // =========================================================================

    // Document Image Service (converts PDFs/images to PNG for vision processing)
    single { DocumentImageService() }

    // RAG services
    single { ChunkingService() }
    single { EmbeddingService(get(), get<AIConfig>()) }

    // Example repository for few-shot learning
    single<ExampleRepository> { DocumentExamplesRepository() }

    // Contact linking policy applier (AI decisions)
    single { ContactLinkingService(get(), get(), appConfig.processor.linkingPolicy) }

    // =========================================================================
    // Document Orchestrator
    // =========================================================================

    // Get intelligence mode and models (single source of truth)
    val mode = appConfig.ai.mode
    val models = AIModels.forMode(mode)

    // Create shared executor for all AI operations
    single<PromptExecutor> { AIProviderFactory.createOpenAiExecutor(appConfig.ai) }

    // Document Orchestrator - tool-calling orchestrator with vision tools
    single {
        DocumentOrchestrator(
            executor = get(),
            orchestratorModel = models.orchestrator,
            visionModel = models.vision,
            mode = mode,
            exampleRepository = get(),
            documentImageService = get(),
            chunkingService = get(),
            embeddingService = get(),
            chunkRepository = get(),
            cbeApiClient = getOrNull(),
            linkingPolicy = appConfig.processor.linkingPolicy,
            indexingUpdater = { runId, status, chunksCount, errorMessage ->
                get<ProcessorIngestionRepository>().updateIndexingStatus(
                    runId = runId,
                    status = status,
                    chunksCount = chunksCount,
                    errorMessage = errorMessage
                )
            },
            documentFetcher = { documentId, tenantId ->
                runCatching {
                    val tenant = TenantId.parse(tenantId)
                    val doc = get<DocumentRepository>().getById(tenant, DocumentId.parse(documentId))
                        ?: return@runCatching null
                    val bytes = get<DocumentStorageService>().downloadDocument(doc.storageKey)
                    GetDocumentImagesTool.DocumentData(bytes = bytes, mimeType = doc.contentType)
                }.getOrNull()
            },
            peppolDataFetcher = { _ -> null },
            contactLookup = { tenantId, vatNumber ->
                val tenant = TenantId.parse(tenantId)
                val contact = get<ContactRepository>().findByVatNumber(tenant, vatNumber).getOrNull()
                    ?: return@DocumentOrchestrator null

                val address = listOfNotNull(
                    contact.addressLine1,
                    contact.city,
                    contact.postalCode,
                    contact.country
                ).joinToString(", ").ifBlank { null }

                LookupContactTool.ContactInfo(
                    id = contact.id.toString(),
                    name = contact.name.value,
                    vatNumber = contact.vatNumber?.value ?: vatNumber,
                    address = address
                )
            },
            contactCreator = { tenantId, name, vatNumber, _ ->
                val normalizedVat = vatNumber?.takeIf { it.isNotBlank() }
                    ?: return@DocumentOrchestrator CreateContactTool.CreateResult(
                        success = false,
                        contactId = null,
                        error = "VAT number required for contact creation"
                    )

                val vat = VatNumber(normalizedVat)
                if (!vat.isValid) {
                    return@DocumentOrchestrator CreateContactTool.CreateResult(
                        success = false,
                        contactId = null,
                        error = "Invalid VAT number"
                    )
                }

                val tenant = TenantId.parse(tenantId)
                val existing = get<ContactRepository>()
                    .findByVatNumber(tenant, vat.value)
                    .getOrNull()
                if (existing != null) {
                    return@DocumentOrchestrator CreateContactTool.CreateResult(
                        success = false,
                        contactId = null,
                        error = "Contact already exists"
                    )
                }

                val request = CreateContactRequest(
                    name = Name(name),
                    vatNumber = vat,
                    source = ContactSource.AI
                )
                val created = get<ContactRepository>().createContact(tenant, request)
                created.fold(
                    onSuccess = {
                        CreateContactTool.CreateResult(
                            success = true,
                            contactId = it.id.toString(),
                            error = null
                        )
                    },
                    onFailure = {
                        CreateContactTool.CreateResult(
                            success = false,
                            contactId = null,
                            error = it.message
                        )
                    }
                )
            },
            storeExtraction = { payload ->
                val ingestionRepository = get<ProcessorIngestionRepository>()
                val draftRepository = get<DocumentDraftRepository>()
                val contactLinkingService = get<ContactLinkingService>()
                val runId = payload.runId
                    ?: ingestionRepository.findProcessingRunId(payload.tenantId, payload.documentId)
                    ?: return@DocumentOrchestrator false

                val classifiedType = payload.documentType
                    ?.trim()
                    ?.uppercase()
                    ?.let { runCatching { ClassifiedDocumentType.valueOf(it) }.getOrNull() }
                val documentType = classifiedType?.toDomainType() ?: DocumentType.Unknown

                val extractedData = payload.extraction.toExtractedDocumentData(documentType)
                    ?: if (documentType == DocumentType.Unknown) {
                        ExtractedDocumentData(
                            documentType = DocumentType.Unknown,
                            rawText = payload.rawText
                        )
                    } else {
                        return@DocumentOrchestrator false
                    }

                val meetsThreshold = payload.confidence >= DocumentOrchestrator.AUTO_CONFIRM_THRESHOLD

                val stored = ingestionRepository.markAsSucceeded(
                    runId = runId,
                    tenantId = payload.tenantId,
                    documentId = payload.documentId,
                    documentType = documentType,
                    extractedData = extractedData,
                    confidence = payload.confidence,
                    rawText = payload.rawText,
                    description = payload.description,
                    keywords = payload.keywords,
                    meetsThreshold = meetsThreshold,
                    force = false
                )

                if (!stored) return@DocumentOrchestrator false

                val decisionType = payload.linkDecisionType
                val decisionContactId = payload.linkDecisionContactId?.takeIf { it.isNotBlank() }
                    ?: payload.contactId?.takeIf { it.isNotBlank() }
                val contactId = decisionContactId
                    ?.let { runCatching { ContactId.parse(it) }.getOrNull() }
                    ?: return@DocumentOrchestrator true

                val tenant = TenantId.parse(payload.tenantId)
                val document = DocumentId.parse(payload.documentId)
                val draft = draftRepository.getByDocumentId(document, tenant)
                if (draft != null &&
                    draft.linkedContactId == null &&
                    draft.counterpartyIntent != CounterpartyIntent.Pending &&
                    draft.draftVersion == 0
                ) {
                    val evidenceFromPayload = payload.linkDecisionEvidence?.let {
                        runCatching { json.decodeFromString<ContactEvidence>(it) }.getOrNull()
                    }

                    contactLinkingService.applyLinkDecision(
                        tenantId = tenant,
                        documentId = document,
                        documentType = documentType,
                        extractedData = extractedData,
                        decisionType = decisionType,
                        contactId = contactId,
                        decisionReason = payload.linkDecisionReason ?: payload.contactReason,
                        decisionConfidence = payload.linkDecisionConfidence ?: payload.contactConfidence,
                        evidence = evidenceFromPayload
                    )
                }

                true
            }
        )
    }

    // Document Processing Worker
    single {
        DocumentProcessingWorker(
            ingestionRepository = get(),
            orchestrator = get(),
            config = appConfig.processor,
            mode = mode,
            tenantRepository = get()
        )
    }
}
