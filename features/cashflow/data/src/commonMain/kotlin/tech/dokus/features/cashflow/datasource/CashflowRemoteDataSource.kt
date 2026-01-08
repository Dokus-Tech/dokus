@file:Suppress(
    "TooManyFunctions", // Cashflow API requires many endpoints
    "LongParameterList", // Query parameters for filtering
    "MaxLineLength" // API endpoint documentation has long query parameter strings
)

package tech.dokus.features.cashflow.datasource

import io.ktor.client.HttpClient
import kotlinx.datetime.LocalDate
import tech.dokus.domain.config.DynamicDokusEndpointProvider
import tech.dokus.domain.enums.BillStatus
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.ids.AttachmentId
import tech.dokus.domain.ids.BillId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.AttachmentDto
import tech.dokus.domain.model.CancelEntryRequest
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.CashflowOverview
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.ConfirmDocumentRequest
import tech.dokus.domain.model.CreateBillRequest
import tech.dokus.domain.model.CreateExpenseRequest
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentPagesResponse
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.MarkBillPaidRequest
import tech.dokus.domain.model.PeppolConnectRequest
import tech.dokus.domain.model.PeppolConnectResponse
import tech.dokus.domain.model.PeppolInboxPollResponse
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.PeppolTransmissionDto
import tech.dokus.domain.model.PeppolValidationResult
import tech.dokus.domain.model.PeppolVerifyResponse
import tech.dokus.domain.model.RecordPaymentRequest
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ReprocessResponse
import tech.dokus.domain.model.SavePeppolSettingsRequest
import tech.dokus.domain.model.SendInvoiceViaPeppolResponse
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.UpdateDraftResponse
import tech.dokus.domain.model.common.PaginatedResponse

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
     * POST /api/v1/invoices/{id}/emails
     */
    suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String? = null,
        message: String? = null
    ): Result<Unit>

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
     * POST /api/v1/bills/{id}/payments
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
     * Upload a document with progress tracking.
     * POST /api/v1/documents/upload
     *
     * Same as [uploadDocument] but with progress callback for UI updates.
     *
     * @param fileContent The file content as ByteArray
     * @param filename Original filename
     * @param contentType MIME type (e.g., "application/pdf", "image/jpeg")
     * @param prefix Storage prefix (e.g., "invoices", "bills", "expenses")
     * @param onProgress Callback with progress from 0.0 to 1.0
     * @return DocumentDto with id and downloadUrl
     */
    suspend fun uploadDocumentWithProgress(
        fileContent: ByteArray,
        filename: String,
        contentType: String,
        prefix: String = "documents",
        onProgress: (Float) -> Unit
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

    // ============================================================================
    // CASHFLOW ENTRIES (Projection Ledger)
    // ============================================================================

    /**
     * List cashflow entries with optional filtering.
     * GET /api/v1/cashflow/entries?fromDate={fromDate}&toDate={toDate}&direction={direction}&status={status}&sourceType={sourceType}&entryId={entryId}&limit={limit}&offset={offset}
     *
     * @param fromDate Filter by event date start
     * @param toDate Filter by event date end
     * @param direction Filter by direction (IN/OUT)
     * @param status Filter by status (OPEN/PAID/OVERDUE/CANCELLED)
     * @param sourceType Filter by source type (INVOICE/BILL/EXPENSE)
     * @param entryId Exact match by entry ID (for deep links)
     * @param limit Items per page
     * @param offset Pagination offset
     */
    suspend fun listCashflowEntries(
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        direction: CashflowDirection? = null,
        status: CashflowEntryStatus? = null,
        sourceType: CashflowSourceType? = null,
        entryId: CashflowEntryId? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<CashflowEntry>>

    /**
     * Get a single cashflow entry by ID.
     * GET /api/v1/cashflow/entries/{id}
     */
    suspend fun getCashflowEntry(entryId: CashflowEntryId): Result<CashflowEntry>

    /**
     * Record a payment against a cashflow entry.
     * POST /api/v1/cashflow/entries/{id}/payments
     */
    suspend fun recordCashflowPayment(
        entryId: CashflowEntryId,
        request: CashflowPaymentRequest
    ): Result<CashflowEntry>

    /**
     * Cancel a cashflow entry.
     * POST /api/v1/cashflow/entries/{id}/cancel
     */
    suspend fun cancelCashflowEntry(
        entryId: CashflowEntryId,
        request: CancelEntryRequest? = null
    ): Result<CashflowEntry>

    // ============================================================================
    // DOCUMENT MANAGEMENT (AI Extraction Pipeline)
    // ============================================================================

    /**
     * List documents with optional filtering.
     * GET /api/v1/documents?draftStatus={draftStatus}&documentType={documentType}&ingestionStatus={ingestionStatus}&search={search}&page={page}&limit={limit}
     *
     * Returns DocumentRecordDto envelope containing document, draft, and latest ingestion.
     *
     * @param draftStatus Filter by draft status (NeedsReview, Ready, Confirmed, Rejected)
     * @param documentType Filter by document type (Invoice, Bill, Expense)
     * @param ingestionStatus Filter by ingestion status (Queued, Processing, Succeeded, Failed)
     * @param search Full-text search query
     * @param page Page number (0-indexed)
     * @param limit Items per page (max 100)
     */
    suspend fun listDocuments(
        draftStatus: DraftStatus? = null,
        documentType: DocumentType? = null,
        ingestionStatus: IngestionStatus? = null,
        search: String? = null,
        page: Int = 0,
        limit: Int = 20
    ): Result<PaginatedResponse<DocumentRecordDto>>

    /**
     * Get a document record by ID with full envelope (document + draft + latest ingestion + confirmed entity).
     * GET /api/v1/documents/{id}
     */
    suspend fun getDocumentRecord(documentId: DocumentId): Result<DocumentRecordDto>

    /**
     * Get the draft for a document.
     * GET /api/v1/documents/{id}/draft
     */
    suspend fun getDocumentDraft(documentId: DocumentId): Result<DocumentDraftDto>

    /**
     * Update a document draft.
     * PATCH /api/v1/documents/{id}/draft
     */
    suspend fun updateDocumentDraft(
        documentId: DocumentId,
        request: UpdateDraftRequest
    ): Result<UpdateDraftResponse>

    /**
     * Update the selected contact for a document draft.
     * PATCH /api/v1/documents/{id}/draft
     *
     * Binds or unbinds a contact to the document. This is persisted before confirmation.
     *
     * @param documentId The document ID
     * @param contactId The contact to bind, or null to clear
     */
    suspend fun updateDocumentDraftContact(
        documentId: DocumentId,
        contactId: ContactId?,
        counterpartyIntent: CounterpartyIntent? = null
    ): Result<Unit>

    /**
     * Reprocess a document (create new ingestion run).
     * POST /api/v1/documents/{id}/reprocess
     *
     * IDEMPOTENT: Returns existing Queued/Processing run unless force=true.
     */
    suspend fun reprocessDocument(
        documentId: DocumentId,
        request: ReprocessRequest = ReprocessRequest()
    ): Result<ReprocessResponse>

    /**
     * Confirm a document and create financial entity (Invoice/Bill/Expense).
     * POST /api/v1/documents/{id}/confirm
     *
     * TRANSACTIONAL + IDEMPOTENT: Fails if entity already exists for documentId.
     */
    suspend fun confirmDocument(
        documentId: DocumentId,
        request: ConfirmDocumentRequest
    ): Result<DocumentRecordDto>

    /**
     * Reject a document draft with a reason.
     * POST /api/v1/documents/{id}/reject
     */
    suspend fun rejectDocument(
        documentId: DocumentId,
        request: RejectDocumentRequest
    ): Result<DocumentRecordDto>

    /**
     * Get ingestion history for a document.
     * GET /api/v1/documents/{id}/ingestions
     */
    suspend fun getDocumentIngestions(documentId: DocumentId): Result<List<DocumentIngestionDto>>

    /**
     * Get available PDF pages for preview rendering.
     * GET /api/v1/documents/{id}/pages?dpi={dpi}&maxPages={maxPages}
     *
     * Returns page metadata with full URLs for authenticated image loading.
     *
     * @param documentId The document ID
     * @param dpi Resolution for rendered pages (72-300, default 150)
     * @param maxPages Maximum pages to return (1-50, default 10)
     */
    suspend fun getDocumentPages(
        documentId: DocumentId,
        dpi: Int = 150,
        maxPages: Int = 10
    ): Result<DocumentPagesResponse>

    // ============================================================================
    // PEPPOL E-INVOICING
    // ============================================================================

    // ----- Settings -----

    /**
     * Get available Peppol providers
     * GET /api/v1/peppol/providers
     */
    suspend fun getPeppolProviders(): Result<List<String>>

    /**
     * Get Peppol settings for current tenant
     * GET /api/v1/peppol/settings
     * Returns null if not configured
     */
    suspend fun getPeppolSettings(): Result<PeppolSettingsDto?>

    /**
     * Save Peppol settings
     * PUT /api/v1/peppol/settings
     */
    suspend fun savePeppolSettings(request: SavePeppolSettingsRequest): Result<PeppolSettingsDto>

    /**
     * Delete Peppol settings
     * DELETE /api/v1/peppol/settings
     */
    suspend fun deletePeppolSettings(): Result<Unit>

    /**
     * Test Peppol connection with current credentials
     * POST /api/v1/peppol/settings/test
     */
    suspend fun testPeppolConnection(): Result<Boolean>

    /**
     * Connect to Peppol by auto-discovering company via Recommand API
     * POST /api/v1/peppol/settings/connect
     *
     * This endpoint:
     * 1. Validates credentials with Recommand
     * 2. Searches for companies matching tenant VAT
     * 3. Returns status indicating next steps (Connected, MultipleMatches, NoCompanyFound, etc.)
     */
    suspend fun connectPeppol(request: PeppolConnectRequest): Result<PeppolConnectResponse>

    // ----- Verification & Validation -----

    /**
     * Verify if a recipient is registered on the Peppol network
     * POST /api/v1/peppol/verify
     */
    suspend fun verifyPeppolRecipient(peppolId: String): Result<PeppolVerifyResponse>

    /**
     * Validate an invoice for Peppol compliance without sending
     * POST /api/v1/peppol/send/validate/{invoiceId}
     */
    suspend fun validateInvoiceForPeppol(invoiceId: InvoiceId): Result<PeppolValidationResult>

    // ----- Sending -----

    /**
     * Send an invoice via Peppol
     * POST /api/v1/peppol/send/invoice/{invoiceId}
     */
    suspend fun sendInvoiceViaPeppol(invoiceId: InvoiceId): Result<SendInvoiceViaPeppolResponse>

    // ----- Inbox -----

    /**
     * Poll Peppol inbox for new documents
     * POST /api/v1/peppol/inbox/poll
     *
     * Creates bills for received invoices
     */
    suspend fun pollPeppolInbox(): Result<PeppolInboxPollResponse>

    // ----- Transmission History -----

    /**
     * List Peppol transmissions with optional filters
     * GET /api/v1/peppol/transmissions?direction={direction}&status={status}&limit={limit}&offset={offset}
     */
    suspend fun listPeppolTransmissions(
        direction: PeppolTransmissionDirection? = null,
        status: PeppolStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<PeppolTransmissionDto>>

    /**
     * Get Peppol transmission for a specific invoice
     * GET /api/v1/peppol/transmissions/invoice/{invoiceId}
     */
    suspend fun getPeppolTransmissionForInvoice(invoiceId: InvoiceId): Result<PeppolTransmissionDto?>

    companion object {
        internal fun create(
            httpClient: HttpClient,
            endpointProvider: DynamicDokusEndpointProvider
        ): CashflowRemoteDataSource {
            return CashflowRemoteDataSourceImpl(httpClient, endpointProvider)
        }
    }
}
