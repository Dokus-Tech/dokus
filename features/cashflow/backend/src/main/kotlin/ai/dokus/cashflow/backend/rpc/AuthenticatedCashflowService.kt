package ai.dokus.cashflow.backend.rpc

import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.*
import ai.dokus.foundation.domain.rpc.CashflowApi
import ai.dokus.foundation.domain.rpc.CashflowOverview
import ai.dokus.foundation.ktor.security.AuthContext
import ai.dokus.foundation.ktor.security.RequestAuthHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * Wrapper for CashflowApi that injects authentication context.
 * This allows authenticated methods to access user information via coroutine context.
 */
class AuthenticatedCashflowService(
    private val delegate: CashflowApi
) : CashflowApi {

    override suspend fun createInvoice(request: CreateInvoiceRequest): Invoice {
        return withAuthContext {
            delegate.createInvoice(request)
        }
    }

    override suspend fun getInvoice(id: InvoiceId): Invoice {
        return withAuthContext {
            delegate.getInvoice(id)
        }
    }

    override suspend fun listInvoices(
        status: InvoiceStatus?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): List<Invoice> {
        return withAuthContext {
            delegate.listInvoices(status, fromDate, toDate, limit, offset)
        }
    }

    override suspend fun listOverdueInvoices(): List<Invoice> {
        return withAuthContext {
            delegate.listOverdueInvoices()
        }
    }

    override suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus) {
        withAuthContext {
            delegate.updateInvoiceStatus(invoiceId, status)
        }
    }

    override suspend fun updateInvoice(invoiceId: InvoiceId, request: CreateInvoiceRequest): Invoice {
        return withAuthContext {
            delegate.updateInvoice(invoiceId, request)
        }
    }

    override suspend fun deleteInvoice(invoiceId: InvoiceId) {
        withAuthContext {
            delegate.deleteInvoice(invoiceId)
        }
    }

    override suspend fun recordPayment(request: RecordPaymentRequest) {
        withAuthContext {
            delegate.recordPayment(request)
        }
    }

    override suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String?,
        message: String?
    ) {
        withAuthContext {
            delegate.sendInvoiceEmail(invoiceId, recipientEmail, message)
        }
    }

    override suspend fun markInvoiceAsSent(invoiceId: InvoiceId) {
        withAuthContext {
            delegate.markInvoiceAsSent(invoiceId)
        }
    }

    override suspend fun calculateInvoiceTotals(items: List<InvoiceItem>): InvoiceTotals {
        return withAuthContext {
            delegate.calculateInvoiceTotals(items)
        }
    }

    override fun watchInvoices(tenantId: TenantId): Flow<Invoice> {
        return delegate.watchInvoices(tenantId)
    }

    // ============================================================================
    // EXPENSE MANAGEMENT
    // ============================================================================

    override suspend fun createExpense(request: CreateExpenseRequest): Expense {
        return withAuthContext {
            delegate.createExpense(request)
        }
    }

    override suspend fun getExpense(id: ExpenseId): Expense {
        return withAuthContext {
            delegate.getExpense(id)
        }
    }

    override suspend fun listExpenses(
        category: ExpenseCategory?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): List<Expense> {
        return withAuthContext {
            delegate.listExpenses(category, fromDate, toDate, limit, offset)
        }
    }

    override suspend fun updateExpense(expenseId: ExpenseId, request: CreateExpenseRequest): Expense {
        return withAuthContext {
            delegate.updateExpense(expenseId, request)
        }
    }

    override suspend fun deleteExpense(expenseId: ExpenseId) {
        withAuthContext {
            delegate.deleteExpense(expenseId)
        }
    }

    override suspend fun categorizeExpense(merchant: String, description: String?): ExpenseCategory {
        return withAuthContext {
            delegate.categorizeExpense(merchant, description)
        }
    }

    override fun watchExpenses(tenantId: TenantId): Flow<Expense> {
        return delegate.watchExpenses(tenantId)
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
        return withAuthContext {
            delegate.uploadInvoiceDocument(invoiceId, fileContent, filename, contentType)
        }
    }

    override suspend fun uploadExpenseReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): AttachmentId {
        return withAuthContext {
            delegate.uploadExpenseReceipt(expenseId, fileContent, filename, contentType)
        }
    }

    override suspend fun getInvoiceAttachments(invoiceId: InvoiceId): List<Attachment> {
        return withAuthContext {
            delegate.getInvoiceAttachments(invoiceId)
        }
    }

    override suspend fun getExpenseAttachments(expenseId: ExpenseId): List<Attachment> {
        return withAuthContext {
            delegate.getExpenseAttachments(expenseId)
        }
    }

    override suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): String {
        return withAuthContext {
            delegate.getAttachmentDownloadUrl(attachmentId)
        }
    }

    override suspend fun deleteAttachment(attachmentId: AttachmentId) {
        withAuthContext {
            delegate.deleteAttachment(attachmentId)
        }
    }

    // ============================================================================
    // STATISTICS & OVERVIEW
    // ============================================================================

    override suspend fun getCashflowOverview(
        fromDate: LocalDate,
        toDate: LocalDate
    ): CashflowOverview {
        return withAuthContext {
            delegate.getCashflowOverview(fromDate, toDate)
        }
    }

    /**
     * Execute a block with authentication context injected into the coroutine context.
     * This allows the block to access the authenticated user via requireAuthenticationInfo().
     */
    private suspend fun <T> withAuthContext(block: suspend () -> T): T {
        val authInfo = RequestAuthHolder.get()
        return if (authInfo != null) {
            withContext(AuthContext(authInfo)) {
                block()
            }
        } else {
            block()
        }
    }
}
