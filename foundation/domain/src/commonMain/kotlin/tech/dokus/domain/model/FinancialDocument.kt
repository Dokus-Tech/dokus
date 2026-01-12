package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.BillStatus
import tech.dokus.domain.enums.CreditNoteStatus
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.RefundClaimStatus
import tech.dokus.domain.enums.SettlementIntent
import tech.dokus.domain.ids.BillId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.InvoiceNumber
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.RefundClaimId
import tech.dokus.domain.ids.TenantId

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

    /**
     * CreditNote DTO - represents a credit note (sales or purchase).
     * Sales credit notes reduce receivables, purchase credit notes reduce payables.
     * No direct cashflow impact - cashflow only when refund is recorded.
     */
    @Serializable
    @SerialName("CreditNote")
    data class CreditNoteDto(
        val id: CreditNoteId,
        override val tenantId: TenantId,
        val contactId: ContactId,
        val creditNoteType: CreditNoteType,
        val creditNoteNumber: String,
        val issueDate: LocalDate,
        val subtotalAmount: Money,
        val vatAmount: Money,
        val totalAmount: Money,
        val status: CreditNoteStatus,
        val settlementIntent: SettlementIntent,
        override val documentId: DocumentId? = null,
        val reason: String? = null,
        override val currency: Currency = Currency.Eur,
        override val notes: String? = null,
        override val createdAt: LocalDateTime,
        override val updatedAt: LocalDateTime
    ) : FinancialDocumentDto {
        override val date: LocalDate get() = issueDate
        override val amount: Money get() = totalAmount
    }
}

/**
 * RefundClaim DTO - represents an expected refund from a credit note.
 */
@Serializable
data class RefundClaimDto(
    val id: RefundClaimId,
    val tenantId: TenantId,
    val creditNoteId: CreditNoteId,
    val counterpartyId: ContactId,
    val amount: Money,
    val currency: Currency,
    val expectedDate: LocalDate? = null,
    val status: RefundClaimStatus,
    val settledAt: LocalDateTime? = null,
    val cashflowEntryId: CashflowEntryId? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

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
 * Extension function to check if document is a credit note.
 */
fun FinancialDocumentDto.isCreditNote(): Boolean = this is FinancialDocumentDto.CreditNoteDto

/**
 * Extension function to check if document is cash-in (money coming in).
 * Note: CreditNotes are NOT cash-in/out until refund is recorded.
 */
fun FinancialDocumentDto.isCashIn(): Boolean = this is FinancialDocumentDto.InvoiceDto

/**
 * Extension function to check if document is cash-out (money going out).
 * Note: CreditNotes are NOT cash-in/out until refund is recorded.
 */
fun FinancialDocumentDto.isCashOut(): Boolean =
    this is FinancialDocumentDto.ExpenseDto || this is FinancialDocumentDto.BillDto
