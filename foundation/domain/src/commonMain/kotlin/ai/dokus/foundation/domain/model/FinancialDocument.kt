package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.Percentage
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.BillStatus
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.ids.BillId
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.DocumentId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.PeppolId
import ai.dokus.foundation.domain.ids.TenantId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed interface representing a financial document that can be an Invoice, Expense, or Bill.
 * This abstraction allows unified handling of documents in the cashflow system.
 *
 * Use [InvoiceDto] for outgoing invoices (Cash-In: money you expect to receive).
 * Use [ExpenseDto] for expenses/receipts (Cash-Out: money you spent).
 * Use [BillDto] for incoming supplier invoices (Cash-Out: money you need to pay).
 */
@Serializable
sealed interface FinancialDocumentDto {
    val documentId: DocumentId?
    val tenantId: TenantId
    val date: LocalDate
    val amount: Money
    val currency: Currency
    val notes: String?
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime

    /**
     * Invoice DTO - represents an outgoing invoice document.
     * Used for transferring invoice data between backend and frontend via RPC.
     */
    @Serializable
    @SerialName("Invoice")
    data class InvoiceDto(
        val id: InvoiceId,
        override val tenantId: TenantId,
        val contactId: ContactId,
        val invoiceNumber: InvoiceNumber,
        val issueDate: LocalDate,
        val dueDate: LocalDate,
        val subtotalAmount: Money,
        val vatAmount: Money,
        val totalAmount: Money,
        val paidAmount: Money = Money.ZERO,
        val status: InvoiceStatus,
        override val currency: Currency = Currency.Eur,
        override val notes: String? = null,
        val termsAndConditions: String? = null,
        val items: List<InvoiceItemDto> = emptyList(),
        val peppolId: PeppolId? = null,
        val peppolSentAt: LocalDateTime? = null,
        val peppolStatus: PeppolStatus? = null,
        override val documentId: DocumentId? = null,
        val paymentLink: String? = null,
        val paymentLinkExpiresAt: LocalDateTime? = null,
        val paidAt: LocalDateTime? = null,
        val paymentMethod: PaymentMethod? = null,
        override val createdAt: LocalDateTime,
        override val updatedAt: LocalDateTime
    ) : FinancialDocumentDto {
        override val date: LocalDate get() = issueDate
        override val amount: Money get() = totalAmount
    }

    /**
     * Expense DTO - represents an expense document.
     * Used for transferring expense data between backend and frontend via RPC.
     */
    @Serializable
    @SerialName("Expense")
    data class ExpenseDto(
        val id: ExpenseId,
        override val tenantId: TenantId,
        override val date: LocalDate,
        val merchant: String,
        override val amount: Money,
        val vatAmount: Money? = null,
        val vatRate: VatRate? = null,
        val category: ExpenseCategory,
        val description: String? = null,
        override val documentId: DocumentId? = null,
        val contactId: ContactId? = null, // Optional vendor reference
        val isDeductible: Boolean = true,
        val deductiblePercentage: Percentage = Percentage.FULL,
        val paymentMethod: PaymentMethod? = null,
        val isRecurring: Boolean = false,
        override val notes: String? = null,
        override val currency: Currency = Currency.Eur,
        override val createdAt: LocalDateTime,
        override val updatedAt: LocalDateTime
    ) : FinancialDocumentDto

    /**
     * Bill DTO - represents an incoming supplier invoice that needs to be paid.
     * Used for Cash-Out tracking of money owed to suppliers/vendors.
     */
    @Serializable
    @SerialName("Bill")
    data class BillDto(
        val id: BillId,
        override val tenantId: TenantId,
        val supplierName: String,
        val supplierVatNumber: String? = null,
        val invoiceNumber: String? = null,
        val issueDate: LocalDate,
        val dueDate: LocalDate,
        override val amount: Money,
        val vatAmount: Money? = null,
        val vatRate: VatRate? = null,
        val status: BillStatus,
        val category: ExpenseCategory,
        val description: String? = null,
        override val documentId: DocumentId? = null,
        val contactId: ContactId? = null, // Optional vendor reference
        val paidAt: LocalDateTime? = null,
        val paidAmount: Money? = null,
        val paymentMethod: PaymentMethod? = null,
        val paymentReference: String? = null,
        override val currency: Currency = Currency.Eur,
        override val notes: String? = null,
        override val createdAt: LocalDateTime,
        override val updatedAt: LocalDateTime
    ) : FinancialDocumentDto {
        override val date: LocalDate get() = issueDate
    }
}

/**
 * Invoice line item DTO
 */
@Serializable
data class InvoiceItemDto(
    val id: String? = null,
    val invoiceId: InvoiceId? = null,
    val description: String,
    val quantity: Double,
    val unitPrice: Money,
    val vatRate: VatRate,
    val lineTotal: Money,
    val vatAmount: Money,
    val sortOrder: Int = 0
)

/**
 * Extension function to get a human-readable type name.
 */
fun FinancialDocumentDto.typeName(): String = when (this) {
    is FinancialDocumentDto.InvoiceDto -> "Invoice"
    is FinancialDocumentDto.ExpenseDto -> "Expense"
    is FinancialDocumentDto.BillDto -> "Bill"
}

/**
 * Extension function to check if document is an invoice.
 */
fun FinancialDocumentDto.isInvoice(): Boolean = this is FinancialDocumentDto.InvoiceDto

/**
 * Extension function to check if document is an expense.
 */
fun FinancialDocumentDto.isExpense(): Boolean = this is FinancialDocumentDto.ExpenseDto

/**
 * Extension function to check if document is a bill.
 */
fun FinancialDocumentDto.isBill(): Boolean = this is FinancialDocumentDto.BillDto

/**
 * Extension function to check if document is cash-in (money coming in).
 */
fun FinancialDocumentDto.isCashIn(): Boolean = this is FinancialDocumentDto.InvoiceDto

/**
 * Extension function to check if document is cash-out (money going out).
 */
fun FinancialDocumentDto.isCashOut(): Boolean =
    this is FinancialDocumentDto.ExpenseDto || this is FinancialDocumentDto.BillDto
