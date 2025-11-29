package ai.dokus.app.cashflow.network

import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.ids.AttachmentId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.AttachmentDto
import ai.dokus.foundation.domain.model.CashflowOverview
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.domain.model.InvoiceTotals
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.domain.rpc.CashflowRemoteService
import ai.dokus.foundation.network.resilient.RemoteServiceDelegate
import ai.dokus.foundation.network.resilient.createRetryDelegate
import ai.dokus.foundation.network.resilient.invoke
import ai.dokus.foundation.network.resilient.withAuth
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
    serviceProvider: () -> CashflowRemoteService,
    tokenManager: TokenManager,
    authManager: AuthManager
) : CashflowRemoteService {

    private val delegate: RemoteServiceDelegate<CashflowRemoteService> =
        createRetryDelegate(serviceProvider).withAuth(tokenManager, authManager)

    // Invoices
    override suspend fun createInvoice(request: CreateInvoiceRequest): FinancialDocumentDto.InvoiceDto =
        delegate { it.createInvoice(request) }

    override suspend fun getInvoice(id: InvoiceId): FinancialDocumentDto.InvoiceDto =
        delegate { it.getInvoice(id) }

    override suspend fun listInvoices(
        status: InvoiceStatus?,
        fromDate: kotlinx.datetime.LocalDate?,
        toDate: kotlinx.datetime.LocalDate?,
        limit: Int,
        offset: Int
    ): List<FinancialDocumentDto.InvoiceDto> = delegate {
        it.listInvoices(status, fromDate, toDate, limit, offset)
    }

    override suspend fun listOverdueInvoices(): List<FinancialDocumentDto.InvoiceDto> =
        delegate { it.listOverdueInvoices() }

    override suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus) =
        delegate { it.updateInvoiceStatus(invoiceId, status) }

    override suspend fun updateInvoice(
        invoiceId: InvoiceId,
        request: CreateInvoiceRequest
    ): FinancialDocumentDto.InvoiceDto = delegate { it.updateInvoice(invoiceId, request) }

    override suspend fun deleteInvoice(invoiceId: InvoiceId) =
        delegate { it.deleteInvoice(invoiceId) }

    override suspend fun recordPayment(request: RecordPaymentRequest) =
        delegate { it.recordPayment(request) }

    override suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String?,
        message: String?
    ) = delegate { it.sendInvoiceEmail(invoiceId, recipientEmail, message) }

    override suspend fun markInvoiceAsSent(invoiceId: InvoiceId) =
        delegate { it.markInvoiceAsSent(invoiceId) }

    override suspend fun calculateInvoiceTotals(items: List<InvoiceItemDto>): InvoiceTotals =
        delegate { it.calculateInvoiceTotals(items) }

    override fun watchInvoices(tenantId: TenantId): Flow<FinancialDocumentDto.InvoiceDto> =
        // For streaming flows, just delegate directly (reconnection is handled by consumer if needed)
        delegate.get().watchInvoices(tenantId)

    // Expenses
    override suspend fun createExpense(request: CreateExpenseRequest): FinancialDocumentDto.ExpenseDto =
        delegate { it.createExpense(request) }

    override suspend fun getExpense(id: ExpenseId): FinancialDocumentDto.ExpenseDto =
        delegate { it.getExpense(id) }

    override suspend fun listExpenses(
        category: ExpenseCategory?,
        fromDate: kotlinx.datetime.LocalDate?,
        toDate: kotlinx.datetime.LocalDate?,
        limit: Int,
        offset: Int
    ): List<FinancialDocumentDto.ExpenseDto> = delegate {
        it.listExpenses(category, fromDate, toDate, limit, offset)
    }

    override suspend fun updateExpense(
        expenseId: ExpenseId,
        request: CreateExpenseRequest
    ): FinancialDocumentDto.ExpenseDto = delegate { it.updateExpense(expenseId, request) }

    override suspend fun deleteExpense(expenseId: ExpenseId) =
        delegate { it.deleteExpense(expenseId) }

    override suspend fun categorizeExpense(merchant: String, description: String?): ExpenseCategory =
        delegate { it.categorizeExpense(merchant, description) }

    override fun watchExpenses(tenantId: TenantId): Flow<FinancialDocumentDto.ExpenseDto> =
        delegate.get().watchExpenses(tenantId)

    // Attachments
    override suspend fun uploadInvoiceDocument(
        invoiceId: InvoiceId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): AttachmentId = delegate { it.uploadInvoiceDocument(invoiceId, fileContent, filename, contentType) }

    override suspend fun uploadExpenseReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): AttachmentId = delegate { it.uploadExpenseReceipt(expenseId, fileContent, filename, contentType) }

    override suspend fun getInvoiceAttachments(invoiceId: InvoiceId): List<AttachmentDto> =
        delegate { it.getInvoiceAttachments(invoiceId) }

    override suspend fun getExpenseAttachments(expenseId: ExpenseId): List<AttachmentDto> =
        delegate { it.getExpenseAttachments(expenseId) }

    override suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): String =
        delegate { it.getAttachmentDownloadUrl(attachmentId) }

    override suspend fun deleteAttachment(attachmentId: AttachmentId) =
        delegate { it.deleteAttachment(attachmentId) }

    override suspend fun getCashflowOverview(
        fromDate: kotlinx.datetime.LocalDate,
        toDate: kotlinx.datetime.LocalDate
    ): CashflowOverview = delegate { it.getCashflowOverview(fromDate, toDate) }
}
