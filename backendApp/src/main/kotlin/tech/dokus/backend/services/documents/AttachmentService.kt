package tech.dokus.backend.services.documents

import tech.dokus.backend.mappers.from
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentSourceRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.AttachmentId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.AttachmentDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.foundation.backend.storage.DocumentUploadValidator
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Service for document attachment lifecycle: upload, list, download URL, delete.
 */
class AttachmentService(
    private val truthService: DocumentTruthService,
    private val documentRepository: DocumentRepository,
    private val sourceRepository: DocumentSourceRepository,
    private val invoiceRepository: InvoiceRepository,
    private val expenseRepository: ExpenseRepository,
    private val storageService: DocumentStorageService,
    private val uploadValidator: DocumentUploadValidator,
) {
    private val logger = loggerFor()

    suspend fun uploadInvoiceAttachment(
        tenantId: TenantId,
        invoiceId: InvoiceId,
        fileBytes: ByteArray,
        filename: String,
        contentType: String,
    ): AttachmentId {
        invoiceRepository.getInvoice(invoiceId, tenantId)
            .onFailure { throw DokusException.InternalError("Failed to verify invoice: ${it.message}") }
            .getOrThrow()
            ?: throw DokusException.NotFound("Invoice not found")

        validateFile(fileBytes, filename, contentType)

        val intake = truthService.intakeBytes(
            tenantId = tenantId,
            filename = filename,
            contentType = contentType,
            prefix = "invoices",
            fileBytes = fileBytes,
            sourceChannel = DocumentSource.Upload
        )

        invoiceRepository.updateDocumentId(invoiceId, tenantId, intake.documentId)
            .onFailure { throw DokusException.InternalError("Failed to link document to invoice") }

        return AttachmentId.parse(intake.documentId.toString())
    }

    suspend fun uploadExpenseAttachment(
        tenantId: TenantId,
        expenseId: ExpenseId,
        fileBytes: ByteArray,
        filename: String,
        contentType: String,
    ): AttachmentId {
        expenseRepository.getExpense(expenseId, tenantId)
            .onFailure { throw DokusException.InternalError("Failed to verify expense: ${it.message}") }
            .getOrThrow()
            ?: throw DokusException.NotFound("Expense not found")

        validateFile(fileBytes, filename, contentType)

        val intake = truthService.intakeBytes(
            tenantId = tenantId,
            filename = filename,
            contentType = contentType,
            prefix = "expenses",
            fileBytes = fileBytes,
            sourceChannel = DocumentSource.Upload
        )

        expenseRepository.updateDocumentId(expenseId, tenantId, intake.documentId)
            .onFailure { throw DokusException.InternalError("Failed to link document to expense") }

        return AttachmentId.parse(intake.documentId.toString())
    }

    suspend fun listInvoiceAttachments(
        tenantId: TenantId,
        invoiceId: InvoiceId,
    ): List<AttachmentDto> {
        val invoice = invoiceRepository.getInvoice(invoiceId, tenantId)
            .onFailure { throw DokusException.InternalError("Failed to verify invoice: ${it.message}") }
            .getOrThrow()
            ?: throw DokusException.BadRequest()

        return resolveAttachments(tenantId, invoice.documentId)
    }

    suspend fun listExpenseAttachments(
        tenantId: TenantId,
        expenseId: ExpenseId,
    ): List<AttachmentDto> {
        val expense = expenseRepository.getExpense(expenseId, tenantId)
            .onFailure { throw DokusException.InternalError("Failed to verify expense: ${it.message}") }
            .getOrThrow()
            ?: throw DokusException.NotFound("Expense not found")

        return resolveAttachments(tenantId, expense.documentId)
    }

    suspend fun getAttachmentDownloadUrl(
        tenantId: TenantId,
        documentId: DocumentId,
    ): String {
        documentRepository.getById(tenantId, documentId)
            ?: throw DokusException.NotFound("Attachment not found")

        val preferredSource = sourceRepository.selectPreferredSource(tenantId, documentId)
            ?: throw DokusException.NotFound("No source available for attachment")

        return storageService.getDownloadUrl(preferredSource.storageKey)
    }

    suspend fun deleteAttachment(
        tenantId: TenantId,
        documentId: DocumentId,
    ) {
        documentRepository.getById(tenantId, documentId)
            ?: throw DokusException.NotFound("Attachment not found")

        val sources = sourceRepository.listByDocument(tenantId, documentId, includeDetached = true)
        for (source in sources) {
            try {
                storageService.deleteDocument(source.storageKey)
            } catch (e: Exception) {
                logger.warn("Failed to delete source blob from storage: ${e.message}")
            }
        }

        val deleted = documentRepository.delete(tenantId, documentId)
        if (!deleted) {
            throw DokusException.InternalError("Failed to delete document from database")
        }
    }

    private fun validateFile(fileBytes: ByteArray, filename: String, contentType: String) {
        val error = uploadValidator.validate(fileBytes, filename, contentType)
        if (error != null) {
            throw DokusException.Validation.Generic(error)
        }
    }

    private suspend fun resolveAttachments(
        tenantId: TenantId,
        documentId: DocumentId?,
    ): List<AttachmentDto> {
        if (documentId == null) return emptyList()
        val document = documentRepository.getById(tenantId, documentId) ?: return emptyList()
        val source = sourceRepository.selectPreferredSource(tenantId, documentId) ?: return emptyList()
        return listOf(AttachmentDto.from(document, source))
    }
}
