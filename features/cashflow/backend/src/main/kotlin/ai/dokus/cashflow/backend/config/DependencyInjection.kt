package ai.dokus.cashflow.backend.config

import ai.dokus.cashflow.backend.database.tables.AttachmentsTable
import ai.dokus.cashflow.backend.database.tables.BillsTable
import ai.dokus.cashflow.backend.database.tables.ExpensesTable
import ai.dokus.cashflow.backend.database.tables.InvoiceItemsTable
import ai.dokus.cashflow.backend.database.tables.InvoicesTable
import ai.dokus.cashflow.backend.repository.AttachmentRepository
import ai.dokus.cashflow.backend.repository.BillRepository
import ai.dokus.cashflow.backend.repository.CashflowRepository
import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.cashflow.backend.service.BillService
import ai.dokus.cashflow.backend.service.CashflowOverviewService
import ai.dokus.cashflow.backend.service.DocumentStorageService
import ai.dokus.cashflow.backend.service.ExpenseService
import ai.dokus.cashflow.backend.service.FromMediaService
import ai.dokus.cashflow.backend.service.IMediaService
import ai.dokus.cashflow.backend.service.InvoiceService
import ai.dokus.cashflow.backend.service.MediaService
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.config.MinioConfig
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.security.JwtValidator
import ai.dokus.foundation.ktor.storage.MinioStorage
import ai.dokus.foundation.ktor.storage.ObjectStorage
import ai.dokus.foundation.ktor.storage.DocumentStorageService as MinioDocumentStorageService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    install(Koin) {
        modules(
            coreModule(appConfig),
            databaseModule,
            repositoryModule,
            storageModule(appConfig),
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
        DatabaseFactory(get(), "cashflow-pool").apply {
            runBlocking {
                init(
                    InvoicesTable,
                    InvoiceItemsTable,
                    ExpensesTable,
                    AttachmentsTable,
                    BillsTable
                )
            }
        }
    }
}

/**
 * Repository module - data access layer
 */
val repositoryModule = module {
    single { AttachmentRepository() }
    single { InvoiceRepository() }
    single { ExpenseRepository() }
    single { BillRepository() }
    single { CashflowRepository(get(), get()) }
}

/**
 * Storage module - object storage configuration
 */
fun storageModule(appConfig: AppBaseConfig) = module {
    val logger = LoggerFactory.getLogger("StorageModule")

    // MinIO Object Storage (when available)
    single<ObjectStorage> {
        val minioConfig = MinioConfig.loadOrNull(appConfig)
        if (minioConfig != null) {
            logger.info("MinIO storage configured: endpoint=${minioConfig.endpoint}, bucket=${minioConfig.bucket}")
            MinioStorage.create(minioConfig)
        } else {
            logger.warn("MinIO not configured, using NoOpStorage. Set MINIO_ENDPOINT to enable.")
            NoOpStorage
        }
    }

    // MinIO Document Storage Service (high-level API)
    single<MinioDocumentStorageService> {
        val objectStorage = get<ObjectStorage>()
        MinioDocumentStorageService(objectStorage)
    }
}

/**
 * No-op storage implementation for when MinIO is not configured.
 * All operations throw an error indicating storage is not configured.
 */
private object NoOpStorage : ObjectStorage {
    override suspend fun put(key: String, data: ByteArray, contentType: String): String {
        throw UnsupportedOperationException("Object storage not configured. Set MINIO_ENDPOINT to enable.")
    }

    override suspend fun get(key: String): ByteArray {
        throw UnsupportedOperationException("Object storage not configured. Set MINIO_ENDPOINT to enable.")
    }

    override suspend fun delete(key: String) {
        throw UnsupportedOperationException("Object storage not configured. Set MINIO_ENDPOINT to enable.")
    }

    override suspend fun exists(key: String): Boolean = false

    override suspend fun getSignedUrl(key: String, expiry: kotlin.time.Duration): String {
        throw UnsupportedOperationException("Object storage not configured. Set MINIO_ENDPOINT to enable.")
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

    // Media service for inter-service communication
    // Uses MEDIA_SERVICE_URL environment variable or default for local dev
    // Bound to interface for testability and dependency inversion
    single<IMediaService> {
        val mediaServiceBaseUrl = System.getenv("MEDIA_SERVICE_URL") ?: "http://localhost:8002"
        MediaService(
            httpClient = get(),
            mediaServiceBaseUrl = mediaServiceBaseUrl
        )
    }

    // Business logic services
    single { InvoiceService(get()) }
    single { ExpenseService(get()) }
    single { BillService(get()) }
    single { CashflowOverviewService(get(), get(), get()) }
    single { FromMediaService(get(), get(), get(), get()) }
}
