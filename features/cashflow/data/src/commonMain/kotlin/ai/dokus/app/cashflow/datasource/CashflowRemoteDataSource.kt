package ai.dokus.app.cashflow.datasource

import ai.dokus.foundation.domain.enums.BillStatus
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.ids.AttachmentId
import ai.dokus.foundation.domain.ids.BillId
import ai.dokus.foundation.domain.ids.DocumentId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.model.AttachmentDto
import ai.dokus.foundation.domain.model.CashflowOverview
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.DocumentDto
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.domain.model.InvoiceTotals
import ai.dokus.foundation.domain.model.MarkBillPaidRequest
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import io.ktor.client.HttpClient
import kotlinx.datetime.LocalDate

/**
 * Remote data source for cashflow operations
 * Provides HTTP-based access to invoice, expense, and attachment management endpoints
 */
interface CashflowRemoteDataSource {

    // ============================================================================
    // INVOICE MANAGEMENT
    // ============================================================================

    /**
     * Create a new invoice with optional document attachments
     * POST /api/v1/invoices
     */
    suspend fun createInvoice(request: CreateInvoiceRequest): Result<FinancialDocumentDto.InvoiceDto>

    /**
     * Get a single invoice by ID with all related documents
     * GET /api/v1/invoices/{id}
     */
    suspend fun getInvoice(id: InvoiceId): Result<FinancialDocumentDto.InvoiceDto>

    /**
     * List invoices with optional filtering
     * GET /api/v1/invoices?status={status}&fromDate={fromDate}&toDate={toDate}&limit={limit}&offset={offset}
     *
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
    ): Result<PaginatedResponse<FinancialDocumentDto.InvoiceDto>>

    /**
     * List all overdue invoices for a tenant
     * GET /api/v1/invoices/overdue
     */
    suspend fun listOverdueInvoices(): Result<List<FinancialDocumentDto.InvoiceDto>>

    /**
     * Update invoice status
     * PATCH /api/v1/invoices/{id}/status
     */
    suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus): Result<Unit>

    /**
     * Update an existing invoice
     * PUT /api/v1/invoices/{id}
     */
    suspend fun updateInvoice(
        invoiceId: InvoiceId,
        request: CreateInvoiceRequest
    ): Result<FinancialDocumentDto.InvoiceDto>

    /**
     * Delete an invoice (soft delete)
     * DELETE /api/v1/invoices/{id}
     */
    suspend fun deleteInvoice(invoiceId: InvoiceId): Result<Unit>

    /**
     * Record a payment for an invoice
     * POST /api/v1/invoices/{id}/payments
     */
    suspend fun recordPayment(request: RecordPaymentRequest): Result<Unit>

    /**
     * Send invoice via email
     * POST /api/v1/invoices/{id}/send-email
     */
    suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String? = null,
        message: String? = null
    ): Result<Unit>

    /**
     * Mark invoice as sent
     * POST /api/v1/invoices/{id}/mark-sent
     */
    suspend fun markInvoiceAsSent(invoiceId: InvoiceId): Result<Unit>

    /**
     * Calculate invoice totals from line items
     * POST /api/v1/invoices/calculate-totals
     */
    suspend fun calculateInvoiceTotals(items: List<InvoiceItemDto>): Result<InvoiceTotals>

    // ============================================================================
    // EXPENSE MANAGEMENT
    // ============================================================================

    /**
     * Create a new expense
     * POST /api/v1/expenses
     */
    suspend fun createExpense(request: CreateExpenseRequest): Result<FinancialDocumentDto.ExpenseDto>

    /**
     * Get a single expense by ID
     * GET /api/v1/expenses/{id}
     */
    suspend fun getExpense(id: ExpenseId): Result<FinancialDocumentDto.ExpenseDto>

    /**
     * List expenses with optional filtering
     * GET /api/v1/expenses?category={category}&fromDate={fromDate}&toDate={toDate}&limit={limit}&offset={offset}
     */
    suspend fun listExpenses(
        category: ExpenseCategory? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<FinancialDocumentDto.ExpenseDto>>

    /**
     * Update an existing expense
     * PUT /api/v1/expenses/{id}
     */
    suspend fun updateExpense(
        expenseId: ExpenseId,
        request: CreateExpenseRequest
    ): Result<FinancialDocumentDto.ExpenseDto>

    /**
     * Delete an expense
     * DELETE /api/v1/expenses/{id}
     */
    suspend fun deleteExpense(expenseId: ExpenseId): Result<Unit>

    /**
     * Automatically categorize an expense based on merchant and description
     * POST /api/v1/expenses/categorize
     */
    suspend fun categorizeExpense(
        merchant: String,
        description: String? = null
    ): Result<ExpenseCategory>

    // ============================================================================
    // BILL MANAGEMENT (Supplier Invoices / Cash-Out)
    // ============================================================================

    /**
     * Create a new bill (supplier invoice)
     * POST /api/v1/cashflow/cash-out/bills
     */
    suspend fun createBill(request: CreateBillRequest): Result<FinancialDocumentDto.BillDto>

    /**
     * Get a single bill by ID
     * GET /api/v1/cashflow/cash-out/bills/{id}
     */
    suspend fun getBill(id: BillId): Result<FinancialDocumentDto.BillDto>

    /**
     * List bills with optional filtering
     * GET /api/v1/cashflow/cash-out/bills?status={status}&category={category}&fromDate={fromDate}&toDate={toDate}&limit={limit}&offset={offset}
     */
    suspend fun listBills(
        status: BillStatus? = null,
        category: ExpenseCategory? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<FinancialDocumentDto.BillDto>>

    /**
     * List all overdue bills
     * GET /api/v1/cashflow/cash-out/bills/overdue
     */
    suspend fun listOverdueBills(): Result<List<FinancialDocumentDto.BillDto>>

    /**
     * Update an existing bill
     * PUT /api/v1/cashflow/cash-out/bills/{id}
     */
    suspend fun updateBill(
        billId: BillId,
        request: CreateBillRequest
    ): Result<FinancialDocumentDto.BillDto>

    /**
     * Mark bill as paid
     * POST /api/v1/cashflow/cash-out/bills/{id}/pay
     */
    suspend fun markBillPaid(
        billId: BillId,
        request: MarkBillPaidRequest
    ): Result<FinancialDocumentDto.BillDto>

    /**
     * Delete a bill
     * DELETE /api/v1/cashflow/cash-out/bills/{id}
     */
    suspend fun deleteBill(billId: BillId): Result<Unit>

    // ============================================================================
    // DOCUMENT/ATTACHMENT MANAGEMENT
    // ============================================================================

    /**
     * Upload a document for an invoice
     * POST /api/v1/invoices/{invoiceId}/attachments
     *
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
     * POST /api/v1/expenses/{expenseId}/attachments
     *
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
     * GET /api/v1/invoices/{invoiceId}/attachments
     */
    suspend fun getInvoiceAttachments(invoiceId: InvoiceId): Result<List<AttachmentDto>>

    /**
     * Get all attachments for an expense
     * GET /api/v1/expenses/{expenseId}/attachments
     */
    suspend fun getExpenseAttachments(expenseId: ExpenseId): Result<List<AttachmentDto>>

    /**
     * Get a download URL for a specific attachment
     * GET /api/v1/attachments/{attachmentId}/download-url
     *
     * Returns a presigned URL valid for a limited time
     */
    suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): Result<String>

    /**
     * Delete an attachment
     * DELETE /api/v1/attachments/{attachmentId}
     */
    suspend fun deleteAttachment(attachmentId: AttachmentId): Result<Unit>

    // ============================================================================
    // DOCUMENT MANAGEMENT (MinIO Storage)
    // ============================================================================

    /**
     * Upload a document to MinIO object storage.
     * POST /api/v1/documents/upload
     *
     * Documents are stored in MinIO and metadata is persisted in the database.
     * Returns a DocumentDto with the document ID and a fresh presigned download URL.
     *
     * @param fileContent The file content as ByteArray
     * @param filename Original filename
     * @param contentType MIME type (e.g., "application/pdf", "image/jpeg")
     * @param prefix Storage prefix (e.g., "invoices", "bills", "expenses")
     * @return DocumentDto with id and downloadUrl
     */
    suspend fun uploadDocument(
        fileContent: ByteArray,
        filename: String,
        contentType: String,
        prefix: String = "documents"
    ): Result<DocumentDto>

    /**
     * Get a document by ID with a fresh presigned download URL.
     * GET /api/v1/documents/{id}
     *
     * Use this to get a fresh download URL for an existing document.
     *
     * @param documentId The document ID
     * @return DocumentDto with fresh downloadUrl
     */
    suspend fun getDocument(documentId: DocumentId): Result<DocumentDto>

    /**
     * Delete a document by ID.
     * DELETE /api/v1/documents/{id}
     *
     * @param documentId The document ID to delete
     */
    suspend fun deleteDocument(documentId: DocumentId): Result<Unit>

    // ============================================================================
    // STATISTICS & OVERVIEW
    // ============================================================================

    /**
     * List combined cashflow documents (invoices + expenses) with pagination.
     * GET /api/v1/cashflow/documents?fromDate={fromDate}&toDate={toDate}&limit={limit}&offset={offset}
     */
    suspend fun listCashflowDocuments(
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<FinancialDocumentDto>>

    /**
     * Get cashflow overview for a date range
     * GET /api/v1/cashflow/overview?fromDate={fromDate}&toDate={toDate}
     */
    suspend fun getCashflowOverview(
        fromDate: LocalDate,
        toDate: LocalDate
    ): Result<CashflowOverview>

    companion object {
        internal fun create(httpClient: HttpClient): CashflowRemoteDataSource {
            return CashflowRemoteDataSourceImpl(httpClient)
        }
    }
}
