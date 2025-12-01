package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.repository.AttachmentRepository
import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.cashflow.backend.service.DocumentStorageService
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.AttachmentId
import ai.dokus.foundation.domain.model.AttachmentDto
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import io.ktor.utils.io.jvm.javaio.copyTo

/**
 * Attachment API Routes
 * Base path: /api/v1/invoices/{invoiceId}/attachments and /api/v1/expenses/{expenseId}/attachments
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.attachmentRoutes() {
    val attachmentRepository by inject<AttachmentRepository>()
    val documentStorageService by inject<DocumentStorageService>()
    val invoiceRepository by inject<InvoiceRepository>()
    val expenseRepository by inject<ExpenseRepository>()
    val logger = LoggerFactory.getLogger("AttachmentRoutes")

    // Invoice attachments
    route("/api/v1/invoices/{invoiceId}/attachments") {
        authenticateJwt {

            // POST /api/v1/invoices/{invoiceId}/attachments - Upload invoice document
            post {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val invoiceId = call.parameters.invoiceId
                    ?: throw DokusException.BadRequest()

                logger.info("Uploading invoice document for: $invoiceId")

                // Verify invoice exists and belongs to tenant
                val invoice = invoiceRepository.getInvoice(invoiceId, tenantId)
                    .onFailure {
                        logger.error("Failed to verify invoice: $invoiceId", it)
                        throw DokusException.InternalError("Failed to verify invoice: ${it.message}")
                    }
                    .getOrThrow()
                    ?: throw DokusException.BadRequest()

                // Handle multipart upload
                val multipart = call.receiveMultipart()
                var fileBytes: ByteArray? = null
                var filename: String? = null
                var contentType: String? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            filename = part.originalFileName ?: "unknown"
                            contentType = part.contentType?.toString() ?: "application/octet-stream"

                            // Read bytes from the stream
                            fileBytes = withContext(Dispatchers.IO) {
                                val outputStream = ByteArrayOutputStream()
                                part.provider().copyTo(outputStream)
                                outputStream.toByteArray()
                            }
                        }
                        else -> {
                            // Ignore non-file parts
                        }
                    }
                    part.dispose()
                }

                if (fileBytes == null || filename == null) {
                    throw DokusException.BadRequest()
                }

                // Validate file
                val validationError = documentStorageService.validateFile(
                    fileBytes!!,
                    filename!!,
                    contentType!!
                )
                if (validationError != null) {
                    logger.error("File validation failed: $validationError")
                    throw DokusException.BadRequest()
                }

                // Store file
                val storageKey = documentStorageService.storeFileLocally(
                    tenantId,
                    "invoice",
                    invoiceId.toString(),
                    filename!!,
                    fileBytes!!
                )
                    .onFailure {
                        logger.error("Failed to store file for invoice: $invoiceId", it)
                        throw DokusException.InternalError("Failed to store file: ${it.message}")
                    }
                    .getOrThrow()

                // Save attachment metadata
                val attachmentId = attachmentRepository.uploadAttachment(
                    tenantId = tenantId,
                    entityType = EntityType.Invoice,
                    entityId = invoiceId.toString(),
                    filename = filename!!,
                    mimeType = contentType!!,
                    sizeBytes = fileBytes!!.size.toLong(),
                    s3Key = storageKey,
                    s3Bucket = "local"
                )
                    .onSuccess { logger.info("Invoice document uploaded: $it for invoice: $invoiceId") }
                    .onFailure {
                        logger.error("Failed to save attachment for invoice: $invoiceId", it)
                        throw DokusException.InternalError("Failed to save attachment: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.Created, UploadAttachmentResponse(attachmentId))
            }

            // GET /api/v1/invoices/{invoiceId}/attachments - List invoice attachments
            get {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val invoiceId = call.parameters.invoiceId
                    ?: throw DokusException.BadRequest()

                logger.info("Listing attachments for invoice: $invoiceId")

                // Verify invoice exists and belongs to tenant
                val invoice = invoiceRepository.getInvoice(invoiceId, tenantId)
                    .onFailure {
                        logger.error("Failed to verify invoice: $invoiceId", it)
                        throw DokusException.InternalError("Failed to verify invoice: ${it.message}")
                    }
                    .getOrThrow()
                    ?: throw DokusException.BadRequest()

                val attachments = attachmentRepository.getAttachments(
                    tenantId = tenantId,
                    entityType = EntityType.Invoice,
                    entityId = invoiceId.toString()
                )
                    .onSuccess { logger.info("Retrieved ${it.size} attachments for invoice: $invoiceId") }
                    .onFailure {
                        logger.error("Failed to get attachments for invoice: $invoiceId", it)
                        throw DokusException.InternalError("Failed to get attachments: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.OK, attachments)
            }
        }
    }

    // Expense attachments
    route("/api/v1/expenses/{expenseId}/attachments") {
        authenticateJwt {

            // POST /api/v1/expenses/{expenseId}/attachments - Upload expense receipt
            post {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val expenseId = call.parameters.expenseId
                    ?: throw DokusException.BadRequest()

                logger.info("Uploading expense receipt for: $expenseId")

                // Verify expense exists and belongs to tenant
                val expense = expenseRepository.getExpense(expenseId, tenantId)
                    .onFailure {
                        logger.error("Failed to verify expense: $expenseId", it)
                        throw DokusException.InternalError("Failed to verify expense: ${it.message}")
                    }
                    .getOrThrow()
                    ?: throw DokusException.BadRequest()

                // Handle multipart upload
                val multipart = call.receiveMultipart()
                var fileBytes: ByteArray? = null
                var filename: String? = null
                var contentType: String? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            filename = part.originalFileName ?: "unknown"
                            contentType = part.contentType?.toString() ?: "application/octet-stream"

                            // Read bytes from the stream
                            fileBytes = withContext(Dispatchers.IO) {
                                val outputStream = ByteArrayOutputStream()
                                part.provider().copyTo(outputStream)
                                outputStream.toByteArray()
                            }
                        }
                        else -> {
                            // Ignore non-file parts
                        }
                    }
                    part.dispose()
                }

                if (fileBytes == null || filename == null) {
                    throw DokusException.BadRequest()
                }

                // Validate file
                val validationError = documentStorageService.validateFile(
                    fileBytes!!,
                    filename!!,
                    contentType!!
                )
                if (validationError != null) {
                    logger.error("File validation failed: $validationError")
                    throw DokusException.BadRequest()
                }

                // Store file
                val storageKey = documentStorageService.storeFileLocally(
                    tenantId,
                    "expense",
                    expenseId.toString(),
                    filename!!,
                    fileBytes!!
                )
                    .onFailure {
                        logger.error("Failed to store file for expense: $expenseId", it)
                        throw DokusException.InternalError("Failed to store file: ${it.message}")
                    }
                    .getOrThrow()

                // Save attachment metadata
                val attachmentId = attachmentRepository.uploadAttachment(
                    tenantId = tenantId,
                    entityType = EntityType.Expense,
                    entityId = expenseId.toString(),
                    filename = filename!!,
                    mimeType = contentType!!,
                    sizeBytes = fileBytes!!.size.toLong(),
                    s3Key = storageKey,
                    s3Bucket = "local"
                )
                    .onSuccess { logger.info("Expense receipt uploaded: $it for expense: $expenseId") }
                    .onFailure {
                        logger.error("Failed to save attachment for expense: $expenseId", it)
                        throw DokusException.InternalError("Failed to save attachment: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.Created, UploadAttachmentResponse(attachmentId))
            }

            // GET /api/v1/expenses/{expenseId}/attachments - List expense attachments
            get {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val expenseId = call.parameters.expenseId
                    ?: throw DokusException.BadRequest()

                logger.info("Listing attachments for expense: $expenseId")

                // Verify expense exists and belongs to tenant
                val expense = expenseRepository.getExpense(expenseId, tenantId)
                    .onFailure {
                        logger.error("Failed to verify expense: $expenseId", it)
                        throw DokusException.InternalError("Failed to verify expense: ${it.message}")
                    }
                    .getOrThrow()
                    ?: throw DokusException.BadRequest()

                val attachments = attachmentRepository.getAttachments(
                    tenantId = tenantId,
                    entityType = EntityType.Expense,
                    entityId = expenseId.toString()
                )
                    .onSuccess { logger.info("Retrieved ${it.size} attachments for expense: $expenseId") }
                    .onFailure {
                        logger.error("Failed to get attachments for expense: $expenseId", it)
                        throw DokusException.InternalError("Failed to get attachments: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.OK, attachments)
            }
        }
    }

    // Attachment operations
    route("/api/v1/attachments") {
        authenticateJwt {

            // GET /api/v1/attachments/{id}/download-url - Get attachment download URL
            get("/{id}/download-url") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val attachmentId = call.parameters.attachmentId
                    ?: throw DokusException.BadRequest()

                logger.info("Getting download URL for attachment: $attachmentId")

                val attachment = attachmentRepository.getAttachment(attachmentId, tenantId)
                    .onFailure {
                        logger.error("Failed to get attachment: $attachmentId", it)
                        throw DokusException.InternalError("Failed to get attachment: ${it.message}")
                    }
                    .getOrThrow()
                    ?: throw DokusException.BadRequest()

                val downloadUrl = documentStorageService.generateDownloadUrl(attachment.s3Key)
                logger.info("Generated download URL for attachment: $attachmentId")

                call.respond(HttpStatusCode.OK, DownloadUrlResponse(downloadUrl))
            }

            // DELETE /api/v1/attachments/{id} - Delete attachment
            delete("/{id}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val attachmentId = call.parameters.attachmentId
                    ?: throw DokusException.BadRequest()

                logger.info("Deleting attachment: $attachmentId")

                // First get the attachment to know the storage key
                val attachment = attachmentRepository.getAttachment(attachmentId, tenantId)
                    .onFailure {
                        logger.error("Failed to get attachment: $attachmentId", it)
                        throw DokusException.InternalError("Failed to get attachment: ${it.message}")
                    }
                    .getOrThrow()
                    ?: throw DokusException.BadRequest()

                // Delete from storage
                documentStorageService.deleteFileLocally(attachment.s3Key)
                    .onFailure {
                        logger.error("Failed to delete file for attachment: $attachmentId", it)
                        throw DokusException.InternalError("Failed to delete file: ${it.message}")
                    }
                    .getOrThrow()

                // Delete from database
                attachmentRepository.deleteAttachment(attachmentId, tenantId)
                    .onSuccess { logger.info("Attachment deleted: $attachmentId") }
                    .onFailure {
                        logger.error("Failed to delete attachment from database: $attachmentId", it)
                        throw DokusException.InternalError("Failed to delete attachment: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

// Response DTOs
@kotlinx.serialization.Serializable
private data class UploadAttachmentResponse(val attachmentId: AttachmentId)

@kotlinx.serialization.Serializable
private data class DownloadUrlResponse(val downloadUrl: String)
