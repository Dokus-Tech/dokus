package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.ids.AttachmentId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.Attachment
import ai.dokus.foundation.domain.model.CashflowOverview
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.Expense
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.InvoiceItem
import ai.dokus.foundation.domain.model.InvoiceTotals
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.rpc.annotations.Rpc

/**
 * Unified Cashflow API combining Invoice and Expense management with document support.
 *
 * This API provides:
 * - Invoice CRUD operations
 * - Expense tracking
 * - Document/attachment management for both invoices and expenses
 * - Payment tracking
 * - Real-time updates via Flow
 */
@Rpc
interface CashflowRemoteService {

    // ============================================================================
    // INVOICE MANAGEMENT
    // ============================================================================

    /**
     * Create a new invoice with optional document attachments
     */
    suspend fun createInvoice(request: CreateInvoiceRequest): Invoice

    /**
     * Get a single invoice by ID with all related documents
     */
    suspend fun getInvoice(id: InvoiceId): Invoice

    /**
     * List invoices with optional filtering
     * @param status Filter by invoice status (DRAFT, SENT, PAID, OVERDUE, CANCELLED)
     * @param fromDate Start date filter
     * @param toDate End date filter
     */
    suspend fun listInvoices(
        status: InvoiceStatus? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<Invoice>

    /**
     * List all overdue invoices for a tenant
     */
    suspend fun listOverdueInvoices(): List<Invoice>

    /**
     * Update invoice status
     */
    suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus)

    /**
     * Update an existing invoice
     */
    suspend fun updateInvoice(
        invoiceId: InvoiceId,
        request: CreateInvoiceRequest
    ): Invoice

    /**
     * Delete an invoice (soft delete)
     */
    suspend fun deleteInvoice(invoiceId: InvoiceId)

    /**
     * Record a payment for an invoice
     */
    suspend fun recordPayment(request: RecordPaymentRequest)

    /**
     * Send invoice via email
     */
    suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String? = null,
        message: String? = null
    )

    /**
     * Mark invoice as sent
     */
    suspend fun markInvoiceAsSent(invoiceId: InvoiceId)

    /**
     * Calculate invoice totals from line items
     */
    suspend fun calculateInvoiceTotals(items: List<InvoiceItem>): InvoiceTotals

    /**
     * Watch for real-time invoice updates
     */
    fun watchInvoices(tenantId: TenantId): Flow<Invoice>

    // ============================================================================
    // EXPENSE MANAGEMENT
    // ============================================================================

    /**
     * Create a new expense
     */
    suspend fun createExpense(request: CreateExpenseRequest): Expense

    /**
     * Get a single expense by ID
     */
    suspend fun getExpense(id: ExpenseId): Expense

    /**
     * List expenses with optional filtering
     */
    suspend fun listExpenses(
        category: ExpenseCategory? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<Expense>

    /**
     * Update an existing expense
     */
    suspend fun updateExpense(
        expenseId: ExpenseId,
        request: CreateExpenseRequest
    ): Expense

    /**
     * Delete an expense
     */
    suspend fun deleteExpense(expenseId: ExpenseId)

    /**
     * Automatically categorize an expense based on merchant and description
     */
    suspend fun categorizeExpense(merchant: String, description: String? = null): ExpenseCategory

    /**
     * Watch for real-time expense updates
     */
    fun watchExpenses(tenantId: TenantId): Flow<Expense>

    // ============================================================================
    // DOCUMENT/ATTACHMENT MANAGEMENT
    // ============================================================================

    /**
     * Upload a document for an invoice
     * @param invoiceId The invoice to attach the document to
     * @param fileContent The file content as ByteArray
     * @param filename Original filename
     * @param contentType MIME type (e.g., "application/pdf", "image/jpeg")
     * @return The attachment ID
     */
    suspend fun uploadInvoiceDocument(
        invoiceId: InvoiceId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): AttachmentId

    /**
     * Upload a receipt for an expense
     * @param expenseId The expense to attach the receipt to
     * @param fileContent The file content as ByteArray
     * @param filename Original filename
     * @param contentType MIME type
     * @return The attachment ID
     */
    suspend fun uploadExpenseReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): AttachmentId

    /**
     * Get all attachments for an invoice
     */
    suspend fun getInvoiceAttachments(invoiceId: InvoiceId): List<Attachment>

    /**
     * Get all attachments for an expense
     */
    suspend fun getExpenseAttachments(expenseId: ExpenseId): List<Attachment>

    /**
     * Get a download URL for a specific attachment
     * Returns a presigned URL valid for a limited time
     */
    suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): String

    /**
     * Delete an attachment
     */
    suspend fun deleteAttachment(attachmentId: AttachmentId)

    // ============================================================================
    // STATISTICS & OVERVIEW
    // ============================================================================

    /**
     * Get cashflow overview for a date range
     */
    suspend fun getCashflowOverview(
        fromDate: LocalDate,
        toDate: LocalDate
    ): CashflowOverview
}
