package tech.dokus.backend.routes.cashflow

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tech.dokus.backend.routes.cashflow.documents.findConfirmedEntity
import tech.dokus.backend.routes.cashflow.documents.toDto
import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentCreatePayload
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.routes.Documents
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.storage.DocumentUploadValidator
import java.security.MessageDigest
import tech.dokus.foundation.backend.storage.DocumentStorageService as MinioDocumentStorageService

/** Allowed prefixes for document storage */
private val ALLOWED_PREFIXES = setOf("documents", "invoices", "bills", "expenses", "receipts")

/**
 * Document upload routes using Ktor Type-Safe Routing.
 * Documents are stored in MinIO and metadata is persisted in DocumentsTable.
 *
 * Endpoints:
 * - POST /api/v1/documents/upload - Upload a document, returns DocumentRecordDto
 *
 * Note: GET/DELETE /api/v1/documents/{id} are handled in DocumentRecordRoutes.kt
 *
 * Base path: /api/v1/documents
 */
internal fun Route.documentUploadRoutes() {
    val minioStorage by inject<MinioDocumentStorageService>()
    val documentRepository by inject<DocumentRepository>()
    val ingestionRepository by inject<DocumentIngestionRunRepository>()
    val draftRepository by inject<DocumentDraftRepository>()
    val invoiceRepository by inject<InvoiceRepository>()
    val billRepository by inject<BillRepository>()
    val expenseRepository by inject<ExpenseRepository>()
    val creditNoteRepository by inject<CreditNoteRepository>()
    val uploadValidator by inject<DocumentUploadValidator>()
    val logger = LoggerFactory.getLogger("DocumentUploadRoutes")
    val context = DocumentUploadContext(
        minioStorage = minioStorage,
        documentRepository = documentRepository,
        ingestionRepository = ingestionRepository,
        draftRepository = draftRepository,
        invoiceRepository = invoiceRepository,
        billRepository = billRepository,
        expenseRepository = expenseRepository,
        creditNoteRepository = creditNoteRepository,
        logger = logger
    )

    authenticateJwt {
        /**
         * POST /api/v1/documents/upload
         * Upload a document to object storage.
         *
         * Request: multipart/form-data with:
         * - file: The document file
         * - prefix: (optional) Storage prefix, e.g., "invoices", "bills", "expenses"
         *
         * Response: DocumentDto with id and fresh download URL
         */
        post<Documents.Upload> {
            val tenantId = dokusPrincipal.requireTenantId()

            logger.info("Document upload request from tenant: $tenantId")

            val payload = call.readUploadPayload()
            validateUploadPayload(payload, uploadValidator, logger)

            val contentHash = sha256Hex(payload.fileBytes)
            val existingDocument = documentRepository.getByContentHash(tenantId, contentHash)
            if (existingDocument != null) {
                logger.info("Duplicate document detected, reusing existing record: ${existingDocument.id}")
                val record = buildExistingDocumentRecord(
                    tenantId = tenantId,
                    existingDocument = existingDocument,
                    context = context
                )
                call.respond(HttpStatusCode.OK, record)
                return@post
            }

            val record = createNewDocumentRecord(
                tenantId = tenantId,
                payload = payload,
                contentHash = contentHash,
                context = context
            )
            call.respond(HttpStatusCode.Created, record)
        }

        // NOTE: GET /api/v1/documents/{id} and DELETE /api/v1/documents/{id}
        // are handled in DocumentRecordRoutes.kt which returns the full DocumentRecordDto
    }
}

private data class UploadPayload(
    val fileBytes: ByteArray,
    val filename: String,
    val contentType: String,
    val prefix: String
)

private data class DocumentUploadContext(
    val minioStorage: MinioDocumentStorageService,
    val documentRepository: DocumentRepository,
    val ingestionRepository: DocumentIngestionRunRepository,
    val draftRepository: DocumentDraftRepository,
    val invoiceRepository: InvoiceRepository,
    val billRepository: BillRepository,
    val expenseRepository: ExpenseRepository,
    val creditNoteRepository: CreditNoteRepository,
    val logger: Logger
)

private suspend fun ApplicationCall.readUploadPayload(): UploadPayload {
    val multipart = receiveMultipart()
    var fileBytes: ByteArray? = null
    var filename: String? = null
    var contentType: String? = null
    var prefix = "documents"

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                filename = part.originalFileName ?: "unknown"
                contentType = part.contentType?.toString() ?: "application/octet-stream"
                fileBytes = part.readBytesWithLimit(DocumentUploadValidator.DEFAULT_MAX_FILE_SIZE_BYTES)
            }
            is PartData.FormItem -> {
                if (part.name == "prefix") {
                    val requestedPrefix = part.value.ifBlank { "documents" }
                    if (requestedPrefix !in ALLOWED_PREFIXES) {
                        throw DokusException.BadRequest(
                            "Invalid prefix. Allowed values: ${ALLOWED_PREFIXES.joinToString()}"
                        )
                    }
                    prefix = requestedPrefix
                }
            }
            else -> { /* Ignore */ }
        }
        part.dispose()
    }

    if (fileBytes == null || filename == null) {
        throw DokusException.BadRequest("No file provided in request")
    }

    return UploadPayload(
        fileBytes = fileBytes!!,
        filename = filename!!,
        contentType = contentType ?: "application/octet-stream",
        prefix = prefix
    )
}

private fun validateUploadPayload(
    payload: UploadPayload,
    uploadValidator: DocumentUploadValidator,
    logger: Logger
) {
    val validationError = uploadValidator.validate(
        fileContent = payload.fileBytes,
        filename = payload.filename,
        mimeType = payload.contentType,
    )
    if (validationError != null) {
        logger.warn("File validation failed: $validationError")
        throw DokusException.Validation.Generic(validationError)
    }
}

private suspend fun buildExistingDocumentRecord(
    tenantId: TenantId,
    existingDocument: DocumentDto,
    context: DocumentUploadContext
): DocumentRecordDto {
    val downloadUrl = runCatching {
        context.minioStorage.getDownloadUrl(existingDocument.storageKey)
    }.onFailure { error ->
        context.logger.warn(
            "Failed to get download URL for ${existingDocument.storageKey}: ${error.message}"
        )
    }.getOrNull()

    val draft = context.draftRepository.getByDocumentId(existingDocument.id, tenantId)
    val latestIngestion = context.ingestionRepository.getLatestForDocument(existingDocument.id, tenantId)
    val confirmedEntity = if (draft?.documentStatus == DocumentStatus.Confirmed) {
        findConfirmedEntity(
            existingDocument.id,
            draft.documentType,
            tenantId,
            context.invoiceRepository,
            context.billRepository,
            context.expenseRepository,
            context.creditNoteRepository
        )
    } else {
        null
    }

    return DocumentRecordDto(
        document = existingDocument.copy(downloadUrl = downloadUrl),
        draft = draft?.toDto(),
        latestIngestion = latestIngestion?.toDto(),
        confirmedEntity = confirmedEntity
    )
}

private suspend fun createNewDocumentRecord(
    tenantId: TenantId,
    payload: UploadPayload,
    contentHash: String,
    context: DocumentUploadContext
): DocumentRecordDto {
    context.logger.info("Uploading to MinIO: tenant=$tenantId, prefix=${payload.prefix}")

    val result = context.minioStorage.uploadDocument(
        tenantId = tenantId,
        prefix = payload.prefix,
        filename = payload.filename,
        data = payload.fileBytes,
        contentType = payload.contentType
    )

    val documentId = context.documentRepository.create(
        tenantId = tenantId,
        payload = DocumentCreatePayload(
            filename = result.filename,
            contentType = result.contentType,
            sizeBytes = result.sizeBytes,
            storageKey = result.key,
            contentHash = contentHash
        )
    )

    context.logger.info("Document created: id=$documentId, key=${result.key}, size=${result.sizeBytes}")

    val runId = context.ingestionRepository.createRun(
        documentId = documentId,
        tenantId = tenantId
    )

    context.logger.info("Ingestion run created: id=$runId, documentId=$documentId")

    val document = context.documentRepository.getById(tenantId, documentId)
        ?: throw DokusException.InternalError("Failed to retrieve created document")

    val ingestionRun = context.ingestionRepository.getById(runId, tenantId)
        ?: throw DokusException.InternalError("Failed to retrieve created ingestion run")

    return DocumentRecordDto(
        document = document.copy(downloadUrl = result.url),
        draft = null,
        latestIngestion = ingestionRun.toDto(),
        confirmedEntity = null
    )
}

private fun sha256Hex(data: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(data)
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}
