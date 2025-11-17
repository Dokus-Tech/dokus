package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.ExpenseId
import ai.dokus.foundation.domain.InvoiceId
import ai.dokus.foundation.domain.InvoiceNumber
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.ExpenseCategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Financial document status representing the approval workflow for uploaded documents.
 * This applies to both invoices and expenses that are uploaded by users.
 */
@Serializable
enum class FinancialDocumentStatus {
    /** Document has been uploaded but needs review/approval */
    PendingApproval,

    /** Document has been approved and is active */
    Approved,

    /** Document has been rejected during review */
    Rejected,

    /** Document is in draft state (not yet submitted for approval) */
    Draft,

    /** Document processing is complete (e.g., invoice paid, expense reimbursed) */
    Completed,

    /** Document has been cancelled/voided */
    Cancelled
}

/**
 * Sealed interface representing a financial document that can be either an Invoice or an Expense.
 * This abstraction allows unified handling of uploaded documents in the cashflow system.
 */
@Serializable
sealed interface FinancialDocument {
    // Shared properties across all document types
    val documentId: String
    val tenantId: TenantId
    val documentNumber: String
    val date: LocalDate
    val amount: Money
    val currency: Currency
    val status: FinancialDocumentStatus
    val description: String?
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime

    /**
     * Represents an invoice document.
     * Invoices are outgoing documents (money you expect to receive).
     */
    @Serializable
    data class InvoiceDocument(
        override val documentId: String,
        override val tenantId: TenantId,
        override val documentNumber: String,
        override val date: LocalDate,
        override val amount: Money,
        override val currency: Currency,
        override val status: FinancialDocumentStatus,
        override val description: String?,
        override val createdAt: LocalDateTime,
        override val updatedAt: LocalDateTime,

        // Invoice-specific fields
        val invoiceId: InvoiceId,
        val clientId: ClientId,
        val invoiceNumber: InvoiceNumber,
        val dueDate: LocalDate,
        val subtotalAmount: Money,
        val vatAmount: Money,
        val paidAmount: Money,
        val items: List<InvoiceItem> = emptyList()
    ) : FinancialDocument

    /**
     * Represents an expense document.
     * Expenses are incoming documents (money you spent).
     */
    @Serializable
    data class ExpenseDocument(
        override val documentId: String,
        override val tenantId: TenantId,
        override val documentNumber: String,
        override val date: LocalDate,
        override val amount: Money,
        override val currency: Currency,
        override val status: FinancialDocumentStatus,
        override val description: String?,
        override val createdAt: LocalDateTime,
        override val updatedAt: LocalDateTime,

        // Expense-specific fields
        val expenseId: ExpenseId,
        val merchant: String,
        val category: ExpenseCategory,
        val receiptUrl: String?,
        val vatAmount: Money?,
        val isDeductible: Boolean
    ) : FinancialDocument
}

/**
 * Extension function to check if a document needs approval.
 */
fun FinancialDocument.needsApproval(): Boolean = status == FinancialDocumentStatus.PendingApproval

/**
 * Extension function to check if a document is approved.
 */
fun FinancialDocument.isApproved(): Boolean = status == FinancialDocumentStatus.Approved

/**
 * Extension function to check if a document is pending (either draft or pending approval).
 */
fun FinancialDocument.isPending(): Boolean =
    status == FinancialDocumentStatus.Draft || status == FinancialDocumentStatus.PendingApproval

/**
 * Extension function to get a human-readable type name.
 */
fun FinancialDocument.typeName(): String = when (this) {
    is FinancialDocument.InvoiceDocument -> "Invoice"
    is FinancialDocument.ExpenseDocument -> "Expense"
}

/**
 * Extension function to get the document icon/emoji representation.
 */
fun FinancialDocument.typeIcon(): String = when (this) {
    is FinancialDocument.InvoiceDocument -> "📄"
    is FinancialDocument.ExpenseDocument -> "🧾"
}
