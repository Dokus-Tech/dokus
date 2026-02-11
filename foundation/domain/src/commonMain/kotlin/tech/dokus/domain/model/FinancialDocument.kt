package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.CreditNoteStatus
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PurchaseOrderStatus
import tech.dokus.domain.enums.QuoteStatus
import tech.dokus.domain.enums.RefundClaimStatus
import tech.dokus.domain.enums.SettlementIntent
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.InvoiceNumber
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.ProFormaId
import tech.dokus.domain.ids.PurchaseOrderId
import tech.dokus.domain.ids.QuoteId
import tech.dokus.domain.ids.RefundClaimId
import tech.dokus.domain.ids.TenantId

/**
 * Sealed interface representing a financial document such as an invoice or expense.
 * This abstraction allows unified handling of documents in the cashflow system.
 *
 * Use [InvoiceDto] for invoices with explicit direction.
 * Use [ExpenseDto] for expenses/receipts (Cash-Out: money you spent).
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
        val direction: DocumentDirection = DocumentDirection.Outbound,
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

    /**
     * Quote DTO - represents a sales quotation/offer.
     * No financial impact until converted to invoice.
     */
    @Serializable
    @SerialName("Quote")
    data class QuoteDto(
        val id: QuoteId,
        override val tenantId: TenantId,
        val contactId: ContactId,
        val quoteNumber: String,
        val issueDate: LocalDate,
        val validUntil: LocalDate,
        val subtotalAmount: Money,
        val vatAmount: Money,
        val totalAmount: Money,
        val status: QuoteStatus, // DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED, CONVERTED
        override val currency: Currency = Currency.Eur,
        override val notes: String? = null,
        val termsAndConditions: String? = null,
        val items: List<String> = emptyList(),
        override val documentId: DocumentId? = null,
        val convertedToInvoiceId: InvoiceId? = null,
        override val createdAt: LocalDateTime,
        override val updatedAt: LocalDateTime
    ) : FinancialDocumentDto {
        override val date: LocalDate get() = issueDate
        override val amount: Money get() = totalAmount
    }

    /**
     * ProForma DTO - represents a pro forma invoice.
     * No financial impact - informational/customs purposes only.
     */
    @Serializable
    @SerialName("ProForma")
    data class ProFormaDto(
        val id: ProFormaId,
        override val tenantId: TenantId,
        val contactId: ContactId,
        val proFormaNumber: String,
        val issueDate: LocalDate,
        val subtotalAmount: Money,
        val vatAmount: Money,
        val totalAmount: Money,
        override val currency: Currency = Currency.Eur,
        override val notes: String? = null,
        val items: List<String> = emptyList(),
        override val documentId: DocumentId? = null,
        val relatedInvoiceId: InvoiceId? = null, // If converted
        override val createdAt: LocalDateTime,
        override val updatedAt: LocalDateTime
    ) : FinancialDocumentDto {
        override val date: LocalDate get() = issueDate
        override val amount: Money get() = totalAmount
    }

    /**
     * PurchaseOrder DTO - represents an order to a supplier.
     * Creates expected cashflow when confirmed, actual when billed.
     */
    @Serializable
    @SerialName("PurchaseOrder")
    data class PurchaseOrderDto(
        val id: PurchaseOrderId,
        override val tenantId: TenantId,
        val supplierId: ContactId,
        val poNumber: String,
        val orderDate: LocalDate,
        val expectedDeliveryDate: LocalDate? = null,
        val subtotalAmount: Money,
        val vatAmount: Money,
        val totalAmount: Money,
        val status: PurchaseOrderStatus, // DRAFT, SENT, CONFIRMED, PARTIALLY_RECEIVED, RECEIVED, CANCELLED
        override val currency: Currency = Currency.Eur,
        override val notes: String? = null,
        val items: List<String> = emptyList(),
        override val documentId: DocumentId? = null,
        val linkedInvoiceIds: List<InvoiceId> = emptyList(),
        override val createdAt: LocalDateTime,
        override val updatedAt: LocalDateTime
    ) : FinancialDocumentDto {
        override val date: LocalDate get() = orderDate
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
