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
 * This implementation throws NotImplementedError for all operations,
 * serving as a graceful degradation when the backend service is unavailable.
 */
class MockCashflowApi : CashflowApi {

    private fun notImplemented(): Nothing {
        throw NotImplementedError("Cashflow service is currently unavailable. Please check your connection.")
    }

    // ============================================================================
    // INVOICE MANAGEMENT
    // ============================================================================

    override suspend fun createInvoice(request: CreateInvoiceRequest): Invoice = notImplemented()

    override suspend fun getInvoice(id: InvoiceId): Invoice = notImplemented()

    override suspend fun listInvoices(
        tenantId: TenantId,
        status: InvoiceStatus?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): List<Invoice> = notImplemented()

    override suspend fun listOverdueInvoices(tenantId: TenantId): List<Invoice> = notImplemented()

    override suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus) = notImplemented()

    override suspend fun updateInvoice(
        invoiceId: InvoiceId,
        request: CreateInvoiceRequest
    ): Invoice = notImplemented()

    override suspend fun deleteInvoice(invoiceId: InvoiceId) = notImplemented()

    override suspend fun recordPayment(request: RecordPaymentRequest) = notImplemented()

    override suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String?,
        message: String?
    ) = notImplemented()

    override suspend fun markInvoiceAsSent(invoiceId: InvoiceId) = notImplemented()

    override suspend fun calculateInvoiceTotals(items: List<InvoiceItem>): InvoiceTotals = notImplemented()

    override fun watchInvoices(tenantId: TenantId): Flow<Invoice> = emptyFlow()

    // ============================================================================
    // EXPENSE MANAGEMENT
    // ============================================================================

    override suspend fun createExpense(request: CreateExpenseRequest): Expense = notImplemented()

    override suspend fun getExpense(id: ExpenseId): Expense = notImplemented()

    override suspend fun listExpenses(
        tenantId: TenantId,
        category: ExpenseCategory?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): List<Expense> = notImplemented()

    override suspend fun updateExpense(
        expenseId: ExpenseId,
        request: CreateExpenseRequest
    ): Expense = notImplemented()

    override suspend fun deleteExpense(expenseId: ExpenseId) = notImplemented()

    override suspend fun categorizeExpense(merchant: String, description: String?): ExpenseCategory = notImplemented()

    override fun watchExpenses(tenantId: TenantId): Flow<Expense> = emptyFlow()

    // ============================================================================
    // DOCUMENT/ATTACHMENT MANAGEMENT
    // ============================================================================

    override suspend fun uploadInvoiceDocument(
        invoiceId: InvoiceId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): AttachmentId = notImplemented()

    override suspend fun uploadExpenseReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): AttachmentId = notImplemented()

    override suspend fun getInvoiceAttachments(invoiceId: InvoiceId): List<Attachment> = notImplemented()

    override suspend fun getExpenseAttachments(expenseId: ExpenseId): List<Attachment> = notImplemented()

    override suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): String = notImplemented()

    override suspend fun deleteAttachment(attachmentId: AttachmentId) = notImplemented()

    // ============================================================================
    // STATISTICS & OVERVIEW
    // ============================================================================

    override suspend fun getCashflowOverview(
        tenantId: TenantId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): CashflowOverview = notImplemented()
}
