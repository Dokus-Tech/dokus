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
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import tech.dokus.database.repository.cashflow.DocumentCreatePayload
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.enums.EntityType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.AttachmentId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.AttachmentDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.routes.Attachments
import tech.dokus.domain.routes.Expenses
import tech.dokus.domain.routes.Invoices
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.storage.DocumentUploadValidator
import kotlin.uuid.Uuid
import tech.dokus.foundation.backend.storage.DocumentStorageService as MinioDocumentStorageService

/**
 * Attachment API Routes using Ktor Type-Safe Routing
 * Base paths:
 * - /api/v1/invoices/{id}/attachments
 * - /api/v1/expenses/{id}/attachments
 * - /api/v1/attachments
 *
 * All routes require JWT authentication and tenant context.
 */
internal fun Route.attachmentRoutes() {
    val documentRepository by inject<DocumentRepository>()
    val uploadValidator by inject<DocumentUploadValidator>()
    val minioStorage by inject<MinioDocumentStorageService>()
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
                ?: throw DokusException.NotFound("Invoice not found")

            // Handle multipart upload
            val (fileBytes, filename, contentType) = handleMultipartUpload(
                DocumentUploadValidator.DEFAULT_MAX_FILE_SIZE_BYTES
            )

            val validationError = uploadValidator.validate(fileBytes, filename, contentType)
            if (validationError != null) {
                logger.warn("File validation failed: $validationError")
                throw DokusException.Validation.Generic(validationError)
            }

            val uploadResult = try {
                minioStorage.uploadDocument(
                    tenantId = tenantId,
                    prefix = "invoices",
                    filename = filename,
                    data = fileBytes,
                    contentType = contentType
                )
            } catch (e: Exception) {
                logger.error("Failed to upload file for invoice: $invoiceId", e)
                throw DokusException.InternalError("Failed to upload file: ${e.message}")
            }

            val documentId = documentRepository.create(
                tenantId = tenantId,
                payload = DocumentCreatePayload(
                    filename = uploadResult.filename,
                    contentType = uploadResult.contentType,
                    sizeBytes = uploadResult.sizeBytes,
                    storageKey = uploadResult.key,
                    contentHash = null
                )
            )

            // Link document to invoice by updating invoice's documentId
            // Note: This replaces any existing attachment. Multiple attachments per invoice
            // would require a join table approach in the future.
            invoiceRepository.updateDocumentId(invoiceId, tenantId, documentId)
                .onFailure {
                    logger.error("Failed to link document to invoice: $invoiceId", it)
                    throw DokusException.InternalError("Failed to link document to invoice")
                }

            val attachmentId = AttachmentId.parse(documentId.toString())
            logger.info("Invoice document uploaded: $attachmentId for invoice: $invoiceId")

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

            // Get attachment from invoice's documentId
            val attachments = invoice.documentId?.let { docId ->
                val document = documentRepository.getById(tenantId, docId)
                document?.let { listOf(it.toAttachmentDto()) } ?: emptyList()
            } ?: emptyList()

            logger.info("Retrieved ${attachments.size} attachments for invoice: $invoiceId")

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
                ?: throw DokusException.NotFound("Expense not found")

            // Handle multipart upload
            val (fileBytes, filename, contentType) = handleMultipartUpload(
                DocumentUploadValidator.DEFAULT_MAX_FILE_SIZE_BYTES
            )

            val validationError = uploadValidator.validate(fileBytes, filename, contentType)
            if (validationError != null) {
                logger.warn("File validation failed: $validationError")
                throw DokusException.Validation.Generic(validationError)
            }

            val uploadResult = try {
                minioStorage.uploadDocument(
                    tenantId = tenantId,
                    prefix = "expenses",
                    filename = filename,
                    data = fileBytes,
                    contentType = contentType
                )
            } catch (e: Exception) {
                logger.error("Failed to upload file for expense: $expenseId", e)
                throw DokusException.InternalError("Failed to upload file: ${e.message}")
            }

            val documentId = documentRepository.create(
                tenantId = tenantId,
                payload = DocumentCreatePayload(
                    filename = uploadResult.filename,
                    contentType = uploadResult.contentType,
                    sizeBytes = uploadResult.sizeBytes,
                    storageKey = uploadResult.key,
                    contentHash = null
                )
            )

            // Link document to expense by updating expense's documentId
            // Note: This replaces any existing attachment. Multiple attachments per expense
            // would require a join table approach in the future.
            expenseRepository.updateDocumentId(expenseId, tenantId, documentId)
                .onFailure {
                    logger.error("Failed to link document to expense: $expenseId", it)
                    throw DokusException.InternalError("Failed to link document to expense")
                }

            val attachmentId = AttachmentId.parse(documentId.toString())
            logger.info("Expense receipt uploaded: $attachmentId for expense: $expenseId")

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
                ?: throw DokusException.NotFound("Expense not found")

            // Get attachment from expense's documentId
            val attachments = expense.documentId?.let { docId ->
                val document = documentRepository.getById(tenantId, docId)
                document?.let { listOf(it.toAttachmentDto()) } ?: emptyList()
            } ?: emptyList()

            logger.info("Retrieved ${attachments.size} attachments for expense: $expenseId")

            call.respond(HttpStatusCode.OK, attachments)
        }

        // ================================================================
        // Attachment Operations
        // ================================================================

        // GET /api/v1/attachments/{id}/url - Get attachment download URL
        get<Attachments.Id.Url> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val attachmentId = AttachmentId(Uuid.parse(route.parent.id))
            val documentId = DocumentId.parse(route.parent.id)

            logger.info("Getting download URL for attachment: $attachmentId")

            val document = documentRepository.getById(tenantId, documentId)
                ?: throw DokusException.NotFound("Attachment not found")

            val downloadUrl = minioStorage.getDownloadUrl(document.storageKey)

            call.respond(HttpStatusCode.OK, DownloadUrlResponse(downloadUrl))
        }

        // DELETE /api/v1/attachments/{id} - Delete attachment
        delete<Attachments.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val attachmentId = AttachmentId(Uuid.parse(route.id))
            val documentId = DocumentId.parse(route.id)

            logger.info("Deleting attachment: $attachmentId")

            val document = documentRepository.getById(tenantId, documentId)
                ?: throw DokusException.NotFound("Attachment not found")

            try {
                minioStorage.deleteDocument(document.storageKey)
            } catch (e: Exception) {
                logger.warn("Failed to delete document from MinIO: ${e.message}")
            }

            val deleted = documentRepository.delete(tenantId, documentId)
            if (!deleted) {
                throw DokusException.InternalError("Failed to delete document from database")
            }

            logger.info("Attachment deleted: $attachmentId")

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun DocumentDto.toAttachmentDto(): AttachmentDto {
    return AttachmentDto(
        id = AttachmentId.parse(id.toString()),
        tenantId = tenantId,
        entityType = EntityType.Attachment,
        entityId = "",
        filename = filename,
        mimeType = contentType,
        sizeBytes = sizeBytes,
        s3Key = storageKey,
        s3Bucket = "minio",
        uploadedAt = uploadedAt
    )
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

// Response DTOs
@Serializable
private data class UploadAttachmentResponse(val attachmentId: AttachmentId)

@Serializable
private data class DownloadUrlResponse(val downloadUrl: String)
