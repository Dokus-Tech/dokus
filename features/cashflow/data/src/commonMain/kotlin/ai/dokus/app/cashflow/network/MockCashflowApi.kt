package ai.dokus.app.cashflow.network

import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.*
import ai.dokus.foundation.domain.rpc.CashflowApi
import ai.dokus.foundation.domain.rpc.CashflowOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate

/**
 * Mock implementation of CashflowApi for offline/fallback scenarios.
 *
 * This implementation returns NotImplementedError for all operations,
 * serving as a graceful degradation when the backend service is unavailable.
 */
class MockCashflowApi : CashflowApi {

    private val notImplemented: Result<Nothing> = Result.failure(
        NotImplementedError("Cashflow service is currently unavailable. Please check your connection.")
    )

    // ============================================================================
    // INVOICE MANAGEMENT
    // ============================================================================

    override suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> = notImplemented

    override suspend fun getInvoice(id: InvoiceId): Result<Invoice> = notImplemented

    override suspend fun listInvoices(
        tenantId: TenantId,
        status: InvoiceStatus?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): Result<List<Invoice>> = notImplemented

    override suspend fun listOverdueInvoices(tenantId: TenantId): Result<List<Invoice>> = notImplemented

    override suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus): Result<Unit> = notImplemented

    override suspend fun updateInvoice(
        invoiceId: InvoiceId,
        request: CreateInvoiceRequest
    ): Result<Invoice> = notImplemented

    override suspend fun deleteInvoice(invoiceId: InvoiceId): Result<Unit> = notImplemented

    override suspend fun recordPayment(request: RecordPaymentRequest): Result<Unit> = notImplemented

    override suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String?,
        message: String?
    ): Result<Unit> = notImplemented

    override suspend fun markInvoiceAsSent(invoiceId: InvoiceId): Result<Unit> = notImplemented

    override suspend fun calculateInvoiceTotals(items: List<InvoiceItem>): Result<InvoiceTotals> = notImplemented

    override fun watchInvoices(tenantId: TenantId): Flow<Invoice> = emptyFlow()

    // ============================================================================
    // EXPENSE MANAGEMENT
    // ============================================================================

    override suspend fun createExpense(request: CreateExpenseRequest): Result<Expense> = notImplemented

    override suspend fun getExpense(id: ExpenseId): Result<Expense> = notImplemented

    override suspend fun listExpenses(
        tenantId: TenantId,
        category: ExpenseCategory?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): Result<List<Expense>> = notImplemented

    override suspend fun updateExpense(
        expenseId: ExpenseId,
        request: CreateExpenseRequest
    ): Result<Expense> = notImplemented

    override suspend fun deleteExpense(expenseId: ExpenseId): Result<Unit> = notImplemented

    override suspend fun categorizeExpense(merchant: String, description: String?): Result<ExpenseCategory> = notImplemented

    override fun watchExpenses(tenantId: TenantId): Flow<Expense> = emptyFlow()

    // ============================================================================
    // DOCUMENT/ATTACHMENT MANAGEMENT
    // ============================================================================

    override suspend fun uploadInvoiceDocument(
        invoiceId: InvoiceId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): Result<AttachmentId> = notImplemented

    override suspend fun uploadExpenseReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): Result<AttachmentId> = notImplemented

    override suspend fun getInvoiceAttachments(invoiceId: InvoiceId): Result<List<Attachment>> = notImplemented

    override suspend fun getExpenseAttachments(expenseId: ExpenseId): Result<List<Attachment>> = notImplemented

    override suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): Result<String> = notImplemented

    override suspend fun deleteAttachment(attachmentId: AttachmentId): Result<Unit> = notImplemented

    // ============================================================================
    // STATISTICS & OVERVIEW
    // ============================================================================

    override suspend fun getCashflowOverview(
        tenantId: TenantId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Result<CashflowOverview> = notImplemented
}
