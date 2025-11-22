package ai.dokus.app.cashflow.network

import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.ids.AttachmentId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.model.*
import ai.dokus.foundation.domain.rpc.CashflowRemoteService
import ai.dokus.foundation.network.resilient.ResilientDelegate
import kotlinx.coroutines.flow.Flow

/**
 * A thin resiliency wrapper around CashflowRemoteService that:
 * - Recreates the underlying RPC client/service once when a call fails due to a transient error
 * - Ensures fresh connection after authentication/login
 *
 * The [serviceProvider] should return a live instance created from a fresh KtorRpcClient
 * (e.g., via createAuthenticatedRpcClient with waitForServices=false).
 */
class ResilientCashflowRemoteService(
    serviceProvider: () -> CashflowRemoteService
) : CashflowRemoteService {

    private val delegate = ResilientDelegate(serviceProvider)

    private fun get(): CashflowRemoteService = delegate.get()

    private suspend inline fun <R> withRetry(crossinline block: suspend (CashflowRemoteService) -> R): R =
        delegate.withRetry(block)

    // Invoices
    override suspend fun createInvoice(request: CreateInvoiceRequest): FinancialDocumentDto.InvoiceDto =
        withRetry { it.createInvoice(request) }

    override suspend fun getInvoice(id: InvoiceId): FinancialDocumentDto.InvoiceDto =
        withRetry { it.getInvoice(id) }

    override suspend fun listInvoices(
        status: InvoiceStatus?,
        fromDate: kotlinx.datetime.LocalDate?,
        toDate: kotlinx.datetime.LocalDate?,
        limit: Int,
        offset: Int
    ): List<FinancialDocumentDto.InvoiceDto> = withRetry {
        it.listInvoices(status, fromDate, toDate, limit, offset)
    }

    override suspend fun listOverdueInvoices(): List<FinancialDocumentDto.InvoiceDto> =
        withRetry { it.listOverdueInvoices() }

    override suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus) =
        withRetry { it.updateInvoiceStatus(invoiceId, status) }

    override suspend fun updateInvoice(
        invoiceId: InvoiceId,
        request: CreateInvoiceRequest
    ): FinancialDocumentDto.InvoiceDto = withRetry { it.updateInvoice(invoiceId, request) }

    override suspend fun deleteInvoice(invoiceId: InvoiceId) =
        withRetry { it.deleteInvoice(invoiceId) }

    override suspend fun recordPayment(request: RecordPaymentRequest) =
        withRetry { it.recordPayment(request) }

    override suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String?,
        message: String?
    ) = withRetry { it.sendInvoiceEmail(invoiceId, recipientEmail, message) }

    override suspend fun markInvoiceAsSent(invoiceId: InvoiceId) =
        withRetry { it.markInvoiceAsSent(invoiceId) }

    override suspend fun calculateInvoiceTotals(items: List<InvoiceItemDto>): InvoiceTotals =
        withRetry { it.calculateInvoiceTotals(items) }

    override fun watchInvoices(organizationId: OrganizationId): Flow<FinancialDocumentDto.InvoiceDto> =
        // For streaming flows, just delegate directly (reconnection is handled by consumer if needed)
        get().watchInvoices(organizationId)

    // Expenses
    override suspend fun createExpense(request: CreateExpenseRequest): FinancialDocumentDto.ExpenseDto =
        withRetry { it.createExpense(request) }

    override suspend fun getExpense(id: ExpenseId): FinancialDocumentDto.ExpenseDto =
        withRetry { it.getExpense(id) }

    override suspend fun listExpenses(
        category: ExpenseCategory?,
        fromDate: kotlinx.datetime.LocalDate?,
        toDate: kotlinx.datetime.LocalDate?,
        limit: Int,
        offset: Int
    ): List<FinancialDocumentDto.ExpenseDto> = withRetry {
        it.listExpenses(category, fromDate, toDate, limit, offset)
    }

    override suspend fun updateExpense(
        expenseId: ExpenseId,
        request: CreateExpenseRequest
    ): FinancialDocumentDto.ExpenseDto = withRetry { it.updateExpense(expenseId, request) }

    override suspend fun deleteExpense(expenseId: ExpenseId) =
        withRetry { it.deleteExpense(expenseId) }

    override suspend fun categorizeExpense(merchant: String, description: String?): ExpenseCategory =
        withRetry { it.categorizeExpense(merchant, description) }

    override fun watchExpenses(organizationId: OrganizationId): Flow<FinancialDocumentDto.ExpenseDto> =
        get().watchExpenses(organizationId)

    // Attachments
    override suspend fun uploadInvoiceDocument(
        invoiceId: InvoiceId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): AttachmentId = withRetry { it.uploadInvoiceDocument(invoiceId, fileContent, filename, contentType) }

    override suspend fun uploadExpenseReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): AttachmentId = withRetry { it.uploadExpenseReceipt(expenseId, fileContent, filename, contentType) }

    override suspend fun getInvoiceAttachments(invoiceId: InvoiceId): List<AttachmentDto> =
        withRetry { it.getInvoiceAttachments(invoiceId) }

    override suspend fun getExpenseAttachments(expenseId: ExpenseId): List<AttachmentDto> =
        withRetry { it.getExpenseAttachments(expenseId) }

    override suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): String =
        withRetry { it.getAttachmentDownloadUrl(attachmentId) }

    override suspend fun deleteAttachment(attachmentId: AttachmentId) =
        withRetry { it.deleteAttachment(attachmentId) }

    override suspend fun getCashflowOverview(
        fromDate: kotlinx.datetime.LocalDate,
        toDate: kotlinx.datetime.LocalDate
    ): CashflowOverview = withRetry { it.getCashflowOverview(fromDate, toDate) }
}
