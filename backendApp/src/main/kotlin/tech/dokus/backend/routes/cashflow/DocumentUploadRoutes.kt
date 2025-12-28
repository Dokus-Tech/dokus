package tech.dokus.backend.routes.cashflow

import ai.dokus.foundation.database.repository.cashflow.DocumentIngestionRunRepository
import ai.dokus.foundation.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentUploadResponse
import tech.dokus.domain.routes.Documents
import tech.dokus.foundation.ktor.security.authenticateJwt
import tech.dokus.foundation.ktor.security.dokusPrincipal
import tech.dokus.foundation.ktor.storage.DocumentUploadValidator
import tech.dokus.foundation.ktor.storage.DocumentStorageService as MinioDocumentStorageService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

/** Allowed prefixes for document storage */
private val ALLOWED_PREFIXES = setOf("documents", "invoices", "bills", "expenses", "receipts")

/**
 * Document upload and retrieval routes using Ktor Type-Safe Routing.
 * Documents are stored in MinIO and metadata is persisted in DocumentsTable.
 *
 * Endpoints:
 * - POST /api/v1/documents/upload - Upload a document, returns DocumentDto with id
 * - GET /api/v1/documents/{id} - Get document by id with fresh presigned download URL
 * - DELETE /api/v1/documents/{id} - Delete a document
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

            // Return with document and ingestion run info
            call.respond(
                HttpStatusCode.Created,
                DocumentUploadResponse(
                    document = document.copy(downloadUrl = result.url),
                    processingId = runId,
                    processingStatus = IngestionStatus.Queued.name
                )
            )
        }

        /**
         * GET /api/v1/documents/{id}
         * Get a document by ID with a fresh presigned download URL.
         *
         * Path parameters:
         * - id: The document ID (UUID)
         *
         * Response: DocumentDto with fresh downloadUrl
         */
        get<Documents.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.id)

            logger.info("Getting document: id=$documentId, tenant=$tenantId")

            // Fetch document (with tenant isolation)
            val document = documentRepository.getById(tenantId, documentId)
                ?: throw DokusException.NotFound("Document not found")

            // Generate fresh presigned URL
            val downloadUrl = minioStorage.getDownloadUrl(document.storageKey)

            call.respond(HttpStatusCode.OK, document.copy(downloadUrl = downloadUrl))
        }

        /**
         * DELETE /api/v1/documents/{id}
         * Delete a document by ID.
         *
         * Path parameters:
         * - id: The document ID (UUID)
         */
        delete<Documents.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.id)

            logger.info("Deleting document: id=$documentId, tenant=$tenantId")

            // Fetch document first to get storage key
            val document = documentRepository.getById(tenantId, documentId)
                ?: throw DokusException.NotFound("Document not found")

            // Delete from MinIO
            try {
                minioStorage.deleteDocument(document.storageKey)
            } catch (e: Exception) {
                logger.warn("Failed to delete document from MinIO: ${e.message}")
                // Continue with DB deletion even if MinIO delete fails
            }

            // Delete from database
            val deleted = documentRepository.delete(tenantId, documentId)
            if (!deleted) {
                throw DokusException.InternalError("Failed to delete document from database")
            }

            call.respond(HttpStatusCode.NoContent)
        }
    }
}
