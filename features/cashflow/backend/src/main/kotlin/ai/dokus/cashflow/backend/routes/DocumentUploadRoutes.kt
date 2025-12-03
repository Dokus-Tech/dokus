package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.repository.DocumentProcessingRepository
import ai.dokus.cashflow.backend.repository.DocumentRepository
import ai.dokus.cashflow.backend.service.DocumentStorageService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.DocumentId
import ai.dokus.foundation.domain.ids.DocumentProcessingId
import ai.dokus.foundation.domain.model.DocumentDto
import ai.dokus.foundation.domain.model.DocumentProcessingDto
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import ai.dokus.foundation.ktor.storage.DocumentStorageService as MinioDocumentStorageService
import ai.dokus.foundation.messaging.messages.DocumentProcessingRequestedMessage
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream

/** Maximum file size in bytes (10 MB) */
private const val MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024

/** Allowed prefixes for document storage */
private val ALLOWED_PREFIXES = setOf("documents", "invoices", "bills", "expenses", "receipts")

/**
 * Document upload and retrieval routes.
 * Documents are stored in MinIO and metadata is persisted in DocumentsTable.
 *
 * Endpoints:
 * - POST /api/v1/documents/upload - Upload a document, returns DocumentDto with id
 * - GET /api/v1/documents/{id} - Get document by id with fresh presigned download URL
 *
 * Base path: /api/v1/documents
 */
/**
 * Response for document upload including processing info.
 */
@Serializable
data class DocumentUploadResponse(
    val document: DocumentDto,
    val processingId: DocumentProcessingId,
    val processingStatus: String
)

fun Route.documentUploadRoutes() {
    val minioStorage by inject<MinioDocumentStorageService>()
    val documentRepository by inject<DocumentRepository>()
    val processingRepository by inject<DocumentProcessingRepository>()
    val localStorageService by inject<DocumentStorageService>()
    val logger = LoggerFactory.getLogger("DocumentUploadRoutes")

    route("/api/v1/documents") {
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
            post("/upload") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

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
                            fileBytes = withContext(Dispatchers.IO) {
                                val outputStream = ByteArrayOutputStream()
                                part.provider().copyTo(outputStream)
                                val bytes = outputStream.toByteArray()

                                // Check file size limit to prevent DoS
                                if (bytes.size > MAX_FILE_SIZE_BYTES) {
                                    throw DokusException.BadRequest(
                                        "File size exceeds maximum limit of ${MAX_FILE_SIZE_BYTES / 1024 / 1024}MB"
                                    )
                                }
                                bytes
                            }
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

                // Validate file using existing validation logic
                val validationError = localStorageService.validateFile(
                    fileBytes!!,
                    filename!!,
                    contentType!!
                )
                if (validationError != null) {
                    logger.warn("File validation failed: $validationError")
                    throw DokusException.BadRequest(validationError)
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

                // Create processing record for AI extraction
                val processing = processingRepository.create(
                    documentId = documentId,
                    tenantId = tenantId
                )

                logger.info("Processing record created: id=${processing.id}, documentId=$documentId")

                // Fetch the created document to return full DTO
                val document = documentRepository.getById(tenantId, documentId)
                    ?: throw DokusException.InternalError("Failed to retrieve created document")

                // TODO: Publish to RabbitMQ for immediate processing
                // val message = DocumentProcessingRequestedMessage.create(
                //     documentId = documentId,
                //     processingId = processing.id,
                //     tenantId = tenantId,
                //     storageKey = result.key,
                //     filename = result.filename,
                //     mimeType = result.contentType,
                //     sizeBytes = result.sizeBytes
                // )
                // messagePublisher.publish(message)

                // Return with document and processing info
                call.respond(
                    HttpStatusCode.Created,
                    DocumentUploadResponse(
                        document = document.copy(downloadUrl = result.url),
                        processingId = processing.id,
                        processingStatus = processing.status.name
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
            get("/{id}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                val documentIdStr = call.parameters["id"]
                    ?: throw DokusException.BadRequest("Document ID is required")

                val documentId = try {
                    DocumentId.parse(documentIdStr)
                } catch (e: Exception) {
                    throw DokusException.BadRequest("Invalid document ID format")
                }

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
            delete("/{id}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                val documentIdStr = call.parameters["id"]
                    ?: throw DokusException.BadRequest("Document ID is required")

                val documentId = try {
                    DocumentId.parse(documentIdStr)
                } catch (e: Exception) {
                    throw DokusException.BadRequest("Invalid document ID format")
                }

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
}
