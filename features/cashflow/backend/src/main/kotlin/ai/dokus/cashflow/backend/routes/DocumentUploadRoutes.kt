package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.service.DocumentStorageService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import ai.dokus.foundation.ktor.storage.DocumentStorageService as MinioDocumentStorageService
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
 * Generic document upload routes.
 * Supports uploading documents to MinIO object storage.
 *
 * Base path: /api/v1/documents
 */
fun Route.documentUploadRoutes() {
    val minioStorage by inject<MinioDocumentStorageService>()
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
             * Response: DocumentUploadResponse with URL and storage key
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
                logger.info("Uploading to MinIO: tenant=$tenantId, prefix=$prefix, filename=$filename")

                try {
                    val result = minioStorage.uploadDocument(
                        tenantId = tenantId,
                        prefix = prefix,
                        filename = filename!!,
                        data = fileBytes!!,
                        contentType = contentType!!
                    )

                    logger.info("Document uploaded to MinIO: key=${result.key}, size=${result.sizeBytes}")

                    call.respond(
                        HttpStatusCode.Created,
                        DocumentUploadResponse(
                            url = result.url,
                            storageKey = result.key,
                            filename = result.filename,
                            contentType = result.contentType,
                            sizeBytes = result.sizeBytes
                        )
                    )
                } catch (e: UnsupportedOperationException) {
                    // MinIO not configured, fall back to local storage
                    logger.info("MinIO not configured, using local storage: tenant=$tenantId, prefix=$prefix, filename=$filename")

                    val storageKey = localStorageService.storeFileLocally(
                        tenantId = tenantId,
                        entityType = prefix,
                        entityId = "uploads",
                        filename = filename!!,
                        fileContent = fileBytes!!
                    ).getOrElse {
                        logger.error("Failed to store file locally", it)
                        throw DokusException.InternalError("Failed to store file: ${it.message}")
                    }

                    val downloadUrl = localStorageService.generateDownloadUrl(storageKey)

                    call.respond(
                        HttpStatusCode.Created,
                        DocumentUploadResponse(
                            url = downloadUrl,
                            storageKey = storageKey,
                            filename = filename!!,
                            contentType = contentType!!,
                            sizeBytes = fileBytes!!.size.toLong()
                        )
                    )
                }
            }

            /**
             * GET /api/v1/documents/{key}/download-url
             * Get a presigned download URL for a document.
             *
             * Path parameters:
             * - key: The storage key (URL encoded)
             *
             * Response: DocumentDownloadUrlResponse with presigned URL
             */
            get("/{key...}/download-url") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                // Get the full key from path segments
                val key = call.parameters.getAll("key")?.joinToString("/")
                    ?: throw DokusException.BadRequest("Storage key is required")

                logger.info("Getting download URL for key (tenant=$tenantId)")

                // Validate key format and tenant ownership
                // Key format: {prefix}/{tenantId}/{uuid}_{filename}
                val keyParts = key.split("/")
                if (keyParts.size < 3) {
                    logger.warn("Tenant $tenantId attempted to access malformed key")
                    throw DokusException.NotFound("Document not found")
                }

                val keyPrefix = keyParts[0]
                val keyTenantId = keyParts[1]

                // Verify prefix is allowed
                if (keyPrefix !in ALLOWED_PREFIXES) {
                    logger.warn("Tenant $tenantId attempted to access invalid prefix: $keyPrefix")
                    throw DokusException.NotFound("Document not found")
                }

                // Verify tenant ownership - must match exactly
                if (keyTenantId != tenantId.toString()) {
                    logger.warn("Tenant $tenantId attempted to access document belonging to tenant $keyTenantId")
                    throw DokusException.NotFound("Document not found")
                }

                try {
                    val url = minioStorage.getDownloadUrl(key)
                    call.respond(HttpStatusCode.OK, DocumentDownloadUrlResponse(url))
                } catch (e: UnsupportedOperationException) {
                    val url = localStorageService.generateDownloadUrl(key)
                    call.respond(HttpStatusCode.OK, DocumentDownloadUrlResponse(url))
                }
            }
        }
    }
}

/**
 * Response for document upload
 */
@Serializable
data class DocumentUploadResponse(
    val url: String,
    val storageKey: String,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long
)

/**
 * Response for document download URL request
 */
@Serializable
data class DocumentDownloadUrlResponse(
    val downloadUrl: String
)
