package tech.dokus.backend.routes.cashflow

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.routes.Documents
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.storage.DocumentUploadValidator
import tech.dokus.foundation.backend.storage.DocumentStorageService as MinioDocumentStorageService
import java.security.MessageDigest

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
    val uploadValidator by inject<DocumentUploadValidator>()
    val logger = LoggerFactory.getLogger("DocumentUploadRoutes")

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

            // Handle multipart upload
            val multipart = call.receiveMultipart()
            var fileBytes: ByteArray? = null
            var filename: String? = null
            var contentType: String? = null
            var prefix = "documents" // default prefix

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        filename = part.originalFileName ?: "unknown"
                        contentType = part.contentType?.toString() ?: "application/octet-stream"

                        // Read file content
                        fileBytes = part.readBytesWithLimit(DocumentUploadValidator.DEFAULT_MAX_FILE_SIZE_BYTES)
                    }
                    is PartData.FormItem -> {
                        if (part.name == "prefix") {
                            val requestedPrefix = part.value.ifBlank { "documents" }
                            // Validate prefix to prevent path traversal
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

            val validationError = uploadValidator.validate(
                fileContent = fileBytes!!,
                filename = filename!!,
                mimeType = contentType!!,
            )
            if (validationError != null) {
                logger.warn("File validation failed: $validationError")
                throw DokusException.Validation.Generic(validationError)
            }

            val contentHash = sha256Hex(fileBytes!!)
            val existingDocument = documentRepository.getByContentHash(tenantId, contentHash)
            if (existingDocument != null) {
                logger.info("Duplicate document detected, reusing existing record: ${existingDocument.id}")
                val downloadUrl = try {
                    minioStorage.getDownloadUrl(existingDocument.storageKey)
                } catch (e: Exception) {
                    logger.warn("Failed to get download URL for ${existingDocument.storageKey}: ${e.message}")
                    null
                }

                val draft = draftRepository.getByDocumentId(existingDocument.id, tenantId)
                val latestIngestion = ingestionRepository.getLatestForDocument(existingDocument.id, tenantId)
                val confirmedEntity = if (draft?.draftStatus == DraftStatus.Confirmed) {
                    findConfirmedEntity(
                        existingDocument.id,
                        draft.documentType,
                        tenantId,
                        invoiceRepository,
                        billRepository,
                        expenseRepository
                    )
                } else {
                    null
                }

                call.respond(
                    HttpStatusCode.OK,
                    DocumentRecordDto(
                        document = existingDocument.copy(downloadUrl = downloadUrl),
                        draft = draft?.toDto(),
                        latestIngestion = latestIngestion?.toDto(),
                        confirmedEntity = confirmedEntity
                    )
                )
                return@post
            }

            // Upload to MinIO
            logger.info("Uploading to MinIO: tenant=$tenantId, prefix=$prefix")

            val result = minioStorage.uploadDocument(
                tenantId = tenantId,
                prefix = prefix,
                filename = filename!!,
                data = fileBytes!!,
                contentType = contentType!!
            )

            // Create document record in database
            val documentId = documentRepository.create(
                tenantId = tenantId,
                filename = result.filename,
                contentType = result.contentType,
                sizeBytes = result.sizeBytes,
                storageKey = result.key,
                contentHash = contentHash
            )

            logger.info("Document created: id=$documentId, key=${result.key}, size=${result.sizeBytes}")

            // Create ingestion run for AI extraction
            val runId = ingestionRepository.createRun(
                documentId = documentId,
                tenantId = tenantId
            )

            logger.info("Ingestion run created: id=$runId, documentId=$documentId")

            // Fetch the created document to return full DTO
            val document = documentRepository.getById(tenantId, documentId)
                ?: throw DokusException.InternalError("Failed to retrieve created document")

            // Fetch the ingestion run to return full DTO
            val ingestionRun = ingestionRepository.getById(runId, tenantId)
                ?: throw DokusException.InternalError("Failed to retrieve created ingestion run")

            // Return DocumentRecordDto with draft=null (no draft yet - document is queued)
            call.respond(
                HttpStatusCode.Created,
                DocumentRecordDto(
                    document = document.copy(downloadUrl = result.url),
                    draft = null, // No draft until extraction completes
                    latestIngestion = ingestionRun.toDto(),
                    confirmedEntity = null
                )
            )
        }

        // NOTE: GET /api/v1/documents/{id} and DELETE /api/v1/documents/{id}
        // are handled in DocumentRecordRoutes.kt which returns the full DocumentRecordDto
    }
}

private fun sha256Hex(data: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(data)
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}
