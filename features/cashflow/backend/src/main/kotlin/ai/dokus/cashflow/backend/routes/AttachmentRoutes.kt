package ai.dokus.cashflow.backend.routes

import ai.dokus.foundation.database.repository.cashflow.AttachmentRepository
import ai.dokus.foundation.database.repository.cashflow.ExpenseRepository
import ai.dokus.foundation.database.repository.cashflow.InvoiceRepository
import ai.dokus.cashflow.backend.service.DocumentStorageService
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.AttachmentId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.routes.Attachments
import ai.dokus.foundation.domain.routes.Expenses
import ai.dokus.foundation.domain.routes.Invoices
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Attachment API Routes using Ktor Type-Safe Routing
 * Base paths:
 * - /api/v1/invoices/{id}/attachments
 * - /api/v1/expenses/{id}/attachments
 * - /api/v1/attachments
 *
 * All routes require JWT authentication and tenant context.
 */
@OptIn(ExperimentalUuidApi::class)
fun Route.attachmentRoutes() {
    val attachmentRepository by inject<AttachmentRepository>()
    val documentStorageService by inject<DocumentStorageService>()
    val invoiceRepository by inject<InvoiceRepository>()
    val expenseRepository by inject<ExpenseRepository>()
    val logger = LoggerFactory.getLogger("AttachmentRoutes")

    authenticateJwt {
        // ================================================================
        // Invoice Attachments
        // ================================================================

        // POST /api/v1/invoices/{id}/attachments - Upload invoice document
        post<Invoices.Id.Attachments> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val invoiceId = InvoiceId(Uuid.parse(route.parent.id))

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
            val (fileBytes, filename, contentType) = handleMultipartUpload()

            // Validate file
            val validationError = documentStorageService.validateFile(fileBytes, filename, contentType)
            if (validationError != null) {
                logger.error("File validation failed: $validationError")
                throw DokusException.BadRequest()
            }

            // Store file
            val storageKey = documentStorageService.storeFileLocally(
                tenantId, "invoice", invoiceId.toString(), filename, fileBytes
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
                filename = filename,
                mimeType = contentType,
                sizeBytes = fileBytes.size.toLong(),
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

        // GET /api/v1/invoices/{id}/attachments - List invoice attachments
        get<Invoices.Id.Attachments> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val invoiceId = InvoiceId(Uuid.parse(route.parent.id))

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

        // ================================================================
        // Expense Attachments
        // ================================================================

        // POST /api/v1/expenses/{id}/attachments - Upload expense receipt
        post<Expenses.Id.Attachments> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val expenseId = ExpenseId(Uuid.parse(route.parent.id))

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
            val (fileBytes, filename, contentType) = handleMultipartUpload()

            // Validate file
            val validationError = documentStorageService.validateFile(fileBytes, filename, contentType)
            if (validationError != null) {
                logger.error("File validation failed: $validationError")
                throw DokusException.BadRequest()
            }

            // Store file
            val storageKey = documentStorageService.storeFileLocally(
                tenantId, "expense", expenseId.toString(), filename, fileBytes
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
                filename = filename,
                mimeType = contentType,
                sizeBytes = fileBytes.size.toLong(),
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

        // GET /api/v1/expenses/{id}/attachments - List expense attachments
        get<Expenses.Id.Attachments> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val expenseId = ExpenseId(Uuid.parse(route.parent.id))

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

        // ================================================================
        // Attachment Operations
        // ================================================================

        // GET /api/v1/attachments/{id}/download-url - Get attachment download URL
        get<Attachments.Id.DownloadUrl> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val attachmentId = AttachmentId(Uuid.parse(route.parent.id))

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
        delete<Attachments.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val attachmentId = AttachmentId(Uuid.parse(route.id))

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

// Helper function to extract file upload data
private suspend fun RoutingContext.handleMultipartUpload(): Triple<ByteArray, String, String> {
    val multipart = call.receiveMultipart()
    var fileBytes: ByteArray? = null
    var filename: String? = null
    var contentType: String? = null

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                filename = part.originalFileName ?: "unknown"
                contentType = part.contentType?.toString() ?: "application/octet-stream"

                fileBytes = withContext(Dispatchers.IO) {
                    val outputStream = ByteArrayOutputStream()
                    part.provider().copyTo(outputStream)
                    outputStream.toByteArray()
                }
            }
            else -> { /* Ignore non-file parts */ }
        }
        part.dispose()
    }

    if (fileBytes == null || filename == null) {
        throw DokusException.BadRequest()
    }

    return Triple(fileBytes!!, filename!!, contentType!!)
}

// Response DTOs
@Serializable
private data class UploadAttachmentResponse(val attachmentId: AttachmentId)

@Serializable
private data class DownloadUrlResponse(val downloadUrl: String)
