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

    override suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> {
        logger.info("createInvoice called for tenant: ${request.tenantId}")
        return invoiceRepository.createInvoice(request)
    }

    override suspend fun getInvoice(id: InvoiceId): Result<Invoice> {
        logger.info("getInvoice called for id: $id")
        val tenantId = requireAuthenticatedTenantId()
        return invoiceRepository.getInvoice(id, tenantId).mapCatching { invoice ->
            invoice ?: throw IllegalArgumentException("Invoice not found or access denied")
        }
    }

    override suspend fun listInvoices(
        tenantId: TenantId,
        status: InvoiceStatus?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): Result<List<Invoice>> {
        logger.info("listInvoices called for tenant: $tenantId")
        return invoiceRepository.listInvoices(tenantId, status, fromDate, toDate, limit, offset)
    }

    override suspend fun listOverdueInvoices(tenantId: TenantId): Result<List<Invoice>> {
        logger.info("listOverdueInvoices called for tenant: $tenantId")
        return invoiceRepository.listOverdueInvoices(tenantId)
    }

    override suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus): Result<Unit> {
        logger.info("updateInvoiceStatus called for invoice: $invoiceId, status: $status")
        val tenantId = requireAuthenticatedTenantId()
        return invoiceRepository.updateInvoiceStatus(invoiceId, tenantId, status).map { }
    }

    override suspend fun updateInvoice(invoiceId: InvoiceId, request: CreateInvoiceRequest): Result<Invoice> {
        logger.info("updateInvoice called for invoice: $invoiceId")
        return invoiceRepository.updateInvoice(invoiceId, request.tenantId, request)
    }

    override suspend fun deleteInvoice(invoiceId: InvoiceId): Result<Unit> {
        logger.info("deleteInvoice called for invoice: $invoiceId")
        val tenantId = requireAuthenticatedTenantId()
        return invoiceRepository.deleteInvoice(invoiceId, tenantId).map { }
    }

    override suspend fun recordPayment(request: RecordPaymentRequest): Result<Unit> {
        logger.info("recordPayment called for invoice: ${request.invoiceId}")
        // TODO: Implement payment recording
        return Result.failure(NotImplementedError("Payment recording not yet implemented"))
    }

    override suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String?,
        message: String?
    ): Result<Unit> {
        logger.info("sendInvoiceEmail called for invoice: $invoiceId")
        // TODO: Implement email sending
        return Result.failure(NotImplementedError("Email sending not yet implemented"))
    }

    override suspend fun markInvoiceAsSent(invoiceId: InvoiceId): Result<Unit> {
        logger.info("markInvoiceAsSent called for invoice: $invoiceId")
        // TODO: Implement mark as sent
        return Result.failure(NotImplementedError("Mark as sent not yet implemented"))
    }

    override suspend fun calculateInvoiceTotals(items: List<InvoiceItem>): Result<InvoiceTotals> {
        logger.info("calculateInvoiceTotals called with ${items.size} items")
        // TODO: Implement totals calculation
        return Result.success(
            InvoiceTotals(
                subtotal = Money.ZERO,
                vatAmount = Money.ZERO,
                total = Money.ZERO
            )
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

    override suspend fun createExpense(request: CreateExpenseRequest): Result<Expense> {
        logger.info("createExpense called for tenant: ${request.tenantId}")
        return expenseRepository.createExpense(request)
    }

    override suspend fun getExpense(id: ExpenseId): Result<Expense> {
        logger.info("getExpense called for id: $id")
        val tenantId = requireAuthenticatedTenantId()
        return expenseRepository.getExpense(id, tenantId).mapCatching { expense ->
            expense ?: throw IllegalArgumentException("Expense not found or access denied")
        }
    }

    override suspend fun listExpenses(
        tenantId: TenantId,
        category: ExpenseCategory?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): Result<List<Expense>> {
        logger.info("listExpenses called for tenant: $tenantId")
        return expenseRepository.listExpenses(tenantId, category, fromDate, toDate, limit, offset)
    }

    override suspend fun updateExpense(expenseId: ExpenseId, request: CreateExpenseRequest): Result<Expense> {
        logger.info("updateExpense called for expense: $expenseId")
        return expenseRepository.updateExpense(expenseId, request.tenantId, request)
    }

    override suspend fun deleteExpense(expenseId: ExpenseId): Result<Unit> {
        logger.info("deleteExpense called for expense: $expenseId")
        val tenantId = requireAuthenticatedTenantId()
        return expenseRepository.deleteExpense(expenseId, tenantId).map { }
    }

    override suspend fun categorizeExpense(merchant: String, description: String?): Result<ExpenseCategory> {
        logger.info("categorizeExpense called for merchant: $merchant")
        // TODO: Implement auto-categorization
        return Result.success(ExpenseCategory.Other)
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
    ): Result<AttachmentId> {
        logger.info("uploadInvoiceDocument called for invoice: $invoiceId, file: $filename (${fileContent.size} bytes)")

        return runCatching {
            val tenantId = requireAuthenticatedTenantId()

            val invoice = invoiceRepository.getInvoice(invoiceId, tenantId).getOrThrow()
                ?: throw IllegalArgumentException("Invoice not found or access denied")

            val validationError = documentStorageService.validateFile(fileContent, filename, contentType)
            if (validationError != null) {
                throw IllegalArgumentException(validationError)
            }

            val storageKey = documentStorageService.storeFileLocally(
                tenantId, "invoice", invoiceId.toString(), filename, fileContent
            ).getOrThrow()

            attachmentRepository.uploadAttachment(
                tenantId = tenantId,
                entityType = EntityType.Invoice,
                entityId = invoiceId.toString(),
                filename = filename,
                mimeType = contentType,
                sizeBytes = fileContent.size.toLong(),
                s3Key = storageKey,
                s3Bucket = "local"
            ).getOrThrow()
        }
    }

    override suspend fun uploadExpenseReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): Result<AttachmentId> {
        logger.info("uploadExpenseReceipt called for expense: $expenseId, file: $filename (${fileContent.size} bytes)")

        return runCatching {
            val tenantId = requireAuthenticatedTenantId()

            val expense = expenseRepository.getExpense(expenseId, tenantId).getOrThrow()
                ?: throw IllegalArgumentException("Expense not found or access denied")

            val validationError = documentStorageService.validateFile(fileContent, filename, contentType)
            if (validationError != null) {
                throw IllegalArgumentException(validationError)
            }

            val storageKey = documentStorageService.storeFileLocally(
                tenantId, "expense", expenseId.toString(), filename, fileContent
            ).getOrThrow()

            attachmentRepository.uploadAttachment(
                tenantId = tenantId,
                entityType = EntityType.Expense,
                entityId = expenseId.toString(),
                filename = filename,
                mimeType = contentType,
                sizeBytes = fileContent.size.toLong(),
                s3Key = storageKey,
                s3Bucket = "local"
            ).getOrThrow()
        }
    }

    override suspend fun getInvoiceAttachments(invoiceId: InvoiceId): Result<List<Attachment>> {
        logger.info("getInvoiceAttachments called for invoice: $invoiceId")

        return runCatching {
            val tenantId = requireAuthenticatedTenantId()

            val invoice = invoiceRepository.getInvoice(invoiceId, tenantId).getOrThrow()
                ?: throw IllegalArgumentException("Invoice not found or access denied")

            attachmentRepository.getAttachments(
                tenantId = tenantId,
                entityType = EntityType.Invoice,
                entityId = invoiceId.toString()
            ).getOrThrow()
        }
    }

    override suspend fun getExpenseAttachments(expenseId: ExpenseId): Result<List<Attachment>> {
        logger.info("getExpenseAttachments called for expense: $expenseId")

        return runCatching {
            val tenantId = requireAuthenticatedTenantId()

            val expense = expenseRepository.getExpense(expenseId, tenantId).getOrThrow()
                ?: throw IllegalArgumentException("Expense not found or access denied")

            attachmentRepository.getAttachments(
                tenantId = tenantId,
                entityType = EntityType.Expense,
                entityId = expenseId.toString()
            ).getOrThrow()
        }
    }

    override suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): Result<String> {
        logger.info("getAttachmentDownloadUrl called for attachment: $attachmentId")

        return runCatching {
            val tenantId = requireAuthenticatedTenantId()

            val attachment = attachmentRepository.getAttachment(attachmentId, tenantId).getOrThrow()
                ?: throw IllegalArgumentException("Attachment not found or access denied")

            // Generate download URL
            documentStorageService.generateDownloadUrl(attachment.s3Key)
        }
    }

    override suspend fun deleteAttachment(attachmentId: AttachmentId): Result<Unit> {
        logger.info("deleteAttachment called for attachment: $attachmentId")

        return runCatching {
            val tenantId = requireAuthenticatedTenantId()

            // First get the attachment to know the storage key
            val attachment = attachmentRepository.getAttachment(attachmentId, tenantId).getOrThrow()
                ?: throw IllegalArgumentException("Attachment not found or access denied")

            // Delete from storage
            documentStorageService.deleteFileLocally(attachment.s3Key).getOrThrow()

            // Delete from database
            attachmentRepository.deleteAttachment(attachmentId, tenantId).getOrThrow()
        }
    }

    // ============================================================================
    // STATISTICS & OVERVIEW
    // ============================================================================

    override suspend fun getCashflowOverview(
        tenantId: TenantId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Result<CashflowOverview> {
        logger.info("getCashflowOverview called for tenant: $tenantId")
        // TODO: Implement overview calculation
        return Result.success(
            CashflowOverview(
                totalIncome = Money.ZERO,
                totalExpenses = Money.ZERO,
                netCashflow = Money.ZERO,
                pendingInvoices = Money.ZERO,
                overdueInvoices = Money.ZERO,
                invoiceCount = 0,
                expenseCount = 0
            )
        )
    }
}
