package tech.dokus.backend.routes.cashflow

import ai.dokus.foundation.database.repository.cashflow.DocumentIngestionRunRepository
import ai.dokus.foundation.database.repository.cashflow.DocumentRepository
import ai.dokus.foundation.database.repository.cashflow.IngestionRunSummary
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.routes.Documents
import tech.dokus.foundation.ktor.security.authenticateJwt
import tech.dokus.foundation.ktor.security.dokusPrincipal
import tech.dokus.foundation.ktor.storage.DocumentUploadValidator
import tech.dokus.foundation.ktor.storage.DocumentStorageService as MinioDocumentStorageService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

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
                storageKey = result.key
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
                    draft = null,  // No draft until extraction completes
                    latestIngestion = ingestionRun.toDto(),
                    confirmedEntity = null
                )
            )
        }

        // NOTE: GET /api/v1/documents/{id} and DELETE /api/v1/documents/{id}
        // are handled in DocumentRecordRoutes.kt which returns the full DocumentRecordDto
    }
}

/**
 * Convert IngestionRunSummary to DTO for API response.
 */
private fun IngestionRunSummary.toDto(): DocumentIngestionDto = DocumentIngestionDto(
    id = id,
    documentId = documentId,
    tenantId = tenantId,
    status = status,
    provider = provider,
    queuedAt = queuedAt,
    startedAt = startedAt,
    finishedAt = finishedAt,
    errorMessage = errorMessage,
    confidence = confidence
)
