package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.*
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
interface CashflowApi {

    // ============================================================================
    // INVOICE MANAGEMENT
    // ============================================================================

    /**
     * Create a new invoice with optional document attachments
     */
    suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice>

    /**
     * Get a single invoice by ID with all related documents
     */
    suspend fun getInvoice(id: InvoiceId): Result<Invoice>

    /**
     * List invoices with optional filtering
     * @param status Filter by invoice status (DRAFT, SENT, PAID, OVERDUE, CANCELLED)
     * @param fromDate Start date filter
     * @param toDate End date filter
     */
    suspend fun listInvoices(
        tenantId: TenantId,
        status: InvoiceStatus? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Invoice>>

    /**
     * List all overdue invoices for a tenant
     */
    suspend fun listOverdueInvoices(tenantId: TenantId): Result<List<Invoice>>

    /**
     * Update invoice status
     */
    suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus): Result<Unit>

    /**
     * Update an existing invoice
     */
    suspend fun updateInvoice(
        invoiceId: InvoiceId,
        request: CreateInvoiceRequest
    ): Result<Invoice>

    /**
     * Delete an invoice (soft delete)
     */
    suspend fun deleteInvoice(invoiceId: InvoiceId): Result<Unit>

    /**
     * Record a payment for an invoice
     */
    suspend fun recordPayment(request: RecordPaymentRequest): Result<Unit>

    /**
     * Send invoice via email
     */
    suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String? = null,
        message: String? = null
    ): Result<Unit>

    /**
     * Mark invoice as sent
     */
    suspend fun markInvoiceAsSent(invoiceId: InvoiceId): Result<Unit>

    /**
     * Calculate invoice totals from line items
     */
    suspend fun calculateInvoiceTotals(items: List<InvoiceItem>): Result<InvoiceTotals>

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
    suspend fun createExpense(request: CreateExpenseRequest): Result<Expense>

    /**
     * Get a single expense by ID
     */
    suspend fun getExpense(id: ExpenseId): Result<Expense>

    /**
     * List expenses with optional filtering
     */
    suspend fun listExpenses(
        tenantId: TenantId,
        category: ExpenseCategory? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Expense>>

    /**
     * Update an existing expense
     */
    suspend fun updateExpense(
        expenseId: ExpenseId,
        request: CreateExpenseRequest
    ): Result<Expense>

    /**
     * Delete an expense
     */
    suspend fun deleteExpense(expenseId: ExpenseId): Result<Unit>

    /**
     * Automatically categorize an expense based on merchant and description
     */
    suspend fun categorizeExpense(merchant: String, description: String? = null): Result<ExpenseCategory>

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
    ): Result<AttachmentId>

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
    ): Result<AttachmentId>

    /**
     * Get all attachments for an invoice
     */
    suspend fun getInvoiceAttachments(invoiceId: InvoiceId): Result<List<Attachment>>

    /**
     * Get all attachments for an expense
     */
    suspend fun getExpenseAttachments(expenseId: ExpenseId): Result<List<Attachment>>

    /**
     * Get a download URL for a specific attachment
     * Returns a presigned URL valid for a limited time
     */
    suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): Result<String>

    /**
     * Delete an attachment
     */
    suspend fun deleteAttachment(attachmentId: AttachmentId): Result<Unit>

    // ============================================================================
    // STATISTICS & OVERVIEW
    // ============================================================================

    /**
     * Get cashflow overview for a date range
     */
    suspend fun getCashflowOverview(
        tenantId: TenantId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Result<CashflowOverview>
}

/**
 * Cashflow overview data
 */
@kotlinx.serialization.Serializable
data class CashflowOverview(
    val totalIncome: Money,
    val totalExpenses: Money,
    val netCashflow: Money,
    val pendingInvoices: Money,
    val overdueInvoices: Money,
    val invoiceCount: Int,
    val expenseCount: Int
)
