package ai.dokus.cashflow.backend.rpc

import ai.dokus.cashflow.backend.repository.AttachmentRepository
import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.cashflow.backend.service.DocumentStorageService
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.*
import ai.dokus.foundation.domain.rpc.CashflowApi
import ai.dokus.foundation.domain.rpc.CashflowOverview
import ai.dokus.foundation.ktor.auth.requireAuthenticatedTenantId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory

/**
 * Implementation of CashflowApi using KotlinX RPC.
 *
 * This is a stub implementation for MVP - will be enhanced with actual business logic.
 */
class CashflowApiImpl(
    private val attachmentRepository: AttachmentRepository,
    private val documentStorageService: DocumentStorageService,
    private val invoiceRepository: InvoiceRepository,
    private val expenseRepository: ExpenseRepository
) : CashflowApi {

    private val logger = LoggerFactory.getLogger(CashflowApiImpl::class.java)

    // ============================================================================
    // INVOICE MANAGEMENT
    // ============================================================================

    override suspend fun createInvoice(request: CreateInvoiceRequest): Invoice {
        val tenantId = requireAuthenticatedTenantId()
        logger.info("createInvoice called for tenant: $tenantId")
        return invoiceRepository.createInvoice(tenantId, request)
            .onSuccess { logger.info("Invoice created with id: ${it.id}") }
            .onFailure { logger.error("Failed to create invoice", it) }
            .getOrThrow()
    }

    override suspend fun getInvoice(id: InvoiceId): Invoice {
        logger.info("getInvoice called for id: $id")
        val tenantId = requireAuthenticatedTenantId()
        return invoiceRepository.getInvoice(id, tenantId)
            .onSuccess { logger.info("Invoice retrieved: $id") }
            .onFailure { logger.error("Failed to get invoice: $id", it) }
            .getOrThrow()
            ?: throw IllegalArgumentException("Invoice not found or access denied")
    }

    override suspend fun listInvoices(
        status: InvoiceStatus?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): List<Invoice> {
        logger.info("listInvoices called")
        val tenantId = requireAuthenticatedTenantId()
        return invoiceRepository.listInvoices(tenantId, status, fromDate, toDate, limit, offset)
            .onSuccess { logger.info("Listed ${it.size} invoices for tenant: $tenantId") }
            .onFailure { logger.error("Failed to list invoices for tenant: $tenantId", it) }
            .getOrThrow()
    }

    override suspend fun listOverdueInvoices(): List<Invoice> {
        logger.info("listOverdueInvoices called")
        val tenantId = requireAuthenticatedTenantId()
        return invoiceRepository.listOverdueInvoices(tenantId)
            .onSuccess { logger.info("Found ${it.size} overdue invoices for tenant: $tenantId") }
            .onFailure { logger.error("Failed to list overdue invoices for tenant: $tenantId", it) }
            .getOrThrow()
    }

    override suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus) {
        logger.info("updateInvoiceStatus called for invoice: $invoiceId, status: $status")
        val tenantId = requireAuthenticatedTenantId()
        invoiceRepository.updateInvoiceStatus(invoiceId, tenantId, status)
            .onSuccess { logger.info("Invoice status updated: $invoiceId -> $status") }
            .onFailure { logger.error("Failed to update invoice status: $invoiceId", it) }
            .getOrThrow()
    }

    override suspend fun updateInvoice(invoiceId: InvoiceId, request: CreateInvoiceRequest): Invoice {
        val tenantId = requireAuthenticatedTenantId()
        logger.info("updateInvoice called for invoice: $invoiceId")
        return invoiceRepository.updateInvoice(invoiceId, tenantId, request)
            .onSuccess { logger.info("Invoice updated: $invoiceId") }
            .onFailure { logger.error("Failed to update invoice: $invoiceId", it) }
            .getOrThrow()
    }

    override suspend fun deleteInvoice(invoiceId: InvoiceId) {
        logger.info("deleteInvoice called for invoice: $invoiceId")
        val tenantId = requireAuthenticatedTenantId()
        invoiceRepository.deleteInvoice(invoiceId, tenantId)
            .onSuccess { logger.info("Invoice deleted: $invoiceId") }
            .onFailure { logger.error("Failed to delete invoice: $invoiceId", it) }
            .getOrThrow()
    }

    override suspend fun recordPayment(request: RecordPaymentRequest) {
        logger.info("recordPayment called for invoice: ${request.invoiceId}")
        // TODO: Implement payment recording
        throw NotImplementedError("Payment recording not yet implemented")
    }

    override suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String?,
        message: String?
    ) {
        logger.info("sendInvoiceEmail called for invoice: $invoiceId")
        // TODO: Implement email sending
        throw NotImplementedError("Email sending not yet implemented")
    }

    override suspend fun markInvoiceAsSent(invoiceId: InvoiceId) {
        logger.info("markInvoiceAsSent called for invoice: $invoiceId")
        // TODO: Implement mark as sent
        throw NotImplementedError("Mark as sent not yet implemented")
    }

    override suspend fun calculateInvoiceTotals(items: List<InvoiceItem>): InvoiceTotals {
        logger.info("calculateInvoiceTotals called with ${items.size} items")
        // TODO: Implement totals calculation
        return InvoiceTotals(
            subtotal = Money.ZERO,
            vatAmount = Money.ZERO,
            total = Money.ZERO
        )
    }

    override fun watchInvoices(tenantId: TenantId): Flow<Invoice> {
        logger.info("watchInvoices called for tenant: $tenantId")
        // TODO: Implement real-time updates
        return emptyFlow()
    }

    // ============================================================================
    // EXPENSE MANAGEMENT
    // ============================================================================

    override suspend fun createExpense(request: CreateExpenseRequest): Expense {
        val tenantId = requireAuthenticatedTenantId()
        logger.info("createExpense called for tenant: $tenantId")
        return expenseRepository.createExpense(tenantId, request)
            .onSuccess { logger.info("Expense created with id: ${it.id}") }
            .onFailure { logger.error("Failed to create expense", it) }
            .getOrThrow()
    }

    override suspend fun getExpense(id: ExpenseId): Expense {
        logger.info("getExpense called for id: $id")
        val tenantId = requireAuthenticatedTenantId()
        return expenseRepository.getExpense(id, tenantId)
            .onSuccess { logger.info("Expense retrieved: $id") }
            .onFailure { logger.error("Failed to get expense: $id", it) }
            .getOrThrow()
            ?: throw IllegalArgumentException("Expense not found or access denied")
    }

    override suspend fun listExpenses(
        category: ExpenseCategory?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): List<Expense> {
        logger.info("listExpenses called")
        val tenantId = requireAuthenticatedTenantId()
        return expenseRepository.listExpenses(tenantId, category, fromDate, toDate, limit, offset)
            .onSuccess { logger.info("Listed ${it.size} expenses for tenant: $tenantId") }
            .onFailure { logger.error("Failed to list expenses for tenant: $tenantId", it) }
            .getOrThrow()
    }

    override suspend fun updateExpense(expenseId: ExpenseId, request: CreateExpenseRequest): Expense {
        val tenantId = requireAuthenticatedTenantId()
        logger.info("updateExpense called for expense: $expenseId")
        return expenseRepository.updateExpense(expenseId, tenantId, request)
            .onSuccess { logger.info("Expense updated: $expenseId") }
            .onFailure { logger.error("Failed to update expense: $expenseId", it) }
            .getOrThrow()
    }

    override suspend fun deleteExpense(expenseId: ExpenseId) {
        logger.info("deleteExpense called for expense: $expenseId")
        val tenantId = requireAuthenticatedTenantId()
        expenseRepository.deleteExpense(expenseId, tenantId)
            .onSuccess { logger.info("Expense deleted: $expenseId") }
            .onFailure { logger.error("Failed to delete expense: $expenseId", it) }
            .getOrThrow()
    }

    override suspend fun categorizeExpense(merchant: String, description: String?): ExpenseCategory {
        logger.info("categorizeExpense called for merchant: $merchant")
        // TODO: Implement auto-categorization
        return ExpenseCategory.Other
    }

    override fun watchExpenses(tenantId: TenantId): Flow<Expense> {
        logger.info("watchExpenses called for tenant: $tenantId")
        // TODO: Implement real-time updates
        return emptyFlow()
    }

    // ============================================================================
    // DOCUMENT/ATTACHMENT MANAGEMENT
    // ============================================================================

    override suspend fun uploadInvoiceDocument(
        invoiceId: InvoiceId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): AttachmentId {
        logger.info("uploadInvoiceDocument called for invoice: $invoiceId, file: $filename (${fileContent.size} bytes)")

        val tenantId = requireAuthenticatedTenantId()

        val invoice = invoiceRepository.getInvoice(invoiceId, tenantId)
            .onFailure { logger.error("Failed to verify invoice: $invoiceId", it) }
            .getOrThrow()
            ?: throw IllegalArgumentException("Invoice not found or access denied")

        val validationError = documentStorageService.validateFile(fileContent, filename, contentType)
        if (validationError != null) {
            logger.error("File validation failed for invoice $invoiceId: $validationError")
            throw IllegalArgumentException(validationError)
        }

        val storageKey = documentStorageService.storeFileLocally(
            tenantId, "invoice", invoiceId.toString(), filename, fileContent
        )
            .onFailure { logger.error("Failed to store file for invoice: $invoiceId", it) }
            .getOrThrow()

        return attachmentRepository.uploadAttachment(
            tenantId = tenantId,
            entityType = EntityType.Invoice,
            entityId = invoiceId.toString(),
            filename = filename,
            mimeType = contentType,
            sizeBytes = fileContent.size.toLong(),
            s3Key = storageKey,
            s3Bucket = "local"
        )
            .onSuccess { logger.info("Invoice document uploaded: $it for invoice: $invoiceId") }
            .onFailure { logger.error("Failed to save attachment for invoice: $invoiceId", it) }
            .getOrThrow()
    }

    override suspend fun uploadExpenseReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): AttachmentId {
        logger.info("uploadExpenseReceipt called for expense: $expenseId, file: $filename (${fileContent.size} bytes)")

        val tenantId = requireAuthenticatedTenantId()

        val expense = expenseRepository.getExpense(expenseId, tenantId)
            .onFailure { logger.error("Failed to verify expense: $expenseId", it) }
            .getOrThrow()
            ?: throw IllegalArgumentException("Expense not found or access denied")

        val validationError = documentStorageService.validateFile(fileContent, filename, contentType)
        if (validationError != null) {
            logger.error("File validation failed for expense $expenseId: $validationError")
            throw IllegalArgumentException(validationError)
        }

        val storageKey = documentStorageService.storeFileLocally(
            tenantId, "expense", expenseId.toString(), filename, fileContent
        )
            .onFailure { logger.error("Failed to store file for expense: $expenseId", it) }
            .getOrThrow()

        return attachmentRepository.uploadAttachment(
            tenantId = tenantId,
            entityType = EntityType.Expense,
            entityId = expenseId.toString(),
            filename = filename,
            mimeType = contentType,
            sizeBytes = fileContent.size.toLong(),
            s3Key = storageKey,
            s3Bucket = "local"
        )
            .onSuccess { logger.info("Expense receipt uploaded: $it for expense: $expenseId") }
            .onFailure { logger.error("Failed to save attachment for expense: $expenseId", it) }
            .getOrThrow()
    }

    override suspend fun getInvoiceAttachments(invoiceId: InvoiceId): List<Attachment> {
        logger.info("getInvoiceAttachments called for invoice: $invoiceId")

        val tenantId = requireAuthenticatedTenantId()

        val invoice = invoiceRepository.getInvoice(invoiceId, tenantId)
            .onFailure { logger.error("Failed to verify invoice: $invoiceId", it) }
            .getOrThrow()
            ?: throw IllegalArgumentException("Invoice not found or access denied")

        return attachmentRepository.getAttachments(
            tenantId = tenantId,
            entityType = EntityType.Invoice,
            entityId = invoiceId.toString()
        )
            .onSuccess { logger.info("Retrieved ${it.size} attachments for invoice: $invoiceId") }
            .onFailure { logger.error("Failed to get attachments for invoice: $invoiceId", it) }
            .getOrThrow()
    }

    override suspend fun getExpenseAttachments(expenseId: ExpenseId): List<Attachment> {
        logger.info("getExpenseAttachments called for expense: $expenseId")

        val tenantId = requireAuthenticatedTenantId()

        val expense = expenseRepository.getExpense(expenseId, tenantId)
            .onFailure { logger.error("Failed to verify expense: $expenseId", it) }
            .getOrThrow()
            ?: throw IllegalArgumentException("Expense not found or access denied")

        return attachmentRepository.getAttachments(
            tenantId = tenantId,
            entityType = EntityType.Expense,
            entityId = expenseId.toString()
        )
            .onSuccess { logger.info("Retrieved ${it.size} attachments for expense: $expenseId") }
            .onFailure { logger.error("Failed to get attachments for expense: $expenseId", it) }
            .getOrThrow()
    }

    override suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): String {
        logger.info("getAttachmentDownloadUrl called for attachment: $attachmentId")

        val tenantId = requireAuthenticatedTenantId()

        val attachment = attachmentRepository.getAttachment(attachmentId, tenantId)
            .onFailure { logger.error("Failed to get attachment: $attachmentId", it) }
            .getOrThrow()
            ?: throw IllegalArgumentException("Attachment not found or access denied")

        // Generate download URL
        logger.info("Generated download URL for attachment: $attachmentId")
        return documentStorageService.generateDownloadUrl(attachment.s3Key)
    }

    override suspend fun deleteAttachment(attachmentId: AttachmentId) {
        logger.info("deleteAttachment called for attachment: $attachmentId")

        val tenantId = requireAuthenticatedTenantId()

        // First get the attachment to know the storage key
        val attachment = attachmentRepository.getAttachment(attachmentId, tenantId)
            .onFailure { logger.error("Failed to get attachment: $attachmentId", it) }
            .getOrThrow()
            ?: throw IllegalArgumentException("Attachment not found or access denied")

        // Delete from storage
        documentStorageService.deleteFileLocally(attachment.s3Key)
            .onFailure { logger.error("Failed to delete file for attachment: $attachmentId", it) }
            .getOrThrow()

        // Delete from database
        attachmentRepository.deleteAttachment(attachmentId, tenantId)
            .onSuccess { logger.info("Attachment deleted: $attachmentId") }
            .onFailure { logger.error("Failed to delete attachment from database: $attachmentId", it) }
            .getOrThrow()
    }

    // ============================================================================
    // STATISTICS & OVERVIEW
    // ============================================================================

    override suspend fun getCashflowOverview(
        fromDate: LocalDate,
        toDate: LocalDate
    ): CashflowOverview {
        logger.info("getCashflowOverview called")
        val tenantId = requireAuthenticatedTenantId()
        // TODO: Implement overview calculation
        return CashflowOverview(
            totalIncome = Money.ZERO,
            totalExpenses = Money.ZERO,
            netCashflow = Money.ZERO,
            pendingInvoices = Money.ZERO,
            overdueInvoices = Money.ZERO,
            invoiceCount = 0,
            expenseCount = 0
        )
    }
}
