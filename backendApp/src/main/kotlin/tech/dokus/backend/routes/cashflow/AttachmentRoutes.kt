package tech.dokus.backend.routes.cashflow

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import tech.dokus.backend.security.requireTenantId
import tech.dokus.backend.services.documents.AttachmentService
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.AttachmentId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.DownloadUrlResponse
import tech.dokus.domain.model.UploadAttachmentResponse
import tech.dokus.domain.routes.Attachments
import tech.dokus.domain.routes.Expenses
import tech.dokus.domain.routes.Invoices
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.storage.DocumentUploadValidator
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
internal fun Route.attachmentRoutes() {
    val attachmentService by inject<AttachmentService>()
    val logger = LoggerFactory.getLogger("AttachmentRoutes")

    authenticateJwt {
        // ================================================================
        // Invoice Attachments
        // ================================================================

        // POST /api/v1/invoices/{id}/attachments - Upload invoice document
        post<Invoices.Id.Attachments> { route ->
            val tenantId = requireTenantId()
            val invoiceId = InvoiceId(Uuid.parse(route.parent.id))

            logger.info("Uploading invoice document for: $invoiceId")

            val (fileBytes, filename, contentType) = handleMultipartUpload(
                DocumentUploadValidator.DEFAULT_MAX_FILE_SIZE_BYTES
            )

            val attachmentId = attachmentService.uploadInvoiceAttachment(
                tenantId = tenantId,
                invoiceId = invoiceId,
                fileBytes = fileBytes,
                filename = filename,
                contentType = contentType,
            )

            logger.info("Invoice document uploaded: $attachmentId for invoice: $invoiceId")
            call.respond(HttpStatusCode.Created, UploadAttachmentResponse(attachmentId))
        }

        // GET /api/v1/invoices/{id}/attachments - List invoice attachments
        get<Invoices.Id.Attachments> { route ->
            val tenantId = requireTenantId()
            val invoiceId = InvoiceId(Uuid.parse(route.parent.id))

            val attachments = attachmentService.listInvoiceAttachments(tenantId, invoiceId)
            call.respond(HttpStatusCode.OK, attachments)
        }

        // ================================================================
        // Expense Attachments
        // ================================================================

        // POST /api/v1/expenses/{id}/attachments - Upload expense receipt
        post<Expenses.Id.Attachments> { route ->
            val tenantId = requireTenantId()
            val expenseId = ExpenseId(Uuid.parse(route.parent.id))

            logger.info("Uploading expense receipt for: $expenseId")

            val (fileBytes, filename, contentType) = handleMultipartUpload(
                DocumentUploadValidator.DEFAULT_MAX_FILE_SIZE_BYTES
            )

            val attachmentId = attachmentService.uploadExpenseAttachment(
                tenantId = tenantId,
                expenseId = expenseId,
                fileBytes = fileBytes,
                filename = filename,
                contentType = contentType,
            )

            logger.info("Expense receipt uploaded: $attachmentId for expense: $expenseId")
            call.respond(HttpStatusCode.Created, UploadAttachmentResponse(attachmentId))
        }

        // GET /api/v1/expenses/{id}/attachments - List expense attachments
        get<Expenses.Id.Attachments> { route ->
            val tenantId = requireTenantId()
            val expenseId = ExpenseId(Uuid.parse(route.parent.id))

            val attachments = attachmentService.listExpenseAttachments(tenantId, expenseId)
            call.respond(HttpStatusCode.OK, attachments)
        }

        // ================================================================
        // Attachment Operations
        // ================================================================

        // GET /api/v1/attachments/{id}/url - Get attachment download URL
        get<Attachments.Id.Url> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            val downloadUrl = attachmentService.getAttachmentDownloadUrl(tenantId, documentId)
            call.respond(HttpStatusCode.OK, DownloadUrlResponse(downloadUrl))
        }

        // DELETE /api/v1/attachments/{id} - Delete attachment
        delete<Attachments.Id> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.id)

            logger.info("Deleting attachment: $documentId")
            attachmentService.deleteAttachment(tenantId, documentId)
            logger.info("Attachment deleted: $documentId")

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

// Helper function to extract file upload data
private suspend fun RoutingContext.handleMultipartUpload(maxFileSizeBytes: Long): Triple<ByteArray, String, String> {
    val multipart = call.receiveMultipart()
    var fileBytes: ByteArray? = null
    var filename: String? = null
    var contentType: String? = null

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                filename = part.originalFileName ?: "unknown"
                contentType = part.contentType?.toString() ?: "application/octet-stream"
                fileBytes = part.readBytesWithLimit(maxFileSizeBytes)
            }
            else -> { /* Ignore non-file parts */ }
        }
        part.dispose()
    }

    if (fileBytes == null || filename == null) {
        throw DokusException.BadRequest("No file provided in request")
    }

    return Triple(fileBytes!!, filename!!, contentType ?: "application/octet-stream")
}
